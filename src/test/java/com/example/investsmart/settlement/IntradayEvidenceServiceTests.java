package com.example.investsmart.settlement;

import com.example.investsmart.exception.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.example.investsmart.settlement.IntradayEvidenceReconciler.Verification.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IntradayEvidenceServiceTests {
    JournalReportFixture f;
    IntradayEvidenceService service;
    @BeforeEach void setup() {
        f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:intraday-report-"+UUID.randomUUID()
                +";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);
        service=f.context.getBean(IntradayEvidenceService.class);
        f.db.update("""
                INSERT INTO paper_intraday_positions(id,user_id,stock_id,direction,quantity,entry_price,
                collateral,realized_pnl,trade_date,created_at) VALUES('owned',1,1,'LONG',0,100,0,20,?,?)
                """,java.sql.Date.valueOf("2026-11-24"),Timestamp.from(JournalReportFixture.AT));
    }
    @AfterEach void close(){f.close();}
    void event(String key,long user,String kind,int units,String collateral,String pnl,String fee) {
        f.db.update("INSERT INTO paper_intraday_events VALUES(?,?,'owned',?,?,?,?,?,?)",key,user,kind,units,
                new BigDecimal(collateral),new BigDecimal(pnl),new BigDecimal(fee),Timestamp.from(JournalReportFixture.AT));
    }
    void complete(){event("a",1,"OPEN",10,"500","0","0.30");event("b",1,"CLOSE",4,"-200","-40","0.11");event("c",1,"CLOSE",6,"-300","60","0.20");}
    @Test void reconcilesOwnedClosedPositionWithoutWritingOrInitializing() {
        complete();var positions=f.db.queryForList("SELECT * FROM paper_intraday_positions");
        var events=f.db.queryForList("SELECT * FROM paper_intraday_events");
        var result=service.read("OWNER@example.invalid","owned");
        assertEquals(MATCHED_POSITION_TOTALS,result.comparison().verification());
        assertEquals(new BigDecimal("20.00"),result.comparison().verifiedGrossRealizedPnl());
        assertEquals(new BigDecimal("0.61"),result.comparison().eventFeeSum());
        assertEquals(positions,f.db.queryForList("SELECT * FROM paper_intraday_positions"));
        assertEquals(events,f.db.queryForList("SELECT * FROM paper_intraday_events"));
        assertEquals(new BigDecimal("1234.56"),f.db.queryForObject("SELECT virtual_balance FROM users WHERE id=1",BigDecimal.class));
        assertEquals(0,f.db.queryForObject("SELECT COUNT(*) FROM settlement_accounts",Integer.class));
    }
    @Test void blocksForeignMissingAndInactiveUsersAndExcludesForeignEventLinks() {
        complete();event("foreign",2,"CLOSE",99,"-999","999","999");
        assertEquals(new BigDecimal("20.00"),service.read(f.owner.getEmail(),"owned").comparison().verifiedGrossRealizedPnl());
        assertThrows(ResourceNotFoundException.class,()->service.read(f.other.getEmail(),"owned"));
        assertThrows(ResourceNotFoundException.class,()->service.read(f.owner.getEmail(),"missing"));
        f.owner.setAccountStatus("SUSPENDED");
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.read(f.owner.getEmail(),"owned"));
    }
    @Test void incompleteUnknownAndConflictingEvidenceWithholdsNumbers() {
        assertEquals(INCOMPLETE,service.read(f.owner.getEmail(),"owned").comparison().verification());
        complete();f.db.update("UPDATE paper_intraday_positions SET realized_pnl=21");
        assertEquals(MISMATCH,service.read(f.owner.getEmail(),"owned").comparison().verification());
        assertNull(service.read(f.owner.getEmail(),"owned").comparison().eventFeeSum());
        f.db.update("UPDATE paper_intraday_events SET event_type='FUTURE' WHERE event_key='b'");
        assertEquals(INCOMPLETE,service.read(f.owner.getEmail(),"owned").comparison().verification());
    }
    @Test void preservesOpenPositionAndLargeExactFees() {
        f.db.update("UPDATE paper_intraday_positions SET quantity=10,collateral=500,realized_pnl=0");
        event("open",1,"OPEN",10,"500","0","99999999999999999.99");
        var result=service.read(f.owner.getEmail(),"owned").comparison();
        assertEquals(MATCHED_POSITION_TOTALS,result.verification());
        assertEquals(new BigDecimal("99999999999999999.99"),result.eventFeeSum());
    }
    @Test void thousandAndOneEventsSuppressPartialTotals() {
        f.db.update("UPDATE paper_intraday_positions SET quantity=1,collateral=0,realized_pnl=0");
        event("open",1,"OPEN",1000,"0","0","0");
        var args=new ArrayList<Object[]>();for(int i=0;i<999;i++)args.add(new Object[]{"close-"+i,Timestamp.from(JournalReportFixture.AT)});
        f.db.batchUpdate("INSERT INTO paper_intraday_events VALUES(?,1,'owned','CLOSE',1,0,0,0,?)",args);
        assertEquals(MATCHED_POSITION_TOTALS,service.read(f.owner.getEmail(),"owned").comparison().verification());
        event("extra",1,"CLOSE",1,"0","0","0");
        var result=service.read(f.owner.getEmail(),"owned").comparison();
        assertEquals(INCOMPLETE,result.verification());assertNull(result.verifiedGrossRealizedPnl());assertNull(result.eventFeeSum());
    }
    @Test void validatesBeforeQueriesAndUsesSeparateReadOnlySnapshot() {
        clearInvocations(f.users,f.intradayRepository);
        for(String id:new String[]{null," ","x".repeat(37)})assertThrows(BadRequestException.class,()->service.read(f.owner.getEmail(),id));
        verifyNoInteractions(f.users,f.intradayRepository);
        for(String email:new String[]{null," ","unknown@example.invalid"})assertThrows(ResourceNotFoundException.class,()->service.read(email,"owned"));
        verifyNoInteractions(f.intradayRepository);
        doAnswer(call->{assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            return call.callRealMethod();}).when(f.intradayRepository).events(1,"owned");
        f.write.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
        f.write.executeWithoutResult(status->{service.read(f.owner.getEmail(),"owned");
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_READ_COMMITTED,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());});
    }
}
