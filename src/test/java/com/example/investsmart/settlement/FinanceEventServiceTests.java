package com.example.investsmart.settlement;

import com.example.investsmart.exception.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FinanceEventServiceTests {
    JournalReportFixture f;
    FinanceEventService service;
    @BeforeEach void setup() {
        f = new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:finance-reader-" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", ""), true);
        service = f.context.getBean(FinanceEventService.class);
        position("owned", 1); position("foreign", 2);
    }
    @AfterEach void close() { f.close(); }
    void position(String id, long user) {
        f.db.update("""
                INSERT INTO paper_financing(id,user_id,stock_id,product,quantity,cost_amount,principal,
                interest,interest_rate,created_at,last_accrued_date) VALUES(?,?,1,'MTF',0,100,75,9,0.1,?,?)
                """, id, user, Timestamp.from(JournalReportFixture.AT), java.sql.Date.valueOf("2026-11-23"));
    }
    void event(String key, String position, long user, String kind, String principal, String amount) {
        f.db.update("INSERT INTO paper_finance_events VALUES(?,?,?,?,?,?,?)", key, position, user,
                kind, new BigDecimal(principal), new BigDecimal(amount), Timestamp.from(JournalReportFixture.AT));
    }
    @Test void separatesAccrualPaymentAndContributionWithoutAddingSnapshotInterest() {
        event("a", "owned", 1, "BORROW", "75.00", "25.00");
        event("b", "owned", 1, "INTEREST", "0.00", "2.50");
        event("c", "owned", 1, "REPAY", "-30.00", "1.00");
        var positions = f.db.queryForList("SELECT * FROM paper_financing");
        var events = f.db.queryForList("SELECT * FROM paper_finance_events");
        var result = service.read("OWNER@example.invalid", "owned", null, null);
        assertEquals(new BigDecimal("1.00"), result.entries().get(0).interpretation().interestPaid());
        assertEquals(new BigDecimal("2.50"), result.entries().get(1).interpretation().interestAccrued());
        assertEquals(new BigDecimal("25.00"), result.entries().get(2).interpretation().contribution());
        assertNull(result.entries().get(2).interpretation().interestPaid());
        assertEquals(positions, f.db.queryForList("SELECT * FROM paper_financing"));
        assertEquals(events, f.db.queryForList("SELECT * FROM paper_finance_events"));
        assertEquals(0, f.db.queryForObject("SELECT COUNT(*) FROM settlement_accounts", Integer.class));
        assertThrows(UnsupportedOperationException.class, () -> result.entries().clear());
    }
    @Test void deniesForeignMissingAndSuspendedIdentityAndFiltersCorruptUserLinks() {
        event("foreign-event", "owned", 2, "INTEREST", "0", "99");
        assertTrue(service.read(f.owner.getEmail(), "owned", 0, 50).entries().isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.read(f.owner.getEmail(), "foreign", 0, 50));
        assertThrows(ResourceNotFoundException.class, () -> service.read(f.owner.getEmail(), "missing", 0, 50));
        for (String identity : new String[]{null, " ", "unknown@example.invalid"})
            assertThrows(ResourceNotFoundException.class, () -> service.read(identity, "owned", 0, 50));
        f.owner.setAccountStatus("SUSPENDED");
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.read(f.owner.getEmail(), "owned", 0, 50));
    }
    @Test void keepsUnknownInvalidAndLargeAmountsExplicitWithStablePagination() {
        event("a", "owned", 1, "INTEREST", "0", "99999999999999999.99");
        event("b", "owned", 1, "REPAY", "1", "2");
        event("c", "owned", 1, "FUTURE", "0", "42");
        var result = service.read(f.owner.getEmail(), "owned", 0, 2);
        assertEquals(List.of("c", "b"), result.entries().stream().map(e -> e.eventKey()).toList());
        assertEquals(FinanceEventClassifier.Kind.UNSUPPORTED, result.entries().get(0).interpretation().kind());
        assertEquals(FinanceEventClassifier.Kind.INVALID, result.entries().get(1).interpretation().kind());
        assertNull(result.entries().get(1).interpretation().interestPaid());
        assertEquals(new BigDecimal("99999999999999999.99"), service.read(f.owner.getEmail(), "owned", 1, 2).entries().getFirst().amount());
        assertTrue(service.read(f.owner.getEmail(), "owned", 2, 2).entries().isEmpty());
    }
    @Test void rejectsBoundsBeforeIdentityLookup() {
        clearInvocations(f.users);
        for (int[] bounds : new int[][]{{-1,50},{100001,50},{0,0},{0,101}})
            assertThrows(BadRequestException.class, () -> service.read(f.owner.getEmail(), "owned", bounds[0], bounds[1]));
        assertThrows(BadRequestException.class, () -> service.read(f.owner.getEmail(), "x".repeat(37), 0, 50));
        verifyNoInteractions(f.users);
    }
    @Test void usesSeparateReadOnlyRepeatableReadSnapshot() {
        var checked = spy(new FinanceEventRepository(f.db, new FinanceEventClassifier()));
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            return call.callRealMethod();
        }).when(checked).ownedPosition(1, "owned");
        var factory = new org.springframework.aop.framework.ProxyFactory(new FinanceEventService(f.users, checked));
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
                f.context.getBean(org.springframework.transaction.PlatformTransactionManager.class),
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        var proxied = (FinanceEventService) factory.getProxy();
        f.write.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
        f.write.executeWithoutResult(status -> {
            proxied.read(f.owner.getEmail(), "owned", 0, 50);
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
        });
    }
}
