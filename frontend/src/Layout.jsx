import { useEffect, useRef, useState } from "react";
import Navbar from "./components/Navbar.jsx";
import Sidebar from "./components/Sidebar.jsx";
import "./components/Layout.css";

function Layout({ children }) {

    const [menuOpen, setMenuOpen] = useState(false);
    const layout = useRef(null);
    useEffect(() => {
        const element = layout.current, navbar = element.querySelector("nav.navbar");
        const measure = () => element.style.setProperty("--navbar-height", `${navbar.getBoundingClientRect().height}px`);
        measure(); const observer = new ResizeObserver(measure); observer.observe(navbar);
        return () => observer.disconnect();
    }, []);

    return (
        <div className="app-layout" ref={layout}>

            <Navbar menuOpen={menuOpen} setMenuOpen={setMenuOpen} />

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
