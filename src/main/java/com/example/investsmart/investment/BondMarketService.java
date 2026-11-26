package com.example.investsmart.investment;

import com.example.investsmart.market.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import java.nio.file.*;

/** Debt discovery is deliberately separate from the equity and paper-order catalogs. */
@Service
public class BondMarketService {
    public record Terms(BigDecimal coupon,BigDecimal faceValue,LocalDate maturity,String rating,String sourceUrl,LocalDate asOf,LocalDate verifiedAt) { }
    public record Bond(String instrumentKey,String symbol,String name,String isin,String exchange,String category,String instrumentType,Terms terms,boolean available) { }
    public record Detail(Bond bond,MarketQuote quote,Instant catalogUpdatedAt,boolean stale,String quoteMessage) { }
    public record BondPage(List<Bond> items,int page,int totalPages,int totalItems,Instant updatedAt,boolean stale,int catalogSize,int verifiedTermsCount) { }
    private final HttpClient client; private final String base; private final Clock clock; private final MarketDataProvider provider;
    private final com.example.investsmart.security.AdminListingOverrides overrides;
    private List<Bond> bonds; private Instant updatedAt=Instant.EPOCH;
    private final Map<String,Terms> reference=new HashMap<>();
    @Autowired public BondMarketService(MarketDataProvider provider,Clock clock,@Value("${investsmart.bonds.reference-file:}") String referenceFile,
            com.example.investsmart.security.AdminListingOverrides overrides){this(provider,clock,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),"https://assets.upstox.com/market-quote/instruments/exchange/",referenceFile,overrides);}
    BondMarketService(MarketDataProvider provider,Clock clock,HttpClient client,String base){this(provider,clock,client,base,"",null);}
    BondMarketService(MarketDataProvider provider,Clock clock,HttpClient client,String base,String referenceFile){this(provider,clock,client,base,referenceFile,null);}
    BondMarketService(MarketDataProvider provider,Clock clock,HttpClient client,String base,String referenceFile,
                      com.example.investsmart.security.AdminListingOverrides overrides){
        this.provider=provider;this.clock=clock;this.client=client;this.base=base;this.overrides=overrides;
        try(var input=getClass().getResourceAsStream("/bond-reference.csv")){
            if(input==null)throw new IOException("Reference snapshot missing");loadReference(input);
            if(!referenceFile.isBlank())try(var external=Files.newInputStream(Path.of(referenceFile))){loadReference(external);}
        }catch(Exception e){throw new IllegalStateException("Bond reference file must contain valid, dated ISIN terms and HTTPS sources.",e);}
    }
    private void loadReference(InputStream input) throws IOException {
        try(var reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))){
            if(!"isin,coupon,faceValue,maturity,rating,sourceUrl,asOf,verifiedAt".equals(reader.readLine()))throw new IOException("Invalid reference header");
            String line;var seen=new HashSet<String>();var today=LocalDate.now(clock);
            while((line=reader.readLine())!=null){if(line.isBlank()||line.startsWith("#"))continue;String[] c=line.split(",",-1);
                if(c.length!=8||!c[0].matches("IN[A-Z0-9]{10}")||!seen.add(c[0]))throw new IOException("Invalid or duplicate reference ISIN");
                var source=URI.create(c[5]);var coupon=number(c[1]);var face=number(c[2]);var asOf=date(c[6]);var verified=date(c[7]);
                if(!"https".equals(source.getScheme())||source.getHost()==null||asOf==null||verified==null||verified.isBefore(asOf)||verified.isAfter(today)
                        ||(coupon!=null&&(coupon.signum()<0||coupon.compareTo(new BigDecimal("100"))>0))||(face!=null&&face.signum()<=0)
                        ||(coupon==null&&face==null&&c[3].isBlank()&&c[4].isBlank()))throw new IOException("Invalid reference terms");
                var previous=reference.get(c[0]);if(previous!=null&&previous.asOf().isAfter(asOf))throw new IOException("Reference override predates stored terms");
                reference.put(c[0],new Terms(coupon,face,date(c[3]),c[4].isBlank()?null:c[4],c[5],asOf,verified));
            }
        }
    }
    private static BigDecimal number(String s){return s.isBlank()?null:new BigDecimal(s);}
    private static LocalDate date(String s){return s.isBlank()?null:LocalDate.parse(s);}
    static String category(String type){return List.of("GS","SG","TB").contains(type)?"government":type.matches("N[A-Z0-9]")?"corporate":null;}
    private synchronized List<Bond> catalog(){
        if(bonds!=null&&updatedAt.plusSeconds(86400).isAfter(clock.instant()))return bonds;
        try{
            var response=client.send(HttpRequest.newBuilder(URI.create(base+"NSE.json.gz")).timeout(Duration.ofSeconds(20)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            if(response.statusCode()!=200)throw new IOException();
            JsonNode root;try(var gzip=new GZIPInputStream(new ByteArrayInputStream(response.body()))){root=new ObjectMapper().readTree(gzip);}
            if(!root.isArray())throw new IOException();
            var rows=new ArrayList<Bond>();
            for(JsonNode n:root){String type=n.path("instrument_type").asText(),isin=n.path("isin").asText(),key=n.path("instrument_key").asText(),category=category(type);
                if(category==null||!"NSE_EQ".equals(n.path("segment").asText())||!isin.matches("IN[A-Z0-9]{10}")||!key.equals("NSE_EQ|"+isin))continue;
                rows.add(new Bond(key,n.path("trading_symbol").asText(),n.path("name").asText(),isin,"NSE",category,type,reference.get(isin),true));
            }
            if(rows.isEmpty())throw new IOException();
            bonds=rows.stream().sorted(Comparator.comparing(Bond::name).thenComparing(Bond::isin)).toList();updatedAt=clock.instant();
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw unavailable();}
        catch(Exception e){if(bonds==null)throw unavailable();}
        return bonds;
    }
    private ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Bond catalog is unavailable. Please retry.");}
    public BondPage list(String category,String query,int page){return list(category,query,page,false);}
    public BondPage list(String category,String query,int page,boolean verifiedOnly){return list(category,query,page,verifiedOnly,false);}
    public BondPage adminList(String category,String query,int page){return list(category,query,page,false,true);}
    private BondPage list(String category,String query,int page,boolean verifiedOnly,boolean includeUnavailable){
        if(!List.of("all","government","corporate").contains(category)||query.length()>80||page<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid bond filters");
        String filter=query.strip().toLowerCase(Locale.ROOT);
        var candidates=catalog().stream().map(this::applyOverride).filter(b->includeUnavailable||b.available()).filter(b->(category.equals("all")||b.category().equals(category))&&(b.name()+" "+b.symbol()+" "+b.isin()).toLowerCase(Locale.ROOT).contains(filter)).toList();
        int verified=(int)candidates.stream().filter(b->b.terms()!=null).count();
        var rows=candidates.stream().filter(b->!verifiedOnly||b.terms()!=null).toList();
        int pages=Math.max(1,(rows.size()+29)/30);if(page>pages)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bond page out of range");
        return new BondPage(rows.subList(Math.min(rows.size(),(page-1)*30),Math.min(rows.size(),page*30)),page,pages,rows.size(),updatedAt,stale(),candidates.size(),verified);
    }
    private Bond applyOverride(Bond bond){return overrides==null?bond:overrides.apply(bond);}
    public Bond resolve(String key){Bond bond=catalog().stream().filter(b->b.instrumentKey().equals(key)).findFirst().map(this::applyOverride).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Bond listing not found"));if(!bond.available())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Bond listing is not available.");return bond;}
    public Bond resolveForAdmin(String key){return catalog().stream().filter(b->b.instrumentKey().equals(key)).findFirst().map(this::applyOverride).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Bond listing not found"));}
    public Detail detail(String key){
        Bond bond=resolve(key);MarketQuote quote=null;String message=null;
        try{quote=provider.getQuote(new MarketInstrument(bond.instrumentKey(),bond.symbol(),bond.name(),bond.exchange(),bond.isin()));
            if(quote==null||quote.getCurrentPrice()==null||quote.getCurrentPrice().signum()<=0){quote=null;message="No reliable traded price is available for this bond.";}
        }catch(RuntimeException failure){message="Price is unavailable from the provider. Reference information remains available.";}
        return new Detail(bond,quote,updatedAt,stale(),message);
    }
    private boolean stale(){return !updatedAt.plusSeconds(86400).isAfter(clock.instant());}
}
