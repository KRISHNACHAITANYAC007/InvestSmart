package com.example.investsmart.market;

import com.example.investsmart.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Exchange-issued identifiers are resolved from the provider catalog, never from client metadata. */
@Service
public class EquityCatalogService {
    private final HttpClient client;
    private final String catalogBase;
    private final Map<String, List<MarketInstrument>> catalogs = new HashMap<>();
    private final Map<String, Instant> updated = new HashMap<>();

    public EquityCatalogService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                "https://assets.upstox.com/market-quote/instruments/exchange/");
    }
    EquityCatalogService(HttpClient client, String catalogBase) {
        this.client = client; this.catalogBase = catalogBase;
    }

    private synchronized List<MarketInstrument> catalog(String exchange) {
        if (updated.getOrDefault(exchange, Instant.EPOCH).plus(Duration.ofHours(24)).isAfter(Instant.now()))
            return catalogs.get(exchange);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(catalogBase + exchange + ".json.gz"))
                    .timeout(Duration.ofSeconds(20)).GET().build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) throw new IllegalStateException("Instrument catalog unavailable");
            JsonNode root;
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
                root = new ObjectMapper().readTree(gzip);
            }
            if (!root.isArray()) throw new IllegalStateException("Invalid instrument catalog");
            List<MarketInstrument> loaded = new ArrayList<>();
            for (JsonNode node : root) {
                String segment = node.path("segment").asText();
                String type = node.path("instrument_type").asText();
                String isin = node.path("isin").asText();
                String key = node.path("instrument_key").asText();
                String symbol = node.path("trading_symbol").asText();
                // BSE's equity series include A, B, X, XT, etc., rather than NSE's EQ series.
                if (!segment.equals(exchange + "_EQ") || !isin.matches("INE[A-Z0-9]{9}")
                        || !key.startsWith(segment + "|") || symbol.isBlank()
                        || type.matches("N[A-Z0-9]") || List.of("GS","SG","TB","GB").contains(type)
                        || (exchange.equals("NSE") && !type.equals("EQ"))) continue;
                loaded.add(new MarketInstrument(key, symbol, node.path("name").asText(symbol), exchange, isin));
            }
            if (loaded.isEmpty()) throw new IllegalStateException("Empty equity catalog");
            catalogs.put(exchange, List.copyOf(loaded)); updated.put(exchange, Instant.now());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stock lookup interrupted. Please retry.");
        } catch (Exception e) {
            if (!catalogs.containsKey(exchange)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    exchange + " stock catalog unavailable. Please retry.");
        }
        return catalogs.get(exchange);
    }

    public MarketInstrument resolve(String key) {
        if (key == null || !key.matches("^(NSE_EQ|BSE_EQ)\\|[A-Za-z0-9]+$"))
            throw new BadRequestException("Select a supported equity instrument.");
        return catalog(key.startsWith("BSE_") ? "BSE" : "NSE").stream()
                .filter(i -> i.getInstrumentKey().equals(key)).findFirst()
                .orElseThrow(() -> new BadRequestException("Stock listing not found. Search and select it again."));
    }

    public List<MarketInstrument> listings(String key, String symbol) {
        List<MarketInstrument> all = new ArrayList<>(catalog("NSE")); all.addAll(catalog("BSE"));
        MarketInstrument selected = key != null && !key.isBlank() ? resolve(key) : all.stream()
                .filter(i -> i.getSymbol().equalsIgnoreCase(symbol)).findFirst()
                .orElseThrow(() -> new BadRequestException("Company not found. Search and select it again."));
        return all.stream().filter(i -> i.getIsin().equals(selected.getIsin()))
                .sorted(Comparator.comparing(MarketInstrument::getExchange).reversed()).toList();
    }

    public List<MarketInstrument> listingsByIsin(String isin) {
        if (isin == null || !isin.matches("INE[A-Z0-9]{9}")) return List.of();
        List<MarketInstrument> all = new ArrayList<>(catalog("NSE")); all.addAll(catalog("BSE"));
        return all.stream().filter(i -> isin.equals(i.getIsin()))
                .sorted(Comparator.comparing(MarketInstrument::getExchange).reversed()).toList();
    }

    public MarketInstrumentSearchResult search(String query, int page) {
        if (query == null || query.trim().length() < 2 || query.trim().length() > 50 || page < 1)
            throw new BadRequestException("Enter 2–50 characters and a valid search page.");
        String filter = query.trim().toLowerCase(Locale.ROOT);
        List<MarketInstrument> all = new ArrayList<>(catalog("NSE")); all.addAll(catalog("BSE"));
        Set<String> matching = new HashSet<>();
        all.stream().filter(i -> (i.getSymbol() + " " + i.getCompanyName()).toLowerCase(Locale.ROOT).contains(filter))
                .forEach(i -> matching.add(i.getIsin()));
        Map<String, List<MarketInstrument>> companies = new LinkedHashMap<>();
        all.stream().filter(i -> matching.contains(i.getIsin()))
                .sorted(Comparator.comparing(MarketInstrument::getCompanyName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(MarketInstrument::getExchange, Comparator.reverseOrder()))
                .forEach(i -> companies.computeIfAbsent(i.getIsin(), ignored -> new ArrayList<>()).add(i));
        List<List<MarketInstrument>> groups = new ArrayList<>(companies.values());
        int pages = Math.max(1, (groups.size() + 29) / 30);
        if (page > pages) throw new BadRequestException("Search page out of range.");
        List<MarketInstrument> results = groups.subList(Math.min((page - 1) * 30, groups.size()), Math.min(page * 30, groups.size()))
                .stream().flatMap(Collection::stream).toList();
        return new MarketInstrumentSearchResult(results, page, pages);
    }
}
