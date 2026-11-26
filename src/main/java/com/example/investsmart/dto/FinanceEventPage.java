package com.example.investsmart.dto;

import com.example.investsmart.settlement.FinanceEventClassifier.Interpretation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** A page of event evidence, never a complete expense or account statement. */
public record FinanceEventPage(String positionId, int page, int size, List<Entry> entries) {
    public FinanceEventPage { entries = List.copyOf(entries); }
    public record Entry(String eventKey, String eventType, BigDecimal principalDelta,
                        BigDecimal amount, Instant occurredAt, Interpretation interpretation) {}
}
