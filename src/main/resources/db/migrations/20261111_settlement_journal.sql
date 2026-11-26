CREATE TABLE IF NOT EXISTS settlement_journal (
 user_id BIGINT NOT NULL,event_key VARCHAR(160) NOT NULL,action VARCHAR(24) NOT NULL,
 fingerprint VARCHAR(64) NOT NULL,amount DECIMAL(19,2) NOT NULL,
 reference_kind VARCHAR(24),reference_id VARCHAR(100),occurred_at TIMESTAMP NOT NULL,
 PRIMARY KEY(user_id,event_key),INDEX ix_settlement_journal_user(user_id,occurred_at)
);
CREATE TABLE IF NOT EXISTS settlement_allocation_events (
 user_id BIGINT NOT NULL,event_key VARCHAR(160) NOT NULL,allocation_id VARCHAR(36) NOT NULL,
 action VARCHAR(24) NOT NULL,amount DECIMAL(19,2) NOT NULL,
 PRIMARY KEY(user_id,event_key,allocation_id)
);
CREATE TABLE IF NOT EXISTS settlement_lot_events (
 user_id BIGINT NOT NULL,event_key VARCHAR(160) NOT NULL,lot_id VARCHAR(36) NOT NULL,
 action VARCHAR(24) NOT NULL,units INT NOT NULL,settled_at_event BOOLEAN NOT NULL,
 PRIMARY KEY(user_id,event_key,lot_id)
);
CREATE TABLE IF NOT EXISTS settlement_dependencies (
 user_id BIGINT NOT NULL,credit_obligation_id VARCHAR(36) NOT NULL,delivery_obligation_id VARCHAR(36) NOT NULL,
 PRIMARY KEY(user_id,credit_obligation_id,delivery_obligation_id)
);
