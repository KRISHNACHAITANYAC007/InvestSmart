package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.investsmart.dto.PeriodSummaryEligibility.*;
class PeriodSummaryEligibilityAssessorTests {
    JournalReportFixture f;PeriodSummaryEligibilityAssessor rules;
    @BeforeEach void setup(){f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:eligibility-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);rules=new PeriodSummaryEligibilityAssessor(f.coverageAssessor);}
    @AfterEach void close(){f.close();}
    JournalPeriodEvidence read(){return f.context.getBean(JournalPeriodService.class).read(f.owner.getEmail(),JournalReportFixture.AT,JournalReportFixture.AT.plusSeconds(60));}
    Bucket bucket(PeriodSummaryEligibility result,Scope scope){return result.buckets().stream().filter(b->b.scope()==scope).findFirst().orElseThrow();}
    void unavailable(PeriodSummaryEligibility result){assertEquals(Status.UNAVAILABLE,bucket(result,Scope.SELECTED_JOURNAL_SETTLED_CHANGE).status());assertEquals(Status.UNAVAILABLE,bucket(result,Scope.SELECTED_JOURNAL_RESERVED_CHANGE).status());}
    JournalPeriodEvidence copy(JournalPeriodEvidence p,List<JournalPeriodEvidence.Entry> rows,boolean truncated,boolean funding,GroupedChargeEvidence grouping){return new JournalPeriodEvidence(p.fromInclusive(),p.toExclusive(),p.initialization(),p.startsBeforeInitialization(),false,truncated,funding,rows,p.limitations(),grouping);}
    @Test void emptyInitializedScopeIsEligibleWithoutAnyAccountSummaryOrZeroAmountClaim(){
        unavailable(rules.assess(read()));f.account(1);var result=rules.assess(read());
        assertEquals(Status.ELIGIBLE_SUPPLIED_DIRECT_ROWS,bucket(result,Scope.SELECTED_JOURNAL_SETTLED_CHANGE).status());
        for(var scope:List.of(Scope.COMPLETE_ACCOUNT_EXPENSES,Scope.ACCOUNT_PROFIT_LOSS,Scope.HISTORICAL_BALANCES))assertEquals(Status.UNAVAILABLE,bucket(result,scope).status());
        assertThrows(UnsupportedOperationException.class,()->result.buckets().clear());assertThrows(UnsupportedOperationException.class,()->result.buckets().getFirst().reasons().clear());
    }
    @Test void knownDirectRowsAtEqualTimesAreEligibleWithoutCausalOrRunningBalanceClaims(){
        f.account(1);f.entry(1,"credit","SETTLED_CREDIT","10",JournalReportFixture.AT);f.entry(1,"withdraw","WITHDRAW","2",JournalReportFixture.AT);
        var before=f.db.queryForList("SELECT * FROM users ORDER BY id");var result=rules.assess(read());
        assertEquals(Status.ELIGIBLE_SUPPLIED_DIRECT_ROWS,bucket(result,Scope.SELECTED_JOURNAL_SETTLED_CHANGE).status());assertEquals(Status.ELIGIBLE_SUPPLIED_DIRECT_ROWS,bucket(result,Scope.SELECTED_JOURNAL_RESERVED_CHANGE).status());assertEquals(before,f.db.queryForList("SELECT * FROM users ORDER BY id"));
    }
    @Test void staleCachedAssessmentAndContradictoryRowCachesAreRejected(){
        f.account(1);var empty=read();f.entry(1,"credit","SETTLED_CREDIT","10",JournalReportFixture.AT);var period=read();
        var stale=rules.assess(period.withAssessment(empty.assessment()));unavailable(stale);assertTrue(bucket(stale,Scope.SELECTED_JOURNAL_SETTLED_CHANGE).reasons().contains(Reason.STALE_ASSESSMENT));
        var entry=period.entries().getFirst();var forged=new JournalPeriodEvidence.Entry(entry.raw(),new JournalActionClassifier().classify("WITHDRAW",entry.raw().amount(),null,null),entry.funding(),entry.fundingEffect());
        unavailable(rules.assess(copy(period,List.of(forged),false,false,period.chargeGrouping())));
    }
    @Test void selectionFundingAndGroupingGapsBlockBothDirectScopes(){
        f.account(1);f.entry(1,"credit","SETTLED_CREDIT","10",JournalReportFixture.AT);var p=read();
        unavailable(rules.assess(copy(p,p.entries(),true,false,p.chargeGrouping())));unavailable(rules.assess(copy(p,p.entries(),false,true,p.chargeGrouping())));unavailable(rules.assess(copy(p,p.entries(),false,false,null)));
        assertTrue(bucket(rules.assess(copy(p,p.entries(),true,false,p.chargeGrouping())),Scope.SELECTED_JOURNAL_RESERVED_CHANGE).reasons().contains(Reason.JOURNAL_LIMIT));
    }
    @Test void unsupportedInvalidAndMissingFundingRowsCannotBecomeZeroEffects(){
        f.account(1);f.entry(1,"unknown","FUTURE_LOT","1",JournalReportFixture.AT);f.entry(1,"bad","WITHDRAW","-1",JournalReportFixture.AT);f.entry(1,"missing","RESERVE","1",JournalReportFixture.AT);f.db.update("UPDATE settlement_journal SET reference_kind='ORDER',reference_id='order' WHERE event_key='missing'");
        var result=rules.assess(read());unavailable(result);assertTrue(bucket(result,Scope.SELECTED_JOURNAL_SETTLED_CHANGE).reasons().containsAll(List.of(Reason.UNSUPPORTED_ACTION,Reason.INVALID_ACTION,Reason.MISSING_FUNDING)));
    }
    @Test void actualCreditRefundBlocksSettledButRetainsKnownReservedScope(){
        var writer=new SettlementFundsTests();writer.setup();writer.cash("deposit","20");var id=writer.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);writer.reserve("reserve","30");writer.time(SettlementFundsTests.CUTOFF);writer.get(()->writer.funds.settle(1,id));writer.release("refund","30");var before=writer.balances();
        try(var report=new JournalReportFixture(writer.fixture.source,false)){
            var period=report.context.getBean(JournalPeriodService.class).read(report.owner.getEmail(),SettlementFundsTests.BEFORE,SettlementFundsTests.CUTOFF.plusSeconds(1));var result=rules.assess(period);
            assertEquals(Status.UNAVAILABLE,bucket(result,Scope.SELECTED_JOURNAL_SETTLED_CHANGE).status());assertTrue(bucket(result,Scope.SELECTED_JOURNAL_SETTLED_CHANGE).reasons().contains(Reason.UNKNOWN_REFUND_DESTINATION));assertEquals(Status.ELIGIBLE_SUPPLIED_DIRECT_ROWS,bucket(result,Scope.SELECTED_JOURNAL_RESERVED_CHANGE).status());assertEquals(before,writer.balances());
        }
    }
    @Test void actualGroupedCompositeWithNegativeDebtCannotBeSummedAsDirectRows(){
        var writer=new SettlementFundsTests();writer.setup();writer.cash("deposit","20");writer.run(()->writer.funds.chargeDebt(1,"loss",writer.n("70")));assertEquals(writer.n("-50"),writer.balances().settled());
        try(var report=new JournalReportFixture(writer.fixture.source,false)){
            var period=report.context.getBean(JournalPeriodService.class).read(report.owner.getEmail(),SettlementFundsTests.BEFORE,SettlementFundsTests.BEFORE.plusSeconds(1));assertEquals(1,period.assessment().groupedCharges());
            var result=rules.assess(period);unavailable(result);assertTrue(bucket(result,Scope.SELECTED_JOURNAL_SETTLED_CHANGE).reasons().contains(Reason.COMPOSITE_EFFECT_OUTSIDE_DIRECT_SCOPE));
        }
    }
    @Test void missingOversizedAndOutsideBoundaryEvidenceIsUnavailable(){
        unavailable(rules.assess(null));f.account(1);f.entry(1,"credit","SETTLED_CREDIT","1",JournalReportFixture.AT);var p=read();
        unavailable(rules.assess(copy(p,Collections.nCopies(1001,p.entries().getFirst()),false,false,p.chargeGrouping())));
        var entry=p.entries().getFirst();var raw=entry.raw();var outside=new SettlementJournalPage.Entry(raw.eventKey(),raw.action(),raw.amount(),null,null,p.toExclusive());
        unavailable(rules.assess(copy(p,List.of(new JournalPeriodEvidence.Entry(outside,entry.interpretation(),entry.funding(),entry.fundingEffect())),false,false,p.chargeGrouping())));
    }
}
