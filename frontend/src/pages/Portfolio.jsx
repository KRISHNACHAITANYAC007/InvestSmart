import { Link } from "react-router-dom";
import { chartPath } from "../api/marketInstruments.js";
import useAccountData from "../hooks/useAccountData.js";
import usePageTab from "../hooks/usePageTab.js";
import PageTabs from "../components/PageTabs.jsx";
import FundsPanel from "../components/FundsPanel.jsx";
import SettlementPanel from "../components/SettlementPanel.jsx";
import MarketDataStatus from "../components/MarketDataStatus.jsx";
import TradeControls from "../components/TradeControls.jsx";
import PaperActivity from "../components/PaperActivity.jsx";
import OptionsDesk from "../components/OptionsDesk.jsx";
import "./Portfolio.css";

function formatCurrency(value) {
    if (value == null) {
        return "Unavailable";
    }

    return Number(value).toLocaleString("en-IN", {
        style: "currency",
        currency: "INR",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function formatPercent(value) {
    return value == null
        ? "Unavailable"
        : `${Number(value).toFixed(2)}%`;
}

function pnlClass(value) {
    if (value == null || Number(value) === 0) {
        return "";
    }

    return Number(value) > 0
        ? "portfolio-pnl-positive"
        : "portfolio-pnl-negative";
}

function Portfolio() {
    const {portfolio,loading,error,refreshing,refreshInfo,refresh,reload}=useAccountData();
    const tab=usePageTab(["holdings","insights","funds","options"],"holdings");

    if(tab==="options")return <div className="portfolio-page"><h1>Portfolio</h1><PageTabs label="Portfolio sections" selected={tab} tabs={[["holdings","Holdings"],["options","Options"],["insights","Insights"],["funds","Funds"]]} /><OptionsDesk /><PaperActivity compact /></div>;
    if (loading) {
        return <h2>Loading portfolio...</h2>;
    }

    if (!portfolio) {
        return <div role="alert"><h2>{error || "Portfolio data is unavailable."}</h2><button onClick={() => refresh()}>Retry</button></div>;
    }

    const riskInsights = portfolio.riskInsights ?? [];

    return (
        <div className="portfolio-page">
            <div className="portfolio-header">
                <div className="page-heading-actions"><h1>Portfolio</h1><button className="market-refresh-button" disabled={refreshing} onClick={() => refresh(true)}>{refreshing ? "Refreshing..." : "Refresh prices"}</button></div>
                <p>Track your investments and holdings.</p>
            </div>

            <PageTabs label="Portfolio sections" selected={tab} tabs={[["holdings","Holdings"],["options","Options"],["insights","Insights"],["funds","Funds"]]} />
            {error && <p role="alert">{error}</p>}<MarketDataStatus info={refreshInfo} />
            {(Number(portfolio.reservedOptionsFunds)>0||Number(portfolio.reservedOrderFunds)>0||Number(portfolio.optionsCost)>0)&&<div className="portfolio-data-notice"><strong>Options and pending orders</strong><p>Option positions: {formatCurrency(portfolio.optionsValue)} · Option cost: {formatCurrency(portfolio.optionsCost)} · Reserved for options: {formatCurrency(portfolio.reservedOptionsFunds)} · Reserved for equity orders: {formatCurrency(portfolio.reservedOrderFunds)}</p><p>Portfolio totals include these positions and reserved cash. An expired option awaiting its exact dated settlement close makes total valuation unavailable.</p><Link to="/portfolio?tab=options">View option positions</Link> · <Link to="/orders">Manage orders</Link></div>}
            {(Number(portfolio.reservedIpoFunds) > 0 || Number(portfolio.unlistedIpoCost) > 0) && <div className="portfolio-data-notice"><strong>IPOs</strong><p>Reserved virtual cash: {formatCurrency(portfolio.reservedIpoFunds)} · Unlisted allocations at cost: {formatCurrency(portfolio.unlistedIpoCost)} · Total cash including reservations: {formatCurrency(portfolio.totalCash)}</p><Link to="/orders?tab=ipos">View IPO applications →</Link></div>}
            {tab === "funds" ? <><PaperActivity compact /><FundsPanel balance={portfolio.balance} onChanged={reload} /><SettlementPanel funds revision={portfolio} /></> : <>
            <PaperActivity compact /><div className="portfolio-summary">
                <div className="portfolio-summary-card">
                    <p className="portfolio-summary-label">Available cash</p>
                    <h2>{formatCurrency(portfolio.balance)}</h2>
                    <span className="portfolio-summary-detail">
                        {formatPercent(portfolio.cashAllocationPercent)} cash allocation, including reservations
                    </span>
                </div>

                <div className="portfolio-summary-card">
                    <p className="portfolio-summary-label">Invested at market value</p>
                    <h2>{formatCurrency(portfolio.holdingsValue)}</h2>
                    <span className="portfolio-summary-detail">
                        {formatPercent(portfolio.investedAllocationPercent)} of portfolio value
                    </span>
                </div>

                <div className="portfolio-summary-card portfolio-total-card">
                    <p className="portfolio-summary-label">Total portfolio value</p>
                    <h2>{formatCurrency(portfolio.totalValue)}</h2>
                </div>

                <div className="portfolio-summary-card">
                    <p className="portfolio-summary-label">Invested cost</p>
                    <h2>{formatCurrency(portfolio.investedAmount)}</h2>
                </div>

                <div className="portfolio-summary-card">
                    <p className="portfolio-summary-label">Realized P&amp;L</p>
                    <h2 className={pnlClass(portfolio.realizedPnl)}>
                        {formatCurrency(portfolio.realizedPnl)}
                    </h2>
                </div>

                <div className="portfolio-summary-card">
                    <p className="portfolio-summary-label">Unrealized P&amp;L</p>
                    <h2 className={pnlClass(portfolio.unrealizedPnl)}>
                        {formatCurrency(portfolio.unrealizedPnl)}
                    </h2>
                </div>
            </div>

            {portfolio.valuationAvailable === false && <p className="portfolio-data-notice" role="status">Some holdings or unlisted IPO allocations have no reliable market price. Total valuation, allocation and unrealized P&amp;L are unavailable. Known invested cost and available cash remain visible.</p>}
            {!portfolio.performanceAvailable && (
                <div className="portfolio-data-notice" role="status">
                    Some cost-basis and profit/loss figures are unavailable
                    because completed trade history does not match current
                    holdings. Known prices and available values are still shown.
                </div>
            )}

            {tab === "holdings" && <SettlementPanel holdings={portfolio.holdings} revision={portfolio} />}
            {tab === "insights" && <section className="portfolio-insights" aria-labelledby="insights-title">
                <div>
                    <h2 id="insights-title">Educational risk insights</h2>
                    <p>
                        These flags describe concentration in current holdings;
                        they are not buy or sell recommendations.
                    </p>
                </div>

                {portfolio.valuationAvailable === false ? <p>Concentration insights require prices for every holding.</p> : riskInsights.length === 0 ? (
                    <p className="portfolio-insights-empty">
                        No concentration flags at the 25% single-stock and
                        40% classified-sector thresholds.
                    </p>
                ) : (
                    <ul>
                        {riskInsights.map((insight) => (
                            <li key={insight}>{insight}</li>
                        ))}
                    </ul>
                )}
            </section>}

            {tab === "holdings" && <div className="portfolio-holdings-section">
                <h2 className="portfolio-section-title">My Holdings</h2>

                {portfolio.holdings.length === 0 ? (
                    <div className="portfolio-empty">
                        <p>You don&apos;t own any stocks yet.</p>
                    </div>
                ) : (
                    <div className="portfolio-holdings">
                        {portfolio.holdings.map((holding) => (
                            <div
                                className="portfolio-holding-card"
                                key={holding.stockId}
                            >
                                <div className="portfolio-holding-header">
                                    <div>
                                        <h3><Link to={chartPath(holding)} state={{companyName:holding.companyName,returnTo:"/portfolio?tab=holdings"}}>{holding.symbol}</Link></h3>
                                        <p>
                                            {holding.companyName}
                                            <span className="portfolio-sector">
                                                {holding.sector || "Unclassified"}
                                            </span>
                                        </p>
                                    </div>

                                    <div className="portfolio-holding-value">
                                        {formatCurrency(holding.currentValue)}
                                    </div>
                                </div>

                                <div className="portfolio-holding-details">
                                    <div>
                                        <span>Quantity</span>
                                        <strong>{holding.quantity}</strong>
                                    </div>
                                    <div>
                                        <span>Current price</span>
                                        <strong>{formatCurrency(holding.currentPrice)}</strong>
                                    </div>
                                    <div>
                                        <span>Average buy price</span>
                                        <strong>{formatCurrency(holding.averageBuyPrice)}</strong>
                                    </div>
                                    <div>
                                        <span>Invested cost</span>
                                        <strong>{formatCurrency(holding.investedAmount)}</strong>
                                    </div>
                                    <div>
                                        <span>Unrealized P&amp;L</span>
                                        <strong className={pnlClass(holding.unrealizedPnl)}>
                                            {formatCurrency(holding.unrealizedPnl)}
                                            {holding.unrealizedPnlPercent != null
                                                ? ` (${formatPercent(holding.unrealizedPnlPercent)})`
                                                : ""}
                                        </strong>
                                    </div>
                                    <div>
                                        <span>Share of holdings</span>
                                        <strong>{formatPercent(holding.allocationPercent)}</strong>
                                    </div>
                                </div>
                                <div className="holding-actions"><Link to={chartPath(holding)} state={{companyName:holding.companyName,returnTo:"/portfolio?tab=holdings"}}>Open chart</Link><TradeControls symbol={holding.symbol} instrumentKey={holding.instrumentKey} /></div>
                            </div>
                        ))}
                    </div>
                )}
            </div>}
            </>}
        </div>
    );
}

export default Portfolio;
