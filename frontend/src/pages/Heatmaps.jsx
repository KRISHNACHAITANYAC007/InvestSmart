import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { chartPath } from "../api/marketInstruments.js";
import { heatmapLayout } from "../api/heatmapLayout.js";
import api from "../api/api.js";
import "./Heatmaps.css";
const money = value => value > 0 ? Number(value).toLocaleString("en-IN", { style: "currency", currency: "INR" }) : "Price unavailable";
const movement = item => Number(item.currentPrice) > 0 && Number(item.previousClose) > 0 ? (item.currentPrice - item.previousClose) / item.previousClose * 100 : null;
const percent = value => value === null ? "N/A" : `${value > 0 ? "+" : ""}${value.toFixed(2)}%`;
export default function Heatmaps() {
    const [params, setParams] = useSearchParams(), location = useLocation();
    const group = ["indices", "nifty50", "all"].includes(params.get("group")) ? params.get("group") : "nifty50";
    const query = (params.get("query") || "").slice(0, 50), page = group === "all" ? Math.max(1, Number.parseInt(params.get("page"), 10) || 1) : 1;
    const [result, setResult] = useState(null), [error, setError] = useState(""), [loading, setLoading] = useState(true), [retry, setRetry] = useState(0);
    const [selected, setSelected] = useState(null), [size, setSize] = useState({ width: 0, height: 0 }), [fullscreenError, setFullscreenError] = useState("");
    const root = useRef(null), grid = useRef(null);
    const key = `${group}:${query}:${page}`, visible = result?.key === key ? result.data : null;
    const update = values => { const next = new URLSearchParams(params); Object.entries(values).forEach(([k, v]) => v ? next.set(k, v) : next.delete(k)); setParams(next); };
    const load = useCallback(async signal => {
        setLoading(true);
        try { const { data } = await api.get("/market/heatmap", { params: { group, query, page }, signal }); if (!signal.aborted) { setResult({ key, data }); setError(""); } }
        catch (failure) { if (!signal.aborted) setError(failure.response?.data?.message || "Could not refresh the heatmap. Retry to load quotes."); }
        finally { if (!signal.aborted) setLoading(false); }
    }, [group, query, page, key]);
    useEffect(() => {
        const controller = new AbortController(), timer = window.setTimeout(() => load(controller.signal), 250);
        const refresh = window.setInterval(() => { if (!document.hidden) load(controller.signal); }, 60000);
        return () => { controller.abort(); clearTimeout(timer); clearInterval(refresh); };
    }, [load, retry]);
    useEffect(() => {
        const element = root.current, canvas = grid.current;
        const fit = () => {
            const bottom = Math.max(12, parseFloat(getComputedStyle(element.parentElement).paddingBottom) || 0);
            const minimum = Array.from(element.children).filter(child => child !== canvas).reduce((height,child) => height + child.offsetHeight + 10, 180);
            const viewportBottom = window.visualViewport ? window.visualViewport.height + window.visualViewport.offsetTop : window.innerHeight;
            element.style.height = document.fullscreenElement === element ? "100dvh" : `${Math.max(minimum, viewportBottom - element.getBoundingClientRect().top - bottom)}px`;
        };
        const observer = new ResizeObserver(entries => { for (const entry of entries) if (entry.target === canvas) setSize({ width: entry.contentRect.width, height: entry.contentRect.height }); else fit(); });
        for (const child of element.children) observer.observe(child); fit(); window.addEventListener("resize", fit); document.addEventListener("fullscreenchange", fit); window.visualViewport?.addEventListener("resize", fit);
        return () => { observer.disconnect(); window.removeEventListener("resize", fit); document.removeEventListener("fullscreenchange", fit); window.visualViewport?.removeEventListener("resize", fit); };
    }, []);
    const items = visible?.items || [], tiles = heatmapLayout(items.length, size.width, size.height);
    const detail = items.find(i => (i.instrumentKey || i.symbol) === selected);
    async function fullscreen() { try { setFullscreenError(""); if (document.fullscreenElement) await document.exitFullscreen(); else await root.current.requestFullscreen(); } catch { setFullscreenError("Fullscreen is unavailable in this browser."); } }
    return <section className="heatmaps-page" ref={root} aria-label="Market heatmap">
        <header className="heatmaps-heading"><div><h1>Heatmaps <span>Daily change</span></h1></div><div><button onClick={() => setRetry(n => n + 1)} disabled={loading}>{loading ? "Updating…" : "Refresh"}</button><button onClick={fullscreen} aria-label="Toggle heatmap fullscreen">⛶</button></div></header>
        <div className="heatmaps-controls"><div role="group" aria-label="Heatmap universe">{[["indices", "Indices"], ["nifty50", "NIFTY 50"], ["all", "All stocks"]].map(([value, name]) => <button key={value} aria-pressed={group === value} className={group === value ? "active" : ""} onClick={() => update({ group: value, query: "", page: "" })}>{name}</button>)}</div>
            {group !== "indices" && <input type="search" aria-label="Search heatmap stocks" placeholder="Find company or symbol" maxLength={50} value={query} onChange={e => update({ query: e.target.value, page: "" })} />}</div>
        <div className="heatmaps-meta"><span>{visible ? `${items.length} of ${visible.totalItems} instruments · Equal area` : "Loading instruments…"}</span><span className="heatmaps-legend">−4% <i /> 0 <i /> +4%</span></div>
        {(error || fullscreenError) && <div className="heatmaps-error" role="alert">{error || fullscreenError}<button onClick={() => setRetry(n => n + 1)}>Retry</button></div>}
        <div className="heatmaps-grid" ref={grid} aria-busy={loading}>
            {!items.length && <p className="heatmaps-empty">{loading ? "Loading market quotes…" : visible ? "No matching instruments." : "Quotes could not be loaded."}</p>}
            {items.map((item, index) => { const change = movement(item), rect = tiles[index]; if (!rect) return null; const id = item.instrumentKey || item.symbol;
                return <Link key={id} className={`heatmaps-tile ${change === null || change === 0 ? "neutral" : change > 0 ? "gain" : "loss"}`} style={{ ...rect, "--heat-strength": change === null ? 0 : Math.min(Math.abs(change) / 4, 1), "--tile-font": `${Math.max(8, Math.min(23, rect.width / Math.max(6, item.symbol.length) * 1.35, rect.height * .23))}px` }}
                    onClick={event => { if (event.nativeEvent.pointerType === "touch" && selected !== id) { event.preventDefault(); setSelected(id); } }} onPointerEnter={event => { if(event.pointerType !== "touch") setSelected(id); }} onFocus={event => { if(event.currentTarget.matches(":focus-visible")) setSelected(id); }} title={`${item.companyName} · ${money(item.currentPrice)} · ${percent(change)} · Open chart`}
                    aria-label={`${item.companyName}, ${item.symbol}, ${money(item.currentPrice)}, daily change ${change === null ? "unavailable" : percent(change)}. Open chart.`}
                    to={chartPath(item)} state={{ companyName: item.companyName, returnTo: location.pathname + location.search }}><strong>{item.symbol}</strong><b>{percent(change)}</b></Link>;
            })}</div>
        <div className="heatmaps-detail" aria-live="polite">{detail ? <><strong>{detail.companyName}</strong><span>{detail.exchange} · {money(detail.currentPrice)} · {percent(movement(detail))}</span></> : <><strong>Explore the market at a glance</strong><span>Focus or tap a tile for details. Tap it again to open its chart.</span></>}</div>
        <footer className="heatmaps-footer"><span>{visible?.stale ? "Some quotes stale / unavailable · " : ""}Upstox{visible?.updatedAt ? ` · ${new Date(visible.updatedAt).toLocaleTimeString()}` : ""}{visible?.constituentAsOf ? ` · Members ${visible.constituentAsOf}` : ""}</span>
            {group === "all" && visible && <div><button disabled={page <= 1 || loading} onClick={() => update({ page: String(page - 1) })}>‹</button><span>{page} / {visible.totalPages}</span><button disabled={page >= visible.totalPages || loading} onClick={() => update({ page: String(page + 1) })}>›</button></div>}</footer>
    </section>;
}
