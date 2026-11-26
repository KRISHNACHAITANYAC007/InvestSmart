package com.example.investsmart.settlement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.entity.User;
import com.example.investsmart.security.AdminHealthStore;

class SettlementProcessingTests {
    SettlementFundsTests fixture;
    SettlementProcessing processing;
    SettlementStatus status;
    UserRepository users;
    User user;
    Clock clock=Clock.fixed(SettlementFundsTests.CUTOFF,ZoneOffset.UTC);
    @BeforeEach void setup(){
        fixture=new SettlementFundsTests();fixture.setup();fixture.time(clock.instant());
        new ResourceDatabasePopulator(new ClassPathResource("db/migrations/20261111_settlement_checks.sql")).execute(fixture.fixture.source);
        users=mock(UserRepository.class);user=new User();user.setId(1L);user.setVirtualBalance(new BigDecimal("0.00"));
        lenient().when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));lenient().when(users.findById(1L)).thenReturn(Optional.of(user));
        var accounting=new SettlementAccounting(fixture.funds,new SettlementLots(fixture.db,fixture.funds,fixture.calendar,clock),users,clock);
        processing=new SettlementProcessing(fixture.db,users,fixture.funds,accounting,clock);status=new SettlementStatus(fixture.db,users);
    }
    @Test void readOnlyStatusDoesNotInitializeOrChangeAccounts(){
        var summary=status.summary(1);assertFalse(summary.initialized());assertEquals(new BigDecimal("0.00"),summary.withdrawable());
        assertEquals(13,status.holdings(1).getFirst().settledUnits());assertTrue(status.history(1,1).isEmpty());
        assertEquals(0,fixture.db.queryForObject("SELECT COUNT(*) FROM settlement_accounts",Integer.class));verify(users,never()).saveAndFlush(any());
    }
    @Test void catchUpSettlesOnceAndRefreshesCompatibilityProjection(){
        String id=fixture.pending("btst","100",SettlementFunds.CreditPolicy.RESTRICTED);
        processing.touch(id);assertTrue(fixture.get(()->processing.process(id)));assertFalse(fixture.get(()->processing.process(id)));
        assertEquals(new BigDecimal("100.00"),user.getVirtualBalance());assertEquals(new BigDecimal("100.00"),status.summary(1).withdrawable());
        assertEquals("SETTLED",status.history(1,1).getFirst().status());assertTrue(processing.candidates().isEmpty());
    }
    @Test void missingCalendarLeavesCashRestrictedAndObservationSurvivesFailure(){
        String id=fixture.pending("btst","100",SettlementFunds.CreditPolicy.RESTRICTED);fixture.db.update("DELETE FROM settlement_calendar_active");
        processing.touch(id);assertFalse(fixture.get(()->processing.process(id)));
        assertEquals(new BigDecimal("100.00"),status.summary(1).restricted());assertNull(status.history(1,1).getFirst().cutoffAt());
        fixture.db.update("DELETE FROM settlement_accounts WHERE user_id=1");
        assertThrows(IllegalStateException.class,()->fixture.get(()->processing.process(id)));
        assertEquals(1,fixture.db.queryForObject("SELECT COUNT(*) FROM settlement_checks",Integer.class));
    }
    @Test void boundedCandidateSelectionRotatesPastPreviouslyCheckedUnknownCoverage(){
        fixture.db.update("DELETE FROM settlement_calendar_active");
        for(int i=0;i<25;i++)fixture.pending("sale-"+i,"1",SettlementFunds.CreditPolicy.REUSABLE);
        var batch=processing.candidates();assertEquals(20,batch.size());
        for(String id:batch)processing.touch(id);
        var next=processing.candidates();assertTrue(next.stream().limit(5).noneMatch(batch::contains));
    }
    @Test void backgroundWorkerWillNotCreateMissingOpeningAccount(){
        String id=fixture.pending("sale","100",SettlementFunds.CreditPolicy.REUSABLE);fixture.db.update("DELETE FROM settlement_accounts");
        assertThrows(IllegalStateException.class,()->fixture.get(()->processing.process(id)));
        assertEquals(0,fixture.db.queryForObject("SELECT COUNT(*) FROM settlement_accounts",Integer.class));
    }
    @Test void jobContinuesOtherCandidatesAndPersistsSanitizedFailureHealthAcrossRestart(){
        fixture.db.execute("CREATE TABLE admin_runtime_health(service_name VARCHAR(40) PRIMARY KEY,last_started_at TIMESTAMP,last_completed_at TIMESTAMP,last_success_at TIMESTAMP,last_failure_at TIMESTAMP,last_failure VARCHAR(200),failure_count BIGINT DEFAULT 0)");
        var worker=mock(SettlementProcessing.class);when(worker.candidates()).thenReturn(List.of("failed","good"));when(worker.process("failed")).thenThrow(new IllegalStateException("private diagnostics"));
        var health=new AdminHealthStore(fixture.db);new SettlementJobs(worker,health,clock,true).run();
        verify(worker).process("good");assertEquals(1L,((Number)new AdminHealthStore(fixture.db).status("t1-settlement").get("failureCount")).longValue());
        assertFalse(health.status("t1-settlement").get("lastFailure").toString().contains("private diagnostics"));assertNotNull(health.status("t1-settlement").get("lastCompletedAt"));
    }
    @Test void disabledJobDoesNotAccessWorkerOrHealth(){
        var worker=mock(SettlementProcessing.class);var health=mock(AdminHealthStore.class);new SettlementJobs(worker,health,clock,false).run();verifyNoInteractions(worker,health);
    }
    @Test void telemetryFailureDoesNotPreventGuardedProcessing(){
        var worker=mock(SettlementProcessing.class);var health=mock(AdminHealthStore.class);
        when(worker.candidates()).thenReturn(List.of("due"));
        doThrow(new IllegalStateException("private storage diagnostics")).when(health).started(anyString(),any());
        doThrow(new IllegalStateException()).when(health).completed(anyString(),any());
        assertDoesNotThrow(()->new SettlementJobs(worker,health,clock,true).run());
        verify(worker).touch("due");verify(worker).process("due");verify(health).succeeded("t1-settlement",clock.instant());
    }
}
