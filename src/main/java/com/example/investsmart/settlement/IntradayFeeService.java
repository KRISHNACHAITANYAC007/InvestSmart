package com.example.investsmart.settlement;

import com.example.investsmart.dto.IntradayFeeEvidence;
import com.example.investsmart.exception.*;
import com.example.investsmart.repository.UserRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.example.investsmart.dto.IntradayFeeEvidence.Verification.*;

@Service
public class IntradayFeeService {
    private final UserRepository users;
    private final IntradayEvidenceRepository evidence;
    private final IntradayEvidenceReconciler reconciler;
    private final IntradayFeeRepository fees;
    public IntradayFeeService(UserRepository users,IntradayEvidenceRepository evidence,
                              IntradayEvidenceReconciler reconciler,IntradayFeeRepository fees) {
        this.users=users;this.evidence=evidence;this.reconciler=reconciler;this.fees=fees;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,propagation=Propagation.REQUIRES_NEW)
    public IntradayFeeEvidence read(String authenticatedEmail,String positionId) {
        if(positionId==null||positionId.isBlank()||positionId.length()>36)throw new BadRequestException("Invalid intraday position.");
        if(authenticatedEmail==null||authenticatedEmail.isBlank())throw missing();
        var user=users.findByEmailIgnoreCase(authenticatedEmail).orElseThrow(IntradayFeeService::missing);
        user.requireActive();
        var position=evidence.owned(user.getId(),positionId);
        if(position==null)throw missing();
        var events=evidence.events(user.getId(),positionId);
        var comparison=reconciler.reconcile(position,events,events.size()>1000);
        if(comparison.verification()!=IntradayEvidenceReconciler.Verification.MATCHED_POSITION_TOTALS)
            return new IntradayFeeEvidence(positionId,comparison,
                    comparison.verification()==IntradayEvidenceReconciler.Verification.MISMATCH?MISMATCH:INCOMPLETE,
                    "POSITION_EVIDENCE_UNVERIFIED",null);
        var links=fees.links(user.getId(),positionId);
        if(links.size()>1000)return new IntradayFeeEvidence(positionId,comparison,INCOMPLETE,"DETAIL_LIMIT",null);
        var byKey=new HashMap<String,IntradayEvidenceReconciler.Event>();
        events.forEach(event->byKey.put(event.key(),event));
        var seen=new HashSet<String>();
        BigDecimal sum=new BigDecimal("0.00");
        for(var link:links) {
            var event=byKey.get(link.eventKey());
            if(event==null||!seen.add(link.eventKey()))return new IntradayFeeEvidence(positionId,comparison,MISMATCH,"EVENT_FILL_COVERAGE_CONFLICT",null);
            if(link.ticketId()==null)return new IntradayFeeEvidence(positionId,comparison,INCOMPLETE,"TICKET_MISSING",null);
            if(!link.validTicket())return new IntradayFeeEvidence(positionId,comparison,MISMATCH,"TICKET_CONFLICT",null);
            if(link.fillId()==null)return new IntradayFeeEvidence(positionId,comparison,INCOMPLETE,"FILL_MISSING",null);
            if(link.fillUser()!=user.getId()||link.fillUnits()!=event.units()||link.fillLeg()!=0
                    ||link.ticketFee()==null||link.fillFee()==null||link.ticketFee().compareTo(event.fee())!=0
                    ||link.fillFee().compareTo(event.fee())!=0)
                return new IntradayFeeEvidence(positionId,comparison,MISMATCH,"FEE_OR_FILL_CONFLICT",null);
            sum=sum.add(event.fee());
        }
        if(seen.size()!=events.size())return new IntradayFeeEvidence(positionId,comparison,INCOMPLETE,"LINK_COVERAGE_MISSING",null);
        return new IntradayFeeEvidence(positionId,comparison,MATCHED_EVENT_FEES,"EVENT_ORDER_FILL_FEES_AGREE",sum);
    }
    private static ResourceNotFoundException missing(){return new ResourceNotFoundException("Intraday fee evidence not found");}
}
