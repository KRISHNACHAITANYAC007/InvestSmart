# InvestSmart — simple project overview

Current checkpoint (28 November 2026): all four listed reporting check groups passed within their tested scope. The bounded query-cost review is complete, including faster matching, approved indexes, schema parity and recovery checks. The read-only report API design and impact review is prepared; implementation has not started. Owner decisions are tracked in DEPLOYMENT_CHECKLIST.md. Report screens, exports, complete account totals and production-capacity acceptance remain unfinished. No retained database was changed and no live AI model is connected.

Updated: 2026-11-27. Start here for the big picture. Technical details and verification live in [PROJECT_HANDOFF.md](PROJECT_HANDOFF.md); study guidance lives in [AI_LEARNING_ROADMAP.md](AI_LEARNING_ROADMAP.md).

## What we are building

For a simple checklist of **done / working on / next**, read [V3_SIMPLE_CHECKLIST.md](V3_SIMPLE_CHECKLIST.md). It also explains AI terms in everyday words.

InvestSmart is a place to practise investing with virtual money. You can explore market information, use paper trades, follow your holdings and learn how trading works without placing real broker orders. V3 aims to make these simulations and their explanations more complete before public launch.

## What adding AI could look like

Picture a learning panel alongside the existing markets and portfolio screens. You could ask “What does diversification mean?” or, with your permission, “Explain the concentration in my portfolio.” The assistant would use available evidence, explain it in plain words and show its sources and timestamps. If information is missing or stale, it should say so.

This is the intended experience. An optional local preview now explains equity numbers using normal Java code; isolated browser checks pass, while full-app/device acceptance remains pending. The older learning assistant remains disabled and unrouted. The new local preview has an optional development-only screen and navigation link. The Advanced AI Agent is a major planned V3 feature; release priority remains nonblocking P2. No live model or multi-step agent is enabled.

An assistant explains information. A future agent could select approved read-only information tools, within the approved staged scope; those tools are not enabled yet. Neither would control your account, bypass permissions, invent prices or place real orders. Financial calculations and trading restrictions stay in normal backend code. A vector database, RAG or multiple agents are not currently required.

## Advanced AI direction ? 2026-11-23

The planned companion will analyze only your paper portfolio, explain observed performance/risk, compare permitted stock information, cite news, explore hypothetical changes and prepare paper-order previews. Bounded tools do the work through existing Java services. Confirmed app actions come later through normal authenticated confirmation; the model cannot confirm for you. AI-1 is an offline foundation only; AI-2 portfolio tools are now authorized; live services/actions remain gated. The feature remains default off, with no paid provider or persisted chats. CORE-05-B2 and other non-AI priorities remain intact.

## Done

- [x] Added internal plain-language equity summaries: available unrealized P&L and largest-listing weights, with clear missing-data warnings. Duplicate listing rows are combined. Twenty-eight backend tests passed. These summaries now feed an optional local preview; isolated synthetic browser checks now pass; full-app/device acceptance remains pending.

- [x] First AI-2 backend tool: reads only the active user's equity holdings with explicit consent, checks cached prices/timestamps and shows equity-only weights and available unrealized P&L. Twenty-two tests passed; this is not a finished chat screen or whole-account analysis.

- [x] Advanced AI Agent AI-1 foundation implemented/tested: offline mock, provider interface, empty tool registry and authenticated consent-aware boundary; 35 tests passed. No real portfolio tools or live provider are enabled. AI-2 read-only work is now authorized and in progress.

- [x] Preserved V2 accounts, data, security, paper trading and release work while prioritizing V3.
- [x] Prepared the V3 roadmap and feature inventory.
- [x] Created the personalized AI learning roadmap. This does not mean your personal study is completed.
- [x] Built the internal journal reader: it reads only the active user's records without changing money.
- [x] Built funding-evidence checks: reserve/spend/release records are compared with their source allocations; missing or inconsistent evidence is flagged.
- [x] Verified those reporting foundations with focused/regression tests and isolated MySQL concurrency, restart and restore checks. Exact results are in the handoff.
- [x] Removed two unused generated starter guides: HELP.md and frontend/README.md. Kept real project requirements and evidence.
- [x] Added and tested an internal financing-event classifier: borrowing contribution, interest accrued and interest paid stay separate. Five new tests and the existing funding/journal tests passed (21 tests total). An internal reader now connects these meanings to owned saved financing events. A report screen and complete totals remain pending.

## Working on now

- [~] AI-2: local consent-based conversation preview is implemented with three supported equity questions, cancel/clear controls and temporary history. All 33 selected backend tests and four frontend tests pass, along with lint and build. Synthetic browser interaction/layout checks now pass. Full-app/device acceptance remains pending; fee/interest reconciliation is next. No live model or whole-account analysis is enabled.

- [~] AI-2 is now authorized. The first internal portfolio tool reads only the signed-in user's equity holdings with consent and quote timestamps. Tests passed; the optional local screen reads this evidence; no live model receives it. Stale numbers stay unavailable.

- [~] Explain fees and financing interest without counting a charge twice. The internal interest, same-day fees, trade-overlap and date-range readers are built and tested. Funding-source checks now run within the date-range reader and pass local and MySQL integration checks. Linked-charge grouping now also passes local and MySQL checks. Unknown refund effects and complete account reports remain unfinished. Existing charging policies are unchanged.
- [~] Reporting overall: backend foundations exist; complete statements, reporting screens and downloads are still pending.

## Next and later

- [ ] Finish fee/interest source verification and period reconciliation.
- [ ] Add approved reporting APIs, screens and safe exports.
- [ ] Continue the scoped AMO, basket, alert/depth, futures, secured options, fund/SIP and eligible ETF/bond simulation work.
- [ ] Review and test the optional educational AI assistant before enabling it. Teach the relevant concepts from the learning roadmap during approved AI implementation.
- [ ] Complete V3 testing and production-readiness checks; launch only after deployment is explicitly resumed and approved.
- [ ] Android app after web acceptance, then iOS app.

## Why the other documents remain

The handoff is the technical history; the work queue tells the next developer what to do. The deployment checklist is the only list of open owner actions. Security/release reports preserve evidence; deployment instructions are needed when launch resumes. Technical plans explain implementation details. They are not disposable duplicates of this overview.

Deployment is **PAUSED BY OWNER DECISION**. M01–M32 remain on hold. Historical release status is **7 PASS / 2 FAIL / 17 BLOCKED**, not a V3 readiness certificate. No purchases, paid AI activation, credential changes or deployment are authorized by this overview.

Keep this file short and update Done/Working on now/Next after meaningful milestones. Use [x] only for evidence-backed completion. Put technical evidence in the handoff and personal study progress in the AI learning roadmap; do not create another owner-dependency checklist here.
