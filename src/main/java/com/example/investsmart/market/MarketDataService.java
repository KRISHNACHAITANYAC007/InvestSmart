package com.example.investsmart.market;

import org.springframework.stereotype.Service;
import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.entity.MarketPriceHistory;
import com.example.investsmart.repository.MarketPriceHistoryRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class MarketDataService {

    static final List<MarketInstrument> HEATMAP_STOCKS = List.of(
            new MarketInstrument("NSE_EQ|INE002A01018", "RELIANCE", "Reliance Industries", "NSE"),
            new MarketInstrument("NSE_EQ|INE040A01034", "HDFCBANK", "HDFC Bank", "NSE"),
            new MarketInstrument("NSE_EQ|INE090A01021", "ICICIBANK", "ICICI Bank", "NSE"),
            new MarketInstrument("NSE_EQ|INE009A01021", "INFY", "Infosys", "NSE"),
            new MarketInstrument("NSE_EQ|INE467B01029", "TCS", "TCS", "NSE"),
            new MarketInstrument("NSE_EQ|INE397D01024", "BHARTIARTL", "Bharti Airtel", "NSE"),
            new MarketInstrument("NSE_EQ|INE062A01020", "SBIN", "State Bank of India", "NSE"),
            new MarketInstrument("NSE_EQ|INE154A01025", "ITC", "ITC", "NSE"),
            new MarketInstrument("NSE_EQ|INE018A01030", "LT", "Larsen & Toubro", "NSE"),
            new MarketInstrument("NSE_EQ|INE296A01032", "BAJFINANCE", "Bajaj Finance", "NSE")
    );

    private final java.time.Clock clock;
    private final Object[] historyLocks=java.util.stream.IntStream.range(0,32).mapToObj(i->new Object()).toArray();
    private final java.util.Map<String,java.time.Instant> historyFetched=java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<>() { protected boolean removeEldestEntry(java.util.Map.Entry<String,java.time.Instant> eldest){return size()>256;} });
    private record HistorySnapshot(java.time.Instant at,List<MarketPriceHistory> points){}
    private final java.util.Map<String,HistorySnapshot> longHistory=java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<>(){protected boolean removeEldestEntry(java.util.Map.Entry<String,HistorySnapshot> entry){return size()>64;}});
    private final MarketDataProvider marketDataProvider;
    private final MarketPriceHistoryRepository marketPriceHistoryRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public MarketDataService(
            MarketDataProvider marketDataProvider,
            MarketPriceHistoryRepository marketPriceHistoryRepository
    ) {
        this(marketDataProvider,marketPriceHistoryRepository,java.time.Clock.systemUTC());
    }
    MarketDataService(MarketDataProvider marketDataProvider,MarketPriceHistoryRepository marketPriceHistoryRepository,java.time.Clock clock) {
        this.clock=clock==null?java.time.Clock.systemUTC():clock;
        this.marketDataProvider = marketDataProvider;
        this.marketPriceHistoryRepository =
                marketPriceHistoryRepository;
    }

    public MarketQuote getQuote(String symbol) {

        return marketDataProvider.getQuote(symbol);
    }

    public MarketQuote getQuote(MarketInstrument instrument) {
        return marketDataProvider.getQuote(instrument);
    }

    public List<MarketPriceHistory> getHistory(String symbol) {

        return getHistory(symbol, "1M");
    }

    public List<MarketPriceHistory> getHistory(
            String symbol,
            String period
    ) {
        return getHistory(symbol, period, null);
    }

    public List<MarketPriceHistory> getHistory(MarketInstrument instrument, String period) {
        return getHistory(instrument.getSymbol(), period, instrument);
    }

    private List<MarketPriceHistory> getHistory(String symbol, String period, MarketInstrument instrument) {
        String key=instrument==null?symbol.toUpperCase():instrument.getInstrumentKey();
        synchronized(historyLocks[Math.floorMod(key.hashCode(),historyLocks.length)]) { return loadHistory(symbol,period,instrument); }
    }
    private List<MarketPriceHistory> loadHistory(String symbol, String period, MarketInstrument instrument) {

        symbol = symbol.toUpperCase();
        period = period.toUpperCase();
        // Existing symbol-only NSE/index history remains readable. Other listings have their own namespace.
        String cacheKey = instrument == null ? symbol : instrument.getInstrumentKey();

        if (!period.equals("1D")
                && !period.equals("1W")
                && !period.equals("1M")
                && !period.equals("6M")
                && !period.equals("1Y") && !period.equals("5Y") && !period.equals("ALL")) {

            throw new IllegalArgumentException(
                    "Invalid period. Use 1D, 1W, 1M, 6M, 1Y, 5Y or ALL."
            );
        }

        /*
         * 1D uses intraday candles.
         *
         * We don't store these in the daily history table
         * because the table currently stores historical daily
         * stock data.
         */
        if (period.equals("1D") || period.equals("1W")) {

            return instrument == null ? marketDataProvider.getHistory(symbol, period)
                    : marketDataProvider.getHistory(instrument, period);
        }

        ZoneId indiaZone =
                ZoneId.of("Asia/Kolkata");

        LocalDate today =
                LocalDate.now(clock.withZone(indiaZone));

        if(period.equals("5Y")||period.equals("ALL")){
            String key=cacheKey+":"+period+":"+today;
            var cached=longHistory.get(key);if(cached!=null&&cached.at().plusSeconds(60).isAfter(clock.instant()))return cached.points();
            // Publish only after every provider window succeeds, including empty pre-listing windows.
            var points=instrument==null?marketDataProvider.getHistory(symbol,period):marketDataProvider.getHistory(instrument,period);
            var sorted=new java.util.TreeMap<LocalDateTime,MarketPriceHistory>();
            LocalDate start=period.equals("5Y")?today.minusYears(5):LocalDate.of(2000,1,1);
            for(var point:points)if(point.getTimestamp()!=null&&!point.getTimestamp().toLocalDate().isBefore(start)&&!point.getTimestamp().toLocalDate().isAfter(today))sorted.put(point.getTimestamp(),point);
            var result=List.copyOf(sorted.values());longHistory.put(key,new HistorySnapshot(clock.instant(),result));return result;
        }

        LocalDate fromDate;

        switch (period) {

            case "1M":
                fromDate = today.minusMonths(1);
                break;

            case "6M":
                fromDate = today.minusMonths(6);
                break;

            case "1Y":
                fromDate = today.minusYears(1);
                break;

            default:
                throw new IllegalArgumentException(
                        "Invalid period: " + period
                );
        }

        LocalDateTime fromDateTime =
                fromDate.atStartOfDay();

        LocalDateTime toDateTime =
                today
                        .plusDays(1)
                        .atStartOfDay()
                        .minusNanos(1);

        /*
         * First try the database.
         */
        List<MarketPriceHistory> history =
                marketPriceHistoryRepository
                        .findBySymbolAndTimestampBetweenOrderByTimestampAsc(
                                cacheKey,
                                fromDateTime,
                                toDateTime
                        );
        if (history.isEmpty() && instrument != null && "NSE".equals(instrument.getExchange())) {
            history = marketPriceHistoryRepository.findBySymbolAndTimestampBetweenOrderByTimestampAsc(
                    symbol, fromDateTime, toDateTime);
        }

        /*
         * Check whether the cached data actually covers
         * the requested period.
         *
         * We allow a few calendar days because weekends
         * and market holidays don't have candles.
         */
        boolean cacheIsValid = false;

        if (!history.isEmpty()) {

            LocalDate firstDate =
                    history.get(0)
                            .getTimestamp()
                            .toLocalDate();

            LocalDate lastDate =
                    history.get(history.size() - 1)
                            .getTimestamp()
                            .toLocalDate();

            boolean startsEarlyEnough =
                    !firstDate.isAfter(
                            fromDate.plusDays(5)
                    );

            boolean endsRecentlyEnough =
                    !lastDate.isBefore(
                            today.minusDays(5)
                    );

            cacheIsValid =
                    startsEarlyEnough
                            && endsRecentlyEnough;
        }

        /*
         * If database already has the required range,
         * return it without calling Upstox.
         */
        java.time.Instant fetched=historyFetched.get(cacheKey+":"+period);
        if (cacheIsValid && fetched!=null && fetched.plusSeconds(60).isAfter(clock.instant())) {

            return history;
        }

        /*
         * Database does not have enough data.
         * Fetch the requested range from Upstox.
         */
        history = instrument == null ? marketDataProvider.getHistory(symbol, period)
                : marketDataProvider.getHistory(instrument, period);

        /*
         * Replace the existing daily cache for this symbol
         * with the newly fetched complete range.
         *
         * This prevents duplicate candles when we move from
         * 1M -> 6M -> 1Y.
         */
        if (!history.isEmpty() && coversRequestedPeriod(history, fromDate, today)) {

            List<MarketPriceHistory> existing =
                    marketPriceHistoryRepository
                            .findBySymbolOrderByTimestampAsc(
                                    cacheKey
                            );

            var ids=new java.util.HashMap<LocalDateTime,Long>();
            for(var item:existing)ids.put(item.getTimestamp(),item.getId());
            // Update matching candles; a shorter request must not delete older cached history.
            history.forEach(item -> {item.setSymbol(cacheKey);item.setId(ids.get(item.getTimestamp()));});
            history =
                    marketPriceHistoryRepository
                            .saveAll(history);
        }

        if (!history.isEmpty()) historyFetched.put(cacheKey+":"+period,clock.instant());
        return history;
    }

    private boolean coversRequestedPeriod(List<MarketPriceHistory> history,
                                          LocalDate fromDate, LocalDate today) {
        LocalDate firstDate = history.get(0).getTimestamp().toLocalDate();
        LocalDate lastDate = history.get(history.size() - 1).getTimestamp().toLocalDate();
        return !firstDate.isAfter(fromDate.plusDays(5))
                && !lastDate.isBefore(today.minusDays(5));
    }

    public List<MarketQuote> getQuotes() {

        return marketDataProvider.getQuotes();
    }

    public String getExchangeStatus(String exchange) {
        return marketDataProvider.getExchangeStatus(exchange);
    }

    public List<MarketQuote> getQuotes(List<MarketInstrument> instruments) {
        return marketDataProvider.getQuotes(instruments);
    }

    public MarketInstrumentSearchResult searchInstruments(String query, int page) {
        if (query == null || query.trim().length() < 2) {
            throw new BadRequestException(
                    "Enter at least 2 characters to search stocks."
            );
        }
        if (query.trim().length() > 50) {
            throw new BadRequestException(
                    "Stock search must be 50 characters or fewer."
            );
        }
        if (page < 1) {
            throw new BadRequestException("Search page must be 1 or greater.");
        }
        return marketDataProvider.searchInstruments(query.trim(), page);
    }

    public List<MarketQuote> getIndices() {

        return marketDataProvider.getIndices();
    }

    public List<MarketQuote> getHeatmapQuotes() {
        return marketDataProvider.getQuotes(HEATMAP_STOCKS);
    }
}
