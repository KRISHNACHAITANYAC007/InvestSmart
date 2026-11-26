import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/api.js";
import "./OptionsDesk.css";
import PaperTicket from "./PaperTicket.jsx";

const money = value => value == null ? "Unavailable" : Number(value).toLocaleString("en-IN", { style: "currency", currency: "INR" });
const changed = () => window.dispatchEvent(new Event("investsmart-account-changed"));
export default function OptionsDesk({ trading = false }) {
    const [contracts, setContracts] = useState([]), [expiry, setExpiry] = useState("");
    const [chain, setChain] = useState([]), [account, setAccount] = useState(null);
    const [error, setError] = useState(""), [notice, setNotice] = useState(""), [loading, setLoading] = useState(false);
    const [revision, setRevision] = useState(0), [ticket, setTicket] = useState(null);
    const [busy, setBusy] = useState(false), [sideFilter, setSideFilter] = useState("ALL");
    const submitting = useRef(false);
    useEffect(() => {
        const c = new AbortController();
        const load = () => api.get("/options/account", { signal: c.signal }).then(({ data }) => { if (!c.signal.aborted) setAccount(data); }).catch(() => { if (!c.signal.aborted) setError("Option account could not be refreshed. Retry to load current reservations."); });
        load(); const timer = setInterval(load, 30000);
        return () => { c.abort(); clearInterval(timer); };
    }, [revision]);
    useEffect(() => {
        if (!trading) return;
        const c = new AbortController();
        api.get("/options/contracts", { signal: c.signal }).then(({ data }) => { if (!c.signal.aborted) { setContracts(data); setExpiry(value => data.some(contract => contract.expiry === value) ? value : data[0]?.expiry || ""); } }).catch(failure => { if (!c.signal.aborted) setError(failure.response?.data?.message || "Option contracts unavailable. Check provider access and retry."); });
        return () => c.abort();
    }, [trading, revision]);
    useEffect(() => {
        if (!trading || !expiry) return;
        const c = new AbortController();
        const load = async () => {
            setLoading(true);
            try { const { data } = await api.get("/options/chain", { params: { expiry }, signal: c.signal }); if (!c.signal.aborted) { setChain(data); setError(""); } }
            catch (failure) { if (!c.signal.aborted) { setChain([]); setError(failure.response?.data?.message || "Option chain unavailable. No executable prices are shown."); } }
            finally { if (!c.signal.aborted) setLoading(false); }
        };
        load(); const timer = setInterval(load, 30000);
        return () => { c.abort(); clearInterval(timer); };
    }, [trading, expiry, revision]);
    function select(quote, side) { setTicket({ quote, side }); setNotice(""); }
    async function cancel(id) {
        if (submitting.current) return; submitting.current = true; setBusy(true); setError("");
        try { await api.delete(`/options/orders/${encodeURIComponent(id)}`); setRevision(n => n + 1); changed(); setNotice("Pending order cancelled; reservation released."); }
        catch (failure) { setError(failure.response?.data?.message || "Order could not be cancelled. Refresh its status."); }
        finally { submitting.current = false; setBusy(false); }
    }
    const expiries = [...new Set(contracts.map(c => c.expiry))];
    return <section className="options-desk">
        <header className="options-heading"><div><h2>NIFTY 50 options</h2><p>CE = Call · PE = Put · Paper trading with virtual funds</p></div><button className="ui-button" disabled={busy || loading} onClick={() => setRevision(n => n + 1)}>Refresh options</button></header>
        <p className="options-disclosure">Buy options or sell owned contracts in whole lots. No naked writing or leverage. Premium × units is debited from available virtual cash. Quotes are provider snapshots; fills use available ask/bid depth, with no fees or slippage model.</p>
        <p className="options-disclosure">Limit orders and protective sell stop-market orders are polled in batches on a 30-second schedule while the server runs; provider delays/backlogs can delay checks, gaps can change stop fills, and triggers can be missed between checks. Orders remain pending until filled, cancelled or contract expiry. Expiry cash settlement uses the exact expiry-day NIFTY daily close after 18:00 IST as an educational proxy, not an official exchange settlement price; missing data leaves settlement pending.</p>
        {error && <p role="alert" className="options-error">{error}</p>}{notice && <p role="status">{notice}</p>}
        {account && <p><strong>Available virtual cash: {money(account.cash)}</strong> · <Link to="/portfolio?tab=funds">Manage funds</Link></p>}
        {!trading && <p><Link to="/market?tab=options">Open option chain and trade</Link></p>}
        {trading && <>
            <div className="options-filters"><label>Expiry<select value={expiry} disabled={busy} onChange={e => { setExpiry(e.target.value); setChain([]); setTicket(null); }}>{expiries.map(date => <option key={date}>{date}</option>)}</select></label><label>Contract type<select value={sideFilter} onChange={e => setSideFilter(e.target.value)}><option value="ALL">CE and PE</option><option>CE</option><option>PE</option></select></label></div>
            {!expiries.length && <p>No current contracts loaded. Refresh to retry provider discovery.</p>}
            {loading && <p role="status">Refreshing option chain…</p>}
            <div className="options-table-scroll" role="region" aria-label="NIFTY option chain" tabIndex={0}><table><caption>Provider option chain · prices per unit</caption><thead><tr><th>Strike</th><th>Type</th><th>Lot size</th><th>LTP</th><th>Bid</th><th>Ask</th><th>Open interest</th><th>Actions</th></tr></thead><tbody>{chain.filter(q => sideFilter === "ALL" || q.contract.type === sideFilter).map(q => <tr key={q.contract.instrumentKey}><td>{q.contract.strike}</td><td>{q.contract.type}</td><td>{q.contract.lotSize}</td><td>{money(q.ltp)}</td><td>{money(q.bid)}</td><td>{money(q.ask)}</td><td>{q.openInterest ?? "Unavailable"}</td><td className="options-actions"><button className="ui-button" disabled={busy || loading} onClick={() => select(q, "BUY")}>Buy</button><button className="ui-button" disabled={busy || loading} onClick={() => select(q, "SELL")}>Sell owned</button></td></tr>)}</tbody></table></div>
            {ticket && <PaperTicket optionQuote={ticket.quote} initialSide={ticket.side} onClose={() => setTicket(null)} onCompleted={() => { setRevision(n => n + 1); changed(); }} />}
        </>}
        {account?.totals&&<p>Option value (saved bids): {money(account.totals.value)} · Invested premium: {money(account.totals.cost)} · Reserved option cash: {money(account.totals.reserved)} · Realized P&amp;L: {money(account.totals.realized)}</p>}
        <h3>Option positions</h3>{!account ? <p>Loading option account…</p> : !account.positions.length ? <p>No open option positions.</p> : <div className="options-table-scroll" role="region" aria-label="Option positions" tabIndex={0}><table><thead><tr><th>Contract / expiry</th><th>Units</th><th>Cost</th><th>Saved premium</th><th>Quote retrieved</th></tr></thead><tbody>{account.positions.map(p => <tr key={`${p.instrument_key}:${p.expiry}`}><td>{p.symbol}<br />{String(p.expiry)}{new Date(`${String(p.expiry).slice(0, 10)}T15:30:00+05:30`) <= new Date() && <strong> · Expiry settlement pending</strong>}</td><td>{p.units}</td><td>{money(p.cost_amount)}</td><td>{money(p.last_price)}</td><td>{p.mark_at ? new Date(p.mark_at).toLocaleString() : "Unavailable"}</td></tr>)}</tbody></table></div>}
        <h3>Option orders</h3>{account && !account.orders.length ? <p>No option orders yet.</p> : <div className="options-table-scroll" role="region" aria-label="Option orders" tabIndex={0}><table><thead><tr><th>Contract</th><th>Side / type</th><th>Units</th><th>Limit / trigger</th><th>Fill</th><th>Reserved cash</th><th>Realized P&amp;L</th><th>Status</th></tr></thead><tbody>{account?.orders.map(o => <tr key={o.id}><td>{o.symbol}<br />{String(o.expiry)}</td><td>{o.side} · {o.order_kind}</td><td>{o.units}</td><td>{money(o.trigger_price)}</td><td>{money(o.fill_price)}</td><td>{money(o.reserved_cash)}</td><td>{money(o.realized_pnl)}</td><td>{o.status}{o.status === "PENDING" && <button className="ui-button" disabled={busy} onClick={() => cancel(o.id)}>Cancel</button>}</td></tr>)}</tbody></table></div>}
    </section>;
}
