package com.example.investsmart.settlement;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.ResourceLock;

/** Every case starts a fresh uniquely labeled synthetic container, never an operator database. */
@EnabledIfEnvironmentVariable(named="V3_JOURNAL_MYSQL",matches="true")
@ResourceLock("investsmart-journal-synthetic-lifecycle")
class ReportingIndexMySqlTests {
    @BeforeEach void start()throws Exception{SettlementJournalMySqlTests.start();}
    @AfterEach void cleanup()throws Exception{SettlementJournalMySqlTests.cleanup();}
    @Test @Timeout(120) void populatedManagedUpgradePreservesFinancialHistoryAndReports()throws Exception{ReportingIndexTestSupport.verifyUpgrade(SettlementJournalMySqlTests.readySource());}
    @Test @Timeout(120) void shippingFreshBaselineHasApprovedNonuniqueIndexOrder()throws Exception{ReportingIndexTestSupport.fresh(SettlementJournalMySqlTests.readySource(),false);}
    @Test @Timeout(120) void runtimeBaselineHasApprovedNonuniqueIndexOrder()throws Exception{ReportingIndexTestSupport.fresh(SettlementJournalMySqlTests.readySource(),true);}
}
