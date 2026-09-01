package com.example.investsmart.controller;

import com.example.investsmart.dto.PortfolioResponse;
import com.example.investsmart.service.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<PortfolioResponse> getPortfolio(
            @PathVariable Long userId) {

        PortfolioResponse portfolio =
                portfolioService.getPortfolio(userId);

        return ResponseEntity.ok(portfolio);
    }

    @GetMapping("/me")
    public ResponseEntity<PortfolioResponse> getMyPortfolio(
            Authentication authentication) {

        String email = authentication.getName();

        PortfolioResponse portfolio =
                portfolioService.getPortfolioByEmail(email);

        return ResponseEntity.ok(portfolio);
    }
}