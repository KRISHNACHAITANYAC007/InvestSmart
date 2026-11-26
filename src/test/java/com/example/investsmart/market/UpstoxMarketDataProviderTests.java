package com.example.investsmart.market;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

class UpstoxMarketDataProviderTests {
    @Test
    void sameTickerNseAndBseQuotesKeepSeparateIdentities() {
        quoteBody.set("""
                {"status":"success","data":{
                  "NSE_EQ:INFY":{"last_price":1500,"prev_close_price":1490},
                  "BSE_EQ:INFY":{"last_price":1501,"prev_close_price":1491,"instrument_token":"BSE_EQ|500209"}
                }}
                """);
        var bse = new MarketInstrument("BSE_EQ|500209", "INFY", "Infosys", "BSE", "INE009A01021");
        var quotes = provider.getQuotes(List.of(infy(), bse));
        assertEquals(2,quotes.size());
        assertEquals("NSE_EQ|INE009A01021",quotes.get(0).getInstrumentKey());
        assertEquals("BSE_EQ|500209",quotes.get(1).getInstrumentKey());
        assertEquals(0,quotes.get(1).getCurrentPrice().compareTo(new java.math.BigDecimal("1501")));
        assertEquals("BSE",provider.getQuote(bse).getExchange());
    }
    @Test void quoteAccessFailureIsSanitizedAndClassified(){
        firstStatus.set(401);laterStatus.set(401);
        var failure=assertThrows(com.example.investsmart.exception.MarketStatusUnavailableException.class,()->provider.getQuote(infy()));
        assertTrue(failure.getMessage().contains("credentials or permissions"));
        assertTrue(!failure.getMessage().contains("Bearer"));
    }
    @Test void rateLimitedQuoteRetainsSafeCooldown(){
        firstStatus.set(429);laterStatus.set(429);
        var failure=assertThrows(com.example.investsmart.exception.MarketStatusUnavailableException.class,()->provider.getQuote(infy()));
        assertEquals(60,failure.getRetryAfterSeconds());
        assertTrue(failure.getMessage().contains("rate limiting"));
    }
    @Test void telemetryFailureCannotDiscardSuccessfulQuoteOrMaskProviderRejection(){
        var health=org.mockito.Mockito.mock(com.example.investsmart.security.AdminHealthStore.class);
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic telemetry failure")).when(health).succeeded(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic telemetry failure")).when(health).failed(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyString());
        var tracked=new UpstoxMarketDataProvider("synthetic", "http://127.0.0.1:"+server.getAddress().getPort(),health);
        assertTrue(tracked.getQuote(infy()).getCurrentPrice().signum()>0);
        firstStatus.set(401);laterStatus.set(401);
        var rejected=assertThrows(com.example.investsmart.exception.MarketStatusUnavailableException.class,()->tracked.getQuote(infy()));
        assertTrue(rejected.getMessage().contains("credentials or permissions"));
    }
    @Test void fiveYearRangeUsesDailyCandles(){
        provider.getHistory(infy(),"5Y");var today=LocalDate.now(ZoneId.of("Asia/Kolkata"));
        assertTrue(candlePath.get().endsWith("/days/1/"+today+"/"+today.minusYears(5)));assertEquals(1,candlePaths.size());
    }
    @Test void allHistoryUsesContiguousBoundedWindowsAndDeduplicates(){
        String date=LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(1).toString();
        candleBody.set("{\"status\":\"success\",\"data\":{\"candles\":[[\""+date+"T15:30:00+05:30\",100,110,90,105,20],[\""+date+"T15:30:00+05:30\",100,110,90,106,20]]}}");
        var rows=provider.getHistory(infy(),"ALL");assertEquals(1,rows.size());assertEquals(0,rows.getFirst().getPrice().compareTo(new java.math.BigDecimal("106")));
        LocalDate expected=LocalDate.of(2000,1,1);for(String path:candlePaths){String[] parts=path.split("/");LocalDate from=LocalDate.parse(parts[parts.length-1]),to=LocalDate.parse(parts[parts.length-2]);assertEquals(expected,from);assertTrue(to.isBefore(from.plusYears(10)));expected=to.plusDays(1);}assertEquals(LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1),expected);
    }
    @Test void failedAllWindowNeverReturnsPartialHistory(){failedCandleRequest.set(2);assertThrows(RuntimeException.class,()->provider.getHistory(infy(),"ALL"));assertEquals(2,candlePaths.size());}
    private static final String QUOTE_BODY = """
            {"status":"success","data":{
              "NSE_EQ:INFY":{"last_price":1500.25,"prev_close_price":1490.0,
                "ohlc":{"high":1510.0,"low":1485.0}}
            }}
            """;

    private HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicInteger firstStatus = new AtomicInteger(200);
    private final AtomicInteger laterStatus = new AtomicInteger(200);
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> marketStatusBody = new AtomicReference<>(
            "{\"status\":\"success\",\"data\":{\"exchange\":\"NSE\",\"status\":\"NORMAL_OPEN\"}}");
    private final AtomicReference<String> quoteBody = new AtomicReference<>(QUOTE_BODY);
    private final AtomicReference<String> indexBody = new AtomicReference<>("""
            {"status":"success","data":{
              "NSE_INDEX:Nifty 50":{"last_price":24500.5,"prev_ohlc":{"close":24400.0}},
              "BSE_INDEX:SENSEX":{"last_price":81000.0,"prev_ohlc":{"close":80800.0}},
              "NSE_INDEX:Nifty Bank":{"last_price":52000.0,"prev_ohlc":{"close":51750.0}},
              "NSE_INDEX:Nifty IT":{"last_price":39000.0,"prev_ohlc":{"close":38600.0}}}}
            """);
    private final AtomicReference<String> indexFullQuoteBody = new AtomicReference<>("""
            {"status":"success","data":{
              "NSE_INDEX:Nifty 50":{"last_price":24500.5,"prev_close_price":24400.0},
              "BSE_INDEX:SENSEX":{"last_price":81000.0,"prev_close_price":80800.0},
              "NSE_INDEX:Nifty Bank":{"last_price":52000.0,"prev_close_price":51750.0},
              "NSE_INDEX:Nifty IT":{"last_price":39000.0,"prev_close_price":38600.0}}}
            """);
    private final java.util.List<String> candlePaths=new java.util.concurrent.CopyOnWriteArrayList<>();
    private final AtomicInteger failedCandleRequest=new AtomicInteger(-1);
    private final AtomicReference<String> candlePath = new AtomicReference<>();
    private final AtomicReference<String> candleBody = new AtomicReference<>(
            "{\"status\":\"success\",\"data\":{\"candles\":[]}}");
    private final AtomicReference<String> searchBody = new AtomicReference<>("""
            {"status":"success","data":[
              {"instrument_key":"NSE_EQ|INE009A01021","trading_symbol":"INFY",
               "name":"Infosys Limited","segment":"NSE_EQ","instrument_type":"EQ"}],
             "meta_data":{"page":{"page_number":1,"total_pages":1}}}
            """);
    private final AtomicInteger candleStatus = new AtomicInteger(200);
    private final AtomicInteger marketStatusCode = new AtomicInteger(200);
    private UpstoxMarketDataProvider provider;

    @BeforeEach
    void startServer() throws IOException {
        requestCount.set(0);
        firstStatus.set(200);
        laterStatus.set(200);
        requestPath.set(null);
        authorization.set(null);
        marketStatusBody.set("{\"status\":\"success\",\"data\":{\"exchange\":\"NSE\",\"status\":\"NORMAL_OPEN\"}}");
        marketStatusCode.set(200);
        quoteBody.set(QUOTE_BODY);
        indexBody.set("""
                {"status":"success","data":{
                  "NSE_INDEX:Nifty 50":{"last_price":24500.5,"prev_ohlc":{"close":24400.0}},
                  "BSE_INDEX:SENSEX":{"last_price":81000.0,"prev_ohlc":{"close":80800.0}},
                  "NSE_INDEX:Nifty Bank":{"last_price":52000.0,"prev_ohlc":{"close":51750.0}},
                  "NSE_INDEX:Nifty IT":{"last_price":39000.0,"prev_ohlc":{"close":38600.0}}}}
                """);
        indexFullQuoteBody.set("""
                {"status":"success","data":{
                  "NSE_INDEX:Nifty 50":{"last_price":24500.5,"prev_close_price":24400.0},
                  "BSE_INDEX:SENSEX":{"last_price":81000.0,"prev_close_price":80800.0},
                  "NSE_INDEX:Nifty Bank":{"last_price":52000.0,"prev_close_price":51750.0},
                  "NSE_INDEX:Nifty IT":{"last_price":39000.0,"prev_close_price":38600.0}}}
                """);
        candlePath.set(null);
        candleBody.set("{\"status\":\"success\",\"data\":{\"candles\":[]}}");
        searchBody.set("""
                {"status":"success","data":[
                  {"instrument_key":"NSE_EQ|INE009A01021","trading_symbol":"INFY",
                   "name":"Infosys Limited","segment":"NSE_EQ","instrument_type":"EQ"}],
                 "meta_data":{"page":{"page_number":1,"total_pages":1}}}
                """);
        candleStatus.set(200);candlePaths.clear();failedCandleRequest.set(-1);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/market-quote/quotes", exchange -> {
            int attempt = requestCount.incrementAndGet();
            requestPath.set(exchange.getRequestURI().toString());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            int status = attempt == 1 ? firstStatus.get() : laterStatus.get();
            String selectedBody = exchange.getRequestURI().getRawQuery().contains("NSE_INDEX")
                    ? indexFullQuoteBody.get() : quoteBody.get();
            byte[] body = (status == 200 ? selectedBody : "{\"status\":\"error\"}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/v3/market-quote/ohlc", exchange -> {
            requestCount.incrementAndGet();
            requestPath.set(exchange.getRequestURI().toString());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = indexBody.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.createContext("/v3/historical-candle", exchange -> {
            requestCount.incrementAndGet();
            candlePath.set(exchange.getRequestURI().toString());
            candlePaths.add(exchange.getRequestURI().toString());
            byte[] body = candleBody.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(candlePaths.size()==failedCandleRequest.get()?503:candleStatus.get(), body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.createContext("/v2/instruments/search", exchange -> {
            requestCount.incrementAndGet();
            requestPath.set(exchange.getRequestURI().toString());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = searchBody.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/v2/market/status/NSE", exchange -> {
            requestCount.incrementAndGet();
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = marketStatusBody.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(marketStatusCode.get(), body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        provider = new UpstoxMarketDataProvider(
                "test-token",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                HttpClient.newHttpClient(),
                Duration.ZERO
        );
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void mapsBatchResponseUsingExchangeAndTradingSymbolKey() {
        List<MarketQuote> quotes = provider.getQuotes(List.of(infy(), tcs()));

        assertEquals(1, quotes.size());
        assertEquals("INFY", quotes.getFirst().getSymbol());
        assertEquals("1500.25", quotes.getFirst().getCurrentPrice().toPlainString());
        assertEquals("1490.0", quotes.getFirst().getPreviousClose().toPlainString());
        assertEquals("1510.0", quotes.getFirst().getDayHigh().toPlainString());
        assertEquals("1485.0", quotes.getFirst().getDayLow().toPlainString());
        assertTrue(requestPath.get().contains("NSE_EQ%7CINE009A01021"));
        assertEquals("Bearer test-token", authorization.get());
    }

    @Test
    void retriesRateLimitedBatchOnceAndSucceeds() {
        firstStatus.set(429);

        List<MarketQuote> quotes = provider.getQuotes(List.of(infy()));

        assertEquals(1, quotes.size());
        assertEquals(2, requestCount.get());
    }

    @Test
    void failsAfterOneRetryWhenProviderContinuesReturning429() {
        firstStatus.set(429);
        laterStatus.set(429);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> provider.getQuotes(List.of(infy())));

        assertTrue(error.getMessage().contains("HTTP 429"));
        assertEquals(2, requestCount.get());
    }

    @Test
    void doesNotRetryOtherHttpFailures() {
        firstStatus.set(500);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> provider.getQuotes(List.of(infy())));

        assertTrue(error.getMessage().contains("HTTP 500"));
        assertEquals(1, requestCount.get());
    }

    @Test
    void acceptsArrayDataFromInstrumentSearchAndUsesItForQuoteLookup() {
        MarketInstrumentSearchResult search = provider.searchInstruments("INFY", 1);

        assertEquals("INFY", search.getInstruments().getFirst().getSymbol());
        assertEquals("NSE_EQ|INE009A01021", search.getInstruments().getFirst().getInstrumentKey());
        assertEquals("Bearer test-token", authorization.get());
        assertTrue(requestPath.get().startsWith("/v2/instruments/search?query=INFY"));

        MarketQuote quote = provider.getQuote("INFY");
        assertEquals("INFY", quote.getSymbol());
        assertEquals("1500.25", quote.getCurrentPrice().toPlainString());

        provider.getHistory("INFY", "1W");
        assertTrue(candlePath.get().contains("NSE_EQ%7CINE009A01021"));
    }

    @Test
    void rejectsMalformedAndUnsuccessfulInstrumentSearchPayloads() {
        searchBody.set("{\"status\":\"success\",\"data\":{}}");
        RuntimeException malformed = assertThrows(RuntimeException.class,
                () -> provider.searchInstruments("INFY", 1));
        assertTrue(malformed.getCause().getMessage().contains("invalid response"));

        searchBody.set("{\"status\":\"error\",\"errors\":[{\"message\":\"bad query\"}]}");
        RuntimeException unsuccessful = assertThrows(RuntimeException.class,
                () -> provider.searchInstruments("INFY", 1));
        assertTrue(unsuccessful.getCause().getMessage().contains("bad query"));
    }

    @Test
    void readsExchangeStatusFromUpstoxResponse() {
        assertEquals("NORMAL_OPEN", provider.getExchangeStatus("NSE"));
        assertEquals("Bearer test-token", authorization.get());
    }

    @Test
    void rejectsMissingOrUnsuccessfulExchangeStatusResponses() {
        marketStatusBody.set("{\"status\":\"success\",\"data\":{\"exchange\":\"NSE\"}}");
        assertThrows(IllegalStateException.class, () -> provider.getExchangeStatus("NSE"));

        marketStatusCode.set(503);
        assertThrows(IllegalStateException.class, () -> provider.getExchangeStatus("NSE"));
    }

    @Test
    void mapsIndexQuotesAndUsesInjectedEndpoint() {
        List<MarketQuote> indices = provider.getIndices();

        assertEquals(List.of("NIFTY 50", "SENSEX", "NIFTY BANK", "NIFTY IT"),
                indices.stream().map(MarketQuote::getSymbol).toList());
        assertEquals("24400.0", indices.getFirst().getPreviousClose().toPlainString());
        assertEquals("81000.0", indices.get(1).getCurrentPrice().toPlainString());
        assertEquals("52000.0", indices.get(2).getCurrentPrice().toPlainString());
        assertEquals("39000.0", indices.getLast().getCurrentPrice().toPlainString());
        assertTrue(requestPath.get().startsWith("/v3/market-quote/ohlc?"));
        assertTrue(requestPath.get().contains("interval=1d"));
        assertTrue(requestPath.get().contains("NSE_INDEX%7CNifty%2050"));
        assertTrue(requestPath.get().contains("NSE_INDEX%7CNifty%20Bank"));
        assertTrue(requestPath.get().contains("NSE_INDEX%7CNifty%20IT"));
        assertEquals(1, requestCount.get(), "complete quote data should not trigger history requests");
        assertNull(candlePath.get());
    }

    @Test
    void leavesPreviousCloseUnavailableInsteadOfInventingZeroChange() {
        indexBody.set("""
                {"status":"success","data":{
                  "NSE_INDEX:Nifty 50":{"last_price":24500.5},
                  "BSE_INDEX:SENSEX":{"last_price":81000.0}}}
                """);
        indexFullQuoteBody.set("""
                {"status":"success","data":{
                  "NSE_INDEX:Nifty 50":{"last_price":24500.5},
                  "BSE_INDEX:SENSEX":{"last_price":81000.0}}}
                """);

        List<MarketQuote> indices = provider.getIndices();

        assertNull(indices.getFirst().getPreviousClose());
        assertNull(indices.get(1).getPreviousClose());
    }

    @Test
    void returnsUnavailableIndexRowsWhenProviderHasNoIndexValues() {
        indexBody.set("{\"status\":\"success\",\"data\":{}}");
        indexFullQuoteBody.set("{\"status\":\"success\",\"data\":{}}");

        List<MarketQuote> indices = provider.getIndices();

        assertEquals(List.of("NIFTY 50", "SENSEX", "NIFTY BANK", "NIFTY IT"),
                indices.stream().map(MarketQuote::getSymbol).toList());
        assertNull(indices.getFirst().getCurrentPrice());
        assertNull(indices.get(1).getCurrentPrice());
    }

    @Test
    void fallsBackToFullQuotesWhenIndexOhlcOmitsPreviousClose() {
        indexBody.set("""
                {"status":"success","data":{
                  "NSE_INDEX:Nifty 50":{"last_price":24500.5},
                  "BSE_INDEX:SENSEX":{"last_price":81000.0}}}
                """);

        List<MarketQuote> indices = provider.getIndices();

        assertEquals("24400.0", indices.getFirst().getPreviousClose().toPlainString());
        assertEquals("80800.0", indices.get(1).getPreviousClose().toPlainString());
    }

    @Test
    void fallsBackToPriorDailyCandleWhenBothIndexQuoteResponsesOmitPreviousClose() {
        LocalDate sessionDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        long sessionTimestamp = sessionDate.atTime(12, 0)
                .atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli();
        indexBody.set(indexQuoteBodyWithoutPreviousClose(sessionTimestamp));
        indexFullQuoteBody.set("""
                {"status":"success","data":{
                  "NSE_INDEX:Nifty 50":{"last_price":24500.5,"last_trade_time":%d},
                  "BSE_INDEX:SENSEX":{"last_price":81000.0,"last_trade_time":%d}}}
                """.formatted(sessionTimestamp, sessionTimestamp));
        candleBody.set("""
                {"status":"success","data":{"candles":[
                  ["%sT00:00:00+05:30",24000,24100,23900,24050,100,0],
                  ["%sT00:00:00+05:30",24100,24500,24000,24400,100,0],
                  ["%sT00:00:00+05:30",24400,24600,24300,24500,100,0]]}}
                """.formatted(sessionDate.minusDays(2), sessionDate.minusDays(1), sessionDate));

        List<MarketQuote> indices = provider.getIndices();

        assertEquals("24400", indices.getFirst().getPreviousClose().toPlainString());
        assertEquals("24400", indices.get(1).getPreviousClose().toPlainString());
        assertTrue(candlePath.get().contains("/days/1/" + sessionDate + "/"));
        assertEquals(4, requestCount.get(),
                "one quote request plus one daily history request should be made per missing index close");
    }

    @Test
    void keepsPreviousCloseUnavailableWhenHistoricalCandlesCannotEstablishPriorSession() {
        LocalDate sessionDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        long sessionTimestamp = sessionDate.atTime(12, 0)
                .atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli();
        indexBody.set(indexQuoteBodyWithoutPreviousClose(sessionTimestamp));
        indexFullQuoteBody.set("""
                {"status":"success","data":{
                  "NSE_INDEX:Nifty 50":{"last_price":24500.5,"last_trade_time":%d},
                  "BSE_INDEX:SENSEX":{"last_price":81000.0,"last_trade_time":%d}}}
                """.formatted(sessionTimestamp, sessionTimestamp));
        candleBody.set("""
                {"status":"success","data":{"candles":[
                  ["%sT00:00:00+05:30",24400,24600,24300,24500,100,0]]}}
                """.formatted(sessionDate));

        List<MarketQuote> indices = provider.getIndices();

        assertNull(indices.getFirst().getPreviousClose());
        assertNull(indices.get(1).getPreviousClose());
    }

    @Test
    void requestsIntradayAndDailyHistoryWithCorrectInstrumentAndIntervals() {
        provider.getHistory("NIFTY 50", "1W");
        assertTrue(candlePath.get().contains("NSE_INDEX%7CNifty%2050/minutes/30/"));

        provider.getHistory("SENSEX", "1M");
        assertTrue(candlePath.get().contains("BSE_INDEX%7CSENSEX/days/1/"));

        provider.getHistory("NIFTY BANK", "1M");
        assertTrue(candlePath.get().contains("NSE_INDEX%7CNifty%20Bank/days/1/"));

        provider.getHistory("NIFTY IT", "1M");
        assertTrue(candlePath.get().contains("NSE_INDEX%7CNifty%20IT/days/1/"));
    }

    @Test
    void usesCurrentDayCandlesForOneDayChartAndFallsBackWhenEmpty() {
        candleBody.set("""
                {"status":"success","data":{"candles":[
                  ["2026-10-28T10:00:00+05:30",100,102,99,101,500,0]]}}
                """);

        List<com.example.investsmart.entity.MarketPriceHistory> history = provider.getHistory("NIFTY 50", "1D");

        assertEquals(1, history.size());
        assertTrue(candlePath.get().contains("/v3/historical-candle/intraday/NSE_INDEX%7CNifty%2050/minutes/5"));

        candleBody.set("{\"status\":\"success\",\"data\":{\"candles\":[]}}");
        provider.getHistory("NIFTY 50", "1D");
        assertTrue(candlePath.get().contains("/v3/historical-candle/NSE_INDEX%7CNifty%2050/minutes/5/"));
    }

    @Test
    void rejectsHttpAndProviderErrorsForCandles() {
        candleStatus.set(502);
        RuntimeException httpError = assertThrows(RuntimeException.class,
                () -> provider.getHistory("NIFTY 50", "1M"));
        assertTrue(httpError.getCause().getMessage().contains("HTTP 502"));

        candleStatus.set(200);
        candleBody.set("{\"status\":\"error\",\"errors\":[{\"message\":\"bad instrument\"}]}");
        RuntimeException providerError = assertThrows(RuntimeException.class,
                () -> provider.getHistory("NIFTY 50", "1M"));
        assertTrue(providerError.getCause().getMessage().contains("bad instrument"));
    }

    private MarketInstrument infy() {
        return new MarketInstrument("NSE_EQ|INE009A01021", "INFY", "Infosys Limited", "NSE");
    }

    private MarketInstrument tcs() {
        return new MarketInstrument("NSE_EQ|INE467B01029", "TCS", "Tata Consultancy Services Limited", "NSE");
    }

    private String indexQuoteBodyWithoutPreviousClose(long sessionTimestamp) {
        return """
                {"status":"success","data":{
                  "NSE_INDEX:Nifty 50":{"last_price":24500.5,"live_ohlc":{"ts":%d}},
                  "BSE_INDEX:SENSEX":{"last_price":81000.0,"live_ohlc":{"ts":%d}}}}
                """.formatted(sessionTimestamp, sessionTimestamp);
    }
}
