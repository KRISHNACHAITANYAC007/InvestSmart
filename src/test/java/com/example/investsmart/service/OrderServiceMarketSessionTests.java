package com.example.investsmart.service;

import com.example.investsmart.dto.OrderRequest;
import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.exception.MarketStatusUnavailableException;
import com.example.investsmart.market.MarketDataService;
import com.example.investsmart.repository.HoldingRepository;
import com.example.investsmart.repository.OrderRepository;
import com.example.investsmart.repository.StockRepository;
import com.example.investsmart.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceMarketSessionTests {
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private StockRepository stockRepository;
    @Mock private HoldingRepository holdingRepository;
    @Mock private MarketDataService marketDataService;

    private OrderService orderService;

    @BeforeEach
    void createService() {
        orderService = new OrderService(orderRepository, userRepository, stockRepository,
                holdingRepository, new NseMarketSessionService(marketDataService), marketDataService, null, org.mockito.Mockito.mock(com.example.investsmart.settlement.SettlementAccounting.class));
        var user = new com.example.investsmart.entity.User(); user.setId(1L);
        var stock = new com.example.investsmart.entity.Stock(); stock.setId(1L); stock.setSymbol("INFY");
        when(userRepository.findByEmailForUpdate("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(stockRepository.findById(1L)).thenReturn(java.util.Optional.of(stock));
    }

    @Test
    void closedMarketPreventsBuyAndSellBeforeAnyOrderSideEffects() {
        when(marketDataService.getExchangeStatus("NSE")).thenReturn("NORMAL_CLOSE");
        OrderRequest request = new OrderRequest();
        request.setStockId(1L);
        request.setQuantity(1);

        assertThrows(BadRequestException.class, () -> orderService.buyStock("user@example.com", request));
        assertThrows(BadRequestException.class, () -> orderService.sellStock("user@example.com", request));

        verify(marketDataService, org.mockito.Mockito.times(2)).getExchangeStatus("NSE");
        verifyNoInteractions(orderRepository, holdingRepository);
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unavailableMarketStatusPreventsBuyAndSellBeforeAnyOrderSideEffects() {
        when(marketDataService.getExchangeStatus("NSE"))
                .thenThrow(new IllegalStateException("Upstox unavailable"));
        OrderRequest request = new OrderRequest();
        request.setStockId(1L);
        request.setQuantity(1);

        assertThrows(MarketStatusUnavailableException.class,
                () -> orderService.buyStock("user@example.com", request));
        assertThrows(MarketStatusUnavailableException.class,
                () -> orderService.sellStock("user@example.com", request));

        verifyNoInteractions(orderRepository, holdingRepository);
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }
}
