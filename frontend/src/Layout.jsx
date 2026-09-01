import { useState } from "react";
import Navbar from "./components/Navbar.jsx";
import Sidebar from "./components/Sidebar.jsx";
import "./components/Layout.css";

function Layout({ children }) {

    const [menuOpen, setMenuOpen] = useState(false);

    return (
        <div className="app-layout">

            <Navbar setMenuOpen={setMenuOpen} />

            <Sidebar
                menuOpen={menuOpen}
                setMenuOpen={setMenuOpen}
            />

            <main className="main-content">
                {children}
            </main>

        </div>
    );
}

export default Layout;