import assert from "node:assert/strict";
import { afterEach, test } from "node:test";
import { createPlatformPasskey, getPlatformPasskey, platformAuthenticatorAvailable } from "./passkeys.js";

const originalWindow = Object.getOwnPropertyDescriptor(globalThis, "window");
const originalNavigator = Object.getOwnPropertyDescriptor(globalThis, "navigator");

afterEach(() => {
    if (originalWindow) Object.defineProperty(globalThis, "window", originalWindow);
    else delete globalThis.window;
    if (originalNavigator) Object.defineProperty(globalThis, "navigator", originalNavigator);
    else delete globalThis.navigator;
});

function installBrowser(credentials, supported = true, secure = true) {
    class PublicKeyCredential {
        static async isUserVerifyingPlatformAuthenticatorAvailable() { return supported; }
    }
    Object.defineProperty(globalThis, "window", {
        configurable: true,
        value: { isSecureContext: secure, PublicKeyCredential },
    });
    Object.defineProperty(globalThis, "navigator", { configurable: true, value: { credentials } });
}

function bytes(...values) {
    return new Uint8Array(values).buffer;
}

test("platform authenticator is only reported in a secure context when supported", async () => {
    installBrowser({ create() {}, get() {} }, true, false);
    assert.equal(await platformAuthenticatorAvailable(), false);
    installBrowser({ create() {}, get() {} }, true);
    assert.equal(await platformAuthenticatorAvailable(), true);
    installBrowser({ create() {}, get() {} }, false);
    assert.equal(await platformAuthenticatorAvailable(), false);
});

test("incomplete passkey options fail with a refresh hint instead of a replace error", async () => {
    installBrowser({ create() {}, get() {} });
    await assert.rejects(
        () => createPlatformPasskey({ user: { id: "AQ" } }),
        /Passkey challenge is missing\. Refresh the page and try again\./,
    );
    await assert.rejects(
        () => createPlatformPasskey({ challenge: "AQ" }),
        /Passkey account details are missing\. Refresh the page and try again\./,
    );
});

test("malformed serialized options fail before requesting device authentication", async () => {
    let called = false;
    installBrowser({ create() { called = true; }, get() { called = true; } });
    await assert.rejects(() => createPlatformPasskey("not-json"), /Passkey setup options are invalid/);
    for (const value of ["null", "[]", "42", undefined]) {
        await assert.rejects(() => getPlatformPasskey(value), /Passkey setup options are missing/);
    }
    assert.equal(called, false);
});

test("local RP mismatch is explained before requesting device authentication", async () => {
    let called = false;
    installBrowser({ create() { called = true; }, get() {} });
    window.location = { hostname: "localhost" };
    await assert.rejects(() => createPlatformPasskey(JSON.stringify({ publicKey: {
        challenge: "AQ", user: { id: "Ag" }, rp: { id: "127.0.0.1" },
    } })), /Local passkey setup requires http:\/\/localhost:5173\/account/);
    assert.equal(called, false);
});

test("object options remain supported alongside serialized ceremony options", async () => {
    let captured;
    installBrowser({ create() {}, async get(options) { captured = options; return null; } });
    for (const options of [{ challenge: "AQ" }, { publicKey: { challenge: "AQ" } }]) {
        await assert.rejects(() => getPlatformPasskey(options), /unlock was cancelled/);
        assert.deepEqual([...captured.publicKey.challenge], [1]);
    }
});

test("registration decodes request buffers and serializes the public credential response", async () => {
    let request;
    installBrowser({
        async create(options) {
            request = options.publicKey;
            return {
                id: "credential",
                rawId: bytes(6, 7),
                type: "public-key",
                getClientExtensionResults: () => ({}),
                response: {
                    clientDataJSON: bytes(1),
                    attestationObject: bytes(2, 3),
                    getTransports: () => ["internal"],
                },
            };
        },
        get() {},
    });
    const result = await createPlatformPasskey(JSON.stringify({
        publicKey: {
            challenge: "AQID",
            user: { id: "BAU", name: "person@example.invalid" },
            excludeCredentials: [{ id: "Bgc", type: "public-key" }],
        },
    }));
    assert.deepEqual([...request.challenge], [1, 2, 3]);
    assert.deepEqual([...request.user.id], [4, 5]);
    assert.deepEqual([...request.excludeCredentials[0].id], [6, 7]);
    assert.equal(result.rawId, "Bgc");
    assert.equal(result.response.clientDataJSON, "AQ");
    assert.equal(result.response.attestationObject, "AgM");
    assert.deepEqual(result.response.transports, ["internal"]);
});

test("assertion decodes allowed credentials and preserves an unavailable user handle as null", async () => {
    let request;
    installBrowser({
        create() {},
        async get(options) {
            request = options.publicKey;
            return {
                id: "credential",
                rawId: bytes(8),
                type: "public-key",
                getClientExtensionResults: () => ({}),
                response: {
                    clientDataJSON: bytes(9),
                    authenticatorData: bytes(10),
                    signature: bytes(11),
                    userHandle: null,
                },
            };
        },
    });
    const result = await getPlatformPasskey(JSON.stringify({
        publicKey: {
            challenge: "AQ",
            allowCredentials: [{ id: "Ag", type: "public-key" }],
        },
    }));
    assert.deepEqual([...request.challenge], [1]);
    assert.deepEqual([...request.allowCredentials[0].id], [2]);
    assert.equal(result.rawId, "CA");
    assert.equal(result.response.userHandle, null);
});
