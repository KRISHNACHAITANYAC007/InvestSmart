package com.example.investsmart.dto;

import java.math.BigDecimal;

public class WatchlistResponse {

    private Long id;
    private Long stockId;
    private String symbol;
    private String companyName;
    private BigDecimal currentPrice;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private BigDecimal previousClose;

    public WatchlistResponse() {
    }

    public WatchlistResponse(
            Long id,
            Long stockId,
            String symbol,
            String companyName,
            BigDecimal currentPrice,
            BigDecimal dayHigh,
            BigDecimal dayLow,
            BigDecimal previousClose) {

        this.id = id;
        this.stockId = stockId;
        this.symbol = symbol;
        this.companyName = companyName;
        this.currentPrice = currentPrice;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;
        this.previousClose = previousClose;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getDayHigh() {
        return dayHigh;
    }

    public void setDayHigh(BigDecimal dayHigh) {
        this.dayHigh = dayHigh;
    }

    public BigDecimal getDayLow() {
        return dayLow;
    }

    public void setDayLow(BigDecimal dayLow) {
        this.dayLow = dayLow;
    }

    public BigDecimal getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(BigDecimal previousClose) {
        this.previousClose = previousClose;
    }
}