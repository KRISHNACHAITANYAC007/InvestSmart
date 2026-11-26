import "../styles/Cards.css";

function OrderList({ orders }) {
    const money = value => value == null ? "Unavailable" : Number(value).toLocaleString("en-IN", { style: "currency", currency: "INR", minimumFractionDigits: 2, maximumFractionDigits: 2 });

    return (
        <div>


            {orders.length === 0 ? (
                <p>No orders yet.</p>
            ) : (
                <div className="order-list" role="region" aria-label="Trade history" tabIndex={0}>

                    <table className="order-table">

                        <thead>
                        <tr>
                            <th>Stock</th>
                            <th>Type</th>
                            <th>Quantity</th>
                            <th>Price</th>
                            <th>Total</th>
                            <th>Status</th>
                        </tr>
                        </thead>

                        <tbody>
                        {orders.map((order) => (
                            <tr key={order.id}>
                                <td>
                                    <strong>{order.companyName || order.symbol || "—"}</strong>
                                    {order.companyName && order.symbol && (
                                        <span className="order-stock-symbol">{order.symbol}</span>
                                    )}
                                </td>
                                <td><span className={`order-side ${order.orderType === "BUY" ? "is-buy" : "is-sell"}`}>{order.orderType}</span></td>
                                <td>{order.quantity}</td>
                                <td>{money(order.price)}</td>
                                <td>{money(order.totalAmount)}</td>
                                <td><span className="order-status-label">{order.status}</span></td>
                            </tr>
                        ))}
                        </tbody>

                    </table>

                </div>
            )}
        </div>
    );
}

export default OrderList;
