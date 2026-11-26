package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import com.example.investsmart.exception.ResourceNotFoundException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PeriodCoverageIntegrationTests {
    JournalReportFixture f;JournalPeriodService service;
    @BeforeEach void setup(){f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:coverage-integration-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);service=f.context.getBean(JournalPeriodService.class);}
    @AfterEach void close(){f.close();}
    JournalPeriodEvidence read(String email){return service.read(email,JournalReportFixture.AT,JournalReportFixture.AT.plusSeconds(60));}
    @Test void attachesOnlyOwnedDiagnosticsAndNeverCertifiesEmptyHistory(){
        f.entry(1,"owned","UNKNOWN","1",JournalReportFixture.AT);f.entry(2,"other","WITHDRAW","1",JournalReportFixture.AT);
        var owned=read(f.owner.getEmail());assertNotNull(owned.assessment());assertEquals(1,owned.assessment().selectedRows());assertEquals(1,owned.assessment().unsupportedRows());
        var other=read(f.other.getEmail());assertEquals(1,other.assessment().selectedRows());assertEquals(0,other.assessment().unsupportedRows());
        assertThrows(ResourceNotFoundException.class,()->read("missing@example.invalid"));
        f.db.update("DELETE FROM settlement_journal");var empty=read(f.owner.getEmail());assertEquals(0,empty.assessment().selectedRows());assertFalse(empty.assessment().fullAccountSummaryAvailable());
        assertEquals(0,f.db.queryForObject("SELECT COUNT(*) FROM settlement_accounts",Integer.class));
    }
    @Test void coverageIsRecheckedInSameReadOnlySnapshotWithoutAdditionalQueriesOrWrites(){
        f.entry(1,"owned","WITHDRAW","1",JournalReportFixture.AT);var source=f.context.getBean(javax.sql.DataSource.class);Object[] connection={null};
        doAnswer(call->{connection[0]=TransactionSynchronizationManager.getResource(source);return call.callRealMethod();}).when(f.periodRepository).entries(anyLong(),any(),any());
        doAnswer(call->{assertSame(connection[0],TransactionSynchronizationManager.getResource(source));assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());return call.callRealMethod();}).when(f.coverageAssessor).assess(any());
        var before=f.db.queryForList("SELECT * FROM users ORDER BY id");var result=read(f.owner.getEmail());
        assertEquals(PeriodCoverageAssessment.Status.ASSESSED,result.assessment().status());verify(f.coverageAssessor,times(4)).assess(any());
        verify(f.repository,times(1)).coverage(1);verify(f.periodRepository,times(1)).entries(eq(1L),any(),any());
        verifyNoInteractions(f.fundingRepository,f.periodChargeRepository,f.chargeRepository);
        assertEquals(before,f.db.queryForList("SELECT * FROM users ORDER BY id"));
    }
    @Test void malformedAndMissingSourceEvidenceBecomesDiagnosticReasons(){
        f.entry(1,"invalid","WITHDRAW","-1",JournalReportFixture.AT);f.entry(1,"missing","RESERVE","1",JournalReportFixture.AT);
        f.db.update("UPDATE settlement_journal SET reference_kind='ORDER',reference_id='order' WHERE event_key='missing'");
        var result=read(f.owner.getEmail());assertEquals(1,result.assessment().invalidRows());assertEquals(1,result.assessment().missingFundingRows());
        assertTrue(result.assessment().reasons().containsAll(List.of(PeriodCoverageAssessment.Reason.INVALID_ACTION,PeriodCoverageAssessment.Reason.MISSING_FUNDING)));
        assertEquals(2,result.entries().size());
    }
    @Test void rowTruncationRetainsRowsButCannotClaimSummaryCompleteness(){
        f.db.batchUpdate("INSERT INTO settlement_journal VALUES(1,?,'WITHDRAW',?,1,NULL,NULL,?)",new org.springframework.jdbc.core.BatchPreparedStatementSetter(){public int getBatchSize(){return 1001;}public void setValues(java.sql.PreparedStatement p,int i)throws java.sql.SQLException{p.setString(1,"r"+i);p.setString(2,"a".repeat(64));p.setTimestamp(3,java.sql.Timestamp.from(JournalReportFixture.AT));}});
        var result=read(f.owner.getEmail());assertTrue(result.truncated());assertEquals(1000,result.entries().size());assertEquals(1000,result.assessment().selectedRows());assertNull(result.assessment().groupedCharges());
        assertTrue(result.assessment().reasons().containsAll(List.of(PeriodCoverageAssessment.Reason.JOURNAL_TRUNCATED,PeriodCoverageAssessment.Reason.GROUPING_UNAVAILABLE)));assertFalse(result.assessment().fullAccountSummaryAvailable());
    }
}
