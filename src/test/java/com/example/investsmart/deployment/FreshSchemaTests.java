package com.example.investsmart.deployment;

import com.example.investsmart.entity.*;
import com.example.investsmart.investment.*;
import com.example.investsmart.repository.*;
import com.example.investsmart.service.PortfolioService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

/** Initializes the shipping SQL then validates every JPA mapping; Hibernate never creates tables. */
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:fresh-v2-schema;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=file:deploy/mysql-init/001-app-schema.sql,file:deploy/mysql-init/002-security-schema.sql",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
    "investsmart.security.initialize-schema=false"
})
@ActiveProfiles("test")
class FreshSchemaTests {
    @Autowired UserRepository users;
    @Autowired StockRepository stocks;
    @Autowired HoldingRepository holdings;
    @Autowired OrderRepository orders;
    @Autowired WatchlistRepository watchlists;
    @Autowired MarketPriceHistoryRepository history;
    @Autowired SavedInterestRepository saved;
    @Autowired IpoApplicationRepository applications;
    @Autowired PortfolioService portfolio;
    @Autowired JdbcTemplate db;
    @Autowired EntityManager entities;
    @Autowired org.springframework.context.ApplicationContext context;

    @Autowired com.example.investsmart.settlement.SettlementJournalService journalReport;
    @Autowired com.example.investsmart.settlement.SettlementFundingService fundingReport;
    @Autowired com.example.investsmart.settlement.FinanceEventService financeReport;

    @Test void ownedFinanceReaderUsesShippingSchemaAndRealJpaIdentity() {
        var owner = user();
        String position = UUID.randomUUID().toString(), key = position + ":INTEREST";
        var at = java.sql.Timestamp.from(Instant.parse("2026-11-24T05:00:00Z"));
        try {
            db.update("""
                    INSERT INTO paper_financing(id,user_id,stock_id,product,quantity,cost_amount,principal,
                    interest,interest_rate,created_at,last_accrued_date) VALUES(?,?,1,'MTF',0,100,75,9,0.1,?,?)
                    """, position, owner.getId(), at, java.sql.Date.valueOf("2026-11-24"));
            db.update("INSERT INTO paper_finance_events VALUES(?,?,?,'INTEREST',0,2.50,?)", key, position, owner.getId(), at);
            var result = financeReport.read(owner.getEmail(), position, null, null);
            assertEquals(new BigDecimal("2.50"), result.entries().getFirst().interpretation().interestAccrued());
            assertEquals(new BigDecimal("9.00"), db.queryForObject("SELECT interest FROM paper_financing WHERE id=?", BigDecimal.class, position));
            assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM settlement_accounts WHERE user_id=?", Integer.class, owner.getId()));
        } finally {
            db.update("DELETE FROM paper_finance_events WHERE user_id=?", owner.getId());
            db.update("DELETE FROM paper_financing WHERE user_id=?", owner.getId());
            users.deleteById(owner.getId());
        }
    }

    @Test void internalJournalUsesRealJpaIdentityWithoutInitializingAccount() {
        var owner=user();
        try {
            var empty=journalReport.read(owner.getEmail(),null,null);
            assertFalse(empty.coverage().initialized()); assertTrue(empty.entries().isEmpty());
            var agentPortfolio=new com.example.investsmart.chat.agent.PortfolioReadTool(users,portfolio,stocks,
                    new com.example.investsmart.chat.agent.ToolRegistry(java.util.List.of(
                            new com.example.investsmart.chat.agent.ToolRegistry.Definition("getMyPortfolio",java.util.Set.of()))),
                    java.time.Clock.fixed(Instant.parse("2026-11-23T05:00:00Z"),ZoneOffset.UTC));
            assertTrue(agentPortfolio.invoke(()->owner.getEmail(),java.util.Map.of(),true).holdings().isEmpty());
            var at=java.sql.Timestamp.from(Instant.parse("2026-11-23T05:00:00Z"));
            db.update("INSERT INTO settlement_journal(user_id,event_key,action,fingerprint,amount,occurred_at) VALUES(?,?,?,?,?,?)",
                    owner.getId(),"synthetic-report","FUTURE_ACTION","a".repeat(64),new BigDecimal("12.34"),at);
            var result=journalReport.read(owner.getEmail().toUpperCase(java.util.Locale.ROOT),0,50);
            assertFalse(result.coverage().initialized());
            assertEquals(new BigDecimal("12.34"),result.entries().getFirst().amount());
            var funding=fundingReport.read(owner.getEmail(),0,50);
            assertFalse(funding.coverage().initialized());
            assertEquals(com.example.investsmart.dto.SettlementFundingPage.Verification.NOT_EVALUATED,funding.entries().getFirst().verification());
            assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM settlement_accounts WHERE user_id=?",Integer.class,owner.getId()));
            owner.setAccountStatus("SUSPENDED"); users.saveAndFlush(owner);
            assertThrows(org.springframework.web.server.ResponseStatusException.class,()->journalReport.read(owner.getEmail(),0,50));
            assertThrows(org.springframework.web.server.ResponseStatusException.class,()->fundingReport.read(owner.getEmail(),0,50));
        } finally {
            db.update("DELETE FROM settlement_journal WHERE user_id=?",owner.getId());
            users.deleteById(owner.getId());
        }
    }

    @Test void deferredLearningHasNoControllerServiceOrRegisteredEndpointByDefault(){
        assertTrue(context.getBeansOfType(com.example.investsmart.chat.agent.AgentPortfolioController.class).isEmpty());
        assertTrue(context.getBeansOfType(com.example.investsmart.chat.agent.PortfolioAnalysisService.class).isEmpty());
        assertTrue(context.getBeansOfType(com.example.investsmart.chat.agent.PortfolioReadTool.class).isEmpty());
        assertTrue(context.getBeansOfType(com.example.investsmart.chat.agent.AgentFoundation.class).isEmpty());
        assertTrue(context.getBeansOfType(com.example.investsmart.chat.agent.AiProvider.class).isEmpty());
        assertTrue(context.getBeansOfType(com.example.investsmart.chat.agent.ToolRegistry.class).isEmpty());
        assertTrue(context.getBeansOfType(com.example.investsmart.chat.ChatController.class).isEmpty());
        assertTrue(context.getBeansOfType(com.example.investsmart.chat.LearningAssistant.class).isEmpty());
        var mappings=(org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping)context.getBean("requestMappingHandlerMapping");
        assertTrue(mappings.getHandlerMethods().keySet().stream().noneMatch(mapping->mapping.getPatternValues().contains("/learning/chat")));
    }

    User user() {
        var user=new User();user.setEmail("fresh-schema-"+UUID.randomUUID()+"@example.invalid");
        user.setName("Schema fixture");user.setRole("USER");user.setEmailVerified(true);
        user.setVirtualBalance(new BigDecimal("1000"));return users.saveAndFlush(user);
    }

    @Test @Transactional void shippingSchemaSupportsEveryPersistentEntityAndPortfolioAccounting() {
        var user=user();var stock=new Stock();stock.setSymbol("SCHEMA");stock.setInstrumentKey("NSE_EQ|SCHEMA");
        stock.setExchange("NSE");stock.setIsin("SCHEMA");stock.setCurrentPrice(new BigDecimal("12.50"));
        stock.setMarketDataUpdatedAt(LocalDateTime.of(2026,11,3,12,0));stock=stocks.saveAndFlush(stock);
        var holding=new Holding();holding.setUser(user);holding.setStock(stock);holding.setQuantity(2);holdings.saveAndFlush(holding);
        var order=new Order();order.setUser(user);order.setStock(stock);order.setOrderType(OrderType.BUY);
        order.setStatus(OrderStatus.COMPLETED);order.setQuantity(2);order.setPrice(new BigDecimal("10"));
        order.setTotalAmount(new BigDecimal("20"));orders.saveAndFlush(order);
        var watchlist=new Watchlist();watchlist.setUser(user);watchlist.setStock(stock);watchlists.saveAndFlush(watchlist);
        var candle=new MarketPriceHistory();candle.setSymbol(stock.getInstrumentKey());candle.setOpen(new BigDecimal("12"));
        candle.setHigh(new BigDecimal("13"));candle.setLow(new BigDecimal("11"));candle.setPrice(new BigDecimal("12.50"));
        candle.setVolume(new BigDecimal("1000"));candle.setTimestamp(LocalDateTime.of(2026,11,3,12,0));history.saveAndFlush(candle);
        var interest=new SavedInterest();interest.setUserId(user.getId());interest.setKind("BOND");interest.setReferenceId("fixture");
        interest.setName("Schema fixture");interest.setSavedAt(Instant.parse("2026-11-03T12:00:00Z"));saved.saveAndFlush(interest);
        var application=new IpoApplication();application.setUserId(user.getId());application.setIpoId("schema-ipo");
        application.setStatus("CANCELLED");application.setReservedAmount(BigDecimal.ZERO);application.setAllocatedCost(BigDecimal.ZERO);
        application.setDraw(new BigDecimal("0.123456789123456789"));application.setAppliedAt(Instant.parse("2026-11-03T12:00:00Z"));
        application.setAllotmentDate(LocalDate.of(2026,11,5));applications.saveAndFlush(application);
        entities.clear();
        var response=portfolio.getPortfolio(user.getId());
        assertEquals(0,response.getTotalValue().compareTo(new BigDecimal("1025")));
        assertEquals(0,response.getInvestedAmount().compareTo(new BigDecimal("20")));
        assertEquals(0,response.getUnrealizedPnl().compareTo(new BigDecimal("5")));
        assertEquals(application.getDraw(),applications.findById(application.getId()).orElseThrow().getDraw());
        assertEquals(Instant.parse("2026-11-03T12:00:00Z"),saved.findById(interest.getId()).orElseThrow().getSavedAt());
        assertEquals(1,db.queryForObject("SELECT id FROM security_mutex WHERE id=1",Integer.class));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM owner_admin",Integer.class));
    }

    @Test @Transactional void freshSchemaRejectsDuplicateNormalizedAccountEmails() {
        var user=user();
        assertThrows(DataIntegrityViolationException.class,()->db.update("INSERT INTO users(email,email_verified) VALUES(?,FALSE)",user.getEmail()));
    }

    @Test void legacyStockInstrumentMigrationPreservesRowsAndEnforcesListingUniqueness() throws Exception {
        try(var connection=java.sql.DriverManager.getConnection("jdbc:h2:mem:instrument-upgrade-"+UUID.randomUUID()+";MODE=MySQL")) {
            var statement=connection.createStatement();
            statement.execute("CREATE TABLE stocks(id BIGINT PRIMARY KEY,symbol VARCHAR(255))");
            statement.execute("INSERT INTO stocks VALUES(1,'INFY'),(2,'TCS')");
            new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("db/migrations/20261110_stock_instrument_key.sql")).populate(connection);
            var rows=statement.executeQuery("SELECT COUNT(*) FROM stocks WHERE instrument_key IS NULL");
            assertTrue(rows.next());assertEquals(2,rows.getInt(1));
            statement.execute("UPDATE stocks SET instrument_key='NSE_EQ|fixture' WHERE id=1");
            assertThrows(java.sql.SQLException.class,()->statement.execute("UPDATE stocks SET instrument_key='NSE_EQ|fixture' WHERE id=2"));
        }
    }

    @Test void freshSecurityBaselineMatchesRuntimeSecuritySchema() throws Exception {
        assertEquals(Files.readString(Path.of("src/main/resources/db/security-schema.sql")).replace("\r\n","\n"),
            Files.readString(Path.of("deploy/mysql-init/002-security-schema.sql")).replace("\r\n","\n"));
    }
}
