package com.example.investsmart.market;

import com.example.investsmart.repository.MarketPriceHistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketDataServiceHeatmapTests {

    @Test
    void requestsTheTenFeaturedNseEquitiesInStableOrder() {
        MarketDataProvider provider = mock(MarketDataProvider.class);
        MarketPriceHistoryRepository historyRepository = mock(MarketPriceHistoryRepository.class);
        when(provider.getQuotes(anyList())).thenReturn(List.of());
        MarketDataService service = new MarketDataService(provider, historyRepository);

        service.getHeatmapQuotes();

        ArgumentCaptor<List<MarketInstrument>> instruments = ArgumentCaptor.forClass(List.class);
        verify(provider).getQuotes(instruments.capture());
        assertEquals(List.of("RELIANCE", "HDFCBANK", "ICICIBANK", "INFY", "TCS",
                        "BHARTIARTL", "SBIN", "ITC", "LT", "BAJFINANCE"),
                instruments.getValue().stream().map(MarketInstrument::getSymbol).toList());
        assertEquals(10, instruments.getValue().stream()
                .map(MarketInstrument::getInstrumentKey).distinct().count());
    }
}
