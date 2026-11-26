package com.example.investsmart.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
/** Selected owned fills and per-ticket checks, never an all-product expense total. */
public record ExecutionFeePeriodEvidence(Instant fromInclusive,Instant toExclusive,SettlementJournalPage.Coverage initialization,
        boolean completeFinancialHistory,boolean selectedTruncated,boolean contextTruncated,List<SelectedFill> selectedFills,
        List<TicketFeeSliceEvidence> tickets,List<String> limitations){
    public record SelectedFill(String fillId,String ticketId,BigDecimal fee,Instant occurredAt) {}
    public ExecutionFeePeriodEvidence{selectedFills=List.copyOf(selectedFills);tickets=List.copyOf(tickets);limitations=List.copyOf(limitations);}
}
