package com.example.investsmart.dto;

/** Supplied MIS linkage only; no ownership, position P&L or account completeness proof. */
public record IntradayTicketSliceEvidence(Status status, String reason, TicketFeeSliceEvidence slice) {
    public enum Status { MATCHED_EVENT_TICKET_SLICE, INCOMPLETE, MISMATCH }
}
