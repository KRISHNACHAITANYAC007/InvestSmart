package com.example.investsmart.dto;

import java.math.BigDecimal;

public class OrderResponse {

    private Long id;
    private String orderType;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private String status;

    private Long stockId;
    private String symbol;
    private String companyName;

    public OrderResponse() {
    }

    public OrderResponse(
            Long id,
            String orderType,
            Integer quantity,
            BigDecimal price,
            BigDecimal totalAmount,
            String status,
            Long stockId,
            String symbol,
            String companyName) {

        this.id = id;
        this.orderType = orderType;
        this.quantity = quantity;
        this.price = price;
        this.totalAmount = totalAmount;
        this.status = status;
        this.stockId = stockId;
        this.symbol = symbol;
        this.companyName = companyName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}