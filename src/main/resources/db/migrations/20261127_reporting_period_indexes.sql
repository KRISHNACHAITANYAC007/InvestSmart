-- Forward migration for owned date-range report selection.
-- Apply only through the managed ledger after backup and stopped writes.
-- Nonunique indexes do not change financial rows or uniqueness constraints.
CREATE INDEX ix_finance_event_user_time ON paper_finance_events(user_id,occurred_at,event_key);
CREATE INDEX ix_paper_fill_user_time ON paper_fills(user_id,created_at,id);
