package com.example.investsmart.dto;

import com.example.investsmart.settlement.JournalActionClassifier;
import com.example.investsmart.settlement.FundingEffectClassifier;
import java.time.Instant;
import java.util.List;

/** Owned saved-row evidence, never an account statement or reconstructed historical balance. */
public record JournalPeriodEvidence(Instant fromInclusive,Instant toExclusive,
        SettlementJournalPage.Coverage initialization,boolean startsBeforeInitialization,
        boolean completeFinancialHistory,boolean truncated,boolean fundingEvidenceTruncated,List<Entry> entries,List<String> limitations,GroupedChargeEvidence chargeGrouping,PeriodCoverageAssessment assessment,PeriodSummaryEligibility summaryEligibility,SelectedJournalChangeSummary selectedDirectChanges,GroupedJournalChangeSummary selectedGroupedChanges) {
    public JournalPeriodEvidence(Instant fromInclusive,Instant toExclusive,SettlementJournalPage.Coverage initialization,
            boolean startsBeforeInitialization,boolean completeFinancialHistory,boolean truncated,boolean fundingEvidenceTruncated,
            List<Entry> entries,List<String> limitations,GroupedChargeEvidence chargeGrouping,PeriodCoverageAssessment assessment,PeriodSummaryEligibility summaryEligibility,SelectedJournalChangeSummary selectedDirectChanges){
        this(fromInclusive,toExclusive,initialization,startsBeforeInitialization,completeFinancialHistory,truncated,fundingEvidenceTruncated,entries,limitations,chargeGrouping,assessment,summaryEligibility,selectedDirectChanges,null);
    }
    public JournalPeriodEvidence(Instant fromInclusive,Instant toExclusive,SettlementJournalPage.Coverage initialization,
            boolean startsBeforeInitialization,boolean completeFinancialHistory,boolean truncated,boolean fundingEvidenceTruncated,
            List<Entry> entries,List<String> limitations,GroupedChargeEvidence chargeGrouping,PeriodCoverageAssessment assessment,PeriodSummaryEligibility summaryEligibility){
        this(fromInclusive,toExclusive,initialization,startsBeforeInitialization,completeFinancialHistory,truncated,fundingEvidenceTruncated,entries,limitations,chargeGrouping,assessment,summaryEligibility,null);
    }
    public JournalPeriodEvidence(Instant fromInclusive,Instant toExclusive,SettlementJournalPage.Coverage initialization,
            boolean startsBeforeInitialization,boolean completeFinancialHistory,boolean truncated,boolean fundingEvidenceTruncated,
            List<Entry> entries,List<String> limitations,GroupedChargeEvidence chargeGrouping,PeriodCoverageAssessment assessment){
        this(fromInclusive,toExclusive,initialization,startsBeforeInitialization,completeFinancialHistory,truncated,fundingEvidenceTruncated,entries,limitations,chargeGrouping,assessment,null);
    }
    /** Unassessed supplied evidence; owned reader always attaches a computed assessment. */
    public JournalPeriodEvidence(Instant fromInclusive,Instant toExclusive,SettlementJournalPage.Coverage initialization,
            boolean startsBeforeInitialization,boolean completeFinancialHistory,boolean truncated,boolean fundingEvidenceTruncated,
            List<Entry> entries,List<String> limitations,GroupedChargeEvidence chargeGrouping){
        this(fromInclusive,toExclusive,initialization,startsBeforeInitialization,completeFinancialHistory,truncated,fundingEvidenceTruncated,entries,limitations,chargeGrouping,null);
    }
    public JournalPeriodEvidence withAssessment(PeriodCoverageAssessment value){
        return new JournalPeriodEvidence(fromInclusive,toExclusive,initialization,startsBeforeInitialization,completeFinancialHistory,truncated,fundingEvidenceTruncated,entries,limitations,chargeGrouping,value);
    }
    public JournalPeriodEvidence withSummaryEligibility(PeriodSummaryEligibility value){
        return new JournalPeriodEvidence(fromInclusive,toExclusive,initialization,startsBeforeInitialization,completeFinancialHistory,truncated,fundingEvidenceTruncated,entries,limitations,chargeGrouping,assessment,value);
    }
    public JournalPeriodEvidence withSelectedDirectChanges(SelectedJournalChangeSummary value){
        return new JournalPeriodEvidence(fromInclusive,toExclusive,initialization,startsBeforeInitialization,completeFinancialHistory,truncated,fundingEvidenceTruncated,entries,limitations,chargeGrouping,assessment,summaryEligibility,value);
    }
    public JournalPeriodEvidence withSelectedGroupedChanges(GroupedJournalChangeSummary value){
        return new JournalPeriodEvidence(fromInclusive,toExclusive,initialization,startsBeforeInitialization,completeFinancialHistory,truncated,fundingEvidenceTruncated,entries,limitations,chargeGrouping,assessment,summaryEligibility,selectedDirectChanges,value);
    }
    public JournalPeriodEvidence withChargeGrouping(GroupedChargeEvidence value){
        return new JournalPeriodEvidence(fromInclusive,toExclusive,initialization,startsBeforeInitialization,completeFinancialHistory,truncated,fundingEvidenceTruncated,entries,limitations,value);
    }
    public record Entry(SettlementJournalPage.Entry raw,JournalActionClassifier.Interpretation interpretation,
                        SettlementFundingPage.Event funding,FundingEffectClassifier.Effect fundingEffect) {}
    public JournalPeriodEvidence { entries=List.copyOf(entries);limitations=List.copyOf(limitations); }
}
