package com.example.investsmart.investment;

import com.example.investsmart.entity.*;
import com.example.investsmart.repository.*;
import com.example.investsmart.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.security.SecureRandom;
import java.util.*;

@Service
public class IpoApplicationTransactions {
    public static final ZoneId INDIA=ZoneId.of("Asia/Kolkata");
    private final IpoApplicationRepository applications;
    private final UserRepository users;
    private final StockRepository stocks;
    private final HoldingRepository holdings;
    private final OrderRepository orders;
    private final Clock clock;
    private final com.example.investsmart.settlement.SettlementAccounting accounting;
    private final SecureRandom random=new SecureRandom();
    public IpoApplicationTransactions(IpoApplicationRepository applications,UserRepository users,StockRepository stocks,
            HoldingRepository holdings,OrderRepository orders,Clock clock,com.example.investsmart.settlement.SettlementAccounting accounting){this.accounting=accounting;this.applications=applications;this.users=users;this.stocks=stocks;this.holdings=holdings;this.orders=orders;this.clock=clock;}

    @Transactional public IpoApplicationView apply(String email,IpoMarketService.Ipo ipo,int lots){
        User user=users.findByEmailForUpdate(email).orElseThrow(()->new ResourceNotFoundException("User not found"));user.requireActive();accounting.prepare(user);
        var existing=applications.findByUserIdAndIpoId(user.getId(),ipo.id());
        if(existing.isPresent()){
            if(existing.get().getLots()!=lots)throw new BadRequestException("An application already exists for this IPO. Its quantity cannot be changed.");
            return IpoApplicationView.from(existing.get());
        }
        var now=ZonedDateTime.now(clock).withZoneSameInstant(INDIA);
        if(!ipo.available()||!"open".equals(ipo.status())||ipo.stale()||ipo.openDate()==null||ipo.closeDate()==null
                ||now.toLocalDate().isBefore(ipo.openDate())||now.toLocalDate().isAfter(ipo.closeDate())
                ||now.toLocalTime().isBefore(ipo.dailyStart()==null?LocalTime.of(10,0):ipo.dailyStart())
                ||now.toLocalTime().isAfter(ipo.dailyEnd()==null?LocalTime.of(17,0):ipo.dailyEnd()))
            throw new BadRequestException("This IPO's virtual application window is not open.");
        if(lots<1||ipo.lotSize()==null||ipo.lotSize()<1||ipo.minimumQuantity()==null||ipo.minimumQuantity()<1
                ||ipo.minimumQuantity()%ipo.lotSize()!=0||ipo.maximumPrice()==null||ipo.maximumPrice().signum()<=0)
            throw new BadRequestException("Valid lot size, minimum quantity and price band are required before applying.");
        long quantity=(long)lots*ipo.lotSize();
        if(quantity<ipo.minimumQuantity()||quantity>Integer.MAX_VALUE)throw new BadRequestException("Choose whole lots meeting the published minimum quantity.");
        BigDecimal amount=ipo.maximumPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2,RoundingMode.HALF_UP);
        if(user.getVirtualBalance().compareTo(amount)<0)throw new BadRequestException("Insufficient available virtual cash. Pending IPO funds are already reserved.");
        IpoApplication a=new IpoApplication();a.setUserId(user.getId());a.setIpoId(ipo.id());a.setName(ipo.name());a.setSymbol(ipo.symbol());a.setIsin(ipo.isin());a.setExchange(ipo.exchange());
        a.setStatus("PENDING");a.setLots(lots);a.setLotSize(ipo.lotSize());a.setMinimumQuantity(ipo.minimumQuantity());a.setAppliedQuantity((int)quantity);a.setAllottedQuantity(0);
        a.setMaximumPrice(ipo.maximumPrice());a.setReservedAmount(amount);a.setAllocatedCost(BigDecimal.ZERO);a.setDraw(BigDecimal.valueOf(random.nextDouble()));
        a.setModelVersion("overall-demand-proxy-v1");a.setReason("Virtual funds reserved. A simulated result will be resolved after 18:00 IST on the published allotment date.");
        a.setAllotmentDate(ipo.allotmentDate());a.setListingDate(ipo.listingDate());a.setAppliedAt(clock.instant());a.setSourceUpdatedAt(ipo.updatedAt());
        a=applications.saveAndFlush(a);accounting.reserve(user,"IPO_APPLICATION",a.getId().toString(),amount,false);return IpoApplicationView.from(a);
    }

    @Transactional public IpoApplicationView cancel(String email,Long id){
        User user=users.findByEmailForUpdate(email).orElseThrow(()->new ResourceNotFoundException("User not found"));user.requireActive();accounting.prepare(user);
        var a=applications.findById(id).filter(row->row.getUserId().equals(user.getId())).orElseThrow(()->new ResourceNotFoundException("Application not found"));
        if("CANCELLED".equals(a.getStatus()))return IpoApplicationView.from(a);
        if(!pending(a))throw new BadRequestException("An application that has been resolved cannot be cancelled.");
        accounting.release(user,"IPO_APPLICATION",a.getId().toString(),a.getReservedAmount(),"CANCEL");a.setReservedAmount(BigDecimal.ZERO);a.setStatus("CANCELLED");a.setResolvedAt(clock.instant());a.setReason("Virtual application cancelled; the entire reservation was returned to available cash.");
        users.save(user);return IpoApplicationView.from(applications.save(a));
    }

    @Transactional public void settle(Long id,Long userId,IpoMarketService.Ipo ipo){
        User user=users.findByIdForUpdate(userId).orElseThrow(()->new ResourceNotFoundException("User not found"));
        var a=applications.findById(id).orElseThrow();if(!a.getUserId().equals(userId)||!a.getIpoId().equals(ipo.id())||!pending(a))return;
        a.setAllotmentDate(ipo.allotmentDate());a.setListingDate(ipo.listingDate());a.setSourceUpdatedAt(ipo.updatedAt());
        if(ipo.isin()!=null)a.setIsin(ipo.isin());if(ipo.exchange()!=null)a.setExchange(ipo.exchange());
        var now=ZonedDateTime.now(clock).withZoneSameInstant(INDIA);
        if(a.getAllotmentDate()==null||now.isBefore(a.getAllotmentDate().atTime(18,0).atZone(INDIA)))return;
        BigDecimal demand=ipo.subscription();
        if(ipo.stale()||demand==null||demand.signum()<=0){a.setStatus("AWAITING_DATA");a.setReason("The allotment date has arrived, but positive subscription data is unavailable. No draw or debit was made; cancellation can release the reservation.");applications.save(a);return;}
        BigDecimal probability=modelProbability(demand);
        BigDecimal price=ipo.cutoffPrice()!=null&&ipo.cutoffPrice().signum()>0?ipo.cutoffPrice():a.getMaximumPrice();
        if(price.compareTo(a.getMaximumPrice())>0){a.setStatus("AWAITING_DATA");a.setReason("Published cut-off exceeds the reserved price band. Settlement is paused; no additional cash was charged.");applications.save(a);return;}
        boolean won=a.getDraw().compareTo(probability)<0;
        int shares=won?a.getMinimumQuantity():0;
        BigDecimal cost=price.multiply(BigDecimal.valueOf(shares)).setScale(2,RoundingMode.HALF_UP);
        if(cost.compareTo(a.getReservedAmount())>0)throw new BadRequestException("Reservation does not cover the simulated allotment.");
        accounting.prepare(user);accounting.spend(user,"IPO_APPLICATION",a.getId().toString(),cost,"ALLOTMENT");accounting.release(user,"IPO_APPLICATION",a.getId().toString(),a.getReservedAmount().subtract(cost),"ALLOTMENT_REFUND");
        a.setReservedAmount(BigDecimal.ZERO);a.setAllottedQuantity(shares);a.setAllocatedCost(cost);a.setIssuePrice(price);a.setAssumedIssuePrice(ipo.cutoffPrice()==null||ipo.cutoffPrice().signum()<=0);
        a.setProbability(probability);a.setSubscription(demand);a.setStatus(won?"ALLOTTED":"NOT_ALLOTTED");a.setResolvedAt(clock.instant());
        a.setReason((won?"Selected":"Not selected")+" by the saved simulated draw under a "+probability.multiply(new BigDecimal("100")).setScale(2,RoundingMode.HALF_UP)+"% demand-proxy model using "+demand.stripTrailingZeros().toPlainString()+"x overall subscription. This assumes application-level demand matches overall demand; actual category rules and valid application counts differ. "+(won?"Only the minimum application quantity was allocated; unused funds were refunded.":"The full reservation was refunded."));
        users.save(user);applications.save(a);
    }

    @Transactional public void promote(Long id,Long userId,Long stockId,IpoMarketService.Ipo ipo){
        User user=users.findByIdForUpdate(userId).orElseThrow();var a=applications.findById(id).orElseThrow();
        if(!a.getUserId().equals(userId)||!"ALLOTTED".equals(a.getStatus())||a.getListingOrderId()!=null)return;
        if(ipo.stale()||!a.getIpoId().equals(ipo.id())||!"listed".equals(ipo.status())||ipo.listingDate()==null
                ||LocalDate.now(clock.withZone(INDIA)).isBefore(ipo.listingDate()))throw new BadRequestException("A fresh, listed IPO is required before transferring shares.");
        var stock=stocks.findById(stockId).orElseThrow();
        if(ipo.isin()==null||!ipo.isin().equals(stock.getIsin())||(a.getIsin()!=null&&!a.getIsin().equals(ipo.isin())))throw new BadRequestException("IPO listing identity does not match.");
        a.setIsin(ipo.isin());a.setListingDate(ipo.listingDate());a.setSourceUpdatedAt(ipo.updatedAt());
        accounting.prepare(user);accounting.deliver(user,"IPO:"+id,stockId,a.getAllottedQuantity(),a.getAllocatedCost());
        var holding=holdings.findByUserIdAndStockId(userId,stockId).orElseGet(Holding::new);
        holding.setUser(user);holding.setStock(stock);holding.setQuantity(Math.addExact(holding.getQuantity()==null?0:holding.getQuantity(),a.getAllottedQuantity()));holdings.save(holding);
        var order=new Order();order.setUser(user);order.setStock(stock);order.setQuantity(a.getAllottedQuantity());order.setPrice(a.getIssuePrice());order.setTotalAmount(a.getAllocatedCost());order.setOrderType(OrderType.BUY);order.setStatus(OrderStatus.COMPLETED);order.setSource("IPO");
        order=orders.saveAndFlush(order);a.setListingOrderId(order.getId());a.setStatus("LISTED");a.setReason(a.getReason()+" Listed shares transferred to holdings at the allocated cost; cash was not charged again.");applications.save(a);
    }
    public static boolean pending(IpoApplication a){return List.of("PENDING","AWAITING_DATA").contains(a.getStatus());}
    public static BigDecimal modelProbability(BigDecimal demand){if(demand==null||demand.signum()<=0)return null;return BigDecimal.ONE.divide(demand,18,RoundingMode.HALF_UP).min(BigDecimal.ONE);}
}
