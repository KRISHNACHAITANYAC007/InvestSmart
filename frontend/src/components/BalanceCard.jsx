import "../styles/Cards.css";

function BalanceCard({ balance }) {

    return (
        <div className="dashboard-card">

            <h2>Available Balance</h2>

            <p className="dashboard-card-value">
                ₹{balance}
            </p>

        </div>
    );
}

export default BalanceCard;