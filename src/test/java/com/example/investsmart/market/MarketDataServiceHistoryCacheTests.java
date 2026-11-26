package com.example.investsmart.market;

import com.example.investsmart.entity.MarketPriceHistory;
import com.example.investsmart.repository.MarketPriceHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceHistoryCacheTests {
    @Mock private MarketDataProvider provider;
    @Mock private MarketPriceHistoryRepository repository;
    @InjectMocks private MarketDataService service;

    @Test
    void intradayPeriodsBypassDailyCache() {
        when(provider.getHistory("INFY", "1W")).thenReturn(List.of());

        service.getHistory("INFY", "1W");

        verify(provider).getHistory("INFY", "1W");
        verify(repository, never()).findBySymbolAndTimestampBetweenOrderByTimestampAsc(
                any(), any(), any());
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void fullRefreshPreservesOlderCachedCandles() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        List<MarketPriceHistory> stale = List.of(point(today.minusDays(2)));
        List<MarketPriceHistory> refreshed = List.of(
                point(today.minusDays(34)), point(today.minusDays(1)));
        when(repository.findBySymbolAndTimestampBetweenOrderByTimestampAsc(any(), any(), any()))
                .thenReturn(stale);
        when(repository.findBySymbolOrderByTimestampAsc("INFY")).thenReturn(stale);
        when(provider.getHistory("INFY", "1M")).thenReturn(refreshed);
        when(repository.saveAll(refreshed)).thenReturn(refreshed);

        assertEquals(refreshed, service.getHistory("INFY", "1M"));

        verify(repository,never()).deleteAll(anyList());
        verify(repository).saveAll(refreshed);
    }

    @Test
    void partialRefreshDoesNotDeleteOrPersistOverExistingCache() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        List<MarketPriceHistory> stale = List.of(point(today.minusDays(2)));
        List<MarketPriceHistory> partial = List.of(point(today.minusDays(10)));
        when(repository.findBySymbolAndTimestampBetweenOrderByTimestampAsc(any(), any(), any()))
                .thenReturn(stale);
        when(provider.getHistory("INFY", "1M")).thenReturn(partial);

        assertEquals(partial, service.getHistory("INFY", "1M"));

        verify(repository, never()).deleteAll(anyList());
        verify(repository, never()).saveAll(anyList());
    }

    @Test void cachedDailyHistoryExpiresAndRefetchesInsteadOfFreezingForDays(){
        var time=new java.util.concurrent.atomic.AtomicReference<>(java.time.Instant.parse("2026-11-12T04:30:00Z"));
        var clock=new java.time.Clock(){public java.time.ZoneId getZone(){return java.time.ZoneOffset.UTC;} public java.time.Clock withZone(java.time.ZoneId zone){return java.time.Clock.fixed(time.get(),zone);} public java.time.Instant instant(){return time.get();}};
        var service=new MarketDataService(provider,repository,clock);
        var rows=List.of(point(LocalDate.of(2026,10,9)),point(LocalDate.of(2026,11,11)));
        when(repository.findBySymbolAndTimestampBetweenOrderByTimestampAsc(any(),any(),any())).thenReturn(rows);
        when(repository.findBySymbolOrderByTimestampAsc("INFY")).thenReturn(List.of());
        when(repository.saveAll(rows)).thenReturn(rows);when(provider.getHistory("INFY","1M")).thenReturn(rows);
        service.getHistory("INFY","1M");service.getHistory("INFY","1M");verify(provider).getHistory("INFY","1M");
        time.set(time.get().plusSeconds(61));service.getHistory("INFY","1M");verify(provider,org.mockito.Mockito.times(2)).getHistory("INFY","1M");
    }
    @Test void allHistoryCachesCompletedNewListingHistoryAndEmptyResults(){
        var recent=List.of(point(LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(1)));
        when(provider.getHistory("NEW","ALL")).thenReturn(recent);
        assertEquals(recent,service.getHistory("NEW","ALL"));assertEquals(recent,service.getHistory("NEW","ALL"));verify(provider).getHistory("NEW","ALL");
        when(provider.getHistory("EMPTY","ALL")).thenReturn(List.of());service.getHistory("EMPTY","ALL");service.getHistory("EMPTY","ALL");verify(provider).getHistory("EMPTY","ALL");
        verify(repository,never()).deleteAll(anyList());
    }
    @Test void failedLongHistoryIsNotCachedOrPublished(){
        when(provider.getHistory("INFY","ALL")).thenThrow(new RuntimeException("synthetic chunk failure")).thenReturn(List.of());
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,()->service.getHistory("INFY","ALL"));
        assertEquals(List.of(),service.getHistory("INFY","ALL"));verify(provider,org.mockito.Mockito.times(2)).getHistory("INFY","ALL");
    }
    private MarketPriceHistory point(LocalDate date) {
        MarketPriceHistory point = new MarketPriceHistory();
        point.setSymbol("INFY");
        point.setTimestamp(date.atTime(15, 30));
        point.setPrice(BigDecimal.TEN);
        return point;
    }

    @Test
    void bseHistoryCannotReadLegacyNseCache() {
        var bse = new MarketInstrument("BSE_EQ|500209", "INFY", "Infosys", "BSE", "INE009A01021");
        when(provider.getHistory(bse,"1M")).thenReturn(List.of());
        service.getHistory(bse,"1M");
        verify(repository).findBySymbolAndTimestampBetweenOrderByTimestampAsc(
                org.mockito.ArgumentMatchers.eq("BSE_EQ|500209"), any(), any());
        verify(repository,never()).findBySymbolAndTimestampBetweenOrderByTimestampAsc(
                org.mockito.ArgumentMatchers.eq("INFY"), any(), any());
    }
}
