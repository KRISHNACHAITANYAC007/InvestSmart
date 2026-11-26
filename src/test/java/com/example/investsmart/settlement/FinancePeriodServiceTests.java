package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import com.example.investsmart.exception.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FinancePeriodServiceTests {
    JournalReportFixture f;FinancePeriodService service;
    final Instant from=JournalReportFixture.AT,to=from.plusSeconds(60);
    @BeforeEach void setup(){f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:finance-period-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);service=f.context.getBean(FinancePeriodService.class);position("open",1,5);position("closed",1,0);position("private-position",2,5);}
    @AfterEach void close(){f.close();}
    void position(String id,long user,int units){f.db.update("INSERT INTO paper_financing(id,user_id,stock_id,product,quantity,cost_amount,principal,interest,interest_rate,created_at,last_accrued_date) VALUES(?,?,1,'MTF',?,100,75,99,0.1,?,'2026-11-23')",id,user,units,java.sql.Timestamp.from(from));}
    void event(String key,String position,long user,String type,String principal,String amount,Instant at){f.db.update("INSERT INTO paper_finance_events VALUES(?,?,?,?,?,?,?)",key,position,user,type,new BigDecimal(principal),new BigDecimal(amount),java.sql.Timestamp.from(at));}
    FinancePeriodEvidence read(){return service.read(f.owner.getEmail(),from,to);}
    @Test void ownedEventsAcrossOpenAndClosedPositionsKeepMeaningsSeparate(){
        event("a","open",1,"BORROW","75","25",from);event("b","open",1,"INTEREST","0","2.50",from);event("c","closed",1,"REPAY","-75","2.50",from);
        var before=f.db.queryForList("SELECT * FROM paper_financing ORDER BY id");var result=read();assertEquals(3,result.entries().size());
        assertEquals(new BigDecimal("25.00"),result.entries().get(0).interpretation().contribution());assertNull(result.entries().get(0).interpretation().interestAccrued());
        assertEquals(new BigDecimal("2.50"),result.entries().get(1).interpretation().interestAccrued());assertNull(result.entries().get(1).interpretation().interestPaid());
        assertEquals(new BigDecimal("2.50"),result.entries().get(2).interpretation().interestPaid());assertFalse(result.completeFinancialHistory());
        assertEquals(before,f.db.queryForList("SELECT * FROM paper_financing ORDER BY id"));assertThrows(UnsupportedOperationException.class,()->result.entries().clear());
    }
    @Test void halfOpenRangeAndStableTiesExcludeForeignRows(){
        event("before","open",1,"INTEREST","0","1",from.minusSeconds(1));event("b","open",1,"INTEREST","0","2",from);event("a","closed",1,"INTEREST","0","3",from);event("end","open",1,"INTEREST","0","4",to);event("foreign","private-position",2,"INTEREST","0","999",from);
        assertEquals(List.of("a","b"),read().entries().stream().map(FinancePeriodEvidence.Entry::eventKey).toList());
        assertEquals(List.of("end"),service.read(f.owner.getEmail(),to,to.plusSeconds(60)).entries().stream().map(FinancePeriodEvidence.Entry::eventKey).toList());
    }
    @Test void missingForeignPositionsAndUnknownInvalidEventsRemainExplicit(){
        event("missing","absent",1,"INTEREST","0","2",from);event("wrong-owner","private-position",1,"BORROW","75","25",from);event("unknown","open",1,"FUTURE","0","1",from);event("invalid","closed",1,"BORROW","-1","1",from);
        var result=read();assertEquals(4,result.entries().size());
        assertEquals(2,result.entries().stream().filter(e->e.ownership()==FinancePeriodEvidence.Ownership.POSITION_UNAVAILABLE&&e.positionId()==null&&e.interpretation()==null).count());
        assertFalse(result.toString().contains("private-position"));assertTrue(result.limitations().containsAll(List.of("POSITION_EVIDENCE_UNAVAILABLE","UNSUPPORTED_EVENT_TYPE","INVALID_EVENT_VALUES")));
    }
    @Test void initializationDoesNotCreateHistoryAndUnknownUsersAreDenied(){
        event("owned","open",1,"INTEREST","0","1",from);assertFalse(read().initialization().initialized());assertEquals(0,f.db.queryForObject("SELECT COUNT(*) FROM settlement_accounts",Integer.class));
        f.account(1);assertTrue(service.read(f.owner.getEmail(),from.minusSeconds(1),to).startsBeforeInitialization());assertFalse(read().completeFinancialHistory());
        assertThrows(ResourceNotFoundException.class,()->service.read(null,from,to));assertThrows(ResourceNotFoundException.class,()->service.read("unknown@example.invalid",from,to));
        f.owner.setAccountStatus("SUSPENDED");assertThrows(org.springframework.web.server.ResponseStatusException.class,()->read());
    }
    @Test void invalidBoundsFailBeforeQueries(){
        clearInvocations(f.users,f.repository,f.financePeriodRepository);
        for(var bounds:new Instant[][]{{null,to},{from,null},{from,from},{to,from},{from.plusNanos(1),to},{from,from.plus(Duration.ofDays(366)).plusSeconds(1)},{Instant.MIN,to},{from,Instant.MAX}})assertThrows(BadRequestException.class,()->service.read(f.owner.getEmail(),bounds[0],bounds[1]));
        verifyNoInteractions(f.users,f.repository,f.financePeriodRepository);assertNotNull(service.read(f.owner.getEmail(),from,from.plus(Duration.ofDays(366))));
    }
    @Test void exactly1000EventsFitAndOverflowIsExplicit(){
        f.db.batchUpdate("INSERT INTO paper_finance_events VALUES(?,'open',1,'INTEREST',0,1,?)",new org.springframework.jdbc.core.BatchPreparedStatementSetter(){public int getBatchSize(){return 1000;}public void setValues(java.sql.PreparedStatement p,int i)throws java.sql.SQLException{p.setString(1,String.format("e%04d",i));p.setTimestamp(2,java.sql.Timestamp.from(from));}});
        assertEquals(1000,read().entries().size());assertFalse(read().truncated());event("extra","closed",1,"INTEREST","0","1",from);
        var result=read();assertEquals(1000,result.entries().size());assertTrue(result.truncated());assertTrue(result.limitations().contains("ROW_LIMIT_EXCEEDED_FIRST_1000_ONLY"));assertFalse(result.completeFinancialHistory());
    }
    @Test void initializationAndEventsShareIndependentReadOnlySnapshotWithoutPositionLoop(){
        event("owned","open",1,"INTEREST","0","1",from);var source=f.context.getBean(javax.sql.DataSource.class);Object[] readResource={null};
        f.write.execute(status->{var outer=TransactionSynchronizationManager.getResource(source);
            doAnswer(call->{readResource[0]=TransactionSynchronizationManager.getResource(source);assertNotSame(outer,readResource[0]);assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());assertEquals(TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());return call.callRealMethod();}).when(f.repository).coverage(1);
            doAnswer(call->{assertSame(readResource[0],TransactionSynchronizationManager.getResource(source));return call.callRealMethod();}).when(f.financePeriodRepository).entries(1,from,to);
            assertEquals(1,read().entries().size());assertSame(outer,TransactionSynchronizationManager.getResource(source));return null;});
        verify(f.repository,times(1)).coverage(1);verify(f.financePeriodRepository,times(1)).entries(1,from,to);verifyNoInteractions(f.financeRepository);
    }
}
