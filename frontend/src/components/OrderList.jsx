import "../styles/Cards.css";

function OrderList({ orders }) {

    return (
        <div>


            {orders.length === 0 ? (
                <p>No orders yet.</p>
            ) : (
                <div className="order-list">

                    <table className="order-table">

                        <thead>
                        <tr>
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
                                <td>{order.orderType}</td>
                                <td>{order.quantity}</td>
                                <td>₹{order.price}</td>
                                <td>₹{order.totalAmount}</td>
                                <td>{order.status}</td>
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