package com.example.investsmart.service;

import com.example.investsmart.entity.*;
import com.example.investsmart.repository.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WatchlistListingTests {
    @Test void choosingBseReplacesTheCompanyRowWithoutChangingStockRecords() {
        var rows = mock(WatchlistRepository.class); var users = mock(UserRepository.class); var stocks = mock(StockRepository.class);
        var user = new User(); user.setId(1L);
        var nse = new Stock(); nse.setId(2L); nse.setInstrumentKey("NSE_EQ|INE009A01021");
        var bse = new Stock(); bse.setId(3L); bse.setInstrumentKey("BSE_EQ|500209"); bse.setIsin("INE009A01021");
        var row = new Watchlist(); row.setId(7L); row.setUser(user); row.setStock(nse);
        when(users.findByEmailForUpdate("test@example.com")).thenReturn(Optional.of(user));
        when(stocks.findById(3L)).thenReturn(Optional.of(bse)); when(rows.findByUserId(1L)).thenReturn(List.of(row)); when(rows.save(row)).thenReturn(row);
        var saved = new WatchlistService(rows,users,stocks).addToWatchlist("test@example.com",3L);
        assertEquals(7L,saved.getId()); assertSame(bse,saved.getStock()); assertEquals(2L,nse.getId());
        verify(stocks,never()).save(any()); verify(rows).save(row);
    }
}
