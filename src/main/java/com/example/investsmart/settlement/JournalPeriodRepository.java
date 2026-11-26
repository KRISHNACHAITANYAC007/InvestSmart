package com.example.investsmart.settlement;

import com.example.investsmart.dto.SettlementJournalPage;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JournalPeriodRepository {
    private final JdbcTemplate db;
    public JournalPeriodRepository(JdbcTemplate db){this.db=db;}
    List<SettlementJournalPage.Entry> entries(long user,Instant from,Instant to){
        return db.query("""
            SELECT event_key,action,amount,reference_kind,reference_id,occurred_at
            FROM settlement_journal WHERE user_id=? AND occurred_at>=? AND occurred_at<?
            ORDER BY occurred_at,event_key LIMIT 1001
            """,(r,n)->new SettlementJournalPage.Entry(r.getString("event_key"),r.getString("action"),r.getBigDecimal("amount"),
                r.getString("reference_kind"),r.getString("reference_id"),r.getTimestamp("occurred_at").toInstant()),
            user,Timestamp.from(from),Timestamp.from(to));
    }
}
