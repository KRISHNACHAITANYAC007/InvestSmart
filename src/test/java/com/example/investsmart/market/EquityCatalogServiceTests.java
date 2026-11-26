package com.example.investsmart.market;

import com.sun.net.httpserver.HttpServer;
import com.example.investsmart.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class EquityCatalogServiceTests {
    @Test void resolvesBseSeriesAndKeepsCompanyListingsTogether() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost",0),0);
        for (String exchange : new String[]{"NSE","BSE"}) {
            String key = exchange.equals("NSE") ? "NSE_EQ|INE009A01021" : "BSE_EQ|500209";
            String json = "[{\"instrument_key\":\""+key+"\",\"segment\":\""+exchange+"_EQ\",\"instrument_type\":\""+(exchange.equals("NSE")?"EQ":"A")+"\",\"isin\":\"INE009A01021\",\"trading_symbol\":\"INFY\",\"name\":\"Infosys\"}]";
            var bytes = new ByteArrayOutputStream(); try(var gzip = new GZIPOutputStream(bytes)) { gzip.write(json.getBytes(StandardCharsets.UTF_8)); }
            byte[] body = bytes.toByteArray();
            server.createContext("/"+exchange+".json.gz",request->{request.sendResponseHeaders(200,body.length);request.getResponseBody().write(body);request.close();});
        }
        server.start();
        try {
            var catalog = new EquityCatalogService(HttpClient.newHttpClient(),"http://localhost:"+server.getAddress().getPort()+"/");
            var search = catalog.search("Infosys",1);
            assertEquals(2,search.getInstruments().size()); assertEquals(1,search.getTotalPages());
            assertEquals("BSE",catalog.resolve("BSE_EQ|500209").getExchange());
            assertEquals("INE009A01021",catalog.resolve("BSE_EQ|500209").getIsin());
            assertEquals(2,catalog.listings("BSE_EQ|500209","INFY").size());
            assertEquals("NSE",catalog.listings(null,"INFY").getFirst().getExchange());
            assertThrows(BadRequestException.class,()->catalog.resolve("BSE_EQ|fake"));
            assertThrows(BadRequestException.class,()->catalog.search("I",1));
        } finally { server.stop(0); }
    }
}
