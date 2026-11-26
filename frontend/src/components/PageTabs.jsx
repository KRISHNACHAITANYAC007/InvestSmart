import { useSearchParams } from "react-router-dom";
import "./PageTabs.css";

export default function PageTabs({ tabs, selected, label }) {
    const [params, setParams] = useSearchParams();
    return <nav className="page-tabs" aria-label={label}>{tabs.map(([value, title]) =>
        <button type="button" key={value} aria-current={selected === value ? "page" : undefined}
            onClick={() => { const next = new URLSearchParams(params); next.set("tab", value); setParams(next); }}>{title}</button>)}</nav>;
}
