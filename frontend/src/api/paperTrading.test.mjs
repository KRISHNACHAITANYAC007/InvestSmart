import test from "node:test";
import assert from "node:assert/strict";
import { createOrderSubmitter, loadTradingListing, validatePaperOrder } from "./paperTrading.js";
import { chartPath, groupCompanies } from "./marketInstruments.js";

test("validates quantity, cash and listing-specific holdings", () => {
    const order = { quantity:2, side:"buy", price:100, balance:200, holdings:1 };
    assert.equal(validatePaperOrder(order), "");
    assert.match(validatePaperOrder({ ...order, quantity:1.5 }), /whole-share/);
    assert.match(validatePaperOrder({ ...order, balance:199 }), /Insufficient/);
    assert.match(validatePaperOrder({ ...order, side:"sell" }), /this listing/);
    assert.match(validatePaperOrder({ ...order, price:null }), /current price/);
    assert.match(validatePaperOrder({ ...order, balance:null }), /cash/);
});
test("concurrent submit attempts place one order and allow a later intentional order", async () => {
    let finish, calls = 0;
    const submit = createOrderSubmitter({ post:async (url, body) => { calls++; assert.equal(url,"/orders/sell"); assert.deepEqual(body,{stockId:9,quantity:3}); return new Promise(r => {finish=r;}); } });
    const first = submit("sell",9,3);
    assert.equal(await submit("sell",9,3),null); assert.equal(calls,1);
    finish({data:{id:1}}); assert.equal((await first).data.id,1);
});
test("listing loads never accept another exchange's quote", async () => {
    const listing = {symbol:"INFY", instrumentKey:"BSE_EQ|500209"};
    const client = { post:async()=>({data:{id:2,instrumentKey:listing.instrumentKey}}), get:async()=>({data:{instrumentKey:"NSE_EQ|INE009A01021",currentPrice:1500}}) };
    await assert.rejects(loadTradingListing(client,listing),/does not match/);
    client.get=async(url,config)=>{ assert.equal(config.params.instrumentKey,listing.instrumentKey); return {data:{instrumentKey:listing.instrumentKey,currentPrice:1501}}; };
    assert.equal((await loadTradingListing(client,listing)).quote.currentPrice,1501);
});
test("company results group by ISIN while chart links retain listing identity", () => {
    const nse = {symbol:"INFY",isin:"INE009A01021",exchange:"NSE",instrumentKey:"NSE_EQ|INE009A01021"};
    const bse = {...nse,exchange:"BSE",instrumentKey:"BSE_EQ|500209"};
    const groups=groupCompanies([bse,nse]); assert.equal(groups.length,1); assert.equal(groups[0].exchange,"NSE"); assert.equal(groups[0].listings.length,2);
    assert.match(chartPath(bse),/instrumentKey=BSE_EQ%7C500209/);
});
