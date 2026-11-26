package com.example.investsmart.investment;
import com.example.investsmart.entity.User;
import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.security.SecurityAccounts;
import jakarta.servlet.http.Cookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @ActiveProfiles("test") @AutoConfigureMockMvc(print=MockMvcPrint.NONE) @Transactional
class InvestmentApiTests {
    @Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired SecurityAccounts security;@Autowired InvestmentAccountService accounts;@Autowired IpoApplicationRepository apps;
    User user(){var u=new User();u.setEmail(UUID.randomUUID()+"@example.invalid");u.setRole("USER");u.setEmailVerified(true);u.setVirtualBalance(new BigDecimal("1000"));return users.saveAndFlush(u);}
    Cookie session(User u){return new Cookie(SecurityAccounts.COOKIE,security.issue(u,new MockHttpServletRequest(),new MockHttpServletResponse()));}
    @Test void interestsAreUserScopedAndRepeatedSaveDoesNotDuplicate() throws Exception {
        var owner=user();var other=user();var first=accounts.save(owner.getEmail(),"IPO","test-ipo","Test IPO");assertEquals(first.id(),accounts.save(owner.getEmail(),"IPO","test-ipo","Test IPO").id());
        mvc.perform(get("/investments/saved")).andExpect(status().isUnauthorized());
        mvc.perform(get("/investments/saved").cookie(session(owner)).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Test IPO")).andExpect(jsonPath("$[0].userId").doesNotExist());
        mvc.perform(get("/investments/saved").cookie(session(other)).with(csrf())).andExpect(content().json("[]"));
        mvc.perform(delete("/investments/saved/"+first.id()).cookie(session(other)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(delete("/investments/saved/"+first.id()).cookie(session(owner)).with(csrf())).andExpect(status().isNoContent());
    }
    @Test void applicationsDoNotExposeStoredDrawAndOwnershipCannotBeBypassed() throws Exception {
        var owner=user();var other=user();var a=new IpoApplication();a.setUserId(owner.getId());a.setIpoId("test-ipo");a.setStatus("PENDING");a.setDraw(new BigDecimal("0.3"));a.setReservedAmount(new BigDecimal("500"));a=apps.saveAndFlush(a);
        mvc.perform(get("/investments/ipo-applications").cookie(session(owner)).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$[0].draw").doesNotExist()).andExpect(jsonPath("$[0].userId").doesNotExist());
        mvc.perform(post("/investments/ipo-applications/"+a.getId()+"/cancel").cookie(session(other)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/investments/ipo-applications/test-ipo").cookie(session(owner)).with(csrf()).contentType("application/json").content("{\"lots\":0}" )).andExpect(status().isBadRequest());
        mvc.perform(post("/investments/saved").cookie(session(owner)).with(csrf()).contentType("application/json").content("{\"kind\":\"UNKNOWN\",\"referenceId\":\"x\"}" )).andExpect(status().isBadRequest());
    }
}
