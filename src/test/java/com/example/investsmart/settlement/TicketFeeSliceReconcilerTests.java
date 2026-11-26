package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.investsmart.dto.TicketFeeSliceEvidence.Status.*;

class TicketFeeSliceReconcilerTests {
    final TicketFeeSliceReconciler rules=new TicketFeeSliceReconciler();final Instant from=JournalReportFixture.AT,to=from.plusSeconds(60);
    BigDecimal n(String v){return new BigDecimal(v).setScale(2);}
    TicketFeeSliceReconciler.Fill fill(String id,int leg,String fee,Instant time){return new TicketFeeSliceReconciler.Fill(id,"ticket",leg,1,n(fee),time);}
    TicketFeeSliceReconciler.Ticket ticket(int quantity,int filled,int legs,String fee){return new TicketFeeSliceReconciler.Ticket(quantity,filled,legs,n(fee));}
    @Test void partialPeriodSelectsDetailOnceAfterWholeTicketReconciles(){
        var fills=List.of(fill("before",0,"0.50",from.minusSeconds(1)),fill("inside",1,"1",from),fill("end",2,"1.50",to));
        var result=rules.reconcile("ticket",ticket(3,3,3,"3"),fills,from,to,false);
        assertEquals(MATCHED_TICKET_SLICE,result.status());assertEquals(n("1"),result.selectedFee());assertEquals(1,result.selectedFillCount());assertEquals(2,result.outsidePeriodFillCount());
        assertEquals(List.of("inside"),result.selectedFills().stream().map(TicketFeeSliceEvidence.SelectedFill::fillId).toList());
        assertThrows(UnsupportedOperationException.class,()->result.selectedFills().clear());
    }
    @Test void partiallyFilledOrCancelledTicketAndVerifiedEmptySliceAreAllowed(){
        var fills=List.of(fill("a",0,"0.50",from.minusSeconds(1)),fill("b",1,"0.50",to));
        var result=rules.reconcile("ticket",ticket(10,2,2,"1"),fills,from,to,false);assertEquals(MATCHED_TICKET_SLICE,result.status());assertEquals(n("0"),result.selectedFee());assertEquals(0,result.selectedFillCount());
        assertEquals(MATCHED_TICKET_SLICE,rules.reconcile("ticket",ticket(10,0,0,"0"),List.of(),from,to,false).status());
    }
    @Test void missingOrLimitedContextWithholdsEvenVisibleFees(){
        var result=rules.reconcile("ticket",ticket(2,2,2,"1"),List.of(fill("visible",0,"0.50",from)),from,to,false);
        assertEquals(INCOMPLETE,result.status());assertNull(result.selectedFee());assertNull(result.selectedFillCount());
        assertEquals(INCOMPLETE,rules.reconcile("ticket",ticket(1,1,1,"1"),List.of(fill("a",0,"1",from)),from,to,true).status());
    }
    @Test void outsideContextConflictAndInvalidMoneyCannotBeIgnored(){
        var fills=List.of(fill("before",0,"2",from.minusSeconds(1)),fill("inside",1,"1",from));
        assertEquals(MISMATCH,rules.reconcile("ticket",ticket(2,2,2,"2"),fills,from,to,false).status());
        var invalid=new TicketFeeSliceReconciler.Fill("bad","ticket",0,1,new BigDecimal("0.001"),from);
        assertEquals(MISMATCH,rules.reconcile("ticket",ticket(1,1,1,"0"),List.of(invalid),from,to,false).status());
        assertEquals(MISMATCH,rules.reconcile("ticket",ticket(1,1,1,"0"),List.of(fill("bad",0,"-1",from)),from,to,false).status());
    }
    @Test void duplicateIdsLegsOrMixedTicketEvidenceAreRejected(){
        for(var fills:List.of(List.of(fill("same",0,"1",from),fill("same",1,"1",from)),List.of(fill("a",0,"1",from),fill("b",0,"1",from)),List.of(fill("a",0,"1",from),new TicketFeeSliceReconciler.Fill("b","other",1,1,n("1"),from)))){
            var result=rules.reconcile("ticket",ticket(2,2,2,"2"),fills,from,to,false);assertEquals(MISMATCH,result.status());assertNull(result.selectedFee());
        }
    }
    @Test void boundsAndStableTieOrderKeepExactZeroFees(){
        var fills=List.of(fill("b",1,"0",from),fill("a",0,"0",from));
        assertEquals(List.of("a","b"),rules.reconcile("ticket",ticket(2,2,2,"0"),fills,from,to,false).selectedFills().stream().map(TicketFeeSliceEvidence.SelectedFill::fillId).toList());
        assertEquals(MISMATCH,rules.reconcile("ticket",ticket(2,2,2,"0"),fills,to,from,false).status());
        assertEquals(MISMATCH,rules.reconcile("ticket",ticket(2,2,2,"0"),fills,from.plusNanos(1),to,false).status());
        assertEquals(MISMATCH,rules.reconcile("ticket",ticket(2,2,2,"0"),fills,from,from.plus(Duration.ofDays(366)).plusSeconds(1),false).status());
    }
    @Test void thousandFullRowsFitAndSentinelInputIsUnavailable(){
        var fills=new ArrayList<TicketFeeSliceReconciler.Fill>();for(int i=0;i<1000;i++)fills.add(fill("f"+i,i,"0.01",from));
        var result=rules.reconcile("ticket",ticket(1000,1000,1000,"10"),fills,from,to,false);assertEquals(MATCHED_TICKET_SLICE,result.status());assertEquals(n("10"),result.selectedFee());
        fills.add(fill("extra",1000,"0.01",from));assertEquals(INCOMPLETE,rules.reconcile("ticket",ticket(1001,1001,1001,"10.01"),fills,from,to,false).status());
    }
    @Test void ownedTicketReaderProofAndSliceAgreeWithoutChangingSavedRows(){
        try(var f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:fee-slice-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true)){
            f.db.update("""
                INSERT INTO paper_tickets(id,user_id,request_id,side,product,variety,order_kind,validity,quantity,filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fees,policy_version,expires_at,created_at)
                VALUES('ticket',1,'request','BUY','CASH','ICEBERG','MARKET','DAY',3,3,2,2,1,10,0,'FILLED',1.50,'test',?,?)
                """,java.sql.Timestamp.from(to),java.sql.Timestamp.from(from.minusSeconds(1)));
            f.db.update("INSERT INTO paper_fills VALUES('old','ticket',1,0,1,10,0.50,NULL,?)",java.sql.Timestamp.from(from.minusSeconds(1)));
            f.db.update("INSERT INTO paper_fills VALUES('new','ticket',1,1,2,10,1.00,NULL,?)",java.sql.Timestamp.from(from));
            var before=f.db.queryForList("SELECT * FROM paper_fills ORDER BY id");
            var proof=f.context.getBean(TicketFeeService.class).read(f.owner.getEmail(),"ticket");assertEquals(TicketFeeEvidence.Verification.MATCHED,proof.verification());
            var raw=f.feeRepository.owned(1,"ticket");var fills=f.db.query("SELECT * FROM paper_fills WHERE ticket_id='ticket'",(r,row)->new TicketFeeSliceReconciler.Fill(r.getString("id"),r.getString("ticket_id"),r.getInt("leg_number"),r.getInt("quantity"),r.getBigDecimal("fee"),r.getTimestamp("created_at").toInstant()));
            var result=rules.reconcile("ticket",new TicketFeeSliceReconciler.Ticket(raw.quantity(),raw.filledUnits(),raw.filledLegs(),raw.fee()),fills,from,to,false);
            assertEquals(MATCHED_TICKET_SLICE,result.status());assertEquals(n("1"),result.selectedFee());assertEquals(n("1.50"),proof.reconciledFee());assertEquals(before,f.db.queryForList("SELECT * FROM paper_fills ORDER BY id"));
        }
    }
}
