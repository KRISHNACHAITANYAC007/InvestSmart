package com.example.investsmart.settlement;

import java.math.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Tracks settlement units. Historical weighted-average accounting continues to determine P&L. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class SettlementLots {
    public record Disposal(int settledUnits, int unsettledUnits) {}
    public record Units(long settled, long unsettled) {}
    private final JdbcTemplate db;
    private final SettlementFunds funds;
    private final SettlementCalendar calendar;
    private final Clock clock;

    public SettlementLots(JdbcTemplate db, SettlementFunds funds, SettlementCalendar calendar, Clock clock) {
        this.db = db; this.funds = funds; this.calendar = calendar; this.clock = clock;
    }

    /** Invoke before changing aggregate holdings, so capture cannot treat the new units as opening stock. */
    public String purchase(long userId, String event, String source, long stockId, int units, BigDecimal cost,
                           String exchange, LocalDate tradeDate) {
        return acquire(userId, event, source, stockId, units, cost, exchange, tradeDate, false);
    }

    /** Provider-confirmed listing/allocation transfer; no cash is debited by this method. */
    public String deliverSettled(long userId, String event, String source, long stockId, int units, BigDecimal cost) {
        return acquire(userId, event, source, stockId, units, cost, null, null, true);
    }

    public Units units(long userId, long stockId) {
        funds.lock(userId);
        long settled = 0, unsettled = 0;
        for (var row : db.queryForList("SELECT settled,remaining_units FROM settlement_lots WHERE user_id=? AND stock_id=?", userId, stockId)) {
            if ((Boolean)row.get("settled")) settled += ((Number)row.get("remaining_units")).longValue();
            else unsettled += ((Number)row.get("remaining_units")).longValue();
        }
        return new Units(settled, unsettled);
    }

    /** Frozen disposition events retain the original BTST dependencies after a delivery clears. */
    public List<String> unsettledDeliveries(long userId, String disposalEvent) {
        funds.lock(userId);
        return db.queryForList("SELECT DISTINCT l.obligation_id FROM settlement_lot_events e JOIN settlement_lots l ON l.id=e.lot_id AND l.user_id=e.user_id WHERE e.user_id=? AND e.event_key=? AND e.action='DISPOSE' AND e.settled_at_event=FALSE AND l.obligation_id IS NOT NULL ORDER BY l.obligation_id", String.class, userId, disposalEvent);
    }

    /** Caller must enforce shared pending-sell reservations before invoking. Settled lots are sold first. */
    public Disposal dispose(long userId, String event, long stockId, int units) {
        validUnits(stockId, units); funds.lock(userId);
        if (!funds.journal(userId, event, "DISPOSE", BigDecimal.valueOf(units).setScale(2), "STOCK", Long.toString(stockId))) {
            int settled = 0, unsettled = 0;
            for (var row : db.queryForList("SELECT units,settled_at_event FROM settlement_lot_events WHERE user_id=? AND event_key=? AND action='DISPOSE'", userId, event)) {
                if ((Boolean)row.get("settled_at_event")) settled += ((Number)row.get("units")).intValue();
                else unsettled += ((Number)row.get("units")).intValue();
            }
            if ((long)settled + unsettled != units) throw new IllegalStateException("Disposal journal does not reconcile.");
            return new Disposal(settled, unsettled);
        }
        var rows = db.queryForList("SELECT id,remaining_units,settled FROM settlement_lots WHERE user_id=? AND stock_id=? AND remaining_units>0 ORDER BY settled DESC,created_at,id", userId, stockId);
        long available = rows.stream().mapToLong(row -> ((Number)row.get("remaining_units")).longValue()).sum();
        if (available < units) throw new IllegalArgumentException("Insufficient settlement lots for this account and listing.");
        int left = units, settled = 0, unsettled = 0;
        for (var row : rows) {
            int take = Math.min(left, ((Number)row.get("remaining_units")).intValue());
            boolean cleared = (Boolean)row.get("settled");
            db.update("UPDATE settlement_lots SET remaining_units=remaining_units-? WHERE id=? AND user_id=?", take, row.get("id"), userId);
            db.update("INSERT INTO settlement_lot_events(user_id,event_key,lot_id,action,units,settled_at_event) VALUES(?,?,?,?,?,?)", userId, event, row.get("id"), "DISPOSE", take, cleared);
            if (cleared) settled += take; else unsettled += take;
            left -= take;
            if (left == 0) break;
        }
        return new Disposal(settled, unsettled);
    }

    private String acquire(long userId, String event, String source, long stockId, int units, BigDecimal cost,
                           String exchange, LocalDate tradeDate, boolean cleared) {
        validUnits(stockId, units);
        if (source == null || source.isBlank() || source.length() > 160 || source.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("A bounded lot source is required.");
        if (cost == null || cost.signum() <= 0) throw new IllegalArgumentException("A positive acquisition cost is required.");
        try { cost = cost.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException invalid) { throw new IllegalArgumentException("Acquisition cost cannot be rounded during accounting."); }
        if (cost.precision() > 19) throw new IllegalArgumentException("Acquisition cost exceeds the supported range.");
        if (!cleared && (exchange == null || !Set.of("NSE","BSE").contains(exchange) || tradeDate == null)) throw new IllegalArgumentException("Verified equity listing exchange and trade date are required.");
        funds.lock(userId);
        if (!funds.journal(userId, event, cleared ? "DELIVER_SETTLED" : "PURCHASE_LOT", cost, "STOCK", Long.toString(stockId), source, Integer.toString(units), exchange, tradeDate == null ? null : tradeDate.toString())) {
            return db.queryForObject("SELECT id FROM settlement_lots WHERE user_id=? AND source_key=?", String.class, userId, source);
        }
        String obligation = null;
        if (!cleared) {
            var next = calendar.next(exchange, "EQUITY", tradeDate);
            if ("TRADING_CLOSED".equals(next.status())) throw new IllegalArgumentException("Reviewed calendar marks this trading date closed.");
            obligation = UUID.randomUUID().toString();
            db.update("INSERT INTO settlement_obligations(id,user_id,source_key,exchange_code,segment_code,trade_date,obligation_kind,credit_policy,amount,remaining_credit,status,calendar_version,expected_date,cutoff_at,created_at) VALUES(?,?,?,?,?,?,?,?,0,0,?,?,?,?,?)", obligation, userId, "DELIVERY:" + UUID.randomUUID(), exchange, "EQUITY", tradeDate, "EQUITY_DELIVERY", "NONE", "PENDING", next.versionId(), next.expectedDate(), next.simulationCutoff() == null ? null : Timestamp.from(next.simulationCutoff()), Timestamp.from(clock.instant()));
        }
        String id = UUID.randomUUID().toString();
        db.update("INSERT INTO settlement_lots(id,user_id,stock_id,source_key,obligation_id,initial_units,remaining_units,settled,cost_amount,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)", id, userId, stockId, source, obligation, units, units, cleared, cost, Timestamp.from(clock.instant()));
        db.update("INSERT INTO settlement_lot_events(user_id,event_key,lot_id,action,units,settled_at_event) VALUES(?,?,?,?,?,?)", userId, event, id, "ACQUIRE", units, cleared);
        return id;
    }

    private static void validUnits(long stockId, int units) {
        if (stockId < 1 || units < 1) throw new IllegalArgumentException("A listing and positive whole units are required.");
    }
}
