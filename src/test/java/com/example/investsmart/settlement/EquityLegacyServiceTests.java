package com.example.investsmart.settlement;

import com.example.investsmart.exception.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.example.investsmart.dto.EquityLegacyEvidence.Verification.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EquityLegacyServiceTests {
    JournalReportFixture f;EquityLegacyService service;
    @BeforeEach void setup(){
        f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:legacy-report-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);
        service=f.context.getBean(EquityLegacyService.class);
        f.db.execute("CREATE TABLE orders(id BIGINT PRIMARY KEY,user_id BIGINT,stock_id BIGINT,order_type VARCHAR(10),quantity INT,price DECIMAL(19,2),total_amount DECIMAL(19,2),status VARCHAR(20))");
        f.db.update("INSERT INTO orders VALUES(100,1,1,'BUY',2,10,20,'COMPLETED')");
        var at=Timestamp.from(JournalReportFixture.AT);
        f.db.update("""
                INSERT INTO paper_tickets(id,user_id,request_id,stock_id,side,product,variety,order_kind,validity,quantity,
                filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fees,policy_version,expires_at,created_at)
                VALUES('ticket',1,'request',1,'BUY','CASH','REGULAR','MARKET','DAY',2,2,1,1,1,10,0,'FILLED',0.50,'test',?,?)
                """,at,at);
        f.db.update("INSERT INTO paper_fills VALUES('fill','ticket',1,0,2,10,0.50,'100',?)",at);
    }
    @AfterEach void close(){f.close();}
    @Test void identifiesOneTradeInTwoRecordsWithoutChangingAnything(){
        var before=f.db.queryForList("SELECT * FROM orders");
        var result=service.read("OWNER@example.invalid","fill");
        assertEquals(MATCHED_DUPLICATE,result.verification());assertEquals(100L,result.duplicateOrderId());
        assertEquals(before,f.db.queryForList("SELECT * FROM orders"));
        assertEquals(1,f.db.queryForObject("SELECT COUNT(*) FROM paper_fills",Integer.class));
        assertEquals(0,f.db.queryForObject("SELECT COUNT(*) FROM settlement_accounts",Integer.class));
    }
    @Test void missingForeignAndInactiveUsersDoNotRevealLegacyIdentity(){
        assertThrows(ResourceNotFoundException.class,()->service.read(f.other.getEmail(),"fill"));
        assertThrows(BadRequestException.class,()->service.read(f.owner.getEmail(),"x".repeat(37)));
        assertThrows(ResourceNotFoundException.class,()->service.read(null,"fill"));
        f.db.update("UPDATE orders SET user_id=2");
        var result=service.read(f.owner.getEmail(),"fill");assertEquals(INCOMPLETE,result.verification());assertNull(result.duplicateOrderId());
        f.owner.setAccountStatus("SUSPENDED");assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.read(f.owner.getEmail(),"fill"));
    }
    @Test void stockDirectionQuantityPriceValueAndStatusMustAgree(){
        for(String change:List.of("stock_id=2","order_type='SELL'","quantity=3","price=11","total_amount=21","status='PENDING'")){
            f.db.update("UPDATE orders SET "+change);
            var result=service.read(f.owner.getEmail(),"fill");assertEquals(MISMATCH,result.verification());assertNull(result.duplicateOrderId());
            f.db.update("UPDATE orders SET stock_id=1,order_type='BUY',quantity=2,price=10,total_amount=20,status='COMPLETED'");
        }
    }
    @Test void namespacesAndMalformedLinksAreNotGuessed(){
        f.db.update("UPDATE paper_tickets SET stock_id=NULL");
        assertEquals(NOT_EVALUATED,service.read(f.owner.getEmail(),"fill").verification());
        f.db.update("UPDATE paper_tickets SET stock_id=1,product='MIS'");
        assertEquals(NOT_EVALUATED,service.read(f.owner.getEmail(),"fill").verification());
        f.db.update("UPDATE paper_tickets SET product='CASH'");
        for(String link:List.of("0100","9999999999999999999","not-numeric")){
            f.db.update("UPDATE paper_fills SET legacy_order_id=?",link);
            assertEquals(MISMATCH,service.read(f.owner.getEmail(),"fill").verification());
        }
        f.db.update("UPDATE paper_fills SET legacy_order_id=NULL");
        assertEquals(INCOMPLETE,service.read(f.owner.getEmail(),"fill").verification());
    }
    @Test void ambiguousReferencesWithholdIdentityAndReadsOwnSnapshot(){
        doAnswer(call->{assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            return call.callRealMethod();}).when(f.legacyRepository).equityReferences("100");
        f.write.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
        f.write.executeWithoutResult(status->{assertEquals(MATCHED_DUPLICATE,service.read(f.owner.getEmail(),"fill").verification());
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());});
        f.db.update("INSERT INTO paper_fills VALUES('duplicate','ticket',1,1,2,10,0.50,'100',?)",Timestamp.from(JournalReportFixture.AT));
        var result=service.read(f.owner.getEmail(),"fill");assertEquals(MISMATCH,result.verification());assertNull(result.duplicateOrderId());
    }
}
