package com.example.investsmart.dto;

import java.math.BigDecimal;
import java.util.List;

public class PortfolioResponse {
    private com.example.investsmart.settlement.SettlementStatus.Summary settlement;
    public com.example.investsmart.settlement.SettlementStatus.Summary getSettlement(){return settlement;}
    public void setSettlement(com.example.investsmart.settlement.SettlementStatus.Summary value){settlement=value;}
    private BigDecimal reservedPaperFunds=BigDecimal.ZERO;
    private java.util.Map<String,Object> paperTrading=java.util.Map.of();
    public BigDecimal getReservedPaperFunds(){return reservedPaperFunds;}public void setReservedPaperFunds(BigDecimal value){reservedPaperFunds=value;}
    public java.util.Map<String,Object> getPaperTrading(){return paperTrading;}public void setPaperTrading(java.util.Map<String,Object> value){paperTrading=value;}
    private BigDecimal reservedOrderFunds=BigDecimal.ZERO;
    public BigDecimal getReservedOrderFunds(){return reservedOrderFunds;}public void setReservedOrderFunds(BigDecimal v){reservedOrderFunds=v;}
    private BigDecimal optionsValue=BigDecimal.ZERO,optionsCost=BigDecimal.ZERO,reservedOptionsFunds=BigDecimal.ZERO;
    public BigDecimal getOptionsValue(){return optionsValue;}public void setOptionsValue(BigDecimal v){optionsValue=v;}
    public BigDecimal getOptionsCost(){return optionsCost;}public void setOptionsCost(BigDecimal v){optionsCost=v;}
    public BigDecimal getReservedOptionsFunds(){return reservedOptionsFunds;}public void setReservedOptionsFunds(BigDecimal v){reservedOptionsFunds=v;}
    public boolean isValuationAvailable() { return totalValue != null; }

    private BigDecimal reservedIpoFunds = BigDecimal.ZERO;
    private BigDecimal unlistedIpoCost = BigDecimal.ZERO;
    private List<com.example.investsmart.investment.IpoApplicationView> ipoApplications = List.of();
    public BigDecimal getReservedIpoFunds(){return reservedIpoFunds;}
    public void setReservedIpoFunds(BigDecimal v){reservedIpoFunds=v;}
    public BigDecimal getUnlistedIpoCost(){return unlistedIpoCost;}
    public void setUnlistedIpoCost(BigDecimal v){unlistedIpoCost=v;}
    public BigDecimal getTotalCash(){return balance == null ? null : balance.add(settlement==null?BigDecimal.ZERO:settlement.restricted()).add(reservedIpoFunds).add(reservedOptionsFunds).add(reservedOrderFunds).add(reservedPaperFunds);}
    public List<com.example.investsmart.investment.IpoApplicationView> getIpoApplications(){return ipoApplications;}
    public void setIpoApplications(List<com.example.investsmart.investment.IpoApplicationView> v){ipoApplications=v;}

    private BigDecimal balance;
    private BigDecimal holdingsValue;
    private BigDecimal totalValue;
    private BigDecimal investedAmount;
    private BigDecimal realizedPnl;
    private BigDecimal unrealizedPnl;
    private BigDecimal cashAllocationPercent;
    private BigDecimal investedAllocationPercent;
    private boolean performanceAvailable;
    private List<HoldingResponse> holdings;
    private List<String> riskInsights;

    public PortfolioResponse() {
    }

    public PortfolioResponse(
            BigDecimal balance,
            BigDecimal holdingsValue,
            BigDecimal totalValue,
            BigDecimal investedAmount,
            BigDecimal realizedPnl,
            BigDecimal unrealizedPnl,
            BigDecimal cashAllocationPercent,
            BigDecimal investedAllocationPercent,
            boolean performanceAvailable,
            List<HoldingResponse> holdings,
            List<String> riskInsights) {

        this.balance = balance;
        this.holdingsValue = holdingsValue;
        this.totalValue = totalValue;
        this.investedAmount = investedAmount;
        this.realizedPnl = realizedPnl;
        this.unrealizedPnl = unrealizedPnl;
        this.cashAllocationPercent = cashAllocationPercent;
        this.investedAllocationPercent = investedAllocationPercent;
        this.performanceAvailable = performanceAvailable;
        this.holdings = holdings;
        this.riskInsights = riskInsights;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getHoldingsValue() {
        return holdingsValue;
    }

    public void setHoldingsValue(BigDecimal holdingsValue) {
        this.holdingsValue = holdingsValue;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getInvestedAmount() {
        return investedAmount;
    }

    public void setInvestedAmount(BigDecimal investedAmount) {
        this.investedAmount = investedAmount;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(BigDecimal realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }

    public BigDecimal getCashAllocationPercent() {
        return cashAllocationPercent;
    }

    public void setCashAllocationPercent(BigDecimal cashAllocationPercent) {
        this.cashAllocationPercent = cashAllocationPercent;
    }

    public BigDecimal getInvestedAllocationPercent() {
        return investedAllocationPercent;
    }

    public void setInvestedAllocationPercent(BigDecimal investedAllocationPercent) {
        this.investedAllocationPercent = investedAllocationPercent;
    }

    public boolean isPerformanceAvailable() {
        return performanceAvailable;
    }

    public void setPerformanceAvailable(boolean performanceAvailable) {
        this.performanceAvailable = performanceAvailable;
    }

    public List<HoldingResponse> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<HoldingResponse> holdings) {
        this.holdings = holdings;
    }

    public List<String> getRiskInsights() {
        return riskInsights;
    }

    public void setRiskInsights(List<String> riskInsights) {
        this.riskInsights = riskInsights;
    }
}
