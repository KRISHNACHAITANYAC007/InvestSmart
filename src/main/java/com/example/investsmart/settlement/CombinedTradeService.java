package com.example.investsmart.settlement;

import com.example.investsmart.dto.*;
import com.example.investsmart.exception.ResourceNotFoundException;
import com.example.investsmart.repository.UserRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.example.investsmart.dto.CombinedTradeEvidence.*;

/** Internal identity evidence only: includes unexecuted legacy orders, not financial totals. */
@Service
public class CombinedTradeService {
    private final UserRepository users;
    private final CombinedTradeRepository records;
    private final TradeRecordCombiner combiner;
    public CombinedTradeService(UserRepository users,CombinedTradeRepository records,TradeRecordCombiner combiner) {
        this.users=users;this.records=records;this.combiner=combiner;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,propagation=Propagation.REQUIRES_NEW)
    public CombinedTradeEvidence read(String authenticatedEmail) {
        if(authenticatedEmail==null||authenticatedEmail.isBlank())throw missing();
        var user=users.findByEmailIgnoreCase(authenticatedEmail).orElseThrow(CombinedTradeService::missing);user.requireActive();
        var fills=records.fills(user.getId());
        var equities=records.equities(user.getId());
        var options=records.options(user.getId());
        if((long)fills.size()+equities.size()+options.size()>1000)
            return new CombinedTradeEvidence(Status.INCOMPLETE,List.of(),List.of("OWNED_RECORD_LIMIT_EXCEEDED"));
        var keys=new ArrayList<Key>();var equityById=new HashMap<Long,EquityLegacyRepository.Legacy>();
        var optionById=new HashMap<String,OptionLegacyRepository.Legacy>();
        var equityLinks=new HashSet<String>();var optionLinks=new HashSet<String>();
        for(var fill:fills) {
            keys.add(new Key(Source.PAPER_FILL,fill.id()));
            if(fill.ticketOwned()&&"CASH".equals(fill.equity().product())&&fill.equity().legacyId()!=null)
                (fill.equity().stock()==null?optionLinks:equityLinks).add(fill.equity().legacyId());
        }
        for(var order:equities){keys.add(new Key(Source.EQUITY_ORDER,Long.toString(order.id())));equityById.put(order.id(),order.value());}
        for(var order:options){keys.add(new Key(Source.OPTION_ORDER,order.id()));optionById.put(order.id(),order.value());}
        var equityRefs=records.references(equityLinks,true);var optionRefs=records.references(optionLinks,false);
        var equityProofs=new ArrayList<EquityLegacyEvidence>();var optionProofs=new ArrayList<OptionLegacyEvidence>();
        for(var fill:fills) {
            if(!fill.ticketOwned()) {
                equityProofs.add(new EquityLegacyEvidence(fill.id(),EquityLegacyEvidence.Verification.INCOMPLETE,"OWNED_TICKET_MISSING",null));
            } else if(fill.equity().stock()!=null) {
                equityProofs.add(EquityLegacyMatcher.match(fill.id(),fill.equity(),equityById::get,id->equityRefs.getOrDefault(id,List.of())));
            } else {
                optionProofs.add(OptionLegacyMatcher.match(fill.id(),fill.option(),optionById::get,id->optionRefs.getOrDefault(id,List.of())));
            }
        }
        var grouped=combiner.combine(keys,equityProofs,optionProofs,false);
        var limitations=new ArrayList<>(grouped.limitations());
        limitations.add("Owned identities read in one snapshot; legacy orders may be unexecuted. No period, amounts or complete financial-history claim.");
        return new CombinedTradeEvidence(grouped.status(),grouped.groups(),limitations);
    }
    private static ResourceNotFoundException missing(){return new ResourceNotFoundException("Trade evidence not found");}
}
