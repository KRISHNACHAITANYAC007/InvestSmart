package com.example.investsmart.dto;

/** Original instruction match only, never current availability or historical refund destination. */
public record PendingCreditPolicyEvidence(Status status, String reason, String obligationId, String policy) {
    public enum Status { MATCHED_SUPPLIED_INSTRUCTION, INCOMPLETE, MISMATCH, NOT_EVALUATED }
}
