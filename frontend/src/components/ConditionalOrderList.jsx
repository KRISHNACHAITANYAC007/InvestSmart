import { useEffect, useState } from "react";
import api from "../api/api.js";

const money = value => Number(value).toLocaleString("en-IN", { style: "currency", currency: "INR" });
export default function ConditionalOrderList() {
    const [orders,setOrders]=useState([]),[error,setError]=useState(""),[revision,setRevision]=useState(0),[busy,setBusy]=useState(false);
    useEffect(()=>{
        const c=new AbortController();
        const load=()=>api.get("/orders/conditional",{signal:c.signal}).then(({data})=>{if(!c.signal.aborted){setOrders(data);setError("");}}).catch(()=>{if(!c.signal.aborted)setError("Conditional order status unavailable. Refresh before placing a replacement order.");});
        load();const timer=setInterval(load,30000);return()=>{c.abort();clearInterval(timer);};
    },[revision]);
    async function cancel(id){if(busy)return;setBusy(true);try{await api.delete(`/orders/conditional/${encodeURIComponent(id)}`);setRevision(n=>n+1);window.dispatchEvent(new Event("investsmart-account-changed"));}catch(failure){setError(failure.response?.data?.message||"Cancellation could not be confirmed. Refresh status.");}finally{setBusy(false);}}
    return <section className="conditional-orders"><h2>Equity limit &amp; stop-loss orders</h2><p>Day orders expire at 15:30 IST. Filled orders appear in trade history below. Buy cash and sell shares stay reserved while pending.</p>{error&&<p role="alert">{error}</p>}{!orders.length?<p>No conditional equity orders.</p>:orders.map(o=><article className="order-card" key={o.id}><strong>{o.symbol} · {o.side} · {o.order_kind}</strong><p>{o.units} shares · Limit/trigger {money(o.trigger_price)} · Reserved cash {money(o.reserved_cash)} · {o.status}</p>{o.status==="PENDING"&&<button className="ui-button" disabled={busy} onClick={()=>cancel(o.id)}>Cancel pending order</button>}</article>)}</section>;
}
