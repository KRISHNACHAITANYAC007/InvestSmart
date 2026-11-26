package com.example.investsmart.dto;

/** MATCHED_DUPLICATE identifies two representations of one trade, not two trades to sum. */
public record EquityLegacyEvidence(String fillId, Verification verification, String reason, Long duplicateOrderId) {
    public enum Verification { MATCHED_DUPLICATE, INCOMPLETE, MISMATCH, NOT_EVALUATED }
}
