package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import com.example.investsmart.exception.ResourceNotFoundException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.example.investsmart.dto.PeriodSummaryEligibility.*;
class PeriodSummaryIntegrationTests {
    JournalReportFixture f;
    @BeforeEach void setup(){f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:summary-integration-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);}
    @AfterEach void close(){f.close();}
    JournalPeriodEvidence read(String email){return f.context.getBean(JournalPeriodService.class).read(email,JournalReportFixture.AT,JournalReportFixture.AT.plusSeconds(60));}
    Bucket bucket(JournalPeriodEvidence p,Scope scope){return p.summaryEligibility().buckets().stream().filter(b->b.scope()==scope).findFirst().orElseThrow();}
    @Test void attachesOwnedScopesWithoutPromotingEmptyOrForeignHistory(){
        f.account(1);f.entry(1,"owned","WITHDRAW","1",JournalReportFixture.AT);f.entry(2,"foreign","UNKNOWN","1",JournalReportFixture.AT);
        var owned=read(f.owner.getEmail());assertEquals(Status.ELIGIBLE_SUPPLIED_DIRECT_ROWS,bucket(owned,Scope.SELECTED_JOURNAL_SETTLED_CHANGE).status());assertFalse(owned.toString().contains("foreign"));
        var other=read(f.other.getEmail());assertEquals(Status.UNAVAILABLE,bucket(other,Scope.SELECTED_JOURNAL_SETTLED_CHANGE).status());assertEquals(1,other.entries().size());
        assertThrows(ResourceNotFoundException.class,()->read("missing"));f.db.update("DELETE FROM settlement_journal WHERE user_id=1");
        var empty=read(f.owner.getEmail());assertEquals(Status.ELIGIBLE_SUPPLIED_DIRECT_ROWS,bucket(empty,Scope.SELECTED_JOURNAL_RESERVED_CHANGE).status());assertEquals(Status.UNAVAILABLE,bucket(empty,Scope.HISTORICAL_BALANCES).status());assertFalse(empty.completeFinancialHistory());
    }
    @Test void eligibilityRechecksOnSameIndependentSnapshotWithFixedQueriesAndNoWrites(){
        f.account(1);f.entry(1,"owned","WITHDRAW","1",JournalReportFixture.AT);var source=f.context.getBean(javax.sql.DataSource.class);Object[] resource={null};
        doAnswer(call->{resource[0]=TransactionSynchronizationManager.getResource(source);return call.callRealMethod();}).when(f.periodRepository).entries(eq(1L),any(),any());
        doAnswer(call->{assertSame(resource[0],TransactionSynchronizationManager.getResource(source));assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());assertEquals(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());var supplied=(JournalPeriodEvidence)call.getArgument(0);assertNotNull(supplied.assessment());assertNull(supplied.selectedGroupedChanges());return call.callRealMethod();}).when(f.eligibilityAssessor).assess(any());
        var before=f.db.queryForList("SELECT * FROM users ORDER BY id");f.write.execute(status->{var outer=TransactionSynchronizationManager.getResource(source);read(f.owner.getEmail());assertNotSame(outer,resource[0]);assertSame(outer,TransactionSynchronizationManager.getResource(source));return null;});
        verify(f.eligibilityAssessor,times(3)).assess(any());verify(f.repository,times(1)).coverage(1);verify(f.periodRepository,times(1)).entries(eq(1L),any(),any());verifyNoInteractions(f.fundingRepository,f.periodChargeRepository,f.chargeRepository);assertEquals(before,f.db.queryForList("SELECT * FROM users ORDER BY id"));
    }
    @Test void missingAndInvalidRowsRemainVisibleWithUnavailableScopes(){
        f.account(1);f.entry(1,"invalid","WITHDRAW","-1",JournalReportFixture.AT);f.entry(1,"missing","RESERVE","1",JournalReportFixture.AT);f.db.update("UPDATE settlement_journal SET reference_kind='ORDER',reference_id='order' WHERE event_key='missing'");
        var result=read(f.owner.getEmail());assertEquals(2,result.entries().size());assertTrue(bucket(result,Scope.SELECTED_JOURNAL_SETTLED_CHANGE).reasons().containsAll(List.of(Reason.INVALID_ACTION,Reason.MISSING_FUNDING)));assertEquals(Status.UNAVAILABLE,bucket(result,Scope.SELECTED_JOURNAL_RESERVED_CHANGE).status());
        assertNull(result.withAssessment(result.assessment()).summaryEligibility(),"Replacing cached assessment must invalidate derived eligibility");
    }
    @Test void truncationPreservesFirstThousandRowsAndWithholdsDirectScopes(){
        f.account(1);f.db.batchUpdate("INSERT INTO settlement_journal VALUES(1,?,'WITHDRAW',?,1,NULL,NULL,?)",new org.springframework.jdbc.core.BatchPreparedStatementSetter(){public int getBatchSize(){return 1001;}public void setValues(java.sql.PreparedStatement p,int i)throws java.sql.SQLException{p.setString(1,"r"+i);p.setString(2,"a".repeat(64));p.setTimestamp(3,java.sql.Timestamp.from(JournalReportFixture.AT));}});
        var result=read(f.owner.getEmail());assertEquals(1000,result.entries().size());assertTrue(result.truncated());assertEquals(Status.UNAVAILABLE,bucket(result,Scope.SELECTED_JOURNAL_SETTLED_CHANGE).status());assertTrue(bucket(result,Scope.SELECTED_JOURNAL_RESERVED_CHANGE).reasons().contains(Reason.JOURNAL_LIMIT));
    }
}
