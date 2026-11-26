package com.example.investsmart.market;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketQuote {

    private String symbol;
    private String instrumentKey;
    private String exchange;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private LocalDateTime lastUpdated;

    public MarketQuote(
            String symbol,
            BigDecimal currentPrice,
            BigDecimal previousClose,
            LocalDateTime lastUpdated) {

        this(symbol, currentPrice, previousClose, null, null, lastUpdated);
    }

    public MarketQuote(
            String symbol,
            BigDecimal currentPrice,
            BigDecimal previousClose,
            BigDecimal dayHigh,
            BigDecimal dayLow,
            LocalDateTime lastUpdated) {

        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.previousClose = previousClose;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;
        this.lastUpdated = lastUpdated;
    }

    public String getSymbol() {
        return symbol;
    }

    public MarketQuote withInstrument(MarketInstrument instrument) {
        this.instrumentKey = instrument.getInstrumentKey();
        this.exchange = instrument.getExchange();
        return this;
    }
    public String getInstrumentKey() { return instrumentKey; }
    public String getExchange() { return exchange; }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getPreviousClose() {
        return previousClose;
    }

    public BigDecimal getDayHigh() {
        return dayHigh;
    }

    public BigDecimal getDayLow() {
        return dayLow;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
}
