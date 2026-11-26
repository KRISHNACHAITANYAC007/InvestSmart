import { Navigate } from "react-router-dom";
import { useAuth } from "./context/useAuth.js";
export default function ProtectedRoute({ children, admin = false }) {
    const { user, loading } = useAuth();
    if (loading) return <p className="session-loading" role="status">Loading your account…</p>;
    if (!user) return <Navigate to="/login" replace />;
    if (admin && user.role !== "ADMIN") return <Navigate to="/dashboard" replace />;
    return children;
}
