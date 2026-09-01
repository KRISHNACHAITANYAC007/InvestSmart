package com.example.investsmart.service;

import com.example.investsmart.dto.HoldingResponse;
import com.example.investsmart.dto.PortfolioResponse;
import com.example.investsmart.entity.Holding;
import com.example.investsmart.entity.User;
import com.example.investsmart.repository.HoldingRepository;
import com.example.investsmart.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.example.investsmart.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PortfolioService {

    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;

    public PortfolioService(
            UserRepository userRepository,
            HoldingRepository holdingRepository) {

        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
    }

    public PortfolioResponse getPortfolio(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        List<Holding> holdings =
                holdingRepository.findByUserId(userId);

        BigDecimal holdingsValue = BigDecimal.ZERO;

        List<HoldingResponse> holdingResponses =
                new java.util.ArrayList<>();

        for (Holding holding : holdings) {

            BigDecimal currentPrice =
                    holding.getStock().getCurrentPrice();

            BigDecimal stockValue =
                    currentPrice.multiply(
                            BigDecimal.valueOf(
                                    holding.getQuantity()
                            )
                    );

            holdingsValue =
                    holdingsValue.add(stockValue);

            HoldingResponse holdingResponse =
                    new HoldingResponse(
                            holding.getStock().getId(),
                            holding.getStock().getSymbol(),
                            holding.getStock().getCompanyName(),
                            holding.getQuantity(),
                            currentPrice,
                            stockValue
                    );

            holdingResponses.add(holdingResponse);
        }

        BigDecimal balance =
                user.getVirtualBalance();

        BigDecimal totalValue =
                balance.add(holdingsValue);

        return new PortfolioResponse(
                balance,
                holdingsValue,
                totalValue,
                holdingResponses
        );
    }

    public PortfolioResponse getPortfolioByEmail(String email) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        return getPortfolio(user.getId());
    }
}