package com.example.investsmart.settlement;

import com.example.investsmart.entity.User;
import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.repository.UserRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Coordinates ledger and JPA projection in the caller's shared-user-lock transaction. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class SettlementAccounting {
    private final SettlementFunds funds;
    private final SettlementLots lots;
    private final UserRepository users;
    private final Clock clock;
    public SettlementAccounting(SettlementFunds funds, SettlementLots lots, UserRepository users, Clock clock) {
        this.funds=funds;this.lots=lots;this.users=users;this.clock=clock;
    }
    public void prepare(User user) {
        users.flush(); // Capture only persisted pre-mutation state, including legacy reservations.
        project(user);
    }
    public void project(User user) {
        var balance=funds.balances(user.getId());
        user.setVirtualBalance(balance.settled().add(balance.reusable()));
        users.saveAndFlush(user);
    }
    private void apply(User user,Runnable operation) {
        try { operation.run(); project(user); }
        catch(IllegalArgumentException rejected) { throw new BadRequestException(rejected.getMessage()); }
    }
    public void reserve(User user,String kind,String id,BigDecimal amount,boolean reusable) {
        if(amount.signum()==0)return;
        apply(user,()->funds.reserve(user.getId(),"RESERVE:"+kind+":"+id,kind,id,amount,reusable));
    }
    public void spend(User user,String kind,String id,BigDecimal amount,String part) {
        if(amount.signum()==0)return;
        apply(user,()->funds.spend(user.getId(),"SPEND:"+kind+":"+id+":"+part,kind,id,amount));
    }
    public void release(User user,String kind,String id,BigDecimal amount,String part) {
        if(amount.signum()==0)return;
        apply(user,()->funds.release(user.getId(),"RELEASE:"+kind+":"+id+":"+part,kind,id,amount));
    }
    public void pay(User user,String id,BigDecimal amount,boolean settledOnly) {
        reserve(user,"PAYMENT",id,amount,!settledOnly);
        spend(user,"PAYMENT",id,amount,"FULL");
    }
    public void credit(User user,String id,BigDecimal amount) {
        if(amount.signum()==0)return;
        apply(user,()->funds.creditSettled(user.getId(),"CREDIT:"+id,amount));
    }
    public void withdraw(User user,String id,BigDecimal amount) {
        apply(user,()->funds.withdraw(user.getId(),"WITHDRAW:"+id,amount));
    }
    public void fee(User user,String id,BigDecimal amount) {
        if(amount.signum()==0)return;
        apply(user,()->funds.chargeDebt(user.getId(),"CHARGE:"+id,amount));
    }
    public void pending(User user,String id,String exchange,String segment,LocalDate date,BigDecimal amount,boolean restricted) {
        if(amount.signum()==0)return;
        apply(user,()->funds.creditPending(user.getId(),"PENDING:"+id,id,exchange,segment,date,amount,restricted?SettlementFunds.CreditPolicy.RESTRICTED:SettlementFunds.CreditPolicy.REUSABLE));
    }
    public void purchase(User user,String id,long stock,int quantity,BigDecimal cost,String exchange) {
        apply(user,()->lots.purchase(user.getId(),"BUY_LOT:"+id,id,stock,quantity,cost,exchange,tradeDate()));
    }
    public void sell(User user,String id,long stock,int quantity,BigDecimal price,String exchange) {
        apply(user,()->{
            String event="SELL_LOTS:"+id;
            var sold=lots.dispose(user.getId(),event,stock,quantity);
            BigDecimal reusable=price.multiply(BigDecimal.valueOf(sold.settledUnits()));
            BigDecimal restricted=price.multiply(BigDecimal.valueOf(sold.unsettledUnits()));
            if(reusable.signum()>0)funds.creditPending(user.getId(),"SALE:"+id,id+":SALE",exchange,"EQUITY",tradeDate(),reusable,SettlementFunds.CreditPolicy.REUSABLE);
            if(restricted.signum()>0){
                String credit=funds.creditPending(user.getId(),"BTST:"+id,id+":BTST",exchange,"EQUITY",tradeDate(),restricted,SettlementFunds.CreditPolicy.RESTRICTED);
                var deliveries=lots.unsettledDeliveries(user.getId(),event);
                if(deliveries.isEmpty())throw new IllegalStateException("BTST sale requires its original delivery obligations.");
                funds.requireDeliveries(user.getId(),"BTST_DELIVERY:"+id,credit,deliveries);
            }
        });
    }
    public void deliver(User user,String id,long stock,int quantity,BigDecimal cost) {
        apply(user,()->lots.deliverSettled(user.getId(),"DELIVER:"+id,id,stock,quantity,cost));
    }
    public LocalDate tradeDate(){return clock.instant().atZone(SettlementCalendar.INDIA).toLocalDate();}
    public String request(){return UUID.randomUUID().toString();}
}
