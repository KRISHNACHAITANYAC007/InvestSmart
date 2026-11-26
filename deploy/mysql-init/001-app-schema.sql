-- InvestSmart V2 fresh database baseline. Run ONLY against an empty database.
-- Existing databases use reviewed forward migrations, never this initializer.
-- Monetary columns without an explicit JPA precision use Hibernate's DECIMAL(38,2).
CREATE TABLE users (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(255), email VARCHAR(255), password VARCHAR(255), role VARCHAR(255),
 virtual_balance DECIMAL(38,2), email_verified BOOLEAN NOT NULL DEFAULT FALSE,
 password_reset_required BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 account_status VARCHAR(255) DEFAULT 'ACTIVE',
 CONSTRAINT uq_users_email UNIQUE(email)
);

CREATE TABLE stocks (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 symbol VARCHAR(255), instrument_key VARCHAR(255), exchange VARCHAR(255), isin VARCHAR(255),
 company_name VARCHAR(255), sector VARCHAR(255),
 current_price DECIMAL(38,2), previous_close DECIMAL(38,2), day_high DECIMAL(38,2), day_low DECIMAL(38,2),
 market_data_updated_at DATETIME(6),
 CONSTRAINT uq_stocks_instrument_key UNIQUE(instrument_key)
);

CREATE TABLE holdings (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT, stock_id BIGINT, quantity INT,
 CONSTRAINT fk_holdings_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_holdings_stock FOREIGN KEY(stock_id) REFERENCES stocks(id)
);

CREATE TABLE orders (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT, stock_id BIGINT,
 order_type ENUM('BUY','SELL'), status ENUM('PENDING','COMPLETED','CANCELLED'),
 quantity INT, price DECIMAL(38,2), total_amount DECIMAL(38,2), source VARCHAR(255),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_orders_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_orders_stock FOREIGN KEY(stock_id) REFERENCES stocks(id)
);

CREATE TABLE watchlists (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT, stock_id BIGINT,
 CONSTRAINT uq_watchlist_user_stock UNIQUE(user_id,stock_id),
 CONSTRAINT fk_watchlists_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_watchlists_stock FOREIGN KEY(stock_id) REFERENCES stocks(id)
);

CREATE TABLE market_price_history (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 symbol VARCHAR(255), `open` DECIMAL(38,2), high DECIMAL(38,2), low DECIMAL(38,2),
 price DECIMAL(38,2), volume DECIMAL(38,2), `timestamp` DATETIME(6)
);

CREATE TABLE saved_interests (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, kind VARCHAR(10) NOT NULL, reference_id VARCHAR(180) NOT NULL,
 name VARCHAR(255), saved_at DATETIME(6),
 CONSTRAINT uq_saved_interest UNIQUE(user_id,kind,reference_id)
);

CREATE TABLE ipo_applications (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, ipo_id VARCHAR(180) NOT NULL,
 name VARCHAR(255), symbol VARCHAR(255), isin VARCHAR(255), exchange VARCHAR(255), status VARCHAR(255) NOT NULL,
 lots INT, lot_size INT, minimum_quantity INT, applied_quantity INT, allotted_quantity INT,
 maximum_price DECIMAL(19,4), reserved_amount DECIMAL(19,2), allocated_cost DECIMAL(19,2), issue_price DECIMAL(19,4),
 draw DECIMAL(20,18), probability DECIMAL(20,18), subscription DECIMAL(30,8),
 model_version VARCHAR(255), reason VARCHAR(1200), allotment_date DATE, listing_date DATE,
 applied_at DATETIME(6), resolved_at DATETIME(6), source_updated_at DATETIME(6),
 listing_order_id BIGINT, assumed_issue_price BIT,
 CONSTRAINT uq_user_ipo UNIQUE(user_id,ipo_id),
 CONSTRAINT uq_ipo_listing_order UNIQUE(listing_order_id)
);
