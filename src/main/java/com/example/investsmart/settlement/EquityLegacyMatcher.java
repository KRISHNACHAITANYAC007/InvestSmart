package com.example.investsmart.settlement;

import com.example.investsmart.dto.EquityLegacyEvidence;
import java.math.*;
import java.util.List;
import java.util.function.Function;
import static com.example.investsmart.dto.EquityLegacyEvidence.Verification.*;

/** Shared rules; caller supplies owned records and reference evidence from one snapshot.
 * Lookup functions can use prefetched maps. This class starts no transaction or query. */
final class EquityLegacyMatcher {
    private EquityLegacyMatcher() {}
    static EquityLegacyEvidence match(String fillId,EquityLegacyRepository.Fill fill,
            Function<Long,EquityLegacyRepository.Legacy> orders,
            Function<String,List<String>> referencesById) {
        if(!"CASH".equals(fill.product())||fill.stock()==null)return new EquityLegacyEvidence(fillId,NOT_EVALUATED,"NOT_CASH_EQUITY",null);
        if(fill.legacyId()==null)return new EquityLegacyEvidence(fillId,INCOMPLETE,"LEGACY_LINK_MISSING",null);
        long legacyId;
        try{if(!fill.legacyId().matches("[1-9][0-9]{0,18}"))throw new NumberFormatException();legacyId=Long.parseLong(fill.legacyId());}
        catch(NumberFormatException invalid){return new EquityLegacyEvidence(fillId,MISMATCH,"INVALID_EQUITY_LINK",null);}
        var order=orders.apply(legacyId);
        if(order==null)return new EquityLegacyEvidence(fillId,INCOMPLETE,"OWNED_LEGACY_ORDER_MISSING",null);
        if(fill.stock()<1||fill.units()<1||!money(fill.price())||fill.price().signum()<=0||!money(order.price())||!money(order.total())
                ||!("BUY".equals(fill.side())||"SELL".equals(fill.side()))||!fill.side().equals(order.side())
                ||order.stock()!=fill.stock()||order.units()!=fill.units()||!"COMPLETED".equals(order.status())
                ||order.price().compareTo(fill.price())!=0
                ||order.total().compareTo(fill.price().multiply(BigDecimal.valueOf(fill.units())))!=0)
            return new EquityLegacyEvidence(fillId,MISMATCH,"TRADE_METADATA_CONFLICT",null);
        var references=referencesById.apply(fill.legacyId());
        if(references.size()!=1||!references.getFirst().equals(fillId))return new EquityLegacyEvidence(fillId,MISMATCH,"AMBIGUOUS_EQUITY_LINK",null);
        return new EquityLegacyEvidence(fillId,MATCHED_DUPLICATE,"ONE_TRADE_TWO_RECORDS",legacyId);
    }
    private static boolean money(BigDecimal value){if(value==null)return false;try{return value.setScale(2,RoundingMode.UNNECESSARY).precision()<=19;}catch(ArithmeticException invalid){return false;}}
}
