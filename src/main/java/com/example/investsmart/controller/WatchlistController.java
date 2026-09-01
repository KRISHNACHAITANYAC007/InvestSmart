package com.example.investsmart.controller;

import com.example.investsmart.dto.WatchlistResponse;
import com.example.investsmart.entity.Watchlist;
import com.example.investsmart.service.WatchlistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(
            WatchlistService watchlistService) {

        this.watchlistService = watchlistService;
    }

    @PostMapping
    public ResponseEntity<Watchlist> addToWatchlist(
            Authentication authentication,
            @RequestParam Long stockId) {

        String email = authentication.getName();

        Watchlist watchlist =
                watchlistService.addToWatchlist(
                        email,
                        stockId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(watchlist);
    }

    @GetMapping("/me")
    public ResponseEntity<List<WatchlistResponse>> getMyWatchlist(
            Authentication authentication) {

        String email = authentication.getName();

        List<WatchlistResponse> watchlist =
                watchlistService.getUserWatchlist(email);

        return ResponseEntity.ok(watchlist);
    }

    @DeleteMapping("/me/{stockId}")
    public ResponseEntity<Void> removeFromWatchlist(
            Authentication authentication,
            @PathVariable Long stockId) {

        String email = authentication.getName();

        watchlistService.removeFromWatchlist(
                email,
                stockId
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}