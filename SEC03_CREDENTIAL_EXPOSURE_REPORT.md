# SEC-03 credential-exposure investigation

Reviewed 2026-11-16. **Local investigation completed within the scope below; SEC-03 remains BLOCKED. Release totals remain 7 PASS / 2 FAIL / 17 BLOCKED.** This is not evidence that an account was compromised, nor clearance that all credentials are safe.

## Fresh evidence

| Check | Result |
| --- | --- |
| Current tracked/nonignored text | 74 pattern indicators: 51 JWT occurrences, 21 password fields, 2 secret properties |
| HTTP scratch file | 51 JWT occurrences representing 14 distinct tokens; 20 password fields representing 2 distinct password values across current/reachable history |
| Locally reachable history | 2 commits, 107 reviewed blobs; 67 indicators (47 JWT occurrences and 20 password fields), confined to `src/main/java/com/example/investsmart/test.http` |
| Scratch blob versions with findings | 1 historical blob; current working copy also reviewed |
| Expiry claims | All 14 distinct current/history tokens have numeric expiry claims in the past at review time; no unexpired/missing-expiry/malformed payloads found |
| Token authentication | Not attempted: payload decoding is not signature, issuer, session-state or service validation. All have email-shaped subjects; values were not returned. No provider-specific claim hint was detected; absence of a hint does not prove issuer identity |
| Request context | Credential occurrences associate with local-application requests, not a demonstrated external Upstox/SMTP/GitHub credential. This is contextual attribution, not authenticated issuer identification |
| Local literal password reuse | One existing named private settings file compared in memory; no exact exposed-password match. No database, service, process environment, vault or external account tested |
| Other current indicators | One documentation SMTP-password placeholder indicator, one scanner-test password fixture, one test-profile signing-secret indicator. These are not evidence of exposed live SMTP/signing credentials; they must not be adopted for production |
| Remotes | One configured GitHub remote and one local remote-tracking ref. Visibility, current remote contents, push/publication history and access by others unverified; no fetch or external request performed |
| Scanner redaction tests | `node --test scripts/check-secret-exposure.test.mjs`: 3/3 PASS |

Occurrence counts are not distinct credential counts. JWT expiry does not expire passwords. Expired bearer claims do not clear earlier exposure or password reuse. The two password values cannot be declared inactive from text alone.

## Scope and limits

Ran the existing `node scripts/check-secret-exposure.mjs target/sec03-current-history-review.json`. Findings contain paths, line numbers, kinds and blob identifiers only. A separate in-memory review decoded expiry claims and compared literal values without writing those values, payloads, subjects, hashes of passwords or account identities. Aggregate evidence: `target/sec03-expiry-reuse-summary.json`.

Scanner scope is current tracked/nonignored text and locally reachable Git blobs up to 2 MiB. The binary image was skipped. No unknown-secret guarantee is possible with pattern matching. The targeted private comparison checked only existing files among application-local properties and named local environment files; it is not a private-file or environment-wide audit. Reflog-only/unreachable objects, Git LFS payloads, remote-only branches/tags, forks, clones, caches, messages, CI artifacts and published archives were not certified. A remote-tracking reference does not prove the current scratch file was pushed or that the remote is public.

No credential was submitted to any endpoint, no login attempted, no live key/session rotated or revoked, no database accessed, no scratch/private file changed or removed, no Git history rewritten and no push/deployment/benchmark performed. Only redacted evidence and investigation documentation were written.

## Closure decision

Local automatic tasks are complete for this bounded investigation: inventory, current/reachable history scanning, expiry-claim classification, contextual service classification, limited exact-reuse comparison and scanner verification. Evidence cannot establish password reuse outside the checked settings or active account state.

SEC-03 cannot close until the credential owner privately confirms affected-account/password remediation or verified inactivity, assesses remote/sharing exposure, and supplies evidence for production secret separation and recoverable key custody. Existing historical artifact scans apply only to their own old candidate; the changed frontend has not received a fresh full candidate clearance. No gate is promoted by this review.

Precise personal actions and safe completion format are maintained **only in DEPLOYMENT_CHECKLIST.md, manual step 2 (SEC-03)**. Credential changes can sign out accounts or interrupt integrations and require separate approval; this investigation does not authorize them. Do not rotate the application encryption key as an incident shortcut: retained factors and encrypted mail depend on it. There is no evidence here requiring blanket rotation of every provider key or signing/encryption key.

Do not proceed to benchmark or hosting setup until this review's unresolved private confirmations are addressed, in accordance with the user's sequencing instruction.
