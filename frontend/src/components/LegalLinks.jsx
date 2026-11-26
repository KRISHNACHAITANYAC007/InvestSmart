import { Link } from "react-router-dom";
import { LEGAL_LINKS } from "../legal/content.js";
import "../pages/LegalPages.css";
export function LegalLinks() {
    return <nav className="legal-links" aria-label="Legal information and creator">{LEGAL_LINKS.map(([path, title]) => <Link key={path} to={path}>{title}</Link>)}</nav>;
}
export function SimulationNotice() {
    return <p className="simulation-notice">Paper trading only. Funds and transactions are virtual; no actual securities are acquired. <Link to="/legal/paper-trading">Read the risk disclaimer</Link>.</p>;
}
