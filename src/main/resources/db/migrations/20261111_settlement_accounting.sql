CREATE TABLE IF NOT EXISTS settlement_accounts (
 user_id BIGINT PRIMARY KEY,opening_cash DECIMAL(19,2) NOT NULL,
 settled_cash DECIMAL(19,2) NOT NULL,initialized_at TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS settlement_obligations (
 id VARCHAR(36) PRIMARY KEY,user_id BIGINT NOT NULL,source_key VARCHAR(160) NOT NULL,
 exchange_code VARCHAR(12) NOT NULL,segment_code VARCHAR(24) NOT NULL,trade_date DATE NOT NULL,
 obligation_kind VARCHAR(24) NOT NULL,credit_policy VARCHAR(24) NOT NULL,
 amount DECIMAL(19,2) NOT NULL,remaining_credit DECIMAL(19,2) NOT NULL,
 status VARCHAR(24) NOT NULL,calendar_version VARCHAR(36),expected_date DATE,cutoff_at TIMESTAMP NULL,
 created_at TIMESTAMP NOT NULL,settled_at TIMESTAMP NULL,
 CONSTRAINT uq_settlement_obligation UNIQUE(user_id,source_key),
 INDEX ix_settlement_due(status,cutoff_at),INDEX ix_settlement_user(user_id,status)
);
CREATE TABLE IF NOT EXISTS settlement_lots (
 id VARCHAR(36) PRIMARY KEY,user_id BIGINT NOT NULL,stock_id BIGINT NOT NULL,source_key VARCHAR(160) NOT NULL,
 obligation_id VARCHAR(36),initial_units INT NOT NULL,remaining_units INT NOT NULL,
 settled BOOLEAN NOT NULL,cost_amount DECIMAL(30,12),created_at TIMESTAMP NOT NULL,
 CONSTRAINT uq_settlement_lot UNIQUE(user_id,source_key),INDEX ix_settlement_lot_user(user_id,stock_id)
);
CREATE TABLE IF NOT EXISTS settlement_allocations (
 id VARCHAR(36) PRIMARY KEY,user_id BIGINT NOT NULL,reference_kind VARCHAR(24) NOT NULL,
 reference_id VARCHAR(100) NOT NULL,source_key VARCHAR(160) NOT NULL,
 amount DECIMAL(19,2) NOT NULL,remaining_amount DECIMAL(19,2) NOT NULL,
 status VARCHAR(24) NOT NULL,created_at TIMESTAMP NOT NULL,
 CONSTRAINT uq_settlement_allocation UNIQUE(user_id,reference_kind,reference_id,source_key),
 INDEX ix_settlement_allocation_user(user_id,status)
);
