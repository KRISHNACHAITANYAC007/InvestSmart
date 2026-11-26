package com.example.investsmart.trading;

import com.example.investsmart.entity.*;
import com.example.investsmart.repository.*;
import com.example.investsmart.service.*;
import com.example.investsmart.market.*;
import com.example.investsmart.options.*;
import java.math.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class PaperTrading {
    public record Ticket(Long stockId,String instrumentKey,LocalDate expiry,Integer quantity,String side,String product,String variety,
            String orderType,String validity,BigDecimal limitPrice,BigDecimal triggerPrice,Integer legs,
            BigDecimal stopLossPercent,BigDecimal targetPercent,String positionId,String requestId,BigDecimal stopLossPrice,BigDecimal targetPrice) {
        public Ticket(Long stockId,String instrumentKey,LocalDate expiry,Integer quantity,String side,String product,String variety,String orderType,String validity,BigDecimal limitPrice,BigDecimal triggerPrice,Integer legs,BigDecimal stopLossPercent,BigDecimal targetPercent,String positionId,String requestId){this(stockId,instrumentKey,expiry,quantity,side,product,variety,orderType,validity,limitPrice,triggerPrice,legs,stopLossPercent,targetPercent,positionId,requestId,null,null);}
    }
    record Quote(String key,String symbol,BigDecimal price,int lot,int freeze,Instant at) {}
    record Prepared(Ticket ticket,Quote quote,int legs,BigDecimal ceiling,BigDecimal cash,BigDecimal fees,Instant expiry) {}
    private final JdbcTemplate db;private final UserRepository users;private final StockRepository stocks;private final HoldingRepository holdings;
    private final OrderService equities;private final OptionTrading options;private final OptionMarket optionMarket;private final MarketDataService market;
    private final NseMarketSessionService sessions;private final Clock clock;private final PaperPolicy policy;private final PaperTriggerTransaction triggers;private final IntradayTrading intraday;private final com.example.investsmart.settlement.SettlementAccounting accounting;
    public PaperTrading(JdbcTemplate db,UserRepository users,StockRepository stocks,HoldingRepository holdings,OrderService equities,
            OptionTrading options,OptionMarket optionMarket,MarketDataService market,NseMarketSessionService sessions,Clock clock,PaperPolicy policy,PaperTriggerTransaction triggers,com.example.investsmart.settlement.SettlementAccounting accounting,IntradayTrading intraday){this.intraday=intraday;this.accounting=accounting;
        this.db=db;this.users=users;this.stocks=stocks;this.holdings=holdings;this.equities=equities;this.options=options;this.optionMarket=optionMarket;this.market=market;this.sessions=sessions;this.clock=clock;this.policy=policy;this.triggers=triggers;
    }
    private ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.BAD_REQUEST,text);}
    private Instant now(){return clock.instant();}private ZonedDateTime india(){return now().atZone(OptionMarket.INDIA);}
    private BigDecimal money(BigDecimal n){return PaperPolicy.money(n);}
    private BigDecimal contribution(BigDecimal value,String product){return product.equals("CASH")?money(value):policy.contribution(value);}
    private int number(Map<String,Object> row,String key){return ((Number)row.get(key)).intValue();}
    private long userId(Map<String,Object> row){return ((Number)row.get("user_id")).longValue();}
    private BigDecimal amount(Map<String,Object> row,String key){return (BigDecimal)row.get(key);}
    private LocalDate date(Object value){return value==null?null:((java.sql.Date)value).toLocalDate();}
    private Map<String,Object> view(Map<String,Object> row){var out=new LinkedHashMap<String,Object>();row.forEach((k,v)->out.put(k.toLowerCase(Locale.ROOT),v));return out;}
    private Map<String,Object> row(String id){return view(db.queryForMap("SELECT * FROM paper_tickets WHERE id=?",id));}
    private Map<String,Object> ownedRow(String id,long user){var rows=db.queryForList("SELECT * FROM paper_tickets WHERE id=? AND user_id=?",id,user);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found.");return view(rows.getFirst());}
    private Map<String,Object> position(String id,long user){var rows=db.queryForList("SELECT * FROM paper_financing WHERE id=? AND user_id=? AND quantity>0",id,user);if(rows.isEmpty())throw bad("Financed position unavailable.");return view(rows.getFirst());}
    private boolean active(Map<String,Object> row){return List.of("PENDING","TRIGGERED","PART_FILLED").contains(row.get("status"));}
    private String value(String v,String fallback){return v==null?fallback:v.toUpperCase(Locale.ROOT);}
    private boolean positive(BigDecimal value){return value!=null&&value.signum()>0&&value.scale()<=2&&value.compareTo(new BigDecimal("10000000"))<=0;}
    private Ticket normalize(Ticket t){if(t==null)throw bad("Order details are required.");return new Ticket(t.stockId(),t.instrumentKey(),t.expiry(),t.quantity(),value(t.side(),"BUY"),value(t.product(),"CASH"),value(t.variety(),"REGULAR"),value(t.orderType(),"MARKET"),value(t.validity(),"DAY"),t.limitPrice(),t.triggerPrice(),t.legs()==null?1:t.legs(),t.stopLossPercent(),t.targetPercent(),t.positionId(),t.requestId(),t.stopLossPrice(),t.targetPrice());}
    private <T> T executableData(java.util.function.Supplier<T> fetch){
        try{return fetch.get();}catch(com.example.investsmart.exception.MarketStatusUnavailableException unavailable){throw unavailable;}catch(RuntimeException unavailable){
            org.slf4j.LoggerFactory.getLogger(PaperTrading.class).warn("Executable paper market data unavailable ({})",unavailable.getClass().getSimpleName());
            throw new com.example.investsmart.exception.MarketStatusUnavailableException("Fresh executable market data is unavailable. No order was filled; retry when the provider is available.",unavailable);
        }
    }
    private Quote quote(Ticket t){
        if(t.stockId()!=null){var s=stocks.findById(t.stockId()).orElseThrow(()->bad("Listing unavailable."));if(s.getInstrumentKey()==null||!s.getInstrumentKey().matches("(NSE|BSE)_EQ\\|.+"))throw bad("Choose an equity listing.");
            var q=executableData(()->market.getQuote(new MarketInstrument(s.getInstrumentKey(),s.getSymbol(),s.getCompanyName(),s.getExchange(),s.getIsin())));
            if(q==null||!s.getInstrumentKey().equals(q.getInstrumentKey())||!positive(q.getCurrentPrice())||q.getLastUpdated()==null)throw new com.example.investsmart.exception.MarketStatusUnavailableException("A fresh price for this listing is unavailable. Refresh and retry.",null);
            Instant fetched=q.getLastUpdated().atZone(OptionMarket.INDIA).toInstant();if(fetched.isBefore(now().minusSeconds(60))||fetched.isAfter(now().plusSeconds(5)))throw new com.example.investsmart.exception.MarketStatusUnavailableException("A fresh price for this listing is unavailable. Refresh and retry.",null);
            return new Quote(s.getInstrumentKey(),s.getSymbol(),q.getCurrentPrice(),1,1000000,now());
        }
        if(t.expiry()==null||t.instrumentKey()==null)throw bad("Choose a listing or option contract.");
        var q=executableData(()->optionMarket.chain(t.expiry())).stream().filter(x->x.contract().instrumentKey().equals(t.instrumentKey())).findFirst().orElseThrow(()->bad("Contract unavailable."));
        if(q.fetchedAt()==null||q.fetchedAt().isBefore(now().minusSeconds(60))||q.fetchedAt().isAfter(now().plusSeconds(5)))throw new com.example.investsmart.exception.MarketStatusUnavailableException("A fresh option quote is unavailable. Refresh and retry.",null);
        var price="BUY".equals(t.side())?q.ask():q.bid();if(!positive(price))throw bad("Executable bid/ask unavailable.");
        return new Quote(q.contract().instrumentKey(),q.contract().symbol(),price,q.contract().lotSize(),q.contract().freezeQuantity(),q.fetchedAt());
    }
    private Prepared prepare(Ticket raw){
        Ticket t=normalize(raw);
        if(t.quantity()==null||t.quantity()<1||t.quantity()>1000000||!List.of("BUY","SELL").contains(t.side())||!List.of("CASH","MTF","COVER").contains(t.product())||!List.of("REGULAR","QUICK","ICEBERG").contains(t.variety())||!List.of("MARKET","LIMIT","STOP_LIMIT","STOP_MARKET").contains(t.orderType())||!List.of("DAY","IOC","GTT").contains(t.validity()))throw bad("Invalid product, order type, validity or quantity.");
        if(t.stockId()!=null&&t.instrumentKey()!=null)throw bad("Choose one instrument identifier.");
        if(t.stockId()==null&&!t.product().equals("CASH"))throw bad("MTF and Cover apply to equity long positions only.");
        if(!t.product().equals("CASH")&&(t.variety().equals("ICEBERG")||t.validity().equals("GTT")||!List.of("MARKET","LIMIT").contains(t.orderType())))throw bad("MTF/Cover entries support Regular Market/Limit DAY or IOC orders.");
        if(t.side().equals("SELL")&&!t.product().equals("CASH")&&t.positionId()==null)throw bad("Choose the financed position to close.");
        if(t.positionId()!=null&&(!t.side().equals("SELL")||t.stockId()==null))throw bad("Financed positions can only be closed by equity sells.");
        if(t.orderType().contains("LIMIT")&&!positive(t.limitPrice()))throw bad("Enter a positive limit price with two decimals.");
        if(t.orderType().startsWith("STOP")&&!positive(t.triggerPrice()))throw bad("Enter a positive stop trigger with two decimals.");
        if(t.orderType().equals("STOP_LIMIT")&&(t.side().equals("BUY")?t.limitPrice().compareTo(t.triggerPrice())<0:t.limitPrice().compareTo(t.triggerPrice())>0))throw bad("SL buy limit must be at or above trigger; SL sell limit at or below trigger.");
        for(BigDecimal percent:Arrays.asList(t.stopLossPercent(),t.targetPercent()))if(percent!=null&&(percent.signum()<=0||percent.compareTo(new BigDecimal("99"))>0||percent.scale()>2))throw bad("Exit percentages must be above 0 and at most 99, with two decimals.");
        if(t.side().equals("SELL")&&(t.stopLossPercent()!=null||t.targetPercent()!=null||t.stopLossPrice()!=null||t.targetPrice()!=null))throw bad("Attach exit percentages to a buy entry.");
        if(t.product().equals("COVER")&&((t.stopLossPercent()==null&&t.stopLossPrice()==null)||t.targetPercent()!=null||t.targetPrice()!=null||!t.validity().equals("DAY")))throw bad("Cover requires a stop-loss percentage, DAY validity and no GTT target.");
        if(t.validity().equals("GTT")&&t.variety().equals("ICEBERG"))throw bad("Use DAY or IOC for iceberg entries.");
        if(t.variety().equals("ICEBERG")&&(!List.of("MARKET","LIMIT").contains(t.orderType())||t.legs()<2||t.legs()>10))throw bad("Iceberg supports Market/Limit and 2–10 legs.");
        if(!t.variety().equals("ICEBERG")&&t.legs()!=1)throw bad("Regular orders have one leg.");
        Quote q=quote(t);if((t.stopLossPrice()!=null&&(!positive(t.stopLossPrice())||t.stopLossPrice().compareTo(q.price())>=0||t.stopLossPercent()!=null))||(t.targetPrice()!=null&&(!positive(t.targetPrice())||t.targetPrice().compareTo(q.price())<=0||t.targetPercent()!=null)))throw bad("Absolute stop must be below the current buy reference and target above it; choose either price or percentage.");if(t.quantity()%q.lot()!=0||t.quantity()/q.lot()<t.legs())throw bad("Use whole lots and at least one lot per leg.");
        int maxLeg=((t.quantity()/q.lot()+t.legs()-1)/t.legs())*q.lot();if(maxLeg>q.freeze())throw bad("An iceberg leg exceeds the provider quantity limit; increase legs or reduce quantity.");
        var local=india();if(t.product().equals("COVER")&&t.side().equals("BUY")&&!local.toLocalTime().isBefore(LocalTime.of(15,20)))throw bad("Cover entries close at 15:20 IST; no new entry now.");if(t.expiry()!=null&&(t.expiry().isBefore(local.toLocalDate())||t.expiry().equals(local.toLocalDate())&&!local.toLocalTime().isBefore(LocalTime.of(15,30))))throw bad("Contract has expired.");
        if(!t.validity().equals("GTT")&&!local.toLocalTime().isBefore(LocalTime.of(15,30)))throw bad("DAY/IOC entries must be placed before 15:30 IST.");
        BigDecimal ceiling=t.orderType().contains("LIMIT")?t.limitPrice():money(q.price().max(t.triggerPrice()==null?q.price():t.triggerPrice()).multiply(BigDecimal.ONE.add(policy.protection)));
        BigDecimal fee=BigDecimal.ZERO;int lots=t.quantity()/q.lot();for(int i=0;i<t.legs();i++){int leg=(lots/t.legs()+(i<lots%t.legs()?1:0))*q.lot();fee=fee.add(policy.fee(ceiling.multiply(BigDecimal.valueOf(leg))));}
        BigDecimal notional=money(ceiling.multiply(BigDecimal.valueOf(t.quantity())));
        BigDecimal cash=t.side().equals("BUY")?contribution(notional,t.product()).add(fee):BigDecimal.ZERO;
        Instant expires=local.toLocalDate().atTime(15,30).atZone(OptionMarket.INDIA).toInstant();if(t.validity().equals("GTT")){expires=now().plus(Duration.ofDays(365));if(t.expiry()!=null)expires=expires.isBefore(t.expiry().atTime(15,30).atZone(OptionMarket.INDIA).toInstant())?expires:t.expiry().atTime(15,30).atZone(OptionMarket.INDIA).toInstant();}
        return new Prepared(t,q,t.legs(),ceiling,cash,fee,expires);
    }
    @Transactional(readOnly=true) public Map<String,Object> preview(String email,Ticket ticket){if(ticket!=null&&"MIS".equals(ticket.product()))return intraday.preview(email,ticket);var user=users.findByEmailIgnoreCase(email).orElseThrow();user.requireActive();var p=prepare(ticket);Map<String,Object> result=new LinkedHashMap<>();result.put("requiredCash",p.cash());result.put("availableCash",user.getVirtualBalance());result.put("estimatedFees",p.fees());result.put("taxes",BigDecimal.ZERO);result.put("feeRate",policy.feeRate);result.put("feeCap",policy.feeCap);result.put("referencePrice",p.quote().price());result.put("ceilingPrice",p.ceiling());result.put("lotSize",p.quote().lot());result.put("leverage",p.ticket().product().equals("CASH")?BigDecimal.ONE:policy.leverage);result.put("annualInterest",policy.annualInterest);result.put("policyVersion",policy.version());result.put("expiresAt",now().plusSeconds(30));return result;}
    @Transactional public Map<String,Object> submit(String email,Ticket raw){
        if(raw!=null&&"MIS".equals(raw.product()))return intraday.submit(email,raw);
        var user=users.findByEmailForUpdate(email).orElseThrow();user.requireActive();accounting.prepare(user);if(raw==null||raw.requestId()==null||!raw.requestId().matches("[a-zA-Z0-9-]{16,80}"))throw bad("A unique request ID is required.");
        var prior=db.queryForList("SELECT * FROM paper_tickets WHERE user_id=? AND request_id=?",user.getId(),raw.requestId());if(!prior.isEmpty())return view(prior.getFirst());
        var p=prepare(raw);var t=p.ticket();if(t.side().equals("BUY")&&(user.getVirtualBalance()==null||user.getVirtualBalance().signum()<0||user.getVirtualBalance().compareTo(p.cash())<0))throw bad("Insufficient unreserved virtual cash; resolve negative balances before new orders.");
        if(t.side().equals("SELL"))requireOwned(user,t,p.quote());
        String id=UUID.randomUUID().toString();accounting.reserve(user,"PAPER_TICKET",id,p.cash(),t.product().equals("CASH"));
        db.update("INSERT INTO paper_tickets(id,user_id,request_id,stock_id,instrument_key,expiry,position_id,side,product,variety,order_kind,validity,quantity,legs,lot_size,limit_price,trigger_price,ceiling_price,stop_percent,target_percent,stop_price,target_price,reserved_cash,reserved_units,status,policy_version,expires_at,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",id,user.getId(),t.requestId(),t.stockId(),p.quote().key(),t.expiry(),t.positionId(),t.side(),t.product(),t.variety(),t.orderType(),t.validity(),t.quantity(),p.legs(),p.quote().lot(),t.limitPrice(),t.triggerPrice(),p.ceiling(),t.stopLossPercent(),t.targetPercent(),t.stopLossPrice(),t.targetPrice(),p.cash(),t.side().equals("SELL")?t.quantity():0,"PENDING",policy.version(),Timestamp.from(p.expiry()),Timestamp.from(now()));
        if(t.orderType().equals("MARKET")||t.validity().equals("IOC"))processLocked(user,row(id));return row(id);
    }
    private Ticket ticket(Map<String,Object> r){return new Ticket(r.get("stock_id")==null?null:((Number)r.get("stock_id")).longValue(),r.get("stock_id")==null?(String)r.get("instrument_key"):null,date(r.get("expiry")),number(r,"quantity"),(String)r.get("side"),(String)r.get("product"),(String)r.get("variety"),(String)r.get("order_kind"),(String)r.get("validity"),amount(r,"limit_price"),amount(r,"trigger_price"),number(r,"legs"),amount(r,"stop_percent"),amount(r,"target_percent"),(String)r.get("position_id"),(String)r.get("request_id"),amount(r,"stop_price"),amount(r,"target_price"));}
    private void requireOwned(User user,Ticket t,Quote q){
        int held;if(t.positionId()!=null){var position=position(t.positionId(),user.getId());if(((Number)position.get("stock_id")).longValue()!=t.stockId()||!position.get("product").equals(t.product()))throw bad("Position does not match this listing/product.");held=number(position,"quantity");}
        else if(t.stockId()!=null)held=holdings.findByUserIdAndStockId(user.getId(),t.stockId()).map(Holding::getQuantity).orElse(0);
        else held=db.queryForObject("SELECT COALESCE(SUM(units),0) FROM option_positions WHERE user_id=? AND instrument_key=? AND expiry=?",Integer.class,user.getId(),q.key(),t.expiry());
        int reserved=db.queryForObject("SELECT COALESCE(SUM(reserved_units),0) FROM paper_tickets WHERE user_id=? AND instrument_key=? AND status IN ('PENDING','TRIGGERED','PART_FILLED') AND (position_id=? OR (position_id IS NULL AND ? IS NULL))",Integer.class,user.getId(),q.key(),t.positionId(),t.positionId());
        if(t.positionId()==null)reserved+=t.stockId()!=null?db.queryForObject("SELECT COALESCE(SUM(units),0) FROM equity_pending_orders WHERE user_id=? AND stock_id=? AND side='SELL' AND status='PENDING'",Integer.class,user.getId(),t.stockId()):db.queryForObject("SELECT COALESCE(SUM(units),0) FROM option_orders WHERE user_id=? AND instrument_key=? AND expiry=? AND side='SELL' AND status='PENDING'",Integer.class,user.getId(),q.key(),t.expiry());
        if(held-reserved<t.quantity())throw bad("Insufficient unreserved units on the selected position/listing.");
    }
    public List<String> pending(){return db.queryForList("SELECT id FROM paper_tickets WHERE status IN ('PENDING','TRIGGERED','PART_FILLED') ORDER BY COALESCE(last_checked_at,created_at) LIMIT 20",String.class);}
    @Transactional public void process(String id){if(intraday.ticket(id)){intraday.process(id);return;}triggers.commitObservation(()->observeLocked(id));var candidate=row(id);var user=users.findByIdForUpdate(userId(candidate)).orElseThrow();db.update("UPDATE paper_tickets SET last_checked_at=? WHERE id=?",Timestamp.from(now()),id);processLocked(user,row(id));}
    private void observeLocked(String id){
        var candidate=row(id);if(!active(candidate)||Boolean.TRUE.equals(candidate.get("triggered")))return;
        if(!((String)candidate.get("order_kind")).startsWith("STOP")&&!"GTT".equals(candidate.get("validity")))return;
        var user=users.findByIdForUpdate(userId(candidate)).orElseThrow();var r=row(id);if(!active(r)||Boolean.TRUE.equals(r.get("triggered"))||!"ACTIVE".equals(user.getAccountStatus())||!((Timestamp)r.get("expires_at")).toInstant().isAfter(now())||!policy.version().equals(r.get("policy_version")))return;
        var t=ticket(r);var q=quote(t);sessions.requireOpenSession(q.key().startsWith("BSE")?"BSE":"NSE");
        boolean buy=t.side().equals("BUY"),stop=t.orderType().startsWith("STOP");BigDecimal threshold=stop?t.triggerPrice():t.limitPrice();if(threshold==null)return;
        boolean hit=stop?(buy?q.price().compareTo(threshold)>=0:q.price().compareTo(threshold)<=0):(buy?q.price().compareTo(threshold)<=0:q.price().compareTo(threshold)>=0);
        if(!hit)return;db.update("UPDATE paper_tickets SET triggered=TRUE,status='TRIGGERED' WHERE id=?",id);
        if("GTT".equals(r.get("validity")))db.update("UPDATE paper_tickets SET validity='DAY',expires_at=? WHERE id=?",Timestamp.from(india().toLocalDate().atTime(15,30).atZone(OptionMarket.INDIA).toInstant()),id);
        claimGroup(user,r);
    }
    public void failure(String id){db.update("UPDATE paper_tickets SET last_error=?,last_checked_at=? WHERE id=? AND status IN ('PENDING','TRIGGERED','PART_FILLED')","Current executable market data unavailable; order remains reserved and retryable.",Timestamp.from(now()),id);}
    private void processLocked(User user,Map<String,Object> r){
        if(!active(r))return;accounting.prepare(user);String id=(String)r.get("id");if(!policy.version().equals(r.get("policy_version"))){release(user,r,"POLICY_CHANGED");return;}if(!((Timestamp)r.get("expires_at")).toInstant().isAfter(now())){release(user,r,"EXPIRED");return;}if(!"ACTIVE".equals(user.getAccountStatus())){release(user,r,"CANCELLED");return;}
        if("COVER".equals(r.get("product"))&&"BUY".equals(r.get("side"))&&!india().toLocalTime().isBefore(LocalTime.of(15,20))){release(user,r,"EXPIRED");return;}
        var t=ticket(r);Quote q=quote(t);if(q.lot()!=number(r,"lot_size")||!q.key().equals(r.get("instrument_key")))throw bad("Contract sizing changed; cancel and replace.");sessions.requireOpenSession(q.key().startsWith("BSE")?"BSE":"NSE");
        boolean buy=t.side().equals("BUY"),stop=t.orderType().startsWith("STOP"),triggered=Boolean.TRUE.equals(r.get("triggered"));
        if(stop&&!triggered){boolean hit=buy?q.price().compareTo(t.triggerPrice())>=0:q.price().compareTo(t.triggerPrice())<=0;if(!hit){ioc(user,r);return;}triggered=true;db.update("UPDATE paper_tickets SET triggered=TRUE,status='TRIGGERED' WHERE id=?",id);if("GTT".equals(r.get("validity")))db.update("UPDATE paper_tickets SET validity='DAY',expires_at=? WHERE id=?",Timestamp.from(india().toLocalDate().atTime(15,30).atZone(OptionMarket.INDIA).toInstant()),id);claimGroup(user,r);}
        boolean fillable=t.orderType().contains("LIMIT")?(buy?q.price().compareTo(t.limitPrice())<=0:q.price().compareTo(t.limitPrice())>=0):!buy||q.price().compareTo(amount(r,"ceiling_price"))<=0;
        if(!fillable){ioc(user,r);return;}if(!stop)claimGroup(user,r);
        int filled=number(r,"filled_units"),legIndex=number(r,"filled_legs"),lots=t.quantity()/q.lot(),legs=number(r,"legs");int quantity=(lots/legs+(legIndex<lots%legs?1:0))*q.lot();
        if(quantity<=0||quantity>q.freeze())throw bad("Contract sizing changed; cancel and replace this order.");
        BigDecimal value=money(q.price().multiply(BigDecimal.valueOf(quantity))),fee=policy.fee(value);
        BigDecimal reserved=amount(r,"reserved_cash");BigDecimal legReserve=buy?contribution(amount(r,"ceiling_price").multiply(BigDecimal.valueOf(quantity)),t.product()).add(policy.fee(amount(r,"ceiling_price").multiply(BigDecimal.valueOf(quantity)))):BigDecimal.ZERO;
        if(filled+quantity==t.quantity())legReserve=reserved;
        db.update("UPDATE paper_tickets SET status='EXECUTING',reserved_cash=reserved_cash-?,reserved_units=0 WHERE id=?",legReserve,id);
        accounting.release(user,"PAPER_TICKET",id,legReserve,"LEG_"+legIndex);
        String legacy=null;
        if(t.product().equals("CASH")){
            if(t.stockId()!=null){var request=new com.example.investsmart.dto.OrderRequest();request.setStockId(t.stockId());request.setQuantity(quantity);var fill=buy?equities.buyStock(user.getEmail(),request,amount(r,"ceiling_price")):equities.sellStock(user.getEmail(),request,t.orderType().contains("LIMIT")?t.limitPrice():null);value=fill.getTotalAmount();fee=policy.fee(value);legacy=fill.getId().toString();}
            else {var fill=options.submit(user.getEmail(),new OptionTrading.Ticket(t.instrumentKey(),t.expiry(),t.side(),"MARKET",quantity/q.lot(),null,id+"-"+legIndex));if(!"FILLED".equals(fill.get("status"))||((Number)fill.get("units")).intValue()!=quantity||((Number)fill.get("lot_size")).intValue()!=q.lot())throw bad("Option fill metadata changed; no fill made.");value=money(((BigDecimal)fill.get("fill_price")).multiply(BigDecimal.valueOf(quantity)));fee=policy.fee(value);legacy=(String)fill.get("id");if(buy&&value.compareTo(amount(r,"ceiling_price").multiply(BigDecimal.valueOf(quantity)))>0)throw bad("Option price moved above the protected price.");if(t.orderType().contains("LIMIT")&&(buy?value.compareTo(t.limitPrice().multiply(BigDecimal.valueOf(quantity)))>0:value.compareTo(t.limitPrice().multiply(BigDecimal.valueOf(quantity)))<0))throw bad("Option price moved beyond the limit.");}
            if(buy&&user.getVirtualBalance().compareTo(fee)<0)throw bad("Insufficient cash for simulated fees.");accounting.fee(user,"PAPER_FEE:"+id+":"+legIndex,fee);
        }else if(buy){String financed=openFinance(user,t,q,quantity,value,fee);db.update("UPDATE paper_tickets SET position_id=? WHERE id=?",financed,id);}else closeFinance(user,position(t.positionId(),user.getId()),quantity,q.price(),fee);
        BigDecimal actual=money(value.divide(BigDecimal.valueOf(quantity),8,RoundingMode.HALF_UP));
        db.update("INSERT INTO paper_fills(id,ticket_id,user_id,leg_number,quantity,price,fee,legacy_order_id,created_at) VALUES(?,?,?,?,?,?,?,?,?)",UUID.randomUUID().toString(),id,user.getId(),legIndex,quantity,actual,fee,legacy,Timestamp.from(now()));
        boolean done=filled+quantity==t.quantity();db.update("UPDATE paper_tickets SET status=?,filled_units=filled_units+?,filled_legs=filled_legs+1,fill_value=fill_value+?,fees=fees+?,reserved_units=?,last_error=NULL WHERE id=?",done?"FILLED":"PART_FILLED",quantity,value,fee,buy?0:t.quantity()-filled-quantity,id);
        if(done&&buy&&!t.product().equals("COVER"))attachExits(user,row(id),actual);
        if(t.validity().equals("IOC")&&!done)release(user,row(id),"CANCELLED");
    }
    private void ioc(User u,Map<String,Object> r){if("IOC".equals(r.get("validity")))release(u,row((String)r.get("id")),"CANCELLED");}
    private void claimGroup(User user,Map<String,Object> r){
        if(r.get("group_id")==null)return;
        for(var sibling:db.queryForList("SELECT * FROM paper_tickets WHERE user_id=? AND group_id=? AND id<>? AND status IN ('PENDING','TRIGGERED','PART_FILLED')",user.getId(),r.get("group_id"),r.get("id")))release(user,view(sibling),"OCO_CANCELLED");
        db.update("UPDATE paper_tickets SET reserved_units=? WHERE id=?",number(r,"quantity")-number(r,"filled_units"),r.get("id"));
        if("GTT".equals(r.get("validity")))db.update("UPDATE paper_tickets SET validity='DAY',expires_at=? WHERE id=?",Timestamp.from(india().toLocalDate().atTime(15,30).atZone(OptionMarket.INDIA).toInstant()),r.get("id"));
    }
    private void release(User user,Map<String,Object> r,String status){if(!active(r))return;accounting.prepare(user);accounting.release(user,"PAPER_TICKET",(String)r.get("id"),amount(r,"reserved_cash"),status);db.update("UPDATE paper_tickets SET status=?,reserved_cash=0,reserved_units=0 WHERE id=?",status,r.get("id"));}
    @Transactional public void cancel(String email,String id){if(intraday.ticket(id)){intraday.cancel(email,id);return;}var user=users.findByEmailForUpdate(email).orElseThrow();user.requireActive();var r=ownedRow(id,user.getId());if(userId(r)!=user.getId())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found.");if(r.get("group_id")!=null)for(var sibling:db.queryForList("SELECT * FROM paper_tickets WHERE user_id=? AND group_id=?",user.getId(),r.get("group_id")))release(user,view(sibling),"CANCELLED");else release(user,r,"CANCELLED");}
    @Transactional public Map<String,Object> edit(String email,String id,Ticket replacement){
        if(intraday.ticket(id))return intraday.edit(email,id,replacement);
        var user=users.findByEmailForUpdate(email).orElseThrow();user.requireActive();var original=ownedRow(id,user.getId());
        if(userId(original)!=user.getId())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found.");
        if(!active(original)||original.get("group_id")!=null||number(original,"filled_units")!=0)throw bad("Only unfilled, unlinked pending entries can be replaced; cancel remaining iceberg legs separately.");
        if(replacement==null||replacement.requestId()==null||replacement.requestId().equals(original.get("request_id")))throw bad("Replacement requires a new request ID.");
        if(db.queryForObject("SELECT COUNT(*) FROM paper_tickets WHERE user_id=? AND request_id=?",Integer.class,user.getId(),replacement.requestId())>0)throw bad("Replacement request already exists; refresh Orders.");
        release(user,original,"REPLACED");return submit(email,replacement);
    }
    @Transactional public void accruePosition(String id){db.update("UPDATE paper_financing SET last_checked_at=? WHERE id=?",Timestamp.from(now()),id);var found=db.queryForMap("SELECT * FROM paper_financing WHERE id=?",id);users.findByIdForUpdate(userId(found)).orElseThrow();var rows=db.queryForList("SELECT * FROM paper_financing WHERE id=? AND quantity>0",id);if(!rows.isEmpty()){var p=view(rows.getFirst());accrue(p);if("COVER".equals(p.get("product"))&&(!india().toLocalTime().isBefore(LocalTime.of(15,20))||((Timestamp)p.get("created_at")).toInstant().atZone(OptionMarket.INDIA).toLocalDate().isBefore(india().toLocalDate())))db.update("UPDATE paper_financing SET close_requested=TRUE WHERE id=?",id);}}
    private void attachExits(User user,Map<String,Object> parent,BigDecimal ignored){
        BigDecimal stop=amount(parent,"stop_percent"),target=amount(parent,"target_percent");if(stop==null&&target==null&&parent.get("stop_price")==null&&parent.get("target_price")==null)return;
        BigDecimal basis=amount(parent,"fill_value").divide(BigDecimal.valueOf(number(parent,"quantity")),8,RoundingMode.HALF_UP);String group=UUID.randomUUID().toString();boolean reserved=false;
        for(boolean loss:List.of(true,false)){BigDecimal percent=loss?stop:target,absolute=amount(parent,loss?"stop_price":"target_price");if(percent==null&&absolute==null)continue;BigDecimal price=absolute!=null?absolute:money(basis.multiply(BigDecimal.ONE.add(percent.divide(new BigDecimal("100")).multiply(loss?BigDecimal.ONE.negate():BigDecimal.ONE))));if(price.signum()<=0)continue;
            String id=UUID.randomUUID().toString();Instant expires=now().plus(Duration.ofDays(365));LocalDate expiry=date(parent.get("expiry"));if(expiry!=null&&expiry.atTime(15,30).atZone(OptionMarket.INDIA).toInstant().isBefore(expires))expires=expiry.atTime(15,30).atZone(OptionMarket.INDIA).toInstant();
            db.update("INSERT INTO paper_tickets(id,user_id,request_id,stock_id,instrument_key,expiry,position_id,side,product,variety,order_kind,validity,quantity,legs,lot_size,limit_price,trigger_price,ceiling_price,group_id,parent_id,reserved_cash,reserved_units,status,policy_version,expires_at,created_at) VALUES(?,?,?,?,?,?,?,'SELL',?,'REGULAR',?,'GTT',?,1,?,?,?,?,?,?,0,?,'PENDING',?,?,?)",id,user.getId(),id,parent.get("stock_id"),parent.get("instrument_key"),expiry,parent.get("position_id"),parent.get("product"),loss?"STOP_LIMIT":"LIMIT",parent.get("quantity"),parent.get("lot_size"),price,loss?price:null,price,group,parent.get("id"),reserved?0:parent.get("quantity"),policy.version(),Timestamp.from(expires),Timestamp.from(now()));reserved=true;
        }
    }
    public Map<String,Object> account(String email){var u=users.findByEmailIgnoreCase(email).orElseThrow();return Map.of("intradayPositions",intraday.positions(u.getId()),"orders",db.queryForList("SELECT p.*,s.symbol FROM paper_tickets p LEFT JOIN stocks s ON s.id=p.stock_id WHERE p.user_id=? ORDER BY p.created_at DESC LIMIT 300",u.getId()).stream().map(this::view).toList(),"positions",db.queryForList("SELECT p.*,s.symbol,s.instrument_key FROM paper_financing p LEFT JOIN stocks s ON s.id=p.stock_id WHERE p.user_id=? ORDER BY p.created_at DESC",u.getId()).stream().map(this::view).toList(),"fills",db.queryForList("SELECT * FROM paper_fills WHERE user_id=? ORDER BY created_at DESC LIMIT 300",u.getId()).stream().map(this::view).toList(),"summary",summary(u.getId()));}
    // Financing lifecycle is separate from unleveraged holdings, so delivery sales cannot spend borrowed positions.
    private String openFinance(User u,Ticket t,Quote q,int quantity,BigDecimal value,BigDecimal fee){
        BigDecimal contribution=policy.contribution(value);if(u.getVirtualBalance().compareTo(contribution.add(fee))<0)throw bad("Insufficient financed contribution and fees.");String id=UUID.randomUUID().toString();accounting.pay(u,"FINANCE_OPEN:"+id,contribution.add(fee),true);
        BigDecimal principal=value.subtract(contribution),stop=t.product().equals("COVER")?(t.stopLossPrice()!=null?t.stopLossPrice():money(q.price().multiply(BigDecimal.ONE.subtract(t.stopLossPercent().divide(new BigDecimal("100")))))):null;
        db.update("INSERT INTO paper_financing(id,user_id,stock_id,product,quantity,cost_amount,principal,interest_rate,last_price,mark_at,stop_price,created_at,last_accrued_date) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",id,u.getId(),t.stockId(),t.product(),quantity,value,principal,t.product().equals("MTF")?policy.annualInterest:BigDecimal.ZERO,q.price(),Timestamp.from(now()),stop,Timestamp.from(now()),india().toLocalDate().minusDays(1));
        event(id+":OPEN",id,u.getId(),"BORROW",principal,contribution,now());return id;
    }
    private void event(String key,String position,long user,String kind,BigDecimal principal,BigDecimal value,Instant at){db.update("INSERT INTO paper_finance_events(event_key,position_id,user_id,event_type,principal_delta,amount,occurred_at) VALUES(?,?,?,?,?,?,?)",key,position,user,kind,principal,value,Timestamp.from(at));}
    private void accrue(Map<String,Object> p){
        LocalDate day=date(p.get("last_accrued_date")).plusDays(1),end=india().toLocalDate();BigDecimal added=BigDecimal.ZERO;
        if(amount(p,"interest_rate").signum()==0){db.update("UPDATE paper_financing SET last_accrued_date=? WHERE id=?",end.minusDays(1),p.get("id"));return;}
        // Historical principal comes from dated events; catch-up is bounded and resumes next tick.
        for(int n=0;day.isBefore(end)&&n<31;day=day.plusDays(1),n++){
            Instant boundary=day.plusDays(1).atStartOfDay(OptionMarket.INDIA).toInstant();BigDecimal principal=db.queryForObject("SELECT COALESCE(SUM(principal_delta),0) FROM paper_finance_events WHERE position_id=? AND occurred_at<?",BigDecimal.class,p.get("id"),Timestamp.from(boundary));
            BigDecimal charge=money(principal.max(BigDecimal.ZERO).multiply(amount(p,"interest_rate")).divide(new BigDecimal("365"),8,RoundingMode.HALF_UP));
            event(p.get("id")+":INTEREST:"+day,(String)p.get("id"),userId(p),"INTEREST",BigDecimal.ZERO,charge,boundary);added=added.add(charge);db.update("UPDATE paper_financing SET last_accrued_date=? WHERE id=?",day,p.get("id"));
        }
        if(added.signum()!=0)db.update("UPDATE paper_financing SET interest=interest+? WHERE id=?",added,p.get("id"));
    }
    private void closeFinance(User u,Map<String,Object> p,int quantity,BigDecimal price,BigDecimal fee){
        accrue(p);p=position((String)p.get("id"),u.getId());if(date(p.get("last_accrued_date")).isBefore(india().toLocalDate().minusDays(1)))throw bad("Interest catch-up is pending; retry after the next job batches.");int held=number(p,"quantity");if(quantity>held)throw bad("Position quantity changed.");
        BigDecimal fraction=BigDecimal.valueOf(quantity).divide(BigDecimal.valueOf(held),12,RoundingMode.HALF_UP),principal=quantity==held?amount(p,"principal"):money(amount(p,"principal").multiply(fraction)),interest=quantity==held?amount(p,"interest"):money(amount(p,"interest").multiply(fraction)),cost=quantity==held?amount(p,"cost_amount"):money(amount(p,"cost_amount").multiply(fraction)),value=money(price.multiply(BigDecimal.valueOf(quantity)));
        accounting.prepare(u);String repayment=UUID.randomUUID().toString();BigDecimal net=value.subtract(principal).subtract(interest).subtract(fee);if(net.signum()>0)accounting.pending(u,"FINANCE_CLOSE:"+repayment,((Number)p.get("stock_id")).longValue()>0?stocks.findById(((Number)p.get("stock_id")).longValue()).orElseThrow().getExchange():"NSE","EQUITY",accounting.tradeDate(),net,false);else if(net.signum()<0)accounting.fee(u,"FINANCE_LOSS:"+repayment,net.negate());
        db.update("UPDATE paper_financing SET quantity=quantity-?,cost_amount=cost_amount-?,principal=principal-?,interest=interest-?,realized_pnl=realized_pnl+?,last_price=?,mark_at=? WHERE id=?",quantity,cost,principal,interest,value.subtract(cost).subtract(interest),price,Timestamp.from(now()),p.get("id"));
        event(repayment,(String)p.get("id"),u.getId(),"REPAY",principal.negate(),interest,now());
    }
    public List<String> financed(){return db.queryForList("SELECT id FROM paper_financing WHERE quantity>0 ORDER BY COALESCE(last_checked_at,created_at) LIMIT 50",String.class);}
    @Transactional public void manage(String id){
        var found=db.queryForMap("SELECT * FROM paper_financing WHERE id=?",id);var user=users.findByIdForUpdate(userId(found)).orElseThrow();var p=position(id,user.getId());accrue(p);p=position(id,user.getId());
        var t=new Ticket(((Number)p.get("stock_id")).longValue(),null,null,number(p,"quantity"),"SELL",(String)p.get("product"),"REGULAR","MARKET","DAY",null,null,1,null,null,id,id);
        Quote q=quote(t);db.update("UPDATE paper_financing SET last_price=?,mark_at=?,last_error=NULL WHERE id=?",q.price(),Timestamp.from(now()),id);
        BigDecimal value=q.price().multiply(BigDecimal.valueOf(number(p,"quantity"))),equity=value.subtract(amount(p,"principal")).subtract(amount(p,"interest"));
        boolean close=Boolean.TRUE.equals(p.get("close_requested"))||equity.compareTo(value.multiply(policy.maintenance))<=0||("COVER".equals(p.get("product"))&&(q.price().compareTo(amount(p,"stop_price"))<=0||!india().toLocalTime().isBefore(LocalTime.of(15,20))||((Timestamp)p.get("created_at")).toInstant().atZone(OptionMarket.INDIA).toLocalDate().isBefore(india().toLocalDate())));
        if(close){db.update("UPDATE paper_financing SET close_requested=TRUE WHERE id=?",id);sessions.requireOpenSession(q.key().startsWith("BSE")?"BSE":"NSE");for(var pending:db.queryForList("SELECT * FROM paper_tickets WHERE user_id=? AND position_id=? AND status IN ('PENDING','TRIGGERED','PART_FILLED')",user.getId(),id))release(user,view(pending),"CANCELLED");closeFinance(user,p,number(p,"quantity"),q.price(),policy.fee(value));String ticket=UUID.randomUUID().toString();db.update("INSERT INTO paper_tickets(id,user_id,request_id,stock_id,instrument_key,position_id,side,product,variety,order_kind,validity,quantity,filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fill_value,fees,policy_version,expires_at,created_at) VALUES(?,?,?,?,?,?,'SELL',?,'REGULAR','MARKET','DAY',?,?,1,1,1,?,0,'FILLED',?,?,?,?,?)",ticket,user.getId(),ticket,t.stockId(),q.key(),id,t.product(),t.quantity(),t.quantity(),q.price(),value,policy.fee(value),policy.version(),Timestamp.from(now()),Timestamp.from(now()));db.update("INSERT INTO paper_fills(id,ticket_id,user_id,leg_number,quantity,price,fee,created_at) VALUES(?,?,?,0,?,?,?,?)",UUID.randomUUID().toString(),ticket,user.getId(),t.quantity(),q.price(),policy.fee(value),Timestamp.from(now()));}
    }
    public void financingFailure(String id){db.update("UPDATE paper_financing SET last_error=? WHERE id=? AND quantity>0","Executable quote/session unavailable; financing/closure remains retryable.",id);}
    @Transactional public void requestClose(String email,String id){var u=users.findByEmailForUpdate(email).orElseThrow();u.requireActive();if(intraday.positionOwned(id,u.getId())){intraday.requestClose(email,id);return;}position(id,u.getId());db.update("UPDATE paper_financing SET close_requested=TRUE WHERE id=?",id);}
    public Map<String,Object> summary(long user){
        BigDecimal reserved=db.queryForObject("SELECT COALESCE(SUM(reserved_cash),0) FROM paper_tickets WHERE user_id=? AND status IN ('PENDING','TRIGGERED','PART_FILLED')",BigDecimal.class,user),fees=db.queryForObject("SELECT COALESCE(SUM(fees),0) FROM paper_tickets WHERE user_id=?",BigDecimal.class,user),principal=BigDecimal.ZERO,interest=BigDecimal.ZERO,value=BigDecimal.ZERO,cost=BigDecimal.ZERO;
        for(var p:db.queryForList("SELECT * FROM paper_financing WHERE user_id=? AND quantity>0",user)){principal=principal.add(amount(p,"principal"));interest=interest.add(amount(p,"interest"));cost=cost.add(amount(p,"cost_amount"));if(date(p.get("last_accrued_date")).isBefore(india().toLocalDate().minusDays(1))||p.get("last_price")==null||p.get("mark_at")==null||((Timestamp)p.get("mark_at")).toInstant().isBefore(now().minusSeconds(60)))value=null;else if(value!=null)value=value.add(amount(p,"last_price").multiply(BigDecimal.valueOf(number(p,"quantity"))));}
        var out=new LinkedHashMap<String,Object>();out.put("reserved",reserved);out.put("fees",fees);out.put("principal",principal);out.put("interest",interest);out.put("value",value);out.put("cost",cost);out.put("realized",db.queryForObject("SELECT COALESCE(SUM(realized_pnl),0) FROM paper_financing WHERE user_id=?",BigDecimal.class,user));return out;
    }
    public void enrich(com.example.investsmart.dto.PortfolioResponse response,long user){
        var s=summary(user);response.setPaperTrading(s);
        BigDecimal reserved=(BigDecimal)s.get("reserved"),principal=(BigDecimal)s.get("principal"),interest=(BigDecimal)s.get("interest"),value=(BigDecimal)s.get("value"),cost=(BigDecimal)s.get("cost");response.setReservedPaperFunds(reserved);
        if(response.getInvestedAmount()!=null)response.setInvestedAmount(response.getInvestedAmount().add(cost));
        if(response.getRealizedPnl()!=null)response.setRealizedPnl(response.getRealizedPnl().add((BigDecimal)s.get("realized")).subtract((BigDecimal)s.get("fees")));
        if(response.getTotalValue()!=null&&value!=null){response.setTotalValue(response.getTotalValue().add(reserved).add(value).subtract(principal).subtract(interest));response.setHoldingsValue(response.getHoldingsValue().add(value));if(response.getUnrealizedPnl()!=null)response.setUnrealizedPnl(response.getUnrealizedPnl().add(value.subtract(cost).subtract(interest)));}
        else {response.setTotalValue(null);response.setHoldingsValue(null);response.setUnrealizedPnl(null);}
        if(principal.signum()>0||response.getTotalValue()==null){response.setCashAllocationPercent(null);response.setInvestedAllocationPercent(null);}
        else if(response.getTotalValue().signum()>0){response.setCashAllocationPercent(response.getTotalCash().multiply(new BigDecimal("100")).divide(response.getTotalValue(),2,RoundingMode.HALF_UP));response.setInvestedAllocationPercent(response.getHoldingsValue().multiply(new BigDecimal("100")).divide(response.getTotalValue(),2,RoundingMode.HALF_UP));}
        intraday.enrich(response,user);
    }
}
