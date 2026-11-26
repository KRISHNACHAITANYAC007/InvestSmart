package com.example.investsmart.service;

import com.example.investsmart.repository.*;
import com.example.investsmart.dto.OrderRequest;
import com.example.investsmart.market.*;
import com.example.investsmart.entity.User;
import java.math.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Persisted, fully reserved, day-valid equity limit and protective stop orders. */
@Service
public class ConditionalOrders {
    public record Ticket(Long stockId,Integer quantity,String side,String orderType,BigDecimal price,String requestId) {}
    private final JdbcTemplate db;private final UserRepository users;private final StockRepository stocks;private final HoldingRepository holdings;private final OrderService orders;private final MarketDataService market;private final Clock clock;private final com.example.investsmart.settlement.SettlementAccounting accounting;
    public ConditionalOrders(JdbcTemplate db,UserRepository users,StockRepository stocks,HoldingRepository holdings,OrderService orders,MarketDataService market,Clock clock,com.example.investsmart.settlement.SettlementAccounting accounting){this.accounting=accounting;this.db=db;this.users=users;this.stocks=stocks;this.holdings=holdings;this.orders=orders;this.market=market;this.clock=clock;}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    public List<Map<String,Object>> list(String email){var user=users.findByEmailIgnoreCase(email).orElseThrow();return db.queryForList("SELECT p.*,s.symbol,s.company_name FROM equity_pending_orders p LEFT JOIN stocks s ON s.id=p.stock_id WHERE p.user_id=? ORDER BY p.created_at DESC LIMIT 200",user.getId()).stream().map(this::view).toList();}
    private Map<String,Object> view(Map<String,Object> row){var result=new LinkedHashMap<String,Object>();row.forEach((k,v)->result.put(k.toLowerCase(Locale.ROOT),v));return result;}
    @Transactional public Map<String,Object> submit(String email,Ticket ticket){
        if(ticket==null||ticket.stockId()==null||ticket.quantity()==null||ticket.quantity()<1||ticket.quantity()>1000000||!List.of("BUY","SELL").contains(ticket.side()==null?"":ticket.side())||!List.of("LIMIT","STOP_MARKET").contains(ticket.orderType()==null?"":ticket.orderType())||ticket.price()==null||ticket.price().signum()<=0||ticket.price().scale()>2||ticket.price().compareTo(new BigDecimal("10000000"))>0||ticket.requestId()==null||!ticket.requestId().matches("[A-Za-z0-9-]{16,80}"))throw bad("Choose quantity, a positive limit/trigger price and order type.");
        if(ticket.side().equals("BUY")&&ticket.orderType().equals("STOP_MARKET"))throw bad("Protective stop-loss orders sell owned shares.");
        var user=users.findByEmailForUpdate(email).orElseThrow();user.requireActive();accounting.prepare(user);var existing=db.queryForList("SELECT * FROM equity_pending_orders WHERE user_id=? AND request_id=?",user.getId(),ticket.requestId());if(!existing.isEmpty())return view(existing.getFirst());
        var stock=stocks.findById(ticket.stockId()).orElseThrow(()->bad("Listing unavailable."));
        if(stock.getInstrumentKey()==null||!stock.getInstrumentKey().matches("(NSE|BSE)_EQ\\|.+"))throw bad("Select a supported equity listing.");
        String id=UUID.randomUUID().toString();
        BigDecimal reserved=BigDecimal.ZERO;
        if(ticket.side().equals("BUY")){
            reserved=ticket.price().multiply(BigDecimal.valueOf(ticket.quantity())).setScale(2,RoundingMode.HALF_UP);if(user.getVirtualBalance()==null||user.getVirtualBalance().compareTo(reserved)<0)throw bad("Insufficient available virtual cash.");accounting.reserve(user,"EQUITY_ORDER",id,reserved,true);
        }else{
            int held=holdings.findByUserIdAndStockId(user.getId(),stock.getId()).map(h->h.getQuantity()).orElse(0);int pending=db.queryForObject("SELECT COALESCE(SUM(units),0) FROM equity_pending_orders WHERE user_id=? AND stock_id=? AND side='SELL' AND status='PENDING'",Integer.class,user.getId(),stock.getId());pending+=db.queryForObject("SELECT COALESCE(SUM(reserved_units),0) FROM paper_tickets WHERE user_id=? AND stock_id=? AND position_id IS NULL AND status IN ('PENDING','TRIGGERED','PART_FILLED')",Integer.class,user.getId(),stock.getId());if(held-pending<ticket.quantity())throw bad("Insufficient unreserved shares on this listing.");
        }
        // Day orders entered after the regular session are not silently carried to another day.
        var now=ZonedDateTime.now(clock).withZoneSameInstant(com.example.investsmart.options.OptionMarket.INDIA);if(!now.toLocalTime().isBefore(LocalTime.of(15,30)))throw bad("Day orders must be placed before 15:30 IST; place a new order next trading day.");
        db.update("INSERT INTO equity_pending_orders(id,user_id,request_id,stock_id,units,side,order_kind,trigger_price,reserved_cash,status,created_at,valid_date) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id,user.getId(),ticket.requestId(),stock.getId(),ticket.quantity(),ticket.side(),ticket.orderType(),ticket.price(),reserved,"PENDING",Timestamp.from(Instant.now()),now.toLocalDate());return view(row(id));
    }
    private Map<String,Object> row(String id){return db.queryForMap("SELECT * FROM equity_pending_orders WHERE id=?",id);}
    @Transactional public void cancel(String email,String id){var user=users.findByEmailForUpdate(email).orElseThrow();user.requireActive();var rows=db.queryForList("SELECT * FROM equity_pending_orders WHERE id=? AND user_id=?",id,user.getId());if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Conditional order not found.");release(user,rows.getFirst(),"CANCELLED");}
    private void release(User user,Map<String,Object> row,String status){if(!"PENDING".equals(row.get("status")))return;accounting.prepare(user);accounting.release(user,"EQUITY_ORDER",(String)row.get("id"),(BigDecimal)row.get("reserved_cash"),status);db.update("UPDATE equity_pending_orders SET status=?,reserved_cash=0 WHERE id=?",status,row.get("id"));}
    public List<String> pending(){return db.queryForList("SELECT id FROM equity_pending_orders WHERE status='PENDING' ORDER BY COALESCE(last_checked_at,created_at) LIMIT 20",String.class);}
    public void touch(String id){db.update("UPDATE equity_pending_orders SET last_checked_at=? WHERE id=?",Timestamp.from(Instant.now()),id);}
    @Transactional public void process(String id){
        var candidate=row(id);var user=users.findByIdForUpdate(((Number)candidate.get("user_id")).longValue()).orElseThrow();var row=row(id);if(!"PENDING".equals(row.get("status")))return;
        var now=ZonedDateTime.now(clock).withZoneSameInstant(com.example.investsmart.options.OptionMarket.INDIA);var date=((java.sql.Date)row.get("valid_date")).toLocalDate();
        if(date.isBefore(now.toLocalDate())||!now.toLocalTime().isBefore(LocalTime.of(15,30))){release(user,row,"EXPIRED");return;}if(!"ACTIVE".equals(user.getAccountStatus())){release(user,row,"CANCELLED");return;}
        var listing=stocks.findById(((Number)row.get("stock_id")).longValue());if(listing.isEmpty()){release(user,row,"CANCELLED");return;}var stock=listing.get();var quote=market.getQuote(new MarketInstrument(stock.getInstrumentKey(),stock.getSymbol(),stock.getCompanyName(),stock.getExchange(),stock.getIsin()));
        if(quote==null||!stock.getInstrumentKey().equals(quote.getInstrumentKey())||quote.getCurrentPrice()==null||quote.getCurrentPrice().signum()<=0)return;
        BigDecimal threshold=(BigDecimal)row.get("trigger_price"),price=quote.getCurrentPrice();boolean buy="BUY".equals(row.get("side")),limit="LIMIT".equals(row.get("order_kind"));boolean triggered=limit?(buy?price.compareTo(threshold)<=0:price.compareTo(threshold)>=0):price.compareTo(threshold)<=0;if(!triggered)return;
        // Release this reservation within the same transaction; regular execution rechecks quote/session.
        // Any rejection rolls everything back, leaving the order pending and funds/units reserved.
        release(user,row,"EXECUTING");var request=new OrderRequest();request.setStockId(stock.getId());request.setQuantity(((Number)row.get("units")).intValue());
        var fill=buy?orders.buyStock(user.getEmail(),request,threshold):orders.sellStock(user.getEmail(),request,limit?threshold:null);
        db.update("UPDATE equity_pending_orders SET status='FILLED',order_id=? WHERE id=?",fill.getId(),id);
    }
}
