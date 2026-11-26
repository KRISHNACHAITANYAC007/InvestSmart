-- Existing databases only: after backup/rehearsal, confirm BOTH this column and
-- its unique constraint are absent. Skip if Hibernate or a prior upgrade added them.
-- Existing rows remain NULL until resolved from the authorized instrument catalog;
-- never invent listing identities from symbol strings.
ALTER TABLE stocks ADD COLUMN instrument_key VARCHAR(255) NULL;
ALTER TABLE stocks ADD CONSTRAINT uq_stocks_instrument_key UNIQUE (instrument_key);
