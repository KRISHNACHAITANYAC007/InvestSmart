package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Package-local operation: caller establishes owned identity and repeatable read snapshot. */
@Component
public class PeriodChargeAdapter {
    private final PeriodChargeRepository records;
    private final SettlementFundingRepository funding;
    private final ChargeEvidenceReconciler reconciler;
    private final PeriodChargeGrouper grouper;
    private final ChargeGroupSourceReconciler bindings;
    public PeriodChargeAdapter(PeriodChargeRepository records,SettlementFundingRepository funding,ChargeEvidenceReconciler reconciler,PeriodChargeGrouper grouper,ChargeGroupSourceReconciler bindings){this.records=records;this.funding=funding;this.reconciler=reconciler;this.grouper=grouper;this.bindings=bindings;}
    GroupedChargeEvidence group(long user,List<SettlementJournalPage.Entry> selected,boolean limited){
        if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                ||!Objects.equals(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel(),java.sql.Connection.TRANSACTION_REPEATABLE_READ))
            throw new IllegalStateException("Period charge evidence requires the caller's read-only repeatable snapshot.");
        if(limited||selected==null||selected.size()>1000)return grouper.group(selected,List.of(),true);
        var parents=selected.stream().filter(row->"REALIZED_CHARGE".equals(row.action())).toList();
        var references=parents.stream().map(row->ChargeEvidenceReconciler.reference(row.eventKey())).distinct().toList();
        var children=new LinkedHashMap<String,SettlementJournalPage.Entry>();int returned=0;
        for(int offset=0;offset<references.size();offset+=100){
            var rows=records.children(user,references.subList(offset,Math.min(offset+100,references.size())),1001-returned);
            returned+=rows.size();
            if(returned>1000)return grouper.group(selected,List.of(),true);
            for(var row:rows){var prior=children.putIfAbsent(row.eventKey(),row);if(prior!=null&&!prior.equals(row))return grouper.group(selected,List.of(),true);}
        }
        // Shared candidate records stay visible to every matching parent; no silent assignment.
        var byParent=new LinkedHashMap<SettlementJournalPage.Entry,List<SettlementJournalPage.Entry>>();
        long proofChildren=0;
        var sourceKeys=new LinkedHashSet<String>();
        for(var parent:parents){
            String reference=ChargeEvidenceReconciler.reference(parent.eventKey());
            var candidates=children.values().stream().filter(row->("CHARGE".equals(row.referenceKind())&&reference.equals(row.referenceId()))
                ||("CHARGE_RESERVE:"+reference).equals(row.eventKey())||("CHARGE_SPEND:"+reference).equals(row.eventKey())).toList();
            proofChildren+=candidates.size();if(proofChildren>1000)return grouper.group(selected,List.of(),true);
            byParent.put(parent,candidates);
            if(candidates.size()==2)candidates.forEach(row->sourceKeys.add(row.eventKey()));
        }
        var keys=new ArrayList<>(sourceKeys);var sourceRows=new ArrayList<SettlementFundingRepository.Evidence>();
        for(int offset=0;offset<keys.size();offset+=100){
            sourceRows.addAll(funding.evidence(user,keys.subList(offset,Math.min(offset+100,keys.size())),1001-sourceRows.size()));
            if(sourceRows.size()>1000)return grouper.group(selected,List.of(),true);
        }
        var grouped=sourceRows.stream().collect(java.util.stream.Collectors.groupingBy(SettlementFundingRepository.Evidence::eventKey));
        var proofs=new ArrayList<ChargeEvidence>();long representedSources=0;
        for(var entry:byParent.entrySet()){
            var checks=new ArrayList<SettlementFundingPage.Event>();
            for(var row:entry.getValue()){
                var evidence=entry.getValue().size()==2?grouped.getOrDefault(row.eventKey(),List.of()):List.<SettlementFundingRepository.Evidence>of();
                representedSources+=evidence.size();if(representedSources>1000)return grouper.group(selected,List.of(),true);
                checks.add(FundingEvidenceChecker.check(row,evidence,false));
            }
            proofs.add(new ChargeEvidence(entry.getKey(),checks,false,false,reconciler.reconcile(entry.getKey(),checks,false)));
        }
        var result=grouper.group(selected,proofs,false);
        var retained=new ArrayList<ChargeGroupSourceEvidence>();
        // Global ambiguity/competing-child checks above remain the grouper's responsibility.
        for(var group:result.groups())if(group.kind()==GroupedChargeEvidence.Kind.VERIFIED_CHARGE){
            var candidates=proofs.stream().filter(proof->proof.parent().eventKey().equals(group.representativeKey())).toList();
            var binding=candidates.size()==1?bindings.reconcile(group,candidates.getFirst(),false):null;
            if(binding==null||binding.status()!=ChargeGroupSourceEvidence.Status.MATCHED_SUPPLIED_GROUP_SOURCE){
                var issues=new ArrayList<>(result.limitations());issues.add("GROUP_SOURCE_BINDING_UNAVAILABLE");
                return new GroupedChargeEvidence(GroupedChargeEvidence.Status.INCOMPLETE,result.groups(),issues);
            }
            retained.add(binding);
        }
        return new GroupedChargeEvidence(result.status(),result.groups(),result.limitations(),retained);
    }
}
