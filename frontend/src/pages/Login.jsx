import { useState } from "react";
import api from "../api/api";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import "./Login.css";

function Login() {

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const { login } = useAuth();
    const navigate = useNavigate();

    async function handleSubmit(event) {
        event.preventDefault();

        try {
            const response = await api.post("/auth/login", {
                email: email,
                password: password
            });

            const token = response.data;

            login(token);

            console.log("Login successful");

            navigate("/dashboard");

        } catch (error) {
            console.log("Login failed:", error);
        }
    }

    return (
        <div className="login-page">

            <div className="login-brand">

                <h1>InvestSmart</h1>

                <p>
                    Learn. Invest. Grow.
                </p>

            </div>


            <div className="login-section">

                <div className="login-box">

                    <h2>Login</h2>

                    <p className="login-subtitle">
                        Sign in to your account
                    </p>


                    <form onSubmit={handleSubmit}>

                        <div className="login-field">

                            <label>Email</label>

                            <input
                                type="email"
                                placeholder="Enter your email"
                                value={email}
                                onChange={(event) =>
                                    setEmail(event.target.value)
                                }
                            />

                        </div>


                        <div className="login-field">

                            <label>Password</label>

                            <input
                                type="password"
                                placeholder="Enter your password"
                                value={password}
                                onChange={(event) =>
                                    setPassword(event.target.value)
                                }
                            />

                        </div>


                        <button
                            className="login-button"
                            type="submit"
                        >
                            Login
                        </button>



                    </form>
                    <br/>

                    <p className="login-register">
                        Don't have an account?{" "}
                        <span onClick={() => navigate("/register")} className="login-reg">
                            Register
                        </span>
                    </p>


                </div>

            </div>

        </div>
    );
}

export default Login;