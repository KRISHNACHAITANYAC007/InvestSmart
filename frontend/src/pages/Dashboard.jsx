import { chartPath } from "../api/marketInstruments.js";
import { Link, useNavigate } from "react-router-dom";
import useAccountData from "../hooks/useAccountData.js";
import MarketDataStatus from "../components/MarketDataStatus.jsx";
import HoldingsPreview from "../components/HoldingsPreview.jsx";
import OrderList from "../components/OrderList.jsx";
import MarketIndices from "../components/MarketIndices.jsx";
import "../styles/Dashboard.css";

const money = value => value == null ? "Unavailable" : Number(value).toLocaleString("en-IN", { style: "currency", currency: "INR", minimumFractionDigits: 2, maximumFractionDigits: 2 });

function Dashboard() {
    const navigate = useNavigate();
    const {portfolio,orders,watchlist,watchlistError,loading,error,refreshInfo,refreshing,refresh:fetchDashboardData}=useAccountData({includeOrders:true,includeWatchlist:true});
    const balance=portfolio?.balance ?? null;
    if (loading) return <h2>Loading dashboard...</h2>;
    if (error && !portfolio) return <div><h2>{error}</h2><button onClick={() => fetchDashboardData()}>Retry</button></div>;

    const recentOrders = [...orders].sort((a,b) => b.id-a.id).slice(0, 5);
    return (
        <div className="dashboard">
            <header className="dashboard-header">
                <div><h1>Dashboard</h1><p className="dashboard-subtitle">Your portfolio at a glance</p></div>
                <button className="market-refresh-button" onClick={() => fetchDashboardData(true)} disabled={refreshing}>
                    {refreshing ? "Refreshing…" : "Refresh prices"}
                </button>
            </header>
            {error && <p role="alert">{error}</p>}
            <div className="dashboard-content">
                <MarketIndices compact returnTo="/dashboard" />
                <section className="dashboard-summary" aria-label="Account overview">
                    <article className="dashboard-stat"><span>Total portfolio value</span><strong>{money(portfolio?.totalValue)}</strong><Link to="/portfolio">View portfolio →</Link></article>
                    <article className="dashboard-stat"><span>Holdings value</span><strong>{money(portfolio?.holdingsValue)}</strong><small className={portfolio?.unrealizedPnl == null ? "" : portfolio.unrealizedPnl >= 0 ? "positive" : "negative"}>Unrealized P&amp;L {money(portfolio?.unrealizedPnl)}</small></article>
                    <article className="dashboard-stat"><span>Available cash</span><strong>{money(balance)}</strong><Link to="/portfolio?tab=funds">Manage virtual funds →</Link></article>
                </section>
                {(Number(portfolio?.reservedIpoFunds) > 0 || Number(portfolio?.unlistedIpoCost) > 0) && <p className="dashboard-account-note">IPO reservations: {money(portfolio.reservedIpoFunds)} · Unlisted cost: {money(portfolio.unlistedIpoCost)}. <Link to="/orders?tab=ipos">View applications</Link></p>}
                <MarketDataStatus info={refreshInfo} />
                <div className="dashboard-holdings-grid">
                <section className="dashboard-section">
                    <div className="recent-orders-header">
                        <h2>My holdings</h2>
                        <Link className="dashboard-section-link" to="/portfolio?tab=holdings">View all</Link>
                    </div>
                    <HoldingsPreview holdings={portfolio?.holdings} />
                </section>
                <section className="dashboard-section dashboard-watchlist-preview">
                    <div className="recent-orders-header">
                        <h2>Watchlist</h2>
                        <Link className="dashboard-section-link" to="/market?tab=watchlist">View all</Link>
                    </div>
                    {watchlistError ? <p className="dashboard-watchlist-empty" role="status">{watchlistError}</p>
                        : watchlist.length === 0 ? <p className="dashboard-watchlist-empty">No stocks saved yet. Add stocks from your Watchlist.</p>
                            : <div className="dashboard-watchlist-scroll">
                                {[...watchlist].sort((a,b) => b.id-a.id).slice(0, 3).map((item) => {
                                    const current = Number(item.currentPrice);
                                    const previous = Number(item.previousClose);
                                    const hasPrices = Number.isFinite(current) && current > 0;
                                    const hasChange = hasPrices && Number.isFinite(previous) && previous > 0;
                                    const change = hasChange ? current - previous : null;
                                    const percent = hasChange ? change / previous * 100 : null;
                                    return <button type="button" className="dashboard-watchlist-row" key={item.id}
                                        onClick={() => navigate(chartPath(item), {state:{companyName:item.companyName,returnTo:"/dashboard"}})}>
                                        <span className="dashboard-watchlist-name"><strong>{item.companyName || item.symbol}</strong><small>{item.symbol}</small></span>
                                        <span className="dashboard-watchlist-metrics">
                                            <strong>{hasPrices ? money(current) : "Price unavailable"}</strong>
                                            <small className={change === null ? "" : change >= 0 ? "positive" : "negative"}>
                                                {change === null ? "Change unavailable" : (change >= 0 ? "+" : "−") + "₹" + Math.abs(change).toFixed(2) + " (" + (percent >= 0 ? "+" : "") + percent.toFixed(2) + "%)"}
                                            </small>
                                        </span>
                                    </button>;
                                })}
                            </div>}
                </section>
                </div>
                <section className="dashboard-section dashboard-orders-section">
                    <div className="recent-orders-header">
                        <h2>Recent orders</h2>
                        <Link className="dashboard-section-link" to="/orders">View all</Link>
                    </div>
                    <OrderList orders={recentOrders} />
                </section>
            </div>
        </div>
    );
}

export default Dashboard;
