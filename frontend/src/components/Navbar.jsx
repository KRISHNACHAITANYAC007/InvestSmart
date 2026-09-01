import { useAuth } from "../context/AuthContext.jsx";
import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import "./Navbar.css";

function Navbar({ menuOpen ,setMenuOpen }) {

    const { logout } = useAuth();
    const navigate = useNavigate();

    function handleLogout() {
        logout();
        navigate("/login");
    }

    function backtodash()
    {
        navigate("/dashboard");
    }

    return (
        <nav className="navbar">

            <div className="navbar-left">

                <button
                    className="menu-button"
                    onClick={() => setMenuOpen(prev => !prev)}
                >
                    ☰
                </button>

                <Link
                    to="/dashboard"
                    className="navbar-title"
                >
                    InvestSmart
                </Link>

            </div>

            <button
                className="navbar-logout"
                onClick={handleLogout}
            >
                Logout
            </button>

        </nav>
    );
}

export default Navbar;