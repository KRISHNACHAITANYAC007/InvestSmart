package com.example.investsmart.settlement;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TicketFeeRepository {
    record Ticket(int quantity, int filledUnits, int filledLegs, BigDecimal fee) {}
    record Fill(long userId, int leg, int quantity, BigDecimal fee) {}
    private final JdbcTemplate db;
    public TicketFeeRepository(JdbcTemplate db) { this.db = db; }
    Ticket owned(long userId, String ticketId) {
        var rows = db.query("SELECT quantity,filled_units,filled_legs,fees FROM paper_tickets WHERE id=? AND user_id=?",
                (rs, row) -> new Ticket(rs.getInt("quantity"), rs.getInt("filled_units"),
                        rs.getInt("filled_legs"), rs.getBigDecimal("fees")), ticketId, userId);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    // Called only after ownership validation; foreign rows are detected internally, never returned in the DTO.
    List<Fill> fills(String ticketId) {
        return db.query("SELECT user_id,leg_number,quantity,fee FROM paper_fills WHERE ticket_id=? ORDER BY leg_number LIMIT 1001",
                (rs, row) -> new Fill(rs.getLong("user_id"), rs.getInt("leg_number"),
                        rs.getInt("quantity"), rs.getBigDecimal("fee")), ticketId);
    }
}
