package com.example.investsmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ImportStockRequest {

    @NotBlank(message = "Instrument key is required")
    @Pattern(
            regexp = "^(NSE_EQ|BSE_EQ)\\|[A-Za-z0-9]+$",
            message = "Select a supported NSE or BSE equity instrument"
    )
    private String instrumentKey;

    @NotBlank(message = "Stock symbol is required")
    private String symbol;

    public ImportStockRequest() {
    }

    public String getInstrumentKey() {
        return instrumentKey;
    }

    public void setInstrumentKey(String instrumentKey) {
        this.instrumentKey = instrumentKey;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
