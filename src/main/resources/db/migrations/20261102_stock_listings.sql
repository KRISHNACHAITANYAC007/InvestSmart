-- Forward migration for deployments using managed schema migrations.
-- Apply once before deploying this version when Hibernate schema update is disabled.
-- Local profiles using ddl-auto=update create these nullable columns automatically;
-- StockMetadataBackfill safely fills existing NSE metadata on every startup.
ALTER TABLE stocks ADD COLUMN exchange VARCHAR(255) NULL;
ALTER TABLE stocks ADD COLUMN isin VARCHAR(255) NULL;
UPDATE stocks SET exchange = 'NSE', isin = SUBSTRING(instrument_key, 8)
WHERE instrument_key LIKE 'NSE_EQ|%' AND (exchange IS NULL OR isin IS NULL);
-- Existing stock IDs, holdings, orders and cash balances are deliberately retained.
-- BSE daily history uses BSE_EQ|<token> in the existing history cache namespace.
