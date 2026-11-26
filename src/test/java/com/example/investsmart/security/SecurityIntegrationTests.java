package com.example.investsmart.security;

import com.example.investsmart.entity.User;
import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.service.WalletService;
import com.example.investsmart.dto.WalletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest @ActiveProfiles("test") @AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class SecurityIntegrationTests {
    @Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired SecurityAccounts accounts;@Autowired SecurityEvents events;@Autowired SecurityCrypto crypto;@Autowired JdbcTemplate db;@Autowired PasswordEncoder passwords;@Autowired ObjectMapper json;@Autowired WalletService wallet;@Autowired AdminService admin;@Autowired AdminListingOverrides listingOverrides;@Autowired AdminHealthStore healthStore;@Autowired PlatformTransactionManager tx;
    static final String PASSWORD="correct horse battery staple",SECRET="GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    @Autowired AdminBootstrap bootstrap;
    @Autowired PasskeyService passkeyService;
    List<Long> created=new ArrayList<>();
    @BeforeEach void clear(){for(String table:List.of("phone_sessions","phone_actions","phone_credentials","equity_pending_orders","option_orders","option_positions","passkey_challenges","passkey_credentials","trusted_device_locks","mail_delivery_failures","security_events","security_alerts","admin_balance_adjustments","admin_listing_overrides","admin_runtime_health","admin_audit","auth_actions","auth_sessions","admin_credentials","owner_admin","mail_outbox"))db.update("DELETE FROM "+table);}
    @AfterEach void cleanup(){for(Long id:created)users.deleteById(id);}
    User user(boolean verified,String role){var u=new User();u.setName("Test account");u.setEmail(UUID.randomUUID()+"@example.invalid");u.setPassword(passwords.encode(PASSWORD));u.setRole(role);u.setEmailVerified(verified);u.setVirtualBalance(new BigDecimal("100"));u=users.saveAndFlush(u);created.add(u.getId());if("ADMIN".equals(role)){db.update("INSERT INTO owner_admin(singleton_id,user_id) VALUES(1,?)",u.getId());db.update("INSERT INTO admin_credentials(user_id,secret_cipher,last_step,recovery_hashes) VALUES(?,?,-1,?)",u.getId(),crypto.encrypt(SECRET),crypto.fingerprint("test-recovery-code"));}return u;}
    Cookie cookie(User user){return new Cookie(SecurityAccounts.COOKIE,accounts.issue(user,new MockHttpServletRequest(),new MockHttpServletResponse()));}
    String body(Object value)throws Exception{return json.writeValueAsString(value);}
    MockHttpServletRequest request(){var r=new MockHttpServletRequest();r.setRemoteAddr("198.51.100.8");r.setRequestURI("/auth/login");return r;}
    void action(User u,String token,String purpose){db.update("INSERT INTO auth_actions(token_hash,user_id,purpose,expires_at) VALUES(?,?,?,?)",crypto.fingerprint(token),u.getId(),purpose,Timestamp.from(Instant.now().plusSeconds(600)));}
    @Test void adminOverviewAndUnfilteredUsersPageUseSameRegisteredUserCount()throws Exception{User owner=user(true,"ADMIN");user(true,"USER");user(true,"USER");User legacy=user(true,"USER");legacy.setEmail(null);legacy.setName(null);users.saveAndFlush(legacy);Cookie c=cookie(owner);mvc.perform(get("/admin/overview").cookie(c)).andExpect(status().isOk()).andExpect(jsonPath("$.users").value(4));mvc.perform(get("/admin/users").cookie(c).param("query","").param("status","ALL")).andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(4));}
    @Test void adminCanReviewAndDeleteUnidentifiableAccountWithoutSendingMail()throws Exception{
        User owner=user(true,"ADMIN"),legacy=user(true,"USER");legacy.setName(null);legacy.setEmail(null);legacy.setVirtualBalance(null);users.saveAndFlush(legacy);Cookie ownerCookie=cookie(owner);
        var details=mvc.perform(get("/admin/users/"+legacy.getId()).cookie(ownerCookie)).andReturn();
        assertEquals(200,details.getResponse().getStatus(),details.getResponse().getContentAsString());
        assertTrue(details.getResponse().getContentAsString().contains("\"applications\":[]"));
        assertTrue(json.readTree(details.getResponse().getContentAsString()).path("portfolio").path("totalValue").isNull());
        db.update("UPDATE auth_sessions SET reauth_at=? WHERE user_id=?",Timestamp.from(Instant.now()),owner.getId());
        var deleted=mvc.perform(delete("/admin/users/"+legacy.getId()).cookie(ownerCookie).with(csrf()).contentType("application/json").content(body(Map.of("confirmation","DELETE "+legacy.getId())))).andReturn();
        assertEquals(200,deleted.getResponse().getStatus(),deleted.getResponse().getContentAsString());
        assertEquals(1,json.readTree(deleted.getResponse().getContentAsString()).path("users").asInt());
        assertTrue(users.findById(legacy.getId()).isEmpty());assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM mail_outbox",Integer.class));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM admin_audit WHERE action_name='DELETE_USER' AND target_id=?",Integer.class,legacy.getId()));
    }

    @Test void loginIssuesHttpOnlyCookieAndLogoutRevokesIt()throws Exception{
        User u=user(true,"USER");PhoneTestCredentials.enroll(db,crypto,u.getId());var first=mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",u.getEmail().toUpperCase(Locale.ROOT),"password",PASSWORD)))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PHONE_REQUIRED")).andReturn();
        String challenge=json.readTree(first.getResponse().getContentAsString()).path("challenge").asText();
        var result=mvc.perform(post("/auth/phone/verify").cookie(first.getResponse().getCookie(PhoneVerification.LOGIN_COOKIE)).with(csrf()).contentType("application/json").content(body(Map.of("challenge",challenge,"code",PhoneTestCredentials.code())))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("SIGNED_IN")).andExpect(jsonPath("$.user.password").doesNotExist()).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("HttpOnly"))).andReturn();
        Cookie session=result.getResponse().getCookie(SecurityAccounts.COOKIE);assertNotNull(session);mvc.perform(get("/users/me").cookie(session)).andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(post("/auth/logout").cookie(session).with(csrf())).andExpect(status().isOk());mvc.perform(get("/users/me").cookie(session)).andExpect(status().isUnauthorized());
    }
    @Test void legacyAdminRoleAloneDoesNotGrantOwnerAccess()throws Exception{User u=user(true,"USER");u.setRole("ADMIN");users.saveAndFlush(u);mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",u.getEmail(),"password",PASSWORD)))).andExpect(status().isForbidden());mvc.perform(get("/admin/users").cookie(cookie(u))).andExpect(status().isUnauthorized());}
    @Test void ownerBootstrapRequiresVerifiedEmailNonWebCommandAndOnlySelectsOneOwner(){User u=user(false,"USER");var args=new org.springframework.boot.DefaultApplicationArguments("--bootstrap-admin-email="+u.getEmail(),"--spring.main.web-application-type=none");assertThrows(IllegalStateException.class,()->bootstrap.run(args));u.setEmailVerified(true);users.saveAndFlush(u);assertThrows(IllegalStateException.class,()->bootstrap.run(new org.springframework.boot.DefaultApplicationArguments("--bootstrap-admin-email="+u.getEmail())));bootstrap.run(args);assertTrue(accounts.isOwner(u.getId()));assertEquals("ADMIN",users.findById(u.getId()).orElseThrow().getRole());bootstrap.run(args);assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM admin_audit WHERE action_name='OWNER_BOOTSTRAP'",Integer.class));User other=user(true,"USER");assertThrows(IllegalStateException.class,()->bootstrap.run(new org.springframework.boot.DefaultApplicationArguments("--bootstrap-admin-email="+other.getEmail(),"--spring.main.web-application-type=none")));}
    @Test void csrfIsRequiredAndLegacyBearerDoesNotGrantAccess()throws Exception{User u=user(true,"USER");Cookie c=cookie(u);mvc.perform(post("/wallet/deposit").cookie(c).contentType("application/json").content("{\"amount\":1}")).andExpect(status().isForbidden());mvc.perform(get("/users/me").header("Authorization","Bearer "+c.getValue())).andExpect(status().isUnauthorized());mvc.perform(get("/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());}
    @Test void ownershipAndAdminChecksProtectEveryLegacyAccountRoute()throws Exception{User a=user(true,"USER"),b=user(true,"USER");Cookie c=cookie(a);for(String path:List.of("/portfolio/","/wallet/","/orders/user/"))mvc.perform(get(path+b.getId()).cookie(c)).andExpect(status().isForbidden());mvc.perform(get("/users").cookie(c)).andExpect(status().isForbidden());mvc.perform(put("/users/"+a.getId()).cookie(c).with(csrf()).contentType("application/json").content("{\"role\":\"ADMIN\",\"virtualBalance\":9999}")).andExpect(status().isForbidden());mvc.perform(get("/admin/users").cookie(c)).andExpect(status().isForbidden());mvc.perform(post("/stocks").cookie(c).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());assertEquals("USER",users.findById(a.getId()).orElseThrow().getRole());assertEquals(0,users.findById(a.getId()).orElseThrow().getVirtualBalance().compareTo(new BigDecimal("100")));}
    @Test void verificationAndResetAreSingleUseAndResetRevokesSessions()throws Exception{User u=user(false,"USER");mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",u.getEmail(),"password",PASSWORD)))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("VERIFY_EMAIL")).andExpect(header().doesNotExist("Set-Cookie"));String token=crypto.randomToken();action(u,token,"VERIFY");mvc.perform(post("/auth/verify-email").with(csrf()).contentType("application/json").content(body(Map.of("token",token)))).andExpect(status().isOk());mvc.perform(post("/auth/verify-email").with(csrf()).contentType("application/json").content(body(Map.of("token",token)))).andExpect(status().isBadRequest());u=users.findById(u.getId()).orElseThrow();Cookie c=cookie(u);String reset=crypto.randomToken();action(u,reset,"RESET");mvc.perform(post("/auth/reset-password").with(csrf()).contentType("application/json").content(body(Map.of("token",reset,"password","a different secure passphrase")))).andExpect(status().isOk());mvc.perform(get("/users/me").cookie(c)).andExpect(status().isUnauthorized());mvc.perform(post("/auth/reset-password").with(csrf()).contentType("application/json").content(body(Map.of("token",reset,"password",PASSWORD)))).andExpect(status().isBadRequest());}
    @Test void expiredVerificationAndRecoveryLinksExplainTheirExpiryWindows()throws Exception{User u=user(true,"USER");String verify=crypto.randomToken(),reset=crypto.randomToken();action(u,verify,"VERIFY");action(u,reset,"RESET");db.update("UPDATE auth_actions SET expires_at=? WHERE token_hash IN (?,?)",Timestamp.from(Instant.now().minusSeconds(1)),crypto.fingerprint(verify),crypto.fingerprint(reset));var verifyResult=mvc.perform(post("/auth/verify-email").with(csrf()).contentType("application/json").content(body(Map.of("token",verify)))).andExpect(status().isBadRequest()).andReturn();var resetResult=mvc.perform(post("/auth/reset-password").with(csrf()).contentType("application/json").content(body(Map.of("token",reset,"password","another secure passphrase")))).andExpect(status().isBadRequest()).andReturn();assertTrue(verifyResult.getResponse().getContentAsString().contains("expired after 24 hours"));assertTrue(resetResult.getResponse().getContentAsString().contains("expired after 30 minutes"));}
    @Test void passkeyEnrollmentIsPlatformBoundAndCannotReplaceAdminMfa()throws Exception{User u=user(true,"USER"),owner=user(true,"ADMIN");Cookie regular=cookie(u),adminCookie=cookie(owner);var registration=mvc.perform(post("/auth/passkeys/registration/options").cookie(regular).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.publicKey").isString()).andExpect(jsonPath("$.publicKey").value(org.hamcrest.Matchers.containsString("\"authenticatorAttachment\":\"platform\""))).andExpect(jsonPath("$.publicKey").value(org.hamcrest.Matchers.containsString("\"userVerification\":\"required\""))).andReturn();String publicKey=json.readTree(registration.getResponse().getContentAsString()).path("publicKey").asText();assertNotNull(passkeyService.browserOptions(publicKey,com.yubico.webauthn.data.PublicKeyCredentialCreationOptions.class).getChallenge());assertTrue(json.readTree(publicKey).path("publicKey").path("challenge").isTextual(),"WebAuthn creation options must include a challenge inside the publicKey object");mvc.perform(post("/auth/passkeys/registration/options").cookie(adminCookie).with(csrf())).andExpect(status().isForbidden());}
    @Test void lockRequiresAOneTimePasskeyAssertionOrFullSignOut()throws Exception{User u=user(true,"USER");Cookie c=cookie(u);String id=db.queryForObject("SELECT id FROM auth_sessions WHERE user_id=?",String.class,u.getId());db.update("INSERT INTO passkey_credentials(credential_hash,credential_id,user_id,user_handle,public_key_cose,signature_count,created_at) VALUES(?,?,?,?,?,?,?)",crypto.fingerprint("credential"),"credential",u.getId(),"handle","a2V5",0,Timestamp.from(Instant.now()));mvc.perform(post("/auth/lock").cookie(c).with(csrf())).andExpect(status().isOk());mvc.perform(get("/users/me").cookie(c)).andExpect(status().isLocked());mvc.perform(get("/auth/lock-status").cookie(c)).andExpect(status().isOk()).andExpect(jsonPath("$.locked").value(true)).andExpect(jsonPath("$.canUnlock").value(true));var options=mvc.perform(post("/auth/unlock/options").cookie(c).with(csrf())).andExpect(status().isOk()).andReturn();String assertionOptions=json.readTree(options.getResponse().getContentAsString()).path("publicKey").asText();assertNotNull(passkeyService.browserOptions(assertionOptions,com.yubico.webauthn.data.PublicKeyCredentialRequestOptions.class).getChallenge());String challenge=json.readTree(options.getResponse().getContentAsString()).path("challenge").asText();mvc.perform(post("/auth/unlock").cookie(c).with(csrf()).contentType("application/json").content(body(Map.of("challenge",challenge,"credential","{")))).andExpect(status().isUnauthorized());mvc.perform(post("/auth/unlock").cookie(c).with(csrf()).contentType("application/json").content(body(Map.of("challenge",challenge,"credential","{}")))).andExpect(status().isBadRequest());mvc.perform(post("/auth/logout").cookie(c).with(csrf())).andExpect(status().isOk());mvc.perform(get("/users/me").cookie(c)).andExpect(status().isUnauthorized());assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM auth_sessions WHERE id=? AND revoked=TRUE",Integer.class,id));assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM trusted_device_locks WHERE session_id=?",Integer.class,id));}
    @Test void signupForcesUserRoleAndQueuesEncryptedVerificationWithoutLeakingToken()throws Exception{String email=UUID.randomUUID()+"@example.invalid";var result=mvc.perform(post("/auth/register").with(csrf()).contentType("application/json").content(body(Map.of("name","New user","email",email,"password",PASSWORD,"role","ADMIN","virtualBalance",99999)))).andExpect(status().isOk()).andExpect(jsonPath("$.token").doesNotExist()).andReturn();User u=users.findByEmail(email).orElseThrow();created.add(u.getId());assertEquals("USER",u.getRole());assertFalse(u.isEmailVerified());assertEquals(0,u.getVirtualBalance().compareTo(new BigDecimal("100000")));String encrypted=db.queryForObject("SELECT body_cipher FROM mail_outbox WHERE recipient=?",String.class,email);assertFalse(encrypted.contains("verify-email"));assertTrue(crypto.decrypt(encrypted).contains("/verify-email?token="));assertFalse(result.getResponse().getContentAsString().contains(email));}
    @Test void failedEmailDeliveryIsActionableAndNeverVerifiesOrLeaksSecrets()throws Exception{String email=UUID.randomUUID()+"@example.invalid";mvc.perform(post("/auth/register").with(csrf()).contentType("application/json").content(body(Map.of("name","New user","email",email,"password",PASSWORD)))).andExpect(status().isOk());User u=users.findByEmail(email).orElseThrow();created.add(u.getId());assertFalse(u.isEmailVerified());assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM mail_delivery_failures f JOIN mail_outbox m ON m.id=f.mail_id WHERE m.recipient=?",Integer.class,email));var status=mvc.perform(get("/auth/mail-status")).andExpect(status().isOk()).andExpect(jsonPath("$.deliveryIssue").value(true)).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SMTP_HOST"))).andReturn().getResponse().getContentAsString();assertFalse(status.contains(email));assertFalse(status.contains(PASSWORD));}
    private AccountMail testMailer(org.springframework.mail.javamail.JavaMailSender sender){
        var factory=new org.springframework.beans.factory.support.StaticListableBeanFactory();
        factory.addBean("sender",sender);
        return new AccountMail(db,crypto,factory.getBeanProvider(org.springframework.mail.javamail.JavaMailSender.class),null,"https://example.invalid","sender@example.invalid","smtp.example.invalid",587,"sender","test-only-password");
    }
    @Test void obsoleteActionMailIsDiscardedBeforeSmtpIncludingLegacyMessages(){
        var sender=org.mockito.Mockito.mock(org.springframework.mail.javamail.JavaMailSender.class);
        var mail=testMailer(sender);
        User u=user(true,"USER");
        for(String state:List.of("expired","used","missing","suspended","wrong-recipient","legacy")){
            String token=crypto.randomToken();action(u,token,"RESET");
            long id=mail.queue(u.getEmail(),"RESET",token);
            if(state.equals("expired"))db.update("UPDATE auth_actions SET expires_at=? WHERE token_hash=?",Timestamp.from(Instant.now().minusSeconds(1)),crypto.fingerprint(token));
            if(state.equals("used"))db.update("UPDATE auth_actions SET used=TRUE WHERE token_hash=?",crypto.fingerprint(token));
            if(state.equals("missing"))db.update("DELETE FROM auth_actions WHERE token_hash=?",crypto.fingerprint(token));
            if(state.equals("suspended"))db.update("UPDATE users SET account_status='SUSPENDED' WHERE id=?",u.getId());
            if(state.equals("wrong-recipient"))db.update("UPDATE mail_outbox SET recipient='someone-else@example.invalid' WHERE id=?",id);
            if(state.equals("legacy"))db.update("DELETE FROM mail_action_links WHERE mail_id=?",id);
            db.update("INSERT INTO mail_delivery_failures(mail_id,error_code,recorded_at) VALUES(?,?,?)",id,"SMTP_NOT_CONFIGURED",Timestamp.from(Instant.now()));
            mail.deliver(id);
            assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM mail_outbox WHERE id=?",Integer.class,id),state);
            assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM mail_action_links WHERE mail_id=?",Integer.class,id));
            assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM mail_delivery_failures WHERE mail_id=?",Integer.class,id));
            db.update("UPDATE users SET account_status='ACTIVE' WHERE id=?",u.getId());
        }
        org.mockito.Mockito.verifyNoInteractions(sender);
    }
    @Test void validActionMailAndDeletionNoticesStillSendAndClearEncryptedBodies(){
        var sender=org.mockito.Mockito.mock(org.springframework.mail.javamail.JavaMailSender.class);
        var mail=testMailer(sender);
        User u=user(false,"USER");
        for(String purpose:List.of("VERIFY","RESET")){
            String token=crypto.randomToken();action(u,token,purpose);
            long id=mail.queue(u.getEmail(),purpose,token);mail.deliver(id);mail.deliver(id);
            assertTrue(db.queryForObject("SELECT sent FROM mail_outbox WHERE id=?",Boolean.class,id));
            assertEquals("",db.queryForObject("SELECT body_cipher FROM mail_outbox WHERE id=?",String.class,id));
        }
        long notice=mail.queueAccountDeletion(u.getEmail(),u.getName(),"");mail.deliver(notice);
        assertTrue(db.queryForObject("SELECT sent FROM mail_outbox WHERE id=?",Boolean.class,notice));
        org.mockito.Mockito.verify(sender,org.mockito.Mockito.times(3)).send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
    }
    @Test void passkeyThrottleReportsRemainingCooldownAndDoesNotExtendItOnRejection()throws Exception{
        User u=user(true,"USER");Cookie session=cookie(u);
        for(int i=0;i<5;i++)events.record("REQUEST_PASSKEY_REGISTER",u.getId(),u.getEmail(),request(),"");
        db.update("UPDATE security_events SET occurred_at=? WHERE user_id=? AND event_type='REQUEST_PASSKEY_REGISTER'",Timestamp.from(Instant.now().minusSeconds(850)),u.getId());
        for(int i=0;i<2;i++){
            var result=mvc.perform(post("/auth/passkeys/registration/options").cookie(session).with(csrf()).header("Origin","http://localhost:5173")).andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After")).andExpect(header().string("Access-Control-Expose-Headers","Retry-After")).andReturn();
            int wait=Integer.parseInt(result.getResponse().getHeader("Retry-After"));assertTrue(wait>0&&wait<=50);
            assertTrue(result.getResponse().getContentAsString().contains("passkey setup"));
        }
        assertEquals(5,db.queryForObject("SELECT COUNT(*) FROM security_events WHERE user_id=? AND event_type='REQUEST_PASSKEY_REGISTER'",Integer.class,u.getId()));
        db.update("UPDATE security_events SET occurred_at=? WHERE user_id=? AND event_type='REQUEST_PASSKEY_REGISTER'",Timestamp.from(Instant.now().minusSeconds(901)),u.getId());
        mvc.perform(post("/auth/passkeys/registration/options").cookie(session).with(csrf())).andExpect(status().isOk());
    }

    @Test void passwordResetAllowsImmediateFreshLoginWithExactNewPassword()throws Exception{
        User u=user(true,"USER");Cookie oldSession=cookie(u);String token=crypto.randomToken();action(u,token,"RESET");
        String replacement="  New passphrase #! 2026  ";
        mvc.perform(post("/auth/reset-password").with(csrf()).contentType("application/json").content(body(Map.of("token",token,"password",replacement)))).andExpect(status().isOk());
        assertTrue(passwords.matches(replacement,users.findById(u.getId()).orElseThrow().getPassword()));
        mvc.perform(get("/users/me").cookie(oldSession)).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",u.getEmail(),"password",PASSWORD)))).andExpect(status().isUnauthorized());
        var signedIn=mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",u.getEmail().toUpperCase(Locale.ROOT),"password",replacement)))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PHONE_SETUP")).andReturn();
        assertNull(signedIn.getResponse().getCookie(SecurityAccounts.COOKIE));
    }
    @Test void accountPasswordChangeAllowsFreshLoginAndRejectsOldPassword()throws Exception{
        User u=user(true,"USER");Cookie session=cookie(u);String replacement="a newly changed password #2026";
        mvc.perform(post("/users/me/password").cookie(session).with(csrf()).contentType("application/json").content(body(Map.of("currentPassword",PASSWORD,"newPassword",replacement)))).andExpect(status().isOk());
        mvc.perform(get("/users/me").cookie(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",u.getEmail(),"password",PASSWORD)))).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",u.getEmail(),"password",replacement)))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PHONE_SETUP"));
    }

    @Test void passwordResetInvalidatesPendingAdminEnrollmentAndLogin(){
        for(boolean enrolled: new boolean[]{false,true}){
            User u=user(true,enrolled?"ADMIN":"USER");
            if(!enrolled){u.setRole("ADMIN");users.saveAndFlush(u);db.update("INSERT INTO owner_admin(singleton_id,user_id) VALUES(1,?)",u.getId());}
            var login=accounts.login(u.getEmail(),PASSWORD,request(),new MockHttpServletResponse());
            String reset=crypto.randomToken();action(u,reset,"RESET");
            accounts.reset(reset,"a replacement secure passphrase");
            String code=SecurityCrypto.totp(enrolled?SECRET:login.secret(),Instant.now().getEpochSecond()/30);
            var response=new MockHttpServletResponse();
            assertThrows(org.springframework.web.server.ResponseStatusException.class,()->accounts.completeMfa(login.challenge(),code,request(),response));
            assertNull(response.getCookie(SecurityAccounts.COOKIE));
            assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM auth_actions WHERE user_id=? AND purpose IN ('MFA_LOGIN','MFA_SETUP') AND (used=FALSE OR payload IS NOT NULL)",Integer.class,u.getId()));
            db.update("DELETE FROM owner_admin");
        }
    }
    @Test void adminRequiresMfaAndRejectsCodeReplay()throws Exception{User u=user(true,"ADMIN");var login=mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",u.getEmail(),"password",PASSWORD)))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("MFA_REQUIRED")).andExpect(header().doesNotExist("Set-Cookie")).andReturn();String challenge=json.readTree(login.getResponse().getContentAsString()).path("challenge").asText();String code=SecurityCrypto.totp(SECRET,Instant.now().getEpochSecond()/30);var result=mvc.perform(post("/auth/mfa").with(csrf()).contentType("application/json").content(body(Map.of("challenge",challenge,"code",code)))).andExpect(status().isOk()).andReturn();Cookie c=result.getResponse().getCookie(SecurityAccounts.COOKIE);mvc.perform(get("/admin/users").cookie(c)).andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray());mvc.perform(post("/auth/mfa").with(csrf()).contentType("application/json").content(body(Map.of("challenge",challenge,"code",code)))).andExpect(status().isBadRequest());mvc.perform(post("/users/me/reauthenticate").cookie(c).with(csrf()).contentType("application/json").content(body(Map.of("password",PASSWORD,"code",code)))).andExpect(status().isUnauthorized());}
    @Test void adminEnrollmentOnlyGrantsAccessAfterCodeAndReturnsRecoveryOnce()throws Exception{User u=user(true,"USER");u.setRole("ADMIN");users.saveAndFlush(u);db.update("INSERT INTO owner_admin(singleton_id,user_id) VALUES(1,?)",u.getId());var response=new MockHttpServletResponse();var login=accounts.login(u.getEmail(),PASSWORD,request(),response);assertEquals("MFA_SETUP",login.state());assertNull(response.getCookie(SecurityAccounts.COOKIE));String code=SecurityCrypto.totp(login.secret(),Instant.now().getEpochSecond()/30);var result=accounts.completeMfa(login.challenge(),code,request(),response);assertEquals(10,result.recoveryCodes().size());assertTrue(accounts.hasMfa(u.getId()));assertNotNull(response.getCookie(SecurityAccounts.COOKIE));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->accounts.completeMfa(login.challenge(),code,request(),new MockHttpServletResponse()));}
    @Test void recoveryCodesAreSingleUseAndMfaFailuresPersistAcrossRollbacks(){User u=user(true,"ADMIN");var login=accounts.login(u.getEmail(),PASSWORD,request(),new MockHttpServletResponse());assertEquals("SIGNED_IN",accounts.completeMfa(login.challenge(),"test-recovery-code",request(),new MockHttpServletResponse()).state());var next=accounts.login(u.getEmail(),PASSWORD,request(),new MockHttpServletResponse());assertThrows(org.springframework.web.server.ResponseStatusException.class,()->accounts.completeMfa(next.challenge(),"test-recovery-code",request(),new MockHttpServletResponse()));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM security_events WHERE event_type='MFA_FAILURE'",Integer.class));for(int i=0;i<4;i++)assertThrows(org.springframework.web.server.ResponseStatusException.class,()->accounts.completeMfa(next.challenge(),"invalid",request(),new MockHttpServletResponse()));var blocked=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->accounts.completeMfa(next.challenge(),SecurityCrypto.totp(SECRET,Instant.now().getEpochSecond()/30),request(),new MockHttpServletResponse()));assertEquals(429,blocked.getStatusCode().value());assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM security_alerts WHERE severity='HIGH'",Integer.class));}
    @Test void concurrentPasswordResetsConsumeTheLinkOnce()throws Exception{User u=user(true,"USER");String token=crypto.randomToken();action(u,token,"RESET");var executor=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);Callable<Boolean> reset=()->{start.await();try{accounts.reset(token,"another long secure passphrase");return true;}catch(org.springframework.web.server.ResponseStatusException invalid){return false;}};try{var first=executor.submit(reset);var second=executor.submit(reset);start.countDown();assertEquals(1,(first.get(15,TimeUnit.SECONDS)?1:0)+(second.get(15,TimeUnit.SECONDS)?1:0));}finally{executor.shutdownNow();}}
    @Test void repeatedFailuresCreateOnePersistentAlertAndOneMinuteCooldown()throws Exception{User u=user(true,"USER");for(int i=0;i<5;i++)mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",u.getEmail(),"password","wrong")))).andExpect(status().isUnauthorized());var blocked=mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",u.getEmail(),"password",PASSWORD)))).andExpect(status().isTooManyRequests()).andReturn();assertTrue(blocked.getResponse().getContentAsString().contains("Try again in 1 minute"));assertFalse(blocked.getResponse().getContentAsString().contains("15 minutes"));assertEquals(5,db.queryForObject("SELECT COUNT(*) FROM security_events WHERE event_type='LOGIN_FAILURE'",Integer.class));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM security_alerts",Integer.class));events.record("LOGIN_FAILURE",u.getId(),u.getEmail(),request(),"Invalid credentials");assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM security_alerts",Integer.class));assertEquals(6,db.queryForObject("SELECT occurrences FROM security_alerts",Integer.class));}
    @Test void adminPermanentlyDeletesOnlyConfirmedNonAdminUsersAfterFreshMfa()throws Exception{
        User owner=user(true,"ADMIN"),target=user(true,"USER");Cookie ownerCookie=cookie(owner),targetCookie=cookie(target);
        String targetSession=db.queryForObject("SELECT id FROM auth_sessions WHERE user_id=?",String.class,target.getId());
        db.update("INSERT INTO trusted_device_locks(session_id,locked_at) VALUES(?,?)",targetSession,Timestamp.from(Instant.now()));
        action(target,crypto.randomToken(),"RESET");
        db.update("INSERT INTO admin_balance_adjustments(occurred_at,actor_id,user_id,amount,balance_after,reason) VALUES(?,?,?,?,?,?)",Timestamp.from(Instant.now()),owner.getId(),target.getId(),new BigDecimal("10.00"),new BigDecimal("110.00"),"Correction before deletion");
        db.update("INSERT INTO saved_interests(user_id,kind,reference_id) VALUES(?,?,?)",target.getId(),"STOCK","DELETE-TEST");
        db.update("INSERT INTO ipo_applications(user_id,ipo_id,status) VALUES(?,?,?)",target.getId(),"DELETE-TEST","APPLIED");
        Timestamp alertTime=Timestamp.from(Instant.now());
        db.update("INSERT INTO security_alerts(rule_name,subject_key,user_id,severity,status,occurrences,first_at,last_at,seen) VALUES(?,?,?,?,?,?,?,?,?)","Repeated login failures",crypto.fingerprint(target.getEmail()),target.getId(),"MEDIUM","OPEN",5,alertTime,alertTime,false);
        db.update("INSERT INTO mail_outbox(recipient,subject_line,body_cipher,created_at) VALUES(?,?,?,?)",target.getEmail(),"Verify your InvestSmart email","encrypted body",Timestamp.from(Instant.now()));
        Long mailId=db.queryForObject("SELECT id FROM mail_outbox WHERE recipient=?",Long.class,target.getEmail());
        db.update("INSERT INTO mail_delivery_failures(mail_id,error_code,recorded_at) VALUES(?,?,?)",mailId,"SMTP_DELIVERY_FAILED",Timestamp.from(Instant.now()));
        events.record("LOGIN_FAILURE",target.getId(),target.getEmail(),request(),"Invalid credentials");
        mvc.perform(delete("/admin/users/"+target.getId()).cookie(ownerCookie).with(csrf()).contentType("application/json").content(body(Map.of("confirmation","not-the-user@example.invalid","reason","User requested permanent account deletion")))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("exact email")));
        assertTrue(users.findById(target.getId()).isPresent());
        db.update("UPDATE auth_sessions SET reauth_at=? WHERE id=?",Timestamp.from(Instant.now().minusSeconds(301)),db.queryForObject("SELECT id FROM auth_sessions WHERE user_id=?",String.class,owner.getId()));
        mvc.perform(delete("/admin/users/"+target.getId()).cookie(ownerCookie).with(csrf()).contentType("application/json").content(body(Map.of("confirmation",target.getEmail(),"reason","User requested permanent account deletion")))).andExpect(status().isPreconditionRequired());
        mvc.perform(post("/users/me/reauthenticate").cookie(ownerCookie).with(csrf()).contentType("application/json").content(body(Map.of("password",PASSWORD,"code",SecurityCrypto.totp(SECRET,Instant.now().getEpochSecond()/30))))).andExpect(status().isOk());
        mvc.perform(delete("/admin/users/"+target.getId()).cookie(ownerCookie).with(csrf()).contentType("application/json").content(body(Map.of("confirmation",target.getEmail(),"reason","User requested permanent account deletion")))).andExpect(status().isOk()).andExpect(jsonPath("$.users").value(1)).andExpect(jsonPath("$.openAlerts").value(0)).andExpect(jsonPath("$.unreadAlerts").value(0));
        assertTrue(users.findById(target.getId()).isEmpty());
        assertNull(db.queryForObject("SELECT user_id FROM admin_balance_adjustments WHERE actor_id=? AND reason=?",Long.class,owner.getId(),"Correction before deletion"));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM security_alerts WHERE user_id IS NULL AND status='RESOLVED' AND seen=TRUE",Integer.class));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM auth_sessions WHERE user_id=?",Integer.class,target.getId()));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM trusted_device_locks WHERE session_id=?",Integer.class,targetSession));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM auth_actions WHERE user_id=?",Integer.class,target.getId()));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM saved_interests WHERE user_id=?",Integer.class,target.getId()));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM ipo_applications WHERE user_id=?",Integer.class,target.getId()));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM mail_outbox WHERE recipient=? AND subject_line=?",Integer.class,target.getEmail(),"Your InvestSmart account has been deleted"));
        String deletionNotice=db.queryForObject("SELECT body_cipher FROM mail_outbox WHERE recipient=? AND subject_line=?",String.class,target.getEmail(),"Your InvestSmart account has been deleted");
        assertTrue(crypto.decrypt(deletionNotice).contains("Reason provided: User requested permanent account deletion"));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM mail_delivery_failures WHERE mail_id=?",Integer.class,mailId));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM security_events WHERE event_type='LOGIN_FAILURE' AND user_id IS NULL",Integer.class));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM admin_audit WHERE action_name='DELETE_USER' AND target_id=?",Integer.class,target.getId()));
        mvc.perform(delete("/admin/users/"+owner.getId()).cookie(ownerCookie).with(csrf()).contentType("application/json").content(body(Map.of("confirmation",owner.getEmail(),"reason","Must not delete the owner account")))).andExpect(status().isBadRequest());
        User noReasonTarget=user(true,"USER");
        mvc.perform(delete("/admin/users/"+noReasonTarget.getId()).cookie(ownerCookie).with(csrf()).contentType("application/json").content(body(Map.of("confirmation",noReasonTarget.getEmail())))).andExpect(status().isOk());
        String standardNotice=db.queryForObject("SELECT body_cipher FROM mail_outbox WHERE recipient=? AND subject_line=?",String.class,noReasonTarget.getEmail(),"Your InvestSmart account has been deleted");
        assertTrue(crypto.decrypt(standardNotice).contains("This action was completed by an authorized administrator as part of account administration."));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM admin_audit WHERE action_name='DELETE_USER' AND target_id=? AND reason=''",Integer.class,noReasonTarget.getId()));
    }
    @Test void suspensionRevokesSessionsAndPreservesCashAndOwner()throws Exception{User owner=user(true,"ADMIN"),u=user(true,"USER");Cookie a=cookie(owner),c=cookie(u);mvc.perform(post("/admin/users/"+u.getId()+"/actions").cookie(a).with(csrf()).contentType("application/json").content(body(Map.of("action","SUSPEND","reason","Investigating repeated unauthorized access")))).andExpect(status().isOk());mvc.perform(get("/portfolio/me").cookie(c)).andExpect(status().isUnauthorized());assertEquals(0,users.findById(u.getId()).orElseThrow().getVirtualBalance().compareTo(new BigDecimal("100")));var w=new WalletRequest();w.setAmount(BigDecimal.ONE);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->wallet.deposit(u.getEmail(),w));mvc.perform(post("/admin/users/"+owner.getId()+"/actions").cookie(a).with(csrf()).contentType("application/json").content(body(Map.of("action","SUSPEND","reason","Cannot suspend owner")))).andExpect(status().isBadRequest());mvc.perform(post("/admin/users/"+u.getId()+"/actions").cookie(a).with(csrf()).contentType("application/json").content(body(Map.of("action","REACTIVATE","reason","Investigation completed")))).andExpect(status().isOk());assertEquals("ACTIVE",users.findById(u.getId()).orElseThrow().getAccountStatus());mvc.perform(get("/portfolio/me").cookie(c)).andExpect(status().isUnauthorized());}
    @Test void staleAdminConfirmationRequiresReauthenticationAndReviewsAreAudited()throws Exception{User owner=user(true,"ADMIN"),u=user(true,"USER");Cookie c=cookie(owner);db.update("UPDATE auth_sessions SET reauth_at=?",Timestamp.from(Instant.now().minusSeconds(301)));mvc.perform(post("/admin/users/"+u.getId()+"/actions").cookie(c).with(csrf()).contentType("application/json").content(body(Map.of("action","REVOKE_SESSIONS","reason","Access review")))).andExpect(status().isPreconditionRequired());for(int i=0;i<5;i++)events.record("ACCESS_DENIED",u.getId(),u.getEmail(),request(),"Protected operation");Long alert=db.queryForObject("SELECT id FROM security_alerts",Long.class);mvc.perform(get("/admin/alerts/"+alert).cookie(c)).andExpect(status().isOk()).andExpect(jsonPath("$.events").isArray()).andExpect(jsonPath("$.subject_key").doesNotExist());var review=body(Map.of("status","RESOLVED","notes","False positive: controlled test"));mvc.perform(post("/admin/alerts/"+alert+"/review").cookie(c).with(csrf()).contentType("application/json").content(review)).andExpect(status().isPreconditionRequired());mvc.perform(post("/users/me/reauthenticate").cookie(c).with(csrf()).contentType("application/json").content(body(Map.of("password",PASSWORD,"code",SecurityCrypto.totp(SECRET,Instant.now().getEpochSecond()/30))))).andExpect(status().isOk());mvc.perform(post("/admin/alerts/"+alert+"/review").cookie(c).with(csrf()).contentType("application/json").content(review)).andExpect(status().isOk());assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM admin_audit WHERE action_name='REVIEW_ALERT'",Integer.class));}
    @Test void staleAlertsShowAccountEmailAndMoveToAuditAfterTwentyFourHours()throws Exception{
        User owner=user(true,"ADMIN"),target=user(true,"USER");Cookie c=cookie(owner);
        for(int i=0;i<5;i++)events.record("ACCESS_DENIED",target.getId(),target.getEmail(),request(),"Protected operation");
        Long alert=db.queryForObject("SELECT id FROM security_alerts",Long.class);
        mvc.perform(get("/admin/alerts").param("status","ALL").cookie(c)).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].user_email").value(target.getEmail()));
        mvc.perform(get("/admin/alerts/"+alert).cookie(c)).andExpect(status().isOk()).andExpect(jsonPath("$.user_email").value(target.getEmail()));
        db.update("UPDATE security_alerts SET last_at=? WHERE id=?",Timestamp.from(Instant.now().minusSeconds(25*60*60)),alert);
        mvc.perform(get("/admin/alerts").param("status","OPEN").cookie(c)).andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0));
        assertEquals("RESOLVED",db.queryForObject("SELECT status FROM security_alerts WHERE id=?",String.class,alert));
        assertEquals(Boolean.TRUE,db.queryForObject("SELECT seen FROM security_alerts WHERE id=?",Boolean.class,alert));
        mvc.perform(get("/admin/audit").cookie(c)).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].action_name").value("ALERT_AUTO_ARCHIVED")).andExpect(jsonPath("$.items[0].actor_email").value("System")).andExpect(jsonPath("$.items[0].target_email").value(target.getEmail()));
    }
    @Test void suspensionAndWithdrawalSerializeOnTheSameUserLock()throws Exception{
        User owner=user(true,"ADMIN"),u=user(true,"USER");
        var executor=Executors.newSingleThreadExecutor();var entered=new CountDownLatch(1);
        var pending=new java.util.concurrent.atomic.AtomicReference<Future<?>>();var request=request();
        accounts.issue(owner,request,new MockHttpServletResponse());
        request.setAttribute("sessionId",db.queryForObject("SELECT id FROM auth_sessions WHERE user_id=? ORDER BY created_at DESC LIMIT 1",String.class,owner.getId()));
        try{
            new TransactionTemplate(tx).executeWithoutResult(status->{
                users.findByIdForUpdate(u.getId()).orElseThrow();
                admin.accountAction(owner.getEmail(),u.getId(),"SUSPEND","Concurrent access review",request);
                pending.set(executor.submit(()->{entered.countDown();var w=new WalletRequest();w.setAmount(BigDecimal.TEN);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->wallet.withdraw(u.getEmail(),w));}));
                try{assertTrue(entered.await(3,TimeUnit.SECONDS));Thread.sleep(100);assertFalse(pending.get().isDone());}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}
            });
            pending.get().get(10,TimeUnit.SECONDS);
            assertEquals(0,users.findById(u.getId()).orElseThrow().getVirtualBalance().compareTo(new BigDecimal("100")));
            assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM admin_audit WHERE action_name='SUSPEND'",Integer.class));
        }finally{executor.shutdownNow();}
    }
    @Test void adminV2OverviewIncludesVerificationGrowthAndSessionActivity()throws Exception{
        User owner=user(true,"ADMIN"),target=user(false,"USER");Cookie c=cookie(owner);Cookie targetCookie=cookie(target);
        mvc.perform(get("/admin/overview").cookie(c)).andExpect(status().isOk()).andExpect(jsonPath("$.verifiedUsers").value(1)).andExpect(jsonPath("$.recentRegistrations").value(2));
        mvc.perform(get("/admin/users/"+target.getId()).cookie(c)).andExpect(status().isOk()).andExpect(jsonPath("$.user.createdAt").isNotEmpty()).andExpect(jsonPath("$.sessions[0].lastActivityAt").isNotEmpty()).andExpect(jsonPath("$.sessions[0].device").isString());
        mvc.perform(get("/admin/users").cookie(c).param("status","ALL")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].created_at").isNotEmpty());
        assertNotNull(targetCookie);
    }
    @Test void adminSupportActionsQueueRateLimitedMailAndResetRevokesSessions()throws Exception{
        User owner=user(true,"ADMIN"),target=user(true,"USER"),unverified=user(false,"USER");Cookie c=cookie(owner),targetCookie=cookie(target);
        var reset=mvc.perform(post("/admin/users/"+target.getId()+"/password-reset").cookie(c).with(csrf()).contentType("application/json").content(body(Map.of("reason","Require reset after a security review")))).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not confirmed"))).andReturn();
        assertFalse(reset.getResponse().getContentAsString().contains("token="));
        mvc.perform(get("/users/me").cookie(targetCookie)).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",target.getEmail(),"password",PASSWORD)))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PASSWORD_RESET_REQUIRED")).andExpect(header().doesNotExist("Set-Cookie"));
        String resetToken=crypto.randomToken();action(target,resetToken,"RESET");
        mvc.perform(post("/auth/reset-password").with(csrf()).contentType("application/json").content(body(Map.of("token",resetToken,"password","A newly chosen secure passphrase")))).andExpect(status().isOk());
        mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",target.getEmail(),"password","A newly chosen secure passphrase")))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PHONE_SETUP"));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM admin_audit WHERE action_name='REQUIRE_PASSWORD_RESET' AND target_id=?",Integer.class,target.getId()));
        mvc.perform(post("/admin/users/"+unverified.getId()+"/resend-verification").cookie(c).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not confirmed")));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM admin_audit WHERE action_name='RESEND_VERIFICATION' AND target_id=?",Integer.class,unverified.getId()));
        mvc.perform(post("/admin/users/"+owner.getId()+"/resend-verification").cookie(c).with(csrf())).andExpect(status().isBadRequest());
        assertFalse(db.queryForObject("SELECT body_cipher FROM mail_outbox WHERE recipient=? ORDER BY id DESC LIMIT 1",String.class,unverified.getEmail()).contains("token="));
    }
    @Test void adminCanRevokeOneSessionAndAdjustBalanceOnlyThroughAuditedLedger()throws Exception{
        User owner=user(true,"ADMIN"),target=user(true,"USER");Cookie c=cookie(owner);cookie(target);
        String session=db.queryForObject("SELECT id FROM auth_sessions WHERE user_id=? ORDER BY created_at DESC LIMIT 1",String.class,target.getId());
        mvc.perform(delete("/admin/users/"+target.getId()+"/sessions/"+session).cookie(c).with(csrf()).contentType("application/json").content(body(Map.of("reason","Revoke this device after user request")))).andExpect(status().isOk());
        assertEquals(Boolean.TRUE,db.queryForObject("SELECT revoked FROM auth_sessions WHERE id=?",Boolean.class,session));
        mvc.perform(post("/admin/users/"+target.getId()+"/balance-adjustments").cookie(c).with(csrf()).contentType("application/json").content(body(Map.of("amount","25.25","reason","Correct approved practice balance")))).andExpect(status().isOk());
        assertEquals(0,users.findById(target.getId()).orElseThrow().getVirtualBalance().compareTo(new BigDecimal("125.25")));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM admin_balance_adjustments WHERE user_id=? AND amount=? AND reason=?",Integer.class,target.getId(),new BigDecimal("25.25"),"Correct approved practice balance"));
        mvc.perform(post("/admin/users/"+target.getId()+"/balance-adjustments").cookie(c).with(csrf()).contentType("application/json").content(body(Map.of("amount","-200","reason","Invalid negative test adjustment")))).andExpect(status().isBadRequest());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM admin_audit WHERE action_name='BALANCE_ADJUSTMENT' AND target_id=?",Integer.class,target.getId()));
    }
    @Test void operationsEndpointExposesSafeMailMarketAndRejectedOrderHealth()throws Exception{
        User owner=user(true,"ADMIN"),target=user(true,"USER");Cookie c=cookie(owner);
        events.record("ORDER_REJECTED",target.getId(),target.getEmail(),request(),"Rejected order: insufficient virtual cash");
        mvc.perform(get("/admin/operations").cookie(c)).andExpect(status().isOk()).andExpect(jsonPath("$.orders.items").isArray()).andExpect(jsonPath("$.orderFailures[0].user_email").value(target.getEmail())).andExpect(jsonPath("$.mail.messages").isArray()).andExpect(jsonPath("$.market.providerTelemetry").isMap()).andExpect(jsonPath("$.settlementJob").isMap()).andExpect(jsonPath("$.listingAdministration").value(org.hamcrest.Matchers.containsString("overrides")));
        assertFalse(json.writeValueAsString(admin.operations("ALL",1)).contains("body_cipher"));
    }
    @Test void listingOverridesPersistAndApplyToIpoTimelineAndAvailability(){
        var source=new com.example.investsmart.investment.IpoMarketService.Ipo("ipo-1","IPO","Example IPO",null,"open","regular",new BigDecimal("10"),new BigDecimal("20"),1,1,java.time.LocalDate.parse("2026-01-01"),java.time.LocalDate.parse("2026-01-02"),null,null,java.time.LocalDate.parse("2026-01-03"),java.time.LocalDate.parse("2026-01-04"),null,null,"NSE",null,null,Instant.now(),false,true,false);
        listingOverrides.save("IPO","ipo-1",false,java.time.LocalDate.parse("2026-02-01"),java.time.LocalDate.parse("2026-02-02"),java.time.LocalDate.parse("2026-02-03"),java.time.LocalDate.parse("2026-02-04"),"Correct verified issuer dates",1L);
        var effective=listingOverrides.apply(source);
        assertFalse(effective.available());assertTrue(effective.datesOverridden());assertEquals(java.time.LocalDate.parse("2026-02-01"),effective.openDate());assertEquals(java.time.LocalDate.parse("2026-02-04"),effective.listingDate());
        listingOverrides.save("IPO","ipo-1",true,null,null,null,null,"Return to provider dates",1L);
        var restored=listingOverrides.apply(source);assertTrue(restored.available());assertFalse(restored.datesOverridden());assertEquals(source.openDate(),restored.openDate());
        var bond=new com.example.investsmart.investment.BondMarketService.Bond("NSE_EQ|IN0000000001","TEST","Test bond","IN0000000001","NSE","government","GS",null,true);
        listingOverrides.save("BOND",bond.instrumentKey(),false,null,null,null,null,"Hide invalid catalog entry",1L);
        assertFalse(listingOverrides.apply(bond).available());
    }
    @Test void listingChangesRequireRecentOwnerReauthentication()throws Exception{
        User owner=user(true,"ADMIN");Cookie c=cookie(owner);
        db.update("UPDATE auth_sessions SET reauth_at=? WHERE user_id=?",Timestamp.from(Instant.now().minusSeconds(301)),owner.getId());
        mvc.perform(post("/admin/operations/listings").cookie(c).with(csrf()).contentType("application/json").content(body(Map.of("type","IPO","referenceId","ipo-1","available",false,"reason","Temporarily disable listing")))).andExpect(status().isPreconditionRequired());
        mvc.perform(post("/admin/operations/ipo-applications/1/settlement-retry").cookie(c).with(csrf()).contentType("application/json").content(body(Map.of("reason","Retry after confirming issuer data")))).andExpect(status().isPreconditionRequired());
        db.update("UPDATE auth_sessions SET reauth_at=? WHERE user_id=?",Timestamp.from(Instant.now()),owner.getId());
        mvc.perform(post("/admin/operations/listings").cookie(c).with(csrf()).contentType("application/json").content(body(Map.of("type","IPO","referenceId","ipo-1","available",true,"openDate","2026-02-02","closeDate","2026-02-01","allotmentDate","2026-02-03","listingDate","2026-02-04","reason","Reject invalid date order")))).andExpect(status().isBadRequest());
    }
    @Test void runtimeHealthCountersPersistAcrossServiceInstances(){
        Instant started=Instant.parse("2026-11-04T10:00:00Z"),failed=Instant.parse("2026-11-04T10:01:00Z"),completed=Instant.parse("2026-11-04T10:02:00Z");
        healthStore.started("IPO_SETTLEMENT",started);healthStore.failed("IPO_SETTLEMENT",failed,"Provider unavailable");healthStore.completed("IPO_SETTLEMENT",completed);
        var afterRestart=new AdminHealthStore(db).status("IPO_SETTLEMENT");
        assertEquals(1L,afterRestart.get("failureCount"));assertEquals(completed,afterRestart.get("lastCompletedAt"));assertEquals("Provider unavailable",afterRestart.get("lastFailure"));
    }
    @Test void totpMatchesRfc6238SixDigitVectorsAndEncryptionAuthenticates(){assertEquals("287082",SecurityCrypto.totp(SECRET,59/30));assertEquals("081804",SecurityCrypto.totp(SECRET,1111111109L/30));assertEquals("050471",SecurityCrypto.totp(SECRET,1111111111L/30));assertEquals("005924",SecurityCrypto.totp(SECRET,1234567890L/30));assertEquals("279037",SecurityCrypto.totp(SECRET,2000000000L/30));assertEquals("353130",SecurityCrypto.totp(SECRET,20000000000L/30));String encrypted=crypto.encrypt("private value");assertEquals("private value",crypto.decrypt(encrypted));assertNotEquals(encrypted,crypto.encrypt("private value"));assertThrows(IllegalStateException.class,()->crypto.decrypt(encrypted.substring(0,encrypted.length()-4)+"AAAA"));}
}
