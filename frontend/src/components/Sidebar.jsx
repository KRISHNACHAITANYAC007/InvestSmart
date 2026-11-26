import { MAIN_PAGES } from "../api/navigation.js";
import { NavLink } from "react-router-dom";
import "./Sidebar.css";
import { useEffect } from "react";
import { useAuth } from "../context/useAuth.js";

function Sidebar({ menuOpen, setMenuOpen }) {
    const { user } = useAuth();
    useEffect(() => { if (!menuOpen) return; const close = event => { if (event.key === "Escape") setMenuOpen(false); }; window.addEventListener("keydown", close); return () => window.removeEventListener("keydown", close); }, [menuOpen, setMenuOpen]);

    function handleLinkClick() {
        setMenuOpen(false);
    }

    return (
        <>
            {menuOpen && (
                <button
                    className="sidebar-overlay"
                    aria-label="Close navigation"
                    onClick={() => setMenuOpen(false)}
                />
            )}

            <aside id="main-navigation" aria-label="Main navigation" aria-hidden={!menuOpen} inert={!menuOpen} className={`sidebar ${menuOpen ? "sidebar-open" : ""}`}>

                <h3 className="sidebar-title">
                    Menu
                </h3>
                {import.meta.env.DEV && import.meta.env.VITE_AGENT_PREVIEW === "true" && <NavLink to="/agent" onClick={handleLinkClick}>Portfolio companion</NavLink>}

                {MAIN_PAGES.map(([path,title]) => <NavLink key={path} className={({isActive}) => `sidebar-link${isActive ? " active" : ""}`} to={path} onClick={handleLinkClick}>{title}</NavLink>)}
                {user?.role === "ADMIN" && <NavLink className="sidebar-link" to="/admin" onClick={handleLinkClick}>Admin</NavLink>}

            </aside>
        </>
    );
}

export default Sidebar;
