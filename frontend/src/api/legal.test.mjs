import test from "node:test";
import assert from "node:assert/strict";
import { LEGAL_DOCUMENTS, LEGAL_LINKS, acknowledgmentNeeded, canBeginAuthentication } from "../legal/content.js";
test("login and registration cannot begin without explicit acknowledgment; factor/recovery flows are preserved", () => {
    for (const mode of ["login", "register"]) {
        assert.equal(acknowledgmentNeeded(mode, null), true);
        for (const value of [false, undefined, null, "true"]) assert.equal(canBeginAuthentication(mode, null, value), false);
        assert.equal(canBeginAuthentication(mode, null, true), true);
    }
    assert.equal(canBeginAuthentication("login", { challenge: "factor" }, false), true);
    for (const mode of ["verify-email", "forgot-password", "reset-password", "resend-verification"]) assert.equal(canBeginAuthentication(mode, null, false), true);
});
test("public legal navigation has unique destinations and no unsupported registration claim", () => {
    assert.deepEqual(LEGAL_LINKS.map(([path]) => path), ["/legal/terms", "/legal/paper-trading", "/legal/market-data", "/legal/privacy", "/about"]);
    assert.equal(new Set(LEGAL_LINKS.map(([path]) => path)).size, 5);
    assert.match(JSON.stringify(LEGAL_DOCUMENTS), /Krishna Chaitanya/);
    assert.doesNotMatch(JSON.stringify(LEGAL_DOCUMENTS), /®/);
});
test("legal pages render accessible public content and navigable links without authentication context", async () => {
    const { createServer } = await import("vite");
    // An in-process test server must not rewrite the running dev server's cache.
    const server = await createServer({ cacheDir: "node_modules/.vite-legal-tests", server: { middlewareMode: true }, appType: "custom" });
    try {
        const { default: Page } = await server.ssrLoadModule("/src/pages/LegalPages.jsx");
        const { createElement } = await import("react");
        const { renderToStaticMarkup } = await import("react-dom/server");
        const { MemoryRouter } = await import("react-router-dom");
        for (const [document, doc] of Object.entries(LEGAL_DOCUMENTS)) {
            const html = renderToStaticMarkup(createElement(MemoryRouter, null, createElement(Page, { document })));
            assert.match(html, /Legal review required before public launch/);
            assert.ok(html.includes(doc.title));
            assert.match(html, /id="legal-title"/);
            for (const [path] of LEGAL_LINKS) assert.ok(html.includes(`href="${path}"`));
            assert.doesNotMatch(html, /mailto:/);
        }
    } finally { await server.close(); }
});
