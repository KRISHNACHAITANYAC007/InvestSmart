package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.investsmart.dto.PeriodSummaryEligibility.Scope.*;
import static com.example.investsmart.dto.SelectedJournalChangeSummary.Status.*;
class SelectedJournalChangeSummarizerTests {
    JournalReportFixture f;SelectedJournalChangeSummarizer rules;
    @BeforeEach void setup(){f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:direct-sum-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);rules=new SelectedJournalChangeSummarizer(f.eligibilityAssessor);}
    @AfterEach void close(){f.close();}
    JournalPeriodEvidence read(){return f.context.getBean(JournalPeriodService.class).read(f.owner.getEmail(),JournalReportFixture.AT,JournalReportFixture.AT.plusSeconds(60));}
    SelectedJournalChangeSummary.Bucket bucket(SelectedJournalChangeSummary result,PeriodSummaryEligibility.Scope scope){return result.buckets().stream().filter(b->b.scope()==scope).findFirst().orElseThrow();}
    BigDecimal settled(SelectedJournalChangeSummary result){return bucket(result,SELECTED_JOURNAL_SETTLED_CHANGE).change();}
    BigDecimal reserved(SelectedJournalChangeSummary result){return bucket(result,SELECTED_JOURNAL_RESERVED_CHANGE).change();}
    JournalPeriodEvidence copy(JournalPeriodEvidence p,boolean truncated,boolean funding,GroupedChargeEvidence grouping){return new JournalPeriodEvidence(p.fromInclusive(),p.toExclusive(),p.initialization(),p.startsBeforeInitialization(),false,truncated,funding,p.entries(),p.limitations(),grouping);}
    @Test void unavailableIsNullWhileEligibleEmptySelectionIsZeroOnlyInDirectScopes(){
        assertNull(settled(rules.summarize(null)));assertNull(settled(rules.summarize(read())));f.account(1);var result=rules.summarize(read());
        assertEquals(new BigDecimal("0.00"),settled(result));assertEquals(new BigDecimal("0.00"),reserved(result));
        for(var scope:List.of(COMPLETE_ACCOUNT_EXPENSES,ACCOUNT_PROFIT_LOSS,HISTORICAL_BALANCES)){assertEquals(UNAVAILABLE,bucket(result,scope).status());assertNull(bucket(result,scope).change());}
        assertThrows(UnsupportedOperationException.class,()->result.buckets().clear());assertThrows(UnsupportedOperationException.class,()->result.buckets().getFirst().reasons().clear());
    }
    @Test void signedCashChangesAreNotJournalMagnitudeSumsOrAccountBalances(){
        f.account(1);f.entry(1,"credit","SETTLED_CREDIT","10",JournalReportFixture.AT);f.entry(1,"withdraw","WITHDRAW","12",JournalReportFixture.AT);
        var users=f.db.queryForList("SELECT * FROM users ORDER BY id");var result=rules.summarize(read());assertEquals(new BigDecimal("-2.00"),settled(result));assertEquals(new BigDecimal("0.00"),reserved(result));assertEquals(users,f.db.queryForList("SELECT * FROM users ORDER BY id"));
    }
    @Test void actualWriterReservationSpendAndSettledRefundUseFundingEffectsOnce(){
        var writer=new SettlementFundsTests();writer.setup();writer.cash("deposit","20");writer.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);writer.reserve("reserve","25");writer.spend("spend","10");writer.release("refund","5");var before=writer.balances();
        try(var report=new JournalReportFixture(writer.fixture.source,false)){
            var p=report.context.getBean(JournalPeriodService.class).read(report.owner.getEmail(),SettlementFundsTests.BEFORE,SettlementFundsTests.BEFORE.plusSeconds(1));var result=rules.summarize(p);
            assertEquals(new BigDecimal("5.00"),settled(result));assertEquals(new BigDecimal("10.00"),reserved(result));assertEquals(before,writer.balances());
        }
    }
    @Test void actualCreditRefundKeepsUnknownSettledBucketNullAndKnownReservedZero(){
        var writer=new SettlementFundsTests();writer.setup();writer.cash("deposit","20");var id=writer.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);writer.reserve("reserve","30");writer.time(SettlementFundsTests.CUTOFF);writer.get(()->writer.funds.settle(1,id));writer.release("refund","30");
        try(var report=new JournalReportFixture(writer.fixture.source,false)){
            var p=report.context.getBean(JournalPeriodService.class).read(report.owner.getEmail(),SettlementFundsTests.BEFORE,SettlementFundsTests.CUTOFF.plusSeconds(1));var result=rules.summarize(p);assertNull(settled(result));assertEquals(new BigDecimal("0.00"),reserved(result));assertTrue(bucket(result,SELECTED_JOURNAL_SETTLED_CHANGE).reasons().contains("UNKNOWN_REFUND_DESTINATION"));
        }
    }
    @Test void actualGroupedNegativeDebtCannotProducePartialOrDoubleCountedTotals(){
        var writer=new SettlementFundsTests();writer.setup();writer.cash("deposit","20");writer.run(()->writer.funds.chargeDebt(1,"loss",writer.n("70")));assertEquals(writer.n("-50"),writer.balances().settled());
        try(var report=new JournalReportFixture(writer.fixture.source,false)){
            var p=report.context.getBean(JournalPeriodService.class).read(report.owner.getEmail(),SettlementFundsTests.BEFORE,SettlementFundsTests.BEFORE.plusSeconds(1));assertEquals(1,p.assessment().groupedCharges());var result=rules.summarize(p);assertNull(settled(result));assertNull(reserved(result));assertTrue(bucket(result,SELECTED_JOURNAL_SETTLED_CHANGE).reasons().contains("COMPOSITE_EFFECT_OUTSIDE_DIRECT_SCOPE"));
        }
    }
    @Test void staleEligibilityAndCoverageCannotAuthorizeMoneyOutput(){
        f.account(1);var empty=read();f.entry(1,"credit","SETTLED_CREDIT","10",JournalReportFixture.AT);var p=read();assertNull(settled(rules.summarize(p.withAssessment(empty.assessment()))));
        f.entry(1,"unknown","FUTURE_ACTION","1",JournalReportFixture.AT);var forged=read().withSummaryEligibility(p.summaryEligibility());var result=rules.summarize(forged);assertNull(settled(result));assertTrue(bucket(result,SELECTED_JOURNAL_RESERVED_CHANGE).reasons().contains("STALE_ELIGIBILITY"));
    }
    @Test void selectedFundingGroupingAndInvalidEvidenceWithholdWholeBucket(){
        f.account(1);f.entry(1,"credit","SETTLED_CREDIT","10",JournalReportFixture.AT);var p=read();
        for(var limited:List.of(copy(p,true,false,p.chargeGrouping()),copy(p,false,true,p.chargeGrouping()),copy(p,false,false,null))){assertNull(settled(rules.summarize(limited)));assertNull(reserved(rules.summarize(limited)));}
        f.entry(1,"invalid","WITHDRAW","-1",JournalReportFixture.AT);assertNull(settled(rules.summarize(read())));
    }
    @Test void halfOpenBoundariesExcludeContextAndEqualTimestampsDoNotImplyRunningBalances(){
        f.account(1);f.entry(1,"before","SETTLED_CREDIT","100",JournalReportFixture.AT.minusSeconds(1));f.entry(1,"inside","SETTLED_CREDIT","10",JournalReportFixture.AT);f.entry(1,"same-time","WITHDRAW","2",JournalReportFixture.AT);f.entry(1,"end","WITHDRAW","200",JournalReportFixture.AT.plusSeconds(60));
        assertEquals(new BigDecimal("8.00"),settled(rules.summarize(read())));
    }
    @Test void thousandMaximumExactRowsFitWideAggregateAndSentinelWithholds(){
        f.account(1);f.db.batchUpdate("INSERT INTO settlement_journal VALUES(1,?,'SETTLED_CREDIT',?,99999999999999999.99,NULL,NULL,?)",new org.springframework.jdbc.core.BatchPreparedStatementSetter(){public int getBatchSize(){return 1000;}public void setValues(java.sql.PreparedStatement p,int i)throws java.sql.SQLException{p.setString(1,"r"+i);p.setString(2,"a".repeat(64));p.setTimestamp(3,java.sql.Timestamp.from(JournalReportFixture.AT));}});
        assertEquals(new BigDecimal("99999999999999999990.00"),settled(rules.summarize(read())));f.entry(1,"sentinel","SETTLED_CREDIT","1",JournalReportFixture.AT);assertNull(settled(rules.summarize(read())));
    }
}
