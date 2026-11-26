package com.example.investsmart.settlement;

import com.example.investsmart.entity.User;
import com.example.investsmart.repository.UserRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import static org.mockito.Mockito.*;

/** Synthetic JDBC fixture; no application configuration, schedulers or retained database access. */
final class JournalReportFixture implements AutoCloseable {
    @Configuration @EnableTransactionManagement
    static class Transactions {}

    final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    final JdbcTemplate db;
    final UserRepository users = mock(UserRepository.class);
    final SettlementJournalRepository repository;
    final SettlementJournalService service;
    final SettlementFundingService fundingService;
    final FinanceEventRepository financeRepository;
    final FinanceEventService financeService;
    final TicketFeeRepository feeRepository;
    final IntradayEvidenceRepository intradayRepository;
    final IntradayFeeRepository intradayFeeRepository;
    final EquityLegacyRepository legacyRepository;
    final OptionLegacyRepository optionLegacyRepository;
    final CombinedTradeRepository combinedRepository;
    final JournalPeriodRepository periodRepository;
    final SettlementFundingRepository fundingRepository;
    final ChargeEvidenceRepository chargeRepository;
    final PeriodChargeRepository periodChargeRepository;
    final PeriodCoverageAssessor coverageAssessor;
    final PeriodSummaryEligibilityAssessor eligibilityAssessor;
    final SelectedJournalChangeSummarizer changeSummarizer;
    final GroupedJournalChangeSummarizer groupedSummarizer;
    final FinancePeriodRepository financePeriodRepository;
    final ExecutionFeePeriodRepository executionFeeRepository;
    final PendingPolicyPeriodRepository pendingPolicyRepository;
    final TransactionTemplate write;
    final User owner = user(1L, "owner@example.invalid");
    final User other = user(2L, "other@example.invalid");
    static final Instant AT = Instant.parse("2026-11-23T05:00:00Z");

    JournalReportFixture(DataSource source, boolean initialize) {
        db = new JdbcTemplate(source);
        if (initialize) {
            db.execute("CREATE TABLE users(id BIGINT PRIMARY KEY,virtual_balance DECIMAL(19,2))");
            db.execute("CREATE TABLE holdings(id BIGINT PRIMARY KEY,user_id BIGINT,quantity INT)");
            new ResourceDatabasePopulator(
                    new ClassPathResource("db/migrations/20261111_paper_ticket.sql"),
                    new ClassPathResource("db/migrations/20261112_intraday.sql"),
                    new ClassPathResource("db/migrations/20261111_settlement_accounting.sql"),
                    new ClassPathResource("db/migrations/20261111_settlement_journal.sql")).execute(source);
            db.update("INSERT INTO users VALUES(1,1234.56),(2,789.12)");
            db.update("INSERT INTO holdings VALUES(1,1,7),(2,2,9)");
        }
        when(users.findByEmailIgnoreCase(anyString())).thenAnswer(call -> {
            String email = call.getArgument(0);
            if (owner.getEmail().equalsIgnoreCase(email)) return Optional.of(owner);
            if (other.getEmail().equalsIgnoreCase(email)) return Optional.of(other);
            return Optional.empty();
        });
        repository = spy(new SettlementJournalRepository(db));
        financeRepository = spy(new FinanceEventRepository(db, new FinanceEventClassifier()));
        feeRepository = spy(new TicketFeeRepository(db));
        intradayRepository = spy(new IntradayEvidenceRepository(db));
        intradayFeeRepository = spy(new IntradayFeeRepository(db));
        legacyRepository = spy(new EquityLegacyRepository(db));
        optionLegacyRepository = spy(new OptionLegacyRepository(db));
        combinedRepository = spy(new CombinedTradeRepository(db));
        periodRepository = spy(new JournalPeriodRepository(db));
        fundingRepository = spy(new SettlementFundingRepository(db));
        chargeRepository = spy(new ChargeEvidenceRepository(db));
        periodChargeRepository = spy(new PeriodChargeRepository(db));
        financePeriodRepository=spy(new FinancePeriodRepository(db,new FinanceEventClassifier()));
        executionFeeRepository=spy(new ExecutionFeePeriodRepository(db));
        pendingPolicyRepository=spy(new PendingPolicyPeriodRepository(db));
        var coverageActions=new JournalActionClassifier();
        coverageAssessor=spy(new PeriodCoverageAssessor(coverageActions,new FundingEffectClassifier(coverageActions)));
        eligibilityAssessor=spy(new PeriodSummaryEligibilityAssessor(coverageAssessor));
        changeSummarizer=spy(new SelectedJournalChangeSummarizer(eligibilityAssessor));
        var chargeRules=new ChargeEvidenceReconciler(coverageActions,new FundingEffectClassifier(coverageActions));
        var chargeGroups=new PeriodChargeGrouper(chargeRules);
        groupedSummarizer=spy(new GroupedJournalChangeSummarizer(eligibilityAssessor,chargeGroups,new ChargeGroupSourceReconciler(chargeGroups,chargeRules)));
        context.register(Transactions.class);
        context.registerBean(DataSource.class, () -> source);
        context.registerBean(JdbcTemplate.class, () -> db);
        context.registerBean(UserRepository.class, () -> users);
        context.registerBean(PlatformTransactionManager.class, () -> new DataSourceTransactionManager(source));
        context.registerBean(SettlementJournalRepository.class, () -> repository);
        context.registerBean(SettlementJournalService.class);
        context.registerBean(SettlementFundingRepository.class, () -> fundingRepository);
        context.registerBean(SettlementFundingService.class);
        context.registerBean(FinanceEventClassifier.class);
        context.registerBean(FinanceEventRepository.class, () -> financeRepository);
        context.registerBean(FinanceEventService.class);
        context.registerBean(TicketFeeRepository.class, () -> feeRepository);
        context.registerBean(TicketFeeService.class);
        context.registerBean(IntradayEvidenceReconciler.class);
        context.registerBean(IntradayEvidenceRepository.class, () -> intradayRepository);
        context.registerBean(IntradayEvidenceService.class);
        context.registerBean(IntradayFeeRepository.class, () -> intradayFeeRepository);
        context.registerBean(IntradayFeeService.class);
        context.registerBean(EquityLegacyRepository.class, () -> legacyRepository);
        context.registerBean(EquityLegacyService.class);
        context.registerBean(OptionLegacyRepository.class, () -> optionLegacyRepository);
        context.registerBean(OptionLegacyService.class);
        context.registerBean(CombinedTradeRepository.class, () -> combinedRepository);
        context.registerBean(TradeRecordCombiner.class);
        context.registerBean(CombinedTradeService.class);
        context.registerBean(JournalActionClassifier.class);
        context.registerBean(FundingEffectClassifier.class);
        context.registerBean(JournalPeriodRepository.class, () -> periodRepository);
        context.registerBean(JournalPeriodService.class);
        context.registerBean(ChargeEvidenceReconciler.class);
        context.registerBean(ChargeEvidenceRepository.class, () -> chargeRepository);
        context.registerBean(ChargeEvidenceService.class);
        context.registerBean(PeriodChargeRepository.class, () -> periodChargeRepository);
        context.registerBean(PeriodChargeGrouper.class);
        context.registerBean(PeriodChargeAdapter.class);
        context.registerBean(ChargeGroupSourceReconciler.class);
        context.registerBean(PeriodCoverageAssessor.class, () -> coverageAssessor);
        context.registerBean(PeriodSummaryEligibilityAssessor.class, () -> eligibilityAssessor);
        context.registerBean(SelectedJournalChangeSummarizer.class, () -> changeSummarizer);
        context.registerBean(GroupedJournalChangeSummarizer.class, () -> groupedSummarizer);
        context.registerBean(FinancePeriodRepository.class, () -> financePeriodRepository);
        context.registerBean(FinancePeriodService.class);
        context.registerBean(TicketFeeSliceReconciler.class);
        context.registerBean(IntradayTicketSliceReconciler.class);
        context.registerBean(ExecutionFeePeriodRepository.class, () -> executionFeeRepository);
        context.registerBean(ExecutionFeePeriodService.class);
        context.registerBean(PendingCreditPolicyReconciler.class);
        context.registerBean(PendingPolicyPeriodRepository.class,()->pendingPolicyRepository);
        context.registerBean(PendingPolicyPeriodService.class);
        context.refresh();
        service = context.getBean(SettlementJournalService.class);
        fundingService = context.getBean(SettlementFundingService.class);
        financeService = context.getBean(FinanceEventService.class);
        write = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
    }

    void account(long user) {
        db.update("INSERT INTO settlement_accounts VALUES(?,?,?,?)", user,
                new BigDecimal("1234.56"), new BigDecimal("1234.56"), Timestamp.from(AT));
    }

    void entry(long user, String key, String action, String amount, Instant at) {
        db.update("INSERT INTO settlement_journal VALUES(?,?,?,?,?,?,?,?)", user, key, action,
                "a".repeat(64), new BigDecimal(amount), null, null, Timestamp.from(at));
    }

    private static User user(long id, String email) {
        var result = new User(); result.setId(id); result.setEmail(email); return result;
    }

    @Override public void close() { context.close(); }
}
