package com.example.investsmart.service;

import com.example.investsmart.dto.HoldingResponse;
import com.example.investsmart.dto.PortfolioResponse;
import com.example.investsmart.entity.Holding;
import com.example.investsmart.entity.User;
import com.example.investsmart.exception.ResourceNotFoundException;
import com.example.investsmart.repository.HoldingRepository;
import com.example.investsmart.repository.OrderRepository;
import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.investment.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioService {

    private com.example.investsmart.settlement.SettlementStatus settlementStatus;
    private com.example.investsmart.options.OptionTrading options;
    private com.example.investsmart.trading.PaperTrading paper;
    @org.springframework.beans.factory.annotation.Autowired public PortfolioService(UserRepository users,HoldingRepository holdings,OrderRepository orders,IpoApplicationRepository ipos,com.example.investsmart.options.OptionTrading options,com.example.investsmart.trading.PaperTrading paper,com.example.investsmart.settlement.SettlementStatus settlementStatus){this(users,holdings,orders,ipos);this.settlementStatus=settlementStatus;this.options=options;this.paper=paper;}
    private static final int CURRENCY_SCALE = 2;

    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final OrderRepository orderRepository;
    private final IpoApplicationRepository ipoApplications;
    private final PortfolioAnalyticsCalculator analyticsCalculator =
            new PortfolioAnalyticsCalculator();

    public PortfolioService(
            UserRepository userRepository,
            HoldingRepository holdingRepository,
            OrderRepository orderRepository, IpoApplicationRepository ipoApplications) {

        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
        this.orderRepository = orderRepository;
        this.ipoApplications = ipoApplications;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(Long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        List<Holding> holdings = holdingRepository.findByUserId(userId);
        Map<Long, Integer> currentQuantities = new HashMap<>();
        for (Holding holding : holdings) {
            currentQuantities.merge(
                    holding.getStock().getId(),
                    holding.getQuantity(),
                    Integer::sum
            );
        }

        PortfolioAnalyticsCalculator.Calculation calculation =
                analyticsCalculator.calculate(
                        orderRepository.findByUserIdOrderByIdAsc(userId),
                        currentQuantities
                );

        boolean valuationAvailable = holdings.stream().filter(h -> h.getQuantity() > 0)
                .allMatch(h -> h.getStock().getCurrentPrice() != null && h.getStock().getCurrentPrice().signum() > 0);
        BigDecimal holdingsValue = !valuationAvailable ? null : holdings.stream()
                .filter(holding -> holding.getQuantity() > 0)
                .map(this::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal investedAmount = BigDecimal.ZERO;
        BigDecimal unrealizedPnl = BigDecimal.ZERO;
        boolean performanceAvailable = calculation.consistent();
        List<HoldingResponse> holdingResponses = new ArrayList<>();

        for (Holding holding : holdings) {
            int quantity = holding.getQuantity();
            if (quantity <= 0) {
                continue;
            }

            BigDecimal savedPrice = holding.getStock().getCurrentPrice();
            BigDecimal currentPrice = savedPrice != null && savedPrice.signum() > 0 ? savedPrice : null;
            BigDecimal currentValue = currentPrice == null ? null : currentValue(holding);

            PortfolioAnalyticsCalculator.PositionPerformance position =
                    calculation.positions().get(holding.getStock().getId());

            BigDecimal averageBuyPrice = null;
            BigDecimal positionInvestedAmount = null;
            BigDecimal positionUnrealizedPnl = null;
            BigDecimal positionUnrealizedPnlPercent = null;

            if (position != null && position.consistent()) {
                averageBuyPrice = position.averageBuyPrice();
                positionInvestedAmount = position.investedAmount();
                positionUnrealizedPnl = currentValue == null ? null : currentValue
                        .subtract(positionInvestedAmount)
                        .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
                positionUnrealizedPnlPercent = positionUnrealizedPnl == null ? null : PortfolioAnalyticsCalculator
                        .percentage(positionUnrealizedPnl, positionInvestedAmount);

                investedAmount = investedAmount.add(positionInvestedAmount);
                if (positionUnrealizedPnl != null) unrealizedPnl = unrealizedPnl.add(positionUnrealizedPnl);
            } else {
                performanceAvailable = false;
            }

            holdingResponses.add(
                    new HoldingResponse(
                            holding.getStock().getId(),
                            holding.getStock().getSymbol(),
                            holding.getStock().getCompanyName(),
                            holding.getStock().getSector(),
                            quantity,
                            currentPrice,
                            currentValue,
                            averageBuyPrice,
                            positionInvestedAmount,
                            positionUnrealizedPnl,
                            positionUnrealizedPnlPercent,
                            !valuationAvailable || currentValue == null ? null : PortfolioAnalyticsCalculator.percentage(
                                    currentValue,
                                    holdingsValue
                            )
                    )
            );
            HoldingResponse response = holdingResponses.get(holdingResponses.size() - 1);
            response.setInstrumentKey(holding.getStock().getInstrumentKey());
            response.setExchange(holding.getStock().getExchange());
            response.setIsin(holding.getStock().getIsin());
        }

        var applications = ipoApplications.findByUserIdOrderByIdDesc(userId);
        BigDecimal reserved = applications.stream().map(IpoApplication::getReservedAmount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unlistedCost = applications.stream().filter(a -> "ALLOTTED".equals(a.getStatus())).map(IpoApplication::getAllocatedCost).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = user.getVirtualBalance();
        boolean totalValuationAvailable = balance != null && valuationAvailable && unlistedCost.signum() == 0;
        BigDecimal totalValue = totalValuationAvailable ? balance.add(reserved).add(holdingsValue) : null;
        BigDecimal totalInvested = performanceAvailable
                ? investedAmount.add(unlistedCost).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)
                : null;
        BigDecimal totalUnrealizedPnl = performanceAvailable && totalValuationAvailable
                ? unrealizedPnl.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)
                : null;
        BigDecimal realizedPnl = calculation.consistent()
                ? calculation.realizedPnl()
                : null;

        var response = new PortfolioResponse(
                balance,
                holdingsValue,
                totalValue,
                totalInvested,
                realizedPnl,
                totalUnrealizedPnl,
                totalValuationAvailable ? PortfolioAnalyticsCalculator.percentage(balance.add(reserved), totalValue) : null,
                totalValuationAvailable ? PortfolioAnalyticsCalculator.percentage(holdingsValue, totalValue) : null,
                performanceAvailable,
                holdingResponses,
                PortfolioAnalyticsCalculator.concentrationInsights(
                        holdings.stream().filter(h -> h.getQuantity() > 0).toList(),
                        holdingsValue
                )
        );
        response.setReservedIpoFunds(reserved);response.setUnlistedIpoCost(unlistedCost);
        response.setIpoApplications(applications.stream().map(IpoApplicationView::from).toList());
        if(options!=null)options.enrich(response,userId);
        if(paper!=null)paper.enrich(response,userId);
        if(settlementStatus!=null){var settlement=settlementStatus.summary(userId);response.setSettlement(settlement);if(response.getTotalValue()!=null){response.setTotalValue(response.getTotalValue().add(settlement.restricted()));if(response.getTotalValue().signum()>0&&response.getCashAllocationPercent()!=null){response.setCashAllocationPercent(response.getTotalCash().multiply(new BigDecimal("100")).divide(response.getTotalValue(),2,RoundingMode.HALF_UP));response.setInvestedAllocationPercent(response.getHoldingsValue().multiply(new BigDecimal("100")).divide(response.getTotalValue(),2,RoundingMode.HALF_UP));}}}
        return response;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PortfolioResponse getPortfolioByEmail(String email) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        return getPortfolio(user.getId());
    }

    private BigDecimal currentValue(Holding holding) {
        return holding.getStock()
                .getCurrentPrice()
                .multiply(BigDecimal.valueOf(holding.getQuantity()))
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }
}
