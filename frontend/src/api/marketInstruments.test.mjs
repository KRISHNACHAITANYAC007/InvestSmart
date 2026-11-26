import test from "node:test";
import assert from "node:assert/strict";
import { isIndexInstrument } from "./marketInstruments.js";

test("market indices are identified consistently across route and quote formats", () => {
    for (const symbol of ["NIFTY 50", "SENSEX", "NIFTY BANK", "NIFTY IT"]) {
        assert.equal(isIndexInstrument({ symbol }), true);
    }
    assert.equal(isIndexInstrument({ symbol: "nifty_it" }), true);
    assert.equal(isIndexInstrument({ symbol: "NIFTY IT INDEX" }), true);
    assert.equal(isIndexInstrument({ instrumentKey: "NSE_INDEX|Nifty IT" }), true);
    assert.equal(isIndexInstrument({ symbol: "INFY", instrumentKey: "NSE_EQ|INE009A01021" }), false);
});
