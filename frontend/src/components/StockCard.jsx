function StockCard(props) {
    return (
        <div>
            <h2>{props.symbol}</h2>
            <p>{props.company}</p>
            <p>₹{props.price}</p>
            <p>{props.change}%</p>
        </div>
    );
}

export default StockCard;