package com.example.investsmart.service;
import com.example.investsmart.entity.*;
import com.example.investsmart.dto.OrderRequest;
import com.example.investsmart.market.*;
import com.example.investsmart.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest @ActiveProfiles("test") @org.springframework.context.annotation.Import(ConditionalOrdersTests.TimeConfig.class)
class ConditionalOrdersTests {
    @Autowired WalletService wallet;
    @Autowired ConditionalOrders conditional;@Autowired OrderService orders;@Autowired UserRepository users;@Autowired StockRepository stocks;@Autowired HoldingRepository holdings;@Autowired OrderRepository history;@Autowired JdbcTemplate db;@Autowired PortfolioService portfolio;
    @MockitoBean MarketDataService market;@MockitoBean NseMarketSessionService sessions;@Autowired MutableClock clock;
    static class MutableClock extends Clock { Instant now=Instant.parse("2026-11-10T04:30:00Z");public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return Clock.fixed(now,zone);}public Instant instant(){return now;} }
    @org.springframework.boot.test.context.TestConfiguration static class TimeConfig { @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary MutableClock testClock(){return new MutableClock();} }
    User user;Stock stock;
    @BeforeEach void setup(){
        clock.now=Instant.parse("2026-11-10T04:30:00Z");
        user=new User();user.setEmail(UUID.randomUUID()+"@example.invalid");user.setName("Conditional test");user.setAccountStatus("ACTIVE");user.setVirtualBalance(new BigDecimal("1000"));user=users.saveAndFlush(user);
        stock=new Stock();stock.setSymbol(UUID.randomUUID().toString());stock.setCompanyName("Fixture");stock.setInstrumentKey("NSE_EQ|"+UUID.randomUUID());stock.setExchange("NSE");stock.setCurrentPrice(BigDecimal.TEN);stock=stocks.saveAndFlush(stock);quote("10");
    }
    @AfterEach void cleanup(){com.example.investsmart.settlement.SettlementTestCleanup.account(db,user.getId());db.update("DELETE FROM equity_pending_orders WHERE user_id=?",user.getId());db.update("DELETE FROM orders WHERE user_id=?",user.getId());db.update("DELETE FROM holdings WHERE user_id=?",user.getId());users.deleteById(user.getId());stocks.deleteById(stock.getId());}
    void quote(String price){when(market.getQuote(any(MarketInstrument.class))).thenReturn(new MarketQuote(stock.getSymbol(),new BigDecimal(price),null,LocalDateTime.now()).withInstrument(new MarketInstrument(stock.getInstrumentKey(),stock.getSymbol(),stock.getCompanyName(),"NSE")));}
    ConditionalOrders.Ticket ticket(String side,String kind,String price,int count){return new ConditionalOrders.Ticket(stock.getId(),count,side,kind,new BigDecimal(price),UUID.randomUUID().toString());}
    OrderRequest request(int count){var r=new OrderRequest();r.setStockId(stock.getId());r.setQuantity(count);return r;}
    void cash(String expected){assertEquals(0,users.findById(user.getId()).orElseThrow().getVirtualBalance().compareTo(new BigDecimal(expected)));}
    @Test void limitReservationPreservesPortfolioTotalAndCancelsExactlyOnce(){var t=ticket("BUY","LIMIT","8",10);var order=conditional.submit(user.getEmail(),t);conditional.submit(user.getEmail(),t);cash("920");assertEquals(0,portfolio.getPortfolio(user.getId()).getTotalValue().compareTo(new BigDecimal("1000")));conditional.cancel(user.getEmail(),(String)order.get("id"));conditional.cancel(user.getEmail(),(String)order.get("id"));cash("1000");}
    @Test void limitOrderExecutesThroughNormalPriceAndSessionGuardsOnce(){var order=conditional.submit(user.getEmail(),ticket("BUY","LIMIT","9",10));String id=(String)order.get("id");conditional.process(id);assertEquals(0,history.findByUserId(user.getId()).size());quote("8");conditional.process(id);conditional.process(id);cash("920");assertEquals(1,history.findByUserId(user.getId()).size());assertEquals(10,holdings.findByUserIdAndStockId(user.getId(),stock.getId()).orElseThrow().getQuantity());}
    @Test void protectiveStopPreventsImmediateAndPendingDoubleSales(){orders.buyStock(user.getEmail(),request(10));var stop=conditional.submit(user.getEmail(),ticket("SELL","STOP_MARKET","9",10));assertThrows(RuntimeException.class,()->orders.sellStock(user.getEmail(),request(1)));assertThrows(RuntimeException.class,()->conditional.submit(user.getEmail(),ticket("SELL","LIMIT","12",1)));quote("8");conditional.process((String)stop.get("id"));cash("900");assertEquals(new BigDecimal("80.00"),db.queryForObject("SELECT SUM(remaining_credit) FROM settlement_obligations WHERE user_id=? AND credit_policy='RESTRICTED'",BigDecimal.class,user.getId()));assertEquals(0,holdings.findByUserIdAndStockId(user.getId(),stock.getId()).orElseThrow().getQuantity());}
    @Test void expiryAndSessionFailureCannotLoseReservedCash(){var order=conditional.submit(user.getEmail(),ticket("BUY","LIMIT","12",10));String id=(String)order.get("id");doThrow(new RuntimeException("Closed")).when(sessions).requireOpenSession("NSE");assertThrows(RuntimeException.class,()->conditional.process(id));cash("880");assertEquals("PENDING",conditional.list(user.getEmail()).getFirst().get("status"));clock.now=Instant.parse("2026-11-10T10:01:00Z");conditional.process(id);cash("1000");assertEquals("EXPIRED",conditional.list(user.getEmail()).getFirst().get("status"));}
    @Test void changedExecutionQuoteCannotBreachTheLimit(){var order=conditional.submit(user.getEmail(),ticket("BUY","LIMIT","9",10));var cheap=new MarketQuote(stock.getSymbol(),new BigDecimal("8"),null,LocalDateTime.now()).withInstrument(new MarketInstrument(stock.getInstrumentKey(),stock.getSymbol(),stock.getCompanyName(),"NSE"));var dear=new MarketQuote(stock.getSymbol(),new BigDecimal("10"),null,LocalDateTime.now()).withInstrument(new MarketInstrument(stock.getInstrumentKey(),stock.getSymbol(),stock.getCompanyName(),"NSE"));when(market.getQuote(any(MarketInstrument.class))).thenReturn(cheap,dear);assertThrows(RuntimeException.class,()->conditional.process((String)order.get("id")));cash("910");assertEquals("PENDING",conditional.list(user.getEmail()).getFirst().get("status"));assertTrue(history.findByUserId(user.getId()).isEmpty());}
    @Test void deletedListingDoesNotHidePendingOrderOrLoseItsCash(){var order=conditional.submit(user.getEmail(),ticket("BUY","LIMIT","9",10));stocks.deleteById(stock.getId());assertEquals(1,conditional.list(user.getEmail()).size());conditional.process((String)order.get("id"));cash("1000");assertEquals("CANCELLED",conditional.list(user.getEmail()).getFirst().get("status"));}

    @Test void legacyBtstCannotFundAnotherBuyOrWithdrawalAndKeepsDeliveryLinks(){
        orders.buyStock(user.getEmail(),request(100));quote("11");orders.sellStock(user.getEmail(),request(100));cash("0");
        var withdrawal=new com.example.investsmart.dto.WalletRequest();withdrawal.setAmount(BigDecimal.ONE);
        assertThrows(com.example.investsmart.exception.BadRequestException.class,()->wallet.withdraw(user.getEmail(),withdrawal));
        assertThrows(com.example.investsmart.exception.BadRequestException.class,()->orders.buyStock(user.getEmail(),request(1)));
        assertThrows(RuntimeException.class,()->conditional.submit(user.getEmail(),ticket("BUY","LIMIT","9",1)));
        assertEquals(new BigDecimal("1100.00"),db.queryForObject("SELECT SUM(remaining_credit) FROM settlement_obligations WHERE user_id=? AND credit_policy='RESTRICTED'",BigDecimal.class,user.getId()));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM settlement_dependencies WHERE user_id=?",Integer.class,user.getId()));
        wallet.deposit(user.getEmail(),withdrawal);wallet.withdraw(user.getEmail(),withdrawal);cash("0");
    }

    @Test void settledOpeningSaleCanFundConditionalOrdersButRefundCannotBecomeWithdrawable(){
        var h=new Holding();h.setUser(user);h.setStock(stock);h.setQuantity(10);holdings.saveAndFlush(h);
        var withdrawal=new com.example.investsmart.dto.WalletRequest();withdrawal.setAmount(new BigDecimal("1000"));wallet.withdraw(user.getEmail(),withdrawal);
        orders.sellStock(user.getEmail(),request(10));cash("100");
        var order=conditional.submit(user.getEmail(),ticket("BUY","LIMIT","9",10));cash("10");
        conditional.cancel(user.getEmail(),(String)order.get("id"));conditional.cancel(user.getEmail(),(String)order.get("id"));cash("100");
        withdrawal.setAmount(BigDecimal.ONE);
        assertThrows(com.example.investsmart.exception.BadRequestException.class,()->wallet.withdraw(user.getEmail(),withdrawal));
        assertEquals(new BigDecimal("100.00"),db.queryForObject("SELECT SUM(remaining_credit) FROM settlement_obligations WHERE user_id=? AND credit_policy='REUSABLE'",BigDecimal.class,user.getId()));
    }
}
