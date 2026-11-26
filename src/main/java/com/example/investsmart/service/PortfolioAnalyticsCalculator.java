package com.example.investsmart.service;

import com.example.investsmart.entity.Holding;
import com.example.investsmart.entity.Order;
import com.example.investsmart.entity.OrderStatus;
import com.example.investsmart.entity.OrderType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class PortfolioAnalyticsCalculator {

    private static final BigDecimal STOCK_CONCENTRATION_LIMIT =
            new BigDecimal("25");
    private static final BigDecimal SECTOR_CONCENTRATION_LIMIT =
            new BigDecimal("40");
    private static final int CURRENCY_SCALE = 2;
    private static final int COST_BASIS_SCALE = 12;

    Calculation calculate(
            List<Order> orders,
            Map<Long, Integer> currentQuantities) {

        Map<Long, TradeState> states = new HashMap<>();

        currentQuantities.forEach((stockId, quantity) ->
                states.put(stockId, new TradeState(quantity))
        );

        List<Order> chronologicalOrders = new ArrayList<>(orders);
        chronologicalOrders.sort(
                Comparator.comparing(
                        Order::getId,
                        Comparator.nullsFirst(Long::compareTo)
                )
        );

        boolean hasUnassignedCompletedOrder = false;
        for (Order order : chronologicalOrders) {
            if (order.getStatus() != OrderStatus.COMPLETED) {
                continue;
            }

            if (order.getStock() == null
                    || order.getStock().getId() == null) {
                hasUnassignedCompletedOrder = true;
                continue;
            }

            Long stockId = order.getStock().getId();
            TradeState state = states.computeIfAbsent(
                    stockId,
                    ignored -> new TradeState(0)
            );

            applyOrder(order, state);
        }

        Map<Long, PositionPerformance> positions = new LinkedHashMap<>();
        BigDecimal realizedPnl = BigDecimal.ZERO;
        boolean allPositionsConsistent = !hasUnassignedCompletedOrder;

        for (Map.Entry<Long, TradeState> entry : states.entrySet()) {
            TradeState state = entry.getValue();
            if (state.quantity != state.expectedQuantity) {
                state.consistent = false;
            }

            PositionPerformance performance = state.toPerformance();
            positions.put(entry.getKey(), performance);

            if (performance.consistent()) {
                realizedPnl = realizedPnl.add(performance.realizedPnl());
            } else {
                allPositionsConsistent = false;
            }
        }

        return new Calculation(
                positions,
                allPositionsConsistent
                        ? money(realizedPnl)
                        : null,
                allPositionsConsistent
        );
    }

    static BigDecimal percentage(BigDecimal amount, BigDecimal total) {
        if (amount == null || total == null || total.signum() <= 0) {
            return BigDecimal.ZERO.setScale(CURRENCY_SCALE);
        }

        return amount
                .multiply(new BigDecimal("100"))
                .divide(total, CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    static List<String> concentrationInsights(
            List<Holding> holdings,
            BigDecimal holdingsValue) {

        List<String> insights = new ArrayList<>();
        if (holdingsValue == null || holdingsValue.signum() <= 0) {
            return insights;
        }

        Map<Long, BigDecimal> stockValues = new LinkedHashMap<>();
        Map<Long, String> stockSymbols = new HashMap<>();
        Map<String, BigDecimal> sectorValues = new TreeMap<>(
                String.CASE_INSENSITIVE_ORDER
        );

        for (Holding holding : holdings) {
            BigDecimal currentValue = holding.getStock()
                    .getCurrentPrice()
                    .multiply(BigDecimal.valueOf(holding.getQuantity()));

            Long stockId = holding.getStock().getId();
            stockValues.merge(stockId, currentValue, BigDecimal::add);
            stockSymbols.put(stockId, holding.getStock().getSymbol());

            String sector = holding.getStock().getSector();
            if (sector != null && !sector.isBlank()) {
                String normalizedSector = sector.trim();
                sectorValues.merge(
                        normalizedSector,
                        currentValue,
                        BigDecimal::add
                );
            }
        }

        stockValues.forEach((stockId, value) -> {
            BigDecimal stockShare = percentage(value, holdingsValue);
            if (stockShare.compareTo(STOCK_CONCENTRATION_LIMIT) > 0) {
                insights.add(
                        stockSymbols.get(stockId)
                                + " makes up " + stockShare.toPlainString()
                                + "% of your holdings. A large single-stock"
                                + " share can increase exposure to company-specific changes."
                );
            }
        });

        sectorValues.forEach((sector, value) -> {
            BigDecimal sectorShare = percentage(value, holdingsValue);
            if (sectorShare.compareTo(SECTOR_CONCENTRATION_LIMIT) > 0) {
                insights.add(
                        sector + " makes up " + sectorShare.toPlainString()
                                + "% of your holdings. A large sector share can"
                                + " increase exposure to sector-wide changes."
                );
            }
        });

        return insights;
    }

    private static void applyOrder(Order order, TradeState state) {
        Integer orderQuantity = order.getQuantity();
        BigDecimal amount = order.getTotalAmount();

        if (orderQuantity == null
                || orderQuantity <= 0
                || amount == null
                || amount.signum() < 0
                || order.getOrderType() == null) {
            state.consistent = false;
            return;
        }

        long quantity = orderQuantity;
        if (order.getOrderType() == OrderType.BUY) {
            state.costBasis = state.costBasis.add(amount);
            state.quantity += quantity;
            return;
        }

        if (order.getOrderType() != OrderType.SELL
                || quantity > state.quantity
                || state.quantity == 0) {
            state.consistent = false;
            return;
        }

        BigDecimal removedCostBasis = quantity == state.quantity
                ? state.costBasis
                : state.costBasis
                        .divide(
                                BigDecimal.valueOf(state.quantity),
                                COST_BASIS_SCALE,
                                RoundingMode.HALF_UP
                        )
                        .multiply(BigDecimal.valueOf(quantity));

        state.costBasis = state.costBasis.subtract(removedCostBasis);
        state.realizedPnl = state.realizedPnl.add(
                amount.subtract(removedCostBasis)
        );
        state.quantity -= quantity;
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    record Calculation(
            Map<Long, PositionPerformance> positions,
            BigDecimal realizedPnl,
            boolean consistent) {
    }

    record PositionPerformance(
            boolean consistent,
            long quantity,
            BigDecimal averageBuyPrice,
            BigDecimal investedAmount,
            BigDecimal realizedPnl) {
    }

    private static final class TradeState {
        private final long expectedQuantity;
        private long quantity;
        private BigDecimal costBasis = BigDecimal.ZERO;
        private BigDecimal realizedPnl = BigDecimal.ZERO;
        private boolean consistent = true;

        private TradeState(long expectedQuantity) {
            this.expectedQuantity = expectedQuantity;
        }

        private PositionPerformance toPerformance() {
            if (!consistent) {
                return new PositionPerformance(
                        false,
                        quantity,
                        null,
                        null,
                        null
                );
            }

            BigDecimal averageBuyPrice = quantity == 0
                    ? BigDecimal.ZERO.setScale(CURRENCY_SCALE)
                    : costBasis.divide(
                            BigDecimal.valueOf(quantity),
                            CURRENCY_SCALE,
                            RoundingMode.HALF_UP
                    );

            return new PositionPerformance(
                    true,
                    quantity,
                    averageBuyPrice,
                    money(costBasis),
                    money(realizedPnl)
            );
        }
    }
}
