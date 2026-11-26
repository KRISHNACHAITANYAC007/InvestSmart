package com.example.investsmart.service;

import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.exception.MarketStatusUnavailableException;
import com.example.investsmart.market.MarketDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class NseMarketSessionServiceTests {
    @Mock private MarketDataService marketDataService;
    @InjectMocks private NseMarketSessionService marketSessionService;

    @Test
    void acceptsOnlyNormalOpenStatus() {
        when(marketDataService.getExchangeStatus("NSE")).thenReturn("NORMAL_OPEN");

        assertDoesNotThrow(() -> { marketSessionService.requireOpenSession(); });
    }

    @Test
    void rejectsClosedPreOpenAndClosingStatuses() {
        for (String status : new String[]{"NORMAL_CLOSE", "PRE_OPEN_START", "PRE_OPEN_END",
                "CLOSING_START", "CLOSING_END", "CTS_CLOSE", "UNKNOWN"}) {
            when(marketDataService.getExchangeStatus("NSE")).thenReturn(status);
            assertThrows(BadRequestException.class, marketSessionService::requireOpenSession, status);
        }
    }

    @Test
    void failsClosedWhenStatusCannotBeRetrievedOrIsNull() {
        when(marketDataService.getExchangeStatus("NSE"))
                .thenThrow(new IllegalStateException("provider unavailable"));
        assertThrows(MarketStatusUnavailableException.class, marketSessionService::requireOpenSession);

        doReturn(null).when(marketDataService).getExchangeStatus("NSE");
        assertThrows(BadRequestException.class, marketSessionService::requireOpenSession);
    }
}
