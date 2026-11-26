import { useEffect, useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { chartPath, companyId, groupCompanies } from "../api/marketInstruments.js";
import api from "../api/api.js";
import AddToWatchlist from "./AddToWatchlist.jsx";
import "./ChartStockSearch.css";

export default function ChartStockSearch({ onAdded, onNavigate }) {
    const location = useLocation();
    const returnTo = location.pathname.startsWith("/market/chart/") ? location.state?.returnTo || "/market" : location.pathname + location.search;
    const [query, setQuery] = useState("");
    const [page, setPage] = useState(1);
    const [result, setResult] = useState(null);
    const [retry, setRetry] = useState(0);
    const resultsRef = useRef(null);
    const inputRef = useRef(null);
    const normalizedQuery = query.trim();
    const searchKey = `${normalizedQuery}:${page}:${retry}`;
    const searching = normalizedQuery.length >= 2 && result?.key !== searchKey;
    const current = result?.key === searchKey ? result : null;

    useEffect(() => {
        if (normalizedQuery.length < 2) return undefined;
        const controller = new AbortController();
        const timer = window.setTimeout(async () => {
            const [catalog, market] = await Promise.allSettled([
                api.get("/stocks", { signal: controller.signal }),
                api.get("/market/instruments", {
                    params: { query: normalizedQuery, page }, signal: controller.signal,
                }),
            ]);
            if (controller.signal.aborted) return;
            const filter = normalizedQuery.toLowerCase();
            const localItems = page === 1 && catalog.status === "fulfilled" && Array.isArray(catalog.value.data)
                ? catalog.value.data.filter(stock =>
                    `${stock.symbol} ${stock.companyName || ""}`.toLowerCase().includes(filter))
                : [];
            const marketData = market.status === "fulfilled" ? market.value.data : null;
            const remoteItems = Array.isArray(marketData?.instruments) ? marketData.instruments : [];
            const merged = new Map();
            for (const item of [...localItems, ...remoteItems]) {
                if (!item.symbol) continue;
                const symbol = item.symbol.toUpperCase();
                const key = item.instrumentKey || `local:${item.id}`;
                merged.set(key, { ...merged.get(key), ...item, symbol });
            }
            setResult({
                key: searchKey,
                items: groupCompanies([...merged.values()]),
                totalPages: Math.max(1, Number(marketData?.totalPages) || 1),
                error: market.status === "rejected" || !Array.isArray(marketData?.instruments)
                    ? localItems.length
                        ? "Market search unavailable. Showing matching saved stocks."
                        : "Stock search is unavailable. Please retry."
                    : "",
            });
        }, 300);
        return () => { controller.abort(); window.clearTimeout(timer); };
    }, [normalizedQuery, page, searchKey]);

    function clearSearch() {
        setQuery(""); setPage(1); setResult(null); inputRef.current?.focus();
    }

    function moveFocus(event) {
        if (event.key === "Escape") { clearSearch(); return; }
        if (event.key !== "ArrowDown" && event.key !== "ArrowUp") return;
        const controls = [...(resultsRef.current?.querySelectorAll("a, button:not(:disabled)") || [])];
        if (!controls.length) return;
        event.preventDefault();
        const index = controls.indexOf(document.activeElement);
        const next = index + (event.key === "ArrowDown" ? 1 : -1);
        if (next < 0) inputRef.current?.focus();
        else controls[Math.min(next, controls.length - 1)].focus();
    }

    return <section className="chart-stock-search" aria-labelledby="chart-stock-search-title" onKeyDown={moveFocus}>
        <label id="chart-stock-search-title" htmlFor="chart-stock-query">Search stocks</label>
        <div className="chart-stock-search-input">
            <input ref={inputRef} id="chart-stock-query" type="search" maxLength={50}
                value={query} placeholder="Company name or ticker"
                aria-describedby="chart-stock-search-status"
                onChange={event => { setQuery(event.target.value); setPage(1); }} />
            {query && <button className="chart-search-clear" type="button" onClick={clearSearch} aria-label="Clear stock search">Clear</button>}
        </div>
        <p id="chart-stock-search-status" className="chart-search-status" role="status">
            {normalizedQuery.length < 2 ? "Enter at least 2 characters to find stocks."
                : searching ? "Searching stocks..."
                    : current?.error || (current?.items.length ? `${current.items.length} matching stocks` : "No matching stocks found.")}
        </p>
        {current?.error && <button type="button" className="chart-search-retry" onClick={() => setRetry(value => value + 1)}>Retry search</button>}
        {current?.items.length > 0 && <ul ref={resultsRef} className="chart-search-results" aria-label="Stock search results">
            {current.items.map(item => <li className="chart-search-result" key={companyId(item)}>
                <div className="chart-search-name"><strong>{item.companyName || item.symbol}</strong><small>{item.symbol}</small></div>
                <div className="chart-search-actions">
                    <Link to={chartPath(item)} onClick={onNavigate}
                        state={{ companyName: item.companyName, returnTo }} aria-label={`Open ${item.symbol} chart`}>Open chart</Link>
                    <AddToWatchlist symbol={item.symbol} instrumentKey={item.instrumentKey} isin={item.isin} onAdded={onAdded} />
                </div>
            </li>)}
        </ul>}
        {current && current.totalPages > 1 && <div className="chart-search-pagination">
            <button type="button" disabled={page <= 1} onClick={() => setPage(value => value - 1)}>Previous</button>
            <span>Page {page} of {current.totalPages}</span>
            <button type="button" disabled={page >= current.totalPages} onClick={() => setPage(value => value + 1)}>Next</button>
        </div>}
    </section>;
}
