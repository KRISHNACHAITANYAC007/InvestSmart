export function phoneSecondsRemaining(expiresAt, serverTime, receivedAt, now = Date.now()) {
    const expires = Date.parse(expiresAt), server = Date.parse(serverTime);
    if (!Number.isFinite(expires) || !Number.isFinite(server)) return 0;
    return Math.max(0, Math.min(30, Math.ceil((expires - server - Math.max(0, now - receivedAt)) / 1000)));
}

export function phoneLinkToken(hash) {
    const values = new URLSearchParams(hash.replace(/^#/, ""));
    for (const kind of ["pair", "setup"]) {
        const token = values.get(kind);
        if (token && /^[A-Za-z0-9_-]{20,200}$/.test(token)) return { kind, token };
    }
    return null;
}

export function isPhoneStep(step) {
    return ["PHONE_SETUP", "PHONE_REQUIRED"].includes(step?.state);
}

// Recovery codes are discarded on background/navigation; repeat unconfirmed enrollment.
export function phoneStageAfterHide(stage) {
    return stage === "recovery" ? "register" : stage === "display" ? "closed" : stage;
}
