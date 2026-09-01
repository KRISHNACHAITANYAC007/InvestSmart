function Welcome(props) {
    return (
        <div>
            <h2>Welcome back {props.name}</h2>
            <p>{props.message}</p>
        </div>
    );
}

export default Welcome;