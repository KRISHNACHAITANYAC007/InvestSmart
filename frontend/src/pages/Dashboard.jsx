import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/api.js";
import PortfolioCard from "../components/PortfolioCard.jsx";
import BalanceCard from "../components/BalanceCard.jsx";
import StockList from "../components/StockList.jsx";
import OrderList from "../components/OrderList.jsx";
import "../styles/Dashboard.css";

function Dashboard() {

    const navigate = useNavigate();

    const [portfolio, setPortfolio] = useState(null);
    const [balance, setBalance] = useState(null);
    const [orders, setOrders] = useState([]);
    const [stocks, setStocks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);


    function fetchDashboardData() {

        setLoading(true);
        setError(null);

        Promise.all([
            api.get("/portfolio/me"),
            api.get("/wallet/me"),
            api.get("/orders/me"),
            api.get("/stocks")
        ])
            .then(([portfolioResponse, balanceResponse, ordersResponse, stocksResponse]) => {

                setPortfolio(portfolioResponse.data);
                setBalance(balanceResponse.data);
                setOrders(ordersResponse.data);
                setStocks(stocksResponse.data);

                setLoading(false);
            })
            .catch((error) => {

                console.log(
                    "Failed to fetch dashboard data:",
                    error
                );

                setError("Failed to load dashboard data.");
                setLoading(false);
            });
    }


    useEffect(() => {

        Promise.all([
            api.get("/portfolio/me"),
            api.get("/wallet/me"),
            api.get("/orders/me"),
            api.get("/stocks")
        ])
            .then(([portfolioResponse, balanceResponse, ordersResponse, stocksResponse]) => {

                setPortfolio(portfolioResponse.data);
                setBalance(balanceResponse.data);
                setOrders(ordersResponse.data);
                setStocks(stocksResponse.data);

                setLoading(false);
            })
            .catch((error) => {

                console.log(
                    "Failed to fetch dashboard data:",
                    error
                );

                setError("Failed to load dashboard data.");
                setLoading(false);
            });

    }, []);


    if (loading) {
        return <h2>Loading dashboard...</h2>;
    }


    if (error) {
        return (
            <div>
                <h2>{error}</h2>

                <button onClick={fetchDashboardData}>
                    Retry
                </button>
            </div>
        );
    }


    const recentOrders = orders.slice(0, 5);


    return (
        <div className="dashboard">

            <header>
                <h1>Dashboard</h1>
            </header>

            <main>

                <section className="dashboard-summary">

                    {portfolio && (
                        <PortfolioCard portfolio={portfolio} />
                    )}

                    {balance !== null && (
                        <BalanceCard balance={balance} />
                    )}

                </section>


                <section className="dashboard-section">
                    <StockList stocks={stocks} />
                </section>


                <section className="dashboard-section dashboard-orders-section">

                    <div className="recent-orders-header">
                        <h2>Recent Orders</h2>

                        {orders.length > 5 && (
                            <button
                                className="show-more-orders"
                                onClick={() => navigate("/orders")}
                            >
                                Show More →
                            </button>
                        )}
                    </div>

                    <OrderList orders={recentOrders} />

                </section>

            </main>

        </div>
    );
}

export default Dashboard;