package com.example.investsmart.settlement;

import com.example.investsmart.reports.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

class JournalPeriodReportMapperTests {
    JournalReportFixture f;final JournalPeriodReportMapper mapper=new JournalPeriodReportMapper();
    @BeforeEach void setup(){f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:external-report-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""),true);}
    @AfterEach void close(){f.close();}
    JournalPeriodReport read(){return mapper.map(f.context.getBean(JournalPeriodService.class).read(f.owner.getEmail(),JournalReportFixture.AT,JournalReportFixture.AT.plusSeconds(1)));}
    @Test void emptyUninitializedIsUnavailableWhileInitializedZeroRemainsVerified(){
        assertNull(read().directChanges().getFirst().change());f.account(1);var result=read();
        assertEquals("0.00",result.directChanges().getFirst().change());assertEquals("0.00",result.groupedChanges().getFirst().change());assertFalse(result.coverage().completeFinancialHistory());assertEquals(3,result.unavailableScopes().size());
        assertThrows(UnsupportedOperationException.class,()->result.directChanges().clear());
    }
    @Test void exactDecimalStringsAndProjectionNeverExposeInternalIdentity(){
        f.account(1);f.entry(1,"private-source-key","SETTLED_CREDIT","9007199254740993.25",JournalReportFixture.AT);var before=ReportingIndexTestSupport.rows(f.db);var result=read();
        assertEquals("9007199254740993.25",result.rows().getFirst().amount());assertEquals("9007199254740993.25",result.directChanges().getFirst().change());assertEquals(1,result.rows().getFirst().ordinal());assertFalse(result.toString().contains("private-source-key"));assertFalse(result.toString().contains("fingerprint"));assertEquals(before,ReportingIndexTestSupport.rows(f.db));
    }
    @Test void unsupportedAndInvalidRowsRemainVisibleWithoutEchoingArbitraryAction(){
        f.account(1);f.entry(1,"a","private-action-text","5",JournalReportFixture.AT);f.entry(1,"b","SETTLED_CREDIT","-2",JournalReportFixture.AT);var result=read();
        assertEquals("UNKNOWN",result.rows().get(0).action());assertEquals("UNSUPPORTED",result.rows().get(0).interpretationStatus());assertNull(result.rows().get(1).amount());assertFalse(result.toString().contains("private-action-text"));assertNull(result.directChanges().getFirst().change());
    }
    @Test void selectionSentinelNeverTurnsPartialEvidenceIntoTotals(){
        f.account(1);f.write.executeWithoutResult(ignored->{for(int i=0;i<1001;i++)f.entry(1,"row-"+i,"SETTLED_CREDIT","1",JournalReportFixture.AT);});var result=read();
        assertEquals(1000,result.rows().size());assertTrue(result.limits().rowsTruncated());assertTrue(result.directChanges().stream().allMatch(b->b.change()==null));assertTrue(result.groupedChanges().stream().allMatch(b->b.change()==null));
    }
    @Test void completeChargeAlternativeIsNeverAddedToDirectScope(){
        var fixture=new PeriodChargeAdapterTests();fixture.setup();try{
            var before=fixture.writer.balances();var result=mapper.map(fixture.read());assertNull(result.directChanges().getFirst().change());assertEquals("-20.00",result.groupedChanges().getFirst().change());assertEquals(before,fixture.writer.balances());
        }finally{fixture.close();}
    }
    @Test void unknownSummaryReasonsAreRedactedAndCannotAuthorizeAnAmount(){
        f.account(1);var period=f.context.getBean(JournalPeriodService.class).read(f.owner.getEmail(),JournalReportFixture.AT,JournalReportFixture.AT.plusSeconds(1));
        var unsafe=new com.example.investsmart.dto.SelectedJournalChangeSummary(period.fromInclusive(),period.toExclusive(),List.of(new com.example.investsmart.dto.SelectedJournalChangeSummary.Bucket(com.example.investsmart.dto.PeriodSummaryEligibility.Scope.SELECTED_JOURNAL_SETTLED_CHANGE,com.example.investsmart.dto.SelectedJournalChangeSummary.Status.VERIFIED_SUPPLIED_CHANGE,new java.math.BigDecimal("0.00"),List.of("private-new-reason"))));
        var result=mapper.map(period.withSelectedDirectChanges(unsafe));assertNull(result.directChanges().getFirst().change());assertFalse(result.toString().contains("private-new-reason"));
    }
}
