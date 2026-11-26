import { useEffect, useState } from "react";
import { Link, Navigate, useLocation, useParams } from "react-router-dom";
import api from "../api/api.js";
import { chartPath } from "../api/marketInstruments.js";
import { legacyDestination } from "../api/navigation.js";

export default function LegacyRedirect() {
    const { pathname, search } = useLocation();
    return <Navigate replace to={legacyDestination(pathname,search)} />;
}
export function StockChartRedirect() {
    const { id } = useParams();
    const [result,setResult] = useState(null);
    const [retry,setRetry] = useState(0);
    const { search } = useLocation();
    useEffect(() => {
        const controller = new AbortController();
        api.get(`/stocks/${id}`,{signal:controller.signal}).then(({data}) => {
            if (!controller.signal.aborted) setResult({id,stock:data});
        }).catch(() => { if (!controller.signal.aborted) setResult({id,error:true}); });
        return () => controller.abort();
    },[id,retry]);
    if (result?.id !== id) return <p>Opening chart…</p>;
    if (result.error) return <div role="alert"><p>Unable to open this stock.</p><button onClick={() => setRetry(n=>n+1)}>Retry</button> <Link to="/market">Markets</Link></div>;
    const destination = new URL(chartPath(result.stock),"https://local.invalid");
    for (const [key,value] of new URLSearchParams(search)) if (key !== "instrumentKey") destination.searchParams.set(key,value);
    return <Navigate replace to={destination.pathname+destination.search} state={{companyName:result.stock.companyName}} />;
}
