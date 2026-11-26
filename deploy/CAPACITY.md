# Capacity verification

The target is 100 authenticated sessions, 30 requests/s for 1800 seconds followed by 60/s for 300 seconds. This is an acceptance workload, not a statement of supported users. Keep one backend worker. See DEPLOYMENT_REQUIREMENTS.md CAP/SCALE requirements and the single dependency list in DEPLOYMENT_CHECKLIST.md.

## Local HTTP application measurement

After backend packaging and frontend build, run from the repository root:

```sh
node scripts/check-v2-containers.mjs --capacity
```

The existing runner creates only its uniquely named disposable MySQL project, validates the loopback synthetic database and runs the real HTTP test server on a random loopback port. The test class is under src/test and cannot be enabled in the packaged production JAR. No production route creates fixture sessions. Sessions and CSRF cookies are persisted/obtained before timing. A test-only trusted loopback proxy assigns 100 benchmark source addresses, preserving the real per-user/per-source rate limits rather than weakening them. This models separate users; it does not certify proxy security or authentication enrollment throughput.

Market prices/session availability are deterministic test doubles and mail delivery is mocked. Do not replace these with real Upstox/SMTP during load testing. The workload uses four orders per 80 requests to remain within the unchanged 30-order/user/15-minute limit. It exercises read-only settlement/activity/history, previews, short limit/cancel, short market entry and partial/final buy-to-close. A separate ten-worker warmup exercises eight steps per session before timing; its read/write p95 is reported separately. Established actors start at staggered read offsets to avoid artificial synchronized waves. Reports contain aggregate metrics only; never session tokens or account identifiers. The test checks final balances against recorded fees, no Delivery holdings, no remaining collateral/positions and no reservation leakage.

A short harness smoke uses V2_LOAD_STEADY_SECONDS=20 and V2_LOAD_BURST_SECONDS=15 in the process environment before the same command. The resulting fullDuration=false is never 100-user capacity acceptance. Normal releases do not run the opt-in 35-minute test implicitly. Full runs need roughly 40 minutes including startup/checks and must remain interruptible/checkpointed without marking an interrupted phase complete.

Evidence: target/http-capacity-result.json and its private smoke-workspace copy. Read p95 <=500ms, mutation p95 <=2s, unexpected errors <1%, no financial invariant violations. Rate-limited requests are counted and reported; intentional abuse-limit acceptance is a separate test. Failed/incomplete measurements cannot certify capacity. Existing unit/API tests cover duplicate requests, cutoff, missing prices, ownership and excess exits outside this steady deterministic workload.

## Limits and host acceptance

compose.capacity.yaml provides optional bounded CPU/memory, servlet queue/connections and Hikari settings. Combine it AFTER base Compose, preserving the required staging override; use the same project and private env file on every command. Trial baseline: 4 vCPU/8 GiB Linux with persistent SSD, separate from the developer PC. Run quiet Compose validation first. Do not apply these limits to an existing VPS without checking its available resources and connection requirements.

The local Java test measures application HTTP plus MySQL, not the packaged frontend/Caddy/TLS path, provider quotas, real-device experience or VPS disk/network behavior. A successful local full run is only preliminary evidence. Repeat the workload and resource monitoring on an isolated staging host before any capacity claim; document host/limits/database size, source snapshot, CPU/heap/DB waits/connections, disk and network conditions. Resource tuning must retain executable-price freshness, application rate limits and source-aware reservations.

Scale only after investigating sustained CPU >70%, heap/connections >80%, queues, disk pressure or SLO misses. Retest a larger single host first. Multiple backend replicas remain unavailable until distributed mail/job claiming, deduplication, process-local caches/locks and recovery have been reviewed and verified.

## Interrupted Windows measurements

The test-only continuity guard aborts on a wall-clock gap over20s or scheduler lag over2s; it never replays a suspended backlog. A failed attempt retains sanitized partial results. The analyzer requires completed execution, measured phase spans/counts, continuous JVM/database/container metrics and reconciliation. Configured durations alone cannot certify capacity; incomplete telemetry makes the memory assessment inconclusive.

The source-checkout Windows helper `scripts/run-v2-capacity.ps1` requests scoped SystemRequired and ExecutionRequired power availability and releases both in finally. It changes no persistent power, display or lock policy. Its PowerShell syntax and native declarations compiled; actual runtime remains unverified because local execution policy rejected the probe. Do not bypass an organization or machine policy to run it. Operator prerequisites and exact commands are maintained only in DEPLOYMENT_CHECKLIST.md.

Microsoft documents that Modern Standby power requests are time-limited on DC power and that explicit user sleep can override requests: [PowerSetRequest](https://learn.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-powersetrequest). The prior standby events establish interruption; they do not establish which power source or user/policy action caused it. A successful short probe is not proof of an uninterrupted35-minute run. The existing isolated Linux source-checkout runner is the alternative; host acceptance remains separate.
