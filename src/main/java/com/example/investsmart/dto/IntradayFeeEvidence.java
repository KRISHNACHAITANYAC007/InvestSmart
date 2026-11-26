package com.example.investsmart.dto;

import com.example.investsmart.settlement.IntradayEvidenceReconciler.Result;
import java.math.BigDecimal;

/** Fees for evidenced events only, not a complete account/period expense statement. */
public record IntradayFeeEvidence(String positionId, Result positionComparison, Verification verification,
                                  String reason, BigDecimal reconciledEventFees) {
    public enum Verification { MATCHED_EVENT_FEES, INCOMPLETE, MISMATCH }
}
