package com.example.investsmart.settlement;

import com.example.investsmart.exception.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChargeEvidenceServiceTests {
    SettlementFundsTests writer;JournalReportFixture f;ChargeEvidenceService service;
    final String reference=ChargeEvidenceReconciler.reference("charge");
    @BeforeEach void setup(){
        writer=new SettlementFundsTests();writer.setup();writer.cash("deposit","20");writer.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);
        writer.run(()->writer.funds.chargeDebt(1,"charge",new BigDecimal("70.00")));
        f=new JournalReportFixture(writer.fixture.source,false);service=f.context.getBean(ChargeEvidenceService.class);
    }
    @AfterEach void close(){f.close();}
    com.example.investsmart.dto.ChargeEvidence read(){return service.read(f.owner.getEmail(),"charge");}
    @Test void ownsParentAndFindsChildrenOutsideRequestedPeriodWithoutMoneyChanges(){
        f.db.update("UPDATE settlement_journal SET occurred_at=DATEADD('DAY',1,occurred_at) WHERE reference_kind='CHARGE'");
        var before=writer.balances();var result=read();assertEquals(ChargeEvidenceReconciler.Status.MATCHED_SUPPLIED_PAIR,result.comparison().status());
        assertEquals(new BigDecimal("20.00"),result.comparison().impliedUnfundedAmount());assertEquals(new BigDecimal("-40.00"),result.comparison().settledEffect());
        assertEquals(2,result.children().size());assertTrue(result.children().stream().allMatch(e->e.journal().occurredAt().isAfter(result.parent().occurredAt())));
        assertEquals(before,writer.balances());assertThrows(UnsupportedOperationException.class,()->result.children().clear());
    }
    @Test void identityAndOwnershipDenialNeverRevealParent(){
        assertThrows(ResourceNotFoundException.class,()->service.read(f.other.getEmail(),"charge"));
        assertThrows(ResourceNotFoundException.class,()->service.read(null,"charge"));
        assertThrows(ResourceNotFoundException.class,()->service.read("missing@example.invalid","charge"));
        assertThrows(BadRequestException.class,()->service.read(f.owner.getEmail(),"x".repeat(161)));
        f.owner.setAccountStatus("SUSPENDED");assertThrows(org.springframework.web.server.ResponseStatusException.class,()->read());
    }
    @Test void missingAndForeignChildrenRemainIncomplete(){
        f.db.update("UPDATE settlement_journal SET user_id=2 WHERE event_key=?","CHARGE_SPEND:"+reference);
        var result=read();assertEquals(1,result.children().size());assertEquals(ChargeEvidenceReconciler.Status.INCOMPLETE,result.comparison().status());
        assertNull(result.comparison().impliedUnfundedAmount());verifyNoInteractions(f.fundingRepository);
        f.db.update("DELETE FROM settlement_journal WHERE reference_kind='CHARGE' AND user_id=1");
        assertTrue(read().children().isEmpty());assertNull(read().comparison().chargeAmount());
    }
    @Test void malformedReferencesAndExtraSiblingsCannotBeSilentlyDropped(){
        f.db.update("UPDATE settlement_journal SET reference_id='wrong' WHERE event_key=?","CHARGE_SPEND:"+reference);
        assertEquals(2,read().children().size());assertEquals(ChargeEvidenceReconciler.Status.MISMATCH,read().comparison().status());
        f.db.update("UPDATE settlement_journal SET reference_id=? WHERE event_key=?",reference,"CHARGE_SPEND:"+reference);
        f.entry(1,"extra","RESERVE","1",JournalReportFixture.AT);
        f.db.update("UPDATE settlement_journal SET reference_kind='CHARGE',reference_id=? WHERE event_key='extra'",reference);
        assertEquals(ChargeEvidenceReconciler.Status.MISMATCH,read().comparison().status());
    }
    @Test void conflictingOrMissingFundingWithholdsNumbers(){
        f.db.update("UPDATE settlement_allocation_events SET amount=0.01 WHERE event_key=?","CHARGE_RESERVE:"+reference);
        assertEquals(ChargeEvidenceReconciler.Status.MISMATCH,read().comparison().status());assertNull(read().comparison().settledEffect());
        f.db.update("DELETE FROM settlement_allocation_events");assertEquals(ChargeEvidenceReconciler.Status.INCOMPLETE,read().comparison().status());
    }
    @Test void childAndFundingSentinelsWithholdResults(){
        var evidence=new SettlementFundingRepository.Evidence("key","RESERVE",new BigDecimal("1.00"),"a","a","CHARGE",reference,"SETTLED",null,null,null,null);
        doReturn(Collections.nCopies(1001,evidence)).when(f.fundingRepository).evidence(eq(1L),anyList());
        var limited=read();assertTrue(limited.fundingTruncated());assertNull(limited.comparison().settledEffect());
        reset(f.fundingRepository);
        f.db.batchUpdate("INSERT INTO settlement_journal VALUES(1,?,'RESERVE',?,1,'CHARGE',?,?)",new org.springframework.jdbc.core.BatchPreparedStatementSetter(){
            public int getBatchSize(){return 1001;}public void setValues(java.sql.PreparedStatement p,int i)throws java.sql.SQLException{p.setString(1,"extra"+i);p.setString(2,"a".repeat(64));p.setString(3,reference);p.setTimestamp(4,java.sql.Timestamp.from(JournalReportFixture.AT));}
        });
        var children=read();assertTrue(children.childrenTruncated());assertEquals(1000,children.children().size());assertNull(children.comparison().settledEffect());
        verifyNoInteractions(f.fundingRepository);
    }
    @Test void allQueriesShareIndependentReadOnlySnapshotWithFixedCalls(){
        var source=f.context.getBean(javax.sql.DataSource.class);Object[] resource={null};
        var before=f.db.queryForList("SELECT * FROM settlement_journal ORDER BY event_key");
        f.write.execute(status->{
            var outer=TransactionSynchronizationManager.getResource(source);
            doAnswer(call->{resource[0]=TransactionSynchronizationManager.getResource(source);assertNotSame(outer,resource[0]);assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());assertEquals(TransactionDefinition.ISOLATION_REPEATABLE_READ,TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());return call.callRealMethod();}).when(f.chargeRepository).parent(1,"charge");
            doAnswer(call->{assertSame(resource[0],TransactionSynchronizationManager.getResource(source));return call.callRealMethod();}).when(f.chargeRepository).children(1,reference);
            doAnswer(call->{assertSame(resource[0],TransactionSynchronizationManager.getResource(source));return call.callRealMethod();}).when(f.fundingRepository).evidence(eq(1L),anyList());
            read();assertSame(outer,TransactionSynchronizationManager.getResource(source));return null;
        });
        verify(f.chargeRepository,times(1)).parent(1,"charge");verify(f.chargeRepository,times(1)).children(1,reference);verify(f.fundingRepository,times(1)).evidence(eq(1L),anyList());
        assertEquals(before,f.db.queryForList("SELECT * FROM settlement_journal ORDER BY event_key"));
    }
}
