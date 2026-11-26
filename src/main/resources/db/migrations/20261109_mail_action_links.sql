-- Apply once to an existing database after backup and isolated rehearsal.
CREATE TABLE IF NOT EXISTS mail_action_links (
 mail_id BIGINT PRIMARY KEY,token_hash VARCHAR(64) NOT NULL,purpose VARCHAR(20) NOT NULL,
 CONSTRAINT fk_mail_action_outbox FOREIGN KEY(mail_id) REFERENCES mail_outbox(id) ON DELETE CASCADE
);