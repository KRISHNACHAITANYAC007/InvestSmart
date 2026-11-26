package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Component;

/** Supplied owned/same-snapshot evidence only. Not a database reader or account expense total. */
@Component
public class ChargeEvidenceReconciler {
    public enum Status { MATCHED_SUPPLIED_PAIR, INCOMPLETE, MISMATCH, NOT_EVALUATED }
    public record Result(Status status,BigDecimal chargeAmount,BigDecimal fundedAmount,
                         BigDecimal impliedUnfundedAmount,BigDecimal settledEffect,
                         BigDecimal creditOriginConsumption,List<String> childKeys,String limitation){
        public Result{childKeys=List.copyOf(childKeys);}
    }
    private final JournalActionClassifier actions;
    private final FundingEffectClassifier funding;
    public ChargeEvidenceReconciler(JournalActionClassifier actions,FundingEffectClassifier funding){this.actions=actions;this.funding=funding;}
    public Result reconcile(SettlementJournalPage.Entry parent,List<SettlementFundingPage.Event> children,boolean truncated){
        if(parent==null)return empty(Status.INCOMPLETE,"MISSING_PARENT");
        if(!"REALIZED_CHARGE".equals(parent.action()))return empty(Status.NOT_EVALUATED,"NOT_COMPOSITE_CHARGE");
        if(parent.eventKey()==null||parent.eventKey().isBlank()||parent.eventKey().length()>160||parent.eventKey().chars().anyMatch(Character::isISOControl)
                ||actions.classify(parent.action(),parent.amount(),parent.referenceKind(),parent.referenceId()).kind()!=JournalActionClassifier.Kind.COMPOSITE_CHARGE)
            return empty(Status.MISMATCH,"INVALID_PARENT");
        if(children==null||truncated||children.size()>1000)return empty(Status.INCOMPLETE,"CHILD_EVIDENCE_UNAVAILABLE_OR_LIMITED");
        // No children can mean missing history, not proof that the whole charge became debt.
        if(children.size()<2)return empty(Status.INCOMPLETE,"MISSING_CHARGE_CHILDREN");
        if(children.size()!=2)return empty(Status.MISMATCH,"AMBIGUOUS_CHARGE_CHILDREN");
        String reference=reference(parent.eventKey());
        SettlementFundingPage.Event reserve=null,spend=null;
        for(var child:children){
            if(child==null||child.journal()==null)return empty(Status.INCOMPLETE,"MISSING_CHILD");
            var row=child.journal();
            if(!"CHARGE".equals(row.referenceKind())||!reference.equals(row.referenceId()))return empty(Status.MISMATCH,"CHILD_REFERENCE_CONFLICT");
            if("RESERVE".equals(row.action())&&("CHARGE_RESERVE:"+reference).equals(row.eventKey())&&reserve==null)reserve=child;
            else if("SPEND".equals(row.action())&&("CHARGE_SPEND:"+reference).equals(row.eventKey())&&spend==null)spend=child;
            else return empty(Status.MISMATCH,"CHILD_KEY_OR_ACTION_CONFLICT");
        }
        if(reserve==null||spend==null)return empty(Status.INCOMPLETE,"MISSING_CHARGE_PAIR");
        var reserved=funding.classify(reserve);var spent=funding.classify(spend);
        if(reserved.coverage()==FundingEffectClassifier.Coverage.MISMATCH||spent.coverage()==FundingEffectClassifier.Coverage.MISMATCH)
            return empty(Status.MISMATCH,"CHILD_SOURCE_CONFLICT");
        if(reserved.coverage()!=FundingEffectClassifier.Coverage.DIRECT_EFFECT||spent.coverage()!=FundingEffectClassifier.Coverage.DIRECT_EFFECT)
            return empty(Status.INCOMPLETE,"CHILD_SOURCE_NOT_VERIFIED");
        var paid=reserve.journal().amount().setScale(2);
        if(paid.compareTo(spend.journal().amount())!=0||paid.compareTo(parent.amount())>0||!normalized(reserve.sources()).equals(normalized(spend.sources())))
            return empty(Status.MISMATCH,"CHARGE_FUNDING_PAIR_CONFLICT");
        var amount=parent.amount().setScale(2);var remainder=amount.subtract(paid);
        return new Result(Status.MATCHED_SUPPLIED_PAIR,amount,paid,remainder,
            reserved.settledOriginAmount().add(remainder).negate(),reserved.creditOriginAmount(),
            List.of(reserve.journal().eventKey(),spend.journal().eventKey()),
            "Writer-rule implication from supplied pair only; not independently verified debt, complete history or an expense total. Count neither child again.");
    }
    private Map<String,SettlementFundingPage.Source> normalized(List<SettlementFundingPage.Source> sources){
        var result=new HashMap<String,SettlementFundingPage.Source>();
        for(var source:sources)result.put(source.allocationId(),new SettlementFundingPage.Source(source.allocationId(),source.amount().setScale(2),source.sourceKey(),source.sourceType(),source.creditPolicy(),source.currentStatus()));
        return result;
    }
    static String reference(String parentKey){return UUID.nameUUIDFromBytes(parentKey.getBytes(StandardCharsets.UTF_8)).toString();}
    private static Result empty(Status status,String reason){return new Result(status,null,null,null,null,null,List.of(),reason);}
}
