S# InvestSmart dependency security report — 2026-11-13

**Dependency remediation: complete for the reviewed clean-install snapshot.** Full and production-only npm audits report zero findings; the resolved Maven OSV scan reports zero matches. The four remaining moderate lint findings were removed with a user-approved, scoped minimatch 11.0.8 override after behavioral and reproducibility verification. This is a reviewed exception to upstream dependency ranges, not an advisory waiver. Deployment, physical-device and full-duration capacity acceptance remain separate; no deployment or 35-minute test was started.

## Frontend results

| Package | Before | Verified after | Result |
| --- | --- | --- | --- |
| axios | 1.19.0 | 1.21.1 | Reported findings resolved |
| vite | 8.2.1 | 8.3.8 | Reported findings resolved |
| brace-expansion | 5.0.9 | 5.0.13 | Reported findings resolved |
| follow-redirects | 1.16.0 | 1.16.3 | Reported findings resolved |
| source-map-js | 1.2.1 | 1.2.2 | Reported findings resolved |
| zod | 4.4.3 | 4.6.7 | Reported findings resolved |
| eslint | 10.8.1 | 10.14.0 | Moderate finding resolved by scoped transitive override |
| minimatch (ESLint subtree) | 10.2.6 | 11.0.8 | Root advisory resolved; reviewed compatibility exception |

Before initial remediation: 6 high, 4 moderate. Before the scoped lint fix: zero high and four moderate. Final full and production-only npm audits: **zero findings**. All four moderate package entries traced to one underlying advisory, GHSA-g3hv-75wg-m249, rather than four independent vulnerabilities. Evidence: target/minimatch-audit-baseline-full.json and target/minimatch-audit-final-{full,production}.json.

The affected chain was minimatch -> eslint/@eslint/config-array -> @eslint/js, entirely in development tooling. Production-only audit was clean even before this change. The vulnerable branch requires attacker-controlled patterns and optimizationLevel >=2; the inspected lint consumers use default level1 and the project uses fixed local globs. No website-input path was found. This limits assessed exposure but was not used as a waiver.

The reviewed fix is a single ESLint-subtree minimatch11.0.8 pin. ESLint10.14.0 and config-array0.23.5 remain unchanged; no global override or unsupported ESLint9 downgrade was used. This intentionally exceeds their declared minimatch10 ranges and is adopted only for the tested project/Node24.13.0 setup. The clean dependency tree and engine-strict reinstall passed. Baseline/candidate diagnostics and file discovery matched for97 project files and5 rule/glob/Windows fixtures; lint,51 tests and build passed, with9 production files byte-identical. [Review and removal policy](deploy/LINT_SECURITY_OVERRIDE.md).

`npm install`, compatible `npm update`, another install and clean `npm ci` were run in an isolated allowlisted frontend workspace. Lint, 51 Node tests and the /api production build passed. Only validated manifest/lockfile changes and generated distribution assets were copied back. Running developer node_modules and servers were preserved; an already-running process is not evidence it has loaded patched libraries. Local installation adoption is tracked exclusively in DEPLOYMENT_CHECKLIST.md.

## Backend results and coverage

Updated Spring Boot parent 4.1.0 → **4.1.2**. Removed explicit vulnerable Jackson 2.21.4 version pins so both Jackson families remain aligned with the managed BOM. Java remains 21; no major framework migration or independent managed-version overrides were introduced.

| Resolved library | Before | After |
| --- | --- | --- |
| Spring Framework | 7.0.8 | 7.0.10 |
| Spring Security | 7.1.0 | 7.1.2 |
| Tomcat | 11.0.22 | 11.0.27 |
| Jackson 2 core/databind | 2.21.4 | 2.21.7 |
| Jackson 3 core/databind | 3.1.4 | 3.1.7 |
| Apache HttpClient 5 | 5.6.1 | 5.6.5 |
| Apache HttpCore 5 | 5.4.2 | 5.4.4 |
| Log4j API | 2.25.4 | 2.25.5 |

The keyless [OSV query-batch API](https://google.github.io/osv.dev/post-v1-querybatch/) checked **143 resolved Maven coordinates**, including transitives and test dependencies. Baseline: 33 package/advisory pairs across 24 distinct advisory IDs. Patched result: **zero matches**, with complete response cardinality checked. Evidence: target/security-backend-findings.json, target/security-backend-tree-after.json and target/security-backend-audit-after.json. Package matching does not imply every baseline advisory was reachable in this application and cannot establish absence of unknown vulnerabilities.

OWASP Dependency-Check 13.0.0 was also attempted, but its NVD data initialization failed with an empty-key/data error. That attempt is **incomplete**, not clean; no NVD API key or credentials were exposed. The successful backend result above is specifically OSV coverage. The reusable Maven scan rejects incomplete inventory, service errors, pagination and malformed responses. Release verification now generates a fresh dependency tree and invokes it; failure prevents packaging. The full npm gate remains fail-closed at all severities.

## Verification evidence

| Check | Result / evidence |
| --- | --- |
| Frontend install / reproducible install | Passed; target/security-frontend-{install-after,ci}.log |
| Frontend lint / tests / production build | Passed after scoped fix; 51 tests; target/minimatch-final-{lint,tests,build}.log |
| Backend `mvnw.cmd -B verify` after patch | Passed; 327 total, zero failures/errors, 15 opt-in skips; target/security-backend-tests-after.log |
| Security/deployment tooling tests | 25 passed; target/minimatch-tooling-final.log |
| Docker Caddy image download | Passed; target/security-docker-pull.log |
| Docker Java 21 image download | Passed; target/security-docker-java-pull.log |
| Docker MySQL 8.4 image download | Passed; target/security-docker-mysql-pull.log |
| Patched isolated runtime / MySQL recovery | Passed: 6/6 tests, migration parity, dump/restore, restart, HTTPS/CSRF and staging restrictions; target/security-runtime-check.log and target/release-checks/smoke-vs46Uv/result.json |
| Full 35-minute capacity test | Not started: explicitly outside this focused remediation; requires its separate acceptance run |

Image downloads establish connectivity and image availability, not a container OS vulnerability scan. Earlier short 100-session results cover their earlier snapshot only; no new full-duration or VPS-capacity claim is made. Physical devices/rendered authentication, provider permissions, production SMTP/hosting and final launch approval remain separate gates in DEPLOYMENT_CHECKLIST.md.

No new approved archive exists. The dependency finding gate is now clear for the reviewed source/lockfile and clean installs. Run the remaining required-container release/capacity checks and independently verify the new archive before any launch or capacity claim. Live dev node_modules were preserved; the clean-install evidence does not claim an already-running process has reloaded its libraries.

## Release follow-up2026-11-15

Fresh isolated full and production-only npm audits0; resolved Maven OSV143/0, complete. No further dependency version changes were needed. Backend330/0fail/0error/15opt-in skips, frontend51/lint/build, tooling36 and required-container nUhx39 passed. Independent54payload/17migration/portable verification passed. These supersede earlier no-current-archive statements for this snapshot only. Capacity was not retried against unresolved standby/script-policy blockers. The full gate matrix, secret-history limits and NO-GO decision remain in RELEASE_READINESS_REPORT.md; operator actions only in DEPLOYMENT_CHECKLIST.md.

## Template/runner continuation2026-11-15

Current full and production-only npm audits0; Maven OSV143/0complete. No dependency version changes were needed. Fresh backend332total/317executed/15opt-in skips/0fail/error, frontend51/lint/build, tooling40 and packaged MySQL/recovery/HTTPS checks passed. Candidate dzYbes independently56payload/17migration/portable checks passed; exact501file known-value scan0matches. SMTP identity was explicitly pinned to the already-enabled current Angus default; this does not establish actual mail receipt. Manual Linux CI capacity workflow is prepared/static-parsed, not executed or published. Full load remains blocked, and production remains NO-GO; all operator steps are only in DEPLOYMENT_CHECKLIST.md.
