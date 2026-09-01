import { useState } from "react";
import api from "../api/api";
import { useNavigate } from "react-router-dom";
import "./Register.css";

function Register() {

    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");

    const navigate = useNavigate();

    async function handleSubmit(event) {

        event.preventDefault();

        setError("");
        setMessage("");

        if (password !== confirmPassword) {
            setError("Passwords do not match.");
            return;
        }

        try {

            await api.post("/auth/register", {
                name: name,
                email: email,
                password: password
            });

            setMessage(
                "Account created successfully. You can now login."
            );

            setTimeout(() => {
                navigate("/login");
            }, 1000);

        } catch (error) {

            console.log(
                "Registration failed:",
                error
            );

            setError(
                error.response?.data?.message ||
                "Registration failed."
            );
        }
    }

    return (
        <div className="register-page">

            <div className="register-brand">

                <h1>InvestSmart</h1>

                <p>
                    Learn. Invest. Grow.
                </p>

            </div>


            <div className="register-section">

                <div className="register-box">

                    <h2>Create account</h2>

                    <p className="register-subtitle">
                        Start your investing journey
                    </p>


                    <form onSubmit={handleSubmit}>

                        <div className="register-field">

                            <label>Name</label>

                            <input
                                type="text"
                                placeholder="Enter your name"
                                value={name}
                                onChange={(event) =>
                                    setName(event.target.value)
                                }
                                required
                            />

                        </div>


                        <div className="register-field">

                            <label>Email</label>

                            <input
                                type="email"
                                placeholder="Enter your email"
                                value={email}
                                onChange={(event) =>
                                    setEmail(event.target.value)
                                }
                                required
                            />

                        </div>


                        <div className="register-field">

                            <label>Password</label>

                            <input
                                type="password"
                                placeholder="Create a password"
                                value={password}
                                onChange={(event) =>
                                    setPassword(event.target.value)
                                }
                                required
                            />

                        </div>


                        <div className="register-field">

                            <label>Confirm Password</label>

                            <input
                                type="password"
                                placeholder="Confirm your password"
                                value={confirmPassword}
                                onChange={(event) =>
                                    setConfirmPassword(
                                        event.target.value
                                    )
                                }
                                required
                            />

                        </div>


                        {error && (
                            <p className="register-error">
                                {error}
                            </p>
                        )}

                        {message && (
                            <p className="register-success">
                                {message}
                            </p>
                        )}


                        <button
                            className="register-button"
                            type="submit"
                        >
                            Create Account
                        </button>

                    </form>


                    <p className="register-login">
                        Already have an account?{" "}
                        <span onClick={() => navigate("/login")}>
                            Login
                        </span>
                    </p>

                </div>

            </div>

        </div>
    );
}

export default Register;