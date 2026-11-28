package com.example.investsmart.reports;

import com.example.investsmart.entity.User;
import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.security.SecurityAccounts;
import com.example.investsmart.settlement.JournalPeriodService;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test") @AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class ReportApiTests {
    static final String ROUTE="/reports/journal-period",FROM="2026-11-25T05:00:00Z",TO="2026-11-25T05:00:01Z";
    @Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired SecurityAccounts security;@Autowired JdbcTemplate db;
    @Autowired com.example.investsmart.security.SecurityEvents events;
    @MockitoSpyBean JournalPeriodService reports;
    User owner,other;Cookie session;Long stockId;
    User user(){var user=new User();user.setEmail(UUID.randomUUID()+"@example.invalid");user.setRole("USER");user.setEmailVerified(true);user.setVirtualBalance(new BigDecimal("123.45"));return users.saveAndFlush(user);}
    @BeforeEach void setup(){owner=user();other=user();session=new Cookie(SecurityAccounts.COOKIE,security.issue(owner,new MockHttpServletRequest(),new MockHttpServletResponse()));clearInvocations(reports);}
    @AfterEach void cleanup(){for(var user:new User[]{owner,other}){for(String table:new String[]{"paper_fills","paper_tickets","holdings","orders","settlement_allocations","settlement_journal","settlement_accounts"})db.update("DELETE FROM "+table+" WHERE user_id=?",user.getId());db.update("DELETE FROM trusted_device_locks WHERE session_id IN (SELECT id FROM auth_sessions WHERE user_id=?)",user.getId());db.update("DELETE FROM auth_sessions WHERE user_id=?",user.getId());db.update("DELETE FROM security_events WHERE user_id=?",user.getId());users.deleteById(user.getId());}if(stockId!=null)db.update("DELETE FROM stocks WHERE id=?",stockId);}
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(){return get(ROUTE).cookie(session).param("from",FROM).param("to",TO);}
    void row(User user,String key,String amount){db.update("INSERT INTO settlement_journal VALUES(?,?,'SETTLED_CREDIT',?,?,NULL,NULL,?)",user.getId(),key,"a".repeat(64),new BigDecimal(amount),Timestamp.from(Instant.parse(FROM)));}
    @Test void anonymousAndInvalidRequestsAreNotCachedAndCannotRead()throws Exception{
        mvc.perform(get(ROUTE).param("from",FROM).param("to",TO)).andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(request().param("userId",other.getId().toString())).andExpect(status().isBadRequest()).andExpect(header().string("Pragma","no-cache"));
        mvc.perform(request().param("from",FROM)).andExpect(status().isBadRequest());
        mvc.perform(get(ROUTE).cookie(session)).andExpect(status().isBadRequest());verifyNoInteractions(reports);
    }
    @Test void strictTimestampBoundsRejectBeforeFinancialReads()throws Exception{
        for(String[] range:new String[][]{{TO,FROM},{FROM,FROM},{"2026-11-25T05:00:00.1Z",TO},{"2026-11-25T05:00:00+00:00",TO},{"2026-02-30T05:00:00Z",TO},{"1969-12-31T23:59:59Z",TO},{FROM,"2038-01-19T03:14:08Z"},{FROM,"2027-11-27T05:00:00Z"}})
            mvc.perform(get(ROUTE).cookie(session).param("from",range[0]).param("to",range[1])).andExpect(status().isBadRequest());verifyNoInteractions(reports);
    }
    @Test void ownedProjectionHidesInternalFieldsAndPreservesCash()throws Exception{
        row(owner,"private-owned-key","10.25");row(other,"foreign-private-key","99.00");db.update("INSERT INTO settlement_accounts VALUES(?,?,?,?)",owner.getId(),new BigDecimal("123.45"),new BigDecimal("123.45"),Timestamp.from(Instant.parse(FROM)));
        var at=Timestamp.from(Instant.parse(FROM));String instrument="REPORT-"+UUID.randomUUID();
        db.update("INSERT INTO stocks(symbol,instrument_key,current_price) VALUES('REPORTTEST',?,10)",instrument);stockId=db.queryForObject("SELECT id FROM stocks WHERE instrument_key=?",Long.class,instrument);
        db.update("INSERT INTO holdings(user_id,stock_id,quantity) VALUES(?,?,7)",owner.getId(),stockId);
        db.update("INSERT INTO orders(user_id,stock_id,order_type,status,quantity,price,total_amount,source,created_at) VALUES(?,?,'BUY','COMPLETED',1,10,10,'SYNTHETIC',?)",owner.getId(),stockId,at);
        String ticket=UUID.randomUUID().toString();db.update("INSERT INTO paper_tickets(id,user_id,request_id,side,product,variety,order_kind,validity,quantity,filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fees,policy_version,expires_at,created_at) VALUES(?,?,?,'BUY','CASH','REGULAR','MARKET','DAY',1,1,1,1,1,10,0,'FILLED',1,'test',?,?)",ticket,owner.getId(),ticket,at,at);
        db.update("INSERT INTO paper_fills VALUES(?,?,?,0,1,10,1,NULL,?)",UUID.randomUUID().toString(),ticket,owner.getId(),at);
        db.update("INSERT INTO settlement_allocations VALUES(?,?,'ORDER','kept-order','SETTLED',8,8,'OPEN',?)",UUID.randomUUID().toString(),owner.getId(),at);
        var before=financialRows();
        var result=mvc.perform(request()).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.rows.length()").value(1)).andExpect(jsonPath("$.rows[0].amount").value("10.25")).andExpect(jsonPath("$.directChanges[0].change").value("10.25")).andExpect(jsonPath("$.coverage.completeFinancialHistory").value(false)).andReturn().getResponse().getContentAsString();
        for(String hidden:new String[]{"private-owned-key","foreign-private-key","fingerprint","referenceId","allocationId","sourceProof",owner.getEmail(),other.getEmail()})assertFalse(result.contains(hidden));
        assertEquals(new BigDecimal("123.45"),users.findById(owner.getId()).orElseThrow().getVirtualBalance());assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM settlement_journal WHERE user_id IN (?,?)",Integer.class,owner.getId(),other.getId()));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM security_events WHERE user_id=? AND event_type='REQUEST_REPORT_JOURNAL'",Integer.class,owner.getId()));verify(reports).read(owner.getEmail(),Instant.parse(FROM),Instant.parse(TO));
        assertEquals(before,financialRows());
    }
    @Test void uninitializedAccountStaysUninitializedAndAmountsUnknown()throws Exception{
        mvc.perform(request()).andExpect(status().isOk()).andExpect(jsonPath("$.directChanges[0].change").isEmpty()).andExpect(jsonPath("$.coverage.initialized").value(false));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM settlement_accounts WHERE user_id=?",Integer.class,owner.getId()));
    }
    @Test void limiterRejectsBeforeFinancialReadAndProvidesRetryHeader()throws Exception{
        for(int i=0;i<10;i++)mvc.perform(request()).andExpect(status().isOk());clearInvocations(reports);
        mvc.perform(request()).andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After")).andExpect(header().string("Cache-Control","no-store"));verifyNoInteractions(reports);
    }
    @Test void inactiveAccountCannotRead()throws Exception{
        owner.setAccountStatus("SUSPENDED");users.saveAndFlush(owner);mvc.perform(request()).andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control","no-store"));verifyNoInteractions(reports);
    }
    @Test void lockedSessionPreservesExisting423AndNoStore()throws Exception{
        String id=db.queryForObject("SELECT id FROM auth_sessions WHERE user_id=?",String.class,owner.getId());db.update("INSERT INTO trusted_device_locks(session_id,locked_at) VALUES(?,?)",id,Timestamp.from(Instant.now()));
        mvc.perform(request()).andExpect(status().isLocked()).andExpect(header().string("Cache-Control","no-store"));verifyNoInteractions(reports);
    }
    @Test void sourceLimitAppliesAcrossIdentitiesBeforeFinancialRead()throws Exception{
        for(int i=0;i<30;i++)events.record("REQUEST_REPORT_JOURNAL",other.getId(),other.getEmail(),new MockHttpServletRequest(),"");
        mvc.perform(request()).andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));verifyNoInteractions(reports);
    }
    java.util.Map<String,Object> financialRows(){var result=new java.util.TreeMap<String,Object>();for(String table:new String[]{"holdings","orders","paper_tickets","paper_fills","settlement_allocations","settlement_accounts","settlement_journal"})result.put(table,db.queryForList("SELECT * FROM "+table+" WHERE user_id=? ORDER BY 1,2",owner.getId()));return result;}
    @Test void unexpectedServiceErrorIsSanitizedAndNotCached()throws Exception{
        doThrow(new IllegalStateException("private-source-error")).when(reports).read(anyString(),any(),any());try{
            var body=mvc.perform(request()).andExpect(status().isInternalServerError()).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse().getContentAsString();assertFalse(body.contains("private-source-error"));
        }finally{doCallRealMethod().when(reports).read(anyString(),any(),any());}
    }
}
