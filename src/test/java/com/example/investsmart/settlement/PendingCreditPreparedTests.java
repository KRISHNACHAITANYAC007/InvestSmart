package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
class PendingCreditPreparedTests {
    PendingCreditPolicyReconcilerTests f;PendingCreditPolicyReconciler rules;
    @BeforeEach void setup(){f=new PendingCreditPolicyReconcilerTests();f.setup();rules=f.rules;}
    @Test void preparedMatchesSingleRowContractAcrossHeadersEvidenceAndLimits(){
        var r=f.row();var rows=Arrays.asList(null,r,new SettlementJournalPage.Entry("other","RELEASE",r.amount(),null,null,r.occurredAt()),new SettlementJournalPage.Entry("bad",r.action(),BigDecimal.ZERO,null,null,r.occurredAt()),new SettlementJournalPage.Entry("bad",r.action(),new BigDecimal("0.001"),null,null,r.occurredAt()),new SettlementJournalPage.Entry("bad",r.action(),r.amount(),"ORDER","order",r.occurredAt()));
        var evidence=Arrays.asList(null,List.<PendingCreditPolicyReconciler.Candidate>of(),List.of(f.candidate()),List.of(f.candidate(),f.candidate()),List.of(f.candidate(),f.copy("other-id","sale")),Arrays.asList(f.candidate(),null),Collections.nCopies(1001,f.candidate()));
        for(boolean limited:List.of(false,true))for(var candidates:evidence){var prepared=rules.prepare(candidates,limited);for(var row:rows)for(var fingerprint:Arrays.asList(null,"invalid",f.fingerprint(),"0".repeat(64)))assertEquals(rules.reconcile(row,fingerprint,candidates,limited),prepared.reconcile(row,fingerprint));}
    }
    @Test void preparedCapturesImmutableRequestLocalCandidateSnapshot(){
        var input=new ArrayList<>(List.of(f.candidate()));var prepared=rules.prepare(input,false);var expected=prepared.reconcile(f.row(),f.fingerprint());input.clear();input.add(f.copy("different-snapshot-id","sale"));
        assertEquals(expected,prepared.reconcile(f.row(),f.fingerprint()));assertEquals("different-snapshot-id",rules.prepare(input,false).reconcile(f.row(),f.fingerprint()).obligationId());assertNotEquals(expected.obligationId(),rules.prepare(input,false).reconcile(f.row(),f.fingerprint()).obligationId());
    }
    @Test void thousandCandidateIndexPreservesMatchAndSentinelAvailability(){
        var input=new ArrayList<PendingCreditPolicyReconciler.Candidate>();for(int i=0;i<999;i++)input.add(f.copy("id"+i,"source"+i));input.add(f.candidate());var prepared=rules.prepare(input,false);var expected=rules.reconcile(f.row(),f.fingerprint(),input,false);
        for(int i=0;i<100;i++)assertEquals(expected,prepared.reconcile(f.row(),f.fingerprint()));input.add(f.copy("sentinel","extra"));assertEquals(PendingCreditPolicyEvidence.Status.INCOMPLETE,rules.prepare(input,false).reconcile(f.row(),f.fingerprint()).status());
    }
    @Test void invalidUnrelatedCandidateCannotBeIgnoredByLookup(){
        var original=f.candidate();var invalid=new PendingCreditPolicyReconciler.Candidate("unrelated",original.source(),original.exchange(),original.segment(),original.tradeDate(),original.kind(),"INVALID",original.originalAmount());var prepared=rules.prepare(List.of(original,invalid),false);assertEquals("INVALID_OR_DUPLICATE_CANDIDATE",prepared.reconcile(f.row(),f.fingerprint()).reason());assertEquals("INVALID_FINGERPRINT",prepared.reconcile(f.row(),"bad").reason());assertEquals("MISSING_JOURNAL",prepared.reconcile(null,f.fingerprint()).reason());
    }
    @Test void normalizedMoneyUnicodeAndAmbiguityRetainExactProof(){
        f.exactWriterEncodingHandlesUnicodeAndColonSourceWithoutEventPrefixInference();var r=f.row();var scaled=new SettlementJournalPage.Entry(r.eventKey(),r.action(),r.amount().stripTrailingZeros(),null,null,r.occurredAt());assertEquals(rules.reconcile(scaled,f.fingerprint(),List.of(f.candidate()),false),rules.prepare(List.of(f.candidate()),false).reconcile(scaled,f.fingerprint()));
        var c=f.candidate();var duplicate=new PendingCreditPolicyReconciler.Candidate("other",c.source(),c.exchange(),c.segment(),c.tradeDate(),c.kind(),c.policy(),c.originalAmount().setScale(3));assertEquals(PendingCreditPolicyEvidence.Status.MISMATCH,rules.prepare(List.of(c,duplicate),false).reconcile(r,f.fingerprint()).status());
    }
}
