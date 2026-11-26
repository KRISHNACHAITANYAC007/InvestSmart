package com.example.investsmart.settlement;

import java.time.Clock;
import com.example.investsmart.security.AdminHealthStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SettlementJobs {
    private final SettlementProcessing processing;private final AdminHealthStore health;private final Clock clock;private final boolean enabled;
    public SettlementJobs(SettlementProcessing processing,AdminHealthStore health,Clock clock,@Value("${investsmart.settlement.scheduler.enabled:true}") boolean enabled){this.processing=processing;this.health=health;this.clock=clock;this.enabled=enabled;}
    @Scheduled(fixedDelay=30000,initialDelay=30000) public void run(){
        if(!enabled)return;
        observe(()->health.started("t1-settlement",clock.instant()));boolean failed=false;
        try{
            for(String id:processing.candidates())try{processing.touch(id);processing.process(id);}catch(RuntimeException unavailable){failed=true;}
        }catch(RuntimeException unavailable){failed=true;}
        finally{
            observe(()->health.completed("t1-settlement",clock.instant()));
            if(failed)observe(()->health.failed("t1-settlement",clock.instant(),"Settlement processing remains retryable; inspect calendar coverage and accounting consistency."));
            else observe(()->health.succeeded("t1-settlement",clock.instant()));
        }
    }
    // Monitoring outages must not prevent guarded financial catch-up work.
    private void observe(Runnable observation){try{observation.run();}catch(RuntimeException failure){org.slf4j.LoggerFactory.getLogger(SettlementJobs.class).warn("Settlement job health observation unavailable.");}}
}
