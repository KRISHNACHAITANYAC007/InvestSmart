package com.example.investsmart.controller;

import com.example.investsmart.dto.WalletRequest;
import com.example.investsmart.dto.WalletResponse;
import com.example.investsmart.entity.User;
import com.example.investsmart.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> deposit(
            Authentication authentication,
            @Valid @RequestBody WalletRequest request) {

        String email = authentication.getName();

        User user =
                walletService.deposit(email, request);

        return ResponseEntity.ok(
                new WalletResponse(
                        user.getVirtualBalance()
                )
        );
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WalletResponse> withdraw(
            Authentication authentication,
            @Valid @RequestBody WalletRequest request) {

        String email = authentication.getName();

        User user =
                walletService.withdraw(email, request);

        return ResponseEntity.ok(
                new WalletResponse(
                        user.getVirtualBalance()
                )
        );
    }

    @GetMapping("/{userId}")
    public ResponseEntity<BigDecimal> getBalance(
            @PathVariable Long userId) {

        BigDecimal balance =
                walletService.getBalance(userId);

        return ResponseEntity.ok(balance);
    }

    @GetMapping("/me")
    public ResponseEntity<BigDecimal> getMyBalance(
            Authentication authentication) {

        String email = authentication.getName();

        BigDecimal balance =
                walletService.getBalanceByEmail(email);

        return ResponseEntity.ok(balance);
    }
}