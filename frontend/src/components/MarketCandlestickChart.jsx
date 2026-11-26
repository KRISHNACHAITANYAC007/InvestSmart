import { updateChartSeries } from "../api/chartUpdates.js";
import { chartTheme } from "../api/chartTheme.js";
import { useEffect, useMemo, useRef, useState } from "react";
import {
    createChart,
    CandlestickSeries,
    HistogramSeries,
    LineStyle
} from "lightweight-charts";

function parseWallClock(timestamp) {
    const [datePart, timePart = "00:00:00"] =
        timestamp.split("T");

    const [year, month, day] =
        datePart.split("-").map(Number);

    const [hour = 0, minute = 0, second = 0] =
        timePart.split(":").map(Number);

    return {
        year,
        month,
        day,
        hour,
        minute,
        second
    };
}

function wallClockToChartTime(timestamp) {
    const {
        year,
        month,
        day,
        hour,
        minute,
        second
    } = parseWallClock(timestamp);

    return Date.UTC(
        year,
        month - 1,
        day,
        hour,
        minute,
        second
    ) / 1000;
}

function formatIndianDate(timestamp) {
    const time = wallClockToChartTime(timestamp);

    return new Intl.DateTimeFormat("en-IN", {
        timeZone: "UTC",
        day: "2-digit",
        month: "short",
        year: "numeric"
    }).format(new Date(time * 1000));
}

function formatIndianTime(timestamp) {
    const time = wallClockToChartTime(timestamp);

    return new Intl.DateTimeFormat("en-IN", {
        timeZone: "UTC",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    }).format(new Date(time * 1000));
}

function formatIndianDateTime(timestamp) {
    const time = wallClockToChartTime(timestamp);

    return new Intl.DateTimeFormat("en-IN", {
        timeZone: "UTC",
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        hour12: true
    }).format(new Date(time * 1000));
}

function isRegularMarketTime(timestamp) {
    const { hour, minute } = parseWallClock(timestamp);

    const totalMinutes = hour * 60 + minute;

    return (
        totalMinutes >= 9 * 60 + 15 &&
        totalMinutes <= 15 * 60 + 25
    );
}

function prepareChartData(history, period) {
    if (!Array.isArray(history) || history.length === 0) {
        return [];
    }

    let sortedHistory = history.filter((item) => typeof item?.timestamp === "string"
        && Number.isFinite(wallClockToChartTime(item.timestamp))
        && [item.open, item.high, item.low, item.price]
            .every((value) => Number.isFinite(Number(value)) && Number(value) > 0))
        .sort(
        (a, b) =>
            wallClockToChartTime(a.timestamp) -
            wallClockToChartTime(b.timestamp)
    );
    sortedHistory = [...new Map(sortedHistory.map(item => [item.timestamp, item])).values()];
    if (!sortedHistory.length) return [];

    /*
     * 1D: Keep only the latest trading session.
     * 1W: Keep all available trading sessions.
     */
    if (period === "1D") {
        const latestDate =
            sortedHistory[sortedHistory.length - 1]
                .timestamp.split("T")[0];

        sortedHistory = sortedHistory.filter(
            (item) =>
                item.timestamp.startsWith(latestDate) &&
                isRegularMarketTime(item.timestamp)
        );

        return sortedHistory.map(item => ({ item, chartTime: wallClockToChartTime(item.timestamp) }));
    }

    /*
     * 1W: Use actual timestamps for all available
     * 30-minute candles across the requested week.
     *
     * Other timeframes use their actual timestamps too.
     */
    return sortedHistory.map((item) => ({
        item,
        chartTime: wallClockToChartTime(item.timestamp)
    }));
}

function MarketCandlestickChart({
                                    history,
                                    period,
                                    loading,
                                    theme = "dark",
                                    dayHigh,
                                    dayLow,
                                    latestPrice
                                }) {
    const containerRef = useRef(null);
    const chartRef = useRef(null);
    const volumeSeriesRef = useRef(null), previousDataRef = useRef([]), chartTimeMapRef = useRef(new Map()), quoteLineRef = useRef(null);
    const candlestickSeriesRef = useRef(null);
    const dayHighLineRef = useRef(null);
    const dayLowLineRef = useRef(null);
    const themeRef = useRef(theme);
    useEffect(() => { themeRef.current = theme; chartRef.current?.applyOptions(chartTheme(theme)); }, [theme]);

    const [selectedCandle, setSelectedCandle] =
        useState(null);
    const chartData = useMemo(() => prepareChartData(history, period), [history, period]);

    const hasData = chartData.length > 0;
    useEffect(() => {
        const container = containerRef.current;

        if (!container || loading || !hasData) {
            if (chartRef.current) {
                chartRef.current.remove();
                chartRef.current = null;
            }
            candlestickSeriesRef.current = null;

            setSelectedCandle(null);
            return;
        }

        /*
         * Remove the previous chart before creating
         * a chart for the selected timeframe.
         */
        if (chartRef.current) {
            chartRef.current.remove();
            chartRef.current = null;
        }
        candlestickSeriesRef.current = null;
        dayHighLineRef.current = null;
        dayLowLineRef.current = null;

        setSelectedCandle(null);

        /*
         * Map chart timestamps to original market data.
         */
        const chartTimeMap = chartTimeMapRef.current;

        const isIntraday =
            period === "1D" || period === "1W";

        const chart = createChart(container, {
            width: container.clientWidth,
            height: Math.max(container.clientHeight, 120),

            layout: {
                background: {
                    color: "#0b0f14"
                },
                textColor: "#8a98a8",
                attributionLogo: false
            },

            grid: {
                vertLines: {
                    color: "#1c2630"
                },
                horzLines: {
                    color: "#1c2630"
                }
            },

            rightPriceScale: {
                borderColor: "#25313d"
            },

            localization: {
                locale: "en-IN",

                timeFormatter: (time) => {
                    const item = chartTimeMap.get(Number(time));

                    if (!item) {
                        return "";
                    }

                    return isIntraday
                        ? formatIndianDateTime(item.timestamp)
                        : formatIndianDate(item.timestamp);
                }
            },

            timeScale: {
                borderColor: "#25313d",
                timeVisible: isIntraday,
                secondsVisible: false,

                /*
                 * Show intraday time labels for 1D and 1W.
                 * Show dates for longer timeframes.
                 */
                tickMarkFormatter: (time) => {
                    const item = chartTimeMap.get(Number(time));

                    if (!item) {
                        return "";
                    }

                    return isIntraday
                        ? formatIndianTime(item.timestamp)
                        : formatIndianDate(item.timestamp);
                }
            },

            crosshair: {
                mode: 0,
                vertLine: { color: "#687887", labelBackgroundColor: "#27333f" },
                horzLine: { color: "#687887", labelBackgroundColor: "#27333f" }
            }
        });

        chart.applyOptions(chartTheme(themeRef.current));
        chartRef.current = chart;

        /*
         * Candlestick series.
         */
        const candlestickSeries = chart.addSeries(
            CandlestickSeries,
            {
                upColor: "#26a69a",
                downColor: "#ef5350",
                borderUpColor: "#26a69a",
                borderDownColor: "#ef5350",
                wickUpColor: "#26a69a",
                wickDownColor: "#ef5350"
            }
        );
        candlestickSeriesRef.current = candlestickSeries;

        /*
         * Volume histogram.
         */
        const volumeSeries = chart.addSeries(
            HistogramSeries,
            {
                priceFormat: {
                    type: "volume"
                },
                priceScaleId: ""
            }
        );

        volumeSeriesRef.current = volumeSeries;
        volumeSeries.priceScale().applyOptions({
            scaleMargins: {
                top: 0.80,
                bottom: 0
            }
        });

        candlestickSeries.priceScale().applyOptions({
            scaleMargins: {
                top: 0.10,
                bottom: 0.25
            }
        });

        /*
         * Update candle details when the pointer
         * moves over a candle.
         */
        const handleCrosshairMove = (param) => {
            if (param.time == null || !param.seriesData) {
                setSelectedCandle(null);
                return;
            }

            const candle = param.seriesData.get(
                candlestickSeries
            );

            if (!candle) {
                setSelectedCandle(null);
                return;
            }

            const originalItem = chartTimeMap.get(
                Number(param.time)
            );

            if (!originalItem) {
                setSelectedCandle(null);
                return;
            }

            const open = Number(candle.open);
            const high = Number(candle.high);
            const low = Number(candle.low);
            const close = Number(candle.close);

            const change = close - open;

            const changePercent =
                open !== 0
                    ? (change / open) * 100
                    : 0;

            setSelectedCandle({
                timestamp: originalItem.timestamp,
                open,
                high,
                low,
                close,
                change,
                changePercent,
                volume: Number(originalItem.volume) || 0
            });
        };

        chart.subscribeCrosshairMove(handleCrosshairMove);

        previousDataRef.current = [];

        /*
         * Resize the chart when its container changes.
         */
        const resizeObserver = new ResizeObserver(() => {
            if (containerRef.current && chartRef.current) {
                chartRef.current.applyOptions({
                    width: containerRef.current.clientWidth,
                    height: Math.max(containerRef.current.clientHeight, 120)
                });
            }
        });

        resizeObserver.observe(container);

        return () => {
            resizeObserver.disconnect();

            chart.unsubscribeCrosshairMove(
                handleCrosshairMove
            );

            if (chartRef.current === chart) {
                chart.remove();
                chartRef.current = null;
                candlestickSeriesRef.current = null;
                dayHighLineRef.current = null;
                dayLowLineRef.current = null;
                quoteLineRef.current = null;
                volumeSeriesRef.current = null;
                previousDataRef.current = [];
            }
        };
    }, [hasData, period, loading]);

    useEffect(() => {
        if (!chartRef.current || !candlestickSeriesRef.current || !volumeSeriesRef.current) return;
        chartTimeMapRef.current.clear();
        chartData.forEach(point => chartTimeMapRef.current.set(Number(point.chartTime), point.item));
        updateChartSeries(chartRef.current, candlestickSeriesRef.current, volumeSeriesRef.current, previousDataRef.current, chartData);
        previousDataRef.current = chartData;
    }, [chartData, loading]);
    useEffect(() => {
        const series = candlestickSeriesRef.current;
        if (!series) return;
        if (quoteLineRef.current) series.removePriceLine(quoteLineRef.current);
        quoteLineRef.current = Number(latestPrice) > 0 ? series.createPriceLine({ price: Number(latestPrice), color: '#6ca0ff', lineWidth: 1, lineStyle: LineStyle.Dashed, axisLabelVisible: true, title: 'Quote' }) : null;
    }, [latestPrice, hasData, loading]);

    useEffect(() => {
        const series = candlestickSeriesRef.current;
        if (!series) return;

        if (dayHighLineRef.current) {
            series.removePriceLine(dayHighLineRef.current);
            dayHighLineRef.current = null;
        }
        if (dayLowLineRef.current) {
            series.removePriceLine(dayLowLineRef.current);
            dayLowLineRef.current = null;
        }
        if (period !== "1D") return;

        const high = Number(dayHigh);
        const low = Number(dayLow);
        if (Number.isFinite(high) && high > 0) {
            dayHighLineRef.current = series.createPriceLine({
                price: high,
                color: "#26a69a",
                lineWidth: 1,
                lineStyle: LineStyle.Dashed,
                axisLabelVisible: true,
                title: "Day high"
            });
        }
        if (Number.isFinite(low) && low > 0) {
            dayLowLineRef.current = series.createPriceLine({
                price: low,
                color: "#ef5350",
                lineWidth: 1,
                lineStyle: LineStyle.Dashed,
                axisLabelVisible: true,
                title: "Day low"
            });
        }
    }, [chartData, dayHigh, dayLow, period, loading]);

    if (loading) {
        return (
            <div className="terminal-chart-message">
                Loading chart data...
            </div>
        );
    }

    if (!history || history.length === 0) {
        return (
            <div className="terminal-chart-message">
                No historical data available for this period.
            </div>
        );
    }

    if (chartData.length === 0) {
        return (
            <div className="terminal-chart-message">
                No valid candles are available for this session. Try another time range.
            </div>
        );
    }

    return (
        <div className="terminal-chart-wrapper">
            {/* Candle information panel */}
            <div className="terminal-candle-details">
                {selectedCandle ? (
                    <>
                        <div className="terminal-candle-date">
                            {period === "1D" || period === "1W"
                                ? formatIndianDateTime(
                                    selectedCandle.timestamp
                                )
                                : formatIndianDate(
                                    selectedCandle.timestamp
                                )}
                        </div>

                        <div className="terminal-candle-values">
                            <span>
                                <label>O</label>
                                <strong>
                                    ₹{selectedCandle.open.toFixed(2)}
                                </strong>
                            </span>

                            <span>
                                <label>H</label>
                                <strong className="terminal-value-high">
                                    ₹{selectedCandle.high.toFixed(2)}
                                </strong>
                            </span>

                            <span>
                                <label>L</label>
                                <strong className="terminal-value-low">
                                    ₹{selectedCandle.low.toFixed(2)}
                                </strong>
                            </span>

                            <span>
                                <label>C</label>
                                <strong>
                                    ₹{selectedCandle.close.toFixed(2)}
                                </strong>
                            </span>

                            <span>
                                <label>Change</label>

                                <strong
                                    className={
                                        selectedCandle.change >= 0
                                            ? "terminal-positive"
                                            : "terminal-negative"
                                    }
                                >
                                    {selectedCandle.change >= 0
                                        ? "+"
                                        : ""}
                                    {selectedCandle.change.toFixed(2)}
                                    {" "}(
                                    {selectedCandle.changePercent >= 0
                                        ? "+"
                                        : ""}
                                    {selectedCandle.changePercent.toFixed(2)}
                                    %)
                                </strong>
                            </span>

                            <span>
                                <label>Volume</label>
                                <strong>
                                    {selectedCandle.volume.toLocaleString(
                                        "en-IN"
                                    )}
                                </strong>
                            </span>
                        </div>
                    </>
                ) : (
                    <div className="terminal-candle-hint">
                        Move your cursor over a candle to see its details.
                    </div>
                )}
            </div>

            {/* Chart */}
            <div
                ref={containerRef}
                className="terminal-chart-container"
            />
        </div>
    );
}

export default MarketCandlestickChart;
