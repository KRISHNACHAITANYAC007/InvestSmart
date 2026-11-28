package com.example.investsmart.reports;

import java.time.Instant;
import java.util.List;

/** Explicit external allowlist. Ordinals are local to this response, never historical cursors. */
public record JournalPeriodReport(Instant fromInclusive,Instant toExclusive,Coverage coverage,Limits limits,
        List<Row> rows,List<Change> directChanges,List<Change> groupedChanges,List<Unavailable> unavailableScopes) {
    public JournalPeriodReport {rows=List.copyOf(rows);directChanges=List.copyOf(directChanges);groupedChanges=List.copyOf(groupedChanges);unavailableScopes=List.copyOf(unavailableScopes);}
    public record Coverage(boolean initialized,Instant initializedAt,boolean startsBeforeInitialization,boolean completeFinancialHistory) {}
    public record Limits(int selectedRowLimit,boolean rowsTruncated,boolean fundingEvidenceTruncated) {}
    public record Row(int ordinal,Instant occurredAt,String action,String amount,String interpretationStatus,List<String> reasonCodes){public Row{reasonCodes=List.copyOf(reasonCodes);}}
    public record Change(String scope,String status,String change,List<String> reasonCodes){public Change{reasonCodes=List.copyOf(reasonCodes);}}
    public record Unavailable(String scope,String status,List<String> reasonCodes){public Unavailable{reasonCodes=List.copyOf(reasonCodes);}}
}
