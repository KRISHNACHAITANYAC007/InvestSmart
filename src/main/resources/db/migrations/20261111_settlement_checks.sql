CREATE TABLE IF NOT EXISTS settlement_checks (
 obligation_id VARCHAR(36) PRIMARY KEY,user_id BIGINT NOT NULL,last_checked_at TIMESTAMP NOT NULL,
 INDEX ix_settlement_checks_user(user_id)
);
