import test from "node:test";
import assert from "node:assert/strict";
import { isPhoneStep, phoneLinkToken, phoneSecondsRemaining, phoneStageAfterHide } from "./phoneVerification.js";

test("phone countdown uses server time and never shows expired codes", () => {
    const server = "2026-11-10T10:00:01Z", expiry = "2026-11-10T10:00:30Z";
    assert.equal(phoneSecondsRemaining(expiry, server, 1000, 1000), 29);
    assert.equal(phoneSecondsRemaining(expiry, server, 1000, 2000), 28);
    assert.equal(phoneSecondsRemaining(expiry, server, 1000, 30000), 0);
    assert.equal(phoneSecondsRemaining(expiry, server, 1000, 40000), 0);
    assert.equal(phoneSecondsRemaining("invalid", server, 1000, 1000), 0);
});
test("only valid pairing and replacement fragments are accepted", () => {
    const token = "a".repeat(43);
    assert.deepEqual(phoneLinkToken(`#pair=${token}`), { kind: "pair", token });
    assert.deepEqual(phoneLinkToken(`#setup=${token}`), { kind: "setup", token });
    for (const hash of ["", "#pair=short", "#pair=https://example.invalid", "#token=" + token]) assert.equal(phoneLinkToken(hash), null);
});
test("regular phone states remain distinct from owner authenticator states", () => {
    assert.equal(isPhoneStep({ state: "PHONE_SETUP" }), true);
    assert.equal(isPhoneStep({ state: "PHONE_REQUIRED" }), true);
    for (const state of ["MFA_REQUIRED", "MFA_SETUP", "SIGNED_IN", "VERIFY_EMAIL"]) assert.equal(isPhoneStep({ state }), false);
    assert.equal(isPhoneStep(null), false);
});

test("background and restored pages cannot redisplay codes or saved recovery codes", () => {
    assert.equal(phoneStageAfterHide("display"), "closed");
    assert.equal(phoneStageAfterHide("recovery"), "register");
    assert.equal(phoneStageAfterHide("register"), "register");
    assert.equal(phoneStageAfterHide("closed"), "closed");
});
