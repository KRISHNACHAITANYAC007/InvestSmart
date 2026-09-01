import { useEffect, useState } from "react";
import api from "../api/api.js";
import "./Orders.css";

function Orders() {

    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {

        api.get("/orders/me")
            .then((response) => {

                setOrders(response.data);
                setLoading(false);

            })
            .catch((error) => {

                console.log(
                    "Failed to fetch orders:",
                    error
                );

                setError(
                    "Failed to load orders."
                );

                setLoading(false);
            });

    }, []);


    if (loading) {
        return <h2>Loading orders...</h2>;
    }


    if (error) {
        return <h2>{error}</h2>;
    }


    return (
        <div className="orders-page">

            <div className="orders-header">
                <h1>Orders</h1>
                <p>View your complete trading history.</p>
            </div>

            {orders.length === 0 ? (

                <div className="orders-empty">
                    <p>
                        You don't have any orders yet.
                    </p>
                </div>

            ) : (

                <div className="orders-list">

                    {[...orders]
                        .sort((a, b) => b.id - a.id)
                        .map((order) => (

                        <div
                            className="order-card"
                            key={order.id}
                        >

                            <div className="order-card-header">

                                <div>
                                    <h3>
                                        {order.orderType} - {order.symbol}
                                    </h3>

                                    <p>
                                        {order.companyName}
                                    </p>
                                </div>

                                <span
                                    className={
                                        order.orderType === "BUY"
                                            ? "order-type-buy"
                                            : "order-type-sell"
                                    }
                                >
                                    {order.orderType}
                                </span>

                            </div>


                            <div className="order-details">

                                <div>
                                    <span>Quantity</span>
                                    <strong>
                                        {order.quantity}
                                    </strong>
                                </div>

                                <div>
                                    <span>Price</span>
                                    <strong>
                                        ₹{order.price}
                                    </strong>
                                </div>

                                <div>
                                    <span>Total Amount</span>
                                    <strong>
                                        ₹{order.totalAmount}
                                    </strong>
                                </div>

                                <div>
                                    <span>Status</span>
                                    <strong className="order-status">
                                        {order.status}
                                    </strong>
                                </div>

                            </div>

                        </div>

                    ))}

                </div>

            )}

        </div>
    );
}

export default Orders;