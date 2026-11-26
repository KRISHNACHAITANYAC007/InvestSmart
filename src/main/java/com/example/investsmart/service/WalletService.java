package com.example.investsmart.service;

import com.example.investsmart.dto.WalletRequest;
import com.example.investsmart.entity.User;
import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.exception.ResourceNotFoundException;
import com.example.investsmart.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final UserRepository userRepository;

    private final com.example.investsmart.settlement.SettlementAccounting accounting;
    public WalletService(UserRepository userRepository,com.example.investsmart.settlement.SettlementAccounting accounting) {
        this.accounting=accounting;
        this.userRepository = userRepository;
    }

    @org.springframework.transaction.annotation.Transactional
    public User deposit(
            String email,
            WalletRequest request) {

        User user = userRepository
                .findByEmailForUpdate(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
        user.requireActive();
        accounting.prepare(user);

        BigDecimal amount = validAmount(request);

        accounting.credit(user,accounting.request(),amount);

        return userRepository.save(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public User withdraw(
            String email,
            WalletRequest request) {

        User user = userRepository
                .findByEmailForUpdate(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
        user.requireActive();
        accounting.prepare(user);

        BigDecimal amount = validAmount(request);

        if (user.getVirtualBalance()
                .compareTo(amount) < 0) {

            throw new BadRequestException(
                    "Insufficient virtual balance"
            );
        }

        accounting.withdraw(user,accounting.request(),amount);

        return userRepository.save(user);
    }

    public BigDecimal getBalance(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        return user.getVirtualBalance();
    }

    private BigDecimal validAmount(WalletRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0
                || request.getAmount().stripTrailingZeros().scale() > 2)
            throw new BadRequestException("Enter a positive amount with at most two decimal places.");
        return request.getAmount();
    }

    public BigDecimal getBalanceByEmail(String email) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        return user.getVirtualBalance();
    }
}
