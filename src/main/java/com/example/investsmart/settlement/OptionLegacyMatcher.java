package com.example.investsmart.settlement;

import com.example.investsmart.dto.OptionLegacyEvidence;
import java.math.*;
import java.util.List;
import java.util.function.Function;
import static com.example.investsmart.dto.OptionLegacyEvidence.Verification.*;

/** Shared rules; caller supplies owned records and reference evidence from one snapshot.
 * Lookup functions can use prefetched maps. This class starts no transaction or query. */
final class OptionLegacyMatcher {
    private OptionLegacyMatcher() {}
    static OptionLegacyEvidence match(String fillId,OptionLegacyRepository.Fill fill,
            Function<String,OptionLegacyRepository.Legacy> orders,
            Function<String,List<String>> referencesById) {
        if(fill.equity()||!"CASH".equals(fill.product()))return new OptionLegacyEvidence(fillId,NOT_EVALUATED,"NOT_CASH_OPTION",null);
        if(fill.legacyId()==null)return new OptionLegacyEvidence(fillId,INCOMPLETE,"LEGACY_LINK_MISSING",null);
        if(fill.legacyId().isBlank()||fill.legacyId().length()>36)return new OptionLegacyEvidence(fillId,MISMATCH,"INVALID_OPTION_LINK",null);
        var order=orders.apply(fill.legacyId());
        if(order==null)return new OptionLegacyEvidence(fillId,INCOMPLETE,"OWNED_OPTION_ORDER_MISSING",null);
        if(fill.key()==null||fill.key().isBlank()||!fill.key().equals(order.key())||fill.expiry()==null||!fill.expiry().equals(order.expiry())
                ||fill.lot()<1||fill.lot()!=order.lot()||fill.units()<1||fill.units()%fill.lot()!=0||fill.units()!=order.units()
                ||!("BUY".equals(fill.side())||"SELL".equals(fill.side()))||!fill.side().equals(order.side())
                ||!"FILLED".equals(order.status())||!"MARKET".equals(order.kind())
                ||!money(fill.price())||!money(order.price())||fill.price().signum()<=0||fill.price().compareTo(order.price())!=0)
            return new OptionLegacyEvidence(fillId,MISMATCH,"OPTION_METADATA_CONFLICT",null);
        var references=referencesById.apply(fill.legacyId());
        if(references.size()!=1||!references.getFirst().equals(fillId))return new OptionLegacyEvidence(fillId,MISMATCH,"AMBIGUOUS_OPTION_LINK",null);
        return new OptionLegacyEvidence(fillId,MATCHED_DUPLICATE,"ONE_OPTION_TRADE_TWO_RECORDS",order.id());
    }
    private static boolean money(BigDecimal value){if(value==null)return false;try{return value.setScale(2,RoundingMode.UNNECESSARY).precision()<=19;}catch(ArithmeticException invalid){return false;}}
}
