package com.example.investsmart.market;

import com.example.investsmart.entity.Stock;
import com.example.investsmart.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class StockQuoteRefreshService {
    private static final int BATCH_SIZE = 500;
    private static final long FRESH_FOR_SECONDS = 60;
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    private final StockRepository stockRepository;
    private final MarketDataService marketDataService;

    public StockQuoteRefreshService(StockRepository stockRepository, MarketDataService marketDataService) {
        this.stockRepository = stockRepository;
        this.marketDataService = marketDataService;
    }

    @Transactional
    public QuoteRefreshResponse refreshAll(boolean force) {
        LocalDateTime now = LocalDateTime.now(INDIA);
        List<Stock> stocks = stockRepository.findAll();
        List<Stock> candidates = new ArrayList<>();
        List<String> stale = new ArrayList<>();
        for (Stock stock : stocks) {
            if (!force && stock.getMarketDataUpdatedAt() != null
                    && stock.getMarketDataUpdatedAt().isAfter(now.minusSeconds(FRESH_FOR_SECONDS))) continue;
            if (stock.getInstrumentKey() == null || stock.getInstrumentKey().isBlank()) {
                try {
                    MarketInstrument found = marketDataService.searchInstruments(stock.getSymbol(), 1)
                            .getInstruments().stream()
                            .filter(i -> i.getSymbol().equalsIgnoreCase(stock.getSymbol()))
                            .filter(i -> "NSE".equals(i.getExchange()))
                            .findFirst().orElse(null);
                    if (found == null) { stale.add(stock.getSymbol()); continue; }
                    stock.setInstrumentKey(found.getInstrumentKey());
                    stock.setExchange(found.getExchange());
                    stock.setIsin(found.getIsin());
                } catch (RuntimeException e) {
                    stale.add(stock.getSymbol());
                    continue;
                }
            }
            candidates.add(stock);
        }

        int updated = 0;
        for (int start = 0; start < candidates.size(); start += BATCH_SIZE) {
            List<Stock> batch = candidates.subList(start, Math.min(start + BATCH_SIZE, candidates.size()));
            List<MarketInstrument> instruments = batch.stream()
                    .map(s -> new MarketInstrument(s.getInstrumentKey(), s.getSymbol(), s.getCompanyName(), s.getExchange(), s.getIsin()))
                    .toList();
            Map<String, MarketQuote> quotes = new HashMap<>();
            try {
                for (MarketQuote quote : marketDataService.getQuotes(instruments)) {
                    if (quote.getInstrumentKey() != null) quotes.put(quote.getInstrumentKey(), quote);
                    // Legacy providers/tests may omit identity; accept only an unambiguous single listing.
                    else if (batch.stream().filter(s -> s.getSymbol().equalsIgnoreCase(quote.getSymbol())).count() == 1)
                        batch.stream().filter(s -> s.getSymbol().equalsIgnoreCase(quote.getSymbol()))
                                .filter(s -> "NSE".equals(s.getExchange()))
                                .forEach(s -> quotes.put(s.getInstrumentKey(), quote));
                }
            } catch (RuntimeException e) {
                batch.forEach(s -> stale.add(s.getSymbol()));
                continue;
            }
            for (Stock stock : batch) {
                MarketQuote quote = quotes.get(stock.getInstrumentKey());
                if (quote == null || quote.getCurrentPrice() == null || quote.getCurrentPrice().signum() <= 0) {
                    stale.add(stock.getSymbol());
                    continue;
                }
                stock.setCurrentPrice(quote.getCurrentPrice());
                if (quote.getPreviousClose() != null) stock.setPreviousClose(quote.getPreviousClose());
                if (quote.getDayHigh() != null) stock.setDayHigh(quote.getDayHigh());
                if (quote.getDayLow() != null) stock.setDayLow(quote.getDayLow());
                stock.setMarketDataUpdatedAt(quote.getLastUpdated() == null ? now : quote.getLastUpdated());
                updated++;
            }
        }
        stockRepository.saveAll(stocks);
        LocalDateTime asOf = stocks.stream().map(Stock::getMarketDataUpdatedAt)
                .filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
        return new QuoteRefreshResponse(updated, asOf, stale.stream().distinct().sorted().toList());
    }
}
