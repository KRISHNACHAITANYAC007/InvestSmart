import { test } from "node:test";
import assert from "node:assert/strict";
import { ticketUnits, switchQuantityMode } from "./paperTicket.js";
test("amount entry floors affordable shares and never invents a partial option lot", () => {
    assert.equal(ticketUnits({ mode: "AMOUNT", amount: "400", price: "150", lot: 1 }), 2);
    assert.equal(ticketUnits({ mode: "AMOUNT", amount: "999", price: "20", lot: 50 }), 0);
    assert.equal(ticketUnits({ mode: "AMOUNT", amount: "1000", price: "20", lot: 50 }), 50);
});
test("switching units/lots preserves the intended exposure instead of multiplying it", () => {
    const lots = switchQuantityMode({ mode: "UNITS", quantity: "100", amount: "" }, "LOTS", 20, 50);
    assert.equal(lots.quantity, "2"); assert.equal(ticketUnits({ ...lots, price: 20, lot: 50 }), 100);
    assert.equal(switchQuantityMode(lots, "UNITS", 20, 50).quantity, "100");
    assert.equal(switchQuantityMode(lots, "AMOUNT", 20, 50).amount, "2000");
});
test("missing prices and invalid metadata cannot produce an amount-based quantity", () => {
    for (const price of [null, 0, -1, "not-a-price"]) assert.equal(ticketUnits({ mode: "AMOUNT", amount: 1000, price, lot: 50 }), 0);
    assert.equal(ticketUnits({ mode: "AMOUNT", amount: 1000, price: 20, lot: 0 }), 0);
});
import { ticketIntent } from './paperTicket.js';
import { test as intentTest } from 'node:test';
import { strict as intentAssert } from 'node:assert';
intentTest('short entry and position-reducing exits are distinct from selling holdings', () => {
    intentAssert.equal(ticketIntent({ side:'SELL', product:'CASH' }), 'Sell holdings');
    intentAssert.equal(ticketIntent({ side:'SELL', product:'MIS' }), 'Sell/Short Intraday');
    intentAssert.equal(ticketIntent({ side:'BUY', product:'MIS', closing:true }), 'Buy to close short');
    intentAssert.equal(ticketIntent({ side:'SELL', product:'MIS', closing:true }), 'Sell to close long');
    intentAssert.equal(ticketIntent({ side:'SELL', product:'CASH', option:true }), 'Sell');
});
