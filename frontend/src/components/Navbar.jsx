import { useAuth } from "../context/useAuth.js";
import { Link, useNavigate } from "react-router-dom";
import "./Navbar.css";
import { useState } from "react";

function Navbar({ menuOpen, setMenuOpen }) {

    const { logout, user } = useAuth();
    const navigate = useNavigate();
    const [error,setError] = useState("");

    async function handleLogout() {
        try { await logout(); navigate("/login"); } catch { setError("Sign out failed. Please retry."); }
    }

    return (
        <nav className="navbar">

            <div className="navbar-left">

                <button
                    className="menu-button"
                    aria-label={menuOpen ? "Close navigation" : "Open navigation"}
                    aria-expanded={menuOpen}
                    aria-controls="main-navigation"
                    onClick={() => setMenuOpen(prev => !prev)}
                >
                    ☰
                </button>

                <Link
                    to="/dashboard"
                    className="navbar-title"
                    aria-label="InvestSmart"
                >
                    <span>Invest</span><span className="navbar-title-accent">Smart</span>
                </Link>

            </div>

            <div className="navbar-account-actions"><Link to="/account">Account</Link>{user?.role === "ADMIN" && <Link to="/admin">Admin</Link>}{error && <span role="alert">{error}</span>}<button
                className="navbar-logout"
                onClick={handleLogout}
            >
                Sign out
            </button></div>

        </nav>
    );
}

export default Navbar;
