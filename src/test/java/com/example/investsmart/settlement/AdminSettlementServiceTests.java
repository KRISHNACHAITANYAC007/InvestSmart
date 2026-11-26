package com.example.investsmart.settlement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.investsmart.entity.User;
import com.example.investsmart.security.SecurityAccounts;
import com.example.investsmart.security.SecurityEvents;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class AdminSettlementServiceTests {
    SettlementFundsTests fixture;
    AdminSettlementService service;
    SecurityAccounts accounts;
    SecurityEvents events;
    MockHttpServletRequest request = new MockHttpServletRequest();
    String csv = "date,trading_open,settlement_open,note\n2026-11-11,true,true,synthetic\n2026-11-12,true,false,synthetic closure\n2026-11-13,true,true,synthetic\n";
    @BeforeEach void setup() {
        fixture=new SettlementFundsTests();fixture.setup();accounts=mock(SecurityAccounts.class);events=mock(SecurityEvents.class);
        var actor=new User();actor.setId(99L);actor.setRole("ADMIN");
        lenient().when(accounts.user("owner@example.invalid")).thenReturn(actor);lenient().when(accounts.isOwner(99L)).thenReturn(true);
        service=new AdminSettlementService(fixture.db,fixture.calendar,accounts,events);
    }
    SettlementCalendar.Coverage activate(boolean reviewed) {
        return fixture.get(()->service.activate("owner@example.invalid","NSE","EQUITY","https://example.org/reviewed.csv",csv,"Reviewed synthetic closure",reviewed,request));
    }
    @Test void activationUsesReviewedWorkingDaysAndPersistsBothAudits() {
        var result=activate(true);assertEquals(result.versionId(),fixture.calendar.next("NSE","EQUITY",SettlementFundsTests.TRADE).versionId());
        assertEquals(SettlementFundsTests.TRADE.plusDays(2),fixture.calendar.next("NSE","EQUITY",SettlementFundsTests.TRADE).expectedDate());
        assertEquals(64,result.checksum().length());assertEquals(1,service.coverage().size());
        verify(accounts).requireRecent(request);verify(events).audit(eq(99L),isNull(),eq("SETTLEMENT_CALENDAR_ACTIVATE"),contains(result.versionId()));
        verify(events).record(eq("ADMIN_ACTION"),eq(99L),anyString(),eq(request),eq("SETTLEMENT_CALENDAR_ACTIVATE"));
    }
    @Test void nonOwnerAndMissingRecentConfirmationCannotActivate() {
        when(accounts.isOwner(99L)).thenReturn(false);assertEquals(403,assertThrows(ResponseStatusException.class,()->activate(true)).getStatusCode().value());
        when(accounts.isOwner(99L)).thenReturn(true);doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.PRECONDITION_REQUIRED)).when(accounts).requireRecent(request);
        assertEquals(428,assertThrows(ResponseStatusException.class,()->activate(true)).getStatusCode().value());verifyNoInteractions(events);
    }
    @Test void missingReviewAndInvalidCsvCannotChangeActiveVersion() {
        var original=service.coverage();assertThrows(ResponseStatusException.class,()->activate(false));
        csv="date,trading_open,settlement_open,note\n2026-11-11,true,true,a\n2026-11-13,true,true,b";
        assertEquals(400,assertThrows(ResponseStatusException.class,()->activate(true)).getStatusCode().value());
        assertEquals(original,service.coverage());verifyNoInteractions(events);
    }
    @Test void auditFailureRollsBackActivation() {
        var original=service.coverage();doThrow(new IllegalStateException()).when(events).audit(any(),any(),any(),any());
        assertThrows(IllegalStateException.class,()->activate(true));assertEquals(original,service.coverage());
    }
}
