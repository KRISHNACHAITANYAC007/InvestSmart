import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { chartPath } from "../api/marketInstruments.js";
import ChartStockSearch from "./ChartStockSearch.jsx";

const INDEX_NAMES = ["NIFTY 50", "SENSEX", "NIFTY BANK", "NIFTY IT"];
const number = value => Number(value) > 0 ? Number(value).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : "—";
const signed = value => value === null ? "—" : `${value >= 0 ? "+" : "−"}${Math.abs(value).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export default function GraphSidebar({ indices, watchlist, loading, refreshing, error, stale, refresh, symbol, instrumentKey, quote, companyName }) {
    const navigate = useNavigate();
    const location = useLocation();
    const [searchOpen, setSearchOpen] = useState(false);
    const searchDialog = useRef(null);
    useEffect(() => { if (searchOpen) searchDialog.current.showModal(); }, [searchOpen]);
    function rows(items) {
        return <table className="terminal-market-table"><thead><tr><th>Company / Symbol</th><th>Last</th><th>Change</th><th>Chg %</th></tr></thead>
            <tbody>{items.map(item => {
                const current = Number(item.currentPrice), previous = Number(item.previousClose);
                const change = current > 0 && previous > 0 ? current - previous : null;
                const active = instrumentKey ? instrumentKey === item.instrumentKey : item.symbol === symbol && !item.instrumentKey?.startsWith("BSE_EQ|");
                return <tr key={item.instrumentKey || item.symbol} className={active ? "active" : ""}>
                    <th><button aria-current={active ? "true" : undefined} title={item.companyName || item.symbol}
                        onClick={() => navigate(chartPath(item), { state: { companyName: item.companyName, returnTo: location.state?.returnTo || "/market" } })}>
                        <strong>{item.companyName || item.symbol}</strong>{item.companyName && <small>{item.symbol}</small>}</button></th>
                    <td>{number(current)}</td><td className={change === null ? "" : change >= 0 ? "terminal-positive" : "terminal-negative"}>{signed(change)}</td>
                    <td className={change === null ? "" : change >= 0 ? "terminal-positive" : "terminal-negative"}>{change === null ? "—" : `${signed(change / previous * 100)}%`}</td>
                </tr>;
            })}</tbody></table>;
    }
    const current = Number(quote?.currentPrice), previous = Number(quote?.previousClose);
    const change = current > 0 && previous > 0 ? current - previous : null;
    const low = Number(quote?.dayLow), high = Number(quote?.dayHigh);
    return <aside className="terminal-watchlist-panel" aria-label="Market watchlist and instrument details">
        <div className="terminal-panel-title"><div><small>YOUR MARKET WORKSPACE</small><h2>Watchlist</h2></div>
            <button className="terminal-add-stock" onClick={() => setSearchOpen(true)}>+ Add stock</button></div>
        <div className="terminal-sidebar-content">
            <h3 className="terminal-section-title">Indices <span>4</span></h3>
            {rows(INDEX_NAMES.map(name => indices.find(i => i.symbol === name) || { symbol: name }))}
            <div className="terminal-section-title"><h3>My watchlist <span>{watchlist.length}</span></h3><button onClick={refresh} disabled={refreshing} aria-label="Refresh watchlist">↻</button></div>
            {error && <p className="terminal-stale-note" role="alert">{error}</p>}
            {stale && <p className="terminal-stale-note" role="status">Some prices are stale. Showing saved values.</p>}
            {loading ? <p className="terminal-empty">Loading watchlist…</p> : watchlist.length ? rows(watchlist.slice(0, 3))
                : <p className="terminal-empty">Your watchlist starts here. Use + Add stock to find a company.</p>}
            <button className="terminal-view-all" onClick={() => navigate("/market?tab=watchlist")}>View all watchlist <span>↗</span></button>
            <section className="terminal-instrument-details" aria-label="Selected instrument details">
                <div className="terminal-detail-heading"><span className="terminal-company-avatar">{symbol.slice(0, 2)}</span><div><h2>{companyName || symbol}</h2><p>{symbol}</p></div></div>
                <div className="terminal-detail-price">{number(current)} <small>INR</small></div>
                <p className={change === null ? "terminal-detail-muted" : change >= 0 ? "terminal-positive" : "terminal-negative"}>{change === null ? "Daily change unavailable" : `${signed(change)} (${signed(change / previous * 100)}%)`}</p>
                <p className="terminal-detail-muted">{quote?.lastUpdated ? `Updated ${new Date(quote.lastUpdated).toLocaleString("en-IN")}` : "Update time unavailable"}</p>
                <dl><div><dt>Previous close</dt><dd>{number(previous)}</dd></div><div><dt>Day low</dt><dd>{number(low)}</dd></div><div><dt>Day high</dt><dd>{number(high)}</dd></div></dl>
                {low > 0 && high > low && current > 0 && <div className="terminal-day-range" aria-label={`Day range ${number(low)} to ${number(high)}`}><i style={{ left: `${Math.max(0, Math.min(100, (current - low) / (high - low) * 100))}%` }} /></div>}
            </section>
        </div>
        <button className="terminal-manage-watchlist" onClick={() => navigate("/heatmaps")}>Explore Heatmaps <span>↗</span></button>
        {searchOpen && <dialog ref={searchDialog} className="terminal-search-dialog" onCancel={() => setSearchOpen(false)}>
            <header><h2>Add stocks</h2><button onClick={() => setSearchOpen(false)} aria-label="Close stock search">×</button></header>
            <ChartStockSearch watchlist={watchlist} onNavigate={() => setSearchOpen(false)} />
        </dialog>}
    </aside>;
}
