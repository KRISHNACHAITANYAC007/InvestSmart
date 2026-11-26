import { test } from 'node:test';
import assert from 'node:assert/strict';
import { styleUnavailable, newProductDraft, PAPER_PRODUCTS } from './paperProducts.js';
test('product and style are independent while unsupported combinations have explanations', () => {
    assert.deepEqual(PAPER_PRODUCTS.map(p => p.value), ['CASH', 'MIS', 'MTF', 'COVER']);
    assert.ok(styleUnavailable('MIS', 'ICEBERG'));
    assert.equal(styleUnavailable('CASH', 'ICEBERG'), '');
    assert.equal(styleUnavailable('MIS', 'QUICK'), '');
    assert.ok(styleUnavailable('COVER', 'QUICK'));
});
test('product changes clear incompatible exits and retain entered quantity', () => {
    const mis = newProductDraft({ quantity: '12', tab: 'ICEBERG', validity: 'GTT', stop: '90', target: '110', kind: 'STOP_LIMIT' }, 'MIS');
    assert.equal(mis.quantity, '12');assert.equal(mis.tab, 'REGULAR');assert.equal(mis.validity, 'DAY');assert.equal(mis.stop, '');assert.equal(mis.target, '');
    assert.equal(newProductDraft(mis, 'COVER').stop, '10');
});
