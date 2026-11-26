package com.example.investsmart.dto;
import java.time.Instant;
import java.util.List;
/** Original policy evidence only; no usable cash, refund destination or complete history. */
public record PendingPolicyPeriodEvidence(Instant fromInclusive,Instant toExclusive,SettlementJournalPage.Coverage initialization,
        boolean completeFinancialHistory,boolean selectedTruncated,boolean candidatesTruncated,List<Entry> entries,List<String> limitations){
    public record Entry(SettlementJournalPage.Entry journal,PendingCreditPolicyEvidence policy) {}
    public PendingPolicyPeriodEvidence{entries=List.copyOf(entries);limitations=List.copyOf(limitations);}
}
