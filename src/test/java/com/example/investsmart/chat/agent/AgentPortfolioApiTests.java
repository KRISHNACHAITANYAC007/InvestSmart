package com.example.investsmart.chat.agent;

import com.example.investsmart.entity.User;
import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.security.SecurityAccounts;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"investsmart.features.agent-enabled=true","investsmart.ai.agent.provider=mock"})
@ActiveProfiles("test") @AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class AgentPortfolioApiTests {
    @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired SecurityAccounts security; @Autowired JdbcTemplate db;
    User owner; Cookie session;
    @BeforeEach void setup(){owner=new User();owner.setEmail(UUID.randomUUID()+"@example.invalid");owner.setRole("USER");owner.setEmailVerified(true);owner.setVirtualBalance(new BigDecimal("123.45"));owner=users.saveAndFlush(owner);session=new Cookie(SecurityAccounts.COOKIE,security.issue(owner,new MockHttpServletRequest(),new MockHttpServletResponse()));}
    @AfterEach void cleanup(){db.update("DELETE FROM auth_sessions WHERE user_id=?",owner.getId());db.update("DELETE FROM security_events WHERE user_id=?",owner.getId());users.deleteById(owner.getId());}
    @Test void requiresExistingSessionAndCsrf() throws Exception {
        mvc.perform(post("/agent/portfolio-analysis").with(csrf()).contentType("application/json").content("{\"topic\":\"OVERVIEW\",\"consent\":true}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/agent/portfolio-analysis").cookie(session).contentType("application/json").content("{\"topic\":\"OVERVIEW\",\"consent\":true}")).andExpect(status().isForbidden());
    }
    @Test void strictConsentTopicAndNoUserSelector() throws Exception {
        for(String body:new String[]{"{}","null","{\"topic\":\"OVERVIEW\",\"consent\":false}","{\"topic\":\"EXECUTE_ORDER\",\"consent\":true}","{\"topic\":\"OVERVIEW\",\"consent\":true,\"userId\":2}","{\"topic\":\"OVERVIEW\",\"consent\":\"true\"}"})
            mvc.perform(post("/agent/portfolio-analysis").cookie(session).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
    }
    @Test void returnsScopedReadWithDecimalStringsWithoutChangingCashOrInitializingAccount() throws Exception {
        mvc.perform(post("/agent/portfolio-analysis").cookie(session).with(csrf()).contentType("application/json").content("{\"topic\":\"OVERVIEW\",\"consent\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("LOCAL_DETERMINISTIC"))
                .andExpect(jsonPath("$.analysis.status").value("EMPTY_EQUITY_VIEW"))
                .andExpect(jsonPath("$.analysis.equityValue").value("0.00"))
                .andExpect(jsonPath("$.analysis.evidence.holdings").isEmpty());
        assertEquals(new BigDecimal("123.45"),users.findById(owner.getId()).orElseThrow().getVirtualBalance());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM settlement_accounts WHERE user_id=?",Integer.class,owner.getId()));
    }
    @Test void rejectsSuspensionAndEnforcesExistingQuota() throws Exception {
        for(int i=0;i<10;i++)mvc.perform(post("/agent/portfolio-analysis").cookie(session).with(csrf()).contentType("application/json").content("{\"topic\":\"CONCENTRATION\",\"consent\":true}")).andExpect(status().isOk());
        mvc.perform(post("/agent/portfolio-analysis").cookie(session).with(csrf()).contentType("application/json").content("{\"topic\":\"OVERVIEW\",\"consent\":true}")).andExpect(status().isTooManyRequests());
        owner.setAccountStatus("SUSPENDED");users.saveAndFlush(owner);
        mvc.perform(post("/agent/portfolio-analysis").cookie(session).with(csrf()).contentType("application/json").content("{\"topic\":\"OVERVIEW\",\"consent\":true}")).andExpect(status().is4xxClientError());
    }
}
