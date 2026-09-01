import { useEffect, useState } from "react";
import api from "../api/api.js";
import "./Wallet.css";

function Wallet() {

    const [balance, setBalance] = useState(null);
    const [amount, setAmount] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");

    useEffect(() => {

        api.get("/wallet/me")
            .then((response) => {

                setBalance(response.data);
                setLoading(false);

            })
            .catch((error) => {

                console.log("Failed to fetch wallet:", error);

                setError("Failed to load wallet.");
                setLoading(false);
            });

    }, []);


    const handleDeposit = async () => {

        if (!amount || Number(amount) <= 0) {
            setError("Enter a valid amount.");
            return;
        }

        setError("");
        setMessage("");

        try {

            const response = await api.post(
                "/wallet/deposit",
                {
                    amount: Number(amount)
                }
            );

            setBalance(response.data.balance);

            setAmount("");

            setMessage(
                "Money deposited successfully."
            );

        } catch (error) {

            console.log("Deposit failed:", error);

            setError(
                error.response?.data?.message ||
                "Deposit failed."
            );
        }
    };


    const handleWithdraw = async () => {

        if (!amount || Number(amount) <= 0) {
            setError("Enter a valid amount.");
            return;
        }

        setError("");
        setMessage("");

        try {

            const response = await api.post(
                "/wallet/withdraw",
                {
                    amount: Number(amount)
                }
            );

            setBalance(response.data.balance);

            setAmount("");

            setMessage(
                "Money withdrawn successfully."
            );

        } catch (error) {

            console.log("Withdraw failed:", error);

            setError(
                error.response?.data?.message ||
                "Withdrawal failed."
            );
        }
    };


    if (loading) {
        return <h2>Loading wallet...</h2>;
    }


    if (balance === null) {
        return <h2>Unable to load wallet.</h2>;
    }


    return (
        <div className="wallet-page">

            <div className="wallet-header">
                <h1>Wallet</h1>
                <p>Manage your available trading balance.</p>
            </div>


            <div className="wallet-balance-card">

                <p className="wallet-balance-label">
                    Available Balance
                </p>

                <h2>
                    ₹{balance}
                </h2>

            </div>


            <div className="wallet-action-card">

                <h2>
                    Add or Withdraw Money
                </h2>

                <p className="wallet-action-description">
                    Enter an amount and choose an action.
                </p>


                <div className="wallet-input-group">

                    <label>
                        Amount
                    </label>

                    <input
                        type="number"
                        min="1"
                        value={amount}
                        onChange={(event) =>
                            setAmount(event.target.value)
                        }
                        placeholder="Enter amount"
                    />

                </div>


                <div className="wallet-buttons">

                    <button
                        className="wallet-deposit-button"
                        onClick={handleDeposit}
                    >
                        Deposit
                    </button>

                    <button
                        className="wallet-withdraw-button"
                        onClick={handleWithdraw}
                    >
                        Withdraw
                    </button>

                </div>


                {message && (
                    <p className="wallet-success">
                        {message}
                    </p>
                )}

                {error && (
                    <p className="wallet-error">
                        {error}
                    </p>
                )}

            </div>

        </div>
    );
}

export default Wallet;