package com.example.investsmart.reports;

import com.example.investsmart.dto.*;
import java.math.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class JournalPeriodReportMapper {
    private static final Set<String> ACTIONS=Set.of("SETTLED_CREDIT","WITHDRAW","PENDING_CREDIT","RESERVE","SPEND","RELEASE","SETTLE","REALIZED_CHARGE","LINK_DELIVERIES");
    private static final Set<String> REASONS;
    static {
        var reasons=new HashSet<String>();for(var reason:PeriodSummaryEligibility.Reason.values())reasons.add(reason.name());
        reasons.addAll(Set.of("GROUP_SOURCE_UNAVAILABLE","GROUP_SOURCE_LIMIT","GROUP_SOURCE_CONFLICT","GLOBAL_GROUP_PARTITION_CONFLICT","CHARGE_CHILD_OUTSIDE_PERIOD","MALFORMED_GROUP_SOURCE","INVALID_GROUP_CHANGE","GROUP_CHANGE_LIMIT","DIRECT_CHANGE_UNAVAILABLE_OR_INVALID","SELECTED_COMPLETE_GROUPS_ONLY_NO_ACCOUNT_TOTAL","EVIDENCE_UNAVAILABLE"));REASONS=Set.copyOf(reasons);
    }
    public JournalPeriodReport map(JournalPeriodEvidence period){
        Objects.requireNonNull(period);var coverage=period.initialization();boolean initialized=coverage!=null&&coverage.initialized();
        boolean safe=initialized&&!period.startsBeforeInitialization()&&!period.truncated()&&!period.fundingEvidenceTruncated();
        var rows=new ArrayList<JournalPeriodReport.Row>();
        for(var entry:period.entries().stream().limit(1000).toList()){
            var raw=entry.raw();var interpretation=entry.interpretation();String kind=interpretation==null?"UNSUPPORTED":interpretation.kind().name();
            String action=ACTIONS.contains(raw.action()==null?"":raw.action())?raw.action():"UNKNOWN";
            boolean invalid="INVALID".equals(kind);String amount=invalid?null:decimal(raw.amount(),19);
            if(raw.amount()!=null&&raw.amount().signum()<0)amount=null;
            rows.add(new JournalPeriodReport.Row(rows.size()+1,raw.occurredAt(),action,amount,kind,
                List.of(invalid||amount==null?"INVALID_ACTION":"UNSUPPORTED".equals(kind)?"UNSUPPORTED_ACTION":"SELECTED_ROW_ONLY")));
        }
        var direct=new ArrayList<JournalPeriodReport.Change>();var grouped=new ArrayList<JournalPeriodReport.Change>();
        for(boolean settled:List.of(true,false)){
            String directScope=settled?"SELECTED_JOURNAL_SETTLED_CHANGE":"SELECTED_JOURNAL_RESERVED_CHANGE";
            var d=period.selectedDirectChanges()==null?null:period.selectedDirectChanges().buckets().stream().filter(b->b.scope().name().equals(directScope)).findFirst().orElse(null);
            direct.add(change(directScope,safe&&d!=null&&d.status()==SelectedJournalChangeSummary.Status.VERIFIED_SUPPLIED_CHANGE,d==null?null:d.change(),d==null?List.of():d.reasons()));
            String groupedScope=settled?"SELECTED_GROUPED_SETTLED_CHANGE":"SELECTED_GROUPED_RESERVED_CHANGE";
            var g=period.selectedGroupedChanges()==null?null:period.selectedGroupedChanges().buckets().stream().filter(b->b.scope().name().equals(groupedScope)).findFirst().orElse(null);
            grouped.add(change(groupedScope,safe&&g!=null&&g.status()==GroupedJournalChangeSummary.Status.VERIFIED_SUPPLIED_GROUPED_CHANGE,g==null?null:g.change(),g==null?List.of():g.reasons()));
        }
        return new JournalPeriodReport(period.fromInclusive(),period.toExclusive(),new JournalPeriodReport.Coverage(initialized,coverage==null?null:coverage.initializedAt(),period.startsBeforeInitialization(),false),
            new JournalPeriodReport.Limits(1000,period.truncated(),period.fundingEvidenceTruncated()),rows,direct,grouped,
            List.of(new JournalPeriodReport.Unavailable("COMPLETE_ACCOUNT_EXPENSES","UNAVAILABLE",List.of("HISTORY_NOT_CERTIFIED","OTHER_PRODUCT_COVERAGE_UNVERIFIED")),new JournalPeriodReport.Unavailable("ACCOUNT_PROFIT_LOSS","UNAVAILABLE",List.of("HISTORY_NOT_CERTIFIED","PORTFOLIO_METHOD_NOT_RECONCILED")),new JournalPeriodReport.Unavailable("HISTORICAL_BALANCES","UNAVAILABLE",List.of("HISTORY_NOT_CERTIFIED"))));
    }
    private static JournalPeriodReport.Change change(String scope,boolean eligible,BigDecimal amount,List<String> reasons){
        boolean known=reasons.stream().allMatch(r->r!=null&&REASONS.contains(r));
        String value=eligible&&known?decimal(amount,22):null;var safeReasons=new LinkedHashSet<String>();
        for(String reason:reasons)safeReasons.add(REASONS.contains(reason==null?"":reason)?reason:"EVIDENCE_UNAVAILABLE");
        if(value==null)safeReasons.add("EVIDENCE_UNAVAILABLE");
        return new JournalPeriodReport.Change(scope,value==null?"UNAVAILABLE":"VERIFIED_SUPPLIED_CHANGE",value,new ArrayList<>(safeReasons));
    }
    private static String decimal(BigDecimal value,int precision){if(value==null)return null;try{var scaled=value.setScale(2,RoundingMode.UNNECESSARY);return scaled.precision()<=precision?scaled.toPlainString():null;}catch(ArithmeticException invalid){return null;}}
}
