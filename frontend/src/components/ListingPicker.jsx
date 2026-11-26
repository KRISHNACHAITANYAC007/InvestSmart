import { useEffect, useRef, useState } from "react";
import api from "../api/api.js";
import "./ListingPicker.css";

export default function ListingPicker({ symbol, instrumentKey, title = "Add to watchlist", onConfirm, onClose }) {
    const dialog = useRef(null);
    const [listings, setListings] = useState(null);
    const [selected, setSelected] = useState("");
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);
    const [retry, setRetry] = useState(0);
    useEffect(() => { dialog.current.showModal(); }, []);
    useEffect(() => {
        const controller = new AbortController();
        async function load() {
            try {
                const { data } = await api.get("/market/listings", { params: { symbol, instrumentKey: instrumentKey || undefined }, signal: controller.signal });
                if (!Array.isArray(data) || !data.length) throw new Error("No available listings.");
                const quotes = await Promise.allSettled(data.map(i => api.get(`/market/quotes/${encodeURIComponent(i.symbol)}`, { params: { instrumentKey: i.instrumentKey }, signal: controller.signal })));
                if (controller.signal.aborted) return;
                setListings(data.map((i, n) => ({ ...i, quote: quotes[n].status === "fulfilled" ? quotes[n].value.data : null })));
                setSelected((data.find(i => i.instrumentKey === instrumentKey) || data.find(i => i.exchange === "NSE") || data[0]).instrumentKey);
            } catch (failure) {
                if (!controller.signal.aborted) setError(failure.response?.data?.message || failure.message);
            }
        }
        void load(); return () => controller.abort();
    }, [symbol, instrumentKey, retry]);
    async function confirm(event) {
        event.preventDefault(); if (saving || !selected) return;
        setSaving(true); setError("");
        try { await onConfirm(listings.find(i => i.instrumentKey === selected)); onClose(); }
        catch (failure) { setError(failure.response?.data?.message || failure.message || "Unable to save listing."); }
        finally { setSaving(false); }
    }
    return <dialog ref={dialog} className="listing-dialog" onCancel={e => { e.preventDefault(); if (!saving) onClose(); }}>
        <form onSubmit={confirm}>
            <header><div><small>CHOOSE EXCHANGE</small><h2>{title}</h2></div><button type="button" onClick={onClose} disabled={saving} aria-label="Close listing selection">×</button></header>
            <h3>{listings?.[0]?.companyName || symbol}</h3><p>Each exchange has its own price. Choose the listing to follow or trade.</p>
            {!listings && !error && <p role="status">Loading listings and prices…</p>}
            <fieldset disabled={saving}><legend>Available listings</legend>{listings?.map(i => <label className={selected === i.instrumentKey ? "selected" : ""} key={i.instrumentKey}>
                <input type="radio" name="listing" checked={selected === i.instrumentKey} onChange={() => setSelected(i.instrumentKey)} />
                <span><strong>{i.exchange}</strong><small>{i.symbol}</small></span>
                <span className="listing-price"><strong>{Number(i.quote?.currentPrice) > 0 ? Number(i.quote.currentPrice).toLocaleString("en-IN", { style: "currency", currency: "INR" }) : "Price unavailable"}</strong>
                    <small>{i.quote?.lastUpdated ? `As of ${new Date(i.quote.lastUpdated).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}` : "Quote unavailable"}</small></span>
            </label>)}</fieldset>
            {error && <p className="listing-error" role="alert">{error}</p>}
            {!listings && error && <button type="button" onClick={() => { setError(""); setRetry(n => n + 1); }}>Retry</button>}
            <footer><button type="button" disabled={saving} onClick={onClose}>Cancel</button><button className="listing-confirm" disabled={!selected || saving}>{saving ? "Saving…" : "Confirm listing"}</button></footer>
        </form>
    </dialog>;
}
