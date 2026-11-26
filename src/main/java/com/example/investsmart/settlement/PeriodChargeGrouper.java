package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.example.investsmart.dto.GroupedChargeEvidence.*;

/** Caller supplies owned rows/proofs from one snapshot. No database/service calls or sums. */
@Component
public class PeriodChargeGrouper {
    private final ChargeEvidenceReconciler reconciler;
    public PeriodChargeGrouper(ChargeEvidenceReconciler reconciler){this.reconciler=reconciler;}
    public GroupedChargeEvidence group(List<SettlementJournalPage.Entry> selected,List<ChargeEvidence> proofs,boolean limited){
        if(selected==null||proofs==null||limited||selected.size()>1000||proofs.size()>1000)return unavailable("INPUT_UNAVAILABLE_OR_LIMITED");
        var present=new LinkedHashMap<String,SettlementJournalPage.Entry>();
        for(var row:selected)if(row==null||!valid(row.eventKey())||present.putIfAbsent(row.eventKey(),row)!=null)return unavailable("INVALID_OR_DUPLICATE_SELECTED_KEY");
        long children=0,sources=0;
        for(var proof:proofs)if(proof!=null){
            children+=proof.children().size();if(children>1000)return unavailable("CHILD_EVIDENCE_LIMIT");
            for(var child:proof.children())sources+=child.sources().size();
            if(sources>1000)return unavailable("SOURCE_EVIDENCE_LIMIT");
        }
        var parentCounts=new HashMap<String,Integer>();var childCounts=new HashMap<String,Integer>();
        for(var proof:proofs)if(proof!=null){
            if(proof.parent()!=null)parentCounts.merge(proof.parent().eventKey(),1,Integer::sum);
            for(var child:proof.children())if(child.journal()!=null)childCounts.merge(child.journal().eventKey(),1,Integer::sum);
        }
        var issues=new LinkedHashSet<String>();var blocked=new HashSet<String>();var candidates=new ArrayList<ChargeEvidence>();
        for(var proof:proofs){
            if(proof==null||proof.parent()==null){issues.add("MISSING_PROOF");continue;}
            var parent=proof.parent();
            var checked=reconciler.reconcile(parent,proof.children(),proof.childrenTruncated()||proof.fundingTruncated());
            boolean conflict=checked.status()!=ChargeEvidenceReconciler.Status.MATCHED_SUPPLIED_PAIR||!checked.equals(proof.comparison());
            if(present.containsKey(parent.eventKey())&&!parent.equals(present.get(parent.eventKey())))conflict=true;
            for(var child:proof.children())if(child.journal()!=null&&present.containsKey(child.journal().eventKey())&&!child.journal().equals(present.get(child.journal().eventKey())))conflict=true;
            if(conflict){
                issues.add("UNVERIFIED_OR_CONFLICTING_CHARGE_PROOF");blocked.add(parent.eventKey());
                for(var child:proof.children())if(child.journal()!=null)blocked.add(child.journal().eventKey());
            }else if(!present.containsKey(parent.eventKey()))issues.add("CHARGE_PARENT_OUTSIDE_SELECTION");
            else candidates.add(proof);
        }
        var matched=new HashMap<String,ChargeEvidence>();var hidden=new HashSet<String>();
        for(var proof:candidates){
            String key=proof.parent().eventKey();
            boolean ambiguous=parentCounts.getOrDefault(key,0)!=1||blocked.contains(key)||childCounts.containsKey(key);
            for(var child:proof.children()){
                String childKey=child.journal().eventKey();
                if(childCounts.getOrDefault(childKey,0)!=1||parentCounts.containsKey(childKey)||blocked.contains(childKey))ambiguous=true;
            }
            if(ambiguous){issues.add("AMBIGUOUS_CHARGE_EVIDENCE_RETAINED");continue;}
            matched.put(key,proof);
            for(var child:proof.children())if(present.containsKey(child.journal().eventKey()))hidden.add(child.journal().eventKey());
        }
        var groups=new ArrayList<Group>();
        for(var row:selected){
            if(hidden.contains(row.eventKey()))continue;
            var proof=matched.get(row.eventKey());
            if(proof==null){
                if("REALIZED_CHARGE".equals(row.action())||"CHARGE".equals(row.referenceKind())||row.eventKey().startsWith("CHARGE_RESERVE:")||row.eventKey().startsWith("CHARGE_SPEND:"))issues.add("UNGROUPED_CHARGE_ROW_RETAINED");
                groups.add(new Group(Kind.UNGROUPED,row.eventKey(),List.of(row),List.of()));
            }else{
                var represented=new ArrayList<SettlementJournalPage.Entry>();represented.add(row);var outside=new ArrayList<String>();
                for(var child:proof.children()){var raw=child.journal();if(present.containsKey(raw.eventKey()))represented.add(present.get(raw.eventKey()));else outside.add(raw.eventKey());}
                groups.add(new Group(Kind.VERIFIED_CHARGE,row.eventKey(),represented,outside));
            }
        }
        var limitations=new ArrayList<String>();limitations.add("Supplied selected rows only; ownership/snapshot/range coverage not established. Outside-selection keys are context, never extra period rows or expense totals.");limitations.addAll(issues);
        return new GroupedChargeEvidence(issues.isEmpty()?Status.GROUPED_SUPPLIED_ROWS:Status.INCOMPLETE,groups,limitations);
    }
    private static boolean valid(String key){return key!=null&&!key.isBlank()&&key.length()<=160&&key.chars().noneMatch(Character::isISOControl);}
    private static GroupedChargeEvidence unavailable(String reason){return new GroupedChargeEvidence(Status.INCOMPLETE,List.of(),List.of(reason));}
}
