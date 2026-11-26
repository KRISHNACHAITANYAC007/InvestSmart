package com.example.investsmart;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@Transactional
class AccountApiTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired SecurityAccounts security;
    @Autowired com.example.investsmart.settlement.SettlementFunds settlement;

    @Test void settlementReadsAreOwnedAndRegularAccountsCannotActivateCalendars() throws Exception {
        var user=new User();user.setEmail("settlement-api-"+UUID.randomUUID()+"@example.invalid");user.setVirtualBalance(new BigDecimal("100"));user.setRole("USER");user.setEmailVerified(true);users.saveAndFlush(user);
        var other=new User();other.setEmail("settlement-other-"+UUID.randomUUID()+"@example.invalid");other.setVirtualBalance(new BigDecimal("200"));other.setRole("USER");other.setEmailVerified(true);users.saveAndFlush(other);
        settlement.creditPending(user.getId(),"own","own","NSE","EQUITY",java.time.LocalDate.of(2026,11,11),new BigDecimal("7.00"),com.example.investsmart.settlement.SettlementFunds.CreditPolicy.RESTRICTED);
        settlement.creditPending(other.getId(),"other","other","NSE","EQUITY",java.time.LocalDate.of(2026,11,11),new BigDecimal("999.00"),com.example.investsmart.settlement.SettlementFunds.CreditPolicy.RESTRICTED);
        Cookie auth=new Cookie(SecurityAccounts.COOKIE,security.issue(user,new MockHttpServletRequest(),new MockHttpServletResponse()));
        mvc.perform(get("/settlement/summary")).andExpect(status().isUnauthorized());
        mvc.perform(get("/settlement/summary").cookie(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.restricted").value(7));
        mvc.perform(get("/settlement/history").param("userId",other.getId().toString()).cookie(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].amount").value(7));
        mvc.perform(get("/settlement/history").param("page","0").cookie(auth)).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/operations/settlement-calendars").cookie(auth)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/operations/settlement-calendars").cookie(auth).with(csrf()).contentType("application/json").content("{}" )).andExpect(status().isForbidden());
    }

    @Test void authenticatedFundsAndPortfolioStayInSyncAndRejectInvalidAmounts() throws Exception {
        var user=new User();user.setEmail("api-check-"+UUID.randomUUID()+"@example.invalid");user.setVirtualBalance(new BigDecimal("100"));user.setRole("USER");user.setEmailVerified(true);users.saveAndFlush(user);
        Cookie authorization=new Cookie(SecurityAccounts.COOKIE,security.issue(user,new MockHttpServletRequest(),new MockHttpServletResponse()));
        mvc.perform(get("/portfolio/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/wallet/deposit").cookie(authorization).with(csrf()).contentType("application/json").content("{\"amount\":25.50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(125.5));
        mvc.perform(get("/portfolio/me").cookie(authorization).with(csrf())).andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(125.5)).andExpect(jsonPath("$.valuationAvailable").value(true));
        mvc.perform(post("/wallet/withdraw").cookie(authorization).with(csrf()).contentType("application/json").content("{\"amount\":500}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/wallet/deposit").cookie(authorization).with(csrf()).contentType("application/json").content("{\"amount\":0.001}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/wallet/me").cookie(authorization).with(csrf())).andExpect(status().isOk()).andExpect(content().string("125.50"));
        mvc.perform(get("/orders/me").cookie(authorization).with(csrf())).andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
