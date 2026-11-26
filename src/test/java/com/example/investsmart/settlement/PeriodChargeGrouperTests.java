package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.investsmart.dto.GroupedChargeEvidence.*;

class PeriodChargeGrouperTests {
    final ChargeEvidenceReconcilerTests fixture=new ChargeEvidenceReconcilerTests();
    final PeriodChargeGrouper grouper=new PeriodChargeGrouper(fixture.rules);
    ChargeEvidence proof(){return new ChargeEvidence(fixture.parent(),fixture.pair(),false,false,fixture.rules.reconcile(fixture.parent(),fixture.pair(),false));}
    List<SettlementJournalPage.Entry> rows(){return List.of(fixture.parent(),fixture.pair().get(0).journal(),fixture.pair().get(1).journal());}
    SettlementJournalPage.Entry ordinary(String key){return new SettlementJournalPage.Entry(key,"WITHDRAW",new BigDecimal("1.00"),null,null,JournalReportFixture.AT);}
    @Test void groupsVerifiedRowsOnceAndPreservesOrdinaryRecords(){
        var rows=new ArrayList<>(rows());rows.add(ordinary("ordinary"));var result=grouper.group(rows,List.of(proof()),false);
        assertEquals(Status.GROUPED_SUPPLIED_ROWS,result.status());assertEquals(2,result.groups().size());
        var group=result.groups().getFirst();assertEquals(Kind.VERIFIED_CHARGE,group.kind());assertEquals("charge",group.representativeKey());assertEquals(3,group.selectedRows().size());
        assertEquals(new HashSet<>(rows),new HashSet<>(result.groups().stream().flatMap(g->g.selectedRows().stream()).toList()));
        assertEquals(4,result.groups().stream().mapToInt(g->g.selectedRows().size()).sum());
        assertThrows(UnsupportedOperationException.class,()->group.selectedRows().clear());assertThrows(UnsupportedOperationException.class,()->result.groups().clear());
    }
    @Test void outsideChildrenAreContextOnlyAndOutsideParentNeverHidesVisibleChild(){
        var parentOnly=grouper.group(List.of(fixture.parent()),List.of(proof()),false);
        assertEquals(Status.GROUPED_SUPPLIED_ROWS,parentOnly.status());assertEquals(1,parentOnly.groups().getFirst().selectedRows().size());assertEquals(2,parentOnly.groups().getFirst().outsideSelectionChildKeys().size());
        var childOnly=grouper.group(List.of(rows().get(1)),List.of(proof()),false);
        assertEquals(Status.INCOMPLETE,childOnly.status());assertEquals(Kind.UNGROUPED,childOnly.groups().getFirst().kind());assertEquals(rows().get(1),childOnly.groups().getFirst().selectedRows().getFirst());
    }
    @Test void duplicateAndConflictingProofsRetainEverySelectedRow(){
        var duplicated=grouper.group(rows(),List.of(proof(),proof()),false);
        assertEquals(Status.INCOMPLETE,duplicated.status());assertEquals(3,duplicated.groups().size());
        var original=proof();var conflicting=new ChargeEvidence(original.parent(),original.children(),true,false,original.comparison());
        var result=grouper.group(rows(),List.of(original,conflicting),false);assertEquals(3,result.groups().size());
        assertTrue(result.groups().stream().allMatch(g->g.kind()==Kind.UNGROUPED));
    }
    @Test void competingOutsideProofBlocksSharedVisibleChild(){
        var original=proof();var other=new SettlementJournalPage.Entry("outside-parent","REALIZED_CHARGE",new BigDecimal("70.00"),null,null,JournalReportFixture.AT);
        var competitor=new ChargeEvidence(other,original.children(),false,false,original.comparison());
        var result=grouper.group(rows(),List.of(original,competitor),false);
        assertEquals(Status.INCOMPLETE,result.status());assertEquals(3,result.groups().size());
    }
    @Test void staleRawRowsAndClaimedResultsCannotHideConflicts(){
        var selected=new ArrayList<>(rows());var raw=selected.get(1);
        selected.set(1,new SettlementJournalPage.Entry(raw.eventKey(),raw.action(),new BigDecimal("51.00"),raw.referenceKind(),raw.referenceId(),raw.occurredAt()));
        assertEquals(3,grouper.group(selected,List.of(proof()),false).groups().size());
        var original=proof();var stale=new ChargeEvidence(original.parent(),original.children(),false,false,null);
        assertEquals(3,grouper.group(rows(),List.of(stale),false).groups().size());
        assertEquals(3,grouper.group(rows(),List.of(),false).groups().size());
    }
    @Test void inputLimitsAndDuplicateIdentitiesAreUnavailable(){
        var exact=new ArrayList<SettlementJournalPage.Entry>();for(int i=0;i<1000;i++)exact.add(ordinary("e"+i));
        assertEquals(1000,grouper.group(exact,List.of(),false).groups().size());exact.add(ordinary("extra"));
        assertTrue(grouper.group(exact,List.of(),false).groups().isEmpty());
        assertTrue(grouper.group(rows(),List.of(proof()),true).groups().isEmpty());
        assertTrue(grouper.group(List.of(ordinary("x"),ordinary("x")),List.of(),false).groups().isEmpty());
        assertTrue(grouper.group(rows(),Collections.nCopies(1001,proof()),false).groups().isEmpty());
        assertTrue(grouper.group(rows(),Collections.nCopies(501,proof()),false).groups().isEmpty());
        assertEquals(List.of("SOURCE_EVIDENCE_LIMIT"),grouper.group(rows(),Collections.nCopies(251,proof()),false).limitations());
        assertEquals(3,grouper.group(rows(),Collections.nCopies(250,proof()),false).groups().size());
    }
    @Test void actualOwnedReaderProofGroupsWriterRowsWithoutChangingMoney(){
        var writer=new SettlementFundsTests();writer.setup();writer.cash("deposit","20");writer.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);
        writer.run(()->writer.funds.chargeDebt(1,"charge",new BigDecimal("70.00")));
        try(var report=new JournalReportFixture(writer.fixture.source,false)){
            var before=writer.balances();var evidence=report.context.getBean(ChargeEvidenceService.class).read(report.owner.getEmail(),"charge");
            var selected=new ArrayList<SettlementJournalPage.Entry>();selected.add(evidence.parent());evidence.children().forEach(e->selected.add(e.journal()));
            var result=grouper.group(selected,List.of(evidence),false);
            assertEquals(Status.GROUPED_SUPPLIED_ROWS,result.status());assertEquals(1,result.groups().size());assertEquals(3,result.groups().getFirst().selectedRows().size());
            assertEquals(before,writer.balances());
        }
    }
}
