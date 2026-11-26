import { useEffect, useState } from "react";
import api from "../api/api.js";

export default function AdminSettlementCalendar() {
    const [coverage, setCoverage] = useState([]), [revision, setRevision] = useState(0), [busy, setBusy] = useState(false);
    const [error, setError] = useState(""), [message, setMessage] = useState(""), [reauth, setReauth] = useState(false);
    useEffect(() => {
        const controller = new AbortController();
        api.get("/admin/operations/settlement-calendars", { signal: controller.signal }).then(({ data }) => {
            if (!controller.signal.aborted) setCoverage(data);
        }).catch(failure => { if (!controller.signal.aborted) setError(failure.response?.data?.message || "Calendar coverage unavailable."); });
        return () => controller.abort();
    }, [revision]);
    async function activate(event) {
        event.preventDefault(); if (busy) return;
        const form = event.currentTarget, fields = new FormData(form);
        setBusy(true); setError(""); setMessage("");
        try {
            if (reauth) {
                await api.post("/users/me/reauthenticate", { password: fields.get("password"), code: fields.get("code") });
                form.elements.password.value = ""; form.elements.code.value = "";
            }
            await api.post("/admin/operations/settlement-calendars", { exchange: fields.get("exchange"), segment: fields.get("segment"), source: fields.get("source"), csv: fields.get("csv"), reason: fields.get("reason"), reviewed: fields.get("reviewed") === "on" });
            setMessage("Reviewed calendar activated and audited. Pending obligations will be rechecked by the bounded settlement worker; resolved outcomes stay unchanged.");
            setReauth(false); form.reset(); setRevision(value => value + 1);
        } catch (failure) {
            if (failure.response?.status === 428) setReauth(true);
            setError(failure.response?.data?.message || "Calendar activation could not be confirmed. Refresh coverage before retrying.");
        } finally { setBusy(false); }
    }
    return <section className="admin-listing-manager"><h3>Reviewed T+1 calendars</h3>
        <p>Every calendar date must be checked against the cited exchange/segment source. Weekdays alone do not determine settlement. Missing coverage keeps settlement pending. Calendar activation changes pending calculations, never settled history.</p>
        {error && <p role="alert" className="account-error">{error}</p>}{message && <p role="status" className="account-notice">{message}</p>}
        <div className="admin-table-scroll" role="region" aria-label="Active settlement coverage" tabIndex={0}><table className="admin-table"><thead><tr><th>Exchange / segment</th><th>Coverage</th><th>Source</th><th>Version / checksum</th></tr></thead><tbody>{coverage.map(row => <tr key={row.versionId}><td>{row.exchange} / {row.segment}</td><td>{row.firstDate} — {row.lastDate}</td><td>{row.source}</td><td>{row.versionId}<br />{row.checksum}</td></tr>)}</tbody></table></div>
        {!coverage.length && <p>No reviewed coverage loaded.</p>}
        <details><summary>Activate an independently reviewed calendar</summary><form onSubmit={activate} className="admin-toolbar">
            <label>Exchange<select name="exchange" disabled={busy}>{["NSE", "BSE", "MCX"].map(value => <option key={value}>{value}</option>)}</select></label>
            <label>Segment<select name="segment" disabled={busy}>{["EQUITY", "FNO", "CURRENCY", "COMMODITY", "MUTUAL_FUND", "BOND"].map(value => <option key={value}>{value}</option>)}</select></label>
            <label>Public source URL<input name="source" type="url" required maxLength={1000} disabled={busy} placeholder="https://…" /></label>
            <label>Reviewed CSV<textarea name="csv" required maxLength={1000000} disabled={busy} rows={8} placeholder="date,trading_open,settlement_open,note" /></label>
            <label>Review reason<input name="reason" required minLength={5} maxLength={300} disabled={busy} /></label>
            <label><input name="reviewed" type="checkbox" required disabled={busy} />I independently reviewed the source and every date, including exceptional trading and settlement closures.</label>
            {reauth && <><label>Admin password<input name="password" type="password" required autoComplete="current-password" disabled={busy} /></label><label>Authenticator code<input name="code" required inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} disabled={busy} /></label></>}
            <button className="ui-button primary" disabled={busy}>{busy ? "Activating…" : "Activate reviewed calendar"}</button>
        </form></details>
    </section>;
}
