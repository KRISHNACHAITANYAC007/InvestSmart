-- Apply once after the existing V2 migrations; back up first. Existing investments are retained.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
-- Resolve any case-insensitive email collisions before applying this unique index.
UPDATE users SET email=LOWER(TRIM(email));
CREATE UNIQUE INDEX uq_users_email ON users(email);
-- Then apply db/security-schema.sql. Production uses ddl-auto=validate.
