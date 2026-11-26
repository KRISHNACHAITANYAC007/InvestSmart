package com.example.investsmart.chat.agent;

import com.example.investsmart.repository.*;
import com.example.investsmart.service.PortfolioService;
import java.math.*;
import java.security.Principal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Internal AI-2 evidence only: no model call, order, refresh or external provider request. */
public class PortfolioReadTool {
    public record Holding(String instrumentKey, String symbol, int quantity, Instant quoteAt,
                          @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal value, @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal unrealizedPnl, @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal equityWeightPercent,
                          String availability) {}
    public record Evidence(Instant readAt, String scope, List<Holding> holdings,
                           @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal equityValue, List<String> limitations) {
        public Evidence { holdings=List.copyOf(holdings); limitations=List.copyOf(limitations); }
    }
    private final UserRepository users;
    private final PortfolioService portfolios;
    private final StockRepository stocks;
    private final ToolRegistry registry;
    private final Clock clock;
    public PortfolioReadTool(UserRepository users, PortfolioService portfolios, StockRepository stocks,
                              ToolRegistry registry, Clock clock) {
        this.users=users; this.portfolios=portfolios; this.stocks=stocks; this.registry=registry; this.clock=clock;
    }

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ, propagation=Propagation.REQUIRES_NEW)
    public Evidence invoke(Principal principal, Map<String,String> arguments, boolean consent) {
        if(principal==null||principal.getName()==null||principal.getName().isBlank())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var user=users.findByEmailIgnoreCase(principal.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        user.requireActive();
        if(!consent)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Explicit portfolio access consent is required.");
        registry.validate("getMyPortfolio",arguments); // Exact empty schema: no user/account selector.
        var portfolio=portfolios.getPortfolioByEmail(user.getEmail());
        var holdings=portfolio.getHoldings();
        if(holdings==null||holdings.size()>1000)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Bounded portfolio evidence unavailable.");
        if(holdings.stream().anyMatch(h->h==null||h.getStockId()==null||h.getQuantity()==null||h.getQuantity()<1))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Portfolio evidence unavailable.");
        var byId=stocks.findAllById(holdings.stream().map(h->h.getStockId()).distinct().toList()).stream()
                .collect(Collectors.toMap(s->s.getId(),Function.identity()));
        Instant now=clock.instant();
        var rows=new ArrayList<Holding>();
        boolean complete=true;
        for(var h:holdings) {
            var stock=byId.get(h.getStockId());
            Instant quoteAt=stock==null||stock.getMarketDataUpdatedAt()==null?null:
                    stock.getMarketDataUpdatedAt().atZone(ZoneId.of("Asia/Kolkata")).toInstant();
            boolean fresh=quoteAt!=null&&!quoteAt.isBefore(now.minusSeconds(60))&&!quoteAt.isAfter(now.plusSeconds(5));
            boolean identity=stock!=null&&h.getInstrumentKey()!=null&&h.getInstrumentKey().equals(stock.getInstrumentKey());
            boolean price=stock!=null&&stock.getCurrentPrice()!=null&&stock.getCurrentPrice().signum()>0
                    &&h.getCurrentPrice()!=null&&h.getCurrentPrice().compareTo(stock.getCurrentPrice())==0;
            BigDecimal expected=price?stock.getCurrentPrice().multiply(BigDecimal.valueOf(h.getQuantity())).setScale(2,RoundingMode.HALF_UP):null;
            boolean valid=fresh&&identity&&price&&h.getCurrentValue()!=null&&h.getCurrentValue().compareTo(expected)==0;
            if(!valid)complete=false;
            rows.add(new Holding(h.getInstrumentKey(),h.getSymbol(),h.getQuantity(),quoteAt,
                    valid?h.getCurrentValue():null,valid&&portfolio.isPerformanceAvailable()?h.getUnrealizedPnl():null,
                    null,valid?"AVAILABLE":"UNAVAILABLE_OR_STALE"));
        }
        BigDecimal total=complete?rows.stream().map(Holding::value).reduce(new BigDecimal("0.00"),BigDecimal::add):null;
        var weighted=new ArrayList<Holding>();
        for(var h:rows)weighted.add(new Holding(h.instrumentKey(),h.symbol(),h.quantity(),h.quoteAt(),h.value(),h.unrealizedPnl(),
                total!=null&&total.signum()>0?h.value().multiply(new BigDecimal("100")).divide(total,2,RoundingMode.HALF_UP):null,h.availability()));
        return new Evidence(now,"EQUITY_HOLDINGS_ONLY",weighted,total,List.of(
                "Educational paper simulation; not financial advice.",
                "Equity weights exclude cash, options, financed positions and IPOs; not whole-account allocation.",
                "Cached quote timestamps are application observations, not guaranteed exchange tick timestamps.",
                "No prior valuation or news was retrieved: daily change and its causes are not established.",
                "Missing cost history or stale data must not be interpreted as zero P&L."));
    }
}
