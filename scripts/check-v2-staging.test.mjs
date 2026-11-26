import { test } from 'node:test';
import assert from 'node:assert/strict';
import { validateStaging } from './check-v2-staging.mjs';
const errors = allowlist => validateStaging({ STAGING_ALLOWED_CIDRS: allowlist }).filter(message => message.startsWith('STAGING_ALLOWED_CIDRS'));
test('staging accepts bounded IPv4 and IPv6 tester allowlists', () => {
    assert.deepEqual(errors('192.0.2.5 198.51.100.0/24 2001:db8::5/128'), []);
});
test('staging fails closed for absent, global, malformed and injected allowlists', () => {
    for (const input of ['', '0.0.0.0/0', '::/0', '0.0.0.0', '::', '192.0.0.0/8', 'example.org', '192.0.2.5/33', '2001:db8::/129', '192.0.2.5/32/1', '192.0.2.5\nrespond /api 200']) assert.ok(errors(input).length, 'Unsafe allowlist accepted');
});
test('staging diagnostics never echo supplied configuration', () => {
    assert.equal(validateStaging({ STAGING_ALLOWED_CIDRS: 'private-value', DB_PASSWORD: 'private-value' }).join(' ').includes('private-value'), false);
});
test('Compose staging additionally requires the root secret while runtime-only mode does not',()=>{
    assert.ok(validateStaging({}, {compose:true}).includes('Missing MYSQL_ROOT_PASSWORD'));
    assert.equal(validateStaging({}).includes('Missing MYSQL_ROOT_PASSWORD'),false);
});
