import { useState } from "react";
import api from "../api/api.js";
import "./LearningChat.css";

const suggestions = [
    "Explain diversification in simple terms.",
    "What can concentration risk tell me about my holdings?",
    "Summarize recent news about an IPO or bond.",
];

export default function LearningChat() {
    const [question, setQuestion] = useState("");
    const [providerConsent, setProviderConsent] = useState(false);
    const [includePortfolio, setIncludePortfolio] = useState(false);
    const [answer, setAnswer] = useState(null);
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);

    async function ask(event) {
        event.preventDefault();
        if (busy) return;
        setBusy(true); setError(""); setAnswer(null);
        try {
            const { data } = await api.post("/learning/chat", { question: question.trim(), providerConsent, includePortfolio });
            setAnswer(data);
        } catch (failure) {
            setError(failure.response?.data?.message || "The learning assistant is unavailable. Please try again later.");
        } finally { setBusy(false); }
    }

    return <section className="learning-chat">
        <header className="learning-chat-header">
            <div><p className="learning-chat-eyebrow">InvestSmart learning</p><h1>Learning assistant</h1></div>
            <p className="learning-chat-disclaimer">Educational information only — not financial advice or a recommendation.</p>
        </header>
        <div className="learning-chat-panel">
            <p>Ask about investing concepts, available market data, IPOs, bonds, or portfolio concepts. Answers use only the evidence shown with them; missing or stale data is called out.</p>
            <div className="learning-chat-suggestions" aria-label="Suggested questions">
                {suggestions.map(value => <button key={value} type="button" className="ui-button" disabled={busy} onClick={() => setQuestion(value)}>{value}</button>)}
            </div>
            <form onSubmit={ask} className="learning-chat-form">
                <label htmlFor="learning-question">Your question</label>
                <textarea id="learning-question" value={question} onChange={event => setQuestion(event.target.value)} minLength={3} maxLength={1000} required rows={4} placeholder="Ask an educational question…" />
                <label className="learning-chat-check"><input type="checkbox" checked={providerConsent} onChange={event => setProviderConsent(event.target.checked)} required /> I understand my question will be sent to InvestSmart&apos;s configured AI provider to generate a response. Avoid sharing personal information.</label>
                <label className="learning-chat-check"><input type="checkbox" checked={includePortfolio} onChange={event => setIncludePortfolio(event.target.checked)} /> Include only relevant portfolio details in the request. This shares selected account financial data with the configured AI provider.</label>
                <button className="ui-button primary" disabled={busy || !providerConsent || question.trim().length < 3}>{busy ? "Preparing an educational answer…" : "Ask"}</button>
            </form>
            {error && <p className="learning-chat-error" role="alert">{error}</p>}
            {answer && <article className="learning-chat-answer" aria-live="polite">
                <h2>Answer</h2><p>{answer.answer}</p>
                {answer.portfolioIncluded && <p className="learning-chat-note">Relevant portfolio context was included at your request.</p>}
                <p className="learning-chat-disclaimer">{answer.disclaimer}</p>
                {answer.sources?.length > 0 && <div><h3>Sources and context</h3><ul>{answer.sources.map(source => <li key={source.id}><strong>[{source.id}] {source.title}</strong>{source.publishedAt && <span> · Published/updated {source.publishedAt}</span>}{source.url && <span> · <a href={source.url} target="_blank" rel="noreferrer">Open source</a></span>}</li>)}</ul></div>}
                {answer.sources?.length === 0 && <p className="learning-chat-note">No current external source was available for this answer.</p>}
            </article>}
        </div>
    </section>;
}
