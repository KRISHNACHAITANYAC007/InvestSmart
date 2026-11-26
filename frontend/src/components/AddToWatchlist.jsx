import { useEffect, useState } from "react";
import api from "../api/api.js";
import { companyId, importListing, notifyWatchlistChanged } from "../api/marketInstruments.js";
import ListingPicker from "./ListingPicker.jsx";

export default function AddToWatchlist({ symbol, instrumentKey, isin, onAdded, compact = false }) {
    const [saved, setSaved] = useState(null);
    const [open, setOpen] = useState(false);
    useEffect(() => {
        let active = true;
        const refresh = () => api.get("/watchlist/me").then(({ data }) => {
            if (active) setSaved(data.find(item => companyId(item) === companyId({ symbol, instrumentKey, isin })
                || (!isin && !instrumentKey && item.symbol.toUpperCase() === symbol.toUpperCase())) || null);
        }).catch(() => {});
        refresh(); window.addEventListener("investsmart-watchlist-changed", refresh);
        return () => { active = false; window.removeEventListener("investsmart-watchlist-changed", refresh); };
    }, [symbol, instrumentKey, isin]);
    return <span className="watchlist-add-control" onClick={event => event.stopPropagation()}>
        <button type="button" className="watchlist-quick-add" onClick={event => { event.preventDefault(); setOpen(true); }}
            aria-label={saved ? `Change ${symbol} watchlist listing` : `Add ${symbol} to watchlist`}>
            {compact ? saved ? "Saved" : "Watchlist" : saved ? "\u2713 Saved \u00b7 Edit" : "+ Add to watchlist"}
        </button>
        {open && <ListingPicker symbol={symbol} instrumentKey={saved?.instrumentKey || instrumentKey}
            title={saved ? "Update watchlist listing" : "Add to watchlist"} onClose={() => setOpen(false)} onConfirm={async listing => {
                const stock = await importListing(api, listing);
                await api.post("/watchlist", null, { params: { stockId: stock.id } });
                setSaved(stock); notifyWatchlistChanged(); onAdded?.(stock);
            }} />}
    </span>;
}
