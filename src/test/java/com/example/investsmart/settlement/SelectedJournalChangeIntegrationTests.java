package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.example.investsmart.dto.PeriodSummaryEligibility.Scope.*;
class SelectedJournalChangeIntegrationTests {
    JournalReportFixture f;
    @BeforeEach void setup(){f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:owned-changes-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);}
    @AfterEach void close(){f.close();}
    JournalPeriodEvidence read(String email){return f.context.getBean(JournalPeriodService.class).read(email,JournalReportFixture.AT,JournalReportFixture.AT.plusSeconds(60));}
    BigDecimal value(JournalPeriodEvidence p,PeriodSummaryEligibility.Scope scope){return p.selectedDirectChanges().buckets().stream().filter(b->b.scope()==scope).findFirst().orElseThrow().change();}
    @Test void ownedSignedChangesExcludeForeignRowsAndDoNotReturnBalances(){
        f.account(1);f.entry(1,"credit","SETTLED_CREDIT","10",JournalReportFixture.AT);f.entry(1,"withdraw","WITHDRAW","12",JournalReportFixture.AT);f.entry(2,"private","SETTLED_CREDIT","999",JournalReportFixture.AT);
        var before=f.db.queryForList("SELECT * FROM users ORDER BY id");var result=read(f.owner.getEmail());assertEquals(new BigDecimal("-2.00"),value(result,SELECTED_JOURNAL_SETTLED_CHANGE));assertEquals(new BigDecimal("0.00"),value(result,SELECTED_JOURNAL_RESERVED_CHANGE));assertFalse(result.toString().contains("private"));assertNull(value(result,HISTORICAL_BALANCES));assertNull(value(read(f.other.getEmail()),SELECTED_JOURNAL_SETTLED_CHANGE));assertEquals(before,f.db.queryForList("SELECT * FROM users ORDER BY id"));
    }
    @Test void initializedEmptyZeroDiffersFromUninitializedOrInvalidNullAndCachesInvalidate(){
        assertNull(value(read(f.owner.getEmail()),SELECTED_JOURNAL_SETTLED_CHANGE));f.account(1);var empty=read(f.owner.getEmail());assertEquals(new BigDecimal("0.00"),value(empty,SELECTED_JOURNAL_SETTLED_CHANGE));
        assertNull(empty.withAssessment(empty.assessment()).selectedDirectChanges());assertNull(empty.withSummaryEligibility(empty.summaryEligibility()).selectedDirectChanges());
        f.entry(1,"bad","WITHDRAW","-1",JournalReportFixture.AT);var invalid=read(f.owner.getEmail());assertEquals(1,invalid.entries().size());assertNull(value(invalid,SELECTED_JOURNAL_SETTLED_CHANGE));
    }
    @Test void calculationSharesIndependentSnapshotWithFixedQueriesAndRetainedEvidence(){
        f.account(1);f.entry(1,"credit","SETTLED_CREDIT","10",JournalReportFixture.AT);var source=f.context.getBean(javax.sql.DataSource.class);Object[] resource={null};
        doAnswer(call->{resource[0]=TransactionSynchronizationManager.getResource(source);return call.callRealMethod();}).when(f.periodRepository).entries(eq(1L),any(),any());
        doAnswer(call->{assertSame(resource[0],TransactionSynchronizationManager.getResource(source));assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());assertEquals(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());var p=(JournalPeriodEvidence)call.getArgument(0);assertNotNull(p.summaryEligibility());assertNull(p.selectedDirectChanges());return call.callRealMethod();}).when(f.changeSummarizer).summarize(any());
        f.write.execute(status->{var outer=TransactionSynchronizationManager.getResource(source);var p=read(f.owner.getEmail());assertNotSame(outer,resource[0]);assertSame(outer,TransactionSynchronizationManager.getResource(source));assertEquals(1,p.entries().size());assertNotNull(p.assessment());return null;});
        verify(f.changeSummarizer,times(1)).summarize(any());verify(f.repository,times(1)).coverage(1);verify(f.periodRepository,times(1)).entries(eq(1L),any(),any());verifyNoInteractions(f.fundingRepository,f.periodChargeRepository,f.chargeRepository);
    }
    @Test void actualCreditRefundReturnsNullSettledAndVerifiedZeroReserved(){
        var writer=new SettlementFundsTests();writer.setup();writer.cash("deposit","20");var id=writer.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);writer.reserve("reserve","30");writer.time(SettlementFundsTests.CUTOFF);writer.get(()->writer.funds.settle(1,id));writer.release("refund","30");var before=writer.balances();
        try(var report=new JournalReportFixture(writer.fixture.source,false)){
            var p=report.context.getBean(JournalPeriodService.class).read(report.owner.getEmail(),SettlementFundsTests.BEFORE,SettlementFundsTests.CUTOFF.plusSeconds(1));assertNull(value(p,SELECTED_JOURNAL_SETTLED_CHANGE));assertEquals(new BigDecimal("0.00"),value(p,SELECTED_JOURNAL_RESERVED_CHANGE));assertEquals(before,writer.balances());
        }
    }
    @Test void thousandRowTruncationPreservesRawEvidenceWithoutPartialSum(){
        f.account(1);f.db.batchUpdate("INSERT INTO settlement_journal VALUES(1,?,'WITHDRAW',?,1,NULL,NULL,?)",new org.springframework.jdbc.core.BatchPreparedStatementSetter(){public int getBatchSize(){return 1001;}public void setValues(java.sql.PreparedStatement p,int i)throws java.sql.SQLException{p.setString(1,"r"+i);p.setString(2,"a".repeat(64));p.setTimestamp(3,java.sql.Timestamp.from(JournalReportFixture.AT));}});
        var p=read(f.owner.getEmail());assertTrue(p.truncated());assertEquals(1000,p.entries().size());assertNull(value(p,SELECTED_JOURNAL_SETTLED_CHANGE));assertNull(value(p,SELECTED_JOURNAL_RESERVED_CHANGE));
    }
}
