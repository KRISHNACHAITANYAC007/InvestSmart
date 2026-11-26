package com.example.investsmart.repository;

import com.example.investsmart.entity.MarketPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketPriceHistoryRepository
        extends JpaRepository<MarketPriceHistory, Long> {

    List<MarketPriceHistory> findBySymbolOrderByTimestampAsc(
            String symbol
    );

    List<MarketPriceHistory>
    findBySymbolAndTimestampBetweenOrderByTimestampAsc(
            String symbol,
            LocalDateTime from,
            LocalDateTime to
    );
}