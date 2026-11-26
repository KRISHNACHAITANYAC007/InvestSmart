package com.example.investsmart.dto;

import com.example.investsmart.settlement.ChargeEvidenceReconciler;
import java.util.List;

/** One owned charge, not account-wide/period expenses or outstanding debt. */
public record ChargeEvidence(SettlementJournalPage.Entry parent,List<SettlementFundingPage.Event> children,
        boolean childrenTruncated,boolean fundingTruncated,ChargeEvidenceReconciler.Result comparison){
    public ChargeEvidence{children=List.copyOf(children);}
}
