package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.investsmart.dto.ChargeGroupSourceEvidence.Status.*;
class ChargeGroupSourceReconcilerTests {
    final PeriodChargeGrouperTests fixture=new PeriodChargeGrouperTests();
    final ChargeGroupSourceReconciler rules=new ChargeGroupSourceReconciler(fixture.grouper,fixture.fixture.rules);
    GroupedChargeEvidence.Group group(List<SettlementJournalPage.Entry> rows){return fixture.grouper.group(rows,List.of(fixture.proof()),false).groups().getFirst();}
    @Test void retainsFullImmutableProofWithEverySelectedChildRepresentedOnce(){
        var proof=fixture.proof();var g=group(fixture.rows());var result=rules.reconcile(g,proof,false);
        assertEquals(MATCHED_SUPPLIED_GROUP_SOURCE,result.status());assertEquals(proof,result.sourceProof());assertEquals(g,result.group());assertEquals(3,result.group().selectedRows().size());
        assertThrows(UnsupportedOperationException.class,()->result.sourceProof().children().clear());assertThrows(UnsupportedOperationException.class,()->result.sourceProof().children().getFirst().sources().clear());
    }
    @Test void partialSelectionRetainsOutsideChildrenOnlyAsFullSourceContext(){
        var parentOnly=group(List.of(fixture.fixture.parent()));var result=rules.reconcile(parentOnly,fixture.proof(),false);
        assertEquals(MATCHED_SUPPLIED_GROUP_SOURCE,result.status());assertEquals(1,result.group().selectedRows().size());assertEquals(2,result.group().outsideSelectionChildKeys().size());assertEquals(2,result.sourceProof().children().size());
        assertTrue(result.reason().contains("NO_PERIOD_CASH_TOTAL"));
        var partial=group(fixture.rows().subList(0,2));assertEquals(MATCHED_SUPPLIED_GROUP_SOURCE,rules.reconcile(partial,fixture.proof(),false).status());
    }
    @Test void missingLimitedAndChildOnlyCannotGrantSourceBinding(){
        var g=group(fixture.rows());assertEquals(INCOMPLETE,rules.reconcile(g,null,false).status());assertEquals(INCOMPLETE,rules.reconcile(g,fixture.proof(),true).status());
        var p=fixture.proof();assertEquals(INCOMPLETE,rules.reconcile(g,new ChargeEvidence(p.parent(),p.children(),true,false,p.comparison()),false).status());
        assertEquals(INCOMPLETE,rules.reconcile(g,new ChargeEvidence(p.parent(),List.of(),false,false,p.comparison()),false).status());
        var childOnly=fixture.grouper.group(List.of(fixture.rows().get(1)),List.of(p),false).groups().getFirst();var result=rules.reconcile(childOnly,p,false);assertEquals(NOT_EVALUATED,result.status());assertNull(result.sourceProof());
    }
    @Test void staleComparisonAndConflictingSelectedOrContextRowsWithholdBothOutputs(){
        var p=fixture.proof();var g=group(fixture.rows());assertEquals(MISMATCH,rules.reconcile(g,new ChargeEvidence(p.parent(),p.children(),false,false,null),false).status());
        var parentOnly=group(List.of(p.parent()));
        for(var bad:List.of(new GroupedChargeEvidence.Group(parentOnly.kind(),"wrong",parentOnly.selectedRows(),parentOnly.outsideSelectionChildKeys()),new GroupedChargeEvidence.Group(parentOnly.kind(),parentOnly.representativeKey(),parentOnly.selectedRows(),List.of("invented")),new GroupedChargeEvidence.Group(g.kind(),g.representativeKey(),List.of(p.parent(),p.parent()),List.of()))){
            var result=rules.reconcile(bad,p,false);assertEquals(MISMATCH,result.status());assertNull(result.group());assertNull(result.sourceProof());
        }
    }
    @Test void oversizeAndUnrelatedProofNeverAuthorizeGroupedAmounts(){
        var g=group(fixture.rows());var oversized=new GroupedChargeEvidence.Group(g.kind(),g.representativeKey(),Collections.nCopies(1001,g.selectedRows().getFirst()),List.of());assertEquals(MISMATCH,rules.reconcile(oversized,fixture.proof(),false).status());
        var p=fixture.proof();var changed=new SettlementJournalPage.Entry(p.parent().eventKey(),p.parent().action(),new BigDecimal("71.00"),null,null,p.parent().occurredAt());
        var proof=new ChargeEvidence(changed,p.children(),false,false,fixture.fixture.rules.reconcile(changed,p.children(),false));assertEquals(MISMATCH,rules.reconcile(g,proof,false).status());
    }
    @Test void actualOwnedWriterProofPreservesNegativeDebtContextWithoutChangingMoney(){
        var writer=new SettlementFundsTests();writer.setup();writer.cash("deposit","20");writer.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);writer.run(()->writer.funds.chargeDebt(1,"charge",writer.n("70")));var before=writer.balances();assertEquals(writer.n("-20"),before.settled());
        try(var report=new JournalReportFixture(writer.fixture.source,false)){
            var p=report.context.getBean(ChargeEvidenceService.class).read(report.owner.getEmail(),"charge");var g=fixture.grouper.group(List.of(p.parent()),List.of(p),false).groups().getFirst();var result=rules.reconcile(g,p,false);
            assertEquals(MATCHED_SUPPLIED_GROUP_SOURCE,result.status());assertEquals(writer.n("-40"),result.sourceProof().comparison().settledEffect());assertEquals(writer.n("20"),result.sourceProof().comparison().impliedUnfundedAmount());assertEquals(before,writer.balances());
        }
    }
}
