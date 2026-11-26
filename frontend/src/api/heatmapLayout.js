// Equal-area subdivision fills the viewport without claiming market-cap weights.
export function heatmapLayout(count, width, height) {
    if (!Number.isInteger(count) || count < 1 || width <= 0 || height <= 0) return [];
    const tiles = [];
    function split(start, size, x, y, w, h) {
        if (size === 1) { tiles[start] = { left: x, top: y, width: w, height: h }; return; }
        const first = Math.floor(size / 2), ratio = first / size;
        if (w > h * 1.15) {
            split(start, first, x, y, w * ratio, h);
            split(start + first, size - first, x + w * ratio, y, w * (1 - ratio), h);
        } else {
            split(start, first, x, y, w, h * ratio);
            split(start + first, size - first, x, y + h * ratio, w, h * (1 - ratio));
        }
    }
    split(0, count, 0, 0, width, height);
    return tiles;
}
