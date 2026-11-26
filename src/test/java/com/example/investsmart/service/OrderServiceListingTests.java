package com.example.investsmart.service;

import com.example.investsmart.dto.OrderRequest;
import com.example.investsmart.entity.*;
import com.example.investsmart.market.*;
import com.example.investsmart.exception.*;
import com.example.investsmart.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceListingTests {
    @Mock OrderRepository orders;
    @Mock UserRepository users;
    @Mock StockRepository stocks;
    @Mock HoldingRepository holdings;
    @Mock MarketDataService market;
    @Mock com.example.investsmart.settlement.SettlementAccounting accounting;
    User user; Stock stock; OrderService service;
    static final String KEY = "BSE_EQ|500209";

    @BeforeEach void setup() {
        user = new User(); user.setId(1L); user.setVirtualBalance(new BigDecimal("10000"));
        stock = new Stock(); stock.setId(9L); stock.setSymbol("INFY"); stock.setInstrumentKey(KEY); stock.setExchange("BSE"); stock.setCurrentPrice(new BigDecimal("1"));
        when(users.findByEmailForUpdate("test@example.com")).thenReturn(Optional.of(user));
        when(stocks.findById(9L)).thenReturn(Optional.of(stock));
        service = new OrderService(orders,users,stocks,holdings,new NseMarketSessionService(market),market,null,accounting);
    }
    OrderRequest request(int quantity) { var request = new OrderRequest(); request.setStockId(9L); request.setQuantity(quantity); return request; }
    void openWithQuote(String key) {
        when(market.getExchangeStatus("BSE")).thenReturn("NORMAL_OPEN");
        when(market.getQuote(any(MarketInstrument.class))).thenReturn(new MarketQuote("INFY",new BigDecimal("1501"),null,LocalDateTime.now())
                .withInstrument(new MarketInstrument(key,"INFY","Infosys",key.startsWith("BSE")?"BSE":"NSE")));
    }
    @Test void buyUsesBseLivePriceAndSelectedStockId() {
        openWithQuote(KEY); when(orders.save(any())).thenAnswer(i->i.getArgument(0));
        Order order = service.buyStock("test@example.com",request(2));
        assertEquals(new BigDecimal("3002"),order.getTotalAmount()); verify(accounting).pay(eq(user),any(),eq(new BigDecimal("3002")),eq(false));verify(accounting).purchase(eq(user),any(),eq(9L),eq(2),eq(new BigDecimal("3002")),eq("BSE"));
        assertSame(stock,order.getStock()); verify(market).getExchangeStatus("BSE"); verify(market,never()).getExchangeStatus("NSE");
        verify(holdings).findByUserIdAndStockId(1L,9L);
    }
    @Test void crossExchangeQuoteCannotChangeCashOrHoldings() {
        openWithQuote("NSE_EQ|INE009A01021");
        assertThrows(MarketStatusUnavailableException.class,()->service.buyStock("test@example.com",request(1)));
        assertEquals(new BigDecimal("10000"),user.getVirtualBalance()); verifyNoInteractions(orders,holdings);
    }
    @Test void noQuoteCannotChangeCashOrHoldings() {
        when(market.getExchangeStatus("BSE")).thenReturn("NORMAL_OPEN");
        assertThrows(MarketStatusUnavailableException.class,()->service.buyStock("test@example.com",request(1)));
        assertEquals(new BigDecimal("10000"),user.getVirtualBalance()); verifyNoInteractions(orders,holdings);
    }
    @Test void cannotSellHoldingsFromAnotherListing() {
        when(market.getExchangeStatus("BSE")).thenReturn("NORMAL_OPEN");
        assertThrows(ResourceNotFoundException.class,()->service.sellStock("test@example.com",request(1)));
        verify(holdings).findByUserIdAndStockId(1L,9L); verifyNoInteractions(orders);
        assertEquals(new BigDecimal("10000"),user.getVirtualBalance());
    }
    @Test void sellUsesSelectedHoldingsAndLivePrice() {
        openWithQuote(KEY); var holding = new Holding(); holding.setQuantity(4); holding.setStock(stock); holding.setUser(user);
        when(holdings.findByUserIdAndStockId(1L,9L)).thenReturn(Optional.of(holding)); when(orders.save(any())).thenAnswer(i->i.getArgument(0));
        var order = service.sellStock("test@example.com",request(2));
        assertEquals(2,holding.getQuantity()); verify(accounting).sell(eq(user),any(),eq(9L),eq(2),eq(new BigDecimal("1501")),eq("BSE")); assertEquals(new BigDecimal("1501"),order.getPrice());
    }
    @Test void closedBseCannotMutateAnOrder() {
        when(market.getExchangeStatus("BSE")).thenReturn("NORMAL_CLOSE");
        assertThrows(BadRequestException.class,()->service.buyStock("test@example.com",request(1)));
        verifyNoInteractions(orders,holdings); verify(market,never()).getQuote(any(MarketInstrument.class));
    }
    @Test void insufficientCashAndInvalidQuantityDoNotMutate() {
        openWithQuote(KEY);
        assertThrows(BadRequestException.class,()->service.buyStock("test@example.com",request(10)));
        assertThrows(BadRequestException.class,()->service.buyStock("test@example.com",request(0)));
        assertEquals(new BigDecimal("10000"),user.getVirtualBalance()); verifyNoInteractions(orders,holdings);
    }
}
