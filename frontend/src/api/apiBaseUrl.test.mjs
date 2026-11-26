import test from "node:test";
import assert from "node:assert/strict";
import { resolveApiBaseUrl } from "./apiBaseUrl.js";

test("local API host matches the browser hostname so CSRF cookies are sent", () => {
    assert.equal(resolveApiBaseUrl(undefined, "localhost"), "http://localhost:8080");
    assert.equal(resolveApiBaseUrl(undefined, "127.0.0.1"), "http://127.0.0.1:8080");
});

test("configured API URL takes precedence over local host detection", () => {
    assert.equal(resolveApiBaseUrl("/api", "127.0.0.1"), "/api");
});

test("deployed and phone-accessed hosts use their own API proxy", () => {
    assert.equal(resolveApiBaseUrl(undefined, "invest.example"), "/api");
    assert.equal(resolveApiBaseUrl(undefined, "192.0.2.10"), "/api");
});
