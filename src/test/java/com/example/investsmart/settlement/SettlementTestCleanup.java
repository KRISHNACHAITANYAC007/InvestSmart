package com.example.investsmart.settlement;

import org.springframework.jdbc.core.JdbcTemplate;

/** Synthetic fixture cleanup only; production account deletion has its own audited workflow. */
public final class SettlementTestCleanup {
    private SettlementTestCleanup() {}
    public static void account(JdbcTemplate db,long user) {
        for(String table:java.util.List.of("settlement_checks","settlement_dependencies","settlement_lot_events","settlement_allocation_events","settlement_journal","settlement_allocations","settlement_lots","settlement_obligations","settlement_accounts")) {
            db.update("DELETE FROM "+table+" WHERE user_id=?",user);
        }
    }
}
