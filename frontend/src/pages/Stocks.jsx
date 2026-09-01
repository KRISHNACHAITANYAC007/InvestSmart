import { useEffect, useState } from "react";
import api from "../api/api.js";
import StockList from "../components/StockList.jsx";
import "./Stocks.css";
function Stocks() {

    const [stocks, setStocks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {

        api.get("/stocks")
            .then((response) => {
                setStocks(response.data);
                setLoading(false);
            })
            .catch((error) => {
                console.log("Failed to fetch stocks:", error);
                setError("Failed to load stocks.");
                setLoading(false);
            });

    }, []);

    if (loading) {
        return <h2>Loading stocks...</h2>;
    }

    if (error) {
        return (
            <div>
                <h2>{error}</h2>

                <button onClick={() => window.location.reload()}>
                    Retry
                </button>
            </div>
        );
    }

    return (
        <div className="stocks-page">

            <div className="stocks-header">
                <h1>Stocks</h1>
                <p>Explore available stocks and start investing.</p>
            </div>

            <StockList stocks={stocks} />

        </div>
    );
}

export default Stocks;