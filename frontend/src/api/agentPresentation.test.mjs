import test from "node:test";
import assert from "node:assert/strict";
import { agentMoney, createAgentRequestGate, appendAgentReply } from "./agentPresentation.js";
test("cancelled or old account requests cannot publish a late reply or finish a newer request", async () => {
    const oldSession = createAgentRequestGate(); const first = oldSession.begin();
    let resolve; const response = new Promise(done => { resolve = done; });
    const visible = [];
    const waiting = response.then(data => { if (oldSession.isCurrent(first)) visible.push(data); });
    oldSession.cancel(); const second = oldSession.begin();
    resolve("old private evidence"); await waiting;
    assert.deepEqual(visible, []); assert.equal(first.signal.aborted, true);
    assert.equal(oldSession.finish(first), false); assert.equal(oldSession.isCurrent(second), true);
    oldSession.cancel();
    const newSession = createAgentRequestGate(); const current = newSession.begin();
    assert.equal(oldSession.isCurrent(second), false); assert.equal(newSession.isCurrent(current), true);
    assert.equal(newSession.finish(current), true);
});
test("in-memory replies drop older evidence after six responses", () => {
    let replies = [];
    for (let i = 0; i < 10; i++) replies = appendAgentReply(replies, { id: i });
    assert.deepEqual(replies.map(reply => reply.id), [4, 5, 6, 7, 8, 9]);
});
test("agent money preserves decimal precision and missing data", () => {
    assert.equal(agentMoney("99999999999999999.99"), "₹99,99,99,99,99,99,99,999.99");
    assert.equal(agentMoney("-1234.5"), "-₹1,234.50");
    for (const value of [null, undefined, 12, "NaN", "1.001", "<script>"]) assert.equal(agentMoney(value), "Unavailable");
});
test("portfolio reply renders escaped evidence, timestamps and explicit unavailable data", async () => {
    const { createServer } = await import("vite");
    const server = await createServer({ cacheDir: "node_modules/.vite-agent-tests", server: { middlewareMode: true }, appType: "custom" });
    try {
        const { PortfolioReply } = await server.ssrLoadModule("/src/pages/AgentConversation.jsx");
        const { createElement } = await import("react");
        const { renderToStaticMarkup } = await import("react-dom/server");
        const html = renderToStaticMarkup(createElement(PortfolioReply, { analysis: {
            status: "INCOMPLETE_VALUATION", equityValue: null, equityUnrealizedPnl: null, largestListings: [],
            evidence: { readAt: "2026-11-23T05:00:00Z", holdings: [{ instrumentKey: "synthetic", symbol: "<img src=x onerror=alert(1)>", quantity: 1, value: null, quoteAt: null, availability: "UNAVAILABLE_OR_STALE" }] },
            explanation: ["Educational paper simulation.", "No daily cause established."]
        } }));
        assert.match(html, /Unavailable/); assert.match(html, /2026-11-23T05:00:00Z/);
        assert.match(html, /&lt;img/); assert.doesNotMatch(html, /<img/); assert.match(html, /<summary>/);
    } finally { await server.close(); }
});
