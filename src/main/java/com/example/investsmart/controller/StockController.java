package com.example.investsmart.controller;

import com.example.investsmart.entity.Stock;
import com.example.investsmart.service.StockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public ResponseEntity<List<Stock>> getStocks() {

        List<Stock> stocks = stockService.getStocks();

        return ResponseEntity.ok(stocks);
    }

    @PostMapping
    public ResponseEntity<Stock> createStock(
            @RequestBody Stock stock) {

        Stock createdStock =
                stockService.createStock(stock);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdStock);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Stock> getStockById(
            @PathVariable Long id) {

        Stock stock = stockService.getStockById(id);

        return ResponseEntity.ok(stock);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Stock> updateStock(
            @PathVariable Long id,
            @RequestBody Stock stock) {

        Stock updatedStock =
                stockService.updateStock(id, stock);

        return ResponseEntity.ok(updatedStock);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(
            @PathVariable Long id) {

        stockService.deleteStock(id);

        return ResponseEntity.noContent().build();
    }
}