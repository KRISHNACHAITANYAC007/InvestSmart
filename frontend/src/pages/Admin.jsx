import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import api from "../api/api.js";
import AdminSettlementCalendar from "../components/AdminSettlementCalendar.jsx";
import "./Account.css";
import "./Admin.css";

const tabs = [["overview", "Overview"], ["users", "Users"], ["security", "Security"], ["operations", "Operations"], ["audit", "Audit"]];
const date = value => value ? new Date(value).toLocaleString() : "Unavailable";
const money = value => value == null ? "Unavailable" : Number(value).toLocaleString("en-IN", { style: "currency", currency: "INR" });
function Pagination({ result, page, setPage, loading }) { return result && <div className="admin-pagination"><span>{result.totalItems} results</span><button className="ui-button" disabled={loading || page <= 1} onClick={() => setPage(page - 1)}>Previous</button><span>{page} / {result.totalPages}</span><button className="ui-button" disabled={loading || page >= result.totalPages} onClick={() => setPage(page + 1)}>Next</button></div>; }
function Table({ columns, rows, empty = "No records to show." }) { return rows?.length ? <div className="admin-table-scroll" role="region" aria-label="Admin records" tabIndex={0}><table className="admin-table"><thead><tr>{columns.map(([key, label]) => <th key={key}>{label}</th>)}</tr></thead><tbody>{rows.map((row, n) => <tr key={row.id ?? n}>{columns.map(([key, , render]) => <td key={key}>{render ? render(row) : String(row[key] ?? "—")}</td>)}</tr>)}</tbody></table></div> : <p className="account-muted">{empty}</p>; }
const activityColumns = [["occurred_at", "Time", row => date(row.occurred_at)], ["event_type", "Event"], ["source_label", "Source"], ["details", "Details"]];

function ListingManager() {
    const [type, setType] = useState("IPO"), [status, setStatus] = useState("open"), [category, setCategory] = useState("all");
    const [query, setQuery] = useState(""), [page, setPage] = useState(1), [revision, setRevision] = useState(0);
    const [items, setItems] = useState(null), [selected, setSelected] = useState(null), [available, setAvailable] = useState(true);
    const [dateOverride, setDateOverride] = useState(false), [dates, setDates] = useState({ openDate: "", closeDate: "", allotmentDate: "", listingDate: "" });
    const [reason, setReason] = useState(""), [reauth, setReauth] = useState(false), [busy, setBusy] = useState(false);
    const [error, setError] = useState(""), [message, setMessage] = useState("");

    useEffect(() => {
        const controller = new AbortController();
        const endpoint = type === "IPO" ? "/admin/operations/listings/ipos" : "/admin/operations/listings/bonds";
        const params = type === "IPO" ? { status, issueType: "all", page } : { category, query, page };
        api.get(endpoint, { params, signal: controller.signal }).then(({ data }) => {
            if (!controller.signal.aborted) { setItems(data); setError(""); }
        }).catch(failure => {
            if (!controller.signal.aborted) setError(failure.response?.data?.message || "Provider listings could not be loaded.");
        });
        return () => controller.abort();
    }, [type, status, category, query, page, revision]);

    function choose(item) {
        setSelected(item);
        setAvailable(Boolean(item.available));
        const hasDates = type === "IPO" && Boolean(item.datesOverridden);
        setDateOverride(hasDates);
        setDates({ openDate: item.openDate || "", closeDate: item.closeDate || "", allotmentDate: item.allotmentDate || "", listingDate: item.listingDate || "" });
        setReason("");
        setReauth(false);
        setError("");
        setMessage("");
    }

    async function update(event) {
        event.preventDefault();
        if (!selected || busy) return;
        const form = event.currentTarget;
        setBusy(true);
        setError("");
        try {
            if (reauth) {
                const credentials = new FormData(form);
                await api.post("/users/me/reauthenticate", { password: credentials.get("password"), code: credentials.get("code") });
                form.elements.password.value = "";
                form.elements.code.value = "";
            }
            await api.post("/admin/operations/listings", {
                type,
                referenceId: type === "IPO" ? selected.id : selected.instrumentKey,
                available,
                ...(type === "IPO" && dateOverride ? dates : { openDate: null, closeDate: null, allotmentDate: null, listingDate: null }),
                reason
            });
            setMessage("Listing override saved and recorded in Audit.");
            setReauth(false);
            setReason("");
            setSelected(null);
            setItems(null);
            setRevision(value => value + 1);
        } catch (failure) {
            if (failure.response?.status === 428) {
                setReauth(true);
                setError("Confirm your Admin password and a fresh authenticator code to change listing settings.");
            } else setError(failure.response?.data?.message || "Listing settings could not be saved.");
        } finally {
            setBusy(false);
        }
    }

    return <section className="admin-listing-manager">
        <h3>IPO and bond listings</h3>
        <p className="account-muted">Provider catalogs remain the source of listing identity and terms. Admin overrides only control availability and (for IPOs) the full validated timeline. Every change requires recent Admin authentication and is recorded in Audit.</p>
        <div className="admin-toolbar">
            <label>Asset type<select value={type} onChange={event => { setType(event.target.value); setSelected(null); setItems(null); setPage(1); setError(""); setMessage(""); }}>{["IPO", "BOND"].map(value => <option key={value}>{value}</option>)}</select></label>
            {type === "IPO" ? <label>Provider status<select value={status} onChange={event => { setStatus(event.target.value); setSelected(null); setItems(null); setPage(1); }}>{["open", "upcoming", "closed", "listed"].map(value => <option key={value}>{value}</option>)}</select></label> :
                <><label>Category<select value={category} onChange={event => { setCategory(event.target.value); setSelected(null); setItems(null); setPage(1); }}>{["all", "government", "corporate"].map(value => <option key={value}>{value}</option>)}</select></label><label>Search bonds<input type="search" maxLength={80} value={query} onChange={event => { setQuery(event.target.value); setSelected(null); setItems(null); setPage(1); }} placeholder="Name, symbol or ISIN" /></label></>}
        </div>
        {error && <p className="account-error" role="alert">{error}</p>}
        {message && <p className="account-notice" role="status">{message}</p>}
        <Table rows={items?.items} columns={[
            ["name", "Listing", row => row.name || row.symbol || row.isin],
            [type === "IPO" ? "id" : "instrumentKey", "Provider ID"],
            [type === "IPO" ? "status" : "category", "Provider status / type"],
            ["available", "Available", row => row.available ? "Yes" : "No"],
            ["edit", "Manage", row => <button className="admin-manage-user" type="button" onClick={() => choose(row)}>Edit</button>]
        ]} empty="No provider listings match these filters." />
        {items && <Pagination result={items} page={page} setPage={setPage} loading={busy} />}
        {selected && <form className="admin-review" onSubmit={update}>
            <h4>Manage {selected.name || selected.symbol || selected.isin}</h4>
            <p className="account-muted">Listing ID: {type === "IPO" ? selected.id : selected.instrumentKey}. Provider data is not edited or deleted.</p>
            <label><span>Available in InvestSmart</span><input type="checkbox" checked={available} onChange={event => setAvailable(event.target.checked)} /></label>
            {type === "IPO" && <><label><span>Override the full IPO timeline</span><input type="checkbox" checked={dateOverride} onChange={event => setDateOverride(event.target.checked)} /></label>
                {dateOverride && <div className="admin-listing-dates">{[["openDate", "Application opens"], ["closeDate", "Application closes"], ["allotmentDate", "Allotment"], ["listingDate", "Listing"]].map(([key, label]) => <label key={key}>{label}<input type="date" required value={dates[key]} onChange={event => setDates(value => ({ ...value, [key]: event.target.value }))} /></label>)}</div>}
                <p className="account-muted">All four dates are required when overriding. They must be ordered open → close → allotment → listing. Disable the override to return to provider dates.</p>
            </>}
            <label>Reason<textarea required minLength={5} maxLength={1000} value={reason} onChange={event => setReason(event.target.value)} /></label>
            {reauth && <><label>Your password<input name="password" type="password" autoComplete="current-password" required maxLength={100} /></label><label>Fresh authenticator or recovery code<input name="code" autoComplete="one-time-code" required maxLength={100} /></label></>}
            <button className="ui-button primary" disabled={busy || reason.trim().length < 5}>{busy ? "Saving…" : "Save audited listing change"}</button>
            <button className="ui-button" type="button" disabled={busy} onClick={() => setSelected(null)}>Cancel</button>
        </form>}
    </section>;
}

function SettlementManager({ applications, onUpdated }) {
    const [selected, setSelected] = useState(null), [reason, setReason] = useState("");
    const [reauth, setReauth] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState(""), [message, setMessage] = useState("");
    async function retry(event) {
        event.preventDefault();
        if (!selected || busy) return;
        const form = event.currentTarget;
        setBusy(true);
        setError("");
        try {
            if (reauth) {
                const credentials = new FormData(form);
                await api.post("/users/me/reauthenticate", { password: credentials.get("password"), code: credentials.get("code") });
                form.elements.password.value = "";
                form.elements.code.value = "";
            }
            const { data } = await api.post(`/admin/operations/ipo-applications/${encodeURIComponent(selected.id)}/settlement-retry`, { reason });
            setMessage(`Application #${data.id} processed with fresh provider data; current status: ${data.status}.`);
            setSelected(null);
            setReason("");
            setReauth(false);
            onUpdated();
        } catch (failure) {
            if (failure.response?.status === 428) {
                setReauth(true);
                setError("Confirm your Admin password and a fresh authenticator code to retry settlement.");
            } else setError(failure.response?.data?.message || "Settlement could not be retried.");
        } finally {
            setBusy(false);
        }
    }
    return <section className="admin-settlement-manager">
        <p className="account-muted">Pending applications can be retried only after the published allotment time. Retry uses fresh provider data and the existing transactional settlement rules; resolved outcomes and balances cannot be manually edited.</p>
        <Table rows={applications} columns={[
            ["name", "IPO"], ["user_email", "User email"], ["status", "Settlement status"],
            ["allotment_date", "Allotment date", row => row.allotment_date || "Unavailable"],
            ["reserved_amount", "Reserved funds", row => money(row.reserved_amount)],
            ["action", "Action", row => ["PENDING", "AWAITING_DATA"].includes(row.status) ?
                <button className="admin-manage-user" type="button" onClick={() => { setSelected(row); setReason(""); setReauth(false); setError(""); setMessage(""); }}>Retry</button> : "Resolved"]
        ]} empty="No IPO applications recorded." />
        {message && <p className="account-notice" role="status">{message}</p>}
        {error && <p className="account-error" role="alert">{error}</p>}
        {selected && <form className="admin-review" onSubmit={retry}>
            <h4>Retry application #{selected.id} settlement</h4>
            <p className="account-muted">{selected.name} · {selected.user_email || `Account #${selected.user_id}`} · {selected.status}</p>
            <label>Reason<textarea required minLength={5} maxLength={1000} value={reason} onChange={event => setReason(event.target.value)} /></label>
            {reauth && <><label>Your password<input name="password" type="password" autoComplete="current-password" required maxLength={100} /></label><label>Fresh authenticator or recovery code<input name="code" autoComplete="one-time-code" required maxLength={100} /></label></>}
            <button className="ui-button primary" disabled={busy || reason.trim().length < 5}>{busy ? "Retrying…" : "Confirm guarded settlement retry"}</button>
            <button className="ui-button" type="button" disabled={busy} onClick={() => setSelected(null)}>Cancel</button>
        </form>}
    </section>;
}

export default function Admin() {
    const [params, setParams] = useSearchParams();
    const requestedTab = params.get("tab"), tab = requestedTab === "alerts" ? "security" : tabs.some(([key]) => key === requestedTab) ? requestedTab : "overview", selected = params.get("id");
    const [overview, setOverview] = useState(null), [result, setResult] = useState(null), [detail, setDetail] = useState(null), [query, setQuery] = useState(""), [filter, setFilter] = useState(() => ["alerts", "security"].includes(requestedTab) ? "OPEN" : "ALL"), [page, setPage] = useState(1), [revision, setRevision] = useState(0), [loading, setLoading] = useState(true), [busy, setBusy] = useState(false), [error, setError] = useState(""), [message, setMessage] = useState("");
    const [action, setAction] = useState("REVOKE_SESSIONS"), [reason, setReason] = useState(""), [reauth, setReauth] = useState(false), [deleteConfirmation, setDeleteConfirmation] = useState(""), [deleteReason, setDeleteReason] = useState(""), [deleteReauth, setDeleteReauth] = useState(false), [reviewStatus, setReviewStatus] = useState("INVESTIGATING"), [notes, setNotes] = useState(""), [reviewReauth, setReviewReauth] = useState(false);
    const [sessionTarget, setSessionTarget] = useState(""), [sessionReason, setSessionReason] = useState(""), [sessionReauth, setSessionReauth] = useState(false), [balanceReauth, setBalanceReauth] = useState(false);
    const detailHasEmail = Boolean(detail?.user?.email?.trim?.());
    const deleteConfirmationMatches = detailHasEmail
        ? deleteConfirmation.trim().toLowerCase() === detail.user.email.toLowerCase()
        : deleteConfirmation.trim().toUpperCase() === `DELETE ${selected}`;
    function select(id) { const next = new URLSearchParams(params); if (id) next.set("id", id); else next.delete("id"); setParams(next); setMessage(""); setError(""); setAction("REVOKE_SESSIONS"); setReason(""); setReauth(false); setDeleteConfirmation(""); setDeleteReason(""); setDeleteReauth(false); setNotes(""); setReviewReauth(false); setSessionTarget(""); setSessionReauth(false); setBalanceReauth(false); }
    function requestSessionRevoke(id) { setSessionTarget(id); setSessionReason(""); setSessionReauth(false); setError(""); }
    useEffect(() => {
        if (!["users", "security", "operations"].includes(tab) || selected) return undefined;
        const timer = setInterval(() => setRevision(value => value + 1), 30000);
        return () => clearInterval(timer);
    }, [tab, selected]);
    useEffect(() => { const c = new AbortController(); async function refresh() { try { const { data } = await api.get("/admin/overview", { signal: c.signal }); if (!c.signal.aborted) setOverview(data); } catch { /* The content request reports errors separately. */ } } void refresh(); const timer = setInterval(refresh, 30000); return () => { c.abort(); clearInterval(timer); }; }, [revision]);
    useEffect(() => {
        const c = new AbortController();
        const timer = setTimeout(async () => {
            setLoading(true);
            setError("");
            setDetail(null);
            setResult(null);
            try {
                if (tab === "overview") {
                    const { data } = await api.get("/admin/overview", { signal: c.signal });
                    if (!c.signal.aborted) setOverview(data);
                } else {
                    const path = selected && tab === "users" ? `/admin/users/${encodeURIComponent(selected)}` : tab === "security" ? selected ? `/admin/alerts/${encodeURIComponent(selected)}` : "/admin/alerts" : `/admin/${tab}`;
                    const { data } = await api.get(path, { signal: c.signal, params: { query, status: filter, page } });
                    if (!c.signal.aborted) {
                        if (selected) {
                            setDetail(data);
                            if (tab === "security") {
                                setReviewStatus(data.status);
                                setNotes(data.notes || "");
                            }
                        } else {
                            setResult(data);
                            if (tab === "users" && !query.trim() && filter === "ALL") {
                                setOverview(current => current ? { ...current, users: data.totalItems } : current);
                            }
                        }
                    }
                }
            } catch (failure) {
                if (!c.signal.aborted) setError(failure.response?.data?.message || "Admin information could not be loaded.");
            } finally {
                if (!c.signal.aborted) setLoading(false);
            }
        }, 250);
        return () => { c.abort(); clearTimeout(timer); };
    }, [tab, selected, query, filter, page, revision]);
    async function accountAction(event) { event.preventDefault(); if (busy) return; const form = event.currentTarget; setBusy(true); setError(""); try { if (reauth) { const data = new FormData(form); await api.post("/users/me/reauthenticate", { password: data.get("password"), code: data.get("code") }); form.elements.password.value = ""; form.elements.code.value = ""; } if (action === "REQUIRE_PASSWORD_RESET") { const { data } = await api.post(`/admin/users/${encodeURIComponent(selected)}/password-reset`, { reason }); setMessage(data.message); } else if (action === "RESEND_VERIFICATION") { const { data } = await api.post(`/admin/users/${encodeURIComponent(selected)}/resend-verification`); setMessage(data.message); } else { await api.post(`/admin/users/${encodeURIComponent(selected)}/actions`, { action, reason }); setMessage("Account action recorded. All sessions for this user have been revoked."); } setReauth(false); setReason(""); setRevision(n => n + 1); } catch (failure) { if (failure.response?.status === 428) { setReauth(true); setError("Confirm your password and a fresh authenticator code to continue."); } else setError(failure.response?.data?.message || "Action could not be completed. Refresh the account before retrying."); } finally { setBusy(false); } }
    async function deleteUser(event) { event.preventDefault(); if (busy) return; const form = event.currentTarget; const hadEmail = Boolean(detail?.user?.email?.trim()); setBusy(true); setError(""); try { if (deleteReauth) { const data = new FormData(form); await api.post("/users/me/reauthenticate", { password: data.get("password"), code: data.get("code") }); form.elements.password.value = ""; form.elements.code.value = ""; } const { data } = await api.delete(`/admin/users/${encodeURIComponent(selected)}`, { data: { confirmation: deleteConfirmation, reason: deleteReason.trim() } }); setOverview(data); select(null); setMessage(hadEmail ? "User permanently deleted. Their alerts were resolved and marked reviewed. A notification email was queued; inbox delivery is not confirmed." : "Account permanently deleted. No notification email was sent because the account had no email address."); setRevision(n => n + 1); } catch (failure) { if (failure.response?.status === 428) { setDeleteReauth(true); setError("Confirm your password and a fresh authenticator code to permanently delete this user."); } else setError(failure.response?.data?.message || "User could not be deleted. Refresh the account before retrying."); } finally { setBusy(false); } }
    async function review(event) { event.preventDefault(); if (busy) return; const form = event.currentTarget; setBusy(true); setError(""); try { if (reviewReauth) { const data = new FormData(form); await api.post("/users/me/reauthenticate", { password: data.get("password"), code: data.get("code") }); form.elements.password.value = ""; form.elements.code.value = ""; } await api.post(`/admin/alerts/${encodeURIComponent(selected)}/review`, { status: reviewStatus, notes }); setMessage("Review saved to the audit history."); setReviewReauth(false); setRevision(n => n + 1); } catch (failure) { if (failure.response?.status === 428) { setReviewReauth(true); setError("Confirm your password and a fresh authenticator code to save this review."); } else setError(failure.response?.data?.message || "Review could not be saved."); } finally { setBusy(false); } }
    async function revokeSession(event) { event.preventDefault(); if (busy || !sessionTarget) return; const form = event.currentTarget; setBusy(true); setError(""); try { if (sessionReauth) { const data = new FormData(form); await api.post("/users/me/reauthenticate", { password: data.get("password"), code: data.get("code") }); form.elements.password.value = ""; form.elements.code.value = ""; } await api.delete(`/admin/users/${encodeURIComponent(selected)}/sessions/${encodeURIComponent(sessionTarget)}`, { data: { reason: sessionReason } }); setMessage("Session revoked and recorded in the audit history."); setSessionTarget(""); setSessionReauth(false); setSessionReason(""); setRevision(n => n + 1); } catch (failure) { if (failure.response?.status === 428) { setSessionReauth(true); setError("Confirm your password and a fresh authenticator code to revoke this session."); } else setError(failure.response?.data?.message || "Session could not be revoked."); } finally { setBusy(false); } }
    async function adjustBalance(event) { event.preventDefault(); if (busy) return; const form = event.currentTarget; const data = new FormData(form); setBusy(true); setError(""); try { if (balanceReauth) { await api.post("/users/me/reauthenticate", { password: data.get("password"), code: data.get("code") }); form.elements.password.value = ""; form.elements.code.value = ""; } await api.post(`/admin/users/${encodeURIComponent(selected)}/balance-adjustments`, { amount: data.get("amount"), reason: data.get("reason") }); setMessage("Balance adjustment recorded with its reason and resulting balance."); setBalanceReauth(false); form.reset(); setRevision(n => n + 1); } catch (failure) { if (failure.response?.status === 428) { setBalanceReauth(true); setError("Confirm your password and a fresh authenticator code to adjust the balance."); } else setError(failure.response?.data?.message || "Balance adjustment could not be recorded."); } finally { setBusy(false); } }
    return <div className="admin-page"><h1>Administration</h1><p className="account-muted">Review accounts, investigate alerts and manage access.</p>
        <nav className="admin-tabs" aria-label="Admin sections">{tabs.map(([key, label]) => <Link key={key} className={tab === key ? "active" : ""} aria-current={tab === key ? "page" : undefined} to={`/admin?tab=${key}`} onClick={() => { setPage(1); setFilter(key === "security" ? "OPEN" : "ALL"); setQuery(""); setMessage(""); }}>{label}{key === "security" && overview?.unreadAlerts > 0 && <span className="admin-badge">{overview.unreadAlerts}</span>}</Link>)}</nav>
        {error && <p className="account-error" role="alert">{error} <button className="ui-button" onClick={() => setRevision(n => n + 1)}>Refresh</button></p>}{message && <p className="account-notice" role="status">{message}</p>}
        {tab === "overview" && (loading && !overview ? <p className="account-muted" role="status">Loading administration overview…</p> : overview && <section className="admin-overview">
            <header className="admin-overview-header">
                <div><span className="admin-overview-kicker">OWNER CONSOLE</span><h2>Platform overview</h2><p>Review account growth, security alerts and service operations.</p></div>
                <span className="admin-overview-refresh">Updates automatically every 30 seconds</span>
            </header>
            <div className="admin-metrics admin-overview-metrics">
                <Link to="/admin?tab=users" className="admin-metric"><span>Registered users</span><strong>{overview.users}</strong><small>Review user accounts</small></Link>
                <Link to="/admin?tab=users" className="admin-metric"><span>Verified users</span><strong>{overview.verifiedUsers}</strong><small>Email verification complete</small></Link>
                <Link to="/admin?tab=users" className={`admin-metric${overview.suspended ? " attention" : ""}`}><span>Suspended accounts</span><strong>{overview.suspended}</strong><small>{overview.suspended ? "Review account status" : "No suspended accounts"}</small></Link>
                <Link to="/admin?tab=users" className="admin-metric"><span>Recent registrations</span><strong>{overview.recentRegistrations}</strong><small>Last 7 days</small></Link>
                <Link to="/admin?tab=security" className={`admin-metric${overview.openAlerts ? " attention" : ""}`}><span>Unresolved alerts</span><strong>{overview.openAlerts}</strong><small>{overview.unreadAlerts} unread · human review required</small></Link>
                <div className={`admin-metric admin-mail-metric${overview.pendingMail ? " attention" : ""}`}><span>Email queue</span><strong>{overview.pendingMail}</strong><small>{overview.pendingMail ? "Awaiting delivery" : "No pending messages"}</small></div>
            </div>
            {overview.pendingMail > 0 && <p className="account-notice admin-mail-notice">{overview.pendingMail} account email(s) are awaiting delivery. This queue count does not confirm SMTP delivery or inbox receipt.</p>}
            <section className="admin-workspace-links" aria-labelledby="admin-workspace-title">
                <div><h3 id="admin-workspace-title">Review workspace</h3><p>Choose an area to investigate or review recorded administrative actions.</p></div>
                <div className="admin-workspace-actions">
                    <Link to="/admin?tab=users">Review users <span aria-hidden="true">→</span></Link>
                    <Link to="/admin?tab=security">Investigate alerts <span aria-hidden="true">→</span></Link>
                    <Link to="/admin?tab=operations">Check service health <span aria-hidden="true">→</span></Link>
                    <Link to="/admin?tab=audit">View audit history <span aria-hidden="true">→</span></Link>
                </div>
            </section>
            <p className="admin-overview-footnote">Security alerts are rule-based signals, not proof of misuse; review them before taking action.</p>
        </section>)}
        {tab !== "overview" && <section className="admin-panel"><div className="admin-toolbar">{selected ? <button className="ui-button" onClick={() => select(null)}>← Back to {tab}</button> : <><h2>{tabs.find(([key]) => key === tab)?.[1]}</h2>{tab === "users" && <input type="search" aria-label="Search users" placeholder="Search name or email" value={query} maxLength={100} onChange={e => { setQuery(e.target.value); setPage(1); }} />}{["users", "security", "operations"].includes(tab) && <select aria-label={`Filter ${tab} by status`} value={filter} onChange={e => { setFilter(e.target.value); setPage(1); }}>{(tab === "users" ? ["ALL", "ACTIVE", "SUSPENDED"] : tab === "security" ? ["OPEN", "INVESTIGATING", "RESOLVED", "ALL"] : ["ALL", "PENDING", "FAILED", "REJECTED", "COMPLETED"]).map(value => <option key={value} value={value}>{value === "RESOLVED" ? "LOGS / RESOLVED" : value.replaceAll("_", " ")}</option>)}</select>}</>}<button className="ui-button" disabled={loading} onClick={() => setRevision(n => n + 1)}>Refresh</button></div>
            {tab === "security" && !selected && <p className="account-muted">Alerts include repeated login failures, recovery requests, rejected orders, access denials and unusual Admin activity. Stale alerts are auto-resolved after 24 hours and recorded in Audit. This view refreshes every 30 seconds.</p>}
            {loading && <p role="status" className="account-muted">Loading…</p>}
            {!selected && tab === "users" && <Table rows={result?.items} columns={[["name", "User", row => <button onClick={() => select(row.id)}>{row.name || "Unnamed user"}</button>], ["email", "Email"], ["created_at", "Registered", row => date(row.created_at)], ["role", "Role"], ["account_status", "Status"], ["email_verified", "Verified", row => row.email_verified ? "Yes" : "No"], ["actions", "Manage", row => row.role === "ADMIN" ? "Owner account" : <button className="admin-manage-user" onClick={() => select(row.id)}>Manage</button>]]} />}
            {!selected && tab === "security" && <Table rows={result?.items} columns={[["rule_name", "Alert", row => <button className={!row.seen ? "admin-alert-unread" : ""} onClick={() => select(row.id)}>{row.rule_name || "Unnamed alert"}</button>], ["severity", "Severity", row => { const severity = String(row.severity || "Unknown"); const severityClass = ["critical", "high", "medium", "low"].includes(severity.toLowerCase()) ? severity.toLowerCase() : "unknown"; return <span className={`admin-severity ${severityClass}`}>{severity}</span>; }], ["user_email", "Account email", row => row.user_email || (row.user_id ? `Account #${row.user_id} (email unavailable)` : "Source-wide (no single account)")], ["status", "Status"], ["occurrences", "Count"], ["last_at", "Last occurrence", row => date(row.last_at)]]} />}
            {tab === "audit" && <Table rows={result?.items} columns={[["occurred_at", "Time", row => date(row.occurred_at)], ["actor_email", "By (email)", row => row.actor_email || (row.actor_id ? `Admin #${row.actor_id}` : "System")], ["target_email", "Affected email", row => row.target_email || (row.target_id ? `Account #${row.target_id} (email unavailable)` : "—")], ["action_name", "Action"], ["reason", "Details"]]} />}
            {detail && tab === "users" && <><h2>{detail.user.name || "Unnamed user"}</h2><dl className="admin-facts">{[["Email", detail.user.email || "Unavailable"], ["Registered", date(detail.user.createdAt)], ["Account status", detail.user.accountStatus], ["Email verified", detail.user.emailVerified ? "Yes" : "No"], ["Available virtual cash", money(detail.portfolio.balance)], ["Holdings value", money(detail.portfolio.holdingsValue)], ["Role", detail.user.role]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl>
                {detail.user.role !== "ADMIN" && <form className="admin-review" onSubmit={accountAction}><label>Account action<select value={action} onChange={e => setAction(e.target.value)}><option value="REVOKE_SESSIONS">Revoke all sessions</option>{detail.user.accountStatus === "ACTIVE" ? <option value="SUSPEND">Suspend account</option> : <option value="REACTIVATE">Reactivate account</option>}<option value="REQUIRE_PASSWORD_RESET">Require password reset</option>{!detail.user.emailVerified && <option value="RESEND_VERIFICATION">Resend verification email</option>}</select></label>{action !== "RESEND_VERIFICATION" && <label>Reason<textarea minLength={5} maxLength={1000} required value={reason} onChange={e => setReason(e.target.value)} placeholder="Explain why this action is needed." /></label>}{reauth && <><label>Your password<input name="password" type="password" autoComplete="current-password" required maxLength={100} /></label><label>Fresh authenticator or recovery code<input name="code" autoComplete="one-time-code" required maxLength={100} /></label></>}<button className={`ui-button ${action === "SUSPEND" ? "danger" : "primary"}`} disabled={busy}>{busy ? "Saving…" : action === "SUSPEND" ? "Suspend and revoke sessions" : action === "REACTIVATE" ? "Reactivate account" : action === "REQUIRE_PASSWORD_RESET" ? "Require reset and revoke sessions" : action === "RESEND_VERIFICATION" ? "Resend verification email" : "Revoke all sessions"}</button></form>}
                {detail.user.role !== "ADMIN" && <section className="admin-review"><h3>Active sessions</h3><Table rows={detail.sessions} columns={[["device", "Device"], ["source", "Source"], ["createdAt", "Created", row => date(row.createdAt)], ["lastActivityAt", "Last activity", row => date(row.lastActivityAt)], ["expiresAt", "Expires", row => date(row.expiresAt)], ["actions", "Action", row => <button className="admin-manage-user" disabled={busy} onClick={() => requestSessionRevoke(row.id)}>Revoke</button>]]} empty="No active sessions." />{sessionTarget && <form className="admin-review" onSubmit={revokeSession}><p>Revoke the selected device session. This requires a reason and recent Admin authentication.</p><label>Reason<textarea required minLength={5} maxLength={1000} value={sessionReason} onChange={e => setSessionReason(e.target.value)} /></label>{sessionReauth && <><label>Your password<input name="password" type="password" autoComplete="current-password" required maxLength={100} /></label><label>Fresh authenticator or recovery code<input name="code" autoComplete="one-time-code" required maxLength={100} /></label></>}<button className="ui-button danger" disabled={busy}>{busy ? "Revoking…" : "Confirm session revocation"}</button><button type="button" className="ui-button" onClick={() => requestSessionRevoke("")}>Cancel</button></form>}</section>}
                {detail.user.role !== "ADMIN" && <section className="admin-review"><h3>Virtual balance correction</h3><p className="account-muted">Corrections are additive or subtractive ledger entries, not direct balance edits. Each requires recent Admin authentication and a reason.</p><form className="admin-review" onSubmit={adjustBalance}><label>Adjustment amount<input name="amount" type="number" step="0.01" min="-1000000" max="1000000" required placeholder="Use a negative amount to subtract" /></label><label>Reason<textarea name="reason" required minLength={5} maxLength={1000} /></label>{balanceReauth && <><label>Your password<input name="password" type="password" autoComplete="current-password" required maxLength={100} /></label><label>Fresh authenticator or recovery code<input name="code" autoComplete="one-time-code" required maxLength={100} /></label></>}<button className="ui-button primary" disabled={busy}>{busy ? "Recording…" : "Record balance adjustment"}</button></form><Table rows={detail.balanceAdjustments} columns={[["occurred_at", "Time", row => date(row.occurred_at)], ["actor_email", "Admin", row => row.actor_email || (row.actor_id ? `Admin #${row.actor_id}` : "Unavailable")], ["amount", "Adjustment", row => money(row.amount)], ["balance_after", "Balance after", row => money(row.balance_after)], ["reason", "Reason"]]} empty="No balance adjustments recorded." /></section>}
                {detail.user.role !== "ADMIN" && <section className="admin-delete-user" aria-labelledby="admin-delete-user-title"><h3 id="admin-delete-user-title">Delete user permanently</h3><p>This permanently removes the account, virtual portfolio, orders, applications, saved items, sessions, passkeys and queued email. Alerts for this account are retained as resolved and reviewed, without the account link. {detailHasEmail ? "A deletion notice will be queued to the user's email address." : `This account has no email address. Type DELETE ${selected} to confirm; no notification email can be sent.`} This cannot be undone.</p><form className="admin-review" onSubmit={deleteUser}><label>{detailHasEmail ? `Type ${detail.user.email} to confirm` : `Type DELETE ${selected} to confirm`}<input type="text" autoComplete="off" required maxLength={254} value={deleteConfirmation} onChange={e => setDeleteConfirmation(e.target.value)} /></label><label>Reason (optional)<textarea maxLength={1000} value={deleteReason} onChange={e => setDeleteReason(e.target.value)} placeholder="Leave blank to use standard wording in the audit record and, when possible, the email." /></label>{deleteReauth && <><label>Your password<input name="password" type="password" autoComplete="current-password" required maxLength={100} /></label><label>Fresh authenticator or recovery code<input name="code" autoComplete="one-time-code" required maxLength={100} /></label></>}<button className="ui-button danger" disabled={busy || !deleteConfirmationMatches}>{busy ? "Deleting…" : "Permanently delete user"}</button></form></section>}
                <h2>Recent activity</h2><Table rows={detail.activity} columns={activityColumns} /><h2>Equity trades</h2><Table rows={detail.orders} columns={[["id", "Order"], ["symbol", "Stock"], ["orderType", "Side"], ["quantity", "Quantity"], ["totalAmount", "Total", row => money(row.totalAmount)], ["status", "Status"]]} /><h2>Holdings</h2><Table rows={detail.portfolio.holdings} columns={[["symbol", "Stock"], ["quantity", "Quantity"], ["currentPrice", "Price", row => money(row.currentPrice)]]} /><h2>IPO applications</h2><Table rows={detail.applications} columns={[["name", "IPO"], ["status", "Status"], ["appliedQuantity", "Shares"], ["reservedAmount", "Reserved", row => money(row.reservedAmount)]]} /><h2>Related alerts</h2><Table rows={detail.alerts} columns={[["rule_name", "Alert", row => <Link to={`/admin?tab=security&id=${row.id}`}>{row.rule_name}</Link>], ["severity", "Severity"], ["status", "Status"]]} />
            </>}
            {detail && tab === "security" && <><h2>{detail.rule_name || "Unnamed alert"}</h2><p className="account-muted">{detail.severity || "Severity unavailable"} · {detail.status} · {detail.occurrences} occurrences · {date(detail.first_at)} – {date(detail.last_at)}</p><p><strong>Account email:</strong> {detail.user_email || (detail.user_id ? `Account #${detail.user_id} (email unavailable)` : "Source-wide (no single account)")}{detail.user_id && <> · <Link className="account-link" to={`/admin?tab=users&id=${detail.user_id}`}>Open account</Link></>}</p><Table rows={detail.events} columns={[["occurred_at", "Time", row => date(row.occurred_at)], ["event_type", "Event"], ["user_email", "Account email", row => row.user_email || "Unavailable / source-wide"], ["source_label", "Source"], ["details", "Details"]]} /><form className="admin-review" onSubmit={review}><label>Review status<select value={reviewStatus} onChange={e => setReviewStatus(e.target.value)}>{["OPEN", "INVESTIGATING", "RESOLVED"].map(value => <option key={value}>{value}</option>)}</select></label><label>Investigation notes<textarea required minLength={5} maxLength={1000} placeholder="Record findings, actions or why this was a false positive." value={notes} onChange={e => setNotes(e.target.value)} /></label>{reviewReauth && <><label>Your password<input name="password" type="password" autoComplete="current-password" required maxLength={100} /></label><label>Fresh authenticator or recovery code<input name="code" autoComplete="one-time-code" required maxLength={100} /></label></>}<button className="ui-button primary" disabled={busy}>{busy ? "Saving…" : "Save review"}</button></form></>}
            {tab === "operations" && !selected && result && <section className="admin-operations"><h3>Service health</h3><dl className="admin-facts">{[["SMTP configured", result.mail?.configured ? "Yes" : "No"], ["Pending emails", result.mail?.pending ?? "Unavailable"], ["Recorded delivery failures", result.mail?.failed ?? "Unavailable"], ["Market data provider configured", result.market?.configured ? "Yes" : "No"], ["Latest quote update", date(result.market?.latestQuoteAt)], ["Stale stock quotes (> 5 min)", result.market?.staleStockCount ?? "Unavailable"], ["Paper job last completed", date(result.paperJob?.lastCompletedAt)], ["Paper job failures", result.paperJob?.failureCount ?? "Unavailable"], ["IPO settlement job last completed", date(result.settlementJob?.lastCompletedAt)], ["Recorded settlement job failures", result.settlementJob?.failureCount ?? "Unavailable"]].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl><p className="account-muted">Recorded market-data provider telemetry: {result.market?.providerTelemetry?.failureCount ?? 0} failures; last success {date(result.market?.providerTelemetry?.lastSuccessAt)}; last failure {result.market?.providerTelemetry?.lastFailure || "None recorded"}.</p><p className="account-muted">Settlement worker last failure: {result.settlementJob?.lastFailure || "None recorded"}. Last-run timestamps and cumulative failure counts persist across service restarts.</p><p className="account-notice">{result.listingAdministration}</p><ListingManager />
                <h3>Order monitoring</h3><Table rows={result.orders?.items} columns={[["created_at", "Time", row => date(row.created_at)], ["user_email", "User email"], ["symbol", "Symbol"], ["order_type", "Side"], ["quantity", "Quantity"], ["total_amount", "Amount", row => money(row.total_amount)], ["status", "Status"], ["source", "Source"]]} empty="No orders match this filter." />
                <h3>Advanced paper tickets</h3><Table rows={result.paperOrders} columns={[["user_email","User"],["instrument_key","Listing"],["product","Product"],["variety","Mode"],["order_kind","Type"],["status","State"],["filled_units","Filled"],["reserved_cash","Reserved",row=>money(row.reserved_cash)],["fees","Fees",row=>money(row.fees)]]} /><h3>Paper financing</h3><Table rows={result.paperFinancing} columns={[["user_email","User"],["product","Product"],["quantity","Units"],["principal","Principal",row=>money(row.principal)],["interest","Interest",row=>money(row.interest)],["close_requested","Closure requested"],["last_error","Last issue"]]} /><h3>Financing ledger</h3><Table rows={result.paperFinanceEvents} columns={[["occurred_at","Time",row=>date(row.occurred_at)],["position_id","Position"],["event_type","Event"],["principal_delta","Principal change",row=>money(row.principal_delta)],["amount","Amount",row=>money(row.amount)]]} /><h3>Rejected order attempts</h3><Table rows={result.orderFailures} columns={[["occurred_at", "Time", row => date(row.occurred_at)], ["user_email", "User email", row => row.user_email || (row.user_id ? `Account #${row.user_id}` : "Unavailable")], ["details", "Failure reason"]]} />
                <h3>Email delivery status</h3><p className="account-muted">Recipients, message subjects and delivery states only. Verification links, tokens and message bodies are never exposed. Sent means SMTP accepted the message; inbox receipt is not confirmed.</p><Table rows={result.mail?.messages} columns={[["recipient", "Recipient"], ["subject_line", "Message"], ["created_at", "Queued", row => date(row.created_at)], ["attempts", "Attempts"], ["sent", "State", row => row.sent ? "Accepted by SMTP" : row.error_code ? `Failed: ${row.error_code}` : "Pending"], ["failed_at", "Last failure", row => date(row.failed_at)]]} />
                <h3>T+1 settlement worker</h3><p>Last completed: {date(result.t1SettlementJob?.lastCompletedAt)}. Cumulative failures: {result.t1SettlementJob?.failureCount ?? "Unavailable"}. Last failure: {result.t1SettlementJob?.lastFailure || "None recorded"}. Each run checks at most 20 obligations; pending obligations survive restarts.</p><Table rows={result.t1SettlementStatus} columns={[["status", "Status"], ["count", "Obligations"]]} /><AdminSettlementCalendar />
                <h3>IPO applications and settlements</h3><Table rows={result.settlements} columns={[["status", "Application status"], ["count", "Count"]]} /><SettlementManager applications={result.settlementApplications} onUpdated={() => setRevision(value => value + 1)} /><Pagination result={result.orders} page={page} setPage={setPage} loading={loading} />
            </section>}
            {!selected && tab !== "operations" && <Pagination result={result} page={page} setPage={setPage} loading={loading} />}
        </section>}
    </div>;
}
