import { spawnSync } from 'node:child_process';
import { appendFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';

// This private observer can only inspect the uniquely named synthetic project.
const [project, output, stop] = process.argv.slice(2);
if (!/^investsmart-v2-smoke-[0-9]+-[a-f0-9]{8}$/.test(project || '') || !output || !stop) process.exit(2);
while (!existsSync(stop)) {
    const ids = spawnSync('docker', ['ps', '--quiet', '--filter', `label=com.docker.compose.project=${project}`], {encoding:'utf8',windowsHide:true,timeout:10000});
    const containers = (ids.stdout || '').trim().split(/\s+/).filter(Boolean);
    let entry = {at:new Date().toISOString(), ok:false};
    if (ids.status === 0 && containers.length && containers.every(id=>/^[a-f0-9]{12,64}$/.test(id))) {
        const result = spawnSync('docker', ['stats','--no-stream','--format','{{json .}}',...containers], {encoding:'utf8',windowsHide:true,timeout:15000});
        try { entry = {...entry,ok:result.status===0,containers:result.stdout.trim().split(/\r?\n/).filter(Boolean).map(line=>{const s=JSON.parse(line);return {id:s.ID,cpu:s.CPUPerc,memory:s.MemUsage,memoryPercent:s.MemPerc,blockIO:s.BlockIO,pids:s.PIDs};})}; }
        catch { entry.reason='Invalid metrics response'; }
    }
    appendFileSync(resolve(output), JSON.stringify(entry)+'\n');
    await new Promise(done=>setTimeout(done,5000));
}
