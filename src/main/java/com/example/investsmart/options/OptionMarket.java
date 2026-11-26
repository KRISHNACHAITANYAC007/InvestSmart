package com.example.investsmart.options;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.*;

/** Authorized provider data only. Contract metadata and executable depth are never supplied by clients. */
@Service
public class OptionMarket {
    public static final String UNDERLYING="NSE_INDEX|Nifty 50";
    public static final ZoneId INDIA=ZoneId.of("Asia/Kolkata");
    public record Contract(String instrumentKey,String symbol,LocalDate expiry,BigDecimal strike,String type,int lotSize,int freezeQuantity) {}
    public record Quote(Contract contract,BigDecimal ltp,BigDecimal bid,BigDecimal ask,int bidQuantity,int askQuantity,BigDecimal openInterest,BigDecimal volume,Instant fetchedAt) {}
    private final String token,base; private final HttpClient http; private final ObjectMapper json;
    @Autowired public OptionMarket(@Value("${upstox.api.token:}") String token,@Value("${upstox.api.base-url:https://api.upstox.com}") String base,ObjectMapper json){this(token,base,json,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());}
    OptionMarket(String token,String base,ObjectMapper json,HttpClient http){this.token=token;this.base=base.replaceAll("/+$","");this.json=json;this.http=http;}
    private JsonNode get(String path){
        if(token.isBlank())throw unavailable();
        try{
            var request=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(10)).header("Authorization","Bearer "+token).header("Accept","application/json").GET().build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw unavailable();
            var root=json.readTree(response.body());
            if(!"success".equals(root.path("status").asText())||!root.path("data").isArray())throw unavailable();
            return root.path("data");
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw unavailable();}catch(Exception e){throw unavailable();}
    }
    private ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"NIFTY options data unavailable. Check provider access and retry.");}
    public List<Contract> contracts(){
        List<Contract> result=new ArrayList<>();
        for(var n:get("/v2/option/contract?instrument_key="+URLEncoder.encode(UNDERLYING,StandardCharsets.UTF_8))){
            try{
                String key=n.path("instrument_key").asText(),type=n.path("instrument_type").asText();
                LocalDate expiry=LocalDate.parse(n.path("expiry").asText());BigDecimal strike=number(n.path("strike_price"));
                int lot=n.path("lot_size").asInt(),freeze=n.path("freeze_quantity").asInt();
                if(!UNDERLYING.equals(n.path("underlying_key").asText())||!"NSE_FO".equals(n.path("segment").asText())||!key.matches("NSE_FO\\|[0-9]+")||!List.of("CE","PE").contains(type)||lot<=0||freeze<lot||strike==null||strike.signum()<=0||expiry.isBefore(LocalDate.now(INDIA)))continue;
                result.add(new Contract(key,n.path("trading_symbol").asText(),expiry,strike,type,lot,freeze));
            }catch(RuntimeException malformed){/* Invalid individual contracts cannot become tradable. */}
        }
        return result.stream().sorted(Comparator.comparing(Contract::expiry).thenComparing(Contract::strike).thenComparing(Contract::type)).toList();
    }
    public List<Quote> chain(LocalDate expiry){
        var contracts=contracts().stream().filter(c->c.expiry().equals(expiry)).toList();
        if(contracts.isEmpty())return List.of();
        var byKey=new HashMap<String,Contract>();contracts.forEach(c->byKey.put(c.instrumentKey(),c));
        List<Quote> result=new ArrayList<>();Instant now=Instant.now();
        for(var row:get("/v2/option/chain?instrument_key="+URLEncoder.encode(UNDERLYING,StandardCharsets.UTF_8)+"&expiry_date="+expiry)){
            if(!UNDERLYING.equals(row.path("underlying_key").asText())||!expiry.toString().equals(row.path("expiry").asText()))continue;
            for(String side:List.of("call_options","put_options")){
                var option=row.path(side);var c=byKey.get(option.path("instrument_key").asText());
                if(c==null||!c.type().equals(side.equals("call_options")?"CE":"PE")||c.strike().compareTo(Optional.ofNullable(number(row.path("strike_price"))).orElse(BigDecimal.ZERO))!=0)continue;
                var data=option.path("market_data");
                result.add(new Quote(c,number(data.path("ltp")),number(data.path("bid_price")),number(data.path("ask_price")),data.path("bid_qty").asInt(),data.path("ask_qty").asInt(),number(data.path("oi")),number(data.path("volume")),now));
            }
        }
        return result;
    }
    static BigDecimal number(JsonNode n){return n.isNumber()?n.decimalValue():null;}
}
