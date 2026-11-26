package com.example.investsmart.settlement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Pure interpretation of existing paper_finance_events, not a new charge or cash calculation. */
@Component
public class FinanceEventClassifier {
    public enum Kind { BORROWING, INTEREST_ACCRUAL, INTEREST_PAYMENT, UNSUPPORTED, INVALID }

    /** Null means not applicable/unverified, never an invented zero expense. */
    public record Interpretation(Kind kind, BigDecimal principalChange, BigDecimal contribution,
                                 BigDecimal interestAccrued, BigDecimal interestPaid) {}

    public Interpretation classify(String eventType, BigDecimal principalDelta, BigDecimal amount) {
        if (!"BORROW".equals(eventType) && !"INTEREST".equals(eventType) && !"REPAY".equals(eventType)) {
            return empty(Kind.UNSUPPORTED);
        }
        if (!exactMoney(principalDelta) || !exactMoney(amount) || amount.signum() < 0) return empty(Kind.INVALID);
        return switch (eventType) {
            case "BORROW" -> principalDelta.signum() > 0 && amount.signum() > 0
                    ? new Interpretation(Kind.BORROWING, principalDelta, amount, null, null) : empty(Kind.INVALID);
            case "INTEREST" -> principalDelta.signum() == 0
                    ? new Interpretation(Kind.INTEREST_ACCRUAL, principalDelta, null, amount, null) : empty(Kind.INVALID);
            case "REPAY" -> principalDelta.signum() <= 0
                    ? new Interpretation(Kind.INTEREST_PAYMENT, principalDelta, null, null, amount) : empty(Kind.INVALID);
            default -> empty(Kind.UNSUPPORTED);
        };
    }

    private boolean exactMoney(BigDecimal value) {
        if (value == null) return false;
        try { return value.setScale(2, RoundingMode.UNNECESSARY).precision() <= 19; }
        catch (ArithmeticException invalid) { return false; }
    }

    private Interpretation empty(Kind kind) { return new Interpretation(kind, null, null, null, null); }
}
