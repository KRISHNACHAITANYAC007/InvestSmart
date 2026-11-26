package com.example.investsmart.chat.agent;

import com.example.investsmart.dto.*;
import com.example.investsmart.entity.*;
import com.example.investsmart.repository.*;
import com.example.investsmart.service.PortfolioService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PortfolioReadToolTests {
    UserRepository users=mock(UserRepository.class);
    StockRepository stocks=mock(StockRepository.class);
    PortfolioService portfolios=mock(PortfolioService.class);
    Instant now=Instant.parse("2026-11-23T05:00:00Z");
    User owner=new User(); PortfolioResponse response=new PortfolioResponse();
    Stock stock=new Stock(); HoldingResponse holding=new HoldingResponse();
    PortfolioReadTool tool;
    @BeforeEach void setup() {
        owner.setEmail("owner@example.invalid");when(users.findByEmailIgnoreCase(owner.getEmail())).thenReturn(Optional.of(owner));
        stock.setId(1L);stock.setInstrumentKey("NSE_EQ|SYNTHETIC");stock.setCurrentPrice(new BigDecimal("10.00"));
        stock.setMarketDataUpdatedAt(now.atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
        holding.setStockId(1L);holding.setInstrumentKey(stock.getInstrumentKey());holding.setSymbol("SYNTHETIC");holding.setQuantity(2);
        holding.setCurrentPrice(new BigDecimal("10.00"));holding.setCurrentValue(new BigDecimal("20.00"));holding.setUnrealizedPnl(new BigDecimal("-3.00"));
        response.setHoldings(List.of(holding));response.setPerformanceAvailable(true);
        when(portfolios.getPortfolioByEmail(owner.getEmail())).thenReturn(response);
        when(stocks.findAllById(any())).thenReturn(List.of(stock));
        tool=new PortfolioReadTool(users,portfolios,stocks,new ToolRegistry(List.of(new ToolRegistry.Definition("getMyPortfolio",Set.of()))),Clock.fixed(now,ZoneOffset.UTC));
    }
    PortfolioReadTool.Evidence read(){return tool.invoke(()->owner.getEmail(),Map.of(),true);}
    @Test void retrievesOnlyServerUserAndLabelsScopeAndTimestamps() {
        var r=read();assertEquals("EQUITY_HOLDINGS_ONLY",r.scope());assertEquals(now,r.holdings().getFirst().quoteAt());
        assertEquals(new BigDecimal("20.00"),r.equityValue());assertEquals(new BigDecimal("100.00"),r.holdings().getFirst().equityWeightPercent());
        assertEquals(new BigDecimal("-3.00"),r.holdings().getFirst().unrealizedPnl());
        verify(portfolios).getPortfolioByEmail(owner.getEmail());verifyNoMoreInteractions(portfolios);
        assertThrows(UnsupportedOperationException.class,()->r.holdings().clear());
    }
    @Test void identityArgumentsAndMissingConsentCannotReadPortfolio() {
        assertThrows(IllegalArgumentException.class,()->tool.invoke(()->owner.getEmail(),Map.of("userId","other"),true));
        assertThrows(ResponseStatusException.class,()->tool.invoke(()->owner.getEmail(),Map.of(),false));verifyNoInteractions(portfolios,stocks);
    }
    @Test void absentUnknownSuspendedIdentityCannotReadPortfolio() {
        assertThrows(ResponseStatusException.class,()->tool.invoke(null,Map.of(),true));
        when(users.findByEmailIgnoreCase("other")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,()->tool.invoke(()->"other",Map.of(),true));
        owner.setAccountStatus("SUSPENDED");assertThrows(ResponseStatusException.class,()->read());verifyNoInteractions(portfolios,stocks);
    }
    @Test void staleMissingAndFutureTimestampSuppressValuationAndPnl() {
        for(var time:Arrays.asList(now.minusSeconds(61),now.plusSeconds(6),null)) {
            stock.setMarketDataUpdatedAt(time==null?null:time.atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
            var r=read();assertNull(r.equityValue());assertNull(r.holdings().getFirst().value());assertNull(r.holdings().getFirst().unrealizedPnl());assertNull(r.holdings().getFirst().equityWeightPercent());
        }
    }
    @Test void mismatchedListingOrSavedValueCannotProduceNumbers() {
        holding.setInstrumentKey("BSE_EQ|OTHER");assertNull(read().equityValue());
        holding.setInstrumentKey(stock.getInstrumentKey());holding.setCurrentValue(new BigDecimal("21.00"));assertNull(read().equityValue());
        when(stocks.findAllById(any())).thenReturn(List.of());assertNull(read().equityValue());
    }
    @Test void incompleteCostHistoryKeepsPnlUnavailableAndEmptyPortfolioIsNotAnError() {
        response.setPerformanceAvailable(false);assertNull(read().holdings().getFirst().unrealizedPnl());
        response.setHoldings(List.of());assertEquals(new BigDecimal("0.00"),read().equityValue());assertTrue(read().holdings().isEmpty());
    }
    @Test void resultBoundRejectsOversizedPortfolio() {
        response.setHoldings(Collections.nCopies(1001,holding));assertThrows(ResponseStatusException.class,()->read());verifyNoInteractions(stocks);
    }
    @Test void equityWeightsExcludeCashAndIncompletePricesCannotInflateKnownWeights() {
        var second=new HoldingResponse();second.setStockId(2L);second.setInstrumentKey("NSE_EQ|SECOND");second.setQuantity(8);
        second.setCurrentPrice(new BigDecimal("10.00"));second.setCurrentValue(new BigDecimal("80.00"));
        var secondStock=new Stock();secondStock.setId(2L);secondStock.setInstrumentKey(second.getInstrumentKey());
        secondStock.setCurrentPrice(new BigDecimal("10.00"));secondStock.setMarketDataUpdatedAt(stock.getMarketDataUpdatedAt());
        response.setHoldings(List.of(holding,second));response.setBalance(new BigDecimal("900.00"));response.setTotalValue(new BigDecimal("1000.00"));
        when(stocks.findAllById(any())).thenReturn(List.of(stock,secondStock));
        var complete=read();assertEquals(new BigDecimal("20.00"),complete.holdings().getFirst().equityWeightPercent());
        assertEquals(new BigDecimal("80.00"),complete.holdings().get(1).equityWeightPercent());
        secondStock.setMarketDataUpdatedAt(null);
        var partial=read();assertNull(partial.equityValue());assertNull(partial.holdings().getFirst().equityWeightPercent());
        assertEquals(new BigDecimal("20.00"),partial.holdings().getFirst().value());assertNull(partial.holdings().get(1).value());
    }
}
