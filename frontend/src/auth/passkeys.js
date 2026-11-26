function fromBase64Url(value, label = "credential data") {
    if (typeof value !== "string" || value.length === 0) {
        throw new Error(`Passkey ${label} is missing. Refresh the page and try again.`);
    }
    const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
    const binary = atob(base64 + "=".repeat((4 - base64.length % 4) % 4));
    return Uint8Array.from(binary, character => character.charCodeAt(0));
}

function toBase64Url(value) {
    const bytes = new Uint8Array(value);
    let binary = "";
    for (const byte of bytes) binary += String.fromCharCode(byte);
    return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

export async function platformAuthenticatorAvailable() {
    return Boolean(window.isSecureContext && navigator.credentials?.create && navigator.credentials?.get
        && window.PublicKeyCredential
        && typeof window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable === "function"
        && await window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable());
}

function decodeOptions(options, registration) {
    // The ceremony API transports Yubico's browser options as a JSON string.
    if (typeof options === "string") {
        try { options = JSON.parse(options); }
        catch { throw new Error("Passkey setup options are invalid. Refresh the page and try again."); }
    }
    if (!options || typeof options !== "object" || Array.isArray(options)) {
        throw new Error("Passkey setup options are missing. Refresh the page and try again.");
    }
    const publicKey = options.publicKey && typeof options.publicKey === "object"
        ? options.publicKey
        : options;
    const decoded = { ...publicKey, challenge: fromBase64Url(publicKey.challenge, "challenge") };
    if (registration) {
        if (!publicKey.user || typeof publicKey.user !== "object") {
            throw new Error("Passkey account details are missing. Refresh the page and try again.");
        }
        decoded.user = { ...publicKey.user, id: fromBase64Url(publicKey.user.id, "account identifier") };
        decoded.excludeCredentials = (publicKey.excludeCredentials || []).map(item => ({ ...item, id: fromBase64Url(item.id, "credential identifier") }));
    } else {
        decoded.allowCredentials = (publicKey.allowCredentials || []).map(item => ({ ...item, id: fromBase64Url(item.id, "credential identifier") }));
    }
    return decoded;
}

function serializeCredential(credential, registration) {
    const response = credential.response;
    const payload = {
        id: credential.id,
        rawId: toBase64Url(credential.rawId),
        type: credential.type,
        clientExtensionResults: credential.getClientExtensionResults(),
        response: {
            clientDataJSON: toBase64Url(response.clientDataJSON),
        },
    };
    if (registration) {
        payload.response.attestationObject = toBase64Url(response.attestationObject);
        payload.response.transports = response.getTransports?.() || [];
    } else {
        payload.response.authenticatorData = toBase64Url(response.authenticatorData);
        payload.response.signature = toBase64Url(response.signature);
        payload.response.userHandle = response.userHandle ? toBase64Url(response.userHandle) : null;
    }
    return payload;
}

export async function createPlatformPasskey(options) {
    const publicKey = decodeOptions(options, true);
    const hostname = typeof window === "undefined" ? undefined : window.location?.hostname;
    if (hostname && publicKey.rp?.id && publicKey.rp.id !== hostname
        && ["localhost", "127.0.0.1", "[::1]"].includes(hostname)) {
        throw new Error("Local passkey setup requires http://localhost:5173/account. Open that address and sign in again. Restart the backend if its public URL was changed.");
    }
    const credential = await navigator.credentials.create({ publicKey });
    if (!credential) throw new Error("Passkey setup was cancelled.");
    return serializeCredential(credential, true);
}

export async function getPlatformPasskey(options) {
    const credential = await navigator.credentials.get({ publicKey: decodeOptions(options, false) });
    if (!credential) throw new Error("Passkey unlock was cancelled.");
    return serializeCredential(credential, false);
}
