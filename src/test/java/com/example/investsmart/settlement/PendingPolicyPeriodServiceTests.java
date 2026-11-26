package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import com.example.investsmart.exception.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class PendingPolicyPeriodServiceTests {
    SettlementFundsTests writer;JournalReportFixture f;PendingPolicyPeriodService service;String id;
    final Instant from=SettlementFundsTests.BEFORE,to=from.plusSeconds(1);
    @BeforeEach void setup(){writer=new SettlementFundsTests();writer.setup();id=writer.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);f=new JournalReportFixture(writer.fixture.source,false);service=f.context.getBean(PendingPolicyPeriodService.class);}
    @AfterEach void close(){f.close();}
    PendingPolicyPeriodEvidence read(){return service.read(f.owner.getEmail(),from,to);}
    @Test void matchesActualWriterOutsidePeriodSettledCandidateWithoutWritesOrHashExposure(){
        f.db.update("UPDATE settlement_obligations SET status='SETTLED',remaining_credit=0,created_at=? WHERE id=?",java.sql.Timestamp.from(from.minusSeconds(10)),id);
        var before=f.db.queryForList("SELECT * FROM settlement_obligations ORDER BY id");var users=f.db.queryForList("SELECT * FROM users ORDER BY id");var result=read();
        assertEquals(1,result.entries().size());assertEquals(id,result.entries().getFirst().policy().obligationId());assertEquals("REUSABLE",result.entries().getFirst().policy().policy());assertFalse(result.completeFinancialHistory());
        String hash=f.db.queryForObject("SELECT fingerprint FROM settlement_journal WHERE event_key='sale'",String.class);assertFalse(result.toString().contains(hash));
        assertEquals(before,f.db.queryForList("SELECT * FROM settlement_obligations ORDER BY id"));assertEquals(users,f.db.queryForList("SELECT * FROM users ORDER BY id"));
    }
    @Test void foreignCandidateAndForeignJournalNeverLeak(){
        f.db.update("UPDATE settlement_obligations SET user_id=2 WHERE id=?",id);var result=read();assertNull(result.entries().getFirst().policy().obligationId());assertFalse(result.toString().contains(id));
        f.db.update("UPDATE settlement_journal SET user_id=2 WHERE event_key='sale'");assertTrue(read().entries().isEmpty());
        var other=service.read(f.other.getEmail(),from,to);assertEquals(id,other.entries().getFirst().policy().obligationId());
    }
    @Test void missingCorruptAndAlteredInstructionKeepRawRowButWithholdPolicy(){
        f.db.update("UPDATE settlement_obligations SET credit_policy='RESTRICTED' WHERE id=?",id);assertNull(read().entries().getFirst().policy().policy());
        f.db.update("DELETE FROM settlement_obligations WHERE id=?",id);assertEquals(1,read().entries().size());assertNull(read().entries().getFirst().policy().obligationId());
        f.db.update("UPDATE settlement_journal SET fingerprint=? WHERE event_key='sale'","x".repeat(64));assertEquals(PendingCreditPolicyEvidence.Status.MISMATCH,read().entries().getFirst().policy().status());
    }
    @Test void boundsWithholdProofAndKeepRawSelection(){
        var original=f.pendingPolicyRepository.candidates(1).getFirst();var candidates=new ArrayList<PendingCreditPolicyReconciler.Candidate>();candidates.add(original);
        for(int i=0;i<999;i++)candidates.add(new PendingCreditPolicyReconciler.Candidate("c"+i,"s"+i,original.exchange(),original.segment(),original.tradeDate(),original.kind(),original.policy(),original.originalAmount()));
        doReturn(candidates).when(f.pendingPolicyRepository).candidates(1);assertEquals("REUSABLE",read().entries().getFirst().policy().policy());
        candidates.add(original);assertTrue(read().candidatesTruncated());assertNull(read().entries().getFirst().policy().policy());
        var instruction=f.pendingPolicyRepository.instructions(1,from,to).getFirst();doReturn(Collections.nCopies(1001,instruction)).when(f.pendingPolicyRepository).instructions(1,from,to);clearInvocations(f.pendingPolicyRepository);
        var limited=read();assertTrue(limited.selectedTruncated());assertEquals(1000,limited.entries().size());assertTrue(limited.entries().stream().allMatch(e->e.policy().policy()==null));verify(f.pendingPolicyRepository,never()).candidates(anyLong());
    }
    @Test void identityAndHalfOpenSupportedRangeAreEnforced(){
        assertTrue(service.read(f.owner.getEmail(),from.minusSeconds(1),from).entries().isEmpty());
        assertThrows(ResourceNotFoundException.class,()->service.read(null,from,to));assertThrows(ResourceNotFoundException.class,()->service.read("absent",from,to));
        assertThrows(BadRequestException.class,()->service.read(f.owner.getEmail(),to,from));assertThrows(BadRequestException.class,()->service.read(f.owner.getEmail(),from.plusNanos(1),to));assertThrows(BadRequestException.class,()->service.read(f.owner.getEmail(),from,from.plus(Duration.ofDays(367))));
        f.owner.setAccountStatus("SUSPENDED");assertThrows(org.springframework.web.server.ResponseStatusException.class,()->read());
    }
    @Test void candidateReadSharesIndependentReadOnlyRepeatableSnapshot(){
        var source=f.context.getBean(javax.sql.DataSource.class);Object[] resource={null};
        f.write.execute(status->{var outer=TransactionSynchronizationManager.getResource(source);
            doAnswer(call->{resource[0]=TransactionSynchronizationManager.getResource(source);assertNotSame(outer,resource[0]);return call.callRealMethod();}).when(f.pendingPolicyRepository).instructions(1,from,to);
            doAnswer(call->{assertSame(resource[0],TransactionSynchronizationManager.getResource(source));assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());assertEquals(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());return call.callRealMethod();}).when(f.pendingPolicyRepository).candidates(1);
            read();assertSame(outer,TransactionSynchronizationManager.getResource(source));return null;});
        verify(f.pendingPolicyRepository,times(1)).instructions(1,from,to);verify(f.pendingPolicyRepository,times(1)).candidates(1);verify(f.repository,times(1)).coverage(1);
    }
}
