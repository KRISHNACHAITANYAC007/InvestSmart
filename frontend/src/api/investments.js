export const money = value => value == null ? "Unavailable" : Number(value).toLocaleString("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 });
export const accountChanged = () => window.dispatchEvent(new Event("investsmart-account-changed"));
export const pendingIpo = status => ["PENDING", "AWAITING_DATA"].includes(status);
export function ipoWindowOpen(ipo, now = new Date()) {
    if (ipo.status !== "open" || ipo.stale || !ipo.openDate || !ipo.closeDate) return false;
    const parts = Object.fromEntries(new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Kolkata", year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit", hourCycle: "h23" }).formatToParts(now).map(p => [p.type, p.value]));
    const date = `${parts.year}-${parts.month}-${parts.day}`, time = `${parts.hour}:${parts.minute}:${parts.second}`;
    return date >= ipo.openDate && date <= ipo.closeDate && time >= (ipo.dailyStart || "10:00:00") && time <= (ipo.dailyEnd || "17:00:00");
}
export function ipoReservation(ipo, lots) {
    const quantity = Number(lots) * ipo.lotSize;
    if (!Number.isSafeInteger(Number(lots)) || Number(lots) < 1 || !Number.isSafeInteger(quantity) || quantity > 2147483647 || !(ipo.minimumQuantity > 0) || quantity < ipo.minimumQuantity || !(ipo.maximumPrice > 0)) return null;
    return { quantity, amount: quantity * ipo.maximumPrice };
}
