import { useEffect, useState } from "react";
import api from "../api/api.js";
import { useNavigate } from "react-router-dom";
import "./Watchlist.css";

function Watchlist() {

    const [watchlist, setWatchlist] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const navigate = useNavigate();

    useEffect(() => {

        api.get("/watchlist/me")
            .then((response) => {
                setWatchlist(response.data);
                setLoading(false);
            })
            .catch((error) => {
                console.log(
                    "Failed to fetch watchlist:",
                    error
                );

                setError("Failed to load watchlist.");
                setLoading(false);
            });

    }, []);

    const removeFromWatchlist = async (stockId) => {

        try {

            await api.delete(
                `/watchlist/me/${stockId}`
            );

            setWatchlist(
                watchlist.filter(
                    (item) => item.stockId !== stockId
                )
            );

        } catch (error) {

            console.log(
                "Failed to remove stock:",
                error
            );

            setError(
                "Failed to remove stock from watchlist."
            );
        }
    };

    if (loading) {
        return <h2>Loading watchlist...</h2>;
    }

    if (error) {
        return <h2>{error}</h2>;
    }

    return (
        <div className="watchlist-page">

            <div className="watchlist-header">
                <h1>Watchlist</h1>
                <p>Track the stocks you're interested in.</p>
            </div>

            {watchlist.length === 0 ? (

                <p>
                    Your watchlist is empty.
                </p>

            ) : (

                <div className="watchlist-list">

                    {watchlist.map((item) => (

                        <div
                            className="watchlist-item"
                            key={item.id}
                            onClick={() =>
                                navigate(
                                    `/stocks/${item.stockId}`
                                )
                            }
                        >

                            <h2 className="watchlist-symbol">
                                {item.symbol}
                            </h2>

                            <p className="watchlist-company">
                                {item.companyName}
                            </p>

                            <p className="watchlist-price">
                                ₹{item.currentPrice}
                            </p>

                            <p
                                className={
                                    item.currentPrice - item.previousClose >= 0
                                        ? "watchlist-change-positive"
                                        : "watchlist-change-negative"
                                }
                            >
                                {item.currentPrice - item.previousClose >= 0 ? "+" : ""}
                                ₹{(
                                item.currentPrice - item.previousClose
                            ).toFixed(2)}
                                {" "}
                                (
                                {item.currentPrice - item.previousClose >= 0 ? "+" : ""}
                                {(
                                    ((item.currentPrice - item.previousClose) /
                                        item.previousClose) *
                                    100
                                ).toFixed(2)}
                                %)
                            </p>
                            <p>
                                Day High:
                                ₹{item.dayHigh}
                            </p>

                            <p>
                                Day Low:
                                ₹{item.dayLow}
                            </p>

                            <p>
                                Previous Close:
                                ₹{item.previousClose}
                            </p>

                            <button
                                className="watchlist-remove-button"
                                onClick={(event) => {

                                    event.stopPropagation();

                                    removeFromWatchlist(
                                        item.stockId
                                    );

                                }}
                            >
                                Remove
                            </button>

                        </div>

                    ))}

                </div>

            )}

        </div>
    );
}

export default Watchlist;