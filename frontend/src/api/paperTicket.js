export function ticketUnits({ mode, quantity, amount, price, lot = 1 }) {
    if (!Number.isInteger(lot) || lot < 1) return 0;
    if (mode === "AMOUNT") return Number(price) > 0 ? Math.floor(Number(amount) / Number(price) / lot) * lot : 0;
    return mode === "LOTS" ? Number(quantity) * lot : Number(quantity);
}
export function ticketIntent({ side, product, closing = false, option = false }) {
    if (closing) return side === 'BUY' ? 'Buy to close short' : 'Sell to close long';
    if (product === 'MIS') return side === 'SELL' ? 'Sell/Short Intraday' : 'Buy/Long Intraday';
    return side === 'SELL' && !option && product === 'CASH' ? 'Sell holdings' : side === 'SELL' ? 'Sell' : 'Buy';
}
export function switchQuantityMode(draft, mode, price, lot) {
    const units = ticketUnits({ ...draft, price, lot });
    return { ...draft, mode, quantity: String(mode === "LOTS" ? Math.floor(units / lot) : units), amount: mode === "AMOUNT" ? String(Math.max(0, units * Number(price) || 0)) : draft.amount };
}
