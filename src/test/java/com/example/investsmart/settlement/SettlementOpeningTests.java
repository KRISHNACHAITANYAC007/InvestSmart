package com.example.investsmart.settlement;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

class SettlementOpeningTests {
    DriverManagerDataSource source;
    JdbcTemplate db;
    TransactionTemplate tx;
    SettlementOpening opening;

    @BeforeEach void setup() {
        source = new DriverManagerDataSource("jdbc:h2:mem:opening-" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        db = new JdbcTemplate(source);
        db.execute("CREATE TABLE users(id BIGINT PRIMARY KEY,virtual_balance DECIMAL(19,2))");
        db.execute("CREATE TABLE holdings(id BIGINT PRIMARY KEY,user_id BIGINT,stock_id BIGINT,quantity INT)");
        for (String table : new String[]{"equity_pending_orders", "option_orders", "paper_tickets"}) {
            db.execute("CREATE TABLE " + table + "(id VARCHAR(36) PRIMARY KEY,user_id BIGINT,reserved_cash DECIMAL(19,2),status VARCHAR(24))");
        }
        db.execute("CREATE TABLE ipo_applications(id BIGINT PRIMARY KEY,user_id BIGINT,reserved_amount DECIMAL(19,2),status VARCHAR(24))");
        new ResourceDatabasePopulator(new ClassPathResource("db/migrations/20261111_settlement_accounting.sql")).execute(source);
        db.update("INSERT INTO users VALUES(1,1234.56),(2,200)");
        db.update("INSERT INTO holdings VALUES(10,1,5,13),(11,1,6,0),(12,2,5,2)");
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        opening = new SettlementOpening(db, Clock.fixed(Instant.parse("2026-11-11T05:00:00Z"), ZoneOffset.UTC));
    }

    boolean capture(long id) { return Boolean.TRUE.equals(tx.execute(status -> opening.capture(id))); }
    int count(String table) { return db.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }

    @Test void openingPreservesCashQuantitiesAndHistoryWithoutNewSettlementDelay() {
        var originalHoldings = db.queryForList("SELECT * FROM holdings ORDER BY id");
        assertTrue(capture(1));
        assertEquals(new BigDecimal("1234.56"), db.queryForObject("SELECT virtual_balance FROM users WHERE id=1", BigDecimal.class));
        assertEquals(new BigDecimal("1234.56"), db.queryForObject("SELECT settled_cash FROM settlement_accounts WHERE user_id=1", BigDecimal.class));
        assertEquals(originalHoldings, db.queryForList("SELECT * FROM holdings ORDER BY id"));
        var lot = db.queryForMap("SELECT * FROM settlement_lots");
        assertEquals(13, ((Number)lot.get("remaining_units")).intValue());
        assertEquals(true, lot.get("settled"));
        assertNull(lot.get("cost_amount"));
        assertNull(lot.get("obligation_id"));
        assertEquals(0, count("settlement_obligations"));
    }

    @Test void existingReservationsAreCapturedSeparatelyAndNeverAddedToFreeCash() {
        db.update("INSERT INTO equity_pending_orders VALUES('same-id',1,100,'PENDING'),('terminal',1,999,'FILLED')");
        db.update("INSERT INTO option_orders VALUES('same-id',1,200,'PENDING'),('zero',1,0,'PENDING')");
        db.update("INSERT INTO paper_tickets VALUES('a',1,30,'PENDING'),('b',1,40,'TRIGGERED'),('c',1,50,'PART_FILLED'),('d',1,999,'CANCELLED')");
        db.update("INSERT INTO ipo_applications VALUES(1,1,60,'PENDING'),(2,1,70,'AWAITING_DATA'),(3,1,999,'ALLOTTED')");
        var equity = db.queryForList("SELECT * FROM equity_pending_orders ORDER BY id");
        capture(1);
        assertEquals(7, count("settlement_allocations"));
        assertEquals(new BigDecimal("550.00"), db.queryForObject("SELECT SUM(remaining_amount) FROM settlement_allocations", BigDecimal.class));
        assertEquals(new BigDecimal("1234.56"), db.queryForObject("SELECT settled_cash FROM settlement_accounts", BigDecimal.class));
        assertEquals(equity, db.queryForList("SELECT * FROM equity_pending_orders ORDER BY id"));
        assertEquals(7, db.queryForObject("SELECT COUNT(*) FROM settlement_allocations WHERE status='RESERVED' AND source_key='OPENING_SETTLED'", Integer.class));
    }

    @Test void accountOwnershipIsRetainedForLotsAndReservations() {
        db.update("INSERT INTO option_orders VALUES('other',2,100,'PENDING')");
        capture(1);
        assertEquals(1, count("settlement_lots"));
        assertEquals(0, count("settlement_allocations"));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM settlement_accounts WHERE user_id=2", Integer.class));
    }

    @Test void replayAndRestartDoNotRebaseAnExistingOpening() {
        capture(1);
        var lots = db.queryForList("SELECT * FROM settlement_lots");
        db.update("UPDATE users SET virtual_balance=999 WHERE id=1");
        db.update("UPDATE holdings SET quantity=20 WHERE id=10");
        opening = new SettlementOpening(db, Clock.systemUTC());
        assertFalse(capture(1));
        assertEquals(lots, db.queryForList("SELECT * FROM settlement_lots"));
        assertEquals(new BigDecimal("1234.56"), db.queryForObject("SELECT opening_cash FROM settlement_accounts", BigDecimal.class));
    }

    @Test void concurrentInitializersUseTheSharedUserRowLock() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        try {
            var start = new CountDownLatch(1);
            Callable<Boolean> task = () -> { start.await(); return capture(1); };
            var first = pool.submit(task); var second = pool.submit(task); start.countDown();
            assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(1, count("settlement_accounts"));
            assertEquals(1, count("settlement_lots"));
        } finally { pool.shutdownNow(); }
    }

    @Test void failedReservationCaptureRollsBackAllOpeningRows() {
        db.update("INSERT INTO paper_tickets VALUES('invalid',1,-1,'PENDING')");
        assertThrows(IllegalStateException.class, () -> capture(1));
        assertEquals(0, count("settlement_accounts"));
        assertEquals(0, count("settlement_lots"));
        db.update("UPDATE paper_tickets SET reserved_cash=10 WHERE id='invalid'");
        assertTrue(capture(1));
    }

    @Test void failedHoldingValidationDoesNotChangeExistingDataOrLeaveMarker() {
        db.update("UPDATE holdings SET quantity=-1 WHERE id=10");
        assertThrows(IllegalStateException.class, () -> capture(1));
        assertEquals(0, count("settlement_accounts"));
        assertEquals(-1, db.queryForObject("SELECT quantity FROM holdings WHERE id=10", Integer.class));
    }

    @Test void negativeLegacyCashIsPreservedRatherThanForgiven() {
        db.update("UPDATE users SET virtual_balance=-42.25 WHERE id=1");
        capture(1);
        assertEquals(new BigDecimal("-42.25"), db.queryForObject("SELECT settled_cash FROM settlement_accounts", BigDecimal.class));
    }

    @Test void nullCashFailsClosedAndTransactionIsMandatory() {
        assertThrows(IllegalStateException.class, () -> opening.capture(1));
        db.update("UPDATE users SET virtual_balance=NULL WHERE id=1");
        assertThrows(IllegalStateException.class, () -> capture(1));
        assertEquals(0, count("settlement_accounts"));
    }

    @Test void outerFailureRollsBackOpeningForSafeRetry() {
        assertThrows(IllegalStateException.class, () -> tx.execute(status -> {
            opening.capture(1);
            throw new IllegalStateException("synthetic enclosing order failure");
        }));
        assertEquals(0, count("settlement_accounts"));
        assertTrue(capture(1));
    }

    @Test void forwardSchemaCanBeReplayedWithoutRewritingOpeningOrLegacyBalances() {
        capture(1);
        var accounts = db.queryForList("SELECT * FROM settlement_accounts");
        new ResourceDatabasePopulator(new ClassPathResource("db/migrations/20261111_settlement_accounting.sql")).execute(source);
        assertEquals(accounts, db.queryForList("SELECT * FROM settlement_accounts"));
        assertEquals(new BigDecimal("1234.56"), db.queryForObject("SELECT virtual_balance FROM users WHERE id=1", BigDecimal.class));
    }
}
