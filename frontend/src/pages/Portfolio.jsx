import { useEffect, useState } from "react";
import api from "../api/api.js";
import "./Portfolio.css";

function Portfolio() {

    const [portfolio, setPortfolio] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {

        api.get("/portfolio/me")
            .then((response) => {

                setPortfolio(response.data);
                setLoading(false);

            })
            .catch((error) => {

                console.log(
                    "Failed to fetch portfolio:",
                    error
                );

                setError(
                    "Failed to load portfolio."
                );

                setLoading(false);
            });

    }, []);


    if (loading) {
        return <h2>Loading portfolio...</h2>;
    }


    if (error) {
        return <h2>{error}</h2>;
    }


    return (
        <div className="portfolio-page">

            <div className="portfolio-header">
                <h1>Portfolio</h1>
                <p>Track your investments and holdings.</p>
            </div>


            <div className="portfolio-summary">

                <div className="portfolio-summary-card">
                    <p className="portfolio-summary-label">
                        Cash Balance
                    </p>

                    <h2>
                        ₹{portfolio.balance}
                    </h2>
                </div>


                <div className="portfolio-summary-card">
                    <p className="portfolio-summary-label">
                        Holdings Value
                    </p>

                    <h2>
                        ₹{portfolio.holdingsValue}
                    </h2>
                </div>


                <div className="portfolio-summary-card portfolio-total-card">
                    <p className="portfolio-summary-label">
                        Total Portfolio Value
                    </p>

                    <h2>
                        ₹{portfolio.totalValue}
                    </h2>
                </div>

            </div>


            <div className="portfolio-holdings-section">

                <h2 className="portfolio-section-title">
                    My Holdings
                </h2>


                {portfolio.holdings.length === 0 ? (

                    <div className="portfolio-empty">
                        <p>
                            You don't own any stocks yet.
                        </p>
                    </div>

                ) : (

                    <div className="portfolio-holdings">

                        {portfolio.holdings.map((holding) => (

                            <div
                                className="portfolio-holding-card"
                                key={holding.stockId}
                            >

                                <div className="portfolio-holding-header">

                                    <div>
                                        <h3>
                                            {holding.symbol}
                                        </h3>

                                        <p>
                                            {holding.companyName}
                                        </p>
                                    </div>

                                    <div className="portfolio-holding-value">
                                        ₹{holding.currentValue}
                                    </div>

                                </div>


                                <div className="portfolio-holding-details">

                                    <div>
                                        <span>
                                            Quantity
                                        </span>

                                        <strong>
                                            {holding.quantity}
                                        </strong>
                                    </div>


                                    <div>
                                        <span>
                                            Current Price
                                        </span>

                                        <strong>
                                            ₹{holding.currentPrice}
                                        </strong>
                                    </div>


                                    <div>
                                        <span>
                                            Current Value
                                        </span>

                                        <strong>
                                            ₹{holding.currentValue}
                                        </strong>
                                    </div>

                                </div>

                            </div>

                        ))}

                    </div>

                )}

            </div>

        </div>
    );
}

export default Portfolio;