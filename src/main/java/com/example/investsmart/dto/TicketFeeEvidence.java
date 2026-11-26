package com.example.investsmart.dto;

import java.math.BigDecimal;

/** One owned ticket only; detail and aggregate describe the same fee, never two expenses. */
public record TicketFeeEvidence(String ticketId, Verification verification, String reason,
                                BigDecimal recordedFee, BigDecimal detailFee, BigDecimal reconciledFee) {
    public enum Verification { MATCHED, MISMATCH, INCOMPLETE }
}
