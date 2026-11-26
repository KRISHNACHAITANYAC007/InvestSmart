# Deployment requirements catalogue

## 2026-11-23 — owner transition decision

**PAUSED BY OWNER DECISION — prioritizing V3 development before public launch.** Production remains NO-GO. Latest documented status: **7 PASS / 2 FAIL / 17 BLOCKED** (focused V2: 7 PASS / 2 FAIL / 15 BLOCKED; two P2 expansion gates remain blocked). These historical results are preserved, not freshly recertified.

V3 Development -> V3 Testing -> Production Readiness -> Website Launch -> Android App -> iOS App. See [V3_DEVELOPMENT_PLAN.md](V3_DEVELOPMENT_PLAN.md) for the approved roadmap. Older deployment-first instructions are superseded by this dated decision; historical conclusions remain intact.

All requirements and acceptance thresholds below remain in force. They describe the historical V2 baseline; future V3 readiness requires exact-source reassessment. Single-worker, verified-data, managed-migration, security, recovery and physical-device requirements are not waived. Canonical pending actions remain in DEPLOYMENT_CHECKLIST.md.

Target: focused educational V2, 100 simultaneous authenticated users. This catalogue defines acceptance criteria; DEPLOYMENT_CHECKLIST.md is the single action/dependency list. Existing local financial data and operator servers must be preserved. No public launch, purchase, owner promotion or store submission is implied.

Status: Verified means only the cited snapshot/check; Implemented needs acceptance; Open has no completion evidence. P0 blocks public release, P1 blocks the declared capacity claim, P2 precedes expansion.

| ID | Priority | Requirement and acceptance evidence | Current status |
| --- | --- | --- | --- |
| TR-01 | P0 | Delivery sells cannot exceed unreserved holdings; MIS shorts need no Delivery holding. Explicit entry/exit intent with owned position reference and no automatic reversal | UX implemented; 43 targeted backend/51 frontend tests passed; rendered acceptance open |
| TR-02 | P0 | Shared money locks, idempotent fills/journals, partial exits, restricted profits, fees/debt and cancellation reservation recovery | Prior unit/MySQL evidence in handoff; rerun affected checks |
| TR-03 | P0 | Fresh authorized listing/contract prices and reviewed working-day settlement calendars; missing data fails unavailable | Implemented; external acceptance in checklist |
| SEC-01 | P0 | Clean online dependency audit and verified build inputs; audit failures prevent packaging | Full+production npm audits0 after approved scoped minimatch11.0.8 compatibility review; Maven OSV143/0. Review/removal policy in deploy/LINT_SECURITY_OVERRIDE.md; release gates still fail closed |
| SEC-02 | P0 | Verified users, CSRF, secure cookies, MFA/passkeys/recovery, owned resources and reasoned owner actions | Strict prod profile guard and auth regressions passed; physical/protected acceptance remains open. |
| SEC-03 | P0 | Separate protected signing/encryption/DB/mail secrets and recoverable encryption-key backup; no secrets in logs/artifacts | Compose secret-separation preflight passed synthetically; exact501file known-value scan clean. Scratch reuse/history and host secret isolation still blocked. |
| SEC-04 | P0 | Abuse limits preserve real client attribution, bounded request bodies and safe errors; test intentional 429 independently | Local HTTPS edge headers and16MB/413 passed; actual-host abuse/client attribution acceptance remains open. |
| INF-01 | P0 | Separate development/staging/production origins, databases and secrets; fresh staging synthetic accounts | Hosting/staging template and quiet merged Compose validation passed; actual separate hosts/origins/volumes/secrets unverified. |
| INF-02 | P0 | Domain/DNS, trusted HTTPS, persistent certificate data, private MySQL/backend ports, SSH/firewall restrictions | Local HTTPS verified; real host acceptance open |
| INF-03 | P0 | Staging direct-peer allowlist rejects unauthorized/forged forwarded requests | Isolated Caddy checks passed; staged acceptance open |
| CAP-01 | P1 | 100 sessions,30 API requests/s for30minutes plus60/s for5minutes; representative reads/previews/orders/cancels/exits | Full hkZvM9 interrupted by Windows standby;1396s metrics gap, drops/timeouts. FAIL for continuous acceptance; short2700request run passed. |
| CAP-02 | P1 | Read p95 <=500ms, mutation p95 <=2s, unexpected errors <1%, no accounting discrepancies | Short2700request run read124ms/write496ms/0errors/reconciled; full attempt invalid with3945 workload errors. No capacity certification. |
| CAP-03 | P1 | Bounded servlet threads/queue, Hikari connections and container resources; record CPU/heap/DB waits/disk saturation | Fail-closed gap/count/span/completion analysis and interruption guard implemented/tested; continuous actual-host resource evidence remains blocked. |
| CAP-04 | P1 | Listing-aware eligible-read coalescing, pagination and upstream quota protection; never execute using expired cache | Existing partial caching; measured quota review pending |
| CAP-05 | P1 | Isolated synthetic load data, deterministic prices, no upstream market/mail traffic; don't weaken production limits | Opt-in test-only loopback server and synthetic MySQL guard, mocked quotes/mail, real CSRF/sessions, unchanged limits |
| SCALE-01 | P0 | One backend worker until distributed mail/order/settlement claims and multi-worker recovery are verified | Single-worker deployment documented |
| SCALE-02 | P2 | Expand after sustained SLO/resource pressure; first retest larger host, then distributed job claims and replicas | Deferred; no replica capacity claim |
| OPS-01 | P0 | Persistent job health, quote freshness, mail failures, disk and backup alerts with tested operator response | App health implemented; host alerting open |
| OPS-02 | P0 | Bounded catch-up and visible retryable closures; restart never fabricates fills or converts MIS to Delivery | Existing tests; affected regression required |
| DATA-01 | P0 | Managed forward migrations match fresh schema; no fresh SQL over retained data | Isolated settlement parity verified 2026-11-13 |
| DATA-02 | P0 | Encrypted off-host backups, retention policy, isolated restore reconciliation and matched app/DB rollback | Local recovery verified; host/off-host drill open |
| REL-01 | P0 | Exact-source tests, required-container candidate, independent archive/payload checksums and explicit launch decision | Current dzYbes required-container and independent56payload/17migration checks passed. Continuous capacity, host/device and explicit launch gates unmet. |
| REL-02 | P0 | Actual signup/reset/pairing mail receipt and final HTTPS destinations | SMTP identity policy explicit and unit-verified; actual provider/sender receipt and HTTPS destinations still blocked. |
| UX-01 | P0 | Desktop Chrome/Edge/Firefox/Safari and real Android/iPhone/tablet flows, keyboard, safe areas, rotation, accessibility, stale/offline/reconnect | Earlier scoped chart visuals only; new screens/device acceptance open |
| GOV-01 | P0 | Clearly disclosed paper simulation and data-provider display/segment permissions; privacy/retention/account deletion reviewed | App labels exist; permissions/production review open |
| FUT-01 | P2 | Stable API compatibility/versioned migrations for future Android clients and V3; signed app/store acceptance separate | Future phase |

## Capacity and scaling decisions

Begin with one backend and private MySQL behind the existing same-origin proxy. A 4-vCPU/8-GiB Linux machine is a trial benchmark configuration, not a purchasing instruction or proven capacity. Record exact host, disk, JVM/container limits, database size, fixture, commit/source snapshot and network conditions with every result. Concurrent registered users alone do not establish throughput.

Measure application HTTP capacity separately from real provider latency/quotas and physical-device usability. Synthetic prices must exist only inside the opt-in test runtime. Keep production rate limits and executable-price freshness rules intact. Full capacity acceptance requires both steady and burst phases; a short smoke is not certification.

Investigate sustained CPU >70%, heap/connection usage >80%, growing DB wait/queue depth, disk pressure or SLO failure. Tune and retest before expanding. No horizontal scaling until all scheduled writers and mail delivery have safe distributed claims, deduplication and failure-recovery tests. Existing process-local caches/locks need a replica-readiness review.

## Checklist integration

DEPLOYMENT_CHECKLIST.md references these IDs for ordered implementation, release decisions and every open user dependency. Keep this file as acceptance reference; do not maintain a second list of requests or silently mark host/device checks complete from local tests.
