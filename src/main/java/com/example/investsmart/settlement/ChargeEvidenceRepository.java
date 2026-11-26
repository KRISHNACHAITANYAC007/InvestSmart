package com.example.investsmart.settlement;

import com.example.investsmart.dto.SettlementJournalPage;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ChargeEvidenceRepository {
    private final JdbcTemplate db;
    public ChargeEvidenceRepository(JdbcTemplate db){this.db=db;}
    private static final RowMapper<SettlementJournalPage.Entry> ROW=(r,n)->new SettlementJournalPage.Entry(
        r.getString("event_key"),r.getString("action"),r.getBigDecimal("amount"),r.getString("reference_kind"),r.getString("reference_id"),r.getTimestamp("occurred_at").toInstant());
    SettlementJournalPage.Entry parent(long user,String key){
        var rows=db.query("SELECT event_key,action,amount,reference_kind,reference_id,occurred_at FROM settlement_journal WHERE user_id=? AND event_key=?",ROW,user,key);
        return rows.isEmpty()?null:rows.getFirst();
    }
    List<SettlementJournalPage.Entry> children(long user,String reference){
        // Exact keys retain malformed reference metadata; reference lookup retains unexpected siblings.
        return db.query("""
            SELECT event_key,action,amount,reference_kind,reference_id,occurred_at FROM settlement_journal
            WHERE user_id=? AND ((reference_kind='CHARGE' AND reference_id=?) OR event_key IN (?,?))
            ORDER BY event_key LIMIT 1001
            """,ROW,user,reference,"CHARGE_RESERVE:"+reference,"CHARGE_SPEND:"+reference);
    }
}
