package com.example.investsmart.market;

import java.time.LocalDateTime;
import java.util.List;

public class QuoteRefreshResponse {
    private final int updatedCount;
    private final LocalDateTime asOf;
    private final List<String> staleSymbols;

    public QuoteRefreshResponse(int updatedCount, LocalDateTime asOf, List<String> staleSymbols) {
        this.updatedCount = updatedCount;
        this.asOf = asOf;
        this.staleSymbols = List.copyOf(staleSymbols);
    }

    public int getUpdatedCount() { return updatedCount; }
    public LocalDateTime getAsOf() { return asOf; }
    public List<String> getStaleSymbols() { return staleSymbols; }
}
