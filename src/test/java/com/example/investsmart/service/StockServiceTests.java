package com.example.investsmart.service;

import com.example.investsmart.entity.Stock;
import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.market.MarketDataService;
import com.example.investsmart.market.MarketInstrument;
import com.example.investsmart.market.MarketInstrumentSearchResult;
import com.example.investsmart.market.MarketQuote;
import com.example.investsmart.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTests {

    private static final String SYMBOL = "INFY";
    private static final String INSTRUMENT_KEY = "NSE_EQ|INE009A01021";

    @Mock
    private StockRepository stockRepository;

    @Mock
    private MarketDataService marketDataService;

    @Mock private com.example.investsmart.market.EquityCatalogService catalog;

    @InjectMocks
    private StockService stockService;

    @Test
    void importsFoundNseInstrumentWithItsLiveQuote() {
        MarketInstrument instrument = instrument();
        when(catalog.resolve(INSTRUMENT_KEY)).thenReturn(instrument);
        when(marketDataService.getQuote(instrument)).thenReturn(quote());
        when(stockRepository.findByInstrumentKey(INSTRUMENT_KEY))
                .thenReturn(Optional.empty());
        when(stockRepository.findBySymbolIgnoreCaseAndInstrumentKeyIsNull(SYMBOL))
                .thenReturn(Optional.empty());
        when(stockRepository.save(org.mockito.ArgumentMatchers.any(Stock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Stock imported = stockService.importNseStock(INSTRUMENT_KEY, SYMBOL);

        assertEquals(INSTRUMENT_KEY, imported.getInstrumentKey());
        assertEquals(SYMBOL, imported.getSymbol());
        assertEquals("Infosys Limited", imported.getCompanyName());
        assertEquals(0, imported.getCurrentPrice().compareTo(new BigDecimal("1500.00")));
        assertEquals(0, imported.getDayHigh().compareTo(new BigDecimal("1520.00")));
        assertEquals(0, imported.getDayLow().compareTo(new BigDecimal("1490.00")));
        verify(stockRepository).save(imported);
    }

    @Test
    void importingSameInstrumentUpdatesExistingStockInsteadOfDuplicating() {
        MarketInstrument instrument = instrument();
        Stock existing = new Stock();
        existing.setId(7L);
        existing.setSymbol(SYMBOL);
        existing.setInstrumentKey(INSTRUMENT_KEY);
        when(catalog.resolve(INSTRUMENT_KEY)).thenReturn(instrument);
        when(marketDataService.getQuote(instrument)).thenReturn(quote());
        when(stockRepository.findByInstrumentKey(INSTRUMENT_KEY))
                .thenReturn(Optional.of(existing));
        when(stockRepository.save(existing)).thenReturn(existing);

        Stock imported = stockService.importNseStock(INSTRUMENT_KEY, SYMBOL);

        assertSame(existing, imported);
        assertEquals(7L, imported.getId());
        verify(stockRepository, never()).findBySymbolIgnoreCaseAndInstrumentKeyIsNull(SYMBOL);
        verify(stockRepository).save(existing);
    }

    @Test
    void savesValidatedInstrumentWhenQuoteIsUnavailable() {
        MarketInstrument instrument = instrument();
        when(catalog.resolve(INSTRUMENT_KEY)).thenReturn(instrument);
        when(marketDataService.getQuote(instrument)).thenThrow(new RuntimeException("provider unavailable"));
        when(stockRepository.findByInstrumentKey(INSTRUMENT_KEY)).thenReturn(Optional.empty());
        when(stockRepository.findBySymbolIgnoreCaseAndInstrumentKeyIsNull(SYMBOL)).thenReturn(Optional.empty());
        when(stockRepository.save(org.mockito.ArgumentMatchers.any(Stock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Stock imported = stockService.importNseStock(INSTRUMENT_KEY, SYMBOL);

        assertEquals(SYMBOL, imported.getSymbol());
        assertEquals("Infosys Limited", imported.getCompanyName());
        assertNull(imported.getCurrentPrice());
        assertNull(imported.getPreviousClose());
        verify(stockRepository).save(imported);
    }

    @Test
    void rejectsInstrumentThatWasNotReturnedByNseSearch() {
        when(catalog.resolve(INSTRUMENT_KEY)).thenThrow(new BadRequestException("Listing not found"));

        assertThrows(
                BadRequestException.class,
                () -> stockService.importNseStock(INSTRUMENT_KEY, SYMBOL)
        );
        verify(marketDataService, never()).getQuote(org.mockito.ArgumentMatchers.any(MarketInstrument.class));
        verify(stockRepository, never()).save(org.mockito.ArgumentMatchers.any(Stock.class));
    }

    private static MarketInstrument instrument() {
        return new MarketInstrument(
                INSTRUMENT_KEY,
                SYMBOL,
                "Infosys Limited",
                "NSE"
        );
    }

    private static MarketInstrumentSearchResult searchResult(
            MarketInstrument instrument) {
        return new MarketInstrumentSearchResult(List.of(instrument), 1, 1);
    }

    private static MarketQuote quote() {
        return new MarketQuote(
                SYMBOL,
                new BigDecimal("1500.00"),
                new BigDecimal("1495.00"),
                new BigDecimal("1520.00"),
                new BigDecimal("1490.00"),
                LocalDateTime.now()
        );
    }
}
