package com.example.investsmart.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** One supplied ticket's verified fill-fee slice, not a complete account/product expense total. */
public record TicketFeeSliceEvidence(String ticketId,Status status,Integer selectedFillCount,Integer outsidePeriodFillCount,
        BigDecimal selectedFee,List<SelectedFill> selectedFills,String limitation){
    public enum Status { MATCHED_TICKET_SLICE, INCOMPLETE, MISMATCH }
    public record SelectedFill(String fillId,BigDecimal fee,Instant occurredAt) {}
    public TicketFeeSliceEvidence{selectedFills=List.copyOf(selectedFills);}
}
