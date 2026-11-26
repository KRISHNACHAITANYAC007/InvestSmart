import { test } from "node:test";
import assert from "node:assert/strict";
import { validateEnvironment, validateComposeEnvironment, inspectArtifacts } from "./check-v2-release.mjs";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, dirname, basename } from "node:path";

const valid = () => ({ DB_URL: "jdbc:mysql://database.invalid/app", DB_USERNAME: "fixture", DB_PASSWORD: "private-db-value", JWT_SECRET: "j".repeat(40), SECURITY_ENCRYPTION_KEY: "e".repeat(40), UPSTOX_API_TOKEN: "private-provider-value", PUBLIC_URL: "https://invest.example", CORS_ORIGINS: "https://invest.example", SMTP_HOST: "smtp.example.invalid", SMTP_USERNAME: "fixture", SMTP_PASSWORD: "private-mail-value", MAIL_FROM: "sender@example.invalid", VITE_API_URL: "/api", SPRING_PROFILES_ACTIVE: "prod" });
test("valid same-origin setup passes without external requests", () => assert.deepEqual(validateEnvironment(valid()), []));
test("Compose requires distinct root, application, signing and encryption secrets without echoing them", () => {
    const env={...valid(), MYSQL_ROOT_PASSWORD:'root-fixture-only'};
    assert.deepEqual(validateComposeEnvironment(env),[]);
    assert.match(validateComposeEnvironment({...env,MYSQL_ROOT_PASSWORD:''}).join(' '),/MYSQL_ROOT_PASSWORD/);
    for(const key of ['DB_PASSWORD','JWT_SECRET','SECURITY_ENCRYPTION_KEY']) {
        const errors=validateComposeEnvironment({...env,MYSQL_ROOT_PASSWORD:env[key]}).join(' ');
        assert.match(errors,/separate/);assert.equal(errors.includes(env[key]),false);
    }
    assert.match(validateComposeEnvironment({...env,DB_PASSWORD:env.JWT_SECRET}).join(' '),/separate DB_PASSWORD and JWT_SECRET/);
});
test("missing or mixed production profiles cannot pass preflight", () => {
    for (const profile of [undefined, "", "prod,test", "local,prod", "prod,prod"]) {
        assert.match(validateEnvironment({...valid(), SPRING_PROFILES_ACTIVE:profile}).join("\n"), /SPRING_PROFILES_ACTIVE/);
    }
});
test("root accounts and trailing-slash origins are rejected", () => {
    for (const change of [{ DB_USERNAME: "ROOT" }, { CORS_ORIGINS: "https://invest.example/" }, { PUBLIC_URL: "https://invest.example:0" }, { PUBLIC_URL: "https://invest.example?" }, { PUBLIC_URL: "https://invest.example#" }]) assert.ok(validateEnvironment({ ...valid(), ...change }).length);
});
test("missing values and invalid origins fail with field names and no secret contents", () => {
    const env = { ...valid(), SMTP_HOST: "", PUBLIC_URL: "https://user:private-url-value@example.invalid/", JWT_SECRET: "private-short-key" };
    const result = validateEnvironment(env).join("\n");
    assert.match(result, /Missing SMTP_HOST/); assert.match(result, /PUBLIC_URL/); assert.match(result, /JWT_SECRET/);
    for (const value of ["private-url-value", "private-short-key", env.DB_PASSWORD, env.SMTP_PASSWORD, env.UPSTOX_API_TOKEN]) assert.equal(result.includes(value), false);
});
test("mismatched origins, weak or reused keys, invalid ports and development targets fail", () => {
    const env = { ...valid(), CORS_ORIGINS: "https://different.example", SECURITY_ENCRYPTION_KEY: "j".repeat(40), SMTP_PORT: "70000", PORT: "not-a-port", VITE_API_URL: "http://localhost:8080", SPRING_PROFILES_ACTIVE: "local" };
    const result = validateEnvironment(env).join("\n");
    for (const field of ["CORS_ORIGINS", "SECURITY_ENCRYPTION_KEY", "SMTP_PORT", "PORT", "VITE_API_URL", "SPRING_PROFILES_ACTIVE"]) assert.match(result, new RegExp(field));
});
test("missing release artifacts fail without starting services", () => {
    const root = mkdtempSync(join(tmpdir(), "investsmart-preflight-"));
    try { const errors = inspectArtifacts(root); assert.equal(errors.length, 2); assert.match(errors.join("\n"), /JAR.*missing/); assert.match(errors.join("\n"), /Frontend.*missing/); }
    finally { assert.equal(dirname(root), tmpdir()); assert.ok(basename(root).startsWith("investsmart-preflight-")); rmSync(root, { recursive: true }); }
});
