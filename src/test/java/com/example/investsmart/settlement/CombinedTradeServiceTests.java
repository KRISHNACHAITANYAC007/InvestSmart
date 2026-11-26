package com.example.investsmart.settlement;

import com.example.investsmart.dto.CombinedTradeEvidence.*;
import com.example.investsmart.exception.ResourceNotFoundException;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CombinedTradeServiceTests {
    JournalReportFixture f;CombinedTradeService service;
    @BeforeEach void setup(){
        f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:combined-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);
        service=f.context.getBean(CombinedTradeService.class);
        f.db.execute("CREATE TABLE orders(id BIGINT PRIMARY KEY,user_id BIGINT,stock_id BIGINT,order_type VARCHAR(10),quantity INT,price DECIMAL(19,2),total_amount DECIMAL(19,2),status VARCHAR(20))");
        f.db.execute("CREATE TABLE option_orders(id VARCHAR(36) PRIMARY KEY,user_id BIGINT,instrument_key VARCHAR(100),expiry DATE,side VARCHAR(10),units INT,lot_size INT,fill_price DECIMAL(19,2),status VARCHAR(20),order_kind VARCHAR(20))");
    }
    @AfterEach void close(){f.close();}
    void ticket(String id,long user,boolean equity){
        var at=Timestamp.from(JournalReportFixture.AT);
        f.db.update("""
            INSERT INTO paper_tickets(id,user_id,request_id,stock_id,instrument_key,expiry,side,product,variety,order_kind,validity,quantity,
            filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fees,policy_version,expires_at,created_at)
            VALUES(?,?,?,?,?,'2026-12-31','BUY','CASH','REGULAR','MARKET','DAY',2,2,1,1,2,10,0,'FILLED',0,'test',?,?)
            """,id,user,id,equity?1L:null,"contract",at,at);
    }
    void fill(String id,String ticket,long user,String link){
        f.db.update("INSERT INTO paper_fills VALUES(?,?,?,0,2,10,0,?,?)",id,ticket,user,link,Timestamp.from(JournalReportFixture.AT));
    }
    void equity(long id,long user){f.db.update("INSERT INTO orders VALUES(?,?,1,'BUY',2,10,20,'COMPLETED')",id,user);}
    @Test void combinesBothNamespacesAndRetainsLegacyOnlyWithoutFinancialWrites(){
        ticket("eq",1,true);ticket("op",1,false);fill("ef","eq",1,"100");fill("of","op",1,"100");equity(100,1);equity(101,1);
        f.db.update("INSERT INTO option_orders VALUES('100',1,'contract','2026-12-31','BUY',2,2,10,'FILLED','MARKET')");
        var before=f.db.queryForList("SELECT * FROM users ORDER BY id");
        var result=service.read(f.owner.getEmail());
        assertEquals(Status.COMBINED_SUPPLIED_RECORDS,result.status());assertEquals(3,result.groups().size());
        assertEquals(5,result.groups().stream().mapToInt(g->g.records().size()).sum());
        assertEquals(2,result.groups().stream().filter(g->g.records().size()==2).count());
        assertEquals(before,f.db.queryForList("SELECT * FROM users ORDER BY id"));
        assertEquals(0,f.db.queryForObject("SELECT COUNT(*) FROM settlement_accounts",Integer.class));
    }
    @Test void retainsMissingAndCrossOwnerTicketsAndUnmatchedOrders(){
        ticket("foreign",2,true);fill("orphan","missing",1,"100");fill("cross","foreign",1,"100");equity(100,1);
        var result=service.read(f.owner.getEmail());
        assertEquals(Status.INCOMPLETE,result.status());assertEquals(3,result.groups().size());
        assertTrue(result.groups().stream().allMatch(g->g.records().size()==1));
    }
    @Test void foreignCompetitorBlocksCollapseWithoutLeakingItsIdentity(){
        ticket("owned",1,true);ticket("foreign",2,true);fill("visible","owned",1,"100");fill("private","foreign",2,"100");equity(100,1);
        var result=service.read(f.owner.getEmail());assertEquals(Status.INCOMPLETE,result.status());assertEquals(2,result.groups().size());
        assertFalse(result.toString().contains("private"));
        f.db.update("DELETE FROM paper_tickets WHERE id='foreign'");
        assertEquals(Status.INCOMPLETE,service.read(f.owner.getEmail()).status());
    }
    @Test void conflictingMetadataRetainsBothRecordsAndUnexecutedOrdersStayVisible(){
        ticket("owned",1,true);fill("visible","owned",1,"100");equity(100,1);
        f.db.update("UPDATE orders SET price=11,total_amount=22 WHERE id=100");
        var result=service.read(f.owner.getEmail());assertEquals(Status.INCOMPLETE,result.status());assertEquals(2,result.groups().size());
        f.db.update("UPDATE orders SET status='PENDING' WHERE id=100");
        assertEquals(2,service.read(f.owner.getEmail()).groups().size());
    }
    @Test void excludesForeignOrdersAndRejectsInactiveOrUnknownIdentity(){
        equity(100,2);assertTrue(service.read(f.owner.getEmail()).groups().isEmpty());
        assertThrows(ResourceNotFoundException.class,()->service.read(null));
        assertThrows(ResourceNotFoundException.class,()->service.read("unknown@example.invalid"));
        f.owner.setAccountStatus("SUSPENDED");
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.read(f.owner.getEmail()));
    }
    @Test void exactGlobalLimitRetainsAllAndOverflowWithholdsPartialGroups(){
        f.db.batchUpdate("INSERT INTO orders VALUES(?,1,1,'BUY',2,10,20,'COMPLETED')",new org.springframework.jdbc.core.BatchPreparedStatementSetter(){
            public int getBatchSize(){return 1000;}public void setValues(java.sql.PreparedStatement p,int i)throws java.sql.SQLException{p.setInt(1,i+1);}
        });
        assertEquals(1000,service.read(f.owner.getEmail()).groups().size());
        fill("extra","missing",1,null);var result=service.read(f.owner.getEmail());
        assertEquals(Status.INCOMPLETE,result.status());assertTrue(result.groups().isEmpty());
        assertTrue(result.limitations().contains("OWNED_RECORD_LIMIT_EXCEEDED"));
    }
    @Test void readsOnceWithinIndependentReadOnlyRepeatableSnapshot(){
        ticket("owned",1,true);fill("visible","owned",1,"100");equity(100,1);
        var source=f.context.getBean(javax.sql.DataSource.class);
        var outer=new org.springframework.transaction.support.TransactionTemplate(f.context.getBean(org.springframework.transaction.PlatformTransactionManager.class));
        outer.execute(status->{
            Object outerResource=TransactionSynchronizationManager.getResource(source);
            doAnswer(call->{
                assertNotSame(outerResource,TransactionSynchronizationManager.getResource(source));
                assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
                assertEquals(TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
                return call.callRealMethod();
            }).when(f.combinedRepository).fills(1L);
            assertEquals(1,service.read(f.owner.getEmail()).groups().size());
            assertSame(outerResource,TransactionSynchronizationManager.getResource(source));return null;
        });
        verify(f.combinedRepository,times(1)).fills(1L);verify(f.combinedRepository,times(1)).equities(1L);
        verify(f.combinedRepository,times(1)).options(1L);verify(f.combinedRepository,times(1)).references(Set.of("100"),true);
        verify(f.combinedRepository,times(1)).references(Set.of(),false);
        verifyNoInteractions(f.legacyRepository,f.optionLegacyRepository);

    }
}
