package com.example.investsmart.dto;

import java.math.BigDecimal;

public class HoldingResponse {

    private Long stockId;
    private String symbol;
    private String companyName;
    private Integer quantity;
    private BigDecimal currentPrice;
    private BigDecimal currentValue;

    public HoldingResponse() {
    }

    public HoldingResponse(
            Long stockId,
            String symbol,
            String companyName,
            Integer quantity,
            BigDecimal currentPrice,
            BigDecimal currentValue) {

        this.stockId = stockId;
        this.symbol = symbol;
        this.companyName = companyName;
        this.quantity = quantity;
        this.currentPrice = currentPrice;
        this.currentValue = currentValue;
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
}