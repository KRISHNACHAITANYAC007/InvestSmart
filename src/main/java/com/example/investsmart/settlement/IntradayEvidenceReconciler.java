package com.example.investsmart.settlement;

import java.math.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Pure comparison of supplied evidence. Does not retrieve/authorize records or calculate account P&L. */
@Component
public class IntradayEvidenceReconciler {
    public enum Verification { MATCHED_POSITION_TOTALS, INCOMPLETE, MISMATCH }
    public record Position(int quantity, BigDecimal collateral, BigDecimal grossRealizedPnl) {}
    public record Event(String key, String kind, int units, BigDecimal collateralDelta,
                        BigDecimal grossRealizedPnl, BigDecimal fee) {}
    /** Event fee sum has NOT been reconciled against ticket/fill charges; never add overlapping charges. */
    public record Result(Verification verification, String reason, BigDecimal verifiedGrossRealizedPnl,
                         BigDecimal eventFeeSum) {}

    public Result reconcile(Position position, List<Event> events, boolean truncated) {
        if (events == null || truncated || events.size() > 1000) return unavailable(Verification.INCOMPLETE,"DETAIL_UNAVAILABLE_OR_LIMITED");
        if (position == null || position.quantity() < 0 || !money(position.collateral())
                || position.collateral().signum() < 0 || (position.quantity() == 0 && position.collateral().signum() != 0)
                || !money(position.grossRealizedPnl()))
            return unavailable(Verification.MISMATCH,"INVALID_POSITION");
        long opened = 0, closed = 0; int openings = 0;
        BigDecimal collateral = new BigDecimal("0.00"), pnl = new BigDecimal("0.00"), fees = new BigDecimal("0.00");
        var keys = new HashSet<String>();
        for (var event : events) {
            if (event == null || event.key() == null || event.key().isBlank() || !keys.add(event.key())
                    || event.units() < 1 || !money(event.collateralDelta()) || !money(event.grossRealizedPnl())
                    || !money(event.fee()) || event.fee().signum() < 0)
                return unavailable(Verification.MISMATCH,"INVALID_OR_DUPLICATE_EVENT");
            if ("OPEN".equals(event.kind())) {
                if (++openings > 1 || event.collateralDelta().signum() < 0 || event.grossRealizedPnl().signum() != 0)
                    return unavailable(Verification.MISMATCH,"INVALID_OPEN");
                opened += event.units();
            } else if ("CLOSE".equals(event.kind())) {
                if (event.collateralDelta().signum() > 0) return unavailable(Verification.MISMATCH,"INVALID_CLOSE");
                closed += event.units();
            } else return unavailable(Verification.INCOMPLETE,"UNSUPPORTED_EVENT");
            collateral = collateral.add(event.collateralDelta());
            pnl = pnl.add(event.grossRealizedPnl()); fees = fees.add(event.fee());
        }
        if (openings != 1) return unavailable(Verification.INCOMPLETE,"OPENING_EVIDENCE_MISSING");
        if (opened - closed != position.quantity() || collateral.compareTo(position.collateral()) != 0
                || pnl.compareTo(position.grossRealizedPnl()) != 0)
            return unavailable(Verification.MISMATCH,"POSITION_EVENT_CONFLICT");
        return new Result(Verification.MATCHED_POSITION_TOTALS,"POSITION_EVENT_TOTALS_AGREE",pnl,fees);
    }
    private static Result unavailable(Verification status, String reason) { return new Result(status,reason,null,null); }
    private static boolean money(BigDecimal value) {
        if (value == null) return false;
        try { return value.setScale(2,RoundingMode.UNNECESSARY).precision() <= 19; }
        catch (ArithmeticException invalid) { return false; }
    }
}
