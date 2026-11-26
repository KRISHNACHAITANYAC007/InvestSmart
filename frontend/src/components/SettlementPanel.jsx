import { useEffect, useState } from "react";
import api from "../api/api.js";
import "./SettlementPanel.css";

const money = value => value == null ? "Unavailable" : Number(value).toLocaleString("en-IN", { style: "currency", currency: "INR" });
export default function SettlementPanel({ holdings = [], funds = false, revision = 0 }) {
    const [data, setData] = useState(null), [error, setError] = useState(""), [page, setPage] = useState(1), [refresh, setRefresh] = useState(0);
    const [loading, setLoading] = useState(true);
    useEffect(() => {
        const controller = new AbortController();
        const timer = setTimeout(() => {
            setLoading(true); setError(""); setData(null);
            Promise.all([api.get("/settlement/summary", { signal: controller.signal }), api.get("/settlement/holdings", { signal: controller.signal }),
                ...(funds ? [api.get("/settlement/history", { params: { page }, signal: controller.signal })] : [])])
                .then(([summary, units, history]) => { if (!controller.signal.aborted) setData({ summary: summary.data, units: units.data, history: history?.data ?? [] }); })
                .catch(failure => { if (!controller.signal.aborted) setError(failure.response?.data?.message || "Settlement information is unavailable. Refresh before relying on these balances."); })
                .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        }, 0);
        return () => { clearTimeout(timer); controller.abort(); };
    }, [funds, page, refresh, revision]);
    return <section className="settlement-panel" aria-label="Paper settlement">
        <header><div><h2>{funds ? "Funds and T+1 settlement" : "Holding settlement"}</h2><p>Educational simulation. Settlement uses reviewed working days, not calendar days.</p></div><button className="ui-button" disabled={loading} onClick={() => setRefresh(value => value + 1)}>Refresh settlement</button></header>
        {loading && <p role="status">Loading settlement information…</p>}
        {error && <p role="alert">{error}</p>}
        {data && <>
            {funds && <><dl className="settlement-balances">{[["Settled cash / loss debt", data.summary.settled], ["Reusable unsettled credit", data.summary.reusable], ["Restricted T+1 credit", data.summary.restricted], ["Reserved funds", data.summary.reserved], ["Paper buying power", data.summary.purchasable], ["Virtual withdrawable", data.summary.withdrawable]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{money(value)}</dd></div>)}</dl>
                <p>{data.summary.simulationCutoff}. Reusable credit can fund eligible delivery purchases; restricted credit cannot be spent or withdrawn. Reservations are shown separately and already excluded from available cash. Negative settled cash represents loss debt.</p>
                <h3>Settlement history</h3>
                <div className="settlement-scroll" role="region" aria-label="Settlement history" tabIndex={0}><table><thead><tr><th>Trade date</th><th>Credit</th><th>Policy</th><th>Status</th><th>Expected working day</th><th>Settled at</th></tr></thead><tbody>{data.history.map(row => <tr key={row.id}><td>{row.tradeDate}</td><td>{money(row.amount)}</td><td>{row.policy}</td><td>{row.status === "PENDING" && !row.cutoffAt ? "Pending — calendar coverage unavailable" : row.status}</td><td>{row.expectedDate || "Unavailable"}</td><td>{row.settledAt ? new Date(row.settledAt).toLocaleString() : "Pending"}</td></tr>)}</tbody></table></div>
                {!data.history.length && <p>No settlement records on this page.</p>}
                <nav aria-label="Settlement history pages"><button className="ui-button" disabled={loading || page <= 1} onClick={() => setPage(value => value - 1)}>Previous</button><span>Page {page}</span><button className="ui-button" disabled={loading || data.history.length < 50} onClick={() => setPage(value => value + 1)}>Next</button></nav></>}
            {!funds && <><p>T+1 units remain unsettled until the reviewed settlement cutoff. Existing owned-quantity and BTST restrictions still apply.</p><div className="settlement-scroll" role="region" aria-label="Settled and T+1 holding units" tabIndex={0}><table><thead><tr><th>Holding</th><th>Settled units</th><th>T+1 units</th></tr></thead><tbody>{data.units.map(row => <tr key={row.stockId}><td>{holdings.find(item => item.stockId === row.stockId)?.symbol || `Stock #${row.stockId}`}</td><td>{row.settledUnits}</td><td>{row.t1Units}</td></tr>)}</tbody></table></div>{!data.units.length && <p>No holding units.</p>}</>}
        </>}
    </section>;
}
