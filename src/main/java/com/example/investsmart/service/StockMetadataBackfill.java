package com.example.investsmart.service;

import com.example.investsmart.repository.StockRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Idempotently fills metadata from existing keys without replacing any stock or account records. */
@Component
public class StockMetadataBackfill {
    private final StockRepository stocks;
    public StockMetadataBackfill(StockRepository stocks) { this.stocks = stocks; }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfill() {
        stocks.findAll().forEach(stock -> {
            String key = stock.getInstrumentKey();
            if (key != null && key.startsWith("NSE_EQ|")) {
                stock.setExchange("NSE"); stock.setIsin(key.substring(7));
            } else if (key != null && key.startsWith("BSE_EQ|")) {
                stock.setExchange("BSE");
            }
        });
    }
}
