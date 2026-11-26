import { test } from 'node:test';
import assert from 'node:assert/strict';
import { resolve, sep } from 'node:path';
import { normalizeResults, requireIsolatedWorkspace } from './check-lint-compatibility.mjs';
test('diagnostic normalization preserves severity, messages, locations and fixes across workspace roots', () => {
    const diagnostic = { ruleId:'no-unused-vars',severity:2,message:'unused',line:2,column:3,endLine:2,endColumn:8,fix:{range:[1,4],text:'x'} };
    const a=resolve('target/a'),b=resolve('target/b');
    const first=normalizeResults([{filePath:a+sep+'src'+sep+'a.js',messages:[diagnostic],errorCount:1}],a);
    assert.deepEqual(first,normalizeResults([{filePath:b+sep+'src'+sep+'a.js',messages:[diagnostic],errorCount:1}],b));
    assert.equal(first[0].filePath,'src/a.js');assert.deepEqual(first[0].messages,[diagnostic]);
});
test('fixture runner refuses source workspace rather than writing fixture files into live frontend', () => {
    assert.throws(()=>requireIsolatedWorkspace(resolve('frontend')),/isolated/);
});
