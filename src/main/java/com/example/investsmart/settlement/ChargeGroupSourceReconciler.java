package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import java.util.List;
import org.springframework.stereotype.Component;
import static com.example.investsmart.dto.ChargeGroupSourceEvidence.Status.*;

/** Caller supplies authorized evidence from one snapshot; whole-period partition remains a separate check. */
@Component
public class ChargeGroupSourceReconciler {
    private final PeriodChargeGrouper grouper;
    private final ChargeEvidenceReconciler charges;
    public ChargeGroupSourceReconciler(PeriodChargeGrouper grouper,ChargeEvidenceReconciler charges){this.grouper=grouper;this.charges=charges;}
    public ChargeGroupSourceEvidence reconcile(GroupedChargeEvidence.Group group,ChargeEvidence proof,boolean limited){
        if(group==null||proof==null||limited)return unavailable(INCOMPLETE,"GROUP_SOURCE_UNAVAILABLE_OR_LIMITED");
        if(group.kind()==GroupedChargeEvidence.Kind.UNGROUPED)return unavailable(NOT_EVALUATED,"UNGROUPED_ROW");
        if(group.kind()!=GroupedChargeEvidence.Kind.VERIFIED_CHARGE||group.selectedRows().isEmpty()||group.selectedRows().size()>3||group.outsideSelectionChildKeys().size()>2)return unavailable(MISMATCH,"INVALID_GROUP_SHAPE");
        if(proof.childrenTruncated()||proof.fundingTruncated())return unavailable(INCOMPLETE,"SOURCE_PROOF_LIMITED");
        try{
            var checked=charges.reconcile(proof.parent(),proof.children(),false);
            if(checked.status()!=ChargeEvidenceReconciler.Status.MATCHED_SUPPLIED_PAIR)
                return unavailable(checked.status()==ChargeEvidenceReconciler.Status.INCOMPLETE?INCOMPLETE:MISMATCH,"SOURCE_PROOF_NOT_MATCHED");
            if(!checked.equals(proof.comparison()))return unavailable(MISMATCH,"STALE_SOURCE_COMPARISON");
            // Re-run grouping against the exact selected rows: absent children must be exactly the context keys.
            var regrouped=grouper.group(group.selectedRows(),List.of(proof),false);
            if(regrouped.status()!=GroupedChargeEvidence.Status.GROUPED_SUPPLIED_ROWS||regrouped.groups().size()!=1||!regrouped.groups().getFirst().equals(group))return unavailable(MISMATCH,"GROUP_SOURCE_PARTITION_CONFLICT");
            return new ChargeGroupSourceEvidence(MATCHED_SUPPLIED_GROUP_SOURCE,"SUPPLIED_SOURCE_CONTEXT_ONLY_NO_PERIOD_CASH_TOTAL",group,proof);
        }catch(NullPointerException|IllegalArgumentException malformed){return unavailable(MISMATCH,"MALFORMED_GROUP_SOURCE");}
    }
    private static ChargeGroupSourceEvidence unavailable(ChargeGroupSourceEvidence.Status status,String reason){return new ChargeGroupSourceEvidence(status,reason,null,null);}
}
