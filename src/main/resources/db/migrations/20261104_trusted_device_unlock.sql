-- Apply once after 20261104_account_security.sql. Existing account and session rows are retained.
CREATE TABLE passkey_credentials (
 credential_hash VARCHAR(64) PRIMARY KEY,
 credential_id TEXT NOT NULL,
 user_id BIGINT NOT NULL,
 user_handle VARCHAR(100) NOT NULL,
 public_key_cose TEXT NOT NULL,
 signature_count BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL,
 last_used_at TIMESTAMP NULL,
 INDEX ix_passkey_user(user_id)
);
CREATE TABLE passkey_challenges (
 challenge_hash VARCHAR(64) PRIMARY KEY,
 user_id BIGINT NOT NULL,
 session_id VARCHAR(64) NOT NULL,
 purpose VARCHAR(30) NOT NULL,
 request_cipher TEXT NOT NULL,
 expires_at TIMESTAMP NOT NULL,
 used BOOLEAN NOT NULL DEFAULT FALSE,
 INDEX ix_passkey_challenge_user(user_id,expires_at)
);
CREATE TABLE trusted_device_locks (
 session_id VARCHAR(64) PRIMARY KEY,
 locked_at TIMESTAMP NOT NULL
);
CREATE TABLE mail_delivery_failures (
 mail_id BIGINT PRIMARY KEY,
 error_code VARCHAR(40) NOT NULL,
 recorded_at TIMESTAMP NOT NULL
);
