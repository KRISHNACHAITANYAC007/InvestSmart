import { useEffect, useState } from "react";
import api from "../api/api.js";
import { useAuth } from "../context/useAuth.js";
import { createPlatformPasskey, platformAuthenticatorAvailable } from "../auth/passkeys.js";
import PhoneSecurity from "../components/PhoneSecurity.jsx";
import "./Account.css";
export default function Account() {
    const { user } = useAuth();
    const [passkeyCooldown,setPasskeyCooldown]=useState(0);
    useEffect(()=>{if(passkeyCooldown<=0)return;const timer=setTimeout(()=>setPasskeyCooldown(n=>Math.max(0,n-1)),1000);return()=>clearTimeout(timer);},[passkeyCooldown]);
    const [sessions, setSessions] = useState(null), [passkeys, setPasskeys] = useState(null), [platformAvailable, setPlatformAvailable] = useState(false);
    const [error, setError] = useState(""), [message, setMessage] = useState(""), [busy, setBusy] = useState(false), [revision, setRevision] = useState(0);
    useEffect(() => {
        const c = new AbortController();
        api.get("/users/me/sessions", { signal: c.signal }).then(({ data }) => { if (!c.signal.aborted) setSessions(data); }).catch(() => { if (!c.signal.aborted) setError("Sessions could not be loaded."); });
        api.get("/auth/passkeys", { signal: c.signal }).then(({ data }) => { if (!c.signal.aborted) setPasskeys(data); }).catch(() => { if (!c.signal.aborted) setError("Trusted-device settings could not be loaded."); });
        platformAuthenticatorAvailable().then(available => { if (!c.signal.aborted) setPlatformAvailable(available); }).catch(() => { if (!c.signal.aborted) setPlatformAvailable(false); });
        return () => c.abort();
    }, [revision]);
    async function revoke(id) { if (busy) return; setBusy(true); setError(""); try { if (id) await api.delete(`/users/me/sessions/${encodeURIComponent(id)}`); else await api.post("/users/me/sessions/revoke-others"); setRevision(n => n + 1); setMessage("Other session access has been revoked."); } catch (failure) { setError(failure.response?.data?.message || "Sessions could not be updated."); } finally { setBusy(false); } }
    async function addPasskey() {
        if (busy || passkeyCooldown>0) return;
        if(window.location.protocol === "http:" && ["127.0.0.1","[::1]"].includes(window.location.hostname)){
            setError("Use http://localhost:5173/account and sign in there before local passkey setup. No setup request was sent.");return;
        }
        setBusy(true); setError(""); setMessage("");
        try {
            const { data: ceremony } = await api.post("/auth/passkeys/registration/options");
            const credential = await createPlatformPasskey(ceremony.publicKey);
            await api.post("/auth/passkeys/registration", { challenge: ceremony.challenge, credential: JSON.stringify(credential) });
            setRevision(n => n + 1);
            setMessage("Trusted-device unlock is ready. Locking keeps this session; signing out revokes it.");
        } catch (failure) { if(failure.response?.status===429)setPasskeyCooldown(Math.min(900,Math.max(1,Number(failure.response.headers?.["retry-after"])||900)));setError(failure.response?.data?.message || failure.message || "Passkey setup could not be completed."); }
        finally { setBusy(false); }
    }
    async function removePasskey(id) {
        if (busy) return;
        setBusy(true); setError(""); setMessage("");
        try { await api.delete(`/auth/passkeys/${encodeURIComponent(id)}`); setRevision(n => n + 1); setMessage("Trusted-device passkey removed."); }
        catch (failure) { setError(failure.response?.data?.message || "Passkey could not be removed."); }
        finally { setBusy(false); }
    }
    async function lockApp() {
        if (busy) return;
        setBusy(true); setError(""); setMessage("");
        try { await api.post("/auth/lock"); window.dispatchEvent(new Event("investsmart-session-ended")); }
        catch (failure) { setError(failure.response?.data?.message || "The app could not be locked."); }
        finally { setBusy(false); }
    }
    async function password(event) { event.preventDefault(); if (busy) return; const form = event.currentTarget, data = new FormData(form); if (data.get("newPassword") !== data.get("confirm")) { setError("Passwords do not match."); return; } setBusy(true); setError(""); try { await api.post("/users/me/password", { currentPassword: data.get("currentPassword"), newPassword: data.get("newPassword") }); form.reset(); window.dispatchEvent(new Event("investsmart-session-ended")); } catch (failure) { setError(failure.response?.data?.message || "Password could not be changed."); } finally { setBusy(false); } }
    return <div className="account-page"><h1>Account & security</h1><p className="account-muted">Manage your sign-in and active sessions.</p>{error && <p role="alert" className="account-error">{error}</p>}{message && <p role="status" className="account-notice">{message}</p>}
        <section className="account-panel"><h2>Your account</h2><p>{user.name} · {user.email}</p><p className="account-muted">Verified email · {user.role === "ADMIN" ? "Owner administrator · Authenticator enabled" : "Personal account"}</p></section>
        <section className="account-panel"><h2>Sign-in method</h2><p className="account-muted">{user.role === "ADMIN" ? "Administrator sign-in requires your email, password and authenticator verification." : "Sign in with your email, password and six-digit phone code. On your paired phone, its biometric/PIN can complete the second step."}</p><p className="account-muted"><strong>Lock</strong> keeps this session active on this device and allows an enrolled passkey to unlock it. <strong>Sign out</strong> revokes the session and requires full sign-in again.</p>
            {user.role !== "ADMIN" && <><h3>Trusted-device unlock</h3><p className="account-muted">A passkey asks your device to verify you with its supported screen lock or biometrics. InvestSmart never receives fingerprint or face data. Full password and phone verification remain available.</p>
                {passkeys === null ? <p className="account-muted">Loading trusted-device settings…</p> : <>{passkeys.length > 0 && <div className="account-passkeys">{passkeys.map(key => <div className="account-session" key={key.id}><div><strong>Platform passkey</strong><small>Added {new Date(key.createdAt).toLocaleString()}{key.lastUsedAt ? ` · Last used ${new Date(key.lastUsedAt).toLocaleString()}` : ""}</small></div><button className="ui-button" disabled={busy} onClick={() => removePasskey(key.id)}>Remove</button></div>)}</div>}
                    {platformAvailable ? <button className="ui-button" disabled={busy || passkeyCooldown>0} onClick={addPasskey}>{passkeyCooldown>0 ? `Retry in ${passkeyCooldown}s` : busy ? "Please wait…" : passkeys.length ? "Add another passkey" : "Set up passkey unlock"}</button> : <p className="account-muted">Platform passkey unlock is not supported in this browser or secure context. Use full sign-in with password and phone verification.</p>}
                    {passkeys.length > 0 && <button className="ui-button" disabled={busy} onClick={lockApp}>Lock this app</button>}
                </>}
            </>}
        </section>
        <section className="account-panel"><div className="admin-detail-heading"><h2>Active sessions</h2><button className="ui-button" disabled={busy} onClick={() => revoke(null)}>Sign out other sessions</button></div>{!sessions ? <p className="account-muted">Loading sessions…</p> : sessions.map(row => <div key={row.id} className="account-session"><div><strong>{row.current ? "This session" : row.device || "Browser session"}</strong><small>{row.source} · Signed in {new Date(row.createdAt).toLocaleString()} · Expires {new Date(row.expiresAt).toLocaleTimeString()}</small></div>{!row.current && <button className="ui-button" disabled={busy} onClick={() => revoke(row.id)}>Sign out</button>}</div>)}</section>
        {user.role !== "ADMIN" && <PhoneSecurity />}
        <section className="account-panel"><h2>Change password</h2><p className="account-muted">Changing your password signs out every session, including this one.</p><form onSubmit={password}><label>Current password<input name="currentPassword" type="password" autoComplete="current-password" required maxLength={100} /></label><label>New password<input name="newPassword" type="password" autoComplete="new-password" required minLength={15} maxLength={64} /></label><label>Confirm new password<input name="confirm" type="password" autoComplete="new-password" required maxLength={64} /></label><button className="ui-button primary" disabled={busy}>{busy ? "Updating…" : "Update password"}</button></form></section>
        <section className="account-panel" aria-labelledby="account-legal-title"><h2 id="account-legal-title">Legal information</h2><SimulationNotice /><LegalLinks /></section>
    </div>;
}
import { LegalLinks, SimulationNotice } from "../components/LegalLinks.jsx";
