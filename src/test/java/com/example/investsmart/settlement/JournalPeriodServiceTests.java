package com.example.investsmart.settlement;

import com.example.investsmart.exception.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JournalPeriodServiceTests {
    JournalReportFixture f;JournalPeriodService service;
    final Instant from=JournalReportFixture.AT,to=from.plusSeconds(60);
    @BeforeEach void setup(){
        f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:period-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);
        service=f.context.getBean(JournalPeriodService.class);
    }
    @AfterEach void close(){f.close();}
    @Test void halfOpenBoundaryAndSameTimestampOrderingExcludeForeignRows(){
        f.account(1);f.entry(1,"before","WITHDRAW","1",from.minusSeconds(1));
        f.entry(1,"b","WITHDRAW","2",from);f.entry(1,"a","SETTLED_CREDIT","3",from);
        f.entry(1,"end","WITHDRAW","4",to);f.entry(2,"foreign","WITHDRAW","5",from);
        var result=service.read("OWNER@example.invalid",from,to);
        assertEquals(List.of("a","b"),result.entries().stream().map(e->e.raw().eventKey()).toList());
        assertEquals(new java.math.BigDecimal("-2.00"),result.entries().get(1).interpretation().settledDelta());
        assertEquals(List.of("end"),service.read(f.owner.getEmail(),to,to.plusSeconds(60)).entries().stream().map(e->e.raw().eventKey()).toList());
        assertFalse(result.truncated());assertFalse(result.completeFinancialHistory());
        assertThrows(UnsupportedOperationException.class,()->result.entries().clear());
        assertThrows(UnsupportedOperationException.class,()->result.limitations().clear());
    }
    @Test void initializationDoesNotProveHistoryAndMissingInitializationDoesNotHideRows(){
        f.entry(1,"saved","WITHDRAW","1",from);
        var uninitialized=service.read(f.owner.getEmail(),from,to);assertFalse(uninitialized.initialization().initialized());
        assertEquals(1,uninitialized.entries().size());assertTrue(uninitialized.limitations().contains("ACCOUNT_NOT_INITIALIZED"));
        assertEquals(0,f.db.queryForObject("SELECT COUNT(*) FROM settlement_accounts",Integer.class));
        f.account(1);var earlier=service.read(f.owner.getEmail(),from.minusSeconds(1),to);
        assertTrue(earlier.startsBeforeInitialization());assertFalse(earlier.completeFinancialHistory());
        assertTrue(earlier.limitations().contains("RANGE_STARTS_BEFORE_INITIALIZATION"));
        assertFalse(service.read(f.owner.getEmail(),from,to).startsBeforeInitialization());
    }
    @Test void malformedUnsupportedAndSourceDependentRowsRemainVisible(){
        f.entry(1,"unknown","FUTURE_ACTION","7",from);f.entry(1,"invalid","WITHDRAW","-1",from);
        f.entry(1,"refund","RELEASE","2",from);
        f.db.update("UPDATE settlement_journal SET reference_kind='ORDER',reference_id='order' WHERE event_key='refund'");
        var result=service.read(f.owner.getEmail(),from,to);assertEquals(3,result.entries().size());
        var byKey=new HashMap<String,com.example.investsmart.dto.JournalPeriodEvidence.Entry>();result.entries().forEach(e->byKey.put(e.raw().eventKey(),e));
        assertEquals("FUTURE_ACTION",byKey.get("unknown").raw().action());
        assertEquals(JournalActionClassifier.Kind.UNSUPPORTED,byKey.get("unknown").interpretation().kind());
        assertEquals(JournalActionClassifier.Kind.INVALID,byKey.get("invalid").interpretation().kind());
        assertNull(byKey.get("refund").interpretation().settledDelta());
    }
    @Test void invalidBoundsAreRejectedBeforeAnyRepositoryRead(){
        for(var bounds:new Instant[][]{{null,to},{from,null},{from,from},{to,from},{from.plusNanos(1),to},{from,to.plusNanos(1)},
                {from,from.plus(Duration.ofDays(366)).plusSeconds(1)},{Instant.MIN,to},{from,Instant.MAX}})
            assertThrows(BadRequestException.class,()->service.read(f.owner.getEmail(),bounds[0],bounds[1]));
        verifyNoInteractions(f.users,f.repository,f.periodRepository);
        assertNotNull(service.read(f.owner.getEmail(),from,from.plus(Duration.ofDays(366))));
    }
    @Test void ownershipAndInactiveIdentityCannotBeSuppliedByCaller(){
        f.entry(2,"other","WITHDRAW","1",from);
        assertTrue(service.read(f.owner.getEmail(),from,to).entries().isEmpty());
        assertEquals(1,service.read(f.other.getEmail(),from,to).entries().size());
        assertThrows(ResourceNotFoundException.class,()->service.read(null,from,to));
        assertThrows(ResourceNotFoundException.class,()->service.read("missing@example.invalid",from,to));
        f.owner.setAccountStatus("SUSPENDED");
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.read(f.owner.getEmail(),from,to));
    }
    @Test void exactly1000RowsFitsAnd1001IsExplicitlyTruncatedWithoutInventingTotals(){
        f.db.batchUpdate("INSERT INTO settlement_journal VALUES(1,?,'WITHDRAW',?,1,NULL,NULL,?)",new org.springframework.jdbc.core.BatchPreparedStatementSetter(){
            public int getBatchSize(){return 1000;}public void setValues(java.sql.PreparedStatement p,int i)throws java.sql.SQLException{
                p.setString(1,String.format("e%04d",i));p.setString(2,"a".repeat(64));p.setTimestamp(3,java.sql.Timestamp.from(from));
            }
        });
        var exact=service.read(f.owner.getEmail(),from,to);assertEquals(1000,exact.entries().size());assertFalse(exact.truncated());
        f.entry(1,"zzz","WITHDRAW","1",from);
        var limited=service.read(f.owner.getEmail(),from,to);assertEquals(1000,limited.entries().size());assertTrue(limited.truncated());
        assertTrue(limited.limitations().contains("ROW_LIMIT_EXCEEDED_FIRST_1000_ONLY"));assertFalse(limited.completeFinancialHistory());
    }
    @Test void coverageAndRowsShareOneIndependentReadOnlyRepeatableSnapshotWithoutWrites(){
        f.account(1);f.entry(1,"saved","WITHDRAW","1",from);var before=f.db.queryForList("SELECT * FROM users ORDER BY id");
        var source=f.context.getBean(javax.sql.DataSource.class);var readResource=new Object[1];
        f.write.execute(status->{
            var outer=TransactionSynchronizationManager.getResource(source);
            doAnswer(call->{
                assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
                assertEquals(TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
                readResource[0]=TransactionSynchronizationManager.getResource(source);assertNotSame(outer,readResource[0]);return call.callRealMethod();
            }).when(f.repository).coverage(1);
            doAnswer(call->{assertSame(readResource[0],TransactionSynchronizationManager.getResource(source));return call.callRealMethod();})
                .when(f.periodRepository).entries(1,from,to);
            assertEquals(1,service.read(f.owner.getEmail(),from,to).entries().size());
            assertSame(outer,TransactionSynchronizationManager.getResource(source));return null;
        });
        verify(f.repository,times(1)).coverage(1);verify(f.periodRepository,times(1)).entries(1,from,to);
        assertEquals(before,f.db.queryForList("SELECT * FROM users ORDER BY id"));
        assertEquals(1,f.db.queryForObject("SELECT COUNT(*) FROM settlement_journal",Integer.class));
    }
}
