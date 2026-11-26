package com.example.investsmart.investment;
import com.example.investsmart.entity.Stock;
import com.example.investsmart.market.*;
import com.example.investsmart.service.StockService;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
class IpoSettlementJobTests {
    @Test void startupCatchupPromotesVerifiedBseOnlyListingOnce(){
        var apps=mock(IpoApplicationRepository.class);var market=mock(IpoMarketService.class);var tx=mock(IpoApplicationTransactions.class);var catalog=mock(EquityCatalogService.class);var stocks=mock(StockService.class);
        var a=new IpoApplication();a.setId(1L);a.setUserId(2L);a.setIpoId("bse-ipo");a.setStatus("PENDING");
        var detail=



                new IpoMarketService.Ipo("bse-ipo","TEST","Test","INE009A01021","listed","sme",BigDecimal.TEN,BigDecimal.TEN,100,200,null,null,null,null,LocalDate.of(2026,11,1),LocalDate.of(2026,11,2),BigDecimal.TEN,BigDecimal.ONE,"BSE",null,null,Instant.parse("2026-11-03T00:00:00Z"),false,true,false);
        when(apps.findByStatusIn(anyList())).thenAnswer(i->"LISTED".equals(a.getStatus())?List.of():List.of(a));when(apps.findById(1L)).thenReturn(Optional.of(a));when(market.detail("bse-ipo",true)).thenReturn(detail);
        doAnswer(i->{a.setStatus("ALLOTTED");return null;}).when(tx).settle(1L,2L,detail);
        var listing=new MarketInstrument("BSE_EQ|12345","TEST","Test","BSE","INE009A01021");when(catalog.listingsByIsin("INE009A01021")).thenReturn(List.of(listing));var stock=new Stock();stock.setId(3L);when(stocks.importNseStock("BSE_EQ|12345","TEST")).thenReturn(stock);
        doAnswer(i->{a.setStatus("LISTED");return null;}).when(tx).promote(1L,2L,3L,detail);
        var job=new IpoSettlementJob(apps,market,tx,catalog,stocks,Clock.fixed(Instant.parse("2026-11-03T00:00:00Z"),ZoneOffset.UTC),true);job.run();job.run();verify(tx,times(1)).settle(1L,2L,detail);verify(tx,times(1)).promote(1L,2L,3L,detail);verify(market,times(1)).detail("bse-ipo",true);
    }
}
