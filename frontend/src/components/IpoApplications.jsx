import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/api.js";
import { money, pendingIpo, accountChanged } from "../api/investments.js";
import "./InvestmentMarket.css";
export function ApplicationSummary({ application: a }) {
    return <><div className="investment-facts"><div><small>Requested shares</small><strong>{a.appliedQuantity}</strong></div><div><small>Reserved cash</small><strong>{money(a.reservedAmount)}</strong></div><div><small>Allotted shares</small><strong>{a.allottedQuantity || 0}</strong></div><div><small>Allocated cost</small><strong>{money(a.allocatedCost)}</strong></div></div>
        <p>{a.reason}</p>{a.assumedIssuePrice && <p className="investment-notice">The published cut-off was unavailable. The upper price band was used for this simulation.</p>}
        <p className="investment-muted">Allotment: {a.allotmentDate || "Not announced"} · Listing: {a.listingDate || "Not announced"}{a.listingOrderId ? ` · IPO trade #${a.listingOrderId}` : ""}</p></>;
}
export default function IpoApplications() {
    const [applications, setApplications] = useState([]), [error, setError] = useState(""), [loading, setLoading] = useState(true), [revision, setRevision] = useState(0), [busy, setBusy] = useState(null);
    const guard = useRef(false);
    useEffect(() => { const controller = new AbortController(); async function load() { try { const { data } = await api.get("/investments/ipo-applications", { signal: controller.signal }); if (!controller.signal.aborted) { setApplications(data); setError(""); } } catch { if (!controller.signal.aborted) setError("Could not refresh IPO applications. Retry to see the latest status."); } finally { if (!controller.signal.aborted) setLoading(false); } } void load(); const changed = () => { void load(); }; window.addEventListener("investsmart-account-changed", changed); const timer = setInterval(changed, 60000); return () => { controller.abort(); clearInterval(timer); window.removeEventListener("investsmart-account-changed", changed); }; }, [revision]);
    async function cancel(id) { if (guard.current) return; guard.current = true; setBusy(id); try { await api.post(`/investments/ipo-applications/${id}/cancel`); accountChanged(); setRevision(n => n + 1); } catch (failure) { setError(failure.response?.data?.message || "Cancellation could not be confirmed. Refresh the status before retrying."); } finally { guard.current = false; setBusy(null); } }
    return <section className="investment-section"><div className="investment-section-heading"><div><h2>IPO applications</h2><p>Virtual cash only. Results are simulated after 18:00 IST on the published allotment date.</p></div><button onClick={() => setRevision(n => n + 1)}>Refresh</button></div>
        {error && <p role="alert" className="investment-notice">{error}</p>}{loading ? <p>Loading applications…</p> : !applications.length ? <p>No virtual applications yet. <Link to="/market?tab=ipos">Explore IPOs</Link></p> : applications.map(a => <article className="ipo-application" key={a.id}><div className="investment-section-heading"><div><h3><Link to={`/market?tab=ipos&ipo=${encodeURIComponent(a.ipoId)}`}>{a.name || a.symbol}</Link></h3><span className="investment-badge">{a.status.replaceAll("_", " ")}</span></div>{pendingIpo(a.status) && <button disabled={busy !== null} onClick={() => cancel(a.id)}>{busy === a.id ? "Cancelling…" : "Cancel & release cash"}</button>}</div><ApplicationSummary application={a} /></article>)}
    </section>;
}
