-- Back up before applying. This migration adds Admin activity and adjustment history.
ALTER TABLE users ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN password_reset_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE auth_sessions ADD COLUMN last_activity_at TIMESTAMP NULL;
UPDATE auth_sessions SET last_activity_at=created_at WHERE last_activity_at IS NULL;
ALTER TABLE auth_sessions MODIFY last_activity_at TIMESTAMP NOT NULL;
ALTER TABLE orders ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
CREATE TABLE admin_balance_adjustments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 occurred_at TIMESTAMP NOT NULL,
 actor_id BIGINT NOT NULL,
 user_id BIGINT,
 amount DECIMAL(19,2) NOT NULL,
 balance_after DECIMAL(19,2) NOT NULL,
 reason VARCHAR(1000) NOT NULL
);
