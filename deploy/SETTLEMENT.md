# Paper settlement operations

Release template note: `settlement-calendar.csv.example` contains the accepted header only; importing it must fail until authoritative dates are reviewed. `calendar-review.example.json` is a human review worksheet, not runtime configuration or an activation mechanism. Keep it outside retained production data until reviewed; no dates, holidays or working-day rules are generated from these templates. Record provenance/checksum and use the existing guarded importer described below. Current release evidence is in RELEASE_READINESS_REPORT.md; older unit boundaries below are historical.

Latest worker/status unit: background batches process at most 20 pending obligations every 30 seconds under the shared user lock, refresh compatibility cash, and persist health under `t1-settlement`. Apply the additional reviewed `20261111_settlement_checks.sql` migration on retained installations before this build. Its independent attempt records provide retry rotation when financial work fails; they are account-owned and removed during audited account deletion. Unknown calendar data remains pending, not a fabricated successful settlement. Tests can disable scheduling with `investsmart.settlement.scheduler.enabled=false`.

Read-only authenticated `/settlement/summary`, `/settlement/history?page=1` and `/settlement/holdings` expose owned balances, bounded history and settled/T1 units. They never initialize opening records. The worker/status unit passed 81 selected backend tests; HTTP endpoint acceptance, presentation, portfolio enrichment and final release validation are separate pending tasks. Older standalone-foundation notes below describe earlier implementation boundaries.

Calendar and ledger foundations are implemented, and financial-route integration is under verification in WORK_QUEUE.md. Installing a calendar alone does not complete the settlement jobs or user presentation. The previously validated release archive predates these changes.

Calendars are exchange/segment-specific, operator-reviewed snapshots. Trading sessions and settlement working days are separate flags. Every calendar date in the imported range must be present, including weekends and closures; exceptional sessions must be explicit. Never generate production coverage by assuming Monday to Friday. No production calendar is bundled.

Use a UTF-8 CSV with this exact header:

```csv
date,trading_open,settlement_open,note
```

Rows contain ISO dates, literal `true`/`false`, and an optional single-line note. Quoted commas and doubled quotes are supported. Supply 1–1830 consecutive dates, at most 4 MiB. Independently check dates against the applicable clearing/exchange publication, including settlement-only holidays and exceptional closures. CSV validation checks structure, not source authenticity. Source URLs must be HTTPS without credentials, query strings or fragments. Keep the publication and review evidence with operational records.

For an existing managed database, back up and rehearse the forward migration `20261111_settlement_calendar.sql` before using this command. The runtime and fresh schema include the same tables. Do not apply fresh initialization over an existing database.

After building a JAR containing `CalendarImport`, validate without starting Spring or connecting to a database:

```powershell
java '-Dloader.main=com.example.investsmart.settlement.CalendarImport' -cp target/InvestSmart-0.0.1-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher --file reviewed-calendar.csv --exchange NSE --segment EQUITY --source https://example.org/public-calendar.csv --validate
```

Use the actual built JAR filename if different. The example URL is a placeholder, not a verified calendar source. Supported exchanges are NSE, BSE and MCX; segment identifiers are EQUITY, FNO, CURRENCY, COMMODITY, MUTUAL_FUND and BOND. An identifier does not establish product availability or authorization.

Only after independent review, privately configure `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` for the intended MySQL database, then replace `--validate` with `--reviewed --reason 'Reviewed against publication and dated operational evidence'`. Do not put credentials in shell command arguments or chat. Import uses one transaction, serializes activations, retains immutable versions and appends an activation audit. Reimporting the active content is idempotent. SHA-256 records exact imported bytes; it does not authenticate the publisher. An old version can be reactivated with a new review reason.

Maintain coverage spanning all unresolved trade dates and their next settlement dates. Activation selects one complete snapshot for each exchange/segment; importing only a new year may leave older unresolved dates uncovered. Historical versions remain retained. A correction must be reviewed before activation; financial reconciliation is part of the later settlement integration, not automatic calendar import behavior.

The calculator selects the first explicitly open settlement day strictly after the trade date. The paper cutoff is **18:00 Asia/Kolkata** on that day, not a broker payout guarantee. Missing coverage yields `COVERAGE_MISSING` with no guessed deadline; a known closed trading date yields `TRADING_CLOSED`. Neither result authorizes a settlement. Tests use synthetic calendars and make no claims about actual exchange holiday dates.

## Existing account preservation

The additive `20261111_settlement_accounting.sql` migration creates accounts, obligations, lots and funding allocations. DDL does not populate them or alter balances. The internal opening-capture service requires an existing financial transaction and locks the same user row as all money services. It is not exposed as an endpoint, scheduled sweep or startup migration. Its activation belongs to the later coordinated route integration.

Capture records the current free virtual balance as settled opening cash, preserving negative amounts. Existing positive aggregate holdings become settled opening lots; zero holdings are skipped and invalid holdings fail the transaction. Cost is deliberately null on these technical opening lots: historical orders and the existing weighted-average analytics retain cost basis. No historical trade is replayed or charged again.

Active equity, option and paper-order cash reservations and unresolved IPO reservations are recorded separately as settled-source reservations. They are already excluded from free virtual cash; capture never adds them back. Product positions, collateral, financing and historical trades remain unchanged. The user marker, lots and allocations commit atomically; repeat/restart capture cannot rebase an existing account. Any failure rolls back the marker too. Callers must flush earlier JPA writes before capture. All routes must join the settlement ledger before it becomes authoritative; the current standalone foundation does not enforce T+1 on the old trading or withdrawal paths.

## Internal ledger rules

`20261111_settlement_journal.sql` adds the append-only accounting event journal, per-source allocation events, lot events and delivery dependencies. All ledger mutations require the enclosing financial transaction and shared user lock. No existing database has been migrated by this development work. Production integration/release acceptance remains a separate task.

The internal `SettlementFunds` service separates free settled cash, reusable pending credits, restricted pending credits and reserved amounts. Withdrawals consume only free settled cash. New reservations allocate settled cash first, followed by eligible reusable credits in creation/id order; callers can require settled-only funding. Restricted credits cannot reserve an order. Negative settled cash blocks new reservations until paid. Partial fills and cancellations use stable, distinct accounting event keys; replays have no effect, while changed instructions using an existing key are rejected. Allocation journals retain the exact source of every reservation, spend and release.

A cancellation restores its pending source if that source has not cleared; otherwise it restores settled cash. Clearing adds only an obligation's remaining free credit. Already spent or reserved proceeds are never added again. Reserved proceeds remain reserved through settlement. The calculator rechecks the active reviewed calendar before clearing: corrected or missing coverage can postpone an unresolved obligation. Resolved outcomes remain immutable. The exact simulation cutoff is eligible, with earlier instants remaining pending.

`SettlementLots` records new purchases as delivery obligations and T1 lots, consumes settled units before unsettled units, and journals the original settled/unsettled sale classification. Partial sales never rewrite acquisition cost or resurrect sold units when delivery clears. This lot ordering determines settlement status only; the existing weighted-average historical analytics remain responsible for P&L. Provider-confirmed listing transfers can add settled lots without a second cash debit.

Restricted BTST cash can be linked to the actual unsettled delivery obligations from its sale events. It clears only after both its own T+1 deadline and all linked deliveries clear. Each dependency must be an owned share-delivery obligation; cash/dependency cycles are forbidden. An uncovered older purchase date therefore cannot be bypassed merely by installing calendar coverage for the later sale. The sale integration must create these links in its own transaction before exposing the resulting credit.

These services are internal foundations; all public financial routes, account deletion, scheduled catch-up and rendered funds/holdings views still need coordinated integration before users receive the complete T+1 behavior.

## Financial-route integration status

The latest checkout routes financial mutations through SettlementAccounting. The sections above describing standalone foundations record the implementation boundary before this integration. The compatibility virtual balance now projects settled plus reusable free cash; restricted credits are excluded. Deposits and additive Admin corrections credit settled cash; withdrawals, subtractive Admin corrections, IPO funding and financed collateral require settled cash. Equity sales split reusable settled-share proceeds from restricted BTST proceeds with delivery links. Option sale/expiry proceeds are restricted pending FNO settlement. Financed closes credit net equity proceeds pending settlement, while uncovered losses become disclosed cash debt. Fees use one idempotent charge event and cannot trap a closing position.

Existing installations require the reviewed forward migrations 20261111_settlement_calendar.sql, 20261111_settlement_accounting.sql, 20261111_settlement_journal.sql and 20261111_settlement_checks.sql. Back up and rehearse on an isolated restore; no existing account database was migrated during development. Fresh/runtime schemas already include these structures.

The bounded worker rotates at most 20 pending obligations per run with independent last-check observations, shared user locks and idempotent journals. Failed/missing-coverage obligations remain pending; health telemetry failures cannot suppress processing. Authenticated /settlement/summary, /settlement/history?page=1 and /settlement/holdings derive ownership from the session, never a supplied user ID. History pages contain at most 50 entries. Portfolio Funds shows settled/reusable/restricted/reserved cash, paper buying power and virtual withdrawable cash; Holdings shows settled/T+1 units.

Owner Operations displays persistent worker health and obligation counts. GET /admin/operations/settlement-calendars lists active coverage; POST activates explicit reviewed CSV, source, exchange/segment and reason after recent password/authenticator confirmation. The same strict CSV parser/checksum/mutex as the operator CLI is used; source review is an owner attestation, not a calendar generated from weekdays. Calendar and Admin audits commit with activation or all roll back. Resolved obligations remain immutable. Actual provider authorization, reviewed production calendars and rendered/device acceptance remain separate deployment gates.
