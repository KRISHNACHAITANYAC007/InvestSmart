import { BrowserRouter, Routes, Route } from "react-router-dom";
import Login from "./pages/Login.jsx";

import Dashboard from "./pages/Dashboard.jsx";
import ProtectedRoute from "./ProtectedRoute.jsx";
import Layout from "./Layout.jsx";
import Stocks from "./pages/Stocks.jsx";
import Watchlist from "./pages/Watchlist.jsx";
import Portfolio from "./pages/Portfolio.jsx";
import Orders from "./pages/Orders.jsx";
import Wallet from "./pages/Wallet.jsx";
import StockDetails from "./pages/StockDetails.jsx";
import Register from "./pages/Register.jsx";
function App() {
    return (
        <BrowserRouter>

            <Routes>

                <Route
                    path="/login"
                    element={<Login />}
                />

                <Route path="/register" element={<Register />} />

                <Route
                    path = "/dashboard"
                    element = {
                   <ProtectedRoute>
                       <Layout><Dashboard /></Layout>
                   </ProtectedRoute>
                   }
                />

                <Route
                    path="/stocks"
                    element={
                        <ProtectedRoute>
                            <Layout>
                                <Stocks />
                            </Layout>
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/watchlist"
                    element={
                        <ProtectedRoute>
                            <Layout>
                                <Watchlist />
                            </Layout>
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/portfolio"
                    element={
                        <ProtectedRoute>
                            <Layout>
                                <Portfolio />
                            </Layout>
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/orders"
                    element={
                        <ProtectedRoute>
                            <Layout>
                                <Orders />
                            </Layout>
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/wallet"
                    element={
                        <ProtectedRoute>
                            <Layout>
                                <Wallet />
                            </Layout>
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/stocks/:id"
                    element={
                        <ProtectedRoute>
                            <Layout>
                                <StockDetails />
                            </Layout>
                        </ProtectedRoute>
                    }
                />



            </Routes>

        </BrowserRouter>
    );
}

export default App;