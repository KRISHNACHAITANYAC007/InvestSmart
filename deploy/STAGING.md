# Focused V2 private staging

Use the validated portable package on a dedicated Linux VPS with Docker Engine/Compose. Start with one backend. Staging and production use separate hosts, domains, credentials, project names and database volumes; the fixed Docker subnet makes same-host coexistence unsupported without a reviewed network change. Never upload the development database, local properties, private HTTP files or repository-wide logs. Open dependencies are tracked only in DEPLOYMENT_CHECKLIST.md.

## Prepare and restrict access

1. Validate the new release archive and every payload checksum, then extract into a new release directory. Preserve the previous directory/images and matching database backup for rollback.
2. Privately prepare a mode-600 environment file outside the archive, using production.env.example as the field reference. Use the staging HTTPS origin consistently for PUBLIC_URL/CORS_ORIGINS and mail links, VITE_API_URL=/api and separate random database/signing/encryption secrets. Use only authorized provider configuration and synthetic test accounts. Never share credentials in chat or commit them.
3. Add STAGING_ALLOWED_CIDRS as space-separated exact tester addresses or narrow CIDRs, including testers' IPv6 addresses when used. Do not allow 0.0.0.0/0 or ::/0. Users behind changing mobile public IPs must arrange a stable egress/VPN or update the allowlist privately. Caddy checks the direct peer, not spoofable forwarded headers. Do not add a CDN/upstream proxy to this topology. An absent allowlist prevents Compose startup; Caddy's fallback permits loopback only.
4. Point the staging domain directly to the VPS. Configure SSH key access and restrict SSH to operator addresses; allow 80/443 for certificate issuance. Publish neither MySQL nor backend/Vite ports. Do not disable the firewall or use a localhost tunnel as production.

From the extracted release root, with the private environment available in the process, run `node scripts/check-v2-staging.mjs --compose`. This also checks the Compose root password and separation from application/signing/encryption secrets. Standalone deployments use the command without `--compose`. This checks configuration/artifacts without deploying or revealing values. Then validate merged Compose without printing secrets:

```sh
docker compose --project-name investsmart-staging --env-file /protected/path/staging.env -f deploy/compose.yaml -f deploy/compose.staging.yaml config --quiet
```

Start only after the archive and restriction checks pass, on a new empty staging volume:

```sh
docker compose --project-name investsmart-staging --env-file /protected/path/staging.env -f deploy/compose.yaml -f deploy/compose.staging.yaml up --build -d --wait
```

Use that same project/environment/both Compose files for every subsequent operation. Omission of the staging override removes its access restriction. Never use `down --volumes` with retained data. The source bundle builds source images; the portable package selects prebuilt-artifact Dockerfiles.

## Accept and operate

Confirm both an allowlisted device can load the app/API and a non-allowlisted device receives 403, even when sending X-Forwarded-For with an allowed address. Check public TLS, /api/health, deep links, cookies/CSRF, actual mail links, quote freshness and settlement calendar coverage. Do not activate guessed production calendar dates. Follow V2_ACCEPTANCE.md for trading, physical-device authentication and owner acceptance; user-operated MFA/passkeys cannot be automated. Owner promotion still requires the separate explicit bootstrap approval/workflow.

Back up the database with a protected database login path and retain the encryption key separately in protected backup storage. Keep backups off the VPS, rehearse restoration in isolation and record retention/restore evidence. For an upgrade, stop writes and apply reviewed forward migrations after backup/restore rehearsal; never initialize fresh SQL over retained data. For rollback, stop writes and restore matching application/database versions together; invalidate sessions. Never overwrite retained encryption keys as a shortcut.

Monitor /api/health, disk/volume capacity, mail failures, quote freshness, t1-settlement and paper/IPO job timestamps/failures. Missing reviewed calendar coverage leaves obligations pending. Existing databases and resolved historical settlements must remain unchanged by calendar activation.

## Device review matrix

| Environment | Required checks | Evidence |
| --- | --- | --- |
| Windows Chrome, Edge, Firefox | Login/recovery, keyboard navigation, desktop chart/ticket, funds/history, order lifecycle | Pending on staged candidate |
| macOS Safari | Same browser flows and passkey session behavior | Pending on available Mac |
| Android Chrome, phone/tablet | 320/400/768 layouts, rotation, keyboard-open forms, chart gestures, pairing/recovery | Physical acceptance pending |
| iPhone/iPad Safari | Safe areas, rotation, keyboard, chart gestures, authentication/recovery | Physical acceptance pending |
| All above | Slow/offline/reconnect, stale prices, duplicate submission protection, cancellation and restored session | Record actual results per candidate |

Emulated rendering supplements real-device results and never marks physical acceptance complete. Restriction and staging tests precede explicit public release approval. Android packaging is a separate post-web phase; preserve stable backend APIs for existing installed clients when developing V3.

References: [Caddy remote-IP matchers](https://caddyserver.com/docs/caddyfile/matchers#remote_ip), [Compose merge rules](https://docs.docker.com/compose/how-tos/multiple-compose-files/merge/).
