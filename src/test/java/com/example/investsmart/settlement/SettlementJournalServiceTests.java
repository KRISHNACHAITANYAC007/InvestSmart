package com.example.investsmart.settlement;

import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SettlementJournalServiceTests {
    JournalReportFixture f;

    @BeforeEach void setup() {
        f = new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:journal-" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", ""), true);
    }

    @AfterEach void close() { f.close(); }

    @Test void ownsDataAndRetainsPrecisionRawActionsAndNullableReferences() {
        f.account(1); f.account(2);
        f.entry(1,"owned","FUTURE_ACTION","99999999999999999.99",JournalReportFixture.AT);
        f.entry(2,"foreign","WITHDRAW","42.00",JournalReportFixture.AT);
        f.db.update("UPDATE settlement_journal SET reference_kind='SYNTHETIC',reference_id='ref-1' WHERE user_id=1");
        var page = f.service.read("OWNER@example.invalid",null,null);
        assertEquals(0,page.page()); assertEquals(50,page.size());
        assertTrue(page.coverage().initialized()); assertEquals(JournalReportFixture.AT,page.coverage().initializedAt());
        assertEquals(1,page.entries().size());
        var entry=page.entries().getFirst();
        assertEquals("owned",entry.eventKey()); assertEquals("FUTURE_ACTION",entry.action());
        assertEquals(new BigDecimal("99999999999999999.99"),entry.amount());
        assertEquals("SYNTHETIC",entry.referenceKind()); assertEquals("ref-1",entry.referenceId());
        var other=f.service.read(f.other.getEmail(),0,1).entries().getFirst();
        assertEquals("foreign",other.eventKey()); assertNull(other.referenceKind()); assertNull(other.referenceId());
        assertThrows(UnsupportedOperationException.class,()->page.entries().clear());
    }

    @Test void paginationHasDeterministicTiesAndEmptyBoundaries() {
        f.entry(1,"a","RESERVE","1.00",JournalReportFixture.AT);
        f.entry(1,"c","SPEND","2.00",JournalReportFixture.AT);
        f.entry(1,"b","RELEASE","3.00",JournalReportFixture.AT);
        f.entry(1,"old","SETTLED_CREDIT","4.00",JournalReportFixture.AT.minusSeconds(1));
        assertEquals(List.of("c","b"),keys(0,2)); assertEquals(List.of("a","old"),keys(1,2));
        assertTrue(keys(2,2).isEmpty()); assertTrue(keys(100000,100).isEmpty());
    }

    @Test void invalidBoundsRejectBeforeIdentityOrJournalQueries() {
        clearInvocations(f.users,f.repository);
        for (Integer[] pair : new Integer[][]{{-1,50},{100001,50},{0,0},{0,101},{Integer.MAX_VALUE,100},{0,Integer.MIN_VALUE}}) {
            assertThrows(BadRequestException.class,()->f.service.read(f.owner.getEmail(),pair[0],pair[1]));
        }
        verifyNoInteractions(f.users,f.repository);
    }

    @Test void unknownBlankAndInactiveIdentitiesNeverReadJournal() {
        for(String email : new String[]{null," ","unknown@example.invalid"}) {
            assertThrows(ResourceNotFoundException.class,()->f.service.read(email,0,50));
        }
        f.owner.setAccountStatus("SUSPENDED");
        var error=assertThrows(ResponseStatusException.class,()->f.service.read(f.owner.getEmail(),0,50));
        assertEquals(403,error.getStatusCode().value()); verifyNoInteractions(f.repository);
    }

    @Test void readingUninitializedOrOrphanRowsDoesNotInitializeOrWriteAnything() {
        var before=snapshot();
        var empty=f.service.read(f.owner.getEmail(),null,null);
        assertFalse(empty.coverage().initialized()); assertNull(empty.coverage().initializedAt());
        assertTrue(empty.entries().isEmpty()); assertEquals(before,snapshot());
        f.entry(1,"actual-orphan","LINK_DELIVERIES","0.00",JournalReportFixture.AT);
        before=snapshot();
        var existing=f.service.read(f.owner.getEmail(),0,100);
        assertFalse(existing.coverage().initialized()); assertEquals(1,existing.entries().size());
        assertEquals(before,snapshot());
    }

    @Test void initializedReadPreservesCashHoldingsAndFundingRecords() {
        f.account(1); f.entry(1,"reserve","RESERVE","10.00",JournalReportFixture.AT);
        f.db.update("INSERT INTO settlement_allocations VALUES('allocation',1,'ORDER','order','SETTLED',10,10,'RESERVED',?)",java.sql.Timestamp.from(JournalReportFixture.AT));
        var before=snapshot(); f.service.read(f.owner.getEmail(),0,50); assertEquals(before,snapshot());
    }

    @Test void readOwnsReadOnlyRepeatableReadEvenInsideWeakerCallerTransaction() {
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            return call.callRealMethod();
        }).when(f.repository).coverage(anyLong());
        f.write.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
        f.write.executeWithoutResult(status -> {
            f.service.read(f.owner.getEmail(),0,50);
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_READ_COMMITTED,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
        });
    }

    private List<String> keys(int page,int size) {
        return f.service.read(f.owner.getEmail(),page,size).entries().stream().map(e->e.eventKey()).toList();
    }

    private Map<String,List<Map<String,Object>>> snapshot() {
        Map<String,List<Map<String,Object>>> result=new LinkedHashMap<>();
        for(String table:List.of("users","holdings","settlement_accounts","settlement_journal","settlement_obligations","settlement_allocations","settlement_lots","settlement_allocation_events","settlement_lot_events","settlement_dependencies")) {
            result.put(table,f.db.queryForList("SELECT * FROM "+table));
        }
        return result;
    }
}
