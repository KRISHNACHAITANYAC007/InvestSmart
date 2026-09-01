import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import api from "../api/api.js";
import "./StockDetails.css";

function StockDetails() {

    const { id } = useParams();
    const navigate = useNavigate();
    const [isInWatchlist, setIsInWatchlist] = useState(false);

    const [stock, setStock] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const [quantity, setQuantity] = useState(1);
    const [user, setUser] = useState(null);

    const [buyMessage, setBuyMessage] = useState("");
    const [buyError, setBuyError] = useState("");

    const [sellMessage, setSellMessage] = useState("");
    const [sellError, setSellError] = useState("");

    const [buyLoading, setBuyLoading] = useState(false);
    const [sellLoading, setSellLoading] = useState(false);

    useEffect(() => {

        Promise.all([
            api.get(`/stocks/${id}`),
            api.get("/users/me")
        ])
            .then(([stockResponse, userResponse]) => {

                setStock(stockResponse.data);
                setUser(userResponse.data);

                setLoading(false);
            })
            .catch((error) => {

                console.log(
                    "Failed to fetch stock or user:",
                    error
                );

                setError(
                    "Failed to load stock details."
                );

                setLoading(false);
            });

    }, [id]);

    useEffect(() => {

        api.get("/watchlist/me")
            .then((response) => {

                const exists = response.data.some(
                    (item) =>
                        item.stockId === Number(id)
                );

                setIsInWatchlist(exists);

            })
            .catch((error) => {
                console.log(
                    "Failed to check watchlist:",
                    error
                );
            });

    }, [id]);

    const addToWatchlist = async () => {

        try {

            await api.post(
                `/watchlist?stockId=${id}`
            );

            setIsInWatchlist(true);

        } catch (error) {

            console.log(
                "Failed to add to watchlist:",
                error
            );

            alert(
                error.response?.data?.message ||
                "Failed to add stock to watchlist."
            );
        }
    };

    const removeFromWatchlist = async () => {

        try {

            await api.delete(
                `/watchlist/me/${id}`
            );

            setIsInWatchlist(false);

        } catch (error) {

            console.log(
                "Failed to remove from watchlist:",
                error
            );

            alert(
                error.response?.data?.message ||
                "Failed to remove stock from watchlist."
            );
        }
    };

    async function handleBuy() {

        setBuyMessage("");
        setBuyError("");

        if (!quantity || quantity <= 0) {
            setBuyError("Quantity must be at least 1.");
            return;
        }

        setBuyLoading(true);

        try {

            const request = {
                userId: user.id,
                stockId: stock.id,
                quantity: quantity
            };

            const response =
                await api.post("/orders/buy", request);

            console.log(
                "Buy successful:",
                response.data
            );

            setBuyMessage(
                "Stock purchased successfully!"
            );

            setTimeout(() => {
                navigate("/portfolio");
            }, 1000);

        } catch (error) {

            console.log(
                "Buy failed:",
                error
            );

            if (error.response) {

                setBuyError(
                    error.response.data.message ||
                    "Failed to purchase stock."
                );

            } else {

                setBuyError(
                    "Something went wrong."
                );
            }

        } finally {

            setBuyLoading(false);
        }
    }

    async function handleSell() {

        setSellMessage("");
        setSellError("");

        if (!quantity || quantity <= 0) {
            setSellError("Quantity must be at least 1.");
            return;
        }

        setSellLoading(true);

        try {

            const request = {
                userId: user.id,
                stockId: stock.id,
                quantity: quantity
            };

            const response =
                await api.post("/orders/sell", request);

            console.log(
                "Sell successful:",
                response.data
            );

            setSellMessage(
                "Stock sold successfully!"
            );

            setTimeout(() => {
                navigate("/portfolio");
            }, 1000);

        } catch (error) {

            console.log(
                "Sell failed:",
                error
            );

            if (error.response) {

                setSellError(
                    error.response.data.message ||
                    "Failed to sell stock."
                );

            } else {

                setSellError(
                    "Something went wrong."
                );
            }

        } finally {

            setSellLoading(false);
        }
    }


    if (loading) {
        return <h2>Loading stock...</h2>;
    }


    if (error) {
        return <h2>{error}</h2>;
    }

    const priceChange =
        stock.currentPrice - stock.previousClose;

    const priceChangePercent =
        (priceChange / stock.previousClose) * 100;

    return (
        <div className="stock-details-page">
            <div className="stock-details-header">

                <div>
                    <div className="stock-details-symbol">
                        {stock.symbol}
                    </div>

                    <p className="stock-details-company">
                        {stock.companyName}
                    </p>
                </div>

                <div>
                    <div className="stock-details-price">
                        ₹{stock.currentPrice}
                    </div>

                    <p
                        className={
                            priceChange >= 0
                                ? "stock-change-positive"
                                : "stock-change-negative"
                        }
                    >
                        {priceChange >= 0 ? "+" : ""}
                        ₹{priceChange.toFixed(2)}
                        {" "}
                        (
                        {priceChangePercent >= 0 ? "+" : ""}
                        {priceChangePercent.toFixed(2)}%
                        )
                    </p>
                </div>

            </div>

            <div className="stock-details-info">

                <div className="stock-info-card">
                    <div className="stock-info-label">
                        Day High
                    </div>

                    <div className="stock-info-value">
                        ₹{stock.dayHigh}
                    </div>
                </div>

                <div className="stock-info-card">
                    <div className="stock-info-label">
                        Day Low
                    </div>

                    <div className="stock-info-value">
                        ₹{stock.dayLow}
                    </div>
                </div>

                <div className="stock-info-card">
                    <div className="stock-info-label">
                        Previous Close
                    </div>

                    <div className="stock-info-value">
                        ₹{stock.previousClose}
                    </div>
                </div>

            </div>

            <div className="stock-trade-box">

                <h3>
                    Trade {stock.symbol}
                </h3>

                <div className="stock-quantity">

                    <label>
                        Quantity
                    </label>

                    <input
                        type="number"
                        min="1"
                        value={quantity}
                        onChange={(event) =>
                            setQuantity(
                                Number(event.target.value)
                            )
                        }
                    />

                </div>

                <p className="stock-total">
                    Total: ₹
                    {stock.currentPrice * quantity}
                </p>

                <div className="stock-trade-buttons">

                    <button
                        className="stock-buy-button"
                        onClick={handleBuy}
                        disabled={buyLoading || sellLoading}
                    >
                        {buyLoading ? "Buying..." : "Buy"}
                    </button>

                    <button
                        className="stock-sell-button"
                        onClick={handleSell}
                        disabled={buyLoading || sellLoading}
                    >
                        {sellLoading ? "Selling..." : "Sell"}
                    </button>

                </div>

                {buyMessage && (
                    <p className="stock-success">
                        {buyMessage}
                    </p>
                )}

                {buyError && (
                    <p className="stock-error">
                        {buyError}
                    </p>
                )}

                {sellMessage && (
                    <p className="stock-success">
                        {sellMessage}
                    </p>
                )}

                {sellError && (
                    <p className="stock-error">
                        {sellError}
                    </p>
                )}

                <div className="stock-details-actions">

                    {isInWatchlist ? (

                        <button
                            className="stock-watchlist-button"
                            onClick={removeFromWatchlist}
                        >
                            ★ Remove from Watchlist
                        </button>

                    ) : (

                        <button
                            className="stock-watchlist-button"
                            onClick={addToWatchlist}
                        >
                            ⭐ Add to Watchlist
                        </button>

                    )}

                    <button
                        className="stock-back-button"
                        onClick={() => navigate("/stocks")}
                    >
                        Back to Stocks
                    </button>

                </div>
            </div>
        </div>
    );
}

export default StockDetails;