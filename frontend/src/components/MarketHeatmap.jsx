import "./MarketHeatmap.css";

const INDEX_ASSETS = [
    ["NIFTY 50", "Nifty 50", "NSE"],
    ["SENSEX", "SENSEX", "BSE"],
    ["NIFTY BANK", "Nifty Bank", "NSE"],
    ["NIFTY IT", "Nifty IT", "NSE"],
];

const STOCK_ASSETS = [
    ["RELIANCE", "Reliance Industries"],
    ["HDFCBANK", "HDFC Bank"],
    ["ICICIBANK", "ICICI Bank"],
    ["INFY", "Infosys"],
    ["TCS", "TCS"],
    ["BHARTIARTL", "Bharti Airtel"],
    ["SBIN", "State Bank of India"],
    ["ITC", "ITC"],
    ["LT", "Larsen & Toubro"],
    ["BAJFINANCE", "Bajaj Finance"],
];

const money = (value) => Number(value).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
});

function MarketHeatmap({ indices, stocks, loading, error, stale, onSelect }) {
    const indexMap = new Map(indices.map((quote) => [quote.symbol.toUpperCase(), quote]));
    const stockMap = new Map(stocks.map((quote) => [quote.symbol.toUpperCase(), quote]));
    const assets = [
        ...INDEX_ASSETS.map(([symbol, name, exchange]) => ({
            symbol, name, exchange, quote: indexMap.get(symbol), type: "index",
        })),
        ...STOCK_ASSETS.map(([symbol, name]) => ({
            symbol, name, exchange: "NSE", quote: stockMap.get(symbol), type: "stock",
        })),
    ];

    return (
        <div className="terminal-heatmap-panel" role="tabpanel" aria-label="Market heatmap">
            <div className="terminal-heatmap-legend">
                <span>Daily change</span>
                <span><i className="heatmap-legend-down" /> Down</span>
                <span><i className="heatmap-legend-flat" /> Flat</span>
                <span><i className="heatmap-legend-up" /> Up</span>
            </div>
            {loading && <p className="terminal-empty" role="status">Loading market heatmap…</p>}
            {!loading && error && stocks.length === 0 && (
                <p className="terminal-empty" role="alert">Featured stock quotes are unavailable right now.</p>
            )}
            {stale && <p className="terminal-stale-note" role="status">One or more quote groups could not refresh; showing the latest loaded values.</p>}
            <div className="terminal-heatmap-grid">
                {assets.map(({ symbol, name, exchange, quote, type }) => {
                    const price = Number(quote?.currentPrice);
                    const previousClose = Number(quote?.previousClose);
                    const hasPrice = Number.isFinite(price) && price > 0;
                    const hasPreviousClose = Number.isFinite(previousClose) && previousClose > 0;
                    const changePercent = hasPrice && hasPreviousClose
                        ? ((price - previousClose) / previousClose) * 100
                        : null;
                    const trend = changePercent === null ? "unavailable"
                        : changePercent > 0 ? "up" : changePercent < 0 ? "down" : "flat";

                    return (
                        <button
                            key={symbol}
                            type="button"
                            className={`terminal-heatmap-tile heatmap-${trend}`}
                            onClick={() => onSelect(symbol)}
                            aria-label={`${name}, ${hasPrice ? `₹${money(price)}` : "price unavailable"}, ${changePercent === null ? "daily change unavailable" : `${changePercent >= 0 ? "+" : ""}${changePercent.toFixed(2)} percent`}`}
                        >
                            <span className="heatmap-tile-kind">{type === "index" ? "INDEX" : exchange}</span>
                            <strong>{symbol}</strong>
                            <span className="heatmap-tile-name">{name}</span>
                            <span className="heatmap-tile-price">{hasPrice ? `₹${money(price)}` : "Unavailable"}</span>
                            <span className="heatmap-tile-change">
                                {changePercent === null ? "—"
                                    : `${changePercent >= 0 ? "+" : "−"}${Math.abs(changePercent).toFixed(2)}%`}
                            </span>
                        </button>
                    );
                })}
            </div>
        </div>
    );
}

export default MarketHeatmap;
