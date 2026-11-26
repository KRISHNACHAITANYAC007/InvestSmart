import { chartPath } from "./marketInstruments.js";
export const MAIN_PAGES = [["/dashboard","Dashboard"],["/market","Markets"],["/heatmaps","Heatmaps"],["/portfolio","Portfolio"],["/orders","Orders"]];
export function legacyDestination(pathname, search = "") {
    const aliases = { "/stocks":"/market?tab=explore", "/watchlist":"/market?tab=watchlist", "/wallet":"/portfolio?tab=funds" };
    if (aliases[pathname]) return aliases[pathname];
    const match = pathname.match(/^\/market\/([^/]+)$/);
    if (match) return chartPath({ symbol:decodeURIComponent(match[1]) }) + search;
    return "/dashboard";
}
