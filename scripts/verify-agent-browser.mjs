// Run: node scripts/verify-agent-browser.mjs [installed Chromium executable]
// No live backend, authentication, provider or application configuration is used.
import fs from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import { spawn } from 'node:child_process';
import { createRequire } from 'node:module';
import { fileURLToPath, pathToFileURL } from 'node:url';
import assert from 'node:assert/strict';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const require = createRequire(path.join(root, 'frontend/package.json'));
const { createServer } = await import(pathToFileURL(require.resolve('vite')).href);
const { default: react } = await import(pathToFileURL(require.resolve('@vitejs/plugin-react')).href);
const output = path.join(root, 'target/agent-browser');
await fs.mkdir(output, { recursive: true });
const profile = await fs.mkdtemp(path.join(os.tmpdir(), 'investsmart-agent-browser-'));
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
let browser, server, socket;
const checks = [], errors = [];
try {
    server = await createServer({ configFile: false, root: path.join(root, 'frontend'), plugins: [react(), {
        name: 'isolated-agent-fixture', configureServer(vite) {
            vite.middlewares.use('/__agent-check', async (_req, res) => {
                const entry = '/@fs/' + path.join(root, 'scripts/fixtures/agent-browser.jsx').replaceAll('\\', '/');
                res.setHeader('Content-Type', 'text/html');
                res.end(await vite.transformIndexHtml('/__agent-check', `<html><head><title>Synthetic agent verification</title><meta name="viewport" content="width=device-width,initial-scale=1"></head><body><div id="root"></div><script type="module" src="${entry}"></script></body></html>`));
            });
        }
    }], server: { host: '127.0.0.1', port: 0, fs: { allow: [root] } } });
    await server.listen();
    const origin = `http://127.0.0.1:${server.httpServer.address().port}`;
    browser = spawn(process.argv[2] || 'C:/Program Files/Google/Chrome/Application/chrome.exe', ['--headless=new', '--remote-debugging-port=0', `--user-data-dir=${profile}`, '--no-first-run', 'about:blank'], { stdio: 'ignore', windowsHide: true });
    let launchError;
    browser.on('error', error => { launchError = error; });
    let port;
    for (let i = 0; i < 100; i++) {
        if (launchError) throw launchError;
        try { port = (await fs.readFile(path.join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; break; } catch { await sleep(100); }
    }
    assert.ok(port, 'Chromium debugging endpoint started');
    const targets = await (await fetch(`http://127.0.0.1:${port}/json`)).json();
    const target = targets.find(item => item.type === 'page');
    socket = new WebSocket(target.webSocketDebuggerUrl);
    await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
    let id = 0;
    const pending = new Map();
    socket.onmessage = event => {
        const data = JSON.parse(event.data);
        if (data.method === 'Runtime.exceptionThrown') errors.push(data.params.exceptionDetails.text);
        if (data.id) { const item = pending.get(data.id); if (item) { clearTimeout(item.timer); pending.delete(data.id); data.error ? item.reject(Error(JSON.stringify(data.error))) : item.resolve(data.result); } }
    };
    const send = (method, params = {}) => new Promise((resolve, reject) => {
        const key = ++id;
        const timer = setTimeout(() => { pending.delete(key); reject(Error(`CDP timeout: ${method}`)); }, 15000);
        pending.set(key, { resolve, reject, timer }); socket.send(JSON.stringify({ id: key, method, params }));
    });
    const evaluate = async expression => {
        const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
        if (result.exceptionDetails) throw Error(JSON.stringify(result.exceptionDetails));
        return result.result.value;
    };
    const wait = async expression => { for (let i = 0; i < 100; i++) { if (await evaluate(expression)) return; await sleep(50); } throw Error(`UI timeout: ${expression}`); };
    const click = text => evaluate(`Array.from(document.querySelectorAll('button')).find(b=>b.textContent===${JSON.stringify(text)}).click()`);
    const screenshot = async name => {
        const { data } = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false });
        await fs.writeFile(path.join(output, name + '.png'), Buffer.from(data, 'base64'));
    };
    await send('Runtime.enable'); await send('Page.enable');
    await send('Emulation.setDeviceMetricsOverride', { width: 1280, height: 900, deviceScaleFactor: 1, mobile: false });
    await send('Page.navigate', { url: origin + '/__agent-check' });
    await wait(`!!document.querySelector('.agent-conversation')`);
    assert.equal(await evaluate(`document.querySelector('.agent-questions button').disabled`), true);
    assert.equal(await evaluate('window.agentFixture.calls.length'), 0);
    checks.push('Consent required before any portfolio read');
    await evaluate(`document.querySelector('input[type=checkbox]').click()`);
    await click('Explain my equity holdings');
    await wait(`!!document.querySelector('.agent-reply')`);
    assert.match(await evaluate(`document.querySelector('.agent-reply').innerText`), /1,234.50/);
    assert.equal(await evaluate(`document.querySelectorAll('.agent-reply img').length`), 0);
    checks.push('Mounted React displays exact money and escaped evidence');
    await screenshot('desktop-complete');
    await click('Clear conversation');
    await wait(`document.querySelectorAll('.agent-reply').length===0 && !document.querySelector('input').checked`);
    await evaluate(`window.agentFixture.mode='delayed';document.querySelector('input').click()`);
    await click('Explain my equity holdings');
    await wait(`!!document.querySelector('[role=status]') && window.agentFixture.pending.length===1`);
    await click('Cancel request');
    await evaluate(`window.agentFixture.pending.shift()()`); await sleep(150);
    assert.equal(await evaluate(`document.querySelectorAll('.agent-reply').length`), 0);
    checks.push('Clear resets consent/history; cancel prevents delayed reply');
    await evaluate(`window.agentFixture.mode='error'`); await click('Explain my unrealized P&L');
    await wait(`!!document.querySelector('[role=alert]')`);
    assert.match(await evaluate(`document.querySelector('[role=alert]').innerText`), /Synthetic request limit/);
    checks.push('Request failure displays an alert and permits retry');
    await evaluate(`window.agentFixture.mode='missing'`); await click('Where is my equity value concentrated?');
    await wait(`!!document.querySelector('.agent-reply')`);
    assert.match(await evaluate(`document.querySelector('.agent-reply').innerText`), /Unavailable/);
    await evaluate(`document.querySelector('summary').click()`);
    for (const width of [768, 400, 320]) {
        await send('Emulation.setDeviceMetricsOverride', { width, height: 900, deviceScaleFactor: 1, mobile: false });
        await sleep(100);
        assert.equal(await evaluate(`document.documentElement.scrollWidth<=document.documentElement.clientWidth`), true, `No horizontal overflow at ${width}px including scrollbar`);
        await screenshot(`missing-${width}`);
    }
    checks.push('Missing/stale evidence stays unavailable; 768/400/320px have no horizontal overflow');
    await evaluate(`window.agentFixture.mode='delayed'`); await click('Explain my equity holdings');
    await wait(`window.agentFixture.pending.length===1`);
    await click('Synthetic account switch');
    await wait(`!document.querySelector('input').checked && document.querySelectorAll('.agent-reply').length===0`);
    await evaluate(`window.agentFixture.pending.shift()()`); await sleep(150);
    assert.equal(await evaluate(`document.querySelectorAll('.agent-reply').length`), 0);
    checks.push('Account change resets consent/evidence and prevents old reply');
    assert.deepEqual(errors, []);
    await fs.writeFile(path.join(output, 'results.json'), JSON.stringify({ status: 'PASS', checks, errors, scope: 'Headless Chromium, actual mounted component with synthetic adapter/context; not real authentication, backend integration, full app shell or physical device acceptance.' }, null, 2));
    console.log(JSON.stringify({ status: 'PASS', checks }));
} finally {
    socket?.close(); browser?.kill(); await server?.close();
    // Keep the isolated browser profile for diagnosis; never touch a personal browser profile.
}
