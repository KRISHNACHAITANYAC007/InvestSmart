import { test } from 'node:test';
import assert from 'node:assert/strict';
import { previewFailure, previewIsFresh } from './paperPreview.js';

test('preview cooldown respects server seconds and HTTP-date without exposing generic server errors', () => {
    assert.equal(previewFailure({ response: { status: 429, headers: { 'retry-after': '900' } } }, 1000).retryAt, 901000);
    assert.equal(previewFailure({ response: { status: 429, headers: { 'retry-after': 'Thu, 01 Jan 1970 00:02:00 GMT' } } }, 1000).retryAt, 120000);
    assert.ok(!previewFailure({ response: { status: 500, data: { message: 'private provider diagnostics' } } }).message.includes('private'));
});
test('expired or undated estimates cannot authorize submission', () => {
    assert.equal(previewIsFresh({ expiresAt: '1970-01-01T00:00:02Z' }, 1000), true);
    assert.equal(previewIsFresh({ expiresAt: '1970-01-01T00:00:02Z' }, 2000), false);
    assert.equal(previewIsFresh({}), false);
    assert.equal(previewIsFresh(null), false);
});

test('provider 503 cooldown survives into preview retry scheduling',()=>{assert.equal(previewFailure({response:{status:503,headers:{'retry-after':'120'},data:{message:'Provider unavailable'}}},1000).retryAt,121000);});
