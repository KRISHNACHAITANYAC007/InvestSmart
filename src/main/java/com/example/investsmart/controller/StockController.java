package com.example.investsmart.controller;

import com.example.investsmart.entity.Stock;
import com.example.investsmart.service.StockService;
import com.example.investsmart.dto.ImportStockRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stocks")
public class StockController {

    private final StockService stockService;
    private final com.example.investsmart.security.SecurityAccounts accounts;
    private final com.example.investsmart.security.SecurityEvents events;

    public StockController(StockService stockService,com.example.investsmart.security.SecurityAccounts accounts,com.example.investsmart.security.SecurityEvents events) {
        this.accounts=accounts;this.events=events;
        this.stockService = stockService;
    }

    @GetMapping
    public ResponseEntity<List<Stock>> getStocks() {

        List<Stock> stocks = stockService.getStocks();

        return ResponseEntity.ok(stocks);
    }

    @PostMapping("/import")
    public ResponseEntity<Stock> importStock(
            @Valid @RequestBody ImportStockRequest request) {
        Stock stock = stockService.importNseStock(
                request.getInstrumentKey(),
                request.getSymbol()
        );
        return ResponseEntity.ok(stock);
    }

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Stock> createStock(
            @RequestBody Stock stock,java.security.Principal principal,jakarta.servlet.http.HttpServletRequest request,
            @RequestParam @jakarta.validation.constraints.Size(min=5,max=1000) String reason) {
        accounts.requireRecent(request);
        if(stock.getId()!=null)throw new com.example.investsmart.exception.BadRequestException("New stocks cannot supply an existing ID.");

        Stock createdStock =
                stockService.createStock(stock);
        events.audit(accounts.user(principal.getName()).getId(),null,"CREATE_STOCK",reason);

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
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Stock> updateStock(
            @PathVariable Long id,
            @RequestBody Stock stock,java.security.Principal principal,jakarta.servlet.http.HttpServletRequest request,
            @RequestParam @jakarta.validation.constraints.Size(min=5,max=1000) String reason) {
        accounts.requireRecent(request);

        Stock updatedStock =
                stockService.updateStock(id, stock);
        events.audit(accounts.user(principal.getName()).getId(),null,"UPDATE_STOCK","Stock "+id+": "+reason);

        return ResponseEntity.ok(updatedStock);
    }

    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> deleteStock(
            @PathVariable Long id,java.security.Principal principal,jakarta.servlet.http.HttpServletRequest request,
            @RequestParam @jakarta.validation.constraints.Size(min=5,max=1000) String reason) {
        accounts.requireRecent(request);

        stockService.deleteStock(id);
        events.audit(accounts.user(principal.getName()).getId(),null,"DELETE_STOCK","Stock "+id+": "+reason);

        return ResponseEntity.noContent().build();
    }
}
