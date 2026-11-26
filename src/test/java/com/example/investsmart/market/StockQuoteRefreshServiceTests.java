package com.example.investsmart.market;

import com.example.investsmart.entity.Stock;
import com.example.investsmart.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockQuoteRefreshServiceTests {
    @Mock private StockRepository stockRepository;
    @Mock private MarketDataService marketDataService;
    @InjectMocks private StockQuoteRefreshService refreshService;

    @Test
    void forceRefreshStoresLatestQuoteAndTimestamp() {
        Stock stock = stock("INFY", "NSE_EQ|INE009A01021", new BigDecimal("1400"));
        when(stockRepository.findAll()).thenReturn(List.of(stock));
        when(marketDataService.getQuotes(anyList())).thenReturn(List.of(
                new MarketQuote("INFY", new BigDecimal("1500.25"), new BigDecimal("1490.00"),
                        new BigDecimal("1510.00"), new BigDecimal("1485.00"), LocalDateTime.now(ZoneId.of("Asia/Kolkata")))
        ));

        QuoteRefreshResponse response = refreshService.refreshAll(true);

        assertEquals(1, response.getUpdatedCount());
        assertTrue(response.getStaleSymbols().isEmpty());
        assertEquals(0, stock.getCurrentPrice().compareTo(new BigDecimal("1500.25")));
        assertEquals(0, stock.getPreviousClose().compareTo(new BigDecimal("1490.00")));
        assertTrue(stock.getMarketDataUpdatedAt() != null);
        verify(stockRepository).saveAll(List.of(stock));
    }

    @Test
    void skipsQuotesUpdatedWithinTheCacheWindow() {
        Stock stock = stock("INFY", "NSE_EQ|INE009A01021", new BigDecimal("1400"));
        stock.setMarketDataUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        when(stockRepository.findAll()).thenReturn(List.of(stock));

        QuoteRefreshResponse response = refreshService.refreshAll(false);

        assertEquals(0, response.getUpdatedCount());
        verify(marketDataService, never()).getQuotes(anyList());
    }

    @Test
    void preservesSavedPriceAndReportsSymbolWhenProviderOmitsIt() {
        Stock stock = stock("INFY", "NSE_EQ|INE009A01021", new BigDecimal("1400"));
        when(stockRepository.findAll()).thenReturn(List.of(stock));
        when(marketDataService.getQuotes(anyList())).thenReturn(List.of());

        QuoteRefreshResponse response = refreshService.refreshAll(true);

        assertEquals(0, response.getUpdatedCount());
        assertEquals(List.of("INFY"), response.getStaleSymbols());
        assertEquals(0, stock.getCurrentPrice().compareTo(new BigDecimal("1400")));
    }

    private Stock stock(String symbol, String key, BigDecimal price) {
        Stock stock = new Stock();
        stock.setSymbol(symbol);
        stock.setInstrumentKey(key);
        stock.setCompanyName(symbol);
        stock.setCurrentPrice(price);
        return stock;
    }
}
