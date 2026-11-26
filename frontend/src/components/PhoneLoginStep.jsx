import { useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/api.js";
import { getPlatformPasskey } from "../auth/passkeys.js";

export default function PhoneLoginStep({ step, onSignedIn, onBack }) {
    const [busy, setBusy] = useState(false), [error, setError] = useState("");
    const [recovery, setRecovery] = useState(false), [pairingUrl, setPairingUrl] = useState("");
    async function run(operation) {
        if (busy) return;
        setBusy(true); setError("");
        try { await operation(); }
        catch (failure) { setError(failure.response?.data?.message || failure.message || "Phone verification could not be completed."); }
        finally { setBusy(false); }
    }
    function submit(event) {
        event.preventDefault(); const code = String(new FormData(event.currentTarget).get("code") || "").trim();
        run(async () => {
            const { data } = await api.post(recovery ? "/auth/phone/recover" : "/auth/phone/verify", { challenge: step.challenge, code });
            if (recovery) setPairingUrl(data.pairingUrl); else onSignedIn(data);
        });
    }
    function usePhone() {
        run(async () => {
            const { data: ceremony } = await api.post("/auth/phone/login/options", { challenge: step.challenge });
            const credential = await getPlatformPasskey(ceremony.publicKey);
            const { data } = await api.post("/auth/phone/login", { ...ceremony, publicKey: undefined, loginChallenge: step.challenge, credential: JSON.stringify(credential) });
            onSignedIn(data);
        });
    }
    return <div className="phone-login-step">
        {step.state === "PHONE_SETUP" ? <p className="account-notice">Open the pairing email on your phone, confirm the link and register its biometric/PIN passkey. Save your recovery codes, then enter the phone code here. Pairing expires after 10 minutes.</p>
            : <p className="account-muted">Open <strong>InvestSmart → Phone codes</strong> on your paired phone and verify with its biometric or PIN. Enter the six-digit code shown there.</p>}
        {pairingUrl ? <p className="account-notice">The old phone credential and sessions have been revoked. <a href={pairingUrl}>Open replacement phone setup</a> on your phone within 10 minutes. You can copy this private link to your phone. Sign in again after pairing.</p> : <>
            <form onSubmit={submit}>
                <label>{recovery ? "Single-use recovery code" : "Six-digit phone code"}<input key={String(recovery)} name="code" autoComplete="one-time-code" inputMode={recovery ? "text" : "numeric"} pattern={recovery ? undefined : "[0-9]{6}"} minLength={recovery ? undefined : 6} maxLength={recovery ? 100 : 6} required /></label>
                <button className="ui-button primary" disabled={busy}>{busy ? "Please wait…" : recovery ? "Replace lost phone" : "Verify and sign in"}</button>
            </form>
            {step.state === "PHONE_REQUIRED" && <><button className="ui-button" disabled={busy} onClick={usePhone}>I’m using my paired phone — verify with biometric/PIN</button><button className="ui-button" disabled={busy} onClick={() => { setRecovery(value => !value); setError(""); }}>{recovery ? "Use phone code" : "Lost phone? Use a recovery code"}</button></>}
        </>}
        {error && <p className="account-error" role="alert">{error}</p>}
        <p><Link to="/phone" target="_blank" rel="noreferrer">Open Phone codes</Link></p>
        <button className="ui-button" disabled={busy} onClick={onBack}>Back to sign in</button>
    </div>;
}
