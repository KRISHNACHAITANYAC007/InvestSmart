import { spawnSync } from 'node:child_process';

/** Query the engine API without Docker info's unrelated diagnostics/plugins. */
export function dockerReadiness(env = process.env, execute = spawnSync) {
    const result = execute('docker', ['version', '--format', '{{.Server.Version}} {{.Server.Os}}'], {
        env, encoding: 'utf8', windowsHide: true, timeout: 10000, maxBuffer: 1024 * 1024,
    });
    if (result.error?.code === 'ETIMEDOUT') return { ready: false, reason: 'Docker engine readiness check timed out.' };
    if (result.error || result.status !== 0) return { ready: false, reason: 'Docker engine is unavailable.' };
    if (!/^\S+ linux$/.test((result.stdout || '').trim())) return { ready: false, reason: 'A responding Linux Docker engine is required.' };
    return { ready: true };
}
