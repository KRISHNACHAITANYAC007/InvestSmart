# V3 work checklist - simple words

Current checkpoint (28 November 2026): the approved C1 report API is implemented and passed 90 focused/regression tests. It returns only signed-in-user evidence, hides internal identifiers, preserves decimal strings and unknown amounts, and keeps saved financial records unchanged. Existing request limits and no-store protection pass. Report pages/exports and complete account totals remain unfinished. No retained database was changed and no live AI model is connected.

Updated: 27 November 2026.

**Where we are:** building better reports about your virtual money. A small portfolio helper also works in a local test preview. No real AI model is connected to that helper yet. V3 is unfinished and public launch is paused.

Use this file to follow the work and choose what to focus on. [Project overview](PROJECT_OVERVIEW.md) is the short introduction. [Technical handoff](PROJECT_HANDOFF.md) holds the test results and detailed history.

## How to read the marks

- [x] Done and checked for the specific task described.
- [~] Partly done; more work or checks remain.
- [ ] Still to do.

A checked item does not mean the whole feature is ready for public use. These are software work statuses, not your personal learning progress. All investing here uses virtual money.

## Done so far in V3

### Planning and keeping existing work safe

- [x] Wrote the V3 plan and listed which features already exist, need improvements or still need building.
- [x] Continued development without replacing existing accounts, holdings, balances or old trades.
- [x] Created the [AI learning guide](AI_LEARNING_ROADMAP.md). Your learning topics have not been marked finished by me.
- [x] Created this simple checklist. Future updates should keep Done / Working on / Next in step with the actual work.

### Reports about virtual money

- [x] Reviewed which selected cash summaries are supported and which wider results must stay unavailable. The next check measures how much database work the report readers need.

- [x] Tested calculations that count a complete verified combined-charge group once. Incomplete date windows stay unavailable. The owned reader now returns these grouped results and passes local and MySQL consistency, restart and backup checks.

- [x] The owned reader retains full verified funding evidence for combined-charge groups. Local and MySQL consistency, restart and backup checks pass. Composite cash calculations remain next.

- [x] Tested calculation rules for eligible selected cash changes, including negative changes and verified zero. The owned reader now returns eligible sums and passes local and MySQL consistency, restart and backup checks; unknown amounts remain unavailable.

- [x] Tested rules that decide whether selected settled-cash and reserved-cash changes have enough evidence for a summary. The owned reader now attaches these results and passes local and MySQL consistency, restart and backup checks. No totals are calculated yet.

- [x] Reviewed older refund and combined-charge evidence, and tested a rule for checking an original pending-credit policy against saved instructions. Its owned database reader now passes local and MySQL changing-record, restart and backup checks; missing refund destinations stay unknown.

- [x] Tested the rule linking one supplied same-day trade event, ticket and fill without adding the fee again. The owned date-range reader now uses this rule and passes local and MySQL changing-record, restart and backup checks.

- [x] Added tested rules for one ticket?s fees inside a date range, checked against its full fill history. The owned batch database reader now passes local and MySQL changing-record, restart and backup checks.

- [x] Added an internal date-range reader for simulated borrowing and interest across open and closed positions. Its local and MySQL changing-record, restart and backup checks pass; no expense total is invented.

- [x] Added tested coverage rules that count evidence problems and explain unavailable summaries. These rules now run in the date-range reader; local and MySQL checks pass.

- [x] Reviewed the reporting requirements against the tested readers. Recorded the remaining work and the reasons some summaries must stay unavailable.

- [x] Added rules to group a charge with its verified funding records in a selected date range. The database connection and its local/MySQL changing-record, restart and backup checks now pass.

- [x] Added a reader for one owned charge and its funding records. Its local and MySQL changing-record, restart and backup checks pass.

- [x] Connected date-range movements to their funding records in one consistent database view. Checked changing records, restart and backup restoration; unknown refunds remain unknown.

- [x] Built an internal reader for a user's saved money-history records. It reads records without changing money.
- [x] Built checks that compare money set aside, spent and returned with the records explaining where it came from. Missing or conflicting records are flagged.
- [x] Tested those earlier readers when records change at the same time, after a database restart and after restoring a backup.
- [x] Taught the report code to keep these separate: your contribution to a financed purchase, interest added and interest paid.
- [x] Connected those meanings to saved records for one financed position owned by the signed-in user. A position here means one recorded paper investment made partly with simulated borrowed money.
- [x] Checked that the new reader keeps exact amounts, blocks other users' records and leaves saved money records unchanged. Its latest test run passed 33 tests, including related reporting checks.

**What you can see today:** these report parts mostly work behind the screen. A finished report page and downloads still need building. The newest interest reader has now passed its own checks while records change, after a database restart and after restoring a backup.

- [x] Built the first rules to compare same-day trade events with saved units, money set aside and profit/loss. Checked app-created records with a loss of 40 followed by a gain of 60: gross profit is 20, with fees of 0.61 shown separately. These rules now have an internal reader for the signed-in user's saved records; a report screen is still pending.

### Portfolio helper: first parts of the planned AI feature

- [x] Built a local practice foundation for a future AI connection. It uses a fake response source for testing, with limits on requests and responses. Technical name: AI-1.
- [x] Built a reader that accesses only the signed-in user's equity holdings after permission. Equity holdings means shares in this part of the portfolio.
- [x] Added checks for missing or old prices. The helper leaves unsupported numbers unavailable.
- [x] Added explanations of share value, profit/loss on shares still held and the largest share holdings by value. The calculations use normal Java code.
- [x] Added an optional local helper screen with three fixed questions, cancel and clear buttons, and temporary replies.
- [x] Checked that changing accounts clears old replies and permission, and that cancelled requests cannot show a late reply.
- [x] Checked the screen in an isolated browser with made-up data, including narrow layouts. Fixed sideways scrolling at 320 pixels wide.

**What works today:** a developer can turn on the local preview. It explains existing equity numbers through three fixed questions. It does not yet offer general AI conversation, cover the entire account or place orders. Technical name for this partly finished stage: AI-2.

## Working on now
Current database-cost work has **4 check groups**, not a fixed number of individual tests:

| Group | Work | Status |
| --- | --- | --- |
| 1 | Larger owned histories and 1,000/1,001-row limits | Owned credit, instruction, financing and fill limits passed; matching improved and rechecked |
| 2 | Combined-charge and same-day-trade batches/context limits | 100/101 batching and global overflow checks passed |
| 3 | Empty and wide date ranges | Basic empty and mixed short/366-day ranges passed |
| 4 | Review growth, fix demonstrated problems if needed, and recheck | Passed within tested scope; production capacity stays a separate release check |

This is the current reporting priority, not a technical requirement to finish all V3 features before live AI. Live-AI setup follows its separate process in [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md); no setup is needed now.

- [~] **Main development task: complete the money-report checks.** The saved-money, interest, same-day trade and duplicate-record readers are built and tested. The date-range reader now includes funding-source checks; its new integration passes local and MySQL checks. Unknown refund destinations stay unknown. Date-range charge grouping now also passes local and MySQL checks. Complete account reports, report pages and downloads remain unfinished. Technical reference: CORE-05-B2.
- [~] **Portfolio helper: partly complete.** Its local screen and equity explanations are built. Checks inside the full application, broader keyboard/accessibility checks and real-device checks remain. Work on wider AI abilities comes later.

This checklist tracks the work as it progresses. Unchecked items remain unfinished even when related smaller parts have passed tests.

## Next, in the current work order

- [x] Checked the newest interest reader while records change, after a database restart and after restoring a backup. All 34 selected tests passed, including the temporary MySQL database check.
- [x] Built and tested the internal comparison of trade fees with the saved order total. The latest 62 tests passed, including an order filled in two parts. Example: if two trades show fees of 2 and 3, check the order total is 5; do not report 2 + 3 + 5 as 10.
- [x] Checked the fee reader while records change, after a database restart and after restoring a backup. All 63 selected tests passed, including the temporary MySQL database check.
- [~] Check fees and profit/loss for paper trades opened and closed on the same day. Internal readers and event-linked fee checks pass; complete account/period reporting is still unfinished.
- [x] Checked the same-day trade reader while records change, after restart and after restoring a backup. It keeps the old and new views consistent and blocks other users' access.
- [x] Compared same-day trade-event fees with their related orders and completed trades, counting each matched charge once. All 84 selected tests passed, including changing-record, restart and backup-restore checks. Missing or conflicting links withhold the verified fee.
- [~] Check links between old trade records and newer fill records so one trade is not counted twice. The first share-trade reader now passes 110 selected tests, including app-created trades. Its changing-record/restart/backup checks now pass (111 selected tests). The option-trade reader and its database resilience checks now pass too; use in complete reports remains unfinished.
- [x] Checked the share-trade overlap reader during record changes, after restart and after restoring a backup. Missing or conflicting old records stay unverified.
- [x] Built and checked the internal option-trade matching reader separately from share trades. All 116 selected tests passed, including changing-record, restart and backup-restore checks.
- [~] Combine verified share and option records for reporting, counting each matched trade once and keeping missing history visible. The first grouping rules pass 47 selected tests. They keep share/option IDs separate and preserve uncertain records. The database reader and report screen are still pending.
- [ ] Finish checks for complete reports over a chosen period. Missing history must be shown clearly.
- [ ] Build the report pages and downloads once their numbers have been checked.

## Other planned V3 features - still ahead

These describe the remaining V3 additions; some older, simpler features already exist. This list is not a promise that every item is starting now.

- [ ] **Orders outside market hours:** save a paper order and check it again when the market opens.
- [ ] **Order baskets:** prepare several paper orders together and clearly show which ones succeed or fail.
- [ ] **Price alerts and order-book information:** notify users about chosen prices and show available buy/sell levels when permitted data exists.
- [ ] **Futures practice:** simulate contracts with their required funds, daily gains/losses and expiry rules.
- [ ] **Secured option selling:** allow only the planned paper trades backed by the required shares or cash.
- [ ] **Mutual funds and regular investment plans:** simulate fund purchases using verified fund prices and scheduled investments.
- [ ] **More eligible ETFs and bonds:** extend trading practice only where the required product and price information is available.
- [ ] **Final V3 checks:** test the combined application, screens, security, retained data and backup recovery before calling it ready.

## What the AI helper may gain later

- [ ] Finish the wider portfolio explanations and checks planned for AI-2. Today's equity-only view is a first part.
- [ ] Explain or compare stocks and show sources for news. Technical stage: AI-3.
- [ ] Answer questions needing several allowed information lookups; explore “what if” examples and prepare a paper-order preview. Technical stage: AI-4.
- [ ] Support the planned app actions through the normal user-confirmation process. The AI cannot confirm an action for you. Technical stage: AI-5.
- [ ] Test the whole helper for wrong answers, privacy mistakes, failures, speed and usage cost. Technical stage: AI-6. Smaller tests already happen during development.

**What ?AI stays disabled? means:** the app is not sending your questions or portfolio to a live AI model. The optional local helper uses normal Java calculations and three fixed questions; general AI conversation and model-selected tools are still unfinished. V3 reporting development continues. This is a feature status, not a pause on the project.

A live AI connection is not enabled. Decisions and approvals needed from you stay in the [deployment checklist](DEPLOYMENT_CHECKLIST.md), which is the single place for those requests. This file does not authorize purchases, live services or launch.

## AI words translated

| Word in the technical notes | Simple meaning here |
| --- | --- |
| AI model / LLM | The software that generates a language answer. The local helper has no live connection to one yet. |
| Provider | The company or service supplying that model. |
| Mock | A fake replacement used for local tests. |
| Tool | A small function the application permits an assistant to use, such as reading your own holdings. |
| Agent | An assistant that can choose among allowed tools to work through a question. The full version is planned. |
| Deterministic | Normal code follows fixed rules to calculate or explain the same supplied facts. |
| Evidence / grounding | The saved records, prices and timestamps supporting an answer. |
| Consent | Your permission to read the relevant portfolio information. |
| Read-only | Reads information without changing it. |
| Backend / frontend | Code behind the screen / the screen you interact with. |
| API / endpoint | A route the screen uses to ask the code behind it for information. |
| Evaluation | Checking whether an answer or action behaves correctly. |
| P&L / unrealized P&L | Profit or loss / the estimated profit or loss on holdings you have not sold. |
| Reconciliation | Comparing records that should agree and explaining any difference. |
| RAG / vector database | Ways to find relevant text for AI answers. They are not required for today's helper. |

## How you can guide the work

You can say things like “continue the current reporting task,” “explain this item first,” or “focus on the helper screen next.” I will explain any effect on the work order or existing requirements before proceeding with a changed scope.

I will announce each new work unit with **new work !!!!** and explain it in simple words. After meaningful progress, I will update this checklist alongside the short overview and the technical checkpoint. You do not need to understand the stage codes to direct the project.
