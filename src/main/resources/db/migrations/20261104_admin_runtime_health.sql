-- Back up before applying. Admin service-health counters and latest events persist here.
CREATE TABLE admin_runtime_health (
 service_name VARCHAR(40) PRIMARY KEY,
 last_started_at TIMESTAMP NULL,
 last_completed_at TIMESTAMP NULL,
 last_success_at TIMESTAMP NULL,
 last_failure_at TIMESTAMP NULL,
 last_failure VARCHAR(200),
 failure_count BIGINT NOT NULL DEFAULT 0
);
