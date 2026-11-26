package com.example.investsmart.service;
import com.example.investsmart.dto.WalletRequest;
import com.example.investsmart.entity.User;
import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class WalletServiceTests {
    @Test void balanceChangesUseTheSharedUserLockAndRejectInvalidAmounts() {
        var users=mock(UserRepository.class);var user=new User();user.setVirtualBalance(new BigDecimal("100"));
        when(users.findByEmailForUpdate("test@example.com")).thenReturn(Optional.of(user));when(users.save(user)).thenReturn(user);
        var accounting=mock(com.example.investsmart.settlement.SettlementAccounting.class);var service=new WalletService(users,accounting);var request=new WalletRequest();request.setAmount(new BigDecimal("25.50"));
        service.deposit("test@example.com",request);verify(accounting).credit(eq(user),nullable(String.class),eq(new BigDecimal("25.50")));
        service.withdraw("test@example.com",request);verify(accounting).withdraw(eq(user),nullable(String.class),eq(new BigDecimal("25.50")));assertEquals(new BigDecimal("100"),user.getVirtualBalance());
        for(String amount:new String[]{"-1","0","0.001","500"}) {request.setAmount(new BigDecimal(amount));assertThrows(BadRequestException.class,()->service.withdraw("test@example.com",request));}
        assertEquals(new BigDecimal("100"),user.getVirtualBalance());verify(users,never()).findByEmail(anyString());verify(users,times(2)).save(user);
    }
}
