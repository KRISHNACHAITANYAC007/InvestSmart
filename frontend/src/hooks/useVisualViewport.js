import { useEffect } from "react";

// Mobile software keyboards do not consistently resize dynamic viewport units.
export function useVisualViewport() {
    useEffect(() => {
        const root = document.documentElement, viewport = window.visualViewport;
        const update = () => {
            const height = viewport?.height ?? window.innerHeight;
            root.style.setProperty("--visual-viewport-top", `${viewport?.offsetTop ?? 0}px`);
            root.style.setProperty("--visual-viewport-height", `${height}px`);
            root.style.setProperty("--keyboard-bottom", `${Math.max(0, window.innerHeight - height - (viewport?.offsetTop ?? 0))}px`);
        };
        update(); viewport?.addEventListener("resize", update); viewport?.addEventListener("scroll", update); window.addEventListener("resize", update);
        return () => {
            viewport?.removeEventListener("resize", update); viewport?.removeEventListener("scroll", update); window.removeEventListener("resize", update);
            root.style.removeProperty("--visual-viewport-top"); root.style.removeProperty("--visual-viewport-height"); root.style.removeProperty("--keyboard-bottom");
        };
    }, []);
}
