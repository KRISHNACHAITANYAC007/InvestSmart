package com.example.investsmart.security;

import com.example.investsmart.entity.User;
import com.example.investsmart.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import com.yubico.webauthn.data.exception.Base64UrlException;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasskeyService implements CredentialRepository {
    public record Passkey(String id, Instant createdAt, Instant lastUsedAt) {}
    public record Ceremony(String challenge, String publicKey) {}

    private final JdbcTemplate db;
    private final UserRepository users;
    private final SecurityAccounts accounts;
    private final SecurityCrypto crypto;
    private final SecurityEvents events;
    private final RelyingParty relyingParty;
    private final TransactionTemplate independent;
    private final ObjectMapper json = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .defaultPropertyInclusion(JsonInclude.Value.construct(
                    JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_ABSENT))
            .defaultBase64Variant(Base64Variants.MODIFIED_FOR_URL)
            .addModule(new Jdk8Module())
            .addModule(new JavaTimeModule())
            .build();

    public PasskeyService(JdbcTemplate db, UserRepository users, SecurityAccounts accounts,
            SecurityCrypto crypto, SecurityEvents events, SecuritySchema schema, PlatformTransactionManager tx,
            @Value("${investsmart.public-url:http://localhost:5173}") String publicUrl) {
        Objects.requireNonNull(schema, "Security schema must be initialized before passkey service.");
        this.db=db;this.users=users;this.accounts=accounts;this.crypto=crypto;this.events=events;
        independent=new TransactionTemplate(tx);independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        URI origin=URI.create(DevelopmentOrigin.canonical(publicUrl));
        if(origin.getHost()==null||origin.getUserInfo()!=null||origin.getQuery()!=null||origin.getFragment()!=null
                ||!(origin.getPath().isEmpty()||"/".equals(origin.getPath()))
                ||!("http".equals(origin.getScheme())||"https".equals(origin.getScheme()))
                ||("http".equals(origin.getScheme())&&!Set.of("localhost","127.0.0.1","::1").contains(origin.getHost().toLowerCase(Locale.ROOT))))
            throw new IllegalStateException("Public URL must use HTTPS (HTTP is allowed only on localhost) for WebAuthn.");
        String allowedOrigin=origin.getScheme()+"://"+origin.getRawAuthority();
        var identity=RelyingPartyIdentity.builder().id(origin.getHost()).name("InvestSmart").build();
        relyingParty=RelyingParty.builder().identity(identity).credentialRepository(this)
                .origins(Set.of(allowedOrigin)).preferredPubkeyParams(List.of(
                        PublicKeyCredentialParameters.ES256,PublicKeyCredentialParameters.EdDSA,
                        PublicKeyCredentialParameters.ES384,PublicKeyCredentialParameters.ES512,
                        PublicKeyCredentialParameters.Ed448,PublicKeyCredentialParameters.RS256,
                        PublicKeyCredentialParameters.RS384,PublicKeyCredentialParameters.RS512)).build();
    }

    public List<Passkey> list(User user) {
        requireRegularUser(user);
        return db.query("SELECT credential_hash,created_at,last_used_at FROM passkey_credentials WHERE user_id=? ORDER BY created_at",
                (rs,n)->new Passkey(rs.getString("credential_hash"),rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("last_used_at")==null?null:rs.getTimestamp("last_used_at").toInstant()),user.getId());
    }

    public boolean hasPasskeys(Long userId) {
        return count("SELECT COUNT(*) FROM passkey_credentials WHERE user_id=?",userId)>0;
    }

    @Transactional public Ceremony registrationOptions(User user,HttpServletRequest request) {
        requireRegularUser(user);
        String sessionId=sessionId(request);
        requireRecentFullAuthentication(sessionId);
        events.limit("PASSKEY_REGISTER",user.getEmail(),request,5,20);
        UserIdentity identity=UserIdentity.builder().name(user.getEmail()).displayName(user.getName())
                .id(userHandle(user)).build();
        var selection=AuthenticatorSelectionCriteria.builder()
                .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                .residentKey(ResidentKeyRequirement.PREFERRED)
                .userVerification(UserVerificationRequirement.REQUIRED).build();
        var options=relyingParty.startRegistration(StartRegistrationOptions.builder().user(identity)
                .authenticatorSelection(selection).build());
        try { return saveChallenge(user,sessionId,"REGISTER",options.toCredentialsCreateJson()); }
        catch(com.fasterxml.jackson.core.JsonProcessingException failure) { throw new IllegalStateException("Could not serialize passkey registration options.",failure); }
    }

    @Transactional public void finishRegistration(User user,String challenge,String response,HttpServletRequest request) {
        requireRegularUser(user);
        var row=consumeChallenge(challenge,user,sessionId(request),"REGISTER");
        try {
            var options=browserOptions((String)row.get("options_json"),PublicKeyCredentialCreationOptions.class);
            var credential=PublicKeyCredential.parseRegistrationResponseJson(response);
            RegistrationResult result=relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(options).response(credential).build());
            if(!result.isUserVerified())throw invalidPasskey();
            var key=result.getKeyId().getId();
            String id=key.getBase64Url();
            db.update("INSERT INTO passkey_credentials(credential_hash,credential_id,user_id,user_handle,public_key_cose,signature_count,created_at) VALUES(?,?,?,?,?,?,?)",
                    crypto.fingerprint(id),id,user.getId(),userHandle(user).getBase64Url(),
                    result.getPublicKeyCose().getBase64Url(),result.getSignatureCount(),Timestamp.from(Instant.now()));
            db.update("UPDATE passkey_challenges SET used=TRUE,request_cipher='' WHERE challenge_hash=?",crypto.fingerprint(challenge));
        } catch(IOException|IllegalArgumentException|RegistrationFailedException|org.springframework.dao.DuplicateKeyException failure) {
            events.record("LOGIN_FAILURE",user.getId(),user.getEmail(),request,"Passkey registration failed");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Passkey setup could not be completed. Retry the setup.");
        }
    }

    @Transactional public void lock(User user,HttpServletRequest request) {
        requireRegularUser(user);
        if(!hasPasskeys(user.getId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Set up a passkey before locking this app.");
        String sessionId=sessionId(request);
        db.update("INSERT INTO trusted_device_locks(session_id,locked_at) VALUES(?,?)",sessionId,Timestamp.from(Instant.now()));
    }

    public Map<String,Object> lockStatus(HttpServletRequest request) {
        String sessionId=(String)request.getAttribute("sessionId");
        Long userId=(Long)request.getAttribute("userId");
        boolean locked=sessionId!=null&&count("SELECT COUNT(*) FROM trusted_device_locks WHERE session_id=?",sessionId)>0;
        boolean canUnlock=userId!=null&&hasPasskeys(userId);
        return Map.of("locked",locked,"canUnlock",canUnlock);
    }

    @Transactional public Ceremony unlockOptions(HttpServletRequest request) {
        String sessionId=sessionId(request);
        Long userId=(Long)request.getAttribute("userId");
        if(userId==null||count("SELECT COUNT(*) FROM trusted_device_locks WHERE session_id=?",sessionId)==0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"This app is not locked. Sign in with email and password if your session has expired.");
        User user=users.findByIdForUpdate(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Please sign in again."));
        user.requireActive();requireRegularUser(user);
        events.limit("PASSKEY_UNLOCK",user.getEmail(),request,10,30);
        if(!hasPasskeys(userId))throw new ResponseStatusException(HttpStatus.CONFLICT,"No passkey is available. Sign out and sign in with email and password.");
        var assertion=relyingParty.startAssertion(StartAssertionOptions.builder().username(user.getEmail())
                .userVerification(UserVerificationRequirement.REQUIRED).timeout(180000L).build());
        try { return saveChallenge(user,sessionId,"UNLOCK",assertion.toCredentialsGetJson()); }
        catch(com.fasterxml.jackson.core.JsonProcessingException failure) { throw new IllegalStateException("Could not serialize passkey assertion options.",failure); }
    }

    @Transactional public SecurityAccounts.Account unlock(String challenge,String response,HttpServletRequest request) {
        String sessionId=sessionId(request);
        Long userId=(Long)request.getAttribute("userId");
        if(userId==null||count("SELECT COUNT(*) FROM trusted_device_locks WHERE session_id=?",sessionId)==0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"This app is not locked. Sign in with email and password.");
        User user=users.findByIdForUpdate(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Please sign in again."));
        user.requireActive();requireRegularUser(user);events.loginAllowed(user.getEmail(),request);
        var row=consumeChallenge(challenge,user,sessionId,"UNLOCK");
        try {
            var options=browserOptions((String)row.get("options_json"),PublicKeyCredentialRequestOptions.class);
            var assertionRequest=AssertionRequest.builder().publicKeyCredentialRequestOptions(options)
                    .username(user.getEmail()).build();
            var credential=PublicKeyCredential.parseAssertionResponseJson(response);
            AssertionResult result=relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(assertionRequest).response(credential).build());
            if(!result.isSuccess()||!user.getEmail().equalsIgnoreCase(result.getUsername())||!result.isUserVerified())
                throw invalidPasskey();
            String credentialId=result.getCredential().getCredentialId().getBase64Url();
            db.update("UPDATE passkey_credentials SET signature_count=?,last_used_at=? WHERE credential_hash=? AND user_id=?",
                    result.getSignatureCount(),Timestamp.from(Instant.now()),crypto.fingerprint(credentialId),userId);
            db.update("UPDATE passkey_challenges SET used=TRUE,request_cipher='' WHERE challenge_hash=?",crypto.fingerprint(challenge));
            db.update("DELETE FROM trusted_device_locks WHERE session_id=?",sessionId);
            return accounts.view(user);
        } catch(IOException|IllegalArgumentException|AssertionFailedException failure) {
            events.record("LOGIN_FAILURE",user.getId(),user.getEmail(),request,"Trusted-device unlock failed");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Passkey unlock failed. Try again or sign out and use email and password.");
        }
    }

    @Transactional public void remove(User user,String id,HttpServletRequest request) {
        requireRegularUser(user);
        requireRecentFullAuthentication(sessionId(request));
        if(id==null||!id.matches("[0-9a-f]{64}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid passkey identifier.");
        if(db.update("DELETE FROM passkey_credentials WHERE credential_hash=? AND user_id=?",id,user.getId())==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Passkey not found.");
    }

    <T> T browserOptions(String encoded,Class<T> type) throws IOException {
        var root=json.readTree(encoded);
        return json.treeToValue(root.has("publicKey")?root.get("publicKey"):root,type);
    }

    private Ceremony saveChallenge(User user,String sessionId,String purpose,String options) {
        String challenge=crypto.randomToken();
        db.update("INSERT INTO passkey_challenges(challenge_hash,user_id,session_id,purpose,request_cipher,expires_at) VALUES(?,?,?,?,?,?)",
                crypto.fingerprint(challenge),user.getId(),sessionId,purpose,crypto.encrypt(options),Timestamp.from(Instant.now().plusSeconds(180)));
        return new Ceremony(challenge,options);
    }

    private Map<String,Object> consumeChallenge(String challenge,User user,String sessionId,String purpose) {
        String hash=crypto.fingerprint(challenge==null?"":challenge);
        var rows=db.queryForList("SELECT * FROM passkey_challenges WHERE challenge_hash=? AND user_id=? AND session_id=? AND purpose=? AND used=FALSE AND expires_at>?",
                hash,user.getId(),sessionId,purpose,Timestamp.from(Instant.now()));
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Passkey challenge is invalid or expired. Start again.");
        var row=rows.getFirst();
        String options=crypto.decrypt((String)row.get("request_cipher"));
        Integer consumed=independent.execute(status->db.update("UPDATE passkey_challenges SET used=TRUE,request_cipher='' WHERE challenge_hash=? AND user_id=? AND session_id=? AND purpose=? AND used=FALSE AND expires_at>?",
                hash,user.getId(),sessionId,purpose,Timestamp.from(Instant.now())));
        if(consumed==null||consumed==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Passkey challenge is invalid or expired. Start again.");
        Map<String,Object> result=new HashMap<>();
        result.put("options_json",options);
        return result;
    }

    private void requireRegularUser(User user) {
        user.requireActive();
        if(!user.isEmailVerified())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Verify your email before setting up trusted-device unlock.");
        if("ADMIN".equals(user.getRole())||accounts.isOwner(user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Owner administrator sign-in always requires password and authenticator verification.");
    }

    private void requireRecentFullAuthentication(String sessionId) {
        if(count("SELECT COUNT(*) FROM auth_sessions WHERE id=? AND revoked=FALSE AND expires_at>? AND reauth_at>?",
                sessionId,Timestamp.from(Instant.now()),Timestamp.from(Instant.now().minusSeconds(300)))==0)
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,"Sign in again before setting up a trusted-device passkey.");
    }

    private int count(String sql,Object... parameters) {
        Integer result=db.queryForObject(sql,Integer.class,parameters);
        if(result==null)throw new IllegalStateException("A database row-count query returned no result.");
        return result;
    }

    private String sessionId(HttpServletRequest request) {
        Object value=request.getAttribute("sessionId");
        if(!(value instanceof String id))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Please sign in again.");
        return id;
    }

    private ByteArray userHandle(User user) {
        return new ByteArray(HexFormat.of().parseHex(crypto.fingerprint("passkey-user:"+user.getId())));
    }

    private ResponseStatusException invalidPasskey() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Passkey verification failed.");
    }

    @Override public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        Long userId=users.findByEmailIgnoreCase(username).map(User::getId).orElse(null);
        if(userId==null)return Set.of();
        return new HashSet<>(db.query("SELECT credential_id FROM passkey_credentials WHERE user_id=?",
                (rs,n)->PublicKeyCredentialDescriptor.builder().id(decodeBase64(rs.getString(1))).build(),userId));
    }

    @Override public Optional<ByteArray> getUserHandleForUsername(String username) {
        return users.findByEmailIgnoreCase(username).map(this::userHandle);
    }

    @Override public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return db.query("SELECT u.email FROM users u JOIN passkey_credentials p ON p.user_id=u.id WHERE p.user_handle=? LIMIT 1",
                rs->rs.next()?Optional.of(rs.getString(1)):Optional.empty(),userHandle.getBase64Url());
    }

    @Override public Optional<RegisteredCredential> lookup(ByteArray credentialId,ByteArray userHandle) {
        String id=credentialId.getBase64Url();
        return db.query("SELECT credential_id,user_handle,public_key_cose,signature_count FROM passkey_credentials WHERE credential_hash=? AND user_handle=?",
                rs->rs.next()?Optional.of(registered(rs.getString(1),rs.getString(2),rs.getString(3),rs.getLong(4))):Optional.empty(),
                crypto.fingerprint(id),userHandle.getBase64Url());
    }

    @Override public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        String id=credentialId.getBase64Url();
        return new HashSet<>(db.query("SELECT credential_id,user_handle,public_key_cose,signature_count FROM passkey_credentials WHERE credential_hash=?",
                (rs,n)->registered(rs.getString(1),rs.getString(2),rs.getString(3),rs.getLong(4)),crypto.fingerprint(id)));
    }

    private RegisteredCredential registered(String id,String handle,String key,long count) {
        return RegisteredCredential.builder().credentialId(decodeBase64(id))
                .userHandle(decodeBase64(handle)).publicKeyCose(decodeBase64(key))
                .signatureCount(count).build();
    }

    private ByteArray decodeBase64(String value) {
        try { return ByteArray.fromBase64Url(value); }
        catch(Base64UrlException failure) { throw new IllegalStateException("Stored passkey data is invalid.",failure); }
    }
}
