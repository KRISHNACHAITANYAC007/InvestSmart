package com.example.investsmart.settlement;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettlementFundingRepository {
    static final int MAX_EVIDENCE = 1000;
    record Evidence(String eventKey, String action, BigDecimal amount, String allocationId,
                    String ownedAllocationId, String referenceKind, String referenceId, String sourceKey,
                    String ownedObligationId, String obligationKind, String creditPolicy, String currentStatus) {}

    private final JdbcTemplate db;
    public SettlementFundingRepository(JdbcTemplate db) { this.db = db; }

    List<Evidence> evidence(long userId, List<String> eventKeys) {
        return evidence(userId,eventKeys,1001);
    }

    List<Evidence> evidence(long userId,List<String> eventKeys,int limit) {
        if(limit<1||limit>1001)throw new IllegalArgumentException("Invalid evidence limit.");
        if (eventKeys.isEmpty()) return List.of();
        // The service supplies at most 100 keys. Every join independently retains ownership.
        if (eventKeys.size() > 100) throw new IllegalArgumentException("Too many funding event keys.");
        String placeholders = String.join(",", Collections.nCopies(eventKeys.size(), "?"));
        String sql = """
                SELECT e.event_key,e.action,e.amount,e.allocation_id,a.id AS owned_allocation_id,
                       a.reference_kind,a.reference_id,a.source_key,o.id AS owned_obligation_id,
                       o.obligation_kind,o.credit_policy,o.status
                FROM settlement_allocation_events e
                LEFT JOIN settlement_allocations a ON a.user_id=e.user_id AND a.id=e.allocation_id
                LEFT JOIN settlement_obligations o ON o.user_id=e.user_id AND o.id=a.source_key
                WHERE e.user_id=? AND e.event_key IN (
                """ + placeholders + ") ORDER BY e.event_key,e.allocation_id LIMIT ?";
        List<Object> args = new ArrayList<>(); args.add(userId); args.addAll(eventKeys); args.add(limit);
        return db.query(sql, (rs, n) -> new Evidence(rs.getString("event_key"), rs.getString("action"),
                rs.getBigDecimal("amount"), rs.getString("allocation_id"), rs.getString("owned_allocation_id"),
                rs.getString("reference_kind"), rs.getString("reference_id"), rs.getString("source_key"),
                rs.getString("owned_obligation_id"), rs.getString("obligation_kind"),
                rs.getString("credit_policy"), rs.getString("status")), args.toArray());
    }
}
