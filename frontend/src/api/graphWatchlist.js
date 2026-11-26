export function createGraphWatchlist(client, onChange) {
    let state = { items: [], loading: true, refreshing: false, listError: "", pricesStale: false };
    let disposed = false;
    let listRequest = 0;
    let listController;
    let pricesInFlight = null;
    const priceController = new AbortController();

    function update(changes) {
        if (disposed) return;
        state = { ...state, ...changes };
        onChange(state);
    }

    async function loadMembership() {
        if (disposed) return;
        const request = ++listRequest;
        listController?.abort();
        listController = new AbortController();
        try {
            const { data } = await client.get("/watchlist/me", { signal: listController.signal });
            if (disposed || request !== listRequest) return;
            if (!Array.isArray(data)) throw new Error("Invalid watchlist response");
            update({
                items: [...data].sort((left, right) => Number(right.id) - Number(left.id)),
                loading: false,
                listError: "",
            });
        } catch {
            if (disposed || request !== listRequest) return;
            update({ loading: false, listError: "Could not reload your watchlist. Showing the last loaded list." });
        }
    }

    function refreshPrices(force) {
        if (disposed) return Promise.resolve();
        if (pricesInFlight) return pricesInFlight;
        update({ refreshing: true });
        pricesInFlight = (async () => {
            try {
                const { data } = await client.post("/market/quotes/refresh", {}, {
                    params: { force }, signal: priceController.signal,
                });
                update({ pricesStale: Boolean(data?.staleSymbols?.length) });
            } catch {
                update({ pricesStale: true });
            } finally {
                if (!disposed) await loadMembership();
                update({ refreshing: false });
                pricesInFlight = null;
            }
        })();
        return pricesInFlight;
    }

    function refresh(force = false) {
        // Membership is independent of provider networking and always starts immediately.
        const membership = loadMembership();
        const prices = refreshPrices(force);
        return Promise.all([membership, prices]);
    }

    return {
        refresh,
        dispose() {
            disposed = true;
            ++listRequest;
            listController?.abort();
            priceController.abort();
        },
    };
}
