import { Link } from "react-router-dom";
import { chartPath } from "../api/marketInstruments.js";
import "./PageTabs.css";
const money = value => value == null ? "Unavailable" : Number(value).toLocaleString("en-IN", { style:"currency", currency:"INR" });
export default function HoldingsPreview({ holdings = [] }) {
    return !holdings.length ? <p>No holdings yet. Explore Markets to place a paper order.</p> : <div className="holdings-preview">{holdings.slice(0,3).map(item =>
        <Link className="holdings-preview-row" key={item.stockId} to={chartPath(item)} state={{ companyName:item.companyName,returnTo:"/dashboard" }}>
            <span><strong>{item.companyName || item.symbol}</strong><small>{item.symbol} · {item.quantity} shares</small></span>
            <span><strong>{money(item.currentValue)}</strong><small>Unrealized P&amp;L {money(item.unrealizedPnl)}</small></span>
        </Link>)}</div>;
}
