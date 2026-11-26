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

    @org.springframework.transaction.annotation.Transactional
    public Watchlist addToWatchlist(
            String email,
            Long stockId) {

        User user = userRepository
                .findByEmailForUpdate(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        user.requireActive();
        Stock stock = stockRepository
                .findById(stockId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock not found"
                        ));

        List<Watchlist> existing =
                watchlistRepository.findByUserId(user.getId());

        List<Watchlist> matches = existing.stream().filter(w ->
                w.getStock().getId().equals(stockId) || stock.getIsin() != null
                        && stock.getIsin().equals(w.getStock().getIsin())).toList();
        if (!matches.isEmpty()) {
            Watchlist saved = matches.get(0);
            saved.setStock(stock);
            if (matches.size() > 1) watchlistRepository.deleteAll(matches.subList(1, matches.size()));
            return watchlistRepository.save(saved);
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
            response.setInstrumentKey(stock.getInstrumentKey());
            response.setExchange(stock.getExchange());
            response.setIsin(stock.getIsin());
            response.setMarketDataUpdatedAt(stock.getMarketDataUpdatedAt());
        }

        return responses;
    }

    @org.springframework.transaction.annotation.Transactional
    public void removeFromWatchlist(
            String email,
            Long stockId) {

        User user = userRepository
                .findByEmailForUpdate(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        user.requireActive();
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
