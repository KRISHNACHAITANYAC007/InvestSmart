package com.example.investsmart.settlement;
import com.example.investsmart.dto.*;
import com.example.investsmart.exception.*;
import com.example.investsmart.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class PendingPolicyPeriodService {
    private final UserRepository users;private final SettlementJournalRepository journal;
    private final PendingPolicyPeriodRepository records;private final PendingCreditPolicyReconciler rules;
    public PendingPolicyPeriodService(UserRepository users,SettlementJournalRepository journal,PendingPolicyPeriodRepository records,PendingCreditPolicyReconciler rules){this.users=users;this.journal=journal;this.records=records;this.rules=rules;}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,propagation=Propagation.REQUIRES_NEW)
    public PendingPolicyPeriodEvidence read(String email,Instant from,Instant to){
        if(from==null||to==null||!from.isBefore(to)||from.getNano()!=0||to.getNano()!=0||from.isBefore(Instant.parse("1970-01-01T00:00:01Z"))||to.isAfter(Instant.parse("2038-01-19T03:14:07Z"))||Duration.between(from,to).compareTo(Duration.ofDays(366))>0)throw new BadRequestException("Invalid pending policy period.");
        if(email==null||email.isBlank())throw missing();var user=users.findByEmailIgnoreCase(email).orElseThrow(PendingPolicyPeriodService::missing);user.requireActive();
        var coverage=journal.coverage(user.getId());var rows=records.instructions(user.getId(),from,to);boolean selectedLimited=rows.size()>1000;
        var candidates=selectedLimited||rows.isEmpty()?List.<PendingCreditPolicyReconciler.Candidate>of():records.candidates(user.getId());boolean candidatesLimited=candidates.size()>1000;
        var prepared=rules.prepare(candidates,selectedLimited||candidatesLimited);
        var entries=rows.stream().limit(1000).map(row->new PendingPolicyPeriodEvidence.Entry(row.row(),prepared.reconcile(row.row(),row.fingerprint()))).toList();
        var limitations=new ArrayList<String>(List.of("Original owned pending-credit instruction policy only; no availability, refund destination, totals or complete history.","Fingerprint is corroboration, not authentication; no complete historical candidate coverage certified."));
        if(selectedLimited)limitations.add("SELECTED_INSTRUCTION_LIMIT");if(candidatesLimited)limitations.add("CANDIDATE_LIMIT");
        if(!coverage.initialized())limitations.add("ACCOUNT_NOT_INITIALIZED");else if(from.isBefore(coverage.initializedAt()))limitations.add("RANGE_STARTS_BEFORE_INITIALIZATION");
        return new PendingPolicyPeriodEvidence(from,to,coverage,false,selectedLimited,candidatesLimited,entries,limitations);
    }
    private static ResourceNotFoundException missing(){return new ResourceNotFoundException("Pending policy evidence not found");}
}
