package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PeriodChargeAdapterTests {
    SettlementFundsTests writer;JournalReportFixture f;JournalPeriodService service;
    final String reference=ChargeEvidenceReconciler.reference("charge");
    @BeforeEach void setup(){writer=new SettlementFundsTests();writer.setup();writer.cash("deposit","20");writer.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);writer.run(()->writer.funds.chargeDebt(1,"charge",new BigDecimal("70.00")));f=new JournalReportFixture(writer.fixture.source,false);service=f.context.getBean(JournalPeriodService.class);}
    @AfterEach void close(){f.close();}
    JournalPeriodEvidence read(){return service.read(f.owner.getEmail(),SettlementFundsTests.BEFORE,SettlementFundsTests.BEFORE.plusSeconds(1));}
    @Test void retainedFullSourceIsImmutableWithoutEnablingCompositeCashSums(){
        var before=writer.balances();var result=read();var sources=result.chargeGrouping().sourceEvidence();assertEquals(1,sources.size());var binding=sources.getFirst();
        assertEquals(ChargeGroupSourceEvidence.Status.MATCHED_SUPPLIED_GROUP_SOURCE,binding.status());assertEquals(new BigDecimal("70.00"),binding.sourceProof().comparison().chargeAmount());assertEquals(new BigDecimal("-40.00"),binding.sourceProof().comparison().settledEffect());
        assertEquals(binding.group(),result.chargeGrouping().groups().stream().filter(g->g.kind()==GroupedChargeEvidence.Kind.VERIFIED_CHARGE).findFirst().orElseThrow());
        assertThrows(UnsupportedOperationException.class,()->sources.clear());assertThrows(UnsupportedOperationException.class,()->binding.sourceProof().children().clear());
        assertTrue(result.selectedDirectChanges().buckets().stream().allMatch(b->b.change()==null));assertEquals(before,writer.balances());verifyNoInteractions(f.chargeRepository);
    }
    @Test void contradictorySourceRemovesRetainedProofAndPreservesRawRows(){
        var before=read();assertEquals(1,before.chargeGrouping().sourceEvidence().size());
        f.db.update("UPDATE settlement_allocation_events SET amount=amount+1 WHERE event_key=?","CHARGE_SPEND:"+reference);
        var result=read();assertEquals(before.entries().size(),result.entries().size());assertTrue(result.chargeGrouping().sourceEvidence().isEmpty());assertEquals(GroupedChargeEvidence.Status.INCOMPLETE,result.chargeGrouping().status());
    }
    @Test void groupsOwnedWriterRecordsOnceWithoutPerParentServicesOrWrites(){
        var before=writer.balances();var result=read();
        assertEquals(GroupedChargeEvidence.Status.GROUPED_SUPPLIED_ROWS,result.chargeGrouping().status());
        assertEquals(1,result.chargeGrouping().groups().stream().filter(g->g.kind()==GroupedChargeEvidence.Kind.VERIFIED_CHARGE).count());
        assertEquals(result.entries().size(),result.chargeGrouping().groups().stream().mapToInt(g->g.selectedRows().size()).sum());
        assertEquals(before,writer.balances());verifyNoInteractions(f.chargeRepository);
        verify(f.periodChargeRepository,times(1)).children(1,List.of(reference),1001);
    }
    @Test void outsideChildrenAreOnlyContextAndOutsideParentLeavesChildrenVisible(){
        f.db.update("UPDATE settlement_journal SET occurred_at=? WHERE reference_kind='CHARGE'",java.sql.Timestamp.from(SettlementFundsTests.BEFORE.plusSeconds(86400)));
        var result=read();var group=result.chargeGrouping().groups().stream().filter(g->g.kind()==GroupedChargeEvidence.Kind.VERIFIED_CHARGE).findFirst().orElseThrow();
        assertEquals(1,group.selectedRows().size());assertEquals(2,group.outsideSelectionChildKeys().size());
        assertEquals(2,result.chargeGrouping().sourceEvidence().getFirst().sourceProof().children().size());
        f.db.update("UPDATE settlement_journal SET occurred_at=? WHERE reference_kind='CHARGE'",java.sql.Timestamp.from(SettlementFundsTests.BEFORE));
        f.db.update("UPDATE settlement_journal SET occurred_at=? WHERE event_key='charge'",java.sql.Timestamp.from(SettlementFundsTests.BEFORE.plusSeconds(86400)));
        clearInvocations(f.periodChargeRepository);var childOnly=read();
        assertEquals(GroupedChargeEvidence.Status.INCOMPLETE,childOnly.chargeGrouping().status());
        assertTrue(childOnly.chargeGrouping().groups().stream().allMatch(g->g.kind()==GroupedChargeEvidence.Kind.UNGROUPED));
        assertTrue(childOnly.chargeGrouping().sourceEvidence().isEmpty());
        verifyNoInteractions(f.periodChargeRepository);
    }
    @Test void foreignSiblingsExcludedAndForeignSourceDoesNotCertifyGrouping(){
        f.entry(2,"private-child","RESERVE","999",SettlementFundsTests.BEFORE);
        f.db.update("UPDATE settlement_journal SET reference_kind='CHARGE',reference_id=? WHERE user_id=2 AND event_key='private-child'",reference);
        assertEquals(GroupedChargeEvidence.Status.GROUPED_SUPPLIED_ROWS,read().chargeGrouping().status());
        assertFalse(read().toString().contains("private-child"));
        f.db.update("UPDATE settlement_allocations SET user_id=2 WHERE reference_kind='CHARGE'");
        var result=read();assertEquals(GroupedChargeEvidence.Status.INCOMPLETE,result.chargeGrouping().status());
        assertTrue(result.chargeGrouping().sourceEvidence().isEmpty());
        assertTrue(result.chargeGrouping().groups().stream().allMatch(g->g.kind()==GroupedChargeEvidence.Kind.UNGROUPED));
    }
    @Test void missingAndUnexpectedSiblingsStayUngrouped(){
        f.db.update("DELETE FROM settlement_journal WHERE event_key=?","CHARGE_SPEND:"+reference);
        var missing=read();assertEquals(GroupedChargeEvidence.Status.INCOMPLETE,missing.chargeGrouping().status());
        assertEquals(missing.entries().size(),missing.chargeGrouping().groups().size());
        f.entry(1,"unexpected","RELEASE","1",SettlementFundsTests.BEFORE);
        f.db.update("UPDATE settlement_journal SET reference_kind='CHARGE',reference_id=? WHERE event_key='unexpected'",reference);
        assertEquals(GroupedChargeEvidence.Status.INCOMPLETE,read().chargeGrouping().status());
    }
    @Test void globalCandidateOverflowPreservesRawPeriodRows(){
        var parent=f.db.queryForObject("SELECT event_key,action,amount,reference_kind,reference_id,occurred_at FROM settlement_journal WHERE event_key='charge'",(r,n)->new SettlementJournalPage.Entry(r.getString(1),r.getString(2),r.getBigDecimal(3),r.getString(4),r.getString(5),r.getTimestamp(6).toInstant()));
        doReturn(Collections.nCopies(1001,parent)).when(f.periodChargeRepository).children(eq(1L),anyList(),eq(1001));
        var result=read();assertFalse(result.entries().isEmpty());assertEquals(GroupedChargeEvidence.Status.INCOMPLETE,result.chargeGrouping().status());assertTrue(result.chargeGrouping().groups().isEmpty());
        assertTrue(result.chargeGrouping().sourceEvidence().isEmpty());
    }
    @Test void parentBatchesUseSharedCandidateBudgetAndSourceOverflowIsExplicit(){
        for(int i=0;i<101;i++)f.entry(1,String.format("p%04d",i),"REALIZED_CHARGE","1",SettlementFundsTests.BEFORE);
        var result=read();assertEquals(GroupedChargeEvidence.Status.INCOMPLETE,result.chargeGrouping().status());
        verify(f.periodChargeRepository,times(2)).children(eq(1L),anyList(),anyInt());
        verify(f.periodChargeRepository).children(eq(1L),argThat(keys->keys.size()==2),eq(999));
        f.db.update("DELETE FROM settlement_journal WHERE event_key LIKE 'p%'");
        var calls=new int[1];var row=new SettlementFundingRepository.Evidence("key","RESERVE",new BigDecimal("1.00"),"a","a","CHARGE",reference,"SETTLED",null,null,null,null);
        doAnswer(call->{if(calls[0]++==0)return call.callRealMethod();return Collections.nCopies(1001,row);}).when(f.fundingRepository).evidence(anyLong(),anyList(),anyInt());
        var limited=read();assertFalse(limited.entries().isEmpty());assertFalse(limited.fundingEvidenceTruncated());
        assertEquals(GroupedChargeEvidence.Status.INCOMPLETE,limited.chargeGrouping().status());assertTrue(limited.chargeGrouping().groups().isEmpty());
    }
    @Test void candidateAndSourceQueriesShareCallerSnapshotAndAdapterRejectsOutsideCalls(){
        var source=f.context.getBean(javax.sql.DataSource.class);Object[] resource={null};
        doAnswer(call->{resource[0]=TransactionSynchronizationManager.getResource(source);assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());return call.callRealMethod();}).when(f.periodRepository).entries(anyLong(),any(),any());
        doAnswer(call->{assertSame(resource[0],TransactionSynchronizationManager.getResource(source));return call.callRealMethod();}).when(f.periodChargeRepository).children(anyLong(),anyList(),anyInt());
        doAnswer(call->{assertSame(resource[0],TransactionSynchronizationManager.getResource(source));return call.callRealMethod();}).when(f.fundingRepository).evidence(anyLong(),anyList(),anyInt());
        read();verifyNoInteractions(f.chargeRepository);
        assertThrows(IllegalStateException.class,()->f.context.getBean(PeriodChargeAdapter.class).group(1,List.of(),false));
    }
}
