# Reviewed lint dependency override

Reviewed 2026-11-13 for the current project on Windows/Node 24.13.0. This is a scoped dependency-range exception with tested compatibility, not an upstream endorsement or advisory waiver.

## Finding and actual exposure

The four affected packages in npm audit (minimatch, eslint, @eslint/config-array and @eslint/js) represented one underlying advisory: [GHSA-g3hv-75wg-m249 / CVE-2026-118764](https://github.com/advisories/GHSA-g3hv-75wg-m249). It concerns excessive CPU/memory use from attacker-controlled glob patterns when optimizationLevel is at least 2. The inspected consumers use default level 1 and this repository configures fixed local globs. No website-input-to-linter route was identified. Production-only audit was already clean; these are build/development dependencies, not server/browser runtime dependencies. This exposure assessment was not used to suppress the finding.

## Exact exception

```json
"overrides": {
  "eslint": {
    "minimatch": "11.0.8"
  }
}
```

The single ancestor rule reaches the two installed consumers: ESLint 10.14.0 and its @eslint/config-array 0.23.5 dependency. Their declared ranges remain ^10.2.6 and ^10.2.4. The exact patched version is intentionally outside those ranges. No global minimatch override, npm force flag, peer bypass, package-source modification or ESLint downgrade was applied. Other dependency subtrees are not overridden.

Overlapping rules for both consumers produced npm override-context validation conflicts during evaluation. They were rejected. The adopted single rule passed a fresh standard install, clean npm ci, engine-strict npm ci and npm ls --all with no problems. No errors were ignored to obtain acceptance.

## Compatibility evidence

- Baseline/candidate ESLint JSON results matched for all 97 project files, including file discovery, rule IDs, severities, messages, locations and fixes.
- Five fixtures preserved unused-variable, React Hooks rules-of-hooks/exhaustive-deps and React Refresh diagnostics. Nested/brace/dot globs, native Windows versus slash paths, and dist/target exclusion matched.
- Clean full and production-only audits both reported zero findings after reinstall.
- Lint, 51 frontend tests and production build passed. All nine generated production files were byte-identical between baseline and candidate.
- Behavioral comparison: scripts/check-lint-compatibility.mjs. Evidence under ignored target/minimatch-compatibility.json, minimatch-bundle-comparison.json, minimatch-audit-final-*.json and minimatch-final-*.log. No running dev server or live dependency directory was replaced.

## Maintenance and removal

Keep the exact pin until upstream ESLint/config-array adopt a patched dependency range, or a reviewed patched backport satisfies their original range. On consumer updates, repeat the isolated discovery/diagnostic/rule checks, install/tree validation, full audit, tests and build. Do not broaden this to a global override or silently change the pin.

Remove the override only after an isolated install without it selects non-vulnerable versions naturally, has a clean dependency tree/audit and preserves lint coverage and project behavior. Revisit version assertions in the comparison harness as part of that reviewed upstream transition; do not weaken them merely to obtain a green run.

Deployment and full-duration load acceptance remain separate; this remediation did not start either.
