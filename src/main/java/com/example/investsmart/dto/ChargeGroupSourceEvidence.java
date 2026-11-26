package com.example.investsmart.dto;
/** Validated supplied group/source binding only; comparison amounts are context, not period totals. */
public record ChargeGroupSourceEvidence(Status status,String reason,GroupedChargeEvidence.Group group,ChargeEvidence sourceProof){
    public enum Status { MATCHED_SUPPLIED_GROUP_SOURCE, INCOMPLETE, MISMATCH, NOT_EVALUATED }
}
