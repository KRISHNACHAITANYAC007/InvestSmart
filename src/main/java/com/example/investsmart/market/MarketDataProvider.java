package com.example.investsmart.market;

import com.example.investsmart.entity.MarketPriceHistory;

import java.util.List;

public interface MarketDataProvider {

    MarketQuote getQuote(String symbol);

    default MarketQuote getQuote(MarketInstrument instrument) {
        return getQuote(instrument.getSymbol());
    }

    List<MarketQuote> getQuotes();

    String getExchangeStatus(String exchange);

    default List<MarketQuote> getQuotes(List<MarketInstrument> instruments) {
        return instruments.stream().map(this::getQuote).toList();
    }

    MarketInstrumentSearchResult searchInstruments(String query, int page);

    List<MarketQuote> getIndices();

    List<MarketPriceHistory> getHistory(String symbol);

    default List<MarketPriceHistory> getHistory(MarketInstrument instrument, String period) {
        return getHistory(instrument.getSymbol(), period);
    }

    default List<MarketPriceHistory> getHistory(
            String symbol,
            String period
    ) {
        return getHistory(symbol);
    }
}
