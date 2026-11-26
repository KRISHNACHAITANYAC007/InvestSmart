package com.example.investsmart.options;

import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.market.MarketDataService;
import com.example.investsmart.service.NseMarketSessionService;
import java.math.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OptionTrading {
    public record Ticket(String instrumentKey,LocalDate expiry,String side,String orderType,Integer lots,BigDecimal price,String requestId) {}
    private final JdbcTemplate db;private final UserRepository users;private final OptionMarket market;private final NseMarketSessionService sessions;private final MarketDataService prices;private final Clock clock;private final com.example.investsmart.settlement.SettlementAccounting accounting;
    public OptionTrading(JdbcTemplate db,UserRepository users,OptionMarket market,NseMarketSessionService sessions,MarketDataService prices,Clock clock,com.example.investsmart.settlement.SettlementAccounting accounting){this.accounting=accounting;this.db=db;this.users=users;this.market=market;this.sessions=sessions;this.prices=prices;this.clock=clock;}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private static BigDecimal money(BigDecimal amount){return amount.setScale(2,RoundingMode.HALF_UP);}
    private static BigDecimal decimal(Map<String,Object> row,String field){return (BigDecimal)row.get(field);}
    private static int units(Map<String,Object> row){return ((Number)row.get("units")).intValue();}
    private static LocalDate expiry(Map<String,Object> row){return ((java.sql.Date)row.get("expiry")).toLocalDate();}
    static Map<String,Object> view(Map<String,Object> row){var result=new LinkedHashMap<String,Object>();row.forEach((k,v)->result.put(k.toLowerCase(Locale.ROOT),v));return result;}
    public Map<String,Object> account(String email){var user=users.findByEmailIgnoreCase(email).orElseThrow();Map<String,Object> result=new LinkedHashMap<>();result.put("cash",user.getVirtualBalance());result.put("totals",totals(user.getId()));result.put("positions",db.queryForList("SELECT * FROM option_positions WHERE user_id=? AND units>0 ORDER BY expiry,strike_price,option_type",user.getId()).stream().map(OptionTrading::view).toList());result.put("orders",db.queryForList("SELECT * FROM option_orders WHERE user_id=? ORDER BY created_at DESC LIMIT 200",user.getId()).stream().map(OptionTrading::view).toList());return result;}
    @Transactional public Map<String,Object> submit(String email,Ticket ticket){
        if(ticket==null||ticket.requestId()==null||!ticket.requestId().matches("[a-zA-Z0-9-]{16,80}")||ticket.expiry()==null||ticket.instrumentKey()==null||!List.of("BUY","SELL").contains(ticket.side()==null?"":ticket.side())||!List.of("MARKET","LIMIT","STOP_MARKET").contains(ticket.orderType()==null?"":ticket.orderType())||ticket.lots()==null||ticket.lots()<1||ticket.lots()>1000)throw bad("Choose a contract, side, order type and whole lots.");
        var user=users.findByEmailForUpdate(email).orElseThrow();user.requireActive();accounting.prepare(user);
        var existing=db.queryForList("SELECT * FROM option_orders WHERE user_id=? AND request_id=?",user.getId(),ticket.requestId());if(!existing.isEmpty())return existing.getFirst();
        var quote=market.chain(ticket.expiry()).stream().filter(q->q.contract().instrumentKey().equals(ticket.instrumentKey())).findFirst().orElseThrow(()->bad("Selected contract is unavailable."));
        var c=quote.contract();if(expiredNow(c.expiry()))throw bad("This contract has expired.");
        long count=(long)ticket.lots()*c.lotSize();if(count>c.freezeQuantity()||count>Integer.MAX_VALUE)throw bad("Quantity exceeds this contract's provider freeze limit.");int quantity=(int)count;
        if(!ticket.orderType().equals("MARKET")&&(ticket.price()==null||ticket.price().signum()<=0||ticket.price().scale()>2||ticket.price().compareTo(new BigDecimal("10000000"))>0))throw bad("Enter a positive limit/trigger premium with at most two decimals.");
        // Stop-market buys have no finite reservation ceiling; expose protective sell stops only.
        if(ticket.side().equals("BUY")&&ticket.orderType().equals("STOP_MARKET"))throw bad("Stop-loss orders close owned positions; choose SELL.");
        String id=UUID.randomUUID().toString();
        BigDecimal reserved=BigDecimal.ZERO;
        if(ticket.side().equals("BUY")){
            BigDecimal premium=ticket.orderType().equals("LIMIT")?ticket.price():checkedExecutable(quote,"BUY",quantity);
            reserved=money(premium.multiply(BigDecimal.valueOf(quantity)));
            if(user.getVirtualBalance()==null||user.getVirtualBalance().compareTo(reserved)<0)throw bad("Insufficient available virtual cash.");
            accounting.reserve(user,"OPTION_ORDER",id,reserved,true);
        }else{
            var positions=db.queryForList("SELECT * FROM option_positions WHERE user_id=? AND instrument_key=? AND expiry=?",user.getId(),c.instrumentKey(),c.expiry());
            int held=positions.isEmpty()?0:units(positions.getFirst());
            Integer pending=db.queryForObject("SELECT COALESCE(SUM(units),0) FROM option_orders WHERE user_id=? AND instrument_key=? AND expiry=? AND side='SELL' AND status='PENDING'",Integer.class,user.getId(),c.instrumentKey(),c.expiry());
            pending+=db.queryForObject("SELECT COALESCE(SUM(reserved_units),0) FROM paper_tickets WHERE user_id=? AND instrument_key=? AND expiry=? AND status IN ('PENDING','TRIGGERED','PART_FILLED')",Integer.class,user.getId(),c.instrumentKey(),c.expiry());
            if(held-pending<quantity)throw bad("Insufficient unreserved option units. Naked option selling is not supported.");
        }
        Instant now=clock.instant();
        db.update("INSERT INTO option_orders(id,user_id,request_id,instrument_key,symbol,expiry,strike_price,option_type,lot_size,units,side,order_kind,trigger_price,reserved_cash,status,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",id,user.getId(),ticket.requestId(),c.instrumentKey(),c.symbol(),c.expiry(),c.strike(),c.type(),c.lotSize(),quantity,ticket.side(),ticket.orderType(),ticket.price(),reserved,"PENDING",Timestamp.from(now));
        if(ticket.orderType().equals("MARKET")){sessions.requireOpenSession("NSE");fill(user,order(id),quote);}
        return order(id);
    }
    private Map<String,Object> order(String id){return db.queryForMap("SELECT * FROM option_orders WHERE id=?",id);}
    static boolean expired(LocalDate date){var now=ZonedDateTime.now(OptionMarket.INDIA);return date.isBefore(now.toLocalDate())||(date.equals(now.toLocalDate())&&!now.toLocalTime().isBefore(LocalTime.of(15,30)));}
    private boolean expiredNow(LocalDate date){var now=clock.instant().atZone(OptionMarket.INDIA);return date.isBefore(now.toLocalDate())||(date.equals(now.toLocalDate())&&!now.toLocalTime().isBefore(LocalTime.of(15,30)));}
    static boolean triggered(String kind,String side,BigDecimal threshold,BigDecimal price){if(price==null||price.signum()<=0)return false;return kind.equals("MARKET")||(kind.equals("LIMIT")?(side.equals("BUY")?price.compareTo(threshold)<=0:price.compareTo(threshold)>=0):price.compareTo(threshold)<=0);}
    static BigDecimal executable(OptionMarket.Quote q,String side,int units){
        return executable(q,side,units,Instant.now());
    }
    private BigDecimal checkedExecutable(OptionMarket.Quote q,String side,int units){return executable(q,side,units,clock.instant());}
    private static BigDecimal executable(OptionMarket.Quote q,String side,int units,Instant now){
        if(q==null||q.fetchedAt()==null||q.fetchedAt().isBefore(now.minusSeconds(60))||q.fetchedAt().isAfter(now.plusSeconds(5)))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"A fresh option quote is unavailable. No fill was made; refresh and retry.");
        BigDecimal price=side.equals("BUY")?q.ask():q.bid();int depth=side.equals("BUY")?q.askQuantity():q.bidQuantity();if(price==null||price.compareTo(new BigDecimal("0.01"))<0||price.compareTo(new BigDecimal("10000000"))>0||price.stripTrailingZeros().scale()>2||depth<units)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Executable option depth is unavailable for this quantity. No fill was made.");return price;
    }
    private void fill(com.example.investsmart.entity.User user,Map<String,Object> order,OptionMarket.Quote quote){
        int count=units(order);String side=(String)order.get("side");BigDecimal price=checkedExecutable(quote,side,count),amount=money(price.multiply(BigDecimal.valueOf(count)));
        if(!triggered((String)order.get("order_kind"),side,decimal(order,"trigger_price"),price))return;
        var positions=db.queryForList("SELECT * FROM option_positions WHERE user_id=? AND instrument_key=? AND expiry=?",user.getId(),order.get("instrument_key"),expiry(order));
        var position=positions.isEmpty()?null:positions.getFirst();int held=position==null?0:units(position);BigDecimal cost=position==null?BigDecimal.ZERO:decimal(position,"cost_amount");BigDecimal realized=BigDecimal.ZERO;
        if(side.equals("BUY")){
            BigDecimal reserved=decimal(order,"reserved_cash");if(amount.compareTo(reserved)>0)return;
            if(held>Integer.MAX_VALUE-count)throw bad("Position quantity limit exceeded.");
            accounting.prepare(user);accounting.spend(user,"OPTION_ORDER",(String)order.get("id"),amount,"FILL");accounting.release(user,"OPTION_ORDER",(String)order.get("id"),reserved.subtract(amount),"PRICE");held+=count;cost=cost.add(amount);
        }else{
            if(held<count)throw bad("Option position no longer covers this order.");
            BigDecimal removed=count==held?cost:money(cost.multiply(BigDecimal.valueOf(count)).divide(BigDecimal.valueOf(held),8,RoundingMode.HALF_UP));
            realized=amount.subtract(removed);held-=count;cost=cost.subtract(removed);accounting.prepare(user);accounting.pending(user,"OPTION_SELL:"+order.get("id"),"NSE","FNO",accounting.tradeDate(),amount,true);
        }
        users.save(user);
        BigDecimal mark=quote.bid()!=null&&quote.bid().signum()>0?quote.bid():null;
        if(position==null)db.update("INSERT INTO option_positions(user_id,instrument_key,symbol,expiry,strike_price,option_type,lot_size,units,cost_amount,last_price,mark_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",user.getId(),order.get("instrument_key"),order.get("symbol"),expiry(order),order.get("strike_price"),order.get("option_type"),order.get("lot_size"),held,cost,mark,Timestamp.from(clock.instant()));
        else db.update("UPDATE option_positions SET units=?,cost_amount=?,last_price=?,mark_at=? WHERE user_id=? AND instrument_key=? AND expiry=?",held,cost,mark,Timestamp.from(clock.instant()),user.getId(),order.get("instrument_key"),expiry(order));
        db.update("UPDATE option_orders SET status='FILLED',fill_price=?,realized_pnl=?,reserved_cash=0,filled_at=? WHERE id=? AND status='PENDING'",price,realized,Timestamp.from(clock.instant()),order.get("id"));
    }
    @Transactional public void cancel(String email,String id){var user=users.findByEmailForUpdate(email).orElseThrow();user.requireActive();var rows=db.queryForList("SELECT * FROM option_orders WHERE id=? AND user_id=?",id,user.getId());if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Option order not found.");release(user,rows.getFirst(),"CANCELLED");}
    private void release(com.example.investsmart.entity.User user,Map<String,Object> order,String status){if(!"PENDING".equals(order.get("status")))return;accounting.prepare(user);accounting.release(user,"OPTION_ORDER",(String)order.get("id"),decimal(order,"reserved_cash"),status);db.update("UPDATE option_orders SET status=?,reserved_cash=0 WHERE id=?",status,order.get("id"));}
    @Transactional public void process(String id){
        var candidate=order(id);var user=users.findByIdForUpdate(((Number)candidate.get("user_id")).longValue()).orElseThrow();var order=order(id);if(!"PENDING".equals(order.get("status")))return;
        if(expiredNow(expiry(order))){release(user,order,"EXPIRED");return;}if(!"ACTIVE".equals(user.getAccountStatus())){release(user,order,"CANCELLED");return;}
        sessions.requireOpenSession("NSE");var quote=market.chain(expiry(order)).stream().filter(q->q.contract().instrumentKey().equals(order.get("instrument_key"))).findFirst().orElseThrow();
        if(quote.contract().lotSize()!=((Number)order.get("lot_size")).intValue()||quote.contract().freezeQuantity()<units(order)||quote.contract().strike().compareTo(decimal(order,"strike_price"))!=0||!quote.contract().type().equals(order.get("option_type")))return;
        fill(user,order,quote);
    }
    public List<String> pending(){return db.queryForList("SELECT id FROM option_orders WHERE status='PENDING' ORDER BY COALESCE(last_checked_at,created_at) LIMIT 20",String.class);}
    public void touch(String id){db.update("UPDATE option_orders SET last_checked_at=? WHERE id=?",Timestamp.from(clock.instant()),id);}
    public void mark(List<OptionMarket.Quote> quotes){for(var q:quotes)db.update("UPDATE option_positions SET last_price=?,mark_at=? WHERE instrument_key=? AND expiry=? AND units>0",q.bid()!=null&&q.bid().signum()>0?q.bid():null,Timestamp.from(q.fetchedAt()),q.contract().instrumentKey(),q.contract().expiry());}
    public List<Map<String,Object>> due(){return db.queryForList("SELECT user_id,instrument_key,expiry FROM option_positions WHERE units>0 AND expiry<=? ORDER BY expiry LIMIT 50",clock.instant().atZone(OptionMarket.INDIA).toLocalDate());}
    @Transactional public void settle(long userId,String key,LocalDate date){
        var now=clock.instant().atZone(OptionMarket.INDIA);if(date.isAfter(now.toLocalDate())||(date.equals(now.toLocalDate())&&now.toLocalTime().isBefore(LocalTime.of(18,0))))return;
        var user=users.findByIdForUpdate(userId).orElseThrow();
        var rows=db.queryForList("SELECT * FROM option_positions WHERE user_id=? AND instrument_key=? AND expiry=? AND units>0",userId,key,date);if(rows.isEmpty())return;
        // Educational cash-settlement proxy: the underlying's exact expiry-date daily close.
        // Missing date/price leaves settlement pending; never use a later quote or fabricate zero.
        BigDecimal close=prices.getHistory("NIFTY 50","1Y").stream().filter(c->c.getTimestamp()!=null&&c.getTimestamp().toLocalDate().equals(date)&&c.getPrice()!=null&&c.getPrice().signum()>0).map(c->c.getPrice()).findFirst().orElse(null);if(close==null)return;
        for(var pending:db.queryForList("SELECT * FROM option_orders WHERE user_id=? AND instrument_key=? AND expiry=? AND status='PENDING'",userId,key,date))release(user,pending,"EXPIRED");
        var position=rows.getFirst();BigDecimal strike=decimal(position,"strike_price");BigDecimal payout=("CE".equals(position.get("option_type"))?close.subtract(strike):strike.subtract(close)).max(BigDecimal.ZERO);
        BigDecimal amount=money(payout.multiply(BigDecimal.valueOf(units(position))));accounting.prepare(user);
        String id=UUID.randomUUID().toString();accounting.pending(user,"OPTION_EXPIRY:"+id,"NSE","FNO",date,amount,true);
        db.update("INSERT INTO option_orders(id,user_id,request_id,instrument_key,symbol,expiry,strike_price,option_type,lot_size,units,side,order_kind,reserved_cash,status,fill_price,realized_pnl,created_at,filled_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",id,userId,id,key,position.get("symbol"),date,strike,position.get("option_type"),position.get("lot_size"),units(position),"SETTLEMENT","EXPIRY_CLOSE",BigDecimal.ZERO,"FILLED",payout,amount.subtract(decimal(position,"cost_amount")),Timestamp.from(clock.instant()),Timestamp.from(clock.instant()));
        db.update("UPDATE option_positions SET units=0,cost_amount=0,last_price=?,mark_at=? WHERE user_id=? AND instrument_key=? AND expiry=?",payout,Timestamp.from(clock.instant()),userId,key,date);
    }
    private static BigDecimal percentage(BigDecimal value,BigDecimal total){return total==null||total.signum()==0?BigDecimal.ZERO:value.multiply(BigDecimal.valueOf(100)).divide(total,2,RoundingMode.HALF_UP);}
    public record Totals(BigDecimal value,BigDecimal cost,BigDecimal reserved,BigDecimal realized) {}
    public Totals totals(long userId){
        BigDecimal value=BigDecimal.ZERO,cost=BigDecimal.ZERO;
        for(var row:db.queryForList("SELECT * FROM option_positions WHERE user_id=? AND units>0",userId)){
            cost=cost.add(decimal(row,"cost_amount"));BigDecimal price=decimal(row,"last_price");
            if(expiredNow(expiry(row))||price==null)value=null;else if(value!=null)value=value.add(price.multiply(BigDecimal.valueOf(units(row))));
        }
        return new Totals(value==null?null:money(value),cost,db.queryForObject("SELECT COALESCE(SUM(reserved_cash),0) FROM option_orders WHERE user_id=? AND status='PENDING'",BigDecimal.class,userId),db.queryForObject("SELECT COALESCE(SUM(realized_pnl),0) FROM option_orders WHERE user_id=? AND status='FILLED'",BigDecimal.class,userId));
    }
    public void enrich(com.example.investsmart.dto.PortfolioResponse response,long userId){
        var t=totals(userId);BigDecimal equityReserved=db.queryForObject("SELECT COALESCE(SUM(reserved_cash),0) FROM equity_pending_orders WHERE user_id=? AND status='PENDING'",BigDecimal.class,userId);response.setReservedOrderFunds(equityReserved);response.setOptionsValue(t.value());response.setOptionsCost(t.cost());response.setReservedOptionsFunds(t.reserved());
        if(response.getInvestedAmount()!=null)response.setInvestedAmount(response.getInvestedAmount().add(t.cost()));
        if(response.getRealizedPnl()!=null)response.setRealizedPnl(response.getRealizedPnl().add(t.realized()));
        if(response.getHoldingsValue()==null||t.value()==null){response.setHoldingsValue(null);response.setTotalValue(null);response.setUnrealizedPnl(null);response.setCashAllocationPercent(null);response.setInvestedAllocationPercent(null);return;}
        response.setHoldingsValue(response.getHoldingsValue().add(t.value()));
        if(response.getTotalValue()!=null){response.setTotalValue(response.getTotalValue().add(t.value()).add(t.reserved()).add(equityReserved));response.setCashAllocationPercent(percentage(response.getTotalCash(),response.getTotalValue()));response.setInvestedAllocationPercent(percentage(response.getHoldingsValue(),response.getTotalValue()));}
        if(response.getUnrealizedPnl()!=null)response.setUnrealizedPnl(response.getUnrealizedPnl().add(t.value().subtract(t.cost())));
    }
}
