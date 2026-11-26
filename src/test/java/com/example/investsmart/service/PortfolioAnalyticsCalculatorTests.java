package com.example.investsmart.service;

import com.example.investsmart.entity.Holding;
import com.example.investsmart.entity.Order;
import com.example.investsmart.entity.OrderStatus;
import com.example.investsmart.entity.OrderType;
import com.example.investsmart.entity.Stock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioAnalyticsCalculatorTests {

    private final PortfolioAnalyticsCalculator calculator =
            new PortfolioAnalyticsCalculator();

    @Test
    void calculatesWeightedAverageAfterMultipleBuysAndPartialSale() {
        PortfolioAnalyticsCalculator.Calculation result = calculator.calculate(
                List.of(
                        order(1, 7, OrderType.BUY, 2, "200.00"),
                        order(2, 7, OrderType.BUY, 2, "280.00"),
                        order(3, 7, OrderType.SELL, 1, "150.00")
                ),
                Map.of(7L, 3)
        );

        PortfolioAnalyticsCalculator.PositionPerformance position =
                result.positions().get(7L);

        assertTrue(result.consistent());
        assertEquals(0, position.averageBuyPrice()
                .compareTo(new BigDecimal("120.00")));
        assertEquals(0, position.investedAmount()
                .compareTo(new BigDecimal("360.00")));
        assertEquals(0, position.realizedPnl()
                .compareTo(new BigDecimal("30.00")));
        assertEquals(0, result.realizedPnl()
                .compareTo(new BigDecimal("30.00")));
    }

    @Test
    void fullSaleAndRebuyResetsAverageCostButKeepsRealizedProfit() {
        PortfolioAnalyticsCalculator.Calculation result = calculator.calculate(
                List.of(
                        order(1, 7, OrderType.BUY, 2, "200.00"),
                        order(2, 7, OrderType.SELL, 2, "240.00"),
                        order(3, 7, OrderType.BUY, 1, "150.00")
                ),
                Map.of(7L, 1)
        );

        PortfolioAnalyticsCalculator.PositionPerformance position =
                result.positions().get(7L);

        assertTrue(result.consistent());
        assertEquals(0, position.averageBuyPrice()
                .compareTo(new BigDecimal("150.00")));
        assertEquals(0, position.investedAmount()
                .compareTo(new BigDecimal("150.00")));
        assertEquals(0, position.realizedPnl()
                .compareTo(new BigDecimal("40.00")));
    }

    @Test
    void aggregatesRealizedProfitAcrossDifferentStocks() {
        PortfolioAnalyticsCalculator.Calculation result = calculator.calculate(
                List.of(
                        order(1, 7, OrderType.BUY, 1, "100.00"),
                        order(2, 9, OrderType.BUY, 1, "200.00"),
                        order(3, 7, OrderType.SELL, 1, "110.00"),
                        order(4, 9, OrderType.SELL, 1, "180.00")
                ),
                Map.of(7L, 0, 9L, 0)
        );

        assertTrue(result.consistent());
        assertEquals(0, result.realizedPnl()
                .compareTo(new BigDecimal("-10.00")));
        assertEquals(0, result.positions().get(7L).realizedPnl()
                .compareTo(new BigDecimal("10.00")));
        assertEquals(0, result.positions().get(9L).realizedPnl()
                .compareTo(new BigDecimal("-20.00")));
    }

    @Test
    void marksCostBasisUnavailableWhenOrderQuantityDoesNotMatchHolding() {
        PortfolioAnalyticsCalculator.Calculation result = calculator.calculate(
                List.of(order(1, 7, OrderType.BUY, 2, "200.00")),
                Map.of(7L, 3)
        );

        assertFalse(result.consistent());
        assertNull(result.realizedPnl());
        assertFalse(result.positions().get(7L).consistent());
        assertNull(result.positions().get(7L).averageBuyPrice());
        assertNull(result.positions().get(7L).investedAmount());
    }

    @Test
    void zeroPortfolioValueHasZeroAllocationPercentage() {
        assertEquals(0, PortfolioAnalyticsCalculator.percentage(
                BigDecimal.ZERO,
                BigDecimal.ZERO
        ).compareTo(new BigDecimal("0.00")));
    }

    @Test
    void doesNotFlagConcentrationAtExactThresholds() {
        Holding first = holding("INFY", "Information Technology", "200.00", 1);
        Holding second = holding("TCS", "Information Technology", "200.00", 1);

        List<String> insights = PortfolioAnalyticsCalculator.concentrationInsights(
                List.of(first, second),
                new BigDecimal("1000.00")
        );

        assertTrue(insights.isEmpty());
    }

    @Test
    void flagsSingleStockAndSectorAboveThresholds() {
        Holding infy = holding("INFY", "Information Technology", "260.00", 1);
        Holding tcs = holding("TCS", "Information Technology", "150.00", 1);
        Holding otherOne = holding("OTHER1", null, "147.50", 1);
        Holding otherTwo = holding("OTHER2", null, "147.50", 1);
        Holding otherThree = holding("OTHER3", null, "147.50", 1);
        Holding otherFour = holding("OTHER4", null, "147.50", 1);

        List<String> insights = PortfolioAnalyticsCalculator.concentrationInsights(
                List.of(infy, tcs, otherOne, otherTwo, otherThree, otherFour),
                new BigDecimal("1000.00")
        );

        assertEquals(2, insights.size());
        assertTrue(insights.stream().anyMatch(message ->
                message.contains("INFY") && message.contains("26.00%")));
        assertTrue(insights.stream().anyMatch(message ->
                message.contains("Information Technology")
                        && message.contains("41.00%")));
    }

    @Test
    void ignoresUnclassifiedHoldingsForSectorFlags() {
        Holding first = holding("AAA", null, "200.00", 1);
        Holding second = holding("BBB", " ", "200.00", 1);
        Holding third = holding("CCC", null, "200.00", 1);
        Holding fourth = holding("DDD", null, "200.00", 1);
        Holding fifth = holding("EEE", null, "200.00", 1);

        List<String> insights = PortfolioAnalyticsCalculator.concentrationInsights(
                List.of(first, second, third, fourth, fifth),
                new BigDecimal("1000.00")
        );

        assertTrue(insights.isEmpty());
    }

    private Order order(
            long id,
            long stockId,
            OrderType type,
            int quantity,
            String totalAmount) {

        Stock stock = new Stock();
        stock.setId(stockId);

        Order order = new Order();
        order.setId(id);
        order.setStock(stock);
        order.setOrderType(type);
        order.setQuantity(quantity);
        order.setTotalAmount(new BigDecimal(totalAmount));
        order.setStatus(OrderStatus.COMPLETED);
        return order;
    }

    private Holding holding(
            String symbol,
            String sector,
            String price,
            int quantity) {

        Stock stock = new Stock();
        stock.setId((long) symbol.hashCode());
        stock.setSymbol(symbol);
        stock.setSector(sector);
        stock.setCurrentPrice(new BigDecimal(price));

        Holding holding = new Holding();
        holding.setStock(stock);
        holding.setQuantity(quantity);
        return holding;
    }
}
