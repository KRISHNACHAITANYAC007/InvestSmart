package com.example.investsmart.market;

import com.example.investsmart.entity.MarketPriceHistory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/market")
public class MarketController {

    private final MarketDataService marketDataService;
    private final StockQuoteRefreshService stockQuoteRefreshService;
    private final MarketSnapshotService snapshots;
    private final EquityCatalogService catalog;

    public MarketController(
            MarketDataService marketDataService,
            StockQuoteRefreshService stockQuoteRefreshService, MarketSnapshotService snapshots, EquityCatalogService catalog) {
        this.catalog = catalog;
        this.snapshots = snapshots;

        this.marketDataService = marketDataService;
        this.stockQuoteRefreshService = stockQuoteRefreshService;
    }

    @GetMapping("/quotes/{symbol}")
    public ResponseEntity<MarketQuote> getQuote(
            @PathVariable String symbol, @RequestParam(required = false) String instrumentKey) {

        MarketQuote quote =
                instrumentKey == null ? marketDataService.getQuote(symbol)
                        : marketDataService.getQuote(catalog.resolve(instrumentKey));

        return ResponseEntity.ok(quote);
    }

    @GetMapping("/quotes")
    public ResponseEntity<List<MarketQuote>> getQuotes() {

        List<MarketQuote> quotes =
                marketDataService.getQuotes();

        return ResponseEntity.ok(quotes);
    }

    @PostMapping("/quotes/refresh")
    public ResponseEntity<QuoteRefreshResponse> refreshQuotes(
            @RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.ok(stockQuoteRefreshService.refreshAll(force));
    }

    @GetMapping("/instruments")
    public ResponseEntity<MarketInstrumentSearchResult> searchInstruments(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(catalog.search(query, page));
    }

    @GetMapping("/listings")
    public List<MarketInstrument> listings(@RequestParam(required = false) String instrumentKey,
                                           @RequestParam(defaultValue = "") String symbol) {
        return catalog.listings(instrumentKey, symbol);
    }

    @GetMapping("/indices")
    public ResponseEntity<List<MarketQuote>> getIndices() {

        List<MarketQuote> indices =
                marketDataService.getIndices();

        return ResponseEntity.ok(indices);
    }

    @GetMapping(value = "/heatmap", params = "!group")
    public ResponseEntity<List<MarketQuote>> getHeatmapQuotes() {
        return ResponseEntity.ok(marketDataService.getHeatmapQuotes());
    }

    @GetMapping("/featured")
    public ResponseEntity<MarketSnapshotService.Snapshot> featured() {
        return ResponseEntity.ok(snapshots.featured());
    }

    @GetMapping(value = "/heatmap", params = "group")
    public ResponseEntity<MarketSnapshotService.Snapshot> heatmap(
            @RequestParam String group, @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(snapshots.heatmap(group, query, page));
    }

    @GetMapping("/history/{symbol}")
    public ResponseEntity<List<MarketPriceHistory>> getHistory(
            @PathVariable String symbol,
            @RequestParam(
                    defaultValue = "1M"
            ) String period, @RequestParam(required = false) String instrumentKey
    ) {

        List<MarketPriceHistory> history =
                instrumentKey == null ? marketDataService.getHistory(symbol, period)
                        : marketDataService.getHistory(catalog.resolve(instrumentKey), period);

        return ResponseEntity.ok(history);
    }
}
