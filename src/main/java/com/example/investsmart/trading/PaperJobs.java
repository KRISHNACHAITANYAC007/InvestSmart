package com.example.investsmart.trading;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaperJobs {
    private final IntradayTrading intraday;private final PaperTrading trading;private final boolean enabled;private final com.example.investsmart.security.AdminHealthStore health;private final java.time.Clock clock;
    public PaperJobs(PaperTrading trading,@Value("${investsmart.options.scheduler.enabled:true}") boolean enabled,com.example.investsmart.security.AdminHealthStore health,java.time.Clock clock,IntradayTrading intraday){this.intraday=intraday;this.trading=trading;this.enabled=enabled;this.health=health;this.clock=clock;}
    @Scheduled(fixedDelay=30000,initialDelay=30000) public void run(){
        if(!enabled)return;
        health.started("paper-trading",clock.instant());boolean failed=false;
        for(String id:trading.pending())try{trading.process(id);}catch(RuntimeException unavailable){failed=true;trading.failure(id);}
        for(String id:trading.financed()){
            try{trading.accruePosition(id);}catch(RuntimeException unavailable){failed=true;trading.financingFailure(id);continue;}
            try{trading.manage(id);}catch(RuntimeException unavailable){failed=true;trading.financingFailure(id);}
        }
        for(String id:intraday.positionsToManage()){try{intraday.observe(id);intraday.manage(id);}catch(RuntimeException unavailable){failed=true;intraday.failure(id);}}
        health.completed("paper-trading",clock.instant());if(failed)health.failed("paper-trading",clock.instant(),"One or more paper orders or financed positions remain retryable.");else health.succeeded("paper-trading",clock.instant());
    }
}
