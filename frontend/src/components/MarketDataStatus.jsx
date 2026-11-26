export default function MarketDataStatus({info}) {
    if(!info)return null;
    const stale=info.failed||info.staleSymbols?.length;
    return <p className={`market-data-status ${stale?"is-stale":""}`} role="status">{info.failed?"Could not refresh prices. Showing saved values.":info.staleSymbols?.length?"Some prices are stale or unavailable. Showing the latest saved values.":`Prices refreshed${info.asOf?` at ${new Date(info.asOf).toLocaleTimeString("en-IN")}`:""}.`}</p>;
}
