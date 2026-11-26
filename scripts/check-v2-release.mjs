import { readFileSync, readdirSync, existsSync } from "node:fs";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const required = ["DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET", "SECURITY_ENCRYPTION_KEY", "UPSTOX_API_TOKEN", "PUBLIC_URL", "CORS_ORIGINS", "SMTP_HOST", "SMTP_USERNAME", "SMTP_PASSWORD", "MAIL_FROM"];
const origin = value => {
    try { const u = new URL(value); return u.protocol === "https:" && u.port !== "0" && !u.username && !u.password && !value.includes("?") && !value.includes("#") && (u.pathname === "/" || u.pathname === "") && !value.endsWith("/") && !value.includes("*") ? u.origin : null; }
    catch { return null; }
};

// Diagnostics intentionally contain field names only, never supplied values or parser errors.
export function validateEnvironment(env) {
    const errors = required.filter(key => !env[key]?.trim()).map(key => `Missing ${key}`);
    if (env.DB_USERNAME?.trim().toLowerCase() === "root") errors.push("DB_USERNAME must be a non-root application account");
    if (env.DB_URL && !env.DB_URL.startsWith("jdbc:mysql:")) errors.push("DB_URL must be a MySQL JDBC URL");
    if (env.JWT_SECRET && Buffer.byteLength(env.JWT_SECRET, "utf8") < 32) errors.push("JWT_SECRET needs at least 32 bytes");
    if (env.SECURITY_ENCRYPTION_KEY && env.SECURITY_ENCRYPTION_KEY.length < 32) errors.push("SECURITY_ENCRYPTION_KEY needs at least 32 characters");
    if (env.JWT_SECRET && env.JWT_SECRET === env.SECURITY_ENCRYPTION_KEY) errors.push("Use separate JWT_SECRET and SECURITY_ENCRYPTION_KEY values");
    const publicOrigin = env.PUBLIC_URL ? origin(env.PUBLIC_URL) : null;
    if (env.PUBLIC_URL && !publicOrigin) errors.push("PUBLIC_URL must be an HTTPS origin");
    if (env.PUBLIC_URL?.endsWith("/")) errors.push("PUBLIC_URL must not end with a slash");
    if (env.CORS_ORIGINS) {
        const cors = env.CORS_ORIGINS.split(",").map(value => origin(value.trim()));
        if (cors.some(value => !value)) errors.push("CORS_ORIGINS must contain exact HTTPS origins");
        if (publicOrigin && !cors.includes(publicOrigin)) errors.push("CORS_ORIGINS must include the PUBLIC_URL origin");
    }
    if (env.MAIL_FROM && !/^[^\s@<>]+@[^\s@<>]+\.[^\s@<>]+$/.test(env.MAIL_FROM)) errors.push("MAIL_FROM must be a sender email address");
    for (const key of ["SMTP_PORT", "PORT"]) if (env[key] && (!/^\d+$/.test(env[key]) || Number(env[key]) < 1 || Number(env[key]) > 65535)) errors.push(`${key} must be a valid port number`);
    if (env.VITE_API_URL !== "/api") errors.push("VITE_API_URL must be /api for the documented same-origin deployment");
    if (env.SPRING_PROFILES_ACTIVE?.trim() !== "prod") errors.push("SPRING_PROFILES_ACTIVE must be exactly prod");
    return errors;
}

// Root credentials belong to Compose/MySQL, never the backend runtime environment.
export function validateComposeEnvironment(env) {
    const errors = validateEnvironment(env);
    if (!env.MYSQL_ROOT_PASSWORD?.trim()) errors.push("Missing MYSQL_ROOT_PASSWORD");
    const keys = ["DB_PASSWORD", "MYSQL_ROOT_PASSWORD", "JWT_SECRET", "SECURITY_ENCRYPTION_KEY"];
    for (let i=0;i<keys.length;i++) for (let j=i+1;j<keys.length;j++) {
        if (env[keys[i]] && env[keys[j]] && env[keys[i]] === env[keys[j]]) errors.push(`Use separate ${keys[i]} and ${keys[j]} values`);
    }
    return [...new Set(errors)];
}

export function inspectArtifacts(root) {
    const errors = [], jar = existsSync(resolve(root, "backend/app.jar")) ? resolve(root, "backend/app.jar") : resolve(root, "target/InvestSmart-0.0.1-SNAPSHOT.jar"), dist = resolve(root, "frontend/dist");
    if (!existsSync(jar)) errors.push("Backend release JAR is missing; run the Maven verify build");
    else {
        const result = spawnSync("jar", ["tf", jar], { encoding: "utf8", windowsHide: true, maxBuffer: 4 * 1024 * 1024 });
        if (result.error || result.status !== 0) errors.push("Cannot inspect the JAR; make the JDK jar tool available");
        else {
            const entries = result.stdout.split(/\r?\n/);
            if (entries.some(path => /(^|\/)application-local\.(properties|ya?ml)$/.test(path))) errors.push("Release JAR contains local configuration");
            for (const path of ["BOOT-INF/classes/com/example/investsmart/trading/PaperTrading.class", "BOOT-INF/classes/db/migrations/20261111_paper_ticket.sql", "BOOT-INF/classes/application-prod.properties", "BOOT-INF/classes/com/example/investsmart/security/AdminBootstrap.class", "BOOT-INF/classes/com/example/investsmart/security/PhoneVerification.class", "BOOT-INF/classes/db/migrations/20261110_phone_verification.sql"]) if (!entries.includes(path)) errors.push("Release JAR is missing production configuration or owner bootstrap code");
        }
    }
    if (!existsSync(resolve(dist, "index.html"))) errors.push("Frontend production build is missing");
    else if (existsSync(resolve(dist, "assets"))) for (const name of readdirSync(resolve(dist, "assets"))) {
        if (name.endsWith(".js") && readFileSync(resolve(dist, "assets", name), "utf8").includes("http://localhost:8080")) {
            errors.push("Frontend build still targets localhost; rebuild with VITE_API_URL=/api"); break;
        }
    }
    return errors;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
    const errors = [...(process.argv.includes("--compose") ? validateComposeEnvironment(process.env) : validateEnvironment(process.env)), ...inspectArtifacts(root)];
    for (const error of errors) console.error(`FAIL: ${error}`);
    if (errors.length) { console.error("V2 preflight incomplete. Fill private configuration and rebuild as indicated; no account or deployment was changed."); process.exitCode = 1; }
    else console.log("PASS: configuration shape and release artifacts. Mail delivery, migrations, owner MFA, provider permissions and visual acceptance still require verification. No external requests or account changes were made.");
}
