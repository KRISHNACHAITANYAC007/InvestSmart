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
