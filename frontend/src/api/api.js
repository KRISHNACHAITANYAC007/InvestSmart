import axios from "axios";
import { resolveApiBaseUrl } from "./apiBaseUrl.js";

const api = axios.create({ baseURL: resolveApiBaseUrl(import.meta.env.VITE_API_URL), withCredentials: true, withXSRFToken: true });
let csrfRequest;
api.interceptors.request.use(async config => {
    if (!["get", "head", "options"].includes((config.method || "get").toLowerCase())) {
        if (!csrfRequest) csrfRequest = api.get("/auth/csrf").finally(() => { csrfRequest = null; });
        const { data } = await csrfRequest;
        config.headers["X-XSRF-TOKEN"] = data.token;
    }
    return config;
});
api.interceptors.response.use(response => response, failure => {
    if (failure.response?.status === 401 && !failure.config?.url?.startsWith("/auth/") && !["/users/me/password","/users/me/reauthenticate"].includes(failure.config?.url)) window.dispatchEvent(new Event("investsmart-session-ended"));
    return Promise.reject(failure);
});
export default api;
