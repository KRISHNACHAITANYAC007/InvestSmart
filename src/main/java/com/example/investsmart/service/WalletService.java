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

    public WalletService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User deposit(
            String email,
            WalletRequest request) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        BigDecimal amount = request.getAmount();

        user.setVirtualBalance(
                user.getVirtualBalance().add(amount)
        );

        return userRepository.save(user);
    }

    public User withdraw(
            String email,
            WalletRequest request) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        BigDecimal amount = request.getAmount();

        if (user.getVirtualBalance()
                .compareTo(amount) < 0) {

            throw new BadRequestException(
                    "Insufficient virtual balance"
            );
        }

        user.setVirtualBalance(
                user.getVirtualBalance().subtract(amount)
        );

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