# InvestSmart Project Handoff

## 2026-11-27 prepared matching repeatability verified

- [x] .\mvnw.cmd -q -Dtest=ReportingQueryCostMySqlTests,PendingCreditPreparedTests test with both opt-in flags true exited 0: 6 tests, 0 failures/errors/skips. Fresh cost suite 80.999 s versus previous 239.641 s after test-only seed transactions; 240 s bound unchanged. No measured request has an ambient fixture transaction; all seed transactions committed first. Container cleanup empty. Log target/core05bg06-prepared-repeat.log; fresh suite counts target/core05bg06-prepared-repeat-tests.json.
- [x] Full 1000-policy DTO equality original/prepared outside timing. node target/compare-report-cost-runs.mjs passed: 110 observations/330 query samples/32 reader shapes; statement text/types/returned-row bounds, fixture cardinalities, guard scenarios and final index definitions equal across run1/run2. Structural acceptance only, performanceAcceptance=false. Run2 control wall 1648-2007 ms / CPU 1516-1797 ms, prepared wall 6.67-9.41 ms / CPU 0 or 15.625 ms (clock granularity). Reader repeats 55.9-67.3 ms. Run2 preserved target/core05bg06-prepared-match-cost-run2.json; comparison manifests include both SHA256 values.
- [~] Overview/checklist/inventory/semantics synchronized. Next: preserve reviewed work on existing GitHub progress branch, then extend mixed valid-source workload before global corrupt/overflow scenarios. B-G06 broader workload/performance acceptance remains partial; canonical schema dependency unchanged. Recovery snapshot is the earlier fresh 47-test run, not rerun unnecessarily for test-only fixture setup changes. No live AI or deployment.
## 2026-11-27 repeatability and fixture overhead unit written

- [~] ReportingQueryCostProbe now compares all 1000 complete original/prepared policy evidence DTOs outside timing and records matchingComparison. Synthetic foreign/candidate/instruction/product/charge/MIS/overflow seeding uses existing fixture write transactions; each commits before service reads/measurements. measure asserts no ambient write transaction. No production transaction/query/schema change; the original 240-second bound remains.
- [ ] Verification pending. Next compile/focused tests and independent enabled cost run; compare all shape/count/guard/index catalogs with preserved run1 before accepting repeatability. Existing 47 recovery + separate cost1 evidence covers the preceding source snapshot only.
## 2026-11-27 prepared matching actual cost verified

- [x] Dedicated .\mvnw.cmd -q -Dtest=ReportingQueryCostMySqlTests test with both V3_JOURNAL_MYSQL/V3_REPORT_COST=true exited 0: 1 test, 0 failures/errors/skips. Fresh report MySQL 8.4.13: 110 observations, 330 query replays, 32 reader shapes; node scripts/check-report-cost-evidence.mjs target/core05bg06-prepared-match-cost.json --prepared passed structural checks, performanceAcceptance=false. Recovery/affected run separately passed 47 tests; do not describe this as one combined run. Cleanup verified no labeled test container remains.
- [x] Same-input pure control wall 1805-3036 ms / thread CPU 1688-2578 ms; prepared including preparation wall 6.92-11.03 ms / CPU samples 0 or 15.625 ms. CPU clock granularity means 0 is not zero work. Optimized owned reader repeats 46.5-79.5 ms. Guards, bounded reads, repeated full reader DTO equality and disposable index equality still pass. Full report copied to target/core05bg06-prepared-match-cost-run1.json; SHA/commands/results in target/core05bg06-prepared-cost-verification.json; log target/core05bg06-prepared-cost.log.
- [~] Test suite time 239.641 s is close to its 240 s method bound. Next strengthen large-input full policy DTO equivalence outside timed samples and reduce synthetic fixture seeding overhead with explicit test-only transactions committed before measurements; retain bound and rerun for independent repeatability. Larger mixed workloads/performance acceptance remain open; schema owner dependency exclusively DEPLOYMENT_CHECKLIST.md. No retained database, API, financial state, live AI or learning-status change.
## 2026-11-27 prepared matching recovery verified

- [x] Separate enabled recovery/affected command (SettlementJournalMySqlTests,ReportingQueryCaptureTests,FinancePeriodServiceTests,ExecutionFeePeriodServiceTests,PendingPolicyPeriodServiceTests,PendingCreditPolicyReconcilerTests,PendingCreditPreparedTests,PeriodChargeAdapterTests) exit 0, all fresh Surefire reports: 47 tests, 0 failures/errors/skips. V3_JOURNAL_MYSQL=true / V3_REPORT_COST=false. Log target/core05bg06-prepared-recovery.log; exact command/suite counts target/core05bg06-prepared-recovery-verification.json. Synthetic container cleanup verified empty. Ownership, changing-record snapshot, restart and dump/restore assertions pass with prepared matching enabled.
- [x] node --test scripts/check-report-cost-evidence.test.mjs: 5 passed. Scoped git diff --check passed (line-ending notices only). Overview/simple checklist synchronized to the 47-test recovery milestone; cost comparison still pending.
- [~] Next dedicated cost fixture (V3_REPORT_COST=true), require prepared measurement structure and compare CPU/wall samples. B-G06 remains partial; no retained database/schema change, live AI or release acceptance.
## 2026-11-27 separate recovery verification running

- [~] Started recovery and affected suites independently of the cost fixture: .\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,ReportingQueryCaptureTests,FinancePeriodServiceTests,ExecutionFeePeriodServiceTests,PendingPolicyPeriodServiceTests,PendingCreditPolicyReconcilerTests,PendingCreditPreparedTests,PeriodChargeAdapterTests test with V3_JOURNAL_MYSQL=true / V3_REPORT_COST=false; log target/core05bg06-prepared-recovery.log. Results pending.
- Process inspection correction: Get-Process returned no Java rows, but Win32_Process positively identifies the live Maven and forked Java processes. Use CIM process evidence and fresh reports; the earlier interrupted run remains unaccepted, but absence from Get-Process alone does not prove Java stopped. Next: inspect completion, then run dedicated cost fixture separately.
## 2026-11-27 full matching run interrupted/unaccepted

- [ ] Combined recovery+cost run produced no fresh MySQL/measurement report, no Java process visible, while exec handle kept yielding and exact labeled synthetic container remained. Interrupted stale command; parsed docker inspect JSON verified journal-synthetic label before removing only that exact4d3a6c6dcontainer/anonymous volumes. No retained DB affected. No full48test PASS claimed.
- [x] Focused Java20/20and final Node validator5/5passed; validator accepted existing110observation structure only, performanceAcceptance false. NEXT separate recovery/pending tests from cost measurement, bounded run output and fresh report timestamps. Schema approval remains canonical/pending; no migration applied.

## 2026-11-27 prepared matching equivalence tests written

- [~] Five cases compare original single-row oracle with prepared path across header/fingerprint/missing/invalid/ambiguous/limited matrix, immutable request copies,1000/1001bounds, invalid unrelated candidates and Unicode/money normalization. Probe now records control vs prepared pure wall/CPU including preparation, with optimized reader same DTO checks. Removed legacy cost hook from recovery class because dedicated cost lifecycle now owns measurements; both classes can run independently under shared resource lock without duplicate benchmark.
- [ ] Tests not run. NEXT focused local then enabled recovery + dedicated cost + pending suites. No source-index migration implementation before approval.

## 2026-11-27 B-G06 request-local matching optimization written

- [~] Added immutable request-local Prepared lookup: validate/deduplicate <=1000candidates and hash once, then invoke unchanged single-row header/selected collision-bucket proof rules for exact statuses/error precedence. Invalid unrelated candidate still invalidates every applicable row; ambiguity retained. PendingPolicyPeriodService prepares once inside same owned snapshot; no static/persistent cache, extra SQL, new amount or authorization change. Single-row API retained as comparison oracle.
- [ ] Tests/optimized benchmark not run. NEXT exhaustive equivalence/copy-isolation/header/limits tests then local and actual MySQL old/new/recovery suite. Schema-index repository proposal awaits canonical approval; independent readonly optimization continues.

## 2026-11-27 B-G06 global guards/test-index comparison verified

- [x] Enabled dedicated matrix/affected command (ReportingQueryCostMySqlTests,ReportingQueryCaptureTests,FinancePeriodServiceTests,ExecutionFeePeriodServiceTests,PendingPolicyPeriodServiceTests,PeriodChargeAdapterTests) exit0:35tests/0failures/0errors/0skipped, log target/core05bg06-guards-index-cost.log. Report target/core05bg06-guards-index-cost.json has110observations. No retained database changed; test container cleanup empty.
- [x]501charge parents exceed child budget with8queries/no proofs;101parents/1010sources exceed source budget with7queries/no proofs; adversarial1000extra outside MIS fills retain101selected fills/no verified fee with3queries. All raw/proof guards pass. Disposable indexes change executed finance scan3002->1row range and fill scan4103->1with exact DTO equality; indexes absent from final catalog. No production DDL or SLA acceptance.
- [x] Prepared concrete ignored review artifacts target/proposed-report-period-indexes.sql/.md covering two index definitions, managed/fresh/runtime parity implementation, disk/write/build-lock impact and non-execution boundary. Open user approval is exclusively DEPLOYMENT_CHECKLIST.md under existing pre-change schema-impact requirement. No migration implementation or retained DDL yet.
- [~] NEXT independent readonly pending-policy matching CPU remediation: request-scoped prepared candidate lookup, same per-row statuses/validation/limits and original instruction proof, no persistent cross-user cache or schema change. Benchmark before/after and run identity/snapshot/equivalence regressions. B-G06partial; source-index repository implementation waits for canonical approval. No AI/learner-status change.

## 2026-11-27 B-G06 global guards/index comparison written

- [~] Added501charge-parent child overflow,101parent/1010source funding overflow, and deliberately corrupt1000extra outside MIS fills; raw/proof/query-count guards asserted. Disposable-fixture index A/B for finance(user,time,key) and fills(user,time,id), exact DTO equality, plans/timings before/after, indexes dropped before final catalog. No production DDL/migration or acceptance. New guards-index-cost report preserves earlier observations.
- [ ] Not verified. NEXT enabled dedicated matrix/affected suites; final guard counts/DDL compatibility pending. Synthetic source graphs/adversarial context are not historical account certification.

## 2026-11-27 B-G06 dedicated product/batch matrix verified

- [x] New dedicated opt-in fresh cost MySQL fixture uses shared locked synthetic lifecycle, independent of recovery history; no retained DB access. cmd.exe /d /c ".\mvnw.cmd -q -Dtest=ReportingQueryCostMySqlTests,ReportingQueryCaptureTests,FinancePeriodServiceTests,ExecutionFeePeriodServiceTests,PendingPolicyPeriodServiceTests,PeriodChargeAdapterTests test > target\core05bg06-matrix-cost.log 2>&1" with process-local journal/cost flags restored exit0:35tests/0failures/0errors/0skipped. Test-owned container cleanup empty. Report target/core05bg06-matrix-cost.json has82observations; prior reports preserved.
- [x] Owned finance/fill1000/1001rows preserve raw1000and truncation. Execution1000full proof fee1000.00/fixed3reads;1001selection guard withholds proof/skips context/fixed2reads. Charge100/101parents preserve source proofs and partial-window null sums with5/7reads. MIS100/101tickets each with owned position/open event preserve1.00fee per ticket with4/6reads. Repeated DTO equality and actual-query plans/read/timing records included.
- [~] NEXT global charge child-source/fill-context1001overflow characterization, then disposable-fixture index A/B comparison for demonstrated user/date full scans; no production DDL or migration approved. Fresh source graphs benchmark contract shapes, not historical economic accounts. Larger mixed-cardinality/repeatability/threshold review remains required. B-G06/CORE-05-B2partial; no AI/learner change.

## 2026-11-27 B-G06 product and batch matrix written

- [~] Added1000/1001owned finance/fill selections (raw1000/truncation/proof and3->2execution reads),100/101charge-parent and MIS-ticket batching (expected5->7/4->6queries). Charge source graphs preserve parent-only partial-window null grouped sums; MIS tickets each have independent owned position/open event. Fixture source graphs are not reconstructed economic account history. Actual query measurements reuse capture; new matrix-cost report preserves earlier files.
- [ ] Not verified. NEXT dedicated fresh opt-in MySQL fixture + affected suites. Global source/context overflow costs and broad mixed skew/threshold review still open. No production/schema/index/AI change.

## 2026-11-27 B-G06 dedicated cost fixture written

- [~] Added opt-in dedicated fresh cost MySQL test, reusing original synthetic lifecycle/readiness and applying shared JUnit resource lock to prevent concurrent lifecycle collisions. Separate cost fixture starts with only fresh schema/owned initialization, not recovery-suite history; cardinalities remain reported per run. Existing recovery suite still independently callable; helper visibility only changed in tests.
- [ ] Not run yet. NEXT extend product/batch matrix and run dedicated + affected suites. Corrected simple table group2 status to still unmeasured (four basic reader ranges do not certify charge/MIS batching). No production behavior or performance acceptance change.

## 2026-11-27 B-G06 selected-history/CPU/ranges verified

- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,ReportingQueryCaptureTests,PendingPolicyPeriodServiceTests,PendingCreditPolicyReconcilerTests test > target\core05bg06-history-cost.log 2>&1" with process-local V3_JOURNAL_MYSQL/V3_REPORT_COST=true restored in finally exit0:16tests/0failures/0errors/0skipped. Synthetic cleanup empty; original snapshot/restart/restore assertions retained. Report target/core05bg06-history-cost.json has51query observations and3pure matching samples.
- [x]1000selected normalized-instruction shapes x1000candidates retain valid original policy with3reporting reads;1001selected sentinel retains1000raw rows/no policy and skips candidates/fixed2reads. Repeated event instruction shapes stress provenance matching, not1000economic transactions. Pure wall941.83/951.07/991.53ms and current-thread CPU859.38/921.88/937.50ms; reader repeated elapsed970.73-1050.50ms. Thread CPU excludes GC/other threads.1001guard reader23.14-25.58ms. Not production SLA/threshold acceptance.
- [x] Empty and366day windows for journal/finance/execution/policy actual queries/DTO invariants checked; wide pending/journal truncation explicit, no false zero/history claim. No new index/schema/production query/AI change. Prior reports preserved.
- [~] NEXT remaining owned finance/fill history boundaries and charge/MIS batch/context matrix; then growth/threshold review and concrete remediation if supported. B-G06/CORE-05-B2partial. Selected-history matching CPU is now an observed cost, not yet optimized or accepted. No owner dependency/learner-status change; canonical dependencies only DEPLOYMENT_CHECKLIST.md.

## 2026-11-27 B-G06 history/CPU/range extension written

- [~] Extended synthetic probe:1000selected pending instructions x1000candidates,3pure matching wall/current-thread CPU samples (null if unsupported),1001selected sentinel retaining1000raw rows and skipping candidates/fixed2queries, plus empty/366day four-reader plans/timings/invariants. Repeated normalized instructions with unique event keys deliberately stress policy proof, not economic transactions. Separate history-cost report preserves prior measurements. No production/schema/AI change.
- [ ] Verification pending; NEXT enabled MySQL + focused pending/capture suites. Charge/MIS and large owned finance/fill matrix remain open. Current inherited240s test limit retained, no acceptance on timeout/interruption.

## 2026-11-27 B-G06 owned candidate scaling verified

- [x] Synthetic actual-query probe now measures exact100/1000/1001owned settled zero-remaining/outside-period cash-credit candidates with one selected instruction. Valid original policy/id retained through1000;1001candidate sentinel withholds both while keeping raw selected row. Fixed3reporting queries asserted. Separate target/core05bg06-owned-candidate-cost.json preserves original foreign-skew report.
- [x] Process-local V3_JOURNAL_MYSQL=true/V3_REPORT_COST=true restored in finally. cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,ReportingQueryCaptureTests,PendingPolicyPeriodServiceTests,PendingCreditPolicyReconcilerTests test > target\core05bg06-owned-candidate-cost.log 2>&1" exit0:16tests/0failures/0errors/0skipped. Existing isolated snapshot/restart/restore assertions retained; labeled synthetic container listing empty after cleanup. No retained DB/services touched.
- [x] Report29query observations: original four-reader foreign-skew cases plus9query observations from3owned-candidate scenarios. Three reader repeats per candidate scenario, observed100case25.17-36.17ms,1000case30.26-85.91ms,1001case32.57-35.87ms. Fixture-specific elapsed times, NOT pure CPU or SLA. Candidate SELECT returns100/1000/1001rows; current candidate cardinality not current cash availability. Exact statement rows-examined still unavailable; handler/iterator metrics separate. No threshold/performance acceptance/index proposal.
- [~] B-G06/CORE-05-B2partial. NEXT selected-instruction1000/1001bounds, many-instruction x many-candidate matching CPU with observed wall/CPU distinction, then charge/MIS batch/context and empty/wide-range matrix. Four current reporting check groups documented in simple checklist; no fixed count of all tests and no new requirement that every V3 feature finish before AI integration. Live AI remains separately gated in DEPLOYMENT_CHECKLIST.md; no account/key needed now. No AI/learner-status or production query/schema/API change.
- [x] Checklist/overview/semantics/inventory synchronized; final QA/evidence below.

## 2026-11-27 B-G06 owned candidate scaling written

- [~] Extended synthetic actual-query probe with exact100/1000/1001owned cash-credit candidates, all settled/zero-remaining/outside-period; one selected instruction. Assert matched original policy/id through1000,1001sentinel withholds proof but retains raw instruction, fixed3queries. Capture plans/read counters and reader/query timings for each. Separate output target/core05bg06-owned-candidate-cost.json preserves first report. Worst-case many-instruction CPU/selected-row bounds and other measurement groups remain pending.
- [ ] Verification pending: enabled MySQL + capture + pending-reader/rule regressions next. No production query/schema/AI change. Four remaining reporting check groups explained as current work breakdown, not a fixed count or mandatory all-V3 gate before live AI; existing AI approval dependencies unchanged in DEPLOYMENT_CHECKLIST.md.

## 2026-11-27 communication preference recorded

- [x] Recorded requested live-AI setup heads-up in canonical DEPLOYMENT_CHECKLIST.md; no setup/activation requested, scheduled or authorized. Explaining reporting checks does not change AI capability or learner status.
- [x] Documentation-only wording verified by scoped read/whitespace check; no application tests needed/run. Current reporting status and measurements unchanged.
- [~] NEXT resume B-G06 remaining owned-size/limits/charge-MIS/wide-range measurements from prior checkpoint. Owner dependencies remain exclusively DEPLOYMENT_CHECKLIST.md.

## 2026-11-27 B-G06 first actual-query observations verified

- [x] Added test-only ReportingQueryCostProbe gated by V3_REPORT_COST inside existing loopback/test-owned MySQL lifecycle. Four owned readers captured actual SQL,100/2000foreign rows per journal/finance/fill table,20query observations/three repeats each, repeated reader DTO equality and ownership/count/policy checks. EXPLAIN JSON/ANALYZE, Handler_read deltas, timings, engine/index/cardinality metadata retained. No production query/schema/index/API change.
- [x] Initial capture regression exit0:2tests/0failures/0errors/0skipped (target/core05bg06-cost-compile.log). Earlier enabled run was interrupted before report and not accepted; orphaned exact test-owned container removed after prior positive label-filter inventory, as documented. Successful rerun process-local V3_JOURNAL_MYSQL=true and V3_REPORT_COST=true, both restored in finally: cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,ReportingQueryCaptureTests test > target\core05bg06-query-cost-mysql-rerun.log 2>&1" exit0:3tests/0failures/0errors/0skipped. Existing snapshot/restart/restore assertions retained. Successful test-owned container cleanup confirmed by empty labeled docker listing. No retained user DB/services touched.
- [x] Measurement report target/core05bg06-query-cost.json, MySQL8.4.13. Final table rows: journal2011, finance events2003, fills2008, obligations4. Executed plans show finance full event scan2003rows and fills full scan2008rows for one owned selected record each at2000foreign rows. Handler_read_rnd_next grows finance107->2007, fills109->2009 across100->2000foreign fixtures; these counters are NOT exact statement rows-examined. Journal narrow period traversed2indexed rows at both sizes, fill ticket context1indexed row, pending candidates3owned rows. Timings recorded but no SLA/production/overall performance PASS claimed.
- [~] B-G06/CORE-05-B2partial. NEXT extend actual-query measurements to large owned histories/pending candidate1000/1001and pure matching CPU, charge/MIS batching/context sentinels, empty/narrow/366day windows. Compare growth and thresholds with invariant assertions before any concrete index-impact proposal; no automatic schema change from this first observation. Exact statement rows-examined remains unavailable; plans/handler/timing measurements stay separately named. Public reports/full expenses/P&L/historical balances unfinished; owner dependencies only DEPLOYMENT_CHECKLIST.md.
- [x] User AI status answered from existing project/learning guide: AI-1 offline foundation and optional AI-2 local equity helper/read-only tools started; no live model/general model conversation/model-selected tools. This turn continues reporting work, changes no AI feature or learner status. Checklist/overview/inventory/semantics synchronized; final QA/evidence below.

## 2026-11-27 B-G06 interrupted run checkpoint

- [~] User status answered: AI-1 offline foundation and optional AI-2 equity local preview/read-only tools have started; no live model/general conversation/model-selected tools enabled. No AI feature/learning state change.
- [ ] Prior enabled cost run interrupted: exec session unavailable, no Java process, no cost JSON or fresh MySQL Surefire report; no acceptance claimed. Unique test-owned container09c728d9 remained; verified journal-synthetic label and removed only that container/anonymous volumes. NEXT rerun enabled isolated suite; flags process-local/restored. Earlier local capture2tests remain valid only for their scope.

## 2026-11-27 B-G06 synthetic actual-query probe written

- [~] Opt-in V3_REPORT_COST phase in existing isolated MySQL harness uses captured actual four-reader SELECTs/normalized binds,100/2000foreign rows per journal/finance/fill table, identical repeated DTO checks, ownership/count assertions. Records EXPLAIN JSON/ANALYZE iterator plan, three query/reader timings, Handler_read deltas and schema/index/cardinality evidence. Exact statement rows-examined remains explicitly unavailable, not mislabeled as handler counts. No SLA/index/production acceptance.
- [ ] Tests not run. NEXT compile/capture regression then enabled isolated measurement. Narrow-window characterization only; charge/MIS/limits/large owned history/wide ranges still pending. No retained DB/query/schema/API/AI change.

## 2026-11-27 GitHub upload verified and B-G06 capture checked

- [x] Explicit user upload request completed for reviewed project source on work/v3-progress-20261127, first commit d667858d7830ac6d023ff05677bd55dc099146b8. git ls-remote branch SHA equals local HEAD after push; main unchanged.521changed paths/535total staged files reviewed. Credential-bearing test.http removed from tracking only/local file preserved; local .codex/runtime/build files excluded. Exact staged scanner reviewed three documentation/test placeholders, one small PNG; no unreviewed pattern finding. No unknown-secret guarantee, history rewrite/rotation, deployment or owner promotion. Existing public history/SEC-03 owner remediation not cleared.
- [x] Continued B-G06 with test-only ReportingQueryCapture JDBC wrapper and two tests. cmd.exe /d /c ".\mvnw.cmd -q -Dtest=ReportingQueryCaptureTests test > target\core05bg06-query-capture.log 2>&1" exit0:2tests/0failures/0errors/0skipped. Actual owned repository SQL/parameter/results, immutable snapshots, prepared-statement reuse/nulls and write exclusion checked. node --test scripts/check-secret-exposure.test.mjs PASS3/3. No production query/schema behavior change.
- [~] NEXT B-G06 use captured actual SELECTs in isolated synthetic MySQL to record parameterized EXPLAIN/rows-examined/time/reader invariants under deterministic size and owner skew. No database plans/workload measurements or performance acceptance yet. B-G06/CORE-05-B2 partial; public reporting and full expenses/P&L/historical balances unfinished. Continuation files committed and pushed; remote SHA verified below. No AI/learner-status change; owner dependencies exclusively DEPLOYMENT_CHECKLIST.md.

## 2026-11-27 GitHub upload preparation

- [~] User explicitly authorized uploading pending project work and continuing V3. Public GitHub origin verified; remote main matches local original HEAD. Current/reachable scan74/67indicators;71current indicators in tracked credential-bearing HTTP scratch. Removed scratch from index only (local file preserved), ignored scratch/.codex local settings; private runtime/build files remain ignored. All remaining project work staged (521changed paths). No history rewrite/credential rotation/deployment. NEXT exact staged-blob review, source commit on progress branch and push; pending.
- [x] node --test scripts/check-secret-exposure.test.mjs PASS3/3. Existing historical credential owner review remains open exclusively DEPLOYMENT_CHECKLIST.md. B-G06 measurement work remains next.

## 2026-11-27 B-G05 remaining-scope review complete; B-G06 next

- [x] Reviewed journal/direct/grouped, finance, execution/MIS, pending policy and weighted-average portfolio contracts against reporting requirements. B-G05 complete within selected-journal eligibility/summary scope; full account expenses/P&L/historical balances remain explicitly unavailable. Updated current semantics and acceptance matrix; preserved historical evidence.
- [x] Latest journal source hashes match recorded145passing-test evidence; no Maven rerun for documentation/source inspection. Older shared fixture hash drift was detected and scoped as expected historical extension, not falsely recertified; pending/execution production source checks pass, financing check/final QA pending below. No retained DB/services/writer/schema/API/AI/learner changes.
- [~] CORE-05-B2partial. NEXT B-G06 isolated synthetic actual-query plan/workload measurements: journal/charge/funding, owned fill/context/MIS, financing user-period and all-state pending candidates. Static schema reveals fill user/time and finance user/time scan risks; no performance verdict/index change yet. Measurement matrix/evidence requirements documented in CORE05_REPORTING_SEMANTICS.md. Benchmark implementation/execution NOT STARTED; no production SLA/public API/UI/export acceptance. Owner dependencies only DEPLOYMENT_CHECKLIST.md.

## 2026-11-27 B-G05 owned grouped cash changes verified

- [x] JournalPeriodService attaches separately named selectedGroupedChanges inside existing owned read-only REQUIRES_NEW/REPEATABLE_READ snapshot after direct sums. No extra SQL, user lookup, independent reader or per-charge service loop. Four pure coverage/three eligibility passes revalidate caches. Existing direct/raw/group/source/eligibility data retained. Compatible DTO constructors; replacing assessment/eligibility/direct results invalidates grouped values, replacing group evidence clears all derived caches.
- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=GroupedJournalChangeIntegrationTests,GroupedJournalChangeSummarizerTests,ChargeGroupSourceReconcilerTests,SelectedJournalChangeIntegrationTests,SelectedJournalChangeSummarizerTests,PeriodSummaryIntegrationTests,PeriodSummaryEligibilityAssessorTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg05-owned-grouped-changes.log 2>&1" exit0:144tests/0failures/0errors/0skipped. Four new owned cases cover negative-debt charge once/direct scope unchanged/foreign exclusion, partial/corrupt proof raw retention, empty-zero/cache invalidation and same independent snapshot/fixed query counts. Counts target/core05bg05-owned-grouped-changes-local-verification.json.
- [x] Final process-local V3_JOURNAL_MYSQL=true restored in finally. cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,GroupedJournalChangeIntegrationTests,GroupedJournalChangeSummarizerTests,ChargeGroupSourceReconcilerTests,SelectedJournalChangeIntegrationTests,SelectedJournalChangeSummarizerTests,PeriodSummaryIntegrationTests,PeriodSummaryEligibilityAssessorTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg05-owned-grouped-changes-mysql.log 2>&1" exit0:145tests/0failures/0errors/0skipped. Counts target/core05bg05-owned-grouped-changes-mysql-verification.json. No remaining failures.
- [x] Actual MySQL full DTO old/new funding snapshot/repair/restart/deleted-child/dump restoration includes grouped values. Explicit parent-only window null versus complete selected parent/children -40.00settled/0.00reserved; source conflict/deleted child null; repaired/restarted/restored complete DTO equal. Existing money/recovery assertions retained. Test-owned container cleanup confirmed by empty docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}'. No retained DB/service changes.
- [x] Selected journal direct and complete-group calculations verified within their separate bounded scopes; never add both representations together. Partial windows, full account expenses/P&L/historical balances unavailable; fullAccountSummaryAvailable false. No writer/schema/index/public API/AI/learner-status change or release acceptance. Checklist/overview/semantics/inventory synchronized; final scoped QA follows.
- [~] B-G05/CORE-05-B2partial. NEXT remaining-scope acceptance review: map journal cash summaries against owned financing/execution-fee/pending-policy evidence and documented account-report requirements; identify genuinely supported remaining summary work versus mandatory unavailable history/product states. Do not sum independent reader snapshots or relabel source subtotals as complete expenses. Preserve weighted-average portfolio method and settled/reserved/refund distinctions. Check remaining B-G05 tests before B-G06 synthetic query-cost review/public API/UI/export. Owner dependencies exclusively DEPLOYMENT_CHECKLIST.md.


- [x] Final scoped whitespace PASS across13changed files; overview/checklist local Markdown links PASS. Source hashes target/core05bg05-owned-grouped-changes-source-hashes.json. No remaining failures. NEXT remaining-scope B-G05acceptance review as above.

## 2026-11-27 owned grouped sums attached

- [~] JournalPeriodService attaches separately named selectedGroupedChanges inside existing snapshot after direct sums. Compatible DTO constructors; assessment/eligibility/direct/group replacement invalidates derived grouped result, group replacement clears all caches. Four pure coverage/three eligibility passes, no extra SQL. Fixture wiring/callback counts updated. No writer/schema/API/AI change.
- [ ] Tests not run. NEXT owned complete/partial/refund/null-zero/conflict/snapshot tests and actual MySQL recovery. B-G05partial.

## 2026-11-27 B-G05 supplied complete-group cash changes verified

- [x] Added immutable GroupedJournalChangeSummary/pure constructor-injected summarizer. Recomputes eligibility/coverage, every retained source binding and global regrouped partition, cached verdict consistency and shared bounds. Complete selected charge contributes settled effect once/reserved net0; represented child effects skipped. Ordinary rows use verified signed deltas. Any outside-period child/child-only/missing/conflicting/limited proof blocks grouped sums. Per-bucket unknown refund/null versus known zero and exact19/22digit money preserved. Direct scopes unchanged; fullAccountSummaryAvailable always false.
- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=GroupedJournalChangeSummarizerTests,ChargeGroupSourceReconcilerTests,SelectedJournalChangeIntegrationTests,SelectedJournalChangeSummarizerTests,PeriodSummaryIntegrationTests,PeriodSummaryEligibilityAssessorTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg05-grouped-changes.log 2>&1" exit0:140tests/0failures/0errors/0skipped. Six new cases cover actual writer negative debt/ordinary cash once/no writes, outside-child/child-only windows, missing/duplicate/stale source bindings, input/selection limits/source corruption, stale eligibility and refund bucket distinction. Evidence target/core05bg05-grouped-changes-verification.json. No remaining failures.
- [x] Pure supplied calculation only, not attached to owned reader. No ownership/DB/snapshot proof, independent MySQL state or historical/expense/P&L/balance certification. No writer/schema/API/AI/learner-status or retained DB/service change. Checklist/overview/semantics/inventory synchronized. Scoped whitespace/local Markdown links PASS. Source hashes target/core05bg05-grouped-changes-source-hashes.json.
- [~] B-G05/CORE-05-B2partial. NEXT attach separately named grouped summary in existing owned JournalPeriodService snapshot with constructor injection/no extra SQL; preserve direct sums/raw rows/eligibility/group/source proof. Invalidate grouped results when assessment/eligibility/group evidence replaced. Add owned/foreign/empty/null-zero/complete-versus-partial/refund/source-conflict/fixed-query tests and actual MySQL concurrent-change/repair/restart/deleted-child/dump equality. No partial-window charge attribution or parent+child sum. B-G06/public reports unfinished; owner dependencies only DEPLOYMENT_CHECKLIST.md.

## 2026-11-27 B-G05 supplied grouped cash calculation written

- [~] Added immutable GroupedJournalChangeSummary/pure summarizer. Rechecks each source binding and global regrouped partition, stale eligibility/coverage, whole-period limits and complete selected parent/children. Charge settled effect counted once/reserved net0; ordinary rows use signed verified effects. Outside-period child context blocks both grouped sums; direct scopes unchanged. Per-bucket null/zero, exact19/22digit money, no account expense/P&L/balance or ownership/snapshot proof.
- [ ] Tests not run. NEXT actual writer mixed funding/debt, partial/child-only/stale/duplicate/source conflicts/refund/bounds tests. No reader/writer/schema/API/AI change; B-G05partial.

## 2026-11-27 B-G05 owned grouped source retention verified

- [x] GroupedChargeEvidence retains immutable sourceEvidence with compatible supplied constructor. PeriodChargeAdapter reuses existing bounded owned reads/global parent-child conflict checks, then revalidates one full proof binding per verified group. No extra SQL or per-charge service loop. Failed binding keeps raw groups/incomplete status and no proofs. Direct scopes unchanged; no composite period sum/writer/schema/API/AI change.
- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=ChargeGroupSourceReconcilerTests,SelectedJournalChangeIntegrationTests,SelectedJournalChangeSummarizerTests,PeriodSummaryIntegrationTests,PeriodSummaryEligibilityAssessorTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg05-owned-group-source.log 2>&1" exit0:134tests/0failures/0errors/0skipped. Two new adapter cases and strengthened partial/child-only/foreign/limit assertions cover full immutable proof/no sum promotion/no writes, source corruption/raw retention and proof absence. Counts target/core05bg05-owned-group-source-local-verification.json.
- [x] Final process-local V3_JOURNAL_MYSQL=true restored in finally. cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,ChargeGroupSourceReconcilerTests,SelectedJournalChangeIntegrationTests,SelectedJournalChangeSummarizerTests,PeriodSummaryIntegrationTests,PeriodSummaryEligibilityAssessorTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg05-owned-group-source-mysql.log 2>&1" exit0:135tests/0failures/0errors/0skipped. Counts target/core05bg05-owned-group-source-mysql-verification.json. No remaining failures.
- [x] Actual MySQL full DTO old/new snapshot, repair, restart, deleted-child and dump restoration now include full retained source proof. One binding equals owned ChargeEvidenceService proof with two outside-period children; foreign source proof absent; fresh conflict/deleted child remove binding; repair/restore reproduce exact result. Existing balances/recovery assertions retained. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after cleanup. No retained DB/service changes.
- [x] Checklist/overview/semantics/inventory synchronized. No ownership/snapshot/global history acceptance from supplied constructors, no complete expenses/P&L/historical balances or release gate promotion. No AI/learner-status or owner-dependency change. Final scoped QA follows.
- [~] B-G05/CORE-05-B2 partial. NEXT pure separately named composite-aware selected cash-change contract: revalidate every retained source proof, global selected/context partition and stale cache, use full matched charge effect once only when parent selected, never add represented child effects again. Child-only/outside-period/missing/conflicting/limited evidence must withhold affected scope; original direct scopes unchanged. No inferred all-debt parent-only result or complete expense/P&L/balance claim. Test overlap/negative debt/refund/boundaries/limits, then owned integration and MySQL equality. B-G06query-cost/public reports unfinished; owner dependencies exclusively DEPLOYMENT_CHECKLIST.md.


- [x] Final scoped whitespace PASS across11changed files; overview/checklist local Markdown links PASS. Source hashes target/core05bg05-owned-group-source-source-hashes.json. No remaining failures. NEXT composite-aware selected cash contract as above; B-G05partial.

## 2026-11-27 owned source retention MySQL assertions written

- [~] Existing charge-period old/new snapshot/repair/restart/deleted-child/dump equality now includes full retained proof. Explicit one binding equals actual owned ChargeEvidenceService proof/two outside-period children, foreign absence, conflict/deleted-child empty bindings. No composite cash sums promoted. NEXT local result then enabled MySQL suite; pending.

## 2026-11-27 owned group/source retention tests written

- [~] Two new tests verify immutable full70.00charge/-40.00source effect, no direct composite sums/no writes/service loops, and corrupted funding removes proof while retaining raw rows. Existing partial/child-only/foreign source/global-limit cases now assert retained proof or its absence. NEXT affected Maven suite; pending; MySQL extension pending.

## 2026-11-27 owned grouped source retention written

- [~] Immutable GroupedChargeEvidence gains sourceEvidence with supplied three-argument constructor preserved. PeriodChargeAdapter reuses existing globally checked/bounded proofs, revalidates one binding per verified group and retains full owned proof without extra SQL/service loop. Binding failure yields incomplete/raw groups retained/no attached proofs. Existing global competing-parent/child and source/candidate budgets preserved. Direct scopes unchanged; no composite period calculation/writer/schema/API/AI change.
- [ ] Tests not run. NEXT retention/privacy/partial/global-limit/snapshot and MySQL recovery checks; B-G05partial.

## 2026-11-27 B-G05 supplied charge group/source binding verified

- [x] Added ChargeGroupSourceEvidence/ChargeGroupSourceReconciler: recompute full charge proof, reject stale comparison, regroup exact selected rows/context and retain immutable full proof only on exact match. Six new tests cover full/partial selection, missing/limited/child-only context, stale/duplicate/representative/context conflicts, oversize/unrelated proof and actual owned writer negative-debt evidence/no writes.
- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=ChargeGroupSourceReconcilerTests,SelectedJournalChangeIntegrationTests,SelectedJournalChangeSummarizerTests,PeriodSummaryIntegrationTests,PeriodSummaryEligibilityAssessorTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg05-group-source.log 2>&1" exit0:132tests/0failures/0errors/0skipped. Counts target/core05bg05-group-source-verification.json. No remaining failures. Scoped whitespace and overview/checklist local links PASS. Source hashes target/core05bg05-group-source-source-hashes.json.
- [x] Pure contract only: no owned reader output or existing direct-sum behavior changed, no composite period cash total, ownership/snapshot/global partition certification or independent MySQL state. No writer/schema/API/AI/learner-status or retained DB/service change. Checklist/overview/semantics/inventory synchronized.
- [~] B-G05/CORE-05-B2 partial. NEXT preserve validated full charge proof in immutable owned bounded group output using existing PeriodChargeAdapter batch evidence. Enforce global uniqueness/competing proofs/selection and source limits; no per-charge service loop. Revalidate cache/selected/context partition, keep direct scopes unchanged, and extend owned tests/MySQL source-change/restart/restore before any new composite cash calculation. Parent-only missing history, child-only/outside-period and conflicting context stay unavailable. B-G06/public reports remain unfinished; owner dependencies only DEPLOYMENT_CHECKLIST.md.

## 2026-11-27 supplied group/source tests written

- [~] Six cases cover full immutable proof, parent-only/partial selection context, missing/limited/child-only evidence, stale cache/representative/context/duplicate conflicts, oversize/unrelated proof and actual owned writer mixed-funding negative-debt context/no writes. NEXT affected Maven suite; pending. Pure contract only; no reader/period totals changed.

## 2026-11-27 B-G05 supplied charge group/source contract written

- [~] Added immutable ChargeGroupSourceEvidence/pure constructor-injected reconciler. Recomputes charge source comparison then regroups exact selected rows; requires identical selected/context partition. Matched output retains immutable full source proof and group; all unavailable results withhold both. No period totals/ownership/whole-period partition proof or writer/schema/API/AI change. Existing direct scopes unchanged.
- [ ] Tests not run. NEXT partial-selection/context/conflicting/stale/limited/actual writer negative-debt tests; owned batch integration/MySQL pending. B-G05partial.

## 2026-11-26 B-G05 owned selected direct-change sums verified

- [x] JournalPeriodService attaches selectedDirectChanges inside existing owned read-only REQUIRES_NEW/REPEATABLE_READ snapshot after coverage/eligibility. Constructor injection; no new SQL, user lookup, independent reader or financial writes. Three pure coverage passes/two eligibility passes/one summarizer revalidate caches. Raw rows/proofs/grouping/limitations retained. Supplied DTO constructors preserved; replacing assessment or eligibility invalidates derived sums. Limitation names selected direct changes rather than complete totals.
- [x] Local cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SelectedJournalChangeIntegrationTests,SelectedJournalChangeSummarizerTests,PeriodSummaryIntegrationTests,PeriodSummaryEligibilityAssessorTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg05-owned-changes.log 2>&1" exit0:126tests/0failures/0errors/0skipped. Five new integration cases cover owned/foreign signed values/no writes, null/zero/cache invalidation, shared independent read-only snapshot/fixed queries, actual credit refund and1001row raw retention/no partial sums. Evidence target/core05bg05-owned-changes-local-verification.json.
- [x] Final process-local V3_JOURNAL_MYSQL=true restored in finally; cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,SelectedJournalChangeIntegrationTests,SelectedJournalChangeSummarizerTests,PeriodSummaryIntegrationTests,PeriodSummaryEligibilityAssessorTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg05-owned-changes-mysql.log 2>&1" exit0:127tests/0failures/0errors/0skipped. Evidence target/core05bg05-owned-changes-mysql-verification.json. No remaining failures.
- [x] Actual MySQL full DTO source-change/repair/restart/deleted-child/dump restoration now covers selected sums. Valid selected1234.55settled/0.01reserved, old uninitialized null, new source conflict null-both, empty deleted selection0.00only in direct scope/full history false and grouped composite null-both. Existing financial/recovery checks retained. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after cleanup. No retained user database/service changes.
- [x] Checklist/overview/semantics/inventory synchronized; no writer/schema/index/public API/AI/learner-status change, no complete expense/P&L/historical balance or release acceptance. Existing weighted-average portfolio unchanged. Final scoped QA below.
- [~] B-G05/CORE-05-B2 partial. NEXT scoped composite cash-effect evidence contract: PeriodChargeAdapter computes full ChargeEvidence before grouper drops amounts/source proof. Preserve/revalidate that existing bounded same-snapshot proof in immutable group output before considering a separately named reconciled cash-change scope. Identity grouping alone cannot authorize amounts; parent-only/child-only/outside-period/missing/conflicting cases remain explicit, selected direct scopes stay unchanged. No per-charge service loop, parent+child sums, inferred debt or full expenses/P&L/historical balances. Add pure contradiction/overlap/negative-debt/boundary tests, then owned integration/MySQL evidence. B-G06query-cost/public reports remain unfinished; owner dependencies exclusively DEPLOYMENT_CHECKLIST.md.


- [x] Final scoped whitespace PASS across13changed files; overview/checklist local Markdown links PASS. Source hashes: target/core05bg05-owned-changes-source-hashes.json. No remaining failures. NEXT composite cash-effect evidence contract as above; B-G05partial.

## 2026-11-26 owned direct-change MySQL assertions written

- [~] Full DTO equality now includes attached selected changes across concurrent source change/repair/restart/deleted child/dump restore. Explicit valid selected1234.55settled/0.01reserved, old uninitialized null, fresh conflict null-both, deleted selection zero only in named direct scope and grouped composite null-both. No account balance/expense reconstruction. NEXT local result then enabled MySQL suite; pending.

## 2026-11-26 owned direct-change tests written

- [~] Five integration cases cover owner/foreign signed values/no writes, null-versus-empty-zero/cache invalidation, same independent read-only repeatable resource/fixed queries, actual credit-refund null-settled/zero-reserved,1001row raw retention/no partial sums. Existing eligibility callback updated for recheck with attached eligibility. NEXT affected suite; pending. MySQL extension pending.

## 2026-11-26 B-G05 owned selected changes attached

- [~] JournalPeriodService attaches selectedDirectChanges inside existing owned read-only repeatable snapshot after eligibility. Internal DTO constructors preserved; replacing assessment/eligibility invalidates derived sums. Constructor injection and fixture spy wiring. Three pure coverage passes/two eligibility passes/one summarizer; no extra SQL or independent readers. Limitation text now names scoped direct changes/no account expense totals.
- [ ] Tests not run. NEXT ownership/null-zero/refund/limits/cache invalidation/shared snapshot and MySQL full DTO recovery assertions. No writer/schema/public API/AI change; B-G05partial.

## 2026-11-26 B-G05 supplied selected direct-change sums verified

- [x] Added immutable SelectedJournalChangeSummary and pure constructor-injected SelectedJournalChangeSummarizer. Recomputes eligibility/coverage; stale attached verdict blocks money output. Sum verified signed funding deltas for RESERVE/SPEND/RELEASE, action deltas for other supported direct rows, per bucket. Exact19digit row/22digit aggregate money under1000rows; no rounding/narrowing. Unavailable whole bucket null, eligible empty/zero0.00. Composite parent/children/full expenses/P&L/historical balances unavailable; no partial sum, current snapshot or independent reader combination.
- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SelectedJournalChangeSummarizerTests,PeriodSummaryIntegrationTests,PeriodSummaryEligibilityAssessorTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg05-direct-changes.log 2>&1" exit0:121tests/0failures/0errors/0skipped. Nine new cases cover null/empty, signed changes/no writes, actual mixed-source reservation/spend/settled refund (5.00settled/10.00reserved change), actual unknown-credit refund (null settled/0.00reserved), grouped negative-debt charge withheld, stale caches, limit/invalid/grouping gaps, half-open/ties,1000max-money aggregate99999999999999999990.00/1001sentinel. Evidence target/core05bg05-direct-changes-verification.json. No remaining failures.
- [x] Pure supplied-evidence component only, not attached to owned reader. No ownership/database/snapshot proof or independent MySQL state; earlier recovery tests do not certify future integration. No writer/schema/public API/AI/learner-status change; retained user DB/services untouched. Checklist/overview/semantics/inventory synchronized; final scoped QA below.
- [~] B-G05/CORE-05-B2 partial. NEXT attach selected direct-change summary inside existing JournalPeriodService owned snapshot with constructor injection, raw/eligibility/coverage/limitations retained, no new SQL or cross-reader call. Invalidate derived sums if assessment/eligibility is replaced. Add owned/foreign/empty/null-zero/refund/limit/fixed-query tests plus actual MySQL source conflict/repair/restart/deleted-child/dump restoration full DTO equality. Composite/full expenses/P&L/historical balances still unavailable. B-G06query-cost/public reporting API/screens/export unfinished; owner dependencies exclusively DEPLOYMENT_CHECKLIST.md.


- [x] Final scoped whitespace PASS across9changed files; overview/checklist local Markdown links PASS. Source hashes: target/core05bg05-direct-changes-source-hashes.json. No remaining failures. NEXT owned selected direct-change attachment as above; B-G05partial.

## 2026-11-26 selected direct-change tests written

- [~] Nine cases cover null versus initialized empty0.00, signed cash/no writes, actual writer mixed-source reserve/spend/settled refund, unknown-credit refund per-bucket null/zero, grouped negative-debt charge withholding, stale caches, limits/invalid/grouping gaps, half-open/ties and1000max-money22digit aggregate/1001sentinel. NEXT affected Maven suite; pending. Pure rule not integrated.

## 2026-11-26 B-G05 selected direct-change sums written

- [~] Added immutable SelectedJournalChangeSummary and pure constructor-injected summarizer. Recomputes eligibility; stale attached verdict blocks direct output. Per-bucket signed funding effects or nonfunding action deltas only; exact19digit row/22digit aggregate money under1000rows. Unavailable null versus eligible empty0.00. No composite/expense/P&L/historical-balance totals, cross-reader sums, DB/ownership proof or writer/schema/API/AI change.
- [ ] Tests not run. NEXT signed/empty/null/stale/refund/grouped negative debt/half-open/max-money/bounds tests. Pure rule not integrated; B-G05partial.

## 2026-11-26 B-G05 owned summary eligibility verified

- [x] JournalPeriodService attaches immutable summaryEligibility after coverage assessment and before leaving its owned read-only REQUIRES_NEW/REPEATABLE_READ transaction. Recomputes coverage in memory (two pure passes) with no new SQL, user lookup, reader loop or separate snapshot. Raw rows/funding/grouping/limitations retained; internal supplied constructors preserved, replacing assessment invalidates eligibility. No amounts/totals/writer/schema/public API/AI change.
- [x] Local cmd.exe /d /c ".\mvnw.cmd -q -Dtest=PeriodSummaryIntegrationTests,PeriodSummaryEligibilityAssessorTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg05-owned-eligibility.log 2>&1" exit0:112tests/0failures/0errors/0skipped. Four new integration cases cover owned/foreign/empty scopes, shared independent repeatable snapshot/fixed queries/no writes, invalid/missing raw retention/cache invalidation and1001row truncation. Evidence target/core05bg05-owned-eligibility-local-verification.json.
- [x] Final process-local V3_JOURNAL_MYSQL=true restored in finally; cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,PeriodSummaryIntegrationTests,PeriodSummaryEligibilityAssessorTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg05-owned-eligibility-mysql.log 2>&1" exit0:113tests/0failures/0errors/0skipped. Evidence target/core05bg05-owned-eligibility-mysql-verification.json. No remaining failures.
- [x] Extended actual MySQL full DTO source-change/repair/restart/deleted-child/dump restoration checks now cover eligibility. Old uninitialized view unavailable; valid owned direct scopes eligible/full expenses unavailable; fresh source conflict blocks both direct scopes while old snapshot equality holds. Verified composite grouping remains outside direct scope. Existing financial assertions retained. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after cleanup. No retained user DB/service changes.
- [x] Checklist/overview and semantics/inventory synchronized; no complete summary/P&L/historical balance or release acceptance claimed. Existing weighted-average portfolio unchanged; no AI/learner-status or owner-dependency change. Final scoped QA below.
- [~] B-G05/CORE-05-B2 partial. NEXT pure immutable selected-journal direct-change summary: recompute eligibility rather than trust cached verdicts, only sum eligible settled/reserved fields within one supplied period, exact money and per-bucket unavailable/null versus verified zero. Preserve refund bucket distinction; composite charges/children and full expenses/P&L/opening/closing/running balances remain unavailable. Test boundaries/empty, signed changes, unknown/invalid/stale/limits and actual writer overlap/negative debt; then integrate inside same owned transaction and extend MySQL equality. No cross-reader/cross-snapshot totals. B-G06query-cost/public reporting API/screens/export remain unfinished; owner dependencies exclusively DEPLOYMENT_CHECKLIST.md.


- [x] Final scoped whitespace PASS across12changed files; overview/checklist local Markdown links PASS. Source hashes: target/core05bg05-owned-eligibility-source-hashes.json. No remaining failures. NEXT selected direct-change sum rules as above; B-G05partial.

## 2026-11-26 owned eligibility MySQL assertions written

- [~] Extended full journal DTO recovery/source-change checks to summaryEligibility. Assert old uninitialized unavailable, new matched direct scopes eligible/full expenses unavailable, concurrent source conflict changes fresh eligibility while old snapshot equality holds; verified composite grouping still blocked in direct scope. Existing restart/deleted-child/dump restoration equality now includes attached eligibility. NEXT local result then enabled MySQL suite; pending.

## 2026-11-26 owned summary eligibility tests written

- [~] Four integration cases cover owner/foreign/empty scopes, same independent read-only repeatable resource/one eligibility call/fixed queries/no writes, invalid/missing raw-row retention and assessment replacement invalidation,1001selection/first1000unavailable scopes. NEXT affected Maven suite; pending. MySQL assertions pending.

## 2026-11-26 B-G05 owned eligibility integration written

- [~] JournalPeriodService attaches immutable summaryEligibility before leaving existing owned read-only repeatable snapshot. Internal DTO preserves supplied-evidence constructors; changing assessment clears derived eligibility. Coverage assessed then rechecked by eligibility rule (two pure passes, no extra SQL). Synthetic fixture spies eligibility; prior coverage call-count assertion updated accordingly. No totals/writer/schema/public API/AI change.
- [ ] Tests not run. NEXT owned scope/raw retention/bounds/snapshot tests and MySQL full DTO recovery checks. B-G05 partial.

## 2026-11-26 B-G05 supplied summary eligibility verified

- [x] Added immutable PeriodSummaryEligibility and constructor-injected pure PeriodSummaryEligibilityAssessor. Separate selected-journal settled/reserved direct-change scopes; recomputes coverage and rejects stale assessment/interpretation/funding caches, invalid/uninitialized/pre-initialization/limited/missing/conflicting/unsupported/grouping states. Unknown credit refund blocks settled scope, while independently known reserved scope can remain eligible. Composite parent/child rows remain outside direct scope even if identity grouped. Full account expenses/P&L/historical balances always unavailable.
- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=PeriodSummaryEligibilityAssessorTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg05-summary-eligibility.log 2>&1" exit0:108tests/0failures/0errors/0skipped. Eight new cases cover empty initialized/uninitialized scope, direct equal-time rows/no writes, stale/forged caches, limits/grouping gaps, unsupported/invalid/missing funding, actual writer credit-origin refund, verified grouped negative-debt charge and null/1001/outside-boundary evidence. Counts target/core05bg05-summary-eligibility-verification.json. No remaining failures.
- [x] Result is diagnostic eligibility only, containing no amounts/totals, ownership/snapshot certification, complete summary-ready flag or combination of independent readers. Empty supplied direct scope can be eligible without asserting zero account activity. Existing weighted-average portfolio behavior unchanged; no writer/schema/API/AI/learner-status or retained DB/service change. Pure rule has no independent MySQL state; future integration requires new evidence. Semantics/checklist/overview/inventory synchronized; final scoped QA follows.
- [~] B-G05/CORE-05-B2 partial. NEXT attach immutable eligibility to JournalPeriodService inside its existing owned snapshot, preserving raw rows/assessment/limitations and keeping composite/full expense/P&L/historical balance scopes unavailable. Add ownership, raw retention, stale/unavailable and same-snapshot/fixed-query tests plus actual MySQL concurrent-change/restart/restore DTO equality. Only then consider explicit eligible selected-direct-change sums in a later unit, with no cross-snapshot totals or double-counting. B-G06query-cost review/public API/screens/export unfinished. Owner dependencies exclusively DEPLOYMENT_CHECKLIST.md.


- [x] Final scoped whitespace PASS across9changed files; overview/checklist local Markdown links PASS. Source hashes: target/core05bg05-summary-eligibility-source-hashes.json. No remaining failures. NEXT owned eligibility integration as above; B-G05 partial.

## 2026-11-26 B-G05 eligibility tests written

- [~] Eight cases cover empty initialized/uninitialized/no fullsummary grant, equal-time direct rows/no writes, stale assessment/forged caches, selection/funding/grouping gaps, unsupported/invalid/missing sources, actual writer credit-refund per-bucket difference, grouped charge/negative debt overlap, and null/1001/outside-boundary evidence. NEXT affected Maven suite; pending. No DB integration/MySQL acceptance claimed for pure eligibility.

## 2026-11-26 B-G05 supplied summary eligibility written

- [~] Added immutable PeriodSummaryEligibility/pure constructor-injected assessor for separate selected-journal settled/reserved direct-change scopes. Recomputes coverage/cache consistency; initialization, limits, unsupported/invalid/missing/conflicting/grouping/composite evidence block scope. Unknown credit refund blocks settled scope while preserving independently known reserved change. Full expenses/P&L/historical balances always unavailable. No sums, cross-reader combination, ownership/DB/snapshot proof or writer/schema/API/AI change.
- [ ] Tests not run. NEXT malformed/stale/empty/bounds/overlap and actual writer refund/negative-debt/grouped charge tests. Pure scope not integrated; B-G05 partial.

## 2026-11-26 B-G04 owned pending policy reader verified

- [x] Added immutable PendingPolicyPeriodEvidence plus constructor-injected repository/service and synthetic fixture wiring. Active owned identity, same read-only REQUIRES_NEW/REPEATABLE_READ snapshot, bounded half-open max366day whole-second range. Private original journal fingerprints and all owned cash-credit candidates, including settled/consumed/outside-period sources. Independent1001sentinels; first1000raw journal rows retained with policy withheld on limits. At most3reporting reads, no per-row service loop; no hashes/source metadata/foreign candidates exposed.
- [x] Local cmd.exe /d /c ".\mvnw.cmd -q -Dtest=PendingPolicyPeriodServiceTests,PendingCreditPolicyReconcilerTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg04-owned-policy.log 2>&1" exit0:113tests/0failures/0errors/0skipped. Six new actual-writer-backed adapter cases cover ownership/privacy/no writes, outside-period consumed/settled sources, malformed/missing evidence,1000/1001bounds and same independent snapshot/fixed queries. Evidence target/core05bg04-owned-policy-local-verification.json.
- [x] Final process-local V3_JOURNAL_MYSQL=true restored in finally; cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,PendingPolicyPeriodServiceTests,PendingCreditPolicyReconcilerTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg04-owned-policy-mysql.log 2>&1" exit0:114tests/0failures/0errors/0skipped. Evidence target/core05bg04-owned-policy-mysql-verification.json. No remaining failures.
- [x] Actual MySQL helper uses SettlementFunds.journal fingerprints and owned/foreign synthetic settled zero-remaining candidates outside period. Existing read preserves REUSABLE despite concurrent candidate corruption; fresh read withholds, repair restores proof. Foreign exclusion/end exclusion, restart/full DTO equality, missing-candidate raw retention and dump-restored DTO equality pass. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after cleanup. No retained database/service change.
- [x] B-G04 complete within retained-provenance review and owned original-policy evidence scope. Current/refund availability not inferred; credit-origin historical refund destinations and parent-only composite charge remain unavailable where evidence is absent. No writer/schema/index/public API/AI or learner-status change; no complete history/account expenses/balance reconciliation or release acceptance. Candidate bounds are not scan-cost acceptance. Checklist/overview/semantics/inventory synchronized; final QA below.
- [~] CORE-05-B2 remains partial. NEXT B-G05 define and test coverage-aware period/account summary eligibility contract: explicit per-bucket scope, unavailable reasons and contradictory/stale evidence rejection; no partial expense totals, fabricated opening/closing/running balances or sum across independent reader snapshots. Retain weighted-average portfolio methodology and all overlaps/negative debt/boundary states. Then B-G06 query-cost review before public report API/UI/export. Owner dependencies exclusively DEPLOYMENT_CHECKLIST.md; no new capture schema/backfill authorized.


- [x] Final scoped whitespace PASS across12changed files; overview/checklist local Markdown links PASS. Source hashes: target/core05bg04-owned-policy-source-hashes.json. No remaining failures. NEXT B-G05 as above; CORE-05-B2 partial.

## 2026-11-26 owned pending policy MySQL checks written

- [~] Added isolated helper using actual SettlementFunds.journal fingerprints and synthetic owned/foreign settled zero-remaining candidates outside period. Concurrent candidate policy change must preserve old match/new withheld proof, repair restores result; other-user exclusion/end exclusion, restart/full DTO equality and deleted-candidate/dump restoration equality. No retained database changes. NEXT local result then enabled MySQL plus affected suite; pending.

## 2026-11-26 owned pending policy tests written

- [~] Six actual-writer-backed adapter cases cover settled/consumed/outside-period candidates/no writes/hash privacy, foreign candidate/journal exclusion, missing/corrupt/altered evidence raw retention,1000/1001candidate and selected bounds, identity/time validation, and same independent read-only repeatable snapshot/fixed queries. NEXT affected Maven suite; pending. MySQL extension pending.

## 2026-11-26 B-G04 owned pending policy reader written

- [~] Added immutable period DTO/repository/service and synthetic fixture wiring. Active server identity, one read-only REQUIRES_NEW/REPEATABLE_READ snapshot, half-open whole-second max366day supported timestamp range. Owned pending journals/private fingerprint and all owned cash-credit candidates (no state/date filter), separate1001sentinels; retain first1000raw rows and withhold proofs on bounds. At most coverage/selection/candidate reads; no per-row service queries or exposed hashes/source metadata.
- [ ] Tests not run. NEXT ownership/missing/corruption/outside-period/limits/snapshot checks, then actual MySQL restart/restore. No writer/schema/API/AI change; B-G04 partial.

## 2026-11-26 B-G04 supplied pending instruction proof verified

- [x] Reviewed retained pending-policy/refund/composite provenance against SettlementFunds writers and current classifiers. Original pending instructions can be corroborated through stored SHA256 fingerprint plus full candidate metadata; current obligation status cannot prove refund destination. Existing exact composite pair proof remains necessary; parent-only charge stays incomplete. Decision table added to CORE05_REPORTING_SEMANTICS.md.
- [x] Added immutable PendingCreditPolicyEvidence and pure PendingCreditPolicyReconciler.1000candidate bound; exact positive money, valid writer metadata, unique candidate identity, full length-prefixed instruction hash match; exactly one match required. Unknown/missing/limited evidence withholds policy and identity; ambiguity/invalid evidence mismatch. No prefix/amount-only inference, checksum decoding, ownership proof, historical refund/availability/account history assertion. No DB/writer/schema/API/AI change.
- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=PendingCreditPolicyReconcilerTests,PeriodFundingTests,FundingEffectClassifierTests,ChargeEvidenceReconcilerTests,ChargeEvidenceServiceTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,PeriodCoverageAssessorTests,PeriodCoverageIntegrationTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg04-pending-policy.log 2>&1" exit0:107tests/0failures/0errors/0skipped. Seven new actual-writer-backed tests cover both policies/no changes to stored records, remaining/status independence, altered metadata, missing/limited/duplicate/ambiguous context,1000/1001bounds, invalid hash/money/reference/action and Unicode/colon encoding. Counts target/core05bg04-pending-policy-verification.json. No remaining failures.
- [x] Pure component has no independent MySQL state; no future owned adapter/recovery acceptance implied. Retained user database/services untouched, unrelated work preserved. Simple checklist/overview and semantics/inventory synchronized; no AI/learner-status change or owner-dependency removal. Final scoped QA follows.
- [~] B-G04/CORE-05-B2 partial. NEXT bounded owned pending instruction/candidate batch adapter in same read-only repeatable snapshot, retrieving original fingerprint privately and outputting only authorized matched identity/policy. Candidate universe must include owned obligations outside selected period and settled/consumed sources; fail unavailable under global bounds, no per-row service loop or source-key prefix guessing. Add ownership, conflicting/missing/multiple-candidate/bounds/snapshot tests and actual MySQL restart/restore before integration acceptance. Refund/parent-only gaps stay explicit; B-G05/06/public reports remain unfinished. New capture schema would require separate impact review; owner dependencies exclusively DEPLOYMENT_CHECKLIST.md.


- [x] Final scoped whitespace PASS across9changed files; overview/checklist Markdown local links PASS. Source hashes: target/core05bg04-pending-policy-source-hashes.json. No remaining failures. NEXT owned pending instruction adapter as above; B-G04 remains partial.

## 2026-11-26 B-G04 instruction proof tests written

- [~] Seven actual-writer-backed tests cover both policies/no writes, status/remaining independence, changed original metadata, missing/limited/duplicate/ambiguous candidates,1000/1001bounds, invalid fingerprint/money/reference/action, Unicode/colon canonical encoding without prefix linkage. NEXT affected Maven suite; pending. No owned DB adapter or MySQL acceptance claimed.

## 2026-11-26 B-G04 pending instruction proof written

- [~] Reviewed SettlementFunds creditPending/journal/returnSource/chargeDebt and existing classifiers. Original pending credit fingerprint retains policy/source/exchange/segment/trade date/amount via length-prefixed SHA256; current obligation status cannot prove historical refund destination. Existing composite pair proof remains required; parent-only charge stays incomplete.
- [~] Added immutable PendingCreditPolicyEvidence and pure PendingCreditPolicyReconciler: bounded1000supplied candidates, reconstruct original instruction hash, exact positive amount, unique identity and unambiguous match; no prefix guessing or hash decoding. Caller still owes owned snapshot/candidate coverage. No DB/writer/schema/API/AI change.
- [ ] Tests not run. NEXT actual-writer matches for both policies, changes/ambiguity/missing/bounds and unavailable output tests. Owned batch integration/MySQL pending; B-G04 partial.

## 2026-11-26 B-G03 owned MIS period linkage verified

- [x] ExecutionFeePeriodRepository/Service now batch selected owned MIS ticket metadata, owned position and event links in the existing read-only REQUIRES_NEW/REPEATABLE_READ snapshot.100ticket batches/global1001sentinel. Foreign events detected internally; no foreign metadata returned. Pure linkage proof gates ticket slice; selected raw owned fills retained on missing/conflicting/limited context. Canonical fill fee once, event/ticket aggregate never added. No per-position service transaction, writer/schema/public API/AI change.
- [x] Local command cmd.exe /d /c ".\mvnw.cmd -q -Dtest=ExecutionFeePeriodServiceTests,IntradayTicketSliceReconcilerTests,TicketFeeSliceReconcilerTests,TicketFeeServiceTests,IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,PaperTradingTests,FreshSchemaTests test > target\core05bg03-mis-period.log 2>&1" exit0:104tests/0failures/0errors/0skipped. Four new owned cases cover event-outside-period selection, missing/foreign/conflicting context,101ticket batching/global sentinel and shared snapshot. Evidence target/core05bg03-mis-period-local-verification.json.
- [x] Final process-local V3_JOURNAL_MYSQL=true restored in finally; cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,ExecutionFeePeriodServiceTests,IntradayTicketSliceReconcilerTests,TicketFeeSliceReconcilerTests,TicketFeeServiceTests,IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,PaperTradingTests,FreshSchemaTests test > target\core05bg03-mis-period-mysql.log 2>&1" exit0:105tests/0failures/0errors/0skipped. Evidence target/core05bg03-mis-period-mysql-verification.json. No remaining test failures.
- [x] Extended isolated12-reader MySQL proof: old snapshot retains0.30MIS fee despite concurrent event corruption; new read withholds fee; foreign event position metadata withheld, repaired link restored. Full integrated DTO equality after restart and dump restoration; deleted MIS events withhold ticket slice before restore. Existing balances/interest/recovery assertions retained. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after test cleanup. No retained user database/service touched.
- [x] B-G03 complete within selected owned paper-fill fee evidence scope. MIS_EVENT_ONLY_HISTORY_NOT_SELECTED and OTHER_PRODUCT_COVERAGE_NOT_CERTIFIED explicitly retain event-only/legacy/option/expiry/IPO gaps; no complete expenses/history/P&L/account balances claimed. Result limits do not certify scan cost. CORE-05-B2 remains partial; no release gate promoted.
- [~] NEXT B-G04: review pending-policy/refund/composite historical provenance against actual writers and existing classifier/grouping contracts. Resolve only links supported by retained evidence; unknown status-at-event/refund destinations remain unknown, parent-only charge incomplete. No historical backfill/inferred prefix links or new capture schema without separate impact review. B-G05/06 and public reports remain unfinished. Owner dependencies stay exclusively in DEPLOYMENT_CHECKLIST.md; no AI/learner-status change.
- [x] Final scoped whitespace PASS across11changed files; overview/checklist local Markdown links PASS. Final source hashes: target/core05bg03-mis-period-source-hashes.json. No remaining failures. NEXT B-G04 as above; CORE-05-B2 remains partial.

## 2026-11-26 owned MIS MySQL checks written

- [~] Extended12-reader synthetic test: concurrent MIS event fee conflict preserves old0.30slice, new snapshot withholds; foreign event identity withheld, repaired link restored. Existing full DTO restart/dump equality now covers integrated MIS proof; removed events must withhold selected ticket slice before restore. No retained database touched. NEXT local result then enabled MySQL/affected suite; pending.

## 2026-11-26 owned MIS period tests written

- [~] Four new integration tests cover event outside period/fill fee once/no ticket writes, event conflicts/foreign event metadata withholding/missing event/foreign position,101MIS-ticket batching/global sentinel with raw retention, and same repeatable read-only resource/no per-ticket fee-service reads. NEXT affected Maven suite; pending. Actual MySQL extension pending.

## 2026-11-26 owned MIS period integration written

- [~] Added selected owned ticket metadata and bounded100-ticket MIS position/event context batches (global1001sentinel) in existing read-only repeatable snapshot. Owned position join; foreign event detected internally with no foreign metadata output. MIS slice requires pure linkage proof, otherwise verified slice withheld; raw owned selection retained. No per-position service loop/writer/schema/API/AI change.
- [ ] Tests not run. NEXT integration ownership/missing/conflict/bounds/snapshot and actual MySQL restart/restore. Event-only history and other products remain explicitly uncertified.

## 2026-11-26 B-G03 supplied MIS ticket slice verified

- [x] Added immutable IntradayTicketSliceEvidence and pure constructor-injected IntradayTicketSliceReconciler after reviewing normal and forced-close IntradayTrading writes. Full ticket proof recomputed; MIS FILLED ticket, matching event key/position/stock/direction/side/units/exact fee, single leg0 fill. Canonical fill timestamp selects [from,to); repeated event/ticket fee never added. Missing/limited context incomplete, conflicts withhold slice. No DB/ownership/position-P&L/account-completeness proof.
- [x] Initial command cmd.exe /d /c ".\mvnw.cmd -q -Dtest=IntradayTicketSliceReconcilerTests,ExecutionFeePeriodServiceTests,TicketFeeSliceReconcilerTests,TicketFeeServiceTests,IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,PaperTradingTests,FreshSchemaTests test > target\core05bg03-mis-slice.log 2>&1" exit0:99tests/0failures/0errors/0skipped. Final cmd.exe /d /c ".\mvnw.cmd -q -Dtest=IntradayTicketSliceReconcilerTests,IntradayFeeServiceTests test > target\core05bg03-mis-slice-final.log 2>&1" exit0:13tests/0failures/0errors/0skipped. Separate counts target/core05bg03-mis-slice-{initial,final}-verification.json. Seven new tests cover all four direction/event combinations, boundaries/outside-period conflicts, missing/duplicate/truncated evidence, identity/metadata/leg/quantity/unsupported cases, exact money/zero/invalid period and persisted-record agreement with actual owned IntradayFeeService while ticket/balance unchanged.
- [x] No writer/schema/API/AI change or retained account/service modification. Pure component has no independent MySQL state; previous owned-reader MySQL evidence does not certify future integration. No remaining test failures. Checklist/overview and semantics/inventory synchronized; final QA follows.
- [~] B-G03/CORE-05-B2 remain partial. NEXT bounded owned MIS event/ticket/position/full-fill batch integration into ExecutionFeePeriodService in its same repeatable snapshot, with ownership/foreign/missing/limit withholding, raw selection preserved and no per-position service loop. Add actual MySQL concurrent snapshot/restart/restore. Existing reader still reports MIS_EVENT_LINKAGE_NOT_CHECKED. Other-product coverage and complete expense totals/public reports remain unfinished. No owner dependency or learner-status change.


- [x] Final scoped whitespace QA PASS across10changed files; overview/checklist local Markdown links PASS. No remaining failures. NEXT owned MIS batch integration described above; B-G03 remains partial.

## 2026-11-26 MIS supplied/owned agreement test written

- [~] Added a seventh new test using persisted fixture ticket/event/fills and actual owned IntradayFeeService proof. Compares canonical slice to owned fee once and asserts stored tickets/balance unchanged. Pending final focused rerun after initial affected suite; no production DB integration claimed. NEXT inspect initial result then rerun both changed test classes.

## 2026-11-26 supplied MIS slice tests written

- [~] Six tests cover LONG/SHORT OPEN/CLOSE side mapping, fill-time half-open selection with outside-period conflicts, missing/duplicate/limited context, identity/position/units/unsupported events, ticket metadata/single leg and exact money/zero/invalid periods. Tests pending. NEXT affected Maven suite; DB integration remains pending.

## 2026-11-26 B-G03 supplied MIS ticket linkage written

- [~] Reviewed IntradayTrading normal/forced close writes and existing IntradayFeeService. Added immutable IntradayTicketSliceEvidence and constructor-injected pure IntradayTicketSliceReconciler: full ticket slice recomputed, single leg0 MIS fill, event key/position/stock/direction/side/units/fee checks. Fill timestamp alone selects canonical fee; event/ticket fee never added. No DB/ownership/position-P&L/account completeness proof or writer/schema/API/AI change.
- [ ] Tests not run. NEXT focused missing/conflicting/duplicate/unsupported/bounds/zero/half-open cases and actual owned existing fee-service agreement. Owned batch integration and MySQL remain pending.

## 2026-11-26 B-G03 owned period reader verified

- [x] Existing immutable DTO/repository/service verified with six focused adapter tests. Active user ownership, missing/foreign parent withholding, outside-period conflicts, bounded batches/sentinels and independent shared read-only snapshot pass. No money writer/schema/public API/AI change.
- [x] Local affected suite exit0:93tests/0failures/0errors/0skipped (target/core05bg03-execution-period.log). Final process-local V3_JOURNAL_MYSQL=true restored in finally; cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,ExecutionFeePeriodServiceTests,TicketFeeSliceReconcilerTests,TicketFeeServiceTests,IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,PaperTradingTests,FreshSchemaTests test > target\core05bg03-execution-period-mysql.log 2>&1" exit0:94tests/0failures/0errors/0skipped. Evidence target/core05bg03-execution-period-mysql-verification.json.
- [x] Extended synthetic MySQL to12parallel readers/five coverage waits. Pre-commit ticket slice2.00 remains consistent; subsequent read5.00; foreign fill excluded and other user's orphan parent withheld. End exclusion, restart/full DTO equality, deleted-fill omission and dump-restored DTO equality pass. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after test cleanup. No retained account DB changed.
- [~] B-G03/CORE-05-B2 remain partial. NEXT corroborate MIS event linkage against selected fills/full-ticket context without adding repeated event fees. Other-product legacy/option/expiry/IPO coverage remains unverified. No complete expense total, public reports or release readiness claimed. No AI/learner-status or owner-dependency change.
- [x] Final scoped whitespace check PASS across11changed/relevant files; overview/checklist Markdown local-link check PASS. Source hashes: target/core05bg03-execution-period-source-hashes.json (before this final checkpoint note). No remaining test failures. NEXT MIS event linkage as above; B-G03 remains partial.

## 2026-11-26 execution-fee local pass and MySQL checks written

- [x] Affected Maven suite exit0:93tests/0failures/0errors/0skipped, including six new reader tests. Log target/core05bg03-execution-period.log; counts target/core05bg03-execution-period-verification.json.
- [~] Extended isolated MySQL test to12parallel readers/five coverage waits: old/new full-ticket context, foreign/orphan withholding, end exclusion, restart and deleted-fill/dump restoration equality. New checks pending; NEXT enabled MySQL plus affected suite. Docker responds29.9.3. Python helper unavailable; failed before any edits, completed edits with PowerShell. No retained DB/writer/schema/API/AI change.

## 2026-11-26 continuation: execution-fee reader verification

- [~] Resumed existing B-G03 reader and six written tests; no implementation restarted. NEXT run affected Maven suite below; result pending. MySQL snapshot/restart/restore and MIS linkage remain pending; no release acceptance.
- Command: cmd.exe /d /c ".\mvnw.cmd -q -Dtest=ExecutionFeePeriodServiceTests,TicketFeeSliceReconcilerTests,TicketFeeServiceTests,IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,PaperTradingTests,FreshSchemaTests test > target\core05bg03-execution-period.log 2>&1".

### 2026-11-26 execution-fee reader tests written

- [~] Six tests cover partial-range/full context, identity/foreign/orphan rows, outside-context corruption,102ticket batching/global sentinel,1001selected limit/invalid bounds, and independent shared read-only snapshot/no per-ticket service reads. NEXT affected Maven suite; pending. No MySQL reader acceptance yet.


## 2026-11-26 B-G03 owned execution-fee period reader written

- [~] Added immutable DTO/repository/service: active server identity, selected owned fills with left-owned-ticket join;1001selected sentinel,100-ticket context batches under1001global sentinel, full context without date filter and foreign-child detection with no foreign metadata output. Same read-only repeatable snapshot, per-ticket verified slice only, raw selected rows retained under limits. No per-ticket service loop/writer/schema/API/AI change. MIS linkage and other-product completeness remain explicitly unverified.
- [ ] Tests not run yet. NEXT focused ownership/partial period/corruption/bounds/snapshot verification and affected suites. New MySQL resilience pending. Prior pure-slice pending documentation QA will be closed in this unit?s final checks.


## 2026-11-25 B-G03 supplied ticket fee slice verified

- [x] Added immutable TicketFeeSliceEvidence and pure TicketFeeSliceReconciler after reviewing PaperTrading/IntradayTrading writes. Canonical selected paper-fill fee only; full supplied ticket filled-unit/leg/fee proof precedes half-open selection. Exact money/unique IDs+legs/ticket-link/time/quantity validation, max1000fullfills; missing/limited context incomplete, conflicts mismatch with no subtotal/counts. Matched result is one ticket's selected fill-fee subtotal only, never ticket aggregate plus fills/events or complete account expenses. Supports already-filled portion of partial/cancelled tickets and verified zero slices. No DB/auth/writer/schema/API/AI change.
- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=TicketFeeSliceReconcilerTests,TicketFeeServiceTests,IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,PaperTradingTests,FreshSchemaTests test > target\core05bg03-ticket-fee-slice.log 2>&1" exit0: 87 tests/0failures/0errors/0skipped. Eight new tests cover partial window and outside context, empty/zero/partial ticket, missing/truncated/invalid/duplicate/mixed evidence, boundaries/ties,1000/1001 and agreement with actual owned TicketFeeService proof without stored-row changes. Evidence target/core05bg03-ticket-fee-slice-verification.json includes suite counts/source hashes. No remaining failures. Pure component has no independent MySQL state; earlier reader recovery checks do not certify future adapter.
- [x] Reporting semantics, checklist/overview and inventory synchronized. Scoped whitespace PASS; final status/local-link QA follows. No AI/learner-status change, no retained accounts/services modified, unrelated work preserved. Owner dependencies only in DEPLOYMENT_CHECKLIST.md; no release acceptance promoted.
- [~] B-G03 and CORE-05-B2 partial. NEXT bounded owned period paper-fill selection with batched owned ticket/full-fill context in one repeatable snapshot; detect foreign/missing ticket-child records without exposing foreign metadata, retain selected raw evidence if context/global bounds fail, use this pure reconciler without per-ticket REQUIRES_NEW service calls. Add ownership/partial-range/bounds/snapshot and actual MySQL restart/restore. Then corroborate MIS event linkage without adding event fees; unknown legacy/option/expiry/IPO coverage stays explicit. No complete expense total or public reports yet.


- [~] Canonical fee/detail, full-ticket context and partial-period semantics documented; simple checklist/overview synchronized while tests run. New pure component does not certify owned period or MIS linkage. NEXT inspect Maven/Surefire. No AI/learner-status change.

### B-G03 ticket fee slice tests written

- [~] Eight tests cover half-open partial slices/full context, partial/cancelled ticket semantics, missing/truncated context, outside-window conflicts, invalid money, duplicate IDs/legs/mixed tickets, stable ties/zero fees,1000/1001 and agreement with actual owned TicketFeeService proof/no stored-row changes. NEXT affected Maven suite; pending.


## 2026-11-25 B-G03 supplied ticket fee slice written

- [~] Reviewed PaperTrading/IntradayTrading fee writes and existing ticket/event checks. Added immutable TicketFeeSliceEvidence and pure TicketFeeSliceReconciler: full-ticket quantity/legs/fee proof before half-open fill selection, max1000 full fills, exact nonnegative fees, duplicate/mixed ticket rejection, per-ticket selected subtotal only. Ticket aggregate/context and intraday events never added. No DB/ownership proof, writer/schema/API/AI change.
- [ ] Verification not run. NEXT partial-period/full-history, missing/conflicting/duplicates/zero/boundary/limit tests and affected suites. Owned period batch adapter and MIS linkage/legacy coverage remain unfinished.


## 2026-11-25 B-G02 owned financing period evidence verified

- [x] Added immutable FinancePeriodEvidence and constructor-injected repository/service. Active server identity, one read-only REQUIRES_NEW/REPEATABLE_READ snapshot, owned half-open whole-second period across open/closed financing positions. Contribution/principal change/accrued/paid interest remain separate typed fields. Owned orphan/foreign-position-linked events retained with positionId and interpretation withheld; unknown/invalid owned-position events explicit.1001sentinel/first1000, max366days/supported timestamp bounds, initialization/history limitations. No current principal/interest snapshot selected/summed, per-position loop, writer/schema/index/public endpoint/AI change. B-G02 complete within internal event-evidence scope; B2 remains partial.
- [x] Initial local affected command exit0 (target/core05bg02-finance-period.log). Final process-local V3_JOURNAL_MYSQL=true restored in finally; cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,FinancePeriodServiceTests,FinanceEventServiceTests,FinanceEventClassifierTests,PeriodCoverageIntegrationTests,PeriodCoverageAssessorTests,PeriodChargeAdapterTests,JournalPeriodServiceTests,SettlementFundingServiceTests,SettlementFundsTests,FreshSchemaTests test > target\core05bg02-finance-period-mysql.log 2>&1" exit0:85tests/0failures/0errors/0skipped. Seven new tests cover meanings/open-closed rows/no writes, range/ties/foreign exclusion, missing/foreign position withholding, unknown/invalid events, identity/init, input and1000/1001 bounds, shared independent snapshot/fixed2queries. Evidence target/core05bg02-finance-period-verification.json contains suite counts/source hashes. No remaining failures.
- [x] Isolated MySQL extended to11parallel readers (coverage latch4). Financing-period old view remains uninitialized/empty; new view contains owned2.50interest; foreign user's owned malformed link retains event but withholds position identity/interpretation. End exclusion, restart/full DTO equality, deleted events/empty incomplete-history result and dump restoration equality pass. Existing virtual balance/interest and other reader checks retained. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after test-owned cleanup. No scan-performance or production/visual acceptance certified.
- [x] Reporting acceptance/semantics, checklist/overview and inventory synchronized. Changed-source whitespace PASS; final inline Node whitespace/status/local-link QA PASS. No AI feature/architecture/learning change, no retained account/service changes or unrelated work discarded. Owner dependencies only in DEPLOYMENT_CHECKLIST.md; no release gate promoted.
- [~] CORE-05-B2 partial. NEXT B-G03 source-aware period execution-fee evidence: inspect ticket/fill timestamps and intraday linkage, define one canonical fee representation with full-ticket checks outside selected range and partial-period detail retained, no ticket+fill+intraday sum. Unknown legacy/option/expiry/IPO coverage remains explicit. Implement bounded owned selection/reconciliation tests then MySQL snapshot/restart/restore. No expense total, account balance or public report API/UI/export yet.


### B-G02 local verification passed

- [x] Local affected Maven command exit0 (target/core05bg02-finance-period.log). NEXT final enabled MySQL suite covering all final sources; results pending. Local suite counts will be recorded with final evidence.


- [~] Financing period semantics and simple checklist/overview synchronized while local tests run. Meaning/source ownership/bounds/scan limitations explicit; no expense total or live acceptance claimed. NEXT inspect local result then enabled MySQL final suite. No AI/learning change.

### B-G02 financing period MySQL checks written

- [~] Extended synthetic MySQL test to11parallel readers/coverage latch4: new account-period reader sees old uninitialized/empty view before interest commit, new owned2.50event afterward; other user receives only its owned malformed event with positionID/interpretation withheld. End exclusion, full restart and deleted-event/dump restoration equality added. Only test extension; no retained DB. NEXT finish initial local run, then enabled MySQL plus affected suites.


### B-G02 financing period tests written

- [~] Seven focused tests cover separate borrowing/accrual/payment meanings across open/closed positions, timestamp bounds/ties/foreign row exclusion, orphan/foreign-position withholding, unsupported/invalid retention, initialization/identity/inactive denial, input validation,1000/1001 and shared independent read-only snapshot/fixed queries/no per-position reads. NEXT affected Maven suite; pending.


## 2026-11-25 B-G02 financing period reader written

- [~] Added immutable FinancePeriodEvidence and constructor-injected repository/service. One active-user owned half-open read across financing positions with initialization metadata; left join retains owned orphan/foreign-position-linked events, withholds position ID/interpretation for unverified links. Max366days/whole-second timestamp range,1001sentinel/first1000, separate typed contribution/principal/accrued/paid meanings; closed positions included, no snapshot sum or per-position service calls. No writer/schema/API/AI change.
- [ ] Verification not run. NEXT ownership, time bounds, missing/corrupt/unsupported records, limits and same-snapshot tests; new MySQL resilience pending.


## 2026-11-25 B-G01 owned assessment integration verified

- [x] JournalPeriodService now attaches immutable PeriodCoverageAssessment before leaving its existing owned read-only REQUIRES_NEW/REPEATABLE_READ transaction. Constructor injection, no extra SQL/user resolution/snapshot or money writes. Internal DTO retains unassessed constructor for supplied-evidence diagnostics; owned reader always computes assessment. Raw rows, grouping, limits and unavailable summary flag preserved. B-G01 complete within diagnostic scope; B2 remains partial.
- [x] Process-local V3_JOURNAL_MYSQL=true restored in finally. cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,PeriodCoverageIntegrationTests,PeriodCoverageAssessorTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,ChargeEvidenceServiceTests,ChargeEvidenceReconcilerTests,PeriodFundingTests,FundingEffectClassifierTests,JournalPeriodServiceTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,SettlementFundsTests,SettlementLotsTests,CombinedTradeServiceTests,FreshSchemaTests test > target\core05bg01-coverage-integration.log 2>&1" exit0:135tests/0failures/0errors/0skipped. Four new integration cases cover owner/foreign/empty diagnostics, same resource/one assessor call/fixed queries/no writes, invalid/missing evidence and1001row truncation/raw retention/null grouping counts. Eight existing pure tests retain contradiction/boundary coverage. Evidence target/core05bg01-coverage-integration-verification.json contains suite counts/source hashes. No remaining failures.
- [x] MySQL full period DTO equality now includes assessment before/after concurrent candidate/source change, repair, restart and deleted-child/dump restoration. Explicit grouped1->ungrouped0 and UNGROUPED_CHARGE reason checks confirm diagnostic changes with source conflicts/missing children; full summary remains unavailable. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after test-owned cleanup.
- [x] Reporting acceptance matrix/semantics, inventory, checklist/overview synchronized. Final inline Node whitespace/status/local-link QA PASS. No schema/writer/public endpoint/AI/learner-status changes, no retained user accounts/services touched, unrelated work preserved. Owner dependencies only in DEPLOYMENT_CHECKLIST.md; no release gate promoted.
- [~] CORE-05-B2 partial. NEXT B-G02: bounded owned account-period financing event evidence across positions, with explicit half-open range and separate principal/contribution/interest-accrued/interest-paid fields; preserve unknown/invalid/missing-position evidence and no current snapshot+event sums. Review writer/source ownership semantics, then pure/batch adapter tests and actual MySQL snapshot/restart/restore. No per-position REQUIRES_NEW loop, financial totals or reconstructed balances. B-G03..06 and public reporting API/UI/export remain unfinished.


- [~] Owned assessment integration contract and simple overview/checklist synchronized while verification runs. No extra queries or totals claimed; B-G01 completion pending test result. NEXT inspect Maven/Surefire and isolated container cleanup. No AI/learner change.

### B-G01 owned integration tests written

- [~] Four new tests cover attached owned/foreign/empty diagnostics, same read-only transaction and fixed queries/no writes, invalid/missing-source reasons,1001row truncation/raw retention/null grouping counts. Existing assessor tests retain contradiction cases. NEXT enabled MySQL plus affected suites; pending.


## 2026-11-25 B-G01 owned assessment integration written

- [~] JournalPeriodService now attaches immutable computed assessment inside existing transaction. Internal DTO retains unassessed constructor for supplied-evidence diagnostics; actual reader invokes assessor before returning. No extra SQL/snapshot or financial changes. Fixture spies assessor. MySQL full DTO equality now covers assessment, with explicit grouped/ungrouped/unavailable-summary checks across source change and missing child.
- [ ] Verification not run. NEXT integration tests and enabled MySQL/affected suites. No schema/API/AI change; full totals remain absent.


## 2026-11-25 B-G01 pure coverage assessment verified

- [x] Added immutable PeriodCoverageAssessment and constructor-injected pure PeriodCoverageAssessor. Bounded selected/source/group/context counts and stable deduplicated enum reasons for history/initialization/truncation/unsupported/invalid/missing/conflicting funding/refund/grouping states. Recomputes cached action/funding effects, validates time/identity/initialization consistency and raw grouping partition/context/visible charge-child role. Contradictory/unavailable input returns null counts; unavailable grouping has null group counts. FullAccountSummaryAvailable always false. No period money totals, ownership/DB proof or history certification. Outside-selection proof responsibility remains with owned reader.
- [x] Initial cmd.exe /d /c ".\mvnw.cmd -q -Dtest=PeriodCoverageAssessorTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,ChargeEvidenceServiceTests,ChargeEvidenceReconcilerTests,PeriodFundingTests,FundingEffectClassifierTests,JournalPeriodServiceTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,SettlementFundsTests,SettlementLotsTests,CombinedTradeServiceTests,FreshSchemaTests test > target\core05bg01-coverage-assessor.log 2>&1" exit0:129 tests/0failures/0errors/0skipped. After role/cache consistency refinement, final cmd.exe /d /c ".\mvnw.cmd -q -Dtest=PeriodCoverageAssessorTests test > target\core05bg01-coverage-assessor-final.log 2>&1" exit0:8tests/0failures/0errors/0skipped. Eight tests cover empty/initialized/pre-initialization, diagnostics/deduplication, flags/unknown groups, stale caches/partition/roles, bounds/false history and actual writer refund/charge with unchanged money. Evidence target/core05bg01-coverage-verification.json contains separate initial/final counts and source hashes. No remaining failures.
- [x] Reporting semantics/checklist/overview synchronized. Scoped whitespace PASS; final inline Node whitespace/status/local-link QA PASS. No financial writer/schema/API/AI/learner changes; retained user accounts/services and unrelated work preserved. Owner dependencies exclusively in DEPLOYMENT_CHECKLIST.md. Pure assessment has no independent database state and is not yet invoked by period reader.
- [~] B-G01 and CORE-05-B2 remain partial. NEXT integrate assessment into the existing owned period reader response without a second snapshot or money writes, and extend tests/MySQL source-change/restart/restore to full assessment equality and unavailable/contradiction states. Preserve existing raw rows/grouping/limits and no summary totals; full account reconciliation, public reporting API/UI/export and B-G02..06 remain unfinished.


### B-G01 initial test result

- [x] Initial affected Maven suite exit0: 129 tests/0failures/0errors/0skipped (target/core05bg01-coverage-assessor.log). NEXT final focused PeriodCoverageAssessorTests rerun for latest role/consistency refinements; pending. No whole-report readiness claim.


### B-G01 contradiction refinement

- [~] Added exact charge child key/action role check and forged-consistent-cache regression (8tests total); funding-effect mismatch/incomplete counts as conflicting/missing even when source flag claims MATCHED. Selected keys reject controls. Initial edit helper hit a local const-variable error after production edit; immediately completed missing test/doc edits. NEXT final focused assessor rerun after initial suite; pending.


- [~] Coverage diagnostic semantics and simple overview/checklist synchronized while tests run. Counts are not financial totals; clean/empty evidence never grants complete history. No AI feature/learning change. NEXT inspect affected test results.

### B-G01 coverage test checkpoint

- [~] Seven tests written: empty initialized/uninitialized, distinct problem-row counts/reason deduplication, truncation/missing grouping with unknown group counts, stale caches/duplicate rows/missing partition, bounds/range/false history, pre-initialization/ungrouped charge, actual writer refund+charge/no balance change. NEXT focused and affected suites; pending. Pure assessor not yet wired into period reader.


## 2026-11-25 B-G01 pure coverage assessor written

- [~] Added immutable PeriodCoverageAssessment and constructor-injected pure PeriodCoverageAssessor. Counts selected unsupported/invalid/missing/conflicting funding/unknown refund/group states; stable deduplicated enum reasons. Recomputes action/funding caches, validates range/initialization/row partition/context and1000 bounds. Contradictory/unavailable input withholds counts; summary availability always false. No totals, DB/auth/schema/API/writer/AI change.
- [ ] Verification not run. NEXT focused diagnostics/contradiction/bounds and actual-reader tests then affected suites. Owned integration/MySQL assessment checks remain pending.


## 2026-11-25 CORE-05-B acceptance review verified

- [x] Reviewed approved inventory/source boundaries against current journal/funding/action/charge/period, ticket/intraday fee, financing, legacy overlap and opening evidence. Added prominent requirement-to-source acceptance matrix to CORE05_REPORTING_SEMANTICS.md. Existing scoped components remain verified within their scopes; B2 and B overall remain partial, C unimplemented. No complete period cash/fee/P&L total or historical balance certified.
- [x] Recorded stable ordered technical gaps B-G01..06 (coverage assessment, financing period evidence, execution-fee period overlap, unresolved source lifecycle, reconciliation eligibility, query-cost review) and C delivery criteria. NEXT B-G01 pure bounded coverage counts/reasons with no money totals/DB/auth changes, followed by owned integration/snapshot/recovery checks. Unknown refunds, unsupported/invalid rows, opening/history gaps, incomplete sources/grouping and truncation must not become zero or complete summaries.
- [x] Updated V3_FEATURE_INVENTORY.md, overview and simple checklist alongside this handoff/queue. Inline Node QA PASS:441 checked application/test/deploy/manifest files byte-identical to target/core05b-acceptance-review-baseline.json;32 local links checked, gap IDs/status/next and whitespace checks pass. Evidence target/core05b-acceptance-review-verification.json. Application tests not rerun for documentation-only review; prior123/0fail/0error/0skip remains historical execution evidence for its source snapshot. No observed validation failures.
- [x] No application/schema/config/financial write, account/service or AI/learner-status change. Unrelated working tree retained; no release gates promoted; owner dependencies exclusively in DEPLOYMENT_CHECKLIST.md. This is a requirements checkpoint, not report implementation/release acceptance.
- [~] CORE-05-B2 remains partial. NEXT implement B-G01 immutable period coverage assessment DTO and pure assessor using explicit row/group/funding states, distinct machine-readable reasons and counts, bounded input and contradiction tests; no numeric cash/P&L total or reconstructed balance. Then integrate into existing owned period snapshot and verify actual MySQL concurrent change/restart/restore before UI/API work.


## 2026-11-25 CORE-05-B acceptance review written

- [~] Compared approved inventory requirements with current readers/classifiers/writers and latest123-test evidence. Added prominent requirement-to-source acceptance matrix, per-scope summary limits and stable B-G01..06 technical gaps to reporting semantics. Next B-G01 is coverage counts/reasons, not money aggregation. No application/schema/test/config change.
- [ ] Documentation QA/source hash comparison pending; no application tests rerun for this documentation unit. Preserve prior123 result as historical execution evidence, not a fresh test run. NEXT verify local links, stable gap IDs and unchanged source/deploy baseline; synchronize simple status.


## 2026-11-25 CORE-05-B2 owned period charge batching verified

- [x] Added PeriodChargeRepository/Adapter to existing JournalPeriodService snapshot and immutable DTO chargeGrouping field. Owned selected parents drive100-reference batches with shared1001candidate sentinel, separate1001proof-source sentinel/100key source batches and combined proof child/source1000bounds. Outside-period children are context; child-only/uncertain/ambiguous records remain ungrouped. Original raw period entries remain when grouping unavailable. Strict caller read-only repeatable transaction guard; no per-parent REQUIRES_NEW service calls, writer/schema/index/public endpoint/AI change. Existing selected-source and proof-source passes have separately bounded budgets, not an unbounded combined fetch.
- [x] Initial affected suite122/0fail/0error/0skip. Previous focused session exit unconfirmed after continuation; final full suite supersedes that pending check. Final process-local V3_JOURNAL_MYSQL=true restored in finally; cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,PeriodChargeAdapterTests,PeriodChargeGrouperTests,ChargeEvidenceServiceTests,ChargeEvidenceReconcilerTests,PeriodFundingTests,FundingEffectClassifierTests,JournalPeriodServiceTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,SettlementFundsTests,SettlementLotsTests,CombinedTradeServiceTests,FreshSchemaTests test > target\core05b2-period-charge-adapter-mysql.log 2>&1" exit0:123tests/0failures/0errors/0skipped. Seven adapter tests cover actual writer grouping/row-once/no writes, outside children/child-only, ownership, missing/extra siblings, candidate/source overflow,102parent two-batch/shared sentinel, same snapshot/no single-parent queries and outside-transaction rejection. Evidence target/core05b2-period-charge-adapter-verification.json contains counts/source hashes.
- [x] MySQL extension: parent-only period includes two outside-period context children; pause batch before concurrent source20->21, old full DTO equal while fresh grouping incomplete/raw parent retained, repair then full DTO equality after restart and child deletion/dump restore. Other user receives only own private child. Existing reader checks retained. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after cleanup. No remaining failures.
- [x] Reporting semantics/checklist/overview synchronized. Scoped whitespace PASS; final inline Node whitespace/status/local-link QA PASS. No AI feature/architecture/learning changes. Retained user accounts/services and unrelated work preserved; no release acceptance promoted. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.
- [~] CORE-05-B2 partial. NEXT reconcile the CORE-05-B acceptance matrix against implemented readers/grouping and document remaining source-specific fee/interest/period coverage gaps and unavailable states before API/UI/export work. Define which direct period effects can be summarized and which must remain unavailable because of refunds, unsupported actions, opening history or truncation; do not infer balances or expenses from grouped magnitudes. Complete account reconciliation, balances/totals and public reports remain unfinished. Candidate reference scan cost remains uncertified.


- [~] Simple checklist/overview synchronized with final adapter MySQL run in progress. Earlier focused process exit remains unconfirmed; final full suite will supersede it. NEXT inspect Maven/Surefire and test-owned cleanup; no AI behavior or learner change.

## 2026-11-25 period charge adapter MySQL extension written

- [x] Initial adapter affected suite122/0fail/0error/0skip. Prior focused process session unavailable after continuation; no exit result claimed for that process. Final full enabled MySQL suite will verify final sources.
- [~] Extended existing synthetic charge scenario: parent-only period uses outside-period children; pause candidate batch before concurrent source20->21; old full DTO equality, fresh incomplete/ungrouped/raw parent retained; repair then restart/deleted-child/dump restoration equality. Other user sees only private child. NEXT enabled MySQL plus affected suites; pending.


### Period charge adapter initial verification

- [x] Affected Maven suite exit0, 122 tests/0failures/0errors/0skipped (target/core05b2-period-charge-adapter.log). All7newadapter tests pass. NEXT final focused PeriodChargeAdapterTests rerun for boundary refinements; pending. MySQL adapter integration remains next.


### Period charge adapter boundary refinement

- [~] Added explicit selected-input1000 bound to package-local adapter as well as its caller; over-limit/null selection returns unavailable grouping before queries. Existing service already caps selected rows. NEXT final focused adapter verification after initial suite.


- [~] Added seventh integration test for102parent batching/shared candidate budget and separate proof-source overflow/raw retention while initial suite runs. Final focused adapter rerun required. Reporting bounds/transaction contract and simple status synchronized; no AI changes. NEXT inspect initial suite then final focused run.

### Period charge adapter tests written

- [~] Six tests written using actual charge writer records: row-once grouping/no money changes/no single-parent service reads, outside child context/child-only retention, foreign sibling/source ownership, missing/extra siblings,1001candidate overflow/raw retention, shared read-only snapshot and rejection outside transaction. NEXT affected Maven suite; pending. New adapter MySQL resilience remains unfinished.


## 2026-11-25 CORE-05-B2 owned period charge adapter written

- [~] Added bounded PeriodChargeRepository and constructor-injected package-local PeriodChargeAdapter to JournalPeriodService. Shares caller active-user/owned read-only repeatable snapshot, no per-parent service calls.100references/query,1001global child and proof-source sentinels; candidates outside period, shared/ambiguous identities preserved for pure grouping. Original raw period entries remain when grouping unavailable; both existing truncation flags suppress grouping. No writer/schema/public API/AI change.
- [ ] Verification not run. NEXT focused integration ownership/outside-period/missing/ambiguous/bounds/snapshot tests and affected suites. New adapter MySQL concurrency/restart/restore remains pending.


## 2026-11-25 CORE-05-B2 supplied period charge grouping tested

- [x] Added immutable GroupedChargeEvidence DTO and constructor-injected pure PeriodChargeGrouper. Recomputes supplied charge proofs and requires cached comparison/full selected raw-row agreement. Cardinality/blocking considers all proofs including outside-selection competitors. Groups only selected unambiguous verified parents with visible children; absent children listed as context keys only, absent parents never introduce charges or hide visible children. Unresolved/conflicting/child-only rows retained. Max1000selected/proofs/combinedchildren/combinedsource rows; invalid/duplicate selected identities/truncation unavailable. No financial sums, ownership/snapshot/date verification, DB/writer/schema/API/AI change.
- [x] Affected cmd.exe /d /c ".\mvnw.cmd -q -Dtest=PeriodChargeGrouperTests,ChargeEvidenceServiceTests,ChargeEvidenceReconcilerTests,PeriodFundingTests,FundingEffectClassifierTests,JournalPeriodServiceTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,SettlementFundsTests,SettlementLotsTests,CombinedTradeServiceTests,FreshSchemaTests test > target\core05b2-period-charge-groups.log 2>&1" exit0: 115 tests/0failures/0errors/0skipped. After source-budget boundary assertion refinement, final cmd.exe /d /c ".\mvnw.cmd -q -Dtest=PeriodChargeGrouperTests test > target\core05b2-period-charge-groups-final.log 2>&1" exit0:7tests/0failures/0errors/0skipped. Seven focused cases cover row-once/ordinary retention/immutability, outside children/parents, ambiguous/outside conflicting proofs, stale row/claimed comparison,1000/1001children/source/input limits and actual writer+owned-reader proof/no money changes. Evidence target/core05b2-period-charge-groups-verification.json includes source hashes. No remaining failures.
- [x] Reporting semantics, checklist/overview synchronized. Scoped whitespace PASS; final inline Node whitespace/status/local-link QA PASS. No AI feature/learning change. Unrelated working tree and retained accounts/services preserved; owner dependencies only in DEPLOYMENT_CHECKLIST.md. Pure grouping has no independent database resilience state and does not certify future DB integration.
- [~] CORE-05-B2 partial. NEXT implement owned bounded period charge-proof batch adapter using selected period rows and parent/reference child candidates outside selection, with shared source checks and this grouper in one repeatable snapshot. No per-parent ChargeEvidenceService calls (REQUIRES_NEW), no unbounded reference scans/queries claimed certified, preserve raw rows when global limits make grouping unavailable. Test ownership/bounds/ambiguity/snapshot and actual MySQL concurrent-change/restart/restore. Complete account reconciliation, balances/totals and public report API/UI/export remain unfinished.


### Period charge grouping initial verification

- [x] Initial affected Maven run exit0 (target/core05b2-period-charge-groups.log); new grouping suite7/7. No failures observed. NEXT final focused PeriodChargeGrouperTests rerun to certify latest boundary assertions; pending. Existing affected results retained; no broader repeat needed for test-only refinement.


- [~] Added explicit1000/1001 combined source-budget boundary test while initial run was in progress; final-source rerun required. Reporting grouping contract and simple overview/checklist synchronized. NEXT finish initial run then final affected suite. No AI/learner changes.

### Period charge grouping test checkpoint

- [~] Seven tests written: exact row-once grouping/ordinary preservation/immutability, context-only outside children, child-only retention, duplicate/conflicting/outside competing proofs, stale raw rows/claimed comparisons,1000/1001 and duplicate identity limits, actual writer+owned-reader proof without money changes. NEXT affected Maven suite; pending.


## 2026-11-25 CORE-05-B2 supplied period charge grouping written

- [~] Added immutable GroupedChargeEvidence and pure constructor-injected PeriodChargeGrouper. Max1000 selected/proofs/combinedchildren/source rows, invalid/duplicate keys unavailable. Recomputes charge proofs, requires full selected-row equality and unique parent/child associations across all proofs, including outside-selection competitors. Groups selected parent with visible verified children; outside child keys context only. Missing parents/uncertain/ambiguous rows retained, no totals/DB/auth/AI change.
- [ ] Verification not run. NEXT supplied grouping invariants and affected suites. Future owned same-snapshot batch period adapter remains unimplemented.


## 2026-11-25 CORE-05-B2 owned charge MySQL resilience verified

- [x] Extended only SettlementJournalMySqlTests with independent synthetic charge scenario after earlier reader assertions. Mixed20settled/30credit funds70charge; pause after owned parent, concurrently change reserve allocation-event20->21. Old complete DTO equals matched snapshot; next read MISMATCH with no settled effect. Repair then full DTO equality after separate backup/restart and after child deletion/restore. Missing child is INCOMPLETE without implied-debt value. Foreign referenced sibling excluded and foreign parent denied; virtual balance1234.57 retained. Earlier reader checks retained. No production code/schema/writer/API/AI change.
- [x] Process-local V3_JOURNAL_MYSQL=true restored in finally. cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,ChargeEvidenceServiceTests,ChargeEvidenceReconcilerTests,PeriodFundingTests,FundingEffectClassifierTests,JournalPeriodServiceTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,SettlementFundsTests,SettlementLotsTests,CombinedTradeServiceTests,FreshSchemaTests test > target\core05b2-charge-resilience.log 2>&1" exit0: 109 tests/0failures/0errors/0skipped. Evidence target/core05b2-charge-resilience-verification.json includes suite counts and test source hash. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after test-owned cleanup. No remaining failures.
- [x] Checklist, overview and reporting semantics synchronized. Scoped whitespace PASS; final inline Node whitespace/status/local-link QA PASS. No AI behavior/architecture/learning change. Retained user accounts/services and unrelated working-tree changes preserved; no release gate promoted. Owner dependencies exclusively in DEPLOYMENT_CHECKLIST.md.
- [~] CORE-05-B2 partial. NEXT define/test bounded period charge grouping: preserve raw selected rows, gather owned charge parents and all children/source proofs outside the selected period within one snapshot, collapse only unambiguous matched parent/child groups, retain unresolved child-only/out-of-period-parent rows, and never sum parent+child magnitudes as expenses. Start with explicit supplied-evidence grouping contract before database batching. Complete account reconciliation, balances/totals and report API/UI/export remain unfinished. Candidate reference lookup scan cost remains uncertified.


- [~] Simple checklist/overview synchronized while owned-charge MySQL verification runs. No result claimed yet; NEXT inspect Maven/Surefire and test-owned cleanup. Non-AI reporting test work, no AI feature or learner-status change.

## 2026-11-25 CORE-05-B2 owned charge MySQL resilience written

- [~] Extended only SettlementJournalMySqlTests with independent synthetic charge scenario after earlier reader assertions. Mixed20settled/30credit funding of70charge; pause owned parent, concurrent source event20->21; old DTO equal/new conflict, repair then restart and child deletion/dump restore equality. Foreign referenced sibling excluded; foreign parent denied; virtual balance retained. Separate backup/restart keeps earlier evidence assertions intact. No production change.
- [ ] Verification pending. NEXT enabled MySQL plus affected suites and test-owned container cleanup.


## 2026-11-25 CORE-05-B2 owned single-charge reader tested

- [x] Added immutable ChargeEvidence DTO, constructor-injected ChargeEvidenceRepository/Service. Active server identity/owned parent; exact expected keys OR owned derived CHARGE reference catches malformed and extra siblings without date filtering. Parent/children/source lookups share read-only REQUIRES_NEW/REPEATABLE_READ snapshot. Max1001 child/source sentinels; separate overflow flags, incomplete results without money values. Missing/ambiguous candidates avoid source lookup; no per-row transaction. Pure reconciler reference derivation shared. No financial writes/init, schema/index/dependency/public API/AI change.
- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=ChargeEvidenceServiceTests,ChargeEvidenceReconcilerTests,PeriodFundingTests,FundingEffectClassifierTests,JournalPeriodServiceTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,SettlementFundsTests,SettlementLotsTests,CombinedTradeServiceTests,FreshSchemaTests test > target\core05b2-charge-reader.log 2>&1" exit0: 108 tests/0failures/0errors/0skipped. Seven new tests backed by actual charge writer cover outside-parent-date children, mixed funds, immutable output/no balance change, identity/inactive denial, missing/foreign/malformed/extra children, missing/conflicting funding,1001 sentinels and one independent read-only snapshot/fixed queries. Evidence target/core05b2-charge-reader-verification.json includes suite counts/source hashes. No remaining failures. Actual MySQL resilience for new reader remains unverified.
- [x] Reporting semantics/checklist/overview synchronized. Changed-source whitespace PASS; final inline Node whitespace/status/local-link QA PASS. No AI feature/architecture/learning-status change in this reporting unit. Retained accounts/services and unrelated working-tree changes preserved. Owner dependencies only in DEPLOYMENT_CHECKLIST.md; no release gate promoted.
- [~] CORE-05-B2 partial. NEXT extend isolated SettlementJournalMySqlTests for owned charge reader: pause after owned parent, concurrently change a child/source amount; old full DTO matched vs new conflict, repair, restart, delete a child then dump restore full DTO equality, foreign-user denial. Keep candidate-query scan limitation explicit. After resilience, design bounded period grouping without double counting child rows or treating absent children as zero funding. No complete account reconciliation, balances/financial totals or public report API/UI/export exists.


- [~] Owned single-charge semantics and simple checklist/overview synchronized while tests run. One-charge reader does not yet group a full period or certify scan performance. NEXT inspect affected results. No AI change.

### Owned charge reader test checkpoint

- [~] Seven tests written using actual charge writer fixtures: mixed funding/outside-parent-date children, ownership/inactive denial, missing/foreign children, malformed exact-key refs and extra siblings, missing/conflicting funding, child/source1001sentinels, immutable output/no writes and same independent read-only snapshot/fixed queries. NEXT affected Maven suite; pending.


## 2026-11-25 CORE-05-B2 owned charge reader written

- [~] Added immutable ChargeEvidence DTO and constructor-injected repository/service. Active server identity, owned parent/exact-key OR derived-reference child lookup without date restriction, shared source checks and reconciler in one read-only REQUIRES_NEW/REPEATABLE_READ snapshot. Max1001 child/source sentinel rows; missing/ambiguous groups avoid funding reads; no per-row services/writes/schema/API/AI change.
- [ ] Verification not run. NEXT ownership, outside-period children, corrupt/missing/ambiguous data, bounds and same-snapshot tests plus affected suites. Actual MySQL source change/restart/restore for new reader remains pending.


## 2026-11-25 CORE-05-B2 supplied composite charge pair tested

- [x] Added constructor-injected pure ChargeEvidenceReconciler: exact valid REALIZED_CHARGE parent, writer-derived UUID reference and exact reserve/spend keys/actions, one matched pair with identical normalized allocation sources and amounts<=parent. Parent charge counted once; child amount is funded portion, remainder explicitly implied unfunded portion. Direct settled effect derived only from supplied writer-rule pair; no outstanding-account-debt or expense-total certification. Missing/limited/unverified children withhold results; empty children cannot prove full debt. Ambiguous/duplicate/conflicting evidence rejected. No owned adapter, financial writer/schema/API/dependency/AI behavior change.
- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=ChargeEvidenceReconcilerTests,PeriodFundingTests,FundingEffectClassifierTests,JournalPeriodServiceTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,SettlementFundsTests,SettlementLotsTests,CombinedTradeServiceTests,FreshSchemaTests test > target\core05b2-charge-rules.log 2>&1" exit0: 101 tests/0failures/0errors/0skipped. Six new tests cover pair order/immutability, mixed funding/charge counted once, missing/limited/ambiguous/duplicate keys, invalid parents, source conflicts/unverified pair, actual writer fully funded/mixed/debt-only scenarios with unchanged money. Evidence target/core05b2-charge-rules-verification.json includes suite counts/source hashes. No remaining failures. Pure component has no independent MySQL state; future owned integration remains unverified.
- [x] User question answered: no live AI-model connection; optional helper is fixed-question Java; reporting development continues. Added explicit plain-language explanation to checklist without changing AI feature or learner status. Roadmap reviewed, no architecture/prerequisite change. Reporting semantics/checklist/overview synchronized; final inline Node whitespace/status/local-link QA PASS. Unrelated work and retained accounts/services preserved. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.
- [~] CORE-05-B2 partial. NEXT implement bounded owned composite-charge evidence reader: active server identity, parent/derived-reference child lookup (including outside period), shared funding checks and sources in one repeatable snapshot, conservative missing/ambiguous groups, no per-row REQUIRES_NEW calls. Test ownership/bounds/no writes/snapshot and actual MySQL concurrent-change/restart/restore before report integration. Complete account reconciliation, report totals, public API/UI/export and live AI remain unfinished.


- [~] Added plain-language explanation of disabled live AI to checklist at user request: local helper is fixed-question Java, broader model connection unfinished; reporting continues. No AI feature/architecture/learning status changed. Composite rules/limits documented and simple status synchronized while tests run. NEXT inspect Maven results.

### Composite charge test checkpoint

- [~] Six tests written: exact mixed funding group, parent-only/truncated missing evidence, duplicate/ambiguous/unrelated child keys, source conflict/unverified pair, invalid/noncharge parent, actual writer fully funded/mixed/debt-only cases with unchanged money. No inference of full debt from absent children. NEXT affected Maven suite; pending.


## 2026-11-25 CORE-05-B2 supplied composite charge rules written

- [~] Added pure constructor-injected ChargeEvidenceReconciler. Validates REALIZED_CHARGE parent and exact writer-derived UUID CHARGE reference/child keys, exactly one verified reserve/spend pair with identical normalized allocation sources and amounts<=parent. Returns parent amount once, funded portion and explicitly implied unfunded remainder/direct settled effect; missing children do not prove all-debt. No owned reader, aggregation, schema/writer/API/AI change.
- [ ] Verification not run. NEXT pure invariants and actual writer/read-source fixtures, then affected suites. Future owned child lookup outside period and MySQL integration remain unfinished.


## 2026-11-25 CORE-05-B2 period funding integration verified

- [x] Extracted FundingEvidenceChecker with existing paginated funding rules preserved. JournalPeriodService now reads source evidence directly within its owned read-only REQUIRES_NEW/REPEATABLE_READ snapshot and includes immutable funding checks/effects per entry. At most10 batches/100keys; shared1001-row sentinel budget, max1000accepted. Overflow suppresses all selected funding source metadata/effects including earlier batches, with separate fundingEvidenceTruncated flag; raw journal/action evidence retained. No per-row service/SQL, financial writes/init, schema/index, public endpoint or AI change.
- [x] Initial affected local run exit0 (target/core05b2-period-funding.log). Final process-local V3_JOURNAL_MYSQL=true restored in finally; cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,PeriodFundingTests,FundingEffectClassifierTests,JournalPeriodServiceTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,SettlementFundsTests,SettlementLotsTests,CombinedTradeServiceTests,FreshSchemaTests test > target\core05b2-period-funding-mysql.log 2>&1" exit0:96 tests/0failures/0errors/0skipped. Six new tests cover ownership/foreign metadata, same transaction/no writes, nonfunding no-query/missing evidence,1000keys/10queries, second-batch sentinel discarding earlier effects, actual mixed refund. MySQL extension pauses after period rows while allocation amount changes0.01->0.02; old full DTO remains equal, fresh source check MISMATCH with null effect, repaired DTO equals restart/dump restore. Evidence target/core05b2-period-funding-verification.json contains suite counts/source hashes. No remaining failures.
- [x] docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after test-owned cleanup. Reporting semantics/checklist/overview synchronized and long user-facing technical status paragraphs shortened; historical evidence retained here. Changed-source whitespace PASS; final inline Node status/local-link QA PASS. No AI change or learner-status change; no roadmap maintenance needed for this non-AI unit. Retained user data/services and unrelated working-tree changes preserved. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.
- [~] CORE-05-B2 partial. NEXT define/test bounded composite REALIZED_CHARGE parent/child evidence rules against SettlementFunds.chargeDebt: writer-derived CHARGE references, matched reserve/spend pairs, missing/ambiguous siblings and uncovered-debt evidence. Do not infer completeness from event prefixes or sum parent+child magnitudes as expenses. Keep historical refund destinations unknown. Complete account reconciliation, balances/financial totals and public report API/UI/export remain unfinished.


- [~] Simplified long technical status paragraphs in checklist/overview; historical test counts remain in handoff. Source-change MySQL test running; no completion claimed. NEXT inspect final results and test-container cleanup.

## 2026-11-25 period funding MySQL source-change check written

- [x] Initial local affected suite exit0 (target/core05b2-period-funding.log); counts pending final evidence collection.
- [~] Extended MySQL test: pause period repository after rows; concurrently change allocation event amount0.01->0.02. Paused full DTO must equal prior view, new read MISMATCH with null settled funding effect; repair before existing restart/dump restoration equality. No retained database used. NEXT enabled MySQL plus affected suites; pending.


- [~] Period funding bounds/overflow semantics and simple checklist/overview synchronized while local tests run; dates refreshed. No acceptance claimed yet. NEXT inspect local run then actual source-change MySQL/restart/restore integration. No AI feature/learning change.

### Period funding test checkpoint

- [~] Six integration tests written: owned sources in same read-only snapshot/no writes, foreign metadata exclusion, nonfunding no query/missing evidence retained,1000keys/10queries/exact global budget, second-batch overflow invalidates earlier funding effects, actual writer mixed refund remains unknown. NEXT affected Maven suite; pending. New source-change MySQL resilience remains unfinished.


## 2026-11-25 CORE-05-B2 period funding integration written

- [~] Extracted shared FundingEvidenceChecker preserving paginated funding service rules. Added bounded repository overload (1..1001rows,100keys) and period adapter batching max10 queries under global1001 sentinel budget in existing owned read-only snapshot. Immutable period entries now include source check and funding effect; source overflow marks all funding entries incomplete with no source numbers. Separate journal/funding truncation flags. No public endpoint/writer/schema/AI change.
- [ ] Verification not run. NEXT focused source ownership, batching/sentinel, no-per-row-query and same-snapshot tests plus affected suites. New integration MySQL source-change/restart/restore remains pending.


## 2026-11-25 CORE-05-B2 supplied funding effects verified

- [x] Added pure constructor-injected FundingEffectClassifier over supplied MATCHED funding evidence. Revalidates journal money/reference shape, no conflicting issues, unique allocation identities, positive exact source money, source key/type/policy/status and exact source sum. Max1000 sources, normalized2decimal output without rounding. RESERVE exposes settled-origin debit and reserved increase; SPEND consumes reserved cash without second settled debit; RELEASE settled-only known, credit-origin complete settled delta withheld regardless of current PENDING/SETTLED status. Other/composite actions not evaluated. No caller ownership/snapshot proof, expense/transaction aggregation, financial writer/schema/API/AI change.
- [x] Final cmd.exe /d /c ".\mvnw.cmd -q -Dtest=FundingEffectClassifierTests,JournalPeriodServiceTests,JournalActionClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,SettlementFundsTests,SettlementLotsTests,CombinedTradeServiceTests,FreshSchemaTests test > target\core05b2-funding-effects-final.log 2>&1" exit0: 89 tests/0failures/0errors/0skipped. Seven new tests include actual writer+owned funding-reader mixed refund after settlement, composite partially funded charge with debt, split effects, withheld unmatched/composite/conflicting evidence, exact OPENING_SETTLED/trailing zeros and1000/1001. Evidence target/core05b2-funding-effects-verification.json includes source hashes. Initial run also passed; final rerun follows enum/precision refinements. No remaining failures. Pure component has no independent database resilience state; no new MySQL integration certified.
- [x] Reporting semantics, simple checklist and overview synchronized. Scoped whitespace PASS; final inline Node status/local-link QA PASS. AI roadmap reviewed with no changed AI architecture/feature/prerequisites or personal learning status, so no AI log update required. Retained accounts/user services and unrelated working-tree changes preserved. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.
- [~] CORE-05-B2 partial. NEXT extract shared source-evidence checking from SettlementFundingService so the owned period adapter can batch allocation evidence within its existing snapshot. Preserve100-key/1000-row evidence limits or explicitly design bounded batching; no per-row REQUIRES_NEW service calls. Integrate the pure funding effects with period evidence and add ownership/bounds/snapshot and MySQL restart/restore tests. Composite charge parent/child grouping, unknown historical refunds, account reconciliation, balances/totals and public report API/UI/export remain unfinished.


### Funding-effect precision refinement

- [~] Normalize exact source amounts to2decimal places before component sums; added OPENING_SETTLED/trailing-zero coverage. No rounding accepted. NEXT final affected suite once initial process exits; final sources not yet certified.


- [~] Funding-effect semantics and simple overview/checklist synchronized. Qualified overlapping test enum names during review; first run may compile final test source or earlier source, so final-source verification rerun required after it exits. NEXT inspect initial result then final affected suite. No source behavior change or AI/learner change.

### Funding effects test checkpoint

- [~] Seven tests written: split reserve/spend, refund uncertainty with current PENDING/SETTLED, unmatched/composite withholding, forged MATCHED conflicts,1000/1001 bounds, actual writer+owned funding-reader mixed refund and partially funded charge/debt. NEXT Maven affected suite; pending. No period batch integration or aggregate claimed.


## 2026-11-25 CORE-05-B2 source funding effects written

- [~] Added constructor-injected pure FundingEffectClassifier over supplied MATCHED SettlementFundingPage.Event. Revalidates journal/reference shape, exact positive source amounts, unique allocations, source type/key/policy/status and sum. RESERVE exposes settled-origin debit; SPEND no second settled debit; RELEASE settled-only known, any credit-origin destination unknown. Composite charge/other actions not evaluated; no totals, ownership/DB/snapshot proof or production writer/schema/API/AI change.
- [ ] Verification not run. NEXT pure invariants and actual writer/funding-reader evidence tests, then affected suites. Future owned period batch adapter and composite charge grouping remain unfinished.


## 2026-11-25 CORE-05-B2 journal period MySQL resilience verified

- [x] Extended only SettlementJournalMySqlTests to tenth concurrent reader. Shared coverage latch waits for3 initialization reads before concurrent commit. Paused period result remains uninitialized/empty; fresh result initialized with a/z in ascending timestamp/key order and excludes the foreign row. Actual MySQL exclusive end boundary confirmed, unknown reservation settled effect retained, completeFinancialHistory remains false. Full period DTO equals restart and dump-restored result; deliberately removed journal rows yield empty evidence without a completeness claim. Existing journal user index eligible for actual bounded period query (EXPLAIN possible_keys); no scan/load-performance certification. Earlier nine readers retained. No production schema/writer/API/AI change.
- [x] Process-local V3_JOURNAL_MYSQL=true restored in finally. cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,JournalPeriodServiceTests,JournalActionClassifierTests,CombinedTradeServiceTests,TradeRecordCombinerTests,OptionLegacyServiceTests,EquityLegacyServiceTests,PaperTradingTests,IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,TicketFeeServiceTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,SettlementFundsTests,SettlementLotsTests,FreshSchemaTests test > target\core05b2-period-resilience.log 2>&1" exit0:183 tests/0failures/0errors/0skipped. Evidence target/core05b2-period-resilience-verification.json includes suite counts and test source hash. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after test cleanup. No remaining failures.
- [x] Checklist, overview and reporting semantics synchronized. Scoped whitespace PASS; final inline Node status/local-link QA PASS. AI roadmap reviewed; no AI feature/architecture/framework/learner-status change, so no learning-log change required. Retained user data/services and unrelated working-tree changes preserved; no deployment gate promoted. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.
- [~] CORE-05-B2 partial. NEXT define and test source-aware period cash-effect coverage from immutable allocation events: distinguish verified reserve/spend source splits, unknown historical refund destinations and linked composite charge effects before any aggregation. Reuse one owned snapshot in a future adapter; no per-row REQUIRES_NEW service calls. Today's source status cannot certify release-time destination. No complete account reconciliation, opening/closing/running balances, financial totals or public report API/UI/export exists.


- [~] Simple checklist/overview synchronized with period MySQL run in progress. No database resilience result claimed yet. NEXT inspect final Maven/Surefire and container cleanup. No AI behavior/architecture/learning-status changes; roadmap reviewed.

## 2026-11-25 CORE-05-B2 period reader MySQL checks written

- [~] Extended only SettlementJournalMySqlTests to tenth concurrent reader; shared coverage latch now waits for3 readers. Period read pauses after initialization, concurrent commit inserts owned/foreign dated rows; old view must stay uninitialized/empty, next view initialized with own a/z only. MySQL end exclusion, unknown reservation effect, foreign-user inventory, restart/full DTO equality, removed-row evidence and dump-restore equality, existing query index eligibility asserted. No production schema/writer/API/AI change.
- [ ] Verification pending. NEXT enabled isolated MySQL and affected suites, inspect results/test-owned cleanup. No retained user database/services touched.


## 2026-11-25 CORE-05-B2 owned journal period evidence tested

- [x] Added immutable JournalPeriodEvidence, constructor-injected JournalPeriodRepository/Service. Active server identity, owned half-open Instant selection, coverage/rows in same independent read-only REQUIRES_NEW/REPEATABLE_READ snapshot. Whole-second bounds within supported MySQL TIMESTAMP range, max366 elapsed days.1001 sentinel/first1000 rows with explicit truncation. Raw unknown/invalid/source-dependent evidence retained alongside classifier interpretation. Initialization metadata does not certify history; completeFinancialHistory always false. No financial writer/init, schema/index/dependency/public API/UI/AI changes.
- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=JournalPeriodServiceTests,JournalActionClassifierTests,SettlementJournalServiceTests,SettlementFundingServiceTests,CombinedTradeServiceTests,SettlementFundsTests,SettlementLotsTests,FinanceEventClassifierTests,FreshSchemaTests test > target\core05b2-journal-period.log 2>&1" exit0: 87 tests/0failures/0errors/0skipped. Seven new tests cover inclusive start/exclusive end and adjacent ranges, timestamp/key ties, immutable output, ownership/inactive denial, uninitialized/pre-initialization evidence, unknown/invalid/refund retention, invalid bounds before database access,366-day limit,1000/1001 and shared independent read-only snapshot/no writes. Evidence target/core05b2-journal-period-verification.json includes suite counts/source hashes. No remaining failures.
- [x] Period contract and limitations documented in CORE05_REPORTING_SEMANTICS.md. Simple overview/checklist synchronized; scoped whitespace PASS, final inline Node status/local-link QA PASS. AI roadmap reviewed without changed AI feature/architecture or learner completion; no AI log update required. User accounts, existing services and unrelated working-tree changes preserved; owner dependencies only in DEPLOYMENT_CHECKLIST.md.
- [~] CORE-05-B2 remains partial. NEXT extend isolated SettlementJournalMySqlTests to this period reader: pause after initialization, commit dated journal event on second connection, old/new range visibility, restart and deliberately removed-record/dump-restore equality, foreign-user exclusion and actual query-index evidence. New reader's database resilience remains unverified. No complete account/period reconciliation, opening/closing/running balances, totals or public reporting API/UI/export exists.


- [~] Period semantics and simple overview/checklist synchronized while tests run. Contract documents whole-second UTC bounds,366-day cap,1000-row truncation, initialization versus history and no aggregate/balance claim. NEXT inspect Maven/Surefire. AI roadmap reviewed; no changed AI behavior/architecture/learning status.

### Period reader tests written

- [~] Seven focused tests cover inclusive/exclusive adjacency, stable timestamp/key ties, immutable output, ownership/inactive denial, uninitialized/pre-initialization evidence, malformed/unsupported/refund retention, invalid/overlong/subsecond bounds,1000/1001 and same independent read-only transaction for coverage/rows without money writes. NEXT affected Maven suites; results pending.


## 2026-11-25 CORE-05-B2 journal period reader written

- [~] Added immutable JournalPeriodEvidence DTO and constructor-injected repository/service. Active server identity, one read-only REQUIRES_NEW/REPEATABLE_READ snapshot, owned half-open Instant bounds, whole-second TIMESTAMP precision, max366 days and supported MySQL timestamp range,1001 sentinel/first1000 with explicit truncation. Retains raw rows plus classifier interpretations; initialization is separate from completeFinancialHistory=false. No aggregate, writer/schema/API/AI change.
- [ ] Verification not run. NEXT focused boundary/ownership/invalid/unknown/coverage/bounds/snapshot tests and affected suites. New-reader MySQL snapshot/restart/restore remains pending.


## 2026-11-25 CORE-05-B2 journal action interpretation verified

- [x] Added pure JournalActionClassifier after reviewing SettlementFunds/SettlementLots writers. Interprets eight cash actions plus zero delivery-link metadata, validates exact magnitude/reference shape, exposes only known direct settled/reserved deltas. Source-dependent reservation/refund settled effects and composite charges remain unknown; unsupported lot units/cost and future actions withhold every delta. No ownership/source verification, aggregation, database/runtime state, schema/writer/API/dependency/AI change. No historical balance/P&L/expense claim.
- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=JournalActionClassifierTests,SettlementFundsTests,SettlementLotsTests,SettlementJournalServiceTests,SettlementFundingServiceTests,CombinedTradeServiceTests,FinanceEventClassifierTests,FreshSchemaTests test > target\core05b2-journal-action.log 2>&1" exit0: 80 tests/0failures/0errors/0skipped. Seven new tests include actual mixed-source reservation/refund after settlement (refund destination still withheld), funded charge with linked records and uncovered debt (no duplicated debit), money/ref validation, unsupported units and zero metadata/settlement. Evidence target/core05b2-journal-action-verification.json includes source hashes. No remaining failures. Pure classifier has no independent database resilience state; previous MySQL evidence remains scoped to earlier readers.
- [x] Reporting action table, historical coverage limits, simple checklist and overview synchronized. Scoped whitespace PASS; final inline Node local-link/status check PASS. AI roadmap reviewed with no changed AI feature/architecture or personal learning status; no learning-log change required. Existing accounts, user services and unrelated working tree retained; no deployment gates changed.
- [~] CORE-05-B2 partial. NEXT implement bounded owned journal period evidence with explicit half-open Instant bounds and coverage metadata, reusing the classifier in one read-only snapshot. Preserve unsupported/invalid/unknown effects, distinguish initialization from complete history, and do not aggregate or fabricate running balances. Then focused boundary/ownership/bounds/snapshot tests and actual MySQL restart/restore. Complete period/account reconciliation, source-dependent bucket totals and public report API/UI/export remain unfinished. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.


- [~] Reporting action table/history gaps and simple checklist/overview synchronized while tests run. NEXT inspect final Maven/Surefire. No AI architecture/feature/learner-status change; roadmap reviewed without changing learning progress.

### Journal interpretation test checkpoint

- [~] Seven tests written, including actual mixed settled/pending refund after source settlement and partially funded realized charge with debt/sibling records. Other checks cover exact signs, no second settled debit, unsupported lot units, malformed money/references and zero cash settlement/metadata. NEXT Maven affected suite; pending. No application writes changed.


## 2026-11-25 CORE-05-B2 journal action interpretation written

- [~] Reviewed SettlementFunds and SettlementLots writers. Added pure JournalActionClassifier for eight cash actions plus zero-valued delivery-link metadata. Exact magnitudes/reference validation; known direct settled/reserved effects only, null for source-dependent refund/reservation and composite-charge uncertainty. Lot quantity/cost actions remain unsupported, never cash by inference. No source verification, ownership, aggregation, period/API/schema/writer/AI change.
- [ ] Verification not run. NEXT tests for signed direct effects, split funding/refunds, composite charges, zero settlement/metadata, invalid/unknown/unit records and actual writer evidence; then affected regression suite. Full historical/period reconciliation remains unfinished.


## 2026-11-25 CORE-05-B2 combined reader MySQL resilience verified

- [x] Extended only SettlementJournalMySqlTests with ninth paused reader. Combined reader pauses after owned fills; concurrent equity/option price conflicts leave two pre-commit pairs while a fresh read retains both conflicts separately (zero pairs). Equity/option ID100 remains namespace-separated. Repaired combined DTO equals post-restart and post-dump-restore DTO; deleting option order retains its fill as a singleton with INCOMPLETE. Foreign-owned order/fill inventory excluded from owner result; other user sees only its own two identities. Existing eight readers retained. No production code/schema/writer/API/AI change.
- [x] Process-local V3_JOURNAL_MYSQL=true restored in finally. cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,CombinedTradeServiceTests,TradeRecordCombinerTests,OptionLegacyServiceTests,EquityLegacyServiceTests,PaperTradingTests,IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,TicketFeeServiceTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test > target\core05b2-combined-resilience.log 2>&1" exit0:131 tests/0failures/0errors/0skipped. Evidence target/core05b2-combined-resilience-verification.json. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after test cleanup. No remaining failures.
- [x] Reporting semantics, checklist and overview synchronized. Scoped whitespace PASS; final inline Node status/local-link validation PASS. AI roadmap reviewed; no AI feature/architecture/framework or learner-status changes. Unrelated working-tree edits preserved, retained user data/services untouched. No deployment/release gate promoted; owner dependencies only in DEPLOYMENT_CHECKLIST.md.
- [~] CORE-05-B2 remains partial. NEXT review remaining account/period statement semantics against journal writers (SETTLED_CREDIT, WITHDRAW, PENDING_CREDIT, RESERVE, SPEND, RELEASE, SETTLE, REALIZED_CHARGE), define a bounded action-effect/coverage contract and tests before aggregation. Today's mutable allocations cannot establish arbitrary past balances; missing historical coverage must stay explicit. No complete period/account reconciliation, financial totals or public report API/UI/export exists. Current combined reader is bounded identity evidence, includes unexecuted orders and does not certify database scan cost.


- [~] Simple checklist/overview synchronized with MySQL verification running. No result or release acceptance claimed. NEXT inspect Maven/Surefire and test-owned container cleanup. AI roadmap reviewed; no AI feature, architecture or learner-status change.

## 2026-11-25 CORE-05-B2 combined reader resilience written

- [~] Extended synthetic MySQL fixture to ninth concurrent reader, paused after owned fills. Concurrent equity/option price conflicts must preserve two old pairs and yield zero new pairs. Repaired snapshot must equal restart/restore result; deleting option order must retain its fill separately. Added foreign-user identity inventory/exclusion checks. Only test code changed; no production schema/writer/API/AI change.
- [ ] Verification pending. NEXT enable process-local V3_JOURNAL_MYSQL for isolated MySQL plus affected suites; inspect results and test container cleanup. Existing user databases/services untouched.


## 2026-11-25 CORE-05-B2 owned combined identity reader tested

- [x] Added CombinedTradeRepository/Service using constructor injection and active server identity; all owned fills and equity/option order identities gathered in one read-only REQUIRES_NEW/REPEATABLE_READ transaction. Left ticket join retains missing/cross-owner-ticket fills. Prefetched maps reuse shared matchers; two grouped reverse queries detect foreign/orphan competitors without exposing their IDs. No per-fill SQL/service transaction, money write/init, schema/index/dependency/API/AI change. Global1000 identity limit (before grouping); overflow returns unavailable with no partial groups. Includes unexecuted legacy orders as identities, not executed trade totals.
- [x] Final cmd.exe /d /c ".\mvnw.cmd -q -Dtest=CombinedTradeServiceTests,TradeRecordCombinerTests,OptionLegacyServiceTests,EquityLegacyServiceTests,PaperTradingTests,FreshSchemaTests test > target\core05b2-combined-reader-final.log 2>&1" exit0: 54 tests/0failures/0errors/0skipped. Evidence target/core05b2-combined-reader-verification.json contains suite counts and source SHA256 hashes. Seven focused tests cover two namespaces, legacy-only/unexecuted/conflicting/missing/cross-owner records, foreign and orphan competitors, identity/inactive denial,1000/1001, no balance change/init, independent repeatable-read transaction and fixed repository calls. First six-test run passed before extra conflict test; final sources rerun. No remaining observed failures.
- [x] Reporting semantics, overview and simple checklist synchronized. Changed-file whitespace and simple-document local links PASS. AI roadmap reviewed: no AI feature/architecture or learner-status change; no AI change-log update needed. Existing accounts and unrelated working-tree edits retained; no retained database or running user service accessed.
- [~] CORE-05-B2 partial. NEXT extend isolated SettlementJournalMySqlTests for this combined reader: concurrent record change old/new snapshot, restart, missing-record then dump restoration equality and foreign-user exclusion. This adapter is not yet certified by those real-database tests. Complete period/account reconciliation, financial totals and public API/UI/export work remain unfinished; legacy-link/user scan cost is not certified. Owner dependencies remain exclusively in DEPLOYMENT_CHECKLIST.md.


### Batch reader verification refinement

- [~] Added metadata-conflict/unexecuted-order retention test (seven focused tests total); semantics and simple overview/checklist synchronized with reader under verification. First affected run was already running; rerun final sources after it finishes. No result claimed yet. NEXT final affected suite and report inspection.


### Batch reader test checkpoint

- [~] Six synthetic tests written for both namespaces, legacy-only/missing/cross-owner records, foreign and orphan competing references, identity/inactive denial, global1000/1001 limit, no financial initialization and independent read-only snapshot with fixed repository-call counts. Fixture registers the new reader and spy. NEXT affected Maven command; results pending. New MySQL resilience remains pending.


## 2026-11-25 CORE-05-B2 owned batch reader written

- [~] Added constructor-injected CombinedTradeRepository/Service: active server identity, one read-only repeatable snapshot, independent owned-fill retention, owned equity/option identities, maximum1000 combined records with overflow unavailable, two grouped reverse-reference queries and prefetched matcher lookups. No per-fill service calls or money/schema/API/AI changes. Legacy orders include unexecuted identities, not executed-trade totals.
- [ ] Verification not yet run. NEXT focused ownership/namespace/missing-ticket/foreign-competitor/bounds/snapshot tests and affected suites. New reader MySQL concurrency/restart/restore still pending.


## 2026-11-25 CORE-05-B2 reusable overlap rules verified

- [x] Extracted EquityLegacyMatcher/OptionLegacyMatcher with caller-supplied lookup functions. Existing services retain authenticated active-user/owned-fill checks, constructor injection and read-only REQUIRES_NEW/REPEATABLE_READ boundaries. Rules and lazy lookup sequence preserved; future batch adapter can supply prefetched maps without per-fill service transactions. No financial write/schema/API/AI/dependency change.
- [x] Final command: cmd.exe /d /c ".\mvnw.cmd -q -Dtest=TradeRecordCombinerTests,OptionLegacyServiceTests,EquityLegacyServiceTests,PaperTradingTests,FreshSchemaTests test > target\core05b2-shared-matchers-final.log 2>&1" exit0. Surefire 47 tests/0 failures/0 errors/0 skipped. Evidence target/core05b2-shared-matchers-verification.json. Existing meaningful ownership, ambiguity, metadata, snapshot and actual-writer regressions pass. Earlier run started before edits and is not used as final evidence. No remaining observed failures.
- [x] CORE05_REPORTING_SEMANTICS.md now specifies bounded same-snapshot batch reads, independent fill retention when ticket data is missing, legacy-only records and reverse competitors outside the selected input. Simple checklist/overview synchronized. Scoped whitespace check passed for9 changed files. AI roadmap reviewed; no AI architecture/feature or personal learner-status change, so no learning-log update required. Unrelated working-tree changes preserved.
- [~] CORE-05-B2 remains partial. NEXT implement owned bounded batch repository/service using these matchers and TradeRecordCombiner, then focused ownership/bounds/no-per-fill-query tests and MySQL concurrent snapshot/restart/dump restore. Combined database report, period/account reconciliation and public API/UI/exports remain unfinished. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.


## 2026-11-25 CORE-05-B2 shared overlap rules (verification pending)

- [~] Extracted equity/option comparison rules into package-private matchers accepting caller-supplied lookup functions. Existing authenticated readers retain ownership checks and read-only REQUIRES_NEW/REPEATABLE_READ transactions; a future batch reader can supply prefetched maps. No schema, writer, endpoint or AI change.
- [ ] Verification pending for changed sources. An initial verification command started before edits because Python was unavailable; it cannot certify the extraction. NEXT run affected suites against final sources.
- [~] Combined database reader and its concurrency/restart/restore tests remain unimplemented. CORE-05-B2 partial.


## 2026-11-24 CORE-05-B2 combined supplied-record rules tested

- [x] Added immutable CombinedTradeEvidence DTO and pure TradeRecordCombiner component. Typed namespace+ID keys, grouping only present verified one-to-one pairs; unmatched/unverified/ambiguous records retained with INCOMPLETE status. Proof cardinality includes out-of-input competitors. Max1000 records/proofs; truncation/invalid duplicate input returns unavailable. Immutable represented identities preserve evidence; no money/units/fees/P&L sums, ownership/DB/full-history or chronological claim. No schema/writer/API/AI change.
- [x] Final cmd.exe /d /c ".\mvnw.cmd -q -Dtest=TradeRecordCombinerTests,OptionLegacyServiceTests,EquityLegacyServiceTests,PaperTradingTests,FreshSchemaTests test > target\core05b2-trade-combiner-final.log 2>&1" returned Maven exit0: 47 tests/0fail/0error/0skipped. Evidence target/core05b2-trade-combiner-verification.json. Eight combiner tests cover namespace collision, immutable groups, unmatched ordering, missing/unverified evidence, shared targets, conflicting proofs,1000/1001 and out-of-input conflicts. Initial run also passed; review closed an uncovered boundary case then reran final sources. No remaining failures.
- [x] Checklist/overview/semantics synchronized; earlier scoped git diff --check exit0; final inline Node checklist/overview status and local-link validation PASS; scoped git diff --check exit0. No learner-status/deployment changes; unrelated work retained.
- [~] CORE-05-B2 partial. NEXT design/implement owned bounded batch snapshot adapter for combined records/proofs, without per-fill REQUIRES_NEW service calls or hiding legacy-only/unresolved records. This pure unit does not implement an actual combined DB report. New adapter concurrency/restart/restore and full period/account/API/UI/export work remain pending. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.


## 2026-11-24 combined trade proof boundary refinement

- [x] Initial focused/affected run exit0 (target/core05b2-trade-combiner.log).
- [~] Review refinement: count all valid matched proofs before checking input presence, so an out-of-input fill competing for a visible legacy identity blocks collapsing that legacy row. Added explicit regression test retaining both visible records. No observed initial test failure; this closes an uncovered edge case. NEXT final affected suite rerun; pending. No database/auth/writer/API change.


- [x] Checklist/semantics synchronized: pure grouping under test, namespace/coverage/immutability bounds explicit; owned batch adapter and database resilience not implemented. NEXT inspect tests and review edge cases before acceptance.


- [~] Pure combined evidence DTO/combiner and seven tests written: namespace-safe identities, verified one-to-one grouping only, unmatched/uncertain retention, conflicting proofs, immutable groups and1000 limits. No DB/auth/financial calculation or report endpoint; caller must supply owned same-snapshot evidence. NEXT focused and affected overlap/writer/schema tests; pending. Future batch adapter and real-db resilience remain unfinished.


## 2026-11-24 CORE-05-B2 combined trade evidence rules (active)

- [~] Resumed next bounded unit: pure combiner for supplied owned-record identities and existing equity/option overlap results. Namespace+ID keys; collapse only present one-to-one verified pairs, retain unmatched/conflicting/missing evidence explicitly. Max1000 records/proofs; immutable groups, no money totals or database/auth claim. Future owned batch adapter must use one snapshot without per-fill REQUIRES_NEW calls. NEXT implement/test pure rules; complete combined DB report still pending. No writer/schema/API/AI change.


## 2026-11-24 CORE-05-B2 option overlap reader resilience verified

- [x] Extended only SettlementJournalMySqlTests: test-only option_orders schema and eighth paused reader. Equity/option IDs both100 route independently. Concurrent option price change leaves old MATCHED snapshot, next read MISMATCH without identity; repaired DTO equal after restart; deleted order INCOMPLETE; dump restoration equal; foreign-user access denied. Prior seven readers retained. No production code/schema/writer/API/auth changes.
- [x] Process-local V3_JOURNAL_MYSQL=true restored in finally. cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,OptionLegacyServiceTests,EquityLegacyServiceTests,PaperTradingTests,IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,TicketFeeServiceTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test > target\core05b2-option-legacy-resilience.log 2>&1" returned Maven exit0. Surefire116 tests/0fail/0error/0skipped; evidence target/core05b2-option-legacy-resilience-verification.json. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after test-owned cleanup. No remaining failures.
- [x] Checklist/overview/semantics synchronized; earlier scoped git diff --check exit0; final inline Node checklist/overview status and local-link validation PASS; scoped git diff --check exit0. No AI/learner-status/deployment gate changes; unrelated work retained and index/scan limitation remains explicit.
- [~] CORE-05-B2 partial. NEXT design bounded combined trade evidence using owned fills plus matched EQUITY/OPTION legacy identities; retain namespace with ID and unavailable/legacy coverage rather than silently dropping records. Avoid nested REQUIRES_NEW per-fill reads or mixed snapshots. Complete period/account reconciliation and reporting API/UI/exports remain unfinished. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.


- [x] Checklist synchronized: option reader resilience running; no completion claim yet. NEXT inspect Maven/Surefire and temporary container cleanup. No AI/learner/deployment changes.


- [~] Test extension written: eighth reader with option-fill latch; identical equity/option ID100 routes independently, old option snapshot matches while new price-conflict snapshot rejects. Repaired pair matches after restart; deleted option order INCOMPLETE; dump restore equality and other-user denial. NEXT enabled MySQL and affected suites; results pending. Only synthetic test schema/data changed, no production changes.


## 2026-11-24 CORE-05-B2 option overlap reader resilience (active)

- [~] Resumed first unfinished task: isolated MySQL option reader snapshot/restart/dump restore. Seed option and equity orders with same textual ID100 in separate namespaces; pause owned option-fill read, commit option price conflict, old read must match/new read reject. Repair synthetic record before backup; restart/missing-order/restore/foreign-user checks. Test-only option_orders schema; no production schema/writer/API change. NEXT implement/run affected suites; pending.


## 2026-11-24 CORE-05-B2 option overlap reader implemented/tested

- [x] Added OptionLegacyEvidence DTO, constructor-injected repository/service and fixture beans. Active server identity, owned fill/ticket and option order; routes only CASH/no-stock option namespace, never numeric equity lookup. Validates contract key/expiry, BUY-SELL, units/lot divisibility, exact price and FILLED/MARKET status. Bounded2 reverse refs suppress ambiguity; missing/conflicting evidence withholds identity. Returned identity is owned order's stored primary key. Read-only REQUIRES_NEW/REPEATABLE_READ; no writer/schema/API/auth/AI change. Scan/index limitation retained.
- [x] Initial affected command -Dtest=OptionLegacyServiceTests,EquityLegacyServiceTests,PaperTradingTests,IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,TicketFeeServiceTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test exit0 (115 tests/0fail/0error/0skip; target/core05b2-option-legacy.log). After stored-ID mapping refinement, final cmd.exe /d /c ".\mvnw.cmd -q -Dtest=OptionLegacyServiceTests,PaperTradingTests,FreshSchemaTests test > target\core05b2-option-legacy-final.log 2>&1" exit0 (34 tests/0fail/0error/0skip). Evidence target/core05b2-option-legacy-verification.json. Five option tests cover ownership/metadata/namespace/ambiguity/snapshot, including numeric-looking option ID with no equity table. Actual option iceberg writer test validates both generated fill/order links. No remaining failures.
- [x] Checklist/overview/semantics synchronized; earlier scoped git diff --check exit0; final inline Node checklist/overview status and local-link validation PASS; scoped git diff --check exit0. No AI/learner-status or deployment gate changes; unrelated work retained.
- [~] CORE-05-B2 partial. NEXT extend isolated MySQL concurrent snapshot/restart/dump-restore to option overlap reader (test option_orders schema), then design bounded combined trade records using matched equity/option links without double counting, retaining unknown/legacy coverage. Complete period/account reconciliation and reporting API/UI/exports remain unfinished. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.


## 2026-11-24 option overlap final ID refinement

- [x] Initial full affected command exit0 (target/core05b2-option-legacy.log), including actual option iceberg links.
- [~] Review refinement: return the owned option order's stored primary key, rather than echoing the fill's reference string, as the dedup identity. No financial behavior/schema changed. NEXT final focused OptionLegacyServiceTests,PaperTradingTests,FreshSchemaTests rerun; pending. Other affected regressions passed before this isolated mapping change. MySQL resilience remains next.


- [x] Checklist/report semantics synchronized with option-only reader under verification and contract/lot/namespace limits; MySQL resilience/report integration still unfinished. NEXT inspect affected test results; no AI/learner/deployment changes.


- [~] Option overlap implementation/test unit written: DTO, repository/service, five focused tests, shared fixture beans and actual option iceberg integration. Validates namespace/owner/contract/expiry/lot/units/side/price/status, max2 references, same read-only snapshot. No production schema/writer/API change. NEXT affected Maven suites; pending. New reader MySQL resilience and combined report dedup remain unfinished.


## 2026-11-24 CORE-05-B2 option legacy overlap (active)

- [~] Resumed option namespace unit. CASH tickets without stock_id use option_orders string IDs, never numeric equity lookup. New internal reader compares owned fill/ticket/order contract key, expiry, side, lot size, units, executed price and FILLED/MARKET status; bounded reverse option references suppress ambiguity. No price/contract inference or fees/P&L summation; index scan limitation persists. NEXT implementation/focused and actual option-writer tests; no schema/writer/API/AI change.


## 2026-11-24 CORE-05-B2 equity legacy reader resilience verified

- [x] Extended only SettlementJournalMySqlTests: test-only orders schema, CASH equity pair and seventh paused reader. Concurrent legacy price/value update leaves old read MATCHED_DUPLICATE while next read becomes MISMATCH with no identity. Repaired synthetic pair yields equal DTO after restart and dump restore; missing order gives INCOMPLETE before restoration. Other-user access denied. Existing six reader checks retained. No production code/schema/writer/API/auth change.
- [x] Process-local V3_JOURNAL_MYSQL=true restored in finally. cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,EquityLegacyServiceTests,PaperTradingTests,IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,TicketFeeServiceTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test > target\core05b2-equity-legacy-resilience.log 2>&1" returned Maven exit0. Surefire111 tests/0fail/0error/0skipped. Evidence target/core05b2-equity-legacy-resilience-verification.json. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after test-owned cleanup. No remaining failures.
- [x] Checklist/overview/semantics synchronized; earlier scoped git diff --check exit0; final inline Node checklist/overview status and local-link validation PASS; scoped git diff --check exit0. No AI/learner-status or deployment/release gate changes. Existing legacy-link scan/index limitation retained; unrelated working-tree changes preserved.
- [~] CORE-05-B2 partial. NEXT review option_orders identity/contract/unit/price semantics and add owned option overlap evidence separately from numeric equity order IDs. Complete combined report deduplication, period/account reconciliation and reporting API/UI/exports remain unfinished. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.


- [x] Simple checklist synchronized with equity overlap resilience running; no completion claimed. NEXT inspect final Maven/Surefire and test container cleanup. No runtime/schema/AI/deployment changes.


- [~] MySQL extension written: seventh reader with owned-fill latch; concurrent legacy price/value conflict, old MATCHED vs new MISMATCH; repaired pair restart equality, deleted order INCOMPLETE, dump restore equality and foreign-user denial. Synthetic orders table exists only in isolated test. NEXT enabled MySQL plus affected suites; result pending. No production code changes.


## 2026-11-24 CORE-05-B2 equity legacy reader resilience (active)

- [~] Resumed first unfinished task. Extend isolated MySQL fixture with synthetic orders table and equity fill/order pair; pause after owned-fill read while another connection changes legacy price/value. Old snapshot must match; next read must reject conflict. Repair synthetic values before dump, verify restart equality, delete legacy order then restore. No production schema/writer/API change. NEXT implement and run opt-in MySQL/affected suites; results pending.


## 2026-11-24 CORE-05-B2 equity legacy overlap reader tested

- [x] Added EquityLegacyEvidence DTO, constructor-injected repository/service. Server identity/active user, owned fill+ticket and owned COMPLETED equity order; canonical positive numeric ID, stock/side/units/price/value comparison, bounded reverse-reference ambiguity detection. MATCHED_DUPLICATE identifies two representations of one trade only; no deletion/fee/P&L/report mutation. Options/non-CASH NOT_EVALUATED; missing/conflicting/ambiguous links suppress duplicate identity. Read-only REQUIRES_NEW/REPEATABLE_READ snapshot. No production writer/schema/API/auth change.
- [x] cmd.exe /d /c ".\mvnw.cmd -q -Dtest=EquityLegacyServiceTests,PaperTradingTests,IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,TicketFeeServiceTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test > target\core05b2-equity-legacy.log 2>&1" returned Maven exit0. Surefire110 tests/0fail/0error/0skipped; evidence target/core05b2-equity-legacy-verification.json. Five focused tests cover exact duplicate/no mutation, foreign/inactive access, metadata/status conflicts, namespace/malformed IDs, ambiguity and separate snapshot. Actual PaperTrading iceberg test verifies generated equity fill matches legacy order with real JPA identity. No remaining failures.
- [x] Checklist/overview/semantics synchronized; earlier scoped git diff --check exit0; final inline Node checklist/overview status and local-link validation PASS; scoped git diff --check exit0. Existing lack of legacy-link index documented: result bound does not certify scan cost. No AI/learner/deployment changes; unrelated work retained.
- [~] CORE-05-B2 partial. NEXT extend isolated MySQL snapshot/restart/dump-restore to this reader (including test orders schema), then option namespace overlap and eventual combined report dedup/period reconciliation. No whole-account historical completeness or deduplicated report has been certified. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.


- [x] Checklist/semantics synchronized: equity-only overlap check under verification, options and full historical deduplication pending; reverse-link index/scan limitation explicit. NEXT inspect Maven final results. No AI/learning/deployment change.


- [~] Equity overlap reader/test unit written: DTO, bounded repository/service, five ownership/metadata/namespace/ambiguity/snapshot tests, actual PaperTrading iceberg integration assertion. Shared fixture registers beans; orders minimal synthetic table exists only in new focused test. Numeric equity IDs parsed before SQL, foreign legacy IDs never returned, options/non-CASH NOT_EVALUATED. NEXT affected Maven tests; results pending. No production writer/schema/API change.


## 2026-11-24 CORE-05-B2 equity legacy overlap (active)

- [~] Reviewed PaperTrading CASH writer: equity legacy_order_id is numeric orders.id; option legacy IDs belong to separate option_orders namespace. First bounded unit: one owned equity fill->owned legacy order evidence, metadata/exact value comparison and at-most2 equity references to detect ambiguous duplicates. Option/non-CASH links NOT_EVALUATED, not guessed. Existing legacy-link index absent; LIMIT bounds results, not scan. No schema/writer/API change. NEXT implement/test equity reader; option namespace/global report dedup/resilience remain pending.


## 2026-11-24 CORE-05-B2 intraday fee links resilience verified

- [x] Prior linkage implementation83/83 passed (long fees0.61 and short0.57) and is now additionally tested on isolated MySQL. Extended only SettlementJournalMySqlTests: six readers, both intraday snapshots paused before concurrent partial-close/event/order/fill commit; old linked fees0.30 vs new0.41. Foreign-user access denied. Exact linked DTO equality after restart and dump restore; deleting close-fill produces INCOMPLETE before restore. No production writer/schema/API/auth/config change.
- [x] Process-local V3_JOURNAL_MYSQL=true restored in finally. cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,TicketFeeServiceTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test > target\core05b2-intraday-links-resilience.log 2>&1" returned Maven exit0. Surefire84 tests/0fail/0error/0skipped; evidence target/core05b2-intraday-links-resilience-verification.json. docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after test-owned cleanup. No remaining failures.
- [x] Checklist done/current/next simplified and synchronized with overview/semantics; historical detail retained here. Earlier scoped git diff --check exit0; final inline Node status/local-link validation PASS; scoped git diff --check exit0. No AI/learner-status change or deployment gate promotion. Unrelated work retained.
- [~] CORE-05-B2 remains partial. NEXT review legacy paper_fills.legacy_order_id and orders linkage, then bounded owned duplicate-trade evidence without changing financial writers or inventing complete historical coverage. Complete account/period reconciliation and reporting API/UI/exports remain unfinished. Event-linked fee matching does not certify unrelated unrepresented tickets or whole-account expenses. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.


- [x] Checklist/report semantics synchronized: prior linkage checks83/83 passed, new linkage resilience currently running. Current milestone does not certify complete position/account fees or legacy/period reporting. NEXT inspect final reports and cleanup, then mark only evidence-backed completion.


- [~] Linkage resilience test written: six parallel readers; intraday snapshot latch waits for both gross and linked-fee reads. Initial matching event/order/fill0.30, concurrent close adds0.11, old linked result0.30 vs new0.41. Restart DTO equality, deleted close-fill gives INCOMPLETE, dump restore returns identical matched result. NEXT opt-in MySQL plus affected suites; pending. Only test code changed; no production writer/schema/API change.


## 2026-11-24 intraday fee links implemented/tested; resilience next

- [x] Prior final command -Dtest=IntradayFeeServiceTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,TicketFeeServiceTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test exit0 (target/core05b2-intraday-fee-links-final.log). Reports83 tests/0fail/0error/0skipped. Actual long fee0.61 and short fee0.57 agree across event/order/fill records; gross P&L stays separate. Six new fee tests cover missing/corrupt links, privacy/metadata, duplicate/bounded rows and snapshot. Internal event-linked coverage only; no whole-account expense claim.
- [~] NEXT add linkage service to isolated MySQL concurrent snapshot/restart/dump-restore verification. No production code change planned. Broader legacy duplicate/period statement/API/UI/export work remains unfinished. Deployment paused; owner dependencies only in DEPLOYMENT_CHECKLIST.md.


## 2026-11-24 intraday fee links final verification checkpoint

- [x] Initial affected Maven run exit0 (target/core05b2-intraday-fee-links.log), including actual long partial-loss/final-profit links0.61.
- [~] Added actual short OPEN SELL/CLOSE BUY integration to cover reversed side predicate: gross100 and linked fees0.57, no turnover-as-profit inference. NEXT rerun affected suites on final tests; result pending. No production edits since initial pass. Linkage MySQL resilience and broader report coverage remain pending.


- [x] Simple checklist/semantics synchronized: event-linked fee verification implemented under test; event/order/fill overlap, same-snapshot joins, metadata/ownership bounds and limited coverage documented. Final Maven verification pending; linkage-specific MySQL resilience remains next. No application screen/AI/learner/deployment changes.


- [~] Linkage/test unit written: IntradayFeeEvidence DTO, repository/service, shared fixture wiring, six corruption/bound/ownership/snapshot tests; actual IntradayTrading test now checks linked fee0.61 once. Three owned bounded SELECTs in one snapshot; event-key ticket lookup validates MIS/stock/side/position/coverage and unique leg0 fill. No source mutation, new schema/API or live AI. NEXT affected Maven tests; results pending. New linkage MySQL resilience remains separate, not covered by prior reader evidence.


## 2026-11-24 CORE-05-B2 intraday event/order/fill fee linkage (active)

- [~] Resumed next unfinished task. Add separate internal owned event-fee evidence reader: same REQUIRES_NEW/REPEATABLE_READ snapshot for position, events and bounded event->ticket->fill joins. Verify ticket ownership/position/product/stock/side/filled quantities and single MIS fill, exact event/order/fill fee agreement. Max1000 events/join rows plus sentinel; no per-event SQL or nested TicketFeeService transaction. Verified fee scoped only to linked events, not all historical/period expenses. NEXT implement/tests; no writer/schema/API/AI change.


## 2026-11-24 CORE-05-B2 owned intraday reader resilience verified

- [x] Extended only SettlementJournalMySqlTests: fifth concurrent reader paused after owned position lookup, separate partial-close commit, old MATCHED gross0/fees0.30 vs subsequent gross-40/fees0.41. Foreign-event exclusion and other-user denial tested. DTO equality holds after MySQL restart and after event deletion/position reset followed by dump restoration; collateral300 restored. Existing journal/funding/finance/ticket-fee checks retained. No production code/writer/API/schema/auth/config change.
- [x] Process-local V3_JOURNAL_MYSQL=true restored in finally. cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,TicketFeeServiceTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test > target\core05b2-intraday-resilience.log 2>&1" returned Maven exit0. Surefire77 tests/0fail/0error/0skipped. Evidence target/core05b2-intraday-resilience-verification.json. Docker29.9.3/local mysql:8.4; docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after test-owned cleanup. No remaining failures.
- [x] Simple checklist/overview/report semantics synchronized; earlier scoped git diff --check exit0; final inline Node checklist status/local-link validation PASS; scoped git diff --check exit0. No AI/learner-status change or deployment/release gate promotion. Existing unrelated working-tree changes preserved.
- [~] CORE-05-B2 partial. NEXT same-snapshot event/ticket/fill fee linkage for owned intraday positions, without adding overlapping event/order/fill charges. Retain missing/foreign/corrupt/unbounded evidence suppression and existing index-performance limitation. Legacy duplicate records, full period reporting and public API/UI/exports remain unfinished. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.


- [x] Simple checklist synchronized: new intraday reader resilience tests are running, not marked complete; related order-fee linkage remains unfinished. NEXT inspect final Maven/Surefire and synthetic-container cleanup. No runtime/schema/AI/deployment changes.


- [~] MySQL test extension written: fifth paused reader, OPEN-to-partial-CLOSE consistent snapshot, foreign event exclusion/other-user denial, post-commit gross-40 and event fees0.41, restart equality, missing-event incomplete result and restored DTO/collateral equality. No production code changes. NEXT explicitly enabled MySQL plus owned reader/pure/writer/reporting/fresh-schema suites; results pending.


## 2026-11-24 CORE-05-B2 owned intraday reader resilience (active)

- [~] Resumed first unfinished checkpoint: extend isolated synthetic MySQL test to new intraday reader. Initial OPEN quantity10/collateral500/P&L0, pause owned-position read, commit partial CLOSE4/collateral-200/P&L-40 on another connection; old snapshot must remain0 and next read-40. Then exact restart/dump-restore equality and privacy checks. No production/writer/schema/API change. NEXT implement extension and run opt-in test plus affected suites; results pending.


## 2026-11-24 CORE-05-B2 owned intraday reader implemented/tested

- [x] Added immutable IntradayPositionEvidence DTO, IntradayEvidenceRepository/Service. Active server identity and owned open/closed position lookup; event predicate includes user+position; at most1001 rows (above1000 suppresses totals) in a separate read-only REQUIRES_NEW/REPEATABLE_READ snapshot. Returns only owned aggregate comparison. No endpoint/init/writer/schema/auth-policy change or AI use. Event fees remain evidence sums, not ticket-reconciled/net expenses.
- [x] Final cmd.exe /d /c ".\mvnw.cmd -q -Dtest=IntradayEvidenceServiceTests,IntradayEvidenceReconcilerTests,IntradayTradingTests,TicketFeeServiceTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test > target\core05b2-intraday-reader.log 2>&1" returned Maven exit0: 76 tests/0fail/0error/0skipped. Evidence target/core05b2-intraday-reader-verification.json. Six adapter tests verify owner/foreign/suspended handling, corrupt foreign event exclusion, open/closed positions, no-write/no-init, unavailable conflicts/unknowns, exact money,1000/1001 and separate snapshot. Actual IntradayTrading test verifies owned reader equals pure gross20/event-fees0.61 result with real JPA identity and saved writer records. Existing reporting/fresh-schema tests pass. No remaining failures.
- [x] Shared fixture uses existing intraday migration; no production migration/index. Documented user/time-index limitation: bounded returned rows do not certify bounded scan or per-position performance. Checklist/overview/semantics synchronized; scoped git diff --check exit0. Final inline Node checklist status/local-link validation PASS. No AI/learning/deployment gate changes; unrelated work retained.
- [~] CORE-05-B2 partial. NEXT extend isolated MySQL concurrent snapshot/restart/restore test for this reader, then same-snapshot event/ticket/fill fee linkage. Existing-reader MySQL evidence does not cover the new adapter. Legacy deduplication, complete period reporting/API/UI/export remain unfinished. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.


- [x] Checklist/report semantics synchronized with owned reader written/under test, fee linkage and new-reader database resilience still pending. Index limitation documented: existing user/time index, bounded returned rows do not guarantee bounded per-position scan. NEXT inspect Maven final result; no personal learning/AI/deployment changes.


- [~] Adapter/test unit written: IntradayPositionEvidence DTO, repository/service and six reader tests; existing actual IntradayTrading test now calls new owned reader and compares results. Shared fixture loads existing intraday migration/beans. Tests cover closed/open records, privacy/corrupt foreign links, incomplete/conflicting/unknown evidence, exact money,1000/1001 bound, no-write/no-init and separate snapshot. NEXT affected Maven suites; results pending. No production migration/index/writer/API change.


## 2026-11-24 CORE-05-B2 owned intraday reader (active)

- [~] Resumed first unfinished task: internal owned position/event adapter over tested pure reconciler. Parameterized user+position queries, max1000 events plus sentinel, constructor injection, same read-only REQUIRES_NEW/REPEATABLE_READ snapshot. Existing event index is(user_id,occurred_at), not position-specific; bounded result does not guarantee bounded scan. No index/schema change proposed in this unit; document limitation. NEXT implement/test adapter; linked ticket fees and MySQL reader resilience remain pending. No account writer/API/AI change.


## 2026-11-24 CORE-05-B2 first intraday reconciliation rules tested

- [x] Added pure IntradayEvidenceReconciler with immutable supplied Position/Event/Result records. One OPEN plus CLOSE events must match remaining units, collateral and signed gross realized P&L; closed positions retain zero collateral. Max1000 events, duplicate/invalid/unknown/missing/truncated evidence suppresses numeric results. EventFeeSum is explicitly not ticket-reconciled/net/account P&L. No database reader/authorization claim, price/margin recalculation, financial writer, schema, API or AI change.
- [x] Final cmd.exe /d /c ".\mvnw.cmd -q -Dtest=IntradayEvidenceReconcilerTests,IntradayTradingTests,TicketFeeServiceTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test > target\core05b2-intraday-final.log 2>&1" returned Maven exit0. Surefire70 tests/0fail/0error/0skipped. Evidence target/core05b2-intraday-verification.json. Seven pure tests cover partial/full exits, signed short gains/large losses, missing/unknown/truncation, quantity/collateral/P&L conflicts, duplicate/invalid events, exact large amounts and1000 boundary. New actual IntradayTrading test verifies loss40+gain60=gross20 separately from event fees0.61 and preserves balance/position. Initial run also exit0. No remaining failures.
- [x] Checklist/overview/report semantics synchronized; earlier scoped git diff --check exit0. Final inline Node checklist status/local-link validation PASS; scoped git diff --check exit0. No personal learning status or deployment/release gate changes; unrelated working-tree changes preserved.
- [~] CORE-05-B2 partial. NEXT owned intraday position/event database adapter with bounded evidence and same snapshot, then ticket/fill fee linkage; assess existing index coverage without silently adding schema. New adapter MySQL concurrent snapshot/restart/restore remains required after implementation. Legacy duplicates, complete period reports and APIs/screens/exports remain unfinished. Owner dependencies only in DEPLOYMENT_CHECKLIST.md.


## 2026-11-24 intraday actual-writer verification checkpoint

- [x] Initial affected Maven run returned exit0 (target/core05b2-intraday-classification.log).
- [~] Added actual IntradayTrading event comparison test: OPEN, partial loss40, final gain60 produces gross20 separately from fees0.61; comparison preserves balance/position. Strengthened large exact fee assertion. NEXT rerun same affected suites on final sources; results pending. Pure unit remains separate from future owned source adapter/ticket fee linkage and database resilience.


- [x] Simple checklist/semantics synchronized: first intraday rules written under verification; owned source reads, fee/ticket linkage and database resilience explicitly unfinished. No runtime account/AI changes. NEXT inspect Maven result before accepting pure interpretation unit.


- [~] Pure IntradayEvidenceReconciler and seven invariant tests written: signed gross P&L separate from fees, unit/collateral totals, one opening, bounded1000 evidence, unknown/missing/truncated suppression, duplicates/invalid amounts and closed collateral rejection. No DB access/ownership claim or verified net fee. NEXT affected Maven tests including actual IntradayTrading regressions; results pending. Owned DB/ticket-link adapter remains next.


## 2026-11-24 CORE-05-B2 intraday interpretation (active)

- [~] Reviewed IntradayTrading OPEN/CLOSE writer and migration: position holds quantity/collateral/gross realized_pnl, no fee aggregate; events store per-event fees and ticket-linked keys. First bounded unit is pure position/event reconciliation (no DB/API): one OPEN plus CLOSE unit/collateral/P&L sums against position; event fee sum is evidence only, not whole-account/net P&L or independently verified fee total. Max1000 events; missing/unknown/invalid evidence withholds verified results. NEXT implement focused invariants tests; DB ownership adapter/ticket fee linkage and MySQL resilience remain next, not certified by pure tests.


## 2026-11-24 CORE-05-B2 ticket fee resilience verified

- [x] Extended only SettlementJournalMySqlTests: fourth concurrent reader pauses after ticket lookup; another transaction adds fill3 to existing fill2 and updates ticket total/counts. Paused report stays MATCHED2 while next read returns MATCHED5. Cross-user ticket access rejected. Exact DTO equality holds after MySQL restart and after deleting fill evidence/resetting fee then restoring dump. Existing journal/funding/finance reader checks retained. No production writer/schema/API/auth/config change.
- [x] Process-local V3_JOURNAL_MYSQL=true restored to previous value in finally. Final cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,TicketFeeServiceTests,PaperTradingTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test > target\core05b2-fee-resilience.log 2>&1" returned Maven exit0. Surefire63 tests/0fail/0error/0skipped: MySQL1, fee7, trading22, finance5, classifier5, funding9, journal7, fresh-schema7. Evidence target/core05b2-fee-resilience-verification.json. Docker29.9.3/local mysql:8.4; docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows after label/name-checked cleanup. No test failures.
- [x] Simple checklist, overview and report semantics synchronized; no AI or learner-status change, deployment remains paused and old release gates unchanged. Earlier scoped git diff --check exit0; final inline Node status/local-link validation PASS and scoped git diff --check exit0.
- [~] CORE-05-B2 remains partial. NEXT review existing intraday event/position fee and realized-value writer semantics, then implement bounded owned reconciliation without adding event/detail values to their overlapping aggregates. Legacy fill/order deduplication and full period/statement/API/UI/export work remain unfinished. Open owner dependencies remain exclusively in DEPLOYMENT_CHECKLIST.md.


- [x] Simple checklist synchronized: fee reader resilience is in progress, not complete. NEXT inspect explicitly enabled MySQL/regression result and cleanup. Runtime/schema/auth/writer behavior unchanged.


- [~] Test extension written: four parallel readers, fee snapshot paused after parent SELECT, concurrent second-fill/fee commit, old matched2 vs new matched5; other-user denial, restart DTO equality and deliberate fill/fee removal followed by dump restore. Docker29.9.3 available. NEXT explicitly enabled MySQL plus affected reporting/PaperTrading/fresh-schema tests; results pending. No production code changed.


## 2026-11-24 CORE-05-B2 ticket fee resilience (active)

- [~] Resumed first unfinished task: add TicketFeeService to existing isolated MySQL test. Plan: begin with one filled leg/fee2, pause after ticket read, commit another leg and total5 on a separate connection; verify old snapshot2 then new snapshot5, restart equality and dump-restore equality. No production code/schema/API/money writer changes. Reviewed handoff, queue, canonical deployment checklist and simple checklist. NEXT implement test extension; results pending.


## 2026-11-24 CORE-05-B2 ticket fee evidence implemented/tested

- [x] Added immutable TicketFeeEvidence DTO, owned TicketFeeRepository and TicketFeeService. Internal per-ticket comparison validates exact parent fee vs fill-fee sum and filled count/quantity coverage, not parent plus children. Active server identity, read-only REQUIRES_NEW/REPEATABLE_READ, constructor injection and1000-row+sentinel bound. Unknown/incomplete/corrupt evidence withholds reconciled amount; foreign child rows never exposed. No financial writer/schema/API/auth/config change.
- [x] Final cmd.exe /d /c ".\mvnw.cmd -q -Dtest=TicketFeeServiceTests,PaperTradingTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test > target\core05b2-fees-final-rerun.log 2>&1" returned Maven exit0. Surefire62 tests/0fail/0error/0skipped. Evidence target/core05b2-fees-verification.json. Seven fee tests: exact2+3=5/no writes, missing/conflicting fees, ownership/suspension/corrupt child, zero/cancelled/large fees,1000/1001 bound, identity/snapshot, no-fill zero vs extra coverage. Actual PaperTrading integration: limit iceberg explicitly processed, partial0.15 then completed/retried0.27 reconciles once and reads preserve balance. Earlier expanded run had1 new test setup failure (LIMIT not processed); corrected sequence and reran final stable sources. No remaining failures.
- [x] Shared synthetic fixture and existing PaperTradingTests updated; checklist/overview/report semantics synchronized. Scoped git diff --check exit0; final inline Node checklist status/local-link validation PASS. No AI or personal learning changes. Unrelated working-tree changes retained; deployment paused and release gates unchanged.
- [~] CORE-05-B2 remains partial. NEXT add new TicketFeeService to isolated MySQL concurrent snapshot/restart/dump-restore test; previous finance reader evidence does not certify this reader. Then reconcile intraday fees/realized values, legacy fill/order overlap and complete period/expense coverage before reporting APIs/screens/exports. Owner dependencies remain exclusively in DEPLOYMENT_CHECKLIST.md.


- [x] Overview synchronized with fee comparison under final verification; no completion claim for full reporting or new-reader resilience. Scoped git diff --check exit0. Corrected actual-writer test rerun remains pending. NEXT confirm Maven results, then finalize evidence/checklist and queue new fee-reader MySQL checks.


- [~] Final expanded run executed62 tests with1 failure/0errors: new actual-writer test expected0.15 before processing a queued LIMIT ticket; reader correctly returned matched0.00 for unfilled ticket. Corrected test to call existing paper.process(id) before partial-fill assertion, preserving writer/fee policy. NEXT rerun same expanded command on final sources; acceptance pending.


- [x] Documentation unit: report semantics specifies ticket-local overlapping fees, coverage/ownership/null limits and bounded evidence. Simple checklist now shows fee comparison written/under verification, without completing overall reporting. Final PaperTrading integration run pending. NEXT inspect final results, then update overview/checklist with verified status and remaining MySQL/legacy/period scope.


## 2026-11-24 CORE-05-B2 fee reader actual-writer verification

- [x] Initial focused Maven command -Dtest=TicketFeeServiceTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test exit0 (target/core05b2-fees-tests.log).
- [~] Added actual PaperTrading iceberg writer integration: partial fee0.15, completed/retried fee0.27 reconciled once; reader preserves balance. Added no-fill pending zero vs corrupt extra detail test. NEXT rerun affected suites plus PaperTradingTests on final sources; results pending. TicketFee reader MySQL restart/restore and broader legacy/period expense reconciliation remain unfinished.


- [~] Implementation unit written: TicketFeeEvidence DTO, constructor-injected TicketFeeRepository/Service, shared fixture wiring and six focused tests. Owned ticket then bounded detail SELECT within separate snapshot; returns MATCHED/MISMATCH/INCOMPLETE, reconciled fee only for exact agreement; foreign detail suppressed. Tests cover2+3=5, no writes, missing/conflicting fees, foreign/inactive users, zero/cancelled/large fees,1000/1001 bound, validation/snapshot. NEXT affected Maven tests; result pending. Checklist current task remains partial.


## 2026-11-24 CORE-05-B2 ticket fee reconciliation (active)

- [~] Resumed next unfinished approved task. Reviewed paper ticket/fill writer: fills.fee and tickets.fees are overlapping detail/aggregate, not additive charges. Bounded internal owned-ticket reader will compare exact summed fill fees plus filled quantity/leg coverage against ticket values in one read-only REPEATABLE_READ snapshot. Max1000 detail rows plus sentinel; incomplete/corrupt/foreign evidence withholds reconciled fee. No account-wide expense claim, legacy dedup claim, API/writer/schema change. NEXT implement focused reader/tests; results pending.


## 2026-11-24 CORE-05-B2 reader database resilience verified

- [x] Extended existing test-only JournalReportFixture and SettlementJournalMySqlTests. Owned-position read pauses before a second connection commits interest; first event page remains pre-commit and next read sees2.50 interest. Deliberately foreign-user event excluded; other user cannot access owned position. Finance DTO equality verified after MySQL restart and after restoring dump; restored outstanding interest2.50 and virtual cash1234.57 checked. No production code/writer/schema/API change.
- [x] Final PowerShell process-local V3_JOURNAL_MYSQL=true (previous value restored in finally), cmd.exe /d /c ".\mvnw.cmd -q -Dtest=SettlementJournalMySqlTests,FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test > target\core05b2-resilience-tests.log 2>&1" returned Maven exit0. Surefire34 tests/0fail/0error/0skipped: MySQL1, reader5, classifier5, funding9, journal7, fresh-schema7. Evidence target/core05b2-resilience-verification.json. Local Docker29.9.3/mysql:8.4 reused; synthetic container label/name cleanup complete, docker ps -a --filter label=investsmart.scope=journal-synthetic --format '{{.Names}} {{.Status}}' returned no rows.
- [x] Checklist/overview/report semantics updated together; no AI or personal learning status change. Earlier scoped test-file git diff --check exit0. No remaining test failures. Final inline Node checklist status/local-link validation PASS; scoped git diff --check exit0.
- [~] CORE-05-B2 remains partial. NEXT owned fill fee-detail versus ticket aggregate reconciliation, retaining exact amounts and explicitly incomplete/unknown history; no addition of the same charge twice. Public statement/period reporting/API/UI/export remain unfinished. Deployment paused; no historical gate promoted. Owner dependencies remain only in DEPLOYMENT_CHECKLIST.md.


- [x] Simple checklist updated to show database resilience verification running, without marking it complete. Scoped test-file git diff --check exit0. Maven still running; NEXT inspect final Surefire results and synthetic-container cleanup before acceptance.


- [~] Test unit written: shared fixture exposes spy-instrumented finance reader; opt-in synthetic MySQL test now pauses after owned-position read, commits interest on another connection, checks pre-commit snapshot, cross-user event exclusion, restart equality and dump restoration of interest/events. Docker29.9.3 and local mysql:8.4 image available. NEXT run selected reporting/fresh-schema tests plus explicitly enabled MySQL test; result pending. Only test files changed; synthetic container cleanup retains label/unique-name checks.


## 2026-11-24 CORE-05-B2 database resilience checks (active)

- [~] Resumed first unfinished task from checklist/handoff: extend existing isolated synthetic MySQL snapshot/restart/dump-restore verification for FinanceEventService. Reviewed handoff, queue, canonical deployment checklist and existing fixture/test. No financial writer/API/schema change planned; no real accounts or deployment. Docker readiness check pending. NEXT add finance reader to existing concurrent test and restore assertions, then run focused checks; results pending.


## 2026-11-24 simple checklist final checkpoint

- [x] V3_SIMPLE_CHECKLIST.md delivered: completed/current/next work, future V3 and AI stages, glossary and priority-guidance examples in plain language. Overview remains short entry point and links checklist; roadmap links checklist without changing personal study status. AGENTS.md preserves announcement and ongoing checklist maintenance preferences.
- [x] Inline Node documentation validation PASS:13 local links across checklist/overview/roadmap resolve; required done/current/next/glossary and implementation-limit descriptions present. Evidence target/v3-simple-checklist-verification.json. Scoped git diff --check exit0. Application tests not run (documentation-only). No remaining documentation verification failures.
- [~] Development status unchanged: new finance reader implemented/tested33/33; CORE-05-B2 partial. NEXT isolated MySQL concurrent snapshot/restart/restore verification for new reader, then fee-detail/aggregate reconciliation. AI preview remains local/default-off, no live model; deployment paused. Owner dependencies remain exclusively in DEPLOYMENT_CHECKLIST.md.


## 2026-11-24 simple V3 progress checklist

- [x] Created V3_SIMPLE_CHECKLIST.md at the user's request: plain-language completed/current/next work, remaining product and AI stages, everyday glossary and how to guide priorities. Evidence references retained in this handoff; owner dependencies only linked to DEPLOYMENT_CHECKLIST.md. Current implementation remains CORE-05-B2 reader complete/tested, database resilience/fee reconciliation next; local AI preview separate from unconnected live AI.
- [x] Linked checklist from PROJECT_OVERVIEW.md and AI_LEARNING_ROADMAP.md; corrected overview's outdated local-preview/navigation description. AGENTS.md now requires milestone synchronization of the checklist. No personal learning status changed and no runtime code or deployment gate changed.
- [~] Verification pending: local links, status/content checks and scoped whitespace check. Application tests not run for documentation-only unit. NEXT verify docs, then resume CORE-05-B2 database snapshot/restart/restore checks and fee reconciliation.


## 2026-11-24 CORE-05-B2 finance reader implemented/tested

- [x] Added FinanceEventPage, FinanceEventRepository and FinanceEventService: owned per-position event page through server identity/active-user guard, explicit bounds and deterministic ordering, existing classifier meanings, immutable DTO and read-only REPEATABLE_READ/REQUIRES_NEW snapshot. Constructor injection; existing position/time index used. No API, account initializer, aggregate expense total, writer, schema/migration or AI change.
- [x] Final command: cmd.exe /d /c ".\mvnw.cmd -q -Dtest=FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test > target\core05b2-reader-final.log 2>&1"; Maven exit0, 33 tests/0fail/0error. Reports summarized in target/core05b2-reader-verification.json. Five reader tests cover ownership/corrupt links, closed positions, exact large amounts/unknown-invalid events, page bounds/ties, no-write preservation and separate read-only snapshot inside weaker transaction. Fresh-schema actual JPA identity test confirms accrued2.50 is not combined with outstanding9.00 and no settlement-account initialization. Existing classifier/funding/journal regressions pass. Initial run also exit0; no remaining failures. Scoped git diff --check exit0.
- [x] Updated semantics and short overview, checkpointed queue/handoff; saved user communication preference in AGENTS.md. AI roadmap reviewed: no new AI implementation or learning status change. Existing unrelated working-tree changes preserved. No deployment or historical gate promotion.
- [~] CORE-05-B2 remains partial. NEXT extend existing isolated MySQL verification for new reader concurrent-commit snapshot/restart/restore, then owned fill-fee detail versus ticket aggregate reconciliation. No lifetime completeness, period statement or reporting screen is implemented by this unit. Canonical owner dependencies stay only in DEPLOYMENT_CHECKLIST.md.


- [x] Documentation unit: reporting semantics now defines owned per-position reader, separate accrued/paid/contribution fields and incomplete coverage limits; overview reflects internal query progress. Final integration/regression run pending. NEXT inspect final Maven exit/reports; preserve broader B2 reconciliation and database resilience checks as unfinished.


## 2026-11-24 CORE-05-B2 reader integration checkpoint

- [x] Initial focused command cmd.exe /d /c .\mvnw.cmd -q -Dtest=FinanceEventServiceTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests,FreshSchemaTests test returned Maven exit0 (target/core05b2-reader-tests.log).
- [~] Added actual application/JPA user + shipping fresh-schema finance-reader integration test: saved accrued event2.50 remains separate from current interest9.00 and reads do not initialize settlement accounts. NEXT rerun affected suites on final sources; result pending. No production schema or money writer changed.


- [x] Saved the user's requested bold 'new work !!!!' announcement/simple-language preference in AGENTS.md for future continuation. AI roadmap reviewed; this reporting reader adds no AI capability, framework or learning-completion change. Current Maven verification still pending; NEXT inspect final result before acceptance.


- [~] Reader implementation/test unit written: FinanceEventPage/Repository/Service plus five focused tests; shared synthetic fixture loads existing paper schema and reader beans. Owned per-position/user SELECTs, stable ties, no aggregates/current-interest addition, null unknown classifications, bounded input and separate snapshot tested. NEXT cmd.exe Maven focused regression; results pending. No production schema/writer/API change.


## 2026-11-24 CORE-05-B2 owned finance-event reader (active)

- [~] Reviewed existing BORROW/INTEREST/REPAY writers and schema. First bounded unit: internal per-position owned event reader, deterministic pagination and existing classifier DTO; no totals, fees or account-wide reconciliation claimed. Position/user predicate uses existing position/time index. Constructor injection and owned read-only REQUIRES_NEW/REPEATABLE_READ snapshot; no writer/schema/API change. Tests pending. NEXT implement reader and focused ownership/precision/no-write/snapshot tests. User communication preference: announce each new work unit with bold 'new work !!!!' and a simple explanation.


## 2026-11-24 final checkpoint - AI-2 isolated browser unit complete

- [x] node scripts/verify-agent-browser.mjs PASS6 check groups; node --check scripts/verify-agent-browser.mjs exit0. Browser results/screenshots in target/agent-browser; desktop and corrected320px visually inspected. Added reusable verifier and fixture, plus scoped AgentConversation.css scrollbar fix; overview and roadmap synchronized. No personal learning statuses changed.
- [x] Final frontend cwd commands: node --test src/api/agentPresentation.test.mjs PASS4/4; node node_modules/eslint/bin/eslint.js . exit0; node node_modules/vite/bin/vite.js build exit0. Logs target/agent-browser/frontend-tests.log, lint.log, build.log. No remaining failures. Backend33/33 evidence from previous unit retained; not rerun because no Java/backend change.
- [~] AI-2 remains partial: isolated component evidence does not certify full app shell/authentication, full keyboard/accessibility acceptance, physical devices, live AI or whole-account analytics. Deployment paused; historical release gates unchanged. NEXT first unfinished CORE task: CORE-05-B2 owned fee/interest source adapter and detail/aggregate reconciliation, beginning with existing classifier/repository semantics review. Canonical owner dependencies remain only in DEPLOYMENT_CHECKLIST.md.


## 2026-11-24 AI-2 synthetic browser verification

- [x] Final node scripts/verify-agent-browser.mjs PASS six browser check groups. Evidence target/agent-browser/results.json and desktop-complete.png, missing-768.png, missing-400.png, missing-320.png. Actual React component mounted in installed headless Chromium using isolated loopback Vite and synthetic context/API adapter. Consent/no premature read, exact money/escaping, clear/cancel delayed response, failure/retry, stale/unavailable evidence, narrow layout and account-switch cleanup passed; no uncaught browser exceptions observed.
- [x] Inspected desktop and320px screenshots. Found and fixed scrollbar-induced320px overflow using body:has(.agent-conversation) minimum-width override; final assertion compares scrollWidth to clientWidth and passes768/400/320. Final320 screenshot confirms no horizontal scrollbar/clipped controls. No other pages' minimum-width policy changed.
- [~] NEXT finish frontend protocol tests/lint/build on final CSS; checks running. Broader full-app/authentication/keyboard accessibility/physical device acceptance is not certified by this isolated fixture. No backend/provider/schema/accounting changes; no external requests or owner prompts automated. After final checks, resume CORE-05-B2 owned fee/interest source adapter/reconciliation.


## 2026-11-24 AI-2 browser layout correction

- [~] First browser interaction run passed six groups, but screenshot review found320px horizontal overflow: global body min-width320 exceeds the scrollbar-reduced client width. Original innerWidth assertion was too weak. Scoped body minimum-width override to pages containing agent-conversation; strengthened verifier to compare document clientWidth. Desktop and320px screenshots inspected; NEXT rerun browser plus frontend tests/lint/build on final CSS. Results pending; no financial/backend change.


- [~] Initial browser run failed before launch: Windows ESM absolute import needed file URL. Corrected verifier to pathToFileURL; no application change. NEXT rerun same command; results pending.


- [~] Browser verifier and synthetic fixture written: scripts/verify-agent-browser.mjs and scripts/fixtures/agent-browser.jsx. Imports actual React component/styles with fixture-only API adapter; installed headless Chromium and loopback Vite; no new packages or runtime code changes. Checks consent, money/escaping, clear/cancel, failure/retry, stale data, narrow widths and account-switch late reply. NEXT node scripts/verify-agent-browser.mjs; result pending.


## 2026-11-24 AI-2 rendered browser checks (active)

- [~] Resumed synthetic rendered UI verification. Computer Use node_repl tool unavailable; installed Edge/Chrome supports an isolated headless browser check via local debugging protocol. Plan: local Vite fixture importing actual AgentConversation, synthetic API adapter and account context; no backend/authentication or real account data. I02/S02/T01 applies; no new runtime dependency/provider. NEXT implement bounded browser verifier; checks pending.


## 2026-11-24 AI-2 local interface automated verification complete

- [x] Final backend command: cmd.exe /d /c ".\mvnw.cmd -q -Dtest=AgentPortfolioApiTests,PortfolioAnalysisServiceTests,PortfolioReadToolTests,AgentFoundationTests,FreshSchemaTests test > target\ai2-ui-backend-final.log 2>&1"; Maven exit0. Surefire:33 tests/0fail/0error (API4, analysis6, read8, foundation9, fresh-schema6). Earlier PowerShell wrapper exit1 came from warning redirection; final native exit and reports establish success.
- [x] Frontend (frontend cwd): node --test src/api/agentPresentation.test.mjs PASS4/4 (target/ai2-ui-frontend-tests.log); node node_modules/eslint/bin/eslint.js . exit0; node node_modules/vite/bin/vite.js build exit0 (target/ai2-ui-lint-final.log, target/ai2-ui-build-final.log). npm.ps1 policy restriction avoided via existing native/node launchers without changing policy.
- [x] Local interface implemented with strict topic/consent, existing session/active-user/CSRF and quotas, default-off mock-only dev/test guard excluding prod. Opt-in Vite DEV screen has cancellation, clear, account-key remount, six temporary replies, timestamps, escaped text and exact decimal formatting. No external model, money writer, schema or auth-policy change. Scope evidence target/ai2-ui-scope.json:420 prior baseline files unchanged;11 changed baseline files are interface/DTO/tests/docs. Six new interface/API-test/frontend files were already present at resume; unrelated work preserved.
- [x] Overview/inventory and AI roadmap synchronized; no personal topic marked Completed. Deployment remains paused and historical release gates unchanged.
- [~] AI-2 overall remains partial. Automated markup/protocol checks do not certify rendered interaction, accessibility or physical devices. NEXT isolated synthetic rendered UI checks for consent, loading/error/cancel/clear/account switching, missing/stale evidence and narrow layout; then resume CORE-05-B2 owned fee/interest adapter/reconciliation. No current automated failures. Owner dependencies remain only in DEPLOYMENT_CHECKLIST.md.


- [x] Documentation unit: overview now describes implemented optional local preview; inventory labels earlier assessment historical; AI roadmap reviewed and I01/I02/S02/T01 change-log entry added. Personal topic statuses untouched. Final backend rerun remains pending; NEXT confirm exit/reports, scope-check and finalize evidence.


## 2026-11-24 AI-2 verification checkpoint

- [x] Focused frontend protocol/render-markup tests PASS4/4: node --test src/api/agentPresentation.test.mjs (frontend cwd), target/ai2-ui-frontend-tests.log. Direct node eslint and vite build both exit0, target/ai2-ui-lint-final.log and target/ai2-ui-build-final.log.
- [~] Backend reports show33 tests/0fail/0error across five selected suites; PowerShell warning redirection produced wrapper exit1. NEXT rerun through cmd.exe to capture Maven exit status reliably before acceptance. No runtime edits; rendered browser/device acceptance pending.


## 2026-11-24 AI-2 interface verification (active)

- [~] Resumed the first unfinished approved unit: existing local endpoint/view verification. Reviewed handoff, queue, deployment checklist and AI roadmap; unrelated working-tree changes preserved. No new runtime changes. NEXT focused backend tests plus frontend protocol tests/lint/build; results pending. Rendered UI acceptance remains unverified.


## 2026-11-23 AI-2 local interface (active)

- [~] Endpoint/view unit written: strict two-field topic/consent DTO parsing, existing session/active-user/CSRF policy and SecurityEvents limiter (10/user,30/source per existing 15-minute window). Default-off/local-mock guards; /agent route requires Vite DEV plus VITE_AGENT_PREVIEW=true. UI has three supported questions, six in-memory replies, cancel/clear/account-key remount, text-only rendering and 15s request timeout. New agent money DTO fields serialize as decimal strings and exact frontend formatting avoids Number precision loss. Tests added for guard/API/CSRF/consent/quota/escaping/money. NEXT backend and frontend checks; results pending. No financial writer/schema/auth configuration or production config changed.

- [x] Reviewed current status and recorded interface impact: additive local/mock-only read endpoint/view under existing session, CSRF and limiter; no auth policy/schema/writer change. Baseline target/ai2-ui-baseline.json captured.
- [~] NEXT implement strict consent/topic request, deterministic reply and opt-in React view with cancel/clear/account-switch cleanup; tests pending. No live provider, freeform agent loop or account action. Learning I01/I02/S02/T01 applies; personal progress unchanged.

## 2026-11-23 AI-2 analysis summaries (implemented/tested)

- [x] FINAL scope check `node target/ai2-summary-verify.cjs` PASS: two new Java files, nine authorized existing changes, all 420 other baseline files unchanged. Evidence target/ai2-summary-verification.json. No remaining test/scope failures; deployment and historical release gates unchanged. NEXT controlled local conversation endpoint/view per the recorded impact assessment; summaries are internal, not a completed user-facing agent.

- [x] AI-2 internal analysis summary unit implemented/tested: PortfolioAnalysisService delegates identity/consent to PortfolioReadTool, sums only complete equity unrealized P&L, merges duplicate listing rows for stable top-three weights and returns explicit incomplete/empty statuses. No invented risk threshold, daily change, causal story or whole-account performance.
- [x] `.\mvnw.cmd -q '-Dtest=PortfolioAnalysisServiceTests,PortfolioReadToolTests,AgentFoundationTests,FreshSchemaTests' test` PASS28/28/0fail/error (target/ai2-summary-tests.log). Six new focused tests plus read-tool/config/default-off fresh-schema checks passed. No current failures; no provider call/endpoint/UI/schema/writer change.
- [~] AI-2 remains partial. Next local conversation interface impact is documented in existing V3 plan/inventory: additive authenticated/CSRF-protected read endpoint, consent and request limits, feature-gated view, cancel/clear/account-switch handling and accessibility tests. It is not implemented or visually accepted. CORE-05-B2 and other V3 priorities preserved. Scope verification next; personal study statuses remain untouched.


- [~] Added PortfolioAnalysisService immutable results and deterministic explanations; same-listing rows merge for top-three concentration, missing costs keep P&L null, stale/missing/inconsistent valuation suppresses totals/ranking. Local mock bean only; identity/consent still delegated to original read tool. Six focused tests plus configuration/default-off assertions added. NEXT `.\mvnw.cmd -q '-Dtest=PortfolioAnalysisServiceTests,PortfolioReadToolTests,AgentFoundationTests,FreshSchemaTests' test`; verification pending. No endpoint/UI/model call or financial writer changed.

- [x] Resumed from verified read-only tool; reviewed current handoff/queue/checklist, feature flag and portfolio DTO/service semantics. Baseline captured in target/ai2-summary-baseline.json.
- [~] Next bounded unit: deterministic equity-only analysis facade and summaries, merging duplicate listing rows and withholding unsupported totals. No risk thresholds invented, no daily attribution/whole-account P&L claim, no provider call/API/UI or writer/schema change. Tests pending. Learning topics S01/T01/I01 explained; personal status untouched.

## 2026-11-23 AI-2 first read-only unit (implemented/tested)

- [x] First AI-2 backend unit implemented/tested: PortfolioReadTool accepts only server Principal + empty getMyPortfolio schema + explicit consent, uses existing PortfolioService, checks cached stock identity/prices/timestamps (60s age/5s future bound), and returns immutable EQUITY_HOLDINGS_ONLY evidence with existing holding unrealized P&L and deterministic equity weights. Incomplete valuation suppresses totals/weights rather than overstating known positions; no daily-return/causal claim.
- [x] Final `.\mvnw.cmd -q '-Dtest=PortfolioReadToolTests,AgentFoundationTests,FreshSchemaTests' test` PASS22/22/0fail/error (target/ai2-final-tests.log). Eight tool tests include cross-user argument denial, consent, suspension, freshness, missing metadata/costs, bounds and cash-vs-equity weights; fresh-schema test exercises real JPA/PortfolioService empty read without initialization. `node target/ai2-verify.cjs` PASS: 2 new Java files, 10 existing source/test/doc changes, 417 other baseline files unchanged; all 24 personal study rows remain Not Started.
- [~] AI-2 overall remains partial: broader account/P&L/risk summaries and conversation UI not done. No tool/model loop or model egress, endpoint, financial writer/schema/auth policy or deployment change. Registry has exactly one read-only tool only under explicit local mock flag/profile. NEXT analysis summaries/controlled conversation integration, retaining scope/freshness/ownership and per-user resource limits before an endpoint. CORE-05-B2 is preserved.


- [x] First run `.\mvnw.cmd -q '-Dtest=PortfolioReadToolTests,AgentFoundationTests,FreshSchemaTests' test` PASS21/21/0fail/error (target/ai2-tests.log).
- [~] Added a multi-holding/cash denominator and partially stale valuation test plus actual-JPA empty-portfolio integration/no-initialization assertion. NEXT repeat same command on final sources; result pending. Tool remains internal, flag off, no provider egress.

- [~] PortfolioReadTool written and local mock registry now allows only getMyPortfolio with an empty argument schema. Server user/consent enforced; cached timestamp freshness and value consistency checked; immutable equity-only evidence with weights, existing holding P&L and limitations. No model egress/endpoint/UI or transaction. Seven tests added plus AI-1/fresh-schema updates. NEXT `.\mvnw.cmd -q '-Dtest=PortfolioReadToolTests,AgentFoundationTests,FreshSchemaTests' test`; verification pending.

- [x] Owner requested start after AI-1 approval boundary; treating this as authorization for AI-2 read-only implementation. CORE-05-B2 preserved. Reviewed existing portfolio DTOs/services and AI-1 flag/registry.
- [~] First unit: owned internal equity-holdings evidence with cached quote timestamps, explicitly scoped equity weights/P&L and no model egress. No new endpoint/UI, schema/writer or live provider. Tests pending; NEXT implement/test tool. Learning A02/I01/S01/S02 explained; no personal topic completed.

## 2026-11-23 Advanced AI Agent / AI-1 checkpoint

- [x] AI-1 implemented/tested: AiProvider, MockAiProvider, ToolRegistry, AgentFoundation and AgentConfiguration plus AgentFoundationTests; FreshSchemaTests extended for default absence. Final `.\mvnw.cmd -q '-Dtest=AgentFoundationTests,FreshSchemaTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests' test` PASS35/35/0fail/error (target/ai1-final-tests.log). Initial identity-rejection assertion failure corrected and final stable sources rerun. `node target/ai1-verify.cjs` PASS: six new Java files, seven documentation files and one existing test changed; all 413 other baseline files unchanged. Initial QA legend-count assertion corrected to count tracker rows, not the status legend. No remaining failures. Evidence target/ai1-verification.json.
- [x] No application tool/executor, live provider, new endpoint/UI, model key, dependency, schema/migration, auth change, paid service or deployment added. Mock configuration is default-off and requires dev/test, excludes prod, and explicitly selects mock. Controls are single-turn input/output/declared-token bounds, consent, server identity and safe errors; live timeouts, per-user quotas, tokenizer accounting and full tool-schema/execution policies must precede a live endpoint. No new AI acceptance/production gate certified.
- [ ] AI-2 portfolio intelligence is PLANNED, not implemented or user-accepted; wait for explicit owner approval. CORE-05-B2 classifier/unfinished adapter and other V3 priorities preserved. Owner-dependent AI decisions only in canonical checklist.


- [~] First AI/fresh-schema run: 14 tests/1 tool-identity rejection assertion failure, no errors. Tightened identity-key checks case-insensitively and flattened conditional configuration; tests were edited during the initial build, so rerun final stable sources before acceptance. NEXT `.\mvnw.cmd -q '-Dtest=AgentFoundationTests,FreshSchemaTests,FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests' test`. No live provider, API/tool execution or release gate enabled.

- [~] Review refinement: removed nested configuration to prevent independent component scanning from bypassing outer profile/flag conditions; all mock beans now guarded at the top-level configuration and provider-specific bean methods. Canonical future owner decisions recorded only in checklist. Learning guide now has 24 cards, A01/A02/A03 P0, optional F04 tuning literacy; all personal status rows preserved. NEXT rerun AI-1/fresh-schema tests after this configuration change.

- [~] Foundation code written under chat/agent: provider contract, offline mock, tool schema registry (no execution), consent/identity/output-budget boundary and explicit local-only default-off configuration. Eight tests added; no real tools, controllers, schema or live-provider adapters. NEXT `.\mvnw.cmd -q '-Dtest=AgentFoundationTests,FreshSchemaTests' test`; results pending.

- [x] Updated existing V3 plan/inventory and learning roadmap with owner-approved advanced scope, AI-1..6 and plain Java provider/tool architecture. Existing CORE-05-B2 preserved; deployment paused, release counts and manual tasks unchanged.
- [~] AI-1 isolated foundation implementation next; tests pending. AI-2 not authorized until explicit approval. No disruptive changes, live provider or application tool enabled.

## Current V3 progress — updated 2026-11-23

**Current stage: AI-2 read tool and internal summaries implemented/tested; conversation interface next; CORE-05-B2 work is preserved for normal continuation.** CORE-05-B overall remains partial. V3 is in development. Deployment is **PAUSED BY OWNER DECISION**; historical release status remains **7 PASS / 2 FAIL / 17 BLOCKED**.

Use **[x] completed with evidence**, **[~] partially implemented/in progress**, **[ ] not started**. Completed documentation does not mean the feature is implemented or accepted for release.

### Completed work

- [x] **AI-2 internal explanations:** PortfolioAnalysisService returns scoped equity value/unrealized P&L and largest-listing explanations, with missing-data/null handling and no invented risk score. Final28/28 tests passed; the conversation screen/API are still pending.

- [x] **AI-2 first internal portfolio tool:** PortfolioReadTool implemented and tested22/22 with strict user/consent scope and timestamped equity-only evidence. No public UI or external AI data sharing; AI-2 overall is not complete.

- [x] **Advanced AI Agent AI-1:** provider-neutral offline foundation, server-identity/consent boundary, bounded requests/results and empty strict tool registry implemented. Default-off/local mock configuration tested; no live tools or UI. Final35/35 tests passed; AI-2 not started.

- [x] **CORE-05-B2 initial classifier:** added FinanceEventClassifier and five tests. It separates existing BORROW contribution, INTEREST accrual and REPAY paid interest, retains principal direction and marks unsupported/invalid input without fabricated zeros. `.\mvnw.cmd -q '-Dtest=FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests' test` PASS21/21/0fail/error (target/core05b2-classifier.log). B2 remains partial: no owned source adapter, fee reconciliation, database query, API or UI added yet. Next is that bounded source-adapter/reconciliation work; no writer/schema change.

- [x] **Simple project guide and cleanup:** created [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) explaining today's product, intended AI experience, done/current/next work and retained documents. Removed only unused generated HELP.md and frontend/README.md after reference inspection; handoff, AI roadmap and all security/deployment/release records preserved. README links the overview. Inline Node existence/status/local-link checks PASS; no broken overview links or missing protected records. Cleanup itself changes no runtime behavior.

- [x] **V3 transition and roadmap:** recorded the deployment pause, preserved M01-M32 and existing release evidence, and created [V3_DEVELOPMENT_PLAN.md](V3_DEVELOPMENT_PLAN.md).
- [x] **AI learning guide:** created [AI_LEARNING_ROADMAP.md](AI_LEARNING_ROADMAP.md) with 23 topic cards, free resources, exercises, learner-owned progress and maintenance rules. This completes the guide, not your personal learning or the AI feature.
- [x] **CORE-01 — feature inventory:** created [V3_FEATURE_INVENTORY.md](V3_FEATURE_INVENTORY.md), mapping nine catalogue items into 20 bounded units with implementation evidence and acceptance criteria. Documentation QA passed.
- [x] **CORE-05-A — actual backend code:** added [SettlementJournalService](src/main/java/com/example/investsmart/settlement/SettlementJournalService.java), [SettlementJournalRepository](src/main/java/com/example/investsmart/settlement/SettlementJournalRepository.java) and [SettlementJournalPage](src/main/java/com/example/investsmart/dto/SettlementJournalPage.java). The service returns only the active user's journal entries, paginates safely and reads coverage/entries in one consistent snapshot without initializing accounts or changing money.
- [x] **CORE-05-A tests:** seven focused tests passed; the affected suite passed 104 executed tests with zero failures/errors. The separate opt-in MySQL test passed concurrent-commit snapshot, restart and dump/restore checks. Real-JPA integration passed; synthetic containers were cleaned. Exact commands and evidence are in the dated CORE-05-A entry below.
- [x] **CORE-05-B1 — source-aware funding evidence:** added [SettlementFundingService](src/main/java/com/example/investsmart/settlement/SettlementFundingService.java), [repository](src/main/java/com/example/investsmart/settlement/SettlementFundingRepository.java) and [DTO](src/main/java/com/example/investsmart/dto/SettlementFundingPage.java). Reserve/spend/release events are checked against owned allocation/source records and return MATCHED, MISMATCH or INCOMPLETE; other actions remain NOT_EVALUATED. No running cash balance or profit is inferred.
- [x] **CORE-05-B1 verification:** 16 focused tests passed; affected regressions passed 113 executed tests/zero failures/errors, plus a separately enabled MySQL snapshot/restart/restore test. Nine new tests include actual financial writers, partial refunds before/after settlement, retry/debt, ownership/corruption and the 1000/1001 evidence boundary. Fresh-schema JPA integration passed. Commands and initial corrected fixture failure are recorded below.
- [x] **Checkpoint rules:** AGENTS.md requires this handoff and WORK_QUEUE.md to be updated after every meaningful change, with truthful status, verification and next action.

### Remaining work

- [~] **CORE-05 overall reporting:** internal journal reading and bounded source-evidence checks are done; complete statements, fees/interest, period reconciliation, public APIs, screens and exports are not done.
- [~] **CORE-05-B:** B1 funding evidence is implemented and tested. [CORE05_REPORTING_SEMANTICS.md](CORE05_REPORTING_SEMANTICS.md) defines its scope and the remaining source/coverage limitations. This is not account-wide financial reconciliation.
- [ ] **Next — CORE-05-B2:** implement verified product fee/interest source classification and further statement/period reconciliation; compare detail with aggregates without double counting. Any newly required writer/schema change needs its concrete impact review first.
- [ ] **CORE-05-C:** implement the approved reporting API, UI and exports after reconciliation is verified.
- [ ] **Later V3 catalogue:** AMO, baskets, user price alerts/general depth, futures, secured option writing, NAV-based funds/SIPs and eligible ETF/bond trading remain pending as scoped in the inventory.
- [~] **InvestSmart Advanced AI Agent:** major planned V3 differentiator; release priority stays nonblocking P2. AI-1 isolated mock foundation is implemented/tested (35 tests). Existing assistant remains disabled/unrouted. Provider/budget undecided; AI-2 needs approval. Portfolio/market/multi-step/scenario/preparation/confirmed-action capabilities are planned, not implemented.

**Prior handoff presentation refresh — [x] complete (historical documentation-only unit):** refreshed the summary and the older AI section shown in the screenshot. Inline Node SHA256/status/local-link checks against target/handoff-presentation-baseline.json PASS: only this handoff and WORK_QUEUE.md changed in that unit. The verifier's dash-literal assertion was corrected for PowerShell stdin encoding; the document itself was unaffected. No application tests were needed for that presentation-only change. Current implementation progress and tests are listed above. All open owner dependencies remain only in DEPLOYMENT_CHECKLIST.md.

## 2026-11-23 — CORE-05-B checkpoint

- [~] B2 initial code unit written: FinanceEventClassifier maps existing BORROW contribution, INTEREST accrual and REPAY paid-interest semantics to separate fields; unknown/invalid amounts are not silently zero. Pure component, not connected to DB/API and not an expense total. Added five focused tests for directions, partial payment, zero/unknown data and precision. NEXT `.\mvnw.cmd -q '-Dtest=FinanceEventClassifierTests,SettlementFundingServiceTests,SettlementJournalServiceTests' test`; results pending. No financial writer or schema modified.

- [~] Owner requested documentation cleanup followed by continued development. Overview added and unused starter guides removed; next safe B2 unit is pure classification of existing BORROW/INTEREST/REPAY finance events, preserving principal vs contribution vs accrued vs paid interest. No schema, writer, API or AI activation. Tests pending implementation. Handoff/queue synchronized before next unit.

- [x] FINAL B1 scope verification: `node target/core05b-verify.cjs` PASS. Five new files (three production Java, one test, one semantics document); seven existing files changed (three tests/fixture, four handoff/planning docs). All 407 other baseline files byte-identical, including financial writers, schemas, public APIs, frontend, authentication/deployment configuration, AI tracker, canonical checklist/manual tasks and prior release evidence. No synthetic MySQL containers remain. No outstanding B1 test failures. B1 is complete; B overall remains partial and NEXT B2. Scope evidence target/core05b-verification.json; release gates remain historical, not recertified.

- [x] B1 complete and locally verified. `.\mvnw.cmd -q '-Dtest=Settlement*Tests,Wallet*Tests,Portfolio*Tests,FreshSchemaTests' test` PASS114total/113executed/1opt-in skip/0fail/error; separate enabled MySQL PASS1/1 retained in target/core05b-mysql-result.xml. Evidence target/core05b-regression.log + core05b-regression-summary.json. Fresh-schema6/6 includes funding service via real JPA and suspended-user denial. No financial writer, schema, endpoint/UI, dependency or deployment change; no current failing tests. NEXT B2, not public exports or historical balance invention. Baseline scope verification follows; release status/AI progress/owner dependencies unchanged.

- [x] Enabled MySQL run PASS: V3_JOURNAL_MYSQL=true scoped to `.\mvnw.cmd -q '-Dtest=SettlementJournalMySqlTests' test` (flag removed in finally); 1 test/0 failures/errors. Journal and funding views both retain the pre-commit snapshot; funding evidence matches after commit and report equality survives restart/dump/restore. Preserved target/core05b-mysql-result.xml before default-suite skip; log target/core05b-mysql.log. NEXT affected `.\mvnw.cmd -q '-Dtest=Settlement*Tests,Wallet*Tests,Portfolio*Tests,FreshSchemaTests' test`; no benchmark or frontend change required.

- [x] Focused rerun PASS: `.\mvnw.cmd -q '-Dtest=SettlementFundingServiceTests,SettlementJournalServiceTests' test` 16/16, zero failures/errors; target/core05b-focused.log. Nine new funding tests include actual writer lifecycle, exact evidence bound and read-only state preservation. Initial fixture issue fixed without relaxing schema constraints.
- [~] NEXT enabled MySQL snapshot/restart/restore run: set V3_JOURNAL_MYSQL=true for `.\mvnw.cmd -q '-Dtest=SettlementJournalMySqlTests' test`, remove flag in finally, preserve result evidence before subsequent suites. No production state or release evidence changed.

- [~] First focused run: `.\mvnw.cmd -q '-Dtest=SettlementFundingServiceTests,SettlementJournalServiceTests' test` had 16 tests/0 assertion failures/1 fixture error. The truncation fixture violated the existing unique allocation-source constraint; corrected it to use distinct owned reusable-credit sources, preserving the constraint. Also checks exact 1000-row MATCHED vs 1001-row INCOMPLETE boundary. NEXT repeat focused command, then opt-in MySQL and affected regressions. Evidence target/core05b-focused.log; no production-code or financial-rule defect inferred from the fixture failure.

- [x] Reviewed handoff/queue/checklist and existing journal, allocation and financial writers; captured target/core05b-baseline.json with inline Node SHA256 command (PASS).
- [x] Defined B1 bounded funding-event evidence checks and remaining B2 fee/interest/account-wide limitations in CORE05_REPORTING_SEMANTICS.md. No historic balance inference or changed accounting policy.
- [~] Verification pending implementation: add owned source-evidence repository/service/DTO and synthetic tests; checkpoint before tests. No runtime changes in this documentation unit. No new owner dependency or release-gate change.
- [~] B1 production code now written: SettlementFundingService/Repository and immutable SettlementFundingPage DTO match bounded owned allocation evidence to RESERVE/SPEND/RELEASE; report MATCHED/MISMATCH/INCOMPLETE/NOT_EVALUATED without inferred cash deltas. Current credit state is labeled current; restricted-origin funding is flagged. No writer/initializer/schema/controller added. Tests pending; NEXT focused synthetic cases and actual-writer lifecycle plus MySQL verification. Other CORE-05-B work remains pending.
- [~] Added nine focused funding-evidence tests, including real SettlementFunds partial spend/refund before/after settlement, duplicate retry and debt-charge children; assertions also compare state before/after read. Extended existing real-JPA and opt-in MySQL tests for funding snapshot/restart/restore. NEXT `.\mvnw.cmd -q '-Dtest=SettlementFundingServiceTests,SettlementJournalServiceTests' test`; verification pending. No money/schema/API behavior modified.

## 2026-11-23 — mandatory handoff checkpoint rule

- [x] Confirmed the existing CORE-05-A completion entry below already records actual code changes, focused/MySQL/regression commands and outcomes, limitations and next task. No completion evidence was missing from that entry.
- [x] Owner instruction persisted in AGENTS.md: update this handoff AND WORK_QUEUE.md after every meaningful code/test/configuration/documentation change or small unit, before the next unit, long verification and turn completion. Apply [x]/[~]/[ ] truthfully, preserve history and explicitly label pending verification. Both documentation updates belong to the same unit.
- [x] Verification PASS: inline Node hash/content checks against target/handoff-rule-baseline.json confirmed exactly AGENTS.md, PROJECT_HANDOFF.md and WORK_QUEUE.md changed, persistent rule/checkpoints present, and all other baseline files unchanged. No failures remain in this documentation unit. Application tests not run because no application/data/configuration/deployment changes occurred.
- [ ] Next development task remains CORE-05-B source-aware reconciliation semantics and impact review. Owner dependencies remain exclusively in DEPLOYMENT_CHECKLIST.md; deployment pause and historical release gates unchanged.

## 2026-11-23 — CORE-05-A backend implementation completed

First V3 application-code unit implemented: SettlementJournalService + SettlementJournalRepository + immutable SettlementJournalPage DTO. Internal active authenticated-user read, bound zero-based pages (default 0/50, maximum 100000/100), stable timestamp/event-key order, raw actions/two-decimal magnitudes, nullable references and initialized-at coverage. REQUIRES_NEW read-only REPEATABLE_READ protects one-page coverage/journal snapshot even under weaker caller transactions. It uses no initialization or financial writer; it does not claim signed cash movements/running balances/P&L. No public route/UI/schema/dependency/deployment change. Next CORE-05-B requires source-aware reconciliation semantics/impact review; public report/export work remains CORE-05-C.

Verification: `.\mvnw.cmd -q '-Dtest=SettlementJournalServiceTests' test` PASS7/7; enabled `$env:V3_JOURNAL_MYSQL='true'; .\mvnw.cmd -q '-Dtest=SettlementJournalServiceTests,SettlementJournalMySqlTests' test` PASS8/8 (flag removed afterward). Actual isolated MySQL test commits between SELECTs, checks index availability, restarts container, dumps/restores synthetic data and compares exact report pages. First harness attempt failed on Java/Windows nested Docker-template quoting, fixed to strict-loopback port discovery and JSON label inspection; confirmed leaked synthetic container removed before passing rerun. Final labeled-container check empty; no retained database or user service accessed.

Affected regression `.\mvnw.cmd -q '-Dtest=Settlement*Tests,Wallet*Tests,Portfolio*Tests,FreshSchemaTests' test` PASS105total/104executed/1opt-in skip/0fail/error; MySQL skip is separately verified above. Fresh-schema6/6 includes real JPA identity, suspended denial and no account initialization. Evidence target/core05a-{focused,mysql,regression}.log and core05a-regression-summary.json. Added tests/fixture and one FreshSchemaTests case without weakening existing checks. Release reports/artifacts were not recertified. AI guide/progress unchanged; M01-M32 and all owner dependencies preserved exclusively in canonical checklist. Deployment remains PAUSED BY OWNER DECISION; historical7PASS/2FAIL/17BLOCKED unchanged. Final baseline scope verification recorded in queue.

## 2026-11-23 — CORE-01 complete; CORE-05-A next

Owner-approved inventory milestone completed in [V3_FEATURE_INVENTORY.md](V3_FEATURE_INVENTORY.md): 10 V2 baseline evidence groups, 6 schema groups, nine approved CORE/DERIV/INVEST parents, optional P2 AI and 20 bounded work units with acceptance criteria/exclusions. Six official Kite/Console/Coin source links opened successfully today. Reviewed existing accounting/opening/history, product events, current routes and test locations; source inspection is not a fresh runtime/device/release acceptance claim.

Key CORE-05 finding: existing /settlement/history lists obligations, while settlement journals, allocations and finance/MIS events provide reusable reporting evidence. Journal magnitudes are not signed cash/P&L deltas; legacy opening captures/current mutable rows cannot establish arbitrary prior running balances. Preserve nullable legacy costs, avoid aggregate/detail double counting and never initialize accounts merely for a read. First next implementation task CORE-05-A is an internal owned bounded read-only journal projection with coverage metadata and focused tests; no API/schema/writer change. See inventory for concrete inputs, DTO contents, pagination, validation and acceptance. Implementation has not started; later reconciliation/API/UI/export work remains separate.

Verification: `node target/core01-verify.cjs` PASS (9 parents/10 baseline groups/6 schema groups/20 units/111 local evidence links/6 verified public links); evidence target/core01-verification.json. Exactly V3_FEATURE_INVENTORY.md created and WORK_QUEUE.md, V3_DEVELOPMENT_PLAN.md and this handoff changed; other baseline files unchanged. No application tests run for documentation/source inspection, retained database untouched. No new owner dependency; none removed. AI scope/progress unchanged, deployment PAUSED BY OWNER DECISION, M01-M32 on hold, historical 7 PASS / 2 FAIL / 17 BLOCKED preserved. No feature, purchase, provider-authentication, credential, infrastructure or release-status change occurred.

## 2026-11-23 — personalized AI learning guide

Owner-approved documentation task completed: [AI_LEARNING_ROADMAP.md](AI_LEARNING_ROADMAP.md) created at repository root with inspected disabled-assistant architecture, 23 eleven-field topic cards (10 P0 / 8 P1 / 5 P2), 22 resource entries/23 URLs opened during today's planning review, standalone synthetic exercises, budget formula and learner-owned progress/change log. All 23 topics start Not Started; only explicit learner confirmation can mark personal learning complete. AI stays optional P2; agent responsibilities/provider/budget remain undecided. No AI feature, paid call or service activation occurred.

AGENTS.md now requires incremental review/teaching during AI changes; V3_DEVELOPMENT_PLAN.md links the guide. Existing content/history preserved. Verification: `node target/ai-learning-verify.cjs` PASS for card fields/counts, resource references, local links, study-hour totals (P0 24-36 / P1 20-30 / P2 12-24), tracker defaults and baseline file preservation. No application tests run for documentation-only changes. Detailed evidence: target/ai-learning-verification.json; local helper/baseline remain in ignored target. No new owner dependencies and none removed. Deployment hold, M01-M32 and historical 7 PASS / 2 FAIL / 17 BLOCKED unchanged. Next development milestone remains CORE-01; first learner topic F01, Google LLM introduction. Maintenance occurs during future work, not a background automation.

## 2026-11-23 — owner transition decision

**PAUSED BY OWNER DECISION — prioritizing V3 development before public launch.** Production remains NO-GO. Latest documented status: **7 PASS / 2 FAIL / 17 BLOCKED** (focused V2: 7 PASS / 2 FAIL / 15 BLOCKED; two P2 expansion gates remain blocked). These historical results are preserved, not freshly recertified.

V3 Development -> V3 Testing -> Production Readiness -> Website Launch -> Android App -> iOS App. See [V3_DEVELOPMENT_PLAN.md](V3_DEVELOPMENT_PLAN.md) for the approved roadmap. Older deployment-first instructions are superseded by this dated decision; historical conclusions remain intact.

Documentation-only transition: existing accounts, holdings, balances, reservations, trades, security/MFA, Upstox, settlement, tests, Docker/hosting configurations, legal pages, KC branding and release artifacts are preserved. No database access, runtime change, credential rotation, purchase, deployment or publication occurred. The old candidate certifies only its original snapshot. CORE-01 inventory is the next specification milestone; new features have not started. Learning is optional P2 and remains disabled. All open owner dependencies remain exclusively in DEPLOYMENT_CHECKLIST.md.

Roadmap approval is evidenced by the owner instruction to implement the approved plan after review and selection of optional P2 learning. No existing deployment/security dependency was completed or removed. Verification results follow after documentation QA.

> **For every agent:** Read this file before changing the project. At the end of each task, update the checklist, current status, decisions, changed areas, verification results, and next steps here. Keep this file concise and factual so the next agent can continue without relying on chat history.

> **User dependencies:** Maintain `DEPLOYMENT_CHECKLIST.md` as the single open-items list for deployment and actions/input needed from the user. Add dependencies immediately; remove completed requests only after verification and record their outcome here. Keep all credentials/recovery codes out of both files. The screenshot-based paper-ticket expansion is now authorized; earlier deferrals of those exact controls are superseded by the approved simulation plan.

## Project at a glance

2026-11-23 transition FINAL: node target/v3-transition-verify.cjs PASS after correcting inserted-text encoding; 32 unique M01-M32 hold labels, unchanged checkbox states in all six original documents, preserved historical non-queue text, local Markdown links valid, and 399 other baseline files byte-identical. Exactly six existing documents changed and V3_DEVELOPMENT_PLAN.md created; target contains local baseline/QA evidence only. No application tests run because no application/configuration/schema changes occurred. Historical release status remains 7 PASS / 2 FAIL / 17 BLOCKED; no gate recertified and no dependencies removed. Remaining release failures/blocks persist under owner hold. NEXT CORE-01 evidence-backed inventory/specification milestone; no features, spending or deployment started.


InvestSmart is an India-focused educational investing application. The project uses Java 21, Spring Boot, Spring Security with JWT, Spring Data JPA, MySQL, and a React/Vite frontend. Market data uses Upstox. The product is educational; it must not present guaranteed returns or personalized financial advice.

Typical backend flow: **controller -> service -> repository**. Keep business logic in services, use DTOs at API boundaries, and use constructor injection. Existing Java code uses explicit constructors/getters/setters rather than Lombok. The frontend uses React function components and hooks, React Router, and the shared Axios client in `frontend/src/api/api.js`.

## Status legend

- `[x]` Complete in code or confirmed by the user (notes identify which).
- `[~]` Partially complete, needs review, or has known remaining work.
- `[ ]` Not started.

## Roadmap checklist

### 2026-11-16 - SEC-03 local investigation refreshed

[x] Fresh bounded redacted current/reachable-history investigation completed:2commits/107blobs;74current/67historical indicators; scratch14distinctJWT/20password fields/2distinct password values. All14JWT expiry claims past at review time; signatures/active service state not verified. One existing private settings file exact-password comparison found no matches; external reuse remains unknown. One configured GitHub remote/local remote-tracking ref detected without fetching or publishing. Scanner redaction tests3/3PASS. Evidence: SEC03_CREDENTIAL_EXPOSURE_REPORT.md and target/sec03-{current-history-review,expiry-reuse-summary}.json. No passwords/tokens/subjects/owner identities returned; no credentials rotated, files/history deleted, database touched or application behavior changed.

[~] SEC-03 remains BLOCKED pending private owner account/reuse/sharing confirmations and actual production secret isolation/recovery evidence; canonical checklist step2 holds precise instructions. Current artifacts were not freshly recertified. Counts remain7PASS/2FAIL/17BLOCKED. User explicitly holds benchmark/hosting until review resolved.

### 2026-11-16 - Workspace header simplified

[x] Latest user request supersedes navbar attribution: removed by KC from shared dashboard/workspace Navbar and restored single-line InvestSmart typography/accessible label. Login/legal/About attribution retained. Navbar.jsx/.css only application changes; npm.cmd run lint/build PASS. No auth/trading/data changes, rendered acceptance not inferred; release status unchanged.

### 2026-11-16 - Chart development import recovery

[x] User Console evidence identifies lightweight-charts 504 Outdated Optimize Dep -> React.lazy chart import rejection. Added startup optimizeDeps.include in frontend/vite.config.js and isolated legal SSR test cache (src/api/legal.test.mjs) from running dev cache. Stopped only confirmed project Vite5173 plus task5175; restarted5173 via npm.cmd run dev -- --force. After54/54 frontend tests, compiled chart component and fresh optimized dependency both HTTP200 (dependency v=8f1290bf,2089499bytes). npm.cmd run lint/build PASS. No backend/auth/data changes. This verifies development module recovery, not rendered chart/device acceptance. Browser tab must reload to retry rejected lazy import. Requested sanitized Console evidence received; dependency removed only after this record. Release counts unchanged7/2/17; old archive certifies own snapshot.

### 2026-11-16 - Legal pages and creator refinement

[x] Screenshot refinement: navbar by KC right-aligned beneath InvestSmart (Navbar.css only). npm.cmd run build PASS; no behavior changes, visual acceptance not inferred.

[x] Latest creator request supersedes single-auth-only placement: plain by KC now sits directly below visible InvestSmart brand in Navbar, authentication identity and legal/About page header. Auth slogan follows the signature; no trademark or logo change. Navbar.jsx/.css, AuthScreen.jsx, LegalPages.jsx/.css updated. npm.cmd run lint and npm.cmd run build PASS. No auth/backend changes or rendered acceptance claim; legal notice remains Account-only.

[x] Placement refinement: screenshot's repeated simulation notice/legal link row moved from shared Layout.jsx to a dedicated Legal information panel in Account.jsx only. Login acknowledgment and public legal-page navigation remain accessible. npm.cmd run lint and npm.cmd run build passed; rendered acceptance still pending. No authentication/backend changes; counts unchanged.

[x] Frontend draft Terms/Privacy/Paper-Trading/Market Data and About routes added publicly; About identifies Krishna Chaitanya. User-approved frontend-only acknowledgment gates login/register and passkey unlock without changing factor/recovery APIs or storing consent. User refinement removes trademark symbols, keeps one plain by KC signature below auth InvestSmart identity, shortens acknowledgment and collapses duplicate legal links. Files: App.jsx, Layout.jsx, AuthScreen.jsx, components/LegalLinks.jsx, pages/LegalPages.jsx/.css, legal/content.js, api/legal.test.mjs. No backend/schema/deployment/private configuration change.

[~] Legal review/contact/provider permissions remain canonical checklist dependencies. 54 frontend tests passed; lint/build verification recorded in queue. Public routes render without auth context in SSR tests; this is not visual/device acceptance. Prior browser attempt stopped at unavailable host/current-URL safety barrier, not bypassed. Current frontend differs from dzYbes; regenerate/verify candidate before release. Counts remain 7PASS/2FAIL/17BLOCKED; previous backend evidence retains its snapshot scope.

### 2026-11-16 - Portable beginner manual guide completed

[x] User-requested WHAT_TO_DO_MANUALLY.txt created at project root as a dated portable expansion of canonical DEPLOYMENT_CHECKLIST.md, with32unverified manual groups/201checkboxes, conditional routes, beginner service UI steps, safe commands/config maps, approximate verified prices, per-task non-secret confirmations and separate restricted-production/public-launch approvals. All2FAIL+17BLOCKED gates mapped;2deferredP2 explicitly excluded from V2 launch. No manual action marked completed.

[x] Safe local preparation completed first: local frontend dependency adoption repaired/verified as recorded below,40tooling tests passed today,51local frontend tests/lint/build/tree passed,9bundle files byte-identical, source and dzYbes archive hash unchanged. Fresh npm audit0 and Maven OSV143/0 on2026-11-16; redacted reachable-history review unchanged. Guide QA found no missing gate, known-sensitive match or credential indicator (target/manual-guide-qa.json). Prior backend332/runtime evidence remains for the unchanged code; no unnecessary full rerun or blocked35min attempt.

[~] Guide confirms external facts that affect purchasing/configuration: DO blocks25/465/587; Brevo supports2525 with TLS but actual allowance/receipt needs operator verification. Upstox1-year read-only Analytics Token is available; permission/endpoints/staticIP/one-token policy must be accepted, not assumed. Public-use/licensing clearance precedes launch spending. Source links/prices date checked in guide. Gates remain7PASS/2FAIL/17BLOCKED; no purchase, remote publication, deployment or credential change.

### 2026-11-16 - Local dependency adoption verified before manual guide

[x] No5173listener or project node_modules process was observed. Root npm ls exited1; npm ci safely aligned frontend to the reviewed lock without stopping any user server or using force. Final npm ls/lint/51tests/build passed, npm ci audit0. All9 dist files byte-identical to validated dzYbes snapshot; code/config source hashes unchanged. No dev server was started; next normal startup uses the installed dependencies. Evidence target/manual-guide-local-{ci,tree,lint,tests,build}.log and manual-guide-local-adoption.json. Local dependency-adoption request can be removed from checklist. Credentials, accounts and retained data untouched.

### 2026-11-15 - Production template and independent-runner preparation

[x] Annotated production.env.example for hosting/SMTP/Upstox/staging, with all credentials/origins blank and explicit Compose DB URL. Optional --compose preflight requires root secret and pairwise distinct DB/root/JWT/encryption values, with no value diagnostics; standalone mode preserved. Shipped header-only settlement CSV + non-activating review worksheet and pinned SMTP server identity true (already Angus default). No dates/credentials/permission assumptions invented.

[x] Tooling40/40; targeted calendar/mail/template tests26/26; fresh full backend332total/317executed/15opt-in skips/0fail/error, frontend51/audits0/lint/build, Maven OSV143/0 passed. Synthetic merged Compose/preflight passed without services. Fresh packaged MySQL6/6+schema/recovery/HTTPS/CSRF/headers/413/staging passed (smoke-3ztP0Z). Candidate dzYbes independently56payload checksums/17migrations/portable checks passed; exact501file known-value matches0. Evidence target/templates-final-build.log, templates-compose-validation.json, templates-independent-verification.json, templates-artifact-secret-review.json.

[~] Manual-only Linux CI capacity workflow prepared/static-parsed; not published/dispatched/executed. Current source remains uncommitted with protected scratch credentials, so no upload/push; Windows policy remains Restricted. No repeated long test against unchanged dependency. Source/template/human/host/provider/calendar/credential acceptance gates remain7PASS/2FAIL/17BLOCKED. Manual steps only checklist. No deployment/credential or production-data changes.

### 2026-11-15 - Release follow-up: actionable safeguards fixed, production still blocked

[x] Fixed omitted/mixed prod profiles in preflight/runtime; HTTPS edge now supplies HSTS/nosniff/no-referrer and16MB request-body ceiling. Actual packaged checks require413 and headers. Test-only continuity guard aborts20s wall gaps/2s scheduler lag and retains partial failures; analyzer rejects missing completion, phase/count/span or telemetry evidence and leaves interrupted memory assessment inconclusive. No financial accounting/schema or frontend application code changed.

[x] Fresh required-container build passed330backend/0fail/0error/15opt-in skips, Maven OSV143/0, isolated frontend51/full+productionnpm0/lint/build, tooling36, MySQL6/6+parity/restore/restart/HTTPS/CSRF/headers/413/staging (smoke-neEV1e). Final source/proxy hashes matched. Candidate nUhx39 independently SHA256/54payload checksums/17migrations/portable inspection passed;499exact payload/JAR/log files had0known-sensitive matches. Evidence target/followup-final-build.log, followup-independent-verification.json and followup-artifact-secret-review.json. Prior interrupted follow-up runtime was not used to certify final changed proxy files.

[~] Read-only exposure review covered2locally reachable commits/107blobs:67historical scratch indicators,74current indicators including3test fixtures;14distinct current JWT-shaped strings have past expiration claims, signatures not verified. Password reuse/remote or unreachable history and host secrets need private review. No credentials were exposed/deleted/rotated and no history rewritten. Restricted script policy blocks native power helper runtime; syntax/native declarations compiled only. No repeated35min test or policy bypass.

[~] Initial/final gates remain7PASS/2FAIL/17BLOCKED (two P2 deferred). Real capacity/leak, host/provider/calendar/credential/device acceptance and launch decision remain unmet. Detailed root causes/results in RELEASE_READINESS_REPORT.md; exact manual steps only DEPLOYMENT_CHECKLIST.md. No production deployment or data change.

### 2026-11-13 - Pre-deployment checks: local runtime passed, capacity interrupted

[x] Fresh backend327/0fail/0error/15opt-in skips; Maven OSV143/0; isolated frontend51/audit0/lint/build; tooling29passed. Added test-only JVM/GC/Hikari/MySQL/status/minute time series, guarded synthetic-project Docker observer, bounded draining, cooldown and metric-analysis tests. No production API/schema changes. Normal Docker Desktop startup restored the absent Linux engine; no reset.

[x] Monitored short2700requests passed with0errors/429/timeouts/drops, readp95124ms/write496ms and financial reconciliation. Separate packaged KbU1vf passed MySQL6/6, settlement parity, dump/restore, restart, local trusted HTTPS/CSRF/SPA and staging restriction. Full attempt hkZvM9 failed continuous capacity acceptance: standby caused1396s metrics gap,3917 dropped work items,26timeouts and2other exceptions; final reconciliation/result unavailable. Confirmed synthetic test fork stopped and isolated project cleaned; no user data or user server process changed.

[~] Full-duration capacity, leak assessment, production host/device/secret clearance and launch approval remain unmet. Built-artifact known-value/exclusion scan passed; tracked scratch credentials/history still need private review. Required-container candidate0Uoq5n passed; independent archive SHA256/54payload checksums/17migration files/portable inspection passed (target/release-independent-verification.json). Exact payload/JAR and selected logs499files matched no known sensitive values. productionLaunchApproved=false. Report RELEASE_READINESS_REPORT.md records every gate and evidence. All user dependencies remain only in deployment checklist. No deployment occurred.

### 2026-11-13 - Reviewed scoped minimatch fix: full npm audit clean

[x] User explicitly selected an isolated scoped-override evaluation. All four moderate npm package findings traced to GHSA-g3hv-75wg-m249 in minimatch, a development-only lint dependency. Production-only audit was already0. The reviewed fix pins minimatch11.0.8 in the ESLint subtree, covering ESLint10.14.0 and its config-array0.23.5 consumer without a global override or downgrade. This exceeds upstream declared10.x ranges and is a documented compatibility exception, not an audit waiver; see deploy/LINT_SECURITY_OVERRIDE.md for scope, evidence and removal criteria.

[x] Baseline/candidate comparison retained identical discovery and full ESLint diagnostics for97 project files plus5 fixtures. Required unused-variable/React Hooks/React Refresh rules remained active. Nested/brace/dot globs, dist/target ignore behavior and native Windows/slash paths matched. Clean install, ci and engine-strict ci passed; npm ls --all returned0 with no problems after reinstall. Lint,51 frontend tests and build passed. All9 production output files were byte-identical to baseline. Final full and production-only npm audits both0. Evidence target/minimatch-compatibility.json, minimatch-bundle-comparison.json, minimatch-audit-final-*.json, minimatch-final-*.log and minimatch-engine-strict-ci.log.

Earlier overlapping-rule candidate layouts were rejected because npm override contexts conflicted; no install error was waived. Source package.json/lock were adopted only after every gate and a byte-check against baseline to preserve unrelated edits. Live node_modules, servers, account data and credentials were untouched. New fixture comparison helper and its tests are retained; deployment tooling25/25 passed. DEPENDENCY_SECURITY_REPORT.md updated and scoped-review document included in future portable packages. Backend/runtime evidence is unchanged because this is dev-tooling-only and production assets are identical.

[~] No deployment, archive generation or35min test was started in this focused task. Dependency findings are resolved for the reviewed clean-install snapshot; remaining release/capacity/device/host gates stay open. Local installation adoption remains tracked exclusively in DEPLOYMENT_CHECKLIST.md.

### 2026-11-13 - Dependency remediation and restored build connectivity

[x] npm registry/advisory access and actual Docker Hub downloads verified: caddy:2-alpine, eclipse-temurin:21-jre and mysql:8.4 pulled successfully (target/security-docker-{pull,java-pull,mysql-pull}.log). This completes the earlier network-restoration request; no Docker reset or security-setting change was needed.

[x] Compatible frontend updates removed all6 high findings; final npm audit has4 moderate development-only ESLint-chain findings and production-only audit has0. Isolated install/clean ci/lint/51 tests/build passed. Spring Boot patched4.1.0→4.1.2 and Jackson pins removed in favor of its managed BOM. Keyless OSV scan of143 resolved Maven dependencies (including test/transitive scope) went from33 package/advisory matches to0. Backend verify passed327 total/0fail/0error/15opt-in skips. Tooling23/23 passed. Fresh resolved-inventory/OSV checks are now release gates; malformed/unavailable data cannot pass.

[~] NVD-based Dependency-Check could not initialize advisory data; it is incomplete, while OSV coverage completed. Four moderate ESLint/minimatch findings remain without a supported in-range fix; no forced minimatch11 override or EOL ESLint9 downgrade. Patched runtime/MySQL integration passed as recorded below; full35min load remains gated, per the user's condition. See DEPENDENCY_SECURITY_REPORT.md for before/after versions, coverage and exact evidence. Existing dev processes/node_modules/data and credentials preserved; no public deployment or approved archive.

Final patched-runtime result: MySQL6/6, settlement migration parity, dump/restore, restart, packaged HTTPS/CSRF and staging direct-peer/forged-header restrictions passed. Evidence target/security-runtime-check.log, target/release-checks/smoke-vs46Uv/5.log and result.json. Temporary smoke containers cleaned. Final deployment/security tooling23/23 passed (target/security-tooling-complete.log). DEPENDENCY_SECURITY_REPORT.md is included in future portable packages. The full35min test was deliberately NOT started because final full npm audit still has4moderate dev-tool findings; production-only audit0 and Maven OSV143/0 do not waive the all-severity release gate. Next obtain a supported compatible lint-chain remediation (or separately reviewed toolchain change), rerun security gates and only then full load/candidate verification. No production credentials or retained account data changed.

### 2026-11-13 - Sell/Short intent and deployment capacity preparation

[x] Added DEPLOYMENT_REQUIREMENTS.md with trading/security/infrastructure/capacity/scaling/operations/data/release/device/governance requirements and linked its gates into the existing checklist. All open user dependencies remain only in that checklist. Optional compose.capacity.yaml sets bounded CPU/memory, HTTP threads/queue and Hikari connections for a trial single-worker host; merged base/staging/capacity Compose quiet validation passed with synthetic configuration. No real host was configured and these defaults do not establish VPS capacity.

[x] Tickets distinguish Sell holdings, Sell/Short Intraday, Buy to close short and Sell to close long; explain owned remaining quantity and pending reservations, block excess position quantity in preview/submission, and lock listing selection for owned exits. Position actions say full square-off. Delivery ownership checks were preserved; short selling without Delivery holdings uses existing MIS collateral/accounting. Added backend regression for zero-holding short entry, partial/final closure and excess-exit rejection without reversal. Blue Buy/red Sell styling and existing product restrictions remain.

Verification: IntradayTradingTests + PaperTradingTests passed 43/43; isolated frontend 51/51 tests, lint/build passed (target/sell-short-backend.log, target/short-frontend-tests.log, target/short-final-{lint,build}.log). Final test compilation passed (target/short-final-compile.log); tooling 21/21 passed (target/short-tooling-final.log). No new production Java financial behavior or schema migration was needed. Rendered/device acceptance was not performed for this change.

[x] Added opt-in test-only HttpCapacityTests with random-loopback real HTTP, synthetic guarded MySQL, 100 persisted sessions/CSRF, deterministic price/session mocks and mocked mail. The packaged synthetic backend is stopped during load to prevent competing workers. Stable per-step request IDs support safe retries. Workload covers previews, pending short/cancel, market short, partial/final owned exits and activity/history/summary reads. Cold warmup metrics are separate; four orders per 80 requests respect the unchanged 30/user/15-minute limit. Aggregate reports contain no tokens/accounts.

First short attempt exposed cold/synchronized-load latency and failed performance while financial reconciliation passed. Revised 100-session short run passed 600 requests at30/s for20s plus900 at60/s for15s: overall read p95 66.6ms, write254.6ms, zero errors/429/busy drops and financial reconciliation true. Evidence target/http-capacity-smoke-passed.json and target/release-checks/smoke-texvZh/http-capacity-result.json. That run also passed MySQL6/6, settlement schema parity, restore/restart, HTTPS/CSRF and staging restrictions. It is not full-duration or VPS acceptance.

[~] Full1800s+300s run was attempted but stopped BEFORE workload when Docker could not resolve auth.docker.io (target/short-capacity-full.log, smoke-1uie3B/2.log). No full capacity result or new validated archive exists. Fresh online audit returned actual6high/4moderate findings; compatible lockfile update still failed and no force/major downgrade was used. Containers cleaned; operator backend25472/Vite22932 remained listening. Next restore build connectivity, rerun full --capacity, resolve advisories and create/independently verify a new candidate. All remaining host/provider/device/launch gates stay in the checklist; no public deployment occurred.

### 2026-11-13 - Focused V2 settlement/staging and dependency gate checkpoint

[x] Focused V2 scope approved: complete settlement/release-critical work; defer CORE/DERIV/INVEST catalogue to V3. Added Portfolio settlement funds/history and settled/T+1 holding-unit presentation. Owner Operations includes worker health and strict reviewed-calendar activation with recent authentication, owner checks, CSV checksum validation, mutex and transactional calendar/Admin audit. Monitoring outages cannot suppress guarded settlement processing. Existing schema/migrations are reused; no retained account database was changed.

[x] Prepared private single-VPS staging overlay, direct-peer IP allowlist, secret-safe preflight, device matrix and runbook in deploy/STAGING.md. Portable release inputs include staging files. Frontend release installs now run in an isolated allowlisted source snapshot to preserve live development dependencies. Added mandatory online npm audit gate: findings or transport failures prevent archive creation.

Verification so far: targeted settlement/API tests 83/83; earlier full Maven verify 325 total, zero failures/errors, 14 opt-in skips. Current isolated offline frontend install/lint/build and 50/50 tests passed (target/v2-isolated-{install,lint,tests,build}.log); deployment tooling 21/21 passed (target/v2-tooling-gate-tests.log). Offline install is not vulnerability acceptance. New isolated MySQL concurrency/migration parity/restore/restart/HTTPS/staging restriction checks are being verified separately; no new validated archive or public deployment is claimed.

[~] Dependency remediation remains incomplete. Earlier audit reported 6 high/4 moderate findings including Axios; compatible update attempts failed on missing Vite registry metadata, direct lookup ETIMEDOUT and unavailable advisory bulk service. No forced peers, automatic major downgrade or audit waiver was applied. Browser host was unavailable before navigation for the new settlement screens, so rendered/device acceptance is not inferred. Exact outstanding external actions remain exclusively in DEPLOYMENT_CHECKLIST.md.

Final isolated financial verification passed: actual MySQL 8.4 suite 6/6, including competing withdrawals and duplicate catch-up over reviewed synthetic closed days. Forward settlement migrations matched fresh-baseline columns. Separate T+1 credits, partial lots/allocations, delivery dependencies and journals survived dump/restore and restart. Trusted HTTPS/SPA/cookie/CSRF checks and staging direct-peer allow/deny plus forged-forwarded-header rejection passed. Evidence: target/v2-container-check.log, target/release-checks/smoke-ZKvvNJ/5.log and result.json. All temporary smoke containers cleaned; operator backend PID25472 and Vite PID22932 retained. No new release archive was created while dependency audit remains unresolved; prior archives retain only their earlier snapshot evidence. Independent verification of a new archive remains unfinished because no eligible archive exists.

### 2026-11-13 - Chart ranges and layout polish

[x] Added 5Y and All on normal/full charts, retaining the exact listing and range across navigation. Full chart is at the toolbar's right; Back to normal is at the full-page header's right. Centered the vertical chart/Watchlist divider grip, unified panel surfaces, retained aligned blue Buy/red Sell/Watchlist actions, and fixed narrow header shrinking. Short tablet viewports scroll the normal chart column to reach its footer; full mode keeps its viewport layout.

[x] All means available authorized provider history from January 2000 onward, with listing-dependent coverage disclosed. Daily provider requests use sequential windows no longer than ten years, sorted timestamp deduplication and no partial result on window failure. Completed long-range snapshots use a bounded 60-second cache, including empty/new-listing results. Shorter daily refreshes preserve older cached rows. Demo long history fails unavailable rather than inventing candles. No financial or database schema changes.

Verification: mvnw.cmd -B -Dtest=UpstoxMarketDataProviderTests,MarketDataServiceHistoryCacheTests,MarketDataServiceHeatmapTests test passed 32/32, zero failures/errors/skips (target/chart-ranges-backend-v2.log). Frontend node --test src/api/*.test.mjs src/auth/*.test.mjs passed 50/50 (target/chart-ranges-tests-v2.log); final npm.cmd run lint and npm.cmd run build passed after visual corrections (target/chart-ranges-lint-final.log and target/chart-ranges-build-final.log).

Native Chrome Guest review used the actual terminal component with an ignored, explicitly labeled synthetic fixture: desktop normal dark/full light and dark; 768px normal/full light and dark; 400px normal light/dark and full light; 320px normal/full light and dark. Observed All selection, return navigation retaining All, right-aligned controls, centered desktop divider and tablet footer access after correction. This certifies scoped rendered layout only: the 120 synthetic candles were not real 5Y/All provider history. No authentication, account orders, physical-device verification or live provider acceptance occurred. Temporary Guest preview closed. No backend/frontend servers started or restarted; backend remains under the user's IntelliJ workflow. No new release archive generated; older archives cover only their snapshots. Remaining release/dependency gates are in DEPLOYMENT_CHECKLIST.md; next roadmap task is T1-04.

### 2026-11-12 - Dedicated full-chart page replaces manual sizing

[x] User superseded manual chart resizing. Added protected /market/chart/:symbol/full route reusing the terminal/chart/ticket components. Workspace now has one Full chart button; Smaller/Larger/Reset/Expand and bottom drag handle plus unused sizing helper/tests are removed. Workspace retains its sidebar-width divider and viewport-based chart height; old saved heights are ignored. Full page hides the sidebar, compacts instrument/header details and fits chart canvas to remaining viewport height. Buy/Sell/Watchlist remain on both pages. Back to workspace preserves exact instrument key and range. Range lives in the URL, so direct links and browser navigation stay consistent. No backend/API/database changes.

Verification: npm.cmd run lint and npm.cmd run build passed; node --test src/api/*.test.mjs src/auth/*.test.mjs passed 49/49. Navigation tests cover encoded symbols, exact listing keys, full/workspace paths and valid/invalid periods. Logs target/full-chart-lint.log, target/full-chart-build.log and target/full-chart-tests.log. Rebuilt after the final CSS correction.

Native visual review succeeded after one app-list timeout/retry. Observed workspace -> full-chart navigation on actual RELIANCE, full desktop dark view, 320px dark, 400px dark/light and 768px light layouts. At 400px found instrument/price overlap; separated the price row and re-inspected the correction. Opened Buy ticket at 400px and Sell ticket at 768px, then closed without order submission. Returned to workspace and confirmed RELIANCE instrument key plus 1D range in URL; closed DevTools. Actual quote/candle polling continued. These checks cover observed rendered layouts and ticket opening, not physical-device authentication or every range/trade lifecycle. Full protected-route wiring is shared with the existing auth guard; no authentication prompts or safety barriers bypassed.

This replaces the earlier manual-resize design. Prior backend/MySQL telemetry-fix evidence remains applicable to its backend snapshot; release archives still require regeneration to include latest frontend. All open deployment dependencies remain exclusively in DEPLOYMENT_CHECKLIST.md.


- 2026-11-12: Port conflict resolved: stopped only the verified background InvestSmart process PID 34464 and confirmed port 8080 free. Backend intentionally left stopped for IntelliJ. This supersedes the earlier running-PID note; no application code or database changed.

### 2026-11-12 - Resizable chart and diagnosed Buy/Sell preview repair

[x] Chart canvas height is adjustable 280..1200px, defaults 540px desktop/360px mobile, and persists locally. Larger/Smaller change 80px; the bottom drag separator supports pointer capture and 40px keyboard adjustments, Home/End, plus Reset. Expand hides the sidebar; Restore retains its previous width. Existing width divider and chart ResizeObserver remain. Buy, Sell and compact Watchlist/Saved share one nonwrapping row, with blue Buy/red Sell styling.

[x] Diagnosed the actual MySQL failure mechanism: paper preview is read-only, but provider success tracking attempted a health-table write on that connection. Both canonical/stored RELIANCE quote probes succeeded; the direct read-only telemetry probe reproduced SQL state S1009. AdminHealthStore write methods now use independent writable REQUIRES_NEW transactions. Provider telemetry exceptions are sanitized and cannot discard successful quote responses or replace genuine provider failures. No pricing/ownership/funds/idempotency constraints were weakened.

Verification: mvnw.cmd -B verify passed 313 tests, zero failures/errors, 13 opt-in skips; frontend npm.cmd run lint/build and 49 API/auth tests passed. node scripts/check-v2-containers.mjs passed isolated MySQL 8.4 tests 5/5, including read-only telemetry/preview plus paper Buy and owned-quantity Sell; existing MIS short/partial-cover tests also passed. Restore/restart/packaged HTTPS/CSRF checks passed. Evidence: target/telemetry-fix-backend.log, target/resize-lint.log, target/resize-build.log, target/resize-tests.log, target/telemetry-fix-mysql-v2.log and target/release-checks/smoke-1oYTBs/result.json. First MySQL attempt failed only because the synthetic service name exceeded its column length; shortened the fixture and reran successfully.

Live confirmation: patched provider plus transaction proxy succeeded for actual RELIANCE inside a read-only MySQL transaction (patchedReadOnlyQuote=true, target/quote-probe-fixed-private.log). Probes used private configuration without printing credentials and did not place account trades. Reloaded only the verified local project backend using its original Java/classpath/local profile/private run environment, removing the IDE console agent; new PID 34464 started successfully. No database schema, account data, owner promotion or public deployment changed. Telemetry writes are intentionally persisted independently.

[~] Latest chart/ticket rendered/device acceptance remains separate: the prior native browser URL safety rejection was not bypassed. Live quote/transaction proof and synthetic MySQL Buy/Sell do not certify the user's browser order interaction. Existing archive xEfwUg does not include this source; the current container-validated staging snapshot is not a newly approved release archive. Open dependencies remain exclusively in DEPLOYMENT_CHECKLIST.md.


### 2026-11-12 - Buy/Sell ticket correction and dynamic chart implementation

[x] Implemented visible-tab, non-overlapping automatic quote refresh (15s), intraday candle refresh (30s) and longer-history refresh (60s), with abort/timeouts, visibility resume, Retry-After/backoff and manual refresh that respects cooldowns. Last successful data is retained on failure, with separate received timestamps and stale/error messages. Candle/volume updates retain the chart instance and preserve the visible logical range; a latest-bar view advances without resetting zoom. Actual intraday timestamps and timestamp deduplication replace artificial dates. The quote marker is separate from provider OHLC/volume; no candles are fabricated from quotes.

[x] Buy remains blue and Sell red; controls stay aligned behind the modal. New tickets focus the heading and reset body scroll; background scrolling is locked while open. Optional GTT exits and lengthy policies collapse; mandatory Cover protection remains visible. Fixed two obsolete Cover checks that used execution style instead of product, and the title follows the selected listing. Existing products, fresh-preview gating and submission/idempotency handling remain.

[x] Provider quote HTTP access failures and throttling now produce sanitized 503 messages and Retry-After headers; existing paper/MIS boundaries preserve those classified errors and preview retry scheduling honors provider cooldowns. The long-history database cache is no longer accepted indefinitely based only on date coverage: a bounded in-memory 60s freshness observation and per-listing striped locks trigger periodic provider revalidation. Provider endpoint/response identity conventions were checked against https://upstox.com/developer/api-documentation/get-full-market-quote-v3/; no account entitlement claim follows.

Verification: npm.cmd run lint and npm.cmd run build passed; node --test src/api/*.test.mjs src/auth/*.test.mjs passed 47/47. Maven command: mvnw.cmd -B -Dtest=UpstoxMarketDataProviderTests,MarketDataServiceHistoryCacheTests,PaperTradingTests,IntradayTradingTests test passed 67/67, zero failures/errors/skips. Logs: target/dynamic-chart-lint.log, target/dynamic-chart-build.log, target/dynamic-chart-frontend-tests.log and target/dynamic-chart-backend-tests.log. New tests cover refresh overlap/disposal, old-instrument isolation, visibility resume, Retry-After, range preservation, provider access/throttling classification and daily-cache expiry.

[~] Latest chart/ticket rendered acceptance is pending. Computer Use initialization/window selection worked, but navigation to the affected chart was rejected because the current Windows browser URL could not be determined confidently. No further app input or alternative browser route was attempted. Existing earlier visual acceptance does not certify this change. IntelliJ debugger router is absent; local unauthenticated quote probes returned 401. The exact live provider failure shown in the screenshot remains unproven, and no provider credentials, permissions or fresh-price guards were changed. A new backend run is needed to load compiled backend changes into a previously running server; no active user backend process was restarted. The xEfwUg archive predates these changes. Open external dependencies remain exclusively in DEPLOYMENT_CHECKLIST.md.


### 2026-11-12 - Screenshot Paper Activity and Phone Codes polish

[x] Applied the approved clean-card design to both supplied screenshots. Paper activity has five responsive summary cards, separate titled position/ticket/fill sections, explicit empty states, readable MIS policy text, status badges and wrapping blue Buy/red Sell actions. Populated tables retain headings, keyboard-focusable horizontal scroll regions and long-name wrapping. Compact Portfolio presentation remains supported. Activity CSS is isolated in its own stylesheet instead of the ticket stylesheet.

[x] Phone Codes has a centered branded card, separated header/action/help areas, full-width primary action, countdown panel and recovery/loading/error styling with narrow/safe-area handling. Existing API/polling, money, passkey ceremonies, code expiry and recovery behavior were preserved. No Java or database changes were needed.

Verification: frontend npm.cmd run lint and npm.cmd run build passed; node --test src/api/*.test.mjs src/auth/*.test.mjs passed 40/40. Logs: target/screenshot-css-lint.log, target/screenshot-css-build.log and target/screenshot-css-tests.log. No new implementation-mirroring CSS tests were added.

Native Computer Use worked on this attempt. Directly inspected real Orders empty activity at desktop, 400px and 320px; real locked Phone Codes at desktop, 768px, 400px and 320px. A clearly labeled ignored synthetic presentation fixture in frontend/target/css-review displayed populated financial tables on desktop/768px/320px, including unavailable valuation, long names, closure retry text, disabled actions and Buy/Sell colors. Horizontal table scrolling at 320px was observed. Synthetic six-digit countdown and recovery-code layouts were inspected at 320px. Keyboard focus outlines on the real phone brand link and primary action were observed. Restored the actual locked /phone page and closed DevTools. These observations certify only those rendered states, not physical devices, authentication ceremonies, every error/pairing state or protected Admin. No authentication prompts were opened or bypassed.

The validated xEfwUg archive predates this polish and does not certify the current frontend. Regenerate and independently validate a release candidate after remaining approved work. All open external dependencies remain in DEPLOYMENT_CHECKLIST.md.


### 2026-11-12 - Equity Intraday/MIS implemented and packaged

[x] Implemented the approved MIS checkpoint without restarting the completed preview-recovery work. Equity long and short positions/events are separate from delivery holdings. The shared paper preview, submit, process, cancel, replace, account and owned-position close paths use the shared user lock and idempotent fills. Additive managed migration `20261112_intraday.sql` matches fresh/runtime schemas; no existing local or production account database was migrated.

[x] Disclosed defaults: 2x simulated buying power using settled-cash collateral plus explicit fees, no overnight interest and no spendable short-sale turnover. Partial exits release proportional collateral (final exit releases the remainder); realized/unrealized P&L, restricted T+1 profits and uncovered-loss debt use shared accounting. New entries stop at 15:10 IST. Bounded jobs persist closure requests and retry with fresh prices/open sessions; unavailable prices never fabricate fills or convert positions to delivery.

[x] Product selection (Delivery, Intraday, MTF, Cover) is separate from execution style (Quick, Regular, Iceberg). MIS supports Quick Market and Regular Market/Limit/SL/SL-M with DAY/IOC. MIS GTT, attached GTT exits and Iceberg remain explicitly unavailable; Delivery Iceberg remains supported. MTF/Cover remain Regular-only. Buy remains blue and Sell red. Orders and Portfolio show intraday direction, collateral, P&L, partial-close tickets and retryable full-close status.

Verification: `node scripts/build-v2-release.mjs --require-containers` passed for frozen source. Maven verify: 308 tests, zero failures/errors, 12 opt-in skips; includes 21 MIS lifecycle/API/accounting tests and the existing 21 paper-engine tests. Frontend lint/build and 40 API/auth tests passed; release tooling 17/17 passed. Isolated MySQL 8.4 tests passed 4/4, including a partial short close; dump/restore and database/application restart preserved a partially closed short plus events. Packaged HTTPS/SPA/CSRF checks passed. Evidence: `target/intraday-required-release-final.log`, `target/release-checks/smoke-2XepeR/result.json`.

Candidate: `target/releases/InvestSmart-v2-xEfwUg.tar.gz`, SHA256 `17f25b663ae6a271ff85b768599e4eb06f2b1d4a8e01c3bcf4e2abde7782e341`. Independent extraction verified the archive checksum, all 45 payload checksums, portable artifact inspection, 17-entry offline migration catalog, packaged MIS class/migration and latest product help. Evidence: `target/release-checks/intraday-independent-verification.json`. Local Vite restarted successfully after packaging. This archive certifies its frozen inputs only; subsequent checkpoint/document updates are outside that snapshot.

Rendered desktop/mobile, physical-device and protected Admin acceptance are not certified by these tests. Native visual host was unavailable on the last attempt; no browser safety or authentication barrier was bypassed. Production launch remains unapproved. Broader T1-04 funds/history/holdings/Admin presentation and the approved feature catalogue remain unfinished; their implementation checkpoint is in WORK_QUEUE.md and all external dependencies remain exclusively in DEPLOYMENT_CHECKLIST.md.


### 2026-11-11 - Market chart and buy/sell visual polish

Screenshot follow-up: the live local Vite CSS endpoint returned 200 with current source styles and without the screenshot's old solid-blue-header rule. Refined the ticket to a 680px desktop dialog/mobile sheet with an independently scrolling body; header and footer no longer depend on sticky positioning inside the long form. Exchange labels/prices now use compact horizontal rows with long-symbol wrapping, and form spacing is tighter. Buy remains blue; Sell uses red accents and a separate deep-red submit background in both themes so dark-mode text accents do not make the action pale pink. Presentation markup only; order payloads/validation/calculations are unchanged. Frontend lint, build and all 36 API/auth tests passed again. Rendered acceptance remains pending until actual browser inspection.

The screenshot-follow-up rendered review attempt again returned native pipe unavailable (Windows error 2) during app listing. No browser window was inspected or safety/authentication barrier bypassed. Local HTTP/CSS checks establish served source only, not rendered acceptance.

Implemented the selected clean Kite-inspired styling as the user's current priority. Consolidated the shared dialog shell and paper-ticket CSS: neutral surfaces, blue Buy/red Sell accents, consistent labels/inputs/tabs, clearer estimates/advanced sections, sticky header/actions, and viewport/safe-area-aware mobile layout. Tickets now initialize their theme from the active chart; order payloads, validation and financial behavior are unchanged. Consolidated chart mobile breakpoints, removed conflicting workspace column overrides so the existing resizer remains authoritative, aligned instrument/price/actions and standardized timeframe controls. Existing light/dark chart rendering is retained.

Verification: frontend ESLint and Vite production build passed; all 36 API/auth Node tests passed. Initial lint found an existing ignored generated preview in frontend/target; ESLint now excludes that build-output directory alongside dist. No new implementation-mirroring CSS tests were added. Rendered desktop/mobile and real keyboard/device acceptance remain pending until actual inspection; automated checks do not establish visual acceptance. Broader T1-04 funds/holdings/valuation and deployment gates remain open; the previous archive does not certify these UI changes.

Rendered review attempt: the current computer-use skill was read and native host initialization/listing attempted. It returned `Computer Use native pipe is unavailable` (Windows error 2: pipe not found), before any browser window could be inspected. No app navigation, authentication, safety-barrier bypass or rendered acceptance occurred.

### 2026-11-11 - Approved T+1 roadmap started (current)

Current V2 release scope was selected for the deployment audit; the full Kite-referenced catalogue remains approved for later phases. `deploy/KITE_RELEASE_AUDIT.md` records official reference links, current evidence, simulation differences and deferred features. Verdict remains not deployable.

T1-04 worker/API unit implemented: bounded 20-obligation scheduled batches; shared-user-lock financial processing and compatibility-balance refresh; separately committed retry observations in additive `20261111_settlement_checks.sql`; persistent t1-settlement health and Admin Operations response fields. Missing calendars stay pending and missing opening markers fail without initializing accounts. New read-only authenticated GET endpoints `/settlement/summary`, `/settlement/history?page=1` (50 rows) and `/settlement/holdings` derive account ownership from the session and do not create opening records. Test profile disables background scheduling. Matching managed/runtime/fresh schemas and regular-account deletion cleanup are updated.

Verification: **81 selected backend tests passed, 0 failures/errors/skips** (target/settlement-worker-tests-v2.log), including seven new worker/status checks. An initial candidate-order timestamp tie failed a fairness test; explicit unchecked-first ordering fixed it. HTTP ownership/pagination acceptance, actual funds/holdings/history presentation, restricted-credit portfolio enrichment and Admin rendered health still remain. Full MySQL migration/restore/restart, release packaging and device/rendered/provider gates remain open. No existing user database was migrated and no public deployment occurred.

T1-03 continuation: `SettlementAccounting` now integrates equity, conditional orders, options, paper tickets/financing, IPO applications/transfers, wallet and Admin adjustments; registration alone still establishes initial virtual cash. Compatibility balance is settled plus reusable free credit. Withdrawals/IPO/financing collateral require settled cash. Equity BTST credits retain delivery dependencies; option sale/expiry credits are restricted pending FNO settlement. Financing closes credit net proceeds pending equity settlement; fees/losses consume eligible cash before recording uncovered debt through one idempotent charge event. Regular-account deletion removes settlement-owned rows. All changes remain local; no existing database was migrated.

Initial integration regression had 11 failures across 124 tests. Wallet concurrency passed alone, and inspection found reused H2 context names with retained non-JPA ledger tables. Test contexts now use distinct database names and financial fixtures clean ledger rows. Tests no longer overwrite a captured balance directly or expect restricted proceeds in available cash. New real-route tests verify BTST buy/withdrawal blocking, delivery links and pending-source conditional refunds; a charge retry test verifies no second charge after a deposit. The next combined run passed 176/177; its sole remaining failure was another outdated OCO cash assertion, now corrected. Full selected regression now passed **177 tests, 0 failures/errors/skips** (target/settlement-routes-tests-v3.log). This verifies the T1-03 integration unit, not MySQL/full-release/device acceptance. Scheduled settlement, funds/holdings/status UI and final release verification remain unfinished.

The approved scope is T+1 with restricted BTST, then the phased Kite-referenced catalogue: AMO, baskets, alerts/depth, reports, futures, covered/cash-secured option writing, verified-data funds/SIPs, ETFs and bond trading. Native Codex Goal is active; `AGENTS.md` and `WORK_QUEUE.md` retain continuation rules and the exact next action. Continuation is subject to platform capacity; no quota bypass or guaranteed automatic quota-reset wakeup is claimed. All open user dependencies remain exclusively in `DEPLOYMENT_CHECKLIST.md`.

T1-01 complete: versioned, exchange/segment-specific operator-reviewed CSV calendars; separate trading/settlement flags; contiguous date and provenance/checksum validation; serialized, transactional, audited activation; fail-pending unknown coverage. T+1 selects the next explicitly open settlement day with disclosed 18:00 IST simulation cutoff. No real calendar was guessed/imported. Operator steps are in `deploy/SETTLEMENT.md`, included in the release allowlist. Managed installations need `20261111_settlement_calendar.sql`.

T1-02 complete as an internal foundation: additive `20261111_settlement_accounting.sql` creates accounts, obligations, lots and funding allocations. `SettlementOpening` requires an enclosing transaction and the shared user-row lock. It captures existing free balances (including negative balances), settled opening holdings and separately held active reservations without changing balances/holdings/history or inventing cost basis. Capture is atomic and idempotent across concurrency/restarts. It is not invoked automatically or exposed publicly.

`SettlementFunds` adds settled/reusable/restricted/reserved buckets, source allocations, partial spend/refund, settled-only withdrawals and idempotent reviewed-calendar settlement. Consumed/reserved proceeds are never credited again; refunds follow their original source's current settlement state. `SettlementLots` adds T1 delivery lots, settled-first disposal and immutable sale classification without changing historical weighted-average P&L. Restricted BTST credits can depend on their original delivery obligations, so missing earlier purchase coverage cannot be bypassed by a covered later sale date. `20261111_settlement_journal.sql` adds accounting/source/lot journals and delivery links; matching runtime/fresh baselines and managed-schema validation are updated. These services require an enclosing shared-lock financial transaction. The earlier standalone-foundation boundary is superseded by T1-03 integration evidence above. T1-04 jobs/UI and final MySQL/release checks remain pending; financial routes now use the ledger.

Verification: latest calendar/funds/lots/opening/fresh-schema targeted Maven run passed **73 tests, 0 failures/errors/skips** (`target/settlement-ledger-tests.log`): 24 funds, 13 lots, 11 opening, 20 calendar and 5 schema tests. Tests cover reservation/source preservation, partial spending/refunds, ownership, double-spend concurrency, concurrent settlement/refund and settlement retries, unknown/corrected coverage, BTST delivery dependencies, restart and transactional failure/retry. An initial funds run exposed zero-scale DTO differences; normalized aggregate money to two decimal places and reran successfully. Previous release packaging tests remain **8/8**; packaging code is unchanged this unit. No existing user database was changed. The prior 57A7Tw archive certifies the earlier paper-ticket snapshot only; latest source requires new MySQL/full-release validation after integration. Device/rendered/provider launch gates remain open; deployment checklist steps 5/6 indicate rerun required.

### 2026-11-11 - Deployment checklist and screenshot paper-ticket expansion (previous candidate)

Created `DEPLOYMENT_CHECKLIST.md` first as requested: ordered deployment steps plus the single open user-input/action table. Future tasks must add dependencies there immediately and remove completed requests only after recording evidence here. Credentials remain in private configuration, never the checklist. Deployment docs and README link to it; release archives include it and `deploy/PAPER_TRADING.md`.

Implemented screenshot-based Quick/Regular/MTF/Iceberg/Cover ticket controls for virtual trading: exchange-specific snapshots, units/amount/lots, Market/Limit/SL/SL-M, DAY/IOC/GTT, protected buy ceilings, linked percentage/absolute-price OCO exits, sequential whole-lot iceberg children, pending replacement/cancellation and responsive light/dark presentation. The shared ticket is used for equities and NIFTY options; MTF/Cover remain equity-long simulations. Positions can request full closure or sell selected units. Linked exits share one unit reservation; independent trigger transactions preserve OCO cancellation if a later fill is rejected. Legacy and new reservations coordinate under the same user lock.

Added server preview/submission/activity/replacement/closure APIs under `/orders/paper`, policy fingerprints, per-fill simulated fees and separate financing/principal/interest ledgers. Defaults: up to 2x equity buying power, 12% yearly MTF simple interest on IST calendar-day closing principal, 25% maintenance equity and 0.03% turnover fee capped at INR20 per fill/iceberg child; statutory taxes excluded. Margin contributions round up to paise. Cover attempts closure at 15:20 IST and cannot enter afterwards. Negative balances block new buys, not deposits/closure. Interest catch-up is idempotent/bounded; unavailable marks/accrual make aggregate value unavailable. Existing legacy routes/historical fee-free trades remain compatible; new ticket charges are explicit. Portfolio/Funds/Orders and owner Operations show financing, reservations, fees, ledger and persistent job health. Regular-account deletion removes the new account-owned rows.

Managed existing databases need reviewed `20261111_paper_ticket.sql` (four tables) before starting this build. Fresh baselines and runtime/test schema support it. No user database migration or account action was performed. Option execution/expiry now uses the shared injected Clock for consistent lifecycle testing. Simulation rules/allowed combinations are documented in `deploy/PAPER_TRADING.md`; these are not actual broker margin/eligibility/fee claims.

Verification: full `node scripts/build-v2-release.mjs --require-containers` exited 0. Maven **207 total, 0 failures/errors, 11 opt-in skips**; the separate MySQL 8.4 phase passed **all 3 tests without skips**, including OCO trigger handling and financed-position repayment. New suites contain **20 paper lifecycle/API/migration tests** and **2 policy tests**. Frontend **36 API/auth tests**, lint and production build passed; release tooling **17 passed**. MySQL restore/restart snapshots now also retain paper tickets, financing and principal-ledger records. HTTPS/SPA/Secure-cookie/CSRF acceptance and project cleanup passed; evidence `target/release-checks/smoke-nkOjbX/result.json`.

Validated current candidate: **`target/releases/InvestSmart-v2-57A7Tw.tar.gz`** plus `.sha256`. Archive digest, **38 payload checksums**, extracted artifact inspection, included deployment checklist/new migration and offline migration catalog (**12 entries**) passed. Evidence: `target/release-checks/paper-ticket-archive-verification.json`. readiness.json has releasePackageValidated=true and productionLaunchApproved=false. Earlier HX9ZWk/IrlSev archives do not certify this final source. Latest ticket rendered review is pending; automated checks do not establish visual/device acceptance. Remaining user dependencies are maintained only in `DEPLOYMENT_CHECKLIST.md`; its production launch steps remain open. No live order, email, owner promotion or public deployment occurred.

### 2026-11-11 - Required-container release and archive verification passed (previous candidate)

Completed the approved Docker readiness fix and end-to-end packaging. Both runners now share `scripts/docker-readiness.mjs`: a ten-second `docker version` server query requiring a Linux engine. Docker Desktop's `docker info` diagnostic call was hanging even though engine version/ps requests worked; no engine restart, Windows security change or weakening of container validation was needed. Four regression tests cover Linux success, timeout, unavailable CLI/server and invalid/Windows engine output without exposing raw diagnostics.

`node scripts/build-v2-release.mjs --require-containers` exited **0**. Full Maven verification: **184 total, 0 failures/errors, 10 opt-in skips**; the separate MySQL 8.4 phase then ran its **2 tests successfully with no skips**. Frontend **33 tests**, ESLint and `/api` production build passed. Release tooling **17 tests** passed. Packaged images, fresh MySQL 8.4 schema, authenticated funds/session behavior, synthetic accounting/encrypted-factor backup/restore, database/application restart, trusted private-CA HTTPS, SPA routes, Secure CSRF cookie and missing-CSRF rejection all passed. Final project containers/volumes were confirmed removed. Evidence: `target/release-checks/smoke-1YPhrM/result.json` and `containers.log`.

Current validated candidate: **`target/releases/InvestSmart-v2-HX9ZWk.tar.gz`**, with adjacent `.sha256`. Archive digest, all **35 payload checksums**, extracted portable artifact inspection and the packaged offline migration catalog (**11 entries**) passed. Evidence: `target/release-checks/validated-archive-verification.json`. Its readiness report records mysql84AndContainers=passed, releasePackageValidated=true and **productionLaunchApproved=false**. Earlier u4HFid/ziUach candidates are superseded. GitHub CI itself has not run; the same required-container command passed locally.

This completes local container/package validation, not all F/G launch acceptance. Remaining: real Android/iPhone and protected owner Admin/rendered checks, intended-owner bootstrap/MFA, production sender/inbox/final HTTPS/hosting, and Upstox profile/entitlement/display authorization. Existing local accounts/database were preserved; no public deployment, owner promotion or external email was performed.

### 2026-11-11 - Local container acceptance passed; final packaging retry blocked (superseded)

The user completed local WSL/Docker installation. Read-only checks confirmed WSL 3.0.5, Docker Linux engine 29.9.3 and Compose 5.7.0. No installer/authentication dialog was automated. The earlier installation blocker is superseded.

Standalone `node scripts/check-v2-containers.mjs` passed on November 10, including fresh packaged runtime images, MySQL 8.4 schema validation, **2 opt-in MySQL tests passed without skips**, synthetic equity/options/IPO reservation and encrypted-factor backup/restore, database/application restart, trusted private-CA HTTPS, SPA routes, Secure CSRF cookie and missing-CSRF rejection. Its result is `target/release-checks/smoke-l7uyiv/result.json`; project containers/volumes were independently confirmed removed. The regular Maven suite passed **184 total, no failures/errors, 10 skips**; frontend **33**, release tooling **13**, lint and production build passed during required-container release attempts.

Fixed smoke-runner portability issues uncovered by actual execution: use engine-inspected port bindings instead of Compose's `:0` output; attach the synthetic database to a host-reachable test network while publishing only loopback; check private-CA HTTPS through Node with explicit certificate/hostname validation instead of Windows curl's unavailable private-CA revocation status; request a ten-second backend stop and include safe process error codes in failures. Production database networking and Windows trust/security settings were unchanged. GitHub CI has not run.

The final integrated `--require-containers` command has **not** passed: one repeat completed its MySQL tests but stalled at backend stop and cleaned up; a subsequent repeat passed backend/frontend/tooling checks but found Docker unavailable before container validation. Starting the installed Docker Desktop app did not immediately restore engine readiness. The archive `InvestSmart-v2-ziUach.tar.gz` remains the latest candidate, with releasePackageValidated=false; no new validated archive was emitted or inferred from the earlier standalone pass. A Docker Desktop status request is pending. Retry `node scripts/build-v2-release.mjs --require-containers` when the engine is responding; mark its archive validated only after command success and checksum/portable inspection.

Public launch is still unapproved. Remaining external gates: real Android/iPhone and owner Admin/rendered acceptance, intended-owner bootstrap/MFA, production inbox/final HTTPS/hosting, and active Upstox profile/entitlement/display permission. The earlier profile 401/contract discovery 200 results remain unchanged. Local accounts/database were preserved; no owner promotion, email submission or public deployment occurred.

### 2026-11-10 - F/G acceptance follow-up (previous candidate)

Implemented the approved current-V2 scope; futures, charges, partial fills, margin/writing, brackets/OCO and other index-option underlyings remain deferred. Option execution now rejects missing fetch timestamps, timestamps older than 60 seconds and timestamps over five seconds in the future with a safe 503; rejected market orders do not spend cash, and pending reservations remain intact until fresh executable depth arrives, cancellation or expiry. This is application fetch-age validation, not certification of an upstream exchange tick's age. Added regression tests for those cases and complete lost-phone recovery through replacement enrollment and successful sign-in while the old credential/display are rejected. No public API or schema changed.

Verification: 23 focused phone/option tests passed. Full release command succeeded: Maven **184 total, 0 failures/errors, 10 skipped** (8 live-provider and 2 MySQL-only tests); frontend **33 passed**, lint and `/api` production build passed; release tooling **13 passed**. New candidate: `target/releases/InvestSmart-v2-ziUach.tar.gz`; final extraction, all **35 payload checksums** and portable artifact inspection passed, with archive digest written alongside it. Changed-file whitespace checks pass. The older u4HFid archive predates this execution fix. Docker remains unavailable, so the new candidate is not container-validated or approved for public launch. See `deploy/V2_ACCEPTANCE.md` for the F/G device/Admin/trading evidence matrix and local setup instructions.

Read-only active-provider check: NIFTY option-contract discovery returned HTTP 200/success and **2,066 contracts**, but `/v2/user/profile` still returned **401**. No order was placed and no raw token/profile was printed or stored. Sanitized evidence is under ignored `target/device-acceptance/provider-readonly.json`. Account-specific exchange/derivatives entitlement and display/redistribution authorization remain open despite contract access.

Selected container validation on this Windows PC as requested. Windows 11 Home x64/hypervisor was detected; WSL and Docker are absent. The built-in WSL bootstrap did not install it. Automatic approval review rejected the command to download/verify/run Microsoft's WSL MSI as **"blocked by policy"** before execution; no further reason was supplied. No alternate installation route was attempted after rejection. The user must finish WSL/Docker setup and any administrator prompts/restart, then run `node scripts/build-v2-release.mjs --require-containers`. A setup-status request is pending. Existing accounts/database were preserved; no owner promotion, mail submission, authentication prompt automation or new rendered/device acceptance occurred.

Next: user-completed local WSL/Docker setup, isolated MySQL 8.4/HTTPS/restore validation, real Android/iPhone and protected Admin/rendered acceptance, verified owner bootstrap/MFA, production sender/inbox/final HTTPS configuration, and Upstox profile/permission resolution. F/G remain partial only where acceptance evidence or those external dependencies are still missing; do not mark them complete wholesale.

### 2026-11-10 - Interrupted V2 release preparation completed (previous candidate)

Completed the release-tooling work shown in the user's interrupted-connection screenshot. V3 controller/service beans are disabled by default and covered by a fresh-schema endpoint-registration test. Production startup and preflight enforce non-root MySQL, required provider/mail settings, separate signing/encryption keys, secure cookies, managed schema and exact HTTPS origins. No local account or database was changed.

`scripts/build-v2-release.mjs` now verifies backend/frontend/tooling, stages an explicit allowlist, rejects symlinks/private configuration, checks extracted-layout artifacts, generates payload/archive SHA-256 checksums and emits readiness.json. Runtime Dockerfiles consume built artifacts. `deploy/RELEASE.md` documents fresh production initialization while preserving local data, checksums, private configuration, owner bootstrap and the packaged offline migration CLI. Existing-installation migration acceptance remains separate. CI workflow `.github/workflows/v2-release.yml` requires container validation before uploading a candidate; the workflow has not been executed here.

Added `scripts/check-v2-containers.mjs` and synthetic Compose/SQL fixtures for MySQL 8.4, authenticated funds/session behavior, equity/options/IPO reservations, encrypted phone-factor retention, backup/restore, database/application restart, trusted Caddy HTTPS, SPA deep links and CSRF rejection. It uses generated secrets, a dedicated loopback database and unique Compose project; cleanup is limited to that project's volumes. This code is prepared but its Docker runtime behavior is unverified because Docker is unavailable. Direct execution exits 1 with an explicit unavailable/not-performed message, without starting services. Do not mark these checks passed.

Verification: complete release command exited 0; Maven **181 total, 0 failures/errors, 10 skipped** (8 live-provider and 2 opt-in MySQL tests); frontend **33 passed**, ESLint and `/api` production build passed; release tooling **13 passed**. Archive `target/releases/InvestSmart-v2-u4HFid.tar.gz` was generated. SHA-256 archive digest, **35 payload checksums**, extracted-package artifact inspection and packaged offline migration catalog (**11 entries**) passed. Private logs are under ignored `target/release-checks`. Readiness explicitly records mysql84AndContainers=not-run, releasePackageValidated=false and productionLaunchApproved=false.

Next: run `node scripts/build-v2-release.mjs --require-containers` on a Docker-capable host/CI, then complete real Android/iPhone and remaining rendered UI/Admin acceptance, intended-owner verification/bootstrap/MFA, production inbox/HTTPS/hosting checks, and account-specific Upstox entitlement/display authorization including the historical profile 401. Earlier local mail/login/Windows Hello user acceptance remains valid within its recorded scope. No authentication prompts were automated and no visual acceptance, public deployment or provider authorization is newly claimed.

### V1 - Core investing application

- [x] V1 is complete per the user. Treat this as user-confirmed; no separate V1 acceptance audit is recorded here.

### V2 - Market data, history, and portfolio intelligence

#### A. Market overview

- [x] Navigation is consolidated into Dashboard, Markets, Heatmaps, Portfolio, Orders. Markets contains Explore/Watchlist/IPOs/Bonds tabs; Portfolio contains Holdings/Insights/Funds tabs; Orders contains Trades/IPO applications. Old URLs redirect without deleting account records; chart navigation preserves listing identity and return destination.
- [x] Dashboard's My holdings preview uses the signed-in user's holdings, not the shared stock catalog. Full company details/trading use one chart workspace; duplicate stock pages/charts and unused styles are removed.
- [~] Visual acceptance is partially observed as of 2026-11-10: native Chrome URL verification and browser access succeeded without bypassing the earlier barrier. Dashboard and Markets were visually inspected at desktop and 400px responsive width; Account was inspected on desktop and Login at desktop/400px. Full-app review, protected dialogs, Admin and real-device passkey flows remain pending; the browser session subsequently returned to Login. See the current acceptance note for exact scope. Automated checks do not count as visual acceptance.
- [x] 2026-11-10: Recovered native Chrome access, verified `localhost:5173` through the address bar/accessibility state, and observed real rendered desktop/400px responsive screenshots. The historical browser-access blocker is resolved for the native path; the browser connector itself still reports its host unavailable.

- [x] `MarketDataProvider.getQuotes()` and `GET /market/quotes` return the starter INFY, TCS, and RELIANCE quotes.
- [x] Market page shows quote cards, loading/error/empty states, and sidebar navigation.
- [x] Dashboard and Market Overview show NIFTY 50 and SENSEX live index snapshots with refresh, unavailable/stale states, and links into index charts.
- [x] Index previous-close mapping falls back to the most recent daily candle before the Upstox quote session when OHLC and full-quote fields omit it; missing/unreliable history remains unavailable.
- [x] Dedicated dark `/heatmaps` page appears below Markets in the main sidebar, with Indices, NIFTY 50, and paginated/searchable All stocks (full NSE equities). Named tiles open charts and show unavailable/stale states.
- [x] Heatmaps defaults to NIFTY 50. Equal-area subdivision and ResizeObserver fit all 50 tiles into the available viewport; compact symbol/change tiles expose full company details through hover/focus/touch and chart labels. Fullscreen, URL filters and return destination are retained. Desktop/phone geometry tests pass; browser visual acceptance remains pending.
- [x] Removed the duplicate upper index strip from the graph workspace; retained the four-index sidebar and moved Upstox attribution into the chart footer.
- [x] Markets shows ten Standard stocks and quick watchlist-add buttons; stock charts support the same add/saved state. Dashboard My holdings/Watchlist and chart Watchlist previews show three entries with full-list links.
- [x] Graph sidebar has compact Indices/My watchlist tables and selected-instrument details. + Add stock opens company/ticker search with chart links, listing selection, pagination, and saved-stock fallback.
- [x] Graph workspace supports persistent light/dark themes; chart colors update in place without changing timeframe or zoom. Responsive graph/sidebar and mobile order-sheet styles are implemented; visual review remains pending.
- [x] Graph and Portfolio Buy/Sell buttons open a shared paper market-order window with quantity, exchange, listing price, cash, holdings, and estimated total. Indices are labeled as not directly tradable.
- [x] Graph watchlist reloads membership immediately on add events, independently refreshes prices, shows the newest three entries, and protects against out-of-order responses. Manual refresh forces fresh prices.
- [x] Market/Watchlist/Graph share paginated NSE/BSE company search, grouped by ISIN. Exchange-specific prices/selection appear in add/trading dialogs. One watchlist row per company follows the chosen listing; stock/holding/order IDs remain listing-specific.

#### B. Authorized live market data

- [x] Upstox provider is implemented and selected; the user confirmed Upstox is working.
- [x] API token is configured through `UPSTOX_API_TOKEN`. Never put token values or other secrets in this file, source control, logs, or agent messages.
- [x] Individual quote and history lookups resolve NSE symbols through Upstox instrument search rather than a three-symbol mapping.
- [x] Dashboard, Portfolio and Watchlist load saved account data independently of quote refresh, refresh on open/every 60 seconds/manually, and react to account/watchlist changes. Batches of up to 500 instruments skip quotes fresher than 60 seconds unless forced, retain saved values on misses, and report staleness. Requests time out at 10 seconds with one 429 retry. Live NSE/BSE refresh persistence, cache skipping, both exchange statuses, and index previous-close availability passed opt-in checks on 2026-11-02; browser interaction remains unverified.
- [x] Added tests for quote persistence, cache skipping, missing symbols, documented response-key mapping, 429 retry/saturation, and non-429 HTTP failure. Backend suite passes (27 tests); frontend lint and production build pass as of 2026-10-26.
- [x] Buy/sell methods lock the user row, load the chosen stock, and check that listing's NSE/BSE session and freshly fetched quote before mutating balances/holdings/orders. Only `NORMAL_OPEN` is accepted; closed sessions return 400, unavailable status/quote returns 503. Sales require holdings of the selected stock ID. Both live exchange statuses and matching session guards passed read-only checks; no order was placed against the user's account.
- [ ] Reconfirm permitted symbols, exchanges, data display, and terms against the active Upstox account before deployment.

#### C. Historical data and charts

- [x] Historical prices have a JPA entity/repository and a market history API.
- [x] Market chart and stock detail pages support 1D, 1W, 1M, 6M, and 1Y ranges.
- [x] Daily history is cached only when it covers the requested window; empty/partial refreshes preserve existing cache. Intraday 1D/1W bypass persistence; 1D tries current-day candles then recent history. Provider requests validate HTTP/payload errors and avoid logging raw responses. Live NIFTY 50/SENSEX candles passed all five ranges on 2026-11-02. Exchange-separated caches and partial-range preservation pass automated tests. Provider account display terms remain a deployment check.

#### D. Portfolio intelligence

- [x] Portfolio responses include average buy price, invested amount, realized/unrealized P&L, and cash/holdings allocation.
- [x] Sector metadata and educational stock/sector concentration insights are implemented.
- [x] Settled accounting choice: weighted-average cost. If order history cannot be reconciled with current holdings, performance figures should be unavailable rather than fabricated.
- [x] Settled concentration thresholds: flag a stock above 25% of holdings and a sector above 40%. Missing sector metadata is unclassified and does not create a sector flag.
- [x] Missing/nonpositive holding prices no longer crash valuation or become fabricated zeros: cash and known cost data remain available; aggregate value, allocations and unrealized P&L are unavailable when prices are incomplete. Holdings include listing metadata for exact chart navigation; zero-quantity holdings do not invalidate valuation.
- [x] Virtual Funds are part of Portfolio. Deposit/withdraw use transactional user-row locking shared with orders, validate currency precision, and refresh portfolio balances. H2 concurrency and authenticated API tests cover double spending, funds/portfolio consistency, and invalid amounts.
- [x] Backend tests pass (13 tests), and frontend lint/build pass as of 2026-10-26. Rerun after relevant code changes.

#### E. IPO practice and bond discovery

- [x] Markets IPO tab uses authenticated Upstox open/upcoming/closed/listed discovery, Mainboard/SME filters, details/prospectus links, provider timestamps, stale/error/empty states and saved interests.
- [x] Virtual IPO applications reserve available cash at the upper band, validate dates/IST daily window/whole lots/minimum quantity, serialize on the same user lock as equity orders/funds, and return the existing user+IPO application on retries. Cancellation refunds once. No real broker/UPI transaction is made.
- [x] Scheduled startup/hourly catchup resolves after 18:00 IST on the published allotment date. One persisted server draw uses the explicitly educational overall-demand proxy `min(1,1/S)`; actual retail odds/category rules are not claimed. Positive subscription is required; missing data pauses with cancellation available. A selection receives the published minimum quantity. Cut-off price is used when positive; missing cut-off uses a disclosed upper-band assumption; an excessive cut-off defers settlement. Unused funds are refunded once.
- [x] Verified listed ISIN/date/status transfers the allocation to the matching NSE/BSE holding and creates one IPO-origin BUY cost record without another cash debit. Orders shows applications/reasons/cancellation; Portfolio exposes reserved funds, total cash and unlisted cost. Unlisted market valuation/P&L remains unavailable.
- [x] Bonds supports government securities, state securities, Treasury bills and corporate N-series debt through a separate NSE catalog, search/pagination/details, on-demand price and saved tracking. Equity search explicitly excludes debt types. No bond orders or calculated yield are offered.
- [x] Versioned ISIN bond reference pipeline has eleven verified snapshots: one NHAI issue (NSE 2026-10-26) and ten government securities (CCIL 2026-05-29), verified 2026-11-03. Optional validated external CSV overrides, Verified terms filtering and coverage counts are implemented. Coverage is limited to matching catalog ISINs; unknown face value/rating and other fields stay unavailable. Names are not parsed into terms and coupon is not yield. Extend only from verified official sources with dates.
- [x] Added manual forward migration `20261103_investments.sql` for applications, saved interests and order source, plus local schema-update support. Explicit checksum migration tooling is now provided; production schema reconciliation remains an operator step.

#### F. V2 user/admin access and launch polish

- [x] Responsive implementation: shared safe areas, mobile touch/form sizing, measured navigation, keyboard-aware dialogs, short-screen heatmaps, chart controls and phone lifecycle fixes are implemented. Full Maven verification, frontend tests/lint/build and release artifact inspection pass on 2026-11-10. Real-device and rendered visual acceptance remain separate pending gates.

- [~] Mandatory regular-user phone verification is implemented (six digits/30 seconds, dedicated phone passkey, same-phone login and recovery). Full Maven verification and frontend/release artifact checks pass; isolated MySQL 9.7 rehearsal passed. Real phone/visual acceptance and production gates remain pending. See the current responsive/phone notes.

- [x] Shared UI tokens use 8px panel/dialog corners and 6px controls, neutral surfaces, consistent button states and readable Dashboard typography. Dashboard retains a compact four-index strip (20px values); Markets retains its larger cards. Removed obsolete starter/auth CSS; Charts/Admin load separately to reduce initial JavaScript.
- [x] Public signup requires verified email before the first session; verification is not resent during normal login. Verification/resend (24-hour) and recovery (30-minute) links are single-use, with purpose-specific expiry errors and safe SMTP failure status; delivery failures never auto-verify an account. HttpOnly cookie sessions, CSRF, password reset/change, session listing/revocation, and Account & security are implemented.
- [x] Optional trusted-device unlock uses platform WebAuthn passkeys with required user verification, encrypted one-use challenges and public credential storage only. Locking preserves the active session; sign-out revokes it. Passkey enrollment is limited to verified regular users and cannot replace owner/admin password plus authenticator MFA. Local email/password and Windows Hello/passkey workflows are user-confirmed working on 2026-11-10 after the fixes below. This is user acceptance, not agent-observed device testing; exhaustive expiry/revocation/reset/fallback scenarios and owner Admin acceptance remain separate pending launch checks.
- [x] 2026-11-10 user-confirmed local acceptance: verification mail receipt, email verification, login with the new password and the regular-user passkey workflow are confirmed working. Owner Admin acceptance is not covered by this confirmation.
- [~] Owner-only Admin has exactly five sections: Overview, Users, Security, Operations and Audit. Overview includes total/verified/suspended users, registrations in the last seven days, unresolved alerts and pending mail. Users supports name/email search, registration/verification state, portfolio/trade/application review, reasoned suspend/reactivate, all-session and single-session revocation, forced password reset (blocks login until reset), rate-limited verification resend, and audited additive/subtractive virtual-balance adjustments. Sensitive actions require recent password+authenticator confirmation. Security supports rule-based login, recovery, rejected-order and unusual Admin activity alerts with notes, status review and automatic 24-hour archival. Operations shows order rows/rejected attempts, safe mail delivery status, quote freshness/Upstox telemetry and IPO settlement/job status; provider-sourced IPO/bond listings support audited availability overrides and validated IPO timeline overrides. Due, unresolved IPO settlements can be retried with fresh provider data through the existing guarded transaction; resolved outcomes cannot be edited. Provider/job latest health timestamps and cumulative failure counts persist in `admin_runtime_health`. Audit is read-only in the UI. Rejected attempts are retained as security events, not order rows. Permanent user deletion remains available only for regular accounts, with exact-email confirmation, reason and audit history; account credentials/data are removed while historical events/audits are retained without account links. Generic user CRUD is otherwise unavailable. User DTOs omit passwords and secrets.
- [x] Owner bootstrap is an explicit non-web server CLI operation after email verification. A separate owner registry rejects legacy ADMIN role values until server authorization. Admin password+TOTP enrollment/login, RFC-vector checks, replay prevention, encrypted authenticator secrets, ten single-use recovery codes and five-minute action confirmation are implemented. Owner self-suspension and browser role changes are unavailable.
- [x] Persistent rule-based alert inbox and independent failure logging cover repeated account/source login failures, access denials, MFA failures, repeated verification/recovery requests, rejected orders and unusual Admin activity; configurable throttling, deduplication/reopening, review audit, masked source attribution and retention are implemented. No external alert messages or automatic account suspension. Suspended users cannot mutate funds/orders/IPO/watchlists; settlement/reconciliation preserves existing obligations.
- [~] SMTP transport, encrypted outbox, immediate delivery attempts, bounded retries and safe failure diagnostics are implemented. Local mail receipt and verification are user-confirmed working. Production sender/domain configuration, HTTPS public verification URL and production verification/recovery inbox receipt remain operator acceptance steps. No external email was sent by the agent.
- [~] Production profile, account-security, trusted-device, Admin V2, listing-override and runtime-health forward migrations, fresh MySQL baselines, schema and DEPLOYMENT.md are provided. Inspect/reconcile existing tables and columns before applying `20261105_admin_v2.sql`, `20261104_admin_listing_overrides.sql` or `20261104_admin_runtime_health.sql`; real-MySQL upgrade/restore rehearsal, public hosting/domain/TLS, provider display permissions and browser/device acceptance remain launch gates. SMTP receipt and authorized Upstox display scope remain unverified. V3 stays deferred.

#### G. Expanded paper trading (user-requested November 10)

- [x] Markets Options discovers NIFTY 50 CE/PE contracts, expiries, strikes, lot/freeze quantities and provider bid/ask/LTP/OI. NIFTY chart links to the chain. Primary navigation remains five pages; Options is a tab within Markets, Portfolio and Orders.
- [x] Premium-funded long option buys and selling owned contracts, lot validation, market/limit/protective sell-stop orders, cash/share reservation, request-id deduplication, cancellation and persisted positions/history are implemented. No naked option writing/margin; simulated equity MTF/Cover is implemented separately below.
- [x] Equity trade tickets support limit and protective sell-stop day orders; pending entries are cancellable in Orders and completed fills use ordinary trade history. Normal sells respect reserved shares. Transactional execution rechecks listing identity, market session and limit price.
- [x] Portfolio totals include saved option bid marks/costs/P&L and all pending-order reserved cash. Contract expiry refunds pending buy reservations and settles long positions from the exact dated underlying daily close after 18:00 IST using a disclosed educational proxy. Missing settlement data makes valuation unavailable and leaves retryable positions.
- [~] Automated provider/accounting/concurrency/expiry checks pass, including fresh MySQL 8.4 runtime/restore acceptance. Provider authorization and rendered/device acceptance are tracked in DEPLOYMENT_CHECKLIST.md. Polling is simulated in bounded 30-second batches. Full child partial fills, actual statutory/broker fee schedules, futures, naked writing/option margin and other option underlyings remain deferred; simulated equity financing/fees, iceberg parent partial completion and linked OCO exits are now implemented.
- [x] Option fills reject missing/stale/future fetch timestamps; regressions confirm unchanged cash/positions on rejection and retained pending reservations. Read-only NIFTY contract discovery returned 200 with 2,066 rows; profile 401 and display authorization remain unresolved. Complete replacement-phone enrollment/sign-in is covered by signed-fixture integration tests, separately from real-device acceptance in F.

#### H. Screenshot order-ticket expansion (authorized November 11)

- [x] Quick/Regular/MTF/Iceberg/Cover controls, separate limit/trigger fields, buy price protection, DAY/IOC/GTT, percentage/absolute OCO exits and server previews are implemented.
- [x] Transactional reservations, sequential children, idempotent fees/interest/repayment, financing risk/closure and negative-balance handling are implemented, with new schema/migration and equity/option/MySQL regression coverage.
- [x] Portfolio, Funds, Orders and owner Operations expose the new activity; runtime image/restore/HTTPS checks and archive inspection pass for the current candidate.
- [~] Latest screenshot-ticket rendered/mobile acceptance and broader launch requirements remain open in DEPLOYMENT_CHECKLIST.md. No real-device or authenticated Admin acceptance is inferred from tests.

### V3 — grounded AI learning assistant: current checklist

**Updated 2026-11-23:** V3 development now precedes deployment; the earlier "deferred until V2 acceptance" sequence is superseded by the owner's transition decision. AI remains **optional P2**. This section describes AI only; completed non-AI V3 work is listed in the current progress summary at the top.

- [x] **Learning documentation:** AI_LEARNING_ROADMAP.md created and linked; 23 study cards, resources, exercises, progress tracker and change log are present. Personal learning completion remains unconfirmed.
- [x] **Existing-code review:** inspected ChatController, LearningAssistant, LearningChat and the default-disabled feature flag; findings are recorded in the learning guide and feature inventory. This marks the inspection complete, not the AI feature.
- [~] **Request/response DTOs and conditional chat API:** definitions and validation exist; the feature stays disabled and enabled-runtime acceptance is pending.
- [~] **Relevant evidence/context:** portfolio, market-quote and optional RSS context assembly exists; coverage, freshness, provenance and grounding still require validation.
- [~] **Model integration:** one synchronous configurable OpenAI-compatible HTTP request is implemented; provider/model choice and reliable enabled behavior are not accepted.
- [~] **Limits/errors/privacy:** request limits, timeout/error handling and consent controls exist; AI-specific privacy/logging/security acceptance is incomplete.
- [~] **Chat UI:** suggestions, loading/error states, sources and disclaimer exist in LearningChat; the component is not registered in current App routes and rendered acceptance is pending.
- [~] **Financial guardrails:** instructions, source-ID filtering and a directive regex exist; these do not prove factual accuracy, injection resistance or safe financial explanations.
- [~] **Advanced agent:** staged scope AI-1..6 is approved as a major planned feature, with only AI-1 authorized for implementation now. No live portfolio tools, multi-step execution or confirmed-action adapters are enabled; AI-2 requires owner approval.

#### Historical AI review — 2026-11-04 (original conclusions preserved)

The review below describes its dated snapshot. Its startup-failure statement is historical; today's default-disabled feature does not certify enabled startup. Original unchecked goals are retained as historical acceptance criteria, not the current implementation checklist.

**Review correction (2026-11-04):** Partial Learning implementation now exists in the working tree, including UI/API/context/model integration. The unchecked items below remain acceptance goals, not an assertion that their code is absent. Its service construction currently blocks application startup; see the latest review note. V3 is not accepted for release.

- [ ] Add sourced company/IPO/bond news and rule-based context before model-generated explanations. Show publication time and sources; never invent news or investment reasons. IPO simulator reasons are already deterministic and auditable in V2.

- [ ] Define safe chat request/response DTOs and a bounded chat API.
- [ ] Build a context service that collects only relevant user portfolio, quote, history, and news context.
- [ ] Connect an AI model and return concise educational responses grounded in available context.
- [ ] Add request limits, error handling, and logging that excludes secrets and sensitive data.
- [ ] Add chat UI, suggested questions, loading/error states, visible context/sources, and an always-visible educational disclaimer.
- [ ] Enforce guardrails: no guaranteed returns, no buy/sell directives, no invented prices/news/sources, and clear uncertainty when data is missing.

### Final hardening and deployment

- [x] Account operations use the authenticated identity. ID-based portfolio/wallet/order reads reject cross-user access; administrative reads use protected admin APIs.
- [x] Manual stock-management operations require Admin, recent confirmation, reason and audit; validated provider stock imports remain available to users.
- [x] Added consistent malformed-input/conflict responses, safe account DTOs, cookie/CSRF ownership tests and transactional failure logging. Relevant test results are recorded in the current note below.
- [x] Production configuration reads database, JWT, encryption, mail and provider credentials from environment variables. Packaged JARs exclude ignored local properties/YAML files; IDE/classpath local configuration remains supported. Actual production values and sender/domain verification are launch acceptance tasks.
- [x] Exact CORS origins and frontend API URL are configurable; production requires HTTPS origins and secure cookies. The actual domain is an operator deployment input.
- [~] Forward SQL scripts cover nullable stock exchange/ISIN and instrument-key uniqueness (`20261110_stock_instrument_key.sql`); an idempotent startup metadata backfill is provided. Explicit checksum migration tooling is implemented; real-MySQL 8.4 and actual-installation upgrade/restore acceptance remain outstanding. Inspect existing columns/constraints before applying forward scripts; local Hibernate may already have added them.
- [~] Cross-device CSS and viewport/keyboard handling are implemented for phone/tablet/desktop, with expanded geometry checks. Full-app rendered review and physical Android/iOS acceptance remain pending; see deploy/V2_ACCEPTANCE.md.
- [~] Single-host Dockerfiles, Compose, Caddy HTTPS/SPA/API proxy, private MySQL, health-based startup and persistent volumes are prepared. Packaged runtime image builds, isolated MySQL 8.4 initialization/restore/restart and local private-CA HTTPS passed. Actual hosting/domain/public TLS and public deployment remain unverified; source-build Dockerfiles and the production network topology are not certified by the smoke topology.
- [~] README setup/features/accounting/source documentation and DEPLOYMENT.md are provided; accepted screenshots remain pending browser review.

#### V2 launch acceptance — separate from feature implementation

- [x] Added provider-neutral `deploy/production.env.example` and read-only `scripts/check-v2-release.mjs`. Private environment/local YAML files are git-ignored; diagnostics report field names only. Preflight checks configuration shape, JAR exclusions/production classes and frontend development-URL leakage, without contacting external services. It does not replace the acceptance steps below.

- [x] Portable release command, runtime package, checksum manifests, readiness report, packaging regression tests and release guide are implemented; complete local release verification and extracted archive checks pass. Production launch remains unapproved.
- [x] Isolated MySQL 8.4/restore/restart/HTTPS smoke runner passed locally after user-completed WSL/Docker installation, including both opt-in MySQL tests and cleanup of project containers/volumes.
- [~] Required-container CI workflow is implemented; the local runner passed, but GitHub CI itself has not been executed.

- [~] Local verification mail receipt and successful email verification are user-confirmed on 2026-11-10. Production SMTP/sender/HTTPS public URL, recovery-message receipt and production link destinations remain unverified.
- [x] Synthetic upgrade/backup restoration passed on isolated MySQL 9.7, and fresh MySQL 8.4 container initialization/backup restoration/restart acceptance now passes. First production launch uses a fresh database and preserves local data. If upgrading an existing installation, its reconciled migrations/restore require separate acceptance.
- [~] Earlier optional Windows Hello unlock was user-confirmed. The new mandatory phone factor requires real Android/iOS pairing, same-phone login, recovery, lock versus sign-out and expiry/revocation/reset acceptance. A supported phone/passkey platform is required; password alone cannot bypass the new policy. Owner authenticator acceptance remains separate.
- [~] Intended owner email has been supplied privately by the user. Verify that account, run the documented non-web bootstrap, enroll its authenticator, and save recovery codes. None of these provisioning steps has been confirmed; do not bypass verification.
- [~] Complete full desktop/mobile visual and interaction acceptance. Native Chrome URL verification/access recovered and limited desktop/400px checks were observed on 2026-11-10; remaining pages/dialogs and Admin are pending.
- [ ] Resolve active Upstox account capabilities/display permission, including the historical profile 401.
- [ ] Supply hosting/domain/HTTPS settings and deploy V2. V3 remains deferred until acceptance.

## Settled decisions and guardrails

- Login appearance: the user prefers the original blue split layout (42% brand panel, right-hand form), restored on 2026-11-03. Keep this appearance while retaining cookie sessions, email verification, password recovery and admin MFA/recovery flows. Other account/authentication pages retain their current layout; login's flat form is deliberate rather than another floating card.

- Release order is V2 completion/security/polish, V2 deployment and acceptance, then V3. Public signup, admin review/suspend scope, inbox-only security alerts and authenticator-based admin access were explicitly selected by the user.
- App authentication uses cookie JWTs backed by persisted sessions, not browser localStorage. Existing users must verify email after the upgrade. Admin bootstrap must be explicit, with the web backend stopped; never auto-promote a signup or bypass verification for convenience.
- First deployment uses one backend instance and one HTTPS frontend/API origin. See DEPLOYMENT.md for managed schema, mail, proxy trust, secrets, backup and rollback. Do not restart or kill the user's local app processes without need; temporary agent preview servers must be cleaned up.

- Follow existing local patterns before introducing a new library, abstraction, or formatting style.
- Keep provider credentials in environment variables; document variable names only, never values.
- Do not scrape exchange webpages. Use an authorized provider and follow its terms and display restrictions.
- Company search uses cached official Upstox NSE/BSE instrument catalogs (24 hours), with one company result grouped by ISIN and NSE preferred when both listings exist. BSE equity series are supported. Quotes are fetched on demand in add/trading dialogs; browsing does not import stocks.
- Dashboard/Watchlist quote refresh is shared through `POST /market/quotes/refresh`; `force=true` is used for manual refresh. Stored quote values remain the fallback if Upstox omits a symbol or the request fails.
- A selected stock is added/upserted to the local simulated-trading catalog; this does not place a real Upstox trade. Only searched/selected stocks are added to `/stocks`.
- Portfolio analytics use weighted-average cost. Do not infer missing trade history or show misleading P&L when data is inconsistent.
- Primary navigation is five pages. Markets defaults to Explore and supports `?tab=watchlist`; Portfolio defaults to Holdings and supports `?tab=insights|funds`. Tabs use browser history. `/stocks`, `/watchlist`, `/wallet`, `/stocks/:id`, and `/market/:symbol` are compatibility redirects; `/market/chart/:symbol` is the canonical detail/trade workspace. Authentication screens remain separate.
- AI features are educational explanations, not personalized financial advice or price predictions.
- Preserve user changes. Before editing, inspect `git status` and relevant diffs; do not reset, checkout, clean, or overwrite unrelated work.

## Handoff update procedure

After each task, edit this document in the same work session:

1. Update the relevant checklist markers and explain partial status briefly.
2. Add settled decisions or changed assumptions the next agent needs.
3. Record main code areas changed and exact checks run with results. If checks were not run, say so; do not copy an old result as current.
4. State the next useful task and known blockers.
5. Remove stale notes, but retain unresolved work and important constraints.

Before starting a new task, inspect the current repository and `git status`; this document is a handoff, not a substitute for verifying the code. Never assume prior chat is available.

## Useful checks

Run from project root when relevant:

```powershell
./mvnw.cmd test
```

Run from `frontend` when relevant:

```powershell
npm.cmd run lint
npm.cmd run build
```

## Current handoff notes

### 2026-11-10 - Responsive devices and final V2 verification

Implemented shared responsive changes for Android/iOS/tablet/desktop browser layouts: safe-area viewport metadata/insets; dynamic viewport fallbacks; keyboard-aware dialog dimensions; measured/wrapping navbar with independently scrolling sidebar; mobile 16px inputs and 44px touch controls; reduced-motion handling; flexible long-content layouts and labeled keyboard-scrollable tables. Preserved the five primary pages and blue login design. Enlarged essential mobile chart controls/labels; heatmaps preserve grid space on short viewports and support tap-to-preview followed by tap-to-open. Geometry tests cover 320/360/390/430/600/768/1024/1280/1440/1920 CSS-pixel widths. These are automated layout inputs, not physical-device acceptance.

Reviewed phone-page lifecycle and fixed background/back-forward-cache restoration so sensitive codes are cleared and cannot silently reappear; recovery-code acknowledgement has a retry path after interruption. Production/non-loopback API fallback now uses /api instead of localhost, preventing a deployed phone from contacting its own local machine. Owner MFA and backend security behavior remain protected by the full regression suite.

Verification this task: full Maven verify passed **175 tests, 0 failures/errors, 8 opt-in live checks skipped**; frontend API/auth tests **33 passed**; preflight tests **4 passed**; frontend lint and production build with VITE_API_URL=/api passed. Release artifact inspection passes; real production environment validation remains incomplete because operator values are not present. No user backend/database restart, account provisioning, real email or public deployment occurred. Docker remains unavailable on PATH; MySQL 8.4/actual-installation restore acceptance remains open. Latest visual attempt: Chrome inventory and address-bar observation succeeded, but opening a new local preview tab was rejected because the browser URL could not be verified with enough confidence. Computer Use ended without bypass. No latest-UI rendered screenshot or physical Android/iOS acceptance is claimed; the temporary port-5179 preview was stopped.

See deploy/V2_ACCEPTANCE.md for device widths, interaction scenarios, verification evidence and remaining gates. Next: complete accessible rendered browser review and real phone/owner prompts, then production SMTP/HTTPS, actual-installation MySQL 8.4 migration/restore, Docker/hosting and provider account/display permission. V2 remains partial; V3 stays deferred.



### 2026-11-10 ? Mandatory phone verification implementation; acceptance remains partial

Implemented the approved regular-user phone flow: password-first restricted login, single-use email pairing link, dedicated user-verified platform passkey, online six-digit/30-second code screen, same-phone password plus passkey login, ten hashed single-use recovery codes, restricted lost-phone replacement, and password-plus-factor Account replacement. Phone credentials are separate from optional session-unlock passkeys. Owner Admin retains its existing mandatory authenticator MFA. No normal application session is issued by password alone; existing sessions require a new full login through the factor_completed upgrade. Code display uses a separate five-minute HttpOnly cookie, exact current server step, atomic replay protection, no-store responses, throttling and security events. The phone webpage requires internet and HTTPS outside localhost; synchronized passkeys are not claimed to identify one physical phone.

Changed areas: PhoneVerification service/controller, SecurityAccounts/session validation and revocation, AccountMail pairing purpose, security schemas/migration and account deletion cleanup; PhoneCodes, PhoneLoginStep, PhoneSecurity and authentication routing; explicit checksum migration ledger and security/protocol tests. New upgrade: db/migrations/20261110_phone_verification.sql. Review DEPLOYMENT.md before starting this build against an existing database. No running user backend/database was restarted or migrated.

Verification actually completed: compilation passed; PhoneVerificationTests **9 passed**, including software-authenticator signed registration/assertions, same-phone login, missing UV/wrong origin, browser binding, replay/concurrency, restricted recovery, expiry/revocation/reset and cooldown. MigrationLedgerTests **3 passed**. Frontend **31 tests passed**, lint passed; production build passed before the final small UI/helper fixes and needs a final rebuild. Earlier full-suite run found six old password-only-login expectations; those tests were updated. Subsequent targeted SecurityIntegrationTests **37 passed** and BrowserCsrfTests **2 passed**; a complete final Maven verify has not yet been rerun. Software authenticator tests do not count as real phone acceptance.

Isolated real-MySQL **9.7** rehearsal passed: fresh shipping schemas, pre-phone synthetic fixture upgrade, old-session factor default FALSE, retained user balance 100000.00, all three phone tables, checksum-ledger application/replay skipping, mysqldump backup and restoration with matching fixture counts/balance. This used temporary target/v2-mysql-rehearsal databases on loopback port 33307, never the user's application database. Production Compose targets MySQL **8.4**; that exact version and a restored copy of the actual existing installation still require rehearsal. The ledger is an explicit operator tool, not automatic historical-schema reconciliation.

Visual acceptance was attempted through the computer-use skill. Native app inventory recovered after one timeout and Chrome state was observed, but opening a preview tab was rejected because the current URL could not be verified. Computer Use ended without bypass. No new application screenshot or protected interaction was accepted. A temporary Vite preview on port 5179 and isolated MySQL service were stopped during cleanup.

Remaining: final full Maven verify, frontend production rebuild/release preflight, code review of credential-replacement and challenge lifecycle, real Android/iPhone-to-PC pairing/recovery and same-phone login, desktop/mobile visuals and owner Admin acceptance. Mandatory enrollment now changes the earlier regular-user password-only fallback decision: unsupported phone browsers require a supported phone/passkey platform; email/password alone cannot bypass verification. Production SMTP/link receipt, actual MySQL 8.4 upgrade/restore, Docker/hosting/domain/TLS and Upstox account/display permissions remain launch gates. Prior read-only Upstox checks returned market data but profile HTTP 401; display/redistribution authorization is not established. Do not mark V2 or deployment complete.


### 2026-11-10 - User accepted the repaired local authentication workflows (current status)

User explicitly reported "done all good all working" after the registration/mail, localhost WebAuthn origin and current-form-value login fixes. Recorded local verification email receipt, successful email verification, login with the updated password and regular-user Windows Hello/passkey workflow as complete by user confirmation. The reported login issue is closed; no further password reset or repeated setup is requested. This supersedes the pending normal-flow authentication notes below and is user acceptance, not an agent-observed device ceremony.

Marked trusted-device feature acceptance complete for the confirmed regular-user workflow. Owner Admin provisioning/MFA and console acceptance, exhaustive unsupported-device/expiry/revocation/reset cases, full-app desktop/mobile review, production mail/HTTPS, real-MySQL upgrade/restore, provider permissions and deployment remain separate pending gates; the user's statement about the current auth fixes does not certify those unrelated items. V2 is not declared production-ready.

Only PROJECT_HANDOFF.md changed in this task; no application code, accounts or credentials changed. No tests were rerun for this documentation-only update. Latest prior verification remains 37 passing security integration tests and 28 passing frontend API/auth tests, lint and production build; those are historical check results, not new runs. Next: continue the remaining owner Admin, visual and deployment acceptance work.


### 2026-11-10 - Login rejected after user-reported password change

User clarified that they changed to a new password and still could not log in; the earlier possible explanation about re-registering with a different password is not an established diagnosis and must not be repeated as fact. Asked which update route was used and whether success was shown; the user's account-specific failure remains unconfirmed. Debugger router tools were unavailable, so verification used isolated API tests rather than observing the user's runtime credentials.

AuthScreen previously submitted React state rather than the live form controls. Added `authSubmission` and named inputs so login/register/reset/MFA submit current FormData, covering browser autofill/replacement values that may not have updated React state. Passwords are preserved exactly, never trimmed or persisted; confirmation compares submitted fields. This addresses a possible stale-field failure mode, not a confirmed runtime root cause for the user's account.

Added API regressions for email-reset -> rejected old credentials -> successful new-password login -> authenticated session, and Account password change -> revoked old session -> successful new-password login. Tests include uppercase email lookup, punctuation and preserved password whitespace. All 37 SecurityIntegrationTests passed with zero failures/errors; frontend ESLint, all 28 API/auth tests and production build passed; AuthScreen whitespace check passed. Log: ignored target/password-login-regression.log. Backend application code did not change in this task; no backend restart is required for the frontend fix. No real user password, hash, token, account or email was read/changed/sent by this task. Follow-up: the user subsequently confirmed all currently discussed authentication workflows working; the reported new-password login issue is resolved by user confirmation. Owner Admin and exhaustive security edge-case acceptance remain pending.

### 2026-11-10 - User confirmed verification and passkey enrollment

User explicitly confirmed receiving verification email, successfully verifying the email and completing passkey enrollment. Marked these specific local acceptance steps complete as user-confirmed; no agent-observed enrollment, lock/unlock or Admin acceptance is claimed. Broader trusted-device/SMTP/visual launch gates remain partial.

User also reported Invalid email or password during login. Source review confirms that this response means no matching account/password pair was found; email verification does not change a password. Re-registering an existing unverified email intentionally preserves the original password, as covered by the preceding registration tests. A later signup password may therefore differ from the account's original credential; this is a possible explanation, not a confirmed diagnosis of the user's account. Use the original password or the normal Forgot password flow, keeping localhost consistent. No password, PIN, verification token or device credential was requested, inspected or changed in this task.

Explained that InvestSmart's current passkey feature unlocks a locked existing session via Windows Hello (device PIN, fingerprint or face); Sign out/revocation/expiry still requires full email/password sign-in. Remaining device acceptance: lock, use Unlock with device passkey, verify retained session and then sign-out behavior. Owner password+authenticator remains separate. Only this handoff changed; no builds/tests or account mutations were performed in this task.


### 2026-11-10 - Registration resend parity and local WebAuthn origin alignment

User reported missing registration mail despite successful explicit resend, and supplied a localhost Account screenshot with a relying-party domain mismatch. Source review found repeated signup of an existing unverified account returned accepted without queuing mail. Registration now sends a new verification action for active unverified existing accounts, preserving their password/name/account records; verified or suspended accounts are not re-registered or mailed by that path. Registration and explicit resend share the existing VERIFY quota, in addition to signup throttling. Rate checks/logging occur before acquiring the registration mutex to avoid independent-transaction lock inversion. Responses remain neutral and accounts are never auto-verified. Only the latest verification link stays valid.

Added `DevelopmentOrigin` to normalize HTTP loopback-IP public origins to `http://localhost` with the same port for AccountMail and PasskeyService. HTTPS production origins and invalid origin inputs retain their existing validation behavior. This aligns locally generated mail links, RP ID and allowed WebAuthn origin without widening allowed origins or changing production security. Local passkey setup on an IP-host page is stopped before sending a setup request with instructions to use localhost; the adapter explains local RP mismatch before invoking the device prompt. Updated local deployment example to localhost. Restart the backend for these service changes, refresh the frontend, and use localhost consistently during sign-in/setup.

Verification: 41 focused backend tests passed (four mocked-SMTP registration integration tests, two local-origin tests, all 35 security integration tests), zero failures/errors. They verify immediate initial-registration delivery invocation, repeat unverified signup without credential changes, shared mail quota, no duplicate verified-account signup mail, canonical link generation, and existing security behavior. Frontend ESLint, all 25 API/auth tests and production build passed. Full `./mvnw.cmd verify` returned exit 0 / BUILD SUCCESS: 161 tests, zero failures/errors, eight opt-in live skips; packaged JAR refreshed. Log: ignored target/registration-origin-full-verify.log. Changed tracked-file whitespace checks passed. No real signup, SMTP message, credential enrollment or Windows Hello interaction was performed by the agent. Initial signup inbox delivery and real-device acceptance still need the user's retry; a mocked SMTP send proves application behavior, not external receipt.

### 2026-11-10 - Verification link connectivity and actionable passkey cooldown

User supplied a received verification email link screenshot showing `ERR_CONNECTION_REFUSED` at IPv4 loopback, and reported too many requests during passkey setup. Read-only diagnostics found Vite listening on IPv6 loopback `::1` only; the link's `127.0.0.1:5173` was unreachable. Set Vite's development host explicitly to `127.0.0.1` (loopback only, no LAN exposure). Vite reloaded its configuration; both `http://localhost:5173/verify-email` and `http://127.0.0.1:5173/verify-email` returned HTTP 200 afterward. These were token-free connectivity checks, not account verification. The user screenshot establishes receipt of that one message; it does not establish all mail/recovery acceptance. The live `/auth/mail-status` read reported configured=true, deliveryIssue=false; no mail was sent by this task and SMTP secrets/bodies were not read or exposed.

Passkey setup retains its existing five requests per identity / twenty per source in fifteen minutes. The backend now calculates remaining cooldown from the limiting request window, returns HTTP 429 with a specific message and `Retry-After`, and exposes that safe header through CORS. The error handler preserves exception response headers. Account displays a retry countdown and disables setup while waiting. Blocked retries do not record another request or extend the window. No persistent events were cleared and restarting does not bypass the limit. Failed setup attempts from the earlier serialization bug can naturally age out; don't repeatedly retry during cooldown.

Changed Vite config, SecurityEvents, RequestCooldownException, exception response-header propagation, CORS exposed headers and Account cooldown UI. Verification: all 35 SecurityIntegrationTests passed (zero failures/errors), including rejection/retry-header/CORS/non-extending-cooldown/expiry regression; all 24 frontend API/auth tests, ESLint and production build passed. Changed tracked-file whitespace checks passed. No full backend suite or real-device passkey enrollment was performed in this task. Restart the backend to load the cooldown/header change, refresh Account, then retry once the countdown expires. Request a fresh verification link rather than reusing the token exposed in the user screenshot. Keep one local origin consistent during sign-in/passkey setup; device/Admin acceptance and the remaining launch gates stay partial.

### 2026-11-10 - Passkey options serialization fix from device acceptance screenshot

User's actual Account screenshot reported "Passkey setup options are missing" before the device prompt. `PasskeyService.Ceremony.publicKey` is a JSON string containing Yubico's `{ publicKey: ... }` browser envelope; Account/sign-in passed that string to an adapter that accepted objects only. The frontend now parses serialized options before unwrapping/decoding them. Registration and unlock regression tests use the actual serialized wire shape; malformed strings fail safely before invoking device authentication, and object inputs remain supported.

Also fixed the backend completion path: stored encrypted browser options have the same envelope and must be unwrapped before deserializing into the Yubico creation/request option classes. Both registration and assertion completion now use a shared envelope reader. Integration assertions deserialize the actual options returned by the secured endpoints. Security requirements, credential verification and one-use challenge handling remain enforced.

Verification: frontend ESLint, all 24 API/auth tests and production build passed. Focused backend passkey integration command (`SecurityIntegrationTests#passkeyEnrollmentIsPlatformBoundAndCannotReplaceAdminMfa+lockRequiresAOneTimePasskeyAssertionOrFullSignOut`) returned exit 0 / BUILD SUCCESS with two tests, zero failures/errors. Full backend suite was not rerun for this localized parser fix. Log: ignored target/passkey-options-fix.log. No Windows Hello prompt was automated and no credential was enrolled by the agent. Restart the backend to load its completion-parser fix, refresh the frontend, then retry setup and complete the device prompt personally. Device/lock/unlock/Admin acceptance remains partial until observed; fixing this error does not establish end-to-end acceptance.

### 2026-11-10 - Native browser acceptance attempt (partial; authentication handoff required)

Read the computer-use skill and its guidance/confirmation policy. The browser connector reported unavailable and the initial native inventory timed out, but the documented native recovery succeeded. Selected the single returned Chrome window and verified its URL from accessibility/address-bar state before interacting. No URL safety rejection or security warning was bypassed. Both local servers were listening on 8080/5173. An existing regular-user session was initially present; no credentials were read from storage or entered by the agent.

Observed actual rendered screenshots: Dashboard and Markets at maximized desktop and 400px responsive width, Account at desktop, and Login at desktop/400px. Confirmed visible navigation links, market tabs, loaded quote/index cards, portfolio summary/empty holdings, Account passkey enrollment entry and session details, and the blue split desktop/stacked mobile Login layout. The 400px views inspected showed usable controls. An incidental approximately 308px pane showed index price/change crowding; narrow-screen index-card layout needs follow-up before full mobile acceptance. This is a limited visual pass, not full-app/mobile-device acceptance; heatmaps, chart themes/dialogs, Portfolio/Orders/IPOs/Bonds/Options details and Admin have not been comprehensively observed in this run. Desktop view was restored and DevTools closed at the end of the observed portion.

The browser session later returned to `/login`; the cause was not established. No passkey was enrolled and no authenticator/security prompt was automated. Account initially offered Set up passkey unlock. User action is required to sign in again, personally enroll/use the platform passkey, verify lock/unlock and sign-out behavior, and sign in to the owner Admin using its authenticator. The skill explicitly prohibits automating user authentication dialogs or changing in-app security settings. The owner console and its five sections have not been visually reviewed in this session; existing automated test results are supporting code evidence only. Do not mark the passkey/Admin acceptance items complete from those results.

Only this handoff changed in this acceptance attempt. No application fixes, builds/tests, account changes, orders, session revocations, password changes, owner bootstrap or email deliveries were performed. Next: user completes normal sign-in/device prompts and supplies an owner-authenticated browser session; resume the remaining page/dialog review and record each observed pass. Keep the three broad acceptance items partial until their actual remaining checks succeed.

### 2026-11-10 - NIFTY options and equity/option conditional paper orders

User expanded scope to NIFTY 50 CE/PE paper trading and selected limit/stop-loss orders as the next missing trading feature. Added an Options tab under Markets, Portfolio and Orders, plus a NIFTY chart link. Upstox contract/chain APIs provide expiry, strike, CE/PE type, lot/freeze quantities, LTP, bid/ask depth and open interest; invalid/foreign contracts and missing prices do not become tradable. Dates/lot sizes are not hardcoded. New authenticated APIs are under `/options` and `/orders/conditional`.

Options support premium-funded long buys and selling owned contracts only, using market, limit and protective sell stop-market orders. No naked writing/margin or futures. Fills require fresh fetched ask/bid depth sufficient for the full quantity; there are no partial fills or LTP fallbacks. Stop triggers use executable bid for options and listing quote for equities. Shared transactional user-row locking coordinates equity, options, funds, IPOs and reservations. Request IDs deduplicate accepted conditional/option submissions; pending buy cash and sell units are reserved, cancellation refunds once, and normal equity sells cannot spend reserved shares. Equity tickets offer Limit/Stop-loss Day orders; filled conditional orders enter ordinary equity history and pending/cancelled/expired entries remain in Orders. Deleted listings remain visible in pending history and are cancelled/refunded by processing.

The single-instance worker polls bounded, fairly rotated batches on a 30-second schedule; provider delays/backlogs can delay checks and missed ticks are not reconstructed. Equity day orders expire at 15:30 IST; option pending orders expire with their contract. Long option expiry settles after 18:00 IST using the exact expiry-day NIFTY daily close as a disclosed educational cash-settlement proxy, not the official settlement price. Missing dated closes leave settlement pending; later quotes/zero substitutions are forbidden. Expired unsettled option value and aggregate portfolio valuation remain unavailable. Option costs, saved bid marks, realized/unrealized P&L and all cash reservations feed portfolio totals; concentration insights still describe equity holdings. Quote retrieval times identify saved snapshots, not guaranteed exchange-live timestamps.

Updated `OptionMarket`, `OptionTrading`, `OptionController`, `OptionJobs`, `ConditionalOrders`/controller, equity execution limit guards/share reservations, portfolio DTO/enrichment, account-deletion cleanup, SQL baselines/startup validation, frontend option desk/tickets/orders/portfolio, README and deployment notes. Existing managed databases must rehearse/apply `20261110_paper_options.sql` before this build; no migration was run against the user's database. Fresh baselines include all three new tables. The trading worker can be disabled with `investsmart.options.scheduler.enabled=false`; tests disable it. Retained legacy service constructors for existing isolated tests while Spring injects the full runtime dependencies.

Verification: final `./mvnw.cmd verify` returned exit 0 / BUILD SUCCESS: **154 tests, zero failures/errors, eight opt-in live skips**, with packaged JAR. Provider, option-account, equity conditional-order and schema tests cover identity/depth failures, price guards, reservations, duplicate/cancel behavior, concurrency, stop gaps, ownership, deleted listings, pending expiry refunds, positive call settlement and zero-payout put settlement. Frontend ESLint, all **22** API/auth tests and production build passed; all four release-script tests and changed tracked-file whitespace checks passed. Logs: ignored `target/trading-v2-final-verify.log`. No backend/frontend listeners were present on 8080/5173 when checked. No authenticated live option API request, real trade, inbox delivery, real MySQL upgrade or browser visual acceptance was performed.

Remaining scope: active-account derivatives data/display permission, real-MySQL upgrade/restore and browser acceptance for all new flows. Brokerage/taxes, partial fills, option shorting/margin, futures, OCO/brackets and other index option underlyings are not implemented. Existing external V2 launch gates remain pending; V3 remains deferred.

### 2026-11-10 - V2 deployment preparation and missing listing migration

Added `deploy/Dockerfile.backend`, `Dockerfile.web`, `compose.yaml`, `Caddyfile` and a restrictive root `.dockerignore`. The bundle builds Java 21 and the frontend with `/api`, serves SPA deep links through Caddy HTTPS, keeps database/backend ports unpublished, waits for health checks and retains database/certificate volumes. Backend runs as an unprivileged user with a read-only filesystem and writable temporary directory. Only MySQL receives its separate root password; the backend receives explicitly selected runtime settings, and the web service receives only the public origin. A dedicated proxy address is trusted for client attribution; change the address/subnet and backend setting together if they conflict with the host network. No infrastructure or domain was provisioned.

Updated the private environment template (database root password and deliberate owner selector), README and DEPLOYMENT.md with empty-volume initialization versus existing-database migrations, quiet Compose validation, image builds, owner bootstrap, backups and release acceptance. The Dockerfiles skip backend tests during assembly; verify the exact checkout before release. Image tags must be reviewed/pinned for deployment. Runtime deployment is still partial, not complete.

Added the missing nullable unique stock instrument-key forward migration and an isolated H2 upgrade regression checking row preservation and rejection of duplicate listing identities. Existing installations must inspect the column and constraint first; the script must not be blindly replayed.

Verification: Compose YAML parsed with SnakeYAML and expected services/build files/private-port configuration checked; all four release-script tests passed. Focused `./mvnw.cmd -Dtest=FreshSchemaTests test` returned exit 0, with four tests passing, including the new isolated H2 migration regression. Official Windows Caddy downloaded to ignored target for validation: `caddy validate --config deploy/Caddyfile --adapter caddyfile` returned exit 0 with a placeholder HTTPS origin (no server started/certificate issued); config was formatted with `caddy fmt`. Its redundant-forward-header warning is expected because the explicit assignment documents the direct-client proxy boundary. Changed tracked-file whitespace checks passed. Docker is not installed, so image builds, Compose semantic validation, actual MySQL execution/network isolation and certificate issuance were not tested.

Also fixed Account's successful lock transition: clear frontend authentication immediately after `/auth/lock` succeeds so protected content leaves the screen and sign-in can offer the existing passkey unlock flow. The server session stays locked and retained. Login already supports unlocking; `/unlock` redirect is compatible with that flow. Frontend ESLint, all 22 API/auth tests and production build passed. Real authenticator/browser acceptance is still pending. Earlier notes claiming the passkey UI was removed do not describe the current checkout, which exposes it in Account and sign-in.

Next: run this bundle on an isolated Docker host, rehearse real-MySQL upgrade and backup restore, then finish SMTP receipt/link checks, intended-owner verification/bootstrap/MFA, desktop/mobile acceptance, provider display permissions and actual hosting/TLS deployment. V3 remains deferred.

### 2026-11-09 - V2 queued action-mail validity

Resolved the remaining mail finding from the November 4 review. `AccountMail` transactionally links verification/reset messages to purpose and a token fingerprint in `mail_action_links`. Delivery checks the unused/unexpired action, active account and matching recipient before SMTP; invalid messages and their failure records are deleted. Verification mail requires the account to remain unverified. Legacy pending action messages without links are discarded; request fresh links after upgrade. Non-action deletion notices retain their delivery behavior. This checks validity immediately before submission; SMTP submission is not atomic with subsequent action consumption or remote inbox delivery.

Updated runtime/fresh security baselines, managed startup table validation and `DEPLOYMENT.md`. Existing managed databases need `20261109_mail_action_links.sql` before the new build. The outbox foreign key cascades link cleanup during resend, account deletion and age cleanup. No migration was applied to a running user database.

Verification: focused `./mvnw.cmd -Dtest=SecurityIntegrationTests,FreshSchemaTests test` passed 37 tests with zero failures/errors. New coverage checks obsolete/legacy mail suppression, failure/link cleanup, valid VERIFY/RESET delivery and deletion-notice preservation using a mock SMTP sender. Full `./mvnw.cmd verify` returned exit 0; reports show 131 tests, zero failures/errors and eight opt-in live skips, with a refreshed packaged JAR. Log: ignored `target/v2-mail-verify.log`. Security-baseline whitespace check passed. Frontend unchanged; its checks were not rerun in this task.

Next: external V2 acceptance gates remain: SMTP inbox/link receipt, isolated real-MySQL upgrade/restore including this migration, owner verification/bootstrap/MFA, desktop/mobile acceptance, provider display permissions and hosting/TLS. The three static authentication/mail findings in the November 4 review now have code fixes; construction/navigation findings are superseded by the November 9 entries. V2 is not yet release-ready. No real mail, account provisioning or public deployment was performed.

### 2026-11-09 - V2 continuation: MFA revocation and navigation

This entry supersedes the corresponding startup/navigation and MFA findings in the November 4 review. The current `LearningAssistant` already has `@Autowired` on its injection constructor; no constructor edit was needed. Restored the settled five-page V2 navigation and removed the Learning route/import from `App.jsx`, preserving the deferred Learning implementation files and backend for later V3 review.

`SecurityAccounts.revokeAll` now acquires the user lock in a transaction and invalidates pending MFA_LOGIN/MFA_SETUP actions, clearing enrollment secrets together with session revocation. MFA completion rechecks purpose, unused state and expiry after acquiring locks. Added integration coverage for password reset invalidating both unfinished enrollment and pending enrolled-admin login, without issuing a session cookie.

Verification: frontend ESLint, all 22 frontend API/auth tests, production build, and all four release-script tests passed. Backend `./mvnw.cmd -q verify` produced fresh Surefire reports: 129 tests, zero failures/errors, eight opt-in live skips; all 32 security integration tests passed. It produced a repackaged JAR but the redirected wrapper invocation returned exit 1 without a Maven error in its log, so this is not recorded as an unqualified successful verify command. Follow-up `./mvnw.cmd -DskipTests verify` returned exit 0 and BUILD SUCCESS, confirming packaging. Logs are ignored under target/v2-continuation-final.log. Changed tracked App.jsx whitespace check passed.

Next: link queued verification/recovery mail to action validity and suppress used/expired action mail before sending; add expired-recovery regression coverage. SMTP receipt, MySQL migration/restore, owner provisioning, browser/device acceptance, provider permissions and public deployment remain unverified. No running user app was stopped, no account was provisioned, and no external message was sent by this task.


### 2026-11-04 — Whole-project review (latest authoritative status)

Reviewed backend controllers/security/services, frontend routes and authentication, deployment configuration/baselines, and available automated checks. This was a source/build review, not exhaustive runtime, penetration, provider, inbox or visual acceptance. Only this handoff was edited; existing application changes were preserved, no running user services were stopped, and no accounts or financial records were changed. **V2 is not currently release-ready. Older passing test counts below are historical, not the current result.**

Current verification:

- `./mvnw.cmd -q verify`: **FAIL**, 128 tests reported: 64 passed, 56 errors, 8 opt-in live skips, zero assertion failures. Application-context failures originate in `LearningAssistant`: Spring reports no default constructor (`NoSuchMethodException LearningAssistant.<init>()`), with multiple constructors present and the intended injection constructor not selected. This also prevents the fresh-schema and security integration contexts from running successfully; their errors do not establish independent schema/security defects. See ignored `target/project-review-backend.log` and Surefire reports. Compilation completed, but this run did not reach successful release packaging.
- Frontend `npm.cmd run lint`: PASS. `node --test src/api/*.test.mjs src/auth/*.test.mjs`: **21 pass / 1 fail (22 total)**. `navigation.test.mjs` expects five destinations, while `MAIN_PAGES` now includes `/learning` as a sixth. Decide whether to retain the newly added destination or restore V2's five-page scope before reconciling the test; do not change expectations merely to hide a scope change.
- Frontend `VITE_API_URL=/api npm.cmd run build`: PASS, no bundle-size warning. This verifies bundling, not browser functionality.
- `node --test scripts/*.test.mjs`: **4/4 PASS**. Production credentials, live preflight, inbox receipt and public deployment were not verified.

Implementation inventory and corrections to earlier assumptions:

- Core market/chart/watchlist, heatmap, portfolio/funds, paper orders, IPO practice and bond discovery code remains present. Original blue split login styling remains present; current visual acceptance is outstanding.
- Owner Admin has Overview, Users, Security, Operations and Audit. In addition to user moderation/session/reset/resend/balance/deletion controls, Operations now exposes provider-backed IPO/bond availability overrides, IPO timeline overrides and guarded settlement retry. This is **not** unrestricted manual listing creation. `AdminHealthStore` persists runtime health to `admin_runtime_health`; do not describe all telemetry as restart-only. Security alerts remain in-app; external critical-alert notification is not implemented.
- Registration verification, ordinary email/password sign-in, recovery and trusted-device WebAuthn lock/unlock are implemented. There is **no application-managed four-digit PIN feature**. Platform authentication may offer the device's own unlock method; actual device/browser acceptance remains pending. Owner/admin password plus authenticator remains required.
- Learning UI, authenticated `/learning/chat`, optional portfolio/market/news context, consent, rate limiting and configurable external-model integration now exist, despite the older V3 checklist marking everything unstarted. Without AI configuration the API returns 503. This implementation is not accepted V3, has no dedicated chat tests in the current test tree, and currently introduces the startup blocker above. Reconcile V2/V3 scope explicitly before release.
- Fresh SQL baselines exist in `deploy/mysql-init/001-app-schema.sql` and `002-security-schema.sql`, with H2 schema-validation tests. No Compose/Docker/Caddy deployment files exist in the current checkout; do not assume the previously discussed portable deployment bundle was completed. Real MySQL upgrade/restore rehearsal remains outstanding.
- The user supplied an intended owner email in chat. Ownership verification and explicit bootstrap/MFA enrollment remain unconfirmed; do not copy the private address into tracked examples or infer that an owner account was provisioned. The older statement that no email was supplied is superseded.

Security follow-up from static inspection (not exploitation or runtime verification):

1. **High priority:** `SecurityAccounts.revokeAll` revokes sessions/locks but does not invalidate pending `MFA_LOGIN`/`MFA_SETUP` actions. Password reset/change can therefore leave previously issued authenticator challenges usable. Invalidate pending authentication challenges atomically with password/session revocation under the account lock; add regression coverage, particularly for unfinished admin enrollment.
2. `completeMfa` checks expiry before acquiring the user lock, but the locked action re-read checks only `used`. Recheck purpose, unused state and expiry after lock acquisition to avoid accepting a challenge that expired while waiting.
3. `AccountMail.deliver` selects pending mail without verifying that its verification/reset action remains unused and unexpired; the scheduled cleanup occurs after delivery and uses a generic 24-hour age. Link queued action mail to token validity and skip obsolete/expired messages before sending, preserving non-action notification mail. Add a regression for expired recovery mail.

Recommended next work, in order: fix the LearningAssistant construction/startup failure; decide the Learning navigation/release scope; fix and test the authentication/mail concerns above; rerun full verification and packaging; then complete SMTP receipt/link checks, isolated MySQL upgrade and restore, owner MFA setup, device/desktop/mobile acceptance, provider display permission and hosting/TLS setup. No new admin pages are necessary to resolve these release gates.

**2026-11-04 — Local SMTP wiring:** The default/local profile now maps the documented `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM`, and `PUBLIC_URL` variables to Spring mail, sender and WebAuthn origin settings, with authenticated required STARTTLS and bounded timeouts. `DEPLOYMENT.md` documents the IntelliJ run-configuration setup and private local properties. No credentials were available or added, so actual SMTP connection and email receipt remain unverified. Without SMTP, registration verification and password-recovery email are unavailable; existing verified accounts can still sign in.

**2026-11-04 — Local verification link CSRF failure:** Screenshots showed registration accepted and a verification link opened, but the verify action returned 403. The frontend default API URL was hardcoded to `localhost`, even when the browser/link used `127.0.0.1`; this can separate the CSRF cookie from the frontend origin. The local API base now follows the browser hostname for localhost/127.0.0.1 while preserving explicit `VITE_API_URL`. Added unit coverage. Verification: dummy-token POST from `127.0.0.1` passes CSRF and reaches token validation (400 rather than 403); frontend tests pass 20/20, lint and production build pass; backend CSRF integration tests pass. Do not reuse the link exposed in the screenshot; request a fresh verification email now that the origin mismatch is fixed.

**2026-11-04 — Local registration request failure:** The local browser used `http://127.0.0.1:5173`, while the development CORS default allowed only `http://localhost:5173`; the CSRF request was therefore rejected before registration. Both loopback development origins are now allowed by default. Verified CSRF GET returns 200 and a validation-only registration POST reaches the backend and returns 400 with CORS headers; no account was created by this check. Local SMTP is still unconfigured, so registration can queue a verification message but delivery/sign-in cannot be completed until an operator configures SMTP. Production CORS remains explicitly configured by `investsmart.cors.origins`.

**2026-11-04 — Verified-email and trusted-device authentication:** Registration now sends an immediate delivery attempt for a 24-hour verification link; resend invalidates earlier links, recovery links expire after 30 minutes, and normal login never re-sends verification. Unverified users receive no session. Delivery failures are safely recorded and surfaced with operator settings/link guidance; accounts are never auto-verified. No SMTP receipt was verified. Regular verified users can optionally enroll platform WebAuthn passkeys with required user verification and unlock only a locked, still-valid session. Only credential IDs, public keys and non-secret ceremony metadata are stored; browser/device biometrics remain local. The relying party advertises Java 21-supported WebAuthn algorithms. Lock and sign-out are explicitly distinct. New devices, expired/revoked sessions, password resets and suspended-account reactivation require full email/password sign-in. Admin password plus authenticator MFA remains unchanged. The blue split login design was preserved.

Verification: `./mvnw.cmd -q test` passed **116 tests, zero failures/errors, 8 opt-in live checks skipped**; 20 security integration tests include verification gating, safe mail failure, passkey registration policy, single-use unlock challenge, locked-session logout/revocation, expiry messages and admin MFA protection. Frontend `node --test src/api/*.test.mjs src/auth/*.test.mjs` passed **18 tests**; `npm.cmd run lint`, `npm.cmd run build`, and the IDE project build passed. Starting the local application initialized the security schema; reconnecting and refreshing the IDE's MySQL schema metadata resolved the `PasskeyService` SQL inspections. SMTP delivery, real device biometric ceremony, isolated production-migration rehearsal and browser/mobile acceptance remain unverified; no account or financial record was changed by this task.

Next: configure SMTP and exact HTTPS `PUBLIC_URL`, verify real receipt and verification/reset link destinations, rehearse `20261104_trusted_device_unlock.sql` once on an isolated MySQL copy, then test enrollment/unlock and fallback with a supported platform authenticator. Review desktop/mobile browser behavior before launch.

**2026-11-03 — Continued deployment preparation in Default mode:** The user's screenshot referred to the earlier request for owner email, SMTP and hosting information, rather than a new application error. Added an empty provider-neutral production environment template, private environment-file ignore rules, a read-only release preflight, four tests for valid/incomplete/redacted configuration and missing artifacts, and usage instructions. Built the production frontend with `VITE_API_URL=/api`; normal dev-server defaults and the restored blue login presentation remain unchanged. The preflight inspects the existing backend JAR and frontend bundle without starting services, sending mail, connecting to databases or changing accounts.

Verification: `node --test scripts/check-v2-release.test.mjs` passes **4/4**; production frontend build passes with no bundle warning (initial JS 365.18 kB). Artifact-only preflight reports **no failures**. Full preflight exits **1 as expected** because production process-environment settings have not been supplied; it deliberately does not load development properties. Git checks confirm `deploy/production.env` and `frontend/.env.local` are ignored, while the example template is not. Relevant tracked-file diff check passes. Backend tests were not repeated for tooling/docs/ignore-rule changes; the earlier 101 backend passes and 8 live skips remain historical results.

Asked for the intended owner email and whether mail/hosting services exist or help choosing is needed. No response is recorded yet; do not infer ownership from old test emails. SMTP delivery, owner enrollment, actual MySQL rehearsal, browser acceptance, provider permissions and public deployment remain open. No live account, service configuration or running user process was changed. V3 remains deferred.

**2026-11-03 — Restored the preferred login appearance:** Compared the current page with the original tracked Login.jsx/Login.css. Restored its blue brand panel, Learn. Invest. Grow. tagline, right-side 390px form, original blue action color and mobile stacked layout through scoped Login.css and presentation changes in AuthScreen.jsx. Preserved all existing cookie login, verification, recovery, MFA enrollment/challenge and single-use recovery-code behavior. Registration and other auth pages retain their current layout; Account/Admin styles are unaffected. Sign-in errors, loading states, accessible account-help links and consistent 6px controls remain available.

Verification for this appearance-only change: frontend lint and production build pass, with no bundle-size warning (initial JS 365.20 kB). Backend tests were not rerun because backend/authentication behavior did not change. No browser visual acceptance was claimed; the existing browser acceptance limitation remains. Checked mail/public-URL property names without printing secret values: SMTP/sender configuration is still absent from the checked local properties. No owner account or mail/deployment settings were guessed or modified.

**2026-11-03 — Interrupted V2 release verification completed:** Fresh `./mvnw.cmd -q verify` succeeds with **101 passed, zero failures/errors, 8 opt-in live checks skipped (109 total)**, including all **19 security/CSRF/non-web bootstrap tests**. Fresh frontend tests pass **15/15**; lint and production build pass with no bundle-size warning. Inspected the packaged `target/InvestSmart-0.0.1-SNAPSHOT.jar`: local properties/YAML configuration is excluded; production configuration and owner bootstrap class are included. Incremental packaging can retain the existing artifact timestamp when application inputs are unchanged; verification used the current source and completed successfully.

Reconciled feature completion versus launch acceptance in the checklist. README/DEPLOYMENT.md now explain local owner setup with an explicit external configuration file, since the JAR intentionally excludes it. No application code changes were needed for this follow-up; no account records were changed or application processes restarted. Diff check still reports only the pre-existing blank line at EOF in `test.http`.

Remaining launch gates: actual SMTP delivery, a verified owner email and authenticator enrollment, MySQL upgrade/restore rehearsal, desktop/mobile acceptance, active Upstox capabilities/display permission and public hosting/domain/TLS. SMTP and owner details have not been supplied; do not guess them or bypass verification. No new browser attempt or live-provider request was made in this follow-up. The earlier browser safety rejection remains an unresolved visual-acceptance limitation. V3 is deferred until V2 acceptance.

**2026-11-03 — V2 UI polish, account security and owner administration:** Implemented the approved public-signup/review-and-suspend/inbox-alerts/authenticator plan before V3. Dashboard now has moderately larger readable text and a compact index strip; Markets keeps its larger index cards. Shared corner/button/input styles, restrained hover states, neutral panels and reduced starter CSS apply across pages. Charts and Admin are lazy-loaded; the final frontend build has a 364.62 kB initial JS chunk plus 182.20 kB Charts and 11.05 kB Admin chunks, without the former >500 kB warning.

Completed the interrupted bond work: eleven dated official reference rows, validated optional external CSV overlay, Verified terms filtering and coverage counts. Fixed the stale assertion and tested invalid/duplicate/older overrides. Unknown terms remain unavailable; reference coverage is not a recommendation or a yield calculation.

Security now uses JWT cookies backed by `auth_sessions`, server-side account/owner checks and cookie/header CSRF. Removed legacy generic user/auth services and entity-based user APIs; order/watchlist writes return safe DTOs. Old bearer tokens are rejected and removed from browser storage. Email verification/recovery uses hashed one-use tokens and encrypted queued mail. Password changes revoke sessions. Account & security supports session review/revocation. User suspension uses the same account lock as financial writes; scheduled IPO accounting continues.

Admin has Overview, paginated Users/Alerts/Audit, read-only portfolio/trade/IPO review, reasoned suspension/reactivation/session revocation, recent password+MFA confirmation and audit records. Only an explicit verified-email non-web bootstrap populates `owner_admin`; an old ADMIN field alone cannot enroll/access Admin. TOTP uses six RFC 6238 vectors, monotonic counter checks and hashed single-use recovery codes. Failure events persist independently of rejected transactions. Configurable login/source/MFA/access-denial rules create deduplicated/reopenable inbox alerts; normal market/funds/provider failures are not classified as abuse. Network details are masked and forwarding is trusted only for configured proxies.

Added production configuration, the forward account-security migration, rerunnable security schema, minimal `/health`, and DEPLOYMENT.md covering SMTP/secrets, HTTPS/proxy/CSRF, backups, MySQL upgrade rehearsal, bootstrap and acceptance. Default local schema initialization is convenience only; production requires managed schema and validation. Existing records are retained. First deployment is one backend instance; add distributed mail claiming before replicas.

Verification: final `./mvnw.cmd -q test` passes **100 tests, zero failures/errors, 8 opt-in live checks skipped (108 total)**. The 18 security tests cover cookie login/logout and real CSRF headers, legacy bearer rejection, cross-user/admin access, forced registration roles, single-use verification/reset, concurrent reset consumption, owner bootstrap/legacy-role rejection, MFA enrollment/replay/recovery/cooldown, persistent alerts/reviews, suspension/reactivation and concurrent suspension-versus-withdrawal. Bond override/filter coverage passes. Frontend `node --test src/api/*.test.mjs` passes **15 tests**; final lint and production build pass without bundle-size warnings. No live provider check was rerun in this task. Real CSRF cookie/header tests use a separate H2 context because Spring's `csrf()` helper replaces the repository in shared MockMvc tests. Diff checking retains only the pre-existing EOF blank line in `test.http`; do not print its existing token-containing content to diagnose whitespace.

Browser acceptance is **not complete**: browser-host discovery failed; native Chrome was found, but opening a separate preview tab was automatically rejected because its URL could not be verified. Stopped all further UI automation. Temporary loopback fixture servers on 5175/8181 were stopped and the fixture script removed. No user app process was stopped/restarted, owner account promoted, real email sent or user financial record modified by this task. Source changes require restarting the user's backend/frontend before review.

Next: configure and verify SMTP privately, apply/rehearse migrations on a database copy, select the verified owner via the documented CLI, enroll MFA/save recovery codes, perform desktop/mobile acceptance and resolve active Upstox account/display permissions, then deploy V2. V3 remains deferred. No hosting/domain or production permission values were guessed; no public deployment was performed.

**2026-11-03 - Local port conflict:** A previously agent-started Vite instance occupied 5173, causing a second frontend to fall back to 5174; a running IntelliJ InvestSmart instance occupied 8080. At the user's request, stopped the verified InvestSmart Java listener and both project Vite listeners to release 8080/5173/5174. Set Vite `server.port=5173` and `server.strictPort=true` so future collisions produce an explicit error instead of silently switching away from the backend's allowed frontend origin. TCP verification confirmed all three ports free; importing the Vite config confirmed both settings; config diff check passed. No application tests were rerun for this configuration-only change. Leave these ports free for the user's own IntelliJ/backend and `npm run dev` launches; the runtime PIDs in the earlier note are historical.

**2026-11-03 - IPOs, bonds and one-screen heatmap:** Implemented the authorized extension described in V2 E above. Kept five main destinations; IPOs/Bonds are Markets tabs with accessible native-dialog detail drawers and per-user saved interests. Added Orders IPO applications, Portfolio reserved/total cash and unlisted cost, shared account refresh notifications, application idempotency/refunds, and a startup/hourly catchup job. No real provider order/payment APIs are called. The simulator is an overall-subscription educational proxy; it is not a retail probability forecast. Applying for more lots does not improve its probability. Debt discovery is separate from equities; verified reference coverage is explicitly limited.

Heatmap layout fills a measured viewport using equal-area cells without invented market-cap weights, defaults to NIFTY 50, supports fullscreen and retains URL filters on chart return. Removed duplicate graph index strip while preserving sidebar indices/attribution. Scoped navbar CSS so it no longer overrides page tabs; mobile sidebar remains a drawer instead of consuming the heatmap's viewport. Added README setup, simulation/accounting rules, data sources and verification commands.

Verification: final `./mvnw.cmd -q test` passes **81 tests, zero failures/errors, 8 opt-in live tests skipped** (89 total). New coverage includes provider mapping/failure/staleness, debt separation/reference coverage, authenticated ownership, hidden draw, reservation/refund/allotment timing, listing cost reconciliation, startup BSE-only catchup, duplicate apply/cancel and concurrent IPO-vs-withdrawal/equity-order/settlement races. `node --test src/api/*.test.mjs` passes **15 tests**, including desktop/phone heatmap geometry and IST IPO window/minimum quantity. Frontend lint/build pass; existing >500 kB bundle warning remains. Diff check only reports the pre-existing EOF blank line in `test.http`.

Read-only live run `./mvnw.cmd -q '-Dinvestsmart.live-check=true' test` had **84 passes and 1 failure** before final additional concurrency/catchup tests were added. **7 of 8 live checks passed**: four IPO lifecycle categories/details, government/corporate bond discovery/reference, all 50 NIFTY prices in one page, index/Infosys quotes, quote persistence/cache, all NIFTY/SENSEX chart ranges and exchange statuses. The separate `/v2/user/profile` capability probe returned **401**. Market/IPO reads worked, so do not label the token entirely expired or claim account capabilities verified. Account-profile/permission checks and display/redistribution terms remain unresolved; no profile or secrets were exposed. All live persistence used isolated H2, not the user's database.

Runtime: backend restarted on **8080** with the local profile (PID 3068); Vite runs at **http://localhost:5173** (PID 18196). Frontend `/heatmaps` returned HTTP 200; unauthenticated IPO applications returned expected 401. Local schema update created the new tables without deleting account records. No real account transaction was performed.

Browser review was attempted again through computer use: app inventory and browser host were unavailable. Visual/fullscreen/mobile interaction acceptance remains pending despite geometry/build/API tests. Next: review the app visually, verify Upstox profile/display permissions, extend official bond reference coverage, then V3 sourced context/news. Production authorization hardening, managed migrations and deployment remain unfinished.

**2026-11-02 — V2 consolidation and verification:** Reduced the sidebar from eight entries to **Dashboard, Markets, Heatmaps, Portfolio, Orders**. Moved Watchlist and Funds into reusable panels inside Markets/Portfolio tabs, retained virtual deposit/withdraw, and added order search/side/status filters. Removed duplicate Stocks, Stock Details, old Market Details/chart pages and their unused components/styles. Legacy routes use replacement redirects; stock-ID routes resolve their instrument key before opening the canonical chart. Company/chart links carry listing identity and a return destination. Dashboard now previews the user's actual holdings. Shared account loading protects against old responses and loads saved values before slow quote refreshes. Portfolio uses listing metadata, supports unavailable prices without fake zero values, and obtains a consistent read transaction. Funds writes use the same user lock as order writes; no production account records were removed.

**Virtual funds layout:** The Funds tab places a paper-trading guide beside the add/withdraw controls on wider screens to use the previously empty space; the guide stacks below the controls on narrower screens. It clarifies that the balance is simulated and explains each action.

The add/withdraw panel also includes clickable ₹1,000, ₹5,000, and ₹10,000 amount shortcuts plus concise amount validation guidance to make its lower area useful.

**Stock search polish:** Hid the browser's duplicate native search-clear glyph and gave the explicit Clear control consistent spacing, width, and subdued styling in both the Markets search and chart-terminal Add stocks dialog. Clear continues to reset results and return focus to the search field.

Reference review used official Kite Marketwatch/Portfolio/Funds documentation, Upstox Pro Web and TradingView Heatmaps documentation. Applied the compact watchlist/chart workflow while retaining the separately requested Heatmaps shortcut. The app remains an educational paper-equity product; advanced broker products were not added.

Verification: `./mvnw.cmd -q '-Dinvestsmart.live-check=true' test` passed **66 tests, zero failures/errors/skips**, including **4 opt-in live checks** using configured local Upstox credentials and an isolated H2 database. Live checks confirmed four index current/previous-close values; NIFTY/SENSEX history for 1D, 1W, 1M, 6M, 1Y; exact NSE/BSE Infosys quotes; stored quote refresh and cache skipping; and both exchange statuses/guards. No live account trades, deposits or withdrawals were performed. H2 tests include concurrent withdrawals (only one succeeds), missing-price/zero-quantity portfolios, and authenticated wallet/portfolio/order API behavior. Live tests are skipped by default and explicitly enabled with the flag above. `node --test src/api/*.test.mjs` passed **13 tests** (redirects, account response races, independent price refresh, graph watchlist, listing identity, order validation/duplicate clicks). Frontend lint/build pass; the existing >500 kB Vite chunk warning remains. Diff check only retains the pre-existing EOF blank line in `src/main/java/com/example/investsmart/test.http`.

Runtime: started/restarted the local backend on **8080** and Vite frontend on **5173**; both were left running for review at `http://localhost:5173`. HTTP checks returned frontend 200 and unauthenticated account API 401 with the expected localhost CORS header. Local credentials were not copied into source, messages, or handoff. Browser visual review could not run because the browser access host is unavailable; mobile, tab Back/refresh interactions, and visual chart/order-window checks remain pending. Active-account display/redistribution permission cannot be inferred from successful API access and remains a pre-deployment check. V3, deployment, admin hardening and the production migration framework were not started. Earlier notes below describe superseded page layouts and historical results.

**2026-11-02 — Graph workspace, exchange identity, and chart orders:** Completed the compact four-index/watchlist sidebar, newest-three preview, View all, selected-instrument price/range/details, + Add stock dialog, and localStorage light/dark preference. Canvas/grid/axes use `applyOptions` so theme/order changes preserve chart zoom and timeframe. Added shared `TradeControls`/`TradeOrderTicket` for graph and Stock Details: Buy/Sell, NSE/BSE selection, quantity, listing-specific quote/holdings, available cash, estimated total, success/error states, and duplicate-click suppression. Unknown network outcomes direct users to Orders instead of resubmitting. Index charts do not offer orders. Full Watchlist now uses the same immediate membership/independent price loader as the graph.

Backend additions: `EquityCatalogService`, exchange/ISIN stock and watchlist metadata, `GET /market/listings?instrumentKey=&symbol=`, optional equity `instrumentKey` parameters on quote/history APIs, NSE/BSE import validation, key-based quote-refresh mapping, and one company watchlist upsert serialized through the user lock. Existing symbol routes default to NSE. New daily cache rows use instrument keys in the existing symbol column; legacy NSE symbol caches remain readable and are never used for BSE. Order execution validates exchange status and matching live quote before balance changes, using holdings keyed by stock ID. `StockMetadataBackfill` retains existing IDs and infers known NSE metadata; `src/main/resources/db/migrations/20261102_stock_listings.sql` is a once-only forward script for managed deployments, not an automatically installed migration framework. Existing unrelated worktree changes were retained.

Verification: `./mvnw.cmd -q test` passed **57 tests**, including new BSE quote identity, equity catalog/series, company watchlist replacement, BSE cache separation, execution price, cash/holdings rejection, and closed-market tests. `node --test src/api/*.test.mjs` passed **8 tests**; frontend lint and production build passed. Vite retains its >500 kB chunk warning. Diff check only retains the pre-existing EOF blank line in `src/main/java/com/example/investsmart/test.http`. Public NSE catalog access succeeded during inspection; no authenticated live Upstox request or order was performed. Neither app was listening on 8080/5173 during verification. Windows computer-use browser review stopped because it could not verify Chrome's URL before opening the isolated preview; desktop/mobile visuals, theme/zoom interactions, and authenticated live flows remain unverified. Temporary fixture preview files were removed and its server stopped. Next: start/restart the apps, verify graph Buy/Sell (with a test account), add/listing selection, watchlist refresh, all SENSEX/NIFTY ranges, and exchange-status behavior; reconfirm active-account data access/display permissions. V2 live-verification items above remain partial; V3/deployment were not started.

**2026-11-02:** Fixed graph Watchlist updates. `graphWatchlist.js` loads `/watchlist/me` immediately, independently refreshes quote prices, re-reads saved values after price refresh, and sorts by watchlist entry ID descending for the three-entry preview. The terminal subscribes to `investsmart-watchlist-changed`; removed redundant add callbacks so the event provides one refresh path from either search or company heading. Pending price work is shared across membership changes; aborts/request sequence checks prevent old membership results replacing newer additions and prevent updates after unmount. List-load errors preserve the last list and are shown separately from stale-price warnings; manual Refresh uses `force=true`. Verification: `node --test src/api/graphWatchlist.test.mjs` passed all four cases (new fourth entry while prices pending, old response after newer response, forced refresh/failure preservation, cleanup). Frontend lint/build passed; Vite retains its bundle-size warning. Authenticated browser check on `/market/chart/INFY` confirmed five total entries with the newest three displayed (LALITHAA, JPL, BAJAJFINSV); manual refresh visibly updated saved prices. No live stock was added for this check; the add-during-refresh behavior was verified with controlled client tests.

**2026-11-02:** Added `ChartStockSearch` above My watchlist in the chart terminal. It starts at two characters with a 300 ms delay, merges local catalog matches and Upstox results by symbol, cancels obsolete searches, supports provider pagination, and shows loading/no-match/failure/retry states. Each result displays company name and ticker with independent Open chart and Add controls; adding reuses `AddToWatchlist` and refreshes the existing three-entry preview. Opening a company changes the existing chart route without resetting its selected time range. Keyboard support uses Tab, arrows through result controls, Enter on links/buttons, and Escape to clear. Added scoped dark/responsive styles. Verification: frontend lint and production build passed; Vite retains the bundle-size warning. No backend APIs changed. Live authenticated search/add and mobile browser checks were not performed in this task.

**2026-11-02:** Implemented the revised Market/Watchlist/Heatmaps flow. Added protected `/heatmaps` route and sidebar link; removed chart-side heatmap tab in favor of a page link. Markets uses `GET /market/featured` for ten named Standard stocks; shared quick-add control resolves/imports NSE stocks, calls the existing watchlist API, prevents repeated submits, and refreshes other controls after success. Dashboard and chart previews cap entries at three. Added `MarketSnapshotService` and grouped `GET /market/heatmap?group=indices|nifty50|all&query=&page=1`; requests without `group` retain the original response. Metadata caches for 24 hours, quote batches for 60 seconds; All stocks pages contain at most 50 instruments. The official NIFTY constituent CSV is saved in `src/main/resources/nifty50-constituents.csv`, retrieved 2026-11-02 from `https://www.niftyindices.com/IndexConstituent/ind_nifty50list.csv`; membership is matched by ISIN. Maintain this versioned snapshot when constituents change. Live public-source check found 2,723 NSE_EQ/EQ instruments and matched all 50 constituents. Corrected BAJFINANCE instrument key using the current Upstox JSON catalog. No stock catalog rows are created merely by browsing.

Verification: `./mvnw.cmd '-Dtest=MarketSnapshotServiceTests,MarketDataServiceHeatmapTests,UpstoxMarketDataProviderTests' test` passed (21 tests), `npm.cmd run lint` and `npm.cmd run build` passed. Vite retains the bundle-size warning. Isolated browser preview with simulated quotes confirmed dark tiles, visible company names, gain/loss/unavailable states and All stocks filter/pagination controls; preview files were removed and its dev server stopped. No app listeners were running on 8080/5173; authenticated live watchlist saving, live quote groups, and mobile visual behavior remain to be confirmed after starting the app. Prior chart-side heatmap notes below describe the superseded layout.

The worktree already contained numerous staged and unstaged edits across market data, charts, portfolio analytics, authentication, and tests, plus untracked authentication/test-profile files. Those edits predate the handoff and were left untouched. Inspect live status/diffs before further changes; do not assume this note is still current.

**2026-10-26:** Created this handoff after checking the project tree. Confirmed the Upstox token property references `UPSTOX_API_TOKEN`; no credential values were copied. The earlier documentation-only change did not run tests/build.

**2026-10-26:** Added paginated NSE equity search on the Market page, quote-and-add behavior to the simulated stock catalog, and dynamic Upstox instrument resolution for quote/history lookups. Added `instrumentKey` to saved stocks and made repeated imports update an existing stock. Added stock-import unit tests. Verification: `./mvnw.cmd test` passed (13 tests); `npm.cmd run lint` and `npm.cmd run build` passed. No live Upstox request was made during verification; provider behavior still needs runtime confirmation with the configured account/token. Existing unrelated worktree changes remain in place.

**2026-10-26:** Implemented shared batched quote refresh for Dashboard and Watchlist, with 60-second freshness caching, explicit force refresh, per-batch 10-second timeout, stale-symbol reporting, and persisted refresh timestamps. Both pages refresh before fetching portfolio/watchlist data so calculated portfolio values use the latest saved quotes. Added refresh-service unit tests. Verification: `./mvnw.cmd test` passed (16 tests), `npm.cmd run lint` passed, and `npm.cmd run build` passed. No live Upstox request was made. Next: run the app with the configured Upstox credentials and verify batch quote response mapping; then add rate-limit retry/backoff and provider-level tests if needed.

**2026-10-26:** Added company name and ticker to each row in the Dashboard's recent orders table. The existing order API already supplies both fields, so no backend/API change was needed. Verification: `npm.cmd run lint` and `npm.cmd run build` passed from `frontend`.

**2026-10-26:** Added a single bounded retry (1-second delay) for HTTP 429 in batched Upstox quote requests. Added provider tests using a localhost HTTP stub, including response mapping from `NSE_EQ:SYMBOL`, partial quote responses, retry success, repeated 429, and HTTP 500. Verification: `./mvnw.cmd test` passed (20 tests); `npm.cmd run lint` and `npm.cmd run build` passed. Attempted the approved live refresh through the existing authenticated Chrome session, but Windows computer use stopped while loading `/dashboard` because it could not verify the browser URL. Whether the frontend refresh request completed is unknown, and no live quote result was verified. Retry the live check manually from the authenticated Dashboard when convenient. No credentials were read or exposed.

**2026-10-26:** Added an Upstox NSE exchange-status lookup and a backend guard for simulated buys and sells. Orders proceed only on `NORMAL_OPEN`; closed sessions return a user-facing message, while status errors fail closed with HTTP 503. Added coverage for open, closed, pre-open, closing, unknown, null, and unavailable statuses, and verified rejected buy/sell calls do not touch order, user, stock, or holding repositories. Verification: `./mvnw.cmd test` passed (27 tests); `npm.cmd run lint` and `npm.cmd run build` passed. No live market-status request was verified. Next: check the behavior through the authenticated app with the configured Upstox account; the backend currently listening may need a restart to load the changes.

**2026-10-28:** Added shared NIFTY 50/SENSEX snapshot cards to Dashboard and Market Overview, including one-minute refresh, manual refresh, unavailable/stale messaging, INR formatting, daily movement, and chart links. Dashboard stock cards are ranked by daily percentage change and limited to five, with a link to the full stock list. Extended Upstox quote/history symbol resolution so the index chart links can load index data. Verification: `npm.cmd run lint`, `npm.cmd run build`, and `./mvnw.cmd test` passed (27 tests). Responsive layouts use mobile breakpoints; no authenticated live Upstox or browser visual check was performed. `git diff --check` reported a trailing blank line in the already-modified `src/main/java/com/example/investsmart/test.http`; it was left untouched. Next: confirm index quote and candle behavior through the authenticated app with the configured Upstox account.

**2026-10-28:** Stabilized Upstox networking and history caching. All provider requests now use the injectable HTTP client and configurable `upstox.api.base-url`; HTTP errors and Upstox error payloads are checked, index previous-close uses the quote response instead of extra candle calls, and raw provider bodies/tokens are not logged. 1D/1W intraday candles bypass daily persistence; partial daily refreshes are returned to the caller but do not replace cached rows. Added provider and cache tests for index response mapping, index candle keys/intervals, HTTP/provider errors, intraday bypass, complete cache replacement, and partial-response preservation. `./mvnw.cmd test` passed (33 tests). `git diff --check` still reports the pre-existing blank line at EOF in `src/main/java/com/example/investsmart/test.http`; no frontend files changed in this task. User confirmed index values are now visible after changing network connection. Next: restart the local backend so the provider changes take effect, then verify chart candles/periods and market-status behavior in the authenticated app; reconfirm account display permissions/terms before deployment.

**2026-10-28:** Reworked `/market/chart/:symbol` into an immersive full-viewport dark chart terminal. It now has a compact branded header with Markets/Logout actions, NIFTY/SENSEX strip, responsive tall candlestick/volume chart, and the signed-in user's complete scrollable watchlist with company name, current price, and daily move. Watchlist refresh uses the existing cached quote refresh API and falls back to saved values; watchlist entries navigate to their charts. No backend/API changes. `npm.cmd run lint` and `npm.cmd run build` passed. Browser screenshot inspection was attempted, but the computer-use helper timed out listing apps before any browser could be selected. Next: open a chart from Markets and visually check the desktop/mobile layouts and scroll behavior; existing local backend may need restart for prior backend changes.

**2026-10-28:** Added a Watchlist search-and-add panel with SENSEX constituent symbol shortcuts; selected instruments are Upstox-searched and imported into the local simulated stock catalog only when needed, then added to the user's list. Starter stock and Dashboard stock cards now open the full chart terminal, as do Watchlist cards. Corrected the 1D chart provider to use Upstox's dedicated intraday candle endpoint first, retaining the recent historical candles as a fallback. Index quote mapping uses `prev_ohlc.close` and keeps previous close unavailable when omitted, so the chart strip no longer fabricates a 0.00 move. Tightened the exchange badge-to-company heading gap. No tests/build were run in this task. Next: restart backend and verify 1D history/index changes with the configured Upstox account; visually review Watchlist add flow and chart navigation.

**2026-10-28:** Kept Dashboard's Your stocks section and added a scrollable three-entry Watchlist preview with chart links and unavailable-price states. Chart history now renders independently of quote availability, supports retry, and reports when candle data is empty/invalid after chart filtering. Index reads independently preserve each index and fall back from OHLC V3 to Full Market Quotes V3 for missing current/previous-close values; missing quotes remain null and are clearly labeled. Watchlist search combines local catalog results with Upstox search, and validated stock imports now save without prices if quote retrieval fails, allowing the selected instrument to be added. Generic API failures now log sanitized route/type/message details. Added 1D intraday/fallback, index fallback/unavailable, and quote-unavailable import tests. Verification: `./mvnw.cmd test` passed (38 tests); `npm.cmd run lint` and `npm.cmd run build` passed. No local listeners were available on 8080/5173, so live Upstox and authenticated browser behavior remain unverified. Next: restart frontend/backend and verify the four reported flows using the configured network and Upstox account.

**2026-10-28:** Added a last-resort previous-close lookup for NIFTY 50 and SENSEX. When both OHLC and full-quote responses omit previous close, the backend uses the quote session timestamp and recent Upstox daily candles to select the latest candle before that session. If the session timestamp or prior candle is unavailable, previous close remains null. Added provider tests for fallback success, direct-field precedence, and insufficient candle history. Verification: `./mvnw.cmd -Dtest=UpstoxMarketDataProviderTests test` passed (17 tests). Live Upstox behavior was not exercised; restart the backend and verify the index cards in the authenticated app.

**2026-10-28:** Expanded the index quote/chart universe to NIFTY 50, SENSEX, NIFTY BANK, and NIFTY IT. Added `GET /market/heatmap` for ten featured NSE equities, a TradingView-style daily-change tile grid, and Watchlist/Heatmap controls in the chart terminal side panel. Demo quotes cover the expanded universe. Verification: focused backend tests passed (18 tests); `npm.cmd run lint` and `npm.cmd run build` passed. No authenticated live Upstox or visual browser check was performed. Restart the backend and frontend, then confirm all four indices and the featured-stock tiles with the configured account.

**Authentication/UI follow-up:** Successful email verification now replaces the verification URL with the login page and carries a sign-in confirmation, so the token is no longer left in the address bar. Incomplete WebAuthn option payloads now produce an actionable refresh message rather than a JavaScript `replace` exception. Account session details wrap on narrow screens, and dashboard portfolio values, index snapshots, and holding/watchlist details have been modestly enlarged. The supplied legacy account credentials were not used and verification was not bypassed; reset any password shared in chat. Verification: frontend tests passed (21), lint and production build passed, and the local frontend returned HTTP 200. The local SMTP status endpoint reports configured with no recorded delivery issue; inbox delivery is not independently verified.

**2026-11-04:** Updated the stock chart header to show a compact BSE/NSE exchange badge using the selected listing, and made index detection account for the displayed company name so indices do not expose equity trade actions. Removed the redundant Buy/Sell selector from the order dialog; the selected action is shown as a BUY/SELL ORDER label, with the original chart buttons remaining the action selectors. The 1D chart now draws labeled dashed green day-high and red day-low levels from the live quote when those values are available. Verification: frontend unit tests passed (21), lint and production build passed. Visual behavior with live BSE/NSE quotes should still be confirmed in the browser.

**2026-11-04:** Fixed trusted-device setup/unlock option parsing: Yubico WebAuthn serializes options as a `{ "publicKey": ... }` object, so the browser adapter now unwraps it before decoding the challenge and credential IDs. Added a backend integration assertion for this response shape. Dashboard typography and summary/index/holding/order details were enlarged for readability, with responsive sizes retained. Verification: frontend tests (21), lint, production build, and the focused passkey security integration test passed.

**2026-11-04:** Aligned the local development origin with its WebAuthn RP ID (`127.0.0.1`). The local app default now uses that origin, and the Vite app redirects `localhost:5173` to `127.0.0.1:5173` so browser origin and configured RP ID agree. A passkey `SecurityError` gives the canonical local URL and sign-in retry guidance. Production HTTPS origin/RP ID validation remains unchanged.

**2026-11-04:** Removed passkey enrollment and unlock prompts from the user interface. Ordinary users now sign in with email/password for new or expired sessions; sign out still revokes the session. Administrator password plus authenticator MFA is unchanged. The legacy `/unlock` route redirects to sign-in; existing passkey records and backend implementation are retained but no longer exposed through the UI. Removed the now-unneeded local hostname redirect and restored `localhost` as the default public URL.

**2026-11-04:** Widened the centered content area across Dashboard, Markets, Portfolio, Orders, Account and Admin, and increased general app text sizing for desktop readability. Standardized default page action buttons with consistent sizing, spacing, borders, hover/focus feedback and disabled states while preserving custom tab, heatmap, chart and primary-action styles. Responsive sizing remains in place. Frontend lint, build and unit tests passed.

**2026-11-04:** Polished action outlines in the virtual-funds flow and chart stock-search dialog so outlined/primary actions remain distinct despite shared button styles. Added a draggable, keyboard-adjustable chart/watchlist divider on desktop; its sidebar width is saved locally, with the stacked mobile layout unchanged.

**Market UI follow-up:** Centralized index detection across route symbols, display names, quote symbols, and exchange index instrument keys, including normalized aliases, so index charts remain non-tradable. The index card grid is rendered once per Market view; the chart terminal has one conditional trade-control slot. Fixed stock-search input sizing when the Clear action is present and added dedicated, responsive styling for search results on the Market page. Verification: 22 frontend tests, lint, production build, and changed-file whitespace checks passed. Verify the chart in a refreshed browser bundle if old duplicate controls remain visible; no browser visual check was available.

**Markets redesign:** Reworked Markets Explore into a denser market-desk layout with a featured-equities table, clear chart links, exchange labels, price/change columns, a dedicated index sidebar, refined search, and mobile breakpoints. Existing Watchlist/IPOs/Bonds tabs and chart routes remain unchanged.

**Market spacing and trade actions:** Rebuilt Watchlist cards into compact responsive quote cards with aligned price/change, day-range details and actions. In chart trading, the Buy/Sell selector buttons are removed while the order ticket is open, and the final action is explicitly labeled “Confirm buy/sell” to avoid a second visible set of Buy/Sell controls. Tightened action spacing and aligned market controls. Verification: frontend lint, production build, all 22 frontend tests, IDE inspections and changed-file whitespace checks passed. Browser visual inspection was not available.

**Chart switching and market search polish:** The chart route now remounts on instrument/exchange changes, resetting instrument-scoped UI state, and any open native order dialog is explicitly closed during unmount. This prevents a stale ticket from remaining over the newly selected chart. Scoped Markets search styles now reset chart-terminal surface rules and use the full content width, eliminating the nested tinted/bordered search panel; results retain clear responsive row styling. Verification: frontend lint, production build, all 22 frontend tests and changed-file whitespace checks passed.

**Virtual funds controls:** Moved “Refresh balance” into the same responsive action row as Add and Withdraw, with a subdued outlined secondary style so all three actions align consistently.

**Brand heading:** Styled the main InvestSmart navbar wordmark with “Invest” in dark text and “Smart” in the same green accent used in the market graph UI.

**Order ticket exchange selector:** Refined the NSE/BSE choice cards with consistent spacing, a full-width Exchange label, legible listing-symbol badges, clear selected/hover states, and compact responsive wrapping. Buy/sell accent color remains tied to the selected order side.

**Order ticket status/actions:** Right-aligned the BUY ORDER/SELL ORDER indicator as a bold side-colored badge and styled Reload details as a properly spaced secondary action, including its hover/focus states.

Updated the order-side badge to use an explicit right-aligned flex row (avoiding inherited paragraph margins) and grouped the final-price/session guidance with order validation and market-closed messages in a padded, readable notice panel.

The order badge sits slightly inset from the left content edge (for both Buy and Sell), with a reduced inset for closer alignment to the exchange selector; the conditional Reload details button remains indented and separated from surrounding notices/footer.

**Admin console — first step:** Reworked the existing owner-only overview into an operations dashboard with linked account/security metrics, the existing unsent-mail queue count, and direct links to Users, Alerts and Audit history. The queue is labeled as awaiting delivery and explicitly does not claim SMTP or inbox success. No backend privileges or account data actions changed.

**Owner email setup:** Added `INVESTSMART_ADMIN_EMAIL` as an environment-based selector for the existing deliberate one-time owner bootstrap. It only takes effect with `--bootstrap-admin=true` in non-web mode; the account must already exist and have verified its email, and a second owner is still rejected. The prior explicit CLI email argument remains supported. Documented that `MAIL_FROM` only identifies the mail sender, not the Admin identity. Added tests for env-selected bootstrap and refusal to bootstrap in web mode.

**Admin user deletion:** Added permanent deletion for regular accounts from the user detail view. It requires the owner Admin's recent password+authenticator confirmation and matching target email; accounts with no usable email can be explicitly confirmed by typing `DELETE <id>`. The reason is optional and stored as blank when omitted. It removes account-associated portfolio, trading, investment, session, passkey and prior queued-mail data while retaining audit/security history with the account link removed. Related alerts are detached, resolved and marked reviewed, updating open/unread counts while preserving alert history. A new encrypted professional deletion notice is queued and immediately attempted only when the account has an email address; otherwise no mail is attempted. Blank reasons use standard wording. The deletion response returns refreshed overview counts for immediate UI updates. Owner/Admin accounts cannot be deleted here; SMTP acceptance is not evidence of inbox receipt.

**Admin user-detail error:** The local runtime reported a `NullPointerException` while serializing portfolio data for a legacy account whose virtual balance is missing. Portfolio valuation is now reported as unavailable for that record, and total-cash serialization handles the missing balance without throwing. Added an integration test covering `GET /admin/users/{id}` for this legacy case.

**Intermittent blank workspace after refresh:** Admin alert rendering now tolerates missing/unknown severity values instead of calling `.toLowerCase()` on null, and optional user email checks are null-safe. Added a root React error boundary so any remaining render exception displays a refreshable diagnostic screen and is logged to the browser console instead of leaving a blank page. Frontend lint and production build pass; if the boundary appears, capture the console error to identify any remaining data-specific failure.

**Admin alert attribution and aging:** Alert list/detail and individual event records now include the linked account email; source-wide alerts are labeled as not tied to one account. Audit history displays actor and affected-account email addresses, and records automatic archival as a `System` action. Open alerts with no new activity for 24 hours are marked resolved/seen and moved into Audit history; aging runs hourly and also on overview/alert-list reads. The Alerts page defaults to open alerts, retains resolved history via its filter, and refreshes every 30 seconds. A new integration test verifies email attribution and the 24-hour archive/audit flow. Verification: `SecurityIntegrationTests` passed; frontend ESLint and production build passed. SMTP/inbox delivery is unrelated and remains unverified.

**2026-11-04 — Admin V2 controls and operations:** Reorganized the owner console into exactly Overview, Users, Security, Operations and Audit, preserving `?tab=alerts` links as Security. Added verified/recent-registration metrics; registration dates; active-session last activity and per-session revocation; rate-limited Admin resend verification; forced password reset that revokes sessions and blocks login until a valid reset is completed; and reasoned virtual-balance adjustments stored in an append-only ledger. Sensitive account, session, balance and alert-review actions enforce recent Admin password+authenticator confirmation. Account deletion anonymizes retained balance-adjustment links. Security rules now create alerts for repeated recovery requests, rejected orders and unusual Admin activity. Operations exposes order filtering, separately retained rejected attempts, SMTP queue/failure state without bodies/links/tokens, quote freshness/Upstox request telemetry and IPO settlement/job status. Provider-backed IPO/bond entries support audited availability controls and validated full IPO date overrides without modifying provider data; due pending settlements can be safely retried using the existing guarded transaction, not manually edited. Upstox and settlement-job health timestamps and cumulative failures now persist across restarts. Order rejections remain security events rather than order rows. Updated fresh MySQL baselines and `DEPLOYMENT.md`; inspect the live schema before applying `20261105_admin_v2.sql`, `20261104_admin_listing_overrides.sql` and `20261104_admin_runtime_health.sql`. No MySQL production migration, SMTP receipt, provider permission check or browser visual acceptance was performed.

Verification: the full Maven suite passed **128 tests, 0 failures/errors, 8 skipped**, including the invalid-IPO-date-order guard; the IDE project build succeeded. Frontend ESLint, all **22** API/auth tests and the production build passed. Launch gates still require authorized external services, actual inbox receipt, isolated real-MySQL migration/restore rehearsal, provider-scope confirmation and browser/device acceptance.

- [~] Simple checklist and overview synchronized with93passing local tests; MySQL checks running, no completion claim. Reporting semantics/inventory final result update pending. NEXT inspect enabled Maven result.


- [~] Adapter ownership, range, batch/sentinel and incomplete-product semantics recorded in CORE05_REPORTING_SEMANTICS.md and V3_FEATURE_INVENTORY.md. MySQL result pending. NEXT inspect final suite then synchronize acceptance and source-hash evidence.


- [~] Supplied MIS linkage semantics, simple checklist/overview and inventory synchronized while tests run. Owned period reader unchanged and still flags MIS linkage unchecked. NEXT inspect Maven result; no MySQL acceptance claimed for this pure rule.


- [x] Initial affected suite exit0:99tests/0failures/0errors/0skipped, including six new pure MIS cases. Log target/core05bg03-mis-slice.log; counts target/core05bg03-mis-slice-initial-verification.json. Seventh new persisted/owned agreement case was added after compilation and requires final rerun.
- [ ] NEXT cmd.exe /d /c ".\mvnw.cmd -q -Dtest=IntradayTicketSliceReconcilerTests,IntradayFeeServiceTests test > target\core05bg03-mis-slice-final.log 2>&1"; pending.


- [~] Initial99-test result and exact pure-versus-owned/MySQL scope documented. Changed Java whitespace PASS. Final agreement rerun pending; NEXT final result, checklist synchronization and local-link/source-hash QA.


- [x] Local affected Maven suite exit0:104tests/0failures/0errors/0skipped, including four new owned integration cases. Log target/core05bg03-mis-period.log; counts target/core05bg03-mis-period-local-verification.json. NEXT enabled MySQL plus same affected suite; final results pending.


- [~] Semantics, inventory, checklist and overview synchronized with104local tests and running MySQL checks. Explicit event-only/other-product gaps retained. NEXT final suite and cleanup evidence; B-G03 acceptance pending.


- [~] Retained-provenance decision table, instruction checksum contract, simple checklist/overview and inventory synchronized while tests run. Refund/parent-only gaps deliberately remain unavailable. NEXT inspect Maven result; bounded owned policy adapter and MySQL remain pending.


- [x] Local affected Maven suite exit0:113tests/0failures/0errors/0skipped (six new adapter tests). Log target/core05bg04-owned-policy.log; counts target/core05bg04-owned-policy-local-verification.json. NEXT enabled MySQL plus affected suite; pending.


- [~] Reader contract/scan-cost limitations and checklist/overview/inventory synchronized while MySQL runs. NEXT final result and cleanup/source-hash QA; no future combined-summary acceptance claimed.


- [x] Changed Java whitespace PASS; overview/checklist local Markdown links PASS. Final MySQL result still pending; NEXT inspect result and test-container cleanup before marking reader verified.


- [~] Eligibility scope table and same-snapshot/no-total constraints recorded; checklist/overview/inventory synchronized while tests run. NEXT inspect Maven results then final QA. Pure rule not integrated; no MySQL acceptance claimed.


- [x] Local affected Maven suite exit0:112tests/0failures/0errors/0skipped, including four new owned integration cases. Log target/core05bg05-owned-eligibility.log; counts target/core05bg05-owned-eligibility-local-verification.json. NEXT enabled MySQL plus affected suite; pending.


- [~] Owned eligibility contract, DTO cache invalidation and no-extra-query constraints documented; checklist/overview/inventory synchronized while MySQL runs. NEXT final result and cleanup/QA. No complete summary acceptance claimed.


- [x] Changed Java whitespace and overview/checklist local Markdown links PASS. MySQL final result remains pending. NEXT final suite report and test-owned container cleanup; no release gate promotion.


- [~] Signed direct-change/null-zero/precision semantics and checklist/overview/inventory synchronized while tests run. NEXT inspect Maven result; pure calculation is not yet attached to owned reader and no MySQL acceptance implied.


- [x] Local affected Maven suite exit0:126tests/0failures/0errors/0skipped, including five new owned sum cases. Log target/core05bg05-owned-changes.log; counts target/core05bg05-owned-changes-local-verification.json. NEXT enabled MySQL plus affected suite; pending.


- [~] Same-snapshot selected amount contract/cache invalidation and checklist/overview/inventory synchronized while MySQL runs. NEXT final result/cleanup/QA; no broader account report acceptance claimed.


- [x] Changed Java whitespace and overview/checklist local Markdown links PASS. MySQL final result pending. Remaining B-G05 review identifies PeriodChargeAdapter already computes full ChargeEvidence but grouper returns identity partition only; any future composite cash summary must retain/revalidate that existing proof rather than infer from grouped IDs. NEXT final current suite then scoped composite-proof contract.


- [~] Contract/global-partition limits and checklist/overview/inventory synchronized while tests run. NEXT Maven result then final QA; no owned integration or MySQL acceptance claimed.


- [~] Owned proof retention/global limits/no-new-sum contract documented; checklist/overview/inventory synchronized while local verification runs. NEXT local result and enabled MySQL. No release acceptance claimed.


- [x] Local affected suite exit0:134tests/0failures/0errors/0skipped. Log target/core05bg05-owned-group-source.log; counts target/core05bg05-owned-group-source-local-verification.json. NEXT enabled MySQL plus affected suite; pending.


- [x] Changed Java whitespace and overview/checklist local Markdown links PASS. MySQL final result pending; NEXT final reports/cleanup and source hashes. No composite cash amount or release readiness promoted.


## 2026-11-27 grouped cash tests written

- [~] Six cases cover actual writer negative debt/ordinary cash once/no writes, outside-child/child-only windows, duplicate/missing/stale proof, null/limits/source corruption, stale eligibility and refund bucket distinction. NEXT affected suite; pending. Pure calculation not attached to reader.


- [~] Grouped scope/complete-window/null-zero semantics and checklist/overview/inventory synchronized while tests run. NEXT result and final QA; no MySQL acceptance claimed for pure rule.


## 2026-11-27 grouped sum integration tests written

- [~] Four cases cover owned negative-debt charge once/direct scope unchanged/foreign denial, partial context and corruption raw retention, empty zero/cache invalidation, and same independent repeatable snapshot/fixed existing queries/no per-charge service reads. Fixture pure dependencies simplified to shared local rules. NEXT affected suite; pending. MySQL extension next.


## 2026-11-27 grouped sums MySQL assertions written

- [~] Full DTO old/new source-change/restart/dump tests include grouped result. Explicit parent-only partial null versus complete parent/children -40.00settled/0.00reserved; conflict/deleted child null and repaired/restarted/restored complete DTO equality. NEXT local result then enabled MySQL suite; pending. No complete account totals claimed.


- [~] Same-snapshot grouped result/cache invalidation semantics and checklist/overview/inventory synchronized. NEXT local result then enabled MySQL. No broader report readiness claimed.


- [x] Local affected Maven suite exit0:144tests/0failures/0errors/0skipped, including four new owned grouped cases. Log target/core05bg05-owned-grouped-changes.log; counts target/core05bg05-owned-grouped-changes-local-verification.json. NEXT enabled MySQL suite; pending.


- [x] Changed Java whitespace and overview/checklist local Markdown links PASS. Final MySQL result pending; NEXT inspect full result/cleanup before marking owned grouped calculation verified.


- [x] Final financing production hash check PASS; six changed documentation files whitespace/local Markdown links PASS. Checklist/overview/inventory synchronized. No tests rerun or scan-performance acceptance claimed. Evidence target/core05bg05-acceptance-review-verification.json. NEXT B-G06 actual-query measurement implementation as above.


## 2026-11-27 B-G06 actual-query capture written

- [~] Added synthetic-test-only JDBC wrapper recording successfully executed prepared SELECT SQL/bind values, normalizing timestamps/dates and preserving null/reused parameters. No credential capture or production schema/query change. Two tests compare actual owned period-repository results and query/parameter capture, immutability, null/reuse and write exclusion. NEXT focused Maven test; pending. MySQL plans/workload NOT measured yet.

- [x] Continuation commit bef9ffee6141c7f30aa45336062f5feb8613add0 pushed and independently matched to remote progress branch. Exact changed-blob review9files/0secret indicators, staged whitespace PASS. Two B-G06tests pass; source backup and continuation upload complete. NEXT isolated actual-query measurement as above; no performance acceptance. This final checkpoint belongs to the same upload unit.

- [~] Static fixture refinement: foreign fill rows use distinct deterministic ticket IDs to preserve unique(ticket,leg). Initial compile/capture test running; final enabled suite will compile latest source before measurement. NEXT local result.


- [x] Initial cmd.exe /d /c ".\mvnw.cmd -q -Dtest=ReportingQueryCaptureTests test > target\core05bg06-cost-compile.log 2>&1" exit0:2tests/0failures/0errors/0skipped. Probe fixture refinement follows initial compilation; NEXT final enabled command compiles current source and measures isolated MySQL. Both V3_JOURNAL_MYSQL and V3_REPORT_COST process-local flags restored in finally. Results pending.


- [~] Probe scope/measurement counter distinctions and checklist/overview/inventory synchronized while enabled suite runs. NEXT inspect report, compare growth and remaining gaps; no benchmark PASS/SLA asserted from implementation alone.


- [~] Cleanup diagnostic: an additional docker label-format guard had Windows quoting failure; the preceding docker ps --filter label=investsmart.scope=journal-synthetic inventory had already positively identified the exact09c728d9container. Only that exact test-owned container was removed. Future inspection should parse docker inspect JSON rather than embed quoted Go-template keys. Enabled rerun now active; no result yet.

- [x] Final8file scoped whitespace and overview/checklist local links PASS;20observations/3executed tests verified. Evidence target/core05bg06-query-cost-verification.json. No remaining test failure; scan-cost acceptance remains pending for the stated matrix. NEXT B-G06 remaining measurement cases.

- [~] Simple four-group reporting breakdown and owned-candidate measurement scope synchronized in checklist/overview/semantics/inventory. Not a promised fixed test count or newly imposed live-AI prerequisite. NEXT enabled result/cleanup and remaining group1 cases.

- [x] Final7file whitespace/local checklist-overview links PASS;29observations/3fixed-query scenarios/16executed tests verified. Evidence target/core05bg06-owned-candidate-verification.json. No remaining test failure; performance acceptance open. NEXT remaining group1 cases as above.

- [~] History/CPU/range scope and checklist/overview/inventory synchronized while enabled test runs. NEXT inspect result, checkpoint evidence, then continue remaining matrix within session limits. No elapsed-time/CPU conflation or economic-transaction claim.


- [~] Dedicated fixture/proof-versus-history scope and simple checklist/overview/inventory synchronized while matrix runs. NEXT inspect test/report results before global overflow/acceptance review.


- [~] Global-guard/adversarial-data and disposable-index boundaries documented; checklist/overview/inventory synchronized while test runs. NEXT final result, dropped-index/cleanup evidence, then assess concrete remediation impact without applying production DDL.


- [x] Focused prepared matching command exit0:20tests/0failures/0errors/0skipped, log target/core05bg06-prepared-local.log; counts target/core05bg06-prepared-local-verification.json. Five new equivalence/request-isolation/limits/invalid-source/encoding cases pass. NEXT full recovery + dedicated measurement + affected suites; schema-index approval remains pending and generic continue instruction does not authorize retained DDL.


- [~] Matching change/snapshot-isolation boundary and structural acceptance rubric documented; checklist/overview/inventory synchronized while full run proceeds. NEXT verified before/after results and source hashes; no timing-only SLA or implicit schema approval.


## 2026-11-27 B-G06 evidence validator written

- [~] Independent Node built-in validator checks local report structure, actual/estimated plans,3repeats, nonnegative finite timings/handler counters and1001output bound; prepared CPU comparison required on request, unsupported CPU null preserved. Never authenticates source or grants performance/SLA acceptance. Four rejection/positive tests written. NEXT Node tests while MySQL continues; no new dependency/schema/AI change.


- [x] Initial Node evidence tests4/4and real110observation report structure PASS, performanceAcceptance false. Hardened CLI invalid-JSON error to avoid raw parser snippets and added non-disclosure CLI fixture test; NEXT final5test run. New files only use Node built-ins; MySQL still running.
