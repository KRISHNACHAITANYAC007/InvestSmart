package com.example.investsmart.settlement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Called before an account's first integrated money write, never by an unguarded startup sweep. */
@Service
public class SettlementOpening {
    private final JdbcTemplate db;
    private final Clock clock;

    public SettlementOpening(JdbcTemplate db, Clock clock) {
        this.db = db;
        this.clock = clock;
    }

    /** Caller must flush any earlier JPA writes before this snapshot. Existing analytics retain cost basis. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean capture(long userId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Opening capture requires the shared financial transaction.");
        }
        // Same row and lock order used by legacy/new money services. Do not lock a parallel mutex.
        BigDecimal balance = db.queryForObject("SELECT virtual_balance FROM users WHERE id=? FOR UPDATE", BigDecimal.class, userId);
        if (db.queryForObject("SELECT COUNT(*) FROM settlement_accounts WHERE user_id=?", Integer.class, userId) != 0) return false;
        balance = exactMoney(balance);
        Timestamp now = Timestamp.from(clock.instant());
        db.update("INSERT INTO settlement_accounts(user_id,opening_cash,settled_cash,initialized_at) VALUES(?,?,?,?)", userId, balance, balance, now);

        for (var lot : db.queryForList("SELECT id,stock_id,quantity FROM holdings WHERE user_id=? ORDER BY id", userId)) {
            Object quantity = lot.get("quantity");
            if (!(quantity instanceof Number) || ((Number) quantity).longValue() < 0 || ((Number) quantity).longValue() > Integer.MAX_VALUE || lot.get("stock_id") == null) {
                throw new IllegalStateException("Existing holding cannot be safely captured; review its private accounting records.");
            }
            int units = ((Number) quantity).intValue();
            if (units == 0) continue;
            // Null cost is deliberate: do not invent prices or rewrite weighted-average historical analytics.
            db.update("INSERT INTO settlement_lots(id,user_id,stock_id,source_key,initial_units,remaining_units,settled,cost_amount,created_at) VALUES(?,?,?,?,?,?,TRUE,NULL,?)", UUID.randomUUID().toString(), userId, lot.get("stock_id"), "OPENING_HOLDING:" + lot.get("id"), units, units, now);
        }
        captureReservations(userId, "EQUITY_ORDER", "SELECT id,reserved_cash AS amount FROM equity_pending_orders WHERE user_id=? AND status='PENDING'", now);
        captureReservations(userId, "OPTION_ORDER", "SELECT id,reserved_cash AS amount FROM option_orders WHERE user_id=? AND status='PENDING'", now);
        captureReservations(userId, "PAPER_TICKET", "SELECT id,reserved_cash AS amount FROM paper_tickets WHERE user_id=? AND status IN ('PENDING','TRIGGERED','PART_FILLED')", now);
        captureReservations(userId, "IPO_APPLICATION", "SELECT id,reserved_amount AS amount FROM ipo_applications WHERE user_id=? AND status IN ('PENDING','AWAITING_DATA')", now);
        return true;
    }

    private void captureReservations(long userId, String kind, String sql, Timestamp now) {
        for (var row : db.queryForList(sql, userId)) {
            BigDecimal amount = exactMoney((BigDecimal) row.get("amount"));
            if (amount.signum() < 0) throw new IllegalStateException("Existing reservation cannot be safely captured; review its private accounting records.");
            if (amount.signum() == 0) continue;
            db.update("INSERT INTO settlement_allocations(id,user_id,reference_kind,reference_id,source_key,amount,remaining_amount,status,created_at) VALUES(?,?,?,?,?,?,?,?,?)", UUID.randomUUID().toString(), userId, kind, row.get("id").toString(), "OPENING_SETTLED", amount, amount, "RESERVED", now);
        }
    }

    private static BigDecimal exactMoney(BigDecimal amount) {
        if (amount == null) throw new IllegalStateException("Opening cash requires an existing exact balance.");
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException invalid) {
            throw new IllegalStateException("Opening amounts cannot be rounded during capture.");
        }
    }
}
