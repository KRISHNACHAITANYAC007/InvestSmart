package com.example.investsmart.settlement;

import com.example.investsmart.dto.FinancePeriodEvidence;
import com.example.investsmart.exception.*;
import com.example.investsmart.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class FinancePeriodService {
    private final UserRepository users;
    private final SettlementJournalRepository journal;
    private final FinancePeriodRepository events;
    public FinancePeriodService(UserRepository users,SettlementJournalRepository journal,FinancePeriodRepository events){this.users=users;this.journal=journal;this.events=events;}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,propagation=Propagation.REQUIRES_NEW)
    public FinancePeriodEvidence read(String authenticatedEmail,Instant from,Instant to){
        if(from==null||to==null||!from.isBefore(to)||from.getNano()!=0||to.getNano()!=0
            ||from.isBefore(Instant.parse("1970-01-01T00:00:01Z"))||to.isAfter(Instant.parse("2038-01-19T03:14:07Z"))
            ||Duration.between(from,to).compareTo(Duration.ofDays(366))>0)throw new BadRequestException("Use an increasing whole-second UTC range of at most 366 days within the supported timestamp range.");
        if(authenticatedEmail==null||authenticatedEmail.isBlank())throw missing();
        var user=users.findByEmailIgnoreCase(authenticatedEmail).orElseThrow(FinancePeriodService::missing);user.requireActive();
        var coverage=journal.coverage(user.getId());var rows=events.entries(user.getId(),from,to);
        boolean before=coverage.initialized()&&from.isBefore(coverage.initializedAt()),truncated=rows.size()>1000;
        var selected=rows.stream().limit(1000).toList();var limitations=new LinkedHashSet<String>();
        limitations.add("Owned financing-event rows only; no complete history, period expense total, current interest reconciliation or balances.");
        limitations.add("Contribution, principal change, interest accrued and interest paid are separate meanings; never add current snapshots to events.");
        limitations.add("Timestamp/key order is stable in this snapshot, not causal order or a cursor across requests.");
        if(!coverage.initialized())limitations.add("ACCOUNT_NOT_INITIALIZED");if(before)limitations.add("RANGE_STARTS_BEFORE_INITIALIZATION");if(truncated)limitations.add("ROW_LIMIT_EXCEEDED_FIRST_1000_ONLY");
        for(var row:selected){
            if(row.ownership()==FinancePeriodEvidence.Ownership.POSITION_UNAVAILABLE)limitations.add("POSITION_EVIDENCE_UNAVAILABLE");
            else if(row.interpretation().kind()==FinanceEventClassifier.Kind.UNSUPPORTED)limitations.add("UNSUPPORTED_EVENT_TYPE");
            else if(row.interpretation().kind()==FinanceEventClassifier.Kind.INVALID)limitations.add("INVALID_EVENT_VALUES");
        }
        return new FinancePeriodEvidence(from,to,coverage,before,false,truncated,selected,new ArrayList<>(limitations));
    }
    private static ResourceNotFoundException missing(){return new ResourceNotFoundException("Financing period evidence not found");}
}
