package com.example.investsmart.trading;

import com.example.investsmart.entity.*;
import com.example.investsmart.repository.*;
import com.example.investsmart.market.*;
import com.example.investsmart.service.NseMarketSessionService;
import com.example.investsmart.settlement.*;
import java.math.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Disclosed 2x equity MIS simulation; short entry turnover is never credited as cash. */
@Service
public class IntradayTrading {
    public static final LocalTime CUTOFF=LocalTime.of(15,10);
    private static final BigDecimal TWO=new BigDecimal("2");
    private final JdbcTemplate db;private final UserRepository users;private final StockRepository stocks;
    private final MarketDataService market;private final NseMarketSessionService sessions;private final Clock clock;
    private final PaperPolicy policy;private final SettlementAccounting accounting;private final SettlementStatus status;
    public IntradayTrading(JdbcTemplate db,UserRepository users,StockRepository stocks,MarketDataService market,NseMarketSessionService sessions,Clock clock,PaperPolicy policy,SettlementAccounting accounting,SettlementStatus status){this.db=db;this.users=users;this.stocks=stocks;this.market=market;this.sessions=sessions;this.clock=clock;this.policy=policy;this.accounting=accounting;this.status=status;}
    private record Prepared(Stock stock,BigDecimal price,BigDecimal ceiling,BigDecimal required,BigDecimal fee){}
    private Instant now(){return clock.instant();}private ZonedDateTime india(){return now().atZone(SettlementCalendar.INDIA);}
    private Timestamp stamp(){return Timestamp.from(now());}
    private static BigDecimal money(BigDecimal value){return value.setScale(2,RoundingMode.HALF_UP);}
    private static BigDecimal margin(BigDecimal value){return value.divide(TWO,2,RoundingMode.CEILING);}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private BigDecimal amount(Map<String,Object> row,String key){return (BigDecimal)row.get(key);}
    private int units(Map<String,Object> row){return ((Number)row.get("quantity")).intValue();}
    private long owner(Map<String,Object> row){return ((Number)row.get("user_id")).longValue();}
    private String version(){return policy.version()+"-mis1";}
    private Map<String,Object> view(Map<String,Object> row){var result=new LinkedHashMap<String,Object>();row.forEach((k,v)->result.put(k.toLowerCase(Locale.ROOT),v));return result;}
    private Map<String,Object> row(String id){return view(db.queryForMap("SELECT * FROM paper_tickets WHERE id=? AND product='MIS'",id));}
    public boolean ticket(String id){return db.queryForObject("SELECT COUNT(*) FROM paper_tickets WHERE id=? AND product='MIS'",Integer.class,id)>0;}
    public boolean positionOwned(String id,long user){return db.queryForObject("SELECT COUNT(*) FROM paper_intraday_positions WHERE id=? AND user_id=?",Integer.class,id,user)>0;}
    private Map<String,Object> position(String id,long user){var found=db.queryForList("SELECT * FROM paper_intraday_positions WHERE id=? AND user_id=? AND quantity>0",id,user);if(found.isEmpty())throw bad("Owned open intraday position unavailable.");return found.getFirst();}
    private boolean active(Map<String,Object> row){return List.of("PENDING","TRIGGERED").contains(row.get("status"));}
    private BigDecimal price(Stock stock){
        MarketQuote quote;
        try{quote=market.getQuote(new MarketInstrument(stock.getInstrumentKey(),stock.getSymbol(),stock.getCompanyName(),stock.getExchange(),stock.getIsin()));}
        catch(com.example.investsmart.exception.MarketStatusUnavailableException unavailable){throw unavailable;}
        catch(RuntimeException failure){throw new com.example.investsmart.exception.MarketStatusUnavailableException("Fresh intraday market data is unavailable. No fill was made.",failure);}
        Instant fetched=quote==null||quote.getLastUpdated()==null?null:quote.getLastUpdated().atZone(SettlementCalendar.INDIA).toInstant();
        if(quote==null||!stock.getInstrumentKey().equals(quote.getInstrumentKey())||quote.getCurrentPrice()==null||quote.getCurrentPrice().signum()<=0||quote.getCurrentPrice().compareTo(new BigDecimal("10000000"))>0||quote.getCurrentPrice().stripTrailingZeros().scale()>2||fetched==null||fetched.isBefore(now().minusSeconds(60))||fetched.isAfter(now().plusSeconds(5)))throw new com.example.investsmart.exception.MarketStatusUnavailableException("A fresh intraday listing price is unavailable. No fill was made.",null);
        return money(quote.getCurrentPrice());
    }
    private Prepared prepare(long user,PaperTrading.Ticket t){
        if(t==null||!"MIS".equals(t.product())||t.stockId()==null||t.instrumentKey()!=null||t.expiry()!=null||t.quantity()==null||t.quantity()<1||t.quantity()>1000000||!Set.of("BUY","SELL").contains(t.side()==null?"":t.side())||!Set.of("QUICK","REGULAR").contains(t.variety()==null?"":t.variety())||!Set.of("MARKET","LIMIT","STOP_LIMIT","STOP_MARKET").contains(t.orderType()==null?"":t.orderType())||!Set.of("DAY","IOC").contains(t.validity()==null?"":t.validity())||t.legs()==null||t.legs()!=1)throw bad("MIS supports equity Quick Market or Regular Market/Limit/SL/SL-M, DAY/IOC and one whole-unit child. GTT/Iceberg are unavailable.");
        if("QUICK".equals(t.variety())&&!"MARKET".equals(t.orderType()))throw bad("Quick intraday entries use Market orders.");
        if(t.stopLossPercent()!=null||t.targetPercent()!=null||t.stopLossPrice()!=null||t.targetPrice()!=null)throw bad("Use position-reducing intraday stop orders; attached GTT exits are unavailable for MIS.");
        if(t.orderType().contains("LIMIT"))checkPrice(t.limitPrice());if(t.orderType().startsWith("STOP"))checkPrice(t.triggerPrice());
        if("STOP_LIMIT".equals(t.orderType())&&("BUY".equals(t.side())?t.limitPrice().compareTo(t.triggerPrice())<0:t.limitPrice().compareTo(t.triggerPrice())>0))throw bad("SL buy limit must be at/above trigger; SL sell limit at/below trigger.");
        if(t.positionId()==null){
            if(india().toLocalTime().isBefore(LocalTime.of(9,15))||!india().toLocalTime().isBefore(CUTOFF))throw bad("MIS entries are available from 09:15 until 15:10 IST; closure is attempted from 15:10.");
            if(db.queryForObject("SELECT COUNT(*) FROM paper_intraday_positions WHERE user_id=? AND quantity>0 AND (close_requested=TRUE OR trade_date<?)",Integer.class,user,india().toLocalDate())>0)throw bad("Resolve pending intraday closures before opening another MIS position.");
        }else{
            var p=position(t.positionId(),user);String exit="LONG".equals(p.get("direction"))?"SELL":"BUY";
            if(((Number)p.get("stock_id")).longValue()!=t.stockId()||!exit.equals(t.side()))throw bad("Intraday exits must reduce the owned position on the same listing.");
            if(Boolean.TRUE.equals(p.get("close_requested")))throw bad("A full intraday closure is already pending.");
            int reserved=db.queryForObject("SELECT COALESCE(SUM(reserved_units),0) FROM paper_tickets WHERE user_id=? AND product='MIS' AND position_id=? AND status IN ('PENDING','TRIGGERED')",Integer.class,user,t.positionId());
            if(units(p)-reserved<t.quantity())throw bad("Insufficient unreserved intraday units.");
        }
        var stock=stocks.findById(t.stockId()).orElseThrow(()->bad("Listing unavailable."));
        if(stock.getInstrumentKey()==null||!stock.getInstrumentKey().matches("(NSE|BSE)_EQ\\|.+")||!Set.of("NSE","BSE").contains(stock.getExchange()==null?"":stock.getExchange()))throw bad("MIS requires an identified NSE/BSE equity listing.");
        BigDecimal price=price(stock),ceiling=t.orderType().contains("LIMIT")&&"BUY".equals(t.side())?t.limitPrice():money(price.max(t.triggerPrice()==null?price:t.triggerPrice()).max(t.limitPrice()==null?price:t.limitPrice()).multiply(new BigDecimal("1.05")));
        BigDecimal value=ceiling.multiply(BigDecimal.valueOf(t.quantity())),fee=policy.fee(value);
        return new Prepared(stock,price,ceiling,t.positionId()==null?margin(value).add(fee):BigDecimal.ZERO,fee);
    }
    private void checkPrice(BigDecimal price){if(price==null||price.signum()<=0||price.stripTrailingZeros().scale()>2||price.compareTo(new BigDecimal("10000000"))>0)throw bad("Enter a positive two-decimal limit/trigger price.");}
    @Transactional(readOnly=true) public Map<String,Object> preview(String email,PaperTrading.Ticket t){var u=users.findByEmailIgnoreCase(email).orElseThrow();u.requireActive();var p=prepare(u.getId(),t);var b=status.summary(u.getId());if(b.withdrawable()==null)throw bad("Settled cash is unavailable for this account; intraday preview cannot be calculated.");return Map.ofEntries(Map.entry("requiredCash",p.required()),Map.entry("availableCash",b.withdrawable()),Map.entry("estimatedFees",p.fee()),Map.entry("taxes",BigDecimal.ZERO),Map.entry("referencePrice",p.price()),Map.entry("ceilingPrice",p.ceiling()),Map.entry("lotSize",1),Map.entry("leverage",TWO),Map.entry("annualInterest",BigDecimal.ZERO),Map.entry("feeRate",policy.feeRate),Map.entry("feeCap",policy.feeCap),Map.entry("policyVersion",version()),Map.entry("expiresAt",now().plusSeconds(30)),Map.entry("intradayCutoff","15:10 IST simulation"));}
    @Transactional public Map<String,Object> submit(String email,PaperTrading.Ticket t){
        var u=users.findByEmailForUpdate(email).orElseThrow();u.requireActive();
        if(t==null||t.requestId()==null||!t.requestId().matches("[A-Za-z0-9-]{16,80}"))throw bad("A unique intraday request ID is required.");
        var prior=db.queryForList("SELECT * FROM paper_tickets WHERE user_id=? AND request_id=?",u.getId(),t.requestId());if(!prior.isEmpty())return view(prior.getFirst());
        accounting.prepare(u);var p=prepare(u.getId(),t);String id=UUID.randomUUID().toString();accounting.reserve(u,"PAPER_TICKET",id,p.required(),false);
        Instant end=india().toLocalDate().atTime(t.positionId()==null?CUTOFF:LocalTime.of(15,30)).atZone(SettlementCalendar.INDIA).toInstant();
        db.update("INSERT INTO paper_tickets(id,user_id,request_id,stock_id,instrument_key,position_id,side,product,variety,order_kind,validity,quantity,legs,lot_size,limit_price,trigger_price,ceiling_price,reserved_cash,reserved_units,status,policy_version,expires_at,created_at) VALUES(?,?,?,?,?,?,?,'MIS',?,?,?,?,1,1,?,?,?,?,?,'PENDING',?,?,?)",id,u.getId(),t.requestId(),t.stockId(),p.stock().getInstrumentKey(),t.positionId(),t.side(),t.variety(),t.orderType(),t.validity(),t.quantity(),t.limitPrice(),t.triggerPrice(),p.ceiling(),p.required(),t.positionId()==null?0:t.quantity(),version(),Timestamp.from(end),stamp());
        if("MARKET".equals(t.orderType())||"IOC".equals(t.validity()))processLocked(u,row(id));return row(id);
    }
    @Transactional public void process(String id){var candidate=row(id);var u=users.findByIdForUpdate(owner(candidate)).orElseThrow();db.update("UPDATE paper_tickets SET last_checked_at=? WHERE id=?",stamp(),id);processLocked(u,row(id));}
    private void processLocked(User u,Map<String,Object> r){
        if(!active(r))return;accounting.prepare(u);
        if(!version().equals(r.get("policy_version"))||!"ACTIVE".equals(u.getAccountStatus())||!((Timestamp)r.get("expires_at")).toInstant().isAfter(now())){release(u,r,"CANCELLED");return;}
        boolean entry=r.get("position_id")==null;Stock stock=stocks.findById(((Number)r.get("stock_id")).longValue()).orElseThrow();sessions.requireOpenSession(stock.getExchange());BigDecimal price=price(stock);
        String kind=(String)r.get("order_kind");boolean buy="BUY".equals(r.get("side"));
        if(kind.startsWith("STOP")&&!Boolean.TRUE.equals(r.get("triggered"))){boolean hit=buy?price.compareTo(amount(r,"trigger_price"))>=0:price.compareTo(amount(r,"trigger_price"))<=0;if(!hit){ioc(u,r);return;}db.update("UPDATE paper_tickets SET triggered=TRUE,status='TRIGGERED' WHERE id=?",r.get("id"));}
        if(kind.contains("LIMIT")&&(buy?price.compareTo(amount(r,"limit_price"))>0:price.compareTo(amount(r,"limit_price"))<0)){ioc(u,r);return;}
        int quantity=((Number)r.get("quantity")).intValue();BigDecimal value=price.multiply(BigDecimal.valueOf(quantity)),fee=policy.fee(value);String id=(String)r.get("id"),position;
        if(entry){
            if(!india().toLocalTime().isBefore(CUTOFF)||db.queryForObject("SELECT COUNT(*) FROM paper_intraday_positions WHERE user_id=? AND quantity>0 AND close_requested=TRUE",Integer.class,u.getId())>0){release(u,r,"CANCELLED");return;}
            BigDecimal collateral=margin(value);if(collateral.add(fee).compareTo(amount(r,"reserved_cash"))>0){ioc(u,r);return;}
            accounting.release(u,"PAPER_TICKET",id,amount(r,"reserved_cash"),"FILL");accounting.pay(u,"MIS_OPEN:"+id,collateral.add(fee),true);
            position=UUID.randomUUID().toString();db.update("INSERT INTO paper_intraday_positions(id,user_id,stock_id,direction,quantity,entry_price,collateral,last_price,mark_at,trade_date,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",position,u.getId(),stock.getId(),buy?"LONG":"SHORT",quantity,price,collateral,price,stamp(),india().toLocalDate(),stamp());
            event(id,u.getId(),position,"OPEN",quantity,collateral,BigDecimal.ZERO,fee);
        }else{
            position=(String)r.get("position_id");var p=position(position,u.getId());String exit="LONG".equals(p.get("direction"))?"SELL":"BUY";
            if(!exit.equals(r.get("side"))||quantity>units(p)||!stock.getId().equals(((Number)p.get("stock_id")).longValue()))throw bad("Intraday exit no longer matches its position.");
            close(u,p,quantity,price,fee,id,stock.getExchange());
        }
        db.update("UPDATE paper_tickets SET status='FILLED',filled_units=quantity,filled_legs=1,fill_value=?,fees=?,reserved_cash=0,reserved_units=0,position_id=?,last_error=NULL WHERE id=?",value,fee,position,id);
        db.update("INSERT INTO paper_fills(id,ticket_id,user_id,leg_number,quantity,price,fee,created_at) VALUES(?,?,?,0,?,?,?,?)",UUID.randomUUID().toString(),id,u.getId(),quantity,price,fee,stamp());
    }
    private void close(User u,Map<String,Object> p,int quantity,BigDecimal price,BigDecimal fee,String event,String exchange){
        int held=units(p);BigDecimal collateral=quantity==held?amount(p,"collateral"):amount(p,"collateral").multiply(BigDecimal.valueOf(quantity)).divide(BigDecimal.valueOf(held),2,RoundingMode.DOWN);
        BigDecimal pnl=price.subtract(amount(p,"entry_price")).multiply(BigDecimal.valueOf(quantity));if("SHORT".equals(p.get("direction")))pnl=pnl.negate();
        BigDecimal returned=pnl.signum()>=0?collateral:collateral.add(pnl).max(BigDecimal.ZERO);
        accounting.credit(u,"MIS_RETURN:"+event,returned);
        if(pnl.signum()>0)accounting.pending(u,"MIS_PROFIT:"+event,exchange,"EQUITY",accounting.tradeDate(),pnl,true);
        else if(collateral.add(pnl).signum()<0)accounting.fee(u,"MIS_LOSS:"+event,collateral.add(pnl).negate());
        accounting.fee(u,"MIS_FEE:"+event,fee);
        db.update("UPDATE paper_intraday_positions SET quantity=quantity-?,collateral=collateral-?,realized_pnl=realized_pnl+?,last_price=?,mark_at=?,last_error=NULL WHERE id=? AND user_id=?",quantity,collateral,pnl,price,stamp(),p.get("id"),u.getId());
        event(event,u.getId(),(String)p.get("id"),"CLOSE",quantity,collateral.negate(),pnl,fee);
    }
    private void event(String key,long user,String position,String kind,int units,BigDecimal collateral,BigDecimal pnl,BigDecimal fee){db.update("INSERT INTO paper_intraday_events(event_key,user_id,position_id,event_type,units,collateral_delta,realized_pnl,fee,occurred_at) VALUES(?,?,?,?,?,?,?,?,?)",key,user,position,kind,units,collateral,pnl,fee,stamp());}
    private void ioc(User u,Map<String,Object> r){if("IOC".equals(r.get("validity")))release(u,r,"CANCELLED");}
    private void release(User u,Map<String,Object> r,String status){if(!active(r))return;accounting.prepare(u);accounting.release(u,"PAPER_TICKET",(String)r.get("id"),amount(r,"reserved_cash"),status);db.update("UPDATE paper_tickets SET status=?,reserved_cash=0,reserved_units=0 WHERE id=?",status,r.get("id"));}
    @Transactional public void cancel(String email,String id){var u=users.findByEmailForUpdate(email).orElseThrow();u.requireActive();var rows=db.queryForList("SELECT * FROM paper_tickets WHERE id=? AND user_id=? AND product='MIS'",id,u.getId());if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found.");release(u,rows.getFirst(),"CANCELLED");}
    @Transactional public Map<String,Object> edit(String email,String id,PaperTrading.Ticket t){var u=users.findByEmailForUpdate(email).orElseThrow();u.requireActive();var old=row(id);if(owner(old)!=u.getId()||!active(old)||t==null||!"MIS".equals(t.product())||Objects.equals(t.requestId(),old.get("request_id")))throw bad("Replace an owned pending MIS order using a new request ID.");if(db.queryForObject("SELECT COUNT(*) FROM paper_tickets WHERE user_id=? AND request_id=?",Integer.class,u.getId(),t.requestId())>0)throw bad("Replacement request already exists.");release(u,old,"REPLACED");return submit(email,t);}
    @Transactional public void requestClose(String email,String id){var u=users.findByEmailForUpdate(email).orElseThrow();u.requireActive();position(id,u.getId());db.update("UPDATE paper_intraday_positions SET close_requested=TRUE WHERE id=?",id);}
    public List<String> positionsToManage(){return db.queryForList("SELECT id FROM paper_intraday_positions WHERE quantity>0 ORDER BY COALESCE(last_checked_at,created_at),id LIMIT 50",String.class);}
    @Transactional(propagation=Propagation.REQUIRES_NEW) public void observe(String id){
        var found=db.queryForList("SELECT user_id FROM paper_intraday_positions WHERE id=? AND quantity>0",Long.class,id);if(found.isEmpty())return;users.findByIdForUpdate(found.getFirst()).orElseThrow();
        db.update("UPDATE paper_intraday_positions SET last_checked_at=?,close_requested=CASE WHEN trade_date<? OR (trade_date=? AND ?=TRUE) THEN TRUE ELSE close_requested END WHERE id=?",stamp(),india().toLocalDate(),india().toLocalDate(),!india().toLocalTime().isBefore(CUTOFF),id);
    }
    @Transactional public void manage(String id){
        var found=db.queryForMap("SELECT user_id FROM paper_intraday_positions WHERE id=?",id);var u=users.findByIdForUpdate(owner(found)).orElseThrow();var rows=db.queryForList("SELECT * FROM paper_intraday_positions WHERE id=? AND quantity>0",id);if(rows.isEmpty())return;var p=rows.getFirst();
        var stock=stocks.findById(((Number)p.get("stock_id")).longValue()).orElseThrow();BigDecimal price=price(stock);db.update("UPDATE paper_intraday_positions SET last_price=?,mark_at=?,last_error=NULL WHERE id=?",price,stamp(),id);
        BigDecimal pnl=price.subtract(amount(p,"entry_price")).multiply(BigDecimal.valueOf(units(p)));if("SHORT".equals(p.get("direction")))pnl=pnl.negate();
        if(!Boolean.TRUE.equals(p.get("close_requested"))&&amount(p,"collateral").add(pnl).signum()>0)return;
        sessions.requireOpenSession(stock.getExchange());
        for(var pending:db.queryForList("SELECT * FROM paper_tickets WHERE user_id=? AND product='MIS' AND position_id=? AND status IN ('PENDING','TRIGGERED')",u.getId(),id))release(u,pending,"CANCELLED");
        accounting.prepare(u);String ticket=UUID.randomUUID().toString();BigDecimal value=price.multiply(BigDecimal.valueOf(units(p))),fee=policy.fee(value);close(u,p,units(p),price,fee,ticket,stock.getExchange());
        db.update("INSERT INTO paper_tickets(id,user_id,request_id,stock_id,instrument_key,position_id,side,product,variety,order_kind,validity,quantity,filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fill_value,fees,policy_version,expires_at,created_at) VALUES(?,?,?,?,?,?,?,'MIS','REGULAR','MARKET','DAY',?,?,1,1,1,?,0,'FILLED',?,?,?,?,?)",ticket,u.getId(),ticket,stock.getId(),stock.getInstrumentKey(),id,"LONG".equals(p.get("direction"))?"SELL":"BUY",units(p),units(p),price,value,fee,version(),stamp(),stamp());
        db.update("INSERT INTO paper_fills(id,ticket_id,user_id,leg_number,quantity,price,fee,created_at) VALUES(?,?,?,0,?,?,?,?)",UUID.randomUUID().toString(),ticket,u.getId(),units(p),price,fee,stamp());
    }
    public void failure(String id){db.update("UPDATE paper_intraday_positions SET last_error=? WHERE id=? AND quantity>0","Fresh price/session unavailable; intraday closure remains retryable.",id);}
    public List<Map<String,Object>> positions(long user){return db.queryForList("SELECT p.*,s.symbol,s.instrument_key,'MIS' AS product FROM paper_intraday_positions p LEFT JOIN stocks s ON s.id=p.stock_id WHERE p.user_id=? ORDER BY p.created_at DESC",user).stream().map(source->{var p=view(source);BigDecimal unrealized=null;if(units(p)==0)unrealized=BigDecimal.ZERO;else if(p.get("mark_at")!=null&&p.get("last_price")!=null&&!((Timestamp)p.get("mark_at")).toInstant().isBefore(now().minusSeconds(60))&&!((Timestamp)p.get("mark_at")).toInstant().isAfter(now().plusSeconds(5))){unrealized=amount(p,"last_price").subtract(amount(p,"entry_price")).multiply(BigDecimal.valueOf(units(p)));if("SHORT".equals(p.get("direction")))unrealized=unrealized.negate();}p.put("unrealized_pnl",unrealized);return p;}).toList();}
    public void enrich(com.example.investsmart.dto.PortfolioResponse r,long user){
        BigDecimal collateral=BigDecimal.ZERO,pnl=BigDecimal.ZERO;boolean available=true;
        for(var p:positions(user)){if(units(p)==0)continue;collateral=collateral.add(amount(p,"collateral"));if(p.get("mark_at")==null||p.get("last_price")==null||((Timestamp)p.get("mark_at")).toInstant().isBefore(now().minusSeconds(60)))available=false;else{BigDecimal change=amount(p,"last_price").subtract(amount(p,"entry_price")).multiply(BigDecimal.valueOf(units(p)));pnl=pnl.add("SHORT".equals(p.get("direction"))?change.negate():change);}}
        if(r.getRealizedPnl()!=null)r.setRealizedPnl(r.getRealizedPnl().add(db.queryForObject("SELECT COALESCE(SUM(realized_pnl),0) FROM paper_intraday_positions WHERE user_id=?",BigDecimal.class,user)));
        if(r.getInvestedAmount()!=null)r.setInvestedAmount(r.getInvestedAmount().add(collateral));
        if(!available){r.setTotalValue(null);r.setHoldingsValue(null);r.setUnrealizedPnl(null);}else if(r.getTotalValue()!=null){r.setTotalValue(r.getTotalValue().add(collateral).add(pnl));r.setHoldingsValue(r.getHoldingsValue().add(collateral).add(pnl));if(r.getUnrealizedPnl()!=null)r.setUnrealizedPnl(r.getUnrealizedPnl().add(pnl));}
        if(collateral.signum()>0||r.getTotalValue()==null){r.setCashAllocationPercent(null);r.setInvestedAllocationPercent(null);}
    }
}
