package com.example.investsmart.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
/** Selected direct journal changes only, never account expenses, profit or historical balances. */
public record SelectedJournalChangeSummary(Instant fromInclusive,Instant toExclusive,List<Bucket> buckets){
    public enum Status { VERIFIED_SUPPLIED_CHANGE, UNAVAILABLE }
    public record Bucket(PeriodSummaryEligibility.Scope scope,Status status,BigDecimal change,List<String> reasons){public Bucket{reasons=List.copyOf(reasons);}}
    public SelectedJournalChangeSummary{buckets=List.copyOf(buckets);}
}
