package com.example.investsmart.settlement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.ResourceLock;
/** Dedicated fresh synthetic fixture; recovery suite remains independently executable. */
@EnabledIfEnvironmentVariable(named="V3_REPORT_COST",matches="true")
@ResourceLock("investsmart-journal-synthetic-lifecycle")
class ReportingQueryCostMySqlTests {
    private boolean ownsContainer;
    @BeforeEach void start()throws Exception{Assumptions.assumeTrue("true".equals(System.getenv("V3_JOURNAL_MYSQL")));ownsContainer=true;SettlementJournalMySqlTests.start();}
    @AfterEach void cleanup()throws Exception{if(ownsContainer)SettlementJournalMySqlTests.cleanup();}
    @Test @Timeout(240) void actualQueriesPreserveInvariantsAcrossCostMatrix()throws Exception{
        var source=SettlementJournalMySqlTests.readySource();
        try(var fixture=new JournalReportFixture(source,true)){fixture.account(1);}
        ReportingQueryCostProbe.run(source);
    }
    @Test @Timeout(240) void indexedMatrixPreservesAllReportContracts()throws Exception{
        var source=SettlementJournalMySqlTests.readySource();
        try(var fixture=new JournalReportFixture(source,true)){fixture.account(1);}
        ReportingQueryCostProbe.run(source,true);
    }
}
