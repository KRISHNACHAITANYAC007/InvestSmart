import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, existsSync, symlinkSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, resolve } from 'node:path';
import { createHash } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { releaseInputs, stageRelease, writeChecksums, filesUnder, prepareFrontend } from './build-v2-release.mjs';
import { inspectArtifacts } from './check-v2-release.mjs';
import { assertLoopbackPort, encryptFixture, decryptFixture } from './check-v2-containers.mjs';

function fixture(run) {
    const workspace = mkdtempSync(resolve(tmpdir(), 'investsmart-release-test-'));
    const root = resolve(workspace, 'source'), stage = resolve(workspace, 'package');
    mkdirSync(root); mkdirSync(stage);
    function put(path, text = 'fixture') { const target = resolve(root, path); mkdirSync(dirname(target), { recursive: true }); writeFileSync(target, text); }
    for (const [source] of releaseInputs) {
        if (['frontend/dist', 'deploy/mysql-init', 'src/main/resources/db'].includes(source)) put(`${source}/fixture.txt`);
        else put(source, source.endsWith('compose.yaml') ? 'deploy/Dockerfile.backend\ndeploy/Dockerfile.web\n' : 'fixture');
    }
    try { run({ root, stage, put }); }
    finally { assert.equal(dirname(workspace), resolve(tmpdir())); assert.match(workspace, /investsmart-release-test-/); rmSync(workspace, { recursive: true, force: true }); }
}
test('allowlisted staging excludes operator secrets, dumps and logs and selects runtime Dockerfiles', () => fixture(({ root, stage, put }) => {
    for (const path of ['deploy/production.env', 'frontend/.env.local', 'src/main/resources/application-local.properties', 'target/private.log', 'backup.sql']) put(path, 'private-value');
    stageRelease(root, stage);
    assert.ok(existsSync(resolve(stage, 'backend/app.jar')));
    assert.equal(filesUnder(stage).some(path => readFileSync(path, 'utf8').includes('private-value')), false);
    assert.match(readFileSync(resolve(stage, 'deploy/compose.yaml'), 'utf8'), /Dockerfile.backend.release/);
    assert.match(readFileSync(resolve(stage, 'deploy/compose.yaml'), 'utf8'), /Dockerfile.web.release/);
}));
test('frontend release installation is isolated from live dev dependencies and private env', () => fixture(({ root, put }) => {
    for (const name of ['index.html','package.json','package-lock.json','vite.config.js','eslint.config.js','src/App.jsx']) put(`frontend/${name}`, 'source');
    put('frontend/node_modules/locked-native.node', 'live-dev');put('frontend/.env.local','private-value');
    const isolated=prepareFrontend(root);
    assert.equal(readFileSync(resolve(root,'frontend/node_modules/locked-native.node'),'utf8'),'live-dev');
    assert.equal(existsSync(resolve(isolated,'node_modules')),false);assert.equal(existsSync(resolve(isolated,'.env.local')),false);
    assert.equal(readFileSync(resolve(isolated,'src/App.jsx'),'utf8'),'source');
}));
test('a private configuration file hidden inside an allowlisted directory fails closed', () => fixture(({ root, stage, put }) => {
    put('frontend/dist/assets/.env.production', 'private-value');
    assert.throws(() => stageRelease(root, stage), /Private configuration/);
}));
test('missing required release input fails rather than producing an incomplete package', () => fixture(({ root, stage }) => {
    rmSync(resolve(root, 'deploy/RELEASE.md'));
    assert.throws(() => stageRelease(root, stage));
}));
test('symbolic link input fails closed', () => fixture(({ root, stage }) => {
    // Windows directory junctions require no developer-mode symlink privilege.
    symlinkSync(resolve(root, 'deploy/mysql-init'), resolve(root, 'frontend/dist/link'), process.platform === 'win32' ? 'junction' : 'dir');
    assert.throws(() => stageRelease(root, stage), /Symbolic links/);
}));
test('checksum manifest matches every payload and is stable across regeneration', () => fixture(({ root, stage }) => {
    stageRelease(root, stage); writeChecksums(stage);
    const first = readFileSync(resolve(stage, 'SHA256SUMS'), 'utf8'); writeChecksums(stage);
    assert.equal(readFileSync(resolve(stage, 'SHA256SUMS'), 'utf8'), first);
    const lines = first.trim().split('\n'); assert.equal(lines.length, filesUnder(stage).length - 1);
    for (const line of lines) { const [hash, file] = line.split('  '); assert.equal(hash, createHash('sha256').update(readFileSync(resolve(stage, file))).digest('hex')); }
}));
test('smoke database endpoint rejects non-loopback and malformed destinations', () => {
    assert.equal(assertLoopbackPort('127.0.0.1:33307\n'), '33307');
    for (const endpoint of ['0.0.0.0:3306', 'database:3306', '127.0.0.1:0', '127.0.0.1:65536', '127.0.0.1:3306\nother:3306']) assert.throws(() => assertLoopbackPort(endpoint));
});
test('synthetic encrypted-factor format authenticates key and ciphertext', () => {
    const encrypted = encryptFixture('synthetic-key', 'synthetic-factor');
    assert.equal(decryptFixture('synthetic-key', encrypted), 'synthetic-factor');
    assert.throws(() => decryptFixture('different-key', encrypted));
    const tampered = Buffer.from(encrypted, 'base64'); tampered[15] ^= 1;
    assert.throws(() => decryptFixture('synthetic-key', tampered.toString('base64')));
});
test('preflight inspects the portable package JAR and rejects bundled local configuration', () => fixture(({ root, stage, put }) => {
    const entries = ['com/example/investsmart/trading/PaperTrading.class', 'db/migrations/20261111_paper_ticket.sql', 'application-prod.properties', 'com/example/investsmart/security/AdminBootstrap.class', 'com/example/investsmart/security/PhoneVerification.class', 'db/migrations/20261110_phone_verification.sql'];
    for (const entry of entries) put(`jar-input/BOOT-INF/classes/${entry}`);
    mkdirSync(resolve(stage, 'backend')); mkdirSync(resolve(stage, 'frontend/dist'), { recursive: true });
    writeFileSync(resolve(stage, 'frontend/dist/index.html'), '<html></html>');
    const archive = () => {
        const result = spawnSync('jar', ['cf', resolve(stage, 'backend/app.jar'), '-C', resolve(root, 'jar-input'), '.'], { encoding: 'utf8', windowsHide: true });
        assert.equal(result.status, 0, 'JDK jar tool is required for release validation');
    };
    archive(); assert.deepEqual(inspectArtifacts(stage), []);
    put('jar-input/BOOT-INF/classes/application-local.properties', 'private-value');
    archive(); assert.match(inspectArtifacts(stage).join('\n'), /local configuration/);
}));
