export const companyId = item => item.isin || (item.instrumentKey?.startsWith("NSE_EQ|") ? item.instrumentKey.slice(7) : null) || `symbol:${item.symbol?.toUpperCase()}`;
export const chartPath = item => `/market/chart/${encodeURIComponent(item.symbol)}` + (item.instrumentKey ? `?instrumentKey=${encodeURIComponent(item.instrumentKey)}` : "");
const INDEX_SYMBOLS = new Set(["NIFTY 50", "SENSEX", "NIFTY BANK", "NIFTY IT"]);
export function isIndexInstrument({ symbol, companyName, quoteSymbol, instrumentKey } = {}) {
    if (/(?:^|_)INDEX\|/i.test(instrumentKey || "")) return true;
    return [symbol, companyName, quoteSymbol].some(value => {
        const normalized = String(value || "").trim().replace(/[_-]+/g, " ").replace(/\s+/g, " ").toUpperCase();
        const withoutSuffix = normalized.replace(/\s+INDEX$/, "");
        return INDEX_SYMBOLS.has(withoutSuffix)
            || [...INDEX_SYMBOLS].some(index => index.replace(/\s/g, "") === withoutSuffix.replace(/\s/g, ""));
    });
}
export function groupCompanies(items) {
    const groups = new Map();
    for (const item of items) {
        if (!item.symbol) continue;
        const id = companyId(item);
        if (!groups.has(id)) groups.set(id, []);
        const group = groups.get(id);
        if (!group.some(i => i.instrumentKey === item.instrumentKey)) group.push(item);
    }
    return [...groups.values()].map(listings => ({ ...(listings.find(i => i.exchange === "NSE" || i.instrumentKey?.startsWith("NSE_EQ|")) || listings[0]), listings }));
}
export const notifyWatchlistChanged = () => window.dispatchEvent(new Event("investsmart-watchlist-changed"));
export async function importListing(client, listing) {
    return (await client.post("/stocks/import", { symbol: listing.symbol, instrumentKey: listing.instrumentKey })).data;
}
