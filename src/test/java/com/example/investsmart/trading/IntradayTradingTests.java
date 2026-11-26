package com.example.investsmart.trading;

import com.example.investsmart.entity.*;
import com.example.investsmart.repository.*;
import com.example.investsmart.service.*;
import com.example.investsmart.market.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest @ActiveProfiles("test") @org.springframework.context.annotation.Import(IntradayTradingTests.TimeConfig.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(print=org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
class IntradayTradingTests {
    @Autowired com.example.investsmart.settlement.IntradayEvidenceReconciler evidenceReconciler;
    @Autowired com.example.investsmart.settlement.IntradayEvidenceService ownedEvidence;
    @Autowired com.example.investsmart.settlement.IntradayFeeService linkedFeeEvidence;
    @Test void actualShortOpenAndBuybackReconcileFeesWithoutCountingTurnoverAsProfit() {
        String id=open("SELL");quote("90");submit(ticket("BUY","MARKET",10,null,null,id));
        var result=linkedFeeEvidence.read(user.getEmail(),id);
        assertEquals(com.example.investsmart.dto.IntradayFeeEvidence.Verification.MATCHED_EVENT_FEES,result.verification());
        assertEquals(new BigDecimal("100.00"),result.positionComparison().verifiedGrossRealizedPnl());
        assertEquals(new BigDecimal("0.57"),result.reconciledEventFees());
    }
    @Test void actualPartialLossAndFinalProfitEventsReconcileWithoutAddingFeesToGrossPnl() {
        String id=open("BUY");quote("90");submit(ticket("SELL","MARKET",4,null,null,id));
        quote("110");submit(ticket("SELL","MARKET",6,null,null,id));
        var saved=position(id);
        var events=db.query("SELECT event_key,event_type,units,collateral_delta,realized_pnl,fee FROM paper_intraday_events WHERE user_id=? AND position_id=?",
                (rs,row)->new com.example.investsmart.settlement.IntradayEvidenceReconciler.Event(rs.getString("event_key"),
                        rs.getString("event_type"),rs.getInt("units"),rs.getBigDecimal("collateral_delta"),
                        rs.getBigDecimal("realized_pnl"),rs.getBigDecimal("fee")),user.getId(),id);
        var balance=users.findById(user.getId()).orElseThrow().getVirtualBalance();
        var result=evidenceReconciler.reconcile(new com.example.investsmart.settlement.IntradayEvidenceReconciler.Position(
                ((Number)saved.get("quantity")).intValue(),(BigDecimal)saved.get("collateral"),(BigDecimal)saved.get("realized_pnl")),events,false);
        assertEquals(com.example.investsmart.settlement.IntradayEvidenceReconciler.Verification.MATCHED_POSITION_TOTALS,result.verification());
        assertEquals(new BigDecimal("20.00"),result.verifiedGrossRealizedPnl());
        assertEquals(new BigDecimal("0.61"),result.eventFeeSum());
        assertEquals(result,ownedEvidence.read(user.getEmail(),id).comparison());
        var linkedFees=linkedFeeEvidence.read(user.getEmail(),id);
        assertEquals(com.example.investsmart.dto.IntradayFeeEvidence.Verification.MATCHED_EVENT_FEES,linkedFees.verification());
        assertEquals(new BigDecimal("0.61"),linkedFees.reconciledEventFees());
        assertEquals(balance,users.findById(user.getId()).orElseThrow().getVirtualBalance());
        assertEquals(saved,position(id));
    }
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;@Autowired com.example.investsmart.security.SecurityAccounts accounts;@Autowired tools.jackson.databind.ObjectMapper json;
    static class TestClock extends Clock {volatile Instant at=Instant.parse("2026-11-12T04:30:00Z");public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return Clock.fixed(at,z);}public Instant instant(){return at;}}
    @org.springframework.boot.test.context.TestConfiguration static class TimeConfig {@org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary TestClock intradayClock(){return new TestClock();}}
    @Autowired TestClock clock;@Autowired PaperTrading paper;@Autowired IntradayTrading intraday;@Autowired UserRepository users;@Autowired StockRepository stocks;@Autowired HoldingRepository holdings;@Autowired JdbcTemplate db;@Autowired PortfolioService portfolio;
    @MockitoBean MarketDataService market;@MockitoBean NseMarketSessionService sessions;
    User user;Stock stock;
    @BeforeEach void setup(){clock.at=Instant.parse("2026-11-12T04:30:00Z");user=new User();user.setEmail(UUID.randomUUID()+"@example.invalid");user.setRole("USER");user.setEmailVerified(true);user.setVirtualBalance(new BigDecimal("10000"));user=users.saveAndFlush(user);stock=new Stock();stock.setSymbol(UUID.randomUUID().toString());stock.setInstrumentKey("NSE_EQ|"+UUID.randomUUID());stock.setExchange("NSE");stock.setCurrentPrice(new BigDecimal("100"));stock=stocks.saveAndFlush(stock);quote("100");}
    @AfterEach void cleanup(){com.example.investsmart.settlement.SettlementTestCleanup.account(db,user.getId());for(String table:List.of("auth_sessions","paper_intraday_events","paper_intraday_positions","paper_fills","paper_tickets","orders","holdings"))db.update("DELETE FROM "+table+" WHERE user_id=?",user.getId());users.deleteById(user.getId());stocks.deleteById(stock.getId());}
    void quote(String price){when(market.getQuote(any(MarketInstrument.class))).thenAnswer(call->new MarketQuote(stock.getSymbol(),new BigDecimal(price),null,clock.at.atZone(com.example.investsmart.settlement.SettlementCalendar.INDIA).toLocalDateTime()).withInstrument(new MarketInstrument(stock.getInstrumentKey(),stock.getSymbol(),"Fixture","NSE")));}
    PaperTrading.Ticket ticket(String side,String kind,int qty,String limit,String trigger,String position){return new PaperTrading.Ticket(stock.getId(),null,null,qty,side,"MIS","REGULAR",kind,"DAY",limit==null?null:new BigDecimal(limit),trigger==null?null:new BigDecimal(trigger),1,null,null,position,UUID.randomUUID().toString());}
    Map<String,Object> submit(PaperTrading.Ticket t){return paper.submit(user.getEmail(),t);}
    String open(String side){return (String)submit(ticket(side,"MARKET",10,null,null,null)).get("position_id");}
    Map<String,Object> position(String id){return db.queryForMap("SELECT * FROM paper_intraday_positions WHERE id=?",id);}
    void cash(String n){assertEquals(0,users.findById(user.getId()).orElseThrow().getVirtualBalance().compareTo(new BigDecimal(n)));}
    BigDecimal profits(){return db.queryForObject("SELECT COALESCE(SUM(remaining_credit),0) FROM settlement_obligations WHERE user_id=? AND credit_policy='RESTRICTED'",BigDecimal.class,user.getId());}

    @Test void shortNeedsNoDeliveryHoldingAndExcessSquareOffNeverReverses(){
        assertTrue(holdings.findByUserId(user.getId()).isEmpty());
        var entry=ticket("SELL","MARKET",10,null,null,null);
        assertEquals(new BigDecimal("2"),paper.preview(user.getEmail(),entry).get("leverage"));
        String id=(String)submit(entry).get("position_id");
        var invalid=ticket("BUY","MARKET",11,null,null,id);
        assertThrows(RuntimeException.class,()->paper.preview(user.getEmail(),invalid));
        assertThrows(RuntimeException.class,()->submit(invalid));
        cash("9499.70");assertEquals(10,((Number)position(id).get("quantity")).intValue());
        assertEquals(1,intraday.positions(user.getId()).size());assertTrue(holdings.findByUserId(user.getId()).isEmpty());
        submit(ticket("BUY","MARKET",4,null,null,id));
        assertEquals(6,((Number)position(id).get("quantity")).intValue());
        assertThrows(RuntimeException.class,()->submit(ticket("BUY","MARKET",7,null,null,id)));
        submit(ticket("BUY","MARKET",6,null,null,id));
        assertEquals(0,((Number)position(id).get("quantity")).intValue());
        assertEquals(1,intraday.positions(user.getId()).size());
    }

    @Test void longEntryUsesHalfCollateralAndDoesNotCreateDeliveryHolding(){String id=open("BUY");cash("9499.70");assertEquals("LONG",position(id).get("direction"));assertEquals(new BigDecimal("500.00"),position(id).get("collateral"));assertTrue(holdings.findByUserId(user.getId()).isEmpty());assertEquals(0,portfolio.getPortfolio(user.getId()).getTotalValue().compareTo(new BigDecimal("9999.70")));}
    @Test void partialLongLossAndFinalProfitDoNotTurnLossesIntoExtraCollateral(){String id=open("BUY");quote("90");submit(ticket("SELL","MARKET",4,null,null,id));cash("9659.59");assertEquals(new BigDecimal("300.00"),position(id).get("collateral"));quote("110");submit(ticket("SELL","MARKET",6,null,null,id));cash("9959.39");assertEquals(new BigDecimal("60.00"),profits());assertEquals(new BigDecimal("20.00"),position(id).get("realized_pnl"));assertEquals(new BigDecimal("0.00"),position(id).get("collateral"));}
    @Test void shortProfitCannotBeWithdrawnUntilItsReviewedSettlement(){String id=open("SELL");quote("90");submit(ticket("BUY","MARKET",10,null,null,id));var w=new com.example.investsmart.dto.WalletRequest();w.setAmount(new BigDecimal("10000"));assertThrows(com.example.investsmart.exception.BadRequestException.class,()->wallet.withdraw(user.getEmail(),w));cash("9999.43");assertEquals(new BigDecimal("100.00"),profits());}
    @Autowired WalletService wallet;
    @Test void shortTurnoverIsNotSpendableAndProfitIsRestricted(){String id=open("SELL");cash("9499.70");quote("90");submit(ticket("BUY","MARKET",10,null,null,id));cash("9999.43");assertEquals(new BigDecimal("100.00"),profits());assertEquals(0,((Number)position(id).get("quantity")).intValue());assertTrue(holdings.findByUserId(user.getId()).isEmpty());assertEquals(0,portfolio.getPortfolio(user.getId()).getTotalValue().compareTo(new BigDecimal("10099.43")));assertEquals(0,portfolio.getPortfolio(user.getId()).getTotalCash().compareTo(new BigDecimal("10099.43")));}
    @Test void partialShortExitsReleaseProportionalCollateralAndKeepRemainder(){String id=open("SELL");quote("110");submit(ticket("BUY","MARKET",3,null,null,id));cash("9619.60");assertEquals(7,((Number)position(id).get("quantity")).intValue());assertEquals(new BigDecimal("350.00"),position(id).get("collateral"));quote("90");submit(ticket("BUY","MARKET",7,null,null,id));cash("9969.41");assertEquals(new BigDecimal("70.00"),profits());assertEquals(new BigDecimal("40.00"),position(id).get("realized_pnl"));}
    @Test void duplicateRequestCannotOpenOrChargeTwice(){var t=ticket("SELL","MARKET",10,null,null,null);var first=submit(t);assertEquals(first.get("id"),submit(t).get("id"));cash("9499.70");assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM paper_intraday_events WHERE user_id=?",Integer.class,user.getId()));}
    @Test void pendingEntryCancellationRefundsOnceAndInvalidReplacementRollsBack(){String id=(String)submit(ticket("BUY","LIMIT",10,"90",null,null)).get("id");cash("9549.73");assertThrows(RuntimeException.class,()->paper.edit(user.getEmail(),id,ticket("BUY","MARKET",0,null,null,null)));cash("9549.73");paper.cancel(user.getEmail(),id);paper.cancel(user.getEmail(),id);cash("10000");}
    @Test void shortClosingStopLimitRetainsTriggerAcrossGap(){String position=open("SELL");String id=(String)submit(ticket("BUY","STOP_LIMIT",10,"110","105",position)).get("id");quote("115");paper.process(id);assertEquals("TRIGGERED",db.queryForObject("SELECT status FROM paper_tickets WHERE id=?",String.class,id));assertEquals(10,((Number)position(position).get("quantity")).intValue());quote("109");paper.process(id);paper.process(id);assertEquals(0,((Number)position(position).get("quantity")).intValue());cash("9909.37");}
    @Test void closingReservationsCannotOversellAndDirectionMustMatch(){String id=open("BUY");submit(ticket("SELL","LIMIT",6,"120",null,id));assertThrows(RuntimeException.class,()->submit(ticket("SELL","MARKET",5,null,null,id)));assertThrows(RuntimeException.class,()->submit(ticket("BUY","MARKET",1,null,null,id)));cash("9499.70");}
    @Test void cutoffPersistsClosureRequestAcrossMissingQuotesAndRestartStyleRetry(){String id=open("SELL");clock.at=Instant.parse("2026-11-12T09:40:00Z");intraday.observe(id);when(market.getQuote(any(MarketInstrument.class))).thenThrow(new RuntimeException("synthetic unavailable"));assertThrows(RuntimeException.class,()->intraday.manage(id));assertEquals(true,position(id).get("close_requested"));assertThrows(RuntimeException.class,()->submit(ticket("BUY","MARKET",1,null,null,null)));quote("100");intraday.manage(id);intraday.manage(id);cash("9999.40");assertEquals(0,((Number)position(id).get("quantity")).intValue());}
    @Test void gapLossCanExceedCollateralAndCreateDebtWithoutInventedCash(){user.setVirtualBalance(new BigDecimal("600"));users.saveAndFlush(user);String id=open("SELL");quote("200");paper.requestClose(user.getEmail(),id);intraday.manage(id);cash("-400.90");assertEquals(0,((Number)position(id).get("quantity")).intValue());assertThrows(RuntimeException.class,()->submit(ticket("BUY","MARKET",1,null,null,null)));}
    @Test void expiredEntryNeverBecomesAnOvernightPosition(){String id=(String)submit(ticket("BUY","LIMIT",10,"90",null,null)).get("id");clock.at=Instant.parse("2026-11-13T04:30:00Z");quote("80");paper.process(id);cash("10000");assertTrue(intraday.positions(user.getId()).isEmpty());}
    @Test void unsupportedProductsAndStylesFailBeforeReservation(){var t=new PaperTrading.Ticket(stock.getId(),null,null,10,"BUY","MIS","ICEBERG","MARKET","DAY",null,null,2,null,null,null,UUID.randomUUID().toString());assertThrows(RuntimeException.class,()->submit(t));cash("10000");assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM paper_tickets WHERE user_id=?",Integer.class,user.getId()));}
    @Test void competingEntriesCannotSpendTheSameCollateral()throws Exception{user.setVirtualBalance(new BigDecimal("1000"));users.saveAndFlush(user);var pool=Executors.newFixedThreadPool(2);try{var start=new CountDownLatch(1);Callable<Boolean> call=()->{start.await();try{submit(ticket("SELL","MARKET",10,null,null,null));return true;}catch(RuntimeException rejected){return false;}};var a=pool.submit(call);var b=pool.submit(call);start.countDown();assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));cash("499.70");}finally{pool.shutdownNow();}}
    @Test void quickMarketEntryIsAnActualIdempotentExecution(){var t=new PaperTrading.Ticket(stock.getId(),null,null,10,"BUY","MIS","QUICK","MARKET","IOC",null,null,1,null,null,null,UUID.randomUUID().toString());var result=submit(t);assertEquals("FILLED",result.get("status"));assertEquals(result.get("id"),submit(t).get("id"));cash("9499.70");}
    @Test void iocUnfilledEntryReleasesItsFullReservation(){var t=new PaperTrading.Ticket(stock.getId(),null,null,10,"BUY","MIS","REGULAR","LIMIT","IOC",new BigDecimal("90"),null,1,null,null,null,UUID.randomUUID().toString());assertEquals("CANCELLED",submit(t).get("status"));cash("10000");assertTrue(intraday.positions(user.getId()).isEmpty());}
    @Test void collateralCannotUseReusableUnsettledDeliverySaleCredit(){
        user.setVirtualBalance(BigDecimal.ZERO);users.saveAndFlush(user);var holding=new Holding();holding.setUser(user);holding.setStock(stock);holding.setQuantity(10);holdings.saveAndFlush(holding);
        var order=new com.example.investsmart.dto.OrderRequest();order.setStockId(stock.getId());order.setQuantity(10);legacyOrders().sellStock(user.getEmail(),order);cash("1000");assertThrows(RuntimeException.class,()->open("SELL"));cash("1000");assertTrue(intraday.positions(user.getId()).isEmpty());
    }
    @Autowired OrderService equityOrders;
    OrderService legacyOrders(){return equityOrders;}
    @Test void closedSessionCannotFillAndReservationIsStillReleasedAtCutoff(){String id=(String)submit(ticket("BUY","LIMIT",10,"110",null,null)).get("id");doThrow(new com.example.investsmart.exception.BadRequestException("Closed")).when(sessions).requireOpenSession("NSE");assertThrows(RuntimeException.class,()->paper.process(id));assertEquals("PENDING",db.queryForObject("SELECT status FROM paper_tickets WHERE id=?",String.class,id));clock.at=Instant.parse("2026-11-12T09:40:00Z");paper.process(id);cash("10000");}
    @Test void staleMarkMakesIntradayValuationUnavailableRatherThanUsingOldProfit(){open("SELL");clock.at=clock.at.plusSeconds(61);assertNull(portfolio.getPortfolio(user.getId()).getTotalValue());assertNull(intraday.positions(user.getId()).getFirst().get("unrealized_pnl"));}
    @Test void anotherUserCannotCloseOrCancelOwnedIntradayExposure(){String position=open("SELL");String order=(String)submit(ticket("BUY","LIMIT",5,"90",null,position)).get("id");var other=new User();other.setEmail(UUID.randomUUID()+"@example.invalid");other.setVirtualBalance(new BigDecimal("10000"));other=users.saveAndFlush(other);String email=other.getEmail();try{assertThrows(RuntimeException.class,()->paper.cancel(email,order));assertThrows(RuntimeException.class,()->paper.requestClose(email,position));var t=ticket("BUY","MARKET",1,null,null,position);assertThrows(RuntimeException.class,()->paper.submit(email,t));assertEquals(10,((Number)position(position).get("quantity")).intValue());}finally{com.example.investsmart.settlement.SettlementTestCleanup.account(db,other.getId());users.deleteById(other.getId());}}
    @Test void authenticatedMisPreviewAndSubmissionRequireCsrfAndExposeOwnedPositions()throws Exception{
        String body=json.writeValueAsString(ticket("SELL","MARKET",10,null,null,null));var cookie=new jakarta.servlet.http.Cookie(com.example.investsmart.security.SecurityAccounts.COOKIE,accounts.issue(user,new org.springframework.mock.web.MockHttpServletRequest(),new org.springframework.mock.web.MockHttpServletResponse()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/orders/paper")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/orders/paper").cookie(cookie).contentType("application/json").content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/orders/paper/preview").cookie(cookie).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType("application/json").content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.leverage").value(2));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/orders/paper").cookie(cookie).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType("application/json").content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("FILLED"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/orders/paper").cookie(cookie)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.intradayPositions[0].direction").value("SHORT"));cash("9499.70");
    }
}
