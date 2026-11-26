package com.example.investsmart.chat.agent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.example.investsmart.chat.agent.PortfolioAnalysisService.Status.*;

class PortfolioAnalysisServiceTests {
    final PortfolioReadTool tool=mock(PortfolioReadTool.class);
    final PortfolioAnalysisService service=new PortfolioAnalysisService(tool);
    final Instant now=Instant.parse("2026-11-23T05:00:00Z");
    PortfolioReadTool.Holding holding(String key,String value,String pnl) {
        return new PortfolioReadTool.Holding(key,key,1,now,n(value),n(pnl),null,"AVAILABLE");
    }
    BigDecimal n(String value){return value==null?null:new BigDecimal(value);}
    PortfolioReadTool.Evidence evidence(String total,PortfolioReadTool.Holding... rows) {
        return new PortfolioReadTool.Evidence(now,"EQUITY_HOLDINGS_ONLY",List.of(rows),n(total),List.of("Educational paper simulation."));
    }
    @Test void summarizesExistingPnlAndEquityWeightsWithoutDailyOrWholeAccountClaims() {
        var result=service.summarize(evidence("100.00",holding("a","20.00","-3.00"),holding("b","80.00","4.00")));
        assertEquals(COMPLETE_EQUITY_VIEW,result.status());assertEquals(n("1.00"),result.equityUnrealizedPnl());
        assertEquals("b",result.largestListings().getFirst().instrumentKey());assertEquals(n("80.00"),result.largestListings().getFirst().equityWeightPercent());
        assertTrue(result.explanation().stream().anyMatch(s->s.contains("not today's change")));
        assertThrows(UnsupportedOperationException.class,()->result.explanation().clear());
    }
    @Test void duplicateListingRowsAreCombinedAndEqualValuesHaveStableOrder() {
        var result=service.summarize(evidence("100",holding("b","25","1"),holding("a","50","-2"),holding("b","25","1")));
        assertEquals(List.of("a","b"),result.largestListings().stream().map(r->r.instrumentKey()).toList());
        assertEquals(n("50.00"),result.largestListings().getFirst().equityWeightPercent());assertEquals(n("0.00"),result.equityUnrealizedPnl());
    }
    @Test void missingCostDoesNotInventZeroPnlButCanShowValuation() {
        var result=service.summarize(evidence("100",holding("a","100",null)));
        assertEquals(INCOMPLETE_COST_HISTORY,result.status());assertNull(result.equityUnrealizedPnl());assertEquals(1,result.largestListings().size());
    }
    @Test void missingStaleOrInconsistentEvidenceWithholdsDerivedNumbers() {
        var stale=new PortfolioReadTool.Holding("a","a",1,now.minusSeconds(61),n("100"),n("1"),null,"AVAILABLE");
        for(var input:List.of(evidence(null,holding("a",null,null)),evidence("100",stale),evidence("99",holding("a","100","1")))) {
            var r=service.summarize(input);assertEquals(INCOMPLETE_VALUATION,r.status());assertNull(r.equityValue());assertNull(r.equityUnrealizedPnl());assertTrue(r.largestListings().isEmpty());
        }
    }
    @Test void emptyViewIsScopedAndRankingIsBounded() {
        var empty=service.summarize(evidence("0.00"));assertEquals(EMPTY_EQUITY_VIEW,empty.status());
        var ranked=service.summarize(evidence("4",holding("d","1","0"),holding("a","1","0"),holding("c","1","0"),holding("b","1","0")));
        assertEquals(List.of("a","b","c"),ranked.largestListings().stream().map(r->r.instrumentKey()).toList());
    }
    @Test void facadeDelegatesIdentityConsentAndEmptyArgumentsToAuthorizedTool() {
        java.security.Principal principal=()->"owner@example.invalid";
        when(tool.invoke(principal,Map.of(),true)).thenReturn(evidence("0.00"));
        assertEquals(EMPTY_EQUITY_VIEW,service.analyze(principal,true).status());verify(tool).invoke(principal,Map.of(),true);
        doThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN)).when(tool).invoke(principal,Map.of(),false);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.analyze(principal,false));
    }
}
