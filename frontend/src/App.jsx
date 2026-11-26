import { BrowserRouter, Navigate, Outlet, Routes, Route, useLocation, useParams } from "react-router-dom";
import { lazy, Suspense } from "react";
import Login from "./pages/Login.jsx";
import Register from "./pages/Register.jsx";
import Market from "./pages/Market.jsx";
import Dashboard from "./pages/Dashboard.jsx";
import Portfolio from "./pages/Portfolio.jsx";
import Orders from "./pages/Orders.jsx";
import Heatmaps from "./pages/Heatmaps.jsx";
const MarketChartTerminal = lazy(() => import("./pages/MarketChartTerminal.jsx"));
import ProtectedRoute from "./ProtectedRoute.jsx";
import Layout from "./Layout.jsx";
import LegalPages from "./pages/LegalPages.jsx";
import { LEGAL_DOCUMENTS } from "./legal/content.js";
import PhoneCodes from "./pages/PhoneCodes.jsx";
import AuthScreen from "./pages/AuthScreen.jsx";
import Account from "./pages/Account.jsx";
import { useVisualViewport } from "./hooks/useVisualViewport.js";
const Admin = lazy(() => import("./pages/Admin.jsx"));
const AgentConversation = lazy(() => import("./pages/AgentConversation.jsx"));
import LegacyRedirect, { StockChartRedirect } from "./components/LegacyRedirect.jsx";

function MainLayout() { return <ProtectedRoute><Layout><Outlet /></Layout></ProtectedRoute>; }
function ChartTerminalRoute({ fullChart = false }) {
    const { symbol = "" } = useParams();
    const location = useLocation();
    const instrumentKey = new URLSearchParams(location.search).get("instrumentKey") || "";
    return <ProtectedRoute><MarketChartTerminal key={`${symbol}:${instrumentKey}`} fullChart={fullChart} /></ProtectedRoute>;
}
export default function App() {
    useVisualViewport();
    return <BrowserRouter><Suspense fallback={<p className="session-loading" role="status">Loading workspace…</p>}><Routes>
        {Object.keys(LEGAL_DOCUMENTS).map(document => <Route key={document} path={document === "about" ? "/about" : `/legal/${document}`} element={<LegalPages document={document} />} />)}
        <Route path="/phone" element={<PhoneCodes />} />
        <Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} /><Route path="/unlock" element={<Navigate replace to="/login" />} />
        {["verify-email","forgot-password","reset-password","resend-verification"].map(mode => <Route key={mode} path={`/${mode}`} element={<AuthScreen key={mode} mode={mode} />} />)}
        <Route element={<MainLayout />}>
            <Route path="/dashboard" element={<Dashboard />} /><Route path="/market" element={<Market />} />
            <Route path="/heatmaps" element={<Heatmaps />} /><Route path="/portfolio" element={<Portfolio />} />
            <Route path="/orders" element={<Orders />} />
            {import.meta.env.DEV && import.meta.env.VITE_AGENT_PREVIEW === "true" && <Route path="/agent" element={<AgentConversation />} />}
            <Route path="/account" element={<Account />} />
            <Route path="/admin" element={<ProtectedRoute admin><Admin /></ProtectedRoute>} />
            <Route path="/stocks" element={<LegacyRedirect />} /><Route path="/watchlist" element={<LegacyRedirect />} />
            <Route path="/wallet" element={<LegacyRedirect />} /><Route path="/stocks/:id" element={<StockChartRedirect />} />
            <Route path="/market/:symbol" element={<LegacyRedirect />} />
        </Route>
        <Route path="/market/chart/:symbol" element={<ChartTerminalRoute />} />
        <Route path="/market/chart/:symbol/full" element={<ChartTerminalRoute fullChart />} />
        <Route path="/" element={<Navigate replace to="/dashboard" />} />
        <Route path="*" element={<Navigate replace to="/dashboard" />} />
    </Routes></Suspense></BrowserRouter>;
}
