package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import com.example.investsmart.exception.*;
import com.example.investsmart.repository.UserRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ChargeEvidenceService {
    private final UserRepository users;
    private final ChargeEvidenceRepository charges;
    private final SettlementFundingRepository funding;
    private final ChargeEvidenceReconciler reconciler;
    public ChargeEvidenceService(UserRepository users,ChargeEvidenceRepository charges,SettlementFundingRepository funding,ChargeEvidenceReconciler reconciler){
        this.users=users;this.charges=charges;this.funding=funding;this.reconciler=reconciler;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,propagation=Propagation.REQUIRES_NEW)
    public ChargeEvidence read(String authenticatedEmail,String eventKey){
        if(eventKey==null||eventKey.isBlank()||eventKey.length()>160||eventKey.chars().anyMatch(Character::isISOControl))throw new BadRequestException("Invalid charge event key.");
        if(authenticatedEmail==null||authenticatedEmail.isBlank())throw missing();
        var user=users.findByEmailIgnoreCase(authenticatedEmail).orElseThrow(ChargeEvidenceService::missing);user.requireActive();
        var parent=charges.parent(user.getId(),eventKey);if(parent==null)throw missing();
        var initial=reconciler.reconcile(parent,List.of(),false);
        if(initial.status()!=ChargeEvidenceReconciler.Status.INCOMPLETE)return new ChargeEvidence(parent,List.of(),false,false,initial);
        var rows=charges.children(user.getId(),ChargeEvidenceReconciler.reference(eventKey));
        boolean childrenLimited=rows.size()>1000;
        var selected=rows.stream().limit(1000).toList();
        // A valid group has exactly2 children; avoid source queries for missing/ambiguous groups.
        var evidence=selected.size()==2&&!childrenLimited?funding.evidence(user.getId(),selected.stream().map(SettlementJournalPage.Entry::eventKey).toList()):List.<SettlementFundingRepository.Evidence>of();
        boolean fundingLimited=evidence.size()>1000;
        var grouped=evidence.stream().collect(java.util.stream.Collectors.groupingBy(SettlementFundingRepository.Evidence::eventKey));
        var children=selected.stream().map(row->FundingEvidenceChecker.check(row,grouped.getOrDefault(row.eventKey(),List.of()),fundingLimited)).toList();
        return new ChargeEvidence(parent,children,childrenLimited,fundingLimited,reconciler.reconcile(parent,children,childrenLimited||fundingLimited));
    }
    private static ResourceNotFoundException missing(){return new ResourceNotFoundException("Charge evidence not found");}
}
