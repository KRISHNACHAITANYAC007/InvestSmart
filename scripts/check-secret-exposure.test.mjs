import {test} from 'node:test';
import assert from 'node:assert/strict';
import {scanText} from './check-secret-exposure.mjs';
test('credential findings never reproduce values',()=>{
    const value='sensitive-fixture-not-a-real-credential';
    const result=scanText(`jwt.secret=${value}\n{"password":"${value}"}`,'scratch.http');
    assert.equal(result.length,2);assert.equal(JSON.stringify(result).includes(value),false);
});
test('provider-looking tokens are indicators and are never returned',()=>{
    const token='ghp_'+'a'.repeat(36);const result=scanText(token,'settings.txt');
    assert.equal(result[0].kind,'provider-token');assert.equal(JSON.stringify(result).includes(token),false);
});
test('environment placeholders are not literal credentials and fixtures remain reviewable',()=>{
    assert.equal(scanText('JWT_SECRET=${JWT_SECRET}\nDB_PASSWORD=\nPUBLIC_URL=https://example.invalid\nSMTP_PASSWORD=\nMAIL_FROM=sender@example.invalid','config.properties').length,0);
    const findings=scanText('{"password":"fixture-only"}','src/test/example.java');
    assert.equal(findings[0].context,'fixture-or-template-review');
});
