package com.example.investsmart.settlement;

import com.example.investsmart.dto.SettlementJournalPage;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettlementJournalRepository {
    private final JdbcTemplate db;

    public SettlementJournalRepository(JdbcTemplate db) {
        this.db = db;
    }

    SettlementJournalPage.Coverage coverage(long userId) {
        var rows = db.query("SELECT initialized_at FROM settlement_accounts WHERE user_id=?",
                (rs, row) -> new SettlementJournalPage.Coverage(true,
                        rs.getTimestamp("initialized_at").toInstant()), userId);
        return rows.isEmpty() ? new SettlementJournalPage.Coverage(false, null) : rows.getFirst();
    }

    List<SettlementJournalPage.Entry> entries(long userId, int size, long offset) {
        return db.query("""
                SELECT event_key,action,amount,reference_kind,reference_id,occurred_at
                FROM settlement_journal WHERE user_id=?
                ORDER BY occurred_at DESC,event_key DESC LIMIT ? OFFSET ?
                """, (rs, row) -> new SettlementJournalPage.Entry(
                        rs.getString("event_key"), rs.getString("action"), rs.getBigDecimal("amount"),
                        rs.getString("reference_kind"), rs.getString("reference_id"),
                        rs.getTimestamp("occurred_at").toInstant()), userId, size, offset);
    }
}
