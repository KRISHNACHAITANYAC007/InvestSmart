package com.example.investsmart.dto;

import com.example.investsmart.settlement.IntradayEvidenceReconciler.Result;

/** Position-local gross evidence only. Event fees are not independently ticket-reconciled. */
public record IntradayPositionEvidence(String positionId, Result comparison) {}
