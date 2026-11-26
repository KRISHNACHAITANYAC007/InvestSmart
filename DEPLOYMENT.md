# V2 deployment runbook

V2 is an educational simulator. Deploy V2 and complete its acceptance checks before starting V3. Start with one backend instance, a managed MySQL database, and the frontend/backend behind one HTTPS origin. Infrastructure, domain registration and provider display permission are operator setup steps; this change does not provision a public host.

## Release configuration

For the focused V2 private single-VPS staging phase, use [deploy/STAGING.md](deploy/STAGING.md) with its required IP-restricted override. It uses separate synthetic data and does not authorize public launch. Broader unimplemented broker-style catalogue features are deferred V3; required settlement/accounting completion remains part of V2.

Use [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md) as the single ordered working checklist and open user-input list. This guide supplies command/reference details. The expanded ticket requires `20261111_paper_ticket.sql` on managed existing databases; see [paper simulation rules](deploy/PAPER_TRADING.md).

### Portable release candidate

Run `node scripts/build-v2-release.mjs` to verify and package the JAR, `/api` frontend assets and runtime deployment files under `target/releases`. Use `--require-containers` to require isolated MySQL 8.4, backup/restore, restart and trusted HTTPS checks. Without Docker the archive explicitly records incomplete container validation; it is not a validated release. CI runs the required-container mode and uploads candidates only on success; adding the workflow does not establish that CI has run. See [RELEASE.md](deploy/RELEASE.md) for checksums, extracted-package commands and the fresh-production-database procedure. Existing local accounts and databases are preserved. Production launch gates remain separate.

### Single-host container bundle

`deploy/compose.yaml` builds the Java 21 backend and Vite frontend, runs MySQL 8.4 on a private network, and serves the app through Caddy on ports 80/443. Only the web service publishes ports. Docker Engine and Docker Compose are required on the deployment host. This is deployment preparation, not evidence of a live deployment or a completed MySQL restore rehearsal.

1. Complete automated verification and all release acceptance gates below. Copy `deploy/production.env.example` to ignored `deploy/production.env` and fill it privately. Set `MYSQL_ROOT_PASSWORD` separately from `DB_PASSWORD`, and use a regular database username. For this bundle use `DB_URL=jdbc:mysql://mysql:3306/investsmart`, `PORT=8080`, `VITE_API_URL=/api`, and matching HTTPS `PUBLIC_URL`/`CORS_ORIGINS`. Compose overrides the JDBC URL and backend port to these internal values. The optional AI/news settings can remain empty for V2.
2. Point the chosen domain at the host and allow inbound TCP 80/443 (UDP 443 is optional for HTTP/3). Caddy obtains and renews certificates; persist its data volume. This configuration assumes Caddy receives client connections directly, without an upstream CDN or proxy.
3. Check that the dedicated `172.30.60.0/24` network does not overlap the host, VPN or existing Docker networks. If changing it, update the web service address and `INVESTSMART_SECURITY_TRUSTEDPROXIES` together. The proxy replaces incoming `X-Forwarded-For`; the backend trusts only that exact proxy address.
4. From the repository root, run `docker compose --env-file deploy/production.env -f deploy/compose.yaml config --quiet`. Avoid printing expanded configuration, which includes secrets. Build with `docker compose --env-file deploy/production.env -f deploy/compose.yaml build`.
5. For a **new empty installation only**, run `docker compose --env-file deploy/production.env -f deploy/compose.yaml up -d`. MySQL executes the checked-in fresh SQL baselines only when initializing an empty data volume. Backend startup validates schema; it does not apply migrations. For an existing database, stop writes, back up, rehearse/restore and apply reviewed forward migrations before starting the backend. Never copy the fresh baseline over an existing database.
6. Verify `https://YOUR_DOMAIN/api/health`, signup email receipt/link destination, sign-in cookies/CSRF, SPA deep links, owner MFA and the acceptance flows below. Provision the owner only after the intended account exists and is verified. Stop the backend with `docker compose --env-file deploy/production.env -f deploy/compose.yaml stop backend`, then use `docker compose --env-file deploy/production.env -f deploy/compose.yaml run --rm --no-deps backend --spring.main.web-application-type=none --bootstrap-admin=true`. Set the documented `INVESTSMART_ADMIN_EMAIL` privately first. Restart with `docker compose --env-file deploy/production.env -f deploy/compose.yaml up -d backend`; enroll MFA and save recovery codes.

The database and Caddy volumes persist across ordinary `compose down`; do not use `down --volumes` against retained data. Back up the database through a protected database login path and back up encryption keys separately; store backups outside Docker volumes and rehearse restoration into an isolated installation. Updating environment passwords does not rotate credentials in an existing MySQL volume. Review and pin image digests for the actual release, rebuild after security updates, retain the previous image and matching database backup, and follow the coordinated rollback procedure below. The build context excludes local properties, environment files, IDE files and test/build output; production secrets enter the runtime environment only.

The container Dockerfiles skip backend tests during image assembly; run `./mvnw.cmd verify` (or `./mvnw verify` on Linux) on the exact release checkout first. Without Docker, image build/startup, network isolation, MySQL initialization and certificate issuance remain unverified.

Configuration references: [Caddy SPA/API routing](https://caddyserver.com/docs/caddyfile/patterns), [reverse proxy headers](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy), and [Compose health-based startup ordering](https://docs.docker.com/compose/how-tos/startup-order/).

An empty provider-neutral template is available at `deploy/production.env.example`. Copy it to the git-ignored `deploy/production.env` and fill it privately, or set the same keys in your hosting service's secret store. It contains no default credentials or guessed owner/domain. Java does not load `.env` files automatically; production still needs the variables in its process environment.

From the project root, check configuration and built artifacts without sending mail, touching the database or starting services:

```powershell
node --env-file=deploy/production.env scripts/check-v2-release.mjs
```

Diagnostics show field names only, never configuration values. A successful preflight verifies configuration shape and artifact packaging; it does not certify SMTP receipt, MySQL migration, account ownership, provider permission or browser acceptance. Without a filled private file, `node scripts/check-v2-release.mjs` reports missing production environment settings. It intentionally does not read the development `application-local.properties`; missing production variables do not mean the local database configuration was removed.

Build with JDK 21 and the project's Maven wrapper. Use Node/npm compatible with the checked-in frontend lockfile. Set these values through your hosting platform's secret configuration, never in source control:

| Variable | Purpose |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection; use a dedicated application account |
| `JWT_SECRET` | Strong random signing key, at least 32 bytes |
| `SECURITY_ENCRYPTION_KEY` | Separate random key, at least 32 characters; encrypts authenticator secrets and pending account mail |
| `UPSTOX_API_TOKEN` | Authorized market-data token |
| `PUBLIC_URL` | Public HTTPS frontend origin, without a trailing slash |
| `CORS_ORIGINS` | Exact allowed HTTPS frontend origin(s), comma-separated; no wildcard |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM` | Verified transactional mail sender; SMTP STARTTLS is required in production |
| `INVESTSMART_ADMIN_EMAIL` | Verified owner account selected only by the explicit non-web admin bootstrap command; does not grant access by itself |
| `PORT` | Backend port, default 8080 |
| `VITE_API_URL` | Set to `/api` at frontend build time for the recommended same-origin layout |

Keep the encryption key in backed-up secret storage. Replacing it without re-encrypting stored data breaks existing authenticators and pending mail. Do not paste secret values into chat, tickets, logs or this document.

### Local SMTP setup

The local/default profile now reads the same mail variables as the production template. Set them in the IntelliJ backend run configuration under **Environment variables**, or in the ignored `src/main/resources/application-local.properties`; never put credentials in tracked files. Supported environment names are:

```text
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-sender@gmail.com
SMTP_PASSWORD=YOUR_PROVIDER_APP_PASSWORD
MAIL_FROM=your-sender@gmail.com
PUBLIC_URL=http://localhost:5173
```

Equivalent private local properties are `spring.mail.host`, `spring.mail.port`, `spring.mail.username`, `spring.mail.password`, `investsmart.mail.from`, and `investsmart.public-url`. For Gmail, use a provider-generated app password for an account with two-step verification; never use the normal account password. Other providers must supply their own SMTP hostname, port, authentication and sender/domain verification requirements. The app uses SMTP authentication and required STARTTLS. Restart the backend after setting values; an already-running IntelliJ process does not inherit environment changes made in another terminal. After startup, check `/auth/mail-status`, register a test account, and verify actual inbox receipt and the link destination. SMTP acceptance/status alone is not proof of receipt. Without SMTP, registration emails and password-reset emails cannot be delivered: new accounts cannot verify/sign in, and users who forget their password cannot use email recovery. Existing verified accounts can still sign in. Do not bypass verification to compensate.

The Maven JAR excludes `application-local.properties` and equivalent YAML files even when they exist in the ignored local resources directory. IDE/classpath development can still use that file. Supply packaged production configuration through environment variables or an external protected configuration location.

Use the `prod` Spring profile. It enforces Secure cookies, managed schema, HTTPS configuration and explicit mail/encryption settings. The JWT cookie is HttpOnly and SameSite=Lax. The frontend fetches a CSRF token before writes; do not strip its `X-XSRF-TOKEN` header at the proxy. Old bearer/localStorage logins are intentionally invalid after this upgrade.

The first release assumes one backend worker. Security limits are persisted in MySQL. Before adding replicas, add distributed mail-job claiming and rehearse scheduled IPO processing under the intended topology.

### Queued action-mail upgrade (2026-11-09)

Existing installations must apply `src/main/resources/db/migrations/20261109_mail_action_links.sql` once after backup and isolated MySQL rehearsal, before starting the new build with schema initialization disabled. Fresh security baselines already include this table. It links mail to a hashed verification/reset token and cascades cleanup when the outbox row is deleted; no plaintext token is added to the schema.

Pending verification/reset mail from older builds has no action link and is discarded on its next delivery attempt. Users must request a fresh verification or password-reset message. Used, expired, missing and inactive-account action mail is also discarded before SMTP submission, including its prior delivery-failure record. Non-action deletion notices retain their delivery behavior. The validity check happens immediately before submission; SMTP acceptance still does not prove inbox receipt.

## Upgrade the database

The NIFTY options and equity conditional-order build additionally requires `src/main/resources/db/migrations/20261110_paper_options.sql` on existing installations, after backup/isolated rehearsal and schema inspection. Fresh security baselines include `option_positions`, `option_orders` and `equity_pending_orders`. These tables retain option positions, request-id deduplication, fill history and cash/share reservations; they must be backed up/restored together with users, holdings and equity orders. Account deletion removes their user records. The single-instance trading worker is enabled by default; `investsmart.options.scheduler.enabled=false` disables both options and equity conditional processing (used in tests). Keep one backend worker for this release. Due expiry/settlement work catches up after restart; missed intraday trigger ticks do not.

Exercise CE and PE lots, bid/ask depth failures, insufficient funds/holdings, duplicate retries, pending cancellation, concurrent equity/options/funds activity, protective stop gaps, day expiry and missing expiry-close recovery with controlled accounts before launch. Live NIFTY option API entitlement, real-MySQL migration and browser/device interactions remain operator acceptance checks. New options permissions are not implied by working equity APIs. Futures, naked option writing/margin, partial fills, OCO/brackets and trading charges are not implemented.

1. Stop application writes and take a database backup through your database provider or `mysqldump` using a protected login path. Do not put passwords in shell command arguments. Take a matching secret-store backup.
2. Restore that backup into an isolated MySQL database and rehearse the upgrade there first.
3. Inspect the actual schema. If `stocks.instrument_key` and its unique constraint are both absent, apply `20261110_stock_instrument_key.sql`; if the column exists but lacks uniqueness, inspect duplicate non-null values and add only the missing constraint after reconciliation. Never replay the column addition or infer instrument identities from ticker strings. Apply existing stock-listing and investment migrations only if their changes have not already been applied by local Hibernate schema update. These are forward scripts, not an automatic migration framework; replaying an `ALTER TABLE ADD COLUMN` fails.
4. Check for duplicate emails ignoring case/whitespace. Resolve collisions deliberately; do not merge users or discard holdings automatically. Audit existing ADMIN roles because the old user API allowed role edits. An old role value cannot authorize an owner in this release: the explicit CLI must populate the owner registry. New registration normalizes email and serializes account creation.
5. Apply `src/main/resources/db/migrations/20261104_account_security.sql` once, then ensure the security tables exist with the current `src/main/resources/db/security-schema.sql`. If the database has the older security schema but lacks passkey, lock and mail-failure tables, apply `20261104_trusted_device_unlock.sql` once instead. After `auth_sessions` exists, inspect `users` (`created_at`, `password_reset_required`), `auth_sessions` (`last_activity_at`), `orders` (`created_at`), `admin_balance_adjustments`, `admin_listing_overrides` and `admin_runtime_health`; apply the corresponding missing changes from `20261105_admin_v2.sql`, `20261104_admin_listing_overrides.sql` and `20261104_admin_runtime_health.sql` only. The current schema script already creates `last_activity_at`, the adjustment ledger, listing override table and persistent health table, and local schema update may already have added timestamp columns. The ledger's `user_id` must allow NULL so deletion can anonymize retained adjustment history; if an older table exists with `NOT NULL`, alter it after backup. The forward SQL is not idempotent: never replay an `ALTER` or `CREATE TABLE`, and never apply the trusted-device migration after the current schema has created those tables.
6. Verify holdings, orders, cash, IPO reservations and application counts against the backup. Existing accounts default to active with unverified email; require verification rather than claiming ownership was previously verified.
7. Start the production build with `spring.jpa.hibernate.ddl-auto=validate` and `investsmart.security.initialize-schema=false`. Missing security tables fail startup. Never enable schema auto-update in production to bypass a migration error.

The automated suite exercises schema initialization and business behavior on H2 in MySQL mode. It does not replace this real-MySQL upgrade rehearsal. Keep the tested backup until the release is accepted. To roll back: stop the new application, restore the matching database and application build together, and invalidate sessions; do not run destructive down-migrations against live accounts.

## Build and serve

```powershell
.\mvnw.cmd clean verify
```

From `frontend`, with `VITE_API_URL=/api` in the build environment:

```powershell
npm.cmd ci
npm.cmd run lint
node --test src/api/*.test.mjs src/auth/*.test.mjs
npm.cmd run build
```

For PowerShell, set `$env:VITE_API_URL='/api'` before the production frontend build. The preflight rejects a frontend bundle that still contains the development API URL. The development server keeps its localhost default when that variable is not set in its environment.

Serve `frontend/dist` as static assets, with unknown non-API paths falling back to `index.html`. Reverse-proxy `/api/` to the backend, stripping `/api` and preserving the HTTP method, cookies, request body and CSRF header. Only the frontend HTTPS origin should be publicly reachable; keep MySQL private and the backend accessible only through the proxy. Obtain a valid TLS certificate through the hosting platform. Do not expose local Vite dev servers publicly.

For correct source-based abuse detection, configure `investsmart.security.trusted-proxies` with the exact reverse-proxy addresses. The proxy must overwrite incoming `X-Forwarded-For` with the real client address. Other forwarding headers are not trusted. Without this configuration, requests through a proxy share its source limit.

Run the packaged backend with `java -jar target/InvestSmart-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`. `GET /health` reports only UP/UNAVAILABLE and checks the database/security schema. Configure the external health probe to use `/api/health`. Keep detailed server logs private.

## Set up your owner administrator

1. Register your own account and verify its email through the configured sender. Confirm ordinary sign-in works.
2. Stop the backend. Set `INVESTSMART_ADMIN_EMAIL` in the protected process environment to the same verified account address, then run the explicit one-time server-side bootstrap command against the same production environment and database:

```powershell
$env:INVESTSMART_ADMIN_EMAIL = 'YOUR_VERIFIED_EMAIL'
java -jar target/InvestSmart-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod --spring.main.web-application-type=none --investsmart.ipo.scheduler.enabled=false --bootstrap-admin=true
```

The equivalent explicit `--bootstrap-admin-email=YOUR_VERIFIED_EMAIL` option remains supported. `INVESTSMART_ADMIN_EMAIL` is only an account selector for `--bootstrap-admin=true`; setting it alone never promotes a user. `MAIL_FROM` is only the email sender and is not used as an owner identity.
3. After startup completes, stop that CLI process and restart the normal web backend. The bootstrap refuses an unverified account or a second owner. It does not run from a public endpoint or promote the first signup.
4. Sign in, add the displayed setup key to your authenticator as a six-digit/30-second account, and verify a code. Full admin access is granted only after verification.
5. Save the ten single-use recovery codes offline. They are displayed once. Administrator sessions expire after 15 minutes; user sessions after one hour. Sensitive admin account actions require confirmation within the last five minutes, using a fresh code when reauthentication is requested.

The Admin area has exactly five sections: Overview, Users, Security, Operations and Audit. Users supports account controls, single/all session revocation, forced password reset, rate-limited verification resend and reasoned ledger-based virtual-balance adjustments. Sensitive actions require recent password-plus-authenticator confirmation. A forced reset revokes sessions and blocks normal login until the account uses a reset link. Security alerts include repeated login/recovery failures, rejected orders and unusual Admin activity. Operations exposes order/rejected-attempt data, delivery state without email links or tokens, market-data freshness/provider telemetry, IPO settlement/job status, audited listing availability and full IPO timeline overrides. Admins may retry only due, unresolved IPO settlements using fresh provider data and the existing transactional settlement rules; resolved results cannot be edited. Audit history is read-only in the UI.

IPO and bond listing identities and terms remain provider-sourced; Admin may override availability for either asset and the complete IPO timeline, with validation, reason and audit record. These overrides do not edit provider data. Rejected order attempts are kept as security events, not order rows. Admin settlement actions only retry due unresolved applications and never permit direct balance, draw, or resolved-status edits. Market-provider and settlement-job latest health timestamps and cumulative failure counts persist in `admin_runtime_health`; quote timestamps and application statuses also persist. The owner cannot suspend themselves or edit balances/trades directly. There is no browser role-management or impersonation. Permanent deletion is limited to regular accounts and uses the separately protected deletion workflow above. Lost authenticator/recovery access requires controlled server-owner recovery; password reset does not remove Admin MFA.

### Local owner setup

For local review, first configure SMTP privately in the ignored local properties file, start the app normally, and verify the account's email. Stop the web backend before bootstrapping. From the project root, set `INVESTSMART_ADMIN_EMAIL` to that verified account and use the same local database, explicitly loading the external file since the packaged JAR excludes it:

```powershell
$env:INVESTSMART_ADMIN_EMAIL = 'YOUR_VERIFIED_EMAIL'
java -jar target/InvestSmart-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --spring.config.additional-location=optional:file:./src/main/resources/application-local.properties --spring.main.web-application-type=none --investsmart.ipo.scheduler.enabled=false --bootstrap-admin=true
```

Replace `YOUR_VERIFIED_EMAIL` with the verified account address. After the bootstrap finishes startup, stop its process and restart the normal backend and frontend on 8080/5173. Sign in with that email and its existing password, complete authenticator enrollment and store the displayed recovery codes. Admin then appears in navigation. Merely editing a role or clearing verification flags is not a supported setup method. These steps were not run against the user's database by the agent.

## Mail, monitoring and alert operation

Account email gets an immediate delivery attempt and is retried every 30 seconds up to five attempts; queued rows are removed after 24 hours. Verification links last 24 hours; reset links last 30 minutes. Resending invalidates the previous link and replaces unsent mail for that purpose. The UI reports missing SMTP configuration or recorded delivery failures using safe diagnostic messages; credentials and provider error details are never sent to users. Accounts remain unverified after any delivery failure. After correcting SMTP, request a fresh link rather than exposing encrypted payloads or token URLs. The Admin overview displays pending mail counts; Operations shows recipients, subject, attempts and delivery state only, never message bodies, links or tokens. A configured sender or accepted SMTP submission does not prove inbox delivery: validate actual receipt and spam placement before public signup opens. No delivery claim is made until that check is complete.

Platform passkey unlock is optional for regular, verified users and requires a recent full sign-in to enroll/remove. Configure `PUBLIC_URL` as the exact HTTPS frontend origin (HTTP localhost only for development) before starting; browser origin/RP ID must match. Passkeys store only credential identifiers/public keys and require user verification. The browser/device handles supported biometrics or screen-lock verification; InvestSmart never receives fingerprint or face data. Locking retains that one server session, while sign-out revokes it. Expired, revoked, reset, or new-device sessions require email/password again. Admin accounts remain on password plus authenticator MFA; passkeys do not replace MFA.

Alerts stay in the Admin Security inbox. Defaults are five account login failures/15 minutes, thirty source login failures/15 minutes, five access denials/10 minutes, five admin MFA failures/10 minutes, three recovery/verification requests/15 minutes, five rejected orders/15 minutes and five Admin actions/10 minutes. Login/MFA cooldowns apply; alerts do not automatically suspend accounts. The first four thresholds use `investsmart.security.login-failures`, `source-failures`, `access-denials` and `mfa-failures` (each under the same prefix); the latter three are fixed thresholds. Repeated alerts update their count within 30 minutes; new evidence reopens a resolved alert. Updating an alert's status/notes requires recent Admin password-plus-authenticator confirmation and creates an audit record. Use review notes to explain a false positive or action taken.

Security events retain 90 days, admin audit history one year, configurable through `investsmart.security.event-retention-days` and `investsmart.security.audit-retention-days`. Logs omit passwords, tokens, full network addresses and request bodies. Update the public privacy notice to describe account-security monitoring and retention before launch.

## Release acceptance

- Run all automated checks on the exact release build. Confirm anonymous/user/admin access, verification-required first login, resend/expiry/delivery errors, recovery, authenticator enrollment and replay rejection, passkey enrollment/one-time unlock challenge, lock versus sign-out, session revocation, cross-user denial, suspension/reactivation and audit records. Exercise Admin listing availability/date overrides and a due settlement retry with controlled provider data; verify each action is audited and resolved settlement outcomes cannot be edited.
- On a supported platform-authenticator browser, verify the platform-passkey prompt and biometric/screen-lock behavior. Confirm email/password fallback on unsupported devices and that Admin continues to require authenticator MFA.
- Use dedicated test accounts to check stock search/listing choice, add/watchlist refresh, all chart periods and both chart themes, virtual orders, IPO reservations/cancellation, portfolio accounting, and bonds' reference sources/coverage.
- Visually review desktop and phone layouts, Dashboard typography/index size, Markets index size, consistent panel/button corners, keyboard focus, dialog dismissal, browser Back and the one-screen heatmap. Automated geometry tests are supporting evidence only.
- Verify the configured Upstox account's exchange capabilities and permitted display/redistribution scope. A profile 401 remains unresolved even if quote/IPO APIs work; do not weaken capability checks to declare a pass.
- Verify health probes, backups/restores, mail receipt, trusted-proxy source attribution, retention and owner recovery-code storage. Deploy only after these operational checks pass. V3 stays deferred until V2 is accepted.


## Mandatory phone verification upgrade (2026-11-10)

This build requires all regular users to complete phone verification after their password. Existing accounts enroll at next sign-in; owner Admin retains password plus authenticator MFA. Apply src/main/resources/db/migrations/20261110_phone_verification.sql once after schema inspection and backup, before starting with schema initialization disabled. Fresh baselines include the three phone tables and auth_sessions.factor_completed. Existing sessions deliberately default to FALSE and must sign in again. Never replay the ALTER against a database whose local schema updater already added the column.

Set the final exact HTTPS PUBLIC_URL before phone enrollment. Pairing emails link to /phone with a one-use fragment token; the page removes it from browser history and exchanges it for a restricted setup cookie. Do not log response bodies, fragments, authenticator responses or private replacement links. Phone code access requires an internet connection, a supported platform passkey and user verification. It expires after five minutes; codes rotate every 30 seconds and work once. Keep recovery codes offline. Password reset does not remove phone verification. Replacing a phone revokes the old credential and application sessions.

MigrationLedger is a standalone Java operator tool independent of Spring startup. Run with target/classes and the MySQL Connector/J JAR on the Java classpath, using com.example.investsmart.deployment.MigrationLedger. Commands: --catalog (offline filenames/checksums), --status (read-only database history), --apply FILE --backup-confirmed --writes-stopped, or --adopt FILE --backup-confirmed --writes-stopped. Read DB_URL, DB_USERNAME and DB_PASSWORD privately from the environment; never pass secrets on the command line. Adopt only after independently verifying every statement's schema/data effect. The ledger serializes MySQL operators, checks normalized-file SHA-256, skips recorded migrations and refuses automatic replay after a STARTED/failed migration or checksum change. Apply reconciled files individually in the existing upgrade runbook's dependency order; catalog date order alone does not reconcile old installations. Back up the ledger with the database.

On 2026-11-10, fresh baseline, synthetic pre-phone upgrade, ledger apply/replay protection and backup restoration passed on an isolated local MySQL 9.7 instance. This is not acceptance of the Compose MySQL 8.4 target or of the actual application's existing database. Rehearse those before deployment. Real phone, owner Admin and desktop/mobile acceptance remain pending; a browser URL-verification barrier ended the visual attempt without bypass.


## Cross-device release checks

See deploy/V2_ACCEPTANCE.md for the phone/tablet/desktop viewport matrix and remaining acceptance gates. The shared origin must serve the SPA at /phone and proxy /api; non-loopback browsers now default to that same-origin API. The frontend uses safe-area insets and visual-viewport keyboard sizing. Validate Android Chrome and iPhone/iPad Safari with real device prompts before claiming device acceptance. Automated geometry and signed software-authenticator tests do not substitute for these checks.
