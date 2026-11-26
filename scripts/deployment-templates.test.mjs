import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {validateComposeEnvironment} from './check-v2-release.mjs';
import {validateStaging} from './check-v2-staging.mjs';

const template=()=>Object.fromEntries(readFileSync('deploy/production.env.example','utf8').split(/\r?\n/).filter(line=>line.trim()&&!line.startsWith('#')).map(line=>{const at=line.indexOf('=');return [line.slice(0,at),line.slice(at+1)];}));
test('release template contains no credentials or usable host acceptance defaults',()=>{
    const env=template();
    for(const key of ['DB_USERNAME','DB_PASSWORD','MYSQL_ROOT_PASSWORD','JWT_SECRET','SECURITY_ENCRYPTION_KEY','UPSTOX_API_TOKEN','SMTP_USERNAME','SMTP_PASSWORD','PUBLIC_URL','CORS_ORIGINS','MAIL_FROM','STAGING_ALLOWED_CIDRS'])assert.ok(env[key]==='',key+' template must remain blank');
    assert.ok(validateComposeEnvironment(env).length>0);assert.ok(validateStaging(env,{compose:true}).some(m=>m.startsWith('STAGING_ALLOWED_CIDRS')));
});
test('populated synthetic template passes preflight without activating a calendar',()=>{
    const env={...template(),DB_USERNAME:'fixture',DB_PASSWORD:'fixture-db-only',MYSQL_ROOT_PASSWORD:'fixture-root-only',JWT_SECRET:'j'.repeat(40),SECURITY_ENCRYPTION_KEY:'e'.repeat(40),UPSTOX_API_TOKEN:'fixture-provider',SMTP_HOST:'mail.invalid',SMTP_USERNAME:'fixture',SMTP_PASSWORD:'fixture-mail-only',PUBLIC_URL:'https://staging.example.invalid',CORS_ORIGINS:'https://staging.example.invalid',MAIL_FROM:'sender@example.invalid',STAGING_ALLOWED_CIDRS:'198.51.100.5/32'};
    assert.deepEqual(validateStaging(env,{compose:true}),[]);
    const review=JSON.parse(readFileSync('deploy/calendar-review.example.json','utf8'));
    assert.equal(review.templateOnly,true);assert.equal(review.activationApproved,false);assert.equal(review.coverageStart,'');assert.equal(review.sha256OfReviewedCsv,'');
});
