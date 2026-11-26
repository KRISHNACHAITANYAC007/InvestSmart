package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.investsmart.dto.IntradayTicketSliceEvidence.Status.*;

class IntradayTicketSliceReconcilerTests {
    final IntradayTicketSliceReconciler rules=new IntradayTicketSliceReconciler(new TicketFeeSliceReconciler());
    final Instant from=Instant.parse("2026-11-24T09:00:00Z"),to=from.plusSeconds(60);
    BigDecimal fee=new BigDecimal("2.00");
    IntradayTicketSliceReconciler.Position position(String direction){return new IntradayTicketSliceReconciler.Position("position",1,direction);}
    IntradayTicketSliceReconciler.Ticket ticket(String side){return new IntradayTicketSliceReconciler.Ticket("ticket","position",1,"MIS",side,"FILLED",new TicketFeeSliceReconciler.Ticket(10,10,1,fee));}
    IntradayTicketSliceReconciler.Event event(String kind){return new IntradayTicketSliceReconciler.Event("ticket","position",kind,10,fee);}
    TicketFeeSliceReconciler.Fill fill(Instant at){return new TicketFeeSliceReconciler.Fill("fill","ticket",0,10,fee,at);}
    IntradayTicketSliceEvidence read(List<IntradayTicketSliceReconciler.Event> events,List<TicketFeeSliceReconciler.Fill> fills){return rules.reconcile(ticket("BUY"),position("LONG"),events,fills,from,to,false);}
    @Test void allLongShortOpenCloseSidesMatchAndCountOnlyFillFee(){
        for(String direction:List.of("LONG","SHORT"))for(String kind:List.of("OPEN","CLOSE")){
            String side=(direction.equals("LONG")==kind.equals("OPEN"))?"BUY":"SELL";
            var result=rules.reconcile(ticket(side),position(direction),List.of(event(kind)),List.of(fill(from)),from,to,false);
            assertEquals(MATCHED_EVENT_TICKET_SLICE,result.status());assertEquals(fee,result.slice().selectedFee());assertEquals(1,result.slice().selectedFillCount());
        }
    }
    @Test void halfOpenFillTimeSelectsZeroOutsideButStillChecksLink(){
        for(Instant at:List.of(from.minusSeconds(1),to)){
            var result=read(List.of(event("OPEN")),List.of(fill(at)));assertEquals(MATCHED_EVENT_TICKET_SLICE,result.status());assertEquals(new BigDecimal("0.00"),result.slice().selectedFee());
        }
        var bad=new IntradayTicketSliceReconciler.Event("ticket","position","OPEN",10,new BigDecimal("9"));
        assertEquals(MISMATCH,read(List.of(bad),List.of(fill(to))).status());
    }
    @Test void absentLimitedOrDuplicateEvidenceWithholdsSlice(){
        assertEquals(INCOMPLETE,read(List.of(),List.of(fill(from))).status());
        assertEquals(INCOMPLETE,read(List.of(event("OPEN")),List.of()).status());
        assertEquals(MISMATCH,read(List.of(event("OPEN"),event("OPEN")),List.of(fill(from))).status());
        assertEquals(INCOMPLETE,read(Collections.nCopies(1001,event("OPEN")),List.of(fill(from))).status());
        assertEquals(INCOMPLETE,read(List.of(event("OPEN")),Collections.nCopies(1001,fill(from))).status());
        var limited=rules.reconcile(ticket("BUY"),position("LONG"),List.of(event("OPEN")),List.of(fill(from)),from,to,true);
        assertEquals(INCOMPLETE,limited.status());assertNull(limited.slice());
    }
    @Test void wrongEventIdentityPositionUnitsAndUnsupportedKindNeverMatch(){
        for(var e:List.of(new IntradayTicketSliceReconciler.Event("other","position","OPEN",10,fee),new IntradayTicketSliceReconciler.Event("ticket","other","OPEN",10,fee),new IntradayTicketSliceReconciler.Event("ticket","position","OPEN",9,fee))){
            var result=read(List.of(e),List.of(fill(from)));assertEquals(MISMATCH,result.status());assertNull(result.slice());
        }
        assertEquals(INCOMPLETE,read(List.of(event("UNKNOWN")),List.of(fill(from))).status());
    }
    @Test void ticketMetadataAndSingleLegConstraintsAreRequired(){
        var t=ticket("BUY");
        for(var bad:List.of(new IntradayTicketSliceReconciler.Ticket(t.id(),t.positionId(),2,t.product(),t.side(),t.status(),t.amounts()),new IntradayTicketSliceReconciler.Ticket(t.id(),t.positionId(),1,"CASH",t.side(),t.status(),t.amounts()),new IntradayTicketSliceReconciler.Ticket(t.id(),t.positionId(),1,t.product(),"SELL",t.status(),t.amounts()),new IntradayTicketSliceReconciler.Ticket(t.id(),t.positionId(),1,t.product(),t.side(),"PART_FILLED",t.amounts()))){
            assertEquals(MISMATCH,rules.reconcile(bad,position("LONG"),List.of(event("OPEN")),List.of(fill(from)),from,to,false).status());
        }
        var wrongLeg=new TicketFeeSliceReconciler.Fill("fill","ticket",1,10,fee,from);
        assertEquals(MISMATCH,read(List.of(event("OPEN")),List.of(wrongLeg)).status());
        assertEquals(MISMATCH,read(List.of(event("OPEN")),List.of(fill(from),fill(from))).status());
    }
    @Test void exactNonnegativeMoneyAndValidPeriodRequired(){
        for(BigDecimal bad:List.of(new BigDecimal("-1"),new BigDecimal("0.001"),new BigDecimal("100000000000000000"))){
            fee=bad;assertEquals(MISMATCH,read(List.of(event("OPEN")),List.of(fill(from))).status());
        }
        fee=new BigDecimal("0.00");assertEquals(new BigDecimal("0.00"),read(List.of(event("OPEN")),List.of(fill(from))).slice().selectedFee());
        assertEquals(MISMATCH,rules.reconcile(ticket("BUY"),position("LONG"),List.of(event("OPEN")),List.of(fill(from)),to,from,false).status());
    }
}
