import test from "node:test";
import assert from "node:assert/strict";
import { createGraphWatchlist } from "./graphWatchlist.js";

const tick = () => new Promise(resolve => setTimeout(resolve, 0));
function deferred() {
    let resolve;
    const promise = new Promise(done => { resolve = done; });
    return { promise, resolve };
}
const stocks = count => Array.from({ length: count }, (_, index) => ({
    id: index + 1, symbol: `STOCK${index + 1}`, currentPrice: 100,
}));

test("new additions appear first while a slow price refresh remains pending", async () => {
    const prices = deferred();
    let items = stocks(3);
    let state;
    let priceCalls = 0;
    const loader = createGraphWatchlist({
        get: async () => ({ data: items }),
        post: () => { priceCalls++; return prices.promise; },
    }, next => { state = next; });
    const initial = loader.refresh();
    await tick();
    assert.equal(state.loading, false);
    assert.equal(state.items.length, 3);
    items = stocks(4);
    const afterAdd = loader.refresh();
    await tick();
    assert.deepEqual(state.items.slice(0, 3).map(item => item.symbol), ["STOCK4", "STOCK3", "STOCK2"]);
    assert.equal(state.items.length, 4);
    assert.equal(priceCalls, 1, "membership events share the in-flight price refresh");
    prices.resolve({ data: { staleSymbols: [] } });
    await Promise.all([initial, afterAdd]);
    assert.equal(state.refreshing, false);
    loader.dispose();
});

test("an older membership response cannot overwrite a later addition", async () => {
    const older = deferred(), newer = deferred(), prices = deferred();
    let calls = 0;
    let state;
    const loader = createGraphWatchlist({
        get: () => ++calls === 1 ? older.promise : newer.promise,
        post: () => prices.promise,
    }, next => { state = next; });
    const first = loader.refresh();
    const second = loader.refresh();
    newer.resolve({ data: stocks(4) });
    await tick();
    older.resolve({ data: stocks(3) });
    await tick();
    assert.equal(state.items[0].symbol, "STOCK4");
    loader.dispose();
    prices.resolve({ data: { staleSymbols: [] } });
    await Promise.all([first, second]);
});

test("manual refresh forces prices and failures preserve the last membership", async () => {
    let failList = false;
    let state;
    const requestedForces = [];
    const loader = createGraphWatchlist({
        get: async () => {
            if (failList) throw new Error("list offline");
            return { data: stocks(4) };
        },
        post: async (_url, _body, config) => {
            requestedForces.push(config.params.force);
            throw new Error("price provider offline");
        },
    }, next => { state = next; });
    await loader.refresh(true);
    assert.deepEqual(requestedForces, [true]);
    assert.equal(state.items.length, 4);
    assert.equal(state.pricesStale, true);
    assert.equal(state.listError, "");
    failList = true;
    await loader.refresh();
    assert.equal(state.items.length, 4);
    assert.match(state.listError, /Could not reload your watchlist/);
    assert.equal(state.refreshing, false);
    loader.dispose();
});

test("unmount cancels requests and prevents late state updates", async () => {
    const list = deferred(), prices = deferred();
    const signals = [];
    let updates = 0;
    const loader = createGraphWatchlist({
        get: (_url, config) => { signals.push(config.signal); return list.promise; },
        post: (_url, _body, config) => { signals.push(config.signal); return prices.promise; },
    }, () => { updates++; });
    const pending = loader.refresh();
    loader.dispose();
    const before = updates;
    assert.ok(signals.every(signal => signal.aborted));
    list.resolve({ data: stocks(4) });
    prices.resolve({ data: { staleSymbols: [] } });
    await pending;
    assert.equal(updates, before);
});
