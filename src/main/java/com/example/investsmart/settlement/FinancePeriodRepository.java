package com.example.investsmart.settlement;

import com.example.investsmart.dto.FinancePeriodEvidence;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FinancePeriodRepository {
    private final JdbcTemplate db;
    private final FinanceEventClassifier classifier;
    public FinancePeriodRepository(JdbcTemplate db,FinanceEventClassifier classifier){this.db=db;this.classifier=classifier;}
    List<FinancePeriodEvidence.Entry> entries(long user,Instant from,Instant to){
        return db.query("""
            SELECT e.event_key,p.id AS owned_position,e.event_type,e.principal_delta,e.amount,e.occurred_at
            FROM paper_finance_events e LEFT JOIN paper_financing p ON p.id=e.position_id AND p.user_id=e.user_id
            WHERE e.user_id=? AND e.occurred_at>=? AND e.occurred_at<?
            ORDER BY e.occurred_at,e.event_key LIMIT 1001
            """,(r,n)->{
                String position=r.getString("owned_position");
                return new FinancePeriodEvidence.Entry(r.getString("event_key"),position,r.getString("event_type"),r.getBigDecimal("principal_delta"),r.getBigDecimal("amount"),r.getTimestamp("occurred_at").toInstant(),
                    position==null?FinancePeriodEvidence.Ownership.POSITION_UNAVAILABLE:FinancePeriodEvidence.Ownership.OWNED_POSITION,
                    position==null?null:classifier.classify(r.getString("event_type"),r.getBigDecimal("principal_delta"),r.getBigDecimal("amount")));
            },user,Timestamp.from(from),Timestamp.from(to));
    }
}
