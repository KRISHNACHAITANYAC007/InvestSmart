package com.example.investsmart.dto;
import java.util.List;
/** Eligibility diagnostics only; contains no amounts and grants no ownership or snapshot proof. */
public record PeriodSummaryEligibility(List<Bucket> buckets){
    public enum Scope { SELECTED_JOURNAL_SETTLED_CHANGE, SELECTED_JOURNAL_RESERVED_CHANGE,
        COMPLETE_ACCOUNT_EXPENSES, ACCOUNT_PROFIT_LOSS, HISTORICAL_BALANCES }
    public enum Status { ELIGIBLE_SUPPLIED_DIRECT_ROWS, UNAVAILABLE }
    public enum Reason { SUPPLIED_JOURNAL_ONLY, HISTORY_NOT_CERTIFIED, OTHER_PRODUCT_COVERAGE_UNVERIFIED,
        PORTFOLIO_METHOD_NOT_RECONCILED, INPUT_UNAVAILABLE, CONTRADICTORY_EVIDENCE, STALE_ASSESSMENT,
        ACCOUNT_NOT_INITIALIZED, BEFORE_INITIALIZATION, JOURNAL_LIMIT, FUNDING_LIMIT, GROUPING_UNAVAILABLE,
        UNSUPPORTED_ACTION, INVALID_ACTION, MISSING_FUNDING, CONFLICTING_FUNDING, UNKNOWN_REFUND_DESTINATION,
        COMPOSITE_EFFECT_OUTSIDE_DIRECT_SCOPE }
    public record Bucket(Scope scope,Status status,List<Reason> reasons){public Bucket{reasons=List.copyOf(reasons);}}
    public PeriodSummaryEligibility{buckets=List.copyOf(buckets);}
}
