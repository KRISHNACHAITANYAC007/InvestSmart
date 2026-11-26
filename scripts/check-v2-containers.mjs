import { spawnSync, spawn } from 'node:child_process';
import { createHash, createCipheriv, createDecipheriv, randomBytes } from 'node:crypto';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { stageRelease } from './build-v2-release.mjs';
import { dockerReadiness } from './docker-readiness.mjs';
import { checkStagingRestriction } from './check-v2-staging-container.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const database = 'investsmart_release_synthetic';
export function assertLoopbackPort(value) {
    const match = /^127\.0\.0\.1:([0-9]+)$/.exec(value.trim());
    if (!match || +match[1] < 1 || +match[1] > 65535) throw new Error('Smoke database must use an isolated loopback port');
    return match[1];
}
export function encryptFixture(secret, text) {
    const iv = randomBytes(12), key = createHash('sha256').update(secret).digest();
    const cipher = createCipheriv('aes-256-gcm', key, iv);
    return Buffer.concat([iv, cipher.update(text, 'utf8'), cipher.final(), cipher.getAuthTag()]).toString('base64');
}
export function decryptFixture(secret, text) {
    const bytes = Buffer.from(text, 'base64');
    const cipher = createDecipheriv('aes-256-gcm', createHash('sha256').update(secret).digest(), bytes.subarray(0, 12));
    cipher.setAuthTag(bytes.subarray(-16));
    return Buffer.concat([cipher.update(bytes.subarray(12, -16)), cipher.final()]).toString('utf8');
}
export function smoke() {
    const logs = resolve(root, 'target/release-checks'); mkdirSync(logs, { recursive: true });
    const workspace = mkdtempSync(resolve(logs, 'smoke-'));
    const context = resolve(workspace, 'package'); mkdirSync(context); stageRelease(root, context);
    const project = `investsmart-v2-smoke-${process.pid}-${randomBytes(4).toString('hex')}`;
    const env = { ...process.env, V2_SMOKE_CONTEXT: context.replaceAll('\\', '/'),
        V2_SMOKE_DB_PASSWORD: randomBytes(32).toString('hex'), V2_SMOKE_ROOT_PASSWORD: randomBytes(32).toString('hex'),
        V2_SMOKE_JWT: randomBytes(40).toString('hex'), V2_SMOKE_ENCRYPTION: randomBytes(40).toString('hex') };
    // No inherited opt-in database target may redirect the test away from this project.
    delete env.V2_TEST_DB_URL; delete env.V2_TEST_DB_USERNAME; delete env.V2_TEST_DB_PASSWORD;
    const prefix = ['compose', '--project-name', project, '-f', resolve(root, 'deploy/smoke/compose.yaml')];
    let sequence = 0;
    function command(program, args, { input, sensitive = false, environment = env, allowFailure = false, timeout = 1200000 } = {}) {
        const result = spawnSync(program, args, { cwd: root, env: environment, input, encoding: 'utf8', windowsHide: true,
            maxBuffer: 64 * 1024 * 1024, timeout });
        if (!sensitive) writeFileSync(resolve(workspace, `${++sequence}.log`), (result.stdout || '') + (result.stderr || ''));
        if (!allowFailure && (result.error || result.status !== 0)) {
            throw new Error(`Isolated container check failed at step ${sequence}${result.error?.code ? ` (${result.error.code})` : ''}; inspect private smoke logs.`);
        }
        return result;
    }
    const compose = (args, options) => command('docker', [...prefix, ...args], options);
    function sql(query, db = database) {
        return compose(['exec', '-T', 'mysql', 'sh', '-c', 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -u root --batch --skip-column-names "$@"', 'sh', db], { input: query, sensitive: true }).stdout.trim();
    }
    const snapshotSql = `SELECT id,virtual_balance,email_verified FROM users WHERE id=91001;
SELECT stock_id,quantity FROM holdings WHERE user_id=91001 ORDER BY stock_id;
SELECT stock_id,quantity,total_amount FROM orders WHERE user_id=91001 ORDER BY id;
SELECT ipo_id,status,reserved_amount FROM ipo_applications WHERE user_id=91001 ORDER BY id;
SELECT id,status,reserved_cash,units FROM equity_pending_orders WHERE user_id=91001 ORDER BY id;
SELECT instrument_key,units,cost_amount FROM option_positions WHERE user_id=91001 ORDER BY instrument_key;
SELECT id,status,reserved_cash,units FROM option_orders WHERE user_id=91001 ORDER BY id;
SELECT id,factor_completed,revoked FROM auth_sessions WHERE user_id=91001 ORDER BY id;
SELECT credential_hash,secret_cipher,recovery_hashes,ready,last_step FROM phone_credentials WHERE user_id=91001;
SELECT id,status,reserved_cash,quantity,filled_units FROM paper_tickets WHERE user_id=91001 ORDER BY id;
SELECT id,quantity,principal,interest FROM paper_financing WHERE user_id=91001 ORDER BY id;
SELECT event_key,principal_delta,amount FROM paper_finance_events WHERE user_id=91001 ORDER BY event_key;
SELECT id,direction,quantity,entry_price,collateral,realized_pnl,close_requested FROM paper_intraday_positions WHERE user_id=91001 ORDER BY id;
SELECT event_key,event_type,units,collateral_delta,realized_pnl,fee FROM paper_intraday_events WHERE user_id=91001 ORDER BY event_key;
SELECT virtual_balance FROM users WHERE id=91002;
SELECT opening_cash,settled_cash FROM settlement_accounts WHERE user_id=91002;
SELECT id,source_key,amount,remaining_credit,status,calendar_version,expected_date,cutoff_at FROM settlement_obligations WHERE user_id=91002 ORDER BY id;
SELECT id,stock_id,obligation_id,initial_units,remaining_units,settled,cost_amount FROM settlement_lots WHERE user_id=91002 ORDER BY id;
SELECT id,reference_kind,reference_id,source_key,amount,remaining_amount,status FROM settlement_allocations WHERE user_id=91002 ORDER BY id;
SELECT credit_obligation_id,delivery_obligation_id FROM settlement_dependencies WHERE user_id=91002 ORDER BY credit_obligation_id,delivery_obligation_id;
SELECT event_key,action,fingerprint,amount FROM settlement_journal WHERE user_id=91002 ORDER BY event_key;
SELECT event_key,allocation_id,action,amount FROM settlement_allocation_events WHERE user_id=91002 ORDER BY event_key;
SELECT event_key,lot_id,action,units,settled_at_event FROM settlement_lot_events WHERE user_id=91002 ORDER BY event_key;
SELECT obligation_id,user_id FROM settlement_checks WHERE user_id=91002 ORDER BY obligation_id;`;
    let started = false, succeeded = false;
    try {
        const readiness = dockerReadiness(env);
        if (!readiness.ready) throw new Error(readiness.reason);
        compose(['config', '--quiet']);
        started = true;
        console.log('Starting isolated MySQL 8.4 and packaged runtime images...');
        compose(['up', '--build', '--detach', '--wait', '--wait-timeout', '300']);
        if (!sql('SELECT VERSION();').startsWith('8.4.')) throw new Error('Expected MySQL 8.4');
        sql('CREATE DATABASE investsmart_release_migrations;');
        const settlementMigrations = ['20261111_settlement_calendar.sql', '20261111_settlement_accounting.sql', '20261111_settlement_journal.sql', '20261111_settlement_checks.sql'];
        for (const file of settlementMigrations) sql(readFileSync(resolve(root, 'src/main/resources/db/migrations', file), 'utf8'), 'investsmart_release_migrations');
        const columns = schema => sql(`SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COALESCE(COLUMN_DEFAULT,'<NULL>') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${schema}' AND TABLE_NAME LIKE 'settlement_%' ORDER BY TABLE_NAME,COLUMN_NAME;`);
        if (columns(database) !== columns('investsmart_release_migrations')) throw new Error('Settlement migrations and fresh baseline differ');
        // Engine bindings are authoritative; some Compose/Desktop versions report :0.
        const container = compose(['ps', '--quiet', 'mysql']).stdout.trim();
        if(!/^[a-f0-9]{12,64}$/.test(container))throw new Error('Cannot identify the isolated MySQL container');
        const bindings = JSON.parse(command('docker', ['inspect', '--format', '{{json .NetworkSettings.Ports}}', container]).stdout);
        const addresses = bindings?.['3306/tcp'];
        if(!Array.isArray(addresses)||addresses.length!==1)throw new Error('Expected exactly one isolated MySQL host binding');
        const port = assertLoopbackPort(`${addresses[0].HostIp}:${addresses[0].HostPort}`);
        const testEnv = { ...env, V2_TEST_DB_URL: `jdbc:mysql://127.0.0.1:${port}/${database}?allowPublicKeyRetrieval=true&useSSL=false`,
            V2_TEST_DB_USERNAME: 'release_fixture', V2_TEST_DB_PASSWORD: env.V2_SMOKE_DB_PASSWORD };
        console.log('Checking authenticated funds and session behavior on MySQL...');
        if (process.platform === 'win32') command(process.env.ComSpec || 'cmd.exe', ['/d', '/s', '/c', 'mvnw.cmd -B -Dtest=MySqlReleaseTests test'], { environment: testEnv });
        else command('sh', ['./mvnw', '-B', '-Dtest=MySqlReleaseTests', 'test'], { environment: testEnv });
        if (process.argv.includes('--capacity')) {
            // Only the test runtime may process synthetic exposure during load.
            compose(['stop', '--timeout', '10', 'backend'], {timeout:60000});
            console.log('Checking opt-in 100-session real HTTP synthetic workload...');
            const loadEnv = { ...testEnv, V2_LOAD_TEST: 'true', JAVA_TOOL_OPTIONS:'-Xmx768m', MAVEN_OPTS:'-Xmx192m' };
            const monitorStop=resolve(workspace,'monitor.stop');
            const observer=spawn(process.execPath,[resolve(root,'scripts/monitor-release-containers.mjs'),project,resolve(workspace,'container-metrics.jsonl'),monitorStop],{cwd:root,env,windowsHide:true,stdio:'ignore'});
            observer.on('error',()=>writeFileSync(resolve(workspace,'monitor-error.json'),JSON.stringify({error:'Container observer failed to start'})));
            for (const file of ['http-capacity-result.json','http-capacity-progress.json']) rmSync(resolve(root,'target',file),{force:true});
            try {
                if (process.platform === 'win32') command(process.env.ComSpec || 'cmd.exe', ['/d', '/s', '/c', 'mvnw.cmd -B -Dtest=HttpCapacityTests test'], { environment: loadEnv, timeout: 3300000 });
                else command('sh', ['./mvnw', '-B', '-Dtest=HttpCapacityTests', 'test'], { environment: loadEnv, timeout: 3300000 });
            } finally {
                writeFileSync(monitorStop,'stop');
                for(const file of ['http-capacity-result.json','http-capacity-progress.json','http-capacity-metrics.jsonl','http-capacity-minutes.jsonl']) if(existsSync(resolve(root,'target',file))) writeFileSync(resolve(workspace,file),readFileSync(resolve(root,'target',file)));
            }
        }
        if (!process.argv.includes('--capacity')) compose(['stop', '--timeout', '10', 'backend'], {timeout:60000});
        const cipher = encryptFixture(env.V2_SMOKE_ENCRYPTION, 'synthetic-factor-retention');
        sql(readFileSync(resolve(root, 'deploy/smoke/fixture.sql'), 'utf8') + `\nINSERT INTO phone_credentials(user_id,credential_hash,credential_id,user_handle,public_key_cose,secret_cipher,recovery_hashes,ready,created_at) VALUES(91001,'${'f'.repeat(64)}','synthetic','synthetic','synthetic','${cipher}','[]',TRUE,NOW());`);
        const baseline = sql(snapshotSql);
        const dump = compose(['exec', '-T', 'mysql', 'sh', '-c', 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -u root --single-transaction --no-tablespaces --set-gtid-purged=OFF investsmart_release_synthetic'], { sensitive: true }).stdout;
        sql('CREATE DATABASE investsmart_release_restore;'); sql(dump, 'investsmart_release_restore');
        if (baseline !== sql(snapshotSql, 'investsmart_release_restore')) throw new Error('Restored synthetic accounting/factor records differ');
        if (decryptFixture(env.V2_SMOKE_ENCRYPTION, sql('SELECT secret_cipher FROM phone_credentials WHERE user_id=91001;', 'investsmart_release_restore')) !== 'synthetic-factor-retention') throw new Error('Restored factor is unreadable');
        // Restart both database and application to verify persisted volumes, not just live connections.
        compose(['restart', 'mysql']); compose(['up', '--detach', '--wait', '--wait-timeout', '180']);
        if (baseline !== sql(snapshotSql)) throw new Error('Restart changed synthetic accounting/factor records');
        console.log('Checking trusted HTTPS, SPA routing and CSRF...');
        const ca = resolve(workspace, 'root.crt');
        compose(['cp', 'web:/data/caddy/pki/authorities/local/root.crt', ca]);
        command(process.execPath, ['scripts/check-v2-https.mjs'], { environment: { ...env, V2_SMOKE_CA_FILE: ca } });
        const webContainer = compose(['ps', '--quiet', 'web']).stdout.trim();
        if (!/^[a-f0-9]{12,64}$/.test(webContainer)) throw new Error('Cannot identify isolated web image');
        const webImage = command('docker', ['inspect', '--format', '{{.Image}}', webContainer]).stdout.trim();
        if (!/^sha256:[a-f0-9]{64}$/.test(webImage)) throw new Error('Cannot verify isolated web image');
        checkStagingRestriction(command, webImage, context, project);
        succeeded = true;
    } finally {
        if (started) {
            const cleanup = compose(['down', '--volumes', '--remove-orphans', '--timeout', '20'], { allowFailure: true });
            if (cleanup.error || cleanup.status !== 0) throw new Error(`Smoke cleanup failed; remove only Compose project ${project}.`);
        }
    }
    if (succeeded) {
        writeFileSync(resolve(workspace, 'result.json'), JSON.stringify({ completedAt: new Date().toISOString(), mysql84: 'passed', settlementMigrationParity: 'passed', restore: 'passed', restart: 'passed', https: 'passed', stagingRestriction: 'passed', project }, null, 2));
        console.log('PASS: isolated MySQL 8.4, restore/restart and packaged HTTPS checks. No visual/device acceptance claimed.');
    }
}
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    try { smoke(); } catch (failure) { console.error(failure.message); process.exitCode = 1; }
}
