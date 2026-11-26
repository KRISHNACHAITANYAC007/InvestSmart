package com.example.investsmart.chat.agent;

import java.math.*;
import java.security.Principal;
import java.util.*;

/** Deterministic explanations of authorized evidence; no LLM, external request or account action. */
public class PortfolioAnalysisService {
    public enum Status { COMPLETE_EQUITY_VIEW, EMPTY_EQUITY_VIEW, INCOMPLETE_VALUATION, INCOMPLETE_COST_HISTORY }
    public record Listing(String instrumentKey, @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal value, @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal equityWeightPercent) {}
    public record Analysis(PortfolioReadTool.Evidence evidence, Status status, @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal equityValue,
                           @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal equityUnrealizedPnl, List<Listing> largestListings,
                           List<String> explanation) {
        public Analysis { largestListings=List.copyOf(largestListings); explanation=List.copyOf(explanation); }
    }
    private final PortfolioReadTool portfolio;
    public PortfolioAnalysisService(PortfolioReadTool portfolio) { this.portfolio=portfolio; }

    public Analysis analyze(Principal principal, boolean consent) {
        // Do not replace this path with model-supplied account identifiers or model calculations.
        return summarize(portfolio.invoke(principal,Map.of(),consent));
    }

    Analysis summarize(PortfolioReadTool.Evidence evidence) {
        var explanation=new ArrayList<>(evidence.limitations());
        explanation.add("This view covers equity listings only; it is not whole-account allocation or total trading performance.");
        explanation.add("Unrealized P&L is an existing holdings calculation, not today's change, realized P&L or a fee/interest statement.");
        explanation.add("Concentration is described by listing weight, not a prediction, risk score or buy/sell recommendation. Separate exchange listings are not merged by company.");
        var values=new TreeMap<String,BigDecimal>();
        BigDecimal sum=new BigDecimal("0.00"),pnl=new BigDecimal("0.00");
        boolean valid="EQUITY_HOLDINGS_ONLY".equals(evidence.scope())&&evidence.equityValue()!=null;
        boolean costComplete=true;
        for(var holding:evidence.holdings()) {
            if(holding.instrumentKey()==null||holding.instrumentKey().isBlank()||holding.quantity()<1
                    ||holding.value()==null||holding.value().signum()<=0||holding.quoteAt()==null
                    ||!"AVAILABLE".equals(holding.availability())
                    ||holding.quoteAt().isBefore(evidence.readAt().minusSeconds(60))
                    ||holding.quoteAt().isAfter(evidence.readAt().plusSeconds(5))) {
                valid=false;continue;
            }
            sum=sum.add(holding.value());
            values.merge(holding.instrumentKey(),holding.value(),BigDecimal::add);
            if(holding.unrealizedPnl()==null)costComplete=false;
            else pnl=pnl.add(holding.unrealizedPnl());
        }
        if(!valid||sum.compareTo(evidence.equityValue())!=0) {
            explanation.add("Some equity valuation evidence is missing, stale or inconsistent. Total value, P&L and concentration ranking are unavailable.");
            return new Analysis(evidence,Status.INCOMPLETE_VALUATION,null,null,List.of(),explanation);
        }
        if(values.isEmpty()) {
            explanation.add("There are no equity holdings in this view. Other account products may still exist.");
            return new Analysis(evidence,Status.EMPTY_EQUITY_VIEW,sum,pnl,List.of(),explanation);
        }
        final BigDecimal total=sum;
        var largest=values.entrySet().stream()
                .sorted(Map.Entry.<String,BigDecimal>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(3).map(e->new Listing(e.getKey(),e.getValue(),
                        e.getValue().multiply(new BigDecimal("100")).divide(total,2,RoundingMode.HALF_UP))).toList();
        explanation.add("The largest listing accounts for "+largest.getFirst().equityWeightPercent().toPlainString()
                +"% of the equity value in this view. A larger listing weight means more exposure to that listing within this subset.");
        if(!costComplete)explanation.add("Some holdings lack consistent cost history. Combined unrealized P&L is unavailable, not zero.");
        else explanation.add("Combined equity unrealized P&L is "+pnl.toPlainString()+" INR in the supplied snapshot. This is a simulated valuation, not cash profit.");
        return new Analysis(evidence,costComplete?Status.COMPLETE_EQUITY_VIEW:Status.INCOMPLETE_COST_HISTORY,
                sum,costComplete?pnl:null,largest,explanation);
    }
}
