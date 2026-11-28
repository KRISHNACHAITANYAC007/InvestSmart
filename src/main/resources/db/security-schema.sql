CREATE TABLE IF NOT EXISTS auth_sessions (
 factor_completed BOOLEAN NOT NULL DEFAULT FALSE,
 id VARCHAR(64) PRIMARY KEY,user_id BIGINT NOT NULL,expires_at TIMESTAMP NOT NULL,created_at TIMESTAMP NOT NULL,last_activity_at TIMESTAMP NOT NULL,
 revoked BOOLEAN NOT NULL DEFAULT FALSE,reauth_at TIMESTAMP NULL,source_label VARCHAR(100),device_label VARCHAR(160),INDEX ix_auth_sessions_user(user_id)
);
CREATE TABLE IF NOT EXISTS auth_actions (
 token_hash VARCHAR(64) PRIMARY KEY,user_id BIGINT NOT NULL,purpose VARCHAR(20) NOT NULL,
 expires_at TIMESTAMP NOT NULL,payload VARCHAR(2000),used BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS passkey_credentials (
 credential_hash VARCHAR(64) PRIMARY KEY,credential_id TEXT NOT NULL,user_id BIGINT NOT NULL,
 user_handle VARCHAR(100) NOT NULL,public_key_cose TEXT NOT NULL,signature_count BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL,last_used_at TIMESTAMP NULL,INDEX ix_passkey_user(user_id)
);
CREATE TABLE IF NOT EXISTS passkey_challenges (
 challenge_hash VARCHAR(64) PRIMARY KEY,user_id BIGINT NOT NULL,session_id VARCHAR(64) NOT NULL,
 purpose VARCHAR(30) NOT NULL,request_cipher TEXT NOT NULL,expires_at TIMESTAMP NOT NULL,
 used BOOLEAN NOT NULL DEFAULT FALSE,INDEX ix_passkey_challenge_user(user_id,expires_at)
);
CREATE TABLE IF NOT EXISTS trusted_device_locks (
 session_id VARCHAR(64) PRIMARY KEY,locked_at TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS admin_credentials (
 user_id BIGINT PRIMARY KEY,secret_cipher VARCHAR(1000) NOT NULL,last_step BIGINT NOT NULL DEFAULT -1,recovery_hashes VARCHAR(2000) NOT NULL
);
CREATE TABLE IF NOT EXISTS owner_admin (singleton_id INT PRIMARY KEY,user_id BIGINT NOT NULL UNIQUE);
CREATE TABLE IF NOT EXISTS security_events (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,occurred_at TIMESTAMP NOT NULL,event_type VARCHAR(60) NOT NULL,
 user_id BIGINT,subject_key VARCHAR(64) NOT NULL,source_key VARCHAR(64) NOT NULL,source_label VARCHAR(100),route VARCHAR(160),details VARCHAR(300),
 INDEX ix_security_event_subject(event_type,subject_key,occurred_at),INDEX ix_security_event_source(event_type,source_key,occurred_at)
);
CREATE TABLE IF NOT EXISTS security_alerts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,rule_name VARCHAR(60) NOT NULL,subject_key VARCHAR(64) NOT NULL,user_id BIGINT,
 severity VARCHAR(10) NOT NULL,status VARCHAR(20) NOT NULL,occurrences INT NOT NULL,
 first_at TIMESTAMP NOT NULL,last_at TIMESTAMP NOT NULL,reviewed_by BIGINT,notes VARCHAR(1200),seen BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS admin_audit (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,occurred_at TIMESTAMP NOT NULL,actor_id BIGINT NOT NULL,
 target_id BIGINT,action_name VARCHAR(60) NOT NULL,reason VARCHAR(1200) NOT NULL
);
CREATE TABLE IF NOT EXISTS security_mutex (id INT PRIMARY KEY);
INSERT INTO security_mutex(id) SELECT 1 WHERE NOT EXISTS(SELECT 1 FROM security_mutex WHERE id=1);
CREATE TABLE IF NOT EXISTS mail_outbox (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,recipient VARCHAR(255) NOT NULL,subject_line VARCHAR(150) NOT NULL,
 body_cipher VARCHAR(4000) NOT NULL,created_at TIMESTAMP NOT NULL,attempts INT NOT NULL DEFAULT 0,sent BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS mail_action_links (
 mail_id BIGINT PRIMARY KEY,token_hash VARCHAR(64) NOT NULL,purpose VARCHAR(20) NOT NULL,
 CONSTRAINT fk_mail_action_outbox FOREIGN KEY(mail_id) REFERENCES mail_outbox(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS mail_delivery_failures (
 mail_id BIGINT PRIMARY KEY,error_code VARCHAR(40) NOT NULL,recorded_at TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS admin_balance_adjustments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,occurred_at TIMESTAMP NOT NULL,actor_id BIGINT NOT NULL,user_id BIGINT,
 amount DECIMAL(19,2) NOT NULL,balance_after DECIMAL(19,2) NOT NULL,reason VARCHAR(1000) NOT NULL
);
CREATE TABLE IF NOT EXISTS admin_listing_overrides (
 asset_type VARCHAR(8) NOT NULL,reference_id VARCHAR(180) NOT NULL,available BOOLEAN NOT NULL,
 open_date DATE,close_date DATE,allotment_date DATE,listing_date DATE,
 reason VARCHAR(1000) NOT NULL,actor_id BIGINT NOT NULL,updated_at TIMESTAMP NOT NULL,
 PRIMARY KEY(asset_type,reference_id)
);
CREATE TABLE IF NOT EXISTS admin_runtime_health (
 service_name VARCHAR(40) PRIMARY KEY,last_started_at TIMESTAMP,last_completed_at TIMESTAMP,
 last_success_at TIMESTAMP,last_failure_at TIMESTAMP,last_failure VARCHAR(200),
 failure_count BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS option_positions (
 user_id BIGINT NOT NULL,instrument_key VARCHAR(100) NOT NULL,symbol VARCHAR(180) NOT NULL,
 expiry DATE NOT NULL,strike_price DECIMAL(19,2) NOT NULL,option_type VARCHAR(2) NOT NULL,
 lot_size INT NOT NULL,units INT NOT NULL,cost_amount DECIMAL(19,2) NOT NULL,
 last_price DECIMAL(19,2),mark_at TIMESTAMP,PRIMARY KEY(user_id,instrument_key,expiry)
);
CREATE TABLE IF NOT EXISTS option_orders (
 id VARCHAR(36) PRIMARY KEY,user_id BIGINT NOT NULL,request_id VARCHAR(80) NOT NULL,
 instrument_key VARCHAR(100) NOT NULL,symbol VARCHAR(180) NOT NULL,expiry DATE NOT NULL,
 strike_price DECIMAL(19,2) NOT NULL,option_type VARCHAR(2) NOT NULL,lot_size INT NOT NULL,
 units INT NOT NULL,side VARCHAR(12) NOT NULL,order_kind VARCHAR(20) NOT NULL,
 trigger_price DECIMAL(19,2),reserved_cash DECIMAL(19,2) NOT NULL,status VARCHAR(20) NOT NULL,
 fill_price DECIMAL(19,2),realized_pnl DECIMAL(19,2),created_at TIMESTAMP NOT NULL,filled_at TIMESTAMP,last_checked_at TIMESTAMP,
 CONSTRAINT uq_option_request UNIQUE(user_id,request_id)
);

CREATE TABLE IF NOT EXISTS equity_pending_orders (
 id VARCHAR(36) PRIMARY KEY,user_id BIGINT NOT NULL,request_id VARCHAR(80) NOT NULL,
 stock_id BIGINT NOT NULL,units INT NOT NULL,side VARCHAR(4) NOT NULL,order_kind VARCHAR(20) NOT NULL,
 trigger_price DECIMAL(19,2) NOT NULL,reserved_cash DECIMAL(19,2) NOT NULL,status VARCHAR(20) NOT NULL,
 order_id BIGINT,created_at TIMESTAMP NOT NULL,valid_date DATE NOT NULL,last_checked_at TIMESTAMP,
 CONSTRAINT uq_equity_pending_request UNIQUE(user_id,request_id)
);
CREATE TABLE IF NOT EXISTS phone_credentials (
 user_id BIGINT PRIMARY KEY,
 credential_hash VARCHAR(64) NOT NULL UNIQUE,
 credential_id TEXT NOT NULL,
 user_handle VARCHAR(100) NOT NULL,
 public_key_cose TEXT NOT NULL,
 signature_count BIGINT NOT NULL DEFAULT 0,
 secret_cipher TEXT NOT NULL,
 last_step BIGINT NOT NULL DEFAULT -1,
 recovery_hashes TEXT NOT NULL,
 ready BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL,
 last_used_at TIMESTAMP NULL
);
CREATE TABLE IF NOT EXISTS phone_actions (
 token_hash VARCHAR(64) PRIMARY KEY,
 user_id BIGINT NOT NULL,
 purpose VARCHAR(30) NOT NULL,
 parent_hash VARCHAR(64) NULL,
 binding_hash VARCHAR(64) NULL,
 payload TEXT NULL,
 used BOOLEAN NOT NULL DEFAULT FALSE,
 expires_at TIMESTAMP NOT NULL,
 INDEX idx_phone_actions_user(user_id)
);
CREATE TABLE IF NOT EXISTS phone_sessions (
 id_hash VARCHAR(64) PRIMARY KEY,
 user_id BIGINT NOT NULL,
 credential_hash VARCHAR(64) NOT NULL,
 expires_at TIMESTAMP NOT NULL,
 INDEX idx_phone_sessions_user(user_id)
);

CREATE TABLE IF NOT EXISTS paper_tickets (
 id VARCHAR(36) PRIMARY KEY,user_id BIGINT NOT NULL,request_id VARCHAR(100) NOT NULL,
 stock_id BIGINT,instrument_key VARCHAR(100),expiry DATE,position_id VARCHAR(36),
 side VARCHAR(4) NOT NULL,product VARCHAR(12) NOT NULL,variety VARCHAR(12) NOT NULL,
 order_kind VARCHAR(20) NOT NULL,validity VARCHAR(8) NOT NULL,quantity INT NOT NULL,filled_units INT NOT NULL DEFAULT 0,
 legs INT NOT NULL,filled_legs INT NOT NULL DEFAULT 0,lot_size INT NOT NULL,
 limit_price DECIMAL(19,2),trigger_price DECIMAL(19,2),ceiling_price DECIMAL(19,2) NOT NULL,
 stop_percent DECIMAL(10,4),target_percent DECIMAL(10,4),stop_price DECIMAL(19,2),target_price DECIMAL(19,2),group_id VARCHAR(36),parent_id VARCHAR(36),
 reserved_cash DECIMAL(19,2) NOT NULL,reserved_units INT NOT NULL DEFAULT 0,
 status VARCHAR(24) NOT NULL,triggered BOOLEAN NOT NULL DEFAULT FALSE,
 fill_value DECIMAL(19,2) NOT NULL DEFAULT 0,fees DECIMAL(19,2) NOT NULL DEFAULT 0,
 policy_version VARCHAR(40) NOT NULL,last_error VARCHAR(300),expires_at TIMESTAMP NOT NULL,
 created_at TIMESTAMP NOT NULL,last_checked_at TIMESTAMP,
 CONSTRAINT uq_paper_request UNIQUE(user_id,request_id),INDEX ix_paper_pending(status,last_checked_at)
);
CREATE TABLE IF NOT EXISTS paper_fills (
 id VARCHAR(36) PRIMARY KEY,ticket_id VARCHAR(36) NOT NULL,user_id BIGINT NOT NULL,leg_number INT NOT NULL,
 quantity INT NOT NULL,price DECIMAL(19,2) NOT NULL,fee DECIMAL(19,2) NOT NULL,
 legacy_order_id VARCHAR(40),created_at TIMESTAMP NOT NULL,CONSTRAINT uq_paper_leg UNIQUE(ticket_id,leg_number),
 INDEX ix_paper_fill_user_time(user_id,created_at,id)
);
CREATE TABLE IF NOT EXISTS paper_financing (
 id VARCHAR(36) PRIMARY KEY,user_id BIGINT NOT NULL,stock_id BIGINT NOT NULL,product VARCHAR(12) NOT NULL,
 quantity INT NOT NULL,cost_amount DECIMAL(19,2) NOT NULL,principal DECIMAL(19,2) NOT NULL,
 interest DECIMAL(19,2) NOT NULL DEFAULT 0,interest_rate DECIMAL(10,6) NOT NULL,
 realized_pnl DECIMAL(19,2) NOT NULL DEFAULT 0,last_price DECIMAL(19,2),mark_at TIMESTAMP,
 stop_price DECIMAL(19,2),close_requested BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL,last_accrued_date DATE NOT NULL,last_checked_at TIMESTAMP,last_error VARCHAR(300)
);
CREATE TABLE IF NOT EXISTS paper_finance_events (
 event_key VARCHAR(140) PRIMARY KEY,position_id VARCHAR(36) NOT NULL,user_id BIGINT NOT NULL,
 event_type VARCHAR(24) NOT NULL,principal_delta DECIMAL(19,2) NOT NULL,
 amount DECIMAL(19,2) NOT NULL,occurred_at TIMESTAMP NOT NULL,INDEX ix_paper_finance_history(position_id,occurred_at),
 INDEX ix_finance_event_user_time(user_id,occurred_at,event_key)
);

CREATE TABLE IF NOT EXISTS settlement_calendar_versions (
 id VARCHAR(36) PRIMARY KEY,exchange_code VARCHAR(12) NOT NULL,segment_code VARCHAR(24) NOT NULL,
 first_date DATE NOT NULL,last_date DATE NOT NULL,source_url VARCHAR(1000) NOT NULL,
 content_sha256 VARCHAR(64) NOT NULL,imported_at TIMESTAMP NOT NULL,
 CONSTRAINT uq_settlement_calendar_content UNIQUE(exchange_code,segment_code,content_sha256)
);
CREATE TABLE IF NOT EXISTS settlement_calendar_days (
 version_id VARCHAR(36) NOT NULL,calendar_date DATE NOT NULL,
 trading_open BOOLEAN NOT NULL,settlement_open BOOLEAN NOT NULL,note VARCHAR(300),
 PRIMARY KEY(version_id,calendar_date),
 CONSTRAINT fk_settlement_calendar_version FOREIGN KEY(version_id) REFERENCES settlement_calendar_versions(id)
);
CREATE TABLE IF NOT EXISTS settlement_calendar_active (
 exchange_code VARCHAR(12) NOT NULL,segment_code VARCHAR(24) NOT NULL,version_id VARCHAR(36) NOT NULL,
 PRIMARY KEY(exchange_code,segment_code),
 CONSTRAINT fk_settlement_calendar_active FOREIGN KEY(version_id) REFERENCES settlement_calendar_versions(id)
);
CREATE TABLE IF NOT EXISTS settlement_calendar_audit (
 id VARCHAR(36) PRIMARY KEY,version_id VARCHAR(36) NOT NULL,activated_at TIMESTAMP NOT NULL,
 operator_label VARCHAR(100) NOT NULL,reason VARCHAR(300) NOT NULL,
 CONSTRAINT fk_settlement_calendar_audit FOREIGN KEY(version_id) REFERENCES settlement_calendar_versions(id)
);
CREATE TABLE IF NOT EXISTS settlement_calendar_mutex (id INT PRIMARY KEY);
INSERT INTO settlement_calendar_mutex(id) SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM settlement_calendar_mutex WHERE id=1);
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


CREATE TABLE IF NOT EXISTS settlement_checks (
 obligation_id VARCHAR(36) PRIMARY KEY,user_id BIGINT NOT NULL,last_checked_at TIMESTAMP NOT NULL,
 INDEX ix_settlement_checks_user(user_id)
);


CREATE TABLE IF NOT EXISTS paper_intraday_positions (
 id VARCHAR(36) PRIMARY KEY,user_id BIGINT NOT NULL,stock_id BIGINT NOT NULL,direction VARCHAR(5) NOT NULL,
 quantity INT NOT NULL,entry_price DECIMAL(19,2) NOT NULL,collateral DECIMAL(19,2) NOT NULL,
 realized_pnl DECIMAL(19,2) NOT NULL DEFAULT 0,last_price DECIMAL(19,2),mark_at TIMESTAMP NULL,
 trade_date DATE NOT NULL,close_requested BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL,last_checked_at TIMESTAMP NULL,last_error VARCHAR(300),
 INDEX ix_intraday_user(user_id,quantity)
);
CREATE TABLE IF NOT EXISTS paper_intraday_events (
 event_key VARCHAR(100) PRIMARY KEY,user_id BIGINT NOT NULL,position_id VARCHAR(36) NOT NULL,
 event_type VARCHAR(16) NOT NULL,units INT NOT NULL,collateral_delta DECIMAL(19,2) NOT NULL,
 realized_pnl DECIMAL(19,2) NOT NULL,fee DECIMAL(19,2) NOT NULL,occurred_at TIMESTAMP NOT NULL,
 INDEX ix_intraday_events(user_id,occurred_at)
);

