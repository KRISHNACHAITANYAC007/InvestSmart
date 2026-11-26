package com.example.investsmart.settlement;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PeriodFundingTests {
    JournalReportFixture f;JournalPeriodService service;
    @BeforeEach void setup(){f=new JournalReportFixture(new DriverManagerDataSource("jdbc:h2:mem:period-funding-"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa",""),true);service=f.context.getBean(JournalPeriodService.class);}
    @AfterEach void close(){f.close();}
    com.example.investsmart.dto.JournalPeriodEvidence read(){return service.read(f.owner.getEmail(),JournalReportFixture.AT,JournalReportFixture.AT.plusSeconds(1));}
    void journal(String key){f.entry(1,key,"RESERVE","1",JournalReportFixture.AT);f.db.update("UPDATE settlement_journal SET reference_kind='ORDER',reference_id=? WHERE user_id=1 AND event_key=?",key,key);}
    void source(String key,long owner){f.db.update("INSERT INTO settlement_allocations VALUES(?,?,'ORDER',?,'SETTLED',1,1,'RESERVED',?)","source-"+key,owner,key,Timestamp.from(JournalReportFixture.AT));f.db.update("INSERT INTO settlement_allocation_events VALUES(1,?,?,'RESERVE',1)",key,"source-"+key);}
    @Test void ownedSourceEffectsShareSnapshotAndDoNotChangeMoney(){
        journal("owned");source("owned",1);Object[] resource={null};
        doAnswer(call->{resource[0]=TransactionSynchronizationManager.getResource(f.context.getBean(javax.sql.DataSource.class));return call.callRealMethod();}).when(f.periodRepository).entries(anyLong(),any(),any());
        doAnswer(call->{assertSame(resource[0],TransactionSynchronizationManager.getResource(f.context.getBean(javax.sql.DataSource.class)));assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());return call.callRealMethod();}).when(f.fundingRepository).evidence(anyLong(),anyList(),anyInt());
        var before=f.db.queryForList("SELECT * FROM users ORDER BY id");var result=read();var entry=result.entries().getFirst();
        assertEquals(com.example.investsmart.dto.SettlementFundingPage.Verification.MATCHED,entry.funding().verification());
        assertEquals(new BigDecimal("-1.00"),entry.fundingEffect().settledDelta());assertEquals(before,f.db.queryForList("SELECT * FROM users ORDER BY id"));
        verify(f.fundingRepository,times(1)).evidence(1,List.of("owned"),1001);
        verify(f.fundingRepository,never()).evidence(anyLong(),anyList());assertFalse(result.fundingEvidenceTruncated());
    }
    @Test void foreignAllocationMetadataCannotLeakThroughOwnedEvent(){
        journal("owned");source("owned",2);var entry=read().entries().getFirst();
        assertEquals(com.example.investsmart.dto.SettlementFundingPage.Verification.INCOMPLETE,entry.funding().verification());
        assertTrue(entry.funding().sources().isEmpty());assertNull(entry.fundingEffect().settledDelta());
        assertFalse(entry.toString().contains("source-owned"));
    }
    @Test void nonFundingRowsNeedNoEvidenceQueryAndMissingSourcesRemainVisible(){
        f.entry(1,"deposit","SETTLED_CREDIT","1",JournalReportFixture.AT);assertEquals(1,read().entries().size());
        verifyNoInteractions(f.fundingRepository);journal("missing");var result=read();
        assertEquals(2,result.entries().size());assertTrue(result.entries().stream().filter(e->e.raw().eventKey().equals("missing")).allMatch(e->e.funding().verification()==com.example.investsmart.dto.SettlementFundingPage.Verification.INCOMPLETE));
    }
    @Test void thousandKeysUseTenQueriesAndGlobalBudgetWithoutFalseOverflow(){
        for(int i=0;i<1000;i++){var key=String.format("e%04d",i);journal(key);source(key,1);}
        var result=read();assertFalse(result.fundingEvidenceTruncated());assertFalse(result.truncated());assertEquals(1000,result.entries().size());
        assertTrue(result.entries().stream().allMatch(e->e.fundingEffect().settledDelta().compareTo(new BigDecimal("-1"))==0));
        verify(f.fundingRepository,times(10)).evidence(eq(1L),anyList(),anyInt());
        verify(f.fundingRepository).evidence(eq(1L),argThat(keys->keys.size()==100&&keys.getFirst().equals("e0900")),eq(101));
    }
    @Test void overflowInSecondBatchDiscardsEarlierEffectsAndKeepsEveryMovement(){
        for(int i=0;i<101;i++)journal(String.format("e%04d",i));
        var calls=new int[1];doAnswer(call->{
            int limit=call.getArgument(2);assertEquals(calls[0]++==0?1001:1,limit);
            var rows=new ArrayList<SettlementFundingRepository.Evidence>();
            for(int i=0;i<(limit==1001?1000:1);i++)rows.add(new SettlementFundingRepository.Evidence("e0000","RESERVE",new BigDecimal("1"),"a"+i,"a"+i,"ORDER","e0000","SETTLED",null,null,null,null));
            return rows;
        }).when(f.fundingRepository).evidence(eq(1L),anyList(),anyInt());
        var result=read();assertTrue(result.fundingEvidenceTruncated());assertFalse(result.truncated());assertEquals(101,result.entries().size());
        assertTrue(result.entries().stream().allMatch(e->e.funding().sources().isEmpty()&&e.fundingEffect().settledDelta()==null));
        assertTrue(result.limitations().contains("FUNDING_EVIDENCE_LIMIT_NO_SOURCE_EFFECTS"));assertEquals(2,calls[0]);
    }
    @Test void actualWriterMixedRefundStaysUnknownWhenIntegrated(){
        var writer=new SettlementFundsTests();writer.setup();writer.cash("deposit","20");var obligation=writer.pending("sale","30",SettlementFunds.CreditPolicy.REUSABLE);
        writer.reserve("reserve","30");writer.time(SettlementFundsTests.CUTOFF);writer.get(()->writer.funds.settle(1,obligation));writer.release("refund","30");
        try(var report=new JournalReportFixture(writer.fixture.source,false)){
            var result=report.context.getBean(JournalPeriodService.class).read(report.owner.getEmail(),SettlementFundsTests.BEFORE.minusSeconds(1),SettlementFundsTests.CUTOFF.plusSeconds(1));
            var refund=result.entries().stream().filter(e->e.raw().eventKey().equals("refund")).findFirst().orElseThrow();
            assertEquals(com.example.investsmart.dto.SettlementFundingPage.Verification.MATCHED,refund.funding().verification());
            assertEquals(FundingEffectClassifier.Coverage.UNKNOWN_CREDIT_REFUND_DESTINATION,refund.fundingEffect().coverage());assertNull(refund.fundingEffect().settledDelta());
        }
    }
}
