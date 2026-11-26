package com.example.investsmart.settlement;

import com.example.investsmart.dto.JournalPeriodEvidence;
import com.example.investsmart.exception.*;
import com.example.investsmart.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class JournalPeriodService {
    private static final Instant MIN=Instant.parse("1970-01-01T00:00:01Z");
    private static final Instant MAX=Instant.parse("2038-01-19T03:14:07Z");
    private final UserRepository users;
    private final SettlementJournalRepository journal;
    private final JournalPeriodRepository periods;
    private final JournalActionClassifier classifier;
    private final SettlementFundingRepository funding;
    private final FundingEffectClassifier effects;
    private final PeriodChargeAdapter charges;
    private final PeriodCoverageAssessor coverageAssessor;
    private final PeriodSummaryEligibilityAssessor eligibility;
    private final SelectedJournalChangeSummarizer changes;
    private final GroupedJournalChangeSummarizer groupedChanges;
    public JournalPeriodService(UserRepository users,SettlementJournalRepository journal,JournalPeriodRepository periods,JournalActionClassifier classifier,SettlementFundingRepository funding,FundingEffectClassifier effects,PeriodChargeAdapter charges,PeriodCoverageAssessor coverageAssessor,PeriodSummaryEligibilityAssessor eligibility,SelectedJournalChangeSummarizer changes,GroupedJournalChangeSummarizer groupedChanges){
        this.users=users;this.journal=journal;this.periods=periods;this.classifier=classifier;this.funding=funding;this.effects=effects;this.charges=charges;this.coverageAssessor=coverageAssessor;
        this.eligibility=eligibility;
        this.changes=changes;this.groupedChanges=groupedChanges;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,propagation=Propagation.REQUIRES_NEW)
    public JournalPeriodEvidence read(String authenticatedEmail,Instant from,Instant to){
        // Whole seconds match the existing TIMESTAMP schema; avoid driver-dependent rounding.
        if(from==null||to==null||!from.isBefore(to)||from.getNano()!=0||to.getNano()!=0
                ||from.isBefore(MIN)||to.isAfter(MAX)||Duration.between(from,to).compareTo(Duration.ofDays(366))>0)
            throw new BadRequestException("Use an increasing range of whole-second UTC instants, at most 366 days, within the supported timestamp range.");
        if(authenticatedEmail==null||authenticatedEmail.isBlank())throw missing();
        var user=users.findByEmailIgnoreCase(authenticatedEmail).orElseThrow(JournalPeriodService::missing);user.requireActive();
        var coverage=journal.coverage(user.getId());
        var rows=periods.entries(user.getId(),from,to);
        boolean truncated=rows.size()>1000;
        boolean before=coverage.initialized()&&coverage.initializedAt()!=null&&from.isBefore(coverage.initializedAt());
        var selected=rows.stream().limit(1000).toList();
        var keys=selected.stream().filter(row->FundingEvidenceChecker.FUNDING.contains(row.action())).map(row->row.eventKey()).toList();
        var sourceRows=new ArrayList<SettlementFundingRepository.Evidence>();
        // At most10 batches/100 keys each, with a single global1001-row sentinel budget.
        for(int offset=0;offset<keys.size()&&sourceRows.size()<=1000;offset+=100)
            sourceRows.addAll(funding.evidence(user.getId(),keys.subList(offset,Math.min(offset+100,keys.size())),1001-sourceRows.size()));
        boolean fundingLimited=sourceRows.size()>1000;
        var grouped=sourceRows.stream().collect(java.util.stream.Collectors.groupingBy(SettlementFundingRepository.Evidence::eventKey));
        var entries=selected.stream().map(row->{
            var evidence=FundingEvidenceChecker.check(row,grouped.getOrDefault(row.eventKey(),List.of()),fundingLimited);
            return new JournalPeriodEvidence.Entry(row,classifier.classify(row.action(),row.amount(),row.referenceKind(),row.referenceId()),
                evidence,effects.classify(evidence));
        }).toList();
        var limitations=new ArrayList<String>();
        limitations.add("Selected direct and complete-group journal changes only; no complete financial history, opening/closing balance, account expense totals or reconciliation.");
        limitations.add("Timestamp/key order is stable within this snapshot, not proof of causal order or a cursor across requests.");
        if(!coverage.initialized())limitations.add("ACCOUNT_NOT_INITIALIZED");
        if(before)limitations.add("RANGE_STARTS_BEFORE_INITIALIZATION");
        if(fundingLimited)limitations.add("FUNDING_EVIDENCE_LIMIT_NO_SOURCE_EFFECTS");
        if(truncated)limitations.add("ROW_LIMIT_EXCEEDED_FIRST_1000_ONLY");
        var result=new JournalPeriodEvidence(from,to,coverage,before,false,truncated,fundingLimited,entries,limitations,charges.group(user.getId(),selected,truncated||fundingLimited));
        var assessed=result.withAssessment(coverageAssessor.assess(result));
        var eligible=assessed.withSummaryEligibility(eligibility.assess(assessed));
        var direct=eligible.withSelectedDirectChanges(changes.summarize(eligible));
        return direct.withSelectedGroupedChanges(groupedChanges.summarize(direct));
    }
    private static ResourceNotFoundException missing(){return new ResourceNotFoundException("Journal period evidence not found");}
}
