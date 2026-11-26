// Preserve the user's historical viewport; advance only a view already at the latest bar.
export function updateChartSeries(chart, candles, volume, previous, next) {
    const scale = chart.timeScale(), range = scale.getVisibleLogicalRange();
    const followsLatest = range && previous.length && range.to >= previous.length - 1;
    candles.setData(next.map(({ chartTime: time, item }) => ({ time, open: Number(item.open), high: Number(item.high), low: Number(item.low), close: Number(item.price) })));
    volume.setData(next.map(({ chartTime: time, item }) => ({ time, value: Math.max(0, Number(item.volume) || 0), color: Number(item.price) >= Number(item.open) ? 'rgba(38,166,154,.52)' : 'rgba(239,83,80,.52)' })));
    if (!previous.length) scale.fitContent();
    else if (range) { const shift = followsLatest ? next.length - previous.length : 0; scale.setVisibleLogicalRange({ from: range.from + shift, to: range.to + shift }); }
}
