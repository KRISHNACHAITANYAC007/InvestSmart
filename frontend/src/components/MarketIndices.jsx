import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/api.js";
import "./MarketIndices.css";

const INDEX_SYMBOLS = ["NIFTY 50", "SENSEX", "NIFTY BANK", "NIFTY IT"];
const currency = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
});

function MarketIndices({ compact = false, returnTo = "/market" }) {
    const [indices, setIndices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState(false);
    const [lastUpdated, setLastUpdated] = useState(null);
    const mounted = useRef(false), request = useRef(null), sequence = useRef(0);
    const value = price => compact ? price.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : currency.format(price);
    const unavailableSymbols = indices
        .filter((item) => !(Number(item.currentPrice) > 0))
        .map((item) => item.symbol);

    const refreshIndices = useCallback(async () => {
        const id = ++sequence.current;
        request.current?.abort();
        const controller = new AbortController(); request.current = controller;
        setRefreshing(true);
        try {
            const response = await api.get("/market/indices", { signal: controller.signal });
            if (!Array.isArray(response.data)) {
                throw new Error("Unexpected market indices response");
            }
            if (!mounted.current || controller.signal.aborted || id !== sequence.current) return;
            setIndices(response.data);
            setLastUpdated(new Date());
            setError(false);
        } catch {
            if (mounted.current && !controller.signal.aborted && id === sequence.current) setError(true);
        } finally {
            if (mounted.current && !controller.signal.aborted && id === sequence.current) { setLoading(false); setRefreshing(false); }
        }
    }, []);

    useEffect(() => {
        mounted.current = true;
        const initialLoad = window.setTimeout(refreshIndices, 0);
        const intervalId = window.setInterval(() => {
            if (!document.hidden) refreshIndices();
        }, 60000);
        return () => {
            mounted.current = false; request.current?.abort();
            window.clearTimeout(initialLoad);
            window.clearInterval(intervalId);
        };
    }, [refreshIndices]);

    return (
        <section className={`market-indices${compact ? " is-compact" : ""}`} aria-labelledby="market-indices-title">
            <div className="market-indices-heading">
                <div>
                    <h2 id="market-indices-title">{compact ? "Market overview" : "Indian indices"}</h2>
                    {!compact && <p>Market snapshot</p>}
                </div>
                <button type="button" onClick={refreshIndices} disabled={refreshing}>
                    {refreshing ? "Updating…" : "Refresh"}
                </button>
            </div>
            {loading ? (
                <p className="market-indices-state" role="status">Loading index quotes…</p>
            ) : (
                <div className="market-index-grid">
                    {INDEX_SYMBOLS.map((symbol) => {
                        const index = indices.find((item) => item.symbol === symbol);
                        if (!index) {
                            return (
                                <article className="market-index-card is-unavailable" key={symbol}>
                                    <strong>{symbol}</strong>
                                    <span>{error ? "Quote unavailable" : "No quote available"}</span>
                                </article>
                            );
                        }

                        const currentPrice = Number(index.currentPrice);
                        const previousClose = Number(index.previousClose);
                        const hasCurrentPrice = Number.isFinite(currentPrice) && currentPrice > 0;
                        const hasPreviousClose = Number.isFinite(previousClose) && previousClose > 0;
                        const change = hasCurrentPrice && hasPreviousClose ? currentPrice - previousClose : null;
                        const changePercent = change === null ? null : (change / previousClose) * 100;
                        const direction = change > 0 ? "positive" : change < 0 ? "negative" : "flat";

                        return (
                            <Link
                                className="market-index-card"
                                to={`/market/chart/${encodeURIComponent(symbol)}`}
                                state={{ companyName: symbol, returnTo }}
                                key={symbol}
                                aria-label={`${symbol} quote and chart`}
                            >
                                <div className="market-index-card-top">
                                    <strong>{symbol}</strong>
                                    <span className="market-index-chart-link" aria-hidden="true">{compact ? "↗" : "View chart ↗"}</span>
                                </div>
                                <span className="market-index-value">
                                    {hasCurrentPrice ? value(currentPrice) : "Price unavailable"}
                                </span>
                                <span className={`market-index-change ${direction}`}>
                                    {!hasCurrentPrice
                                        ? "Live quote unavailable"
                                        : change === null
                                        ? "Previous close unavailable"
                                        : `${change > 0 ? "+" : ""}${value(change)} (${changePercent > 0 ? "+" : ""}${changePercent.toFixed(2)}%)`}
                                </span>
                            </Link>
                        );
                    })}
                </div>
            )}
            <p className={`market-indices-footnote${error ? " is-stale" : ""}`} role="status">
                {error
                    ? lastUpdated
                        ? `Could not update quotes. Showing values from ${lastUpdated.toLocaleTimeString()}.`
                        : "Live index quotes are unavailable right now."
                    : unavailableSymbols.length
                        ? `Live quotes unavailable for ${unavailableSymbols.join(", ")}.`
                        : lastUpdated
                        ? `Data via Upstox · Updated ${lastUpdated.toLocaleTimeString()}`
                        : "Market data via Upstox"}
            </p>
        </section>
    );
}

export default MarketIndices;
