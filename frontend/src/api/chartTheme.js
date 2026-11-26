export function chartTheme(theme) {
    const light = theme === "light";
    const border = light ? "#dce3eb" : "#25313d";
    return {
        layout: { background: { color: light ? "#ffffff" : "#0b0f14" }, textColor: light ? "#58687c" : "#8a98a8" },
        grid: { vertLines: { color: light ? "#eff2f6" : "#1c2630" }, horzLines: { color: light ? "#eff2f6" : "#1c2630" } },
        rightPriceScale: { borderColor: border }, timeScale: { borderColor: border },
        crosshair: { vertLine: { color: light ? "#91a0b1" : "#60758b" }, horzLine: { color: light ? "#91a0b1" : "#60758b" } },
    };
}
