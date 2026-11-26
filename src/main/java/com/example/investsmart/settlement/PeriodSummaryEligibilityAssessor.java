package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.example.investsmart.dto.PeriodSummaryEligibility.*;

/** Rechecks one supplied journal period; never combines independently read DTOs or sums money. */
@Component
public class PeriodSummaryEligibilityAssessor {
    private final PeriodCoverageAssessor coverage;
    public PeriodSummaryEligibilityAssessor(PeriodCoverageAssessor coverage){this.coverage=coverage;}
    public PeriodSummaryEligibility assess(JournalPeriodEvidence period){
        var blocked=EnumSet.noneOf(Reason.class);PeriodCoverageAssessment checked=null;
        try{checked=coverage.assess(period);}catch(NullPointerException|IllegalArgumentException malformed){blocked.add(Reason.CONTRADICTORY_EVIDENCE);}
        if(checked==null&&blocked.isEmpty())blocked.add(Reason.INPUT_UNAVAILABLE);
        if(checked!=null){
            if(checked.status()==PeriodCoverageAssessment.Status.INPUT_UNAVAILABLE)blocked.add(Reason.INPUT_UNAVAILABLE);
            if(checked.status()==PeriodCoverageAssessment.Status.CONTRADICTORY_EVIDENCE)blocked.add(Reason.CONTRADICTORY_EVIDENCE);
            if(period!=null&&period.assessment()!=null&&!checked.equals(period.assessment()))blocked.add(Reason.STALE_ASSESSMENT);
            for(var reason:checked.reasons())switch(reason){
                case ACCOUNT_NOT_INITIALIZED->blocked.add(Reason.ACCOUNT_NOT_INITIALIZED);
                case RANGE_BEFORE_INITIALIZATION->blocked.add(Reason.BEFORE_INITIALIZATION);
                case JOURNAL_TRUNCATED->blocked.add(Reason.JOURNAL_LIMIT);
                case FUNDING_TRUNCATED->blocked.add(Reason.FUNDING_LIMIT);
                case GROUPING_UNAVAILABLE,GROUPING_INCOMPLETE->blocked.add(Reason.GROUPING_UNAVAILABLE);
                case UNSUPPORTED_ACTION->blocked.add(Reason.UNSUPPORTED_ACTION);
                case INVALID_ACTION->blocked.add(Reason.INVALID_ACTION);
                case MISSING_FUNDING->blocked.add(Reason.MISSING_FUNDING);
                case CONFLICTING_FUNDING->blocked.add(Reason.CONFLICTING_FUNDING);
                case UNGROUPED_CHARGE->blocked.add(Reason.COMPOSITE_EFFECT_OUTSIDE_DIRECT_SCOPE);
                default->{} // History and refund-destination restrictions apply to distinct scopes below.
            }
            // Grouping proves identity partition, but its DTO does not carry full composite cash-effect proof.
            if(period!=null&&period.entries().stream().anyMatch(e->e!=null&&e.raw()!=null&&("REALIZED_CHARGE".equals(e.raw().action())||"CHARGE".equals(e.raw().referenceKind()))))blocked.add(Reason.COMPOSITE_EFFECT_OUTSIDE_DIRECT_SCOPE);
        }
        var settled=blocked.clone();var reserved=blocked.clone();
        if(checked!=null&&checked.reasons().contains(PeriodCoverageAssessment.Reason.UNKNOWN_REFUND_DESTINATION))settled.add(Reason.UNKNOWN_REFUND_DESTINATION);
        return new PeriodSummaryEligibility(List.of(direct(Scope.SELECTED_JOURNAL_SETTLED_CHANGE,settled),direct(Scope.SELECTED_JOURNAL_RESERVED_CHANGE,reserved),
            unavailable(Scope.COMPLETE_ACCOUNT_EXPENSES,Reason.HISTORY_NOT_CERTIFIED,Reason.OTHER_PRODUCT_COVERAGE_UNVERIFIED),
            unavailable(Scope.ACCOUNT_PROFIT_LOSS,Reason.HISTORY_NOT_CERTIFIED,Reason.PORTFOLIO_METHOD_NOT_RECONCILED),
            unavailable(Scope.HISTORICAL_BALANCES,Reason.HISTORY_NOT_CERTIFIED)));
    }
    private static Bucket direct(Scope scope,EnumSet<Reason> blockers){var reasons=EnumSet.of(Reason.SUPPLIED_JOURNAL_ONLY);reasons.addAll(blockers);return new Bucket(scope,blockers.isEmpty()?Status.ELIGIBLE_SUPPLIED_DIRECT_ROWS:Status.UNAVAILABLE,new ArrayList<>(reasons));}
    private static Bucket unavailable(Scope scope,Reason... reasons){return new Bucket(scope,Status.UNAVAILABLE,List.of(reasons));}
}
