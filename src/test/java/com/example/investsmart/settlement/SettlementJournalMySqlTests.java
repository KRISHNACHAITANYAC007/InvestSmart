package com.example.investsmart.settlement;

import java.nio.charset.StandardCharsets;
import com.example.investsmart.dto.JournalPeriodEvidence;
import com.example.investsmart.dto.PeriodSummaryEligibility;
import static com.example.investsmart.dto.PeriodSummaryEligibility.Scope.*;
import static com.example.investsmart.dto.PeriodSummaryEligibility.Status.*;
import java.sql.Connection;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** Opt-in local synthetic database only; never accepts operator database URLs or credentials. */
@EnabledIfEnvironmentVariable(named="V3_JOURNAL_MYSQL", matches="true")
@org.junit.jupiter.api.parallel.ResourceLock("investsmart-journal-synthetic-lifecycle")
class SettlementJournalMySqlTests {
    static final String DATABASE="investsmart_journal_synthetic";
    static final String NAME="investsmart-journal-test-"+UUID.randomUUID();
    static final String PASSWORD=UUID.randomUUID().toString();
    static boolean created;

    @BeforeAll static void start() throws Exception {
        // Values are passed via environment and never printed or stored in source/logs.
        created=true;
        docker(Map.of("MYSQL_ROOT_PASSWORD",UUID.randomUUID().toString(),"MYSQL_PASSWORD",PASSWORD),null,
                "run","--detach","--pull=never","--name",NAME,"--label","investsmart.scope=journal-synthetic",
                "--publish","127.0.0.1::3306","--env","MYSQL_ROOT_PASSWORD","--env","MYSQL_PASSWORD",
                "--env","MYSQL_USER=journal_test","--env","MYSQL_DATABASE="+DATABASE,"mysql:8.4");
    }

    @AfterAll static void cleanup() throws Exception {
        if(created) {
            // Only the unique container created by this class and its anonymous test volume.
            String labels=docker(Map.of(),null,"inspect","--format","{{json .Config.Labels}}",NAME).trim();
            assertEquals("journal-synthetic",new tools.jackson.databind.ObjectMapper().readTree(labels).path("investsmart.scope").asText());
            docker(Map.of(),null,"rm","--force","--volumes",NAME);
        }
    }

    @Test @Timeout(240)
    void snapshotIsAtomicAndSurvivesRestartAndDumpRestore() throws Exception {
        DriverManagerDataSource source=readySource();
        var pool=Executors.newFixedThreadPool(12);
        try(var f=new JournalReportFixture(source,true)) {
            var executionPeriodService=f.context.getBean(ExecutionFeePeriodService.class);
            var financePeriodService=f.context.getBean(FinancePeriodService.class);
            var periodService=f.context.getBean(JournalPeriodService.class);
            var periodFrom=JournalReportFixture.AT;
            var periodTo=periodFrom.plusSeconds(1);
            var combinedService=f.context.getBean(CombinedTradeService.class);
            var optionService=f.context.getBean(OptionLegacyService.class);
            f.db.execute("CREATE TABLE option_orders(id VARCHAR(36) PRIMARY KEY,user_id BIGINT,instrument_key VARCHAR(100),expiry DATE,side VARCHAR(4),units INT,lot_size INT,fill_price DECIMAL(19,2),status VARCHAR(16),order_kind VARCHAR(20))");
            f.db.update("INSERT INTO option_orders VALUES('100',1,'OPTION',?,'BUY',100,50,100,'FILLED','MARKET')",java.sql.Date.valueOf("2026-11-26"));
            intradayTicket(f,"option-ticket","BUY",100,"0.30");
            f.db.update("UPDATE paper_tickets SET product='CASH',stock_id=NULL,position_id=NULL,instrument_key='OPTION',expiry=?,lot_size=50 WHERE id='option-ticket'",java.sql.Date.valueOf("2026-11-26"));
            f.db.update("UPDATE paper_fills SET legacy_order_id='100' WHERE id='fill-option-ticket'");
            var legacyService=f.context.getBean(EquityLegacyService.class);
            f.db.execute("CREATE TABLE orders(id BIGINT PRIMARY KEY,user_id BIGINT,stock_id BIGINT,order_type VARCHAR(10),quantity INT,price DECIMAL(19,2),total_amount DECIMAL(19,2),status VARCHAR(20))");
            f.db.update("INSERT INTO orders VALUES(100,1,1,'BUY',2,100,200,'COMPLETED')");
            intradayTicket(f,"legacy-ticket","BUY",2,"0.50");
            f.db.update("UPDATE paper_tickets SET product='CASH',position_id=NULL WHERE id='legacy-ticket'");
            f.db.update("UPDATE paper_fills SET legacy_order_id='100' WHERE id='fill-legacy-ticket'");
            f.db.update("INSERT INTO orders VALUES(200,2,1,'BUY',2,100,200,'COMPLETED')");
            f.db.update("INSERT INTO paper_fills VALUES('foreign-only','missing-foreign-ticket',2,0,2,100,0,'200',?)",java.sql.Timestamp.from(JournalReportFixture.AT));
            var linkedFees=f.context.getBean(IntradayFeeService.class);
            var intradayService=f.context.getBean(IntradayEvidenceService.class);
            f.db.update("""
                    INSERT INTO paper_intraday_positions(id,user_id,stock_id,direction,quantity,entry_price,
                    collateral,realized_pnl,trade_date,created_at) VALUES('intraday',1,1,'LONG',10,100,500,0,?,?)
                    """,java.sql.Date.valueOf("2026-11-24"),java.sql.Timestamp.from(JournalReportFixture.AT));
            f.db.update("INSERT INTO paper_intraday_events VALUES('intraday-open',1,'intraday','OPEN',10,500,0,0.30,?)",
                    java.sql.Timestamp.from(JournalReportFixture.AT));
            intradayTicket(f,"intraday-open","BUY",10,"0.30");
            var feeService=f.context.getBean(TicketFeeService.class);
            f.db.update("""
                    INSERT INTO paper_tickets(id,user_id,request_id,side,product,variety,order_kind,validity,
                    quantity,filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fees,
                    policy_version,expires_at,created_at)
                    VALUES('ticket',1,'request','BUY','CASH','ICEBERG','LIMIT','DAY',2,1,2,1,1,10,0,'PART_FILLED',2,'test',?,?)
                    """,java.sql.Timestamp.from(JournalReportFixture.AT),java.sql.Timestamp.from(JournalReportFixture.AT));
            f.db.update("INSERT INTO paper_fills(id,ticket_id,user_id,leg_number,quantity,price,fee,created_at) VALUES('fill-0','ticket',1,0,1,10,2,?)",
                    java.sql.Timestamp.from(JournalReportFixture.AT));
            f.db.update("""
                    INSERT INTO paper_financing(id,user_id,stock_id,product,quantity,cost_amount,principal,
                    interest,interest_rate,created_at,last_accrued_date) VALUES('owned',1,1,'MTF',0,100,75,0,0.1,?,?)
                    """,java.sql.Timestamp.from(JournalReportFixture.AT),java.sql.Date.valueOf("2026-11-23"));
            var coverageRead=new CountDownLatch(5);
            var positionRead=new CountDownLatch(1);
            var ticketRead=new CountDownLatch(1);
            var intradayRead=new CountDownLatch(2);
            var legacyRead=new CountDownLatch(1);
            var optionRead=new CountDownLatch(1);
            var combinedRead=new CountDownLatch(1);
            var committed=new CountDownLatch(1);
            doAnswer(call->{
                var fills=call.callRealMethod();combinedRead.countDown();
                assertTrue(committed.await(15,TimeUnit.SECONDS));return fills;
            }).when(f.combinedRepository).fills(1);
            doAnswer(call->{
                var fill=call.callRealMethod();optionRead.countDown();
                assertTrue(committed.await(15,TimeUnit.SECONDS));return fill;
            }).when(f.optionLegacyRepository).ownedFill(1,"fill-option-ticket");
            doAnswer(call->{
                var fill=call.callRealMethod();legacyRead.countDown();
                assertTrue(committed.await(15,TimeUnit.SECONDS));return fill;
            }).when(f.legacyRepository).ownedFill(1,"fill-legacy-ticket");
            doAnswer(call->{
                var position=call.callRealMethod();
                intradayRead.countDown();
                assertTrue(committed.await(15,TimeUnit.SECONDS));
                return position;
            }).when(f.intradayRepository).owned(1,"intraday");
            doAnswer(call->{
                var ticket=call.callRealMethod();
                ticketRead.countDown();
                assertTrue(committed.await(15,TimeUnit.SECONDS));
                return ticket;
            }).when(f.feeRepository).owned(1,"ticket");
            doAnswer(call->{
                var owned=call.callRealMethod();
                positionRead.countDown();
                assertTrue(committed.await(15,TimeUnit.SECONDS));
                return owned;
            }).when(f.financeRepository).ownedPosition(1,"owned");
            doAnswer(call->{
                var coverage=call.callRealMethod();
                coverageRead.countDown();
                assertTrue(committed.await(15,TimeUnit.SECONDS));
                return coverage;
            }).when(f.repository).coverage(anyLong());
            var executionPeriodReading=pool.submit(()->executionPeriodService.read(f.owner.getEmail(),periodFrom,periodTo));
            var financePeriodReading=pool.submit(()->financePeriodService.read(f.owner.getEmail(),periodFrom,periodTo));
            var periodReading=pool.submit(()->periodService.read(f.owner.getEmail(),periodFrom,periodTo));
            var reading=pool.submit(()->f.service.read(f.owner.getEmail(),0,50));
            var fundingReading=pool.submit(()->f.fundingService.read(f.owner.getEmail(),0,50));
            var financeReading=pool.submit(()->f.financeService.read(f.owner.getEmail(),"owned",0,50));
            var feeReading=pool.submit(()->feeService.read(f.owner.getEmail(),"ticket"));
            var intradayReading=pool.submit(()->intradayService.read(f.owner.getEmail(),"intraday"));
            var linkedReading=pool.submit(()->linkedFees.read(f.owner.getEmail(),"intraday"));
            var legacyReading=pool.submit(()->legacyService.read(f.owner.getEmail(),"fill-legacy-ticket"));
            var optionReading=pool.submit(()->optionService.read(f.owner.getEmail(),"fill-option-ticket"));
            var combinedReading=pool.submit(()->combinedService.read(f.owner.getEmail()));
            try {
                assertTrue(combinedRead.await(15,TimeUnit.SECONDS));
                assertTrue(coverageRead.await(15,TimeUnit.SECONDS));
                assertTrue(positionRead.await(15,TimeUnit.SECONDS));
                assertTrue(ticketRead.await(15,TimeUnit.SECONDS));
                assertTrue(intradayRead.await(15,TimeUnit.SECONDS));
                assertTrue(legacyRead.await(15,TimeUnit.SECONDS));
                assertTrue(optionRead.await(15,TimeUnit.SECONDS));
                f.write.executeWithoutResult(status->{
                    f.db.update("UPDATE paper_intraday_events SET fee=9 WHERE event_key='intraday-open'");
                    f.db.update("UPDATE option_orders SET fill_price=101 WHERE id='100'");
                    f.db.update("UPDATE orders SET price=101,total_amount=202 WHERE id=100");
                    f.db.update("INSERT INTO paper_intraday_events VALUES('intraday-close',1,'intraday','CLOSE',4,-200,-40,0.11,?)",
                            java.sql.Timestamp.from(JournalReportFixture.AT));
                    intradayTicket(f,"intraday-close","SELL",4,"0.11");
                    f.db.update("INSERT INTO paper_intraday_events VALUES('intraday-foreign',2,'intraday','CLOSE',99,-999,999,999,?)",
                            java.sql.Timestamp.from(JournalReportFixture.AT));
                    f.db.update("UPDATE paper_intraday_positions SET quantity=6,collateral=300,realized_pnl=-40 WHERE id='intraday'");
                    f.db.update("INSERT INTO paper_fills(id,ticket_id,user_id,leg_number,quantity,price,fee,created_at) VALUES('fill-1','ticket',1,1,1,10,3,?)",
                            java.sql.Timestamp.from(JournalReportFixture.AT));
                    f.db.update("UPDATE paper_tickets SET fees=5,filled_units=2,filled_legs=2,status='FILLED' WHERE id='ticket'");
                    f.db.update("INSERT INTO paper_finance_events VALUES('interest','owned',1,'INTEREST',0,2.50,?)",java.sql.Timestamp.from(JournalReportFixture.AT));
                    // Deliberately malformed ownership link must never leak into user one's evidence.
                    f.db.update("INSERT INTO paper_finance_events VALUES('foreign','owned',2,'INTEREST',0,99,?)",java.sql.Timestamp.from(JournalReportFixture.AT));
                    f.db.update("UPDATE paper_financing SET interest=2.50 WHERE id='owned'");
                    f.account(1);
                    f.entry(1,"a","SETTLED_CREDIT","1234.56",JournalReportFixture.AT);
                    f.entry(2,"period-foreign","WITHDRAW","1.00",JournalReportFixture.AT);
                    f.entry(1,"z","RESERVE","0.01",JournalReportFixture.AT);
                    f.db.update("UPDATE settlement_journal SET reference_kind='ORDER',reference_id='order' WHERE user_id=1 AND event_key='z'");
                    f.db.update("INSERT INTO settlement_allocations VALUES('source',1,'ORDER','order','SETTLED',0.01,0.01,'RESERVED',?)",java.sql.Timestamp.from(JournalReportFixture.AT));
                    f.db.update("INSERT INTO settlement_allocation_events VALUES(1,'z','source','RESERVE',0.01)");
                    f.db.update("UPDATE users SET virtual_balance=1234.57 WHERE id=1");
                });
            } finally { committed.countDown(); }
            var executionPeriodBefore=executionPeriodReading.get(15,TimeUnit.SECONDS);
            assertFalse(executionPeriodBefore.initialization().initialized());
            assertEquals(new java.math.BigDecimal("2.00"),executionPeriodBefore.tickets().stream().filter(t->t.ticketId().equals("ticket")).findFirst().orElseThrow().selectedFee());
            assertFalse(executionPeriodBefore.toString().contains("fill-1"));
            assertEquals(new java.math.BigDecimal("0.30"),executionPeriodBefore.tickets().stream().filter(t->t.ticketId().equals("intraday-open")).findFirst().orElseThrow().selectedFee());
            var conflictingMisPeriod=executionPeriodService.read(f.owner.getEmail(),periodFrom,periodTo);
            assertNull(conflictingMisPeriod.tickets().stream().filter(t->t.ticketId().equals("intraday-open")).findFirst().orElseThrow().selectedFee());
            f.db.update("UPDATE paper_intraday_events SET fee=0.30,user_id=2,position_id='private-position' WHERE event_key='intraday-open'");
            var foreignMisPeriod=executionPeriodService.read(f.owner.getEmail(),periodFrom,periodTo);
            assertFalse(foreignMisPeriod.toString().contains("private-position"));
            assertEquals("MIS_EVENT_OWNERSHIP_CONFLICT",foreignMisPeriod.tickets().stream().filter(t->t.ticketId().equals("intraday-open")).findFirst().orElseThrow().limitation());
            f.db.update("UPDATE paper_intraday_events SET user_id=1,position_id='intraday' WHERE event_key='intraday-open'");
            var financePeriodBefore=financePeriodReading.get(15,TimeUnit.SECONDS);
            assertFalse(financePeriodBefore.initialization().initialized());assertTrue(financePeriodBefore.entries().isEmpty(),"Financing period coverage and events must share pre-commit snapshot");
            var periodBefore=periodReading.get(15,TimeUnit.SECONDS);
            assertFalse(periodBefore.initialization().initialized());
            assertTrue(periodBefore.entries().isEmpty(),"Period initialization and rows must share the pre-commit snapshot");
            assertFalse(periodBefore.completeFinancialHistory());
            assertEquals(UNAVAILABLE,eligibility(periodBefore,SELECTED_JOURNAL_SETTLED_CHANGE).status());
            assertNull(directChange(periodBefore,SELECTED_JOURNAL_SETTLED_CHANGE));
            var combinedBefore=combinedReading.get(15,TimeUnit.SECONDS);
            assertEquals(2,pairedGroups(combinedBefore),"Both namespaces must retain their pre-commit matched orders");
            assertFalse(combinedBefore.toString().contains("foreign-only"));
            var beforeCommit=reading.get(15,TimeUnit.SECONDS);
            assertFalse(beforeCommit.coverage().initialized());
            assertTrue(beforeCommit.entries().isEmpty(),"Coverage and entries must use the same pre-commit snapshot");
            var fundingBeforeCommit=fundingReading.get(15,TimeUnit.SECONDS);
            assertEquals(com.example.investsmart.dto.OptionLegacyEvidence.Verification.MATCHED_DUPLICATE,
                    optionReading.get(15,TimeUnit.SECONDS).verification(),"Option fill and legacy order must share the pre-commit snapshot");
            assertEquals(com.example.investsmart.dto.EquityLegacyEvidence.Verification.MATCHED_DUPLICATE,
                    legacyReading.get(15,TimeUnit.SECONDS).verification(),"Fill and legacy order must share the pre-commit snapshot");
            var intradayBeforeCommit=intradayReading.get(15,TimeUnit.SECONDS).comparison();
            var linkedBefore=linkedReading.get(15,TimeUnit.SECONDS);
            assertEquals(com.example.investsmart.dto.IntradayFeeEvidence.Verification.MATCHED_EVENT_FEES,linkedBefore.verification());
            assertEquals(new java.math.BigDecimal("0.30"),linkedBefore.reconciledEventFees(),
                    "Events, tickets and fills must use the pre-commit snapshot");
            assertEquals(IntradayEvidenceReconciler.Verification.MATCHED_POSITION_TOTALS,intradayBeforeCommit.verification());
            assertEquals(new java.math.BigDecimal("0.00"),intradayBeforeCommit.verifiedGrossRealizedPnl());
            assertEquals(new java.math.BigDecimal("0.30"),intradayBeforeCommit.eventFeeSum(),
                    "Intraday position and events must share the pre-commit snapshot");
            var feeBeforeCommit=feeReading.get(15,TimeUnit.SECONDS);
            assertEquals(com.example.investsmart.dto.TicketFeeEvidence.Verification.MATCHED,feeBeforeCommit.verification());
            assertEquals(new java.math.BigDecimal("2.00"),feeBeforeCommit.reconciledFee(),
                    "Ticket and fill fee evidence must use the same pre-commit snapshot");
            assertTrue(financeReading.get(15,TimeUnit.SECONDS).entries().isEmpty(),
                    "Position ownership and event rows must use the same pre-commit snapshot");
            assertFalse(fundingBeforeCommit.coverage().initialized()); assertTrue(fundingBeforeCommit.entries().isEmpty());
            doCallRealMethod().when(f.repository).coverage(anyLong());
            doCallRealMethod().when(f.financeRepository).ownedPosition(1,"owned");
            doCallRealMethod().when(f.feeRepository).owned(1,"ticket");
            doCallRealMethod().when(f.intradayRepository).owned(1,"intraday");
            doCallRealMethod().when(f.legacyRepository).ownedFill(1,"fill-legacy-ticket");
            doCallRealMethod().when(f.optionLegacyRepository).ownedFill(1,"fill-option-ticket");
            doCallRealMethod().when(f.combinedRepository).fills(1);
            var combinedConflict=combinedService.read(f.owner.getEmail());
            assertEquals(0,pairedGroups(combinedConflict),"New snapshot must retain conflicting orders separately");
            assertEquals(com.example.investsmart.dto.CombinedTradeEvidence.Status.INCOMPLETE,combinedConflict.status());
            var optionConflict=optionService.read(f.owner.getEmail(),"fill-option-ticket");
            assertEquals(com.example.investsmart.dto.OptionLegacyEvidence.Verification.MISMATCH,optionConflict.verification());
            assertNull(optionConflict.duplicateOptionOrderId());
            f.db.update("UPDATE option_orders SET fill_price=100 WHERE id='100'");
            var expectedOption=optionService.read(f.owner.getEmail(),"fill-option-ticket");
            assertEquals(com.example.investsmart.dto.OptionLegacyEvidence.Verification.MATCHED_DUPLICATE,expectedOption.verification());
            assertEquals("100",expectedOption.duplicateOptionOrderId());
            assertThrows(com.example.investsmart.exception.ResourceNotFoundException.class,
                    ()->optionService.read(f.other.getEmail(),"fill-option-ticket"));
            var legacyConflict=legacyService.read(f.owner.getEmail(),"fill-legacy-ticket");
            assertEquals(com.example.investsmart.dto.EquityLegacyEvidence.Verification.MISMATCH,legacyConflict.verification());
            assertNull(legacyConflict.duplicateOrderId());
            f.db.update("UPDATE orders SET price=100,total_amount=200 WHERE id=100");
            var expectedLegacy=legacyService.read(f.owner.getEmail(),"fill-legacy-ticket");
            assertEquals(com.example.investsmart.dto.EquityLegacyEvidence.Verification.MATCHED_DUPLICATE,expectedLegacy.verification());
            assertThrows(com.example.investsmart.exception.ResourceNotFoundException.class,
                    ()->legacyService.read(f.other.getEmail(),"fill-legacy-ticket"));
            var expectedCombined=combinedService.read(f.owner.getEmail());
            assertEquals(2,pairedGroups(expectedCombined));
            assertFalse(expectedCombined.toString().contains("foreign-only"));
            assertFalse(expectedCombined.groups().stream().flatMap(g->g.records().stream()).anyMatch(k->k.source()==com.example.investsmart.dto.CombinedTradeEvidence.Source.EQUITY_ORDER&&k.id().equals("200")));
            var otherCombined=combinedService.read(f.other.getEmail());
            assertEquals(2,otherCombined.groups().size());
            assertTrue(otherCombined.groups().stream().flatMap(g->g.records().stream()).allMatch(k->k.id().equals("foreign-only")||k.id().equals("200")));
            var expectedPeriod=periodService.read(f.owner.getEmail(),periodFrom,periodTo);
            assertTrue(expectedPeriod.initialization().initialized());
            assertEquals(List.of("a","z"),expectedPeriod.entries().stream().map(e->e.raw().eventKey()).toList());
            assertEquals(new java.math.BigDecimal("1234.56"),expectedPeriod.entries().getFirst().interpretation().settledDelta());
            assertNull(expectedPeriod.entries().get(1).interpretation().settledDelta(),"Reservation source effect remains unknown");
            assertFalse(expectedPeriod.completeFinancialHistory());assertFalse(expectedPeriod.truncated());
            assertEquals(ELIGIBLE_SUPPLIED_DIRECT_ROWS,eligibility(expectedPeriod,SELECTED_JOURNAL_SETTLED_CHANGE).status());
            assertEquals(ELIGIBLE_SUPPLIED_DIRECT_ROWS,eligibility(expectedPeriod,SELECTED_JOURNAL_RESERVED_CHANGE).status());
            assertEquals(UNAVAILABLE,eligibility(expectedPeriod,COMPLETE_ACCOUNT_EXPENSES).status());
            assertEquals(new java.math.BigDecimal("1234.55"),directChange(expectedPeriod,SELECTED_JOURNAL_SETTLED_CHANGE));
            assertEquals(new java.math.BigDecimal("0.01"),directChange(expectedPeriod,SELECTED_JOURNAL_RESERVED_CHANGE));
            assertNull(directChange(expectedPeriod,COMPLETE_ACCOUNT_EXPENSES));
            assertTrue(periodService.read(f.owner.getEmail(),periodFrom.minusSeconds(1),periodFrom).entries().isEmpty(),"End instant must be excluded on MySQL");
            assertEquals(List.of("period-foreign"),periodService.read(f.other.getEmail(),periodFrom,periodTo).entries().stream().map(e->e.raw().eventKey()).toList());
            var expected=f.service.read(f.owner.getEmail(),0,50);
            var expectedFunding=f.fundingService.read(f.owner.getEmail(),0,50);
            var expectedExecutionPeriod=executionPeriodService.read(f.owner.getEmail(),periodFrom,periodTo);
            assertEquals(new java.math.BigDecimal("5.00"),expectedExecutionPeriod.tickets().stream().filter(t->t.ticketId().equals("ticket")).findFirst().orElseThrow().selectedFee());
            assertFalse(expectedExecutionPeriod.toString().contains("foreign-only"));
            var foreignExecutionPeriod=executionPeriodService.read(f.other.getEmail(),periodFrom,periodTo);
            assertEquals(1,foreignExecutionPeriod.selectedFills().size());assertNull(foreignExecutionPeriod.selectedFills().getFirst().ticketId());assertTrue(foreignExecutionPeriod.tickets().isEmpty());
            assertTrue(executionPeriodService.read(f.owner.getEmail(),periodFrom.minusSeconds(1),periodFrom).selectedFills().isEmpty());
            var expectedFinancePeriod=financePeriodService.read(f.owner.getEmail(),periodFrom,periodTo);
            assertEquals(1,expectedFinancePeriod.entries().size());assertEquals("interest",expectedFinancePeriod.entries().getFirst().eventKey());
            assertEquals(new java.math.BigDecimal("2.50"),expectedFinancePeriod.entries().getFirst().interpretation().interestAccrued());
            assertFalse(expectedFinancePeriod.completeFinancialHistory());
            var foreignFinancePeriod=financePeriodService.read(f.other.getEmail(),periodFrom,periodTo);
            assertEquals(1,foreignFinancePeriod.entries().size());assertNull(foreignFinancePeriod.entries().getFirst().positionId());assertNull(foreignFinancePeriod.entries().getFirst().interpretation());
            assertEquals(com.example.investsmart.dto.FinancePeriodEvidence.Ownership.POSITION_UNAVAILABLE,foreignFinancePeriod.entries().getFirst().ownership());
            assertTrue(financePeriodService.read(f.owner.getEmail(),periodFrom.minusSeconds(1),periodFrom).entries().isEmpty());
            var expectedFinance=f.financeService.read(f.owner.getEmail(),"owned",0,50);
            var expectedFee=feeService.read(f.owner.getEmail(),"ticket");
            var expectedIntraday=intradayService.read(f.owner.getEmail(),"intraday");
            var expectedLinked=linkedFees.read(f.owner.getEmail(),"intraday");
            assertEquals(com.example.investsmart.dto.IntradayFeeEvidence.Verification.MATCHED_EVENT_FEES,expectedLinked.verification());
            assertEquals(new java.math.BigDecimal("0.41"),expectedLinked.reconciledEventFees());
            assertThrows(com.example.investsmart.exception.ResourceNotFoundException.class,
                    ()->linkedFees.read(f.other.getEmail(),"intraday"));
            assertEquals(IntradayEvidenceReconciler.Verification.MATCHED_POSITION_TOTALS,expectedIntraday.comparison().verification());
            assertEquals(new java.math.BigDecimal("-40.00"),expectedIntraday.comparison().verifiedGrossRealizedPnl());
            assertEquals(new java.math.BigDecimal("0.41"),expectedIntraday.comparison().eventFeeSum());
            assertThrows(com.example.investsmart.exception.ResourceNotFoundException.class,
                    ()->intradayService.read(f.other.getEmail(),"intraday"));
            assertEquals(com.example.investsmart.dto.TicketFeeEvidence.Verification.MATCHED,expectedFee.verification());
            assertEquals(new java.math.BigDecimal("5.00"),expectedFee.reconciledFee());
            assertThrows(com.example.investsmart.exception.ResourceNotFoundException.class,
                    ()->feeService.read(f.other.getEmail(),"ticket"));
            assertEquals(1,expectedFinance.entries().size());
            assertEquals(new java.math.BigDecimal("2.50"),expectedFinance.entries().getFirst().interpretation().interestAccrued());
            assertThrows(com.example.investsmart.exception.ResourceNotFoundException.class,
                    ()->f.financeService.read(f.other.getEmail(),"owned",0,50));
            assertEquals(com.example.investsmart.dto.SettlementFundingPage.Verification.MATCHED,expectedFunding.entries().getFirst().verification());
            assertTrue(expected.coverage().initialized());
            assertEquals(List.of("z","a"),expected.entries().stream().map(e->e.eventKey()).toList());
            assertEquals(new java.math.BigDecimal("0.01"),expected.entries().getFirst().amount());
            assertEquals(List.of("period-foreign"),f.service.read(f.other.getEmail(),0,50).entries().stream().map(e->e.eventKey()).toList());

            // Verify the real index is usable on a bounded owned journal query; no schema changes.
            var plan=f.db.queryForList("EXPLAIN SELECT event_key,action,amount,reference_kind,reference_id,occurred_at FROM settlement_journal WHERE user_id=1 ORDER BY occurred_at DESC,event_key DESC LIMIT 50");
            assertTrue(plan.stream().anyMatch(row->String.valueOf(row.get("possible_keys")).contains("ix_settlement_journal_user")));

            var periodPlan=f.db.queryForList("EXPLAIN SELECT event_key,action,amount,reference_kind,reference_id,occurred_at FROM settlement_journal WHERE user_id=? AND occurred_at>=? AND occurred_at<? ORDER BY occurred_at,event_key LIMIT 1001",
                    1,java.sql.Timestamp.from(periodFrom),java.sql.Timestamp.from(periodTo));
            assertTrue(periodPlan.stream().anyMatch(row->String.valueOf(row.get("possible_keys")).contains("ix_settlement_journal_user")),"Existing index must be eligible for the owned period query; this is not a load benchmark");

            var periodRowsRead=new CountDownLatch(1);
            var sourceCommitted=new CountDownLatch(1);
            doAnswer(call->{
                var rows=call.callRealMethod();periodRowsRead.countDown();
                assertTrue(sourceCommitted.await(15,TimeUnit.SECONDS));return rows;
            }).when(f.periodRepository).entries(1,periodFrom,periodTo);
            var periodSourceReading=pool.submit(()->periodService.read(f.owner.getEmail(),periodFrom,periodTo));
            try {
                assertTrue(periodRowsRead.await(15,TimeUnit.SECONDS));
                f.write.executeWithoutResult(status->f.db.update("UPDATE settlement_allocation_events SET amount=0.02 WHERE user_id=1 AND event_key='z' AND allocation_id='source'"));
            } finally {sourceCommitted.countDown();}
            assertEquals(expectedPeriod,periodSourceReading.get(15,TimeUnit.SECONDS),"Period rows and funding sources must share the pre-commit snapshot");
            doCallRealMethod().when(f.periodRepository).entries(1,periodFrom,periodTo);
            var changedPeriod=periodService.read(f.owner.getEmail(),periodFrom,periodTo);
            var changedReserve=changedPeriod.entries().stream().filter(e->e.raw().eventKey().equals("z")).findFirst().orElseThrow();
            assertEquals(com.example.investsmart.dto.SettlementFundingPage.Verification.MISMATCH,changedReserve.funding().verification());
            assertNull(changedReserve.fundingEffect().settledDelta());
            assertEquals(UNAVAILABLE,eligibility(changedPeriod,SELECTED_JOURNAL_SETTLED_CHANGE).status());
            assertTrue(eligibility(changedPeriod,SELECTED_JOURNAL_RESERVED_CHANGE).reasons().contains(PeriodSummaryEligibility.Reason.CONFLICTING_FUNDING));
            assertNull(directChange(changedPeriod,SELECTED_JOURNAL_SETTLED_CHANGE));
            assertNull(directChange(changedPeriod,SELECTED_JOURNAL_RESERVED_CHANGE));
            f.db.update("UPDATE settlement_allocation_events SET amount=0.01 WHERE user_id=1 AND event_key='z' AND allocation_id='source'");
            assertEquals(expectedPeriod,periodService.read(f.owner.getEmail(),periodFrom,periodTo));

            byte[] dump=docker(Map.of(),null,"exec",NAME,"sh","-c",
                    "MYSQL_PWD=\"$MYSQL_PASSWORD\" exec mysqldump --user=journal_test --no-tablespaces --single-transaction "+DATABASE).getBytes(StandardCharsets.UTF_8);
            assertTrue(dump.length>1000);
            docker(Map.of(),null,"restart",NAME);
            try(var restarted=new JournalReportFixture(readySource(),false)) {
                var restartedExecutionPeriod=restarted.context.getBean(ExecutionFeePeriodService.class);
                var restartedFinancePeriod=restarted.context.getBean(FinancePeriodService.class);
                assertEquals(expectedFinancePeriod,restartedFinancePeriod.read(restarted.owner.getEmail(),periodFrom,periodTo));
                assertEquals(expectedExecutionPeriod,restartedExecutionPeriod.read(restarted.owner.getEmail(),periodFrom,periodTo));
                var restartedPeriod=restarted.context.getBean(JournalPeriodService.class);
                assertEquals(expectedPeriod,restartedPeriod.read(restarted.owner.getEmail(),periodFrom,periodTo));
                var restartedCombined=restarted.context.getBean(CombinedTradeService.class);
                assertEquals(expectedCombined,restartedCombined.read(restarted.owner.getEmail()));
                var restartedFees=restarted.context.getBean(TicketFeeService.class);
                var restartedIntraday=restarted.context.getBean(IntradayEvidenceService.class);
                var restartedLinked=restarted.context.getBean(IntradayFeeService.class);
                var restartedLegacy=restarted.context.getBean(EquityLegacyService.class);
                var restartedOption=restarted.context.getBean(OptionLegacyService.class);
                assertEquals(expectedOption,restartedOption.read(restarted.owner.getEmail(),"fill-option-ticket"));
                restarted.db.update("DELETE FROM option_orders WHERE id='100'");
                var missingCombined=restartedCombined.read(restarted.owner.getEmail());
                assertEquals(1,pairedGroups(missingCombined));
                assertEquals(com.example.investsmart.dto.CombinedTradeEvidence.Status.INCOMPLETE,missingCombined.status());
                assertTrue(missingCombined.groups().stream().anyMatch(g->g.representative().id().equals("fill-option-ticket")&&g.records().size()==1));
                assertEquals(com.example.investsmart.dto.OptionLegacyEvidence.Verification.INCOMPLETE,
                        restartedOption.read(restarted.owner.getEmail(),"fill-option-ticket").verification());
                assertEquals(expectedLegacy,restartedLegacy.read(restarted.owner.getEmail(),"fill-legacy-ticket"));
                restarted.db.update("DELETE FROM orders WHERE id=100");
                assertEquals(com.example.investsmart.dto.EquityLegacyEvidence.Verification.INCOMPLETE,
                        restartedLegacy.read(restarted.owner.getEmail(),"fill-legacy-ticket").verification());
                assertEquals(expectedLinked,restartedLinked.read(restarted.owner.getEmail(),"intraday"));
                restarted.db.update("DELETE FROM paper_fills WHERE ticket_id='intraday-close'");
                assertEquals(com.example.investsmart.dto.IntradayFeeEvidence.Verification.INCOMPLETE,
                        restartedLinked.read(restarted.owner.getEmail(),"intraday").verification());
                assertEquals(expectedIntraday,restartedIntraday.read(restarted.owner.getEmail(),"intraday"));
                restarted.db.update("DELETE FROM paper_intraday_events");
                restarted.db.update("UPDATE paper_intraday_positions SET quantity=10,collateral=500,realized_pnl=0 WHERE id='intraday'");
                assertEquals(IntradayEvidenceReconciler.Verification.INCOMPLETE,
                        restartedIntraday.read(restarted.owner.getEmail(),"intraday").comparison().verification());
                assertEquals(expectedFee,restartedFees.read(restarted.owner.getEmail(),"ticket"));
                assertEquals(expected,restarted.service.read(restarted.owner.getEmail(),0,50));
                assertEquals(expectedFunding,restarted.fundingService.read(restarted.owner.getEmail(),0,50));
                assertEquals(expectedFinance,restarted.financeService.read(restarted.owner.getEmail(),"owned",0,50));
                restarted.db.update("DELETE FROM paper_finance_events");
                assertNull(restartedExecutionPeriod.read(restarted.owner.getEmail(),periodFrom,periodTo).tickets().stream().filter(t->t.ticketId().equals("intraday-open")).findFirst().orElseThrow().selectedFee(),"Deleted MIS event must withhold the canonical slice");
                var removedFinancePeriod=restartedFinancePeriod.read(restarted.owner.getEmail(),periodFrom,periodTo);
                assertTrue(removedFinancePeriod.entries().isEmpty());assertFalse(removedFinancePeriod.completeFinancialHistory());
                restarted.db.update("DELETE FROM paper_fills WHERE ticket_id='ticket'");
                assertFalse(restartedExecutionPeriod.read(restarted.owner.getEmail(),periodFrom,periodTo).selectedFills().stream().anyMatch(r->"ticket".equals(r.ticketId())));
                restarted.db.update("UPDATE paper_tickets SET fees=0,filled_units=0,filled_legs=0 WHERE id='ticket'");
                assertEquals(new java.math.BigDecimal("0.00"),restartedFees.read(restarted.owner.getEmail(),"ticket").reconciledFee());
                restarted.db.update("UPDATE paper_financing SET interest=0 WHERE id='owned'");
                assertTrue(restarted.financeService.read(restarted.owner.getEmail(),"owned",0,50).entries().isEmpty());
                restarted.db.update("DELETE FROM settlement_journal WHERE user_id=1");
                restarted.db.update("DELETE FROM settlement_allocation_events WHERE user_id=1");
                assertTrue(restarted.service.read(restarted.owner.getEmail(),0,50).entries().isEmpty());
                var removedPeriod=restartedPeriod.read(restarted.owner.getEmail(),periodFrom,periodTo);
                assertTrue(removedPeriod.entries().isEmpty());assertFalse(removedPeriod.completeFinancialHistory());
                assertEquals(new java.math.BigDecimal("0.00"),directChange(removedPeriod,SELECTED_JOURNAL_SETTLED_CHANGE));
                assertNull(directChange(removedPeriod,HISTORICAL_BALANCES));
                docker(Map.of(),dump,"exec","--interactive",NAME,"sh","-c",
                        "MYSQL_PWD=\"$MYSQL_PASSWORD\" exec mysql --user=journal_test "+DATABASE);
                assertEquals(expected,restarted.service.read(restarted.owner.getEmail(),0,50));
                assertEquals(expectedFunding,restarted.fundingService.read(restarted.owner.getEmail(),0,50));
                assertEquals(expectedFinance,restarted.financeService.read(restarted.owner.getEmail(),"owned",0,50));
                assertEquals(expectedFinancePeriod,restartedFinancePeriod.read(restarted.owner.getEmail(),periodFrom,periodTo));
                assertEquals(expectedExecutionPeriod,restartedExecutionPeriod.read(restarted.owner.getEmail(),periodFrom,periodTo));
                assertEquals(expectedFee,restartedFees.read(restarted.owner.getEmail(),"ticket"));
                assertEquals(new java.math.BigDecimal("2.50"),restarted.db.queryForObject("SELECT interest FROM paper_financing WHERE id='owned'",java.math.BigDecimal.class));
                assertEquals(expectedIntraday,restartedIntraday.read(restarted.owner.getEmail(),"intraday"));
                assertEquals(new java.math.BigDecimal("300.00"),restarted.db.queryForObject("SELECT collateral FROM paper_intraday_positions WHERE id='intraday'",java.math.BigDecimal.class));
                assertEquals(expectedLinked,restartedLinked.read(restarted.owner.getEmail(),"intraday"));
                assertEquals(new java.math.BigDecimal("1234.57"),restarted.db.queryForObject("SELECT virtual_balance FROM users WHERE id=1",java.math.BigDecimal.class));
                assertEquals(expectedLegacy,restartedLegacy.read(restarted.owner.getEmail(),"fill-legacy-ticket"));
                assertEquals(expectedOption,restartedOption.read(restarted.owner.getEmail(),"fill-option-ticket"));
                assertEquals(expectedCombined,restartedCombined.read(restarted.owner.getEmail()));
                assertEquals(expectedPeriod,restartedPeriod.read(restarted.owner.getEmail(),periodFrom,periodTo));
            }
            verifyOwnedChargeReader(pool);
            verifyPendingPolicyReader(pool);
        } finally {
            pool.shutdownNow(); assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));
        }
    }

    private static void verifyPendingPolicyReader(ExecutorService pool) throws Exception {
        var from=JournalReportFixture.AT.plusSeconds(7200);var to=from.plusSeconds(1);
        com.example.investsmart.dto.PendingPolicyPeriodEvidence expected;
        byte[] dump;
        try(var f=new JournalReportFixture(readySource(),false)){
            var writer=new SettlementFunds(f.db,null,null,java.time.Clock.fixed(from,java.time.ZoneOffset.UTC));
            writer.journal(1,"policy-event","PENDING_CREDIT",new java.math.BigDecimal("30.00"),null,null,"policy-source","NSE","EQUITY","2026-11-23","REUSABLE");
            writer.journal(2,"private-policy-event","PENDING_CREDIT",new java.math.BigDecimal("30.00"),null,null,"private-policy-source","NSE","EQUITY","2026-11-23","RESTRICTED");
            for(long user:List.of(1L,2L))f.db.update("INSERT INTO settlement_obligations(id,user_id,source_key,exchange_code,segment_code,trade_date,obligation_kind,credit_policy,amount,remaining_credit,status,created_at) VALUES(?,?,?,'NSE','EQUITY','2026-11-23','CASH_CREDIT',?,30,0,'SETTLED',?)",user==1?"policy-owned":"private-policy-owned",user,user==1?"policy-source":"private-policy-source",user==1?"REUSABLE":"RESTRICTED",java.sql.Timestamp.from(from.minusSeconds(86400)));
            var service=f.context.getBean(PendingPolicyPeriodService.class);var selected=new CountDownLatch(1);var committed=new CountDownLatch(1);
            doAnswer(call->{var rows=call.callRealMethod();selected.countDown();assertTrue(committed.await(15,TimeUnit.SECONDS));return rows;}).when(f.pendingPolicyRepository).instructions(1,from,to);
            var reading=pool.submit(()->service.read(f.owner.getEmail(),from,to));
            try{assertTrue(selected.await(15,TimeUnit.SECONDS));f.db.update("UPDATE settlement_obligations SET credit_policy='RESTRICTED' WHERE id='policy-owned'");}finally{committed.countDown();}
            assertEquals("REUSABLE",reading.get(15,TimeUnit.SECONDS).entries().getFirst().policy().policy());
            assertNull(service.read(f.owner.getEmail(),from,to).entries().getFirst().policy().policy());
            f.db.update("UPDATE settlement_obligations SET credit_policy='REUSABLE' WHERE id='policy-owned'");expected=service.read(f.owner.getEmail(),from,to);
            assertEquals(1,expected.entries().size());assertEquals("policy-owned",expected.entries().getFirst().policy().obligationId());assertFalse(expected.toString().contains("private-policy"));assertFalse(expected.completeFinancialHistory());
            assertEquals("RESTRICTED",service.read(f.other.getEmail(),from,to).entries().getFirst().policy().policy());
            assertTrue(service.read(f.owner.getEmail(),from.minusSeconds(1),from).entries().isEmpty());
            dump=docker(Map.of(),null,"exec",NAME,"sh","-c","MYSQL_PWD=\"$MYSQL_PASSWORD\" exec mysqldump --user=journal_test --single-transaction --skip-comments --no-tablespaces "+DATABASE).getBytes(StandardCharsets.UTF_8);
        }
        assertTrue(dump.length>1000);docker(Map.of(),null,"restart",NAME);
        try(var f=new JournalReportFixture(readySource(),false)){
            var service=f.context.getBean(PendingPolicyPeriodService.class);assertEquals(expected,service.read(f.owner.getEmail(),from,to));
            f.db.update("DELETE FROM settlement_obligations WHERE id='policy-owned'");var missing=service.read(f.owner.getEmail(),from,to);assertEquals(1,missing.entries().size());assertNull(missing.entries().getFirst().policy().policy());
            docker(Map.of(),dump,"exec","--interactive",NAME,"sh","-c","MYSQL_PWD=\"$MYSQL_PASSWORD\" exec mysql --user=journal_test "+DATABASE);
            assertEquals(expected,service.read(f.owner.getEmail(),from,to));
        }
    }

    private static void verifyOwnedChargeReader(ExecutorService pool) throws Exception {
        String key="charge-resilience",reference=ChargeEvidenceReconciler.reference(key);
        String reserve="CHARGE_RESERVE:"+reference,spend="CHARGE_SPEND:"+reference;
        var at=JournalReportFixture.AT.plusSeconds(3600);
        com.example.investsmart.dto.ChargeEvidence expectedCharge;
        com.example.investsmart.dto.JournalPeriodEvidence expectedChargePeriod;
        com.example.investsmart.dto.JournalPeriodEvidence expectedCompleteChargePeriod;
        try(var f=new JournalReportFixture(readySource(),false)) {
            f.entry(1,key,"REALIZED_CHARGE","70.00",at);
            for(var child:List.of(reserve,spend)) {
                String action=child.equals(reserve)?"RESERVE":"SPEND";
                f.entry(1,child,action,"50.00",at.plusSeconds(1));
                f.db.update("UPDATE settlement_journal SET reference_kind='CHARGE',reference_id=? WHERE user_id=1 AND event_key=?",reference,child);
            }
            f.db.update("INSERT INTO settlement_obligations(id,user_id,source_key,exchange_code,segment_code,trade_date,obligation_kind,credit_policy,amount,remaining_credit,status,created_at) VALUES('charge-obligation',1,'charge-obligation','NSE','EQUITY','2026-11-23','CASH_CREDIT','REUSABLE',30,0,'PENDING',?)",java.sql.Timestamp.from(at));
            for(var source:List.of("charge-cash","charge-credit")) {
                boolean cash=source.equals("charge-cash");
                f.db.update("INSERT INTO settlement_allocations VALUES(?,1,'CHARGE',?,?,?,0,'CLOSED',?)",source,reference,cash?"SETTLED":"charge-obligation",cash?20:30,java.sql.Timestamp.from(at));
                for(var child:List.of(reserve,spend))f.db.update("INSERT INTO settlement_allocation_events VALUES(1,?,?,?,?)",child,source,child.equals(reserve)?"RESERVE":"SPEND",cash?20:30);
            }
            f.entry(2,"charge-private-extra","RESERVE","999.00",at);
            f.db.update("UPDATE settlement_journal SET reference_kind='CHARGE',reference_id=? WHERE user_id=2 AND event_key='charge-private-extra'",reference);
            var service=f.context.getBean(ChargeEvidenceService.class);
            expectedCharge=service.read(f.owner.getEmail(),key);
            assertEquals(ChargeEvidenceReconciler.Status.MATCHED_SUPPLIED_PAIR,expectedCharge.comparison().status());
            assertEquals(new java.math.BigDecimal("20.00"),expectedCharge.comparison().impliedUnfundedAmount());
            assertEquals(new java.math.BigDecimal("-40.00"),expectedCharge.comparison().settledEffect());
            assertEquals(2,expectedCharge.children().size());assertFalse(expectedCharge.toString().contains("charge-private-extra"));
            assertThrows(com.example.investsmart.exception.ResourceNotFoundException.class,()->service.read(f.other.getEmail(),key));
            var parentRead=new CountDownLatch(1);var committed=new CountDownLatch(1);
            doAnswer(call->{var row=call.callRealMethod();parentRead.countDown();assertTrue(committed.await(15,TimeUnit.SECONDS));return row;})
                    .when(f.chargeRepository).parent(1,key);
            var reading=pool.submit(()->service.read(f.owner.getEmail(),key));
            try {
                assertTrue(parentRead.await(15,TimeUnit.SECONDS));
                f.write.executeWithoutResult(status->f.db.update("UPDATE settlement_allocation_events SET amount=21 WHERE user_id=1 AND event_key=? AND allocation_id='charge-cash'",reserve));
            } finally {committed.countDown();}
            assertEquals(expectedCharge,reading.get(15,TimeUnit.SECONDS),"Parent, child and funding sources must share the pre-commit snapshot");
            doCallRealMethod().when(f.chargeRepository).parent(1,key);
            var conflict=service.read(f.owner.getEmail(),key);
            assertEquals(ChargeEvidenceReconciler.Status.MISMATCH,conflict.comparison().status());assertNull(conflict.comparison().settledEffect());
            f.db.update("UPDATE settlement_allocation_events SET amount=20 WHERE user_id=1 AND event_key=? AND allocation_id='charge-cash'",reserve);
            assertEquals(expectedCharge,service.read(f.owner.getEmail(),key));
            var periods=f.context.getBean(JournalPeriodService.class);
            expectedChargePeriod=periods.read(f.owner.getEmail(),at,at.plusSeconds(1));
            assertEquals(1,expectedChargePeriod.entries().size());
            assertEquals(com.example.investsmart.dto.PeriodCoverageAssessment.Status.ASSESSED,expectedChargePeriod.assessment().status());
            assertEquals(1,expectedChargePeriod.assessment().groupedCharges());
            assertFalse(expectedChargePeriod.assessment().fullAccountSummaryAvailable());
            assertEquals(UNAVAILABLE,eligibility(expectedChargePeriod,SELECTED_JOURNAL_SETTLED_CHANGE).status());
            assertTrue(eligibility(expectedChargePeriod,SELECTED_JOURNAL_RESERVED_CHANGE).reasons().contains(PeriodSummaryEligibility.Reason.COMPOSITE_EFFECT_OUTSIDE_DIRECT_SCOPE));
            assertNull(directChange(expectedChargePeriod,SELECTED_JOURNAL_SETTLED_CHANGE));
            assertNull(directChange(expectedChargePeriod,SELECTED_JOURNAL_RESERVED_CHANGE));
            assertEquals(com.example.investsmart.dto.GroupedChargeEvidence.Status.GROUPED_SUPPLIED_ROWS,expectedChargePeriod.chargeGrouping().status());
            assertEquals(2,expectedChargePeriod.chargeGrouping().groups().getFirst().outsideSelectionChildKeys().size());
            assertNull(groupedChange(expectedChargePeriod,0));
            expectedCompleteChargePeriod=periods.read(f.owner.getEmail(),at,at.plusSeconds(2));
            assertEquals(new java.math.BigDecimal("-40.00"),groupedChange(expectedCompleteChargePeriod,0));
            assertEquals(new java.math.BigDecimal("0.00"),groupedChange(expectedCompleteChargePeriod,1));
            assertEquals(1,expectedChargePeriod.chargeGrouping().sourceEvidence().size());
            var retained=expectedChargePeriod.chargeGrouping().sourceEvidence().getFirst();
            assertEquals(expectedCharge,retained.sourceProof());
            assertEquals(2,retained.sourceProof().children().size());
            assertFalse(expectedChargePeriod.toString().contains("charge-private-extra"));
            assertTrue(periods.read(f.other.getEmail(),at,at.plusSeconds(1)).chargeGrouping().sourceEvidence().isEmpty());
            assertEquals(List.of("charge-private-extra"),periods.read(f.other.getEmail(),at,at.plusSeconds(1)).entries().stream().map(e->e.raw().eventKey()).toList());
            var candidatesRead=new CountDownLatch(1);var fundingCommitted=new CountDownLatch(1);
            doAnswer(call->{var rows=call.callRealMethod();candidatesRead.countDown();assertTrue(fundingCommitted.await(15,TimeUnit.SECONDS));return rows;})
                .when(f.periodChargeRepository).children(eq(1L),anyList(),anyInt());
            var periodReading=pool.submit(()->periods.read(f.owner.getEmail(),at,at.plusSeconds(1)));
            try {
                assertTrue(candidatesRead.await(15,TimeUnit.SECONDS));
                f.write.executeWithoutResult(status->f.db.update("UPDATE settlement_allocation_events SET amount=21 WHERE user_id=1 AND event_key=? AND allocation_id='charge-cash'",reserve));
            }finally{fundingCommitted.countDown();}
            assertEquals(expectedChargePeriod,periodReading.get(15,TimeUnit.SECONDS),"Period rows, child candidates and funding must share one pre-commit snapshot");
            doCallRealMethod().when(f.periodChargeRepository).children(eq(1L),anyList(),anyInt());
            var changed=periods.read(f.owner.getEmail(),at,at.plusSeconds(1));
            assertEquals(com.example.investsmart.dto.GroupedChargeEvidence.Status.INCOMPLETE,changed.chargeGrouping().status());
            assertTrue(changed.assessment().reasons().contains(com.example.investsmart.dto.PeriodCoverageAssessment.Reason.UNGROUPED_CHARGE));
            assertEquals(0,changed.assessment().groupedCharges());
            assertNull(groupedChange(periods.read(f.owner.getEmail(),at,at.plusSeconds(2)),0));
            assertTrue(changed.chargeGrouping().sourceEvidence().isEmpty());
            assertFalse(changed.assessment().fullAccountSummaryAvailable());
            assertEquals(1,changed.entries().size());assertEquals(com.example.investsmart.dto.GroupedChargeEvidence.Kind.UNGROUPED,changed.chargeGrouping().groups().getFirst().kind());
            f.db.update("UPDATE settlement_allocation_events SET amount=20 WHERE user_id=1 AND event_key=? AND allocation_id='charge-cash'",reserve);
            assertEquals(expectedChargePeriod,periods.read(f.owner.getEmail(),at,at.plusSeconds(1)));
            assertEquals(expectedCompleteChargePeriod,periods.read(f.owner.getEmail(),at,at.plusSeconds(2)));
        }
        byte[] backup=docker(Map.of(),null,"exec",NAME,"sh","-c",
                "MYSQL_PWD=\"$MYSQL_PASSWORD\" exec mysqldump --user=journal_test --no-tablespaces --single-transaction "+DATABASE).getBytes(StandardCharsets.UTF_8);
        assertTrue(backup.length>1000);docker(Map.of(),null,"restart",NAME);
        try(var f=new JournalReportFixture(readySource(),false)) {
            var service=f.context.getBean(ChargeEvidenceService.class);
            assertEquals(expectedCharge,service.read(f.owner.getEmail(),key));
            var periods=f.context.getBean(JournalPeriodService.class);
            assertEquals(expectedChargePeriod,periods.read(f.owner.getEmail(),at,at.plusSeconds(1)));
            assertEquals(expectedCompleteChargePeriod,periods.read(f.owner.getEmail(),at,at.plusSeconds(2)));
            f.db.update("DELETE FROM settlement_journal WHERE user_id=1 AND event_key=?",spend);
            var missing=service.read(f.owner.getEmail(),key);
            assertEquals(ChargeEvidenceReconciler.Status.INCOMPLETE,missing.comparison().status());assertNull(missing.comparison().impliedUnfundedAmount());
            var incompletePeriod=periods.read(f.owner.getEmail(),at,at.plusSeconds(1));
            assertEquals(com.example.investsmart.dto.GroupedChargeEvidence.Status.INCOMPLETE,incompletePeriod.chargeGrouping().status());
            assertTrue(incompletePeriod.assessment().reasons().contains(com.example.investsmart.dto.PeriodCoverageAssessment.Reason.UNGROUPED_CHARGE));
            assertTrue(incompletePeriod.chargeGrouping().sourceEvidence().isEmpty());
            assertNull(groupedChange(periods.read(f.owner.getEmail(),at,at.plusSeconds(2)),0));
            docker(Map.of(),backup,"exec","--interactive",NAME,"sh","-c","MYSQL_PWD=\"$MYSQL_PASSWORD\" exec mysql --user=journal_test "+DATABASE);
            assertEquals(expectedCharge,service.read(f.owner.getEmail(),key));
            assertEquals(expectedChargePeriod,periods.read(f.owner.getEmail(),at,at.plusSeconds(1)));
            assertEquals(expectedCompleteChargePeriod,periods.read(f.owner.getEmail(),at,at.plusSeconds(2)));
            assertThrows(com.example.investsmart.exception.ResourceNotFoundException.class,()->service.read(f.other.getEmail(),key));
            assertEquals(new java.math.BigDecimal("1234.57"),f.db.queryForObject("SELECT virtual_balance FROM users WHERE id=1",java.math.BigDecimal.class));
        }
    }

    private static PeriodSummaryEligibility.Bucket eligibility(JournalPeriodEvidence period,PeriodSummaryEligibility.Scope scope){
        return period.summaryEligibility().buckets().stream().filter(bucket->bucket.scope()==scope).findFirst().orElseThrow();
    }
    private static java.math.BigDecimal directChange(JournalPeriodEvidence period,PeriodSummaryEligibility.Scope scope){
        return period.selectedDirectChanges().buckets().stream().filter(bucket->bucket.scope()==scope).findFirst().orElseThrow().change();
    }
    private static java.math.BigDecimal groupedChange(JournalPeriodEvidence period,int bucket){return period.selectedGroupedChanges().buckets().get(bucket).change();}

    private static long pairedGroups(com.example.investsmart.dto.CombinedTradeEvidence evidence) {
        return evidence.groups().stream().filter(group->group.records().size()==2).count();
    }

    private static void intradayTicket(JournalReportFixture f,String id,String side,int units,String fee) {
        var at=java.sql.Timestamp.from(JournalReportFixture.AT);
        f.db.update("""
                INSERT INTO paper_tickets(id,user_id,request_id,stock_id,position_id,side,product,variety,order_kind,
                validity,quantity,filled_units,legs,filled_legs,lot_size,ceiling_price,reserved_cash,status,fees,policy_version,expires_at,created_at)
                VALUES(?,1,?,1,'intraday',?,'MIS','REGULAR','MARKET','DAY',?,?,1,1,1,100,0,'FILLED',?,'test',?,?)
                """,id,id,side,units,units,new java.math.BigDecimal(fee),at,at);
        f.db.update("INSERT INTO paper_fills(id,ticket_id,user_id,leg_number,quantity,price,fee,created_at) VALUES(?,?,1,0,?,100,?,?)",
                "fill-"+id,id,units,new java.math.BigDecimal(fee),at);
    }

    static DriverManagerDataSource readySource() throws Exception {
        String binding=docker(Map.of(),null,"port",NAME,"3306/tcp").trim();
        if(!binding.matches("127\\.0\\.0\\.1:[0-9]{1,5}"))throw new IllegalStateException("Synthetic container must bind loopback only");
        String port=binding.substring(binding.lastIndexOf(':')+1);
        var source=new DriverManagerDataSource("jdbc:mysql://127.0.0.1:"+port+"/"+DATABASE
                +"?allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC", "journal_test",PASSWORD);
        long end=System.nanoTime()+Duration.ofSeconds(60).toNanos();
        while(System.nanoTime()<end) {
            try(Connection connection=source.getConnection()) {
                if(connection.isValid(2))return source;
            } catch(java.sql.SQLException notReady) { /* bounded readiness; never print connection credentials */ }
            Thread.sleep(500);
        }
        throw new IllegalStateException("Synthetic MySQL did not become ready");
    }

    private static String docker(Map<String,String> environment,byte[] input,String... args) throws Exception {
        var command=new ArrayList<String>();command.add("docker");command.addAll(List.of(args));
        var builder=new ProcessBuilder(command).redirectErrorStream(true);builder.environment().putAll(environment);
        var process=builder.start();
        try {
            if(input!=null)process.getOutputStream().write(input);
            process.getOutputStream().close();
            var output=CompletableFuture.supplyAsync(()->{
                try{return process.getInputStream().readAllBytes();}
                catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}
            });
            if(!process.waitFor(90,TimeUnit.SECONDS))throw new IllegalStateException("Synthetic Docker operation timed out");
            byte[] bytes=output.get(10,TimeUnit.SECONDS);
            if(process.exitValue()!=0)throw new IllegalStateException("Synthetic Docker operation failed (exit "+process.exitValue()+")");
            return new String(bytes,StandardCharsets.UTF_8);
        } finally { if(process.isAlive())process.destroyForcibly(); }
    }
}
