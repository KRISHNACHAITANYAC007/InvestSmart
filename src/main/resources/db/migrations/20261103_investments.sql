-- Apply once on managed MySQL deployments before this application version.
-- Local ddl-auto=update creates these objects automatically. Retains existing account records.
ALTER TABLE orders ADD COLUMN source VARCHAR(255) NULL;
CREATE TABLE saved_interests (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, kind VARCHAR(10) NOT NULL, reference_id VARCHAR(180) NOT NULL,
 name VARCHAR(255), saved_at DATETIME(6),
 CONSTRAINT uq_saved_interest UNIQUE(user_id,kind,reference_id)
);
CREATE TABLE ipo_applications (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, ipo_id VARCHAR(180) NOT NULL, name VARCHAR(255), symbol VARCHAR(255),
 isin VARCHAR(255), exchange VARCHAR(255), status VARCHAR(255) NOT NULL,
 lots INT, lot_size INT, minimum_quantity INT, applied_quantity INT, allotted_quantity INT,
 maximum_price DECIMAL(19,4), reserved_amount DECIMAL(19,2), allocated_cost DECIMAL(19,2), issue_price DECIMAL(19,4),
 draw DECIMAL(20,18), probability DECIMAL(20,18), subscription DECIMAL(30,8),
 model_version VARCHAR(255), reason VARCHAR(1200), allotment_date DATE, listing_date DATE,
 applied_at DATETIME(6), resolved_at DATETIME(6), source_updated_at DATETIME(6),
 listing_order_id BIGINT UNIQUE, assumed_issue_price BIT,
 CONSTRAINT uq_user_ipo UNIQUE(user_id,ipo_id)
);
-- Scheduled resolution is guarded by the same transactional user lock as orders/funds.
-- Do not drop reservations or delete these records during upgrades.
