package com.example.investsmart.settlement;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static com.example.investsmart.dto.SettlementFundingPage.Verification.*;

class SettlementFundingServiceTests {
    JournalReportFixture f;
    @BeforeEach void setup() {
        f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:funding-report-"+UUID.randomUUID()
                +";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);
    }
    @AfterEach void close() { f.close(); }

    @Test void matchesSplitFundingWithoutConvertingEventsToCashMovements() {
        obligation("credit",1,"REUSABLE","PENDING");
        allocation("cash",1,"SETTLED"); allocation("pending",1,"credit");
        journal("reserve","RESERVE","100.00");
        evidence(1,"reserve","cash","RESERVE","30.25"); evidence(1,"reserve","pending","RESERVE","69.75");
        var result=read().entries().getFirst();
        assertEquals(MATCHED,result.verification()); assertTrue(result.issues().isEmpty());
        assertEquals(List.of("SETTLED_ORIGIN","CREDIT_ORIGIN"),result.sources().stream().map(s->s.sourceType()).toList());
        assertEquals(new BigDecimal("69.75"),result.sources().get(1).amount());
        assertEquals("PENDING",result.sources().get(1).currentStatus());
        f.db.update("UPDATE settlement_obligations SET status='SETTLED',remaining_credit=0 WHERE id='credit'");
        assertEquals("SETTLED",read().entries().getFirst().sources().get(1).currentStatus());
        assertThrows(UnsupportedOperationException.class,()->result.sources().clear());
    }

    @Test void missingEventsOrOwnedAllocationStayIncompleteAndForeignDataNeverJoins() {
        journal("reserve","RESERVE","1.00");
        assertEquals(INCOMPLETE,read().entries().getFirst().verification());
        allocation("foreign",2,"SETTLED"); evidence(1,"reserve","foreign","RESERVE","1.00");
        evidence(2,"reserve","foreign","RESERVE","999.00");
        var result=read().entries().getFirst();
        assertEquals(INCOMPLETE,result.verification()); assertTrue(result.sources().isEmpty());
        assertEquals(List.of("MISSING_OWNED_ALLOCATION"),result.issues());
    }

    @Test void missingOrForeignCreditCannotBeCertified() {
        journal("reserve","RESERVE","1.00"); allocation("a",1,"foreign-credit");
        obligation("foreign-credit",2,"REUSABLE","PENDING"); evidence(1,"reserve","a","RESERVE","1.00");
        var result=read().entries().getFirst();
        assertEquals(INCOMPLETE,result.verification()); assertNull(result.sources().getFirst().creditPolicy());
        assertNull(result.sources().getFirst().currentStatus());
        assertTrue(result.issues().contains("UNRESOLVED_OWNED_CREDIT"));
    }

    @Test void amountActionAndReferenceDisagreementsAreExplicit() {
        journal("reserve","RESERVE","2.00"); allocation("a",1,"OPENING_SETTLED");
        f.db.update("UPDATE settlement_allocations SET reference_id='different' WHERE id='a'");
        evidence(1,"reserve","a","SPEND","1.00");
        var result=read().entries().getFirst();
        assertEquals(MISMATCH,result.verification());
        assertTrue(result.issues().containsAll(List.of("ACTION_MISMATCH","REFERENCE_MISMATCH","AMOUNT_MISMATCH")));
    }

    @Test void restrictedCreditAndNegativeEvidenceAreNotMatched() {
        journal("reserve","RESERVE","1.00"); obligation("restricted",1,"RESTRICTED","SETTLED");
        allocation("a",1,"restricted"); evidence(1,"reserve","a","RESERVE","-1.00");
        var result=read().entries().getFirst();
        assertEquals(MISMATCH,result.verification());
        assertTrue(result.issues().containsAll(List.of("INVALID_ALLOCATION_AMOUNT","INELIGIBLE_RESTRICTED_SOURCE","AMOUNT_MISMATCH")));
    }

    @Test void unknownMetadataAndChargeParentsDoNotPretendToBeCashReconciliation() {
        f.entry(1,"metadata","LINK_DELIVERIES","0.00",JournalReportFixture.AT);
        f.entry(1,"unknown","FUTURE_ACTION","99.99",JournalReportFixture.AT);
        f.entry(1,"charge","REALIZED_CHARGE","10.00",JournalReportFixture.AT);
        assertEquals(3,read().entries().size());
        assertTrue(read().entries().stream().allMatch(e->e.verification()==NOT_EVALUATED&&e.sources().isEmpty()));
    }

    @Test void evidenceBoundNeverPresentsTruncatedProofAsComplete() {
        journal("big","RESERVE","1001.00");
        for(int i=0;i<1001;i++){String id="a"+i,source="credit"+i;obligation(source,1,"REUSABLE","PENDING");allocation(id,1,source);evidence(1,"big",id,"RESERVE","1.00");}
        var result=read().entries().getFirst();
        assertEquals(INCOMPLETE,result.verification()); assertEquals(List.of("EVIDENCE_LIMIT"),result.issues());
        assertTrue(result.sources().isEmpty());
        f.db.update("DELETE FROM settlement_allocation_events WHERE user_id=1 AND allocation_id='a1000'");
        f.db.update("UPDATE settlement_journal SET amount=1000 WHERE user_id=1 AND event_key='big'");
        var exact=read().entries().getFirst();
        assertEquals(MATCHED,exact.verification()); assertEquals(1000,exact.sources().size());
    }

    @Test void validatesIdentityBoundsAndKeepsReadOnlySnapshotWithoutInitialization() {
        var original=f.db.queryForList("SELECT * FROM users ORDER BY id");
        var empty=read();assertFalse(empty.coverage().initialized());assertTrue(empty.entries().isEmpty());
        assertEquals(original,f.db.queryForList("SELECT * FROM users ORDER BY id"));
        assertEquals(0,f.db.queryForObject("SELECT COUNT(*) FROM settlement_accounts",Integer.class));
        doAnswer(call->{assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(java.sql.Connection.TRANSACTION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());return call.callRealMethod();
        }).when(f.repository).coverage(anyLong());
        read();
        assertThrows(com.example.investsmart.exception.BadRequestException.class,()->f.fundingService.read(f.owner.getEmail(),-1,50));
        assertThrows(com.example.investsmart.exception.BadRequestException.class,()->f.fundingService.read(f.owner.getEmail(),0,101));
        assertThrows(com.example.investsmart.exception.ResourceNotFoundException.class,()->f.fundingService.read("missing@example.invalid",0,50));
        f.owner.setAccountStatus("SUSPENDED");
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->read());
    }

    @Test void realWritersPartialSpendRefundSettlementRetryAndDebtReconcileWithoutWrites() {
        var writer=new SettlementFundsTests();writer.setup();
        try(var report=new JournalReportFixture(writer.fixture.source,false)) {
            writer.cash("deposit","50");String credit=writer.pending("sale","100",SettlementFunds.CreditPolicy.REUSABLE);
            writer.reserve("reserve","100");writer.spend("fill","50");writer.spend("fill","50");
            writer.release("refund-before","20");writer.time(SettlementFundsTests.CUTOFF);
            assertTrue(writer.get(()->writer.funds.settle(1,credit)));writer.release("refund-after","30");
            writer.run(()->writer.funds.chargeDebt(1,"fee-or-loss",writer.n("180")));
            assertEquals(writer.n("-80"),writer.balances().settled());
            var before=new LinkedHashMap<String,List<Map<String,Object>>>();
            for(String table:List.of("users","holdings","settlement_accounts","settlement_journal","settlement_obligations","settlement_allocations","settlement_allocation_events"))before.put(table,report.db.queryForList("SELECT * FROM "+table));
            var result=report.fundingService.read(report.owner.getEmail(),0,50);
            assertEquals(6,result.entries().stream().filter(e->e.verification()==MATCHED).count());
            assertTrue(result.entries().stream().noneMatch(e->e.verification()==MISMATCH||e.verification()==INCOMPLETE));
            for(var entry:before.entrySet())assertEquals(entry.getValue(),report.db.queryForList("SELECT * FROM "+entry.getKey()));
        }
    }

    private com.example.investsmart.dto.SettlementFundingPage read(){return f.fundingService.read(f.owner.getEmail(),null,null);}
    private void journal(String key,String action,String amount){f.entry(1,key,action,amount,JournalReportFixture.AT);f.db.update("UPDATE settlement_journal SET reference_kind='ORDER',reference_id='order' WHERE user_id=1 AND event_key=?",key);}
    private void allocation(String id,long user,String source){f.db.update("INSERT INTO settlement_allocations VALUES(?,?, 'ORDER','order',?,100,100,'RESERVED',?)",id,user,source,Timestamp.from(JournalReportFixture.AT));}
    private void evidence(long user,String key,String allocation,String action,String amount){f.db.update("INSERT INTO settlement_allocation_events VALUES(?,?,?,?,?)",user,key,allocation,action,new BigDecimal(amount));}
    private void obligation(String id,long user,String policy,String status){f.db.update("INSERT INTO settlement_obligations(id,user_id,source_key,exchange_code,segment_code,trade_date,obligation_kind,credit_policy,amount,remaining_credit,status,created_at) VALUES(?,?,?,'NSE','EQUITY','2026-11-23','CASH_CREDIT',?,100,100,?,?)",id,user,id,policy,status,Timestamp.from(JournalReportFixture.AT));}
}
