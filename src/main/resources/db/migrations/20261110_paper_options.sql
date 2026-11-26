-- Apply once after isolated rehearsal and backup. Fresh baselines already include this schema.
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