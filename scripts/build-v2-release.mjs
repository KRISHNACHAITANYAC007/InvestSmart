import { cpSync, existsSync, lstatSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from "node:fs";
import { createHash } from "node:crypto";
import { dirname, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";
import { inspectArtifacts } from "./check-v2-release.mjs";
import { dockerReadiness } from "./docker-readiness.mjs";

export const releaseInputs = [
    ["target/InvestSmart-0.0.1-SNAPSHOT.jar", "backend/app.jar"],
    ["frontend/dist", "frontend/dist"],
    ...["Caddyfile", "Caddyfile.staging", "compose.yaml", "compose.staging.yaml", "STAGING.md", "Dockerfile.backend.release", "Dockerfile.web.release", "production.env.example", "V2_ACCEPTANCE.md", "RELEASE.md", "PAPER_TRADING.md", "SETTLEMENT.md", "KITE_RELEASE_AUDIT.md", "mysql-init"].map(name => [`deploy/${name}`, `deploy/${name}`]),
    ["src/main/resources/db", "src/main/resources/db"],
    ["scripts/check-v2-release.mjs", "scripts/check-v2-release.mjs"],
    ["scripts/check-v2-staging.mjs", "scripts/check-v2-staging.mjs"],
    ["DEPLOYMENT.md", "DEPLOYMENT.md"], ["README.md", "README.md"],
    ["DEPLOYMENT_CHECKLIST.md", "DEPLOYMENT_CHECKLIST.md"],
    ["DEPLOYMENT_REQUIREMENTS.md", "DEPLOYMENT_REQUIREMENTS.md"],
    ["DEPENDENCY_SECURITY_REPORT.md", "DEPENDENCY_SECURITY_REPORT.md"],
    ["deploy/CAPACITY.md", "deploy/CAPACITY.md"],
    ["deploy/compose.capacity.yaml", "deploy/compose.capacity.yaml"],
    ["deploy/LINT_SECURITY_OVERRIDE.md", "deploy/LINT_SECURITY_OVERRIDE.md"],
    ["deploy/settlement-calendar.csv.example", "deploy/settlement-calendar.csv.example"],
    ["deploy/calendar-review.example.json", "deploy/calendar-review.example.json"],
];

export function filesUnder(root) {
    const files = [];
    for (const name of readdirSync(root)) {
        const path = resolve(root, name), stat = lstatSync(path);
        if (stat.isSymbolicLink()) throw new Error("Symbolic links are not allowed in release inputs");
        if (stat.isDirectory()) files.push(...filesUnder(path)); else if (stat.isFile()) files.push(path);
    }
    return files.sort();
}
export function stageRelease(root, staging) {
    for (const [from, to] of releaseInputs) {
        const source = resolve(root, from);
        if (lstatSync(source).isSymbolicLink()) throw new Error("Symbolic links are not allowed in release inputs");
        const sources = lstatSync(source).isDirectory() ? filesUnder(source) : [source];
        if (sources.some(path => /(?:^|[\\/])(?:\.env[^\\/]*|production\.env|application-local\.[^\\/]+)$/.test(path))) throw new Error("Private configuration was found in release inputs");
        mkdirSync(dirname(resolve(staging, to)), { recursive: true });
        cpSync(source, resolve(staging, to), { recursive: true });
    }
    const compose = resolve(staging, "deploy/compose.yaml");
    writeFileSync(compose, readFileSync(compose, "utf8").replace("deploy/Dockerfile.backend", "deploy/Dockerfile.backend.release").replace("deploy/Dockerfile.web", "deploy/Dockerfile.web.release"));
}
export function writeChecksums(staging) {
    const manifest = filesUnder(staging).filter(path => !path.endsWith("SHA256SUMS")).map(path => `${createHash("sha256").update(readFileSync(path)).digest("hex")}  ${relative(staging, path).replaceAll("\\", "/")}`).join("\n") + "\n";
    writeFileSync(resolve(staging, "SHA256SUMS"), manifest);
}
function run(command, args, cwd, env, log) {
    const windowsCommand = process.platform === "win32" && ["npm", "mvnw"].includes(command);
    const result = windowsCommand
        ? spawnSync(process.env.ComSpec || "cmd.exe", ["/d", "/s", "/c", `${command}.cmd ${args.join(" ")}`], { cwd, env, encoding: "utf8", windowsHide: true, maxBuffer: 32 * 1024 * 1024 })
        : spawnSync(command === "mvnw" ? "sh" : command, command === "mvnw" ? ["./mvnw", ...args] : args, { cwd, env, encoding: "utf8", windowsHide: true, maxBuffer: 32 * 1024 * 1024 });
    writeFileSync(log, (result.stdout || "") + (result.stderr || ""));
    if (result.error || result.status !== 0) throw new Error(`Release check failed: ${command}. Inspect its private target/release-checks log.`);
}
export function prepareFrontend(root) {
    mkdirSync(resolve(root, "target"), { recursive: true });
    const workspace = mkdtempSync(resolve(root, "target/release-frontend-"));
    for (const name of ["src", "public", "index.html", "package.json", "package-lock.json", "vite.config.js", "eslint.config.js"]) {
        const source = resolve(root, "frontend", name);
        if (name === "public" && !existsSync(source)) continue;
        if (lstatSync(source).isSymbolicLink()) throw new Error("Symbolic frontend build input is forbidden");
        const inputs = lstatSync(source).isDirectory() ? filesUnder(source) : [source];
        if (inputs.some(path => /(?:^|[\\/])(?:\.env[^\\/]*|production\.env|application-local\.[^\\/]+)$/.test(path))) throw new Error("Private frontend build input is forbidden");
        cpSync(source, resolve(workspace, name), { recursive: true });
    }
    return workspace;
}
export function build(root, requireContainers = false) {
    const logs = resolve(root, "target/release-checks"); mkdirSync(logs, { recursive: true });
    const env = { ...process.env, VITE_API_URL: "/api" };
    const frontend = prepareFrontend(root);
    const steps = [
        ["backend", "mvnw", ["-B", "verify"], root],
        ["backend-inventory", "mvnw", ["-B", "dependency:tree", "-DoutputType=json", "-DoutputFile=target/release-checks/backend-dependencies.json"], root],
        ["backend-audit", process.execPath, ["scripts/check-maven-advisories.mjs", "target/release-checks/backend-dependencies.json", "target/release-checks/backend-advisories.json"], root],
        ["frontend-install", "npm", ["ci"], frontend],
        ["frontend-audit", "npm", ["audit", "--audit-level=low", "--json", "--fetch-retries=0", "--fetch-timeout=15000"], frontend],
        ["frontend-lint", "npm", ["run", "lint"], frontend],
        ["frontend-tests", process.execPath, ["--test", ...["api", "auth"].flatMap(dir => readdirSync(resolve(frontend, "src", dir)).filter(name => name.endsWith(".test.mjs")).map(name => `src/${dir}/${name}`))], frontend],
        ["frontend-build", "npm", ["run", "build"], frontend],
        ["release-tests", process.execPath, ["--test", ...readdirSync(resolve(root, "scripts")).filter(name => name.endsWith(".test.mjs")).map(name => `scripts/${name}`)], root],
    ];
    for (const [name, command, args, cwd] of steps) { console.log(`Checking ${name}…`); run(command, args, cwd, env, resolve(logs, `${name}.log`)); }
    const generated = resolve(root, "frontend/dist");
    if (dirname(generated) !== resolve(root, "frontend")) throw new Error("Invalid generated frontend destination");
    rmSync(generated, { recursive: true, force: true });
    cpSync(resolve(frontend, "dist"), generated, { recursive: true });
    const issues = inspectArtifacts(root); if (issues.length) throw new Error(issues.join("; "));
    const docker = dockerReadiness(env);
    let containers = "not-run";
    if (docker.ready) {
        run(process.execPath, ["scripts/check-v2-containers.mjs"], root, env, resolve(logs, "containers.log")); containers = "passed";
    } else if (requireContainers) throw new Error(`${docker.reason} No validated package was produced.`);
    const releases = resolve(root, "target/releases"); mkdirSync(releases, { recursive: true });
    const staging = mkdtempSync(resolve(releases, "InvestSmart-v2-"));
    stageRelease(root, staging);
    const stagedIssues = inspectArtifacts(staging); if (stagedIssues.length) throw new Error(stagedIssues.join("; "));
    const readiness = { generatedAt: new Date().toISOString(), freshProductionDatabase: true,
        automatedBuildChecks: "passed", mysql84AndContainers: containers, releasePackageValidated: containers === "passed", productionLaunchApproved: false,
        externalGates: ["real-device-and-rendered-acceptance", "owner-provisioning-and-MFA", "production-mail-and-HTTPS", "hosting-and-TLS", "Upstox-entitlement-and-display-permission"] };
    writeFileSync(resolve(staging, "readiness.json"), JSON.stringify(readiness, null, 2) + "\n");
    writeChecksums(staging);
    const archive = `${staging}.tar.gz`;
    run("tar", ["-czf", archive, "-C", staging, "."], root, env, resolve(logs, "archive.log"));
    writeFileSync(`${archive}.sha256`, `${createHash("sha256").update(readFileSync(archive)).digest("hex")}  ${archive.split(/[\\/]/).pop()}\n`);
    console.log(`Release package: ${relative(root, archive)}`);
    console.log(containers === "passed" ? "Package validation passed. Production launch gates remain open." : "Package prepared; MySQL 8.4/container validation remains pending. Production launch is not approved.");
    return { archive, readiness };
}
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    try { build(resolve(dirname(fileURLToPath(import.meta.url)), ".."), process.argv.includes("--require-containers")); }
    catch (failure) { console.error(failure.message); process.exitCode = 1; }
}
