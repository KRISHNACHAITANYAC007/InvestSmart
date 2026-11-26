import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { existsSync, readFileSync, writeFileSync, mkdirSync, realpathSync, rmSync } from 'node:fs';
import { resolve, dirname, basename, relative, sep } from 'node:path';
import { pathToFileURL, fileURLToPath } from 'node:url';

const repo = resolve(dirname(fileURLToPath(import.meta.url)), '..');
export function normalizeResults(results, workspace) {
    return results.map(result => ({ ...result, filePath: relative(workspace, result.filePath).split(sep).join('/') }))
        .sort((a, b) => a.filePath.localeCompare(b.filePath));
}
export function requireIsolatedWorkspace(workspace) {
    const actual = realpathSync(workspace);
    if (dirname(actual) !== realpathSync(resolve(repo, 'target')) || !basename(actual).startsWith('release-frontend-'))
        throw new Error('Lint compatibility fixtures require an isolated target/release-frontend workspace');
    return actual;
}
async function capture(workspace) {
    workspace = requireIsolatedWorkspace(workspace);
    const { ESLint } = await import(pathToFileURL(resolve(workspace, 'node_modules/eslint/lib/api.js')).href);
    const engine = new ESLint({ cwd: workspace });
    const project = normalizeResults(await engine.lintFiles(['.']), workspace);
    assert.ok(project.length > 0, 'Project discovery must not be empty');
    assert.ok(project.every(result => result.errorCount === 0 && result.warningCount === 0), 'Baseline project must lint clean');
    const fixture = '__minimatch_compat_cases__';
    const files = {
        'src/Unused.js': 'const unused = 42; export default function Page(){ return window.location.href; }\n',
        'src/Hooks.jsx': 'import {useEffect,useState} from "react";\nexport default function Hooks({enabled,value}){ if(enabled){useState(0);} useEffect(()=>{console.log(value);},[]); return <div>{value}</div>; }\n',
        'src/Refresh.jsx': 'export function helper(){return 42;}\nexport function Refresh(){return <div/>;}\n',
        'src/nested/Windows.jsx': 'export default function Windows(){return <div>ok</div>;}\n',
        'src/.Hidden.jsx': 'export default function Hidden(){return <div/>;}\n',
        'src/ignored.txt': 'not JavaScript\n',
    };
    const ignored = ['dist/__minimatch_ignored__.jsx', 'target/__minimatch_ignored__.jsx'];
    function put(path, text) { const file = resolve(workspace, path); mkdirSync(dirname(file), { recursive: true }); writeFileSync(file, text); }
    try {
        for (const [path, text] of Object.entries(files)) put(`${fixture}/${path}`, text);
        for (const path of ignored) put(path, 'this is deliberately invalid JavaScript !!!');
        const cases = normalizeResults(await engine.lintFiles([`${fixture}/**/*.{js,jsx}`]), workspace);
        const separate = normalizeResults(await engine.lintFiles([`${fixture}/**/*.js`, `${fixture}/**/*.jsx`]), workspace);
        assert.deepEqual(cases, separate, 'Brace glob must equal JS/JSX union');
        assert.equal(cases.length, 5, 'Fixture discovery must retain nested and dot JSX, exclude text');
        const rules = [...new Set(cases.flatMap(result => result.messages.map(message => message.ruleId)))].sort();
        for (const rule of ['no-unused-vars', 'react-hooks/rules-of-hooks', 'react-hooks/exhaustive-deps', 'react-refresh/only-export-components'])
            assert.ok(rules.includes(rule), `Required diagnostic missing: ${rule}`);
        for (const path of ignored) assert.equal(await engine.isPathIgnored(resolve(workspace, path)), true, 'dist/target must remain ignored');
        const all = normalizeResults(await engine.lintFiles(['.']), workspace);
        assert.deepEqual(all.filter(result => !result.filePath.startsWith(`${fixture}/`)), project, 'Ignored fixtures must not change project diagnostics/discovery');
        const nativePath = resolve(workspace, fixture, 'src/nested/Windows.jsx');
        const native = normalizeResults(await engine.lintFiles([nativePath]), workspace);
        const slash = normalizeResults(await engine.lintFiles([nativePath.replaceAll('\\', '/')]), workspace);
        assert.deepEqual(native, slash, 'Native and forward-slash paths must lint identically');
        const versions = {};
        const eslintPackage = resolve(workspace, 'node_modules/eslint/package.json');
        const configEntry = createRequire(eslintPackage).resolve('@eslint/config-array');
        let configPackage;
        for (let dir = dirname(configEntry); dir.startsWith(workspace + sep); dir = dirname(dir)) {
            const file = resolve(dir, 'package.json');
            if (existsSync(file) && JSON.parse(readFileSync(file, 'utf8')).name === '@eslint/config-array') { configPackage = file; break; }
        }
        assert.ok(configPackage, 'Actual config-array package could not be identified');
        const consumers = { eslint: eslintPackage, '@eslint/config-array': configPackage };
        for (const [consumer, packageFile] of Object.entries(consumers)) {
            const require = createRequire(packageFile);
            versions[consumer] = { consumer: require('./package.json').version, minimatch: require('minimatch/package.json').version };
        }
        return { project, cases, rules, versions, nativeWindows: process.platform === 'win32' };
    } finally {
        const generated = resolve(workspace, fixture);
        if (dirname(generated) !== workspace) throw new Error('Invalid fixture cleanup path');
        rmSync(generated, { recursive: true, force: true });
        for (const path of ignored) rmSync(resolve(workspace, path), { force: true });
    }
}
export async function compare(baselinePath, candidatePath, output) {
    const baseline = await capture(baselinePath), candidate = await capture(candidatePath);
    assert.deepEqual(candidate.project, baseline.project, 'Project ESLint diagnostics or file selection changed');
    assert.deepEqual(candidate.cases, baseline.cases, 'Fixture rule severity/message/location/fixes changed');
    for (const consumer of ['eslint', '@eslint/config-array']) {
        assert.equal(candidate.versions[consumer].consumer, baseline.versions[consumer].consumer, 'Consumer version changed during override comparison');
        assert.equal(baseline.versions[consumer].minimatch, '10.2.6');
        assert.equal(candidate.versions[consumer].minimatch, '11.0.8');
    }
    const report = { checkedAt: new Date().toISOString(), compatible: true, projectFiles: candidate.project.length, fixtureFiles: candidate.cases.length, rules: candidate.rules, nativeWindows: candidate.nativeWindows, baseline: baseline.versions, candidate: candidate.versions };
    writeFileSync(output, JSON.stringify(report, null, 2) + '\n');
    return report;
}
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    try {
        const { baseline, candidate } = JSON.parse(readFileSync(process.argv[2] || 'target/minimatch-workspaces.json', 'utf8'));
        console.log(JSON.stringify(await compare(baseline, candidate, process.argv[3] || 'target/minimatch-compatibility.json')));
    } catch (error) { console.error('Lint compatibility failed: ' + error.message); process.exitCode = 1; }
}
