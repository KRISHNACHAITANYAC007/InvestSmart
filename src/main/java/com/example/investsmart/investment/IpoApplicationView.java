package com.example.investsmart.investment;
import java.math.BigDecimal;
import java.time.*;
public record IpoApplicationView(Long id,String ipoId,String name,String symbol,String status,Integer lots,
        Integer appliedQuantity,Integer allottedQuantity,BigDecimal reservedAmount,BigDecimal allocatedCost,
        BigDecimal issuePrice,BigDecimal probability,BigDecimal subscription,String modelVersion,String reason,
        LocalDate allotmentDate,LocalDate listingDate,Instant appliedAt,Instant resolvedAt,Instant sourceUpdatedAt,
        Long listingOrderId,Boolean assumedIssuePrice) {
    public static IpoApplicationView from(IpoApplication a){return new IpoApplicationView(a.getId(),a.getIpoId(),a.getName(),a.getSymbol(),a.getStatus(),a.getLots(),a.getAppliedQuantity(),a.getAllottedQuantity(),a.getReservedAmount(),a.getAllocatedCost(),a.getIssuePrice(),a.getProbability(),a.getSubscription(),a.getModelVersion(),a.getReason(),a.getAllotmentDate(),a.getListingDate(),a.getAppliedAt(),a.getResolvedAt(),a.getSourceUpdatedAt(),a.getListingOrderId(),a.getAssumedIssuePrice());}
}
