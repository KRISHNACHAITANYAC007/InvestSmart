package com.example.investsmart.repository;

import com.example.investsmart.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findBySymbolIgnoreCaseAndInstrumentKeyIsNull(String symbol);

    Optional<Stock> findByInstrumentKey(String instrumentKey);

    Optional<Stock> findBySymbolIgnoreCase(String symbol);
}
