package com.example.investsmart.investment;
import com.example.investsmart.entity.*;
import com.example.investsmart.repository.*;
import com.example.investsmart.service.*;
import com.example.investsmart.dto.WalletRequest;
import com.example.investsmart.exception.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest @ActiveProfiles("test")
class IpoAccountingTests {
    static class TestClock extends Clock { volatile Instant now=Instant.parse("2026-11-03T05:00:00Z"); public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return Clock.fixed(now,zone);}public Instant instant(){return now;} }
    @TestConfiguration static class Config { @Bean @Primary TestClock testClock(){return new TestClock();} }
    @Autowired TestClock clock; @Autowired IpoApplicationTransactions tx; @Autowired IpoApplicationRepository apps;
    @Autowired UserRepository users; @Autowired StockRepository stocks; @Autowired HoldingRepository holdings; @Autowired OrderRepository orders;
    @Autowired PortfolioService portfolio; @Autowired WalletService wallet; @Autowired InvestmentAccountService accounts;
    @Autowired OrderService orderService;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.example.investsmart.market.MarketDataService market;
    User user;
    @BeforeEach void setup(){clock.now=Instant.parse("2026-11-03T05:00:00Z");user=new User();user.setEmail(UUID.randomUUID()+"@example.invalid");user.setVirtualBalance(new BigDecimal("10000"));user=users.save(user);}
    @AfterEach void cleanup(){orders.deleteAll(orders.findByUserId(user.getId()));holdings.deleteAll(holdings.findByUserId(user.getId()));apps.deleteAll(apps.findByUserIdOrderByIdDesc(user.getId()));users.deleteById(user.getId());}
    IpoMarketService.Ipo ipo(String status,String subscription,String cutoff,boolean stale){return new IpoMarketService.Ipo("test-ipo","TEST","Test IPO","INE009A01021",status,"regular",new BigDecimal("90"),new BigDecimal("100"),10,10,LocalDate.of(2026,11,3),LocalDate.of(2026,11,4),LocalTime.of(10,0),LocalTime.of(17,0),LocalDate.of(2026,11,5),LocalDate.of(2026,11,8),cutoff==null?null:new BigDecimal(cutoff),subscription==null?null:new BigDecimal(subscription),"NSE,BSE",null,null,clock.instant(),stale,true,false);}
    IpoApplication apply(int lots){tx.apply(user.getEmail(),ipo("open","1",null,false),lots);return apps.findByUserIdAndIpoId(user.getId(),"test-ipo").orElseThrow();}
    void due(){clock.now=Instant.parse("2026-11-05T12:30:00Z");}
    void cash(String amount){assertEquals(0,users.findById(user.getId()).orElseThrow().getVirtualBalance().compareTo(new BigDecimal(amount)));}
    @Test void reservationRetryAndCancellationAreIdempotent(){
        var a=apply(2);cash("8000");var repeat=tx.apply(user.getEmail(),ipo("open","1",null,false),2);assertEquals(a.getId(),repeat.id());cash("8000");
        assertThrows(BadRequestException.class,()->tx.apply(user.getEmail(),ipo("open","1",null,false),3));
        var p=portfolio.getPortfolio(user.getId());assertEquals(0,p.getTotalCash().compareTo(new BigDecimal("10000")));assertEquals(0,p.getTotalValue().compareTo(new BigDecimal("10000")));assertEquals(0,p.getReservedIpoFunds().compareTo(new BigDecimal("2000")));
        tx.cancel(user.getEmail(),a.getId());tx.cancel(user.getEmail(),a.getId());cash("10000");
        assertEquals("CANCELLED",tx.apply(user.getEmail(),ipo("closed","1",null,false),2).status());cash("10000");
    }
    @Test void noEarlySettlementThenMinimumQuantityAndPartialRefundAtCutoff(){
        var a=apply(3);clock.now=Instant.parse("2026-11-05T12:29:59Z");tx.settle(a.getId(),user.getId(),ipo("closed","1","95",false));cash("7000");assertEquals("PENDING",apps.findById(a.getId()).orElseThrow().getStatus());
        due();tx.settle(a.getId(),user.getId(),ipo("closed","1","95",false));cash("9050");
        var result=apps.findById(a.getId()).orElseThrow();assertEquals(10,result.getAllottedQuantity());assertEquals("ALLOTTED",result.getStatus());assertFalse(result.getAssumedIssuePrice());
        var p=portfolio.getPortfolio(user.getId());assertFalse(p.isValuationAvailable());assertNull(p.getTotalValue());assertNull(p.getUnrealizedPnl());assertEquals(0,p.getInvestedAmount().compareTo(new BigDecimal("950")));
        tx.settle(a.getId(),user.getId(),ipo("closed","1000","90",false));cash("9050");assertEquals(result.getDraw(),apps.findById(a.getId()).orElseThrow().getDraw());
    }
    @Test void missingDataWaitsAndCancellationReleasesAllCash(){var a=apply(1);due();tx.settle(a.getId(),user.getId(),ipo("closed",null,null,false));assertEquals("AWAITING_DATA",apps.findById(a.getId()).orElseThrow().getStatus());cash("9000");tx.cancel(user.getEmail(),a.getId());cash("10000");}
    @Test void higherThanReservedCutoffDoesNotDebitExtra(){var a=apply(1);due();tx.settle(a.getId(),user.getId(),ipo("closed","1","101",false));assertEquals("AWAITING_DATA",apps.findById(a.getId()).orElseThrow().getStatus());cash("9000");}
    @Test void savedLosingDrawRefundsAndNeverRerolls(){var a=apply(1);a.setDraw(new BigDecimal("0.9"));apps.save(a);due();tx.settle(a.getId(),user.getId(),ipo("closed","10",null,false));cash("10000");assertEquals("NOT_ALLOTTED",apps.findById(a.getId()).orElseThrow().getStatus());tx.settle(a.getId(),user.getId(),ipo("closed","1",null,false));cash("10000");assertEquals("NOT_ALLOTTED",apps.findById(a.getId()).orElseThrow().getStatus());}
    @Test void listingTransfersOneCostRecordWithoutAnotherCashCharge(){
        var a=apply(2);due();tx.settle(a.getId(),user.getId(),ipo("closed","1",null,false));cash("9000");
        var stock=new Stock();stock.setSymbol("IPO-TEST");stock.setCompanyName("Test");stock.setInstrumentKey("NSE_EQ|IPO_TEST_"+user.getId());stock.setIsin("INE009A01021");stock.setCurrentPrice(new BigDecimal("120"));stock=stocks.save(stock);
        clock.now=Instant.parse("2026-11-08T05:00:00Z");
        try { tx.promote(a.getId(),user.getId(),stock.getId(),ipo("listed","1",null,false));tx.promote(a.getId(),user.getId(),stock.getId(),ipo("listed","1",null,false));cash("9000");
            assertEquals(10,holdings.findByUserIdAndStockId(user.getId(),stock.getId()).orElseThrow().getQuantity());var trades=orders.findByUserId(user.getId());assertEquals(1,trades.size());assertEquals("IPO",trades.getFirst().getSource());
            var p=portfolio.getPortfolio(user.getId());assertTrue(p.isPerformanceAvailable());assertTrue(p.isValuationAvailable());assertEquals(0,p.getInvestedAmount().compareTo(new BigDecimal("1000")));assertEquals(0,p.getUnrealizedPnl().compareTo(new BigDecimal("200")));assertEquals(0,p.getTotalValue().compareTo(new BigDecimal("10200")));
        }finally{orders.deleteAll(orders.findByUserId(user.getId()));holdings.deleteAll(holdings.findByUserId(user.getId()));stocks.deleteById(stock.getId());}
    }
    @Test void anotherUserCannotCancelOrSeeAnApplication(){var a=apply(1);var other=new User();other.setEmail(UUID.randomUUID()+"@example.invalid");other.setVirtualBalance(BigDecimal.ZERO);other=users.save(other);String email=other.getEmail();try{assertThrows(ResourceNotFoundException.class,()->tx.cancel(email,a.getId()));assertTrue(accounts.applications(email).isEmpty());cash("9000");}finally{users.deleteById(other.getId());}}
    @Test void staleOrClosedOrInvalidLotsNeverReserveCash(){assertThrows(BadRequestException.class,()->tx.apply(user.getEmail(),ipo("closed","1",null,false),1));assertThrows(BadRequestException.class,()->tx.apply(user.getEmail(),ipo("open","1",null,true),1));assertThrows(BadRequestException.class,()->tx.apply(user.getEmail(),ipo("open","1",null,false),0));cash("10000");}
    @Test void ipoReservationAndWithdrawalCannotSpendTheSameFunds() throws Exception {
        user.setVirtualBalance(new BigDecimal("1500"));users.save(user);var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        try {var first=executor.submit(()->{start.await();try{apply(1);return true;}catch(BadRequestException e){return false;}});var second=executor.submit(()->{start.await();var r=new WalletRequest();r.setAmount(new BigDecimal("1000"));try{wallet.withdraw(user.getEmail(),r);return true;}catch(BadRequestException e){return false;}});start.countDown();assertEquals(1,(first.get(15,TimeUnit.SECONDS)?1:0)+(second.get(15,TimeUnit.SECONDS)?1:0));cash("500");}finally{executor.shutdownNow();}
    }
    @Test void probabilityUsesExplicitOverallProxy(){assertNull(IpoApplicationTransactions.modelProbability(BigDecimal.ZERO));assertEquals(0,IpoApplicationTransactions.modelProbability(new BigDecimal("0.5")).compareTo(BigDecimal.ONE));assertEquals(0,IpoApplicationTransactions.modelProbability(new BigDecimal("4")).compareTo(new BigDecimal("0.25")));}

    @Test void concurrentIdenticalApplicationsReserveOnce() throws Exception {
        var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        Callable<Long> action=()->{start.await();return tx.apply(user.getEmail(),ipo("open","1",null,false),1).id();};
        try{var first=executor.submit(action);var second=executor.submit(action);start.countDown();assertEquals(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS));cash("9000");assertEquals(1,apps.findByUserIdOrderByIdDesc(user.getId()).size());}finally{executor.shutdownNow();}
    }

    @Test void cancellationAndSettlementRaceCannotRefundTwice() throws Exception {
        var a=apply(1);due();var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        try{var cancel=executor.submit(()->{start.await();try{tx.cancel(user.getEmail(),a.getId());}catch(BadRequestException resolved){}return true;});var settle=executor.submit(()->{start.await();tx.settle(a.getId(),user.getId(),ipo("closed","1",null,false));return true;});start.countDown();cancel.get(15,TimeUnit.SECONDS);settle.get(15,TimeUnit.SECONDS);
            var row=apps.findById(a.getId()).orElseThrow();assertTrue(List.of("CANCELLED","ALLOTTED").contains(row.getStatus()));cash("CANCELLED".equals(row.getStatus())?"10000":"9000");assertEquals(0,row.getReservedAmount().signum());
        }finally{executor.shutdownNow();}
    }

    @Test void ipoAndEquityOrderShareTheSameAvailableCashLock() throws Exception {
        user.setVirtualBalance(new BigDecimal("1500"));users.save(user);var stock=new Stock();stock.setSymbol("IPO-RACE");stock.setInstrumentKey("NSE_EQ|RACE"+user.getId());stock.setCurrentPrice(new BigDecimal("1000"));stock=stocks.save(stock);final var selectedStock=stock;
        when(market.getExchangeStatus("NSE")).thenReturn("NORMAL_OPEN");
        when(market.getQuote(any(com.example.investsmart.market.MarketInstrument.class))).thenReturn(new com.example.investsmart.market.MarketQuote("IPO-RACE",new BigDecimal("1000"),null,java.time.LocalDateTime.now()).withInstrument(new com.example.investsmart.market.MarketInstrument(stock.getInstrumentKey(),"IPO-RACE","Test","NSE")));
        var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        try{var ipo=executor.submit(()->{start.await();try{apply(1);return true;}catch(BadRequestException e){return false;}});var buy=executor.submit(()->{start.await();var r=new com.example.investsmart.dto.OrderRequest();r.setStockId(selectedStock.getId());r.setQuantity(1);try{orderService.buyStock(user.getEmail(),r);return true;}catch(BadRequestException e){return false;}});start.countDown();assertEquals(1,(ipo.get(15,TimeUnit.SECONDS)?1:0)+(buy.get(15,TimeUnit.SECONDS)?1:0));cash("500");}
        finally{executor.shutdownNow();orders.deleteAll(orders.findByUserId(user.getId()));holdings.deleteAll(holdings.findByUserId(user.getId()));stocks.deleteById(stock.getId());}
    }
}
