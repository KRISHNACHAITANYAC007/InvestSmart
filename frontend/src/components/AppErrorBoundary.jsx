import { Component } from "react";

export default class AppErrorBoundary extends Component {
    state = { hasError: false };

    static getDerivedStateFromError() {
        return { hasError: true };
    }

    componentDidCatch(error, info) {
        console.error("InvestSmart workspace render failed", error, info.componentStack);
    }

    render() {
        if (this.state.hasError) {
            return <main className="session-loading" role="alert">
                <h1>This page could not be displayed</h1>
                <p>Refresh the page to try again. If it keeps happening, check the browser console for the render error.</p>
                <button className="ui-button" onClick={() => window.location.reload()}>Refresh page</button>
            </main>;
        }
        return this.props.children;
    }
}
