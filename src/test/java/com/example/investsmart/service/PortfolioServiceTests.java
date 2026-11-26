package com.example.investsmart.service;

import com.example.investsmart.dto.PortfolioResponse;
import com.example.investsmart.entity.User;
import com.example.investsmart.repository.HoldingRepository;
import com.example.investsmart.repository.OrderRepository;
import com.example.investsmart.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock private com.example.investsmart.investment.IpoApplicationRepository ipoApplications;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void missingPriceKeepsCashAndCostButDoesNotInventValuation() {
        User user = new User(); user.setId(1L); user.setVirtualBalance(new BigDecimal("500"));
        var stock = new com.example.investsmart.entity.Stock(); stock.setId(2L);stock.setSymbol("INFY");stock.setCompanyName("Infosys");stock.setInstrumentKey("BSE_EQ|500209");stock.setIsin("INE009A01021");
        var holding = new com.example.investsmart.entity.Holding(); holding.setStock(stock);holding.setQuantity(2);
        var buy = new com.example.investsmart.entity.Order();buy.setStock(stock);buy.setQuantity(2);buy.setOrderType(com.example.investsmart.entity.OrderType.BUY);buy.setStatus(com.example.investsmart.entity.OrderStatus.COMPLETED);buy.setTotalAmount(new BigDecimal("200"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(holdingRepository.findByUserId(1L)).thenReturn(List.of(holding));
        when(orderRepository.findByUserIdOrderByIdAsc(1L)).thenReturn(List.of(buy));
        var result=portfolioService.getPortfolio(1L);
        assertEquals(new BigDecimal("500"),result.getBalance()); assertEquals(new BigDecimal("200.00"),result.getInvestedAmount());
        assertFalse(result.isValuationAvailable());assertTrue(result.isPerformanceAvailable());
        assertNull(result.getTotalValue());assertNull(result.getHoldingsValue());assertNull(result.getUnrealizedPnl());assertNull(result.getCashAllocationPercent());
        var row=result.getHoldings().getFirst();assertNull(row.getCurrentValue());assertNull(row.getUnrealizedPnlPercent());assertNull(row.getAllocationPercent());assertEquals("BSE_EQ|500209",row.getInstrumentKey());
        assertEquals("BSE",row.getExchange());assertEquals("INE009A01021",row.getIsin());assertTrue(result.getRiskInsights().isEmpty());
    }

    @Test
    void zeroQuantityWithoutPriceDoesNotInvalidateAnEmptyPortfolio() {
        User user = new User();user.setVirtualBalance(new BigDecimal("500"));
        var stock=new com.example.investsmart.entity.Stock();stock.setId(2L);
        var holding=new com.example.investsmart.entity.Holding();holding.setStock(stock);holding.setQuantity(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));when(holdingRepository.findByUserId(1L)).thenReturn(List.of(holding));
        when(orderRepository.findByUserIdOrderByIdAsc(1L)).thenReturn(List.of());
        var result=portfolioService.getPortfolio(1L);assertTrue(result.isValuationAvailable());assertEquals(0,result.getTotalValue().compareTo(new BigDecimal("500")));assertTrue(result.getHoldings().isEmpty());
    }

    @Test
    void zeroValuePortfolioHasZeroAllocationsAndProfitLoss() {
        User user = new User();
        user.setId(1L);
        user.setVirtualBalance(BigDecimal.ZERO);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(holdingRepository.findByUserId(1L)).thenReturn(List.of());
        when(orderRepository.findByUserIdOrderByIdAsc(1L)).thenReturn(List.of());

        PortfolioResponse portfolio = portfolioService.getPortfolio(1L);

        assertEquals(0, portfolio.getCashAllocationPercent()
                .compareTo(new BigDecimal("0.00")));
        assertEquals(0, portfolio.getInvestedAllocationPercent()
                .compareTo(new BigDecimal("0.00")));
        assertEquals(0, portfolio.getInvestedAmount()
                .compareTo(new BigDecimal("0.00")));
        assertEquals(0, portfolio.getRealizedPnl()
                .compareTo(new BigDecimal("0.00")));
        assertEquals(0, portfolio.getUnrealizedPnl()
                .compareTo(new BigDecimal("0.00")));
    }
}
