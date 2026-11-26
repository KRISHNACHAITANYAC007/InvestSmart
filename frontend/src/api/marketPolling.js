// One visible-tab request at a time. Manual refresh never bypasses provider cooldowns.
export function createMarketPoller({ load, onSuccess, onState, interval, documentRef = document, timers = window, now = Date.now }) {
    let disposed = false, inFlight = false, timer, controller, blockedUntil = 0, failures = 0;
    function schedule(delay) { timers.clearTimeout(timer); if (!disposed) timer = timers.setTimeout(() => { void refresh(); }, delay); }
    async function refresh() {
        if (disposed || inFlight) return;
        if (documentRef.hidden) { schedule(interval); return; }
        if (now() < blockedUntil) { schedule(blockedUntil - now()); return; }
        inFlight = true; controller = new AbortController(); onState({ refreshing: true });
        try {
            const value = await load(controller.signal);
            if (disposed || controller.signal.aborted) return;
            failures = 0; blockedUntil = 0; onSuccess(value);
            onState({ refreshing: false, error: '', updatedAt: now(), retryAt: 0 });
        } catch (failure) {
            if (disposed || controller.signal.aborted) return;
            failures++;
            const header = failure.response?.headers?.['retry-after'];
            const seconds = Number(header), date = typeof header === 'string' ? Date.parse(header) : NaN;
            const retry = Number.isFinite(seconds) && seconds > 0 ? seconds * 1000 : Number.isFinite(date) ? Math.max(1000, date - now()) : 0;
            const delay = Math.max(retry, Math.min(300000, interval * 2 ** Math.min(failures, 5)));
            blockedUntil = now() + delay;
            onState({ refreshing: false, error: failure.response?.status === 429 ? 'Provider rate limit. Automatic refresh will retry after the cooldown.' : 'Refresh unavailable. Last received data may be stale.', retryAt: blockedUntil });
        } finally { inFlight = false; schedule(Math.max(interval, blockedUntil - now())); }
    }
    function visibility() { if (!documentRef.hidden) void refresh(); }
    documentRef.addEventListener('visibilitychange', visibility);
    void refresh();
    return { refresh, dispose() { disposed = true; controller?.abort(); timers.clearTimeout(timer); documentRef.removeEventListener('visibilitychange', visibility); } };
}
