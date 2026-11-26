package com.example.investsmart.settlement;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.example.investsmart.settlement.IntradayEvidenceReconciler.*;
import static com.example.investsmart.settlement.IntradayEvidenceReconciler.Verification.*;
import static org.junit.jupiter.api.Assertions.*;

class IntradayEvidenceReconcilerTests {
    final IntradayEvidenceReconciler reader = new IntradayEvidenceReconciler();
    BigDecimal n(String value) { return new BigDecimal(value); }
    Event event(String key,String kind,int units,String collateral,String pnl,String fee) {
        return new Event(key,kind,units,n(collateral),n(pnl),n(fee));
    }
    Position position(int units,String collateral,String pnl) { return new Position(units,n(collateral),n(pnl)); }
    Event open() { return event("open","OPEN",10,"500.00","0.00","0.30"); }
    @Test void partialAndCompletedExitsKeepGrossProfitAndFeesSeparate() {
        var partial=List.of(open(),event("loss","CLOSE",4,"-200.00","-40.00","0.11"));
        var result=reader.reconcile(position(6,"300.00","-40.00"),partial,false);
        assertEquals(MATCHED_POSITION_TOTALS,result.verification());
        assertEquals(n("-40.00"),result.verifiedGrossRealizedPnl());assertEquals(n("0.41"),result.eventFeeSum());
        var completed=new ArrayList<>(partial);completed.add(event("profit","CLOSE",6,"-300.00","60.00","0.20"));
        result=reader.reconcile(position(0,"0.00","20.00"),completed,false);
        assertEquals(MATCHED_POSITION_TOTALS,result.verification());
        assertEquals(n("20.00"),result.verifiedGrossRealizedPnl());assertEquals(n("0.61"),result.eventFeeSum());
        assertEquals(3,completed.size());
    }
    @Test void shortSaleTurnoverIsNeverInventedAsProfitOrCash() {
        var events=List.of(open(),event("close","CLOSE",10,"-500.00","100.00","0.27"));
        var result=reader.reconcile(position(0,"0","100"),events,false);
        assertEquals(MATCHED_POSITION_TOTALS,result.verification());assertEquals(n("100.00"),result.verifiedGrossRealizedPnl());
        assertEquals(n("0.57"),result.eventFeeSum());
    }
    @Test void lossesLargerThanCollateralAreStillSignedGrossLosses() {
        var events=List.of(open(),event("gap","CLOSE",10,"-500","-1000.00","0.60"));
        var result=reader.reconcile(position(0,"0","-1000"),events,false);
        assertEquals(MATCHED_POSITION_TOTALS,result.verification());assertEquals(n("-1000.00"),result.verifiedGrossRealizedPnl());
    }
    @Test void missingOpeningUnknownAndTruncatedEvidenceAreUnavailableNotZero() {
        for(var events:List.of(List.<Event>of(),List.of(event("close","CLOSE",1,"-1","0","0")),
                List.of(event("future","FUTURE",1,"0","0","0")))) {
            var result=reader.reconcile(position(0,"0","0"),events,false);
            assertEquals(INCOMPLETE,result.verification());assertNull(result.verifiedGrossRealizedPnl());assertNull(result.eventFeeSum());
        }
        assertEquals(INCOMPLETE,reader.reconcile(position(10,"500","0"),List.of(open()),true).verification());
        assertEquals(INCOMPLETE,reader.reconcile(position(0,"0","0"),Collections.nCopies(1001,open()),false).verification());
    }
    @Test void quantityCollateralAndPnlConflictsNeverMatch() {
        for(var p:List.of(position(9,"500","0"),position(10,"499","0"),position(10,"500","1"))) {
            var result=reader.reconcile(p,List.of(open()),false);
            assertEquals(MISMATCH,result.verification());assertNull(result.verifiedGrossRealizedPnl());
        }
        assertEquals(MISMATCH,reader.reconcile(position(0,"0","0"),List.of(open(),event("exit","CLOSE",11,"-500","0","0")),false).verification());
    }
    @Test void duplicatesInvalidDirectionsAndFractionalMoneyFailClosed() {
        for(var events:List.of(List.of(open(),open()),List.of(open(),event("close","CLOSE",1,"1","0","0")),
                List.of(event("open","OPEN",10,"500","1","0")),List.of(event("open","OPEN",10,"500","0","-1")),
                List.of(event("open","OPEN",10,"500","0","0.001")))) {
            var result=reader.reconcile(position(10,"500","0"),events,false);
            assertEquals(MISMATCH,result.verification());assertNull(result.eventFeeSum());
        }
    }
    @Test void exactLargeAmountsAndTheThousandEventBoundaryArePreserved() {
        var large=reader.reconcile(position(10,"99999999999999999.99","0"),
                List.of(event("open","OPEN",10,"99999999999999999.99","0","99999999999999999.99")),false);
        assertEquals(MATCHED_POSITION_TOTALS,large.verification());
        assertEquals(n("99999999999999999.99"),large.eventFeeSum());
        var events=new ArrayList<Event>();events.add(event("open","OPEN",1000,"0","0","0"));
        for(int i=0;i<999;i++)events.add(event("close-"+i,"CLOSE",1,"0","0","0"));
        var result=reader.reconcile(position(1,"0","0"),events,false);
        assertEquals(MATCHED_POSITION_TOTALS,result.verification());
        assertEquals(MISMATCH,reader.reconcile(position(-1,"0","0"),events,false).verification());
    }
}
