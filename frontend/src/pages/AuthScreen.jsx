import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import api from "../api/api.js";
import { useAuth } from "../context/useAuth.js";
import { getPlatformPasskey, platformAuthenticatorAvailable } from "../auth/passkeys.js";
import { authSubmission } from "../auth/authSubmission.js";
import PhoneLoginStep from "../components/PhoneLoginStep.jsx";
import { isPhoneStep } from "../auth/phoneVerification.js";
import { acknowledgmentNeeded, canBeginAuthentication } from "../legal/content.js";
import { LegalLinks } from "../components/LegalLinks.jsx";
import "./PhoneCodes.css";
import "./Account.css";
import "./Login.css";

const titles = { login: "Login", register: "Create your account", "verify-email": "Verify your email", "forgot-password": "Reset your password", "reset-password": "Choose a new password", "resend-verification": "Resend verification" };
function PasswordVisibilityIcon({ visible }) {
    return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z" />
        <circle cx="12" cy="12" r="3" />
        {!visible && <path d="m3 3 18 18" />}
    </svg>;
}
export default function AuthScreen({ mode = "login" }) {
    const { login } = useAuth(), navigate = useNavigate(), location = useLocation(), [params] = useSearchParams();
    const [email, setEmail] = useState(""), [name, setName] = useState(""), [password, setPassword] = useState(""), [confirmation, setConfirmation] = useState(""), [code, setCode] = useState("");
    const [passwordVisible, setPasswordVisible] = useState(false), [confirmationVisible, setConfirmationVisible] = useState(false), [lockAvailable, setLockAvailable] = useState(false), [passkeySupported, setPasskeySupported] = useState(false);
    const [step, setStep] = useState(null), [error, setError] = useState(""), [message, setMessage] = useState(() => location.state?.notice || ""), [busy, setBusy] = useState(false), [codes, setCodes] = useState(null);
    const [acknowledged, setAcknowledged] = useState(false);
    const emailRequired = ["login", "register", "forgot-password", "resend-verification"].includes(mode);
    const passwordRequired = ["login", "register", "reset-password"].includes(mode);
    useEffect(() => {
        if (mode !== "login") return undefined;
        const controller = new AbortController();
        api.get("/auth/lock-status", { signal: controller.signal }).then(({ data }) => {
            if (!controller.signal.aborted) setLockAvailable(Boolean(data.locked && data.canUnlock));
        }).catch(() => { if (!controller.signal.aborted) setLockAvailable(false); });
        platformAuthenticatorAvailable().then(available => { if (!controller.signal.aborted) setPasskeySupported(available); }).catch(() => { if (!controller.signal.aborted) setPasskeySupported(false); });
        return () => controller.abort();
    }, [mode]);
    async function unlockWithPasskey() {
        if (busy) return;
        if (!canBeginAuthentication(mode, null, acknowledged)) { setError("Please read and acknowledge the terms and disclaimers before continuing."); return; }
        setBusy(true); setError(""); setMessage("");
        try {
            const { data: ceremony } = await api.post("/auth/unlock/options");
            const credential = await getPlatformPasskey(ceremony.publicKey);
            const { data } = await api.post("/auth/unlock", { challenge: ceremony.challenge, credential: JSON.stringify(credential) });
            login(data);
            navigate("/dashboard", { replace: true });
        } catch (failure) { setError(failure.response?.data?.message || failure.message || "Passkey unlock failed. Sign in with email and password instead."); }
        finally { setBusy(false); }
    }
    async function submit(event) {
        event.preventDefault(); if (busy) return;
        if (!canBeginAuthentication(mode, step, acknowledged)) { setError("Please read and acknowledge the terms and disclaimers before continuing."); return; }
        const form = new FormData(event.currentTarget); setBusy(true); setError(""); setMessage("");
        try {
            if (["register", "reset-password"].includes(mode) && form.get("password") !== form.get("confirmation")) { setError("Passwords do not match."); return; }
            const path = step ? "mfa" : mode;
            const body = authSubmission(mode, form, params.get("token"), step?.challenge);
            if (typeof body.email === "string") setEmail(body.email);
            const { data } = await api.post(`/auth/${path}`, body);
            if (mode === "verify-email") {
                navigate("/login", { replace: true, state: { notice: data.message || "Email verified. You can now sign in." } });
                return;
            }
            if (mode === "login") {
                if (data.state === "SIGNED_IN") {
                    login(data.user); setPassword(""); setCode(""); setStep(null);
                    if (data.recoveryCodes) setCodes(data.recoveryCodes);
                    else navigate(data.user.role === "ADMIN" ? "/admin" : "/dashboard");
                }
                else if (data.state === "VERIFY_EMAIL") setMessage("This account is not verified. Sign-in is blocked until you open a valid verification link. Resend verification to request a new 24-hour link.");
                else if (data.state === "PASSWORD_RESET_REQUIRED") { setPassword(""); setMessage("An administrator requires a password reset before you can sign in. Use the reset link sent to your email, or select Forgot password to request a fresh link."); }
                else { setStep(data); setPassword(""); }
            } else {
                setMessage(data.message); setPassword(""); setConfirmation("");
                if (["register", "resend-verification", "forgot-password"].includes(mode)) {
                    try { const status = await api.get("/auth/mail-status"); setMessage(`${data.message} ${status.data.message}`); }
                    catch { setMessage(`${data.message} Email delivery status could not be checked. Your account remains unverified until you use a valid verification link; try resending later or contact support.`); }
                }
            }
        } catch (failure) { setError(failure.response?.data?.message || "The request could not be completed. Please retry."); }
        finally { setBusy(false); }
    }
    const loginPage = mode === "login";
    return <main className={`auth-page${loginPage ? " login-page" : ""}`}><aside className={loginPage ? "login-brand" : "auth-brand-wrap"}><Link to="/login" className="auth-brand">InvestSmart</Link><Link className="creator-signature" to="/about">by KC</Link><p className="auth-brand-tagline">{loginPage ? "Learn. Invest. Grow." : "Practice investing with confidence"}</p></aside><div className={loginPage ? "login-section" : "auth-content"}><section className={`auth-panel${loginPage ? " login-box" : ""}`}>
        <h1>{codes ? "Save your recovery codes" : isPhoneStep(step) ? "Phone verification" : step ? "Admin verification" : titles[mode]}</h1>
        <p className="account-muted">{codes ? "Each code works once. Store these somewhere private before continuing." : isPhoneStep(step) ? "A second verification is required before your workspace opens." : step ? "Use your authenticator app or a saved recovery code." : loginPage ? "Sign in with your email and password. Your browser may offer to save or autofill your password." : "An educational workspace. All trading uses virtual funds."}</p>
        {codes ? <><div className="recovery-codes">{codes.map(value => <code key={value}>{value}</code>)}</div><button className="ui-button primary" onClick={() => { setCodes(null); navigate("/admin"); }}>I have saved these codes</button></> : isPhoneStep(step) ? <PhoneLoginStep step={step} onBack={() => { setStep(null); setCode(""); }} onSignedIn={data => { login(data.user); setStep(null); navigate("/dashboard"); }} /> : <form onSubmit={submit}>
            {loginPage && lockAvailable && passkeySupported && !step && <button className="ui-button" type="button" disabled={busy || !acknowledged} onClick={unlockWithPasskey}>{busy ? "Please wait…" : "Unlock with device passkey"}</button>}
            {loginPage && lockAvailable && <p className="account-muted">Unlock keeps the current session on this device. Email and password sign-in remains available.</p>}
            {step?.secret && <aside className="account-notice"><p>Add a time-based account in your authenticator app using this setup key:</p><code className="setup-secret">{step.secret}</code><p>Account: {email} · Issuer: InvestSmart · 6 digits · 30 seconds</p></aside>}
            {!step && mode === "register" && <label>Full name<input name="name" autoComplete="name" value={name} minLength={2} maxLength={50} required onChange={e => setName(e.target.value)} /></label>}
            {!step && emailRequired && <label>Email address<input name="email" type="email" autoComplete="email" placeholder={loginPage ? "Enter your email" : undefined} value={email} maxLength={254} required onChange={e => setEmail(e.target.value)} /></label>}
            {!step && passwordRequired && <label>Password<div className="auth-password-field"><input name="password" type={passwordVisible ? "text" : "password"} autoComplete={mode === "login" ? "current-password" : "new-password"} placeholder={loginPage ? "Enter your password" : undefined} value={password} minLength={mode === "login" ? 1 : 15} maxLength={64} required onChange={e => setPassword(e.target.value)} /><button className="auth-password-toggle" type="button" aria-label={passwordVisible ? "Hide password" : "Show password"} aria-pressed={passwordVisible} onClick={() => setPasswordVisible(value => !value)}><PasswordVisibilityIcon visible={passwordVisible} /></button></div>{mode !== "login" && <small>Use 15–64 characters. A memorable passphrase works well.</small>}</label>}
            {!step && ["register", "reset-password"].includes(mode) && <label>Confirm password<div className="auth-password-field"><input name="confirmation" type={confirmationVisible ? "text" : "password"} autoComplete="new-password" value={confirmation} required maxLength={64} onChange={e => setConfirmation(e.target.value)} /><button className="auth-password-toggle" type="button" aria-label={confirmationVisible ? "Hide password" : "Show password"} aria-pressed={confirmationVisible} onClick={() => setConfirmationVisible(value => !value)}><PasswordVisibilityIcon visible={confirmationVisible} /></button></div></label>}
            {step && <label>Authenticator or recovery code<input name="code" autoComplete="one-time-code" value={code} required maxLength={100} onChange={e => setCode(e.target.value.trim())} /></label>}
            {error && <p className="account-error" role="alert">{error}</p>}{message && <p className="account-notice" role="status">{message}</p>}
            {acknowledgmentNeeded(mode, step) && <label className="legal-acknowledgment"><input type="checkbox" checked={acknowledged} required onChange={event => setAcknowledged(event.target.checked)} /><span>I agree to the <Link to="/legal/terms" target="_blank" rel="noopener noreferrer">Terms</Link> and acknowledge the <Link to="/legal/privacy" target="_blank" rel="noopener noreferrer">Privacy</Link>, <Link to="/legal/paper-trading" target="_blank" rel="noopener noreferrer">Simulation Risks</Link> and <Link to="/legal/market-data" target="_blank" rel="noopener noreferrer">Data Disclaimer</Link>.</span></label>}
            <button className="ui-button primary" disabled={busy || !canBeginAuthentication(mode, step, acknowledged) || (mode === "verify-email" && !params.get("token"))}>{busy ? "Please wait…" : step ? "Verify and sign in" : mode === "login" ? "Sign in" : mode === "register" ? "Create account" : mode === "verify-email" ? "Verify email" : mode === "reset-password" ? "Update password" : "Send email"}</button>
            {step && <button className="ui-button" type="button" onClick={() => { setStep(null); setCode(""); }}>Back to sign in</button>}

        </form>}
        {!codes && <>{loginPage && <p className="login-register">Don&apos;t have an account? <Link to="/register">Register</Link></p>}<nav className="auth-links" aria-label="Account help">{!loginPage && <><Link to="/login">Sign in</Link><Link to="/register">Create account</Link></>}<Link to="/phone">Phone codes</Link><Link to="/forgot-password">Forgot password?</Link><Link to="/resend-verification">Resend verification</Link></nav></>}
        <details className="auth-legal-details"><summary>Legal information</summary><LegalLinks /></details>
    </section><p className="auth-footer">Learn with market data. No real orders or payments.</p></div></main>;
}
