package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import java.math.*;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.example.investsmart.dto.SelectedJournalChangeSummary.Status.*;
import static com.example.investsmart.dto.PeriodSummaryEligibility.Scope.*;

/** Pure single-period calculation. Caller owns authorization and snapshot proof. */
@Component
public class SelectedJournalChangeSummarizer {
    private final PeriodSummaryEligibilityAssessor eligibility;
    public SelectedJournalChangeSummarizer(PeriodSummaryEligibilityAssessor eligibility){this.eligibility=eligibility;}
    public SelectedJournalChangeSummary summarize(JournalPeriodEvidence period){
        var checked=eligibility.assess(period);
        boolean stale=period!=null&&period.summaryEligibility()!=null&&!checked.equals(period.summaryEligibility());
        var buckets=new ArrayList<SelectedJournalChangeSummary.Bucket>();
        for(var bucket:checked.buckets()){
            var reasons=new ArrayList<>(bucket.reasons().stream().map(Enum::name).toList());
            boolean direct=bucket.scope()==SELECTED_JOURNAL_SETTLED_CHANGE||bucket.scope()==SELECTED_JOURNAL_RESERVED_CHANGE;
            if(!direct||bucket.status()!=PeriodSummaryEligibility.Status.ELIGIBLE_SUPPLIED_DIRECT_ROWS||stale){
                if(direct&&stale)reasons.add("STALE_ELIGIBILITY");
                buckets.add(new SelectedJournalChangeSummary.Bucket(bucket.scope(),UNAVAILABLE,null,reasons));continue;
            }
            BigDecimal sum=new BigDecimal("0.00");boolean invalid=false;
            for(var entry:period.entries()){
                boolean funding=FundingEvidenceChecker.FUNDING.contains(entry.raw().action());
                BigDecimal change=bucket.scope()==SELECTED_JOURNAL_SETTLED_CHANGE
                    ?funding?entry.fundingEffect().settledDelta():entry.interpretation().settledDelta()
                    :funding?entry.fundingEffect().reservedDelta():entry.interpretation().reservedDelta();
                if(!exact(change,19)){invalid=true;break;}sum=sum.add(change.setScale(2));
            }
            // Up to1000 exact DECIMAL(19,2) row deltas require at most22 decimal digits in the sum.
            if(invalid||!exact(sum,22)){reasons.add("DIRECT_CHANGE_UNAVAILABLE_OR_INVALID");buckets.add(new SelectedJournalChangeSummary.Bucket(bucket.scope(),UNAVAILABLE,null,reasons));}
            else buckets.add(new SelectedJournalChangeSummary.Bucket(bucket.scope(),VERIFIED_SUPPLIED_CHANGE,sum,reasons));
        }
        return new SelectedJournalChangeSummary(period==null?null:period.fromInclusive(),period==null?null:period.toExclusive(),buckets);
    }
    private static boolean exact(BigDecimal value,int precision){if(value==null)return false;try{return value.setScale(2,RoundingMode.UNNECESSARY).precision()<=precision;}catch(ArithmeticException invalid){return false;}}
}
