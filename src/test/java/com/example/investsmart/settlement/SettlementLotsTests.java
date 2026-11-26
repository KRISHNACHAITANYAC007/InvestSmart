package com.example.investsmart.settlement;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

class SettlementLotsTests {
    SettlementFundsTests fixture;
    SettlementLots lots;
    @BeforeEach void setup() {
        fixture = new SettlementFundsTests(); fixture.setup(); time(SettlementFundsTests.BEFORE);
    }
    void time(Instant now) {
        fixture.time(now);
        lots = new SettlementLots(fixture.db, fixture.funds, fixture.calendar, Clock.fixed(now,ZoneOffset.UTC));
    }
    String buy(String event, int units) {
        return fixture.get(() -> lots.purchase(1,event,event,5,units,new BigDecimal("150.00"),"NSE",SettlementFundsTests.TRADE));
    }
    SettlementLots.Disposal sell(String event, int units) { return fixture.get(() -> lots.dispose(1,event,5,units)); }
    SettlementLots.Units units() { return fixture.get(() -> lots.units(1,5)); }
    String obligation(String lot) { return fixture.db.queryForObject("SELECT obligation_id FROM settlement_lots WHERE id=?",String.class,lot); }

    @Test void purchasesCreatePendingUnitsAndLeaveAggregateHistoryAndCashUntouched() {
        var holdings = fixture.db.queryForList("SELECT * FROM holdings ORDER BY id");
        String id = buy("purchase",10);
        assertEquals(new SettlementLots.Units(13,10),units());
        assertEquals(holdings,fixture.db.queryForList("SELECT * FROM holdings ORDER BY id"));
        assertEquals(new BigDecimal("0.00"),fixture.balances().settled());
        assertNotNull(obligation(id));
        assertEquals(new BigDecimal("150.000000000000"),fixture.db.queryForObject("SELECT cost_amount FROM settlement_lots WHERE id=?",BigDecimal.class,id));
    }

    @Test void settledUnitsAreSoldFirstAndBtstClassificationSurvivesDeliveryAndReplay() {
        String id = buy("purchase",10);
        assertEquals(new SettlementLots.Disposal(13,5),sell("sale",18));
        assertEquals(new SettlementLots.Units(0,5),units());
        time(SettlementFundsTests.CUTOFF);
        assertTrue(fixture.get(() -> fixture.funds.settle(1,obligation(id))));
        assertEquals(new SettlementLots.Units(5,0),units());
        assertEquals(new SettlementLots.Disposal(13,5),sell("sale",18));
        assertEquals(new SettlementLots.Units(5,0),units());
        assertEquals(new BigDecimal("0.00"),fixture.balances().settled());
    }

    @Test void deliveryDoesNotResurrectUnitsAlreadySoldThroughBtst() {
        String id = buy("purchase",10); sell("all",23);
        time(SettlementFundsTests.CUTOFF); fixture.get(() -> fixture.funds.settle(1,obligation(id)));
        assertEquals(new SettlementLots.Units(0,0),units());
        assertEquals(new SettlementLots.Disposal(13,10),sell("all",23));
    }

    @Test void acquisitionAndDisposalRetriesCannotChangeQuantityPriceOrListing() {
        String id = buy("purchase",10); assertEquals(id,buy("purchase",10));
        assertThrows(IllegalArgumentException.class, () -> buy("purchase",11));
        sell("sale",4); assertThrows(IllegalArgumentException.class, () -> sell("sale",5));
        assertThrows(IllegalArgumentException.class, () -> fixture.get(() -> lots.dispose(1,"sale",6,4)));
        assertEquals(new SettlementLots.Units(9,10),units());
    }

    @Test void excessDisposalAndOuterFailureRollBackLotsAndJournal() {
        buy("purchase",10);
        assertThrows(IllegalArgumentException.class, () -> sell("bad",24));
        assertThrows(IllegalStateException.class, () -> fixture.run(() -> {
            lots.dispose(1,"rollback",5,15); throw new IllegalStateException("synthetic outer failure");
        }));
        assertEquals(new SettlementLots.Units(13,10),units());
        assertEquals(new SettlementLots.Disposal(13,2),sell("rollback",15));
    }

    @Test void listingTransferIsSettledImmediatelyAndDoesNotCreateCashOrAnotherObligation() {
        String id = fixture.get(() -> lots.deliverSettled(1,"ipo-transfer","IPO:1",7,20,new BigDecimal("400.00")));
        assertEquals(id,fixture.get(() -> lots.deliverSettled(1,"ipo-transfer","IPO:1",7,20,new BigDecimal("400.00"))));
        assertEquals(new SettlementLots.Units(20,0),fixture.get(() -> lots.units(1,7)));
        assertNull(obligation(id)); assertEquals(0,fixture.db.queryForObject("SELECT COUNT(*) FROM settlement_obligations",Integer.class));
        assertEquals(new BigDecimal("0.00"),fixture.balances().settled());
    }

    @Test void unknownCalendarKeepsPurchasedUnitsPending() {
        fixture.db.update("DELETE FROM settlement_calendar_active"); String id = buy("purchase",10);
        time(SettlementFundsTests.CUTOFF.plusSeconds(86400));
        assertFalse(fixture.get(() -> fixture.funds.settle(1,obligation(id))));
        assertEquals(new SettlementLots.Units(13,10),units());
    }

    @Test void anotherAccountsLotsCannotBeDisposedOrTransferredThroughReplay() {
        buy("purchase",10);
        assertThrows(IllegalArgumentException.class, () -> fixture.get(() -> lots.dispose(2,"sale",5,3)));
        assertEquals(new SettlementLots.Disposal(2,0),fixture.get(() -> lots.dispose(2,"sale",5,2)));
        assertEquals(new SettlementLots.Units(13,10),units());
    }

    @Test void concurrentDisposalsCannotSellTheSameUnitsTwice() throws Exception {
        fixture.get(() -> lots.units(1,5));
        var pool = Executors.newFixedThreadPool(2);
        try {
            var start = new CountDownLatch(1);
            Callable<Boolean> action = () -> { start.await(); try { sell(java.util.UUID.randomUUID().toString(),10); return true; } catch (IllegalArgumentException rejected) { return false; } };
            var a = pool.submit(action); var b = pool.submit(action); start.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
            assertEquals(new SettlementLots.Units(3,0),units());
        } finally { pool.shutdownNow(); }
    }

    @Test void invalidUnitsAndFractionalCashFailWithoutNewLots() {
        assertThrows(IllegalArgumentException.class, () -> buy("bad",0));
        assertThrows(IllegalArgumentException.class, () -> fixture.get(() -> lots.purchase(1,"bad","bad",5,1,new BigDecimal("1.001"),"NSE",SettlementFundsTests.TRADE)));
        assertEquals(0,fixture.db.queryForObject("SELECT COUNT(*) FROM settlement_lots",Integer.class));
    }

    @Test void btstCashWaitsForBothSaleDeadlineAndUnderlyingDeliveryCoverage() {
        String lot = buy("purchase",10); sell("btst-sale",23);
        String delivery = obligation(lot);
        var dependencies = fixture.get(() -> lots.unsettledDeliveries(1,"btst-sale"));
        assertEquals(java.util.List.of(delivery),dependencies);
        var day = SettlementFundsTests.TRADE;
        String credit = fixture.get(() -> fixture.funds.creditPending(1,"btst-credit","btst-credit","NSE","EQUITY",day.plusDays(1),new BigDecimal("100.00"),SettlementFunds.CreditPolicy.RESTRICTED));
        fixture.run(() -> fixture.funds.requireDeliveries(1,"btst-link",credit,dependencies));
        // Sale calendar is covered but the earlier purchase date is deliberately absent.
        fixture.reviewed(java.util.List.of(new ReviewedCalendar.Day(day.plusDays(1),true,true,"synthetic"),new ReviewedCalendar.Day(day.plusDays(2),true,true,"synthetic")),"e");
        time(SettlementFundsTests.CUTOFF.plusSeconds(86400));
        assertFalse(fixture.get(() -> fixture.funds.settle(1,credit)));
        assertEquals(new BigDecimal("100.00"),fixture.balances().restricted());
        assertEquals(new BigDecimal("0.00"),fixture.balances().withdrawable());
        fixture.reviewed(java.util.List.of(new ReviewedCalendar.Day(day,true,true,"synthetic"),new ReviewedCalendar.Day(day.plusDays(1),true,true,"synthetic"),new ReviewedCalendar.Day(day.plusDays(2),true,true,"synthetic")),"f");
        assertTrue(fixture.get(() -> fixture.funds.settle(1,credit)));
        assertEquals(new BigDecimal("100.00"),fixture.balances().withdrawable());
        assertEquals("SETTLED",fixture.db.queryForObject("SELECT status FROM settlement_obligations WHERE id=?",String.class,delivery));
        assertEquals(new SettlementLots.Units(0,0),units());
        fixture.run(() -> fixture.funds.requireDeliveries(1,"btst-link",credit,dependencies));
        assertEquals(dependencies,fixture.get(() -> lots.unsettledDeliveries(1,"btst-sale")));
    }

    @Test void dependencyLinksRequireOwnedDeliveryAndPendingRestrictedCash() {
        String delivery = obligation(buy("purchase",10));
        String reusable = fixture.pending("sale","100",SettlementFunds.CreditPolicy.REUSABLE);
        String restricted = fixture.pending("btst","100",SettlementFunds.CreditPolicy.RESTRICTED);
        assertThrows(IllegalArgumentException.class, () -> fixture.run(() -> fixture.funds.requireDeliveries(1,"bad-link",reusable,java.util.List.of(delivery))));
        assertThrows(IllegalArgumentException.class, () -> fixture.run(() -> fixture.funds.requireDeliveries(1,"bad-link",restricted,java.util.List.of(reusable))));
        assertThrows(IllegalArgumentException.class, () -> fixture.run(() -> fixture.funds.requireDeliveries(2,"other-account",restricted,java.util.List.of(delivery))));
        fixture.run(() -> fixture.funds.requireDeliveries(1,"link",restricted,java.util.List.of(delivery,delivery)));
        assertEquals(1,fixture.db.queryForObject("SELECT COUNT(*) FROM settlement_dependencies",Integer.class));
        time(SettlementFundsTests.CUTOFF); fixture.get(() -> fixture.funds.settle(1,restricted));
        assertThrows(IllegalArgumentException.class, () -> fixture.run(() -> fixture.funds.requireDeliveries(1,"late-link",restricted,java.util.List.of(delivery))));
        assertEquals(1,fixture.db.queryForObject("SELECT COUNT(*) FROM settlement_dependencies",Integer.class));
    }

    @Test void partialDisposalRetainsAcquisitionCostAndUnrelatedListingLots() {
        String id = buy("purchase",10);
        fixture.get(() -> lots.deliverSettled(1,"transfer","IPO:7",7,4,new BigDecimal("80.00")));
        sell("sale",16);
        assertEquals(new BigDecimal("150.000000000000"),fixture.db.queryForObject("SELECT cost_amount FROM settlement_lots WHERE id=?",BigDecimal.class,id));
        assertEquals(10,fixture.db.queryForObject("SELECT initial_units FROM settlement_lots WHERE id=?",Integer.class,id));
        assertEquals(new SettlementLots.Units(4,0),fixture.get(() -> lots.units(1,7)));
    }
}
