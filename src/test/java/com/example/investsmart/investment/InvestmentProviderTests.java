package com.example.investsmart.investment;
import com.sun.net.httpserver.HttpServer;
import com.example.investsmart.market.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.*;
import java.net.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.atomic.*;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestmentProviderTests {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path referenceDirectory;
    @Test void externalReferencesRequireCurrentDatedHttpsTermsAndRejectDuplicatesOrOlderOverrides() throws Exception {
        var path=referenceDirectory.resolve("terms.csv");String header="isin,coupon,faceValue,maturity,rating,sourceUrl,asOf,verifiedAt\n";
        var clock=Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"),ZoneOffset.UTC);
        String valid="INE906B07CB9,8.30,1000,2027-01-25,,https://example.invalid/reference,2026-11-02,2026-11-03\n";
        java.nio.file.Files.writeString(path,header+valid);
        assertDoesNotThrow(()->new BondMarketService(mock(MarketDataProvider.class),clock,HttpClient.newHttpClient(),"http://localhost/",path.toString()));
        for(String invalid:java.util.List.of(valid+valid,valid.replace("https:","http:"),valid.replace("2026-11-03","2026-11-04"),valid.replace("2026-11-02","2026-09-01"),valid.replace("8.30","101"),valid.replace(",1000,",",-1,"),"INE906B07CB9,,,,,https://example.invalid/reference,2026-11-02,2026-11-03\n")){
            java.nio.file.Files.writeString(path,header+invalid);assertThrows(IllegalStateException.class,()->new BondMarketService(mock(MarketDataProvider.class),clock,HttpClient.newHttpClient(),"http://localhost/",path.toString()));
        }
    }
    static class MutableClock extends Clock { Instant now=Instant.parse("2026-11-03T05:00:00Z");public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return Clock.fixed(now,z);}public Instant instant(){return now;} }
    @Test void ipoFieldsMapAndStaleReadsNeverEnableFreshApply() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("localhost",0),0);var status=new AtomicInteger(200);var calls=new AtomicInteger();
        String body="""
            {"status":"success","data":{"id":"example-ipo","symbol":"TEST","name":"Example IPO","status":"open","issue_type":"sme","lot_size":50,"minimum_quantity":100,"minimum_price":90,"maximum_price":100,"bidding_start_date":"2026-11-03","bidding_end_date":"2026-11-04","daily_start_time":"10:00:00","daily_end_time":"17:00:00","total_subscription":"2.5","timeline":{"allotment_date":"2026-11-05","listing_date":"2026-11-08"},"rhp_url":"javascript:alert(1)"}}
            """;
        server.createContext("/v2/ipos/example-ipo",request->{calls.incrementAndGet();byte[] bytes=body.getBytes(StandardCharsets.UTF_8);request.sendResponseHeaders(status.get(),bytes.length);request.getResponseBody().write(bytes);request.close();});server.start();
        try{var clock=new MutableClock();var service=new IpoMarketService("test-only","http://localhost:"+server.getAddress().getPort(),HttpClient.newHttpClient(),clock);
            var ipo=service.detail("example-ipo",false);assertEquals(100,ipo.minimumQuantity());assertEquals(LocalDate.of(2026,11,5),ipo.allotmentDate());assertEquals(0,ipo.subscription().compareTo(new java.math.BigDecimal("2.5")));assertNull(ipo.rhpUrl());assertFalse(ipo.stale());
            service.detail("example-ipo",false);assertEquals(1,calls.get());clock.now=clock.now.plusSeconds(301);status.set(503);
            assertTrue(service.detail("example-ipo",false).stale());assertThrows(ResponseStatusException.class,()->service.detail("example-ipo",true));assertThrows(ResponseStatusException.class,()->service.detail("../bad",true));
        }finally{server.stop(0);}
    }
    @Test void invalidPayloadIsAnUnavailableErrorRatherThanAnEmptySuccess() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("localhost",0),0);server.createContext("/v2/ipos",r->{byte[] b="{\"status\":\"error\",\"data\":[]}".getBytes(StandardCharsets.UTF_8);r.sendResponseHeaders(200,b.length);r.getResponseBody().write(b);r.close();});server.start();
        try{var service=new IpoMarketService("test","http://localhost:"+server.getAddress().getPort(),HttpClient.newHttpClient(),Clock.systemUTC());assertThrows(ResponseStatusException.class,()->service.list("open","all",1));assertThrows(ResponseStatusException.class,()->service.list("invalid","all",1));}finally{server.stop(0);}
    }
    @Test void debtCatalogIncludesGovernmentTreasuryAndCorporateButNotEquityAndDoesNotInferTerms() throws Exception {
        String json="""
            [{"instrument_key":"NSE_EQ|IN0020250091","isin":"IN0020250091","segment":"NSE_EQ","instrument_type":"GS","name":"6.48 GS 2035","trading_symbol":"648GS2035"},
             {"instrument_key":"NSE_EQ|IN0120250003","isin":"IN0120250003","segment":"NSE_EQ","instrument_type":"TB","name":"91 day bill","trading_symbol":"TB"},
             {"instrument_key":"NSE_EQ|INE906B07CB9","isin":"INE906B07CB9","segment":"NSE_EQ","instrument_type":"N2","name":"NHAI","trading_symbol":"830NHAI27"},
             {"instrument_key":"NSE_EQ|INE009A01021","isin":"INE009A01021","segment":"NSE_EQ","instrument_type":"EQ","name":"Infosys","trading_symbol":"INFY"}]
            """;
        var bytes=new ByteArrayOutputStream();try(var gzip=new GZIPOutputStream(bytes)){gzip.write(json.getBytes(StandardCharsets.UTF_8));}byte[] body=bytes.toByteArray();var server=HttpServer.create(new InetSocketAddress("localhost",0),0);server.createContext("/NSE.json.gz",r->{r.sendResponseHeaders(200,body.length);r.getResponseBody().write(body);r.close();});server.start();
        try{var provider=mock(MarketDataProvider.class);when(provider.getQuote(any(MarketInstrument.class))).thenThrow(new IllegalStateException());var service=new BondMarketService(provider,Clock.systemUTC(),HttpClient.newHttpClient(),"http://localhost:"+server.getAddress().getPort()+"/");
            assertEquals(3,service.list("all","",1).totalItems());assertEquals(2,service.list("government","",1).totalItems());assertEquals(1,service.list("corporate","",1).totalItems());
            assertNull(service.resolve("NSE_EQ|IN0120250003").terms());assertEquals(LocalDate.of(2035,10,6),service.resolve("NSE_EQ|IN0020250091").terms().maturity());assertEquals(2,service.list("all","",1,true).totalItems());assertEquals(3,service.list("all","",1,true).catalogSize());assertEquals(2,service.list("all","",1,true).verifiedTermsCount());var reference=service.resolve("NSE_EQ|INE906B07CB9").terms();assertEquals(LocalDate.of(2027,1,25),reference.maturity());assertNull(reference.rating());assertEquals(0,reference.coupon().compareTo(new java.math.BigDecimal("8.3")));
            var detail=service.detail("NSE_EQ|INE906B07CB9");assertNull(detail.quote());assertNotNull(detail.quoteMessage());assertNotNull(detail.bond().terms());assertThrows(ResponseStatusException.class,()->service.resolve("NSE_EQ|INE009A01021"));
        }finally{server.stop(0);}
    }
}
