package com.example.investsmart.investment;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

@Service
public class IpoMarketService {
    public record Ipo(String id,String symbol,String name,String isin,String status,String issueType,
                      BigDecimal minimumPrice,BigDecimal maximumPrice,Integer lotSize,Integer minimumQuantity,
                      LocalDate openDate,LocalDate closeDate,LocalTime dailyStart,LocalTime dailyEnd,
                      LocalDate allotmentDate,LocalDate listingDate,BigDecimal cutoffPrice,BigDecimal subscription,
                      String exchange,String rhpUrl,String drhpUrl,Instant updatedAt,boolean stale,
                      boolean available,boolean datesOverridden) { }
    public record Page<T>(List<T> items,int page,int totalPages,int totalItems,Instant updatedAt,boolean stale) { }
    private record Cached(JsonNode data,Instant updatedAt) { }
    private final String token,baseUrl;
    private final HttpClient client;
    private final Clock clock;
    private final com.example.investsmart.security.AdminListingOverrides overrides;
    private final Map<String,Cached> cache=new HashMap<>();
    private final ObjectMapper mapper=new ObjectMapper();

    @Autowired public IpoMarketService(@Value("${upstox.api.token:}") String token,
            @Value("${upstox.api.base-url:https://api.upstox.com}") String baseUrl,Clock clock,
            com.example.investsmart.security.AdminListingOverrides overrides) {
        this(token,baseUrl,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),clock,overrides);
    }
    IpoMarketService(String token,String baseUrl,HttpClient client,Clock clock,
                     com.example.investsmart.security.AdminListingOverrides overrides){this.token=token;this.baseUrl=baseUrl;this.client=client;this.clock=clock;this.overrides=overrides;}
    IpoMarketService(String token,String baseUrl,HttpClient client,Clock clock){this(token,baseUrl,client,clock,null);}
    private synchronized Cached fetch(String path,boolean fresh) {
        Cached saved=cache.get(path);
        if(!fresh&&saved!=null&&saved.updatedAt().plusSeconds(300).isAfter(clock.instant()))return saved;
        try {
            var request=HttpRequest.newBuilder(URI.create(baseUrl+path)).timeout(Duration.ofSeconds(10))
                    .header("Accept","application/json").header("Authorization","Bearer "+token).GET().build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw new IllegalStateException("HTTP "+response.statusCode());
            JsonNode root=mapper.readTree(response.body());
            if(!"success".equals(root.path("status").asText())||root.path("data").isMissingNode())throw new IllegalStateException("Invalid IPO response");
            Cached loaded=new Cached(root,clock.instant());cache.put(path,loaded);return loaded;
        } catch(InterruptedException e){Thread.currentThread().interrupt();throw unavailable();}
        catch(Exception e){if(saved!=null&&!fresh)return saved;throw unavailable();}
    }
    private ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"IPO information is unavailable from the provider. Please retry.");}
    public Page<Ipo> list(String status,String issueType,int page){
        if(!List.of("open","upcoming","closed","listed").contains(status)||!List.of("all","regular","sme").contains(issueType)||page<1||page>10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid IPO filter or page");
        var cached=fetch("/v2/ipos?status="+status+"&page_number="+page+"&records=30"+(issueType.equals("all")?"":"&issue_type="+issueType),false);
        if(!cached.data().path("data").isArray())throw unavailable();
        boolean stale=stale(cached);var items=new ArrayList<Ipo>();
        for(JsonNode item:cached.data().path("data"))items.add(applyOverride(parse(item,cached.updatedAt(),stale)));
        var meta=cached.data().path("meta_data").path("page");
        return new Page<>(items,page,Math.max(1,meta.path("total_pages").asInt(1)),meta.path("total_records").asInt(items.size()),cached.updatedAt(),stale);
    }
    public Ipo detail(String id,boolean fresh){
        if(id==null||!id.matches("[a-zA-Z0-9_-]{1,180}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid IPO identifier");
        var cached=fetch("/v2/ipos/"+id,fresh);
        if(!cached.data().path("data").isObject())throw unavailable();
        Ipo result=applyOverride(parse(cached.data().path("data"),cached.updatedAt(),stale(cached)));
        if(!id.equals(result.id()))throw unavailable();return result;
    }
    private boolean stale(Cached c){return !c.updatedAt().plusSeconds(300).isAfter(clock.instant());}
    private Ipo applyOverride(Ipo ipo){return overrides==null?ipo:overrides.apply(ipo);}
    static Ipo parse(JsonNode n,Instant updatedAt,boolean stale){
        JsonNode t=n.path("timeline");
        return new Ipo(n.path("id").asText(),n.path("symbol").asText(),n.path("name").asText(),text(n,"isin"),n.path("status").asText(),n.path("issue_type").asText(),
                decimal(n,"minimum_price"),decimal(n,"maximum_price"),integer(n,"lot_size"),integer(n,"minimum_quantity"),
                date(n,"bidding_start_date"),date(n,"bidding_end_date"),time(n,"daily_start_time"),time(n,"daily_end_time"),
                date(t,"allotment_date"),date(t,"listing_date"),decimal(n,"cut_off_price"),decimal(n,"total_subscription"),text(n,"listing_exchange"),url(n,"rhp_url"),url(n,"drhp_url"),updatedAt,stale,true,false);
    }
    static String text(JsonNode n,String key){String value=n.path(key).asText("");return value.isBlank()?null:value;}
    static BigDecimal decimal(JsonNode n,String key){try{String v=text(n,key);return v==null?null:new BigDecimal(v);}catch(Exception e){return null;}}
    static Integer integer(JsonNode n,String key){try{BigDecimal d=decimal(n,key);return d==null?null:d.intValueExact();}catch(Exception e){return null;}}
    static LocalDate date(JsonNode n,String key){try{return LocalDate.parse(n.path(key).asText());}catch(Exception e){return null;}}
    static LocalTime time(JsonNode n,String key){try{return LocalTime.parse(n.path(key).asText());}catch(Exception e){return null;}}
    static String url(JsonNode n,String key){String value=text(n,key);try{return value!=null&&"https".equalsIgnoreCase(URI.create(value).getScheme())?value:null;}catch(Exception e){return null;}}
}
