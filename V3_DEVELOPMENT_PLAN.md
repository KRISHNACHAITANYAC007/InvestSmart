# InvestSmart V3 development roadmap

AI-2 analysis update (2026-11-23): deterministic internal PortfolioAnalysisService implemented/tested (28 backend tests). It summarizes equity-only value/unrealized P&L and top-three listing concentration, combines duplicate listing rows, and withholds incomplete totals. No model egress, real risk score, daily attribution or whole-account return.

Next interface assessment (not implemented in this unit): additive local/mock-only POST /agent/portfolio-analysis, strict topic enum plus explicit consent, existing session/active-user/CSRF checks and existing per-user request limiting before reads. No user/account selector, arbitrary prompts/actions or persistent history. A frontend opt-in view would show deterministic replies and timestamped source holdings, with loading/error/cancel/clear states and account-switch cleanup. Tests must cover default-off absence, authorization, CSRF, consent, quotas, rendering and stale/error cases; protocol tests cannot certify visual/device acceptance. Existing API contracts/financial writers remain unchanged. Live-provider activation/production or storage changes remain separately assessed.

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


Approved transition: 2026-11-23. V2 deployment is **PAUSED BY OWNER DECISION** because V3 development takes priority before public launch. This roadmap authorizes the documentation transition; it does not automatically start feature implementation. Open owner dependencies live exclusively in DEPLOYMENT_CHECKLIST.md.

Sequence: **V3 Development -> V3 Testing -> Production Readiness -> Website Launch -> Android App -> iOS App**.

## Baseline and scope

Latest documented release status is **7 PASS / 2 FAIL / 17 BLOCKED — production NO-GO**. These are historical results, not fresh certification. The focused V2 subset is 7 PASS / 2 FAIL / 15 BLOCKED; two additional P2 expansion gates remain blocked. Neither postponement nor roadmap approval changes a gate result. Previous candidate dzYbes certifies only its own source snapshot; the newer frontend already requires new candidate verification.

V2 implementations include account authentication/MFA/passkeys, owner administration, Upstox market data, charts/watchlists, equity and conditional paper orders, MIS, option buying, simulated financing, IPO applications, bond discovery, wallet/portfolio/history, T+1/BTST accounting, settlement administration, legal pages and KC attribution. Preserve all of these, retained accounts/data, tests, security controls, deployment configurations and release artifacts.

Implementation is distinct from acceptance: physical-device/protected flows, current rendered UI, provider permissions, production secrets, actual-host operations and continuous capacity remain unverified. The interrupted capacity attempt remains failed/invalid, not proof of an application-capacity defect. SEC-03 remains blocked.

Existing catalogue scope comes from WORK_QUEUE.md CORE-01..05, DERIV-01..02 and INVEST-01..02 and the phased catalogue recorded in PROJECT_HANDOFF.md. The earlier grounded-learning scope includes sourced news/context and partial UI/API/model integration. Learning backend beans are conditional on investsmart.features.learning-enabled, currently false; LearningChat is absent from current App routes. Historical startup concerns require reproduction, not assumption of a current enabled-runtime failure. Owner selection makes learning **optional P2**, excluded from V3 launch blockers. No paid model usage is activated.

## Prioritized development phases

Detailed working-tree evidence, classifications, bounded units and acceptance criteria are maintained in [V3_FEATURE_INVENTORY.md](V3_FEATURE_INVENTORY.md). It distinguishes implementation from acceptance and prepares the first CORE-05 reporting unit without changing application behavior.

P0 establishes scope/integrity or release acceptance; P1 features remain in the proposed V3 release; P2 is optional expansion. Complexity is relative: M moderate, L substantial, XL a major financial subsystem. These are not calendar estimates. Additional segments must be bounded by CORE-01 rather than treated as permission for arbitrary products.

| Phase / feature | Priority / complexity | Backend and API requirements | Frontend requirements | Database requirements | Dependencies and acceptance |
| --- | --- | --- | --- | --- | --- |
| 0 — CORE-01: bounded feature inventory | P0 / M | Map existing services/APIs and provider dependencies against the documented Kite/Console/Coin catalogue | Map current screens and missing workflows | Document schema coverage; no migration | Classify every feature existing, partial, missing, provider-dependent or excluded with source/code evidence and acceptance criteria |
| 1 — CORE-05: ledger, statements and P&L exports | P0 / L | Owned, bounded reports/exports using existing journals | Reconciled funds, fees, interest, settlement and P&L views | Reuse journals; indexes/read structures only for demonstrated needs | Reconcile reports to retained balances/trades; test cross-user denial and safe CSV output |
| 2 — CORE-02: after-market orders | P1 / L | Persistent queue, cancellation, opening revalidation and idempotent execution | Queued/rejected/cancelled states and opening disclosures | Queue lifecycle and execution deduplication | Reviewed sessions/calendars and fresh prices; test cancellation races, restart and unavailable opening data |
| 2 — CORE-03: baskets | P1 / XL | Preview, shared reservations, submission and explicit per-leg outcomes | Basket editor, preview and partial-outcome recovery | Basket/leg lifecycle, reservation references and request deduplication | Shared lock/ledger; test insufficient funds, concurrent submits, retries and crashes between legs |
| 2 — CORE-04: alerts and depth | P1 / L | Deduplicated price evaluation/history and authorized depth reads | Alert management/history; timestamped stale/unavailable depth | Alert definitions, trigger history and deduplication | No invented depth; test repeated polling, restart duplicates, stale quotes and ownership |
| 3 — DERIV-01: futures simulation | P1 / XL | Verified contracts, disclosed collateral, mark-to-market, closure and final settlement | Contract discovery, collateral/risk preview and lifecycle status | Positions, collateral and daily/final settlement journals | Authorized specifications/prices/calendars; test expiry, losses/debt, replay and restore |
| 3 — DERIV-02: secured option writing | P1 / XL | Covered calls and cash-secured puts with deliverable collateral and settlement | Coverage preview, locked assets and settlement explanations | Coverage allocations, obligations and settlement events | No uncovered writing; test concurrent collateral use, cancellation, expiry and delivery obligations |
| 4 — INVEST-01: mutual funds and SIPs | P1 / XL | Verified NAV discovery, simulated orders/units and idempotent SIP scheduling | NAV provenance, orders, units and SIP controls | Fund identity, orders, units, schedules and execution history | No invented NAV; test missing NAV, duplicate schedules, cancellation, calendar rules and restart |
| 4 — INVEST-02: ETFs and bonds | P1 / XL | Reuse eligible equity paths for ETFs; verified bond terms/settlement | Product-specific discovery, previews and disclosures | Extend metadata/accounting only where current structures cannot represent products | CORE-01 bounds additional segments; missing authorized specifications/prices prevent execution |
| 5 - InvestSmart Advanced AI Agent | Major planned feature; nonblocking P2 | AI-1 provider/tool foundation, then approved AI-2..6 personalized read tools, market context, bounded scenarios/previews and confirmed workflows | Conversational evidence/preview interface planned; existing UI remains unrouted | No persistent sensitive chat by default; no new storage in AI-1 | See current phase table above; AI-2 requires approval and no live/paid services enabled |
| 6 — V3 verification/readiness | P0 / XL | Exact-source regression, capacity/security review and candidate packaging | Rendered desktop/device/accessibility acceptance | Upgrade, fresh-schema parity, restore and reconciliation | Reassess canonical gates; deployment stays paused until separately resumed |

Missing authorized pricing, specifications, NAV or calendar data must fail unavailable. Any feature deferred from required V3 scope needs a recorded scope decision. Calendar days never substitute for settlement working days.

## Architecture, conflicts and approval boundaries

- Use constructor injection, service-owned business logic, DTO boundaries, the shared user lock and source-aware reservations. Journal/job writes must be idempotent; legacy routes obey the same constraints.
- Extend APIs additively where practical. Before disruptive auth, financial, schema, existing-contract, security or infrastructure changes, prepare a concrete impact assessment and obtain owner approval. Roadmap approval alone does not authorize such changes.
- Add managed forward migrations and matching fresh/runtime schema baselines for schema additions. Rehearse upgrade/restore on an isolated retained-data copy; never apply a fresh baseline over an existing database.
- Keep one backend worker. Distributed claims/replicas remain later P2 expansion. Use bounded queries, pagination and authorized read coalescing; measure quotas/performance without allowing stale execution or weaker rate limits.
- Preserve legal/simulation disclosures and current KC placement. New screens explain collateral, settlement, unavailable data and partial execution.
- This dated decision supersedes older instructions requiring V2 deployment before V3. Historical reports retain their original conclusions. New products compete for existing cash/holding reservations and must integrate with the shared accounting rather than parallel balances. Reports must reuse settlement history rather than introduce conflicting ledgers.
- SEC-03 still constrains source publication and hosting/benchmark activity. Local planning/development can proceed within applicable safety constraints; host evidence remains pending until deployment resumes. No authentication prompt or browser barrier may be bypassed.

## Testing and release acceptance

For each approved implementation unit, checkpoint WORK_QUEUE.md with changed areas, exact commands/results, failures and next action before long verification. Run meaningful affected financial/security regressions, including concurrent reserve/spend/cancel, retry deduplication, job replay, crash/restart and restore reconciliation. Existing tests/security requirements may not be weakened.

At readiness, run full backend/frontend/tooling checks, fresh online dependency audits, isolated MySQL migration/fresh-schema parity, retained-data upgrade/restore, packaged HTTPS/security checks, continuous steady/burst capacity acceptance and independently verified candidate checksums. Reconcile cash, credit, reservations, positions, fees/debt and journals. Old V2 artifacts cannot prove V3 readiness. Automated protocol tests do not certify rendered UI or physical devices.

Deployment resumes only through the existing canonical checklist and a separate owner decision. All M01-M32 tasks remain on hold, incomplete and available; no competing deployment checklist is created.

## Budget and next milestone

Personalized study and future AI teaching/maintenance guidance lives in [AI_LEARNING_ROADMAP.md](AI_LEARNING_ROADMAP.md). Its P0/P1 study prerequisites do not change AI's optional P2 release priority or authorize new features. Advanced-agent capabilities are now defined above; live provider/model/budget remain unselected. Preserve learner-owned progress and review affected topics during AI changes.

Use free/local tools where suitable; minimize subscriptions and preserve eligible GitHub Student Developer Pack benefits. Do not activate time-limited Azure credits prematurely. Eventual infrastructure should prefer monthly billing over long prepayments without weakening security or acceptance. No purchase or paid service activation is authorized.

CORE-01 and CORE-05-A are completed as recorded in the inventory and queue. CORE-05-B1 now adds internal owned source-evidence reconciliation for funding journal events, with focused/actual-writer/real-JPA tests and isolated MySQL snapshot/restart/restore verification. [CORE05_REPORTING_SEMANTICS.md](CORE05_REPORTING_SEMANTICS.md) defines interpretation and limits. No public API, schema or financial writer changed. CORE-05-B remains partial: next **B2 product fee/interest source classification and further statement/period reconciliation**; API/UI/exports remain CORE-05-C. Exact commands/results are in WORK_QUEUE.md; local tests do not establish release readiness. Do not automatically implement the whole roadmap. Owner dependencies and any required approvals remain only in DEPLOYMENT_CHECKLIST.md.
