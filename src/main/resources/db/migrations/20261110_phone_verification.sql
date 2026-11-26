-- Apply once after the existing V2 migrations, after backing up the database.
-- Existing sessions deliberately require a new complete login after this upgrade.
ALTER TABLE auth_sessions ADD COLUMN factor_completed BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE phone_credentials (
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
CREATE TABLE phone_actions (
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
CREATE TABLE phone_sessions (
 id_hash VARCHAR(64) PRIMARY KEY,
 user_id BIGINT NOT NULL,
 credential_hash VARCHAR(64) NOT NULL,
 expires_at TIMESTAMP NOT NULL,
 INDEX idx_phone_sessions_user(user_id)
);
