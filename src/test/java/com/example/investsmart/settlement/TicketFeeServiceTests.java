package com.example.investsmart.settlement;

import com.example.investsmart.exception.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.example.investsmart.dto.TicketFeeEvidence.Verification.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketFeeServiceTests {
    JournalReportFixture f;
    TicketFeeService service;
    @BeforeEach void setup() {
        f = new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:ticket-fees-" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", ""), true);
        service = f.context.getBean(TicketFeeService.class);
        f.db.update("""
                INSERT INTO paper_tickets(id,user_id,request_id,side,product,variety,order_kind,validity,
                quantity,filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fees,
                policy_version,expires_at,created_at)
                VALUES('ticket',1,'request','BUY','CNC','REGULAR','MARKET','DAY',2,2,2,2,1,10,0,'FILLED',5,'test',?,?)
                """, Timestamp.from(JournalReportFixture.AT), Timestamp.from(JournalReportFixture.AT));
    }
    @AfterEach void close() { f.close(); }
    void fill(int leg, long user, String amount) {
        f.db.update("INSERT INTO paper_fills(id,ticket_id,user_id,leg_number,quantity,price,fee,created_at) VALUES(?,'ticket',?,?,1,10,?,?)",
                "fill-" + leg, user, leg, new BigDecimal(amount), Timestamp.from(JournalReportFixture.AT));
    }
    @Test void reconcilesFiveOnceRatherThanAddingParentAndChildrenAndDoesNotWrite() {
        fill(0,1,"2"); fill(1,1,"3");
        var tickets = f.db.queryForList("SELECT * FROM paper_tickets");
        var fills = f.db.queryForList("SELECT * FROM paper_fills");
        var result = service.read("OWNER@example.invalid", "ticket");
        assertEquals(MATCHED,result.verification());
        assertEquals(new BigDecimal("5.00"),result.reconciledFee());
        assertEquals(result.recordedFee(),result.detailFee());
        assertEquals(tickets,f.db.queryForList("SELECT * FROM paper_tickets"));
        assertEquals(fills,f.db.queryForList("SELECT * FROM paper_fills"));
        assertEquals(0,f.db.queryForObject("SELECT COUNT(*) FROM settlement_accounts",Integer.class));
    }
    @Test void missingDetailsAndConflictingFeesWithholdVerifiedAmount() {
        fill(0,1,"2");
        assertEquals(INCOMPLETE,service.read(f.owner.getEmail(),"ticket").verification());
        assertNull(service.read(f.owner.getEmail(),"ticket").reconciledFee());
        fill(1,1,"4");
        var mismatch=service.read(f.owner.getEmail(),"ticket");
        assertEquals(MISMATCH,mismatch.verification()); assertEquals("FEE_CONFLICT",mismatch.reason());
        assertNull(mismatch.reconciledFee());
    }
    @Test void rejectsForeignUsersAndNeverReturnsCorruptForeignFillAmount() {
        assertThrows(ResourceNotFoundException.class,()->service.read(f.other.getEmail(),"ticket"));
        assertThrows(ResourceNotFoundException.class,()->service.read(f.owner.getEmail(),"absent"));
        fill(0,1,"2");fill(1,2,"999");
        var result=service.read(f.owner.getEmail(),"ticket");
        assertEquals("OWNERSHIP_CONFLICT",result.reason());assertNull(result.detailFee());assertNull(result.reconciledFee());
        f.owner.setAccountStatus("SUSPENDED");
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.read(f.owner.getEmail(),"ticket"));
    }
    @Test void supportsZeroFeesPartialCancelledTicketsAndLargeExactMoney() {
        f.db.update("UPDATE paper_tickets SET filled_units=1,filled_legs=1,status='CANCELLED',fees=0");
        fill(0,1,"0");assertEquals(MATCHED,service.read(f.owner.getEmail(),"ticket").verification());
        f.db.update("UPDATE paper_tickets SET fees=99999999999999999.99");
        f.db.update("UPDATE paper_fills SET fee=99999999999999999.99");
        assertEquals(new BigDecimal("99999999999999999.99"),service.read(f.owner.getEmail(),"ticket").reconciledFee());
        f.db.update("UPDATE paper_fills SET fee=-1");
        assertEquals("INVALID_DETAIL",service.read(f.owner.getEmail(),"ticket").reason());
    }
    @Test void boundsDetailAtOneThousandAndDoesNotPublishPartialSum() {
        f.db.update("UPDATE paper_tickets SET quantity=1000,filled_units=1000,filled_legs=1000,fees=1000");
        var args=new ArrayList<Object[]>();
        for(int i=0;i<1000;i++) args.add(new Object[]{"fill-"+i,i,Timestamp.from(JournalReportFixture.AT)});
        f.db.batchUpdate("INSERT INTO paper_fills(id,ticket_id,user_id,leg_number,quantity,price,fee,created_at) VALUES(?,'ticket',1,?,1,10,1,?)",args);
        assertEquals(MATCHED,service.read(f.owner.getEmail(),"ticket").verification());
        fill(1000,1,"1");
        var result=service.read(f.owner.getEmail(),"ticket");
        assertEquals(INCOMPLETE,result.verification());assertEquals("DETAIL_LIMIT",result.reason());assertNull(result.detailFee());assertNull(result.reconciledFee());
    }
    @Test void noFillsAreZeroOnlyWhenTicketAlsoReportsNoFillsAndNoFees() {
        f.db.update("UPDATE paper_tickets SET filled_units=0,filled_legs=0,fees=0,status='PENDING'");
        assertEquals(new BigDecimal("0.00"),service.read(f.owner.getEmail(),"ticket").reconciledFee());
        fill(0,1,"0");
        var conflict=service.read(f.owner.getEmail(),"ticket");
        assertEquals(MISMATCH,conflict.verification());assertEquals("COVERAGE_CONFLICT",conflict.reason());
        assertNull(conflict.reconciledFee());
    }
    @Test void validatesIdentityAndTicketBeforeQueryAndOwnsSnapshot() {
        clearInvocations(f.users,f.feeRepository);
        for(String id:new String[]{null," ","x".repeat(37)})assertThrows(BadRequestException.class,()->service.read(f.owner.getEmail(),id));
        verifyNoInteractions(f.users,f.feeRepository);
        for(String email:new String[]{null," ","unknown@example.invalid"})assertThrows(ResourceNotFoundException.class,()->service.read(email,"ticket"));
        verifyNoInteractions(f.feeRepository);
        doAnswer(call->{
            assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            return call.callRealMethod();
        }).when(f.feeRepository).owned(1,"ticket");
        f.write.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
        f.write.executeWithoutResult(status->{service.read(f.owner.getEmail(),"ticket");
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());});
    }
}
