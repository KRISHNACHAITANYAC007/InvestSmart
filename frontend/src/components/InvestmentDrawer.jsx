import { useEffect, useRef } from "react";
export default function InvestmentDrawer({ title, onClose, children }) {
    const dialog = useRef(null);
    useEffect(() => { const element = dialog.current; element.showModal(); const previous = document.body.style.overflow; document.body.style.overflow = "hidden"; return () => { element.close(); document.body.style.overflow = previous; }; }, []);
    return <dialog className="investment-drawer" ref={dialog} onCancel={onClose} aria-labelledby="investment-detail-title"><header><div><small>Markets</small><h2 id="investment-detail-title">{title}</h2></div><button onClick={onClose} aria-label="Close details">✕</button></header>{children}</dialog>;
}
