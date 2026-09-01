package com.example.investsmart.dto;

import java.math.BigDecimal;
import java.util.List;

public class PortfolioResponse {

    private BigDecimal balance;
    private BigDecimal holdingsValue;
    private BigDecimal totalValue;
    private List<HoldingResponse> holdings;

    public PortfolioResponse() {
    }

    public PortfolioResponse(
            BigDecimal balance,
            BigDecimal holdingsValue,
            BigDecimal totalValue,
            List<HoldingResponse> holdings) {

        this.balance = balance;
        this.holdingsValue = holdingsValue;
        this.totalValue = totalValue;
        this.holdings = holdings;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getHoldingsValue() {
        return holdingsValue;
    }

    public void setHoldingsValue(BigDecimal holdingsValue) {
        this.holdingsValue = holdingsValue;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public List<HoldingResponse> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<HoldingResponse> holdings) {
        this.holdings = holdings;
    }
}