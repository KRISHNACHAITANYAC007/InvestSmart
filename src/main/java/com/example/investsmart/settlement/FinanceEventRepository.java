package com.example.investsmart.settlement;

import com.example.investsmart.dto.FinanceEventPage;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FinanceEventRepository {
    private final JdbcTemplate db;
    private final FinanceEventClassifier classifier;
    public FinanceEventRepository(JdbcTemplate db, FinanceEventClassifier classifier) {
        this.db = db; this.classifier = classifier;
    }
    boolean ownedPosition(long user, String position) {
        return !db.queryForList("SELECT id FROM paper_financing WHERE id=? AND user_id=?",
                String.class, position, user).isEmpty();
    }
    List<FinanceEventPage.Entry> entries(long user, String position, int size, long offset) {
        return db.query("""
                SELECT event_key,event_type,principal_delta,amount,occurred_at
                FROM paper_finance_events WHERE position_id=? AND user_id=?
                ORDER BY occurred_at DESC,event_key DESC LIMIT ? OFFSET ?
                """, (rs, row) -> new FinanceEventPage.Entry(rs.getString("event_key"),
                rs.getString("event_type"), rs.getBigDecimal("principal_delta"), rs.getBigDecimal("amount"),
                rs.getTimestamp("occurred_at").toInstant(), classifier.classify(rs.getString("event_type"),
                rs.getBigDecimal("principal_delta"), rs.getBigDecimal("amount"))), position, user, size, offset);
    }
}
