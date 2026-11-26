# InvestSmart

For a plain-language explanation of the project, planned AI experience and completed/upcoming work, start with [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md).

Deployment steps and anything needed from the operator are tracked in [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md). Screenshot-based Quick/Regular/MTF/Iceberg/Cover paper tickets, SL/SL-M, linked exits and simulated financing/fees are documented in [paper-trading rules](deploy/PAPER_TRADING.md).

An educational Indian-market investing app using virtual cash. Java 21 / Spring Boot / MySQL serve a React / Vite interface. Upstox supplies market information; simulated orders and IPO applications stay inside InvestSmart.

## Run locally

For a portable V2 release candidate, run `node scripts/build-v2-release.mjs` from the repository root. Use `--require-containers` on a Docker-capable host for MySQL 8.4/restore/HTTPS validation. See [release instructions](deploy/RELEASE.md). A prepared archive is not production launch approval; external acceptance remains required.

Prerequisites: JDK 21, Node.js/npm, and a MySQL database named `investsmart`. Configure your local datasource and JWT signing key in environment variables or the ignored `src/main/resources/application-local.properties`. Supply the authorized provider token using `UPSTOX_API_TOKEN`; never commit credentials. The property names are `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`, `jwt.secret`, and `upstox.api.token`. V2 sessions last one hour for users and fifteen minutes for administrators.

From the project root:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

From `frontend`:

```powershell
npm.cmd ci
npm.cmd run dev
```

Open `http://localhost:5173`. The backend runs on port 8080. Local Hibernate schema update adds new tables; managed deployments should apply the forward scripts in `src/main/resources/db/migrations` once, with a database backup and normal migration review. These scripts are not an automatic migration framework.

Vite uses port 5173 with strict port checking. Stop a duplicate project dev server if it is occupied; the frontend will not silently move to 5174. Existing processes are not automatically stopped.

V2 now uses revocable HttpOnly cookie sessions with CSRF protection. The old localStorage bearer token is discarded. Existing users must verify their email and sign in again; their cash, holdings and orders remain intact. Configure transactional mail for verification/recovery before testing account flows. Locally, use the ignored local properties file with `spring.mail.host`, `spring.mail.port`, any required SMTP credentials/TLS settings, `investsmart.mail.from`, and `investsmart.public-url=http://localhost:5173`. Mail goes only through that configured sender; no verification tokens are printed in responses or logs. Without SMTP, mail remains queued and account verification cannot complete.

## Where to find things

| Page | Features |
| --- | --- |
| Dashboard | Index snapshots, holdings and watchlist previews |
| Markets | Explore, Watchlist, IPOs, Bonds; stock search and listing selection |
| Chart workspace | Candles, ranges, light/dark mode, index sidebar, watchlist, virtual Buy/Sell |
| Heatmaps | One-screen NIFTY 50, four indices, paginated All stocks, fullscreen |
| Portfolio | Holdings, weighted-average performance, Insights, Funds, IPO reservations |
| Orders | Equity trades and IPO applications / cancellation |
| Account & security | Email verification, password change/recovery, active sessions and session revocation |
| Admin (owner only) | User lookup, suspension/reactivation, session revocation, security alerts and audit history |

Heatmap tiles have equal area, not market-cap weighting. Missing daily changes show N/A. NIFTY constituents come from the dated resource snapshot; All stocks uses the provider's NSE equity catalog. Saved filters are retained when returning from a chart.

## IPO practice

IPOs use the provider's open/upcoming/closed/listed data. A virtual application reserves the upper price band times the requested whole-lot share quantity from available cash. A user can have one application per IPO; retries return the existing application. Pending applications can be cancelled to release the reservation.

On startup and hourly at five minutes past the hour, the scheduler catches up applications due after **18:00 IST on the published allotment date**. `investsmart.ipo.scheduler.enabled=false` disables this job. With positive published overall subscription `S`, selection uses `min(1, 1/S)` against one stored server-generated draw. This is a deliberately simplified educational demand proxy, **not real retail allotment odds**; valid application counts, categories and SME rules differ. Requesting more lots does not increase this model's probability. A selection receives the published minimum quantity. Missing subscription or an invalid price pauses settlement; cancellation remains available.

Allocated cost uses the positive published cut-off, or the upper band with a visible assumption when cut-off is absent. Unused cash is refunded. After a fresh listed status, listing date, and catalog ISIN match, shares enter holdings with one IPO-origin cost record and no second cash charge. Unknown listing identity defers transfer. Available cash excludes pending reservations; total cash includes them. Unlisted allocations have known cost but no fabricated market value.

No real broker order, IPO payment or UPI mandate is submitted. Orders/funds/IPO writes share the same user-row lock to prevent double spending.

## Bond discovery

The provider's NSE GS/SG/TB and corporate N-series instruments are separate from paper equities. Bonds can be searched and saved. Quotes are requested only when opening details. Verified terms are joined by ISIN from the versioned `bond-reference.csv`; current reference coverage is limited. Missing coupon, maturity, face value and rating remain unavailable. Do not infer terms from a trading name or present coupon as yield.

The Bonds tab includes a **Verified terms** filter and displays how many matching catalog instruments have reference terms, before pagination. This is source coverage, not a guarantee that every field is known.

To extend coverage, add only verified official ISIN terms with source URL, source date and verification date; retain unknown fields as empty. This version has eleven reference rows: one NHAI issue from the [NSE bond table](https://www.nseindia.com/market-data/bonds-traded-in-capital-market), dated 26 October 2026, and ten government securities from the [CCIL notification dated 29 May 2026](https://www.ccilindia.com/documents/d/ccil/Default%20Fund%20Eligible%20Securities%20Notification_29_May_26-1780070041202-pdf). Verification dates are 3 November 2026. Government face value/rating fields remain unknown where the publication does not supply them.

Set `investsmart.bonds.reference-file` to an operator-maintained CSV file to overlay the bundled snapshot. Use the exact header `isin,coupon,faceValue,maturity,rating,sourceUrl,asOf,verifiedAt`. Overrides require valid dated ISIN terms and HTTPS sources, cannot predate bundled terms, and reject duplicate ISINs, future verification dates or invalid numeric values. This is a trusted operator-maintained file, not a public upload endpoint. Bond trading and calculated yield are not offered.

## User and owner access

Public registration always creates an ordinary user with virtual funds. Verified users can access their own account; legacy ID routes enforce ownership. Generic user CRUD is disabled, and manual stock CRUD requires an administrator, recent confirmation, a reason and an audit entry. Normal validated stock imports for watchlists/trading remain available.

The owner uses password plus an authenticator, with single-use recovery codes. Provisioning is an explicit server CLI operation after email verification, never an automatic first-signup promotion. A legacy ADMIN role alone is insufficient; the CLI selects the owner in a separate registry. Admin supports read-only account/investment review and suspension/reactivation/session revocation; it cannot edit balances, rewrite trades or impersonate a user. Security alerts use configurable rules and require human review. Alerts appear in the Admin inbox, with no external alert emails.

See [DEPLOYMENT.md](DEPLOYMENT.md) for the exact owner bootstrap command, migration sequence, SMTP and HTTPS configuration, monitoring, backup/restore and V2 acceptance checklist. V3 remains deferred until V2 is deployed and accepted.

For Admin on your development machine, follow [Local owner setup](DEPLOYMENT.md#local-owner-setup). The JAR deliberately excludes the ignored local configuration, so the local bootstrap command explicitly loads it externally. Admin appears after verified-email bootstrap and authenticator enrollment; it is not automatically assigned to an existing account.

## Verification

```powershell
.\mvnw.cmd test
# Optional read-only checks using the configured local provider account and isolated H2:
.\mvnw.cmd '-Dinvestsmart.live-check=true' test
```

From `frontend`:

```powershell
node --test src/api/*.test.mjs
npm.cmd run lint
npm.cmd run build
```

Backend tests cover provider payloads, stale/unavailable data, account ownership, IPO cash reservations/refunds/timing/listing, concurrency, portfolio cost reconciliation, listing-specific equity orders and market sessions. The live suite never places a real account trade or changes local user balances. A capability/profile 401 needs separate provider-account review even if market-data endpoints succeed.

For visual acceptance, check desktop and mobile heatmap fit, fullscreen, chart return, light/dark chart appearance, listing selection, saved interests, IPO cancellation and browser Back. The latest verification status and deployment work are in [PROJECT_HANDOFF.md](PROJECT_HANDOFF.md).

A single-host Docker Compose bundle is available in `deploy/compose.yaml`, with a Java backend, static frontend served through Caddy HTTPS, private MySQL, health checks and persistent volumes. Follow [DEPLOYMENT.md](DEPLOYMENT.md) for private configuration, fresh versus existing database setup, owner bootstrap and release acceptance. Container runtime, public TLS and real MySQL restore acceptance must be completed on a Docker host before launch.

NIFTY 50 CE/PE paper trading is under **Markets → Options**, with provider expiry/strike/lot discovery, LTP/bid/ask/open interest, long option positions and order history. Buy contracts or sell owned units using market, limit or protective sell stop-market tickets. Portfolio totals include saved option bid values and all reserved cash; expired positions awaiting settlement make valuation unavailable. **Portfolio → Options** and **Orders → Options** show positions and history. The NIFTY chart links to its option chain.

Equity tickets also offer **Limit · Day** and **Stop-loss market · Day**. Pending orders reserve cash or owned shares and can be cancelled in Orders; normal market sells respect reserved shares. Equity day orders expire at 15:30 IST. Option pending orders expire with their contract. Trigger processing polls bounded batches on a 30-second schedule while the backend is running; provider delays/backlogs can delay checks, missed ticks are not reconstructed, and stop orders can gap. There are no partial fills, brokerage/tax estimates, option writing, margin, futures or OCO/bracket orders in this implementation. It never submits an order to Upstox.

Option fills require sufficient provider ask depth for buys or bid depth for sells, fetched again for execution. Option sell-stop triggers use the bid; equity triggers use the selected listing quote and execution rechecks both the session and the limit. Expiry is cash-settled after 18:00 IST using the exact expiry-date NIFTY daily close as a disclosed educational proxy, **not an official exchange settlement price**. Missing dated closes leave settlement pending; no later quote or invented zero is substituted. Contract expiry and lot sizes come from the provider, not hardcoded weekly rules. Provider sources: [contracts](https://upstox.com/developer/api-documentation/get-option-contracts/) and [option chain](https://upstox.com/developer/api-documentation/get-pc-option-chain/). Confirm your account's derivatives data/display access before launch.

Provider contracts: [IPO discovery](https://upstox.com/developer/api-documentation/get-ipos/), [IPO details](https://upstox.com/developer/api-documentation/get-ipo-details/), [instrument catalogs](https://upstox.com/developer/api-documentation/instruments/). API access does not establish redistribution permission; verify active-account display terms before deployment. This app does not provide personalized financial advice or guaranteed returns.
