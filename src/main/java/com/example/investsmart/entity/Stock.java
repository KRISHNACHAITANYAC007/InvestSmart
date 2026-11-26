package com.example.investsmart.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
public class Stock{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    @Column(unique = true)
    private String instrumentKey;
    private String exchange;
    private String isin;
    private String companyName;
    private String sector;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private LocalDateTime marketDataUpdatedAt;

    public Stock(){}

    public Long getId()
    {
        return id;
    }
    public void setId(Long id)
    {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getInstrumentKey() {
        return instrumentKey;
    }

    public void setInstrumentKey(String instrumentKey) {
        this.instrumentKey = instrumentKey;
    }

    public String getExchange() {
        if (exchange != null) return exchange;
        return instrumentKey != null && instrumentKey.startsWith("BSE_") ? "BSE" : "NSE";
    }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public String getIsin() {
        return isin != null ? isin : instrumentKey != null && instrumentKey.startsWith("NSE_EQ|")
                ? instrumentKey.substring(7) : null;
    }
    public void setIsin(String isin) { this.isin = isin; }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(BigDecimal previousClose) {
        this.previousClose = previousClose;
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

    public LocalDateTime getMarketDataUpdatedAt() { return marketDataUpdatedAt; }

    public void setMarketDataUpdatedAt(LocalDateTime marketDataUpdatedAt) {
        this.marketDataUpdatedAt = marketDataUpdatedAt;
    }

}
