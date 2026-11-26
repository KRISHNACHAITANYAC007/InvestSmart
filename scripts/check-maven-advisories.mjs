import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

export function queriesFromTree(tree) {
    if (!tree || !Array.isArray(tree.children)) throw new Error('Resolved Maven dependency tree is required');
    const coordinates = new Map();
    function visit(node) {
        if (node.scope) {
            if (![node.groupId, node.artifactId, node.version].every(value => typeof value === 'string' && value.trim())) throw new Error('Incomplete resolved Maven coordinate');
            const name = `${node.groupId}:${node.artifactId}`;
            coordinates.set(`${name}:${node.version}`, { package: { ecosystem: 'Maven', name }, version: node.version });
        }
        for (const child of node.children || []) visit(child);
    }
    visit(tree);
    if (!coordinates.size) throw new Error('No resolved dependencies; scan cannot certify an empty inventory');
    return [...coordinates.values()];
}
export function evaluateResponse(queries, response) {
    if (!response || response.error || !Array.isArray(response.results) || response.results.length !== queries.length) throw new Error('Incomplete or failed advisory response');
    const findings = [];
    response.results.forEach((result, index) => {
        if (!result || result.error || result.next_page_token || (result.vulns != null && !Array.isArray(result.vulns))) throw new Error('Incomplete advisory result; pagination or service error needs review');
        for (const vulnerability of result.vulns || []) {
            if (!vulnerability.id) throw new Error('Advisory identifier missing');
            findings.push({ name: queries[index].package.name, version: queries[index].version, id: vulnerability.id });
        }
    });
    return { scanned: queries.length, findings, complete: true, source: 'https://api.osv.dev/v1/querybatch' };
}
export function scan(treeFile, reportFile) {
    const queries = queriesFromTree(JSON.parse(readFileSync(treeFile, 'utf8')));
    // Only resolved package names/versions are transmitted; no source, credentials or project files.
    const result = spawnSync(process.platform === 'win32' ? 'curl.exe' : 'curl', ['--silent', '--show-error', '--fail', '--request', 'POST', '--header', 'Content-Type: application/json', '--data-binary', '@-', '--connect-timeout', '10', '--max-time', '30', 'https://api.osv.dev/v1/querybatch'], {
        input: JSON.stringify({ queries }), encoding: 'utf8', windowsHide: true, timeout: 35000, maxBuffer: 8 * 1024 * 1024,
    });
    if (result.error || result.status !== 0) throw new Error('Advisory service unavailable; no clean scan result');
    const report = { ...evaluateResponse(queries, JSON.parse(result.stdout)), checkedAt: new Date().toISOString() };
    writeFileSync(reportFile, JSON.stringify(report, null, 2) + '\n');
    return report;
}
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    try {
        const report = scan(process.argv[2] || 'target/security-backend-tree-after.json', process.argv[3] || 'target/security-backend-audit-after.json');
        console.log(`Scanned ${report.scanned} resolved Maven dependencies; ${report.findings.length} package/advisory matches.`);
        if (report.findings.length) process.exitCode = 1;
    } catch { console.error('Backend advisory scan incomplete. Inspect inventory/network privately; do not mark security acceptance complete.'); process.exitCode = 2; }
}
