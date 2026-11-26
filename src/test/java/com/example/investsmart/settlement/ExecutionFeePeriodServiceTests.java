package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import com.example.investsmart.exception.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class ExecutionFeePeriodServiceTests {
    JournalReportFixture f;ExecutionFeePeriodService service;final Instant from=JournalReportFixture.AT,to=from.plusSeconds(60);
    @BeforeEach void setup(){f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:execution-fee-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);service=f.context.getBean(ExecutionFeePeriodService.class);ticket("ticket",1,2,2,"1.50");fill("old","ticket",1,0,"0.50",from.minusSeconds(1));fill("new","ticket",1,1,"1.00",from);}
    @AfterEach void close(){f.close();}
    void ticket(String id,long user,int units,int legs,String fee){f.db.update("""
        INSERT INTO paper_tickets(id,user_id,request_id,side,product,variety,order_kind,validity,quantity,filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fees,policy_version,expires_at,created_at)
        VALUES(?,?,?,'BUY','CASH','ICEBERG','MARKET','DAY',?,?,?,?,1,10,0,'FILLED',?,'test',?,?)
        """,id,user,id,units,units,legs,legs,new BigDecimal(fee),java.sql.Timestamp.from(to),java.sql.Timestamp.from(from));}
    void fill(String id,String ticket,long user,int leg,String fee,Instant at){f.db.update("INSERT INTO paper_fills VALUES(?,?,?,?,1,10,?,NULL,?)",id,ticket,user,leg,new BigDecimal(fee),java.sql.Timestamp.from(at));}
    ExecutionFeePeriodEvidence read(){return service.read(f.owner.getEmail(),from,to);}
    void mis(String id){
        if(f.db.queryForObject("SELECT COUNT(*) FROM paper_intraday_positions WHERE id='position'",Integer.class)==0)
            f.db.update("INSERT INTO paper_intraday_positions(id,user_id,stock_id,direction,quantity,entry_price,collateral,realized_pnl,trade_date,created_at) VALUES('position',1,1,'LONG',1,10,5,0,?,?)",java.sql.Date.valueOf("2026-11-24"),java.sql.Timestamp.from(from));
        ticket(id,1,1,1,"1");f.db.update("UPDATE paper_tickets SET product='MIS',stock_id=1,position_id='position' WHERE id=?",id);
        fill("fill-"+id,id,1,0,"1",from);
        f.db.update("INSERT INTO paper_intraday_events VALUES(?,1,'position','OPEN',1,5,0,1,?)",id,java.sql.Timestamp.from(from.minusSeconds(1)));
    }
    @Test void ownedMisLinkCountsFillOnceEvenWhenEventIsOutsidePeriod(){
        mis("mis");var before=f.db.queryForList("SELECT * FROM paper_tickets ORDER BY id");var result=read();
        var slice=result.tickets().stream().filter(t->t.ticketId().equals("mis")).findFirst().orElseThrow();
        assertEquals(TicketFeeSliceEvidence.Status.MATCHED_TICKET_SLICE,slice.status());assertEquals(new BigDecimal("1.00"),slice.selectedFee());
        assertFalse(result.limitations().contains("MIS_EVENT_LINKAGE_NOT_CHECKED"));assertTrue(result.limitations().contains("MIS_EVENT_ONLY_HISTORY_NOT_SELECTED"));
        assertEquals(before,f.db.queryForList("SELECT * FROM paper_tickets ORDER BY id"));
        f.db.update("UPDATE paper_intraday_events SET fee=9 WHERE event_key='mis'");
        assertNull(read().tickets().stream().filter(t->t.ticketId().equals("mis")).findFirst().orElseThrow().selectedFee());
    }
    @Test void missingForeignAndConflictingMisLinksWithholdWithoutLeaking(){
        mis("mis");f.db.update("UPDATE paper_intraday_events SET user_id=2,position_id='private-position' WHERE event_key='mis'");
        var result=read();assertFalse(result.toString().contains("private-position"));
        assertEquals("MIS_EVENT_OWNERSHIP_CONFLICT",result.tickets().stream().filter(t->t.ticketId().equals("mis")).findFirst().orElseThrow().limitation());
        f.db.update("DELETE FROM paper_intraday_events WHERE event_key='mis'");
        assertEquals(TicketFeeSliceEvidence.Status.INCOMPLETE,read().tickets().stream().filter(t->t.ticketId().equals("mis")).findFirst().orElseThrow().status());
        f.db.update("UPDATE paper_intraday_positions SET user_id=2 WHERE id='position'");
        assertNull(read().tickets().stream().filter(t->t.ticketId().equals("mis")).findFirst().orElseThrow().selectedFee());
        assertEquals(2,read().selectedFills().size());
    }
    @Test void misContextBatchesAndGlobalLimitRetainRawRows(){
        for(int i=0;i<101;i++)mis("m"+i);var result=read();assertEquals(102,result.selectedFills().size());
        verify(f.executionFeeRepository,times(2)).misContext(eq(1L),anyList(),anyInt());
        var row=new ExecutionFeePeriodRepository.MisContext("m0",null,null,false);
        doReturn(Collections.nCopies(1001,row)).when(f.executionFeeRepository).misContext(eq(1L),anyList(),anyInt());
        var limited=read();assertTrue(limited.limitations().contains("MIS_CONTEXT_LIMIT"));assertEquals(102,limited.selectedFills().size());
        assertTrue(limited.tickets().stream().filter(t->t.ticketId().startsWith("m")).allMatch(t->t.selectedFee()==null));
    }
    @Test void misReadSharesSelectionSnapshotAndSkipsContextWhenSelectionLimited(){
        mis("mis");var source=f.context.getBean(javax.sql.DataSource.class);Object[] resource={null};
        doAnswer(call->{resource[0]=TransactionSynchronizationManager.getResource(source);return call.callRealMethod();}).when(f.executionFeeRepository).selected(1,from,to);
        doAnswer(call->{assertSame(resource[0],TransactionSynchronizationManager.getResource(source));assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());assertEquals(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());return call.callRealMethod();}).when(f.executionFeeRepository).misContext(eq(1L),anyList(),anyInt());
        read();verifyNoInteractions(f.feeRepository);
    }
    @Test void halfOpenSelectionReconcilesWholeTicketWithoutDoubleCounting(){
        var before=f.db.queryForList("SELECT * FROM users ORDER BY id");var result=read();assertEquals(1,result.selectedFills().size());assertEquals("new",result.selectedFills().getFirst().fillId());assertEquals(new BigDecimal("1.00"),result.tickets().getFirst().selectedFee());assertEquals(1,result.tickets().getFirst().outsidePeriodFillCount());assertFalse(result.completeFinancialHistory());assertEquals(before,f.db.queryForList("SELECT * FROM users ORDER BY id"));
        assertTrue(service.read(f.owner.getEmail(),from.minusSeconds(1),from).selectedFills().stream().allMatch(r->r.fillId().equals("old")));
    }
    @Test void foreignParentAndMissingTicketsRetainOnlyOwnedRawEvidence(){
        ticket("private-ticket",2,1,1,"1");fill("private-fill","private-ticket",2,0,"1",from);fill("wrong-link","private-ticket",1,1,"2",from);fill("orphan","absent",1,0,"3",from);
        var result=read();assertEquals(3,result.selectedFills().size());assertEquals(2,result.selectedFills().stream().filter(r->r.ticketId()==null).count());assertFalse(result.toString().contains("private-ticket"));assertFalse(result.toString().contains("private-fill"));assertTrue(result.limitations().contains("OWNED_TICKET_UNAVAILABLE"));
        assertThrows(ResourceNotFoundException.class,()->service.read(null,from,to));f.owner.setAccountStatus("SUSPENDED");assertThrows(org.springframework.web.server.ResponseStatusException.class,()->read());
    }
    @Test void foreignAndConflictingOutsideContextWithholdSlice(){
        f.db.update("UPDATE paper_fills SET user_id=2 WHERE id='old'");var result=read();assertEquals(TicketFeeSliceEvidence.Status.MISMATCH,result.tickets().getFirst().status());assertNull(result.tickets().getFirst().selectedFee());assertFalse(result.toString().contains("old"));
        f.db.update("UPDATE paper_fills SET user_id=1,fee=9 WHERE id='old'");assertEquals(TicketFeeSliceEvidence.Status.MISMATCH,read().tickets().getFirst().status());
    }
    @Test void batchesFullContextAndStopsAtGlobalSentinel(){
        for(int i=0;i<101;i++){var key="t"+i;ticket(key,1,1,1,"1");fill("f"+i,key,1,0,"1",from);}
        assertEquals(102,read().tickets().size());verify(f.executionFeeRepository,times(2)).context(anyList(),anyInt());
        var row=new ExecutionFeePeriodRepository.Context(1,new TicketFeeSliceReconciler.Fill("x","ticket",0,1,new BigDecimal("1"),from));doReturn(Collections.nCopies(1001,row)).when(f.executionFeeRepository).context(anyList(),anyInt());
        var limited=read();assertTrue(limited.contextTruncated());assertEquals(102,limited.selectedFills().size());assertTrue(limited.tickets().stream().allMatch(t->t.selectedFee()==null));
    }
    @Test void selectedLimitAndInvalidBoundsFailConservatively(){
        f.db.batchUpdate("INSERT INTO paper_fills VALUES(?,'missing',1,?,1,10,1,NULL,?)",new org.springframework.jdbc.core.BatchPreparedStatementSetter(){public int getBatchSize(){return 1000;}public void setValues(java.sql.PreparedStatement p,int i)throws java.sql.SQLException{p.setString(1,"extra"+i);p.setInt(2,i);p.setTimestamp(3,java.sql.Timestamp.from(from));}});
        var result=read();assertTrue(result.selectedTruncated());assertEquals(1000,result.selectedFills().size());assertTrue(result.tickets().stream().allMatch(t->t.selectedFee()==null));verify(f.executionFeeRepository,never()).context(anyList(),anyInt());
        assertThrows(BadRequestException.class,()->service.read(f.owner.getEmail(),to,from));assertThrows(BadRequestException.class,()->service.read(f.owner.getEmail(),from.plusNanos(1),to));
    }
    @Test void contextRunsOnSameIndependentReadOnlySnapshotWithoutTicketServiceLoop(){
        var source=f.context.getBean(javax.sql.DataSource.class);Object[] resource={null};
        f.write.execute(status->{var outer=TransactionSynchronizationManager.getResource(source);doAnswer(call->{resource[0]=TransactionSynchronizationManager.getResource(source);assertNotSame(outer,resource[0]);assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());return call.callRealMethod();}).when(f.executionFeeRepository).selected(1,from,to);
            doAnswer(call->{assertSame(resource[0],TransactionSynchronizationManager.getResource(source));return call.callRealMethod();}).when(f.executionFeeRepository).context(anyList(),anyInt());read();assertSame(outer,TransactionSynchronizationManager.getResource(source));return null;});
        verifyNoInteractions(f.feeRepository);verify(f.executionFeeRepository,times(1)).selected(1,from,to);verify(f.executionFeeRepository,times(1)).context(anyList(),anyInt());
    }
}
