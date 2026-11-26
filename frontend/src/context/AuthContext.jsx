import { useEffect, useState } from "react";
import { AuthContext } from "./authContext.js";
import api from "../api/api.js";

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null), [loading, setLoading] = useState(true);
    const refresh = async () => {
        const { data } = await api.get("/users/me");
        setUser(data);
        return data;
    };
    useEffect(() => {
        localStorage.removeItem("token");
        const controller = new AbortController();
        api.get("/users/me", { signal: controller.signal }).then(({ data }) => { if (!controller.signal.aborted) setUser(data); }).catch(() => { if (!controller.signal.aborted) setUser(null); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
        const ended = () => { setUser(null); };
        window.addEventListener("investsmart-session-ended", ended);
        return () => { controller.abort(); window.removeEventListener("investsmart-session-ended", ended); };
    }, []);
    const logout = async () => { await api.post("/auth/logout"); setUser(null); };
    const login = value => { setUser(value); };
    return <AuthContext.Provider value={{ user, loading, isAuthenticated: !!user, login, logout, refresh }}>{children}</AuthContext.Provider>;
}
