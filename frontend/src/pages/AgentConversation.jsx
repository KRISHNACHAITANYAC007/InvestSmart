import { useEffect, useRef, useState } from "react";
import api from "../api/api.js";
import { useAuth } from "../context/useAuth.js";
import "./AgentConversation.css";
import { agentMoney as money, createAgentRequestGate, appendAgentReply } from "../api/agentPresentation.js";

const questions = [["OVERVIEW", "Explain my equity holdings"], ["UNREALIZED_PNL", "Explain my unrealized P&L"], ["CONCENTRATION", "Where is my equity value concentrated?"]];

export function PortfolioReply({ analysis }) {
    return <article className="agent-reply">
        <h3>Equity snapshot</h3>
        <p>Read at {analysis.evidence.readAt}. {analysis.status.replaceAll("_", " ")}</p>
        <dl><dt>Equity value</dt><dd>{money(analysis.equityValue)}</dd><dt>Equity unrealized P&amp;L</dt><dd>{money(analysis.equityUnrealizedPnl)}</dd></dl>
        <ul>{analysis.explanation.map((line, index) => <li key={index}>{line}</li>)}</ul>
        {analysis.largestListings.length > 0 && <><h4>Largest listings within equities</h4><ul>{analysis.largestListings.map(row => <li key={row.instrumentKey}>{row.instrumentKey}: {row.equityWeightPercent}%</li>)}</ul></>}
        <details><summary>Holdings and quote observations</summary><ul>{analysis.evidence.holdings.map((row, index) => <li key={`${row.instrumentKey}:${index}`}>{row.symbol || row.instrumentKey} · {row.quantity} units · {money(row.value)} · Quote observed: {row.quoteAt || "Unavailable"} · {row.availability}</li>)}</ul></details>
    </article>;
}

function SessionConversation() {
    const [consent, setConsent] = useState(false), [messages, setMessages] = useState([]), [busy, setBusy] = useState(false), [error, setError] = useState("");
    const gate = useRef(createAgentRequestGate());
    useEffect(() => { const current = gate.current; return () => current.cancel(); }, []);
    function cancel() { gate.current.cancel(); setBusy(false); }
    function clear() { cancel(); setMessages([]); setError(""); setConsent(false); }
    async function ask(topic, label) {
        if (!consent || busy) return;
        const controller = gate.current.begin(); setBusy(true); setError("");
        try {
            const { data } = await api.post("/agent/portfolio-analysis", { topic, consent: true }, { signal: controller.signal, timeout: 15000 });
            if (gate.current.isCurrent(controller)) setMessages(old => appendAgentReply(old, { label, analysis: data.analysis }));
        } catch (failure) {
            if (gate.current.isCurrent(controller)) setError(failure.response?.data?.message || "Portfolio analysis is unavailable. Retry when the service is available.");
        } finally {
            if (gate.current.finish(controller)) setBusy(false);
        }
    }
    return <section className="agent-conversation" aria-labelledby="agent-title">
        <h1 id="agent-title">Portfolio companion</h1>
        <p>Local deterministic preview · No external model call. Educational paper simulation, not financial advice.</p>
        <p>These three questions use equity evidence only. Freeform conversation, whole-account analysis, daily causes and order actions are not available here.</p>
        <label><input type="checkbox" checked={consent} disabled={busy} onChange={e => setConsent(e.target.checked)} /> Allow this view to read my equity portfolio for these questions. No conversation is saved.</label>
        <div className="agent-questions">{questions.map(([topic, label]) => <button className="ui-button" key={topic} disabled={!consent || busy} onClick={() => ask(topic, label)}>{label}</button>)}</div>
        <div className="agent-questions"><button className="ui-button" disabled={!busy} onClick={cancel}>Cancel request</button><button className="ui-button" onClick={clear}>Clear conversation</button></div>
        {busy && <p role="status">Reading your authorized equity evidence…</p>}
        {error && <p role="alert">{error}</p>}
        <div role="log" aria-label="Portfolio conversation" aria-live="polite">{messages.map((message, index) => <div key={index}><h2>{message.label}</h2><PortfolioReply analysis={message.analysis} /></div>)}</div>
    </section>;
}
export default function AgentConversation() {
    const { user } = useAuth();
    return <SessionConversation key={user?.id ?? user?.email ?? "signed-out"} />;
}
