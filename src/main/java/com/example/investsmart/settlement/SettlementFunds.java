package com.example.investsmart.settlement;

import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Internal ledger. Financial routes must adopt it together before its balances become authoritative. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class SettlementFunds {
    public enum CreditPolicy { REUSABLE, RESTRICTED }
    public record Balances(BigDecimal settled, BigDecimal reusable, BigDecimal restricted,
                           BigDecimal reserved, BigDecimal purchasable, BigDecimal withdrawable) {}
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final JdbcTemplate db;
    private final SettlementOpening opening;
    private final SettlementCalendar calendar;
    private final Clock clock;

    public SettlementFunds(JdbcTemplate db, SettlementOpening opening, SettlementCalendar calendar, Clock clock) {
        this.db = db; this.opening = opening; this.calendar = calendar; this.clock = clock;
    }

    public Balances balances(long userId) {
        lock(userId);
        BigDecimal settled = settled(userId);
        BigDecimal reusable = pending(userId, CreditPolicy.REUSABLE);
        BigDecimal restricted = pending(userId, CreditPolicy.RESTRICTED);
        BigDecimal reserved = db.queryForObject("SELECT COALESCE(SUM(remaining_amount),0) FROM settlement_allocations WHERE user_id=?", BigDecimal.class, userId).setScale(2);
        return new Balances(settled, reusable, restricted, reserved,
                settled.signum() < 0 ? ZERO : settled.add(reusable), settled.max(ZERO));
    }

    public void creditSettled(long userId, String event, BigDecimal amount) {
        amount = money(amount); lock(userId);
        if (journal(userId, event, "SETTLED_CREDIT", amount, null, null)) addSettled(userId, amount);
    }

    public void withdraw(long userId, String event, BigDecimal amount) {
        amount = money(amount); lock(userId);
        if (!journal(userId, event, "WITHDRAW", amount, null, null)) return;
        if (settled(userId).compareTo(amount) < 0) throw unavailable("Insufficient settled withdrawable cash.");
        addSettled(userId, amount.negate());
    }

    /** Internal loss/fee charge: spend eligible cash first and retain only the uncovered shortfall as debt. */
    public void chargeDebt(long userId, String event, BigDecimal amount) {
        amount = money(amount); lock(userId);
        if (!journal(userId, event, "REALIZED_CHARGE", amount, null, null)) return;
        BigDecimal cash = settled(userId);
        BigDecimal payable = cash.signum() < 0 ? ZERO : cash.add(pending(userId,CreditPolicy.REUSABLE)).min(amount);
        if (payable.signum() > 0) {
            String reference = UUID.nameUUIDFromBytes(event.getBytes(StandardCharsets.UTF_8)).toString();
            reserve(userId,"CHARGE_RESERVE:"+reference,"CHARGE",reference,payable,true);
            spend(userId,"CHARGE_SPEND:"+reference,"CHARGE",reference,payable);
        }
        BigDecimal debt = amount.subtract(payable);
        if(debt.signum()>0)addSettled(userId,debt.negate());
    }

    /** No credit is made to settled cash until the obligation clears. Missing dates stay pending. */
    public String creditPending(long userId, String event, String source, String exchange, String segment,
                                LocalDate tradeDate, BigDecimal amount, CreditPolicy policy) {
        amount = money(amount); text(source, 160); Objects.requireNonNull(tradeDate); Objects.requireNonNull(policy);
        if (exchange == null || segment == null || !Set.of("NSE", "BSE", "MCX").contains(exchange) || !Set.of("EQUITY", "FNO", "CURRENCY", "COMMODITY", "MUTUAL_FUND", "BOND").contains(segment)) {
            throw new IllegalArgumentException("Unsupported settlement exchange or segment.");
        }
        lock(userId);
        if (!journal(userId, event, "PENDING_CREDIT", amount, null, null, source, exchange, segment, tradeDate.toString(), policy.name())) {
            return db.queryForObject("SELECT id FROM settlement_obligations WHERE user_id=? AND source_key=?", String.class, userId, source);
        }
        var next = calendar.next(exchange, segment, tradeDate);
        if ("TRADING_CLOSED".equals(next.status())) throw unavailable("Reviewed calendar marks this trading date closed.");
        String id = UUID.randomUUID().toString();
        db.update("INSERT INTO settlement_obligations(id,user_id,source_key,exchange_code,segment_code,trade_date,obligation_kind,credit_policy,amount,remaining_credit,status,calendar_version,expected_date,cutoff_at,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id, userId, source, exchange, segment, tradeDate, "CASH_CREDIT", policy.name(), amount, amount, "PENDING", next.versionId(), next.expectedDate(), timestamp(next.simulationCutoff()), now());
        return id;
    }

    /** Settled cash first, then reusable pending credits in deterministic creation/id order. */
    public void reserve(long userId, String event, String kind, String reference, BigDecimal amount, boolean reusableAllowed) {
        amount = money(amount); reference(kind, reference); lock(userId);
        if (!journal(userId, event, "RESERVE", amount, kind, reference, Boolean.toString(reusableAllowed))) return;
        BigDecimal free = settled(userId);
        if (free.signum() < 0) throw unavailable("Outstanding cash debt blocks new reservations.");
        BigDecimal available = reusableAllowed ? free.add(pending(userId, CreditPolicy.REUSABLE)) : free;
        if (available.compareTo(amount) < 0) throw unavailable("Insufficient eligible cash; restricted and reserved credits cannot fund this order.");
        BigDecimal settledPart = free.min(amount);
        if (settledPart.signum() > 0) {
            addSettled(userId, settledPart.negate());
            allocate(userId, event, kind, reference, "SETTLED", settledPart);
        }
        BigDecimal left = amount.subtract(settledPart);
        if (left.signum() == 0) return;
        for (var row : db.queryForList("SELECT id,remaining_credit FROM settlement_obligations WHERE user_id=? AND status='PENDING' AND credit_policy='REUSABLE' AND remaining_credit>0 ORDER BY created_at,id", userId)) {
            BigDecimal take = left.min((BigDecimal)row.get("remaining_credit"));
            String source = (String)row.get("id");
            db.update("UPDATE settlement_obligations SET remaining_credit=remaining_credit-? WHERE id=? AND user_id=?", take, source, userId);
            allocate(userId, event, kind, reference, source, take);
            left = left.subtract(take);
            if (left.signum() == 0) break;
        }
        if (left.signum() != 0) throw new IllegalStateException("Funding allocation did not reconcile.");
    }

    /** A stable event key is required for each partial fill or refund, including retries. */
    public void spend(long userId, String event, String kind, String reference, BigDecimal amount) {
        resolve(userId, event, kind, reference, amount, false);
    }

    public void release(long userId, String event, String kind, String reference, BigDecimal amount) {
        resolve(userId, event, kind, reference, amount, true);
    }

    /** Link BTST cash to its actual unsettled delivery obligations in the same sale transaction. */
    public void requireDeliveries(long userId, String event, String creditObligation, Collection<String> deliveries) {
        text(creditObligation, 36);
        if (deliveries == null || deliveries.isEmpty() || deliveries.size() > 1000) throw unavailable("A bounded set of delivery obligations is required.");
        var ids = new TreeSet<String>();
        for (String id : deliveries) { text(id, 36); ids.add(id); }
        lock(userId);
        if (!journal(userId, event, "LINK_DELIVERIES", ZERO, "OBLIGATION", creditObligation, ids.toArray(String[]::new))) return;
        var credits = db.queryForList("SELECT status,obligation_kind,credit_policy FROM settlement_obligations WHERE user_id=? AND id=?", userId, creditObligation);
        if (credits.isEmpty() || !"PENDING".equals(credits.getFirst().get("status")) || !"CASH_CREDIT".equals(credits.getFirst().get("obligation_kind")) || !"RESTRICTED".equals(credits.getFirst().get("credit_policy"))) {
            throw unavailable("Only pending restricted cash may depend on share deliveries.");
        }
        for (String id : ids) {
            var rows = db.queryForList("SELECT obligation_kind,status FROM settlement_obligations WHERE user_id=? AND id=?", userId, id);
            if (rows.isEmpty() || !"EQUITY_DELIVERY".equals(rows.getFirst().get("obligation_kind")) || !Set.of("PENDING","SETTLED").contains(rows.getFirst().get("status"))) {
                throw unavailable("An owned share-delivery obligation is required.");
            }
            // Delivery obligations cannot themselves depend on cash, preventing dependency cycles.
            if (db.queryForObject("SELECT COUNT(*) FROM settlement_dependencies WHERE user_id=? AND credit_obligation_id=? AND delivery_obligation_id=?", Integer.class, userId, creditObligation, id) == 0) {
                db.update("INSERT INTO settlement_dependencies(user_id,credit_obligation_id,delivery_obligation_id) VALUES(?,?,?)", userId, creditObligation, id);
            }
        }
    }

    /** Rechecks the active reviewed calendar; never settles from a stale stored deadline alone. */
    public boolean settle(long userId, String obligationId) {
        text(obligationId, 36); lock(userId);
        var rows = db.queryForList("SELECT * FROM settlement_obligations WHERE id=? AND user_id=?", obligationId, userId);
        if (rows.isEmpty()) throw unavailable("Settlement obligation unavailable for this account.");
        var row = rows.getFirst();
        if ("SETTLED".equals(row.get("status"))) return false;
        if (!"PENDING".equals(row.get("status"))) throw unavailable("Settlement obligation is not pending.");
        boolean delivered = true;
        var dependencies = db.queryForList("SELECT d.delivery_obligation_id,o.status,o.obligation_kind FROM settlement_dependencies d LEFT JOIN settlement_obligations o ON o.id=d.delivery_obligation_id AND o.user_id=d.user_id WHERE d.user_id=? AND d.credit_obligation_id=? ORDER BY d.delivery_obligation_id", userId, obligationId);
        if (!dependencies.isEmpty() && !"CASH_CREDIT".equals(row.get("obligation_kind"))) throw new IllegalStateException("Only cash credits may have delivery dependencies.");
        for (var dependency : dependencies) {
            if (!"EQUITY_DELIVERY".equals(dependency.get("obligation_kind"))) throw new IllegalStateException("Delivery dependency is missing or invalid.");
            if (!"SETTLED".equals(dependency.get("status")) && !settle(userId, (String)dependency.get("delivery_obligation_id"))) delivered = false;
        }
        var next = calendar.next((String)row.get("exchange_code"), (String)row.get("segment_code"), ((java.sql.Date)row.get("trade_date")).toLocalDate());
        db.update("UPDATE settlement_obligations SET calendar_version=?,expected_date=?,cutoff_at=? WHERE id=? AND user_id=?", next.versionId(), next.expectedDate(), timestamp(next.simulationCutoff()), obligationId, userId);
        if (!delivered || !"READY".equals(next.status()) || clock.instant().isBefore(next.simulationCutoff())) return false;
        BigDecimal remaining = (BigDecimal)row.get("remaining_credit");
        if (remaining.signum() < 0) throw new IllegalStateException("Settlement credit cannot be negative.");
        if (!journal(userId, "SETTLE:" + obligationId, "SETTLE", remaining, "OBLIGATION", obligationId)) {
            throw new IllegalStateException("Settlement journal and obligation state disagree.");
        }
        addSettled(userId, remaining);
        db.update("UPDATE settlement_obligations SET remaining_credit=0,status='SETTLED',settled_at=? WHERE id=? AND user_id=?", now(), obligationId, userId);
        db.update("UPDATE settlement_lots SET settled=TRUE WHERE user_id=? AND obligation_id=?", userId, obligationId);
        return true;
    }

    private void resolve(long userId, String event, String kind, String reference, BigDecimal amount, boolean release) {
        amount = money(amount); reference(kind, reference); lock(userId);
        String action = release ? "RELEASE" : "SPEND";
        if (!journal(userId, event, action, amount, kind, reference)) return;
        var rows = db.queryForList("SELECT * FROM settlement_allocations WHERE user_id=? AND reference_kind=? AND reference_id=? AND remaining_amount>0 ORDER BY CASE WHEN source_key IN ('SETTLED','OPENING_SETTLED') THEN 0 ELSE 1 END,created_at,id", userId, kind, reference);
        BigDecimal reserved = rows.stream().map(r -> (BigDecimal)r.get("remaining_amount")).reduce(ZERO, BigDecimal::add);
        if (reserved.compareTo(amount) < 0) throw unavailable("Insufficient reserved funding for this account and reference.");
        BigDecimal left = amount;
        for (var row : rows) {
            String id = (String)row.get("id");
            BigDecimal take = left.min((BigDecimal)row.get("remaining_amount"));
            if (release) returnSource(userId, (String)row.get("source_key"), take);
            BigDecimal after = ((BigDecimal)row.get("remaining_amount")).subtract(take);
            db.update("UPDATE settlement_allocations SET remaining_amount=?,status=? WHERE id=? AND user_id=?", after, after.signum() == 0 ? "CLOSED" : "RESERVED", id, userId);
            db.update("INSERT INTO settlement_allocation_events(user_id,event_key,allocation_id,action,amount) VALUES(?,?,?,?,?)", userId, event, id, action, take);
            left = left.subtract(take);
            if (left.signum() == 0) break;
        }
    }

    private void returnSource(long userId, String source, BigDecimal amount) {
        if (Set.of("SETTLED", "OPENING_SETTLED").contains(source)) { addSettled(userId, amount); return; }
        var rows = db.queryForList("SELECT status FROM settlement_obligations WHERE user_id=? AND id=?", userId, source);
        if (rows.isEmpty()) throw new IllegalStateException("Reservation source is missing.");
        if ("SETTLED".equals(rows.getFirst().get("status"))) addSettled(userId, amount);
        else if ("PENDING".equals(rows.getFirst().get("status"))) {
            db.update("UPDATE settlement_obligations SET remaining_credit=remaining_credit+? WHERE id=? AND user_id=?", amount, source, userId);
        } else throw new IllegalStateException("Reservation source is not releasable.");
    }

    private void allocate(long userId, String event, String kind, String reference, String source, BigDecimal amount) {
        var ids = db.queryForList("SELECT id FROM settlement_allocations WHERE user_id=? AND reference_kind=? AND reference_id=? AND source_key=?", String.class, userId, kind, reference, source);
        String id;
        if (ids.isEmpty()) {
            id = UUID.randomUUID().toString();
            db.update("INSERT INTO settlement_allocations(id,user_id,reference_kind,reference_id,source_key,amount,remaining_amount,status,created_at) VALUES(?,?,?,?,?,?,?,?,?)", id, userId, kind, reference, source, amount, amount, "RESERVED", now());
        } else {
            id = ids.getFirst();
            db.update("UPDATE settlement_allocations SET amount=amount+?,remaining_amount=remaining_amount+?,status='RESERVED' WHERE id=? AND user_id=?", amount, amount, id, userId);
        }
        db.update("INSERT INTO settlement_allocation_events(user_id,event_key,allocation_id,action,amount) VALUES(?,?,?,?,?)", userId, event, id, "RESERVE", amount);
    }

    boolean journal(long userId, String event, String action, BigDecimal amount, String kind, String reference, String... extra) {
        text(event, 160);
        var parts = new ArrayList<>(Arrays.asList(action, amount.toPlainString(), kind, reference));
        parts.addAll(Arrays.asList(extra));
        StringBuilder canonical = new StringBuilder();
        for (String part : parts) canonical.append(part == null ? "-1:" : part.length() + ":" + part);
        String fingerprint;
        try { fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        var existing = db.queryForList("SELECT fingerprint FROM settlement_journal WHERE user_id=? AND event_key=?", String.class, userId, event);
        if (!existing.isEmpty()) {
            if (!existing.getFirst().equals(fingerprint)) throw unavailable("This accounting event key has already been used with different instructions.");
            return false;
        }
        db.update("INSERT INTO settlement_journal(user_id,event_key,action,fingerprint,amount,reference_kind,reference_id,occurred_at) VALUES(?,?,?,?,?,?,?,?)", userId, event, action, fingerprint, amount, kind, reference, now());
        return true;
    }

    void lock(long userId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Settlement funding requires a financial transaction.");
        opening.capture(userId);
    }
    private BigDecimal settled(long userId) { return db.queryForObject("SELECT settled_cash FROM settlement_accounts WHERE user_id=?", BigDecimal.class, userId); }
    private BigDecimal pending(long userId, CreditPolicy policy) { return db.queryForObject("SELECT COALESCE(SUM(remaining_credit),0) FROM settlement_obligations WHERE user_id=? AND status='PENDING' AND credit_policy=?", BigDecimal.class, userId, policy.name()).setScale(2); }
    private void addSettled(long userId, BigDecimal amount) { db.update("UPDATE settlement_accounts SET settled_cash=settled_cash+? WHERE user_id=?", amount, userId); }
    private Timestamp now() { return Timestamp.from(clock.instant()); }
    private static Timestamp timestamp(Instant instant) { return instant == null ? null : Timestamp.from(instant); }
    private static IllegalArgumentException unavailable(String reason) { return new IllegalArgumentException(reason); }
    private static void text(String value, int max) { if (value == null || value.isBlank() || value.length() > max || value.chars().anyMatch(Character::isISOControl)) throw unavailable("A bounded nonblank accounting reference is required."); }
    private static void reference(String kind, String reference) { text(kind, 24); text(reference, 100); if (!kind.matches("[A-Z_]+")) throw unavailable("Invalid accounting reference kind."); }
    private static BigDecimal money(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw unavailable("A positive exact monetary amount is required.");
        try { amount = amount.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException invalid) { throw unavailable("Monetary amounts cannot be rounded during accounting."); }
        if (amount.precision() > 19) throw unavailable("Monetary amount exceeds the supported range.");
        return amount;
    }
}
