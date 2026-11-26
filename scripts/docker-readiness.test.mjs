import { test } from 'node:test';
import assert from 'node:assert/strict';
import { dockerReadiness } from './docker-readiness.mjs';

test('readiness queries the responding Linux server with a bounded version request', () => {
    const env = { FIXTURE: 'private-value' };
    const result = dockerReadiness(env, (command, args, options) => {
        assert.equal(command, 'docker'); assert.deepEqual(args, ['version', '--format', '{{.Server.Version}} {{.Server.Os}}']);
        assert.equal(options.timeout, 10000); assert.equal(options.env, env); assert.equal(options.windowsHide, true);
        return { status: 0, stdout: '29.9.3 linux\r\n' };
    });
    assert.deepEqual(result, { ready: true });
});
test('timeout cannot report readiness and does not expose raw diagnostics', () => {
    const result = dockerReadiness({}, () => ({ status: null, error: { code: 'ETIMEDOUT', message: 'private-value' } }));
    assert.equal(result.ready, false); assert.match(result.reason, /timed out/); assert.ok(!result.reason.includes('private-value'));
});
test('missing CLI or unavailable server fails closed', () => {
    for (const failure of [{ error: { code: 'ENOENT' } }, { status: 1, stderr: 'private-value' }])
        assert.deepEqual(dockerReadiness({}, () => failure), { ready: false, reason: 'Docker engine is unavailable.' });
});
test('Windows engine and missing or malformed server data are rejected', () => {
    for (const stdout of ['29.9.3 windows', '', '<no value>', '29.9.3 linux\nextra'])
        assert.equal(dockerReadiness({}, () => ({ status: 0, stdout })).ready, false);
});
