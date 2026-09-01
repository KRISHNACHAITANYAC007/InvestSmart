package com.example.investsmart.dto;

import java.math.BigDecimal;

public class WalletResponse {

    private BigDecimal balance;

    public WalletResponse() {
    }

    public WalletResponse(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}