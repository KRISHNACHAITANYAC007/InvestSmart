package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.example.investsmart.dto.PendingCreditPolicyEvidence.Status.*;

/** Caller must authorize candidates and supply the original journal fingerprint in one snapshot. */
@Component
public class PendingCreditPolicyReconciler {
    public record Candidate(String id,String source,String exchange,String segment,LocalDate tradeDate,
                            String kind,String policy,BigDecimal originalAmount) {}
    /** Immutable request-local lookup; never store it globally or share across user snapshots. */
    public final class Prepared {
        private final boolean unavailable,invalid;
        private final Map<String,List<Candidate>> byFingerprint;
        private Prepared(boolean unavailable,boolean invalid,Map<String,List<Candidate>> index){this.unavailable=unavailable;this.invalid=invalid;this.byFingerprint=Map.copyOf(index);}
        public PendingCreditPolicyEvidence reconcile(SettlementJournalPage.Entry row,String fingerprint){
            // Reuse the single-row contract for exact header/error precedence; empty evidence is intentional.
            var header=PendingCreditPolicyReconciler.this.reconcile(row,fingerprint,unavailable?null:List.of(),unavailable);
            if(!"NO_RETAINED_INSTRUCTION_MATCH".equals(header.reason()))return header;
            if(invalid)return empty(MISMATCH,"INVALID_OR_DUPLICATE_CANDIDATE");
            return PendingCreditPolicyReconciler.this.reconcile(row,fingerprint,byFingerprint.getOrDefault(fingerprint,List.of()),false);
        }
    }
    public Prepared prepare(List<Candidate> candidates,boolean limited){
        if(candidates==null||limited||candidates.size()>1000)return new Prepared(true,false,Map.of());
        var ids=new HashSet<String>();var index=new HashMap<String,List<Candidate>>();
        for(var candidate:candidates){
            if(!valid(candidate)||!ids.add(candidate.id()))return new Prepared(false,true,Map.of());
            index.computeIfAbsent(fingerprint(candidate.originalAmount(),candidate),key->new ArrayList<>()).add(candidate);
        }
        var frozen=new HashMap<String,List<Candidate>>();index.forEach((key,value)->frozen.put(key,List.copyOf(value)));
        return new Prepared(false,false,frozen);
    }
    public PendingCreditPolicyEvidence reconcile(SettlementJournalPage.Entry row,String fingerprint,
                                                 List<Candidate> candidates,boolean limited){
        if(row==null)return empty(INCOMPLETE,"MISSING_JOURNAL");
        if(!"PENDING_CREDIT".equals(row.action()))return empty(NOT_EVALUATED,"NOT_PENDING_CREDIT");
        if(!text(row.eventKey(),160)||row.referenceKind()!=null||row.referenceId()!=null||!money(row.amount()))
            return empty(MISMATCH,"INVALID_PENDING_JOURNAL");
        if(fingerprint==null||candidates==null||limited||candidates.size()>1000)return empty(INCOMPLETE,"INSTRUCTION_EVIDENCE_UNAVAILABLE_OR_LIMITED");
        if(!fingerprint.matches("[0-9a-f]{64}"))return empty(MISMATCH,"INVALID_FINGERPRINT");
        var ids=new HashSet<String>();var matches=new ArrayList<Candidate>();
        for(var candidate:candidates){
            if(!valid(candidate)||!ids.add(candidate.id()))
                return empty(MISMATCH,"INVALID_OR_DUPLICATE_CANDIDATE");
            if(candidate.originalAmount().compareTo(row.amount())==0&&fingerprint.equals(fingerprint(row.amount(),candidate)))matches.add(candidate);
        }
        if(matches.isEmpty())return empty(INCOMPLETE,"NO_RETAINED_INSTRUCTION_MATCH");
        if(matches.size()!=1)return empty(MISMATCH,"AMBIGUOUS_INSTRUCTION_MATCH");
        var match=matches.getFirst();
        return new PendingCreditPolicyEvidence(MATCHED_SUPPLIED_INSTRUCTION,"ORIGINAL_INSTRUCTION_ONLY_NO_REFUND_OR_AVAILABILITY_PROOF",match.id(),match.policy());
    }
    private static boolean valid(Candidate candidate){
        return candidate!=null&&text(candidate.id(),36)&&text(candidate.source(),160)
            &&Set.of("NSE","BSE","MCX").contains(candidate.exchange()==null?"":candidate.exchange())
            &&Set.of("EQUITY","FNO","CURRENCY","COMMODITY","MUTUAL_FUND","BOND").contains(candidate.segment()==null?"":candidate.segment())
            &&candidate.tradeDate()!=null&&"CASH_CREDIT".equals(candidate.kind())
            &&Set.of("REUSABLE","RESTRICTED").contains(candidate.policy()==null?"":candidate.policy())&&money(candidate.originalAmount());
    }
    private static String fingerprint(BigDecimal amount,Candidate candidate){
        // Mirrors SettlementFunds.journal's length-prefixed UTF-8 canonical form; never decodes a hash.
        String[] parts={"PENDING_CREDIT",amount.setScale(2).toPlainString(),null,null,candidate.source(),candidate.exchange(),candidate.segment(),candidate.tradeDate().toString(),candidate.policy()};
        var canonical=new StringBuilder();for(var part:parts)canonical.append(part==null?"-1:":part.length()+":"+part);
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static boolean text(String value,int max){return value!=null&&!value.isBlank()&&value.length()<=max&&value.chars().noneMatch(Character::isISOControl);}
    private static boolean money(BigDecimal amount){if(amount==null||amount.signum()<=0)return false;try{return amount.setScale(2,RoundingMode.UNNECESSARY).precision()<=19;}catch(ArithmeticException invalid){return false;}}
    private static PendingCreditPolicyEvidence empty(PendingCreditPolicyEvidence.Status status,String reason){return new PendingCreditPolicyEvidence(status,reason,null,null);}
}
