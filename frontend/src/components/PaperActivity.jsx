import { useEffect, useState } from "react";
import api from "../api/api.js";
import PaperTicket from "./PaperTicket.jsx";
import "./PaperActivity.css";
const money = value => value == null ? "Unavailable" : Number(value).toLocaleString("en-IN", { style: "currency", currency: "INR" });
const pending = row => ["PENDING", "TRIGGERED", "PART_FILLED"].includes(row.status);

function ActivitySection({ title, description, empty, count, columns, children }) {
    return <section className="paper-activity-section">
        <header className="paper-section-heading"><h3>{title}</h3>{description && <p>{description}</p>}</header>
        {count ? <div role="region" aria-label={title} tabIndex={0} className="paper-table-scroll"><table><caption className="paper-table-caption">{title}</caption><thead><tr>{columns.map(column => <th scope="col" key={column}>{column}</th>)}</tr></thead><tbody>{children}</tbody></table></div>
            : <p className="paper-empty">{empty}</p>}
    </section>;
}
function PositionState({ row }) {
    const state = row.quantity === 0 ? "Closed" : row.close_requested ? "Closure pending" : "Open";
    return <><span className={"paper-state " + (row.quantity === 0 ? "is-closed" : row.close_requested ? "is-pending" : "is-open")}>{state}</span>{row.last_error && <small className="paper-row-error">{row.last_error}</small>}</>;
}

export default function PaperActivity({ compact = false }) {
    const [account, setAccount] = useState(null), [error, setError] = useState(""), [revision, setRevision] = useState(0), [busy, setBusy] = useState(false), [editing, setEditing] = useState(null);
    useEffect(() => {
        const controller = new AbortController(); let sequence = 0;
        async function load() { const own = ++sequence; try { const { data } = await api.get("/orders/paper", { signal: controller.signal }); if (!controller.signal.aborted && own === sequence) { setAccount(data); setError(""); } } catch { if (!controller.signal.aborted && own === sequence) setError("Paper ticket activity unavailable. Refresh to retry."); } }
        const refresh = () => setRevision(value => value + 1); load(); const timer = setInterval(load, 30000); window.addEventListener("investsmart-account-changed", refresh);
        return () => { controller.abort(); clearInterval(timer); window.removeEventListener("investsmart-account-changed", refresh); };
    }, [revision]);
    async function action(path, method) { if (busy) return; setBusy(true); setError(""); try { await api.request({ url: path, method }); window.dispatchEvent(new Event("investsmart-account-changed")); setRevision(value => value + 1); } catch (failure) { setError(failure.response?.data?.message || "Action unavailable. Refresh to check the current state."); } finally { setBusy(false); } }
    async function edit(row) { if (busy) return; setBusy(true); try { if (row.stock_id) setEditing({ row }); else { const { data } = await api.get("/options/chain", { params: { expiry: row.expiry } }); const quote = data.find(item => item.contract.instrumentKey === row.instrument_key); if (!quote) throw new Error("Contract unavailable."); setEditing({ row, quote }); } } catch { setError("Current listing/contract unavailable for replacement."); } finally { setBusy(false); } }
    return <section className={"paper-activity" + (compact ? " is-compact" : "")} aria-label="Paper trading activity">
        <header className="paper-activity-header"><div><h2>{compact ? "Paper funds and positions" : "Paper trading activity"}</h2><p>Track virtual funds, positions and order execution.</p></div><button type="button" disabled={busy} onClick={() => setRevision(value => value + 1)}>Refresh activity</button></header>
        {error && <p className="paper-activity-error" role="alert">{error}</p>}
        {!account ? <p className="paper-empty" role="status">{error ? "Activity could not be loaded. Use Refresh activity to retry." : "Loading paper activity..."}</p> : <>
            <dl className="paper-metrics">{[["Reserved tickets", account.summary.reserved], ["Borrowed principal", account.summary.principal], ["Unpaid interest", account.summary.interest], ["Simulated fees paid", account.summary.fees], ["Financed market value", account.summary.value]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{money(value)}</dd></div>)}</dl>
            <p className="paper-activity-note">All trading and financing is virtual. Stale marks can make valuation unavailable. Debt reduces net portfolio value; financed and intraday positions stay separate from delivery holdings.</p>
            <div className="paper-activity-sections">
                <ActivitySection title="MTF and Cover positions" count={account.positions.length} empty="No MTF or Cover positions yet." columns={["Listing", "Product", "Units", "Principal", "Interest", "State", "Actions"]}>
                    {account.positions.map(row => <tr key={row.id}><td className="paper-listing">{row.symbol}</td><td>{row.product}</td><td>{row.quantity}</td><td>{money(row.principal)}</td><td>{money(row.interest)}</td><td><PositionState row={row} /></td><td><div className="paper-row-actions">{row.quantity > 0 && <><button className="is-sell" type="button" disabled={busy || row.close_requested} onClick={() => setEditing({ row, position: row })}>Sell units</button><button type="button" disabled={busy || row.close_requested} onClick={() => action(`/orders/paper/positions/${encodeURIComponent(row.id)}/close`, "post")}>Request full square-off</button></>}</div></td></tr>)}
                </ActivitySection>
                <ActivitySection title="Intraday / MIS positions" description="2x simulated buying power. Closure attempted from 15:10 IST." count={(account.intradayPositions || []).length} empty="No intraday positions yet." columns={["Listing", "Direction", "Units", "Collateral", "Entry", "Unrealized P&L", "Realized P&L", "State", "Actions"]}>
                    {(account.intradayPositions || []).map(row => <tr key={row.id}><td className="paper-listing">{row.symbol}</td><td><span className={row.direction === "SHORT" ? "paper-side is-sell" : "paper-side is-buy"}>{row.direction}</span></td><td>{row.quantity}</td><td>{money(row.collateral)}</td><td>{money(row.entry_price)}</td><td>{money(row.unrealized_pnl)}</td><td>{money(row.realized_pnl)}</td><td><PositionState row={row} /></td><td><div className="paper-row-actions">{row.quantity > 0 && <><button className={row.direction === "SHORT" ? "is-buy" : "is-sell"} type="button" disabled={busy || row.close_requested} onClick={() => setEditing({ row, position: row })}>{row.direction === "SHORT" ? "Buy to close short" : "Sell to close long"}</button><button type="button" disabled={busy || row.close_requested} onClick={() => action(`/orders/paper/positions/${encodeURIComponent(row.id)}/close`, "post")}>Request full square-off</button></>}</div></td></tr>)}
                </ActivitySection>
                {!compact && <>
                    <ActivitySection title="Tickets and linked exits" count={account.orders.length} empty="No paper tickets or linked exits yet." columns={["Listing", "Mode", "Side / type", "Filled units / legs", "Status", "Reserved / fees", "Actions"]}>
                        {account.orders.map(row => <tr key={row.id}><td className="paper-listing">{row.symbol || row.instrument_key}{row.parent_id && <small>Linked exit</small>}</td><td>{row.product} / {row.variety}</td><td><span className={row.side === "BUY" ? "paper-side is-buy" : "paper-side is-sell"}>{row.side}</span> / {row.order_kind}<small>{row.validity}{row.triggered ? " / Triggered" : ""}</small></td><td>{row.filled_units}/{row.quantity} units<small>{row.filled_legs}/{row.legs} legs</small></td><td><span className={"paper-state " + (pending(row) ? "is-pending" : "is-closed")}>{row.status}</span>{row.last_error && <small className="paper-row-error">{row.last_error}</small>}</td><td>{money(row.reserved_cash)}<small>Fees {money(row.fees)}</small></td><td><div className="paper-row-actions">{pending(row) && <><button type="button" disabled={busy} onClick={() => action(`/orders/paper/${encodeURIComponent(row.id)}`, "delete")}>{row.group_id ? "Cancel linked exits" : "Cancel remaining"}</button>{!row.group_id && row.filled_units === 0 && <button type="button" disabled={busy} onClick={() => edit(row)}>Replace</button>}</>}</div></td></tr>)}
                    </ActivitySection>
                    <ActivitySection title="Executed children and fees" count={account.fills.length} empty="No paper executions yet. Filled orders will appear here." columns={["Ticket", "Leg", "Units", "Price", "Fee"]}>
                        {account.fills.map(row => <tr key={row.id}><td className="paper-ticket-reference">{row.ticket_id}</td><td>{row.leg_number + 1}</td><td>{row.quantity}</td><td>{money(row.price)}</td><td>{money(row.fee)}</td></tr>)}
                    </ActivitySection>
                </>}
            </div>
        </>}{editing && <PaperTicket symbol={editing.row.symbol} instrumentKey={editing.row.stock_id ? editing.row.instrument_key : undefined} optionQuote={editing.quote} initialOrder={editing.position ? undefined : editing.row} financedPosition={editing.position} onClose={() => setEditing(null)} onCompleted={() => setRevision(value => value + 1)} />}
    </section>;
}
