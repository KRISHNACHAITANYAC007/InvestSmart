package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.investsmart.dto.PendingCreditPolicyEvidence.Status.*;

class PendingCreditPolicyReconcilerTests {
    final PendingCreditPolicyReconciler rules=new PendingCreditPolicyReconciler();
    SettlementFundsTests writer;String id;
    @BeforeEach void setup(){writer=new SettlementFundsTests();writer.setup();id=writer.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);}
    SettlementJournalPage.Entry row(){return writer.db.queryForObject("SELECT * FROM settlement_journal WHERE event_key='sale'",(r,n)->new SettlementJournalPage.Entry(r.getString("event_key"),r.getString("action"),r.getBigDecimal("amount"),r.getString("reference_kind"),r.getString("reference_id"),r.getTimestamp("occurred_at").toInstant()));}
    String fingerprint(){return writer.db.queryForObject("SELECT fingerprint FROM settlement_journal WHERE event_key='sale'",String.class);}
    PendingCreditPolicyReconciler.Candidate candidate(){return writer.db.queryForObject("SELECT * FROM settlement_obligations WHERE id=?",(r,n)->new PendingCreditPolicyReconciler.Candidate(r.getString("id"),r.getString("source_key"),r.getString("exchange_code"),r.getString("segment_code"),r.getDate("trade_date").toLocalDate(),r.getString("obligation_kind"),r.getString("credit_policy"),r.getBigDecimal("amount")),id);}
    PendingCreditPolicyEvidence read(){return rules.reconcile(row(),fingerprint(),List.of(candidate()),false);}
    PendingCreditPolicyReconciler.Candidate copy(String key,String source){var c=candidate();return new PendingCreditPolicyReconciler.Candidate(key,source,c.exchange(),c.segment(),c.tradeDate(),c.kind(),c.policy(),c.originalAmount());}
    @Test void matchesBothActualWriterPoliciesWithoutModifyingRecords(){
        for(var policy:SettlementFunds.CreditPolicy.values()){
            writer.db.update("DELETE FROM settlement_journal WHERE event_key='sale'");writer.db.update("DELETE FROM settlement_obligations WHERE id=?",id);
            id=writer.pending("sale","30",policy);
            var before=writer.db.queryForList("SELECT * FROM settlement_obligations ORDER BY id");var journal=writer.db.queryForList("SELECT * FROM settlement_journal ORDER BY event_key");var balance=writer.balances();
            var result=read();assertEquals(MATCHED_SUPPLIED_INSTRUCTION,result.status());assertEquals(policy.name(),result.policy());assertEquals(id,result.obligationId());
            assertEquals(before,writer.db.queryForList("SELECT * FROM settlement_obligations ORDER BY id"));assertEquals(journal,writer.db.queryForList("SELECT * FROM settlement_journal ORDER BY event_key"));assertEquals(balance,writer.balances());
        }
    }
    @Test void currentRemainingAndStatusAreNotOriginalInstructionOrRefundProof(){
        writer.reserve("reserve","10");writer.time(SettlementFundsTests.CUTOFF);assertTrue(writer.get(()->writer.funds.settle(1,id)));
        var result=read();assertEquals(MATCHED_SUPPLIED_INSTRUCTION,result.status());assertEquals("REUSABLE",result.policy());
        assertTrue(result.reason().contains("NO_REFUND_OR_AVAILABILITY_PROOF"));
    }
    @Test void changedRetainedMetadataCannotBeTreatedAsOriginalPolicy(){
        var original=candidate();
        for(String change:List.of("credit_policy='RESTRICTED'","source_key='unrelated'","exchange_code='BSE'","segment_code='FNO'","trade_date='2026-11-10'","amount=31")){
            writer.db.update("UPDATE settlement_obligations SET "+change+" WHERE id=?",id);
            var result=read();assertEquals(INCOMPLETE,result.status());assertNull(result.policy());assertNull(result.obligationId());
            writer.db.update("UPDATE settlement_obligations SET credit_policy=?,source_key=?,exchange_code=?,segment_code=?,trade_date=?,amount=? WHERE id=?",original.policy(),original.source(),original.exchange(),original.segment(),java.sql.Date.valueOf(original.tradeDate()),original.originalAmount(),id);
        }
    }
    @Test void missingLimitedDuplicateAndAmbiguousCandidatesWithholdIdentity(){
        assertEquals(INCOMPLETE,rules.reconcile(row(),null,List.of(candidate()),false).status());
        assertEquals(INCOMPLETE,rules.reconcile(row(),fingerprint(),List.of(),false).status());
        assertEquals(INCOMPLETE,rules.reconcile(row(),fingerprint(),List.of(candidate()),true).status());
        assertEquals(MISMATCH,rules.reconcile(row(),fingerprint(),List.of(candidate(),candidate()),false).status());
        var ambiguous=rules.reconcile(row(),fingerprint(),List.of(candidate(),copy("different-id","sale")),false);
        assertEquals(MISMATCH,ambiguous.status());assertNull(ambiguous.policy());assertNull(ambiguous.obligationId());
    }
    @Test void thousandCandidateBoundRetainsOneMatchAndSentinelWithholds(){
        var candidates=new ArrayList<PendingCreditPolicyReconciler.Candidate>();for(int i=0;i<999;i++)candidates.add(copy("id"+i,"source"+i));candidates.add(candidate());
        assertEquals(MATCHED_SUPPLIED_INSTRUCTION,rules.reconcile(row(),fingerprint(),candidates,false).status());
        candidates.add(copy("sentinel","source1000"));assertEquals(INCOMPLETE,rules.reconcile(row(),fingerprint(),candidates,false).status());
    }
    @Test void invalidFingerprintMoneyReferencesAndOtherActionsNeverMatch(){
        assertEquals(MISMATCH,rules.reconcile(row(),"x".repeat(64),List.of(candidate()),false).status());
        var r=row();for(var amount:List.of(new BigDecimal("0"),new BigDecimal("-1"),new BigDecimal("0.001"))){
            assertEquals(MISMATCH,rules.reconcile(new SettlementJournalPage.Entry(r.eventKey(),r.action(),amount,null,null,r.occurredAt()),fingerprint(),List.of(candidate()),false).status());
        }
        assertEquals(MISMATCH,rules.reconcile(new SettlementJournalPage.Entry(r.eventKey(),r.action(),r.amount(),"ORDER","order",r.occurredAt()),fingerprint(),List.of(candidate()),false).status());
        assertEquals(NOT_EVALUATED,rules.reconcile(new SettlementJournalPage.Entry(r.eventKey(),"RELEASE",r.amount(),null,null,r.occurredAt()),fingerprint(),List.of(candidate()),false).status());
    }
    @Test void exactWriterEncodingHandlesUnicodeAndColonSourceWithoutEventPrefixInference(){
        writer.db.update("DELETE FROM settlement_journal WHERE event_key='sale'");writer.db.update("DELETE FROM settlement_obligations WHERE id=?",id);
        id=writer.get(()->writer.funds.creditPending(1,"sale","source:雪😀","NSE","EQUITY",SettlementFundsTests.TRADE,new BigDecimal("30.00"),SettlementFunds.CreditPolicy.RESTRICTED));
        assertEquals(MATCHED_SUPPLIED_INSTRUCTION,read().status());assertEquals("RESTRICTED",read().policy());
    }
}
