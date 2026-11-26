package com.example.investsmart.investment;
import com.example.investsmart.market.EquityCatalogService;
import com.example.investsmart.service.StockService;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.*;
import java.time.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import com.example.investsmart.security.AdminHealthStore;

@Component
public class IpoSettlementJob {
    private static final Logger log=LoggerFactory.getLogger(IpoSettlementJob.class);
    private final IpoApplicationRepository applications;private final IpoMarketService market;private final IpoApplicationTransactions transactions;
    private final EquityCatalogService catalog;private final StockService stocks;private final Clock clock;private final boolean enabled;
    private final AdminHealthStore healthStore;
    private volatile Instant lastStartedAt,lastCompletedAt,lastFailureAt;private volatile String lastFailure;private volatile long failuresSinceRestart;
    @Autowired public IpoSettlementJob(IpoApplicationRepository applications,IpoMarketService market,IpoApplicationTransactions transactions,EquityCatalogService catalog,StockService stocks,Clock clock,@Value("${investsmart.ipo.scheduler.enabled:true}") boolean enabled,AdminHealthStore healthStore){this.applications=applications;this.market=market;this.transactions=transactions;this.catalog=catalog;this.stocks=stocks;this.clock=clock;this.enabled=enabled;this.healthStore=healthStore;}
    IpoSettlementJob(IpoApplicationRepository applications,IpoMarketService market,IpoApplicationTransactions transactions,EquityCatalogService catalog,StockService stocks,Clock clock,boolean enabled){this(applications,market,transactions,catalog,stocks,clock,enabled,null);}
    @Scheduled(cron="0 5 * * * *",zone="Asia/Kolkata")
    @EventListener(ApplicationReadyEvent.class)
    public void run(){
        if(!enabled)return;
        lastStartedAt=clock.instant();
        if(healthStore!=null)healthStore.started("IPO_SETTLEMENT",lastStartedAt);
        var details=new HashMap<String,IpoMarketService.Ipo>();
        for(var a:applications.findByStatusIn(List.of("PENDING","AWAITING_DATA","ALLOTTED"))){
            try {
                var detail=details.computeIfAbsent(a.getIpoId(),id->market.detail(id,true));
                if(IpoApplicationTransactions.pending(a))transactions.settle(a.getId(),a.getUserId(),detail);
                var current=applications.findById(a.getId()).orElseThrow();
                var today=LocalDate.now(clock.withZone(IpoApplicationTransactions.INDIA));
                if("ALLOTTED".equals(current.getStatus())&&detail.isin()!=null&&detail.listingDate()!=null&&!today.isBefore(detail.listingDate())&&"listed".equals(detail.status())){
                    var listing=catalog.listingsByIsin(detail.isin()).stream().findFirst().orElseThrow();
                    var stock=stocks.importNseStock(listing.getInstrumentKey(),listing.getSymbol());transactions.promote(current.getId(),current.getUserId(),stock.getId(),detail);
                }
            }catch(RuntimeException failure){failuresSinceRestart++;lastFailureAt=clock.instant();lastFailure=failure.getClass().getSimpleName();if(healthStore!=null)healthStore.failed("IPO_SETTLEMENT",lastFailureAt,lastFailure);log.info("IPO application {} deferred after {}",a.getId(),failure.getClass().getSimpleName());}
        }
        lastCompletedAt=clock.instant();
        if(healthStore!=null)healthStore.completed("IPO_SETTLEMENT",lastCompletedAt);
    }
    public Map<String,Object> health(){Map<String,Object> status=healthStore==null?new LinkedHashMap<>():new LinkedHashMap<>(healthStore.status("IPO_SETTLEMENT"));if(healthStore==null){status.put("lastStartedAt",lastStartedAt);status.put("lastCompletedAt",lastCompletedAt);status.put("lastFailureAt",lastFailureAt);status.put("lastFailure",lastFailure);status.put("failureCount",failuresSinceRestart);}status.put("enabled",enabled);status.put("failuresSinceRestart",failuresSinceRestart);return status;}
}
