import assert from "node:assert/strict";
import { test } from "node:test";
import { authSubmission } from "./authSubmission.js";

test("login submits actual form values and preserves autofilled passwords exactly", () => {
    const form = new FormData();
    form.set("email", "person@example.invalid");
    form.set("password", "  updated password #!  ");
    assert.deepEqual(authSubmission("login", form), { email: "person@example.invalid", password: "  updated password #!  " });
});
test("reset and registration use the same submitted password without confirmation leakage", () => {
    const form = new FormData();
    form.set("name", "Fixture"); form.set("email", "person@example.invalid");
    form.set("password", "fresh new passphrase"); form.set("confirmation", "fresh new passphrase");
    assert.deepEqual(authSubmission("reset-password", form, "fixture-token"), { token: "fixture-token", password: "fresh new passphrase" });
    assert.deepEqual(authSubmission("register", form), { name: "Fixture", email: "person@example.invalid", password: "fresh new passphrase" });
});
test("verification, recovery and MFA send only their required fields", () => {
    const form = new FormData(); form.set("email", "person@example.invalid");form.set("password", "not-for-mfa");form.set("code", " 123456 ");
    assert.deepEqual(authSubmission("login", form, null, "challenge"), { challenge: "challenge", code: "123456" });
    assert.deepEqual(authSubmission("verify-email", form, "token"), { token: "token" });
    assert.deepEqual(authSubmission("forgot-password", form), { email: "person@example.invalid" });
});
