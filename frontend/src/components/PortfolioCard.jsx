import "../styles/Cards.css";

function PortfolioCard({ portfolio }) {

    return (
        <div className="dashboard-card">

            <h2>Portfolio</h2>

            <p>
                Balance: ₹{portfolio.balance}
            </p>

            <p>
                Holdings Value: ₹{portfolio.holdingsValue}
            </p>

            <p className="dashboard-card-value">
                ₹{portfolio.totalValue}
            </p>

        </div>
    );
}

export default PortfolioCard;