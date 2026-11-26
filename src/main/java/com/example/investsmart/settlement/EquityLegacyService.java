package com.example.investsmart.settlement;

import com.example.investsmart.dto.EquityLegacyEvidence;
import com.example.investsmart.exception.*;
import com.example.investsmart.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class EquityLegacyService {
    private final UserRepository users;
    private final EquityLegacyRepository records;
    public EquityLegacyService(UserRepository users,EquityLegacyRepository records){this.users=users;this.records=records;}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,propagation=Propagation.REQUIRES_NEW)
    public EquityLegacyEvidence read(String authenticatedEmail,String fillId){
        if(fillId==null||fillId.isBlank()||fillId.length()>36)throw new BadRequestException("Invalid fill.");
        if(authenticatedEmail==null||authenticatedEmail.isBlank())throw missing();
        var user=users.findByEmailIgnoreCase(authenticatedEmail).orElseThrow(EquityLegacyService::missing);user.requireActive();
        var fill=records.ownedFill(user.getId(),fillId);if(fill==null)throw missing();
        return EquityLegacyMatcher.match(fillId,fill,
                id->records.ownedOrder(user.getId(),id),records::equityReferences);
    }
    private static ResourceNotFoundException missing(){return new ResourceNotFoundException("Equity trade evidence not found");}
}
