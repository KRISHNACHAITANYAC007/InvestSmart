package com.example.investsmart.security;

import com.example.investsmart.entity.User;
import com.example.investsmart.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import jakarta.servlet.http.*;
import java.net.URI;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Online second-factor code screen. Its cookie never authorizes application APIs. */
@Service
public class PhoneVerification implements CredentialRepository {
    public static final String LOGIN_COOKIE="INVESTSMART_PHONE_LOGIN";
    public static final String SETUP_COOKIE="INVESTSMART_PHONE_SETUP";
    public static final String DISPLAY_COOKIE="INVESTSMART_PHONE_DISPLAY";
    private final JdbcTemplate db;
    private final UserRepository users;
    private final SecurityCrypto crypto;
    private final SecurityEvents events;
    private final AccountMail mail;
    private final PasswordEncoder passwords;
    private final boolean secure;
    private final String base;
    private final RelyingParty rp;
    private final ObjectMapper json=JsonMapper.builder().addModule(new Jdk8Module()).addModule(new JavaTimeModule()).build();
    public record Ceremony(String challenge,String publicKey) {}
    public record Start(String state,String challenge,long mailId) {}
    public record Setup(List<String> recoveryCodes) {}
    public record Code(String code,Instant expiresAt,Instant serverTime) {}

    public PhoneVerification(JdbcTemplate db,UserRepository users,SecurityCrypto crypto,SecurityEvents events,
            AccountMail mail,PasswordEncoder passwords,SecuritySchema schema,
            @Value("${investsmart.security.secure-cookie:false}") boolean secure,
            @Value("${investsmart.public-url:http://localhost:5173}") String base) {
        this.db=db;this.users=users;this.crypto=crypto;this.events=events;this.mail=mail;this.passwords=passwords;this.secure=secure;
        this.base=DevelopmentOrigin.canonical(base).replaceAll("/+$","");
        URI origin=URI.create(this.base);
        if(origin.getHost()==null||origin.getUserInfo()!=null||origin.getQuery()!=null||origin.getFragment()!=null
                ||!origin.getPath().isEmpty()||!("https".equals(origin.getScheme())
                ||("http".equals(origin.getScheme())&&Set.of("localhost","127.0.0.1","::1").contains(origin.getHost()))))
            throw new IllegalStateException("Phone verification requires an exact HTTPS public origin (localhost allowed for development).");
        rp=RelyingParty.builder().identity(RelyingPartyIdentity.builder().id(origin.getHost()).name("InvestSmart phone verification").build())
                .credentialRepository(this).origins(Set.of(this.base)).preferredPubkeyParams(List.of(PublicKeyCredentialParameters.ES256,PublicKeyCredentialParameters.RS256)).build();
    }

    public boolean enrolled(Long id){return count("SELECT COUNT(*) FROM phone_credentials WHERE user_id=? AND ready=TRUE",id)>0;}

    /** Called after the password and account state have been checked under the user lock. */
    public Start start(User user,HttpServletRequest request,HttpServletResponse response){
        regular(user);
        events.limit("PHONE_LOGIN",user.getEmail(),request,10,50,user.getId());
        String binding=crypto.randomToken();
        String challenge=action(user.getId(),"LOGIN",600,null,crypto.fingerprint(binding),null);
        cookie(response,LOGIN_COOKIE,binding,600);
        long mailId=0;
        if(!enrolled(user.getId())){
            events.limit("PHONE_PAIR",user.getEmail(),request,3,20,user.getId());
            String token=crypto.randomToken();
            db.update("UPDATE auth_actions SET used=TRUE,payload=NULL WHERE user_id=? AND purpose='PHONE_PAIR'",user.getId());
            db.update("INSERT INTO auth_actions(token_hash,user_id,purpose,expires_at,payload) VALUES(?,?,'PHONE_PAIR',?,?)",
                    crypto.fingerprint(token),user.getId(),time(600),crypto.encrypt(crypto.fingerprint(challenge)));
            mailId=mail.queue(user.getEmail(),"PHONE_PAIR",token);
        }
        return new Start(enrolled(user.getId())?"PHONE_REQUIRED":"PHONE_SETUP",challenge,mailId);
    }

    @Transactional public void openPairing(String token,HttpServletResponse response){
        String hash=crypto.fingerprint(token);
        var ids=db.queryForList("SELECT user_id FROM auth_actions WHERE token_hash=? AND purpose='PHONE_PAIR'",Long.class,hash);
        if(ids.isEmpty())throw invalid();
        User user=lock(ids.getFirst());
        var rows=db.queryForList("SELECT * FROM auth_actions WHERE token_hash=? AND purpose='PHONE_PAIR' AND used=FALSE AND expires_at>? FOR UPDATE",hash,Timestamp.from(Instant.now()));
        if(rows.isEmpty()||enrolled(user.getId()))throw invalid();
        var row=rows.getFirst();
        String parent=crypto.decrypt((String)row.get("payload"));
        activeParent(parent,user.getId());
        db.update("UPDATE auth_actions SET used=TRUE,payload=NULL WHERE token_hash=?",hash);
        String setup=action(user.getId(),"ENROLL",600,parent,null,null);
        cookie(response,SETUP_COOKIE,setup,600);
    }

    @Transactional public Ceremony registrationOptions(HttpServletRequest request){
        var setup=setup(request);User user=lock(id(setup));
        if(enrolled(user.getId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"A phone credential already exists. Use phone replacement.");
        events.limit("PHONE_REGISTER",user.getEmail(),request,5,30,user.getId());
        var identity=UserIdentity.builder().name(user.getEmail()).displayName(user.getName()).id(handle(user)).build();
        var selection=AuthenticatorSelectionCriteria.builder().authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                .residentKey(ResidentKeyRequirement.REQUIRED).userVerification(UserVerificationRequirement.REQUIRED).build();
        var options=rp.startRegistration(StartRegistrationOptions.builder().user(identity).authenticatorSelection(selection).build());
        try{return ceremony(user,"REGISTER",hashCookie(request,SETUP_COOKIE),options.toCredentialsCreateJson());}
        catch(java.io.IOException e){throw new IllegalStateException("Could not create phone registration options.",e);}
    }

    @Transactional(noRollbackFor=ResponseStatusException.class)
    public Setup register(String challenge,String credential,HttpServletRequest request){
        var setup=setup(request);User user=lock(id(setup));
        var ceremony=consume(challenge,"REGISTER",hashCookie(request,SETUP_COOKIE));
        if(enrolled(user.getId()))throw invalid();
        try{
            var options=options((String)ceremony.get("payload"),PublicKeyCredentialCreationOptions.class);
            var result=rp.finishRegistration(FinishRegistrationOptions.builder().request(options)
                    .response(PublicKeyCredential.parseRegistrationResponseJson(credential)).build());
            if(!result.isUserVerified())throw invalid();
            List<String> recovery=new ArrayList<>();for(int i=0;i<10;i++)recovery.add(crypto.randomToken());
            db.update("DELETE FROM phone_credentials WHERE user_id=? AND ready=FALSE",user.getId());
            String credentialId=result.getKeyId().getId().getBase64Url();
            db.update("INSERT INTO phone_credentials(user_id,credential_hash,credential_id,user_handle,public_key_cose,signature_count,secret_cipher,last_step,recovery_hashes,ready,created_at) VALUES(?,?,?,?,?,?,?,-1,?,FALSE,?)",
                    user.getId(),crypto.fingerprint(credentialId),credentialId,handle(user).getBase64Url(),result.getPublicKeyCose().getBase64Url(),result.getSignatureCount(),
                    crypto.encrypt(crypto.newTotpSecret()),String.join(",",recovery.stream().map(crypto::fingerprint).toList()),Timestamp.from(Instant.now()));
            db.update("UPDATE phone_actions SET payload=? WHERE token_hash=?",crypto.encrypt(crypto.fingerprint(credentialId)),hashCookie(request,SETUP_COOKIE));
            events.record("PHONE_ENROLLED",user.getId(),user.getEmail(),request,"Phone credential created; recovery acknowledgement pending");
            return new Setup(recovery);
        }catch(ResponseStatusException e){throw e;}
        catch(Exception e){failure(user,request);throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Phone pairing failed. Start pairing again.");}
    }

    @Transactional public void acknowledge(HttpServletRequest request,HttpServletResponse response){
        var setup=setup(request);User user=lock(id(setup));
        if(setup.get("payload")==null||count("SELECT COUNT(*) FROM phone_credentials WHERE user_id=? AND credential_hash=? AND ready=FALSE",user.getId(),crypto.decrypt((String)setup.get("payload")))!=1)throw invalid();
        db.update("UPDATE phone_credentials SET ready=TRUE WHERE user_id=?",user.getId());
        db.update("UPDATE phone_actions SET used=TRUE,payload=NULL WHERE token_hash=?",hashCookie(request,SETUP_COOKIE));
        display(user,response);cookie(response,SETUP_COOKIE,"",0);
    }

    /** Only discoverable credentials in the dedicated phone repository are accepted. */
    @Transactional public Ceremony displayOptions(HttpServletRequest request){
        events.limit("PHONE_OPEN",events.source(request),request,30,30);
        var options=rp.startAssertion(StartAssertionOptions.builder().userVerification(UserVerificationRequirement.REQUIRED).build());
        try{return new Ceremony(saveAction(0L,"DISPLAY",180,null,null,options.toJson()),options.toCredentialsGetJson());}
        catch(java.io.IOException e){throw new IllegalStateException("Could not create phone verification options.",e);}
    }

    @Transactional(noRollbackFor=ResponseStatusException.class)
    public void openDisplay(String challenge,String credential,HttpServletRequest request,HttpServletResponse response){
        // Lock the credential's user before consuming/validating the challenge.
        var user=assertionUser(credential);events.mfaAllowed(user.getId());events.loginAllowed(user.getEmail(),request);
        var row=consume(challenge,"DISPLAY",null);
        assertion(user,row,credential,request);
        display(user,response);
    }

    @Transactional public Code code(HttpServletRequest request){
        String hash=hashCookie(request,DISPLAY_COOKIE);
        var ids=db.queryForList("SELECT user_id FROM phone_sessions WHERE id_hash=? AND expires_at>?",Long.class,hash,Timestamp.from(Instant.now()));
        if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Open the code screen with your phone biometric or PIN.");
        User user=lock(ids.getFirst());
        var rows=db.queryForList("SELECT c.secret_cipher FROM phone_sessions s JOIN phone_credentials c ON c.user_id=s.user_id AND c.credential_hash=s.credential_hash WHERE s.id_hash=? AND s.expires_at>? AND c.ready=TRUE",hash,Timestamp.from(Instant.now()));
        if(rows.isEmpty())throw invalid();
        Instant now=Instant.now();long step=now.getEpochSecond()/30;
        return new Code(SecurityCrypto.totp(crypto.decrypt((String)rows.getFirst().get("secret_cipher")),step),Instant.ofEpochSecond((step+1)*30),now);
    }

    @Transactional public User verifyCode(String challenge,String code,HttpServletRequest request){
        var row=login(challenge,request);User user=lock(id(row));
        if(code==null||!code.matches("[0-9]{6}")){failure(user,request);throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Use a six-digit phone code, or choose lost-phone recovery.");}
        verify(user,code,false,request);
        finishLogin(challenge,user,request);
        return user;
    }

    @Transactional public Ceremony loginOptions(String challenge,HttpServletRequest request){
        var row=login(challenge,request);User user=lock(id(row));
        if(!enrolled(user.getId()))throw invalid();
        events.mfaAllowed(user.getId());events.loginAllowed(user.getEmail(),request);
        events.limit("PHONE_ASSERT",user.getEmail(),request,15,50,user.getId());
        var options=rp.startAssertion(StartAssertionOptions.builder().username(user.getEmail()).userVerification(UserVerificationRequirement.REQUIRED).build());
        try{return new Ceremony(saveAction(user.getId(),"LOGIN_ASSERT",180,crypto.fingerprint(challenge),null,options.toJson()),options.toCredentialsGetJson());}
        catch(java.io.IOException e){throw new IllegalStateException("Could not create phone login options.",e);}
    }

    @Transactional(noRollbackFor=ResponseStatusException.class)
    public User verifyLoginPasskey(String challenge,String credential,String loginChallenge,HttpServletRequest request){
        var row=login(loginChallenge,request);User user=lock(id(row));
        events.mfaAllowed(user.getId());events.loginAllowed(user.getEmail(),request);
        assertion(user,consume(challenge,"LOGIN_ASSERT",crypto.fingerprint(loginChallenge)),credential,request);
        finishLogin(loginChallenge,user,request);return user;
    }

    @Transactional public String recover(String challenge,String recovery,HttpServletRequest request){
        var row=login(challenge,request);User user=lock(id(row));
        verify(user,recovery,true,request);
        return replacement(user,crypto.fingerprint(challenge),request);
    }

    @Transactional public String replace(User authenticated,String password,String code,HttpServletRequest request){
        User user=lock(authenticated.getId());
        events.loginAllowed(user.getEmail(),request);
        if(password==null||!passwords.matches(password,user.getPassword())){
            events.record("LOGIN_FAILURE",user.getId(),user.getEmail(),request,"Phone replacement password rejected");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid password.");
        }
        verify(user,code,false,request);
        String challenge=action(user.getId(),"REPLACE",600,null,null,null);
        return replacement(user,crypto.fingerprint(challenge),request);
    }

    private String replacement(User user,String parent,HttpServletRequest request){
        // Keep the authorized replacement transaction; revoke every other capability.
        db.update("UPDATE auth_sessions SET revoked=TRUE WHERE user_id=?",user.getId());
        db.update("DELETE FROM phone_sessions WHERE user_id=?",user.getId());
        db.update("DELETE FROM phone_credentials WHERE user_id=?",user.getId());
        db.update("UPDATE auth_actions SET used=TRUE,payload=NULL WHERE user_id=? AND purpose IN ('PHONE_PAIR','MFA_LOGIN','MFA_SETUP')",user.getId());
        db.update("UPDATE phone_actions SET used=TRUE,payload=NULL WHERE user_id=? AND token_hash<>?",user.getId(),parent);
        String setup=action(user.getId(),"ENROLL",600,parent,null,null);
        events.record("PHONE_REPLACED",user.getId(),user.getEmail(),request,"Old credential and sessions revoked; replacement pending");
        return base+"/phone#setup="+setup;
    }

    @Transactional public void openReplacement(String token,HttpServletResponse response){
        var row=read(token,"ENROLL");lock(id(row));activeParent((String)row.get("parent_hash"),id(row));
        // Exchange the bearer link once, replacing it with a setup cookie.
        consume(token,"ENROLL",null);
        String setup=action(id(row),"ENROLL",600,(String)row.get("parent_hash"),null,null);
        cookie(response,SETUP_COOKIE,setup,600);
    }

    public Map<String,Object> status(User user){
        regular(user);
        var rows=db.queryForList("SELECT created_at,last_used_at FROM phone_credentials WHERE user_id=? AND ready=TRUE",user.getId());
        if(rows.isEmpty())return Map.of("enrolled",false);
        Map<String,Object> result=new LinkedHashMap<>();result.put("enrolled",true);
        result.put("createdAt",rows.getFirst().get("created_at"));result.put("lastUsedAt",rows.getFirst().get("last_used_at"));return result;
    }

    public void close(HttpServletRequest request,HttpServletResponse response){
        db.update("DELETE FROM phone_sessions WHERE id_hash=?",hashCookie(request,DISPLAY_COOKIE));
        cookie(response,DISPLAY_COOKIE,"",0);cookie(response,SETUP_COOKIE,"",0);
    }

    private void verify(User user,String code,boolean recoveryOnly,HttpServletRequest request){
        events.mfaAllowed(user.getId());events.loginAllowed(user.getEmail(),request);
        var rows=db.queryForList("SELECT * FROM phone_credentials WHERE user_id=? AND ready=TRUE FOR UPDATE",user.getId());
        if(rows.isEmpty())throw invalid();var row=rows.getFirst();
        long step=Instant.now().getEpochSecond()/30;
        if(!recoveryOnly&&code!=null&&code.matches("[0-9]{6}")&&step>((Number)row.get("last_step")).longValue()
                &&java.security.MessageDigest.isEqual(SecurityCrypto.totp(crypto.decrypt((String)row.get("secret_cipher")),step).getBytes(java.nio.charset.StandardCharsets.UTF_8),code.getBytes(java.nio.charset.StandardCharsets.UTF_8))){
            db.update("UPDATE phone_credentials SET last_step=?,last_used_at=? WHERE user_id=?",step,Timestamp.from(Instant.now()),user.getId());return;
        }
        if(recoveryOnly||code!=null&&!code.matches("[0-9]{6}")){
            var hashes=new ArrayList<>(Arrays.asList(((String)row.get("recovery_hashes")).split(",")));
            if(code!=null&&hashes.remove(crypto.fingerprint(code))){
                db.update("UPDATE phone_credentials SET recovery_hashes=? WHERE user_id=?",String.join(",",hashes),user.getId());
                events.record("RECOVERY_CODE_USED",user.getId(),user.getEmail(),request,"Phone replacement authorized");return;
            }
        }
        failure(user,request);throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid, expired or already used phone code. Use the next code, or recover access with a backup code.");
    }

    private void assertion(User user,Map<String,Object> row,String credential,HttpServletRequest request){
        try{
            var assertion=json.readValue(crypto.decrypt((String)row.get("payload")),AssertionRequest.class);
            var result=rp.finishAssertion(FinishAssertionOptions.builder().request(assertion).response(PublicKeyCredential.parseAssertionResponseJson(credential)).build());
            if(!result.isSuccess()||!result.isUserVerified()||!user.getEmail().equalsIgnoreCase(result.getUsername()))throw invalid();
            if(db.update("UPDATE phone_credentials SET signature_count=?,last_used_at=? WHERE user_id=? AND credential_hash=? AND ready=TRUE",result.getSignatureCount(),Timestamp.from(Instant.now()),user.getId(),crypto.fingerprint(result.getCredential().getCredentialId().getBase64Url()))!=1)throw invalid();
        }catch(Exception e){failure(user,request);throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Phone verification failed. Try again.");}
    }

    private User assertionUser(String credential){
        try{
            String credentialId=PublicKeyCredential.parseAssertionResponseJson(credential).getId().getBase64Url();
            var ids=db.queryForList("SELECT user_id FROM phone_credentials WHERE credential_hash=? AND ready=TRUE",Long.class,crypto.fingerprint(credentialId));
            if(ids.isEmpty())throw invalid();return lock(ids.getFirst());
        }catch(java.io.IOException e){throw invalid();}
    }
    private Map<String,Object> login(String token,HttpServletRequest request){
        var row=read(token,"LOGIN");lock(id(row));row=read(token,"LOGIN");
        if(!hashCookie(request,LOGIN_COOKIE).equals(row.get("binding_hash")))throw invalid();return row;
    }
    private void finishLogin(String challenge,User user,HttpServletRequest request){
        consume(challenge,"LOGIN",null);
        events.record("LOGIN_SUCCESS",user.getId(),user.getEmail(),request,"Password and phone verification");
    }
    private Map<String,Object> setup(HttpServletRequest request){
        String token=cookieValue(request,SETUP_COOKIE);var row=read(token,"ENROLL");lock(id(row));row=read(token,"ENROLL");activeParent((String)row.get("parent_hash"),id(row));return row;
    }
    private void activeParent(String hash,long user){
        if(count("SELECT COUNT(*) FROM phone_actions WHERE token_hash=? AND user_id=? AND purpose IN ('LOGIN','REPLACE') AND used=FALSE AND expires_at>?",hash,user,Timestamp.from(Instant.now()))==0)throw invalid();
    }
    private Map<String,Object> read(String token,String purpose){
        var rows=db.queryForList("SELECT * FROM phone_actions WHERE token_hash=? AND purpose=? AND used=FALSE AND expires_at>?",crypto.fingerprint(token==null?"":token),purpose,Timestamp.from(Instant.now()));
        if(rows.isEmpty())throw invalid();return rows.getFirst();
    }
    private Map<String,Object> consume(String token,String purpose,String parent){
        var row=read(token,purpose);
        if(parent!=null&&!parent.equals(row.get("parent_hash")))throw invalid();
        if(db.update("UPDATE phone_actions SET used=TRUE WHERE token_hash=? AND used=FALSE AND expires_at>?",crypto.fingerprint(token),Timestamp.from(Instant.now()))!=1)throw invalid();return row;
    }
    private Ceremony ceremony(User user,String purpose,String parent,String options){
        return new Ceremony(saveAction(user.getId(),purpose,180,parent,null,options),options);
    }
    private String saveAction(long user,String purpose,int seconds,String parent,String binding,String plain){return action(user,purpose,seconds,parent,binding,crypto.encrypt(plain));}
    private String action(long user,String purpose,int seconds,String parent,String binding,String payload){
        String token=crypto.randomToken();db.update("INSERT INTO phone_actions(token_hash,user_id,purpose,parent_hash,binding_hash,payload,expires_at) VALUES(?,?,?,?,?,?,?)",crypto.fingerprint(token),user,purpose,parent,binding,payload,time(seconds));return token;
    }
    private void display(User user,HttpServletResponse response){
        String token=crypto.randomToken();String credential=db.queryForObject("SELECT credential_hash FROM phone_credentials WHERE user_id=? AND ready=TRUE",String.class,user.getId());
        db.update("INSERT INTO phone_sessions(id_hash,user_id,credential_hash,expires_at) VALUES(?,?,?,?)",crypto.fingerprint(token),user.getId(),credential,time(300));cookie(response,DISPLAY_COOKIE,token,300);
    }
    private <T>T options(String encrypted,Class<T> type)throws java.io.IOException{var root=json.readTree(crypto.decrypt(encrypted));return json.treeToValue(root.has("publicKey")?root.get("publicKey"):root,type);}
    private User lock(long id){User user=users.findByIdForUpdate(id).orElseThrow(this::invalid);regular(user);if(user.isPasswordResetRequired())throw invalid();return user;}
    private void regular(User user){user.requireActive();if(!user.isEmailVerified()||!"USER".equals(user.getRole())||count("SELECT COUNT(*) FROM owner_admin WHERE user_id=?",user.getId())>0)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Phone verification is for verified regular users. Owner Admin requires its authenticator.");}
    private void failure(User user,HttpServletRequest request){events.record("MFA_FAILURE",user.getId(),"mfa:"+user.getId(),request,"Phone verification rejected");events.record("LOGIN_FAILURE",user.getId(),user.getEmail(),request,"Phone verification rejected");}
    private long id(Map<String,Object> row){return ((Number)row.get("user_id")).longValue();}
    private Timestamp time(int seconds){return Timestamp.from(Instant.now().plusSeconds(seconds));}
    private int count(String sql,Object... parameters){return Objects.requireNonNull(db.queryForObject(sql,Integer.class,parameters));}
    private ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"This phone challenge is invalid or expired. Sign in again to restart.");}
    private String cookieValue(HttpServletRequest request,String name){if(request.getCookies()!=null)for(Cookie c:request.getCookies())if(name.equals(c.getName()))return c.getValue();return "";}
    private String hashCookie(HttpServletRequest request,String name){return crypto.fingerprint(cookieValue(request,name));}
    private void cookie(HttpServletResponse response,String name,String value,int seconds){response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from(name,value).httpOnly(true).secure(secure).sameSite("Strict").path("/").maxAge(seconds).build().toString());}
    private ByteArray handle(User user){return new ByteArray(HexFormat.of().parseHex(crypto.fingerprint("phone-user:"+user.getId())));}

    @Scheduled(fixedDelay=3600000) @Transactional public void cleanup(){
        db.update("DELETE FROM phone_sessions WHERE expires_at<?",Timestamp.from(Instant.now()));
        db.update("DELETE FROM phone_actions WHERE expires_at<?",Timestamp.from(Instant.now()));
    }
    @Override public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username){return new HashSet<>(db.query("SELECT c.credential_id FROM phone_credentials c JOIN users u ON u.id=c.user_id WHERE LOWER(u.email)=LOWER(?) AND c.ready=TRUE",(rs,n)->PublicKeyCredentialDescriptor.builder().id(decode(rs.getString(1))).build(),username));}
    @Override public Optional<ByteArray> getUserHandleForUsername(String username){return users.findByEmailIgnoreCase(username).map(this::handle);}
    @Override public Optional<String> getUsernameForUserHandle(ByteArray handle){return db.query("SELECT u.email FROM users u JOIN phone_credentials c ON c.user_id=u.id WHERE c.user_handle=? AND c.ready=TRUE",rs->rs.next()?Optional.of(rs.getString(1)):Optional.empty(),handle.getBase64Url());}
    @Override public Optional<RegisteredCredential> lookup(ByteArray id,ByteArray handle){return lookupAll(id).stream().filter(c->c.getUserHandle().equals(handle)).findFirst();}
    @Override public Set<RegisteredCredential> lookupAll(ByteArray id){return new HashSet<>(db.query("SELECT * FROM phone_credentials WHERE credential_hash=? AND ready=TRUE",(rs,n)->RegisteredCredential.builder().credentialId(decode(rs.getString("credential_id"))).userHandle(decode(rs.getString("user_handle"))).publicKeyCose(decode(rs.getString("public_key_cose"))).signatureCount(rs.getLong("signature_count")).build(),crypto.fingerprint(id.getBase64Url())));}
    private ByteArray decode(String value){try{return ByteArray.fromBase64Url(value);}catch(com.yubico.webauthn.data.exception.Base64UrlException e){throw new IllegalStateException("Stored phone credential is invalid.",e);}}
}
