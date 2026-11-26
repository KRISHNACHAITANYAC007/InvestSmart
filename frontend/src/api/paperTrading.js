export function validatePaperOrder({ quantity, side, price, balance, holdings }) {
    if (!Number.isSafeInteger(quantity) || quantity < 1) return "Enter a whole-share quantity of at least 1.";
    if (!(Number(price) > 0) || !Number.isFinite(Number(price))) return "A current price for this listing is required.";
    if (side === "buy" && (!Number.isFinite(Number(balance)) || balance == null)) return "Available cash could not be loaded.";
    if (side === "buy" && Number(price) * quantity > Number(balance)) return "Insufficient virtual balance.";
    if (side === "sell" && quantity > Number(holdings || 0)) return "Insufficient holdings on this listing.";
    return "";
}

export async function loadTradingListing(client, listing, signal) {
    const [{ data: stock }, { data: quote }] = await Promise.all([
        client.post("/stocks/import", { symbol: listing.symbol, instrumentKey: listing.instrumentKey }, { signal }),
        client.get(`/market/quotes/${encodeURIComponent(listing.symbol)}`, { params: { instrumentKey: listing.instrumentKey }, signal }),
    ]);
    if (stock.instrumentKey !== listing.instrumentKey || quote.instrumentKey !== listing.instrumentKey)
        throw new Error("The quote does not match the selected listing. Please retry.");
    return { stock, quote };
}

export function createOrderSubmitter(client) {
    let pending = false;
    return async (side, stockId, quantity) => {
        if (pending) return null;
        pending = true;
        try { return await client.post(`/orders/${side}`, { stockId, quantity }); }
        finally { pending = false; }
    };
}
