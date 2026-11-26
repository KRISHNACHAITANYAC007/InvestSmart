package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.investsmart.settlement.ChargeEvidenceReconciler.Status.*;

class ChargeEvidenceReconcilerTests {
    final JournalActionClassifier actions=new JournalActionClassifier();
    final ChargeEvidenceReconciler rules=new ChargeEvidenceReconciler(actions,new FundingEffectClassifier(actions));
    BigDecimal n(String v){return new BigDecimal(v).setScale(2);}
    SettlementJournalPage.Entry parent(){return new SettlementJournalPage.Entry("charge","REALIZED_CHARGE",n("70"),null,null,JournalReportFixture.AT);}
    List<SettlementFundingPage.Event> pair(){
        var ref=UUID.nameUUIDFromBytes("charge".getBytes(StandardCharsets.UTF_8)).toString();
        var sources=List.of(new SettlementFundingPage.Source("cash",n("20"),"SETTLED","SETTLED_ORIGIN",null,null),
            new SettlementFundingPage.Source("credit",n("30"),"obligation","CREDIT_ORIGIN","REUSABLE","SETTLED"));
        return List.of(new SettlementFundingPage.Event(new SettlementJournalPage.Entry("CHARGE_RESERVE:"+ref,"RESERVE",n("50"),"CHARGE",ref,JournalReportFixture.AT),SettlementFundingPage.Verification.MATCHED,sources,List.of()),
            new SettlementFundingPage.Event(new SettlementJournalPage.Entry("CHARGE_SPEND:"+ref,"SPEND",n("50"),"CHARGE",ref,JournalReportFixture.AT),SettlementFundingPage.Verification.MATCHED,sources,List.of()));
    }
    @Test void groupsOneChargeWithoutAddingParentAndChildren(){
        var result=rules.reconcile(parent(),pair(),false);assertEquals(MATCHED_SUPPLIED_PAIR,result.status());
        assertEquals(n("70"),result.chargeAmount());assertEquals(n("50"),result.fundedAmount());assertEquals(n("20"),result.impliedUnfundedAmount());
        assertEquals(n("-40"),result.settledEffect());assertEquals(n("30"),result.creditOriginConsumption());
        assertThrows(UnsupportedOperationException.class,()->result.childKeys().clear());
        assertEquals(result,rules.reconcile(parent(),List.of(pair().get(1),pair().get(0)),false));
    }
    @Test void missingOrLimitedChildrenNeverProveAllDebt(){
        for(var children:List.of(List.<SettlementFundingPage.Event>of(),List.of(pair().getFirst()))){
            var result=rules.reconcile(parent(),children,false);assertEquals(INCOMPLETE,result.status());assertNull(result.impliedUnfundedAmount());assertNull(result.settledEffect());
        }
        assertEquals(INCOMPLETE,rules.reconcile(parent(),pair(),true).status());
        assertEquals(INCOMPLETE,rules.reconcile(parent(),Collections.nCopies(1001,pair().getFirst()),false).status());
        assertEquals(INCOMPLETE,rules.reconcile(null,pair(),false).status());
    }
    @Test void ambiguousDuplicateAndUnrelatedKeysWithholdGrouping(){
        assertEquals(MISMATCH,rules.reconcile(parent(),List.of(pair().getFirst(),pair().getFirst()),false).status());
        assertEquals(MISMATCH,rules.reconcile(parent(),List.of(pair().getFirst(),pair().get(1),pair().get(1)),false).status());
        var other=new SettlementJournalPage.Entry("other","REALIZED_CHARGE",n("70"),null,null,JournalReportFixture.AT);
        assertEquals(MISMATCH,rules.reconcile(other,pair(),false).status());
        var original=pair().get(1);var row=original.journal();
        var wrong=new SettlementFundingPage.Event(new SettlementJournalPage.Entry("prefix-only",row.action(),row.amount(),row.referenceKind(),row.referenceId(),row.occurredAt()),original.verification(),original.sources(),List.of());
        assertEquals(MISMATCH,rules.reconcile(parent(),List.of(pair().getFirst(),wrong),false).status());
    }
    @Test void sourcePairConflictAndUnverifiedEvidenceAreNotMatched(){
        var original=pair().get(1);
        var missing=new SettlementFundingPage.Event(original.journal(),SettlementFundingPage.Verification.INCOMPLETE,original.sources(),List.of());
        assertEquals(INCOMPLETE,rules.reconcile(parent(),List.of(pair().getFirst(),missing),false).status());
        var sources=List.of(new SettlementFundingPage.Source("different",n("50"),"SETTLED","SETTLED_ORIGIN",null,null));
        var conflicting=new SettlementFundingPage.Event(original.journal(),original.verification(),sources,List.of());
        assertEquals(MISMATCH,rules.reconcile(parent(),List.of(pair().getFirst(),conflicting),false).status());
        var smaller=new SettlementJournalPage.Entry("charge","REALIZED_CHARGE",n("40"),null,null,JournalReportFixture.AT);
        assertEquals(MISMATCH,rules.reconcile(smaller,pair(),false).status());
    }
    @Test void invalidParentAndOtherActionsCannotBecomeChargeTotals(){
        var wrong=new SettlementJournalPage.Entry("charge","WITHDRAW",n("70"),null,null,JournalReportFixture.AT);
        assertEquals(NOT_EVALUATED,rules.reconcile(wrong,pair(),false).status());
        var invalid=new SettlementJournalPage.Entry("charge","REALIZED_CHARGE",new BigDecimal("70.001"),null,null,JournalReportFixture.AT);
        assertEquals(MISMATCH,rules.reconcile(invalid,pair(),false).status());
    }
    @Test void actualWriterFundedMixedAndUnfundedChargesPreserveMoney(){
        for(var scenario:List.of(new String[]{"100","0","70","0","-70"},new String[]{"20","30","70","20","-40"},new String[]{"0","0","70","unknown","-70"})){
            var writer=new SettlementFundsTests();writer.setup();
            if(n(scenario[0]).signum()>0)writer.cash("deposit",scenario[0]);
            if(n(scenario[1]).signum()>0)writer.pending("sale",scenario[1],SettlementFunds.CreditPolicy.REUSABLE);
            writer.run(()->writer.funds.chargeDebt(1,"charge",n(scenario[2])));
            try(var report=new JournalReportFixture(writer.fixture.source,false)){
                var before=writer.balances();var events=report.fundingService.read(report.owner.getEmail(),0,100).entries();
                var parent=events.stream().filter(e->e.journal().eventKey().equals("charge")).findFirst().orElseThrow().journal();
                var children=events.stream().filter(e->"CHARGE".equals(e.journal().referenceKind())).toList();
                var result=rules.reconcile(parent,children,false);
                if(scenario[3].equals("unknown")){assertEquals(INCOMPLETE,result.status());assertNull(result.impliedUnfundedAmount());}
                else{assertEquals(MATCHED_SUPPLIED_PAIR,result.status());assertEquals(n(scenario[3]),result.impliedUnfundedAmount());assertEquals(n(scenario[4]),result.settledEffect());}
                assertEquals(before,writer.balances());
            }
        }
    }
}
