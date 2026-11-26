package com.example.investsmart.settlement;

import com.example.investsmart.dto.SettlementFundingPage;
import java.math.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Pure interpretation of supplied matched funding evidence; caller owns authentication/snapshot.
 * Does not prove source ownership/history or resolve composite charge siblings. Never aggregate as expenses. */
@Component
public class FundingEffectClassifier {
    public enum Coverage { DIRECT_EFFECT, UNKNOWN_CREDIT_REFUND_DESTINATION, INCOMPLETE, MISMATCH, NOT_EVALUATED }
    public record Effect(Coverage coverage,BigDecimal settledDelta,BigDecimal reservedDelta,
                         BigDecimal settledOriginAmount,BigDecimal creditOriginAmount,String limitation) {}
    private static final BigDecimal ZERO=new BigDecimal("0.00");
    private final JournalActionClassifier actions;
    public FundingEffectClassifier(JournalActionClassifier actions){this.actions=actions;}
    public Effect classify(SettlementFundingPage.Event event){
        if(event==null||event.journal()==null)return empty(Coverage.INCOMPLETE,"MISSING_EVENT");
        var row=event.journal();
        if(!Set.of("RESERVE","SPEND","RELEASE").contains(row.action()==null?"":row.action()))
            return empty(Coverage.NOT_EVALUATED,"Composite charges and other actions require separate evidence; no transaction total.");
        if(event.verification()!=SettlementFundingPage.Verification.MATCHED)
            return empty(event.verification()==SettlementFundingPage.Verification.MISMATCH?Coverage.MISMATCH:Coverage.INCOMPLETE,"FUNDING_EVIDENCE_NOT_MATCHED");
        if(!event.issues().isEmpty()||actions.classify(row.action(),row.amount(),row.referenceKind(),row.referenceId()).kind()==JournalActionClassifier.Kind.INVALID)
            return empty(Coverage.MISMATCH,"INVALID_JOURNAL_OR_CONFLICTING_EVIDENCE");
        if(event.sources().isEmpty())return empty(Coverage.INCOMPLETE,"MISSING_SOURCES");
        if(event.sources().size()>1000)return empty(Coverage.INCOMPLETE,"SOURCE_LIMIT");
        var ids=new HashSet<String>();BigDecimal settled=ZERO,credit=ZERO;
        for(var source:event.sources()){
            if(source==null||source.allocationId()==null||source.allocationId().isBlank()||!ids.add(source.allocationId())
                    ||!money(source.amount())||source.amount().signum()<=0)
                return empty(Coverage.MISMATCH,"INVALID_OR_DUPLICATE_SOURCE");
            if("SETTLED_ORIGIN".equals(source.sourceType())&&("SETTLED".equals(source.sourceKey())||"OPENING_SETTLED".equals(source.sourceKey())))
                settled=settled.add(source.amount().setScale(2));
            else if("CREDIT_ORIGIN".equals(source.sourceType())&&source.sourceKey()!=null&&!source.sourceKey().isBlank()
                    &&!Set.of("SETTLED","OPENING_SETTLED").contains(source.sourceKey())
                    &&"REUSABLE".equals(source.creditPolicy())&&("PENDING".equals(source.currentStatus())||"SETTLED".equals(source.currentStatus())))
                credit=credit.add(source.amount().setScale(2));
            else return empty(Coverage.MISMATCH,"UNRESOLVED_OR_INELIGIBLE_SOURCE");
        }
        if(settled.add(credit).compareTo(row.amount())!=0)return empty(Coverage.MISMATCH,"SOURCE_SUM_MISMATCH");
        var amount=row.amount().setScale(2);
        return switch(row.action()){
            case "RESERVE"->new Effect(Coverage.DIRECT_EFFECT,settled.negate(),amount,settled,credit,"Source-origin split only; not an expense or complete transaction.");
            case "SPEND"->new Effect(Coverage.DIRECT_EFFECT,ZERO,amount.negate(),settled,credit,"Consumes reserved funds; no second settled debit or independent expense classification.");
            case "RELEASE"->new Effect(credit.signum()==0?Coverage.DIRECT_EFFECT:Coverage.UNKNOWN_CREDIT_REFUND_DESTINATION,
                credit.signum()==0?settled:null,amount.negate(),settled,credit,"Credit source current status is not its release-time destination; no historical bucket inference.");
            default->throw new IllegalStateException("Unexpected funding action");
        };
    }
    private static Effect empty(Coverage coverage,String reason){return new Effect(coverage,null,null,null,null,reason);}
    private static boolean money(BigDecimal value){if(value==null)return false;try{return value.setScale(2,RoundingMode.UNNECESSARY).precision()<=19;}catch(ArithmeticException invalid){return false;}}
}
