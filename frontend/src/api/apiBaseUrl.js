export function resolveApiBaseUrl(configuredUrl, browserHostname) {
    if (configuredUrl) return configuredUrl;
    const hostname = browserHostname
        ?? (typeof window === "undefined" ? "localhost" : window.location.hostname);
    if (hostname === "localhost" || hostname === "127.0.0.1") {
        return `http://${hostname}:8080`;
    }
    return "/api";
}
