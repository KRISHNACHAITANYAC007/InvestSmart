import { useState } from "react";
import TradeOrderTicket from "./TradeOrderTicket.jsx";

export default function TradeControls({ symbol, instrumentKey, onCompleted, watchlistAction }) {
    const [side, setSide] = useState(null);
    return <div className="chart-trade-controls" aria-label="Paper trading actions">
            <button type="button" className="chart-buy-button" disabled={!!side} onClick={() => setSide("buy")}>Buy</button>
            <button type="button" className="chart-sell-button" disabled={!!side} onClick={() => setSide("sell")}>Sell</button>
        {watchlistAction}
        {side && <TradeOrderTicket symbol={symbol} instrumentKey={instrumentKey} initialSide={side} onClose={() => setSide(null)} onCompleted={onCompleted} />}
    </div>;
}
