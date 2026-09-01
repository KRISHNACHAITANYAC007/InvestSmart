package com.example.investsmart.service;

import com.example.investsmart.dto.WatchlistResponse;
import com.example.investsmart.entity.Stock;
import com.example.investsmart.entity.User;
import com.example.investsmart.entity.Watchlist;
import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.exception.ResourceNotFoundException;
import com.example.investsmart.repository.StockRepository;
import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.repository.WatchlistRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;

    public WatchlistService(
            WatchlistRepository watchlistRepository,
            UserRepository userRepository,
            StockRepository stockRepository) {

        this.watchlistRepository = watchlistRepository;
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
    }

    public Watchlist addToWatchlist(
            String email,
            Long stockId) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        Stock stock = stockRepository
                .findById(stockId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock not found"
                        ));

        List<Watchlist> existing =
                watchlistRepository.findByUserId(user.getId());

        boolean alreadyExists = existing.stream()
                .anyMatch(w ->
                        w.getStock().getId().equals(stockId)
                );

        if (alreadyExists) {
            throw new BadRequestException(
                    "Stock already exists in watchlist"
            );
        }

        Watchlist watchlist = new Watchlist();

        watchlist.setUser(user);
        watchlist.setStock(stock);

        return watchlistRepository.save(watchlist);
    }

    public List<WatchlistResponse> getUserWatchlist(
            String email) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        List<Watchlist> watchlists =
                watchlistRepository.findByUserId(
                        user.getId()
                );

        List<WatchlistResponse> responses =
                new ArrayList<>();

        for (Watchlist watchlist : watchlists) {

            Stock stock = watchlist.getStock();

            WatchlistResponse response =
                    new WatchlistResponse(
                            watchlist.getId(),
                            stock.getId(),
                            stock.getSymbol(),
                            stock.getCompanyName(),
                            stock.getCurrentPrice(),
                            stock.getDayHigh(),
                            stock.getDayLow(),
                            stock.getPreviousClose()
                    );

            responses.add(response);
        }

        return responses;
    }

    public void removeFromWatchlist(
            String email,
            Long stockId) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        Watchlist watchlist =
                watchlistRepository
                        .findByUserIdAndStockId(
                                user.getId(),
                                stockId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Stock not found in watchlist"
                                )
                        );

        watchlistRepository.delete(watchlist);
    }
}