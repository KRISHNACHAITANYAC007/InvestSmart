import { useEffect,useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/api.js";
import { chartPath } from "../api/marketInstruments.js";
import "./Orders.css";
import "../components/PageTabs.css";
import PageTabs from "../components/PageTabs.jsx";
import usePageTab from "../hooks/usePageTab.js";
import ConditionalOrderList from "../components/ConditionalOrderList.jsx";
import OptionsDesk from "../components/OptionsDesk.jsx";
import PaperActivity from "../components/PaperActivity.jsx";
import IpoApplications from "../components/IpoApplications.jsx";

const money=value=>Number(value).toLocaleString("en-IN",{style:"currency",currency:"INR"});
export default function Orders(){
    const tab=usePageTab(["trades","ipos","options"],"trades");
    const [orders,setOrders]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState("");
    const [query,setQuery]=useState(""),[side,setSide]=useState("all"),[status,setStatus]=useState("all"),[revision,setRevision]=useState(0);
    useEffect(()=>{
        const controller=new AbortController();
        async function load(){try{const {data}=await api.get("/orders/me",{signal:controller.signal});if(!controller.signal.aborted){setOrders(data);setError("");}}catch{if(!controller.signal.aborted)setError("Could not refresh orders. Showing the last loaded history.");}finally{if(!controller.signal.aborted)setLoading(false);}}
        void load();const refresh=()=>{void load();};window.addEventListener("investsmart-account-changed",refresh);
        return ()=>{controller.abort();window.removeEventListener("investsmart-account-changed",refresh);};
    },[revision]);
    const filter=query.trim().toLowerCase();
    const visible=[...orders].sort((a,b)=>b.id-a.id).filter(o=>(`${o.companyName} ${o.symbol} ${o.id}`).toLowerCase().includes(filter)&&(side==="all"||o.orderType===side)&&(status==="all"||o.status===status));
    const statuses=[...new Set(orders.map(o=>o.status))];
    if(tab==="options")return <div className="orders-page"><h1>Orders</h1><PageTabs label="Order sections" selected={tab} tabs={[["trades","Trades"],["options","Options"],["ipos","IPO applications"]]} /><OptionsDesk /><PaperActivity /></div>;
    if(tab==="ipos")return <div className="orders-page"><h1>Orders</h1><PageTabs label="Order sections" selected={tab} tabs={[["trades","Trades"],["options","Options"],["ipos","IPO applications"]]} /><IpoApplications /></div>;
    return <div className="orders-page"><header className="page-heading-actions"><div><h1>Orders</h1><p>Your paper-trading history.</p></div><button className="market-refresh-button" onClick={()=>setRevision(n=>n+1)}>Refresh</button></header>
        <PageTabs label="Order sections" selected={tab} tabs={[["trades","Trades"],["options","Options"],["ipos","IPO applications"]]} />
        <PaperActivity /><ConditionalOrderList />
        <div className="orders-filters"><input type="search" aria-label="Search orders" placeholder="Company, symbol or order number" value={query} onChange={e=>setQuery(e.target.value)} />
            <select aria-label="Order side" value={side} onChange={e=>setSide(e.target.value)}><option value="all">All sides</option><option value="BUY">Buy</option><option value="SELL">Sell</option></select>
            <select aria-label="Order status" value={status} onChange={e=>setStatus(e.target.value)}><option value="all">All statuses</option>{statuses.map(s=><option key={s}>{s}</option>)}</select></div>
        {error&&<p role="alert">{error}</p>}{loading?<p>Loading orders…</p>:!visible.length?<div className="orders-empty"><p>{orders.length?"No orders match these filters.":"No paper orders yet."}</p><Link to="/market">Explore Markets</Link></div>:<div className="orders-list">{visible.map(order=><article className="order-card" key={order.id}>
            <div className="order-card-header"><div><h3><Link to={chartPath(order)} state={{companyName:order.companyName,returnTo:"/orders"}}>{order.symbol}</Link></h3><p>{order.companyName} · Order #{order.id}</p></div><span className={order.orderType==="BUY"?"order-type-buy":"order-type-sell"}>{order.orderType}</span></div>
            {order.source === "IPO" && <p>IPO allotment · Already paid from reserved virtual funds</p>}
            <div className="order-details"><div><span>Quantity</span><strong>{order.quantity}</strong></div><div><span>Price</span><strong>{money(order.price)}</strong></div><div><span>Total</span><strong>{money(order.totalAmount)}</strong></div><div><span>Status</span><strong className="order-status">{order.status}</strong></div></div>
        </article>)}</div>}
    </div>;
}
