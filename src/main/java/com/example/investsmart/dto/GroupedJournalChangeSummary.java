package com.example.investsmart.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
/** Selected complete groups only; never complete expenses, P&L or account balances. */
public record GroupedJournalChangeSummary(Instant fromInclusive,Instant toExclusive,boolean fullAccountSummaryAvailable,List<Bucket> buckets){
    public enum Scope { SELECTED_GROUPED_SETTLED_CHANGE, SELECTED_GROUPED_RESERVED_CHANGE }
    public enum Status { VERIFIED_SUPPLIED_GROUPED_CHANGE, UNAVAILABLE }
    public record Bucket(Scope scope,Status status,BigDecimal change,List<String> reasons){public Bucket{reasons=List.copyOf(reasons);}}
    public GroupedJournalChangeSummary{buckets=List.copyOf(buckets);}
}
