package com.example.investsmart.settlement;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IntradayEvidenceRepository {
    private final JdbcTemplate db;
    public IntradayEvidenceRepository(JdbcTemplate db) { this.db = db; }
    IntradayEvidenceReconciler.Position owned(long user, String position) {
        var rows = db.query("SELECT quantity,collateral,realized_pnl FROM paper_intraday_positions WHERE id=? AND user_id=?",
                (rs,row) -> new IntradayEvidenceReconciler.Position(rs.getInt("quantity"),
                        rs.getBigDecimal("collateral"),rs.getBigDecimal("realized_pnl")),position,user);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    List<IntradayEvidenceReconciler.Event> events(long user, String position) {
        return db.query("""
                SELECT event_key,event_type,units,collateral_delta,realized_pnl,fee
                FROM paper_intraday_events WHERE user_id=? AND position_id=?
                ORDER BY occurred_at,event_key LIMIT 1001
                """, (rs,row) -> new IntradayEvidenceReconciler.Event(rs.getString("event_key"),
                rs.getString("event_type"),rs.getInt("units"),rs.getBigDecimal("collateral_delta"),
                rs.getBigDecimal("realized_pnl"),rs.getBigDecimal("fee")),user,position);
    }
}
