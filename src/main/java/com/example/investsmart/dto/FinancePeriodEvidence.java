package com.example.investsmart.dto;

import com.example.investsmart.settlement.FinanceEventClassifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Owned event rows across positions; no aggregate interest expense or account balance. */
public record FinancePeriodEvidence(Instant fromInclusive,Instant toExclusive,SettlementJournalPage.Coverage initialization,
        boolean startsBeforeInitialization,boolean completeFinancialHistory,boolean truncated,List<Entry> entries,List<String> limitations){
    public enum Ownership { OWNED_POSITION, POSITION_UNAVAILABLE }
    public record Entry(String eventKey,String positionId,String eventType,BigDecimal principalDelta,BigDecimal amount,
                        Instant occurredAt,Ownership ownership,FinanceEventClassifier.Interpretation interpretation) {}
    public FinancePeriodEvidence{entries=List.copyOf(entries);limitations=List.copyOf(limitations);}
}
