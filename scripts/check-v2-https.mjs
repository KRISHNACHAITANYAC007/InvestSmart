import https from 'node:https';
import { readFileSync } from 'node:fs';

// Trust only this isolated Caddy CA. Normal certificate/hostname validation stays enabled.
const ca = readFileSync(process.env.V2_SMOKE_CA_FILE);
function request(path, method = 'GET', payload = '{}') {
    return new Promise((resolve, reject) => {
        const oversized = method === 'POST' && Buffer.byteLength(payload) > 16_000_000;
        const req = https.request({ hostname: 'localhost', family: 4, port: 18443, path, method,
            ca, rejectUnauthorized: true, timeout: 20000,
            headers: method === 'POST' ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(payload), ...(oversized ? {Expect:'100-continue'} : {}) } : {} }, res => {
            let body = '';
            res.setEncoding('utf8');
            res.on('data', chunk => { body += chunk; if (body.length > 4 * 1024 * 1024) res.destroy(new Error('Response too large')); });
            res.on('error', reject);
            res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, body }));
        });
        req.on('timeout', () => req.destroy(new Error('HTTPS timeout')));
        req.on('error', reject);
        if (oversized) { req.on('continue',()=>req.end(payload));req.flushHeaders(); }
        else req.end(method === 'POST' ? payload : undefined);
    });
}
try {
    if ((await request('/api/health')).status !== 200) throw new Error('Health check failed');
    for (const path of ['/phone', '/market/chart/INFY']) {
        const result = await request(path);
        if (result.status !== 200 || !result.body.includes('<div id="root">')) throw new Error('SPA route failed');
        if (result.headers['strict-transport-security'] !== 'max-age=31536000' || result.headers['x-content-type-options'] !== 'nosniff' || result.headers['referrer-policy'] !== 'no-referrer') throw new Error('Browser security headers missing');
    }
    const csrf = await request('/api/auth/csrf');
    if (csrf.status !== 200 || !(csrf.headers['set-cookie'] || []).some(cookie => /^XSRF-TOKEN=/.test(cookie) && /;\s*Secure(?:;|$)/i.test(cookie))) throw new Error('Secure CSRF cookie missing');
    if ((await request('/api/auth/login', 'POST')).status !== 403) throw new Error('Missing CSRF was accepted');
    if ((await request('/api/auth/login', 'POST', Buffer.alloc(16_000_001, 32))).status !== 413) throw new Error('Oversized body was not rejected at the edge');
    console.log('PASS: trusted private-CA HTTPS, browser headers, body-size limit, SPA routes, secure cookie and CSRF rejection.');
} catch {
    console.error('Isolated HTTPS acceptance failed; certificate/hostname validation remains required.');
    process.exitCode = 1;
}
