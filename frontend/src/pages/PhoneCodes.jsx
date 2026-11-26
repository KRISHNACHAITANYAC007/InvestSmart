import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/api.js";
import { createPlatformPasskey, getPlatformPasskey } from "../auth/passkeys.js";
import { phoneLinkToken, phoneSecondsRemaining, phoneStageAfterHide } from "../auth/phoneVerification.js";
import "./Account.css";
import "./PhoneCodes.css";

export default function PhoneCodes() {
    const [link, setLink] = useState(() => phoneLinkToken(window.location.hash));
    const [stage, setStage] = useState("closed"), [codes, setCodes] = useState(null);
    const [busy, setBusy] = useState(false), [error, setError] = useState("");
    const [sample, setSample] = useState(null), [now, setNow] = useState(() => Date.now());
    const generation = useRef(0);
    useEffect(() => { window.history.replaceState(null, "", "/phone"); const current=generation; return () => { current.current++; }; }, []);
    useEffect(() => {
        const hide = () => { if (document.hidden && ["display", "recovery"].includes(stage)) { generation.current++; setSample(null); setCodes(null); setStage(phoneStageAfterHide(stage)); if (stage === "display") api.post("/auth/phone/close").catch(() => {}); } };
        const pagehide = () => { generation.current++; setSample(null); setCodes(null); setStage(phoneStageAfterHide(stage)); };
        const pageshow = event => { if(event.persisted) pagehide(); };
        document.addEventListener("visibilitychange", hide); window.addEventListener("pagehide", pagehide); window.addEventListener("pageshow", pageshow);
        return () => { document.removeEventListener("visibilitychange", hide); window.removeEventListener("pagehide", pagehide); window.removeEventListener("pageshow", pageshow); };
    }, [stage]);
    useEffect(() => {
        if (stage !== "display") return undefined;
        let active = true, pending = false;
        const controller = new AbortController();
        let expiry = 0;
        const tick = async () => {
            setNow(Date.now());
            if (pending || Date.now() < expiry) return;
            pending = true;
            try {
                const { data } = await api.get("/auth/phone/code", { signal: controller.signal });
                if (!active || document.hidden) return;
                const receivedAt = Date.now(); setSample({ ...data, receivedAt });
                expiry = receivedAt + Math.max(0, Date.parse(data.expiresAt) - Date.parse(data.serverTime));
            } catch (failure) { if (active) { setSample(null); setStage("closed"); setError(failure.response?.data?.message || "Code access expired. Open with your phone biometric/PIN again."); } }
            finally { pending = false; }
        };
        tick(); const timer = setInterval(tick, 500);
        return () => { active = false; controller.abort(); clearInterval(timer); };
    }, [stage]);
    async function run(operation) {
        if (busy) return;
        const revision = generation.current;
        setBusy(true); setError("");
        try { await operation(() => revision === generation.current && !document.hidden); }
        catch (failure) { if (revision === generation.current) setError(failure.response?.data?.message || failure.message || "Phone verification could not be completed."); }
        finally { setBusy(false); }
    }
    function openLink() {
        run(async valid => {
            await api.post(link.kind === "pair" ? "/auth/phone/pair" : "/auth/phone/replacement", { token: link.token });
            if (valid()) { setLink(null); setStage("register"); }
        });
    }
    function register() {
        run(async valid => {
            const { data: ceremony } = await api.post("/auth/phone/registration/options");
            const credential = await createPlatformPasskey(ceremony.publicKey);
            const { data } = await api.post("/auth/phone/registration", { challenge: ceremony.challenge, credential: JSON.stringify(credential) });
            if (valid()) { setCodes(data.recoveryCodes); setStage("recovery"); }
        });
    }
    function open() {
        run(async valid => {
            const { data: ceremony } = await api.post("/auth/phone/display/options");
            const credential = await getPlatformPasskey(ceremony.publicKey);
            await api.post("/auth/phone/display", { challenge: ceremony.challenge, credential: JSON.stringify(credential) });
            if (valid()) setStage("display");
        });
    }
    function acknowledge() {
        setCodes(null); setStage("saved");
        run(async valid => { await api.post("/auth/phone/acknowledge"); if (valid()) setStage("display"); });
    }
    function close() {
        generation.current++; setSample(null); setCodes(null); setStage("closed"); api.post("/auth/phone/close").catch(() => {});
    }
    const seconds = sample ? phoneSecondsRemaining(sample.expiresAt, sample.serverTime, sample.receivedAt, now) : 0;
    return <main className="phone-page"><section className="account-panel phone-panel" aria-labelledby="phone-title"><header className="phone-heading"><Link className="phone-brand" to="/login">Invest<span>Smart</span></Link><span className="phone-eyebrow">PC sign-in verification</span><h1 id="phone-title">Phone codes</h1>
        <p className="account-muted">Use this paired phone to verify PC sign-in. Internet access is required. Its biometric/PIN unlocks code access for five minutes.</p></header>
        <div className="phone-stage">
        {link ? <><p>Confirm the pairing you requested after entering your password. This link expires after 10 minutes.</p><button className="ui-button primary" disabled={busy} onClick={openLink}>Confirm phone pairing</button></>
            : stage === "register" ? <><p>Register this phone’s passkey. Your phone may synchronize passkeys through its platform account.</p><button className="ui-button primary" disabled={busy} onClick={register}>Pair using phone biometric/PIN</button></>
                : stage === "recovery" ? <><h2>Save your recovery codes</h2><p>Each code works once. Keep them somewhere private outside this phone. They let you replace a lost phone.</p><div className="recovery-codes">{codes?.map(code => <code key={code}>{code}</code>)}</div><button className="ui-button primary" disabled={busy} onClick={acknowledge}>I have saved these codes</button></>
                    : stage === "saved" ? <><p>Your recovery codes have been cleared from this screen. If confirmation was interrupted, retry it; if already confirmed, open your phone codes.</p><button className="ui-button primary" disabled={busy} onClick={acknowledge}>Confirm codes saved</button><button className="ui-button" disabled={busy} onClick={open}>Open with phone biometric/PIN</button></>
                    : stage === "display" ? <><div className="phone-code-panel"><span className="phone-code-label">Your sign-in code</span><div className="phone-code" aria-label={seconds > 0 ? "Current six-digit phone code" : "Refreshing code"}>{seconds > 0 ? sample.code : "••••••"}</div><p role="status">{seconds > 0 ? `New code in ${seconds} seconds` : "Loading the next code…"}</p></div><p>Enter this code on your PC. It works once; if already used, wait for the next code.</p><button className="ui-button" onClick={close}>Hide and lock codes</button></>
                        : <button className="ui-button primary" disabled={busy} onClick={open}>Open with phone biometric/PIN</button>}
        </div>
        {busy && <p className="phone-busy" role="status">Follow your device prompt…</p>}{error && <p className="account-error" role="alert">{error}</p>}
        <footer className="phone-footer"><p className="account-muted">First use: sign in on your PC and open the pairing email on your phone. Lost phone: choose recovery during PC sign-in.</p><Link className="phone-back-link" to="/login">Back to sign in</Link></footer>
    </section></main>;
}
