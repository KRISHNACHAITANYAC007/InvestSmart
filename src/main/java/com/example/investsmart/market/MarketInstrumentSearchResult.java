package com.example.investsmart.market;

import java.util.List;

public class MarketInstrumentSearchResult {

    private final List<MarketInstrument> instruments;
    private final int page;
    private final int totalPages;

    public MarketInstrumentSearchResult(
            List<MarketInstrument> instruments,
            int page,
            int totalPages) {
        this.instruments = instruments;
        this.page = page;
        this.totalPages = totalPages;
    }

    public List<MarketInstrument> getInstruments() {
        return instruments;
    }

    public int getPage() {
        return page;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
