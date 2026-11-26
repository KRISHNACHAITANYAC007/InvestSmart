package com.example.investsmart.dto;

import java.math.BigDecimal;
import java.util.List;

/** Evidence checks on one journal page, not account-wide cash/P&L reconciliation. */
public record SettlementFundingPage(SettlementJournalPage.Coverage coverage, int page, int size,
                                    List<Event> entries) {
    public SettlementFundingPage { entries = List.copyOf(entries); }

    public enum Verification { MATCHED, MISMATCH, INCOMPLETE, NOT_EVALUATED }

    public record Event(SettlementJournalPage.Entry journal, Verification verification,
                        List<Source> sources, List<String> issues) {
        public Event { sources = List.copyOf(sources); issues = List.copyOf(issues); }
    }

    /** Current source status is not the cash bucket at the time of this event. */
    public record Source(String allocationId, BigDecimal amount, String sourceKey, String sourceType,
                         String creditPolicy, String currentStatus) {}
}
