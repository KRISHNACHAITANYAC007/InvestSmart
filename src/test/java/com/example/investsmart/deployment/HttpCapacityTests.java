package com.example.investsmart.deployment;

import com.example.investsmart.entity.*;
import com.example.investsmart.repository.*;
import com.example.investsmart.market.*;
import com.example.investsmart.security.*;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Opt-in real HTTP application measurement, not packaged/proxied VPS certification. */
@EnabledIfEnvironmentVariable(named="V2_LOAD_TEST",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "server.address=127.0.0.1","spring.jpa.hibernate.ddl-auto=validate","investsmart.security.initialize-schema=false",
    "investsmart.security.trusted-proxies=127.0.0.1","investsmart.paper.scheduler.enabled=false",
    "server.tomcat.threads.max=80","server.tomcat.accept-count=100","spring.datasource.hikari.maximum-pool-size=12"})
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(MySqlReleaseTests.TimeConfig.class)
class HttpCapacityTests {
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry){
        String url=System.getenv("V2_TEST_DB_URL");
        if(url==null||!url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/investsmart_release_synthetic\\?allowPublicKeyRetrieval=true&useSSL=false"))throw new IllegalStateException("Only the isolated synthetic smoke database is allowed.");
        registry.add("spring.datasource.url",()->url);registry.add("spring.datasource.username",()->System.getenv("V2_TEST_DB_USERNAME"));registry.add("spring.datasource.password",()->System.getenv("V2_TEST_DB_PASSWORD"));
    }
    @MockitoBean MarketDataService market;
    @MockitoBean com.example.investsmart.service.NseMarketSessionService sessions;
    @MockitoBean AccountMail mail;
    @Autowired UserRepository users;@Autowired StockRepository stocks;@Autowired SecurityAccounts security;@Autowired JdbcTemplate db;
    @Autowired tools.jackson.databind.ObjectMapper json;@Autowired Clock clock;
    @LocalServerPort int port;
    final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    final List<Long> reads=Collections.synchronizedList(new ArrayList<>()),writes=Collections.synchronizedList(new ArrayList<>());
    final AtomicLong failures=new AtomicLong(),limited=new AtomicLong(),scheduled=new AtomicLong(),busyDrops=new AtomicLong();
    @Autowired javax.sql.DataSource dataSource;
    final Map<Integer,AtomicLong> statuses=new ConcurrentHashMap<>();
    final AtomicLong timeouts=new AtomicLong();
    final long monitorStart=System.nanoTime();
    int minuteReadStart,minuteWriteStart;long minuteScheduled,minuteFailures;
    String monitorPhase="initialization";
    long stock;long peakHeap;int peakThreads;int peakConnections;double peakCpu;
    class Actor { long user;String cookie,csrf,ip,pending,position;String requestId=UUID.randomUUID().toString();int step;final AtomicBoolean busy=new AtomicBoolean(); }
    HttpResponse<String> request(Actor a,String method,String path,Object body)throws Exception{
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(10)).header("X-Forwarded-For",a.ip);
        if(a.cookie!=null)builder.header("Cookie",a.cookie);
        if(!method.equals("GET"))builder.header("X-XSRF-TOKEN",a.csrf).header("Content-Type","application/json");
        builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        return client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
    }
    Map<String,Object> ticket(String side,String kind,int quantity,String position){
        var t=new LinkedHashMap<String,Object>();t.put("stockId",stock);t.put("quantity",quantity);t.put("side",side);t.put("product","MIS");t.put("variety","REGULAR");t.put("orderType",kind);t.put("validity","DAY");t.put("legs",1);t.put("requestId",UUID.randomUUID().toString());
        if(kind.equals("LIMIT"))t.put("limitPrice",110);if(position!=null)t.put("positionId",position);return t;
    }
    void step(Actor a)throws Exception{
        int s=a.step%80;String method="GET",path="/settlement/summary";Object body=null;
        if(s==0){method="POST";path="/orders/paper/preview";body=ticket("SELL","MARKET",2,null);}
        if(s==1){method="POST";path="/orders/paper";body=ticket("SELL","LIMIT",2,null);}
        if(s==2){if(a.pending==null)throw new IllegalStateException("Missing pending ticket");method="DELETE";path="/orders/paper/"+a.pending;}
        if(s==3){method="POST";path="/orders/paper";body=ticket("SELL","MARKET",2,null);}
        if(s==6||s==7){if(a.position==null)throw new IllegalStateException("Missing owned position");method="POST";path="/orders/paper";body=ticket("BUY","MARKET",1,a.position);}
        if(s>=8&&s%2==0)path="/orders/paper";
        if(s>=8&&s%3==0)path="/settlement/history";
        if(body instanceof Map<?,?>) {
            @SuppressWarnings("unchecked") var instructions=(Map<String,Object>)body;
            instructions.put("requestId",a.requestId);
        }
        long start=System.nanoTime();var response=request(a,method,path,body);long elapsed=System.nanoTime()-start;
        (method.equals("GET")||s==0?reads:writes).add(elapsed);
        statuses.computeIfAbsent(response.statusCode(),key->new AtomicLong()).incrementAndGet();
        if(response.statusCode()==429)limited.incrementAndGet();
        if(response.statusCode()!=200)throw new IllegalStateException("HTTP status "+response.statusCode());
        if(s==1)a.pending=json.readTree(response.body()).path("id").asText();
        if(s==2)a.pending=null;
        if(s==3)a.position=json.readTree(response.body()).path("position_id").asText();
        if(s==7)a.position=null;
        a.step++;a.requestId=UUID.randomUUID().toString();
    }
    void sample()throws Exception{
        var os=java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        double currentCpu=os instanceof com.sun.management.OperatingSystemMXBean observed?observed.getProcessCpuLoad():-1;
        peakCpu=Math.max(peakCpu,currentCpu);
        peakHeap=Math.max(peakHeap,java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        peakThreads=Math.max(peakThreads,java.lang.management.ManagementFactory.getThreadMXBean().getThreadCount());
        peakConnections=Math.max(peakConnections,db.query("SHOW STATUS LIKE 'Threads_connected'",(rs,n)->rs.getInt(2)).getFirst());
        var m=new LinkedHashMap<String,Object>();
        m.put("wallMillis",System.currentTimeMillis());m.put("elapsedSeconds",(System.nanoTime()-monitorStart)/1_000_000_000L);m.put("phase",monitorPhase);
        var memory=java.lang.management.ManagementFactory.getMemoryMXBean();
        m.put("heapUsed",memory.getHeapMemoryUsage().getUsed());m.put("heapMax",memory.getHeapMemoryUsage().getMax());m.put("nonHeapUsed",memory.getNonHeapMemoryUsage().getUsed());
        m.put("threads",java.lang.management.ManagementFactory.getThreadMXBean().getThreadCount());
        if(os instanceof com.sun.management.OperatingSystemMXBean observed){m.put("processCpu",currentCpu);m.put("systemCpu",observed.getCpuLoad());m.put("committedVirtualMemory",observed.getCommittedVirtualMemorySize());}
        m.put("gc",java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream().map(gc->Map.of("name",gc.getName(),"count",gc.getCollectionCount(),"millis",gc.getCollectionTime())).toList());
        m.put("memoryPools",java.lang.management.ManagementFactory.getMemoryPoolMXBeans().stream().filter(mp->mp.getCollectionUsage()!=null).map(mp->Map.of("name",mp.getName(),"postGcUsed",mp.getCollectionUsage().getUsed())).toList());
        if(dataSource instanceof com.zaxxer.hikari.HikariDataSource hikari){var pool=hikari.getHikariPoolMXBean();m.put("hikari",Map.of("active",pool.getActiveConnections(),"idle",pool.getIdleConnections(),"pending",pool.getThreadsAwaitingConnection(),"total",pool.getTotalConnections(),"max",hikari.getMaximumPoolSize()));}
        m.put("database",db.queryForList("SHOW GLOBAL STATUS WHERE Variable_name IN ('Threads_connected','Threads_running','Max_used_connections','Innodb_row_lock_waits','Innodb_row_lock_time')"));
        m.put("scheduled",scheduled.get());m.put("errors",failures.get());m.put("rateLimited",limited.get());m.put("busyDrops",busyDrops.get());m.put("timeouts",timeouts.get());
        var counts=new TreeMap<String,Long>();statuses.forEach((status,count)->counts.put(status.toString(),count.get()));m.put("httpStatuses",counts);
        Files.writeString(Path.of("target/http-capacity-metrics.jsonl"),json.writeValueAsString(m)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    void minute()throws Exception{
        var r=new ArrayList<Long>();var w=new ArrayList<Long>();
        synchronized(reads){r.addAll(reads.subList(minuteReadStart,reads.size()));minuteReadStart=reads.size();}
        synchronized(writes){w.addAll(writes.subList(minuteWriteStart,writes.size()));minuteWriteStart=writes.size();}
        var m=new LinkedHashMap<String,Object>();m.put("phase",monitorPhase);m.put("elapsedSeconds",(System.nanoTime()-monitorStart)/1_000_000_000L);m.put("requests",scheduled.get()-minuteScheduled);m.put("errors",failures.get()-minuteFailures);m.put("readP95Ms",percentile(r));m.put("writeP95Ms",percentile(w));
        minuteScheduled=scheduled.get();minuteFailures=failures.get();
        Files.writeString(Path.of("target/http-capacity-minutes.jsonl"),json.writeValueAsString(m)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    Map<String,Object> phase(List<Actor> actors,ExecutorService pool,int seconds,int rate)throws Exception{
        long initialFailures=failures.get(),initialLimited=limited.get(),initialScheduled=scheduled.get(),start=System.nanoTime();int readStart=reads.size(),writeStart=writes.size(),i=0;
        monitorPhase=rate==30?"steady":"burst";long previousWall=System.currentTimeMillis();
        while(System.nanoTime()-start<seconds*1_000_000_000L){
            long target=start+i*1_000_000_000L/rate;long delay=target-System.nanoTime();if(delay>0)LockSupport.parkNanos(delay);
            long wall=System.currentTimeMillis();CapacityContinuity.check(previousWall,wall,System.nanoTime()-target);previousWall=wall;
            Actor a=actors.get(i++%actors.size());scheduled.incrementAndGet();
            if(!a.busy.compareAndSet(false,true)){busyDrops.incrementAndGet();failures.incrementAndGet();continue;}
            pool.submit(()->{try{step(a);}catch(Exception e){failures.incrementAndGet();if(e instanceof java.net.http.HttpTimeoutException)timeouts.incrementAndGet();}finally{a.busy.set(false);}});
            if(i%(rate*5)==0)sample();
            if(i%(rate*60)==0)minute();
            if(i%(rate*60)==0)Files.writeString(Path.of("target/http-capacity-progress.json"),json.writeValueAsString(Map.of("phaseTargetRps",rate,"phaseElapsedSeconds",(System.nanoTime()-start)/1_000_000_000L,"scheduled",scheduled.get(),"unexpectedErrors",failures.get(),"busyDrops",busyDrops.get(),"peakHeapBytes",peakHeap)));
        }
        CapacityContinuity.check(previousWall,System.currentTimeMillis(),0);
        long drainDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);
        while(actors.stream().anyMatch(a->a.busy.get())){if(System.nanoTime()>drainDeadline)throw new IllegalStateException("Load drain timed out");Thread.sleep(10); }
        double readP95=percentile(reads.subList(readStart,reads.size())),writeP95=percentile(writes.subList(writeStart,writes.size()));
        long count=scheduled.get()-initialScheduled;double errors=(failures.get()-initialFailures)/(double)Math.max(1,count);
        return Map.of("seconds",seconds,"targetRps",rate,"scheduled",count,"unexpectedErrors",failures.get()-initialFailures,"rateLimited",limited.get()-initialLimited,"readP95Ms",readP95,"writeP95Ms",writeP95,"thresholdsPassed",readP95>=0&&writeP95>=0&&readP95<=500&&writeP95<=2000&&errors<.01);
    }
    double percentile(List<Long> samples){if(samples.isEmpty())return -1;var copy=new ArrayList<>(samples);Collections.sort(copy);return copy.get((int)Math.ceil(copy.size()*.95)-1)/1_000_000d;}
    @Test void measureSyntheticHttpWorkload()throws Exception{
        int steady=Integer.parseInt(System.getenv().getOrDefault("V2_LOAD_STEADY_SECONDS","1800")),burst=Integer.parseInt(System.getenv().getOrDefault("V2_LOAD_BURST_SECONDS","300"));
        if(steady<1||steady>1800||burst<1||burst>300)throw new IllegalArgumentException("Bounded load duration required");
        for(String file:List.of("metrics","minutes"))Files.deleteIfExists(Path.of("target/http-capacity-"+file+".jsonl"));
        var listing=new Stock();listing.setSymbol("CAPACITY_"+UUID.randomUUID());listing.setInstrumentKey("NSE_EQ|"+UUID.randomUUID());listing.setExchange("NSE");listing.setCurrentPrice(new BigDecimal("100"));listing=stocks.saveAndFlush(listing);stock=listing.getId();final var quoteListing=listing;
        when(market.getQuote(any(MarketInstrument.class))).thenAnswer(call->new MarketQuote(quoteListing.getSymbol(),new BigDecimal("100"),null,clock.instant().atZone(com.example.investsmart.settlement.SettlementCalendar.INDIA).toLocalDateTime()).withInstrument(new MarketInstrument(quoteListing.getInstrumentKey(),quoteListing.getSymbol(),"Synthetic","NSE")));
        var actors=new ArrayList<Actor>();
        for(int i=0;i<100;i++){
            var u=new User();u.setEmail(UUID.randomUUID()+"@example.invalid");u.setRole("USER");u.setEmailVerified(true);u.setVirtualBalance(new BigDecimal("100000"));u=users.saveAndFlush(u);
            var a=new Actor();a.user=u.getId();a.ip="198.18.0."+(i+1);a.cookie=SecurityAccounts.COOKIE+"="+security.issue(u,new MockHttpServletRequest(),new MockHttpServletResponse());
            var csrf=request(a,"GET","/auth/csrf",null);assertEquals(200,csrf.statusCode());
            String pair=csrf.headers().allValues("set-cookie").stream().filter(c->c.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow().split(";",2)[0];a.csrf=URLDecoder.decode(pair.substring(pair.indexOf('=')+1),java.nio.charset.StandardCharsets.UTF_8);a.cookie+="; "+pair;actors.add(a);
        }
        var pool=Executors.newFixedThreadPool(100);var report=new LinkedHashMap<String,Object>();report.put("completed",false);report.put("fullDuration",false);
        try{
            // Warm established sessions outside timing; report cold costs separately.
            // Four orders per 80 requests respect the unchanged 30/user/15-minute limit.
            var warmupPool=Executors.newFixedThreadPool(10);
            try {
                var warmup=new ArrayList<Future<?>>();
                for(var a:actors)warmup.add(warmupPool.submit(()->{try{for(int i=0;i<8;i++)step(a);}catch(Exception e){throw new IllegalStateException("Synthetic warmup failed",e);}}));
                for(var job:warmup)job.get(180,TimeUnit.SECONDS);
            } finally { warmupPool.shutdownNow();assertTrue(warmupPool.awaitTermination(15,TimeUnit.SECONDS)); }
            report.put("warmupReadP95Ms",percentile(reads));report.put("warmupWriteP95Ms",percentile(writes));
            reads.clear();writes.clear();statuses.clear();failures.set(0);limited.set(0);scheduled.set(0);busyDrops.set(0);
            for(int i=0;i<actors.size();i++)actors.get(i).step=8+i%72;
            var steadyResult=phase(actors,pool,steady,30);var burstResult=phase(actors,pool,burst,60);report.put("steady",steadyResult);report.put("burst",burstResult);
            boolean reconciled=true;
            for(var a:actors){
                if(a.pending!=null)assertEquals(200,request(a,"DELETE","/orders/paper/"+a.pending,null).statusCode());
                for(var p:db.queryForList("SELECT id,quantity FROM paper_intraday_positions WHERE user_id=? AND quantity>0",a.user))assertEquals(200,request(a,"POST","/orders/paper",ticket("BUY","MARKET",((Number)p.get("quantity")).intValue(),(String)p.get("id"))).statusCode());
                BigDecimal fees=db.queryForObject("SELECT COALESCE(SUM(fee),0) FROM paper_intraday_events WHERE user_id=?",BigDecimal.class,a.user);
                BigDecimal balance=users.findById(a.user).orElseThrow().getVirtualBalance();
                reconciled&=balance.compareTo(new BigDecimal("100000").subtract(fees))==0;
                reconciled&=db.queryForObject("SELECT COUNT(*) FROM holdings WHERE user_id=?",Integer.class,a.user)==0;
                reconciled&=db.queryForObject("SELECT COUNT(*) FROM paper_intraday_positions WHERE user_id=? AND (quantity<>0 OR collateral<>0)",Integer.class,a.user)==0;
                reconciled&=db.queryForObject("SELECT COALESCE(SUM(remaining_amount),0) FROM settlement_allocations WHERE user_id=?",BigDecimal.class,a.user).signum()==0;
            }
            double readP95=percentile(reads),writeP95=percentile(writes),errorRate=failures.get()/(double)Math.max(1,scheduled.get());
            boolean passed=Boolean.TRUE.equals(steadyResult.get("thresholdsPassed"))&&Boolean.TRUE.equals(burstResult.get("thresholdsPassed"))&&reconciled;
            monitorPhase="cooldown";
            int cooldown=steady==1800&&burst==300?300:5;
            for(int elapsed=0;elapsed<cooldown;elapsed+=5){long wall=System.currentTimeMillis();sample();Thread.sleep(5000);CapacityContinuity.check(wall,System.currentTimeMillis(),0);}
            report.put("cooldownSeconds",cooldown);report.put("monitoringCompleted",true);
            report.put("httpStatuses",statuses.entrySet().stream().collect(java.util.stream.Collectors.toMap(e->e.getKey().toString(),e->e.getValue().get())));report.put("timeouts",timeouts.get());
            report.put("peakProcessCpuFraction",peakCpu);
            report.put("sessions",100);report.put("fullDuration",steady==1800&&burst==300);report.put("thresholdsPassed",passed);report.put("financialReconciliation",reconciled);report.put("readP95Ms",readP95);report.put("writeP95Ms",writeP95);report.put("errorRate",errorRate);report.put("busyDrops",busyDrops.get());report.put("peakHeapBytes",peakHeap);report.put("peakJvmThreads",peakThreads);report.put("peakDbConnections",peakConnections);report.put("availableCpus",Runtime.getRuntime().availableProcessors());report.put("maxHeapBytes",Runtime.getRuntime().maxMemory());report.put("jdk",System.getProperty("java.version"));report.put("os",System.getProperty("os.name"));report.put("scope","Opt-in synthetic HTTP app + MySQL; not packaged VPS/proxy/provider/device acceptance");
            Path out=Path.of("target/http-capacity-result.json");Files.createDirectories(out.getParent());Files.writeString(out,json.writeValueAsString(report));
            report.put("completed",true);Files.writeString(out,json.writeValueAsString(report));
            assertTrue(passed,"Capacity thresholds or financial reconciliation failed; inspect aggregate report.");
        }catch(Exception|AssertionError failure){
            report.put("interrupted",failure instanceof CapacityContinuity.InterruptedBenchmark);
            report.put("failureKind",failure.getClass().getSimpleName());
            report.put("scheduled",scheduled.get());report.put("unexpectedErrors",failures.get());report.put("busyDrops",busyDrops.get());report.put("timeouts",timeouts.get());
            Files.writeString(Path.of("target/http-capacity-result.json"),json.writeValueAsString(report));throw failure;
        }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(15,TimeUnit.SECONDS));}
    }
}
