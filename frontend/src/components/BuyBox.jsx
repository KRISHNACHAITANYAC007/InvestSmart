import { useState } from "react";

function BuyBox() {
    const [quantity, setQuantity] = useState(1);

    return (
        <div>
            <h2>Buy INFY</h2>

            <p>Current Price: ₹1799</p>
            <input
                type="number"
                value={quantity}
                onChange={(e) => setQuantity(Number(e.target.value))}
            />
            <button onClick={() => setQuantity(quantity + 1)}>
                +
            </button>



            <button
                onClick={() => {
                    if (quantity > 1) {
                        setQuantity(quantity - 1);
                    }
                }}
            >
                -
            </button>

            <p>Quantity: {quantity}</p>
            <p>Total: ₹{quantity * 1799}</p>
        </div>
    );
}

export default BuyBox;