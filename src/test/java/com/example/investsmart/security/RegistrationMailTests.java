package com.example.investsmart.security;

import com.example.investsmart.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties={"spring.mail.host=smtp.example.invalid","spring.mail.username=fixture","spring.mail.password=fixture","investsmart.mail.from=sender@example.invalid","investsmart.public-url=http://127.0.0.1:5173"})
@ActiveProfiles("test") @AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class RegistrationMailTests {
    @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired UserRepository users;@Autowired PasswordEncoder passwords;
    @MockitoBean JavaMailSender sender;
    String email;
    static final String PASSWORD="correct horse battery staple";
    @BeforeEach void setup(){email=UUID.randomUUID()+"@example.invalid";db.update("DELETE FROM security_events");}
    @AfterEach void cleanup(){users.findByEmailIgnoreCase(email).ifPresent(u->{db.update("DELETE FROM auth_actions WHERE user_id=?",u.getId());db.update("DELETE FROM security_events WHERE user_id=?",u.getId());users.deleteById(u.getId());});db.update("DELETE FROM mail_delivery_failures WHERE mail_id IN (SELECT id FROM mail_outbox WHERE recipient=?)",email);db.update("DELETE FROM mail_outbox WHERE recipient=?",email);}
    void register(String password)throws Exception{mvc.perform(post("/auth/register").with(csrf()).contentType("application/json").content("{\"name\":\"Fixture\",\"email\":\""+email+"\",\"password\":\""+password+"\"}")).andExpect(status().isOk());}
    @Test void firstRegistrationImmediatelySubmitsMailAfterAccountCommit()throws Exception{
        register(PASSWORD);
        var mail=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);verify(sender).send(mail.capture());
        assertEquals(email,mail.getValue().getTo()[0]);assertTrue(mail.getValue().getText().contains("http://localhost:5173/verify-email?token="));
        assertFalse(users.findByEmailIgnoreCase(email).orElseThrow().isEmailVerified());
        assertTrue(db.queryForObject("SELECT sent FROM mail_outbox WHERE recipient=?",Boolean.class,email));
    }
    @Test void repeatUnverifiedRegistrationSendsAgainWithoutChangingCredentials()throws Exception{
        register(PASSWORD);register("a different long password");
        verify(sender,times(2)).send(any(SimpleMailMessage.class));
        var u=users.findByEmailIgnoreCase(email).orElseThrow();assertTrue(passwords.matches(PASSWORD,u.getPassword()));assertFalse(passwords.matches("a different long password",u.getPassword()));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM auth_actions WHERE user_id=? AND purpose='VERIFY' AND used=FALSE",Integer.class,u.getId()));
    }
    @Test void registrationAndResendShareQuotaAndVerifiedAccountsAreNotReRegistered()throws Exception{
        register(PASSWORD);
        for(int i=0;i<2;i++)mvc.perform(post("/auth/resend-verification").with(csrf()).contentType("application/json").content("{\"email\":\""+email+"\"}")).andExpect(status().isOk());
        mvc.perform(post("/auth/register").with(csrf()).contentType("application/json").content("{\"name\":\"Fixture\",\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}")).andExpect(status().isTooManyRequests());
        verify(sender,times(3)).send(any(SimpleMailMessage.class));
    }
    @Test void verifiedAccountGetsNoDuplicateSignupMailOrCredentialChanges()throws Exception{
        register(PASSWORD);var u=users.findByEmailIgnoreCase(email).orElseThrow();u.setEmailVerified(true);users.saveAndFlush(u);
        register("a different long password");verify(sender,times(1)).send(any(SimpleMailMessage.class));assertTrue(passwords.matches(PASSWORD,users.findById(u.getId()).orElseThrow().getPassword()));
    }
}
