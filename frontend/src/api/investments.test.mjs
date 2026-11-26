import test from 'node:test';
import assert from 'node:assert/strict';
import { ipoWindowOpen, ipoReservation } from './investments.js';
test('IPO application uses IST dates and published minimum lots', () => {
    const ipo = { status: 'open', openDate: '2026-11-03', closeDate: '2026-11-04', dailyStart: '10:00:00', dailyEnd: '17:00:00', lotSize: 50, minimumQuantity: 100, maximumPrice: 20 };
    assert.equal(ipoWindowOpen(ipo, new Date('2026-11-03T04:29:59Z')), false);
    assert.equal(ipoWindowOpen(ipo, new Date('2026-11-03T04:30:00Z')), true);
    assert.equal(ipoWindowOpen(ipo, new Date('2026-11-03T11:30:01Z')), false);
    assert.equal(ipoWindowOpen({ ...ipo, stale: true }, new Date('2026-11-03T05:00Z')), false);
    assert.equal(ipoReservation(ipo, 1), null);
    assert.deepEqual(ipoReservation(ipo, 2), { quantity: 100, amount: 2000 });
    assert.equal(ipoReservation(ipo, 2.5), null);
});
