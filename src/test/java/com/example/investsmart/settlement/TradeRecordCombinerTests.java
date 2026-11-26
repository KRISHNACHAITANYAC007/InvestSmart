package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.example.investsmart.dto.CombinedTradeEvidence.*;
import static org.junit.jupiter.api.Assertions.*;

class TradeRecordCombinerTests {
    final TradeRecordCombiner combiner=new TradeRecordCombiner();
    Key fill(String id){return new Key(Source.PAPER_FILL,id);}
    Key equity(String id){return new Key(Source.EQUITY_ORDER,id);}
    Key option(String id){return new Key(Source.OPTION_ORDER,id);}
    EquityLegacyEvidence matched(String fill,long order){return new EquityLegacyEvidence(fill,EquityLegacyEvidence.Verification.MATCHED_DUPLICATE,"verified",order);}
    @Test void sameIdInTwoNamespacesRemainsTwoTrades(){
        var records=List.of(fill("share"),equity("100"),fill("option"),option("100"));
        var result=combiner.combine(records,List.of(matched("share",100)),List.of(new OptionLegacyEvidence("option",OptionLegacyEvidence.Verification.MATCHED_DUPLICATE,"verified","100")),false);
        assertEquals(Status.COMBINED_SUPPLIED_RECORDS,result.status());assertEquals(2,result.groups().size());
        assertEquals(List.of(fill("share"),equity("100")),result.groups().getFirst().records());
        assertEquals(List.of(fill("option"),option("100")),result.groups().get(1).records());
        assertEquals(4,records.size());assertThrows(UnsupportedOperationException.class,()->result.groups().clear());
        assertThrows(UnsupportedOperationException.class,()->result.groups().getFirst().records().clear());
    }
    @Test void unmatchedOldRecordsStayVisibleAndOrderingFollowsRepresentatives(){
        var records=List.of(equity("1"),option("old"),fill("new"),equity("2"));
        var result=combiner.combine(records,List.of(matched("new",1)),List.of(),false);
        assertEquals(List.of(option("old"),fill("new"),equity("2")),result.groups().stream().map(Group::representative).toList());
        assertEquals(4,result.groups().stream().mapToInt(g->g.records().size()).sum());
    }
    @Test void unverifiedAndOutOfInputLinksCannotHideRecords(){
        var records=List.of(fill("a"),equity("1"));
        var bad=new EquityLegacyEvidence("a",EquityLegacyEvidence.Verification.MISMATCH,"conflict",null);
        var result=combiner.combine(records,List.of(bad),List.of(),false);
        assertEquals(Status.INCOMPLETE,result.status());assertEquals(2,result.groups().size());
        result=combiner.combine(records,List.of(matched("a",2)),List.of(),false);
        assertEquals(Status.INCOMPLETE,result.status());assertEquals(2,result.groups().size());
    }
    @Test void multipleFillsClaimingOneLegacyRecordRemainSeparate(){
        var records=List.of(fill("a"),fill("b"),equity("1"));
        var result=combiner.combine(records,List.of(matched("a",1),matched("b",1)),List.of(),false);
        assertEquals(Status.INCOMPLETE,result.status());assertEquals(3,result.groups().size());
    }
    @Test void competingProofOutsideInputCannotHideVisibleLegacyRecord(){
        var records=List.of(fill("visible"),equity("1"));
        var result=combiner.combine(records,List.of(matched("visible",1),matched("outside-input",1)),List.of(),false);
        assertEquals(Status.INCOMPLETE,result.status());assertEquals(2,result.groups().size());
        assertEquals(records,result.groups().stream().map(Group::representative).toList());
    }
    @Test void conflictingOrRepeatedProofsForOneFillRemainSeparate(){
        var records=List.of(fill("a"),equity("1"),option("1"));
        var result=combiner.combine(records,List.of(matched("a",1)),List.of(new OptionLegacyEvidence("a",OptionLegacyEvidence.Verification.MATCHED_DUPLICATE,"verified","1")),false);
        assertEquals(Status.INCOMPLETE,result.status());assertEquals(3,result.groups().size());
        result=combiner.combine(records,List.of(matched("a",1),matched("a",1)),List.of(),false);
        assertEquals(3,result.groups().size());
    }
    @Test void limitsAndDuplicateInputFailUnavailable(){
        var records=new ArrayList<Key>();for(int i=0;i<1000;i++)records.add(fill("fill-"+i));
        assertEquals(1000,combiner.combine(records,List.of(),List.of(),false).groups().size());
        records.add(fill("sentinel"));assertTrue(combiner.combine(records,List.of(),List.of(),false).groups().isEmpty());
        assertTrue(combiner.combine(List.of(fill("a"),fill("a")),List.of(),List.of(),false).groups().isEmpty());
        assertTrue(combiner.combine(List.of(fill("a")),List.of(),List.of(),true).groups().isEmpty());
        assertTrue(combiner.combine(List.of(fill("a")),Collections.nCopies(1001,matched("a",1)),List.of(),false).groups().isEmpty());
    }
    @Test void namespaceNotEvaluatedDoesNotOverrideValidProofButUnverifiedProofDoes(){
        var records=List.of(fill("a"),equity("1"));
        var ignored=new OptionLegacyEvidence("a",OptionLegacyEvidence.Verification.NOT_EVALUATED,"equity",null);
        assertEquals(1,combiner.combine(records,List.of(matched("a",1)),List.of(ignored),false).groups().size());
        var invalid=new EquityLegacyEvidence("a",EquityLegacyEvidence.Verification.INCOMPLETE,"missing",null);
        var result=combiner.combine(records,List.of(matched("a",1),invalid),List.of(),false);
        assertEquals(Status.INCOMPLETE,result.status());assertEquals(2,result.groups().size());
    }
}
