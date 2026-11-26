package com.example.investsmart.settlement;

import com.example.investsmart.dto.OptionLegacyEvidence;
import com.example.investsmart.exception.*;
import com.example.investsmart.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class OptionLegacyService {
    private final UserRepository users;private final OptionLegacyRepository records;
    public OptionLegacyService(UserRepository users,OptionLegacyRepository records){this.users=users;this.records=records;}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,propagation=Propagation.REQUIRES_NEW)
    public OptionLegacyEvidence read(String authenticatedEmail,String fillId){
        if(fillId==null||fillId.isBlank()||fillId.length()>36)throw new BadRequestException("Invalid fill.");
        if(authenticatedEmail==null||authenticatedEmail.isBlank())throw missing();
        var user=users.findByEmailIgnoreCase(authenticatedEmail).orElseThrow(OptionLegacyService::missing);user.requireActive();
        var fill=records.ownedFill(user.getId(),fillId);if(fill==null)throw missing();
        return OptionLegacyMatcher.match(fillId,fill,
                id->records.ownedOrder(user.getId(),id),records::optionReferences);
    }
    private static ResourceNotFoundException missing(){return new ResourceNotFoundException("Option trade evidence not found");}
}
