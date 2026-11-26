package com.example.investsmart.options;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class OptionJobs {
    private final OptionTrading trading;private final boolean enabled;private final com.example.investsmart.service.ConditionalOrders equities;
    public OptionJobs(OptionTrading trading,com.example.investsmart.service.ConditionalOrders equities,@Value("${investsmart.options.scheduler.enabled:true}") boolean enabled){this.trading=trading;this.equities=equities;this.enabled=enabled;}
    @Scheduled(fixedDelay=30000,initialDelay=30000) public void run(){if(!enabled)return;for(String id:equities.pending())try{equities.touch(id);equities.process(id);}catch(RuntimeException unavailable){/* Preserve reservations and retry. */}for(String id:trading.pending())try{trading.touch(id);trading.process(id);}catch(RuntimeException unavailable){/* Preserve reservation and retry; missing/closed market must never create a fill. */}for(var row:trading.due())try{trading.settle(((Number)row.get("user_id")).longValue(),(String)row.get("instrument_key"),((java.sql.Date)row.get("expiry")).toLocalDate());}catch(RuntimeException unavailable){/* Settlement remains pending until its exact dated close is available. */}}
}
