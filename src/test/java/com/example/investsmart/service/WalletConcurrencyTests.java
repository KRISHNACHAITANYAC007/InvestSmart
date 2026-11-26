package com.example.investsmart.service;

import com.example.investsmart.dto.WalletRequest;
import com.example.investsmart.entity.User;
import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class WalletConcurrencyTests {
    @Autowired WalletService wallet;
    @Autowired UserRepository users;

    @Test void concurrentWithdrawalsCannotSpendTheSameCashTwice() throws Exception {
        var user=new User();user.setEmail("funds-check-"+UUID.randomUUID()+"@example.invalid");user.setVirtualBalance(new BigDecimal("100"));
        user=users.save(user);String email=user.getEmail();Long id=user.getId();
        var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        Callable<Boolean> withdraw=()->{start.await();var request=new WalletRequest();request.setAmount(new BigDecimal("70"));try{wallet.withdraw(email,request);return true;}catch(BadRequestException expected){return false;}};
        try {
            var first=executor.submit(withdraw);var second=executor.submit(withdraw);start.countDown();
            int succeeded=(first.get(10,TimeUnit.SECONDS)?1:0)+(second.get(10,TimeUnit.SECONDS)?1:0);
            assertEquals(1,succeeded);assertEquals(0,users.findById(id).orElseThrow().getVirtualBalance().compareTo(new BigDecimal("30")));
        } finally {executor.shutdownNow();users.deleteById(id);}
    }
}
