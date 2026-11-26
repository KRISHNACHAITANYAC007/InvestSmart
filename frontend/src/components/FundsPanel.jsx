import { useRef,useState } from "react";
import api from "../api/api.js";
import "./FundsPanel.css";

export default function FundsPanel({ balance,onChanged }) {
    const [amount,setAmount]=useState(""),[busy,setBusy]=useState(false),[error,setError]=useState(""),[message,setMessage]=useState("");
    const busyRef=useRef(false);
    async function change(action){
        if(busyRef.current)return;
        const value=Number(amount);
        if(!Number.isFinite(value)||value<=0||Math.abs(value*100-Math.round(value*100))>0.000001){setError("Enter a positive amount with at most two decimal places.");return;}
        busyRef.current=true;setBusy(true);setError("");setMessage("");
        try {
            await api.post(`/wallet/${action}`,{amount:value});setAmount("");setMessage(`Virtual funds ${action==="deposit"?"added":"withdrawn"} successfully.`);
            await onChanged?.();
        } catch(failure){setError(failure.response?.data?.message||"The request could not be confirmed. Refresh your balance before trying again.");}
        finally {busyRef.current=false;setBusy(false);}
    }
    return <section className="wallet-page" aria-label="Virtual funds"><div className="wallet-header"><h2>Virtual funds</h2><p>Manage your paper-trading balance. These actions move virtual money only.</p></div>
        <div className="wallet-balance-card"><p className="wallet-balance-label">Available balance</p><h2>{Number(balance).toLocaleString("en-IN",{style:"currency",currency:"INR"})}</h2></div>
        <div className="wallet-content-grid">
            <div className="wallet-action-card"><h2>Add or withdraw virtual funds</h2><div className="wallet-input-group"><label htmlFor="virtual-funds-amount">Amount</label><input id="virtual-funds-amount" type="number" min="0.01" step="0.01" value={amount} disabled={busy} onChange={e=>setAmount(e.target.value)} placeholder="Enter amount" /></div>
                <div className="wallet-amount-shortcuts" aria-label="Quick amount choices">
                    <span>Quick amounts</span>
                    {[1000, 5000, 10000].map(value => (
                        <button key={value} type="button" disabled={busy} onClick={() => {setAmount(String(value));setError("");setMessage("");}}>
                            ₹{value.toLocaleString("en-IN")}
                        </button>
                    ))}
                </div>
                <div className="wallet-buttons">
                    <button className="wallet-deposit-button" disabled={busy} onClick={()=>change("deposit")}>Add virtual funds</button>
                    <button className="wallet-withdraw-button" disabled={busy} onClick={()=>change("withdraw")}>Withdraw virtual funds</button>
                    <button className="wallet-refresh-button" disabled={busy} onClick={()=>onChanged?.()}>Refresh balance</button>
                </div>
                <p className="wallet-amount-note">Enter a positive amount with up to two decimal places. Withdrawals use settled cash only; reusable and restricted T+1 credits are not withdrawable. See the settlement breakdown below. All funds are simulated.</p>
                {busy&&<p role="status">Updating virtual funds…</p>}{message&&<p className="wallet-success" role="status">{message}</p>}{error&&<p className="wallet-error" role="alert">{error}</p>}
            </div>
            <aside className="wallet-guide-card" aria-labelledby="wallet-guide-title">
                <span className="wallet-guide-label">PAPER TRADING</span>
                <h2 id="wallet-guide-title">Practice at your own pace</h2>
                <p>Your balance is simulated for learning and practice. These controls do not move real money.</p>
                <ul>
                    <li><strong>Add funds</strong><span>Increase your virtual balance for practice trades.</span></li>
                    <li><strong>Withdraw</strong><span>Reduce the virtual cash available in your account.</span></li>
                    <li><strong>Refresh</strong><span>Load your latest balance after an update.</span></li>
                </ul>
            </aside>
        </div>
    </section>;
}
