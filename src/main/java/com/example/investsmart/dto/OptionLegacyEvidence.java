package com.example.investsmart.dto;

/** Two representations of one executed option trade, not two trades to sum. */
public record OptionLegacyEvidence(String fillId, Verification verification, String reason, String duplicateOptionOrderId) {
    public enum Verification { MATCHED_DUPLICATE, INCOMPLETE, MISMATCH, NOT_EVALUATED }
}
