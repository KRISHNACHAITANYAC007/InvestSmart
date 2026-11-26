package com.example.investsmart.options;

import com.example.investsmart.entity.*;
import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.market.MarketDataService;
import com.example.investsmart.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest @ActiveProfiles("test")
class OptionTradingTests {
    @Autowired OptionTrading trading;@Autowired UserRepository users;@Autowired JdbcTemplate db;@Autowired PortfolioService portfolios;
    @MockitoBean OptionMarket market;@MockitoBean NseMarketSessionService sessions;@MockitoBean MarketDataService prices;
    User user;LocalDate expiry;OptionMarket.Contract contract;OptionMarket.Quote quote;
    @BeforeEach void setup(){
        user=new User();user.setName("Option test");user.setEmail(UUID.randomUUID()+"@example.invalid");user.setRole("USER");user.setAccountStatus("ACTIVE");user.setEmailVerified(true);user.setVirtualBalance(new BigDecimal("10000"));user=users.saveAndFlush(user);
        expiry=LocalDate.now(OptionMarket.INDIA).plusDays(7);contract=new OptionMarket.Contract("NSE_FO|123","NIFTY fixture CE",expiry,new BigDecimal("24000"),"CE",50,1000);
        quote=quote("9","10");when(market.chain(expiry)).thenReturn(List.of(quote));
    }
    @AfterEach void cleanup(){com.example.investsmart.settlement.SettlementTestCleanup.account(db,user.getId());db.update("DELETE FROM option_orders WHERE user_id=?",user.getId());db.update("DELETE FROM option_positions WHERE user_id=?",user.getId());users.deleteById(user.getId());}
    OptionMarket.Quote quote(String bid,String ask){return new OptionMarket.Quote(contract,new BigDecimal("9.5"),new BigDecimal(bid),new BigDecimal(ask),1000,1000,BigDecimal.TEN,BigDecimal.TEN,Instant.now());}
    OptionTrading.Ticket ticket(String side,String kind,int lots,String price){return new OptionTrading.Ticket(contract.instrumentKey(),expiry,side,kind,lots,price==null?null:new BigDecimal(price),UUID.randomUUID().toString());}
    void cash(String expected){assertEquals(0,users.findById(user.getId()).orElseThrow().getVirtualBalance().compareTo(new BigDecimal(expected)));}
    int held(){return db.queryForObject("SELECT COALESCE(SUM(units),0) FROM option_positions WHERE user_id=?",Integer.class,user.getId());}
    @Test void marketBuyUsesWholeLotsAskAndPortfolioBidValue(){
        var order=trading.submit(user.getEmail(),ticket("BUY","MARKET",1,null));assertEquals("FILLED",order.get("status"));cash("9500");assertEquals(50,held());
        var portfolio=portfolios.getPortfolio(user.getId());assertEquals(0,portfolio.getTotalValue().compareTo(new BigDecimal("9950")));assertEquals(0,portfolio.getOptionsCost().compareTo(new BigDecimal("500")));assertEquals(0,portfolio.getUnrealizedPnl().compareTo(new BigDecimal("-50")));
    }
    @Test void duplicateRequestReservesOnceAndCancelRefundsOnce(){
        var request=ticket("BUY","LIMIT",1,"8");var first=trading.submit(user.getEmail(),request);var second=trading.submit(user.getEmail(),request);assertEquals(first.get("id"),second.get("id"));cash("9600");
        assertEquals(0,portfolios.getPortfolio(user.getId()).getTotalValue().compareTo(new BigDecimal("10000")));
        trading.cancel(user.getEmail(),(String)first.get("id"));trading.cancel(user.getEmail(),(String)first.get("id"));cash("10000");assertEquals(0,held());
    }
    @Test void limitFillsOnlyAtOrBetterThanLimitAndRefundsDifference(){
        var order=trading.submit(user.getEmail(),ticket("BUY","LIMIT",1,"9"));String id=(String)order.get("id");trading.process(id);assertEquals(0,held());
        when(market.chain(expiry)).thenReturn(List.of(quote("7","8")));trading.process(id);trading.process(id);assertEquals(50,held());cash("9600");
    }
    @Test void sellStopReservesOwnedUnitsAndGapFillsAtBid(){
        trading.submit(user.getEmail(),ticket("BUY","MARKET",1,null));var stop=trading.submit(user.getEmail(),ticket("SELL","STOP_MARKET",1,"8"));
        assertThrows(ResponseStatusException.class,()->trading.submit(user.getEmail(),ticket("SELL","MARKET",1,null)));
        trading.process((String)stop.get("id"));assertEquals(50,held());when(market.chain(expiry)).thenReturn(List.of(quote("7","8")));trading.process((String)stop.get("id"));cash("9500");assertEquals(new BigDecimal("350.00"),db.queryForObject("SELECT SUM(remaining_credit) FROM settlement_obligations WHERE user_id=? AND credit_policy='RESTRICTED'",BigDecimal.class,user.getId()));assertEquals(0,held());
        assertEquals(0,trading.totals(user.getId()).realized().compareTo(new BigDecimal("-150")));
    }
    @Test void missingDepthClosedMarketAndFreezeRejectWithoutMutations(){
        when(market.chain(expiry)).thenReturn(List.of(new OptionMarket.Quote(contract,BigDecimal.TEN,BigDecimal.TEN,BigDecimal.TEN,1,1,null,null,Instant.now())));
        assertThrows(ResponseStatusException.class,()->trading.submit(user.getEmail(),ticket("BUY","MARKET",1,null)));cash("10000");
        when(market.chain(expiry)).thenReturn(List.of(quote));assertThrows(ResponseStatusException.class,()->trading.submit(user.getEmail(),ticket("BUY","MARKET",21,null)));cash("10000");
        doThrow(new com.example.investsmart.exception.BadRequestException("Closed")).when(sessions).requireOpenSession("NSE");assertThrows(RuntimeException.class,()->trading.submit(user.getEmail(),ticket("BUY","MARKET",1,null)));cash("10000");assertEquals(0,held());
    }
    @Test void nakedSalesStopBuysAndInvalidLotsAreRejected(){
        assertThrows(ResponseStatusException.class,()->trading.submit(user.getEmail(),ticket("SELL","MARKET",1,null)));
        assertThrows(ResponseStatusException.class,()->trading.submit(user.getEmail(),ticket("BUY","STOP_MARKET",1,"5")));
        assertThrows(ResponseStatusException.class,()->trading.submit(user.getEmail(),ticket("BUY","LIMIT",0,"5")));cash("10000");
    }
    @Test void staleMissingAndFutureQuoteTimesCannotFillOrSpendCash(){
        for(Instant timestamp:Arrays.asList(null,Instant.now().minusSeconds(120),Instant.now().plusSeconds(120))){
            when(market.chain(expiry)).thenReturn(List.of(new OptionMarket.Quote(contract,quote.ltp(),quote.bid(),quote.ask(),1000,1000,null,null,timestamp)));
            var failure=assertThrows(ResponseStatusException.class,()->trading.submit(user.getEmail(),ticket("BUY","MARKET",1,null)));
            assertEquals(503,failure.getStatusCode().value());cash("10000");assertEquals(0,held());
            assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM option_orders WHERE user_id=?",Integer.class,user.getId()));
        }
    }
    @Test void stalePendingQuotePreservesReservationUntilFreshDepthArrives(){
        var order=trading.submit(user.getEmail(),ticket("BUY","LIMIT",1,"10"));String id=(String)order.get("id");
        when(market.chain(expiry)).thenReturn(List.of(new OptionMarket.Quote(contract,quote.ltp(),quote.bid(),quote.ask(),1000,1000,null,null,Instant.now().minusSeconds(120))));
        assertThrows(ResponseStatusException.class,()->trading.process(id));cash("9500");assertEquals(0,held());
        var pending=trading.account(user.getEmail());assertEquals("PENDING",((List<Map<String,Object>>)pending.get("orders")).getFirst().get("status"));
        when(market.chain(expiry)).thenReturn(List.of(quote("8","9")));trading.process(id);trading.process(id);cash("9550");assertEquals(50,held());
    }
    @Test void concurrentReservationsCannotDoubleSpend()throws Exception{
        user.setVirtualBalance(new BigDecimal("750"));users.saveAndFlush(user);var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Callable<Boolean> action=()->{start.await();try{trading.submit(user.getEmail(),ticket("BUY","LIMIT",1,"10"));return true;}catch(ResponseStatusException rejected){return false;}};
        try{var a=pool.submit(action);var b=pool.submit(action);start.countDown();assertEquals(1,(a.get(15,TimeUnit.SECONDS)?1:0)+(b.get(15,TimeUnit.SECONDS)?1:0));cash("250");}finally{pool.shutdownNow();}
    }
    @Test void expiryNeverUsesAnUndatedOrWrongDayQuoteAndSettlesOnce(){
        LocalDate date=LocalDate.now(OptionMarket.INDIA).minusDays(1);
        db.update("INSERT INTO option_positions(user_id,instrument_key,symbol,expiry,strike_price,option_type,lot_size,units,cost_amount,last_price,mark_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",user.getId(),contract.instrumentKey(),contract.symbol(),date,contract.strike(),"CE",50,50,new BigDecimal("500"),BigDecimal.TEN,java.sql.Timestamp.from(Instant.now()));
        when(prices.getHistory("NIFTY 50","1Y")).thenReturn(List.of());trading.settle(user.getId(),contract.instrumentKey(),date);cash("10000");assertEquals(50,held());assertNull(trading.totals(user.getId()).value());
        var candle=new MarketPriceHistory();candle.setTimestamp(date.atStartOfDay());candle.setPrice(new BigDecimal("24020"));when(prices.getHistory("NIFTY 50","1Y")).thenReturn(List.of(candle));
        trading.settle(user.getId(),contract.instrumentKey(),date);trading.settle(user.getId(),contract.instrumentKey(),date);cash("10000");assertEquals(new BigDecimal("1000.00"),db.queryForObject("SELECT SUM(remaining_credit) FROM settlement_obligations WHERE user_id=? AND credit_policy='RESTRICTED'",BigDecimal.class,user.getId()));assertEquals(0,held());assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM option_orders WHERE user_id=? AND side='SETTLEMENT'",Integer.class,user.getId()));
    }
    @Test void pendingExpiryRefundsOnceWithoutAMarketQuote(){var order=trading.submit(user.getEmail(),ticket("BUY","LIMIT",1,"5"));db.update("UPDATE option_orders SET expiry=? WHERE id=?",LocalDate.now(OptionMarket.INDIA).minusDays(1),order.get("id"));trading.process((String)order.get("id"));trading.process((String)order.get("id"));cash("10000");assertEquals(0,held());}
    @Test void outOfMoneyPutSettlesAtZeroRatherThanKeepingAStalePremium(){LocalDate date=LocalDate.now(OptionMarket.INDIA).minusDays(1);db.update("INSERT INTO option_positions(user_id,instrument_key,symbol,expiry,strike_price,option_type,lot_size,units,cost_amount,last_price,mark_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",user.getId(),"NSE_FO|124","NIFTY fixture PE",date,new BigDecimal("24000"),"PE",50,50,new BigDecimal("500"),BigDecimal.TEN,java.sql.Timestamp.from(Instant.now()));var candle=new MarketPriceHistory();candle.setTimestamp(date.atStartOfDay());candle.setPrice(new BigDecimal("24020"));when(prices.getHistory("NIFTY 50","1Y")).thenReturn(List.of(candle));trading.settle(user.getId(),"NSE_FO|124",date);cash("10000");assertEquals(0,held());assertEquals(0,trading.totals(user.getId()).realized().compareTo(new BigDecimal("-500")));}
    @Test void cancellationCannotAccessAnotherAccount(){var order=trading.submit(user.getEmail(),ticket("BUY","LIMIT",1,"5"));var other=new User();other.setEmail(UUID.randomUUID()+"@example.invalid");other.setAccountStatus("ACTIVE");other.setVirtualBalance(BigDecimal.ZERO);other=users.saveAndFlush(other);try{String email=other.getEmail();assertThrows(ResponseStatusException.class,()->trading.cancel(email,(String)order.get("id")));cash("9750");}finally{users.deleteById(other.getId());}}
}
