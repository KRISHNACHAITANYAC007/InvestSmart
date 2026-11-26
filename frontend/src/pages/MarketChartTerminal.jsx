import { useCallback, useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import api from "../api/api.js";
import { chartViewPath, chartPeriod, CHART_PERIODS } from "../api/chartNavigation.js";
import { createMarketPoller } from "../api/marketPolling.js";
import { createGraphWatchlist } from "../api/graphWatchlist.js";
import { useAuth } from "../context/useAuth.js";
import { isIndexInstrument } from "../api/marketInstruments.js";
import MarketCandlestickChart from "../components/MarketCandlestickChart.jsx";
import TradeControls from "../components/TradeControls.jsx";
import AddToWatchlist from "../components/AddToWatchlist.jsx";
import GraphSidebar from "../components/GraphSidebar.jsx";
import "./MarketChartTerminal.css";

const PERIODS = CHART_PERIODS;
const REFRESH_INTERVAL = 60000;

const money = (value, digits = 2) => Number(value).toLocaleString("en-IN", {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
});

function MarketChartTerminal({ fullChart = false }) {
    const { symbol = "" } = useParams();
    const normalizedSymbol = decodeURIComponent(symbol).toUpperCase();

    const navigate = useNavigate();
    const location = useLocation();
    const instrumentKey = new URLSearchParams(location.search).get("instrumentKey") || "";
    const quoteKey = instrumentKey || normalizedSymbol;
    const [theme, setTheme] = useState(() => { try { return localStorage.getItem("investsmart-chart-theme") === "light" ? "light" : "dark"; } catch { return "dark"; } });
    useEffect(() => { try { localStorage.setItem("investsmart-chart-theme", theme); } catch { /* Storage may be disabled. */ } }, [theme]);
    const { logout } = useAuth();
    const [logoutError, setLogoutError] = useState("");
    const workspaceRef = useRef(null);
    const resizingSidebar = useRef(false);
    const [maxSidebarWidth, setMaxSidebarWidth] = useState(480);
    const [sidebarWidth, setSidebarWidth] = useState(() => {
        try {
            const savedWidth = Number(localStorage.getItem("investsmart-chart-sidebar-width"));
            return Number.isFinite(savedWidth) ? Math.min(480, Math.max(260, savedWidth)) : 340;
        } catch { return 340; }
    });

    const [quoteResult, setQuoteResult] = useState({ symbol: "", quote: null, error: "" });
    const [indices, setIndices] = useState([]);
    const [historyResult, setHistoryResult] = useState({ key: "", history: [], error: "" });
    const [watchlistState, setWatchlistState] = useState({
        items: [], loading: true, refreshing: false, listError: "", pricesStale: false,
    });
    const watchlistLoader = useRef(null);
    const { items: watchlist, loading: watchlistLoading, refreshing: watchlistRefreshing,
        listError: watchlistError, pricesStale: watchlistStale } = watchlistState;
    const selectedPeriod = chartPeriod(new URLSearchParams(location.search).get("period"));
    const quotePoller = useRef(null), historyPoller = useRef(null);
    const [feedStatus, setFeedStatus] = useState({});

    const historyKey = `${quoteKey}:${selectedPeriod}`;
    const hasCurrentHistory = historyResult.key === historyKey;
    const history = hasCurrentHistory ? historyResult.history : [];
    const historyLoading = !hasCurrentHistory;
    const historyError = hasCurrentHistory ? historyResult.error : "";
    const selectedStock = watchlist.find((item) => instrumentKey ? item.instrumentKey === instrumentKey : item.symbol.toUpperCase() === normalizedSymbol && item.exchange !== "BSE");
    const fetchedQuote = quoteResult.symbol === quoteKey ? quoteResult.quote : null;
    const quote = fetchedQuote || (selectedStock ? {
        symbol: selectedStock.symbol,
        currentPrice: selectedStock.currentPrice,
        previousClose: selectedStock.previousClose,
        dayHigh: selectedStock.dayHigh, dayLow: selectedStock.dayLow, lastUpdated: selectedStock.marketDataUpdatedAt
    } : null);
    const isIndex = isIndexInstrument({
        symbol: normalizedSymbol,
        companyName: location.state?.companyName,
        quoteSymbol: quote?.symbol,
        instrumentKey: instrumentKey || quote?.instrumentKey,
    });
    const displaySymbol = quote?.symbol || normalizedSymbol;
    const exchange = selectedStock?.exchange || quote?.exchange || location.state?.exchange
        || (instrumentKey.startsWith("BSE_EQ|") ? "BSE" : instrumentKey.startsWith("NSE_EQ|") ? "NSE" : "");
    const loading = !quote && quoteResult.symbol !== quoteKey;
    const quoteError = quoteResult.symbol === quoteKey
        && (!quote || !(Number(quote.currentPrice) > 0))
        ? quoteResult.error || "Live quote is unavailable right now."
        : "";

    const loadWatchlist = useCallback(() => watchlistLoader.current?.refresh(true), []);

    useEffect(() => {
        const poller = createMarketPoller({
            interval: 15000,
            load: async signal => { const { data } = await api.get(`/market/quotes/${encodeURIComponent(normalizedSymbol)}`, { params: { instrumentKey: instrumentKey || undefined }, signal, timeout: 10000 }); if (!(Number(data?.currentPrice) > 0) || (instrumentKey && data.instrumentKey !== instrumentKey)) throw new Error('Invalid listing quote'); return data; },
            onSuccess: data => setQuoteResult({ symbol: quoteKey, quote: data, error: '' }),
            onState: status => { setFeedStatus(old => ({ ...old, [quoteKey]: { ...old[quoteKey], ...status } })); if (status.error) setQuoteResult(old => old.symbol === quoteKey ? { ...old, error: status.error } : { symbol: quoteKey, quote: null, error: status.error }); }
        });
        quotePoller.current = poller;
        return () => { poller.dispose(); quotePoller.current = null; };
    }, [normalizedSymbol, instrumentKey, quoteKey]);

    useEffect(() => {
        const poller = createMarketPoller({
            interval: ['1D', '1W'].includes(selectedPeriod) ? 30000 : 60000,
            load: async signal => { const { data } = await api.get(`/market/history/${encodeURIComponent(normalizedSymbol)}`, { params: { period: selectedPeriod, instrumentKey: instrumentKey || undefined }, signal, timeout: ["5Y","ALL"].includes(selectedPeriod) ? 60000 : 20000 }); if (!Array.isArray(data) || !data.length) throw new Error('Candle history unavailable'); return data; },
            onSuccess: data => setHistoryResult({ key: historyKey, history: data, error: '' }),
            onState: status => { setFeedStatus(old => ({ ...old, [historyKey]: { ...old[historyKey], ...status } })); if (status.error) setHistoryResult(old => old.key === historyKey ? { ...old, error: status.error } : { key: historyKey, history: [], error: status.error }); }
        });
        historyPoller.current = poller;
        return () => { poller.dispose(); historyPoller.current = null; };
    }, [normalizedSymbol, instrumentKey, selectedPeriod, historyKey]);

    useEffect(() => {
        const controller = new AbortController();
        const load = async () => {
            try { const { data } = await api.get("/market/indices", { signal: controller.signal }); if (Array.isArray(data)) setIndices(data); }
            catch (error) { if (!controller.signal.aborted) console.warn("Index refresh unavailable", error); }
        };
        load(); const timer = window.setInterval(() => { if (!document.hidden) load(); }, REFRESH_INTERVAL);
        return () => { controller.abort(); window.clearInterval(timer); };
    }, []);

    useEffect(() => {
        const loader = createGraphWatchlist(api, setWatchlistState);
        watchlistLoader.current = loader;
        const refresh = () => { void loader.refresh(false); };
        const initial = window.setTimeout(refresh, 0);
        window.addEventListener("investsmart-watchlist-changed", refresh);
        const timer = window.setInterval(() => { if (!document.hidden) refresh(); }, REFRESH_INTERVAL);
        return () => {
            window.clearTimeout(initial);
            window.clearInterval(timer);
            window.removeEventListener("investsmart-watchlist-changed", refresh);
            watchlistLoader.current = null;
            loader.dispose();
        };
    }, []);

    const currentPrice = Number(quote?.currentPrice);
    const previousClose = Number(quote?.previousClose);
    const hasCurrentPrice = Number.isFinite(currentPrice) && currentPrice > 0;
    const hasPreviousClose = Number.isFinite(previousClose) && previousClose > 0;
    const change = hasCurrentPrice && hasPreviousClose ? currentPrice - previousClose : null;
    const changePercent = change === null ? null : (change / previousClose) * 100;

    const handleLogout = async () => {
        try { await logout(); navigate("/login"); } catch { setLogoutError("Sign out failed. Please retry."); }
    };

    const updateSidebarWidth = clientX => {
        const bounds = workspaceRef.current?.getBoundingClientRect();
        if (!bounds) return;
        setSidebarWidth(Math.max(260, Math.min(maxSidebarWidth, bounds.right - clientX)));
    };

    const handleResizeKeyDown = event => {
        if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
            event.preventDefault();
            const direction = event.key === "ArrowLeft" ? 1 : -1;
            setSidebarWidth(width => Math.min(maxSidebarWidth, Math.max(260, width + direction * (event.shiftKey ? 40 : 16))));
        } else if (event.key === "Home") {
            event.preventDefault();
            setSidebarWidth(260);
        } else if (event.key === "End") {
            event.preventDefault();
            setSidebarWidth(maxSidebarWidth);
        }
    };

    useEffect(() => {
        const workspace = workspaceRef.current;
        if (!workspace) return undefined;
        const updateLimit = () => {
            const limit = Math.max(260, Math.min(480, workspace.clientWidth - 520));
            setMaxSidebarWidth(limit);
            setSidebarWidth(width => Math.min(width, limit));
        };
        const observer = new ResizeObserver(updateLimit);
        observer.observe(workspace);
        updateLimit();
        return () => observer.disconnect();
    }, []);

    useEffect(() => {
        try { localStorage.setItem("investsmart-chart-sidebar-width", String(sidebarWidth)); } catch { /* Storage may be disabled. */ }
    }, [sidebarWidth]);

    return (
        <div className={`terminal-page${fullChart ? " terminal-full" : ""}`} data-theme={theme}>
            <header className="terminal-topbar">
                <div className="terminal-top-left">
                    <button className="terminal-brand" onClick={() => navigate("/dashboard")}>Invest<span>Smart</span></button>
                    <span className="terminal-market-label">MARKETS</span>
                </div>
                <div className="terminal-top-actions">
                    <button className="terminal-theme-toggle" onClick={() => setTheme(value => value === "dark" ? "light" : "dark")} aria-label="Toggle chart light and dark mode" aria-pressed={theme === "light"}>{theme === "dark" ? "Light mode" : "Dark mode"}</button>
                    <button className="terminal-back" onClick={() => navigate(fullChart ? chartViewPath(normalizedSymbol, instrumentKey, selectedPeriod) : location.state?.returnTo || "/market", { state: location.state })}>← {fullChart ? "Back to normal" : "Back"}</button>
                    {logoutError && <span role="alert">{logoutError}</span>}<button className="terminal-logout" onClick={handleLogout}>Logout</button>
                </div>
            </header>

            <main ref={workspaceRef} className="terminal-workspace" style={{ gridTemplateColumns: fullChart ? "minmax(0, 1fr)" : `minmax(0, 1fr) 10px ${sidebarWidth}px` }}>
                <section className="terminal-chart-column">
                    <header className="terminal-instrument-header">
                        <div className="terminal-instrument-name">
                            <div>
                                <div className="terminal-stock-heading">
                                    <h1>{selectedStock?.companyName || location.state?.companyName || displaySymbol}</h1>
                                    {!isIndex && exchange && <span className="terminal-exchange-badge">{exchange}</span>}
                                </div>
                                {isIndex ? <p className="terminal-detail-muted">Index - not directly tradable{(instrumentKey === "NSE_INDEX|Nifty 50" || normalizedSymbol.replace(/[^A-Z0-9]/g, "") === "NIFTY50") && <> · <a href="/market?tab=options">NIFTY CE / PE option chain</a></>}</p> : <TradeControls key={quoteKey} symbol={normalizedSymbol} instrumentKey={instrumentKey} onCompleted={() => { void quotePoller.current?.refresh(); void watchlistLoader.current?.refresh(true); }} watchlistAction={<AddToWatchlist symbol={normalizedSymbol} instrumentKey={instrumentKey} isin={selectedStock?.isin} compact />} />}
                                <p>{displaySymbol} · {isIndex ? "Index" : "Equity"}</p>
                            </div>
                        </div>
                        <div className="terminal-live-price">
                            <strong>{loading ? "Loading quote…" : hasCurrentPrice ? `₹${money(currentPrice)}` : "Quote unavailable"}</strong>
                            {change !== null ? <span className={change >= 0 ? "terminal-positive" : "terminal-negative"}>
                                {change >= 0 ? "+" : "−"}₹{money(Math.abs(change))} ({changePercent >= 0 ? "+" : "−"}{Math.abs(changePercent).toFixed(2)}%)
                            </span> : <small>{hasCurrentPrice ? "Previous close unavailable" : quoteError || "Live quote unavailable"}</small>}
                            {hasPreviousClose && <small>Prev close ₹{money(previousClose)}</small>}
                            {quoteError && <button className="terminal-quote-retry" onClick={() => void quotePoller.current?.refresh()}>Retry quote</button>}
                        </div>
                    </header>

                    <div className="terminal-chart-toolbar">
                        <div className="terminal-chart-title">
                            <strong>{displaySymbol}</strong><span>·</span><span>Candles</span>
                        </div>
                        <div className="terminal-periods" role="group" aria-label="Chart time range">
                            {PERIODS.map((period) => (
                                <button key={period} className={selectedPeriod === period ? "active" : ""}
                                    aria-pressed={selectedPeriod === period}
                                    onClick={() => { navigate(chartViewPath(normalizedSymbol,instrumentKey,period,fullChart),{replace:true,state:location.state}); }}>{period === "ALL" ? "All" : period}</button>
                            ))}
                        </div>
                    {!fullChart && <button type="button" className="terminal-full-link" onClick={() => navigate(chartViewPath(normalizedSymbol,instrumentKey,selectedPeriod,true),{state:location.state})}>Full chart</button>}
                    </div>

                    {selectedPeriod === "ALL" && <p className="terminal-history-note">All available provider history from 2000 onward. Actual coverage depends on the listing.</p>}
                    <div className="terminal-feed-status">
                        <span>Quote received: {feedStatus[quoteKey]?.updatedAt ? new Date(feedStatus[quoteKey].updatedAt).toLocaleTimeString('en-IN') : 'Waiting'}</span>
                        <span>Candles received: {feedStatus[historyKey]?.updatedAt ? new Date(feedStatus[historyKey].updatedAt).toLocaleTimeString('en-IN') : 'Waiting'}</span>
                        <button type="button" disabled={feedStatus[quoteKey]?.refreshing || feedStatus[historyKey]?.refreshing} onClick={() => { void quotePoller.current?.refresh(); void historyPoller.current?.refresh(); }}>Refresh chart</button>
                        {(feedStatus[quoteKey]?.refreshing || feedStatus[historyKey]?.refreshing) && <span role="status">Refreshing...</span>}
                    </div>
                    {(historyError || feedStatus[quoteKey]?.error) && <p className="terminal-feed-warning" role="status">{historyError || feedStatus[quoteKey]?.error} Last received data is retained; this chart does not authorize an order fill.</p>}
                    <MarketCandlestickChart key={historyKey} history={history} period={selectedPeriod} loading={historyLoading} theme={theme}
                        dayHigh={quote?.dayHigh} dayLow={quote?.dayLow} latestPrice={feedStatus[quoteKey]?.error ? null : fetchedQuote?.currentPrice} />
                    <footer className="terminal-chart-footer">
                        <span>Market data via Upstox</span>
                        <span>INR · {selectedPeriod === "1D" ? "5 min" : selectedPeriod === "1W" ? "30 min" : "1 day"}</span>
                        <span>Educational view · Paper trading only</span>
                    </footer>
                </section>

                {!fullChart && <div className="terminal-resize-handle" role="separator" aria-orientation="vertical"
                    aria-label="Resize chart and watchlist panels" aria-valuemin={260} aria-valuemax={maxSidebarWidth}
                    aria-valuenow={sidebarWidth} tabIndex={0} title="Drag to resize panels; use arrow keys"
                    onPointerDown={event => {
                        if (window.innerWidth <= 700) return;
                        event.preventDefault();
                        resizingSidebar.current = true;
                        event.currentTarget.setPointerCapture(event.pointerId);
                    }}
                    onPointerMove={event => { if (resizingSidebar.current) updateSidebarWidth(event.clientX); }}
                    onPointerUp={() => { resizingSidebar.current = false; }}
                    onPointerCancel={() => { resizingSidebar.current = false; }}
                    onKeyDown={handleResizeKeyDown} />}

                {!fullChart && <GraphSidebar indices={indices} watchlist={watchlist} loading={watchlistLoading} refreshing={watchlistRefreshing}
                    error={watchlistError} stale={watchlistStale} refresh={loadWatchlist} symbol={normalizedSymbol}
                    instrumentKey={instrumentKey} quote={quote} companyName={selectedStock?.companyName || location.state?.companyName} />}
            </main>
        </div>
    );
}

export default MarketChartTerminal;
