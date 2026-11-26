package com.example.investsmart.options;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class OptionMarketTests {
    HttpServer server;OptionMarket market;String contracts,chain,query;int status=200;
    LocalDate expiry=LocalDate.now(OptionMarket.INDIA).plusDays(7);
    @BeforeEach void setup()throws Exception{
        contracts="{\"status\":\"success\",\"data\":[{\"underlying_key\":\"NSE_INDEX|Nifty 50\",\"segment\":\"NSE_FO\",\"instrument_key\":\"NSE_FO|123\",\"instrument_type\":\"CE\",\"expiry\":\""+expiry+"\",\"lot_size\":50,\"freeze_quantity\":1000,\"strike_price\":24000,\"trading_symbol\":\"NIFTY fixture CE\"}]}";
        chain="{\"status\":\"success\",\"data\":[{\"underlying_key\":\"NSE_INDEX|Nifty 50\",\"expiry\":\""+expiry+"\",\"strike_price\":24000,\"call_options\":{\"instrument_key\":\"NSE_FO|123\",\"market_data\":{\"ltp\":10,\"bid_price\":9,\"ask_price\":11,\"bid_qty\":500,\"ask_qty\":500}}}]}";
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/",exchange->{query=exchange.getRequestURI().getRawQuery();byte[] body=(exchange.getRequestURI().getPath().endsWith("/contract")?contracts:chain).getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,body.length);try(var out=exchange.getResponseBody()){out.write(body);}});server.start();
        market=new OptionMarket("fixture-token","http://127.0.0.1:"+server.getAddress().getPort(),new ObjectMapper(),HttpClient.newHttpClient());
    }
    @AfterEach void stop(){server.stop(0);}
    @Test void derivesLotsExpiryStrikeAndExecutableDepthFromProvider(){var rows=market.chain(expiry);assertEquals(1,rows.size());var q=rows.getFirst();assertEquals(50,q.contract().lotSize());assertEquals("CE",q.contract().type());assertEquals(expiry,q.contract().expiry());assertEquals(0,q.ask().compareTo(new java.math.BigDecimal("11")));assertEquals(500,q.askQuantity());assertTrue(query.contains("expiry_date="+expiry));assertTrue(query.contains("NSE_INDEX%7CNifty+50"));}
    @Test void rejectsForeignUnderlyingAndInvalidLotMetadata(){contracts=contracts.replace("Nifty 50","Nifty Bank");assertTrue(market.contracts().isEmpty());contracts=contracts.replace("Nifty Bank","Nifty 50").replace("\"lot_size\":50","\"lot_size\":0");assertTrue(market.contracts().isEmpty());}
    @Test void neverMatchesAChainToAnotherContractOrStrike(){chain=chain.replace("NSE_FO|123","NSE_FO|999");assertTrue(market.chain(expiry).isEmpty());chain=chain.replace("NSE_FO|999","NSE_FO|123").replace("24000","25000");assertTrue(market.chain(expiry).isEmpty());}
    @Test void missingPricesRemainUnavailableRatherThanZeroOrLtpFallback(){chain=chain.replace("\"bid_price\":9,\"ask_price\":11,","");var q=market.chain(expiry).getFirst();assertNull(q.bid());assertNull(q.ask());assertThrows(ResponseStatusException.class,()->OptionTrading.executable(q,"BUY",50));}
    @Test void httpAndPayloadFailuresUseSafeMessages(){status=401;var error=assertThrows(ResponseStatusException.class,()->market.contracts());assertEquals(503,error.getStatusCode().value());assertFalse(error.getMessage().contains("fixture-token"));status=200;contracts="{\"status\":\"error\",\"data\":[]}";assertThrows(ResponseStatusException.class,()->market.contracts());}
}
