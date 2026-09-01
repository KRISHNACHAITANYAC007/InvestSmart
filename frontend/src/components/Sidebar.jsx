import { Link } from "react-router-dom";
import "./Sidebar.css";

function Sidebar({ menuOpen, setMenuOpen }) {

    function handleLinkClick() {
        setMenuOpen(false);
    }

    return (
        <>
            {menuOpen && (
                <div
                    className="sidebar-overlay"
                    onClick={() => setMenuOpen(false)}
                ></div>
            )}

            <aside className={`sidebar ${menuOpen ? "sidebar-open" : ""}`}>

                <h3 className="sidebar-title">
                    Menu
                </h3>

                <Link
                    className="sidebar-link"
                    to="/dashboard"
                    onClick={handleLinkClick}
                >
                    Dashboard
                </Link>

                <Link
                    className="sidebar-link"
                    to="/stocks"
                    onClick={handleLinkClick}
                >
                    Stocks
                </Link>

                <Link
                    className="sidebar-link"
                    to="/watchlist"
                    onClick={handleLinkClick}
                >
                    Watchlist
                </Link>

                <Link
                    className="sidebar-link"
                    to="/portfolio"
                    onClick={handleLinkClick}
                >
                    Portfolio
                </Link>

                <Link
                    className="sidebar-link"
                    to="/orders"
                    onClick={handleLinkClick}
                >
                    Orders
                </Link>

                <Link
                    className="sidebar-link"
                    to="/wallet"
                    onClick={handleLinkClick}
                >
                    Wallet
                </Link>

            </aside>
        </>
    );
}

export default Sidebar;