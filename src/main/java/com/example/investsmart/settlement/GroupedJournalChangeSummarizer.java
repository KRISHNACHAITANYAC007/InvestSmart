package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import java.math.*;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.example.investsmart.dto.GroupedJournalChangeSummary.*;

/** Caller owns authentication/snapshot; source proofs and global partition are rechecked here. */
@Component
public class GroupedJournalChangeSummarizer {
    private final PeriodSummaryEligibilityAssessor eligibility;private final PeriodChargeGrouper grouper;private final ChargeGroupSourceReconciler bindings;
    public GroupedJournalChangeSummarizer(PeriodSummaryEligibilityAssessor eligibility,PeriodChargeGrouper grouper,ChargeGroupSourceReconciler bindings){this.eligibility=eligibility;this.grouper=grouper;this.bindings=bindings;}
    public GroupedJournalChangeSummary summarize(JournalPeriodEvidence period){
        var fresh=eligibility.assess(period);var common=new LinkedHashSet<String>();
        var proofs=new HashMap<String,ChargeEvidence>();
        try{
            if(period==null||period.chargeGrouping()==null)common.add("GROUP_SOURCE_UNAVAILABLE");
            else{
                var groups=period.chargeGrouping();
                if(groups.sourceEvidence().size()>1000)common.add("GROUP_SOURCE_LIMIT");
                else{
                    for(var binding:groups.sourceEvidence()){
                        var checked=bindings.reconcile(binding.group(),binding.sourceProof(),false);
                        if(!checked.equals(binding)||checked.status()!=ChargeGroupSourceEvidence.Status.MATCHED_SUPPLIED_GROUP_SOURCE||proofs.putIfAbsent(binding.group().representativeKey(),binding.sourceProof())!=null)common.add("GROUP_SOURCE_CONFLICT");
                    }
                    var checked=grouper.group(period.entries().stream().map(JournalPeriodEvidence.Entry::raw).toList(),new ArrayList<>(proofs.values()),false);
                    if(checked.status()!=GroupedChargeEvidence.Status.GROUPED_SUPPLIED_ROWS||groups.status()!=checked.status()||!checked.groups().equals(groups.groups())||groups.groups().stream().filter(g->g.kind()==GroupedChargeEvidence.Kind.VERIFIED_CHARGE).count()!=proofs.size())common.add("GLOBAL_GROUP_PARTITION_CONFLICT");
                    if(groups.groups().stream().anyMatch(g->!g.outsideSelectionChildKeys().isEmpty()))common.add("CHARGE_CHILD_OUTSIDE_PERIOD");
                }
            }
        }catch(NullPointerException|IllegalArgumentException malformed){common.add("MALFORMED_GROUP_SOURCE");}
        if(period!=null&&period.summaryEligibility()!=null&&!fresh.equals(period.summaryEligibility()))common.add("STALE_ELIGIBILITY");
        var results=new ArrayList<Bucket>();
        for(boolean settled:List.of(true,false)){
            var reasons=new LinkedHashSet<>(common);
            var scope=settled?Scope.SELECTED_GROUPED_SETTLED_CHANGE:Scope.SELECTED_GROUPED_RESERVED_CHANGE;
            var direct=fresh.buckets().stream().filter(b->b.scope()==(settled?PeriodSummaryEligibility.Scope.SELECTED_JOURNAL_SETTLED_CHANGE:PeriodSummaryEligibility.Scope.SELECTED_JOURNAL_RESERVED_CHANGE)).findFirst().orElseThrow();
            for(var reason:direct.reasons())if(reason!=PeriodSummaryEligibility.Reason.SUPPLIED_JOURNAL_ONLY&&reason!=PeriodSummaryEligibility.Reason.COMPOSITE_EFFECT_OUTSIDE_DIRECT_SCOPE)reasons.add(reason.name());
            BigDecimal sum=new BigDecimal("0.00");
            if(reasons.isEmpty())try{
                var rows=new HashMap<String,JournalPeriodEvidence.Entry>();period.entries().forEach(e->rows.put(e.raw().eventKey(),e));
                for(var group:period.chargeGrouping().groups()){
                    BigDecimal change;
                    if(group.kind()==GroupedChargeEvidence.Kind.VERIFIED_CHARGE)change=settled?proofs.get(group.representativeKey()).comparison().settledEffect():new BigDecimal("0.00");
                    else{var e=rows.get(group.representativeKey());boolean funding=FundingEvidenceChecker.FUNDING.contains(e.raw().action());change=settled?(funding?e.fundingEffect().settledDelta():e.interpretation().settledDelta()):(funding?e.fundingEffect().reservedDelta():e.interpretation().reservedDelta());}
                    if(!exact(change,19)){reasons.add("INVALID_GROUP_CHANGE");break;}sum=sum.add(change.setScale(2));
                }
                if(!exact(sum,22))reasons.add("GROUP_CHANGE_LIMIT");
            }catch(NullPointerException|IllegalArgumentException malformed){reasons.add("INVALID_GROUP_CHANGE");}
            boolean available=reasons.isEmpty();reasons.add("SELECTED_COMPLETE_GROUPS_ONLY_NO_ACCOUNT_TOTAL");results.add(new Bucket(scope,available?Status.VERIFIED_SUPPLIED_GROUPED_CHANGE:Status.UNAVAILABLE,available?sum:null,new ArrayList<>(reasons)));
        }
        return new GroupedJournalChangeSummary(period==null?null:period.fromInclusive(),period==null?null:period.toExclusive(),false,results);
    }
    private static boolean exact(BigDecimal n,int precision){if(n==null)return false;try{return n.setScale(2,RoundingMode.UNNECESSARY).precision()<=precision;}catch(ArithmeticException invalid){return false;}}
}
