package com.example.investsmart.market;

import com.example.investsmart.entity.MarketPriceHistory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Component
public class DemoMarketDataProvider implements MarketDataProvider {
    private static final Map<String, BigDecimal[]> DEMO_FEATURED_QUOTES = Map.ofEntries(
            Map.entry("HDFCBANK", new BigDecimal[]{new BigDecimal("1712.40"), new BigDecimal("1698.20")}),
            Map.entry("ICICIBANK", new BigDecimal[]{new BigDecimal("1284.60"), new BigDecimal("1271.80")}),
            Map.entry("BHARTIARTL", new BigDecimal[]{new BigDecimal("1930.00"), new BigDecimal("1944.50")}),
            Map.entry("SBIN", new BigDecimal[]{new BigDecimal("842.15"), new BigDecimal("835.70")}),
            Map.entry("ITC", new BigDecimal[]{new BigDecimal("417.35"), new BigDecimal("421.10")}),
            Map.entry("LT", new BigDecimal[]{new BigDecimal("3612.00"), new BigDecimal("3575.50")}),
            Map.entry("BAJFINANCE", new BigDecimal[]{new BigDecimal("9780.00"), new BigDecimal("9662.00")})
    );

    @Override public List<MarketPriceHistory> getHistory(String symbol,String period){
        if("5Y".equalsIgnoreCase(period)||"ALL".equalsIgnoreCase(period))throw new com.example.investsmart.exception.MarketStatusUnavailableException("Verified long-range history is unavailable from the demo provider.",null);
        return getHistory(symbol);
    }
    @Override
    public MarketQuote getQuote(String symbol) {

        if (symbol.equalsIgnoreCase("NIFTY 50")
                || symbol.equalsIgnoreCase("SENSEX")
                || symbol.equalsIgnoreCase("NIFTY BANK")
                || symbol.equalsIgnoreCase("NIFTY IT")) {
            return getIndices().stream()
                    .filter(index -> index.getSymbol().equalsIgnoreCase(symbol))
                    .findFirst().orElseThrow();
        }

        if (symbol.equals("INFY")) {
            return new MarketQuote(
                    "INFY",
                    new BigDecimal("1525.40"),
                    new BigDecimal("1508.20"),
                    LocalDateTime.now()
            );
        }

        if (symbol.equals("TCS")) {
            return new MarketQuote(
                    "TCS",
                    new BigDecimal("3210.75"),
                    new BigDecimal("3185.10"),
                    LocalDateTime.now()
            );
        }

        if (symbol.equals("RELIANCE")) {
            return new MarketQuote(
                    "RELIANCE",
                    new BigDecimal("1400.75"),
                    new BigDecimal("1447.10"),
                    LocalDateTime.now()
            );
        }

        BigDecimal[] featuredQuote = DEMO_FEATURED_QUOTES.get(symbol.toUpperCase());
        if (featuredQuote != null) {
            return new MarketQuote(symbol.toUpperCase(), featuredQuote[0], featuredQuote[1], LocalDateTime.now());
        }

        throw new RuntimeException("Stock not found");
    }

    @Override
    public List<MarketQuote> getQuotes() {

        List<MarketQuote> quotes = new ArrayList<>();

        quotes.add(getQuote("INFY"));
        quotes.add(getQuote("TCS"));
        quotes.add(getQuote("RELIANCE"));

        return quotes;
    }

    @Override
    public String getExchangeStatus(String exchange) {
        if (!"NSE".equalsIgnoreCase(exchange)) {
            throw new IllegalArgumentException("Unsupported exchange: " + exchange);
        }
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();
        boolean weekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
        boolean regularSession = !time.isBefore(LocalTime.of(9, 15))
                && time.isBefore(LocalTime.of(15, 30));
        return weekday && regularSession ? "NORMAL_OPEN" : "NORMAL_CLOSE";
    }

    @Override
    public MarketInstrumentSearchResult searchInstruments(String query, int page) {
        String normalizedQuery = query.trim().toUpperCase();
        List<MarketInstrument> results = List.of(
                new MarketInstrument(
                        "NSE_EQ|INE009A01021",
                        "INFY",
                        "Infosys Limited",
                        "NSE"
                ),
                new MarketInstrument(
                        "NSE_EQ|INE467B01029",
                        "TCS",
                        "Tata Consultancy Services Limited",
                        "NSE"
                ),
                new MarketInstrument(
                        "NSE_EQ|INE002A01018",
                        "RELIANCE",
                        "Reliance Industries Limited",
                        "NSE"
                )
        ).stream()
                .filter(instrument -> instrument.getSymbol()
                        .contains(normalizedQuery)
                        || instrument.getCompanyName()
                        .toUpperCase()
                        .contains(normalizedQuery))
                .toList();
        return new MarketInstrumentSearchResult(results, 1, 1);
    }
    @Override
    public List<MarketQuote> getIndices() {

        return List.of(
                new MarketQuote(
                        "NIFTY 50",
                        new BigDecimal("25000.00"),
                        new BigDecimal("24800.00"),
                        LocalDateTime.now()
                ),
                new MarketQuote(
                        "SENSEX",
                        new BigDecimal("82000.00"),
                        new BigDecimal("81500.00"),
                        LocalDateTime.now()
                ),
                new MarketQuote(
                        "NIFTY BANK",
                        new BigDecimal("52000.00"),
                        new BigDecimal("51750.00"),
                        LocalDateTime.now()
                ),
                new MarketQuote(
                        "NIFTY IT",
                        new BigDecimal("39000.00"),
                        new BigDecimal("38600.00"),
                        LocalDateTime.now()
                )
        );
    }
    @Override
    public List<MarketPriceHistory> getHistory(String symbol) {

        List<MarketPriceHistory> history = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        BigDecimal basePrice =
                getQuote(symbol).getCurrentPrice();

        for (int i = 30; i >= 0; i--) {

            MarketPriceHistory point =
                    new MarketPriceHistory();

            point.setSymbol(symbol);

            point.setPrice(
                    basePrice.subtract(
                            new BigDecimal(i * 2)
                    )
            );

            point.setTimestamp(
                    now.minusDays(i)
            );

            history.add(point);
        }

        return history;
    }
}
