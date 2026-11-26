package com.example.investsmart.settlement;

import static org.junit.jupiter.api.Assertions.*;
import static com.example.investsmart.settlement.SettlementFunds.CreditPolicy.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

class SettlementFundsTests {
    SettlementOpeningTests fixture;
    JdbcTemplate db;
    TransactionTemplate tx;
    SettlementCalendar calendar;
    SettlementFunds funds;
    static final LocalDate TRADE = LocalDate.of(2026,11,11);
    static final Instant BEFORE = Instant.parse("2026-11-12T12:29:59Z");
    static final Instant CUTOFF = Instant.parse("2026-11-12T12:30:00Z");
    @BeforeEach void setup() {
        fixture = new SettlementOpeningTests(); fixture.setup(); db = fixture.db; tx = fixture.tx;
        db.update("UPDATE users SET virtual_balance=0 WHERE id=1");
        for (String migration : List.of("20261111_settlement_calendar.sql", "20261111_settlement_journal.sql")) {
            new ResourceDatabasePopulator(new ClassPathResource("db/migrations/" + migration)).execute(fixture.source);
        }
        calendar = new SettlementCalendar(db, Clock.fixed(BEFORE, ZoneOffset.UTC));
        var days = List.of(new ReviewedCalendar.Day(TRADE,true,true,"synthetic"), new ReviewedCalendar.Day(TRADE.plusDays(1),true,true,"synthetic"), new ReviewedCalendar.Day(TRADE.plusDays(2),true,true,"synthetic"));
        reviewed(days, "a"); time(BEFORE);
    }
    void reviewed(List<ReviewedCalendar.Day> days, String checksum) {
        tx.executeWithoutResult(status -> calendar.activate(new ReviewedCalendar("NSE","EQUITY","https://example.org/synthetic.csv",checksum.repeat(64),days),"TEST_OPERATOR","Synthetic reviewed dates only"));
    }
    void time(Instant now) { funds = new SettlementFunds(db, fixture.opening, calendar, Clock.fixed(now, ZoneOffset.UTC)); }
    void run(Runnable op) { tx.executeWithoutResult(status -> op.run()); }
    <T> T get(Supplier<T> op) { return tx.execute(status -> op.get()); }
    BigDecimal n(String value) { return new BigDecimal(value).setScale(2); }
    SettlementFunds.Balances balances() { return get(() -> funds.balances(1)); }
    String pending(String key, String amount, SettlementFunds.CreditPolicy policy) { return get(() -> funds.creditPending(1,key,key,"NSE","EQUITY",TRADE,n(amount),policy)); }
    void reserve(String event, String amount) { run(() -> funds.reserve(1,event,"ORDER","order-1",n(amount),true)); }
    void spend(String event, String amount) { run(() -> funds.spend(1,event,"ORDER","order-1",n(amount))); }
    void release(String event, String amount) { run(() -> funds.release(1,event,"ORDER","order-1",n(amount))); }
    void cash(String key, String amount) { run(() -> funds.creditSettled(1,key,n(amount))); }
    int journals() { return db.queryForObject("SELECT COUNT(*) FROM settlement_journal",Integer.class); }

    @Test void pendingCreditsSeparatePurchasableRestrictedAndWithdrawableAmounts() {
        cash("deposit","10"); pending("sale","100",REUSABLE); pending("btst","200",RESTRICTED);
        var b = balances();
        assertEquals(n("10"),b.settled()); assertEquals(n("100"),b.reusable());
        assertEquals(n("200"),b.restricted()); assertEquals(n("110"),b.purchasable()); assertEquals(n("10"),b.withdrawable());
        assertThrows(IllegalArgumentException.class, () -> run(() -> funds.withdraw(1,"bad-withdraw",n("11"))));
        assertEquals(3,journals());
        run(() -> funds.withdraw(1,"withdraw",n("10")));
        assertEquals(n("0"),balances().withdrawable());
    }

    @Test void restrictedBtstCannotBeReservedOrWithdrawnBeforeClearance() {
        String id = pending("btst","300",RESTRICTED);
        assertThrows(IllegalArgumentException.class, () -> reserve("buy","1"));
        assertEquals(n("0"),balances().purchasable()); assertEquals(n("0"),balances().reserved());
        time(CUTOFF); assertTrue(get(() -> funds.settle(1,id)));
        assertEquals(n("300"),balances().withdrawable()); reserve("buy","100");
        assertEquals(n("200"),balances().withdrawable());
    }

    @Test void partiallySpentPendingCreditSettlesOnlyItsUnspentUnreservedRemainder() {
        String id = pending("sale","200",REUSABLE); reserve("reserve","100"); spend("fill-1","70");
        time(CUTOFF); assertTrue(get(() -> funds.settle(1,id)));
        assertEquals(n("100"),balances().settled()); assertEquals(n("30"),balances().reserved());
        release("cancel-rest","30"); assertEquals(n("130"),balances().settled());
        assertEquals(n("0"),balances().reserved()); assertEquals(n("0"),balances().reusable());
        assertFalse(get(() -> funds.settle(1,id)));
        release("cancel-rest","30"); spend("fill-1","70");
        assertEquals(n("130"),balances().settled());
    }

    @Test void refundBeforeSettlementRestoresPendingSourceWithoutMakingItWithdrawable() {
        String id = pending("sale","200",REUSABLE); reserve("reserve","100"); release("refund","40");
        assertEquals(n("140"),balances().reusable()); assertEquals(n("60"),balances().reserved());
        assertEquals(n("0"),balances().withdrawable());
        spend("fill","60"); time(CUTOFF); get(() -> funds.settle(1,id));
        assertEquals(n("140"),balances().withdrawable());
    }

    @Test void settlementDoesNotReturnFullySpentProceeds() {
        String id = pending("sale","100",REUSABLE); reserve("reserve","100"); spend("fill","100");
        time(CUTOFF); assertTrue(get(() -> funds.settle(1,id)));
        assertEquals(n("0"),balances().settled()); assertEquals(n("0"),balances().reserved());
    }

    @Test void reservationSurvivesSettlementAndRefundReturnsToSettledCash() {
        String id = pending("sale","100",REUSABLE); reserve("reserve","100");
        time(CUTOFF); get(() -> funds.settle(1,id));
        assertEquals(n("0"),balances().settled()); assertEquals(n("100"),balances().reserved());
        release("cancel","100"); assertEquals(n("100"),balances().settled());
    }

    @Test void mixedSourcesConsumeSettledCashFirstAndReconcileEveryAllocation() {
        cash("deposit","100"); pending("sale","200",REUSABLE); reserve("reserve","250");
        spend("partial-fill","175"); release("partial-refund","75");
        assertEquals(n("125"),balances().reusable()); assertEquals(n("0"),balances().settled());
        assertEquals(n("0"),balances().reserved());
        assertEquals(n("250"),db.queryForObject("SELECT SUM(amount) FROM settlement_allocations",BigDecimal.class));
        assertEquals(n("175"),db.queryForObject("SELECT SUM(amount) FROM settlement_allocation_events WHERE action='SPEND'",BigDecimal.class));
        assertEquals(n("75"),db.queryForObject("SELECT SUM(amount) FROM settlement_allocation_events WHERE action='RELEASE'",BigDecimal.class));
    }

    @Test void retriesAreIdempotentAndDifferentInstructionsCannotReuseAnEventKey() {
        cash("deposit","100"); cash("deposit","100"); reserve("reserve","60"); reserve("reserve","60");
        assertThrows(IllegalArgumentException.class, () -> reserve("reserve","61"));
        assertThrows(IllegalArgumentException.class, () -> cash("reserve","60"));
        assertEquals(n("40"),balances().settled()); assertEquals(n("60"),balances().reserved());
        spend("fill","20"); spend("fill","20"); release("refund","40"); release("refund","40");
        assertEquals(n("80"),balances().settled()); assertEquals(4,journals());
    }

    @Test void existingOpeningReservationIsConsumableAndReleasableWithoutASecondDebit() {
        db.update("INSERT INTO equity_pending_orders VALUES('legacy',1,100,'PENDING')");
        assertEquals(n("100"),balances().reserved());
        run(() -> funds.spend(1,"fill","EQUITY_ORDER","legacy",n("60")));
        run(() -> funds.release(1,"cancel","EQUITY_ORDER","legacy",n("40")));
        assertEquals(n("40"),balances().settled()); assertEquals(n("0"),balances().reserved());
    }

    @Test void reserveCanRequireOnlySettledCashForCollateralOrIpoFunding() {
        pending("sale","100",REUSABLE);
        assertThrows(IllegalArgumentException.class, () -> run(() -> funds.reserve(1,"ipo","IPO_APPLICATION","1",n("1"),false)));
        assertEquals(n("100"),balances().reusable()); assertEquals(n("0"),balances().reserved());
    }

    @Test void missingCoverageNeverSettlesAndLaterReviewedCoverageCanResolveIt() {
        db.update("DELETE FROM settlement_calendar_active"); String id = pending("sale","100",REUSABLE);
        time(CUTOFF.plusSeconds(86400)); assertFalse(get(() -> funds.settle(1,id)));
        assertNull(db.queryForObject("SELECT cutoff_at FROM settlement_obligations WHERE id=?",java.sql.Timestamp.class,id));
        reviewed(List.of(new ReviewedCalendar.Day(TRADE,true,true,"synthetic"),new ReviewedCalendar.Day(TRADE.plusDays(1),true,true,"synthetic")),"b");
        assertTrue(get(() -> funds.settle(1,id))); assertEquals(n("100"),balances().settled());
    }

    @Test void correctedCalendarCanPostponeAnUnresolvedObligationAndMissingCoverageClearsOldDeadline() {
        String id = pending("sale","100",REUSABLE); time(CUTOFF);
        reviewed(List.of(new ReviewedCalendar.Day(TRADE,true,true,"synthetic"),new ReviewedCalendar.Day(TRADE.plusDays(1),true,false,"synthetic holiday"),new ReviewedCalendar.Day(TRADE.plusDays(2),true,true,"synthetic")),"c");
        assertFalse(get(() -> funds.settle(1,id)));
        assertEquals(TRADE.plusDays(2),db.queryForObject("SELECT expected_date FROM settlement_obligations",java.sql.Date.class).toLocalDate());
        db.update("DELETE FROM settlement_calendar_active"); assertFalse(get(() -> funds.settle(1,id)));
        assertNull(db.queryForObject("SELECT expected_date FROM settlement_obligations",java.sql.Date.class));
        assertEquals(n("100"),balances().reusable()); assertEquals(n("0"),balances().settled());
    }

    @Test void exactCutoffIsRequiredAndResolvedOutcomesDoNotChangeOnCalendarRevision() {
        String id = pending("sale","100",REUSABLE); assertFalse(get(() -> funds.settle(1,id)));
        time(CUTOFF); assertTrue(get(() -> funds.settle(1,id)));
        db.update("DELETE FROM settlement_calendar_active"); assertFalse(get(() -> funds.settle(1,id)));
        assertEquals(n("100"),balances().settled());
    }

    @Test void differentAccountsCannotSpendRefundOrSettleAnothersRecords() {
        String id = pending("sale","100",REUSABLE); reserve("reserve","100");
        assertThrows(IllegalArgumentException.class, () -> run(() -> funds.release(2,"steal","ORDER","order-1",n("100"))));
        assertThrows(IllegalArgumentException.class, () -> get(() -> funds.settle(2,id)));
        assertEquals(n("100"),balances().reserved());
        assertEquals(n("200"),get(() -> funds.balances(2)).settled());
    }

    @Test void concurrentOrdersCannotSpendTheSameUnsettledProceedsTwice() throws Exception {
        pending("sale","100",REUSABLE);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var start = new CountDownLatch(1);
            Callable<Boolean> task = () -> { start.await(); try { reserve(UUID.randomUUID().toString(),"75"); return true; } catch (IllegalArgumentException rejected) { return false; } };
            var a = pool.submit(task); var b = pool.submit(task); start.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
            assertEquals(n("25"),balances().reusable()); assertEquals(n("75"),balances().reserved());
        } finally { pool.shutdownNow(); }
    }

    @Test void enclosingFailureRollsBackAllocationsAndJournalsForSafeRetry() {
        pending("sale","100",REUSABLE);
        assertThrows(IllegalStateException.class, () -> run(() -> { funds.reserve(1,"reserve","ORDER","order-1",n("80"),true); throw new IllegalStateException("synthetic outer failure"); }));
        assertEquals(n("100"),balances().reusable()); assertEquals(n("0"),balances().reserved()); assertEquals(1,journals());
        reserve("reserve","80"); assertEquals(n("80"),balances().reserved());
    }

    @Test void negativeOpeningCashIsNotForgivenAndBlocksNewOrdersUntilPaid() {
        db.update("UPDATE users SET virtual_balance=-10 WHERE id=1"); pending("sale","100",REUSABLE);
        assertEquals(n("0"),balances().purchasable()); assertThrows(IllegalArgumentException.class, () -> reserve("reserve","1"));
        cash("deposit","10"); reserve("reserve","1"); assertEquals(n("99"),balances().reusable());
    }

    @Test void invalidAmountsAndClosedDatesCannotCreateJournalOrMoney() {
        for (BigDecimal amount : List.of(new BigDecimal("1.001"),new BigDecimal("-1"),BigDecimal.ZERO,new BigDecimal("100000000000000000"))) {
            assertThrows(IllegalArgumentException.class, () -> run(() -> funds.creditSettled(1,"bad",amount)));
        }
        reviewed(List.of(new ReviewedCalendar.Day(TRADE,false,false,"synthetic closed"),new ReviewedCalendar.Day(TRADE.plusDays(1),true,true,"synthetic")),"d");
        assertThrows(IllegalArgumentException.class, () -> pending("closed","100",REUSABLE)); assertEquals(0,journals());
        assertThrows(IllegalStateException.class, () -> funds.balances(1));
    }

    @Test void restartRetainsIdempotencyAndSourceClassification() {
        String id = pending("sale","100",REUSABLE); reserve("reserve","80"); spend("fill","50");
        time(CUTOFF); get(() -> funds.settle(1,id)); time(CUTOFF.plusSeconds(10));
        spend("fill","50"); reserve("reserve","80"); release("cancel","30");
        assertEquals(n("50"),balances().settled()); assertEquals(n("0"),balances().reserved());
    }

    @Test void simultaneousSettlementAndRefundReconcileRegardlessOfLockOrder() throws Exception {
        String id = pending("sale","100",REUSABLE); reserve("reserve","80"); spend("fill","50"); time(CUTOFF);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var start = new CountDownLatch(1);
            var settlement = pool.submit(() -> { start.await(); return get(() -> funds.settle(1,id)); });
            var refund = pool.submit(() -> { start.await(); release("refund","30"); return true; });
            start.countDown(); assertTrue(settlement.get(10,TimeUnit.SECONDS)); assertTrue(refund.get(10,TimeUnit.SECONDS));
            assertEquals(n("50"),balances().settled()); assertEquals(n("0"),balances().reserved());
        } finally { pool.shutdownNow(); }
    }

    @Test void concurrentSettlementRetriesCreditOnce() throws Exception {
        String id = pending("sale","100",REUSABLE); time(CUTOFF);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var start = new CountDownLatch(1);
            Callable<Boolean> task = () -> { start.await(); return get(() -> funds.settle(1,id)); };
            var a = pool.submit(task); var b = pool.submit(task); start.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
            assertEquals(n("100"),balances().settled());
            assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM settlement_journal WHERE action='SETTLE'",Integer.class));
        } finally { pool.shutdownNow(); }
    }

    @Test void failedSourceJournalWriteRollsBackRefundAndItsParentEvent() {
        pending("sale","100",REUSABLE); reserve("reserve","100");
        db.execute("DROP TABLE settlement_allocation_events");
        assertThrows(RuntimeException.class, () -> release("refund","40"));
        assertEquals(n("0"),balances().reusable()); assertEquals(n("100"),balances().reserved());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM settlement_journal WHERE event_key='refund'",Integer.class));
        new ResourceDatabasePopulator(new ClassPathResource("db/migrations/20261111_settlement_journal.sql")).execute(fixture.source);
        release("refund","40"); assertEquals(n("40"),balances().reusable());
    }

    @Test void anObligationSourceCannotBeCreditedTwiceUnderDifferentEventKeys() {
        pending("sale","100",REUSABLE);
        assertThrows(RuntimeException.class, () -> get(() -> funds.creditPending(1,"duplicate-event","sale","NSE","EQUITY",TRADE,n("100"),REUSABLE)));
        assertEquals(n("100"),balances().reusable()); assertEquals(1,journals());
    }

    @Test void changedReferenceOrFundingPolicyCannotReuseAReservationEvent() {
        cash("cash","100"); reserve("reserve","30");
        assertThrows(IllegalArgumentException.class, () -> run(() -> funds.reserve(1,"reserve","ORDER","other",n("30"),true)));
        assertThrows(IllegalArgumentException.class, () -> run(() -> funds.reserve(1,"reserve","ORDER","order-1",n("30"),false)));
        assertEquals(n("70"),balances().settled()); assertEquals(n("30"),balances().reserved());
    }

    @Test void realizedChargeUsesEligibleCashThenDebtAndRetryCannotChargeAfterNewDeposit(){
        cash("cash","10");pending("sale","20",REUSABLE);pending("btst","100",RESTRICTED);
        run(()->funds.chargeDebt(1,"fee-or-loss",n("50")));
        assertEquals(n("-20"),balances().settled());assertEquals(n("0"),balances().reusable());assertEquals(n("100"),balances().restricted());
        cash("new-deposit","100");run(()->funds.chargeDebt(1,"fee-or-loss",n("50")));
        assertEquals(n("80"),balances().settled());assertEquals(n("100"),balances().restricted());
        assertThrows(IllegalArgumentException.class,()->run(()->funds.chargeDebt(1,"fee-or-loss",n("51"))));
    }
}
