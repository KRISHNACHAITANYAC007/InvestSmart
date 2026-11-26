import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import AgentConversation from '../../frontend/src/pages/AgentConversation.jsx';
import { AuthContext } from '../../frontend/src/context/authContext.js';
import api from '../../frontend/src/api/api.js';
import '../../frontend/src/index.css';
import '../../frontend/src/styles/ui.css';

// Synthetic adapter only in this standalone fixture; the application never imports it.
window.agentFixture = { mode: 'complete', calls: [], pending: [] };
api.defaults.adapter = async config => {
    if (config.url === '/auth/csrf') return { data: { token: 'synthetic' }, status: 200, config };
    const fixture = window.agentFixture;
    fixture.calls.push(JSON.parse(config.data));
    const mode = fixture.mode;
    if (mode === 'delayed') await new Promise(resolve => fixture.pending.push(resolve));
    if (mode === 'error') throw { response: { status: 429, data: { message: 'Synthetic request limit reached.' } } };
    const missing = mode === 'missing';
    return { status: 200, config, data: { mode: 'LOCAL_DETERMINISTIC', analysis: {
        status: missing ? 'INCOMPLETE_VALUATION' : 'COMPLETE_EQUITY_VIEW',
        equityValue: missing ? null : '1234.50', equityUnrealizedPnl: missing ? null : '-12.50',
        largestListings: missing ? [] : [{ instrumentKey: 'SYNTHETIC_LISTING', equityWeightPercent: '100.00' }],
        explanation: [missing ? 'Stale evidence: total valuation is unavailable.' : 'Synthetic equity evidence only.', '<img src=x onerror=alert(1)>'],
        evidence: { readAt: '2026-11-24T08:00:00Z', holdings: [{ instrumentKey: 'SYNTHETIC_LISTING', symbol: 'SYNTHETIC', quantity: 1, value: missing ? null : '1234.50', quoteAt: missing ? null : '2026-11-24T08:00:00Z', availability: missing ? 'UNAVAILABLE_OR_STALE' : 'AVAILABLE' }] }
    } } };
};
function Fixture() {
    const [user, setUser] = useState({ id: 'synthetic-a' });
    return <><button id="switch-account" onClick={() => setUser({ id: 'synthetic-b' })}>Synthetic account switch</button>
        <AuthContext.Provider value={{ user }}><main className="app-layout"><AgentConversation /></main></AuthContext.Provider></>;
}
createRoot(document.getElementById('root')).render(<React.StrictMode><Fixture /></React.StrictMode>);
