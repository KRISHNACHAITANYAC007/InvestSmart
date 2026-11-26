package com.example.investsmart.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Internal audit projection: amounts are magnitudes, never signed cash deltas or P&L. */
public record SettlementJournalPage(Coverage coverage, int page, int size, List<Entry> entries) {
    public SettlementJournalPage {
        entries = List.copyOf(entries);
    }

    public record Coverage(boolean initialized, Instant initializedAt) {}

    public record Entry(String eventKey, String action, BigDecimal amount,
                        String referenceKind, String referenceId, Instant occurredAt) {}
}
