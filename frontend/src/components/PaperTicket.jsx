import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { PAPER_PRODUCTS, styleUnavailable, newProductDraft } from "../api/paperProducts.js";
import { previewFailure, previewIsFresh } from "../api/paperPreview.js";
import { ticketUnits, switchQuantityMode, ticketIntent } from "../api/paperTicket.js";
import api from "../api/api.js";
import { loadTradingListing } from "../api/paperTrading.js";
import "./TradeOrderTicket.css";
import "./PaperTicket.css";

const money = value => value == null ? "Unavailable" : Number(value).toLocaleString("en-IN", { style: "currency", currency: "INR" });
const numeric = value => value === "" || value == null ? null : Number(value);
export default function PaperTicket({ symbol, instrumentKey, initialSide = "buy", optionQuote, initialOrder, financedPosition, onClose, onCompleted }) {
    const dialog = useRef(null), busyRef = useRef(false), previewBlockedUntil = useRef(0);
    const [previewRetryAt, setPreviewRetryAt] = useState(0);
    const [previewRevision, setPreviewRevision] = useState(0), [previewNow, setPreviewNow] = useState(Date.now);
    useEffect(() => { const timer=setInterval(()=>setPreviewNow(Date.now()),1000); return ()=>clearInterval(timer); }, []);
    const [listings, setListings] = useState([]), [selected, setSelected] = useState(instrumentKey || ""), [details, setDetails] = useState(null);
    const [listingPrices, setListingPrices] = useState({}), [liveReference, setLiveReference] = useState(null);
    const [draft, setDraft] = useState(() => ({ product: financedPosition?.product || initialOrder?.product || "CASH", tab: initialOrder?.variety || "REGULAR", quantity: String(initialOrder?.quantity || financedPosition?.quantity || optionQuote?.contract.lotSize || 1), kind: initialOrder?.order_kind || "MARKET", validity: initialOrder?.validity || "DAY", limit: initialOrder?.limit_price ?? "", trigger: initialOrder?.trigger_price ?? "", legs: initialOrder?.legs || 2, stop: initialOrder?.stop_price ?? initialOrder?.stop_percent ?? "", target: initialOrder?.target_price ?? initialOrder?.target_percent ?? "", exitMode: initialOrder?.stop_price != null || initialOrder?.target_price != null ? "PRICE" : "PERCENT", mode: "UNITS", amount: "" }));
    const [requestId, setRequestId] = useState(() => crypto.randomUUID());
    const [estimate, setEstimate] = useState(null), [error, setError] = useState(""), [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false), [success, setSuccess] = useState(null), [dark, setDark] = useState(() => typeof document !== "undefined" && document.querySelector(".terminal-page")?.dataset.theme === "dark"), [advanced, setAdvanced] = useState(false);
    const side = (financedPosition ? financedPosition.product === "MIS" && financedPosition.direction === "SHORT" ? "BUY" : "SELL" : initialOrder?.side || initialSide).toUpperCase(), option = optionQuote != null;
    useEffect(() => { const current = dialog.current, overflow = document.body.style.overflow; current.showModal(); document.body.style.overflow = "hidden"; current.querySelector("h2")?.focus({ preventScroll: true }); current.querySelector(".paper-ticket-body").scrollTop = 0; return () => { current.close(); document.body.style.overflow = overflow; }; }, []);
    useEffect(() => {
        if (option) return undefined;
        const controller = new AbortController();
        api.get("/market/listings", { params: { symbol, instrumentKey }, signal: controller.signal }).then(({ data }) => { if (!controller.signal.aborted) { setListings(data); setSelected(data.find(row => row.instrumentKey === instrumentKey)?.instrumentKey || data.find(row => row.exchange === "NSE")?.instrumentKey || data[0]?.instrumentKey || ""); } }).catch(() => { if (!controller.signal.aborted) setError("Listings unavailable. Close and retry."); });
        return () => controller.abort();
    }, [option, symbol, instrumentKey]);
    useEffect(() => {
        if (option) return undefined;
        const listing = listings.find(row => row.instrumentKey === selected); if (!listing) return undefined;
        const controller = new AbortController();
        loadTradingListing(api, listing, controller.signal).then(data => { if (!controller.signal.aborted) setDetails({ ...data, key: selected }); }).catch(() => { if (!controller.signal.aborted) setError("Selected listing price unavailable. Close and retry."); });
        return () => controller.abort();
    }, [option, selected, listings]);
    useEffect(() => {
        if (option || !listings.length) return undefined;
        const controller = new AbortController(); let inFlight = false;
        async function refresh() {
            if (inFlight) return; inFlight = true;
            const results = await Promise.allSettled(listings.map(row => api.get(`/market/quotes/${encodeURIComponent(row.symbol)}`, { params: { instrumentKey: row.instrumentKey }, signal: controller.signal })));
            if (!controller.signal.aborted) setListingPrices(Object.fromEntries(results.map((result, index) => [listings[index].instrumentKey, result.status === "fulfilled" && result.value.data.instrumentKey === listings[index].instrumentKey ? result.value.data.currentPrice : null])));
            inFlight = false;
        }
        refresh(); const timer = setInterval(refresh, 60000); return () => { controller.abort(); clearInterval(timer); };
    }, [option, listings]);
    const current = details?.key === selected ? details : null;
    const priceKey = option ? optionQuote.contract.instrumentKey : selected;
    const price = liveReference?.key === priceKey ? liveReference.value : option ? side === "BUY" ? optionQuote.ask : optionQuote.bid : current?.quote?.currentPrice, lot = option ? optionQuote.contract.lotSize : 1;
    const quantity = ticketUnits({ ...draft, price, lot });
    const payload = useMemo(() => ({ stockId: option ? null : current?.stock?.id, instrumentKey: option ? optionQuote.contract.instrumentKey : null, expiry: option ? optionQuote.contract.expiry : null, quantity, side, product: draft.product, variety: ["QUICK", "ICEBERG"].includes(draft.tab) ? draft.tab : "REGULAR", orderType: draft.tab === "QUICK" ? "MARKET" : draft.kind, validity: draft.product === "COVER" ? "DAY" : draft.validity, limitPrice: numeric(draft.limit), triggerPrice: numeric(draft.trigger), legs: draft.tab === "ICEBERG" ? Number(draft.legs) : 1, stopLossPercent: draft.exitMode === "PERCENT" ? numeric(draft.stop) : null, targetPercent: draft.product === "COVER" || draft.exitMode !== "PERCENT" ? null : numeric(draft.target), stopLossPrice: draft.exitMode === "PRICE" ? numeric(draft.stop) : null, targetPrice: draft.product === "COVER" || draft.exitMode !== "PRICE" ? null : numeric(draft.target), positionId: financedPosition?.id || initialOrder?.position_id || null, requestId }), [option, current?.stock?.id, optionQuote, quantity, side, draft, initialOrder?.position_id, financedPosition?.id, requestId]);
    const intent = ticketIntent({ side, product: draft.product, closing: !!payload.positionId, option });
    const excessExit = !!financedPosition && quantity > Number(financedPosition.quantity);
    const payloadKey = JSON.stringify(payload);
    useEffect(() => {
        if (success || uncertain || excessExit || (!payload.stockId && !payload.instrumentKey) || !Number.isInteger(payload.quantity) || payload.quantity < 1 || payload.quantity % lot !== 0) return undefined;
        const controller = new AbortController(); let inFlight = false;
        async function refresh() {
            if (inFlight || Date.now() < previewBlockedUntil.current) return; inFlight = true;
            try { const { data } = await api.post("/orders/paper/preview", payload, { signal: controller.signal, timeout: 10000 }); if (!controller.signal.aborted) { setEstimate({ key: payloadKey, value: data }); if (Number(data.referencePrice) > 0) setLiveReference({ key: priceKey, value: data.referencePrice }); } }
            catch (failure) { if (!controller.signal.aborted) { const retry=previewFailure(failure); previewBlockedUntil.current=retry.retryAt; setPreviewRetryAt(retry.retryAt); setEstimate({ key: payloadKey, error: retry.message, retryAt: retry.retryAt }); } }
            finally { inFlight = false; }
        }
        const timer = setTimeout(refresh, 350), interval = setInterval(refresh, 15000);
        return () => { clearTimeout(timer); clearInterval(interval); controller.abort(); };
    }, [payload, payloadKey, lot, priceKey, success, uncertain, previewRevision, excessExit]);
    const preview = estimate?.key === payloadKey ? estimate : null;
    const freshPreview=previewIsFresh(preview?.value,previewNow);
    const retrySeconds=Math.max(0,Math.ceil((previewRetryAt-previewNow)/1000));
    function change(field, value) { setDraft(old => ({ ...old, [field]: value })); setRequestId(crypto.randomUUID()); setError(""); }
    function tab(value) { setDraft(old => ({ ...newProductDraft(old, old.product), tab: value })); setRequestId(crypto.randomUUID()); }
    function close() { if (!busyRef.current) onClose(); }
    async function submit(event) {
        event.preventDefault(); if (busyRef.current || excessExit || !previewIsFresh(preview?.value) || styleUnavailable(draft.product, draft.tab) || uncertain) return;
        busyRef.current = true; setBusy(true); setError("");
        try { const { data } = initialOrder ? await api.put(`/orders/paper/${encodeURIComponent(initialOrder.id)}`, payload) : await api.post("/orders/paper", payload); setSuccess(data); window.dispatchEvent(new Event("investsmart-account-changed")); onCompleted?.(); }
        catch (failure) { if (!failure.response) { setUncertain(true); setError("Confirmation interrupted. Check Orders before submitting another ticket."); } else setError(failure.response.data?.message || "Paper order rejected."); }
        finally { busyRef.current = false; setBusy(false); }
    }
    const input = (field, label, min = "0.01", max) => <label>{label}<input type="number" min={min} max={max} step={field === "legs" ? "1" : "0.01"} value={draft[field]} onChange={event => change(field, event.target.value)} /></label>;
    return <dialog ref={dialog} className={`trade-ticket paper-ticket ${side.toLowerCase()}`} data-paper-theme={dark ? "dark" : "light"} onCancel={event => { event.preventDefault(); close(); }} aria-labelledby="paper-ticket-title"><form onSubmit={submit}>
        <header><div><small>{initialOrder ? "REPLACE PAPER ORDER" : "PAPER ORDER"} · {side}</small><h2 id="paper-ticket-title" tabIndex={-1}>{option ? optionQuote.contract.symbol : listings.find(item => item.instrumentKey === selected)?.companyName || symbol}</h2></div><div className="paper-heading-actions"><button type="button" onClick={() => setDark(value => !value)}>{dark ? "Light" : "Dark"}</button><button type="button" aria-label="Close ticket" disabled={busy} onClick={close}>×</button></div></header>
        <div className="paper-ticket-body">
        {success ? <section role="status"><h3>{success.status.replaceAll("_", " ")}</h3><p>Paper order #{success.id}</p><p>{success.filled_units} of {success.quantity} units filled. Review reservations, linked exits and charges in Orders.</p></section> : <>
            <fieldset className="paper-products" disabled={busy || uncertain || !!financedPosition || !!initialOrder}><legend>Product</legend>{PAPER_PRODUCTS.map(item => <button key={item.value} type="button" aria-pressed={draft.product === item.value} disabled={option && item.value !== "CASH"} onClick={() => { setDraft(old => newProductDraft(old, item.value)); setRequestId(crypto.randomUUID()); setError(""); }}>{option && item.value === "CASH" ? "Normal" : side === "SELL" && !financedPosition && !initialOrder && item.value === "CASH" ? "Sell holdings" : side === "SELL" && !payload.positionId && item.value === "MIS" ? "Sell/Short Intraday" : item.label}</button>)}</fieldset>
            <fieldset className="paper-tabs" disabled={busy || uncertain}><legend>Execution style</legend>{["QUICK", "REGULAR", "ICEBERG"].map(value => <button key={value} type="button" aria-pressed={draft.tab === value} disabled={!!styleUnavailable(draft.product, value)} title={styleUnavailable(draft.product, value) || undefined} onClick={() => tab(value)}>{value[0] + value.slice(1).toLowerCase()}</button>)}</fieldset>
            <p className="paper-product-notice"><strong>{intent}</strong>{financedPosition ? ` ? ${financedPosition.quantity} units remain on this position. Pending exits reduce available units. This order cannot reverse the position.` : !option && side === "SELL" && draft.product === "CASH" ? ". Delivery sells use owned, unreserved shares only. To sell without holdings, explicitly select Sell/Short Intraday above." : ""}</p>
            {excessExit && <p role="alert" className="trade-ticket-error">Exit quantity exceeds this position. Reduce the quantity; no reverse position will be opened.</p>}
            {draft.product === "MIS" && <p className="paper-product-notice">{financedPosition || initialOrder?.position_id ? "Reduce the selected intraday position only." : side === "SELL" ? "Sell/Short: open an equity short position. Sale turnover is not available cash." : "Buy/Long: open an equity intraday position."} 2x simulated buying power with settled collateral and no overnight interest. Closure is attempted from 15:10 IST, subject to fresh prices and an open session. Intraday GTT and Iceberg are unavailable.</p>}
            {["MTF", "COVER"].includes(draft.product) && <p className="paper-product-notice">MTF and Cover support Regular Market/Limit orders with DAY/IOC; Cover requires DAY and a protective stop. Quick and Iceberg are unavailable for these products.</p>}
            {!option && <fieldset className="paper-listings" disabled={busy || uncertain || !!financedPosition || !!initialOrder?.position_id}><legend>Exchange · listing snapshots</legend>{listings.map(listing => <label key={listing.instrumentKey}><input type="radio" checked={selected === listing.instrumentKey} onChange={() => { setSelected(listing.instrumentKey); setRequestId(crypto.randomUUID()); }} />{listing.exchange} · {listing.symbol}<strong>{money(listing.instrumentKey === selected ? price : listingPrices[listing.instrumentKey])}</strong></label>)}</fieldset>}
            <p>{option ? `Expiry ${optionQuote.contract.expiry} · ${lot} units/lot` : "Listing-specific paper execution"} · Reference {money(price)}</p>
            <fieldset className="paper-form-grid" disabled={busy || uncertain}><legend>Entry details</legend><label>Quantity mode<select value={draft.mode} onChange={event => { setDraft(old => switchQuantityMode(old, event.target.value, price, lot)); setRequestId(crypto.randomUUID()); }}><option value="UNITS">{option ? "Units" : "Shares"}</option><option value="AMOUNT">Notional amount</option>{option && <option value="LOTS">Lots</option>}</select></label><label>{draft.mode === "AMOUNT" ? "Notional amount (₹)" : draft.mode === "LOTS" ? "Whole lots" : "Quantity"}<input type="number" min={draft.mode === "AMOUNT" ? "0.01" : "1"} step={draft.mode === "AMOUNT" ? "0.01" : "1"} value={draft.mode === "AMOUNT" ? draft.amount : draft.quantity} required onChange={event => change(draft.mode === "AMOUNT" ? "amount" : "quantity", event.target.value)} /></label>
                {draft.tab !== "QUICK" && <label>Order type<select value={draft.kind} onChange={event => { setDraft(old => ({ ...old, kind: event.target.value, limit: "", trigger: "" })); setRequestId(crypto.randomUUID()); }}><option value="MARKET">Market</option><option value="LIMIT">Limit</option>{!(["MTF", "COVER"].includes(draft.product) || draft.tab === "ICEBERG") && <><option value="STOP_LIMIT">SL · Stop-limit</option><option value="STOP_MARKET">SL-M · Stop-market</option></>}</select></label>}
                {draft.kind.includes("LIMIT") && draft.tab !== "QUICK" && input("limit", "Limit price")}{draft.kind.startsWith("STOP") && draft.tab !== "QUICK" && input("trigger", "Trigger price")}{draft.tab === "ICEBERG" && input("legs", "Sequential legs", "2", "10")}
            </fieldset>
            {side === "BUY" && draft.product !== "MIS" && draft.tab !== "QUICK" && <details className="paper-exit-details" open={draft.product === "COVER"}><summary>{draft.product === "COVER" ? "Required protective exit" : "Add stop-loss / target exits"}</summary><fieldset className="paper-exits" disabled={busy || uncertain}><legend>{draft.product === "COVER" ? "Mandatory protective exit" : "GTT exits after complete entry fill"}</legend><label>Exit values<select value={draft.exitMode} onChange={event => { change("exitMode", event.target.value); setDraft(old => ({ ...old, stop: old.product === "COVER" ? event.target.value === "PRICE" ? (Number(price) * 0.9).toFixed(2) : "10" : "", target: "" })); }}><option value="PERCENT">Percentage of entry fill</option><option value="PRICE">Absolute price</option></select></label><label className="paper-exit-enable"><input type="checkbox" checked={draft.product === "COVER" || draft.stop !== ""} disabled={draft.product === "COVER"} onChange={event => change("stop", event.target.checked ? draft.exitMode === "PRICE" ? (Number(price) * 0.9).toFixed(2) : "10" : "")} />Stop-loss</label>{(draft.product === "COVER" || draft.stop !== "") && input("stop", draft.exitMode === "PRICE" ? "Stop price" : "Stop-loss %", "0.01", draft.exitMode === "PERCENT" ? "99" : undefined)}{draft.product !== "COVER" && <><label className="paper-exit-enable"><input type="checkbox" checked={draft.target !== ""} onChange={event => change("target", event.target.checked ? draft.exitMode === "PRICE" ? (Number(price) * 1.1).toFixed(2) : "10" : "")} />Target</label>{draft.target !== "" && input("target", draft.exitMode === "PRICE" ? "Target price" : "Target %", "0.01", draft.exitMode === "PERCENT" ? "99" : undefined)}</>}<p>Both values create an OCO pair sharing one reservation. Triggering one cancels the other; a limit exit can remain unfilled after a gap. Iceberg exits activate after the parent fully fills.</p></fieldset></details>}
            <button type="button" className="paper-advanced-toggle" aria-expanded={advanced} onClick={() => setAdvanced(value => !value)}>Advanced {advanced ? "−" : "+"}</button>
            {advanced && <section className="paper-assumptions"><label>Validity<select value={draft.validity} disabled={busy || uncertain || draft.product === "COVER"} onChange={event => change("validity", event.target.value)}><option>DAY</option><option>IOC</option>{!(["MIS", "MTF", "COVER"].includes(draft.product) || draft.tab === "ICEBERG") && <option>GTT</option>}</select></label><p>Buy market/SL-M protection: 5% above reference/trigger. SL-limit uses the limit price. IOC fills a whole child or cancels; no partial child fills. GTT expires after 365 days or contract expiry.</p></section>}
            <details className="paper-policy-details"><summary>Simulation rules and charges</summary>
            <p className="paper-assumptions">Virtual funds only. MTF/Cover: up to 2× buying power; MTF interest defaults to 12% yearly on calendar-day closing principal. Simulated fees default to 0.03%, capped at ₹20 per filled order/iceberg child. Statutory taxes excluded. These are simulation settings, not broker rates.</p>
            {draft.product === "COVER" && <p>Stop-market protection and automatic closure attempt at 15:20 IST. Gaps can exceed the stop; missing data leaves closure pending.</p>}{draft.product === "MTF" && <p>Financed positions remain separate from delivery holdings. Below 25% maintenance equity, closure is requested. Gaps may leave negative cash and block new buys.</p>}
            </details>
            {preview?.value && <p className="paper-assumptions">Current policy: {Number(preview.value.annualInterest) * 100}% yearly MTF interest; {Number(preview.value.feeRate) * 100}% simulated fee capped at {money(preview.value.feeCap)} per filled child. Taxes excluded.</p>}<dl className="paper-preview">{[["Units", Number.isFinite(quantity) ? quantity : "Invalid"], ["Required cash", money(preview?.value?.requiredCash)], ["Available", money(preview?.value?.availableCash)], ["Estimated fees", money(preview?.value?.estimatedFees)], ["Buying power", preview?.value ? `${preview.value.leverage}×` : "Unavailable"]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl><p className="paper-assumptions">Estimates expire after 30 seconds. Submission rechecks price and funds.</p>
            {!preview && <p role="status">Complete the fields for a fresh server estimate.</p>}{preview?.error && <div className="paper-preview-error"><p role="alert" className="trade-ticket-error">{preview.error}</p><button type="button" disabled={busy || retrySeconds>0} onClick={()=>setPreviewRevision(value=>value+1)}>{retrySeconds>0 ? `Retry in ${retrySeconds}s` : "Retry estimate"}</button></div>}{preview?.value && !freshPreview && <p role="status">Estimate expired. Waiting for a fresh estimate before submission.</p>}
        </>}{error && <p role="alert" className="trade-ticket-error">{error}</p>}
        </div>
        <footer><Link to="/orders" onClick={event => { if (busyRef.current) event.preventDefault(); else close(); }}>View orders</Link><button type="button" disabled={busy} onClick={close}>{success || uncertain ? "Done" : "Cancel"}</button>{!success && !uncertain && <button className="trade-ticket-submit" disabled={busy || excessExit || !!styleUnavailable(draft.product, draft.tab) || !freshPreview || !Number.isInteger(quantity) || quantity < 1}>{busy ? "Submitting…" : `${initialOrder ? "Replace" : intent} · ${quantity || 0}`}</button>}</footer>
    </form></dialog>;
}
