import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/api.js";
import AddToWatchlist from "../components/AddToWatchlist.jsx";
import ChartStockSearch from "../components/ChartStockSearch.jsx";
import { chartPath } from "../api/marketInstruments.js";
import PageTabs from "../components/PageTabs.jsx";
import usePageTab from "../hooks/usePageTab.js";
import WatchlistPanel from "../components/WatchlistPanel.jsx";
import MarketIndices from "../components/MarketIndices.jsx";
import InvestmentMarket from "../components/InvestmentMarket.jsx";
import OptionsDesk from "../components/OptionsDesk.jsx";
import "./Market.css";

const currency = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
});

function Market() {
    const tab=usePageTab(["explore","watchlist","ipos","bonds","options"],"explore");
    const [quotes, setQuotes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [featuredStale, setFeaturedStale] = useState(false);

    useEffect(() => {
        let active = true;
        const load = () => api.get("/market/featured")
            .then((response) => {
                if (active) { setQuotes(response.data.items); setFeaturedStale(response.data.stale); setError(""); }
            })
            .catch(() => { if (active) { setError("Could not refresh Standard stocks."); setFeaturedStale(true); } })
            .finally(() => { if (active) setLoading(false); });
        load();
        const timer = window.setInterval(() => { if (!document.hidden) load(); }, 60000);
        return () => { active = false; window.clearInterval(timer); };
    }, []);

    return (
        <div className="market-page">
            <header className="market-page-heading">
                <div>
                    <p className="market-eyebrow">INVESTSMART · MARKET DESK</p>
                    <h1>Markets</h1>
                    <p className="market-page-subtitle">Track Indian equities and explore market data.</p>
                </div>
            </header>
            <PageTabs label="Market sections" selected={tab} tabs={[["explore","Explore"],["watchlist","Watchlist"],["options","Options"],["ipos","IPOs"],["bonds","Bonds"]]} />
            {tab === "options" ? <OptionsDesk trading /> : tab === "watchlist" ? <WatchlistPanel /> : tab === "ipos" ? <InvestmentMarket key="IPO" kind="IPO" /> : tab === "bonds" ? <InvestmentMarket key="BOND" kind="BOND" /> : (
                <div className="market-overview-layout">
                    <div className="market-primary-column">
                        <section className="market-search" aria-labelledby="market-search-title">
                            <div className="market-section-heading">
                                <div><h2 id="market-search-title">Find an instrument</h2><p>Search companies by name or ticker.</p></div>
                            </div>
                            <ChartStockSearch watchlist={[]} />
                        </section>

                        <section className="market-starter-section" aria-labelledby="starter-stocks-title">
                            <div className="market-section-heading">
                                <div><h2 id="starter-stocks-title">Featured equities</h2><p>Prices and daily movement</p></div>
                                {!loading && !error && <span className="market-result-count">{quotes.length} instruments</span>}
                            </div>
                            {loading && <p className="market-state" role="status">Loading market data…</p>}
                            {error && <p className="market-state market-state-error" role="alert">{error}</p>}
                            {featuredStale && <p className="market-state market-state-stale" role="status">Some prices may be delayed. Showing the latest available values.</p>}
                            {!loading && !error && quotes.length === 0 && (
                                <p className="market-state">No market quotes are available right now.</p>
                            )}
                            {!loading && quotes.length > 0 && (
                                <div className="market-table-scroll" role="region" aria-label="Market equity quotes" tabIndex={0}>
                                    <table className="market-equities-table">
                                        <thead><tr><th scope="col">Instrument</th><th scope="col">Exchange</th><th scope="col">Last price</th><th scope="col">Day change</th><th scope="col"><span className="market-visually-hidden">Actions</span></th></tr></thead>
                                        <tbody>{quotes.map((quote) => {
                                            const price = Number(quote.currentPrice);
                                            const close = Number(quote.previousClose);
                                            const hasPrice = Number.isFinite(price) && price > 0;
                                            const hasChange = hasPrice && Number.isFinite(close) && close > 0;
                                            const change = hasChange ? price - close : null;
                                            const changePercent = hasChange ? change / close * 100 : null;
                                            const exchange = quote.exchange || (quote.instrumentKey?.startsWith("BSE_EQ|") ? "BSE" : "NSE");
                                            return <tr key={quote.instrumentKey || quote.symbol}>
                                                <td><Link className="market-instrument-name" state={{ companyName: quote.companyName, returnTo: "/market?tab=explore" }} to={chartPath(quote)}>
                                                    <strong>{quote.companyName || quote.symbol}</strong><small>{quote.symbol}</small>
                                                </Link></td>
                                                <td><span className="market-exchange-tag">{exchange}</span></td>
                                                <td className="market-price-cell">{hasPrice ? currency.format(price) : "Unavailable"}</td>
                                                <td><span className={`market-change-cell${change === null ? "" : change >= 0 ? " positive" : " negative"}`}>
                                                    {change === null ? "Unavailable" : `${change >= 0 ? "+" : "−"}${currency.format(Math.abs(change))} (${changePercent >= 0 ? "+" : ""}${changePercent.toFixed(2)}%)`}
                                                </span></td>
                                                <td><AddToWatchlist symbol={quote.symbol} instrumentKey={quote.instrumentKey} /></td>
                                            </tr>;
                                        })}</tbody>
                                    </table>
                                </div>
                            )}
                        </section>
                    </div>
                    <aside className="market-sidebar" aria-label="Market indices">
                        <MarketIndices />
                        <div className="market-sidebar-note"><span className="market-sidebar-note-mark" aria-hidden="true">i</span><p>Market data is provided for educational use. Prices may be delayed.</p></div>
                    </aside>
                </div>
            )}
        </div>
    );
}

export default Market;
