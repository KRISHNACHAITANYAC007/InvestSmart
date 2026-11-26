package com.example.investsmart.market;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.math.BigDecimal;

@Service
public class MarketSnapshotService {
    public record Item(String symbol, String companyName, String instrumentKey, String exchange,
                       BigDecimal currentPrice, BigDecimal previousClose, LocalDateTime lastUpdated) { }
    public record Snapshot(List<Item> items, int page, int totalPages, int totalItems,
                           Instant updatedAt, boolean stale, String constituentAsOf) { }
    private record CachedQuote(MarketQuote quote, Instant fetchedAt) { }
    private final MarketDataProvider provider;
    private final HttpClient client;
    private final String catalogUrl;
    private final Map<String, CachedQuote> quotes = new HashMap<>();
    private final Set<String> niftyIsins = new HashSet<>();
    private List<MarketInstrument> catalog = List.of();
    private Instant catalogUpdated = Instant.EPOCH;
    private List<MarketQuote> indices = List.of();
    private Instant indicesUpdated = Instant.EPOCH;

    @Autowired
    public MarketSnapshotService(MarketDataProvider provider) {
        this(provider, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                "https://assets.upstox.com/market-quote/instruments/exchange/NSE.json.gz");
    }

    MarketSnapshotService(MarketDataProvider provider, HttpClient client, String catalogUrl) {
        this.provider = provider;
        this.client = client;
        this.catalogUrl = catalogUrl;
        try (InputStream stream = getClass().getResourceAsStream("/nifty50-constituents.csv")) {
            if (stream == null) throw new IOException("NIFTY constituent snapshot missing");
            new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().skip(1)
                    .filter(line -> !line.isBlank()).forEach(line -> {
                        String[] fields = line.split(","); niftyIsins.add(fields[fields.length - 1].trim());
                    });
        } catch (IOException e) { throw new IllegalStateException("Cannot load NIFTY constituents", e); }
    }

    public synchronized Snapshot featured() {
        return stockSnapshot(MarketDataService.HEATMAP_STOCKS, 1, false, null);
    }

    public synchronized Snapshot heatmap(String group, String query, int page) {
        if (page < 1 || query.length() > 50) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or search");
        if (group.equals("indices")) return indexSnapshot();
        if (!group.equals("all") && !group.equals("nifty50"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use indices, nifty50 or all");
        boolean metadataStale = !refreshCatalog();
        String filter = query.trim().toLowerCase(Locale.ROOT);
        List<MarketInstrument> selected = catalog.stream()
                .filter(item -> !group.equals("nifty50") || niftyIsins.contains(item.getInstrumentKey().split("\\|")[1]))
                .filter(item -> (item.getSymbol() + " " + item.getCompanyName()).toLowerCase(Locale.ROOT).contains(filter))
                .toList();
        return stockSnapshot(selected, page, metadataStale, group.equals("nifty50") ? "2026-11-02" : null);
    }

    private boolean refreshCatalog() {
        if (catalogUpdated.plus(Duration.ofHours(24)).isAfter(Instant.now())) return true;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(catalogUrl))
                    .timeout(Duration.ofSeconds(20)).GET().build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) throw new IOException("Instrument catalog HTTP " + response.statusCode());
            JsonNode root;
            try (GZIPInputStream stream = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
                root = new ObjectMapper().readTree(stream);
            }
            if (!root.isArray()) throw new IOException("Invalid instrument catalog");
            Map<String, MarketInstrument> loaded = new TreeMap<>();
            for (JsonNode node : root) {
                if (!node.path("segment").asText().equals("NSE_EQ") || !node.path("instrument_type").asText().equals("EQ")) continue;
                String symbol = node.path("trading_symbol").asText();
                String key = node.path("instrument_key").asText();
                if (symbol.isBlank() || !key.startsWith("NSE_EQ|")) continue;
                loaded.put(symbol, new MarketInstrument(key, symbol, node.path("name").asText(symbol), "NSE"));
            }
            if (loaded.isEmpty()) throw new IOException("Empty instrument catalog");
            catalog = List.copyOf(loaded.values()); catalogUpdated = Instant.now(); return true;
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (Exception e) { /* Keep the last successful metadata snapshot. */ }
        if (catalog.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "NSE catalog unavailable. Please retry.");
        return false;
    }

    private Snapshot stockSnapshot(List<MarketInstrument> universe, int page, boolean stale, String asOf) {
        int totalPages = Math.max(1, (universe.size() + 49) / 50);
        if (page > totalPages) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page out of range");
        List<MarketInstrument> selected = universe.subList(Math.min((page - 1) * 50, universe.size()), Math.min(page * 50, universe.size()));
        Instant now = Instant.now();
        List<MarketInstrument> expired = selected.stream().filter(item -> !quotes.containsKey(item.getSymbol())
                || quotes.get(item.getSymbol()).fetchedAt().plusSeconds(60).isBefore(now)).toList();
        if (!expired.isEmpty()) {
            try {
                for (MarketQuote quote : provider.getQuotes(expired)) {
                    if (quote.getCurrentPrice() != null) quotes.put(quote.getSymbol(), new CachedQuote(quote, now));
                }
            } catch (RuntimeException e) { stale = true; }
        }
        List<Item> items = new ArrayList<>(); Instant oldest = null;
        for (MarketInstrument instrument : selected) {
            CachedQuote cached = quotes.get(instrument.getSymbol());
            MarketQuote quote = cached == null ? null : cached.quote();
            if (cached == null || cached.fetchedAt().plusSeconds(60).isBefore(now)) stale = true;
            if (cached != null && (oldest == null || cached.fetchedAt().isBefore(oldest))) oldest = cached.fetchedAt();
            items.add(new Item(instrument.getSymbol(), instrument.getCompanyName(), instrument.getInstrumentKey(), instrument.getExchange(),
                    quote == null ? null : quote.getCurrentPrice(), quote == null ? null : quote.getPreviousClose(), quote == null ? null : quote.getLastUpdated()));
        }
        return new Snapshot(items, page, totalPages, universe.size(), oldest, stale, asOf);
    }

    private Snapshot indexSnapshot() {
        Instant now = Instant.now(); boolean stale = false;
        if (indicesUpdated.plusSeconds(60).isBefore(now)) {
            try {
                List<MarketQuote> loaded = provider.getIndices();
                if (loaded.stream().allMatch(q -> q.getCurrentPrice() == null)) throw new IllegalStateException("Index quotes unavailable");
                indices = loaded; indicesUpdated = now;
            } catch (RuntimeException e) { stale = true; }
        }
        List<Item> items = new ArrayList<>();
        for (String symbol : List.of("NIFTY 50", "SENSEX", "NIFTY BANK", "NIFTY IT")) {
            MarketQuote quote = indices.stream().filter(q -> q.getSymbol().equals(symbol)).findFirst().orElse(null);
            items.add(new Item(symbol, symbol, null, symbol.equals("SENSEX") ? "BSE" : "NSE", quote == null ? null : quote.getCurrentPrice(),
                    quote == null ? null : quote.getPreviousClose(), quote == null ? null : quote.getLastUpdated()));
            if (quote == null || quote.getCurrentPrice() == null) stale = true;
        }
        return new Snapshot(items, 1, 1, 4, indicesUpdated.equals(Instant.EPOCH) ? null : indicesUpdated, stale, null);
    }
}
