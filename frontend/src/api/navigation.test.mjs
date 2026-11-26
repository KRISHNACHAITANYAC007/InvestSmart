import test from "node:test";
import assert from "node:assert/strict";
import {MAIN_PAGES,legacyDestination} from "./navigation.js";
test("five destinations retain the approved heatmap shortcut",()=>{
    assert.deepEqual(MAIN_PAGES.map(([path])=>path),["/dashboard","/market","/heatmaps","/portfolio","/orders"]);
});
test("legacy routes retain useful destinations and chart exchange identity",()=>{
    assert.equal(legacyDestination("/stocks"),"/market?tab=explore");assert.equal(legacyDestination("/watchlist"),"/market?tab=watchlist");assert.equal(legacyDestination("/wallet"),"/portfolio?tab=funds");
    assert.equal(legacyDestination("/market/INFY","?instrumentKey=BSE_EQ%7C500209"),"/market/chart/INFY?instrumentKey=BSE_EQ%7C500209");
    assert.equal(legacyDestination("/market/NIFTY%2050"),"/market/chart/NIFTY%2050");
});
