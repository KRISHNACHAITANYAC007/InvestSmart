import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/api.js";

export default function PhoneSecurity() {
    const [phone, setPhone] = useState(null), [error, setError] = useState(""), [busy, setBusy] = useState(false), [pairingUrl, setPairingUrl] = useState("");
    useEffect(() => {
        const controller = new AbortController();
        api.get("/users/me/phone", { signal: controller.signal }).then(({ data }) => setPhone(data)).catch(failure => { if (!controller.signal.aborted) setError(failure.response?.data?.message || "Phone settings could not be loaded."); });
        return () => controller.abort();
    }, []);
    async function replace(event) {
        event.preventDefault(); if (busy) return;
        const form = event.currentTarget, data = new FormData(form); setBusy(true); setError("");
        try {
            const result = await api.post("/users/me/phone/replace", { password: data.get("password"), code: String(data.get("code") || "").trim() });
            form.reset(); setPairingUrl(result.data.pairingUrl);
            // Keep this restricted replacement screen visible; server sessions are already revoked.
        } catch (failure) { setError(failure.response?.data?.message || "Phone replacement could not be authorized."); }
        finally { setBusy(false); }
    }
    return <section className="account-panel"><h2>Phone login verification</h2>
        <p>Every new sign-in requires your password and phone verification. <Link to="/phone">Open Phone codes</Link> on your paired phone.</p>
        {phone && <p>{phone.enrolled ? `Paired ${new Date(phone.createdAt).toLocaleString()}` : "No completed phone pairing. Sign out and sign in to finish setup."}{phone.lastUsedAt && ` · Last used ${new Date(phone.lastUsedAt).toLocaleString()}`}</p>}
        <p className="account-muted">Replacing your phone revokes its old credential and all application sessions. Use a current phone code or one of your saved recovery codes. Phone verification cannot be disabled.</p>
        {pairingUrl ? <p className="account-notice">Old access is revoked. <a href={pairingUrl}>Open this private replacement link on your phone</a> within 10 minutes, save the new recovery codes, then sign in again.</p>
            : phone?.enrolled && <form onSubmit={replace}><label>Current password<input name="password" type="password" autoComplete="current-password" required maxLength={100} /></label><label>Phone or recovery code<input name="code" autoComplete="one-time-code" required maxLength={100} /></label><button className="ui-button" disabled={busy}>{busy ? "Please wait…" : "Revoke old phone and pair replacement"}</button></form>}
        {error && <p role="alert" className="account-error">{error}</p>}
    </section>;
}
