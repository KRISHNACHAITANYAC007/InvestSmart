 # InvestSmart release-readiness report

## Administrative note — 2026-11-23

**PAUSED BY OWNER DECISION**: prioritizing V3 development before public launch. Latest verified historical status remains **7 PASS / 2 FAIL / 17 BLOCKED — production NO-GO**. No gates were rerun or promoted by this documentation transition. All historical conclusions and artifact evidence below retain their original snapshot scope; V3 needs a new candidate and readiness assessment. M01–M32 remain **ON HOLD — V3 DEVELOPMENT IN PROGRESS** in the canonical DEPLOYMENT_CHECKLIST.md and its portable guide. See V3_DEVELOPMENT_PLAN.md for the approved roadmap.

## SEC-03 investigation: 2026-11-16

Local bounded credential investigation refreshed without live authentication, remote fetch, credential change, database access or history mutation.2reachable commits/107blobs;74current/67history indicators. Scratch14distinct JWTs have past expiry claims (signatures unverified);20password fields represent2distinct password values. Context is local-app requests; no proven live provider credential. No exact password reuse in the one existing targeted private settings file checked; active accounts/external reuse remain unknown. GitHub remote and one remote-tracking ref do not establish visibility/publication. Scanner redaction tests3/3PASS. Full scope/limits: SEC03_CREDENTIAL_EXPOSURE_REPORT.md; aggregate evidence target/sec03-current-history-review.json and target/sec03-expiry-reuse-summary.json. Precise owner-only steps remain exclusively in canonical checklist step2. **SEC-03 BLOCKED; totals7PASS/2FAIL/17BLOCKED, production NO-GO.** Benchmark/hosting held per user instruction. Other release gates are not recertified by this investigation.

## Legal/creator frontend change: 2026-11-16

Development chart import follow-up: Console trace confirmed Vite Outdated Optimize Dep504. Chart startup prebundling/test-cache isolation and force-restarted confirmed5173 restored fresh dependency HTTP200 after54/54tests; lint/build PASS. No production runtime/auth/database change. This is development import-path evidence, not rendered/device or full release-candidate acceptance. Counts remain7/2/17.

Public draft legal/About routes and user-approved frontend-only acknowledgment added; creator refinement uses plain by KC beneath auth InvestSmart with no trademark and collapsed legal links. API/authentication protocols, backend, database and deployment configuration unchanged. 54 frontend tests passed; lint and build passed before the final trademark text removal, with final checks recorded in WORK_QUEUE.md. SSR public-route checks do not certify rendered/mobile/device UX. Browser host/current-URL safety barrier prevented prior visual acceptance and was not bypassed. Legal review, verified public contact and provider rights remain GOV-01 dependencies in canonical checklist. New bundle requires a new verified release candidate; dzYbes is historical snapshot evidence only. Counts unchanged **7PASS/2FAIL/17BLOCKED**, production NO-GO.

## Manual-guide handoff:2026-11-16

WHAT_TO_DO_MANUALLY.txt now provides32 ordered personal task groups/201unchecked actions, all19failed/blocked gates, cost/source references, safe completion messages and explicit separate deployment/exposure approvals. It is a user-requested portable expansion of the canonical checklist, not evidence that manual acceptance occurred. Guide QA: no missing gate or known credential value/indicator. Local dependency adoption was safely completed (no live frontend process observed): ci/tree/lint/51tests/build passed and9assets remained byte-identical to dzYbes. Todaytooling40passed, npm audit0 and Maven OSV143/0; code/config and candidate archive remain unchanged, so prior332backend/runtime evidence retains its scope. No new full-duration measurement, physical acceptance, host/provider approval or public deployment. **Gate counts remain7PASS/2FAIL/17BLOCKED; production NO-GO.** Details: target/manual-guide-qa.json, manual-guide-local-adoption.json and manual-guide-local-verification.json.


## Current template/runner continuation:2026-11-15

**Production NO-GO. Initial and final:7PASS /2FAIL /17BLOCKED.** The24 focused V2 gates remain7PASS/2FAIL/15BLOCKED; two P2 expansion gates are deferred. Local template/configuration preparation is complete, but no external or physical acceptance was inferred from it.

| Fresh evidence | Result | Location |
| --- | --- | --- |
| Backend | PASS:332total,317executed,15opt-in skips,0failures/errors | target/release-checks/backend.log; targeted template/calendar/mail tests26/26 also passed |
| Frontend | PASS:51tests,lint/build,isolated clean install; full+production npm audits0 | target/release-checks/frontend-*.log; target/templates-production-audit.log |
| Maven advisories | PASS:143resolved coordinates/0OSV matches,complete | target/release-checks/backend-advisories.json |
| Tooling | PASS:40tests | target/templates-tooling.log; current release-tests.log |
| Configuration templates | PASS within synthetic scope:empty inputs fail closed; filled fixture passes Compose/staging preflight and merged base/staging/capacity config --quiet | target/templates-compose-validation.json; no services or real configuration used |
| Calendar/SMTP policy | PASS locally:header-only calendar rejected; worksheet has no activation/dates; STARTTLS and server identity explicit | ProductionRequirementsTests/SettlementCalendarTests and deployment-templates.test.mjs; no actual mail/provider/calendar accepted |
| Packaged runtime | PASS:MySQL6/6,migration parity,restore/restart,local HTTPS/CSRF/headers/413,staging restrictions | target/release-checks/smoke-3ztP0Z/result.json |
| Candidate | PASS for package only:56payload checksums,17migrations,portable inspection | target/templates-independent-verification.json |
| Secret inspection | Same credential-review blocker; exact501payload/JAR/log files had0known-sensitive matches | target/templates-secret-history.json; target/templates-artifact-secret-review.json; no credentials changed/exposed |
| Runner preparation | Manual-only Linux CI workflow prepared and safe-parser syntax/manual/read-only/bounded-job contract checked, not published or executed | target/templates-runner-availability.json; .github/workflows/capacity.yml |
| Full load / leak acceptance | BLOCKED; previous two capacity FAIL states retained | Restricted Windows policy unchanged; no approved independent published current snapshot/host; no repeated35-minute attempt |

Candidate: [InvestSmart-v2-dzYbes.tar.gz](target/releases/InvestSmart-v2-dzYbes.tar.gz), SHA256 `beefbe50cd6b1b89aaa2266d3c30a6ddf3e60fb550f1fadb3ec52ba72ad0ab3f`. Source fingerprint `566f060654dea4fc3a40a840879eb283c0df371bc40c80e57a15a405145c8785` covers373 scoped code/config/template/workflow inputs. Inputs did not change during final verification; payload checksums certify its own packaged snapshot and productionLaunchApproved=false. Manual CI workflow is source-checkout tooling, not part of the portable runtime archive.

Additional local work completed: annotated hosting/SMTP/Upstox/staging template; Compose-only validation for missing/reused DB/root/JWT/encryption secrets; shipped header-only CSV and review worksheet; explicit SMTP certificate identity setting; prepared isolated Linux CI alternative with manual dispatch only, protected-file guard, read-only permissions, timeout/concurrency bound and aggregate-only artifacts. Current Angus already defaults identity checking to true; this is an explicit policy pin, not a claim of a previously demonstrated vulnerability ([provider documentation](https://eclipse-ee4j.github.io/angus-mail/docs/api/org.eclipse.angus.mail/org/eclipse/angus/mail/smtp/package-summary.html)).

No authenticated independent runner for the current snapshot was established. GitHub CLI is absent; connected workflow tools do not provide a new workflow dispatch here. The repository has uncommitted/untracked current code and credential-bearing scratch material, so no source was uploaded or pushed. CI will require a reviewed publication decision and runner/billing authorization; a configured GitHub connector alone does not certify current-source execution. Exact operator actions are only in DEPLOYMENT_CHECKLIST.md (manual steps1-8, including the CI alternative). No deployment, real mail/broker action, credential change, history rewrite or policy bypass occurred.


## Previous hardening follow-up:2026-11-15

**Production remains BLOCKED / NO-GO. Initial:7PASS,2FAIL,17BLOCKED. Final:7PASS,2FAIL,17BLOCKED.** This catalogue includes two deferred P2 gates; focused V2 has24 applicable gates (7PASS/2FAIL/15BLOCKED). Local code/configuration defects were fixed; unverified acceptance was not promoted to PASS. The two FAIL states retain the unsuccessful interrupted capacity attempt, not a proven application-capacity regression. No new full run was attempted against the unchanged standby/execution-policy blocker.

| Fresh verification | Result | Exact evidence |
| --- | --- | --- |
| Backend regression | PASS:330 tests,0failures/errors,15 opt-in skips | target/release-checks/backend.log; wallet/trading/settlement/auth/portfolio regressions included; skips not represented as executed tests |
| Backend known-advisory audit | PASS:143 resolved coordinates,0 OSV findings,complete | target/release-checks/backend-advisories.json |
| Frontend | PASS:isolated clean install, full+production npm audits0,51tests,lint/build | target/release-checks/frontend-*.log; target/followup-production-audit.log |
| Verification tooling | PASS:36 tests | target/followup-tooling-final.log and current release-tests.log |
| Actual packaged runtime | PASS:MySQL6/6, migration parity, restore/restart, HTTPS/CSRF, browser headers,16MB body limit/413 and staging restriction | target/release-checks/smoke-neEV1e/result.json,10.log and target/followup-final-build.log |
| Exact source/proxy inputs | PASS:no code/config changes during final run; checked production/staging Caddy files match source | target/followup-final-source.json and followup-source-comparison.json |
| Candidate archive | PASS:SHA256,54payload checksums,17migration SQL files,portable artifact checks | target/followup-independent-verification.json |
| Artifact/log known-value scan | PASS in scope:499files,0matches | target/followup-artifact-secret-review.json; no unknown-secret or history clearance implied |
| Local/history exposure review | Completed read-only; credential clearance BLOCKED | target/followup-secret-history-final.json:2commits/107blobs;74current/67historical indicators; owner reuse/remote history unverified |
| Production environment / power runner | BLOCKED:14 current-process config issues; script policy Restricted | target/followup-environment.json; followup-power-static.json; followup-capacity-block.json. Power-helper syntax/declarations compiled; native API/runner not executed |
| New full capacity / leak acceptance | BLOCKED; previous acceptance failures retained | Unchanged standby/policy dependency. Guard/analyzer unit tests passed; they are not a new35-minute result |

Latest candidate: [InvestSmart-v2-nUhx39.tar.gz](target/releases/InvestSmart-v2-nUhx39.tar.gz), SHA256 `039801b0b4918f5477053b981b4aba8e20fabb8d66618c4170c263eca78c8f60`. Source fingerprint `003d405ce8a22e35acf9db4f48a3868abceb95baaf781231ba0407d862e9f73c` covers368 scoped inputs, excluding private files, HTTP scratch, documentation and generated dist. Archive checksums lock its actual payload, including docs. Candidate productionLaunchApproved=false; validated packaging is not production or capacity acceptance. Previous build attempt was interrupted and had earlier proxy inputs; only this fresh matching-input result certifies the final local package.

Safe work completed: strict prod-only preflight/runtime profile checks; HSTS/MIME/referrer headers; bounded request bodies with protocol413 verification; interruption fail-fast and partial-result retention; fail-closed capacity/memory analysis; redact-safe current/reachable-history exposure tooling. Dependencies needed no further version changes: current audits remain clean. No frontend regression requiring a code change was found. Review of authentication/session/ownership paths and full regression tests is not a penetration test or physical-device certification.

**Manual steps:** DEPLOYMENT_CHECKLIST.md, ?Manual acceptance steps for the remaining gates,? is the single operator action list, with exact commands and evidence requirements. Hosting/domains/SMTP/provider/calendar/factors/devices/off-host recovery and credential-reuse/history decisions require operator involvement. No passwords, tokens, signing/encryption keys or existing account data were modified or deleted. No history rewrite, policy bypass, public deployment, broker order or external mail occurred.



Verification2026-11-13; finalized2026-11-14 IST. **Production readiness: BLOCKED (NO-GO).** No production deployment or credential rotation was performed. Local package checks do not waive failed capacity, secret-review, host or physical-device gates.

## Previous verified local results (2026-11-13/14 snapshot)

| Check | Result | Evidence |
| --- | --- | --- |
| Backend | PASS:327 tests,0 failures/errors,15 opt-in skips | target/release-checks/backend.log; skips are reported, not treated as executed tests |
| Maven known advisories | PASS:143 resolved coordinates,0 OSV findings | target/release-checks/backend-advisories.json; unknown vulnerabilities and incomplete historical NVD coverage are not certified |
| Frontend | PASS:clean install, full npm audit0,51 tests, lint/build | target/release-checks/frontend-*.log; live node_modules preserved |
| Tooling | PASS:29 tests | target/release-tooling-complete.log |
| MySQL8.4/runtime | PASS:6/6, schema parity, restore/restart, HTTPS/SPA/cookies/CSRF, staging restrictions | target/release-checks/smoke-KbU1vf/result.json and target/release-packaged-final.log |
| Secret review | PASS within built-artifact scope; repository/host clearance BLOCKED | target/release-artifact-secret-review.json and target/release-secret-review.json |
| Environment | BLOCKED:13 required fields missing/invalid in current process | target/release-environment-review.json; private files were not loaded or values displayed |
| Candidate archive | PASS for local package only | target/releases/InvestSmart-v2-0Uoq5n.tar.gz; independent SHA256 and54 payload checksums,17 migration SQL files and portable artifact inspection passed |

Source fingerprint: `a8e0d0cbe16da5885eacf181d659763eb1d36b79b95db6694162f123791a028b` (362 scoped inputs; excludes private files, HTTP scratch requests, documentation and generated dist). Source comparison found no changes during the load attempt. Host: shared Windows11,12 logical CPUs,16GiB RAM; Node24.13.0/Java21.0.12/Docker Linux29.9.3. Low available host RAM and standby mean this is not a controlled VPS benchmark. Test JVM heap was bounded to768MiB and Hikari to12 connections; these are recorded test settings, not a certified deployment sizing recommendation.

## Capacity and stability

- **Short harness validation PASS:**100 sessions,60s at30/s plus15s at60/s;2700 requests, zero errors/429/timeouts/drops, read p95 124.35ms, mutation p95 496.25ms, financial reconciliation true. Warmup read/write p95 511/815ms is reported separately. Evidence: smoke-Lw5hMF workload/metrics and target/release-monitored-short-analysis.json. Its later redundant Docker-stop failure prevented that attempt completing all package checks; separate KbU1vf checks passed.
- **Full attempt FAIL / monitoring BLOCKED:**configured1800s at30/s plus300s at60/s and300s cooldown, but Windows standby interrupted continuous execution. Largest application gap1396.252s; container gap1398.191s. Last sample:28198 scheduled,3945 workload errors (3917 busy drops,26 timeouts,2 other request exceptions);24253 captured HTTP responses were200. No final reconciliation/result JSON was produced. Configured durations are not completion evidence.
- Interrupted samples show peak heap119.2MiB, peak normalized JVM CPU10.7%, peak13 DB connections and peak12 active pool connections. These sampled peaks cannot rule out events during missing intervals. Latency/throughput comparisons from this attempt cannot establish stable performance.
- **Memory leak assessment: INCONCLUSIVE.** Short-run post-GC usage was broadly stable; interrupted full-run/cooldown evidence cannot certify absence of leaks.
- The confirmed synthetic Surefire fork was stopped after the interruption was established; temporary project cleanup passed. No user application process or retained account database was stopped/reset. Failure evidence remains under target/release-checks/smoke-hkZvM9, target/release-capacity-full-analysis.json and target/release-power-events.json. Earlier Docker availability/stop failures are retained in the logs, not waived.

## Every release gate

PASS applies only to the stated scope. FAIL records an unsuccessful acceptance attempt; BLOCKED means required evidence is absent. Deferred P2 gates do not block focused V2.

| Gate | Priority | Status | Evidence / limitation |
| --- | --- | --- | --- |
| TR-01 | P0 | PASS | Backend intent/ownership/excess-exit regressions passed; rendered acceptance is UX-01. |
| TR-02 | P0 | PASS | Full backend regression and fresh MySQL6/6; separate restore/restart passed. No final financial acceptance from interrupted capacity run. |
| TR-03 | P0 | BLOCKED | Live provider authorization and reviewed settlement-calendar coverage lack acceptance. |
| SEC-01 | P0 | PASS | Fresh clean-install full npm audit0; Maven OSV143 coordinates/0 findings. Scoped override remains reviewed. |
| SEC-02 | P0 | BLOCKED | Automated security tests passed; physical authentication and protected owner flows lack acceptance. |
| SEC-03 | P0 | BLOCKED | Fresh local2commit/107blob review and3redaction tests passed;14JWTs with expired claims/2distinct exposed password values. Active account/external reuse, remote sharing and production secret custody unverified. Earlier501-file artifact scan certifies its historical snapshot only, not newer frontend/candidate. |
| SEC-04 | P0 | BLOCKED | Strict profiles, edge headers and16MB/413 checks passed locally; actual-host attribution and intentional abuse-limit acceptance unverified. Limits were not weakened. |
| INF-01 | P0 | BLOCKED | Synthetic template/Compose preflight passed, including secret separation; real staging/production host separation unverified. |
| INF-02 | P0 | BLOCKED | Trusted local HTTPS passed; public DNS/TLS/firewall/host persistence unverified. |
| INF-03 | P0 | BLOCKED | Isolated direct-peer and forged-header checks passed; actual staging host unverified. |
| CAP-01 | P1 | FAIL | Full attempt interrupted by Windows standby; required continuous1800s/300s workload not completed validly. Short2700-request run is not capacity certification. |
| CAP-02 | P1 | FAIL | Interrupted attempt:3945 workload errors/28198 scheduled, including3917 dropped work items and26timeouts. Invalid host conditions prevent attributing these to application capacity. |
| CAP-03 | P1 | BLOCKED | Resource samples retained, but1396s application/1398s container gaps prevent continuous acceptance. Host tuning remains unverified. |
| CAP-04 | P1 | BLOCKED | Real provider quota/latency and listing-aware cache behavior under host load unverified. |
| CAP-05 | P1 | PASS | Unique synthetic MySQL project, loopback HTTP/DB guard,100 synthetic sessions, mocked quotes/mail and unchanged production limits. Temporary projects cleaned. |
| SCALE-01 | P0 | PASS | Single worker configuration retained; synthetic packaged worker stopped during test runtime. No replicas enabled. |
| SCALE-02 | P2 | BLOCKED | Deferred P2 expansion gate; outside focused V2. |
| OPS-01 | P0 | BLOCKED | Application tests exist; actual host alerting, disk and operator response acceptance unavailable. |
| OPS-02 | P0 | PASS | Backend catch-up/retry regressions and isolated restart persistence passed; real host monitoring remains OPS-01. |
| DATA-01 | P0 | PASS | Fresh MySQL schema validation and managed settlement-migration parity passed. Existing production adoption still requires its own rehearsal. |
| DATA-02 | P0 | BLOCKED | Local synthetic dump/restore and restart passed; encrypted off-host retention and matched host rollback unverified. |
| REL-01 | P0 | BLOCKED | Current dzYbes required-container candidate/independent56payload checksums passed; capacity, host/device and explicit launch gates remain unmet. |
| REL-02 | P0 | BLOCKED | Actual production SMTP receipt and final HTTPS link destinations unverified. |
| UX-01 | P0 | BLOCKED | Physical devices, protected flows and broader latest rendered acceptance unverified. Protocol tests do not certify them. |
| GOV-01 | P0 | BLOCKED | Paper simulation labels retained; active-account display/segment permissions and production privacy/retention review unverified. |
| FUT-01 | P2 | BLOCKED | Deferred P2 mobile/V3 expansion gate; outside focused V2. |

## Secret-review boundaries and next release decision

The tracked HTTP scratch file contains14 distinct expired JWTs and20 literal password fields; production association/history exposure is unverified. Values were never displayed or changed. Checks of430 extracted JAR/dist entries found no forbidden local-config/scratch files and no known-value matches, including the reviewed scratch passwords. Pattern scans of386 current working files are not exhaustive secret or Git-history certification. Host credentials were not supplied or changed.

Open operator dependencies remain exclusively in DEPLOYMENT_CHECKLIST.md. Capacity needs a fresh uninterrupted benchmark with continuous telemetry and reconciliation. Real host/domain/TLS/mail, provider permissions, reviewed calendar coverage, off-host recovery, owner authentication, protected/device acceptance and explicit launch approval remain unmet. No public capacity claim, deployment, purchase, owner promotion or external message is authorized by this report.

## Previous candidate verification (own snapshot only)

Required-container builder completed successfully; latest runtime evidence: target/release-checks/smoke-0gDFZv/result.json. Independently verified archive: [InvestSmart-v2-0Uoq5n.tar.gz](target/releases/InvestSmart-v2-0Uoq5n.tar.gz). SHA256: `73b05f179976738618d3847e259b909d591bbaa3367258e38aab8eff54a654a9`. All54 payload checksums match;17 migration SQL files are included; portable artifact checks passed; no forbidden private-file entries. Candidate readiness declares productionLaunchApproved=false. Exact candidate/JAR entries and selected current logs were scanned across499 files with zero known-sensitive-value matches (target/release-final-secret-review.json); this does not clear the tracked scratch/history or real host credential gates. Archive/docs certify their packaged snapshot only; this live report records the subsequent independent result.

No automatic deployment occurred. **Final decision remains NO-GO for production and no certified100-session capacity claim.**

## Follow-up root-cause classification (2026-11-15)

Initial catalogue:7PASS,2FAIL,17BLOCKED. The failed capacity attempt is an interrupted environment measurement, not established application performance failure. An untested host/device/permission gate has missing acceptance evidence rather than a demonstrated code defect. No gate is closed merely by writing a procedure. Operator actions and commands are maintained exclusively in DEPLOYMENT_CHECKLIST.md.

| Failed/blocked gate | Root cause / missing evidence | Category | Checklist step |
| --- | --- | --- | --- |
| TR-03 | Active-account data permissions and reviewed working-day calendar not accepted | External/human | 7 |
| SEC-02 | Physical factors and protected owner/device flows not observed | Additional testing + human | 6 |
| SEC-03 | Scratch credentials persist in reachable Git history; password reuse/host secret protection unverified | External/human | 2,3 |
| SEC-04 | Actual staged peer attribution, abuse and request limits not accepted | Additional host testing | 4 |
| INF-01 | Separate real staging/production hosts/origins/volumes/secrets unavailable | External configuration | 3 |
| INF-02 | Public DNS/trusted TLS/firewall/persistence not accepted on chosen host | External + host testing | 3 |
| INF-03 | Direct-peer restriction only tested synthetically, not on actual host | Additional host testing | 4 |
| CAP-01 | Modern Standby interrupted the timed workload; scoped runner blocked by script policy | External environment + testing | 1 |
| CAP-02 | Scheduler backlog/timeouts across standby invalidated SLO measurement | Additional uninterrupted testing | 1 |
| CAP-03 | Long telemetry gaps prevent continuous CPU/heap/pool/stability acceptance | Additional uninterrupted testing | 1 |
| CAP-04 | Authorized upstream quota/latency under actual-host workload unverified | External + testing | 1,7 |
| SCALE-02 | Deferred expansion/distributed claims acceptance | Deferred V3, not V2 blocker | Scope boundary |
| OPS-01 | Real host alerts/storage and operator response unverified | External + human testing | 8 |
| DATA-02 | Encrypted off-host retention and matched host rollback not rehearsed | External + recovery testing | 8 |
| REL-01 | Final launch depends on capacity, host/device and explicit decision | External + testing | 1,8 |
| REL-02 | Production sender and actual HTTPS mail destinations unverified | Credentials + human receipt | 5 |
| UX-01 | Physical browser/device, keyboard/accessibility/offline acceptance incomplete | Human testing | 6 |
| GOV-01 | Active provider entitlements/privacy/retention review unverified | Permission + human review | 7 |
| FUT-01 | Mobile/store/V3 compatibility phase deferred | Deferred V3, not V2 blocker | Scope boundary |

### Actionable issues fixed in this follow-up

- Production preflight and runtime now require exactly the prod profile. Omitted/mixed profiles cannot enable development/test overrides in a production launch. Regression coverage includes missing and mixed profiles.
- Caddy now supplies one-year HSTS (without preload/subdomain expansion), MIME protection and no-referrer policy at the HTTPS edge. This avoids relying on backend scheme inference while forwarded scheme headers remain deliberately ignored.
- The edge now rejects bodies over16MB, preserving space for existing4MiB calendar input plus normal JSON encoding. DTO validation alone did not bound incoming JSON allocation. Packaged protocol checks require413 for oversized content and verify browser headers.
- The test-only scheduler aborts on a20s wall-clock gap or2s schedule lag instead of replaying an interrupted backlog; sanitized partial failure results are retained. The analyzer requires completion, actual phase spans/counts and continuous JVM/database/container evidence. Optimistic configuration flags, missing data and gaps cannot certify capacity or memory stability.
- Read-only exposure tooling scans current text and all locally reachable Git blobs up to2MiB, outputs only paths/categories/locations, and never values. Tests cover redaction, environment placeholders and provider-token indicators. It neither rewrites history nor rotates/deletes credentials.

### Security and interruption review boundaries

JWT signatures/expiry are checked server-side; authentication also requires the persisted, unrevoked, factor-completed session and current user status/DB role. Session cookies remain HttpOnly/Secure in prod and SameSite=Lax. CSRF remains enabled for mutations including login; CORS is explicit and credentialed. Money locks/journals and ownership boundaries were preserved; no production financial data or schema changed. Unit/protocol tests cannot replace physical MFA or host acceptance.

The latest read-only scan found74 current indicators (71 scratch-file occurrences plus3 known test-fixture properties),67 historical indicators confined to the scratch file across2reachable commits/107blobs, and14 distinct current JWT-shaped strings with past expiration claims. Signatures, password reuse and remote/unreachable/LFS history were not certified. No values were exposed, removed or changed.

Windows exposes Modern Standby; prior power events show real sleep transitions. The original thread-level sleep request did not prevent them. The proposed scoped SystemRequired/ExecutionRequired helper changes no persistent policy or lock behavior, and its syntax/native declarations compiled, but actual execution was rejected by local PowerShell policy. No bypass or repeated full run was attempted. Microsoft documents DC power limits and user-sleep precedence: [PowerSetRequest](https://learn.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-powersetrequest). The past events do not establish the prior power source or specific trigger. A policy-approved/signed runner on AC or a separate uninterrupted Linux host is still required.
