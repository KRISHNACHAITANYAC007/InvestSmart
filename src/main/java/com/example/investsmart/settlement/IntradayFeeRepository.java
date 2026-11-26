package com.example.investsmart.settlement;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IntradayFeeRepository {
    record Link(String eventKey, String ticketId, boolean validTicket, BigDecimal ticketFee,
                String fillId, long fillUser, int fillUnits, int fillLeg, BigDecimal fillFee) {}
    private final JdbcTemplate db;
    public IntradayFeeRepository(JdbcTemplate db) { this.db=db; }
    List<Link> links(long user,String position) {
        return db.query("""
                SELECT e.event_key,t.id AS ticket_id,t.fees AS ticket_fee,
                CASE WHEN t.user_id=e.user_id AND t.position_id=e.position_id AND t.product='MIS'
                  AND t.stock_id=p.stock_id AND t.status='FILLED' AND t.quantity=e.units
                  AND t.filled_units=e.units AND t.filled_legs=1
                  AND ((p.direction='LONG' AND ((e.event_type='OPEN' AND t.side='BUY') OR (e.event_type='CLOSE' AND t.side='SELL')))
                    OR (p.direction='SHORT' AND ((e.event_type='OPEN' AND t.side='SELL') OR (e.event_type='CLOSE' AND t.side='BUY'))))
                  THEN 1 ELSE 0 END AS valid_ticket,
                f.id AS fill_id,f.user_id AS fill_user,f.quantity AS fill_units,f.leg_number,f.fee AS fill_fee
                FROM paper_intraday_events e
                JOIN paper_intraday_positions p ON p.id=e.position_id AND p.user_id=e.user_id
                LEFT JOIN paper_tickets t ON t.id=e.event_key
                LEFT JOIN paper_fills f ON f.ticket_id=t.id
                WHERE e.user_id=? AND e.position_id=? ORDER BY e.event_key,f.leg_number LIMIT 1001
                """,(rs,row)->new Link(rs.getString("event_key"),rs.getString("ticket_id"),rs.getBoolean("valid_ticket"),
                rs.getBigDecimal("ticket_fee"),rs.getString("fill_id"),rs.getLong("fill_user"),
                rs.getInt("fill_units"),rs.getInt("leg_number"),rs.getBigDecimal("fill_fee")),user,position);
    }
}
