package com.example.investsmart.investment;

import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.Clock;

@Configuration
@EnableScheduling
public class InvestmentConfiguration {
    @Bean public Clock investmentClock() { return Clock.systemUTC(); }
}
