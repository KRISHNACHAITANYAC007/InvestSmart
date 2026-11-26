package com.example.investsmart.market;

import com.example.investsmart.service.NseMarketSessionService;
import com.example.investsmart.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in, read-only provider checks. Uses configured local credentials but never the local database. */
@EnabledIfSystemProperty(named="investsmart.live-check",matches="true")
@ActiveProfiles("local")
@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:investsmart-live-check;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false", "investsmart.ipo.scheduler.enabled=false"})
class LiveMarketDataTests {
    @Autowired MarketDataService market;
    @Autowired EquityCatalogService catalog;
    @Autowired NseMarketSessionService sessions;
    @Autowired com.example.investsmart.service.StockService stocks;
    @Autowired StockQuoteRefreshService refresh;
    @Autowired com.example.investsmart.repository.StockRepository stockRepository;
    @Autowired org.springframework.core.env.Environment environment;
    @Autowired com.example.investsmart.investment.IpoMarketService ipos;
    @Autowired com.example.investsmart.investment.BondMarketService bonds;
    @Autowired MarketSnapshotService snapshots;

    @Test void ipoDiscoveryWorksForEveryLifecycleStage() {
        for(String status:List.of("open","upcoming","closed","listed")){
            var page=ipos.list(status,"all",1);assertNotNull(page.updatedAt());
            if(!page.items().isEmpty()) {var detail=ipos.detail(page.items().getFirst().id(),true);assertFalse(detail.id().isBlank());assertFalse(detail.stale());}
        }
    }
    @Test void debtCatalogIncludesGovernmentAndCorporateDiscovery() {
        assertTrue(bonds.list("government","",1).totalItems()>0);assertTrue(bonds.list("corporate","",1).totalItems()>0);
        var reference=bonds.detail("NSE_EQ|INE906B07CB9");assertNotNull(reference.bond().terms());
    }
    @Test void fiftyNiftyConstituentsFitOneDataPage() {
        var page=snapshots.heatmap("nifty50","",1);assertEquals(50,page.items().size());assertEquals(1,page.totalPages());
        assertTrue(page.items().stream().allMatch(q->q.currentPrice()!=null&&q.currentPrice().signum()>0),"Some NIFTY quote prices unavailable");
    }

    @Test void configuredAccountEnablesNseAndBse() throws Exception {
        // Inspect capability flags only. Never log or expose the personal profile or bearer token.
        var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(
                environment.getProperty("upstox.api.base-url","https://api.upstox.com")+"/v2/user/profile"))
                .timeout(java.time.Duration.ofSeconds(10)).header("Accept","application/json")
                .header("Authorization","Bearer "+environment.getProperty("upstox.api.token","")).GET().build();
        var client=java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
        var response=client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode(),"Account capability lookup did not succeed");
        var root=new tools.jackson.databind.ObjectMapper().readTree(response.body());
        assertEquals("success",root.path("status").asText());
        assertTrue(root.path("data").path("is_active").asBoolean(),"Configured provider account is inactive");
        var exchanges=new java.util.HashSet<String>();
        root.path("data").path("exchanges").forEach(exchange->exchanges.add(exchange.asText()));
        assertTrue(exchanges.contains("NSE")&&exchanges.contains("BSE"),"Both equity exchanges must be enabled");
    }

    @Test void indicesAndBothInfosysListingsHaveLivePrices() {
        for(var quote:market.getIndices()) {
            assertTrue(quote.getCurrentPrice()!=null&&quote.getCurrentPrice().signum()>0,"Index quote unavailable: "+quote.getSymbol());
            assertTrue(quote.getPreviousClose()!=null&&quote.getPreviousClose().signum()>0,"Previous close unavailable: "+quote.getSymbol());
        }
        var listings=catalog.listings("NSE_EQ|INE009A01021","INFY");
        assertTrue(listings.stream().anyMatch(i->i.getExchange().equals("BSE")),"BSE equity metadata unavailable");
        for(var listing:listings){var quote=market.getQuote(listing);assertEquals(listing.getInstrumentKey(),quote.getInstrumentKey());assertTrue(quote.getCurrentPrice()!=null&&quote.getCurrentPrice().signum()>0,"Listing quote unavailable: "+listing.getExchange());}
    }

    @Test void refreshPersistsBothListingsWithoutMixingThem() {
        var listings=catalog.listings("NSE_EQ|INE009A01021","INFY");
        for(var listing:listings) stocks.importNseStock(listing.getInstrumentKey(),listing.getSymbol());
        var response=refresh.refreshAll(true);
        assertTrue(response.getStaleSymbols().isEmpty(),"Some live listing quotes could not be refreshed");
        for(var listing:listings){var stock=stockRepository.findByInstrumentKey(listing.getInstrumentKey()).orElseThrow();assertEquals(listing.getExchange(),stock.getExchange());assertNotNull(stock.getMarketDataUpdatedAt());assertTrue(stock.getCurrentPrice().signum()>0);}
        assertEquals(0,refresh.refreshAll(false).getUpdatedCount());
    }

    @Test void niftyAndSensexHaveEverySupportedHistoryRange() {
        for(String symbol:List.of("NIFTY 50","SENSEX")) for(String period:List.of("1D","1W","1M","6M","1Y")) {
            var history=market.getHistory(symbol,period);
            assertFalse(history.isEmpty(),"No candles for "+symbol+" "+period);
            assertTrue(history.stream().allMatch(c->c.getTimestamp()!=null&&c.getPrice()!=null&&c.getPrice().signum()>0),"Invalid candles for "+symbol+" "+period);
        }
    }

    @Test void bothExchangesReportStatusAndSessionGuardMatchesIt() {
        for(String exchange:List.of("NSE","BSE")){
            String status=market.getExchangeStatus(exchange);assertNotNull(status);assertFalse(status.isBlank());
            if("NORMAL_OPEN".equalsIgnoreCase(status)) assertDoesNotThrow(()->{sessions.requireOpenSession(exchange);});
            else assertThrows(BadRequestException.class,()->sessions.requireOpenSession(exchange));
        }
    }
}
