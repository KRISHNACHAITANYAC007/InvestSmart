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
