import { isIP } from 'node:net';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateEnvironment, validateComposeEnvironment, inspectArtifacts } from './check-v2-release.mjs';

export function validateStaging(env, {compose = false} = {}) {
    const errors = compose ? validateComposeEnvironment(env) : validateEnvironment(env);
    const ranges = (env.STAGING_ALLOWED_CIDRS || '').trim().split(/\s+/).filter(Boolean);
    if (!ranges.length || ranges.length > 32) errors.push('STAGING_ALLOWED_CIDRS needs 1-32 explicit tester addresses/ranges');
    for (const range of ranges) {
        const [address, prefix, extra] = range.split('/'), family = isIP(address);
        const max = family === 4 ? 32 : 128;
        // Keep staged access narrow; reject unspecified/global networks and Caddy syntax injection.
        if (!family || extra != null || address === '0.0.0.0' || address === '::' ||
            (prefix != null && (!/^\d+$/.test(prefix) || +prefix < (family === 4 ? 24 : 64) || +prefix > max)))
            errors.push('STAGING_ALLOWED_CIDRS contains an invalid or overly broad range');
    }
    return [...new Set(errors)];
}
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    const issues = [...validateStaging(process.env, {compose:process.argv.includes('--compose')}), ...inspectArtifacts(resolve(dirname(fileURLToPath(import.meta.url)), '..'))];
    for (const issue of issues) console.error(issue);
    if (issues.length) process.exitCode = 1;
    else console.log('PASS: staging configuration/artifact preflight only. Host, TLS, IP restriction, devices and release approval are not certified.');
}
