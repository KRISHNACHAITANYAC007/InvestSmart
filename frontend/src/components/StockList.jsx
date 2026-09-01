import { useNavigate } from "react-router-dom";
import "../styles/Cards.css";

function StockList({ stocks }) {

    const navigate = useNavigate();

    return (
        <div>


            {stocks.length === 0 ? (
                <p>No stocks available.</p>
            ) : (
                <div className="stock-list">

                    {stocks.map((stock) => {

                        const priceChange =
                            stock.currentPrice - stock.previousClose;

                        const priceChangePercent =
                            (priceChange / stock.previousClose) * 100;

                        return (
                            <div
                                className="stock-item"
                                key={stock.id}
                                onClick={() =>
                                    navigate(`/stocks/${stock.id}`)
                                }
                            >

                                <div className="stock-item-header">

                                    <div>
                                        <div className="stock-symbol">
                                            {stock.symbol}
                                        </div>

                                        <p>
                                            {stock.companyName}
                                        </p>
                                    </div>

                                    <div>
                                        <div className="stock-price">
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

                                <div className="stock-stats">

                                    <div>
                                        <span>Day High</span>
                                        <strong>₹{stock.dayHigh}</strong>
                                    </div>

                                    <div>
                                        <span>Day Low</span>
                                        <strong>₹{stock.dayLow}</strong>
                                    </div>

                                    <div>
                                        <span>Prev. Close</span>
                                        <strong>₹{stock.previousClose}</strong>
                                    </div>

                                </div>

                            </div>
                        );
                    })}

                </div>
            )}
        </div>
    );
}

export default StockList;