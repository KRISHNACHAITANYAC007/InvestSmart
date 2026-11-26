package com.example.investsmart.settlement;

import java.math.*;
import org.springframework.stereotype.Component;

/** Interprets one supplied journal row. Does not verify its ownership, source or transaction. */
@Component
public class JournalActionClassifier {
    public enum Kind { SETTLED_CREDIT, WITHDRAWAL, PENDING_CREDIT, RESERVATION, RESERVED_SPEND,
        REFUND, SETTLEMENT_TRANSFER, COMPOSITE_CHARGE, DELIVERY_METADATA, UNSUPPORTED, INVALID }
    /** Deltas describe this action only, not sibling events or a complete transaction.
     * Null means unknown; zero means this action does not change that bucket.
     * No result is a P&L, expense total, historical balance or reconciliation verdict. */
    public record Interpretation(Kind kind,BigDecimal settledDelta,BigDecimal reservedDelta,String limitation) {}
    private static final BigDecimal ZERO=new BigDecimal("0.00");
    public Interpretation classify(String action,BigDecimal amount,String referenceKind,String referenceId) {
        if(action==null)return empty(Kind.UNSUPPORTED,"UNKNOWN_ACTION");
        var kind=switch(action){
            case "SETTLED_CREDIT"->Kind.SETTLED_CREDIT;case "WITHDRAW"->Kind.WITHDRAWAL;
            case "PENDING_CREDIT"->Kind.PENDING_CREDIT;case "RESERVE"->Kind.RESERVATION;
            case "SPEND"->Kind.RESERVED_SPEND;case "RELEASE"->Kind.REFUND;
            case "SETTLE"->Kind.SETTLEMENT_TRANSFER;case "REALIZED_CHARGE"->Kind.COMPOSITE_CHARGE;
            case "LINK_DELIVERIES"->Kind.DELIVERY_METADATA;default->Kind.UNSUPPORTED;
        };
        if(kind==Kind.UNSUPPORTED)return empty(kind,"UNKNOWN_OR_NON_CASH_ACTION");
        if(!exact(amount)||amount.signum()<0)return empty(Kind.INVALID,"INVALID_MAGNITUDE");
        amount=amount.setScale(2);
        boolean metadata=kind==Kind.DELIVERY_METADATA;
        if(metadata?amount.signum()!=0:kind!=Kind.SETTLEMENT_TRANSFER&&amount.signum()==0)
            return empty(Kind.INVALID,"INVALID_MAGNITUDE");
        boolean allocation=kind==Kind.RESERVATION||kind==Kind.RESERVED_SPEND||kind==Kind.REFUND;
        boolean obligation=metadata||kind==Kind.SETTLEMENT_TRANSFER;
        if(allocation&&(!text(referenceKind,24)||!referenceKind.matches("[A-Z_]+")||!text(referenceId,100))
            ||obligation&&(!"OBLIGATION".equals(referenceKind)||!text(referenceId,36))
            ||!allocation&&!obligation&&(referenceKind!=null||referenceId!=null))
            return empty(Kind.INVALID,"INVALID_REFERENCE");
        return switch(kind){
            case SETTLED_CREDIT->new Interpretation(kind,amount,ZERO,"Not necessarily income or profit.");
            case WITHDRAWAL->new Interpretation(kind,amount.negate(),ZERO,"Withdrawal is not a trading loss.");
            case PENDING_CREDIT->new Interpretation(kind,ZERO,ZERO,"Pending policy/source is not retained in these row fields; no reusable/restricted split.");
            case RESERVATION->new Interpretation(kind,null,amount,"Source split requires allocation evidence; reserved cash is not an expense.");
            case RESERVED_SPEND->new Interpretation(kind,ZERO,amount.negate(),"Spends previously reserved funds; not a second settled debit or independently classified expense.");
            case REFUND->new Interpretation(kind,null,amount.negate(),"Refund destination depends on source status at release time, not its current status.");
            case SETTLEMENT_TRANSFER->new Interpretation(kind,amount,ZERO,"Transfers remaining credit; no new income, and delivery settlement may carry zero cash.");
            case COMPOSITE_CHARGE->empty(kind,"Linked reserve/spend and uncovered debt require separate evidence; never add all magnitudes as expenses.");
            case DELIVERY_METADATA->new Interpretation(kind,ZERO,ZERO,"Links delivery dependencies; no cash movement.");
            default->throw new IllegalStateException("Unexpected interpretation");
        };
    }
    private static Interpretation empty(Kind kind,String reason){return new Interpretation(kind,null,null,reason);}
    private static boolean exact(BigDecimal value){if(value==null)return false;try{return value.setScale(2,RoundingMode.UNNECESSARY).precision()<=19;}catch(ArithmeticException invalid){return false;}}
    private static boolean text(String value,int max){return value!=null&&!value.isBlank()&&value.length()<=max&&value.chars().noneMatch(Character::isISOControl);}
}
