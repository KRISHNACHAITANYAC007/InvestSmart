package com.example.investsmart.service;

import com.example.investsmart.dto.LoginRequest;
import com.example.investsmart.dto.RegisterRequest;
import com.example.investsmart.entity.User;
import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElse(null);

        if (user == null ||
                !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword())) {

            throw new BadRequestException(
                    "Invalid email or password"
            );
        }

        return jwtService.generateToken(
                user.getEmail(),
                user.getRole()
        );
    }

    public User register(RegisterRequest request) {

        if (userRepository
                .findByEmail(request.getEmail())
                .isPresent()) {

            throw new BadRequestException(
                    "Email is already registered"
            );
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setRole("USER");

        user.setVirtualBalance(
                new BigDecimal("100000")
        );

        return userRepository.save(user);
    }
}