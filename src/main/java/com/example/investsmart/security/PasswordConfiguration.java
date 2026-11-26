package com.example.investsmart.security;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class PasswordConfiguration {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
}
