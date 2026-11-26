package com.example.investsmart.deployment;

import com.example.investsmart.entity.User;
import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.security.SecurityAccounts;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Only the generated loopback release-smoke database is accepted; never the operator database. */
@EnabledIfEnvironmentVariable(named="V2_TEST_DB_URL",matches=".+")
@SpringBootTest(properties={"spring.jpa.hibernate.ddl-auto=validate","investsmart.security.initialize-schema=false","spring.sql.init.mode=never"})
@ActiveProfiles("test") @AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@org.springframework.context.annotation.Import(MySqlReleaseTests.TimeConfig.class)
class MySqlReleaseTests {
    @org.springframework.boot.test.context.TestConfiguration static class TimeConfig {
        @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary java.time.Clock mysqlPaperClock(){return java.time.Clock.fixed(java.time.Instant.parse("2026-11-11T04:30:00Z"),java.time.ZoneOffset.UTC);}
    }
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.example.investsmart.market.MarketDataService market;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.example.investsmart.service.NseMarketSessionService sessions;
    @Autowired com.example.investsmart.trading.PaperTrading paper;@Autowired com.example.investsmart.repository.StockRepository stocks;@Autowired java.time.Clock clock;
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry){
        String url=System.getenv("V2_TEST_DB_URL");
        if(!url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/investsmart_release_synthetic\\?allowPublicKeyRetrieval=true&useSSL=false"))
            throw new IllegalStateException("MySQL release tests require the isolated loopback smoke database.");
        registry.add("spring.datasource.url",()->url);
        registry.add("spring.datasource.username",()->System.getenv("V2_TEST_DB_USERNAME"));
        registry.add("spring.datasource.password",()->System.getenv("V2_TEST_DB_PASSWORD"));
    }
    @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired UserRepository users;@Autowired SecurityAccounts accounts;
    @Autowired com.example.investsmart.trading.IntradayTrading intraday;
    @Autowired com.example.investsmart.settlement.SettlementOpening opening;
    @Autowired com.example.investsmart.settlement.SettlementCalendar calendar;
    @Test void mysqlSettlementConcurrentWithdrawalsAndDuplicateCatchUpPreserveCash() throws Exception {
        var user=new User();user.setEmail(UUID.randomUUID()+"@example.invalid");user.setRole("USER");user.setEmailVerified(true);user.setVirtualBalance(new BigDecimal("100.00"));long id=users.saveAndFlush(user).getId();
        var tx=new org.springframework.transaction.support.TransactionTemplate(transactions);
        var trade=java.time.LocalDate.of(2026,11,13);
        var reviewed=new com.example.investsmart.settlement.ReviewedCalendar("BSE","EQUITY","https://example.org/synthetic-calendar.csv","c".repeat(64),java.util.List.of(
                new com.example.investsmart.settlement.ReviewedCalendar.Day(trade,true,true,"synthetic Friday"),
                new com.example.investsmart.settlement.ReviewedCalendar.Day(trade.plusDays(1),false,false,"synthetic closed"),
                new com.example.investsmart.settlement.ReviewedCalendar.Day(trade.plusDays(2),false,false,"synthetic closed"),
                new com.example.investsmart.settlement.ReviewedCalendar.Day(trade.plusDays(3),true,true,"synthetic Monday")));
        String version=tx.execute(status->calendar.activate(reviewed,"RELEASE_TEST","Synthetic settlement dates only"));
        var before=new com.example.investsmart.settlement.SettlementFunds(db,opening,calendar,java.time.Clock.fixed(java.time.Instant.parse("2026-11-16T12:29:59Z"),java.time.ZoneOffset.UTC));
        var after=new com.example.investsmart.settlement.SettlementFunds(db,opening,calendar,java.time.Clock.fixed(java.time.Instant.parse("2026-11-16T12:30:00Z"),java.time.ZoneOffset.UTC));
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            String obligation=tx.execute(status->before.creditPending(id,"restricted","restricted","BSE","EQUITY",trade,new BigDecimal("50.00"),com.example.investsmart.settlement.SettlementFunds.CreditPolicy.RESTRICTED));
            assertEquals(trade.plusDays(3),db.queryForObject("SELECT expected_date FROM settlement_obligations WHERE id=?",java.sql.Date.class,obligation).toLocalDate());
            assertEquals(Boolean.FALSE,tx.execute(status->before.settle(id,obligation)));
            var start=new java.util.concurrent.CountDownLatch(1);var outcomes=new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            for(int i=0;i<2;i++){String event="withdraw-"+i;outcomes.add(pool.submit(()->{start.await();try{tx.executeWithoutResult(status->before.withdraw(id,event,new BigDecimal("80.00")));return true;}catch(IllegalArgumentException rejected){return false;}}));}
            start.countDown();int accepted=0;for(var outcome:outcomes)if(outcome.get(20,java.util.concurrent.TimeUnit.SECONDS))accepted++;
            assertEquals(1,accepted);assertEquals(new BigDecimal("20.00"),db.queryForObject("SELECT settled_cash FROM settlement_accounts WHERE user_id=?",BigDecimal.class,id));
            var fill1=pool.submit(()->tx.execute(status->after.settle(id,obligation)));var fill2=pool.submit(()->tx.execute(status->after.settle(id,obligation)));
            assertNotEquals(fill1.get(20,java.util.concurrent.TimeUnit.SECONDS),fill2.get(20,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(new BigDecimal("70.00"),db.queryForObject("SELECT settled_cash FROM settlement_accounts WHERE user_id=?",BigDecimal.class,id));
            assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM settlement_journal WHERE user_id=? AND action='SETTLE'",Integer.class,id));
        } finally {
            pool.shutdownNow();pool.awaitTermination(20,java.util.concurrent.TimeUnit.SECONDS);
            com.example.investsmart.settlement.SettlementTestCleanup.account(db,id);users.deleteById(id);
            db.update("DELETE FROM settlement_calendar_active WHERE version_id=?",version);db.update("DELETE FROM settlement_calendar_audit WHERE version_id=?",version);db.update("DELETE FROM settlement_calendar_days WHERE version_id=?",version);db.update("DELETE FROM settlement_calendar_versions WHERE id=?",version);
        }
    }
    @Test @Transactional void mysqlIntradayShortCollateralAndPartialExitDoNotCreditShortTurnover(){
        var u=new User();u.setName("Synthetic MIS");u.setEmail(UUID.randomUUID()+"@example.invalid");u.setRole("USER");u.setEmailVerified(true);u.setVirtualBalance(new BigDecimal("10000"));u=users.saveAndFlush(u);
        var s=new com.example.investsmart.entity.Stock();s.setSymbol("MIS_"+UUID.randomUUID());s.setInstrumentKey("NSE_EQ|"+UUID.randomUUID());s.setExchange("NSE");s.setCurrentPrice(new BigDecimal("100"));s=stocks.saveAndFlush(s);final var stock=s;
        org.mockito.Mockito.when(market.getQuote(org.mockito.ArgumentMatchers.any(com.example.investsmart.market.MarketInstrument.class))).thenReturn(new com.example.investsmart.market.MarketQuote(s.getSymbol(),new BigDecimal("100"),null,clock.instant().atZone(com.example.investsmart.options.OptionMarket.INDIA).toLocalDateTime()).withInstrument(new com.example.investsmart.market.MarketInstrument(s.getInstrumentKey(),s.getSymbol(),"Synthetic","NSE")));
        String position=(String)paper.submit(u.getEmail(),new com.example.investsmart.trading.PaperTrading.Ticket(s.getId(),null,null,10,"SELL","MIS","QUICK","MARKET","DAY",null,null,1,null,null,null,UUID.randomUUID().toString())).get("position_id");
        assertEquals(0,users.findById(u.getId()).orElseThrow().getVirtualBalance().compareTo(new BigDecimal("9499.70")));
        org.mockito.Mockito.when(market.getQuote(org.mockito.ArgumentMatchers.any(com.example.investsmart.market.MarketInstrument.class))).thenReturn(new com.example.investsmart.market.MarketQuote(stock.getSymbol(),new BigDecimal("90"),null,clock.instant().atZone(com.example.investsmart.options.OptionMarket.INDIA).toLocalDateTime()).withInstrument(new com.example.investsmart.market.MarketInstrument(stock.getInstrumentKey(),stock.getSymbol(),"Synthetic","NSE")));
        paper.submit(u.getEmail(),new com.example.investsmart.trading.PaperTrading.Ticket(s.getId(),null,null,4,"BUY","MIS","REGULAR","MARKET","DAY",null,null,1,null,null,position,UUID.randomUUID().toString()));
        assertEquals(6,db.queryForObject("SELECT quantity FROM paper_intraday_positions WHERE id=?",Integer.class,position));
        assertEquals(new BigDecimal("300.00"),db.queryForObject("SELECT collateral FROM paper_intraday_positions WHERE id=?",BigDecimal.class,position));
        assertEquals(new BigDecimal("40.00"),db.queryForObject("SELECT SUM(remaining_credit) FROM settlement_obligations WHERE user_id=? AND credit_policy='RESTRICTED'",BigDecimal.class,u.getId()));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM holdings WHERE user_id=?",Integer.class,u.getId()));
    }
    @Autowired com.example.investsmart.security.AdminHealthStore health;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Test void healthWritesAreIndependentOfReadOnlyPreviewAndPaperBuySellWorks(){
        String service="MYSQL_"+UUID.randomUUID().toString().substring(0,20);
        var read=new org.springframework.transaction.support.TransactionTemplate(transactions);read.setReadOnly(true);
        read.executeWithoutResult(tx->{
            assertTrue(db.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>)java.sql.Connection::isReadOnly));
            health.started(service,clock.instant());health.succeeded(service,clock.instant());health.failed(service,clock.instant(),"synthetic");health.completed(service,clock.instant());
            assertTrue(db.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>)java.sql.Connection::isReadOnly));tx.setRollbackOnly();
        });
        assertNotNull(health.status(service).get("lastSuccessAt"));assertEquals(1L,health.status(service).get("failureCount"));
        var u=new User();u.setEmail(UUID.randomUUID()+"@example.invalid");u.setRole("USER");u.setEmailVerified(true);u.setVirtualBalance(new BigDecimal("10000"));u=users.saveAndFlush(u);
        var listing=new com.example.investsmart.entity.Stock();listing.setSymbol("PREVIEW_"+UUID.randomUUID());listing.setInstrumentKey("NSE_EQ|"+UUID.randomUUID());listing.setExchange("NSE");listing.setCurrentPrice(new BigDecimal("100"));listing=stocks.saveAndFlush(listing);final var stock=listing;long userId=u.getId();
        org.mockito.Mockito.when(market.getQuote(org.mockito.ArgumentMatchers.any(com.example.investsmart.market.MarketInstrument.class))).thenAnswer(call->{health.succeeded(service,clock.instant());return new com.example.investsmart.market.MarketQuote(stock.getSymbol(),new BigDecimal("100"),null,clock.instant().atZone(com.example.investsmart.options.OptionMarket.INDIA).toLocalDateTime()).withInstrument(new com.example.investsmart.market.MarketInstrument(stock.getInstrumentKey(),stock.getSymbol(),"Synthetic","NSE"));});
        try{
            var buy=new com.example.investsmart.trading.PaperTrading.Ticket(stock.getId(),null,null,2,"BUY","CASH","REGULAR","MARKET","DAY",null,null,1,null,null,null,UUID.randomUUID().toString());
            assertNotNull(paper.preview(u.getEmail(),buy).get("requiredCash"));assertEquals("FILLED",paper.submit(u.getEmail(),buy).get("status"));
            var sell=new com.example.investsmart.trading.PaperTrading.Ticket(stock.getId(),null,null,2,"SELL","CASH","REGULAR","MARKET","DAY",null,null,1,null,null,null,UUID.randomUUID().toString());
            assertNotNull(paper.preview(u.getEmail(),sell).get("referencePrice"));assertEquals("FILLED",paper.submit(u.getEmail(),sell).get("status"));assertEquals(0,db.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM holdings WHERE user_id=?",Integer.class,userId));
        }finally{com.example.investsmart.settlement.SettlementTestCleanup.account(db,userId);for(String table:java.util.List.of("paper_fills","paper_tickets","orders","holdings"))db.update("DELETE FROM "+table+" WHERE user_id=?",userId);users.deleteById(userId);stocks.deleteById(stock.getId());db.update("DELETE FROM admin_runtime_health WHERE service_name=?",service);}
    }
    @Test void actualMySql84AndManagedSecuritySchemaArePresent(){
        assertTrue(db.queryForObject("SELECT VERSION()",String.class).startsWith("8.4."));
        db.queryForList("SELECT factor_completed FROM auth_sessions WHERE 1=0");
        for(String table:java.util.List.of("phone_credentials","phone_actions","phone_sessions","option_positions","option_orders","equity_pending_orders","ipo_applications","paper_tickets","paper_fills","paper_financing","paper_finance_events","paper_intraday_positions","paper_intraday_events"))db.queryForList("SELECT * FROM "+table+" WHERE 1=0");
    }
    @Test @Transactional void fundsAndSessionOwnershipWorkOnMySql()throws Exception{
        User user=new User();user.setName("Release fixture");user.setEmail(UUID.randomUUID()+"@example.invalid");user.setEmailVerified(true);user.setRole("USER");user.setVirtualBalance(new BigDecimal("100"));user=users.saveAndFlush(user);
        Cookie session=new Cookie(SecurityAccounts.COOKIE,accounts.issue(user,new MockHttpServletRequest(),new MockHttpServletResponse()));
        mvc.perform(post("/wallet/deposit").cookie(session).with(csrf()).contentType("application/json").content("{\"amount\":25.50}")).andExpect(status().isOk());
        mvc.perform(get("/wallet/me").cookie(session)).andExpect(status().isOk()).andExpect(content().string("125.50"));
        mvc.perform(get("/portfolio/me").cookie(session)).andExpect(status().isOk());
        db.update("UPDATE auth_sessions SET revoked=TRUE WHERE user_id=?",user.getId());
        mvc.perform(get("/users/me").cookie(session)).andExpect(status().isUnauthorized());
    }
    @Test void paperOcoAndFinancingExecuteOnActualMySql(){
        var user=new User();user.setName("MySQL paper fixture");user.setEmail(UUID.randomUUID()+"@example.invalid");user.setRole("USER");user.setEmailVerified(true);user.setVirtualBalance(new BigDecimal("10000"));user=users.saveAndFlush(user);
        var stock=new com.example.investsmart.entity.Stock();stock.setSymbol("MYSQL_FIXTURE_"+UUID.randomUUID());stock.setInstrumentKey("NSE_EQ|"+UUID.randomUUID());stock.setExchange("NSE");stock.setCurrentPrice(new BigDecimal("100"));stock=stocks.saveAndFlush(stock);
        final var listing=stock;String email=user.getEmail();long userId=user.getId();
        java.util.function.Consumer<String> quote=price->org.mockito.Mockito.when(market.getQuote(org.mockito.ArgumentMatchers.any(com.example.investsmart.market.MarketInstrument.class))).thenReturn(new com.example.investsmart.market.MarketQuote(listing.getSymbol(),new BigDecimal(price),null,clock.instant().atZone(com.example.investsmart.options.OptionMarket.INDIA).toLocalDateTime()).withInstrument(new com.example.investsmart.market.MarketInstrument(listing.getInstrumentKey(),listing.getSymbol(),"Synthetic","NSE")));
        try{
            quote.accept("100");var request=new com.example.investsmart.trading.PaperTrading.Ticket(stock.getId(),null,null,10,"BUY","CASH","REGULAR","MARKET","DAY",null,null,1,new BigDecimal("10"),new BigDecimal("10"),null,UUID.randomUUID().toString());String parent=(String)paper.submit(email,request).get("id");
            String stop=db.queryForObject("SELECT id FROM paper_tickets WHERE parent_id=? AND order_kind='STOP_LIMIT'",String.class,parent);quote.accept("80");paper.process(stop);assertEquals("TRIGGERED",db.queryForObject("SELECT status FROM paper_tickets WHERE id=?",String.class,stop));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM paper_tickets WHERE parent_id=? AND status='OCO_CANCELLED'",Integer.class,parent));quote.accept("90");paper.process(stop);
            BigDecimal before=users.findById(userId).orElseThrow().getVirtualBalance();quote.accept("100");paper.submit(email,new com.example.investsmart.trading.PaperTrading.Ticket(stock.getId(),null,null,10,"BUY","MTF","REGULAR","MARKET","DAY",null,null,1,null,null,null,UUID.randomUUID().toString()));String financed=db.queryForObject("SELECT id FROM paper_financing WHERE user_id=?",String.class,userId);paper.requestClose(email,financed);paper.manage(financed);assertEquals(0,users.findById(userId).orElseThrow().getVirtualBalance().compareTo(before.subtract(new BigDecimal("0.60"))));assertEquals(0,db.queryForObject("SELECT quantity FROM paper_financing WHERE id=?",Integer.class,financed));
        }finally{com.example.investsmart.settlement.SettlementTestCleanup.account(db,userId);for(String table:java.util.List.of("paper_fills","paper_finance_events","paper_tickets","paper_financing","orders","holdings"))db.update("DELETE FROM "+table+" WHERE user_id=?",userId);users.deleteById(userId);stocks.deleteById(stock.getId());}
    }
}
