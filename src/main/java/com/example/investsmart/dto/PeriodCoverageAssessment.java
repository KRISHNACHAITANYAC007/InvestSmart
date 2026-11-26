package com.example.investsmart.dto;

import java.util.List;

/** Diagnostic counts only. Null counts mean unassessed, never zero activity. */
public record PeriodCoverageAssessment(Status status,Integer selectedRows,Integer unsupportedRows,Integer invalidRows,
        Integer missingFundingRows,Integer conflictingFundingRows,Integer unknownRefundRows,Integer groupedCharges,
        Integer ungroupedChargeRows,boolean fullAccountSummaryAvailable,List<Reason> reasons){
    public enum Status { ASSESSED, INPUT_UNAVAILABLE, CONTRADICTORY_EVIDENCE }
    public enum Reason { HISTORY_NOT_CERTIFIED, INPUT_MISSING, INPUT_LIMIT, INVALID_PERIOD, CONTRADICTORY_EVIDENCE,
        ACCOUNT_NOT_INITIALIZED, RANGE_BEFORE_INITIALIZATION, JOURNAL_TRUNCATED, FUNDING_TRUNCATED,
        GROUPING_UNAVAILABLE, GROUPING_INCOMPLETE, UNSUPPORTED_ACTION, INVALID_ACTION, MISSING_FUNDING,
        CONFLICTING_FUNDING, UNKNOWN_REFUND_DESTINATION, UNGROUPED_CHARGE }
    public PeriodCoverageAssessment{reasons=List.copyOf(reasons);}
}
