package com.example.investsmart.settlement;

import com.example.investsmart.dto.SettlementFundingPage;
import com.example.investsmart.dto.SettlementJournalPage;
import java.math.BigDecimal;
import java.util.*;
import static com.example.investsmart.dto.SettlementFundingPage.Verification.*;

/** Shared source-evidence rules; caller supplies owned evidence from one snapshot. */
final class FundingEvidenceChecker {
    static final Set<String> FUNDING=Set.of("RESERVE","SPEND","RELEASE");
    private FundingEvidenceChecker() {}
    static SettlementFundingPage.Event check(SettlementJournalPage.Entry entry,
                                               List<SettlementFundingRepository.Evidence> rows, boolean truncated) {
        if (!FUNDING.contains(entry.action())) return result(entry, NOT_EVALUATED, List.of(), "ACTION_NOT_EVALUATED");
        if (truncated) return result(entry, INCOMPLETE, List.of(), "EVIDENCE_LIMIT");
        if (rows.isEmpty()) return result(entry, INCOMPLETE, List.of(), "MISSING_ALLOCATION_EVENTS");
        var issues = new LinkedHashSet<String>();
        var sources = new ArrayList<SettlementFundingPage.Source>();
        BigDecimal sum = BigDecimal.ZERO;
        boolean incomplete = false;
        for (var row : rows) {
            sum = sum.add(row.amount());
            if (!entry.action().equals(row.action())) issues.add("ACTION_MISMATCH");
            if (row.amount().signum() <= 0) issues.add("INVALID_ALLOCATION_AMOUNT");
            if (row.ownedAllocationId() == null) {
                incomplete = true; issues.add("MISSING_OWNED_ALLOCATION");
                // Do not reveal source metadata or IDs belonging to another user's allocation.
                continue;
            }
            if (!Objects.equals(entry.referenceKind(), row.referenceKind())
                    || !Objects.equals(entry.referenceId(), row.referenceId())) issues.add("REFERENCE_MISMATCH");
            String type;
            if (Set.of("SETTLED", "OPENING_SETTLED").contains(row.sourceKey())) {
                type = "SETTLED_ORIGIN";
            } else if (row.ownedObligationId() != null && "CASH_CREDIT".equals(row.obligationKind())
                    && Set.of("REUSABLE", "RESTRICTED").contains(row.creditPolicy())
                    && Set.of("PENDING", "SETTLED").contains(row.currentStatus())) {
                type = "CREDIT_ORIGIN";
                if ("RESTRICTED".equals(row.creditPolicy())) issues.add("INELIGIBLE_RESTRICTED_SOURCE");
            } else {
                type = "UNRESOLVED"; incomplete = true; issues.add("UNRESOLVED_OWNED_CREDIT");
            }
            sources.add(new SettlementFundingPage.Source(row.allocationId(), row.amount(), row.sourceKey(), type,
                    row.creditPolicy(), row.currentStatus()));
        }
        if (entry.amount().signum() <= 0 || sum.compareTo(entry.amount()) != 0) issues.add("AMOUNT_MISMATCH");
        boolean mismatch = issues.stream().anyMatch(i -> !Set.of("MISSING_OWNED_ALLOCATION", "UNRESOLVED_OWNED_CREDIT").contains(i));
        return new SettlementFundingPage.Event(entry, mismatch ? MISMATCH : incomplete ? INCOMPLETE : MATCHED,
                sources, new ArrayList<>(issues));
    }

    private static SettlementFundingPage.Event result(SettlementJournalPage.Entry entry,
                                                SettlementFundingPage.Verification status,
                                                List<SettlementFundingPage.Source> sources, String issue) {
        return new SettlementFundingPage.Event(entry, status, sources, List.of(issue));
    }
}
