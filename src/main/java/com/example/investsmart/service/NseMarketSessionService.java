package com.example.investsmart.service;

import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.exception.MarketStatusUnavailableException;
import com.example.investsmart.market.MarketDataService;
import org.springframework.stereotype.Service;

@Service
public class NseMarketSessionService {
    private final MarketDataService marketDataService;

    public NseMarketSessionService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public void requireOpenSession() {
        requireOpenSession("NSE");
    }

    public void requireOpenSession(String exchange) {
        if (!"NSE".equals(exchange) && !"BSE".equals(exchange))
            throw new BadRequestException("Select a supported stock exchange.");
        final String status;
        try {
            status = marketDataService.getExchangeStatus(exchange);
        } catch (RuntimeException exception) {
            throw new MarketStatusUnavailableException(
                    "Unable to confirm " + exchange + " market status. Please try again shortly.", exception);
        }

        if (!"NORMAL_OPEN".equalsIgnoreCase(status == null ? "" : status.trim())) {
            throw new BadRequestException(
                    exchange + " market is closed. Buy and sell orders are accepted only during an active trading session.");
        }
    }
}
