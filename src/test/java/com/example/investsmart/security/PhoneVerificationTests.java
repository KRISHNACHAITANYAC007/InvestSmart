package com.example.investsmart.security;

import com.example.investsmart.entity.User;
import com.example.investsmart.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:phone-auth;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test") @AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class PhoneVerificationTests {
    @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired SecurityCrypto crypto;@Autowired UserRepository users;
    @Autowired SecurityAccounts accounts;@Autowired PhoneVerification phone;@Autowired PasswordEncoder passwords;@Autowired ObjectMapper json;
    static final String PASSWORD="phone test correct horse battery";
    User user;
    record Login(String challenge,Cookie binding){}
    record Pair(PhoneWebAuthnFixture key,Cookie setup,List<String> recovery,Cookie display){}
    @BeforeEach void prepare(){
        for(String table:List.of("phone_sessions","phone_actions","phone_credentials","auth_actions","auth_sessions","security_alerts","security_events","mail_outbox","owner_admin","admin_credentials"))db.update("DELETE FROM "+table);
        user=new User();user.setName("Phone test");user.setEmail(UUID.randomUUID()+"@example.invalid");user.setEmailVerified(true);user.setRole("USER");user.setPassword(passwords.encode(PASSWORD));user.setVirtualBalance(new BigDecimal("100"));user=users.saveAndFlush(user);
    }
    @AfterEach void cleanup(){users.deleteById(user.getId());}
    String body(Object value)throws Exception{return json.writeValueAsString(value);}
    JsonNode data(MvcResult result)throws Exception{return json.readTree(result.getResponse().getContentAsString());}
    MvcResult postJson(String path,Object body,Cookie... cookies)throws Exception{var request=post(path).with(csrf()).contentType("application/json").content(body(body));if(cookies.length>0)request.cookie(cookies);return mvc.perform(request).andReturn();}
    Login login(boolean enrolled)throws Exception{
        var result=postJson("/auth/login",Map.of("email",user.getEmail(),"password",PASSWORD));
        assertEquals(200,result.getResponse().getStatus());assertEquals(enrolled?"PHONE_REQUIRED":"PHONE_SETUP",data(result).path("state").asText());assertNull(result.getResponse().getCookie(SecurityAccounts.COOKIE));
        return new Login(data(result).path("challenge").asText(),result.getResponse().getCookie(PhoneVerification.LOGIN_COOKIE));
    }
    Cookie pairLink()throws Exception{
        String cipher=db.queryForObject("SELECT body_cipher FROM mail_outbox WHERE subject_line='Pair your InvestSmart phone' AND recipient=? ORDER BY id DESC LIMIT 1",String.class,user.getEmail());
        String token=crypto.decrypt(cipher).split("#pair=")[1].split("\\s")[0];
        var result=postJson("/auth/phone/pair",Map.of("token",token));assertEquals(200,result.getResponse().getStatus());
        assertEquals(400,postJson("/auth/phone/pair",Map.of("token",token)).getResponse().getStatus());
        return result.getResponse().getCookie(PhoneVerification.SETUP_COOKIE);
    }
    Pair pair()throws Exception{
        Cookie setup=pairLink();var options=postJson("/auth/phone/registration/options",Map.of(),setup);assertEquals(200,options.getResponse().getStatus());
        var key=new PhoneWebAuthnFixture(json);
        var registered=postJson("/auth/phone/registration",Map.of("challenge",data(options).path("challenge").asText(),"credential",key.registration(data(options).path("publicKey").asText(),"http://localhost:5173",true)),setup);
        assertEquals(200,registered.getResponse().getStatus(),registered.getResponse().getContentAsString());
        List<String> recovery=new ArrayList<>();data(registered).path("recoveryCodes").forEach(n->recovery.add(n.asText()));
        var acknowledge=postJson("/auth/phone/acknowledge",Map.of(),setup);assertEquals(200,acknowledge.getResponse().getStatus());
        return new Pair(key,setup,recovery,acknowledge.getResponse().getCookie(PhoneVerification.DISPLAY_COOKIE));
    }
    String currentCode(){String cipher=db.queryForObject("SELECT secret_cipher FROM phone_credentials WHERE user_id=?",String.class,user.getId());return SecurityCrypto.totp(crypto.decrypt(cipher),Instant.now().getEpochSecond()/30);}
    @Test void initialPairingRequiresEmailAndNoRestrictedCookieAuthorizesTheApp()throws Exception{
        Login login=login(false);
        mvc.perform(get("/users/me").cookie(login.binding())).andExpect(status().isUnauthorized());
        assertEquals(400,postJson("/auth/phone/registration/options",Map.of(),login.binding()).getResponse().getStatus());
        Pair pair=pair();assertEquals(10,pair.recovery().size());
        for(Cookie cookie:List.of(pair.setup(),pair.display()))mvc.perform(get("/users/me").cookie(cookie)).andExpect(status().isUnauthorized());
        var code=mvc.perform(get("/auth/phone/code").cookie(pair.display())).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn();
        assertTrue(data(code).path("code").asText().matches("[0-9]{6}"));assertFalse(code.getResponse().getContentAsString().contains("secret"));
        var signed=postJson("/auth/phone/verify",Map.of("challenge",login.challenge(),"code",currentCode()),login.binding());
        assertEquals(200,signed.getResponse().getStatus());Cookie session=signed.getResponse().getCookie(SecurityAccounts.COOKIE);assertNotNull(session);assertTrue(session.isHttpOnly());
        mvc.perform(get("/users/me").cookie(session)).andExpect(status().isOk());
        assertEquals(400,postJson("/auth/phone/verify",Map.of("challenge",login.challenge(),"code",currentCode()),login.binding()).getResponse().getStatus());
    }
    @Test void phoneDisplayAndSamePhoneLoginRequireSignedUserVerifiedAssertions()throws Exception{
        login(false);Pair pair=pair();
        var options=postJson("/auth/phone/display/options",Map.of());
        String challenge=data(options).path("challenge").asText();String credential=pair.key().assertion(data(options).path("publicKey").asText(),"http://localhost:5173",true,1);
        var opened=postJson("/auth/phone/display",Map.of("challenge",challenge,"credential",credential));assertEquals(200,opened.getResponse().getStatus(),opened.getResponse().getContentAsString());
        assertEquals(400,postJson("/auth/phone/display",Map.of("challenge",challenge,"credential",credential)).getResponse().getStatus());
        Login login=login(true);var loginOptions=postJson("/auth/phone/login/options",Map.of("challenge",login.challenge()),login.binding());
        var signed=postJson("/auth/phone/login",Map.of("challenge",data(loginOptions).path("challenge").asText(),"loginChallenge",login.challenge(),"credential",pair.key().assertion(data(loginOptions).path("publicKey").asText(),"http://localhost:5173",true,2)),login.binding());
        assertEquals(200,signed.getResponse().getStatus(),signed.getResponse().getContentAsString());assertNotNull(signed.getResponse().getCookie(SecurityAccounts.COOKIE));
    }
    @Test void wrongOriginAndMissingUserVerificationAreRejectedAndChallengesAreConsumed()throws Exception{
        login(false);Pair pair=pair();
        for(boolean uv:List.of(false,true)){
            var options=postJson("/auth/phone/display/options",Map.of());String challenge=data(options).path("challenge").asText();
            String credential=pair.key().assertion(data(options).path("publicKey").asText(),uv?"https://attacker.invalid":"http://localhost:5173",uv,1);
            assertEquals(401,postJson("/auth/phone/display",Map.of("challenge",challenge,"credential",credential)).getResponse().getStatus());
            assertEquals(400,postJson("/auth/phone/display",Map.of("challenge",challenge,"credential",credential)).getResponse().getStatus());
        }
    }
    @Test void loginIsBrowserBoundAndOldStepsAndReusedCodesAreRejected()throws Exception{
        PhoneTestCredentials.enroll(db,crypto,user.getId());Login login=login(true);
        assertEquals(400,postJson("/auth/phone/verify",Map.of("challenge",login.challenge(),"code",currentCode())).getResponse().getStatus());
        String old=SecurityCrypto.totp(PhoneTestCredentials.SECRET,Instant.now().getEpochSecond()/30-1);
        assertEquals(401,postJson("/auth/phone/verify",Map.of("challenge",login.challenge(),"code",old),login.binding()).getResponse().getStatus());
        String code=currentCode();assertEquals(200,postJson("/auth/phone/verify",Map.of("challenge",login.challenge(),"code",code),login.binding()).getResponse().getStatus());
        Login second=login(true);assertEquals(401,postJson("/auth/phone/verify",Map.of("challenge",second.challenge(),"code",code),second.binding()).getResponse().getStatus());
    }
    @Test void concurrentSameCodeCanIssueOnlyOneSession()throws Exception{
        PhoneTestCredentials.enroll(db,crypto,user.getId());Login a=login(true),b=login(true);String code=currentCode();
        var pool=Executors.newFixedThreadPool(2);try{
            var futures=pool.invokeAll(List.of(()->postJson("/auth/phone/verify",Map.of("challenge",a.challenge(),"code",code),a.binding()).getResponse().getStatus(),()->postJson("/auth/phone/verify",Map.of("challenge",b.challenge(),"code",code),b.binding()).getResponse().getStatus()));
            List<Integer> statuses=new ArrayList<>();for(var future:futures)statuses.add((Integer)future.get());Collections.sort(statuses);assertEquals(List.of(200,401),statuses);
            assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM auth_sessions WHERE user_id=?",Integer.class,user.getId()));
        }finally{pool.shutdownNow();}
    }
    @Test void recoveryCannotDirectlySignInAndReplacementRevokesOldCredentialsAndSessions()throws Exception{
        PhoneTestCredentials.enroll(db,crypto,user.getId());Login login=login(true);
        assertEquals(401,postJson("/auth/phone/verify",Map.of("challenge",login.challenge(),"code",PhoneTestCredentials.RECOVERY),login.binding()).getResponse().getStatus());
        Cookie old=new Cookie(SecurityAccounts.COOKIE,accounts.issue(user,new MockHttpServletRequest(),new MockHttpServletResponse()));
        var recovered=postJson("/auth/phone/recover",Map.of("challenge",login.challenge(),"code",PhoneTestCredentials.RECOVERY),login.binding());assertEquals(200,recovered.getResponse().getStatus());assertNull(recovered.getResponse().getCookie(SecurityAccounts.COOKIE));
        mvc.perform(get("/users/me").cookie(old)).andExpect(status().isUnauthorized());assertFalse(phone.enrolled(user.getId()));
        String token=data(recovered).path("pairingUrl").asText().split("#setup=")[1];
        assertEquals(200,postJson("/auth/phone/replacement",Map.of("token",token)).getResponse().getStatus());
        assertEquals(400,postJson("/auth/phone/replacement",Map.of("token",token)).getResponse().getStatus());
        assertEquals(400,postJson("/auth/phone/recover",Map.of("challenge",login.challenge(),"code",PhoneTestCredentials.RECOVERY),login.binding()).getResponse().getStatus());
    }
    @Test void expiryResetAndRevocationInvalidatePendingPhoneActionsWithoutRemovingMfa()throws Exception{
        PhoneTestCredentials.enroll(db,crypto,user.getId());Login login=login(true);
        accounts.revokeAll(user.getId());assertTrue(phone.enrolled(user.getId()));
        assertEquals(400,postJson("/auth/phone/verify",Map.of("challenge",login.challenge(),"code",currentCode()),login.binding()).getResponse().getStatus());
        Login expired=login(true);db.update("UPDATE phone_actions SET expires_at=? WHERE token_hash=?",Timestamp.from(Instant.now().minusSeconds(1)),crypto.fingerprint(expired.challenge()));
        assertEquals(400,postJson("/auth/phone/verify",Map.of("challenge",expired.challenge(),"code",currentCode()),expired.binding()).getResponse().getStatus());
        Login pending=login(true);String reset=crypto.randomToken();db.update("INSERT INTO auth_actions(token_hash,user_id,purpose,expires_at) VALUES(?,?,'RESET',?)",crypto.fingerprint(reset),user.getId(),Timestamp.from(Instant.now().plusSeconds(60)));
        accounts.reset(reset,"a changed phone password 2026");assertTrue(phone.enrolled(user.getId()));
        assertEquals(400,postJson("/auth/phone/verify",Map.of("challenge",pending.challenge(),"code",currentCode()),pending.binding()).getResponse().getStatus());
    }
    @Test void lostPhoneReplacementCanEnrollAndSignInWhileOldPhoneStaysRevoked()throws Exception{
        login(false);Pair old=pair();Login pending=login(true);
        var recovered=postJson("/auth/phone/recover",Map.of("challenge",pending.challenge(),"code",old.recovery().getFirst()),pending.binding());
        assertEquals(200,recovered.getResponse().getStatus());assertNull(recovered.getResponse().getCookie(SecurityAccounts.COOKIE));
        mvc.perform(get("/auth/phone/code").cookie(old.display())).andExpect(status().isUnauthorized());
        String token=data(recovered).path("pairingUrl").asText().split("#setup=")[1];
        var opened=postJson("/auth/phone/replacement",Map.of("token",token));assertEquals(200,opened.getResponse().getStatus());
        Cookie setup=opened.getResponse().getCookie(PhoneVerification.SETUP_COOKIE);
        var options=postJson("/auth/phone/registration/options",Map.of(),setup);assertEquals(200,options.getResponse().getStatus());
        var replacement=new PhoneWebAuthnFixture(json);
        var registered=postJson("/auth/phone/registration",Map.of("challenge",data(options).path("challenge").asText(),"credential",replacement.registration(data(options).path("publicKey").asText(),"http://localhost:5173",true)),setup);
        assertEquals(200,registered.getResponse().getStatus(),registered.getResponse().getContentAsString());assertEquals(10,data(registered).path("recoveryCodes").size());
        assertEquals(200,postJson("/auth/phone/acknowledge",Map.of(),setup).getResponse().getStatus());
        assertEquals(400,postJson("/auth/phone/registration/options",Map.of(),old.setup()).getResponse().getStatus());
        var oldOptions=postJson("/auth/phone/display/options",Map.of());
        var rejected=postJson("/auth/phone/display",Map.of("challenge",data(oldOptions).path("challenge").asText(),"credential",old.key().assertion(data(oldOptions).path("publicKey").asText(),"http://localhost:5173",true,1)));
        assertEquals(400,rejected.getResponse().getStatus());assertNull(rejected.getResponse().getCookie(PhoneVerification.DISPLAY_COOKIE));
        Login fresh=login(true);var signOptions=postJson("/auth/phone/login/options",Map.of("challenge",fresh.challenge()),fresh.binding());
        var signed=postJson("/auth/phone/login",Map.of("challenge",data(signOptions).path("challenge").asText(),"loginChallenge",fresh.challenge(),"credential",replacement.assertion(data(signOptions).path("publicKey").asText(),"http://localhost:5173",true,1)),fresh.binding());
        assertEquals(200,signed.getResponse().getStatus(),signed.getResponse().getContentAsString());
        mvc.perform(get("/users/me").cookie(signed.getResponse().getCookie(SecurityAccounts.COOKIE))).andExpect(status().isOk());
    }
    @Test void legacySessionsAndExpiredDisplaySessionsCannotBypassVerification()throws Exception{
        Cookie legacy=new Cookie(SecurityAccounts.COOKIE,accounts.issue(user,new MockHttpServletRequest(),new MockHttpServletResponse()));
        db.update("UPDATE auth_sessions SET factor_completed=FALSE WHERE user_id=?",user.getId());mvc.perform(get("/users/me").cookie(legacy)).andExpect(status().isUnauthorized());
        login(false);Pair pair=pair();db.update("UPDATE phone_sessions SET expires_at=?",Timestamp.from(Instant.now().minusSeconds(1)));
        mvc.perform(get("/auth/phone/code").cookie(pair.display())).andExpect(status().isUnauthorized());
    }
    @Test void phoneFailuresTriggerCooldownWithoutGrantingSessions()throws Exception{
        PhoneTestCredentials.enroll(db,crypto,user.getId());Login login=login(true);
        for(int i=0;i<5;i++)assertEquals(401,postJson("/auth/phone/verify",Map.of("challenge",login.challenge(),"code","bad"),login.binding()).getResponse().getStatus());
        assertEquals(429,postJson("/auth/phone/verify",Map.of("challenge",login.challenge(),"code",currentCode()),login.binding()).getResponse().getStatus());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM auth_sessions WHERE user_id=?",Integer.class,user.getId()));
        assertTrue(db.queryForObject("SELECT COUNT(*) FROM security_alerts WHERE user_id=?",Integer.class,user.getId())>0);
    }
}
