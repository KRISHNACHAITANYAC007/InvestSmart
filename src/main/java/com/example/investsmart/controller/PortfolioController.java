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
    private final com.example.investsmart.security.SecurityAccounts accounts;

    public PortfolioController(PortfolioService portfolioService,com.example.investsmart.security.SecurityAccounts accounts) {
        this.accounts=accounts;
        this.portfolioService = portfolioService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<PortfolioResponse> getPortfolio(
            @PathVariable Long userId,Authentication authentication,jakarta.servlet.http.HttpServletRequest request) {
        accounts.requireOwner(userId,authentication.getName(),request);

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
