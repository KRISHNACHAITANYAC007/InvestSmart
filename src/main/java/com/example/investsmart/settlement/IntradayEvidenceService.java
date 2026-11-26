package com.example.investsmart.settlement;

import com.example.investsmart.dto.IntradayPositionEvidence;
import com.example.investsmart.exception.*;
import com.example.investsmart.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Internal read. Callers supply server-authenticated identity, never model/request account selectors. */
@Service
public class IntradayEvidenceService {
    private final UserRepository users;
    private final IntradayEvidenceRepository evidence;
    private final IntradayEvidenceReconciler reconciler;
    public IntradayEvidenceService(UserRepository users, IntradayEvidenceRepository evidence, IntradayEvidenceReconciler reconciler) {
        this.users=users; this.evidence=evidence; this.reconciler=reconciler;
    }
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ, propagation=Propagation.REQUIRES_NEW)
    public IntradayPositionEvidence read(String authenticatedEmail, String positionId) {
        if(positionId==null||positionId.isBlank()||positionId.length()>36) throw new BadRequestException("Invalid intraday position.");
        if(authenticatedEmail==null||authenticatedEmail.isBlank()) throw missing();
        var user=users.findByEmailIgnoreCase(authenticatedEmail).orElseThrow(IntradayEvidenceService::missing);
        user.requireActive();
        var position=evidence.owned(user.getId(),positionId);
        if(position==null) throw missing();
        var events=evidence.events(user.getId(),positionId);
        return new IntradayPositionEvidence(positionId,reconciler.reconcile(position,events,events.size()>1000));
    }
    private static ResourceNotFoundException missing() { return new ResourceNotFoundException("Intraday evidence not found"); }
}
