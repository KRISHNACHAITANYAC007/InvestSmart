package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.example.investsmart.dto.SettlementFundingPage.Verification.*;
import static com.example.investsmart.settlement.FundingEffectClassifier.Coverage.*;
import static org.junit.jupiter.api.Assertions.*;

class FundingEffectClassifierTests {
    final FundingEffectClassifier classifier=new FundingEffectClassifier(new JournalActionClassifier());
    BigDecimal n(String v){return new BigDecimal(v).setScale(2);}
    SettlementFundingPage.Source settled(String id,String amount){return new SettlementFundingPage.Source(id,n(amount),"SETTLED","SETTLED_ORIGIN",null,null);}
    SettlementFundingPage.Source credit(String id,String amount,String status){return new SettlementFundingPage.Source(id,n(amount),"obligation","CREDIT_ORIGIN","REUSABLE",status);}
    SettlementFundingPage.Event event(String action,String amount,List<SettlementFundingPage.Source> sources){
        return new SettlementFundingPage.Event(new SettlementJournalPage.Entry("event",action,n(amount),"ORDER","order",JournalReportFixture.AT),MATCHED,sources,List.of());
    }
    @Test void splitReserveAndSpendHaveDistinctDirectEffects(){
        var sources=List.of(settled("s","20"),credit("c","30","SETTLED"));
        var reserve=classifier.classify(event("RESERVE","50",sources));assertEquals(DIRECT_EFFECT,reserve.coverage());
        assertEquals(n("-20"),reserve.settledDelta());assertEquals(n("50"),reserve.reservedDelta());assertEquals(n("30"),reserve.creditOriginAmount());
        var spend=classifier.classify(event("SPEND","50",sources));assertEquals(n("0"),spend.settledDelta());assertEquals(n("-50"),spend.reservedDelta());
    }
    @Test void creditRefundDestinationStaysUnknownRegardlessOfCurrentStatus(){
        for(var status:List.of("PENDING","SETTLED")){
            var result=classifier.classify(event("RELEASE","50",List.of(settled("s","20"),credit("c","30",status))));
            assertEquals(UNKNOWN_CREDIT_REFUND_DESTINATION,result.coverage());assertNull(result.settledDelta());
            assertEquals(n("-50"),result.reservedDelta());assertEquals(n("20"),result.settledOriginAmount());
        }
        var known=classifier.classify(event("RELEASE","20",List.of(settled("s","20"))));assertEquals(DIRECT_EFFECT,known.coverage());assertEquals(n("20"),known.settledDelta());
    }
    @Test void incompleteMismatchedAndCompositeEventsWithholdAllEffects(){
        var original=event("RESERVE","20",List.of(settled("s","20")));
        for(var verification:List.of(SettlementFundingPage.Verification.INCOMPLETE,SettlementFundingPage.Verification.MISMATCH,SettlementFundingPage.Verification.NOT_EVALUATED)){
            var result=classifier.classify(new SettlementFundingPage.Event(original.journal(),verification,original.sources(),List.of()));
            assertNull(result.settledDelta());assertNull(result.reservedDelta());assertNull(result.settledOriginAmount());
        }
        assertEquals(FundingEffectClassifier.Coverage.INCOMPLETE,classifier.classify(null).coverage());
        assertEquals(FundingEffectClassifier.Coverage.NOT_EVALUATED,classifier.classify(event("REALIZED_CHARGE","20",List.of())).coverage());
    }
    @Test void conflictsCannotBeCertifiedByMatchedFlag(){
        var original=event("RESERVE","20",List.of(settled("s","20")));
        var exact=classifier.classify(event("RESERVE","20",List.of(new SettlementFundingPage.Source("s",new BigDecimal("20.000"),"OPENING_SETTLED","SETTLED_ORIGIN",null,null))));
        assertEquals(n("-20"),exact.settledDelta());
        for(var sources:List.of(List.of(settled("s","19")),List.of(settled("s","10"),settled("s","10")),
                List.of(new SettlementFundingPage.Source("s",n("20"),"obligation","CREDIT_ORIGIN","RESTRICTED","SETTLED")),
                List.of(new SettlementFundingPage.Source("s",n("20"),"obligation","SETTLED_ORIGIN",null,null)),
                List.of(new SettlementFundingPage.Source("s",new BigDecimal("20.001"),"SETTLED","SETTLED_ORIGIN",null,null)))){
            var result=classifier.classify(event("RESERVE","20",sources));assertEquals(FundingEffectClassifier.Coverage.MISMATCH,result.coverage());assertNull(result.reservedDelta());
        }
        assertEquals(FundingEffectClassifier.Coverage.MISMATCH,classifier.classify(new SettlementFundingPage.Event(original.journal(),MATCHED,original.sources(),List.of("CONFLICT"))).coverage());
        assertEquals(FundingEffectClassifier.Coverage.MISMATCH,classifier.classify(event("RESERVE","0",List.of(settled("s","0")))).coverage());
    }
    @Test void exactSourceLimitAndOverflowAreDifferent(){
        var sources=new ArrayList<SettlementFundingPage.Source>();for(int i=0;i<1000;i++)sources.add(settled("s"+i,"0.01"));
        assertEquals(DIRECT_EFFECT,classifier.classify(event("RESERVE","10",sources)).coverage());
        sources.add(settled("extra","0.01"));var limited=classifier.classify(event("RESERVE","10.01",sources));
        assertEquals(FundingEffectClassifier.Coverage.INCOMPLETE,limited.coverage());assertNull(limited.settledDelta());
    }
    @Test void actualMixedRefundRetainsUncertaintyAfterItsSourceSettles(){
        var f=new SettlementFundsTests();f.setup();f.cash("deposit","20");var obligation=f.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);
        f.reserve("reserve","30");f.time(SettlementFundsTests.CUTOFF);f.get(()->f.funds.settle(1,obligation));f.release("refund","30");
        try(var report=new JournalReportFixture(f.fixture.source,false)){
            var before=f.balances();var events=report.fundingService.read(report.owner.getEmail(),0,100).entries();
            var reserve=classifier.classify(events.stream().filter(e->e.journal().eventKey().equals("reserve")).findFirst().orElseThrow());
            assertEquals(n("-20"),reserve.settledDelta());assertEquals(n("10"),reserve.creditOriginAmount());
            var refund=classifier.classify(events.stream().filter(e->e.journal().eventKey().equals("refund")).findFirst().orElseThrow());
            assertEquals(UNKNOWN_CREDIT_REFUND_DESTINATION,refund.coverage());assertNull(refund.settledDelta());assertEquals(n("-30"),refund.reservedDelta());
            assertEquals(before,f.balances());
        }
    }
    @Test void actualChargeChildrenRemainIndividualEffectsNotExpenseTotals(){
        var f=new SettlementFundsTests();f.setup();f.cash("deposit","20");f.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);
        f.run(()->f.funds.chargeDebt(1,"charge",n("70")));
        try(var report=new JournalReportFixture(f.fixture.source,false)){
            var before=f.balances();var events=report.fundingService.read(report.owner.getEmail(),0,100).entries();
            assertEquals(FundingEffectClassifier.Coverage.NOT_EVALUATED,classifier.classify(events.stream().filter(e->e.journal().eventKey().equals("charge")).findFirst().orElseThrow()).coverage());
            var children=events.stream().filter(e->"CHARGE".equals(e.journal().referenceKind())).map(classifier::classify).toList();
            assertEquals(2,children.size());assertTrue(children.stream().allMatch(e->e.coverage()==DIRECT_EFFECT));
            assertEquals(n("-20"),before.settled());assertEquals(before,f.balances());
        }
    }
}
