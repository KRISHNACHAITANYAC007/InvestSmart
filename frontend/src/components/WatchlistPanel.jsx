import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/api.js";
import { createGraphWatchlist } from "../api/graphWatchlist.js";
import { chartPath, notifyWatchlistChanged } from "../api/marketInstruments.js";
import ChartStockSearch from "./ChartStockSearch.jsx";
import AddToWatchlist from "./AddToWatchlist.jsx";
import "./WatchlistPanel.css";

const money = value => Number(value) > 0 ? Number(value).toLocaleString("en-IN", { style:"currency", currency:"INR" }) : "Unavailable";
export default function WatchlistPanel() {
    const [state, setState] = useState({ items:[], loading:true, refreshing:false, listError:"", pricesStale:false });
    const [showAdd, setShowAdd] = useState(false);
    const [error, setError] = useState("");
    const loaderRef = useRef(null);
    useEffect(() => {
        const loader = createGraphWatchlist(api, setState); loaderRef.current = loader;
        const refresh = () => { void loader.refresh(false); };
        refresh(); window.addEventListener("investsmart-watchlist-changed", refresh);
        const timer = setInterval(() => { if (!document.hidden) refresh(); }, 60000);
        return () => { loader.dispose(); clearInterval(timer); window.removeEventListener("investsmart-watchlist-changed", refresh); };
    }, []);
    async function remove(stockId) {
        try { await api.delete(`/watchlist/me/${stockId}`); notifyWatchlistChanged(); setError(""); }
        catch { setError("Could not remove this stock. Please retry."); }
    }
    return <div className="watchlist-page">
        <div className="watchlist-header"><div><h2>My watchlist</h2><p>Your companies, at a glance.</p></div><div className="watchlist-header-actions">
            <button className="watchlist-add-button" onClick={() => setShowAdd(v => !v)}>{showAdd ? "Close search" : "+ Add stocks"}</button>
            <button className="watchlist-refresh-button" disabled={state.refreshing} onClick={() => loaderRef.current.refresh(true)}>{state.refreshing ? "Refreshing…" : "Refresh prices"}</button>
        </div></div>
        {showAdd && <ChartStockSearch watchlist={state.items} />}
        {(error || state.listError) && <p role="alert" className="market-data-status is-stale">{error || state.listError}</p>}
        {state.pricesStale && <p role="status" className="market-data-status is-stale">Some prices are stale or unavailable. Showing saved values.</p>}
        {state.loading ? <p>Loading watchlist…</p> : !state.items.length ? <p>Your watchlist is empty. Add a company to get started.</p> : <div className="watchlist-list">
            {state.items.map(item => {
                const price = Number(item.currentPrice), close = Number(item.previousClose);
                const change = price > 0 && close > 0 ? price - close : null;
                return <article className="watchlist-item" key={item.id}>
                    <header className="watchlist-item-heading">
                        <Link className="watchlist-chart-link" to={chartPath(item)} state={{ companyName:item.companyName, returnTo:"/market?tab=watchlist" }}>
                            <span className="watchlist-symbol">{item.symbol}</span><span className="watchlist-company">{item.companyName}</span>
                        </Link>
                        <span className={`watchlist-item-move${change === null ? " unavailable" : change >= 0 ? " positive" : " negative"}`}>
                            {change === null ? "Change unavailable" : `${change >= 0 ? "+" : "−"}${money(Math.abs(change))} (${(change / close * 100 >= 0 ? "+" : "")}${(change / close * 100).toFixed(2)}%)`}
                        </span>
                    </header>
                    <div className="watchlist-item-quote"><span>Last price</span><strong>{money(price)}</strong></div>
                    <dl className="watchlist-item-details">
                        <div><dt>Previous close</dt><dd>{money(close)}</dd></div>
                        <div><dt>Day low</dt><dd>{money(item.dayLow)}</dd></div>
                        <div><dt>Day high</dt><dd>{money(item.dayHigh)}</dd></div>
                    </dl>
                    <footer className="watchlist-item-actions">
                        <AddToWatchlist symbol={item.symbol} instrumentKey={item.instrumentKey} isin={item.isin} />
                        <button className="watchlist-remove-button" onClick={() => remove(item.stockId)}>Remove</button>
                    </footer>
                </article>;
            })}
        </div>}
    </div>;
}
