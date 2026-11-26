# V2 release package

Build from the source checkout with Java 21+, Node 24, npm and tar available:

```sh
node scripts/build-v2-release.mjs
```

This runs backend verification, frontend installation/tests/lint/build and tooling tests before packaging. Docker is optional for preparing an archive, but mandatory for validation:

```sh
node scripts/build-v2-release.mjs --require-containers
```

The required-container mode runs isolated MySQL 8.4 initialization, API/session checks, backup/restore, restart persistence and trusted local HTTPS checks against the packaged runtime images. It creates only synthetic records in a uniquely named Docker project. Port 18443 must be free. Temporary container volumes are removed on completion; private logs stay under ignored `target/release-checks`. A failed check produces no new validated archive.

Archives and their SHA-256 digest files are under `target/releases`. Extract an archive into a new directory, verify its `.sha256` digest before extraction and then run `sha256sum -c SHA256SUMS` inside it. Review `readiness.json`: `releasePackageValidated` certifies the automated package checks only; `productionLaunchApproved` remains false until external acceptance is documented. CI artifacts are release candidates, not deployment approval.

## Fresh production database

Use a new production volume and a non-root application account. Preserve the developer database and accounts. Do not point this fresh-schema procedure at an existing database or reuse its volume. Runtime Dockerfiles consume the packaged JAR and frontend assets; they do not compile source.

Copy `deploy/production.env.example` to a private `deploy/production.env` file and restrict access. Configure distinct random signing/encryption/database secrets, the actual sender, provider token and final HTTPS origin. Keep `VITE_API_URL=/api`. Preserve the encryption key in protected backups; changing it makes existing encrypted factors unreadable. Passkeys must be enrolled against the final origin.

With those variables supplied in the process environment, run `node scripts/check-v2-release.mjs` from the extracted root. It does not load the private env file automatically. Then follow `DEPLOYMENT.md` for Compose configuration, startup, backup and explicit verified-owner bootstrap. Packaged manual JAR commands use `backend/app.jar` instead of the source checkout's `target/InvestSmart-0.0.1-SNAPSHOT.jar`. Do not run owner bootstrap until the intended account is verified; the user completes MFA prompts personally.

For an existing installation, stop and use the separate migration/backup runbook in `DEPLOYMENT.md`; fresh initialization is not an upgrade. Migration SQL is included. The standalone migration CLI can run from the packaged JAR with Java's `-Dloader.main=com.example.investsmart.deployment.MigrationLedger -cp backend/app.jar org.springframework.boot.loader.launch.PropertiesLauncher`; run it from the extracted root so it finds `src/main/resources/db/migrations`. Use `--catalog` first; applying/adopting requires independently reconciled schema and a backup.

## Launch acceptance

Record actual Android/iPhone pairing, same-phone login and recovery; remaining rendered desktop/mobile and owner Admin checks; owner verification/bootstrap/MFA; production verification/recovery/pairing inbox receipt and HTTPS links; hosting/TLS; and active Upstox exchange/derivatives entitlement and display/redistribution permission. Market reads alone do not establish permission. Follow `deploy/V2_ACCEPTANCE.md`; never substitute automated tests for visual/device acceptance. V3 chat is disabled by default.
