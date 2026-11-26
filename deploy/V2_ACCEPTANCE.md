# V2 responsive and release acceptance

## Implemented responsive behavior (2026-11-10)

The website keeps its existing five-page navigation and blue split login design. Shared rules cover safe-area insets, 44px touch controls, 16px mobile form text, reduced motion, flexible headers and contained table scrolling. Sidebar position follows the measured header height; its contents scroll independently. Dialogs use the visual viewport when the software keyboard reduces usable height. Chart controls wrap, essential mobile labels are larger, and the heatmap reserves a usable grid on short screens. Touch users preview a heatmap tile before tapping it again to navigate.

Phone code/recovery screens clear sensitive content when backgrounded or restored from the back/forward cache. Enrollment recovery-code acknowledgement can be retried after interrupted delivery. Production/non-loopback API fallback is `/api`, so a phone does not send application requests to its own localhost.

## Acceptance matrix

These are target viewports, not claims of device/browser acceptance. Geometry tests cover all listed widths; other layout behavior requires rendered inspection.

| Class | Widths in CSS pixels | Rendered/device acceptance |
| --- | --- | --- |
| Phones | 320, 360, 390, 430 | Pending Android Chrome and iPhone Safari |
| Tablets/split screen | 600, 768, 1024 | Pending Android tablet and iPad Safari |
| Desktop | 1280, 1440, 1920 | Full latest-UI review pending |

Exercise portrait/landscape, a short 320px-high viewport, keyboard-open forms, 200% zoom, touch, keyboard focus and reduced motion. Verify no page-wide horizontal overflow; wide financial tables may scroll within their labeled regions. Verify safe-area spacing and that the focused input, close/cancel and confirmation actions remain reachable. Check chart gestures without preventing surrounding-page scrolling.

Cover Login/register/verify/reset, Phone codes/enrollment/recovery, Dashboard, all Markets tabs, charts in both themes, Heatmaps, Portfolio tabs, Orders tabs and conditional cancellation, Account and all five owner Admin sections. Include long instrument names, large balances, empty/loading/error states and open trading/listing/IPO/Admin dialogs.

## Verified automated checks

- Full Maven `verify` (2026-11-11 expanded ticket): 207 tests, 0 failures/errors, 11 skipped (8 opt-in live-provider and 3 MySQL-only checks). The separate real-MySQL phase then passed all 3 MySQL tests, including paper OCO and financed-position repayment. New coverage includes 20 paper lifecycle/API/migration tests and 2 policy tests.
- Frontend API/auth tests: 36 passed. Includes quantity/amount/lot conversion and existing geometry, phone lifecycle and routing coverage. ESLint and production build pass.
- Release-tool tests: 17 passed, including portable JAR inspection, private-file/symlink rejection, checksum manifests, synthetic encryption integrity and bounded Docker readiness success/failure cases. Frontend lint and `/api` build pass. Earlier archives passed SHA-256/payload verification and portable inspection; see the handoff for the latest validated candidate.
- Isolated packaged Docker runtime acceptance passed on 2026-11-10: fresh MySQL 8.4 schema validation, both opt-in authenticated funds/session tests (2 passed, no skips), synthetic accounting/encrypted-factor backup restoration, database/application restart, private-CA HTTPS, SPA deep links, Secure CSRF cookie and rejection without CSRF. Project containers and volumes were cleaned up. Evidence: ignored `target/release-checks/smoke-l7uyiv/result.json`.
- Real MySQL 9.7 synthetic upgrade/backup restore passed in the prior task. It does not certify production MySQL 8.4 or the actual existing installation.

Automated geometry/protocol tests do not establish visual or real-device acceptance. Real phone biometric/PIN and owner MFA prompts must be completed by the user. Browser URL/security barriers must not be bypassed.

## Remaining release gates

### F/G acceptance procedure

Use disposable test accounts and virtual balances. Record the build/archive, date, browser/device, result and evidence for each row. Passing protocol tests establishes automated coverage; the rendered/device columns require separate observations.

| Area | Required acceptance | Current evidence |
| --- | --- | --- |
| F phone pairing | Verified regular user pairs on a real phone, saves recovery codes, enters the current six-digit code on the PC, and signs in on the same phone | Automated signed WebAuthn fixtures pass; Android/iPhone pending |
| F phone lifecycle | Wrong/expired/reused codes fail; recovery grants replacement only; old phone/display/sessions stop working; replacement phone signs in | Automated replacement-through-new-sign-in coverage added; physical-device pending |
| F owner Admin | Verified owner completes authenticator setup/login; exactly five sections render; stale confirmation blocks sensitive actions until password+MFA succeeds | Existing security integration coverage; rendered owner acceptance pending |
| F Admin actions | On disposable accounts exercise search/review, suspension/reactivation, session revocation, forced reset/resend, balance ledger and exact-email deletion; verify audits and retained history | Existing integration coverage; protected browser dialogs pending |
| F production mail | Verification, recovery and phone pairing reach an inbox and use the final HTTPS origin; expired links fail | Local receipt was user-confirmed; production sender/origin pending |
| G paper orders | NIFTY CE/PE expiry/strike and lot selection; market/limit/sell-stop; ownership/depth/quote failures; cancellation and consistent positions/history | Automated provider/accounting coverage; live entitlement and rendered controls pending |
| G accounting/expiry | Competing reservations cannot overspend; stale fetch timestamps cannot fill; cancellation/expiry refunds and exact-date settlement happen once | Automated concurrency/stale-depth/expiry coverage; MySQL 8.4 schema, funds/session and retention/restore checks passed |

The option execution freshness check accepts a fetch timestamp up to 60 seconds old, with at most five seconds of future clock skew. This measures application fetch age; it does not certify the age of an upstream exchange tick or provider display rights. Missing, older or excessive-future timestamps cannot create fills; pending reservations remain intact until a valid fill, cancellation or expiry.

### Local Docker setup prerequisite

The user completed WSL/Docker installation after the earlier installer-policy rejection. Runtime checks now confirm WSL 3.0.5, Docker Linux engine 29.9.3 and Compose 5.7.0. No installer or Windows security prompt was automated.

For future machines follow the [official Windows instructions](https://docs.docker.com/desktop/setup/install/windows-install/), then verify `wsl --version`, `docker version` and `docker compose version` before running `node scripts/build-v2-release.mjs --require-containers`. Readiness uses a ten-second engine-version query requiring Linux: `docker info` can hang on unrelated Desktop diagnostics even while version/ps respond. Timeouts, absent engines and Windows engines fail closed. The smoke database joins a host-reachable test network and publishes only to loopback; production database networking remains private. Port discovery validates engine bindings. HTTPS uses Node with the exported temporary Caddy CA and normal certificate/hostname verification, without altering Windows trust/security settings. Do not use the existing application database for synthetic acceptance. Windows and phone authentication prompts remain user-operated.

1. Complete latest-UI rendered desktop/mobile review and real Android/iPhone pairing, same-phone login, recovery and expiry/revocation checks.
2. Finish intended-owner verification/bootstrap, authenticator enrollment/recovery-code storage and protected Admin acceptance.
3. Container/MySQL 8.4 acceptance is passed locally; rerun for later releases. A fresh production database is the selected first-launch path. Existing-installation migration/restore acceptance applies only if upgrading retained data and is not established by the synthetic fresh-database test.
4. Supply private production configuration, hosting/domain/TLS, and verify actual verification/recovery/pairing inbox receipt and HTTPS destinations.
5. Resolve Upstox profile HTTP 401 and obtain account-specific exchange/derivatives entitlement and data display/redistribution authorization. Successful market reads alone do not complete this gate.

Read-only provider follow-up (2026-11-10): the configured token returned HTTP 200/success for NIFTY option-contract discovery (2,066 rows); profile still returned HTTP 401. No order was submitted and no raw profile/token was recorded. This verifies that contract discovery is accessible with the current configuration; it does not establish display/redistribution permission or full account entitlement. Sanitized evidence: ignored `target/device-acceptance/provider-readonly.json`.

Do not deploy or mark V2 release-ready until these gates are evidenced. V3 remains deferred.

Rendered inspection history (2026-11-10): limited desktop/400px Dashboard/Markets, desktop Account and desktop/400px Login inspection was recorded earlier. A subsequent attempt was rejected because the current browser URL could not be verified; Computer Use stopped without bypass. Those limited earlier observations do not establish acceptance of the full latest UI or physical devices. The isolated Vite preview was stopped.
