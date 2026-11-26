package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import static com.example.investsmart.dto.PeriodCoverageAssessment.*;

/** Supplied-evidence diagnostics only: no database, authentication, money sums or summary eligibility grant. */
@Component
public class PeriodCoverageAssessor {
    private final JournalActionClassifier actions;
    private final FundingEffectClassifier effects;
    public PeriodCoverageAssessor(JournalActionClassifier actions,FundingEffectClassifier effects){this.actions=actions;this.effects=effects;}
    public PeriodCoverageAssessment assess(JournalPeriodEvidence period){
        var reasons=EnumSet.of(Reason.HISTORY_NOT_CERTIFIED);
        if(period==null)return unavailable(Status.INPUT_UNAVAILABLE,reasons,Reason.INPUT_MISSING);
        if(period.entries().size()>1000)return unavailable(Status.INPUT_UNAVAILABLE,reasons,Reason.INPUT_LIMIT);
        if(period.fromInclusive()==null||period.toExclusive()==null||!period.fromInclusive().isBefore(period.toExclusive())
            ||period.fromInclusive().getNano()!=0||period.toExclusive().getNano()!=0
            ||period.fromInclusive().isBefore(Instant.parse("1970-01-01T00:00:01Z"))||period.toExclusive().isAfter(Instant.parse("2038-01-19T03:14:07Z"))
            ||Duration.between(period.fromInclusive(),period.toExclusive()).compareTo(Duration.ofDays(366))>0)
            return unavailable(Status.INPUT_UNAVAILABLE,reasons,Reason.INVALID_PERIOD);
        var coverage=period.initialization();
        if(coverage==null)return unavailable(Status.INPUT_UNAVAILABLE,reasons,Reason.INPUT_MISSING);
        if(coverage.initialized()!=(coverage.initializedAt()!=null)||period.completeFinancialHistory()
            ||period.startsBeforeInitialization()!=(coverage.initialized()&&period.fromInclusive().isBefore(coverage.initializedAt())))
            return unavailable(Status.CONTRADICTORY_EVIDENCE,reasons,Reason.CONTRADICTORY_EVIDENCE);
        if(!coverage.initialized())reasons.add(Reason.ACCOUNT_NOT_INITIALIZED);
        if(period.startsBeforeInitialization())reasons.add(Reason.RANGE_BEFORE_INITIALIZATION);
        if(period.truncated())reasons.add(Reason.JOURNAL_TRUNCATED);
        if(period.fundingEvidenceTruncated())reasons.add(Reason.FUNDING_TRUNCATED);
        var rows=new LinkedHashMap<String,JournalPeriodEvidence.Entry>();
        int unsupported=0,invalid=0,missing=0,conflicting=0,refunds=0;long sourceCount=0;
        for(var entry:period.entries()){
            if(entry==null||entry.raw()==null||entry.raw().eventKey()==null||entry.raw().eventKey().isBlank()||entry.raw().eventKey().length()>160||entry.raw().eventKey().chars().anyMatch(Character::isISOControl)
                ||entry.raw().occurredAt()==null||entry.raw().occurredAt().isBefore(period.fromInclusive())||!entry.raw().occurredAt().isBefore(period.toExclusive())
                ||rows.putIfAbsent(entry.raw().eventKey(),entry)!=null)
                return unavailable(Status.CONTRADICTORY_EVIDENCE,reasons,Reason.CONTRADICTORY_EVIDENCE);
            var raw=entry.raw();var interpreted=actions.classify(raw.action(),raw.amount(),raw.referenceKind(),raw.referenceId());
            if(!interpreted.equals(entry.interpretation())||entry.funding()==null||!raw.equals(entry.funding().journal())
                ||!effects.classify(entry.funding()).equals(entry.fundingEffect()))
                return unavailable(Status.CONTRADICTORY_EVIDENCE,reasons,Reason.CONTRADICTORY_EVIDENCE);
            sourceCount+=entry.funding().sources().size();if(sourceCount>1000)return unavailable(Status.INPUT_UNAVAILABLE,reasons,Reason.INPUT_LIMIT);
            if(interpreted.kind()==JournalActionClassifier.Kind.UNSUPPORTED){unsupported++;reasons.add(Reason.UNSUPPORTED_ACTION);}
            if(interpreted.kind()==JournalActionClassifier.Kind.INVALID){invalid++;reasons.add(Reason.INVALID_ACTION);}
            if(FundingEvidenceChecker.FUNDING.contains(raw.action()==null?"":raw.action())){
                if(entry.funding().verification()==SettlementFundingPage.Verification.MISMATCH||entry.fundingEffect().coverage()==FundingEffectClassifier.Coverage.MISMATCH){conflicting++;reasons.add(Reason.CONFLICTING_FUNDING);}
                else if(entry.funding().verification()!=SettlementFundingPage.Verification.MATCHED||entry.fundingEffect().coverage()==FundingEffectClassifier.Coverage.INCOMPLETE){missing++;reasons.add(Reason.MISSING_FUNDING);}
            }
            if(entry.fundingEffect().coverage()==FundingEffectClassifier.Coverage.UNKNOWN_CREDIT_REFUND_DESTINATION){refunds++;reasons.add(Reason.UNKNOWN_REFUND_DESTINATION);}
        }
        Integer grouped=null,ungrouped=null;var grouping=period.chargeGrouping();
        if(grouping!=null&&grouping.status()==null)return contradiction(reasons);
        if(grouping==null||grouping.status()==GroupedChargeEvidence.Status.INCOMPLETE&&grouping.groups().isEmpty()&&!rows.isEmpty())reasons.add(Reason.GROUPING_UNAVAILABLE);
        else{
            if(grouping.groups().size()>1000)return unavailable(Status.INPUT_UNAVAILABLE,reasons,Reason.INPUT_LIMIT);
            if(grouping.status()==GroupedChargeEvidence.Status.INCOMPLETE)reasons.add(Reason.GROUPING_INCOMPLETE);
            var seen=new HashSet<String>();grouped=0;ungrouped=0;int context=0;
            for(var group:grouping.groups()){
                context+=group.outsideSelectionChildKeys().size();if(context>1000)return unavailable(Status.INPUT_UNAVAILABLE,reasons,Reason.INPUT_LIMIT);
                if(group.kind()==null||group.selectedRows().isEmpty()||!Objects.equals(group.representativeKey(),group.selectedRows().getFirst().eventKey()))return contradiction(reasons);
                for(var raw:group.selectedRows())if(raw==null||!rows.containsKey(raw.eventKey())||!raw.equals(rows.get(raw.eventKey()).raw())||!seen.add(raw.eventKey()))return contradiction(reasons);
                if(group.kind()==GroupedChargeEvidence.Kind.UNGROUPED){
                    if(group.selectedRows().size()!=1||!group.outsideSelectionChildKeys().isEmpty())return contradiction(reasons);
                    var raw=group.selectedRows().getFirst();if(chargeRow(raw)){ungrouped++;reasons.add(Reason.UNGROUPED_CHARGE);}
                }else{
                    var parent=group.selectedRows().getFirst();if(!"REALIZED_CHARGE".equals(parent.action())||rows.get(parent.eventKey()).interpretation().kind()!=JournalActionClassifier.Kind.COMPOSITE_CHARGE)return contradiction(reasons);
                    String ref=ChargeEvidenceReconciler.reference(parent.eventKey());var expected=new HashSet<>(List.of("CHARGE_RESERVE:"+ref,"CHARGE_SPEND:"+ref));
                    for(var raw:group.selectedRows().subList(1,group.selectedRows().size()))if(!expected.remove(raw.eventKey())||!(raw.eventKey().equals("CHARGE_RESERVE:"+ref)?"RESERVE":"SPEND").equals(raw.action())||!"CHARGE".equals(raw.referenceKind())||!ref.equals(raw.referenceId())||rows.get(raw.eventKey()).funding().verification()!=SettlementFundingPage.Verification.MATCHED||rows.get(raw.eventKey()).fundingEffect().coverage()!=FundingEffectClassifier.Coverage.DIRECT_EFFECT)return contradiction(reasons);
                    if(!expected.equals(new HashSet<>(group.outsideSelectionChildKeys()))||expected.size()!=group.outsideSelectionChildKeys().size()||expected.stream().anyMatch(rows::containsKey))return contradiction(reasons);
                    grouped++;
                }
            }
            if(seen.size()!=rows.size())return contradiction(reasons);
        }
        return new PeriodCoverageAssessment(Status.ASSESSED,rows.size(),unsupported,invalid,missing,conflicting,refunds,grouped,ungrouped,false,new ArrayList<>(reasons));
    }
    private static boolean chargeRow(SettlementJournalPage.Entry row){return "REALIZED_CHARGE".equals(row.action())||"CHARGE".equals(row.referenceKind())||row.eventKey().startsWith("CHARGE_RESERVE:")||row.eventKey().startsWith("CHARGE_SPEND:");}
    private static PeriodCoverageAssessment contradiction(EnumSet<Reason> reasons){return unavailable(Status.CONTRADICTORY_EVIDENCE,reasons,Reason.CONTRADICTORY_EVIDENCE);}
    private static PeriodCoverageAssessment unavailable(Status status,EnumSet<Reason> reasons,Reason reason){reasons.add(reason);return new PeriodCoverageAssessment(status,null,null,null,null,null,null,null,null,false,new ArrayList<>(reasons));}
}
