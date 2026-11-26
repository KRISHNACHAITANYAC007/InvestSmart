package com.example.investsmart.settlement;

import com.example.investsmart.exception.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.example.investsmart.dto.OptionLegacyEvidence.Verification.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OptionLegacyServiceTests {
    JournalReportFixture f;OptionLegacyService service;
    @BeforeEach void setup(){
        f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:option-overlap-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);
        service=f.context.getBean(OptionLegacyService.class);
        f.db.execute("CREATE TABLE option_orders(id VARCHAR(36) PRIMARY KEY,user_id BIGINT,instrument_key VARCHAR(100),expiry DATE,side VARCHAR(4),units INT,lot_size INT,fill_price DECIMAL(19,2),status VARCHAR(16),order_kind VARCHAR(20))");
        // A numeric-looking ID still belongs to the option namespace; no equity orders table exists here.
        f.db.update("INSERT INTO option_orders VALUES('100',1,'OPTION',?,'BUY',100,50,10,'FILLED','MARKET')",java.sql.Date.valueOf("2026-11-26"));
        var at=Timestamp.from(JournalReportFixture.AT);
        f.db.update("""
                INSERT INTO paper_tickets(id,user_id,request_id,instrument_key,expiry,side,product,variety,order_kind,validity,quantity,
                filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fees,policy_version,expires_at,created_at)
                VALUES('ticket',1,'request','OPTION',?,'BUY','CASH','ICEBERG','MARKET','DAY',100,100,1,1,50,10,0,'FILLED',0.30,'test',?,?)
                """,java.sql.Date.valueOf("2026-11-26"),at,at);
        f.db.update("INSERT INTO paper_fills VALUES('fill','ticket',1,0,100,10,0.30,'100',?)",at);
    }
    @AfterEach void close(){f.close();}
    @Test void matchingOptionLinkIsOneTradeAndDoesNotWrite(){
        var before=f.db.queryForList("SELECT * FROM option_orders");
        var result=service.read("OWNER@example.invalid","fill");
        assertEquals(MATCHED_DUPLICATE,result.verification());assertEquals("100",result.duplicateOptionOrderId());
        assertEquals(before,f.db.queryForList("SELECT * FROM option_orders"));
        assertEquals(1,f.db.queryForObject("SELECT COUNT(*) FROM paper_fills",Integer.class));
        assertEquals(0,f.db.queryForObject("SELECT COUNT(*) FROM settlement_accounts",Integer.class));
    }
    @Test void foreignMissingAndInactiveEvidenceDoesNotExposeIdentity(){
        assertThrows(ResourceNotFoundException.class,()->service.read(f.other.getEmail(),"fill"));
        assertThrows(BadRequestException.class,()->service.read(f.owner.getEmail(),"x".repeat(37)));
        assertThrows(ResourceNotFoundException.class,()->service.read(null,"fill"));
        f.db.update("UPDATE option_orders SET user_id=2");
        var result=service.read(f.owner.getEmail(),"fill");assertEquals(INCOMPLETE,result.verification());assertNull(result.duplicateOptionOrderId());
        f.owner.setAccountStatus("SUSPENDED");assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.read(f.owner.getEmail(),"fill"));
    }
    @Test void contractExpiryLotUnitsDirectionPriceAndStatusMustAgree(){
        for(String change:List.of("instrument_key='OTHER'","expiry='2026-11-27'","lot_size=25","units=51","side='SELL'","fill_price=11","status='PENDING'","order_kind='EXPIRY_CLOSE'")){
            f.db.update("UPDATE option_orders SET "+change);
            var result=service.read(f.owner.getEmail(),"fill");assertEquals(MISMATCH,result.verification());assertNull(result.duplicateOptionOrderId());
            f.db.update("UPDATE option_orders SET instrument_key='OPTION',expiry='2026-11-26',lot_size=50,units=100,side='BUY',fill_price=10,status='FILLED',order_kind='MARKET'");
        }
        f.db.update("UPDATE paper_fills SET quantity=51");assertEquals(MISMATCH,service.read(f.owner.getEmail(),"fill").verification());
    }
    @Test void equityAndUnsupportedProductsAreNotRoutedToOptions(){
        f.db.update("UPDATE paper_tickets SET stock_id=1");assertEquals(NOT_EVALUATED,service.read(f.owner.getEmail(),"fill").verification());
        f.db.update("UPDATE paper_tickets SET stock_id=NULL,product='MIS'");assertEquals(NOT_EVALUATED,service.read(f.owner.getEmail(),"fill").verification());
        f.db.update("UPDATE paper_tickets SET product='CASH'");
        f.db.update("UPDATE paper_fills SET legacy_order_id=?","x".repeat(37));assertEquals(MISMATCH,service.read(f.owner.getEmail(),"fill").verification());
        f.db.update("UPDATE paper_fills SET legacy_order_id=NULL");assertEquals(INCOMPLETE,service.read(f.owner.getEmail(),"fill").verification());
    }
    @Test void ambiguousLinksWithholdIdentityAndUseSeparateReadOnlySnapshot(){
        doAnswer(call->{assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            return call.callRealMethod();}).when(f.optionLegacyRepository).optionReferences("100");
        f.write.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
        f.write.executeWithoutResult(status->{assertEquals(MATCHED_DUPLICATE,service.read(f.owner.getEmail(),"fill").verification());assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());});
        f.db.update("INSERT INTO paper_fills VALUES('duplicate','ticket',1,1,100,10,0.30,'100',?)",Timestamp.from(JournalReportFixture.AT));
        var result=service.read(f.owner.getEmail(),"fill");assertEquals(MISMATCH,result.verification());assertNull(result.duplicateOptionOrderId());
    }
}
