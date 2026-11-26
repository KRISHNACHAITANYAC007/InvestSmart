package com.example.investsmart.security;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminHealthStore {
    private final JdbcTemplate db;

    public AdminHealthStore(JdbcTemplate db) {
        this.db = db;
    }

    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void started(String service, Instant at) {
        db.update("INSERT INTO admin_runtime_health(service_name,last_started_at,failure_count) VALUES(?,?,0) ON DUPLICATE KEY UPDATE last_started_at=VALUES(last_started_at)",
                service, Timestamp.from(at));
    }

    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void completed(String service, Instant at) {
        db.update("INSERT INTO admin_runtime_health(service_name,last_completed_at,failure_count) VALUES(?,?,0) ON DUPLICATE KEY UPDATE last_completed_at=VALUES(last_completed_at)",
                service, Timestamp.from(at));
    }

    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void succeeded(String service, Instant at) {
        db.update("INSERT INTO admin_runtime_health(service_name,last_success_at,failure_count) VALUES(?,?,0) ON DUPLICATE KEY UPDATE last_success_at=VALUES(last_success_at)",
                service, Timestamp.from(at));
    }

    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void failed(String service, Instant at, String reason) {
        db.update("INSERT INTO admin_runtime_health(service_name,last_failure_at,last_failure,failure_count) VALUES(?,?,?,1) ON DUPLICATE KEY UPDATE last_failure_at=VALUES(last_failure_at),last_failure=VALUES(last_failure),failure_count=failure_count+1",
                service, Timestamp.from(at), SecurityEvents.bounded(reason, 200));
    }

    public Map<String,Object> status(String service) {
        List<Map<String,Object>> rows = db.query(
                "SELECT last_started_at,last_completed_at,last_success_at,last_failure_at,last_failure,failure_count FROM admin_runtime_health WHERE service_name=?",
                (rs, row) -> {
                    Map<String,Object> status = new LinkedHashMap<>();
                    status.put("lastStartedAt", instant(rs.getTimestamp("last_started_at")));
                    status.put("lastCompletedAt", instant(rs.getTimestamp("last_completed_at")));
                    status.put("lastSuccessAt", instant(rs.getTimestamp("last_success_at")));
                    status.put("lastFailureAt", instant(rs.getTimestamp("last_failure_at")));
                    status.put("lastFailure", rs.getString("last_failure"));
                    status.put("failureCount", rs.getLong("failure_count"));
                    return status;
                }, service);
        if (!rows.isEmpty()) return rows.getFirst();
        Map<String,Object> empty = new LinkedHashMap<>();
        empty.put("lastStartedAt", null);
        empty.put("lastCompletedAt", null);
        empty.put("lastSuccessAt", null);
        empty.put("lastFailureAt", null);
        empty.put("lastFailure", null);
        empty.put("failureCount", 0L);
        return empty;
    }

    private Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
