import "../styles/Cards.css";

function PortfolioCard({ portfolio }) {
    const money = value => value == null ? "Unavailable" : Number(value).toLocaleString("en-IN",{style:"currency",currency:"INR"});

    return (
        <div className="dashboard-card">

            <h2>Portfolio</h2>

            <p>
                Balance: {money(portfolio.balance)}
            </p>

            <p>
                Holdings Value: {money(portfolio.holdingsValue)}
            </p>

            <p className="dashboard-card-value">
                {money(portfolio.totalValue)}
            </p>

        </div>
    );
}

export default PortfolioCard;
