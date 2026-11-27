# InvestSmart V3 feature inventory and acceptance matrix

AI-2 analysis update (2026-11-23): deterministic internal PortfolioAnalysisService implemented/tested (28 backend tests). It summarizes equity-only value/unrealized P&L and top-three listing concentration, combines duplicate listing rows, and withholds incomplete totals. No model egress, real risk score, daily attribution or whole-account return.

Historical interface assessment (now implemented locally; verification checkpoint in handoff): additive local/mock-only POST /agent/portfolio-analysis, strict topic enum plus explicit consent, existing session/active-user/CSRF checks and existing per-user request limiting before reads. No user/account selector, arbitrary prompts/actions or persistent history. A frontend opt-in view would show deterministic replies and timestamped source holdings, with loading/error/cancel/clear states and account-switch cleanup. Tests must cover default-off absence, authorization, CSRF, consent, quotas, rendering and stale/error cases; protocol tests cannot certify visual/device acceptance. Existing API contracts/financial writers remain unchanged. Live-provider activation/production or storage changes remain separately assessed.

AI-2 update (2026-11-23): owner instructed start after AI-1 review; read-only implementation is now authorized. First internal getMyPortfolio tool is implemented/tested (22 tests) with server identity/explicit consent, cached quote timestamps and equity-only weights/P&L. No LLM receives portfolio data; no public endpoint/UI/live provider added. AI-2 overall remains partial; other CORE priorities and deployment hold preserved. Older approval-pending notes below are historical.

## 2026-11-23 Advanced AI Agent decision (current authority)

InvestSmart Advanced AI Agent is a major planned V3 differentiator. This supersedes earlier basic-chat-only scope and undecided agent responsibilities; release priority remains nonblocking P2 until a separately approved change. CORE/DERIV/INVEST priorities and unfinished CORE-05-B2 work are preserved. Only AI-1 foundation is authorized now; AI-2 requires explicit owner approval.

Selected architecture: React -> existing authenticated Spring boundary -> orchestration -> provider abstraction -> allowlisted tool registry -> existing services. Financial calculations remain deterministic Java. Existing learning endpoint/component stay disabled/unrouted. AI-1 adds no endpoint or live provider and no portfolio tools. Separate agent flag defaults off; explicit local dev/test mock only, never prod. No database writes, persisted chats, Python service, vector store, Redis, additional agents or paid activation.

Plain Java interfaces are selected now: existing HTTP code is coupled to context/model parsing; isolate before choosing a live provider. Spring AI fits Spring but adds dependencies/BOM compatibility work; LangChain4j is a Java alternative rather than an additional stack; a provider-native SDK fits after provider selection but couples protocol. All were reviewed against official docs on 2026-11-23. No new dependency is justified for a mock-only foundation. Live model/provider/budget remain unselected.

| Phase | Scope | Acceptance before advancing |
| --- | --- | --- |
| AI-1 (implemented/tested 2026-11-23; 35 tests) | Provider-neutral contracts, default-off local mock, secure tool descriptors, bounded authenticated consent-aware single-turn foundation | Disabled/non-prod isolation, validation, secret-safe failures, identity not passed to model, no application tool execution; backend tests |
| AI-2 | Owned portfolio/holdings/P&L/trade reads, deterministic allocation/concentration and basic UI | Owner approval first; cross-user denial, missing/stale valuation and exact financial regression |
| AI-3 | Authorized stock/compare/news tools | Provider rights not presumed; timestamps/citations, metrics availability, no invented causal news |
| AI-4 | Bounded multi-step tool loop, hypotheticals, order preparation and short context | Step/time/token ceilings, cancel/failure/loop tests; preview uses existing services; no execution |
| AI-5 | User-confirmed watchlist/alerts and approved paper workflows | Server-bound normal authenticated confirmation, CSRF/MFA/ownership, idempotency/audit/recovery; model cannot confirm |
| AI-6 | End-to-end evaluations and optimization | Injection/authorization/accuracy/tool selection/failure/rate/cost/latency/rendered accessibility acceptance; no release-gate waiver |

Planned capabilities cover education, feature help, personalized paper portfolios/performance/risk, stock comparison, permitted news, hypothetical changes, paper-order preparation, confirmed watchlist/alert actions and bounded multi-step tasks. Examples: portfolio declines require comparable valuations and observed holding contributors; missing history means unavailable, not an invented daily return. Comparisons require verified metric inputs. Hypotheticals use existing rules with explicit quantity/rounding and missing-input handling; they execute nothing. News supports attributed context, not proof of price causation. Instrument ambiguity is resolved before a preview. The normal application workflow owns confirmation and revalidates prices/permissions; a conversational yes is not a reusable authorization.

Reuse map: PortfolioService/analytics for allocation/P&L; OrderService/PaperTrading previews for eligible scenarios; MarketDataProvider and existing listing identity for quotes/history; WatchlistService for scoped later operations; existing sourced RSS context is a partial starting point requiring provenance/freshness review. General alert operations depend on CORE-04 and remain unavailable until implemented. Historical valuation, volatility/fundamental metrics and complete fee reports cannot be assumed to exist. No duplicate ledger or invented model calculations.

Risks: legacy context may share sensitive holdings, lacks explicit maximum quote age in the learning path, and regex/citation filtering is not full safety validation. Before live use: least-privilege tools, principal bound outside prompts, request/schema validation, bounded execution and requests per user, redacted observability, provider retention/licensing review, explicit consent, safe rendering and injection evaluation. No arbitrary SQL/HTTP/commands, no real orders, no bypass of normal financial or security controls. Keep short-term context bounded in memory and nonpersistent by default; persistent memory/RAG requires demonstrated need and impact review.

Costs: zero external inference for mock development. Future cost estimates require measured tokens/tool turns plus current provider rates, taxes and retention/usage terms; free tiers are not unlimited public-use licenses. No purchases or Azure credits. Genuine new owner-dependent live-provider/privacy/budget decisions belong only in DEPLOYMENT_CHECKLIST.md. AI-off must preserve ordinary app startup.


CORE-01 specification milestone, reviewed **2026-11-23**. Authority: [V3_DEVELOPMENT_PLAN.md](V3_DEVELOPMENT_PLAN.md) and the approved CORE-01 execution plan. This inventory bounds existing catalogue work; it does not authorize all features, additional segments or disruptive changes. All money is simulated.

**Deployment remains PAUSED BY OWNER DECISION.** Historical release results remain **7 PASS / 2 FAIL / 17 BLOCKED**. No gate is recertified here; M01-M32 remain on hold in [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md), the sole open owner-dependency list. No retained database was opened, modified or migrated for this review.

## Classification and verification rules

- **Existing:** an implementation was located, with the stated boundaries. This does not mean release accepted.
- **Partial:** reusable implementation exists but does not satisfy the whole catalogue deliverable.
- **Missing:** no implementation for the named capability was found in the inspected controllers/services/routes/schemas; not a claim about unreachable external branches.
- **Provider-dependent:** required authorized data/specifications/calendars or provider acceptance remain unavailable/unverified. This is a separate dependency flag and may accompany existing/partial/missing.
- **Excluded:** outside the bounded current V3 catalogue; comparison material is not implicit scope authorization.

Evidence below is current working-tree source inspection on 2026-11-23. Test links establish where relevant checks exist, **not a new test run**. Historical outcomes are dated in [PROJECT_HANDOFF.md](PROJECT_HANDOFF.md) and [RELEASE_READINESS_REPORT.md](RELEASE_READINESS_REPORT.md). The newer frontend is not certified by the old dzYbes release artifact. Rendered UI, physical devices, protected human prompts, live data permission and host acceptance remain distinct.

## Public comparison references

All links were opened during this review on 2026-11-23. They identify comparison capabilities, not exact InvestSmart policies, current exchange specifications or a license to redistribute data. Revalidate operational details when implementing; do not copy broker-specific timing, leverage, fees or taxes into simulation defaults.

| Ref | Official source | Bounded use here |
| --- | --- | --- |
| K1 | [Kite orders](https://kite.trade/docs/kite/orders/) | Compare order lifecycle, AMO, baskets, alerts and product concepts; adapt only approved paper scope |
| K2 | [Kite marketwatch](https://kite.trade/docs/kite/marketwatch/) | Compare instrument selection/watchlists and market-depth presentation |
| K3 | [Kite charting](https://kite.trade/docs/kite/charting/) | Compare chart navigation and context; do not promise every chart/indicator |
| C1 | [Console funds statement](https://support.zerodha.com/category/console/ledger) | Compare user-readable statement and funds-history needs |
| C2 | [Console reports](https://support.zerodha.com/category/console/reports) | Compare reporting, tradebook and P&L concepts; no statutory document claim |
| I1 | [Coin mutual funds](https://support.zerodha.com/category/mutual-funds/understanding-mutual-funds) | Compare discovery, scheme details, units/orders/history and NAV concepts |

These references do not establish that InvestSmart has the same eligibility, coverage or legal status. Covered stock calls/cash-secured puts are requirements of the approved local roadmap, not a claim that a cited overview defines their contract lifecycle. Actual contract/NAV/calendar sources require their own reviewed provenance.

## Existing V2 baseline to preserve

Evidence groups B01-B10 link backend/API, frontend, schema and tests. References to groups in the V3 matrix inherit these concrete paths.

| ID / capability | Classification and current evidence | Acceptance boundary / V3 reuse |
| --- | --- | --- |
| B01 — authentication/account/owner controls | **Existing.** [SecurityAccounts](src/main/java/com/example/investsmart/security/SecurityAccounts.java), [SecurityConfig](src/main/java/com/example/investsmart/config/SecurityConfig.java), [AuthController](src/main/java/com/example/investsmart/controller/AuthController.java), [PhoneVerification](src/main/java/com/example/investsmart/security/PhoneVerification.java), [PasskeyService](src/main/java/com/example/investsmart/security/PasskeyService.java); [Account](frontend/src/pages/Account.jsx), [Admin](frontend/src/pages/Admin.jsx). Account/security/phone migrations in S01. [SecurityIntegrationTests](src/test/java/com/example/investsmart/security/SecurityIntegrationTests.java), [PhoneVerificationTests](src/test/java/com/example/investsmart/security/PhoneVerificationTests.java), [BrowserCsrfTests](src/test/java/com/example/investsmart/security/BrowserCsrfTests.java) | Reuse authenticated identity, active-user checks, MFA/CSRF, ownership and protected owner flows. Physical/protected acceptance remains open; no promotion or factor change |
| B02 — market discovery, quotes/charts/watchlists | **Existing; provider-dependent.** [MarketController](src/main/java/com/example/investsmart/market/MarketController.java), [EquityCatalogService](src/main/java/com/example/investsmart/market/EquityCatalogService.java), [UpstoxMarketDataProvider](src/main/java/com/example/investsmart/market/UpstoxMarketDataProvider.java), [WatchlistService](src/main/java/com/example/investsmart/service/WatchlistService.java); [MarketChartTerminal](frontend/src/pages/MarketChartTerminal.jsx), [Market](frontend/src/pages/Market.jsx), [WatchlistPanel](frontend/src/components/WatchlistPanel.jsx). Listing migration S01. [UpstoxMarketDataProviderTests](src/test/java/com/example/investsmart/market/UpstoxMarketDataProviderTests.java), [EquityCatalogServiceTests](src/test/java/com/example/investsmart/market/EquityCatalogServiceTests.java), [WatchlistListingTests](src/test/java/com/example/investsmart/service/WatchlistListingTests.java) | Preserve listing identity, unavailable/stale states and authorized data. Display rights/quotas and latest broader rendered acceptance remain open. Not general depth UI or all-product discovery |
| B03 — legacy equity/conditional orders | **Existing.** [OrderController](src/main/java/com/example/investsmart/controller/OrderController.java), [OrderService](src/main/java/com/example/investsmart/service/OrderService.java), [ConditionalOrders](src/main/java/com/example/investsmart/service/ConditionalOrders.java); [Orders](frontend/src/pages/Orders.jsx), [ConditionalOrderList](frontend/src/components/ConditionalOrderList.jsx). Equity pending orders S02. [ConditionalOrdersTests](src/test/java/com/example/investsmart/service/ConditionalOrdersTests.java), [OrderServiceListingTests](src/test/java/com/example/investsmart/service/OrderServiceListingTests.java), [OrderServiceMarketSessionTests](src/test/java/com/example/investsmart/service/OrderServiceMarketSessionTests.java) | Preserve shared accounting and held-unit restrictions; a pending conditional order is not an AMO queue or basket |
| B04 — expanded paper tickets/financing | **Existing within disclosed simulation scope.** [PaperTrading](src/main/java/com/example/investsmart/trading/PaperTrading.java), [PaperController](src/main/java/com/example/investsmart/trading/PaperController.java), [PaperJobs](src/main/java/com/example/investsmart/trading/PaperJobs.java), [PaperPolicy](src/main/java/com/example/investsmart/trading/PaperPolicy.java); [PaperTicket](frontend/src/components/PaperTicket.jsx), [PaperActivity](frontend/src/components/PaperActivity.jsx). Ticket/fill/finance schema S03. [PaperTradingTests](src/test/java/com/example/investsmart/trading/PaperTradingTests.java), [PaperPolicyTests](src/test/java/com/example/investsmart/trading/PaperPolicyTests.java), [paperTicket.test](frontend/src/api/paperTicket.test.mjs) | Preserve supported partial-fill/iceberg, trigger/protection/group-child behavior and financing. Existing grouped stop/target children are not multi-instrument baskets. Simulated fees/interest are not statutory charges |
| B05 — intraday/MIS long/short | **Existing; provider-dependent.** [IntradayTrading](src/main/java/com/example/investsmart/trading/IntradayTrading.java), B04 API/UI, S04 positions/events; [IntradayTradingTests](src/test/java/com/example/investsmart/trading/IntradayTradingTests.java), [paperTrading.test](frontend/src/api/paperTrading.test.mjs) | Preserve explicit entry/exit, collateral, partial closes, debt and guarded catch-up. MIS short equity is not an uncovered option position. Stale/missing quotes must not fabricate closure |
| B06 — long NIFTY options and owned exits | **Existing; provider-dependent.** [OptionMarket](src/main/java/com/example/investsmart/options/OptionMarket.java), [OptionTrading](src/main/java/com/example/investsmart/options/OptionTrading.java), [OptionController](src/main/java/com/example/investsmart/options/OptionController.java), [OptionJobs](src/main/java/com/example/investsmart/options/OptionJobs.java); [OptionsDesk](frontend/src/components/OptionsDesk.jsx), S02 options schema. [OptionMarketTests](src/test/java/com/example/investsmart/options/OptionMarketTests.java), [OptionTradingTests](src/test/java/com/example/investsmart/options/OptionTradingTests.java) | Existing SELL closes owned units; not option writing. Existing executable bid/ask quantities do not establish a general market-depth feature. Preserve freshness/depth rejection and disclosed expiry proxy; actual derivatives permissions remain unverified |
| B07 — wallet/portfolio/analytics | **Existing; reporting partial.** [WalletService](src/main/java/com/example/investsmart/service/WalletService.java), [PortfolioService](src/main/java/com/example/investsmart/service/PortfolioService.java), [PortfolioAnalyticsCalculator](src/main/java/com/example/investsmart/service/PortfolioAnalyticsCalculator.java); [Portfolio](frontend/src/pages/Portfolio.jsx), [FundsPanel](frontend/src/components/FundsPanel.jsx). Users/holdings/orders baseline S01 plus S03/S04. [PortfolioAnalyticsCalculatorTests](src/test/java/com/example/investsmart/service/PortfolioAnalyticsCalculatorTests.java), [PortfolioServiceTests](src/test/java/com/example/investsmart/service/PortfolioServiceTests.java), [WalletConcurrencyTests](src/test/java/com/example/investsmart/service/WalletConcurrencyTests.java) | Preserve historical weighted-average analytics and discrepancy/unavailable behavior. Current summary values and activity lists are not a reconciled, period-filtered report/export suite |
| B08 — T+1/BTST settlement/calendar | **Existing; provider/calendar-dependent acceptance.** [SettlementAccounting](src/main/java/com/example/investsmart/settlement/SettlementAccounting.java), [SettlementFunds](src/main/java/com/example/investsmart/settlement/SettlementFunds.java), [SettlementLots](src/main/java/com/example/investsmart/settlement/SettlementLots.java), [SettlementJobs](src/main/java/com/example/investsmart/settlement/SettlementJobs.java), [SettlementController](src/main/java/com/example/investsmart/settlement/SettlementController.java), [SettlementStatus](src/main/java/com/example/investsmart/settlement/SettlementStatus.java); [SettlementPanel](frontend/src/components/SettlementPanel.jsx), [AdminSettlementCalendar](frontend/src/components/AdminSettlementCalendar.jsx), S05. [SettlementFundsTests](src/test/java/com/example/investsmart/settlement/SettlementFundsTests.java), [SettlementLotsTests](src/test/java/com/example/investsmart/settlement/SettlementLotsTests.java), [SettlementProcessingTests](src/test/java/com/example/investsmart/settlement/SettlementProcessingTests.java), [AdminSettlementServiceTests](src/test/java/com/example/investsmart/settlement/AdminSettlementServiceTests.java) | Shared funds/allocations/lot journals and reviewed calendars are reusable. Existing /settlement/history lists obligations only. Missing calendar coverage stays pending/unavailable; no calendar-day shortcut |
| B09 — IPO simulation/bond discovery | **Existing; bond trading missing; provider-dependent.** [InvestmentController](src/main/java/com/example/investsmart/investment/InvestmentController.java), [IpoApplicationTransactions](src/main/java/com/example/investsmart/investment/IpoApplicationTransactions.java), [IpoSettlementJob](src/main/java/com/example/investsmart/investment/IpoSettlementJob.java), [BondMarketService](src/main/java/com/example/investsmart/investment/BondMarketService.java); [InvestmentMarket](frontend/src/components/InvestmentMarket.jsx), [IpoApplications](frontend/src/components/IpoApplications.jsx), S06. [InvestmentProviderTests](src/test/java/com/example/investsmart/investment/InvestmentProviderTests.java), [InvestmentApiTests](src/test/java/com/example/investsmart/investment/InvestmentApiTests.java), [IpoAccountingTests](src/test/java/com/example/investsmart/investment/IpoAccountingTests.java) | Saved interest and verified bond references are not bond execution. Preserve IPO reservations/transfers and unavailable data behavior |
| B10 — legal/branding/deployment/recovery | **Existing preparation; acceptance partial.** [LegalPages](frontend/src/pages/LegalPages.jsx), [legal content](frontend/src/legal/content.js), [App](frontend/src/App.jsx), [Navbar](frontend/src/components/Navbar.jsx), [compose](deploy/compose.yaml), [release builder](scripts/build-v2-release.mjs). [legal.test](frontend/src/api/legal.test.mjs), [FreshSchemaTests](src/test/java/com/example/investsmart/deployment/FreshSchemaTests.java), [MySqlReleaseTests](src/test/java/com/example/investsmart/deployment/MySqlReleaseTests.java). S01/S05 fresh baselines | Preserve KC attribution, disclosures, Docker/security/runbooks, artifacts and evidence. Host/secret/provider/device/mail/capacity/recovery acceptance remains canonical; no public release implied |

### Schema evidence map

Schema files are references only; no migration is applied in CORE-01.

| Ref | Managed migration / baseline evidence | Relevant data |
| --- | --- | --- |
| S01 | [app fresh baseline](deploy/mysql-init/001-app-schema.sql), [runtime security baseline](src/main/resources/db/security-schema.sql), [fresh security baseline](deploy/mysql-init/002-security-schema.sql), [SecuritySchema](src/main/java/com/example/investsmart/security/SecuritySchema.java), [MigrationLedger](src/main/java/com/example/investsmart/deployment/MigrationLedger.java); managed [account security](src/main/resources/db/migrations/20261104_account_security.sql), [phone verification](src/main/resources/db/migrations/20261110_phone_verification.sql), [stock listings](src/main/resources/db/migrations/20261102_stock_listings.sql), [instrument key](src/main/resources/db/migrations/20261110_stock_instrument_key.sql) | Identity/security/account records and listing-aware retained orders/holdings; catalogue includes additional owner/mail migrations |
| S02 | [paper options](src/main/resources/db/migrations/20261110_paper_options.sql) | option_positions, option_orders, equity_pending_orders |
| S03 | [paper ticket](src/main/resources/db/migrations/20261111_paper_ticket.sql) | paper_tickets, paper_fills, paper_financing, paper_finance_events |
| S04 | [intraday](src/main/resources/db/migrations/20261112_intraday.sql) | paper_intraday_positions, paper_intraday_events |
| S05 | [calendar](src/main/resources/db/migrations/20261111_settlement_calendar.sql), [accounting](src/main/resources/db/migrations/20261111_settlement_accounting.sql), [journal](src/main/resources/db/migrations/20261111_settlement_journal.sql), [checks](src/main/resources/db/migrations/20261111_settlement_checks.sql) | Accounts, obligations, allocations, lots, journal, allocation/lot events and dependencies |
| S06 | [investments](src/main/resources/db/migrations/20261103_investments.sql) | Saved interests and IPO applications; no mutual-fund unit/SIP or bond-position implementation found |

## Approved V3 catalogue: gaps and acceptance

All rows retain their parent roadmap priority. Dependency references identify needed inputs, not new owner-action lists. No prerequisite may waive B01/B08 accounting/security constraints.

| ID / priority | Classification / evidence / reference | Backend and API work | Frontend work | Database needs | Dependencies and acceptance |
| --- | --- | --- | --- | --- | --- |
| CORE-01 / P0 | **Existing after this inventory's QA:** code/source review and bounded matrix; K1-K3/C1-C2/I1 | No runtime API | No UI change | None | Every parent and bounded unit mapped; references/evidence resolve; existing vs accepted distinguished |
| CORE-05 / P0 | **Partial:** B07/B08, S03-S05; C1/C2 | Owned bounded journal/statement/fees/interest/P&L queries and exports from existing sources; explicit coverage/reconciliation | Funds/report views, period and unavailable/incomplete states, export controls | Reuse journals/events; schema/index additions only for demonstrated gaps, with managed/fresh/runtime parity | No reconstructed fake history; cross-user denial, no duplicate cash/P&L, precise rounding and concurrent-read/restart/restore tests |
| CORE-02 / P1 | **Missing AMO lifecycle:** B03/B04 existing conditional/day orders are not AMO; K1 | Persist AMO lifecycle, cancellations and opening-session revalidation before guarded execution | Explicit queued/opening/rejected/cancelled states | AMO lifecycle plus stable deduplication/reference records | B02/B08 reviewed sessions/calendars; races/retry/restart cannot double-fill or retain released reservations; missing opening inputs unavailable |
| CORE-03 / P1 | **Missing basket orchestration:** B04 reusable per-ticket preview/fills; K1 | Multi-instrument preview, shared reservations, per-leg submit/outcome and recovery | Basket editor/preview and partial-outcome review | Basket/leg lifecycle and idempotency records linked to existing funding | B03-B08; concurrent baskets cannot overspend/reuse held units; partial execution survives crash; no false all-or-nothing promise |
| CORE-04 alerts / P1 | **Missing user price-alert product:** security alerts in B01 are different; K1 | Owned definitions, deduplicated evaluation and trigger/history reads | Create/edit/disable alerts and review history | Definitions/triggers/deduplication lifecycle | Authorized fresh quotes; deterministic trigger tests, stale/unavailable behavior, ownership and restart duplicate suppression; external notifications not implicitly authorized |
| CORE-04 depth / P1 | **Partial/provider-dependent:** B06 executable option depth is narrower than general depth UI; K2 | Bounded authorized listing/contract depth DTO/read with timestamps | Depth display and explicit unavailable/stale states | No persistence by default; metadata only if justified | Authorized upstream coverage/quotas; no synthetic levels, no stale execution; listing identity and result bounds tested |
| DERIV-01 / P1 | **Missing futures product; provider-dependent:** B06 contracts, B08 funding patterns reusable; K1 product context only | Verified futures contracts, collateral, daily mark-to-market, close and final settlement | Contract/risk/collateral previews and obligation lifecycle | Futures positions/collateral and idempotent daily/final events | Verified contract/calendar/price inputs; replay/expiry/loss/debt/closure/restore invariants; disclose simulation methodology before implementation |
| DERIV-02 / P1 | **Missing secured writing; provider-dependent:** B06 sells owned options only; S02 not a writing obligation ledger | Covered stock calls/cash-secured puts with deliverable collateral and full settlement lifecycle | Coverage/locked assets, risk/obligations and settlement status | Coverage allocations and contract obligations linked to shared funds/lots | No uncovered writing; stock-contract coverage is additional to current NIFTY scope; concurrent sale/withdrawal cannot consume coverage; expiry/delivery/restore tests |
| INVEST-01 / P1 | **Missing NAV orders/units/SIPs; provider-dependent:** B09 saved interests/IPO data are not fund units; I1 | Verified scheme/NAV discovery, simulated subscriptions/redemptions and idempotent SIP schedules | Scheme/NAV provenance, units/orders and schedule management | Fund identity, order/unit accounting, schedules/execution events | Authorized applicable NAV/calendar/cutoff rules; no quote-as-NAV shortcut; duplicate schedule, cancellation, missing NAV, rounding and restart tests |
| INVEST-02 ETFs / P1 | **Partial generic equity infrastructure; ETF coverage unverified/provider-dependent:** B02 catalogue filters INE and NSE EQ, so ETF inclusion cannot be assumed | Verified eligible ETF identity and execution via existing shared equity rules where compatible | Product classification/disclosures and unavailable discovery states | Extend metadata only if verified inputs do not fit current model | No blanket filter widening; explicit verified eligibility cases and listing/settlement/ownership regressions |
| INVEST-02 bonds / P1 | **Partial discovery, missing trading; provider-dependent:** B09/S06 | Verified bond trading specifications, pricing, quantity/accrual/settlement handling appropriate to approved instruments | Product-specific preview, disclosures and position/history | Terms/position/events only where existing structures cannot represent obligations | Read-only bond discovery is preserved; no execution from incomplete reference metadata; test missing terms/quotes/calendar and exact cashflows |
| Advanced AI Agent / nonblocking P2 | Major planned V3 scope; original assistant partial/disabled, isolated AI-1 foundation under verification | Provider abstraction/tool contracts first; approved read tools/market/scenario/previews/confirmed actions later | Conversational interface planned, not implemented in AI-1 | No new schema or persisted chat in AI-1 | AI-2 approval required; ownership, freshness, financial rules and normal confirmation remain server controlled |

## Bounded development units

Execute in roadmap order: CORE-01 -> CORE-05 -> CORE-02/03/04 -> DERIV-01/02 -> INVEST-01/02 -> exact-source V3 acceptance. Within a parent, complete A before B/C. Each unit requires an implementation-impact review before disruptive changes. Complexity: M moderate, L substantial, XL major financial work; not time estimates.

| Unit | Priority / complexity | Bounded deliverable and dependency | Acceptance / completion evidence |
| --- | --- | --- | --- |
| CORE-01 | P0 / M | This inventory and evidence/acceptance map | Documentation QA; no behavior/schema change |
| CORE-05-A | P0 / M | Read-only owned settlement journal projection, coverage metadata and bounded queries; details below | Synthetic ownership/precision/order/empty/uninitialized/read-only tests; no migration or public API |
| CORE-05-B | P0 / L | Source-aware statement reconciliation and fees/interest classification, after A | Validate against allocations/obligations and product events; opening/partial-fill/refund/debt/interest scenarios; explicit missing-history coverage |
| CORE-05-C | P0 / L | Owned reporting API/UI and safe exports after B | Stable documented report semantics, bounded period/query/output, cross-user denial, CSV formula safety, no double-counted P&L and rendered acceptance |
| CORE-02-A | P1 / L | Persist AMO submission/cancellation/reservation lifecycle using shared locks | Idempotent submit/cancel, competing money writes, pending data states, managed schema parity |
| CORE-02-B | P1 / L | Opening-session worker and UI after A | Revalidate current eligibility/funds/price/calendar; restart and cancel/execution races cannot duplicate outcomes |
| CORE-03-A | P1 / L | Basket definition/preview plus authoritative funding plan, using existing per-leg validation | No false success/guaranteed fill from preview; concurrent fund/unit bounds |
| CORE-03-B | P1 / XL | Submission, per-leg journal linkage and recovery UI after A | Crash between legs, retry/cancel and partial rejection preserve spent/released amounts |
| CORE-04-A | P1 / L | User alerts/history, separate from security alerts | Owned management, trigger deduplication, stale quotes and restart replay |
| CORE-04-B | P1 / M | Authorized depth read/display using B02/B06 | No invented levels, timestamp/identity checks, bounded reads and unavailable UX |
| DERIV-01-A | P1 / L | Verified futures contract discovery and disclosed simulation-risk preview | Reject incomplete specifications; no activation from a quote alone |
| DERIV-01-B | P1 / XL | Collateral, daily/final lifecycle and UI after A | Exact reconciliation, idempotent mark-to-market, expiry, losses, restore |
| DERIV-02-A | P1 / XL | Verified stock-option obligations and secured coverage model | No naked entry; funding/lots shared with legacy routes; collateral sale/withdraw conflicts rejected |
| DERIV-02-B | P1 / XL | Covered-call/cash-secured-put lifecycle/UI after A | Partial exits, release, expiry/assignment/delivery and restart/restore reconcile without duplicate collateral |
| INVEST-01-A | P1 / L | Verified fund/NAV discovery plus simulated orders/units | Applicable NAV/rounding/settlement correctness; missing NAV unavailable |
| INVEST-01-B | P1 / L | SIP schedules and user lifecycle after A | Cancel/retry/missed-run/restart handling; no duplicate execution or manufactured NAV |
| INVEST-02-A | P1 / L | Explicit verified ETF coverage through compatible equity paths | Regress listing identity, lots/units and settlement; do not admit unsupported instruments |
| INVEST-02-B | P1 / XL | Bounded verified bond execution and history | Terms/price/accrual/settlement inputs complete; precise cashflows; current discovery unaffected |
| AI optional | P2 / L | Review existing sourced educational flow only after separately scoped work | Apply roadmap topic guidance; no personal-learning completion inferred; no autonomous account actions |
| V3 acceptance | P0 / XL | Full exact-source financial/security/provider/capacity/UI/recovery/candidate assessment | Current candidate evidence, not old V2 certification; deployment still requires separate resumption |

### Explicit exclusions and scope limits

- Real broker order execution, deposits/payments/bank transfers, depository access and broker-account promotion are excluded; wallet operations remain virtual.
- Uncovered option writing is excluded. New derivatives do not inherit production/exchange suitability from the paper simulator.
- Arbitrary currency/commodity/NPS/FD/international segments, corporate-action automation, tax-filing reports/statutory contract notes, research recommendations and exhaustive feature parity are excluded from the bounded current catalogue. Displayed enum strings or public product categories do not authorize them.
- Additional ETF/bond/fund instruments require verified eligibility/specifications under their parent task; no undefined 'all segments' expansion. New product classes outside these parents require a scope decision in the canonical process.
- Mobile packaging/publication and replicas/distributed-worker infrastructure remain later expansion, not CORE-01 deliverables.
- Autonomous AI account/trading actions, multi-agent infrastructure and RAG ingestion are not approved requirements. The learning roadmap and personal progress are unchanged by this inventory.

## CORE-05 source map and accounting hazards

2026-11-23 implementation update: CORE-05-B1 now provides an internal owned funding-evidence view via [SettlementFundingService](src/main/java/com/example/investsmart/settlement/SettlementFundingService.java). [CORE05_REPORTING_SEMANTICS.md](CORE05_REPORTING_SEMANTICS.md) defines MATCHED/MISMATCH/INCOMPLETE/NOT_EVALUATED and the 1000-row evidence bound. Focused16/16 and affected113executed regressions passed; isolated MySQL verifies both reports under commit interleaving and restart/restore (see queue for exact evidence). B1 compares RESERVE/SPEND/RELEASE with source allocations; it does not reconstruct historical cash or classify fees/interest. B overall stays partial; B2 is next. No financial writer/schema/API/UI change.

| Source | What can be reused | What it does not establish |
| --- | --- | --- |
| settlement_accounts / [SettlementOpening](src/main/java/com/example/investsmart/settlement/SettlementOpening.java) | opening_cash, current settled_cash, initialized_at; source-aware legacy reservation capture | Pre-capture cash transaction history; opening lots intentionally have null historical cost |
| settlement_journal / SettlementFunds | Unique (user_id,event_key), action, amount magnitude, reference fields and occurred_at; fingerprint protects replay identity | No signed per-bucket cash delta or stored running balance; amount is not inherently a debit/credit or P&L |
| settlement_allocations + allocation events | Funding-source linkage and reserve/spend/release amounts per event | Reserve then spend must not be counted twice; joins must be user-scoped and avoid allocation fan-out |
| settlement_obligations + dependencies + lot events | Reusable/restricted proceeds, due/pending/settled status and BTST delivery linkage | Current remaining credit is mutable; it is not a historical snapshot at an arbitrary past time |
| paper_tickets + paper_fills | Executed quantities/prices and simulated fees, legacy_order_id linkage | Ticket aggregates and per-fill fees are overlapping sources, not additive totals |
| paper_financing + paper_finance_events | Principal/interest snapshots, BORROW/REPAY/interest event provenance and accrued-date state | Snapshot interest plus interest events cannot simply be summed; financed cost/proceeds are not all profit |
| paper_intraday_positions + paper_intraday_events | MIS realized P&L, fee/collateral event detail and state | Position aggregate plus its events double counts; short-sale turnover is not free cash |
| orders/holdings + PortfolioAnalyticsCalculator + option records | Existing weighted-average realized/unrealized methodology and current ownership | Legacy incomplete trade history may yield unavailable analytics; no invented cost basis, fills or statutory taxes |

Source-derived finding: `/settlement/history` returns obligation entries with page size 50, not the funds journal, all product fees or a complete statement. Current summary uses settled/reusable/restricted/reserved buckets and a legacy fallback for uninitialized accounts. Reporting must not call financial initialization or mutate cash merely to display history.

Before a full statement, CORE-05-B must document and verify action-specific effects including SETTLED_CREDIT, WITHDRAW, PENDING_CREDIT, RESERVE, SPEND, RELEASE, SETTLE and REALIZED_CHARGE. Source-aware refunds can return to a pending obligation or settled cash depending on settlement state. Debt charges may emit additional funding events; logical presentation and reconciliation must distinguish linked events without discarding evidence. Do not infer all intermediate bucket balances from today's mutable rows. If retained evidence cannot establish a requested historic balance, report coverage/incompleteness rather than fabricate it. Any newly required capture fields need a separate approved managed migration; existing data must remain untouched.

## First implementation specification: CORE-05-A

Implemented 2026-11-23 after CORE-01: [SettlementJournalService](src/main/java/com/example/investsmart/settlement/SettlementJournalService.java), [repository](src/main/java/com/example/investsmart/settlement/SettlementJournalRepository.java), [internal DTO](src/main/java/com/example/investsmart/dto/SettlementJournalPage.java). Read-only REPEATABLE_READ uses REQUIRES_NEW so a weaker caller transaction cannot mix coverage and journal snapshots; callers inside transactions consume an additional connection and see committed state, not their suspended uncommitted writes. No endpoint/UI was added. Tests: [focused service tests](src/test/java/com/example/investsmart/settlement/SettlementJournalServiceTests.java), [isolated MySQL lifecycle test](src/test/java/com/example/investsmart/settlement/SettlementJournalMySqlTests.java) and real-JPA coverage in FreshSchemaTests. Focused 7/7 and enabled MySQL 1/1 passed; affected regression 104 executed/0 failures/errors with that opt-in MySQL test skipped on the default run. Evidence/commands in WORK_QUEUE.md. The initial MySQL harness quoting failure was corrected and all synthetic containers cleaned. Historical release gates unchanged.

**Goal:** create an internal, read-only settlement-journal projection with coverage metadata and meaningful tests. This is an initial reporting component, not a complete financial statement. No application implementation is included in CORE-01.

Implementation boundary:

- Constructor-injected report service/repository; caller supplies authenticated identity through existing account resolution and active-user checks. All SQL binds the resolved user ID. No endpoint accepts another user's ID, no arbitrary SQL and no model involvement.
- Internal DTO result: coverage (initialized boolean and nullable initializedAt), page/size, and entries with eventKey, action, amount, referenceKind, referenceId, occurredAt. Keep internal enum handling forward-compatible by retaining raw action strings; do not reinterpret unknown actions as zero cash effects.
- Use BigDecimal and preserve two-decimal journal magnitude. Call the field amount, not signedDelta/debit/profit. Return existing event keys/references only within the owning user's internal view; never include fingerprints or cross-user IDs.
- Page is zero-based, default 0; size defaults 50, allowed 1-100; page allowed 0-100000. Reject invalid/overflowing input before SQL; order occurred_at DESC,event_key DESC with bounded LIMIT/OFFSET. No total-count scan required. Cross-page snapshot/cursor semantics are a later public-report requirement; do not claim consistency across changing pages from this internal offset projection.
- Read coverage from settlement_accounts; if absent return initialized=false/initializedAt=null and only rows actually present. Do not call SettlementFunds.balances, SettlementAccounting.prepare or SettlementOpening.capture: these are financial write paths. Do not create an opening record or reconstruct older journal rows.
- Use one read-only transaction for coverage and one page, with the existing supported database transaction semantics; concurrency tests verify a self-consistent completed-state read on MySQL. Existing money writes retain the shared user lock; this projection adds no competing write lock or process mutex.
- No managed migration, new persistent ledger, changed financial writer, public API, UI or export in this unit. If adequate query behavior requires an index/schema change, stop that extension for its impact review instead of silently migrating.

Acceptance cases for the next unit:

1. Active owning user sees only their existing journal rows; unknown/inactive users rejected using established account error conventions; two-user data never mixes.
2. Equal timestamps sort deterministically by event key; empty/page boundary/invalid limit cases return the specified result or validation error.
3. Magnitudes and action/reference values round-trip exactly, including unknown actions; no derived running cash/P&L is asserted.
4. Uninitialized account stays uninitialized and unchanged; initialized coverage matches stored capture time. No initialization, journal, balance, reservation or holding writes occur.
5. A concurrent controlled financial transaction is observed as a completed snapshot, not partially committed events. Restart and isolated restore return the same stored page/order/precision for identical data.
6. Existing affected settlement/wallet/portfolio regressions remain intact. Add focused report tests using synthetic fixtures and a meaningful opt-in MySQL consistency/restore check; do not rerun an unchanged blocked long benchmark.

Impact assessment: additive internal read path; no intended auth-policy, public-contract, schema, security, financial-writer or infrastructure change. It can increase read load, so keep limits and inspect the existing user/time index/query plan using synthetic data. It does not validate production readiness. A later API/UI/export, altered accounting interpretation or new capture fields requires its own concrete specification and applicable approval before disruptive implementation.

## Completion record

CORE-01 is complete only after documentation QA confirms parent/unit coverage, evidence links, boundary statements and unchanged unrelated files. Verification commands/results are recorded in WORK_QUEUE.md and PROJECT_HANDOFF.md; this text alone is not QA evidence. Next milestone is CORE-05-A above, not automatic execution of the rest of V3. No new AI scope/prerequisite was discovered, so AI_LEARNING_ROADMAP.md and personal learning statuses remain unchanged.

## 2026-11-25 reporting acceptance reconciliation

CORE-05-A/B1 readers and scoped B2 funding/action/charge/period components have local evidence; B2 and B overall remain partial. The current requirement-to-source matrix and ordered technical gaps B-G01..06 are in [CORE05_REPORTING_SEMANTICS.md](CORE05_REPORTING_SEMANTICS.md). Latest selected execution is123 tests including enabled MySQL, recorded in handoff; this documentation review reruns no application tests. B-G01 counts/reasons and owned response integration now pass local/MySQL checks (135 selected tests); B-G02 owned financing period evidence now passes85 selected tests including MySQL; B-G03 pure ticket fee period slices are locally tested; NEXT owned batch fee evidence and MIS overlap checks, with no current snapshot/event sums or account financial totals. Per-position fee/interest checks do not certify account-wide period expenses or complete history. CORE-05-C API/UI/export remains unimplemented. No schema/writer/AI/release change; owner dependencies stay only in DEPLOYMENT_CHECKLIST.md.

### 2026-11-26 B-G03 adapter checkpoint

- [~] Owned execution-fee period batch reader implemented;93selected local tests pass. MySQL resilience suite running. MIS linkage and other-product completeness remain unfinished; no public reports or expense total. See CORE05_REPORTING_SEMANTICS.md and latest handoff.


Final adapter verification 2026-11-26:94selected tests pass with zero failures/errors/skips, including isolated MySQL concurrent snapshot, restart and deleted-fill/dump restoration equality. Earlier pending statements are historical. Owned period adapter verified within paper-fill scope; B-G03 remains partial pending MIS event linkage and other-product coverage. Exact command/evidence in latest handoff.


### 2026-11-26 B-G03 supplied MIS slice checkpoint

- [~] Pure supplied MIS event/ticket/full-fill slice linkage written; six tests and affected suite running. Owned period reader integration, MySQL resilience and other-product coverage pending. No account expense total/API/AI change.


Final supplied MIS slice verification 2026-11-26: initial99tests and final13tests pass with zero failures/errors/skips; seven new tests total including persisted-record agreement with the actual owned fee reader. Pure component is complete within supplied-evidence scope only. Owned period batch integration and actual MySQL checks are NEXT; MIS_EVENT_LINKAGE_NOT_CHECKED remains accurate. No complete expense total or public reporting acceptance.


### 2026-11-26 B-G03 owned MIS integration checkpoint

- [~] Owned period MIS linkage integrated with bounded batch context in same snapshot.104local tests pass; extended MySQL suite running. Raw selection retained on missing/conflicting/limited links. Event-only and legacy/option/expiry/IPO completeness remains unverified; no complete report or expense total.


Final owned MIS period integration verification 2026-11-26:105selected tests pass, zero failures/errors/skips, including actual MySQL concurrent event conflict/foreign identity withholding/restart/deleted-event/dump restoration. B-G03 complete within selected owned paper-fill evidence scope only; event-only and legacy/option/expiry/IPO completeness remain explicitly unavailable. CORE-05-B2 partial. NEXT B-G04 retained refund/pending-policy/composite provenance review; then B-G05coverage-aware summaries/B-G06query-cost review. No complete expense total/public reports/AI/release acceptance. Historical pending statements above preserve earlier scope.


### 2026-11-26 B-G04 retained provenance checkpoint

- [~] Writer/classifier review complete; pure pending instruction-policy checksum proof written with seven actual-writer-backed tests, affected suite running. Current status never proves release destination; parent-only composite charge stays incomplete. Owned batch integration/MySQL next. No writer/schema/API/AI change.


Final B-G04 supplied instruction proof verification 2026-11-26:107selected tests pass, zero failures/errors/skips, seven new actual-writer-backed tests. Original pending policy can be corroborated against full retained instruction fingerprint; current/refund availability is not inferred. Pure supplied component verified only; owned batch adapter and actual MySQL recovery next. B-G04/CORE-05-B2 remain partial; historical refund and parent-only charge gaps remain unavailable. Exact command/counts in latest handoff.


### 2026-11-26 B-G04 owned policy reader checkpoint

- [~] Bounded owned pending policy period reader written;113local tests pass. Private hashes/foreign candidates withheld; outside-period/settled/consumed candidates included. MySQL recovery suite running. No totals/refund reconstruction/API/AI change.


Final B-G04 owned pending policy reader verification 2026-11-26:114selected tests pass, zero failures/errors/skips, including actual MySQL old/new snapshot, foreign exclusion, restart, missing-candidate retention and dump restoration. B-G04 complete within retained-provenance review/owned original-policy scope; absent historical refund/parent-only charge proof remains unavailable. CORE-05-B2 partial. NEXT B-G05coverage-aware summary eligibility contract; no cross-snapshot sums, complete totals or public reports. Exact commands/counts in latest handoff.


### 2026-11-26 B-G05 supplied summary eligibility checkpoint

- [~] Pure per-bucket selected-journal settled/reserved direct-change eligibility written; eight tests and affected suite running. Unknown refund only blocks unknown bucket; composite parent/children stay outside direct scope. Full expenses/P&L/historical balances always unavailable. No sums/owned integration/API/AI change.


Final B-G05 supplied eligibility verification 2026-11-26:108selected tests pass, zero failures/errors/skips, including eight new scoped/actual-writer cases. Pure selected-direct-change eligibility only; no amounts, owned integration or independent MySQL acceptance. NEXT attach inside existing owned journal transaction and extend MySQL consistency/restart/restore checks. Full expenses/P&L/historical balances remain unavailable; B-G05/CORE-05-B2 partial. Exact command/counts in latest handoff.


### 2026-11-26 B-G05 owned eligibility checkpoint

- [~] Eligibility attached in existing owned journal snapshot with no extra SQL;112local tests pass, extended MySQL suite running. Still diagnostic statuses only, no amounts/totals/full expenses/P&L/historical balances. B-G05partial.


Final B-G05 owned eligibility verification 2026-11-26:113selected tests pass with zero failures/errors/skips, including actual MySQL concurrent source conflict, repair, restart and removed-child/dump restoration equality. Attached eligibility adds no SQL and no totals. NEXT eligible selected-journal direct-change sum rules with per-bucket null/zero semantics, then same-snapshot integration. Composite/full expenses/P&L/historical balances unavailable. B-G05/CORE-05-B2 partial; exact commands/counts in latest handoff.


### 2026-11-26 B-G05 selected direct-change sum checkpoint

- [~] Pure selected-journal signed sum rules written with per-bucket null/zero and exact wide aggregate semantics; nine tests/affected suite running. Rechecks cached eligibility; no partial composite/expense/P&L/balance totals. Owned reader integration/MySQL pending.


Final B-G05 supplied selected direct-change sum verification 2026-11-26:121selected tests pass, zero failures/errors/skips, nine new exact signed/null-zero/actual-writer/boundary/limit cases. Pure calculation verified only; owned reader does not return sums yet. NEXT same-snapshot attachment and MySQL source-change/restart/restore. No complete expenses/P&L/historical balances or partial composite totals. B-G05/CORE-05-B2partial; exact command/counts in latest handoff.


### 2026-11-26 B-G05 owned selected-change checkpoint

- [~] Selected direct-change sums attached inside existing owned snapshot;126local tests pass, extended MySQL suite running. No extra SQL. Unknown versus zero preserved; composite/full expense/P&L/balance sums unavailable. B-G05partial.


Final B-G05 owned selected-change verification 2026-11-26:127selected tests pass, zero failures/errors/skips, including actual MySQL source conflict/repair/restart/deleted-child/dump restoration equality. Owned reader now returns eligible selected direct cash changes with per-bucket null/zero; no new SQL. NEXT retain/revalidate existing bounded full charge proof for a separately scoped composite cash effect, never infer from identity grouping. Composite/full expense/P&L/historical balance sums remain unavailable; B-G05/CORE-05-B2partial. Exact commands/counts in latest handoff.


### 2026-11-27 B-G05 group/source binding

- [~] Pure immutable full source-proof/group binding written; six tests/affected suite running. No period amounts or reader behavior change. Owned batch retention/global proof-conflict checks/MySQL next; B-G05partial.


Final supplied charge group/source binding verification2026-11-27:132selected tests pass, zero failures/errors/skips, six new cases including actual owned writer negative-debt proof. Full source/context retained only on exact revalidated group match. Pure contract only; existing reader/direct sums unchanged, no composite period totals. NEXT bounded owned proof retention/global conflict checks/MySQL recovery. B-G05partial; exact command/counts in latest handoff.


### 2026-11-27 B-G05 owned grouped proof checkpoint

- [~] Existing bounded owned adapter retains revalidated immutable source proof after global grouping checks; no extra SQL or composite sum. Local suite running; MySQL recovery pending. B-G05partial.


Final owned grouped source retention verification2026-11-27:135selected tests pass, zero failures/errors/skips, including actual MySQL full proof snapshot/repair/restart/deleted-child/dump restoration equality. No extra SQL or composite sums. NEXT separately scoped composite-aware selected cash changes with fresh proof/global partition validation and no parent+child double count. Direct scopes unchanged; B-G05/CORE-05-B2partial. Exact commands/counts in latest handoff.


### 2026-11-27 complete-group cash calculation

- [~] Pure grouped signed cash rules written; six tests/affected suite running. Full source/global partition rechecked, parent and children must all be selected. Direct scopes unchanged; no owned integration/complete account totals. B-G05partial.


Final supplied complete-group cash verification2026-11-27:140selected tests pass, zero failures/errors/skips, six new actual-writer/overlap/window/stale/conflict/refund cases. Separate grouped scopes only; complete parent/children required inside period, original direct scopes unchanged. Pure result not yet attached to owned reader; NEXT same-snapshot attachment and MySQL recovery. No full account expenses/P&L/historical balances; B-G05partial. Exact command/counts in latest handoff.


### 2026-11-27 owned grouped cash checkpoint

- [~] Grouped calculation attached inside owned journal snapshot; four integration tests/affected suite running, MySQL pending. No new SQL. Direct/grouped scopes remain separate; partial windows unavailable. B-G05partial.


Final owned grouped cash verification2026-11-27:145selected tests pass, zero failures/errors/skips, including actual MySQL source-change/repair/restart/deleted-child/dump equality. Separately named grouped results attached in existing owned snapshot without extra SQL; complete-window -40.00settled/0.00reserved, partial/conflicting windows null. Direct/grouped representations never added together; no complete account expense/P&L/historical balance certification. NEXT B-G05remaining-scope acceptance review before B-G06query-cost/public reporting work. Exact commands/counts in latest handoff.


### 2026-11-27 summary acceptance and query-cost handoff

- [x] B-G05 complete within selected-journal summary contract after source/evidence audit. Financing/fees/policy stay source-specific; full expenses/account P&L/historical balances unavailable. No new tests run for this review; latest recorded journal suite145passing, production-reader hashes checked.
- [ ] B-G06 synthetic actual-query plan/workload measurements next; specification in CORE05_REPORTING_SEMANTICS.md. Static fill user/time and finance user/time scan risks require measurement, not automatic indexes. CORE-05-B2/public reports remain unfinished.


### 2026-11-27 B-G06 instrumentation and GitHub checkpoint

- [~] Actual report SQL capture implemented in test scope;2tests pass. MySQL plans/workload still pending; no production query/schema change. Reviewed source uploaded to progress branch; private scratch/settings retained locally. Deployment/history credential remediation remain separate.


### 2026-11-27 B-G06 first actual-query measurement

- [~] Isolated synthetic opt-in probe written/running for four narrow owned readers against100/2000added foreign histories, actual SQL plans/read counters/timings and DTO invariants. Capture regression2tests pass. No production index/schema/query change; performance acceptance pending.

### 2026-11-27 B-G06 measured narrow-reader scan growth

- [x] First synthetic actual-query observations captured;3selected tests pass including recovery. Finance2003event/fill2008row executed scans for1owned row at2000foreign histories; narrow journal/index context stays small in these fixtures. No performance acceptance/index change. Remaining owned-size/limits/charge/MIS/wide-range matrix next. AI foundation/local helper started, live model disabled; no AI implementation in this unit.

### 2026-11-27 B-G06 owned-candidate bounds

- [~]100/1000/1001owned-candidate actual-query scenarios written/running with raw-retention/proof-withholding/fixed-query assertions. This begins remaining check group1; groups2-4 unaccepted. No production/schema/AI change.

B-G06owned-candidate verification2026-11-27:16selected tests pass, zero failures/errors/skips, successful isolated cleanup/recovery.29actual-query observations include100/1000/1001owned settled/outside-period candidates with one selected instruction, fixed3queries, valid original policy through1000/null proof at1001/raw row retained. Reader elapsed samples25.17-85.91ms overall in these fixtures, not pure CPU/SLA; no performance acceptance or index change. NEXT selected-instruction bounds/many-instruction CPU and other measurement groups. Exact command/counts in latest handoff; B-G06partial.

### 2026-11-27 B-G06 many-instruction/range checks

- [~]1000x1000provenance matching CPU/wall samples,1001selected guard and empty/366day four-reader measurements written/running. No new production behavior; remaining batch/large-owned-product cases open.


B-G06history/CPU/range observations verified2026-11-27:16selected tests pass,51query observations/3pure samples.1000instructions x1000candidates ~942-992ms elapsed/~859-938ms current-thread CPU;1001selected guard fixed2reads/no proof. Four-reader empty/366day windows checked with truncation invariants. Repeated normalized instructions are synthetic provenance workload only. No SLA/index/production change. NEXT owned finance/fill boundaries and charge/MIS matrix, then growth/remediation review.


### 2026-11-27 B-G06 dedicated product/batch matrix

- [~] Fresh isolated measurement fixture plus owned finance/fill1000/1001and charge/MIS100/101batch assertions written/running. Earlier recovery evidence remains separate; new benchmark has no production/SLA acceptance.


B-G06fresh product/batch matrix verified2026-11-27:35selected tests pass,82actual-query observations. Owned finance/fill1000/1001guards pass; charge100/101source graphs use5/7queries and MIS100/101use4/6queries. No per-record service loop or economic-history certification. NEXT global overflow and disposable-fixture index A/B before any production schema impact review. No performance acceptance/index migration yet.


### 2026-11-27 B-G06 global guards and disposable index comparison

- [~] Child/source/fill-context overflow and synthetic index A/B scenarios written/running; no production schema change. Measurement/threshold/impact review remain open.


### 2026-11-27 B-G06 matching remediation

- [~] Request-local prepared checksum lookup replaces repeated whole-candidate matching in owned policy reader.20focused tests pass; full recovery/measurement run pending. No production database/schema changes; schema proposal approval separate.


B-G06 prepared matching verification 2026-11-27: separate recovery/affected run 47 passed; dedicated synthetic cost test 1 passed, 110 observations. Same-input pure control 1805-3036 ms versus prepared including preparation 6.92-11.03 ms; current-thread CPU clock granularity 15.625 ms means a 0 sample does not mean zero work. Full reader repeats 46.5-79.5 ms; guards and exact repeated reader DTOs unchanged. No production SLA. Run1 preserved in target/core05bg06-prepared-match-cost-run1.json and verification manifest. Next full large-input proof DTO equivalence, test fixture seed overhead reduction and independent rerun; schema implementation remains separately gated.

B-G06 repeatability verified 2026-11-27: enabled dedicated + prepared suite 6 tests passed, cost suite 80.999 s with 240 s bound unchanged. All 1000 full proof DTOs equal original oracle. Compared two fresh isolated runs: 110 observations, 32 reader shapes, 330 query samples, statement/type/returned-row shapes, cardinalities/guards and final index definitions equal. Run2 control 1648-2007 ms versus prepared including preparation 6.67-9.41 ms; reader 55.9-67.3 ms. CPU granularity preserved. Artifacts target/core05bg06-prepared-match-cost-run2.json and target/core05bg06-prepared-repeat-comparison.json include paired hashes. No SLA/concurrent-capacity acceptance; next mixed valid-source workloads.
