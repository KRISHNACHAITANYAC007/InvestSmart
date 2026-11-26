package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.investsmart.dto.PeriodCoverageAssessment.*;

class PeriodCoverageAssessorTests {
    JournalReportFixture f;PeriodCoverageAssessor assessor;
    @BeforeEach void setup(){f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:coverage-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);var actions=new JournalActionClassifier();assessor=new PeriodCoverageAssessor(actions,new FundingEffectClassifier(actions));}
    @AfterEach void close(){f.close();}
    JournalPeriodEvidence read(){return f.context.getBean(JournalPeriodService.class).read(f.owner.getEmail(),JournalReportFixture.AT,JournalReportFixture.AT.plusSeconds(60));}
    JournalPeriodEvidence copy(JournalPeriodEvidence p,List<JournalPeriodEvidence.Entry> rows,GroupedChargeEvidence groups,boolean truncated,boolean funding){return new JournalPeriodEvidence(p.fromInclusive(),p.toExclusive(),p.initialization(),p.startsBeforeInitialization(),p.completeFinancialHistory(),truncated,funding,rows,p.limitations(),groups);}
    @Test void emptyInitializedAndUninitializedPeriodsNeverGrantFullSummary(){
        var uninitialized=assessor.assess(read());assertEquals(Status.ASSESSED,uninitialized.status());assertEquals(0,uninitialized.selectedRows());assertTrue(uninitialized.reasons().contains(Reason.ACCOUNT_NOT_INITIALIZED));assertFalse(uninitialized.fullAccountSummaryAvailable());
        f.account(1);var initialized=assessor.assess(read());assertFalse(initialized.reasons().contains(Reason.ACCOUNT_NOT_INITIALIZED));assertEquals(List.of(Reason.HISTORY_NOT_CERTIFIED),initialized.reasons());assertFalse(initialized.fullAccountSummaryAvailable());
        assertThrows(UnsupportedOperationException.class,()->initialized.reasons().clear());
    }
    @Test void countsDistinctProblemRowsAndDeduplicatesReasons(){
        f.entry(1,"unknown1","UNKNOWN","1",JournalReportFixture.AT);f.entry(1,"unknown2","DISPOSE","2",JournalReportFixture.AT);f.entry(1,"invalid","WITHDRAW","-1",JournalReportFixture.AT);
        f.entry(1,"missing","RESERVE","1",JournalReportFixture.AT);f.entry(1,"conflict","RESERVE","1",JournalReportFixture.AT);
        f.db.update("UPDATE settlement_journal SET reference_kind='ORDER',reference_id='order' WHERE action='RESERVE'");
        f.db.update("INSERT INTO settlement_allocations VALUES('a',1,'ORDER','order','SETTLED',2,2,'RESERVED',?)",java.sql.Timestamp.from(JournalReportFixture.AT));
        f.db.update("INSERT INTO settlement_allocation_events VALUES(1,'conflict','a','RESERVE',2)");
        var result=assessor.assess(read());assertEquals(Status.ASSESSED,result.status());assertEquals(5,result.selectedRows());assertEquals(2,result.unsupportedRows());assertEquals(1,result.invalidRows());assertEquals(1,result.missingFundingRows());assertEquals(1,result.conflictingFundingRows());
        assertEquals(1,result.reasons().stream().filter(r->r==Reason.UNSUPPORTED_ACTION).count());
    }
    @Test void flagsTruncationAndUnknownGroupingWithoutInventingGroupCount(){
        f.entry(1,"row","WITHDRAW","1",JournalReportFixture.AT);var p=read();
        var result=assessor.assess(copy(p,p.entries(),null,true,true));assertEquals(Status.ASSESSED,result.status());assertNull(result.groupedCharges());assertTrue(result.reasons().containsAll(List.of(Reason.JOURNAL_TRUNCATED,Reason.FUNDING_TRUNCATED,Reason.GROUPING_UNAVAILABLE)));
        assertEquals(1,result.selectedRows());assertFalse(result.fullAccountSummaryAvailable());
    }
    @Test void staleCachesAndIncompletePartitionsWithholdCounts(){
        f.entry(1,"row","WITHDRAW","1",JournalReportFixture.AT);var p=read();var e=p.entries().getFirst();
        var stale=new JournalPeriodEvidence.Entry(e.raw(),null,e.funding(),e.fundingEffect());
        assertEquals(Status.CONTRADICTORY_EVIDENCE,assessor.assess(copy(p,List.of(stale),p.chargeGrouping(),false,false)).status());
        var missing=new GroupedChargeEvidence(GroupedChargeEvidence.Status.GROUPED_SUPPLIED_ROWS,List.of(),List.of());
        var result=assessor.assess(copy(p,p.entries(),missing,false,false));assertEquals(Status.CONTRADICTORY_EVIDENCE,result.status());assertNull(result.selectedRows());
        assertEquals(Status.CONTRADICTORY_EVIDENCE,assessor.assess(copy(p,List.of(e,e),p.chargeGrouping(),false,false)).status());
    }
    @Test void boundsAndInvalidPeriodAreUnavailableAndFalseHistoryIsNotPromoted(){
        assertEquals(Status.INPUT_UNAVAILABLE,assessor.assess(null).status());
        f.entry(1,"row","WITHDRAW","1",JournalReportFixture.AT);var p=read();
        var over=assessor.assess(copy(p,Collections.nCopies(1001,p.entries().getFirst()),p.chargeGrouping(),false,false));assertEquals(Status.INPUT_UNAVAILABLE,over.status());assertNull(over.selectedRows());
        var wrongRange=new JournalPeriodEvidence(p.toExclusive(),p.fromInclusive(),p.initialization(),false,false,false,false,p.entries(),List.of(),p.chargeGrouping());assertEquals(Status.INPUT_UNAVAILABLE,assessor.assess(wrongRange).status());
        var falseHistory=new JournalPeriodEvidence(p.fromInclusive(),p.toExclusive(),p.initialization(),false,true,false,false,p.entries(),List.of(),p.chargeGrouping());assertEquals(Status.CONTRADICTORY_EVIDENCE,assessor.assess(falseHistory).status());
    }
    @Test void preInitializationRangeAndUnresolvedChargeHaveExplicitReasons(){
        f.account(1);f.entry(1,"charge","REALIZED_CHARGE","70",JournalReportFixture.AT);
        var p=f.context.getBean(JournalPeriodService.class).read(f.owner.getEmail(),JournalReportFixture.AT.minusSeconds(1),JournalReportFixture.AT.plusSeconds(1));
        var result=assessor.assess(p);assertEquals(Status.ASSESSED,result.status());assertEquals(1,result.ungroupedChargeRows());assertTrue(result.reasons().containsAll(List.of(Reason.RANGE_BEFORE_INITIALIZATION,Reason.UNGROUPED_CHARGE,Reason.GROUPING_INCOMPLETE)));
    }
    @Test void consistentCachesCannotHideWrongChargeChildRole(){
        var fixture=new ChargeEvidenceReconcilerTests();var actions=new JournalActionClassifier();var effects=new FundingEffectClassifier(actions);
        var entries=new ArrayList<JournalPeriodEvidence.Entry>();var raws=new ArrayList<SettlementJournalPage.Entry>();raws.add(fixture.parent());
        for(var child:fixture.pair()){
            var r=child.journal();raws.add(new SettlementJournalPage.Entry(r.eventKey(),r.action().equals("RESERVE")?"SPEND":"RESERVE",r.amount(),r.referenceKind(),r.referenceId(),r.occurredAt()));
        }
        for(var raw:raws){var sources=raw.action().equals("REALIZED_CHARGE")?List.<SettlementFundingPage.Source>of():fixture.pair().getFirst().sources();
            var event=new SettlementFundingPage.Event(raw,sources.isEmpty()?SettlementFundingPage.Verification.NOT_EVALUATED:SettlementFundingPage.Verification.MATCHED,sources,List.of());
            entries.add(new JournalPeriodEvidence.Entry(raw,actions.classify(raw.action(),raw.amount(),raw.referenceKind(),raw.referenceId()),event,effects.classify(event)));
        }
        var grouping=new GroupedChargeEvidence(GroupedChargeEvidence.Status.GROUPED_SUPPLIED_ROWS,List.of(new GroupedChargeEvidence.Group(GroupedChargeEvidence.Kind.VERIFIED_CHARGE,"charge",raws,List.of())),List.of());
        var input=copy(read(),entries,grouping,false,false);
        assertEquals(Status.CONTRADICTORY_EVIDENCE,assessor.assess(input).status());
    }
    @Test void actualWriterRefundAndVerifiedChargeRemainScopedDiagnostics(){
        var writer=new SettlementFundsTests();writer.setup();writer.cash("deposit","20");var credit=writer.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);writer.reserve("reserve","30");writer.time(SettlementFundsTests.CUTOFF);writer.get(()->writer.funds.settle(1,credit));writer.release("refund","30");writer.run(()->writer.funds.chargeDebt(1,"charge",new BigDecimal("70.00")));
        try(var report=new JournalReportFixture(writer.fixture.source,false)){
            var before=writer.balances();var p=report.context.getBean(JournalPeriodService.class).read(report.owner.getEmail(),SettlementFundsTests.BEFORE.minusSeconds(1),SettlementFundsTests.CUTOFF.plusSeconds(1));
            var result=assessor.assess(p);assertEquals(Status.ASSESSED,result.status());assertEquals(1,result.unknownRefundRows());assertEquals(1,result.groupedCharges());assertTrue(result.reasons().contains(Reason.UNKNOWN_REFUND_DESTINATION));assertFalse(result.fullAccountSummaryAvailable());assertEquals(before,writer.balances());
        }
    }
}
