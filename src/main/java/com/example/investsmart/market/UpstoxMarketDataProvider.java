package com.example.investsmart.market;

import com.example.investsmart.entity.MarketPriceHistory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicLong;
import com.example.investsmart.security.AdminHealthStore;

@Component
@Primary
public class UpstoxMarketDataProvider
        implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(UpstoxMarketDataProvider.class);
    private static final List<IndexInstrument> INDEX_INSTRUMENTS = List.of(
            new IndexInstrument("NIFTY 50", "NSE_INDEX|Nifty 50", "NSE_INDEX:Nifty 50"),
            new IndexInstrument("SENSEX", "BSE_INDEX|SENSEX", "BSE_INDEX:SENSEX"),
            new IndexInstrument("NIFTY BANK", "NSE_INDEX|Nifty Bank", "NSE_INDEX:Nifty Bank"),
            new IndexInstrument("NIFTY IT", "NSE_INDEX|Nifty IT", "NSE_INDEX:Nifty IT")
    );

    private record IndexInstrument(String symbol, String instrumentKey, String dataKey) { }

    private final String accessToken;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final Duration rateLimitRetryDelay;
    private final AdminHealthStore healthStore;
    private final AtomicLong failures = new AtomicLong();
    private volatile Instant lastSuccessAt;
    private volatile Instant lastFailureAt;
    private volatile String lastFailure;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Autowired
    public UpstoxMarketDataProvider(
            @Value("${upstox.api.token:}") String accessToken,
            @Value("${upstox.api.base-url:https://api.upstox.com}") String baseUrl,
            AdminHealthStore healthStore
    ) {
        this(accessToken, baseUrl,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                Duration.ofSeconds(1), healthStore);
    }

    UpstoxMarketDataProvider(String accessToken, String baseUrl,
                             HttpClient httpClient, Duration rateLimitRetryDelay) {
        this(accessToken, baseUrl, httpClient, rateLimitRetryDelay, null);
    }

    UpstoxMarketDataProvider(String accessToken, String baseUrl,
                             HttpClient httpClient, Duration rateLimitRetryDelay,
                             AdminHealthStore healthStore) {
        this.accessToken = accessToken;
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.rateLimitRetryDelay = rateLimitRetryDelay;
        this.healthStore = healthStore;
    }

    private HttpResponse<String> sendTracked(HttpRequest request)
            throws java.io.IOException, InterruptedException {
        return sendTracked(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendTracked(
            HttpRequest request,
            HttpResponse.BodyHandler<String> bodyHandler
    ) throws java.io.IOException, InterruptedException {
        try {
            HttpResponse<String> response = httpClient.send(request, bodyHandler);
            if (response.statusCode() == 200) {
                lastSuccessAt = Instant.now();
                persistHealth(() -> healthStore.succeeded("UPSTOX", lastSuccessAt));
            }
            else recordFailure("HTTP " + response.statusCode());
            return response;
        } catch (java.io.IOException | InterruptedException failure) {
            recordFailure(failure.getClass().getSimpleName());
            throw failure;
        }
    }

    private void persistHealth(Runnable write) {
        if(healthStore==null)return;
        try{write.run();}catch(RuntimeException failure){log.warn("Provider telemetry storage unavailable ({})",failure.getClass().getSimpleName());}
    }

    private void recordFailure(String reason) {
        failures.incrementAndGet();
        lastFailureAt = Instant.now();
        lastFailure = reason;
        persistHealth(() -> healthStore.failed("UPSTOX", lastFailureAt, reason));
    }

    public Map<String,Object> health() {
        Map<String,Object> status = new LinkedHashMap<>();
        status.put("configured", !accessToken.isBlank());
        Map<String,Object> saved = healthStore == null ? Map.of() : healthStore.status("UPSTOX");
        status.put("failureCount", saved.getOrDefault("failureCount", failures.get()));
        status.put("failuresSinceRestart", failures.get());
        status.put("lastSuccessAt", saved.get("lastSuccessAt") == null ? lastSuccessAt : saved.get("lastSuccessAt"));
        status.put("lastFailureAt", saved.get("lastFailureAt") == null ? lastFailureAt : saved.get("lastFailureAt"));
        status.put("lastFailure", saved.get("lastFailure") == null ? lastFailure : saved.get("lastFailure"));
        return status;
    }

    @Override
    public MarketQuote getQuote(String symbol) {
        if (isIndex(symbol)) {
            return getIndices().stream()
                    .filter(index -> index.getSymbol().equalsIgnoreCase(symbol.trim()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Index not found: " + symbol
                    ));
        }
        return getQuote(symbol, getInstrumentKey(symbol));
    }

    @Override
    public MarketQuote getQuote(MarketInstrument instrument) {
        return getQuote(
                instrument.getSymbol(),
                instrument.getInstrumentKey()
        ).withInstrument(instrument);
    }

    private MarketQuote getQuote(String symbol, String instrumentKey) {

        String url =
                baseUrl + "/v3/market-quote/quotes"
                        + "?instrument_key="
                        + instrumentKey.replace("|", "%7C").replace(" ", "%20");

        try {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(10))
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + accessToken
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    sendTracked(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );
            if (response.statusCode() != 200) {
                int status=response.statusCode();
                String message=status==401||status==403 ? "Market data access is unavailable. Provider credentials or permissions need review. No order was placed."
                    : status==429 ? "The market data provider is rate limiting requests. Wait before retrying; no order was placed."
                    : "Fresh executable market data is unavailable. Retry when the provider is available; no order was placed.";
                int retry=15;
                String header=response.headers().firstValue("Retry-After").orElse("");
                try { retry=Math.max(1,Math.min(86400,Integer.parseInt(header))); } catch(NumberFormatException ignored) {
                    try { retry=(int)Math.max(1,Math.min(86400,Duration.between(Instant.now(),java.time.ZonedDateTime.parse(header,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).getSeconds())); } catch(java.time.format.DateTimeParseException absent) { if(status==429)retry=60; }
                }
                throw new com.example.investsmart.exception.MarketStatusUnavailableException(message,null,retry);
            }

            JsonNode root = objectMapper.readTree(response.body());
            ensureSuccessfulPayload(root, "quote");

            JsonNode quoteData = quoteNode(root.path("data"), instrumentKey, symbol);

            if (quoteData.isMissingNode() || !quoteData.path("last_price").isNumber()) {

                throw new RuntimeException(
                        "No market data returned for "
                                + symbol
                );
            }

            BigDecimal currentPrice =
                    quoteData
                            .get("last_price")
                            .decimalValue();

            BigDecimal previousClose = quoteData.path("prev_close_price").isNumber()
                    ? quoteData.path("prev_close_price").decimalValue() : null;

            BigDecimal dayHigh = quoteData.path("ohlc").path("high").isNumber()
                    ? quoteData.path("ohlc").path("high").decimalValue()
                    : null;
            BigDecimal dayLow = quoteData.path("ohlc").path("low").isNumber()
                    ? quoteData.path("ohlc").path("low").decimalValue()
                    : null;

            LocalDateTime lastUpdated =
                    LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

            return new MarketQuote(
                    symbol.toUpperCase(),
                    currentPrice,
                    previousClose,
                    dayHigh,
                    dayLow,
                    lastUpdated
            );

        } catch (com.example.investsmart.exception.MarketStatusUnavailableException unavailable) {
            throw unavailable;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upstox quote request was interrupted", e);
        } catch (Exception e) {
            log.warn("Upstox quote request failed: {}", safeFailure(e));
            throw new RuntimeException("Failed to fetch Upstox market data", e);
        }
    }

    private JsonNode quoteNode(JsonNode data, String key, String symbol) {
        JsonNode node = data.path(key);
        if (!node.isMissingNode()) return node;
        // Quote object keys use exchange:trading_symbol, and instrument_token disambiguates aliases.
        for (JsonNode candidate : data) {
            if (key.equals(candidate.path("instrument_token").asText())) return candidate;
        }
        return data.path(key.substring(0, key.indexOf('|')) + ":" + symbol.toUpperCase());
    }

    @Override
    public List<MarketQuote> getQuotes() {

        String instrumentKeys =
                "NSE_EQ|INE009A01021,"
                        + "NSE_EQ|INE467B01029,"
                        + "NSE_EQ|INE002A01018";

        String encodedInstrumentKeys =
                instrumentKeys.replace("|", "%7C");

        String url = baseUrl + "/v3/market-quote/quotes"
                + "?instrument_key=" + encodedInstrumentKeys;

        try {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(10))
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + accessToken
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    sendTracked(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Upstox quote request failed with HTTP "
                                + response.statusCode()
                );
            }

            JsonNode root =
                    objectMapper.readTree(
                            response.body()
                    );
            ensureSuccessfulPayload(root, "quote");

            JsonNode data =
                    root.get("data");

            List<MarketQuote> quotes =
                    new ArrayList<>();

            quotes.add(
                    createQuoteFromData(
                            data,
                            "INFY"
                    )
            );

            quotes.add(
                    createQuoteFromData(
                            data,
                            "TCS"
                    )
            );

            quotes.add(
                    createQuoteFromData(
                            data,
                            "RELIANCE"
                    )
            );

            return quotes;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upstox quote request was interrupted", e);
        } catch (Exception e) {
            log.warn("Upstox quote request failed: {}", safeFailure(e));
            throw new RuntimeException("Failed to fetch Upstox market data", e);
        }
    }

    @Override
    public List<MarketQuote> getQuotes(List<MarketInstrument> instruments) {
        if (instruments == null || instruments.isEmpty()) return List.of();
        String keys = instruments.stream()
                .map(MarketInstrument::getInstrumentKey)
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
        String url = baseUrl + "/v3/market-quote/quotes?instrument_key="
                + keys.replace("|", "%7C");
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + accessToken).GET().build();
            HttpResponse<String> response = sendBatchQuoteRequest(request);
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Upstox quote request failed with HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            ensureSuccessfulPayload(root, "quote");
            JsonNode data = root.path("data");
            List<MarketQuote> quotes = new ArrayList<>();
            for (MarketInstrument instrument : instruments) {
                String key = instrument.getInstrumentKey();
                JsonNode node = quoteNode(data, key, instrument.getSymbol());
                if (node.isMissingNode() || !node.path("last_price").isNumber()) continue;
                BigDecimal price = node.path("last_price").decimalValue();
                BigDecimal previous = node.path("prev_close_price").isNumber()
                        ? node.path("prev_close_price").decimalValue() : null;
                JsonNode ohlc = node.path("ohlc");
                BigDecimal high = ohlc.path("high").isNumber() ? ohlc.path("high").decimalValue() : null;
                BigDecimal low = ohlc.path("low").isNumber() ? ohlc.path("low").decimalValue() : null;
                quotes.add(new MarketQuote(instrument.getSymbol().toUpperCase(), price, previous,
                        high, low, LocalDateTime.now(ZoneId.of("Asia/Kolkata"))).withInstrument(instrument));
            }
            return quotes;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upstox quote request was interrupted", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Upstox market data", e);
        }
    }

    @Override
    public String getExchangeStatus(String exchange) {
        String url = baseUrl + "/v2/market/status/" + exchange;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET().build();
        try {
            HttpResponse<String> response = sendTracked(request);
            JsonNode root = checkedJson(response, "market status");
            String status = root
                    .path("data").path("status").asText();
            if (status.isBlank()) {
                throw new IllegalStateException("Upstox returned no exchange market status");
            }
            return status;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upstox market status request was interrupted", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve exchange market status", e);
        }
    }

    private HttpResponse<String> sendBatchQuoteRequest(HttpRequest request)
            throws java.io.IOException, InterruptedException {
        HttpResponse<String> response = sendTracked(request);
        if (response.statusCode() != 429) {
            return response;
        }

        if (!rateLimitRetryDelay.isZero() && !rateLimitRetryDelay.isNegative()) {
            Thread.sleep(rateLimitRetryDelay.toMillis());
        }
        return sendTracked(request);
    }

    @Override
    public MarketInstrumentSearchResult searchInstruments(String query, int page) {
        String encodedQuery = URLEncoder.encode(
                query,
                StandardCharsets.UTF_8
        );
        String url = baseUrl + "/v2/instruments/search"
                + "?query=" + encodedQuery
                + "&exchanges=NSE&segments=EQ&instrument_types=EQ"
                + "&page_number=" + page + "&records=30";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = sendTracked(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode root = checkedArrayJson(response, "instrument search");
            JsonNode instruments = root.path("data");
            int responsePage = root.path("meta_data").path("page")
                    .path("page_number").asInt(page);
            int totalPages = root.path("meta_data").path("page")
                    .path("total_pages").asInt(responsePage);
            List<MarketInstrument> results = new ArrayList<>();
            if (!instruments.isArray()) {
                return new MarketInstrumentSearchResult(results, responsePage, totalPages);
            }

            for (JsonNode instrument : instruments) {
                if (!"NSE_EQ".equals(instrument.path("segment").asText())
                        || !"EQ".equals(instrument.path("instrument_type").asText())) {
                    continue;
                }
                String instrumentKey = instrument.path("instrument_key").asText();
                String symbol = instrument.path("trading_symbol").asText();
                if (instrumentKey.isBlank() || symbol.isBlank()) {
                    continue;
                }
                results.add(new MarketInstrument(
                        instrumentKey,
                        symbol,
                        instrument.path("name").asText(symbol),
                        "NSE"
                ));
            }
            return new MarketInstrumentSearchResult(results, responsePage, totalPages);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Stock search was interrupted. Please try again.",
                    exception
            );
        } catch (Exception exception) {
            log.warn("Upstox instrument search failed: {}", safeFailure(exception));
            throw new RuntimeException(
                    "Unable to search stocks with Upstox right now.",
                    exception
            );
        }
    }

    @Override
    public List<MarketQuote> getIndices() {
        String encodedInstrumentKeys = INDEX_INSTRUMENTS.stream()
                .map(IndexInstrument::instrumentKey)
                .collect(java.util.stream.Collectors.joining(","))
                .replace("|", "%7C")
                .replace(" ", "%20")
                .replace(",", "%2C");

        String ohlcUrl = baseUrl + "/v3/market-quote/ohlc"
                + "?instrument_key=" + encodedInstrumentKeys
                + "&interval=1d";
        JsonNode ohlcData = requestIndexData(ohlcUrl, "index OHLC quote");

        JsonNode fullQuoteData = null;
        if (INDEX_INSTRUMENTS.stream().anyMatch(index ->
                !hasIndexPrice(ohlcData, index.dataKey())
                        || !hasIndexPreviousClose(ohlcData, index.dataKey()))) {
            String quotesUrl = baseUrl + "/v3/market-quote/quotes"
                    + "?instrument_key=" + encodedInstrumentKeys;
            fullQuoteData = requestIndexData(quotesUrl, "index full quote");
        }

        JsonNode resolvedFullQuoteData = fullQuoteData;
        return INDEX_INSTRUMENTS.stream()
                .map(index -> createIndexQuote(ohlcData, resolvedFullQuoteData,
                        index.symbol(), index.dataKey()))
                .toList();
    }

    private MarketQuote createIndexQuote(
            JsonNode ohlcData,
            JsonNode fullQuoteData,
            String symbol,
            String dataKey
    ) {

        JsonNode ohlcQuote = ohlcData == null ? null : ohlcData.get(dataKey);
        JsonNode fullQuote = fullQuoteData == null ? null : fullQuoteData.get(dataKey);

        BigDecimal currentPrice = decimalOrNull(ohlcQuote == null ? null : ohlcQuote.get("last_price"));
        if (currentPrice == null) {
            currentPrice = decimalOrNull(fullQuote == null ? null : fullQuote.get("last_price"));
        }
        BigDecimal previousClose = decimalOrNull(ohlcQuote == null ? null
                : ohlcQuote.path("prev_ohlc").get("close"));
        if (previousClose == null) {
            previousClose = decimalOrNull(fullQuote == null ? null : fullQuote.get("prev_close_price"));
        }
        if (previousClose == null && currentPrice != null) {
            LocalDate sessionDate = indexSessionDate(ohlcQuote, fullQuote);
            if (sessionDate != null) {
                previousClose = fetchPreviousSessionClose(
                        dataKey.replace(':', '|'), sessionDate);
            }
        }

        return new MarketQuote(
                symbol,
                currentPrice,
                previousClose,
                LocalDateTime.now()
        );
    }

    private boolean hasIndexPrice(JsonNode data, String key) {
        return data != null && decimalOrNull(data.path(key).get("last_price")) != null;
    }

    private boolean hasIndexPreviousClose(JsonNode data, String key) {
        return data != null && decimalOrNull(data.path(key).path("prev_ohlc").get("close")) != null;
    }

    private LocalDate indexSessionDate(JsonNode ohlcQuote, JsonNode fullQuote) {
        JsonNode ohlcTimestamp = ohlcQuote == null ? null
                : ohlcQuote.path("live_ohlc").get("ts");
        LocalDate date = indiaDateFromEpochMillis(ohlcTimestamp);
        if (date != null) {
            return date;
        }
        JsonNode lastTradeTime = fullQuote == null ? null : fullQuote.get("last_trade_time");
        return indiaDateFromEpochMillis(lastTradeTime);
    }

    private LocalDate indiaDateFromEpochMillis(JsonNode timestamp) {
        if (timestamp == null || timestamp.isNull()) {
            return null;
        }
        try {
            long epochMillis = timestamp.isNumber()
                    ? timestamp.longValue()
                    : Long.parseLong(timestamp.asText());
            if (epochMillis <= 0) {
                return null;
            }
            return Instant.ofEpochMilli(epochMillis)
                    .atZone(ZoneId.of("Asia/Kolkata"))
                    .toLocalDate();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private BigDecimal fetchPreviousSessionClose(String instrumentKey, LocalDate sessionDate) {
        LocalDate fromDate = sessionDate.minusDays(14);
        String encodedInstrument = instrumentKey.replace("|", "%7C").replace(" ", "%20");
        String url = baseUrl + "/v3/historical-candle/" + encodedInstrument
                + "/days/1/" + sessionDate + "/" + fromDate;
        JsonNode data = requestIndexData(url, "index historical candle");
        JsonNode candles = data == null ? null : data.get("candles");
        if (candles == null || !candles.isArray()) {
            return null;
        }

        LocalDate previousDate = null;
        BigDecimal previousClose = null;
        for (JsonNode candle : candles) {
            if (!candle.isArray() || candle.size() < 5 || !candle.path(4).isNumber()) {
                continue;
            }
            LocalDate candleDate;
            try {
                candleDate = OffsetDateTime.parse(candle.path(0).asText())
                        .atZoneSameInstant(ZoneId.of("Asia/Kolkata"))
                        .toLocalDate();
            } catch (RuntimeException exception) {
                continue;
            }
            if (candleDate.isBefore(sessionDate)
                    && (previousDate == null || candleDate.isAfter(previousDate))) {
                previousDate = candleDate;
                previousClose = candle.path(4).decimalValue();
            }
        }
        return previousClose;
    }

    private BigDecimal decimalOrNull(JsonNode value) {
        return value != null && value.isNumber() ? value.decimalValue() : null;
    }

    private JsonNode requestIndexData(String url, String operation) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = sendTracked(request);
            return checkedJson(response, operation).get("data");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Upstox {} request was interrupted", operation);
            return null;
        } catch (Exception exception) {
            log.warn("Upstox {} request failed: {}", operation, safeFailure(exception));
            return null;
        }
    }

    private MarketQuote createQuoteFromData(
            JsonNode data,
            String symbol
    ) {

        String dataKey =
                "NSE_EQ:" + symbol;

        JsonNode quoteData =
                data.get(dataKey);

        if (quoteData == null) {

            throw new RuntimeException(
                    "No market data returned for "
                            + symbol
            );
        }

        BigDecimal currentPrice =
                quoteData
                        .get("last_price")
                        .decimalValue();

        BigDecimal previousClose =
                quoteData
                        .get("prev_close_price")
                        .decimalValue();

        return new MarketQuote(
                symbol,
                currentPrice,
                previousClose,
                LocalDateTime.now()
        );
    }

    @Override
    public List<MarketPriceHistory> getHistory(
            String symbol
    ) {

        return getHistory(
                symbol,
                "1M"
        );
    }

    @Override
    public List<MarketPriceHistory> getHistory(
            String symbol,
            String period
    ) {

        return getHistory(new MarketInstrument(getInstrumentKey(symbol), symbol, symbol,
                symbol.equalsIgnoreCase("SENSEX") ? "BSE" : "NSE"), period);
    }

    @Override
    public List<MarketPriceHistory> getHistory(MarketInstrument instrument, String period) {

        String symbol = instrument.getSymbol().toUpperCase();
        period = period.toUpperCase();

        String instrumentKey = instrument.getInstrumentKey();

        ZoneId indiaZone =
                ZoneId.of("Asia/Kolkata");

        LocalDate today =
                LocalDate.now(indiaZone);

        /*
         * 1D
         *
         * We request the last 7 calendar days using
         * 5-minute candles.
         *
         * Then we keep only the latest trading day.
         *
         * This also works when today is Saturday/Sunday
         * because the latest available trading day will
         * naturally be selected.
         */
        if (period.equals("1D")) {

            try {
                List<MarketPriceHistory> intraday = fetchIntradayCandles(instrumentKey, symbol);
                if (!intraday.isEmpty()) {
                    return intraday;
                }
            } catch (RuntimeException exception) {
                log.info("Intraday candles unavailable for {}; trying recent history", symbol);
            }

            LocalDate fromDate =
                    today.minusDays(7);

            LocalDate toDate =
                    today;

            return fetchCandles(
                    instrumentKey,
                    symbol,
                    "minutes",
                    5,
                    fromDate,
                    toDate,
                    true
            );
        }

        LocalDate fromDate;

        switch (period) {

            case "1W":
                fromDate = today.minusWeeks(1);

                return fetchCandles(
                        instrumentKey,
                        symbol,
                        "minutes",
                        30,
                        fromDate,
                        today,
                        false
                );

            case "1M":
                fromDate = today.minusMonths(1);
                break;

            case "6M":
                fromDate = today.minusMonths(6);
                break;

            case "1Y":
                fromDate = today.minusYears(1);
                break;
            case "5Y":
                fromDate = today.minusYears(5);
                break;
            case "ALL":
                fromDate = LocalDate.of(2000,1,1);
                break;

            default:
                throw new IllegalArgumentException(
                        "Invalid period: " + period
                                + ". Use 1D, 1W, 1M, 6M, 1Y, 5Y or ALL."
                );
        }
        var candles=new java.util.TreeMap<LocalDateTime,MarketPriceHistory>();
        for(LocalDate from=fromDate;!from.isAfter(today);){
            LocalDate to=from.plusYears(10).minusDays(1);if(to.isAfter(today))to=today;
            for(var point:fetchCandles(instrumentKey,symbol,"days",1,from,to,false)){
                if(point.getTimestamp()==null)continue;
                if((period.equals("5Y")||period.equals("ALL"))&&(point.getTimestamp().toLocalDate().isBefore(from)||point.getTimestamp().toLocalDate().isAfter(to)))continue;
                candles.put(point.getTimestamp(),point);
            }
            from=to.plusDays(1);
        }
        return new ArrayList<>(candles.values());
    }

    private List<MarketPriceHistory> fetchCandles(
            String instrumentKey,
            String symbol,
            String unit,
            int interval,
            LocalDate fromDate,
            LocalDate toDate,
            boolean latestTradingDayOnly
    ) {

        String encodedInstrument =
                instrumentKey.replace("|", "%7C").replace(" ", "%20");

        String url =
                baseUrl + "/v3/historical-candle/"
                        + encodedInstrument
                        + "/"
                        + unit
                        + "/"
                        + interval
                        + "/"
                        + toDate
                        + "/"
                        + fromDate;

        return fetchCandlesFromUrl(url, symbol, latestTradingDayOnly);
    }

    private List<MarketPriceHistory> fetchIntradayCandles(String instrumentKey, String symbol) {
        String encodedInstrument = instrumentKey.replace("|", "%7C").replace(" ", "%20");
        String url = baseUrl + "/v3/historical-candle/intraday/" + encodedInstrument + "/minutes/5";
        return fetchCandlesFromUrl(url, symbol, false);
    }

    private List<MarketPriceHistory> fetchCandlesFromUrl(
            String url,
            String symbol,
            boolean latestTradingDayOnly
    ) {
        try {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(10))
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + accessToken
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    sendTracked(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            JsonNode root = checkedJson(response, "historical candle");

            JsonNode data =
                    root.get("data");

            if (data == null
                    || data.get("candles") == null) {

                return new ArrayList<>();
            }

            JsonNode candles =
                    data.get("candles");

            List<MarketPriceHistory> history =
                    new ArrayList<>();

            LocalDate latestTradingDate = null;

            /*
             * First find the latest available trading day.
             *
             * This is mainly required for 1D because the
             * current date can be a weekend/holiday.
             */
            if (latestTradingDayOnly) {

                for (JsonNode candle : candles) {

                    LocalDateTime timestamp =
                            java.time.OffsetDateTime
                                    .parse(
                                            candle
                                                    .get(0)
                                                    .asText()
                                    )
                                    .toLocalDateTime();

                    LocalDate candleDate =
                            timestamp.toLocalDate();

                    if (latestTradingDate == null
                            || candleDate.isAfter(
                            latestTradingDate
                    )) {

                        latestTradingDate =
                                candleDate;
                    }
                }
            }

            for (JsonNode candle : candles) {

                LocalDateTime timestamp =
                        java.time.OffsetDateTime
                                .parse(
                                        candle
                                                .get(0)
                                                .asText()
                                )
                                .toLocalDateTime();

                /*
                 * For 1D, keep only candles belonging
                 * to the latest trading day.
                 */
                if (latestTradingDayOnly
                        && !timestamp
                        .toLocalDate()
                        .equals(latestTradingDate)) {

                    continue;
                }

                MarketPriceHistory point =
                        new MarketPriceHistory();

                point.setSymbol(symbol);

                point.setOpen(
                        candle
                                .get(1)
                                .decimalValue()
                );

                point.setHigh(
                        candle
                                .get(2)
                                .decimalValue()
                );

                point.setLow(
                        candle
                                .get(3)
                                .decimalValue()
                );

                point.setPrice(
                        candle
                                .get(4)
                                .decimalValue()
                );

                point.setVolume(
                        candle
                                .get(5)
                                .decimalValue()
                );

                point.setTimestamp(timestamp);

                history.add(point);
            }

            /*
             * Upstox commonly returns newest candles first.
             * Lightweight Charts expects chronological data.
             */
            Collections.sort(
                    history,
                    java.util.Comparator.comparing(
                            MarketPriceHistory::getTimestamp
                    )
            );

            return history;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upstox history request was interrupted", e);
        } catch (Exception e) {
            log.warn("Upstox history request failed: {}", safeFailure(e));
            throw new RuntimeException("Failed to fetch historical data from Upstox", e);
        }
    }

    private JsonNode checkedJson(HttpResponse<String> response, String operation) throws Exception {
        if (response.statusCode() != 200) {
            recordFailure("HTTP " + response.statusCode());
            throw new IllegalStateException("Upstox " + operation + " request failed with HTTP "
                    + response.statusCode());
        }
        try {
            JsonNode root = objectMapper.readTree(response.body());
            ensureSuccessfulPayload(root, operation);
            lastSuccessAt = Instant.now();
            return root;
        } catch (Exception failure) {
            recordFailure(failure.getClass().getSimpleName());
            throw failure;
        }
    }

    private JsonNode checkedArrayJson(HttpResponse<String> response, String operation) throws Exception {
        if (response.statusCode() != 200) {
            recordFailure("HTTP " + response.statusCode());
            throw new IllegalStateException("Upstox " + operation + " request failed with HTTP "
                    + response.statusCode());
        }
        try {
            JsonNode root = objectMapper.readTree(response.body());
            ensureSuccessfulArrayPayload(root, operation);
            lastSuccessAt = Instant.now();
            return root;
        } catch (Exception failure) {
            recordFailure(failure.getClass().getSimpleName());
            throw failure;
        }
    }

    private void ensureSuccessfulPayload(JsonNode root, String operation) {
        String status = root.path("status").asText();
        if ("error".equalsIgnoreCase(status) || !root.path("data").isObject()) {
            throw unsuccessfulPayload(root, operation);
        }
    }

    private void ensureSuccessfulArrayPayload(JsonNode root, String operation) {
        String status = root.path("status").asText();
        if ("error".equalsIgnoreCase(status) || !root.path("data").isArray()) {
            throw unsuccessfulPayload(root, operation);
        }
    }

    private IllegalStateException unsuccessfulPayload(JsonNode root, String operation) {
        String code = root.path("errors").path(0).path("errorCode").asText();
        String message = root.path("errors").path(0).path("message").asText();
        String detail = !message.isBlank() ? message : (!code.isBlank() ? code : "invalid response");
        return new IllegalStateException("Upstox " + operation + " response was unsuccessful: " + detail);
    }

    private String safeFailure(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return exception.getClass().getSimpleName();
        return exception.getClass().getSimpleName() + ": " + message.replaceAll("(?i)Bearer\\s+\\S+", "Bearer [redacted]");
    }

    private String getInstrumentKey(
            String symbol
    ) {
        if (symbol.equalsIgnoreCase("NIFTY 50")) {
            return "NSE_INDEX|Nifty 50";
        }
        if (symbol.equalsIgnoreCase("SENSEX")) {
            return "BSE_INDEX|SENSEX";
        }
        if (symbol.equalsIgnoreCase("NIFTY BANK")) {
            return "NSE_INDEX|Nifty Bank";
        }
        if (symbol.equalsIgnoreCase("NIFTY IT")) {
            return "NSE_INDEX|Nifty IT";
        }
        return searchInstruments(symbol, 1).getInstruments().stream()
                .filter(instrument -> instrument.getSymbol()
                        .equalsIgnoreCase(symbol))
                .map(MarketInstrument::getInstrumentKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "NSE stock not found: " + symbol
                ));
    }

    private boolean isIndex(String symbol) {
        return symbol != null
                && (symbol.equalsIgnoreCase("NIFTY 50")
                || symbol.equalsIgnoreCase("SENSEX")
                || symbol.equalsIgnoreCase("NIFTY BANK")
                || symbol.equalsIgnoreCase("NIFTY IT"));
    }
}
