package com.example.investsmart.market;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class MarketSnapshotServiceTests {
    @Test
    void pagesAndSearchesCatalogFiltersNonEquitiesAndCachesMetadataAndQuotes() throws Exception {
        MarketDataProvider provider = mock(MarketDataProvider.class);
        when(provider.getQuotes(anyList())).thenAnswer(call -> ((List<MarketInstrument>)call.getArgument(0)).stream()
                .map(i -> new MarketQuote(i.getSymbol(), BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now())).toList());
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < 55; i++) {
            if (i > 0) json.append(',');
            json.append("{\"segment\":\"NSE_EQ\",\"instrument_type\":\"EQ\",\"trading_symbol\":\"STOCK%02d\",\"name\":\"Company %02d\",\"instrument_key\":\"NSE_EQ|INE%02d\"}".formatted(i,i,i));
        }
        json.append(",{\"segment\":\"NSE_FO\",\"instrument_type\":\"FUT\",\"trading_symbol\":\"FUTURE\"},");
        json.append("{\"segment\":\"NSE_EQ\",\"instrument_type\":\"EQ\",\"trading_symbol\":\"INFY\",\"name\":\"Infosys\",\"instrument_key\":\"NSE_EQ|INE009A01021\"}]");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) { gzip.write(json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
        AtomicInteger downloads = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/catalog", exchange -> {
            downloads.incrementAndGet(); byte[] body = bytes.toByteArray();
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        }); server.start();
        try {
            MarketSnapshotService service = new MarketSnapshotService(provider, HttpClient.newHttpClient(),
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/catalog");
            var first = service.heatmap("all", "", 1);
            assertEquals(56, first.totalItems()); assertEquals(50, first.items().size());
            assertFalse(first.stale()); assertEquals(2, first.totalPages());
            service.heatmap("all", "", 1);
            verify(provider, times(1)).getQuotes(anyList());
            assertEquals(6, service.heatmap("all", "", 2).items().size());
            assertEquals(1, service.heatmap("all", "Company 54", 1).totalItems());
            var nifty = service.heatmap("nifty50", "", 1);
            assertEquals("INFY", nifty.items().getFirst().symbol());
            assertEquals("2026-11-02", nifty.constituentAsOf());
            assertEquals(1, downloads.get());
        } finally { server.stop(0); }
    }

    @Test
    void featuredStocksKeepNamesAndUnavailableRowsWhenProviderFails() {
        MarketDataProvider provider = mock(MarketDataProvider.class);
        when(provider.getQuotes(anyList())).thenThrow(new IllegalStateException("offline"));
        var result = new MarketSnapshotService(provider).featured();
        assertEquals(10, result.items().size()); assertTrue(result.stale());
        assertEquals("Reliance Industries", result.items().getFirst().companyName());
        assertNull(result.items().getFirst().currentPrice());
        assertNull(result.updatedAt());
    }

    @Test
    void indexFailuresKeepFourNamedUnavailableTilesAndRejectInvalidGroups() {
        MarketDataProvider provider = mock(MarketDataProvider.class);
        when(provider.getIndices()).thenThrow(new IllegalStateException("offline"));
        var service = new MarketSnapshotService(provider);
        var result = service.heatmap("indices", "", 1);
        assertEquals(4, result.items().size()); assertTrue(result.stale());
        assertTrue(result.items().stream().allMatch(i -> i.currentPrice() == null));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.heatmap("other", "", 1));
    }
}
