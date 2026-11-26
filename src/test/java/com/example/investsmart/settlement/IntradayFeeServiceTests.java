package com.example.investsmart.settlement;

import com.example.investsmart.exception.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.example.investsmart.dto.IntradayFeeEvidence.Verification.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IntradayFeeServiceTests {
    JournalReportFixture f;
    IntradayFeeService service;
    @BeforeEach void setup(){
        f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:intraday-fees-"+UUID.randomUUID()
                +";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);
        service=f.context.getBean(IntradayFeeService.class);
        f.db.update("""
                INSERT INTO paper_intraday_positions(id,user_id,stock_id,direction,quantity,entry_price,
                collateral,realized_pnl,trade_date,created_at) VALUES('owned',1,1,'LONG',10,100,500,0,?,?)
                """,java.sql.Date.valueOf("2026-11-24"),Timestamp.from(JournalReportFixture.AT));
        linked("open","OPEN","BUY",10,"500","0","2");
    }
    @AfterEach void close(){f.close();}
    @Test void suppliedMisSliceAgreesWithOwnedFeeReaderWithoutChangingStoredRows(){
        var before=f.db.queryForList("SELECT * FROM paper_tickets ORDER BY id");
        var owned=service.read(f.owner.getEmail(),"owned");
        var rules=new IntradayTicketSliceReconciler(new TicketFeeSliceReconciler());
        var ticket=f.db.queryForObject("SELECT * FROM paper_tickets WHERE id='open'",(r,n)->
                new IntradayTicketSliceReconciler.Ticket(r.getString("id"),r.getString("position_id"),r.getLong("stock_id"),r.getString("product"),r.getString("side"),r.getString("status"),
                        new TicketFeeSliceReconciler.Ticket(r.getInt("quantity"),r.getInt("filled_units"),r.getInt("filled_legs"),r.getBigDecimal("fees"))));
        var events=f.db.query("SELECT * FROM paper_intraday_events WHERE event_key='open'",(r,n)->
                new IntradayTicketSliceReconciler.Event(r.getString("event_key"),r.getString("position_id"),r.getString("event_type"),r.getInt("units"),r.getBigDecimal("fee")));
        var fills=f.db.query("SELECT * FROM paper_fills WHERE ticket_id='open'",(r,n)->
                new TicketFeeSliceReconciler.Fill(r.getString("id"),r.getString("ticket_id"),r.getInt("leg_number"),r.getInt("quantity"),r.getBigDecimal("fee"),r.getTimestamp("created_at").toInstant()));
        var result=rules.reconcile(ticket,new IntradayTicketSliceReconciler.Position("owned",1,"LONG"),events,fills,JournalReportFixture.AT,JournalReportFixture.AT.plusSeconds(1),false);
        assertEquals(com.example.investsmart.dto.IntradayTicketSliceEvidence.Status.MATCHED_EVENT_TICKET_SLICE,result.status());
        assertEquals(owned.reconciledEventFees(),result.slice().selectedFee());
        assertEquals(before,f.db.queryForList("SELECT * FROM paper_tickets ORDER BY id"));
        assertEquals(new BigDecimal("1234.56"),f.db.queryForObject("SELECT virtual_balance FROM users WHERE id=1",BigDecimal.class));
    }
    void linked(String id,String kind,String side,int units,String collateral,String pnl,String fee){
        var at=Timestamp.from(JournalReportFixture.AT);
        f.db.update("INSERT INTO paper_intraday_events VALUES(?,1,'owned',?,?,?,?,?,?)",id,kind,units,
                new BigDecimal(collateral),new BigDecimal(pnl),new BigDecimal(fee),at);
        f.db.update("""
                INSERT INTO paper_tickets(id,user_id,request_id,stock_id,position_id,side,product,variety,order_kind,
                validity,quantity,filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fees,policy_version,expires_at,created_at)
                VALUES(?,1,?,1,'owned',?,'MIS','REGULAR','MARKET','DAY',?,?,1,1,1,100,0,'FILLED',?,'test',?,?)
                """,id,id,side,units,units,new BigDecimal(fee),at,at);
        f.db.update("INSERT INTO paper_fills(id,ticket_id,user_id,leg_number,quantity,price,fee,created_at) VALUES(?,?,1,0,?,100,?,?)",
                "fill-"+id,id,units,new BigDecimal(fee),at);
    }
    @Test void reconcilesEventOrderAndFillFeesOnceAndDoesNotWrite(){
        linked("close","CLOSE","SELL",4,"-200","40","3");
        f.db.update("UPDATE paper_intraday_positions SET quantity=6,collateral=300,realized_pnl=40");
        var before=f.db.queryForList("SELECT * FROM paper_tickets ORDER BY id");
        var result=service.read("OWNER@example.invalid","owned");
        assertEquals(MATCHED_EVENT_FEES,result.verification());assertEquals(new BigDecimal("5.00"),result.reconciledEventFees());
        assertEquals(new BigDecimal("40.00"),result.positionComparison().verifiedGrossRealizedPnl());
        assertEquals(before,f.db.queryForList("SELECT * FROM paper_tickets ORDER BY id"));
        assertEquals(new BigDecimal("1234.56"),f.db.queryForObject("SELECT virtual_balance FROM users WHERE id=1",BigDecimal.class));
        assertEquals(0,f.db.queryForObject("SELECT COUNT(*) FROM settlement_accounts",Integer.class));
    }
    @Test void missingFillOrTicketIsIncomplete(){
        f.db.update("DELETE FROM paper_fills");
        assertEquals(INCOMPLETE,service.read(f.owner.getEmail(),"owned").verification());
        assertNull(service.read(f.owner.getEmail(),"owned").reconciledEventFees());
        f.db.update("DELETE FROM paper_tickets");
        assertEquals("TICKET_MISSING",service.read(f.owner.getEmail(),"owned").reason());
    }
    @Test void corruptTicketOwnershipProductStockSideAndCoverageNeverMatch(){
        for(String change:List.of("user_id=2","product='CASH'","stock_id=2","position_id='other'","side='SELL'",
                "status='PENDING'","quantity=11","filled_units=9","filled_legs=2")){
            f.db.update("UPDATE paper_tickets SET "+change);
            assertEquals(MISMATCH,service.read(f.owner.getEmail(),"owned").verification());
            assertNull(service.read(f.owner.getEmail(),"owned").reconciledEventFees());
            f.db.update("UPDATE paper_tickets SET user_id=1,product='MIS',stock_id=1,position_id='owned',side='BUY',status='FILLED',quantity=10,filled_units=10,filled_legs=1");
        }
    }
    @Test void conflictingFeesForeignFillsAndDuplicateFillsWithholdAmount(){
        f.db.update("UPDATE paper_tickets SET fees=3");assertEquals(MISMATCH,service.read(f.owner.getEmail(),"owned").verification());
        f.db.update("UPDATE paper_tickets SET fees=2");
        for(String change:List.of("fee=3","user_id=2","quantity=9","leg_number=1")){
            f.db.update("UPDATE paper_fills SET "+change);
            assertEquals(MISMATCH,service.read(f.owner.getEmail(),"owned").verification());
            assertNull(service.read(f.owner.getEmail(),"owned").reconciledEventFees());
            f.db.update("UPDATE paper_fills SET fee=2,user_id=1,quantity=10,leg_number=0");
        }
    }
    @Test void joinRowBoundSuppressesTruncatedEvidenceAndExtraMisFills(){
        var args=new ArrayList<Object[]>();for(int i=1;i<1000;i++)args.add(new Object[]{"extra-"+i,i,Timestamp.from(JournalReportFixture.AT)});
        f.db.batchUpdate("INSERT INTO paper_fills(id,ticket_id,user_id,leg_number,quantity,price,fee,created_at) VALUES(?,'open',1,?,10,100,2,?)",args);
        assertEquals(MISMATCH,service.read(f.owner.getEmail(),"owned").verification());
        f.db.update("INSERT INTO paper_fills(id,ticket_id,user_id,leg_number,quantity,price,fee,created_at) VALUES('sentinel','open',1,1000,10,100,2,?)",Timestamp.from(JournalReportFixture.AT));
        var result=service.read(f.owner.getEmail(),"owned");assertEquals(INCOMPLETE,result.verification());
        assertEquals("DETAIL_LIMIT",result.reason());assertNull(result.reconciledEventFees());
    }
    @Test void validatesOwnershipAndPositionEvidenceBeforeLinksAndUsesOneSnapshot(){
        clearInvocations(f.intradayFeeRepository);
        assertThrows(ResourceNotFoundException.class,()->service.read(f.other.getEmail(),"owned"));
        assertThrows(BadRequestException.class,()->service.read(f.owner.getEmail(),"x".repeat(37)));
        assertThrows(ResourceNotFoundException.class,()->service.read(null,"owned"));
        f.db.update("UPDATE paper_intraday_positions SET collateral=499");
        assertEquals(MISMATCH,service.read(f.owner.getEmail(),"owned").verification());
        verifyNoInteractions(f.intradayFeeRepository);
        f.db.update("UPDATE paper_intraday_positions SET collateral=500");
        doAnswer(call->{assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            return call.callRealMethod();}).when(f.intradayFeeRepository).links(1,"owned");
        f.write.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
        f.write.executeWithoutResult(status->{assertEquals(MATCHED_EVENT_FEES,service.read(f.owner.getEmail(),"owned").verification());
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());});
        f.owner.setAccountStatus("SUSPENDED");
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.read(f.owner.getEmail(),"owned"));
    }
}
