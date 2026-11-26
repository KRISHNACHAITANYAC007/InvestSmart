package com.example.investsmart.security;

import com.example.investsmart.entity.User;
import com.example.investsmart.repository.UserRepository;
import java.math.BigDecimal;
import java.util.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Keep the real CookieCsrfTokenRepository isolated from csrf() test-helper replacement. */
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:browser-csrf;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test") @AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class BrowserCsrfTests {
    @Autowired org.springframework.jdbc.core.JdbcTemplate db;@Autowired SecurityCrypto crypto;@Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired PasswordEncoder passwords;@Autowired ObjectMapper json;
    User user;Cookie xsrf;String token;
    @BeforeEach void prepare()throws Exception{
        user=new User();user.setName("Browser flow");user.setEmail(UUID.randomUUID()+"@example.invalid");user.setRole("USER");user.setEmailVerified(true);user.setPassword(passwords.encode("correct horse battery staple"));user.setVirtualBalance(new BigDecimal("100"));user=users.saveAndFlush(user);PhoneTestCredentials.enroll(db,crypto,user.getId());
        var result=mvc.perform(get("/auth/csrf")).andExpect(status().isOk()).andReturn();
        xsrf=result.getResponse().getCookie("XSRF-TOKEN");assertNotNull(xsrf);token=json.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }
    @AfterEach void cleanup(){for(String table:List.of("phone_sessions","phone_actions","phone_credentials"))db.update("DELETE FROM "+table+" WHERE user_id=?",user.getId());users.deleteById(user.getId());}
    Cookie login()throws Exception{
        String body=json.writeValueAsString(Map.of("email",user.getEmail(),"password","correct horse battery staple"));
        var first=mvc.perform(post("/auth/login").cookie(xsrf).header("X-XSRF-TOKEN",token).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PHONE_REQUIRED")).andReturn().getResponse();
        assertNull(first.getCookie(SecurityAccounts.COOKIE));
        String challenge=json.readTree(first.getContentAsString()).path("challenge").asText();
        var response=mvc.perform(post("/auth/phone/verify").cookie(xsrf,first.getCookie(PhoneVerification.LOGIN_COOKIE)).header("X-XSRF-TOKEN",token).contentType("application/json").content(json.writeValueAsString(Map.of("challenge",challenge,"code",PhoneTestCredentials.code())))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("SIGNED_IN")).andReturn().getResponse();
        Cookie session=response.getCookie(SecurityAccounts.COOKIE);assertNotNull(session);assertTrue(session.isHttpOnly());return session;
    }
    @Test void actualCookieAndHeaderPermitWritesWhileTamperingDoesNot()throws Exception{
        Cookie session=login();
        mvc.perform(post("/wallet/deposit").cookie(session,xsrf).header("X-XSRF-TOKEN",token).contentType("application/json").content("{\"amount\":1}")).andExpect(status().isOk());
        mvc.perform(post("/wallet/deposit").cookie(session,xsrf).header("X-XSRF-TOKEN","tampered").contentType("application/json").content("{\"amount\":1}")).andExpect(status().isForbidden());
        assertEquals(0,users.findById(user.getId()).orElseThrow().getVirtualBalance().compareTo(new BigDecimal("101")));
    }
    @Test void cookieLoginLogoutAndStaleCookieCleanupWorkWithoutTestSecurityHelpers()throws Exception{
        Cookie session=login();mvc.perform(get("/users/me").cookie(session)).andExpect(status().isOk());
        mvc.perform(post("/auth/logout").cookie(session,xsrf).header("X-XSRF-TOKEN",token)).andExpect(status().isOk());
        mvc.perform(get("/users/me").cookie(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/logout").cookie(session,xsrf).header("X-XSRF-TOKEN",token)).andExpect(status().isOk()).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("Max-Age=0")));
    }
}
