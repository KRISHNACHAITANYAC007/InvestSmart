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
 legacy_order_id VARCHAR(40),created_at TIMESTAMP NOT NULL,CONSTRAINT uq_paper_leg UNIQUE(ticket_id,leg_number)
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
 amount DECIMAL(19,2) NOT NULL,occurred_at TIMESTAMP NOT NULL,INDEX ix_paper_finance_history(position_id,occurred_at)
);
