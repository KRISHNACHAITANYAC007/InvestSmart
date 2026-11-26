package com.example.investsmart.security;

import com.example.investsmart.entity.User;
import com.example.investsmart.repository.UserRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SecurityAccounts {
    public static final String COOKIE="INVESTSMART_SESSION";
    public record Account(Long id,String name,String email,String role,String accountStatus,boolean emailVerified,boolean mfaEnabled,Instant createdAt){}
    public record LoginResult(String state,Account user,String challenge,String secret,String authenticatorUri,List<String> recoveryCodes){}
    private final PhoneVerification phone;private final UserRepository users;private final JdbcTemplate db;private final PasswordEncoder passwords;private final JwtService jwt;private final SecurityCrypto crypto;private final SecurityEvents events;private final AccountMail mail;private final boolean secure;
    public SecurityAccounts(PhoneVerification phone,UserRepository users,JdbcTemplate db,PasswordEncoder passwords,JwtService jwt,SecurityCrypto crypto,SecurityEvents events,AccountMail mail,@Value("${investsmart.security.secure-cookie:false}") boolean secure){this.phone=phone;this.users=users;this.db=db;this.passwords=passwords;this.jwt=jwt;this.crypto=crypto;this.events=events;this.mail=mail;this.secure=secure;}
    public User user(String email){return users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Please sign in again."));}
    public Account view(User u){return new Account(u.getId(),u.getName(),u.getEmail(),u.getRole(),u.getAccountStatus(),u.isEmailVerified(),("ADMIN".equals(u.getRole())?hasMfa(u.getId()):phone.enrolled(u.getId())),u.getCreatedAt());}
    public boolean hasMfa(Long id){return db.queryForObject("SELECT COUNT(*) FROM admin_credentials WHERE user_id=?",Integer.class,id)>0;}
    public boolean isOwner(Long id){return db.queryForObject("SELECT COUNT(*) FROM owner_admin WHERE singleton_id=1 AND user_id=?",Integer.class,id)>0;}
    public static String normalize(String email){return email==null?"":email.trim().toLowerCase(Locale.ROOT);}
    public void checkPassword(String password){if(password==null||password.length()<15||password.length()>64||password.getBytes(StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a password of 15–64 characters (at most 72 UTF-8 bytes).");}
    @Transactional public long register(String name,String email,String password,HttpServletRequest request){
        email=normalize(email);events.limit("REGISTER",email,request,3,20);checkPassword(password);events.limit("VERIFY",email,request,3,20);
        db.queryForObject("SELECT id FROM security_mutex WHERE id=1 FOR UPDATE",Integer.class);
        var existing=users.findByEmailIgnoreCase(email);
        if(existing.isPresent()){
            var current=users.findByIdForUpdate(existing.get().getId()).orElseThrow();
            if(current.isEmailVerified()||!"ACTIVE".equals(current.getAccountStatus()))return 0;
            return sendAction(current,"VERIFY",86400);
        }
        // Registration and explicit resend share the verification-mail quota.
        var u=new User();u.setName(name.trim());u.setEmail(email);u.setPassword(passwords.encode(password));u.setRole("USER");u.setEmailVerified(false);u.setAccountStatus("ACTIVE");u.setVirtualBalance(new BigDecimal("100000"));users.saveAndFlush(u);return sendAction(u,"VERIFY",86400);
    }
    private String action(Long id,String purpose,int seconds,String payload){String token=crypto.randomToken();db.update("INSERT INTO auth_actions(token_hash,user_id,purpose,expires_at,payload) VALUES(?,?,?,?,?)",crypto.fingerprint(token),id,purpose,Timestamp.from(Instant.now().plusSeconds(seconds)),payload);return token;}
    private long sendAction(User user,String purpose,int seconds){db.update("UPDATE auth_actions SET used=TRUE WHERE user_id=? AND purpose=?",user.getId(),purpose);return mail.queue(user.getEmail(),purpose,action(user.getId(),purpose,seconds,null));}
    @Transactional public long requestMail(String email,String purpose,HttpServletRequest request){email=normalize(email);User u=users.findByEmailIgnoreCase(email).orElse(null);events.limit(purpose,email,request,3,20,u==null?null:u.getId());if(u!=null&&"ACTIVE".equals(u.getAccountStatus())&&(!"VERIFY".equals(purpose)||!u.isEmailVerified())){u=users.findByIdForUpdate(u.getId()).orElseThrow();return sendAction(u,purpose,"VERIFY".equals(purpose)?86400:1800);}return 0;}
    private Map<String,Object> consume(String token,String purpose){
        String hash=crypto.fingerprint(token==null?"":token);var ids=db.queryForList("SELECT user_id FROM auth_actions WHERE token_hash=? AND purpose=? AND used=FALSE AND expires_at>?",Long.class,hash,purpose,Timestamp.from(Instant.now()));
        if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,expiredActionMessage(purpose));
        // All account security operations lock user first, then challenge, including resend/reset races.
        users.findByIdForUpdate(ids.getFirst()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"This link is invalid."));
        var rows=db.queryForList("SELECT * FROM auth_actions WHERE token_hash=? AND purpose=? AND used=FALSE AND expires_at>? FOR UPDATE",hash,purpose,Timestamp.from(Instant.now()));if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,expiredActionMessage(purpose));db.update("UPDATE auth_actions SET used=TRUE WHERE token_hash=?",hash);return rows.getFirst();
    }
    private String expiredActionMessage(String purpose){return switch(purpose){case "VERIFY"->"This verification link is invalid, expired after 24 hours, or already used. Request a fresh verification email.";case "RESET"->"This password-reset link is invalid, expired after 30 minutes, or already used. Request a new password-reset email.";case "MFA_LOGIN","MFA_SETUP"->"This authenticator challenge is invalid or expired. Sign in again to get a new challenge.";default->"This link or challenge is invalid or expired.";};}
    @Transactional public void verify(String token){var row=consume(token,"VERIFY");var u=users.findByIdForUpdate(((Number)row.get("user_id")).longValue()).orElseThrow();u.setEmailVerified(true);users.save(u);}
    @Transactional public void reset(String token,String password){checkPassword(password);var row=consume(token,"RESET");var u=users.findByIdForUpdate(((Number)row.get("user_id")).longValue()).orElseThrow();u.requireActive();u.setPassword(passwords.encode(password));u.setPasswordResetRequired(false);users.save(u);revokeAll(u.getId());}
    @Transactional public LoginResult login(String email,String password,HttpServletRequest request,HttpServletResponse response){
        email=normalize(email);events.loginAllowed(email,request);User u=users.findByEmailIgnoreCase(email).orElse(null);
        // Always run a password comparison to avoid a cheap unknown-account timing path.
        String hash=u==null?"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy":u.getPassword();
        boolean valid=password!=null&&password.length()<=100&&hash!=null&&passwords.matches(password,hash);
        if(!valid){events.record("LOGIN_FAILURE",u==null?null:u.getId(),email,request,"Invalid credentials");throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password.");}
        u=users.findByIdForUpdate(u.getId()).orElseThrow();u.requireActive();
        if(!u.isEmailVerified())return new LoginResult("VERIFY_EMAIL",null,null,null,null,null);
        if(u.isPasswordResetRequired())return new LoginResult("PASSWORD_RESET_REQUIRED",null,null,null,null,null);
        if("ADMIN".equals(u.getRole())){
            if(!isOwner(u.getId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Owner access must be provisioned by the server operator.");
            boolean enrolled=hasMfa(u.getId());String secret=enrolled?null:crypto.newTotpSecret();String challenge=action(u.getId(),enrolled?"MFA_LOGIN":"MFA_SETUP",300,secret==null?null:crypto.encrypt(secret));
            String uri=secret==null?null:"otpauth://totp/InvestSmart:"+java.net.URLEncoder.encode(u.getEmail(),StandardCharsets.UTF_8)+"?secret="+secret+"&issuer=InvestSmart&algorithm=SHA1&digits=6&period=30";
            return new LoginResult(enrolled?"MFA_REQUIRED":"MFA_SETUP",null,challenge,secret,uri,null);
        }
        var next=phone.start(u,request,response);request.setAttribute("phoneMailId",next.mailId());return new LoginResult(next.state(),null,next.challenge(),null,null,null);
    }
    @Transactional public LoginResult completeMfa(String challenge,String code,HttpServletRequest request,HttpServletResponse response){
        var rows=db.queryForList("SELECT * FROM auth_actions WHERE token_hash=? AND purpose IN ('MFA_LOGIN','MFA_SETUP') AND used=FALSE AND expires_at>?",crypto.fingerprint(challenge==null?"":challenge),Timestamp.from(Instant.now()));
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Sign in again to get a new authenticator challenge.");
        Long id=((Number)rows.getFirst().get("user_id")).longValue();var u=users.findByIdForUpdate(id).orElseThrow();u.requireActive();if(!"ADMIN".equals(u.getRole())||!isOwner(id))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Owner setup required.");events.mfaAllowed(id);
        var row=db.queryForMap("SELECT * FROM auth_actions WHERE token_hash=? FOR UPDATE",crypto.fingerprint(challenge));if(Boolean.TRUE.equals(row.get("used")) || !List.of("MFA_LOGIN","MFA_SETUP").contains(row.get("purpose")) || !((Timestamp)row.get("expires_at")).toInstant().isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This authenticator challenge is invalid or expired. Sign in again.");
        List<String> recovery=null;
        if("MFA_SETUP".equals(row.get("purpose"))){if(hasMfa(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Authenticator already enrolled. Sign in again.");String secret=crypto.decrypt((String)row.get("payload"));long step=validStep(secret,code,-1);if(step<0){mfaFailure(u,request);throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid authenticator code.");}recovery=new ArrayList<>();for(int i=0;i<10;i++)recovery.add(crypto.randomToken());db.update("INSERT INTO admin_credentials(user_id,secret_cipher,last_step,recovery_hashes) VALUES(?,?,?,?)",id,crypto.encrypt(secret),step,String.join(",",recovery.stream().map(crypto::fingerprint).toList()));}
        else verifyMfa(u,code,request);
        db.update("UPDATE auth_actions SET used=TRUE,payload=NULL WHERE token_hash=?",crypto.fingerprint(challenge));issue(u,request,response);events.record("LOGIN_SUCCESS",id,u.getEmail(),request,"Admin MFA");return new LoginResult("SIGNED_IN",view(u),null,null,null,recovery);
    }
    private long validStep(String secret,String code,long last){if(code==null||!code.matches("[0-9]{6}"))return -1;long now=Instant.now().getEpochSecond()/30;for(long step=now-1;step<=now+1;step++)if(step>last&&java.security.MessageDigest.isEqual(SecurityCrypto.totp(secret,step).getBytes(StandardCharsets.UTF_8),code.getBytes(StandardCharsets.UTF_8)))return step;return -1;}
    @Transactional public LoginResult completePhone(String challenge,String code,HttpServletRequest request,HttpServletResponse response){
        User u=phone.verifyCode(challenge,code,request);issue(u,request,response);return new LoginResult("SIGNED_IN",view(u),null,null,null,null);
    }
    @Transactional(noRollbackFor=ResponseStatusException.class) public LoginResult completePhonePasskey(String challenge,String credential,String loginChallenge,HttpServletRequest request,HttpServletResponse response){
        User u=phone.verifyLoginPasskey(challenge,credential,loginChallenge,request);issue(u,request,response);return new LoginResult("SIGNED_IN",view(u),null,null,null,null);
    }
    private void mfaFailure(User u,HttpServletRequest request){events.record("MFA_FAILURE",u.getId(),"mfa:"+u.getId(),request,"Authenticator rejected");}
    private void verifyMfa(User u,String code,HttpServletRequest request){
        events.mfaAllowed(u.getId());var rows=db.queryForList("SELECT * FROM admin_credentials WHERE user_id=? FOR UPDATE",u.getId());if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Enroll an authenticator first.");var row=rows.getFirst();
        long step=validStep(crypto.decrypt((String)row.get("secret_cipher")),code,((Number)row.get("last_step")).longValue());
        if(step>=0){db.update("UPDATE admin_credentials SET last_step=? WHERE user_id=?",step,u.getId());return;}
        var hashes=new ArrayList<>(Arrays.asList(((String)row.get("recovery_hashes")).split(",")));if(code!=null&&hashes.remove(crypto.fingerprint(code))){db.update("UPDATE admin_credentials SET recovery_hashes=? WHERE user_id=?",String.join(",",hashes),u.getId());events.record("RECOVERY_CODE_USED",u.getId(),u.getEmail(),request,"");return;}
        mfaFailure(u,request);throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid or previously used authenticator/recovery code.");
    }
    public String issue(User u,HttpServletRequest request,HttpServletResponse response){String id=crypto.randomToken();Instant now=Instant.now(),expiry=now.plusSeconds("ADMIN".equals(u.getRole())?900:3600);String device=SecurityEvents.bounded(request.getHeader("User-Agent"),160);db.update("INSERT INTO auth_sessions(id,user_id,expires_at,created_at,last_activity_at,reauth_at,source_label,device_label,factor_completed) VALUES(?,?,?,?,?,?,?,?,TRUE)",id,u.getId(),Timestamp.from(expiry),Timestamp.from(now),Timestamp.from(now),Timestamp.from(now),events.masked(request),device);String token=jwt.sessionToken(u.getEmail(),u.getRole(),id,expiry);response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from(COOKIE,token).httpOnly(true).secure(secure).sameSite("Lax").path("/").maxAge(Duration.between(now,expiry)).build().toString());return token;}
    public User authenticate(String token,HttpServletRequest request){String id=jwt.sessionId(token);String email=jwt.extractEmail(token);if(id==null)return null;var u=users.findByEmailIgnoreCase(email).orElse(null);if(u==null||!u.isEmailVerified()||!"ACTIVE".equals(u.getAccountStatus()))return null;Instant now=Instant.now();Integer n=db.queryForObject("SELECT COUNT(*) FROM auth_sessions WHERE id=? AND user_id=? AND revoked=FALSE AND factor_completed=TRUE AND expires_at>?",Integer.class,id,u.getId(),Timestamp.from(now));if(n==0||("ADMIN".equals(u.getRole())&&(!isOwner(u.getId())||!hasMfa(u.getId()))))return null;request.setAttribute("sessionId",id);request.setAttribute("userId",u.getId());db.update("UPDATE auth_sessions SET last_activity_at=? WHERE id=? AND user_id=? AND (last_activity_at IS NULL OR last_activity_at<?)",Timestamp.from(now),id,u.getId(),Timestamp.from(now.minusSeconds(60)));if(db.queryForObject("SELECT COUNT(*) FROM trusted_device_locks WHERE session_id=?",Integer.class,id)>0){request.setAttribute("sessionLocked",true);return null;}return u;}
    @Transactional public void revokeAll(Long id){users.findByIdForUpdate(id).orElseThrow();db.update("UPDATE auth_actions SET used=TRUE,payload=NULL WHERE user_id=? AND purpose IN ('MFA_LOGIN','MFA_SETUP') AND used=FALSE",id);db.update("UPDATE phone_actions SET used=TRUE,payload=NULL WHERE user_id=?",id);db.update("DELETE FROM phone_sessions WHERE user_id=?",id);db.update("UPDATE auth_actions SET used=TRUE,payload=NULL WHERE user_id=? AND purpose='PHONE_PAIR'",id);db.update("DELETE FROM trusted_device_locks WHERE session_id IN (SELECT id FROM auth_sessions WHERE user_id=?)",id);db.update("UPDATE auth_sessions SET revoked=TRUE WHERE user_id=?",id);}
    @Transactional public void revokeSession(Long userId,String sessionId){db.update("DELETE FROM trusted_device_locks WHERE session_id=? AND session_id IN (SELECT id FROM auth_sessions WHERE user_id=?)",sessionId,userId);db.update("UPDATE auth_sessions SET revoked=TRUE WHERE id=? AND user_id=?",sessionId,userId);}
    @Transactional public void revokeOtherSessions(Long userId,String currentSessionId){db.update("DELETE FROM trusted_device_locks WHERE session_id IN (SELECT id FROM auth_sessions WHERE user_id=? AND id<>?)",userId,currentSessionId);db.update("UPDATE auth_sessions SET revoked=TRUE WHERE user_id=? AND id<>?",userId,currentSessionId);}
    public void logout(HttpServletRequest request,HttpServletResponse response){Object id=request.getAttribute("sessionId");if(id!=null){db.update("UPDATE auth_sessions SET revoked=TRUE WHERE id=?",id);db.update("DELETE FROM trusted_device_locks WHERE session_id=?",id);}response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from(COOKIE,"").httpOnly(true).secure(secure).sameSite("Lax").path("/").maxAge(0).build().toString());}
    public List<Map<String,Object>> sessions(User u,String current){return db.query("SELECT id,created_at,last_activity_at,expires_at,source_label,device_label FROM auth_sessions WHERE user_id=? AND revoked=FALSE AND expires_at>? ORDER BY created_at DESC",(rs,n)->{Map<String,Object> row=new LinkedHashMap<>();row.put("id",rs.getString("id"));row.put("current",rs.getString("id").equals(current));row.put("createdAt",rs.getTimestamp("created_at").toInstant());var activity=rs.getTimestamp("last_activity_at");row.put("lastActivityAt",activity==null?rs.getTimestamp("created_at").toInstant():activity.toInstant());row.put("expiresAt",rs.getTimestamp("expires_at").toInstant());row.put("source",rs.getString("source_label"));row.put("device",rs.getString("device_label"));return row;},u.getId(),Timestamp.from(Instant.now()));}
    @Transactional public void changePassword(String email,String current,String replacement,HttpServletRequest request){var u=users.findByEmailForUpdate(email).orElseThrow();u.requireActive();events.loginAllowed(email,request);if(!passwords.matches(current,u.getPassword())){events.record("LOGIN_FAILURE",u.getId(),email,request,"Password confirmation failed");throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Current password is incorrect.");}checkPassword(replacement);u.setPassword(passwords.encode(replacement));users.save(u);revokeAll(u.getId());events.record("PASSWORD_CHANGED",u.getId(),email,request,"");}
    @Transactional public void reauthenticate(String email,String password,String code,HttpServletRequest request){var u=users.findByEmailForUpdate(email).orElseThrow();u.requireActive();events.loginAllowed(email,request);if(!passwords.matches(password,u.getPassword())){events.record("LOGIN_FAILURE",u.getId(),email,request,"Admin reauthentication failed");throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid password.");}if(!"ADMIN".equals(u.getRole()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Administrator required.");verifyMfa(u,code,request);db.update("UPDATE auth_sessions SET reauth_at=? WHERE id=? AND revoked=FALSE",Timestamp.from(Instant.now()),request.getAttribute("sessionId"));}
    public void requireRecent(HttpServletRequest request){Integer n=db.queryForObject("SELECT COUNT(*) FROM auth_sessions WHERE id=? AND revoked=FALSE AND reauth_at>?",Integer.class,request.getAttribute("sessionId"),Timestamp.from(Instant.now().minusSeconds(300)));if(n==0)throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,"Confirm your password and authenticator code before this action.");}
    public void requireOwner(Long id,String email,HttpServletRequest request){User u=user(email);if(!u.getId().equals(id)){events.record("ACCESS_DENIED",u.getId(),email,request,"Cross-account access denied");throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Access denied.");}}
}
