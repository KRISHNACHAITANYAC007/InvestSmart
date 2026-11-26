package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.example.investsmart.dto.CombinedTradeEvidence.*;

/** Pure evidence grouping. Caller must retrieve/authorize records and proofs in one snapshot. */
@Component
public class TradeRecordCombiner {
    private record Proof(String fillId,Key legacy,boolean matched,boolean ignored) {}
    public CombinedTradeEvidence combine(List<Key> records,List<EquityLegacyEvidence> equities,
                                          List<OptionLegacyEvidence> options,boolean truncated) {
        if(records==null||equities==null||options==null||truncated||records.size()>1000
                ||(long)equities.size()+options.size()>1000)return unavailable("INPUT_UNAVAILABLE_OR_LIMITED");
        var present=new LinkedHashSet<Key>();
        for(var key:records)if(!valid(key)||!present.add(key))return unavailable("INVALID_OR_DUPLICATE_RECORD_IDENTITY");
        var proofs=new ArrayList<Proof>();
        for(var link:equities){
            if(link==null){proofs.add(new Proof(null,null,false,false));continue;}
            proofs.add(new Proof(link.fillId(),link.duplicateOrderId()==null||link.duplicateOrderId()<1?null:
                    new Key(Source.EQUITY_ORDER,link.duplicateOrderId().toString()),
                    link.verification()==EquityLegacyEvidence.Verification.MATCHED_DUPLICATE,
                    link.verification()==EquityLegacyEvidence.Verification.NOT_EVALUATED));
        }
        for(var link:options){
            if(link==null){proofs.add(new Proof(null,null,false,false));continue;}
            proofs.add(new Proof(link.fillId(),link.duplicateOptionOrderId()==null?null:
                    new Key(Source.OPTION_ORDER,link.duplicateOptionOrderId()),
                    link.verification()==OptionLegacyEvidence.Verification.MATCHED_DUPLICATE,
                    link.verification()==OptionLegacyEvidence.Verification.NOT_EVALUATED));
        }
        var issues=new LinkedHashSet<String>();var blocked=new HashSet<Key>();
        var candidates=new ArrayList<Proof>();var fillCounts=new HashMap<Key,Integer>();var legacyCounts=new HashMap<Key,Integer>();
        for(var proof:proofs){
            if(proof.ignored())continue;
            var fill=new Key(Source.PAPER_FILL,proof.fillId());
            if(!proof.matched()||!valid(fill)||!valid(proof.legacy())){
                issues.add("UNVERIFIED_OR_INVALID_LINK");blocked.add(fill);continue;
            }
            fillCounts.merge(fill,1,Integer::sum);legacyCounts.merge(proof.legacy(),1,Integer::sum);
            if(!present.contains(fill)||!present.contains(proof.legacy())){
                issues.add("LINKED_RECORD_NOT_IN_INPUT");blocked.add(fill);continue;
            }
            candidates.add(proof);
        }
        var merged=new HashMap<Key,Key>();var hidden=new HashSet<Key>();
        for(var proof:candidates){
            var fill=new Key(Source.PAPER_FILL,proof.fillId());
            if(blocked.contains(fill)||fillCounts.get(fill)!=1||legacyCounts.get(proof.legacy())!=1){
                issues.add("AMBIGUOUS_LINKS_RETAINED");continue;
            }
            merged.put(fill,proof.legacy());hidden.add(proof.legacy());
        }
        var groups=new ArrayList<Group>();
        for(var key:records){
            if(hidden.contains(key))continue;
            var legacy=merged.get(key);groups.add(new Group(key,legacy==null?List.of(key):List.of(key,legacy)));
        }
        var limitations=new ArrayList<String>();
        limitations.add("Only supplied records/proofs are grouped; ownership, snapshot consistency and complete history are not established here.");
        limitations.addAll(issues);
        return new CombinedTradeEvidence(issues.isEmpty()?Status.COMBINED_SUPPLIED_RECORDS:Status.INCOMPLETE,groups,limitations);
    }
    private static boolean valid(Key key){return key!=null&&key.source()!=null&&key.id()!=null&&!key.id().isBlank()&&key.id().length()<=140;}
    private static CombinedTradeEvidence unavailable(String reason){return new CombinedTradeEvidence(Status.INCOMPLETE,List.of(),List.of(reason));}
}
