import { resolve } from 'node:path';

/** Uses only a temporary no-network container, never a host port or retained volume. */
export function checkStagingRestriction(command, image, context, project) {
    if (!/^investsmart-v2-smoke-[a-z0-9-]+$/.test(project)) throw new Error('Invalid isolated staging test project');
    const config = resolve(context, 'deploy/Caddyfile.staging');
    for (const [suffix, allowed, expected] of [['allow', '127.0.0.1/32', 200], ['deny', '192.0.2.1/32', 403]]) {
        const name = `${project}-restriction-${suffix}`;
        try {
            command('docker', ['run', '--detach', '--rm', '--network', 'none', '--name', name,
                '-e', 'PUBLIC_URL=:8080', '-e', `STAGING_ALLOWED_CIDRS=${allowed}`,
                '--mount', `type=bind,source=${config},target=/etc/caddy/Caddyfile,readonly`, image]);
            let status;
            for (let attempt = 0; attempt < 10; attempt++) {
                // A forwarded allowed address must not bypass a direct-peer denial.
                const result = command('docker', ['exec', name, 'wget', '-S', '-O', '/dev/null', '--header', 'X-Forwarded-For: 192.0.2.1', 'http://127.0.0.1:8080/'], { allowFailure: true });
                status = +(result.stderr || '').match(/HTTP\/1\.1 (\d{3})/)?.[1];
                if (status) break;
            }
            if (status !== expected) throw new Error('Staging direct-peer access restriction failed');
        } finally {
            command('docker', ['rm', '--force', name], { allowFailure: true });
        }
    }
}
