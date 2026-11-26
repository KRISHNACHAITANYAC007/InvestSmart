package com.example.investsmart.dto;

import java.math.BigDecimal;

public class HoldingResponse {
    private String instrumentKey;
    private String exchange;
    private String isin;
    public String getInstrumentKey() { return instrumentKey; }
    public void setInstrumentKey(String value) { instrumentKey = value; }
    public String getExchange() { return exchange; }
    public void setExchange(String value) { exchange = value; }
    public String getIsin() { return isin; }
    public void setIsin(String value) { isin = value; }

    private Long stockId;
    private String symbol;
    private String companyName;
    private String sector;
    private Integer quantity;
    private BigDecimal currentPrice;
    private BigDecimal currentValue;
    private BigDecimal averageBuyPrice;
    private BigDecimal investedAmount;
    private BigDecimal unrealizedPnl;
    private BigDecimal unrealizedPnlPercent;
    private BigDecimal allocationPercent;

    public HoldingResponse() {
    }

    public HoldingResponse(
            Long stockId,
            String symbol,
            String companyName,
            String sector,
            Integer quantity,
            BigDecimal currentPrice,
            BigDecimal currentValue,
            BigDecimal averageBuyPrice,
            BigDecimal investedAmount,
            BigDecimal unrealizedPnl,
            BigDecimal unrealizedPnlPercent,
            BigDecimal allocationPercent) {

        this.stockId = stockId;
        this.symbol = symbol;
        this.companyName = companyName;
        this.sector = sector;
        this.quantity = quantity;
        this.currentPrice = currentPrice;
        this.currentValue = currentValue;
        this.averageBuyPrice = averageBuyPrice;
        this.investedAmount = investedAmount;
        this.unrealizedPnl = unrealizedPnl;
        this.unrealizedPnlPercent = unrealizedPnlPercent;
        this.allocationPercent = allocationPercent;
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

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getAverageBuyPrice() {
        return averageBuyPrice;
    }

    public void setAverageBuyPrice(BigDecimal averageBuyPrice) {
        this.averageBuyPrice = averageBuyPrice;
    }

    public BigDecimal getInvestedAmount() {
        return investedAmount;
    }

    public void setInvestedAmount(BigDecimal investedAmount) {
        this.investedAmount = investedAmount;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }

    public BigDecimal getUnrealizedPnlPercent() {
        return unrealizedPnlPercent;
    }

    public void setUnrealizedPnlPercent(BigDecimal unrealizedPnlPercent) {
        this.unrealizedPnlPercent = unrealizedPnlPercent;
    }

    public BigDecimal getAllocationPercent() {
        return allocationPercent;
    }

    public void setAllocationPercent(BigDecimal allocationPercent) {
        this.allocationPercent = allocationPercent;
    }
}
