package com.example.investsmart.settlement;
import com.example.investsmart.dto.SettlementJournalPage;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class PendingPolicyPeriodRepository {
    record Instruction(SettlementJournalPage.Entry row,String fingerprint) {}
    private final JdbcTemplate db;
    public PendingPolicyPeriodRepository(JdbcTemplate db){this.db=db;}
    List<Instruction> instructions(long user,Instant from,Instant to){
        return db.query("SELECT event_key,action,amount,reference_kind,reference_id,occurred_at,fingerprint FROM settlement_journal WHERE user_id=? AND action='PENDING_CREDIT' AND occurred_at>=? AND occurred_at<? ORDER BY occurred_at,event_key LIMIT 1001",
            (r,n)->new Instruction(new SettlementJournalPage.Entry(r.getString("event_key"),r.getString("action"),r.getBigDecimal("amount"),r.getString("reference_kind"),r.getString("reference_id"),r.getTimestamp("occurred_at").toInstant()),r.getString("fingerprint")),user,Timestamp.from(from),Timestamp.from(to));
    }
    List<PendingCreditPolicyReconciler.Candidate> candidates(long user){
        // Current state and creation date must not exclude consumed, settled or outside-period originals.
        return db.query("SELECT id,source_key,exchange_code,segment_code,trade_date,obligation_kind,credit_policy,amount FROM settlement_obligations WHERE user_id=? AND obligation_kind='CASH_CREDIT' ORDER BY id LIMIT 1001",
            (r,n)->new PendingCreditPolicyReconciler.Candidate(r.getString("id"),r.getString("source_key"),r.getString("exchange_code"),r.getString("segment_code"),r.getDate("trade_date").toLocalDate(),r.getString("obligation_kind"),r.getString("credit_policy"),r.getBigDecimal("amount")),user);
    }
}
