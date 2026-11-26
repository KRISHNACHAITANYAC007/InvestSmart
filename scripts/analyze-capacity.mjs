import {readFileSync,writeFileSync} from 'node:fs';
import {resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
export function analyze(result,samples,minutes,containers){
    const steady=samples.filter(s=>s.phase==='steady'),cool=samples.filter(s=>s.phase==='cooldown');
    const avg=xs=>xs.length?xs.reduce((a,b)=>a+b,0)/xs.length:null;
    const window=xs=>({readP95Ms:avg(xs.filter(m=>m.readP95Ms>=0).map(m=>m.readP95Ms)),writeP95Ms:avg(xs.filter(m=>m.writeP95Ms>=0).map(m=>m.writeP95Ms))});
    const sm=minutes.filter(m=>m.phase==='steady');
    const postGc=s=>s.memoryPools?.reduce((n,p)=>n+p.postGcUsed,0);
    const changed=steady.filter((s,i)=>i===0||JSON.stringify(s.gc)!==JSON.stringify(steady[i-1].gc));
    const first=changed.slice(0,3),last=changed.slice(-3);
    const early=avg(first.map(postGc)),late=avg(last.map(postGc));
    const memoryEvidence=changed.length>=6&&early>0;
    const growth=memoryEvidence?late/early-1:null;
    const gaps=samples.slice(1).map((s,i)=>(s.wallMillis-samples[i].wallMillis)/1000);
    const maxGap=Math.max(0,...gaps);
    const containerGaps=containers.slice(1).map((c,i)=>(Date.parse(c.at)-Date.parse(containers[i].at))/1000);
    const maxContainerGap=Math.max(0,...containerGaps);
    const validSample=s=>Number.isFinite(s.wallMillis)&&s.heapMax>0&&Number.isFinite(s.heapUsed)&&s.hikari?.max>0&&Number.isFinite(s.hikari.active)&&Array.isArray(s.database)&&s.database.some(d=>d.Variable_name==='Threads_connected')&&Array.isArray(s.gc)&&s.gc.length>0;
    const monitoring=samples.length>1&&cool.length>0&&containers.length>1&&containers.every(c=>c.ok&&Number.isFinite(Date.parse(c.at)))&&samples.every(validSample)&&gaps.every(g=>g>=0)&&containerGaps.every(g=>g>=0)&&maxGap<=20&&maxContainerGap<=30&&result.interrupted!==true?'PASS':'BLOCKED';
    const span=phase=>{const phaseSamples=samples.filter(s=>s.phase===phase);return phaseSamples.length>1?(phaseSamples.at(-1).wallMillis-phaseSamples[0].wallMillis)/1000:0;};
    const fullDuration=result.fullDuration===true&&result.completed===true&&monitoring==='PASS'&&result.steady?.seconds===1800&&result.burst?.seconds===300&&result.steady?.scheduled>=54000&&result.burst?.scheduled>=18000&&span('steady')>=1790&&span('burst')>=290&&span('cooldown')>=290;
    const workload=result.completed===true&&result.interrupted!==true&&result.thresholdsPassed===true&&result.financialReconciliation===true?'PASS':'FAIL';
    return {
        fullDuration,
        workload,
        monitoring,
        capacityAcceptance:fullDuration&&workload==='PASS'?'PASS':'BLOCKED',
        maxContainerGapSeconds:maxContainerGap,
        maxSampleGapSeconds:maxGap,
        sampledSeconds:samples.length*5,containerSamples:containers.length,
        earlySteady:window(sm.slice(0,5)),lateSteady:window(sm.slice(-5)),
        sustainedCpuAbove70Seconds:steady.filter(s=>s.processCpu>.7).length*5,
        heapAbove80Seconds:samples.filter(s=>s.heapMax>0&&s.heapUsed/s.heapMax>.8).length*5,
        poolAbove80Seconds:samples.filter(s=>s.hikari&&s.hikari.active/s.hikari.max>.8).length*5,
        maxPendingConnections:Math.max(0,...samples.map(s=>s.hikari?.pending||0)),
        memory:{assessment:monitoring!=='PASS'||result.completed!==true||!memoryEvidence?'INCONCLUSIVE':growth>.2?'INVESTIGATE':'NO_GROWTH_SIGNAL_IN_THIS_RUN',postGcSamples:changed.length,earlyPostGcBytes:early,latePostGcBytes:late,growthFraction:growth,cooldownFirstHeap:cool[0]?.heapUsed,cooldownLastHeap:cool.at(-1)?.heapUsed,limitation:'Sampled GC cycles include young collections and workload history growth; this finite test cannot prove absence of leaks.'}
    };
}
if(process.argv[1]&&resolve(process.argv[1])===fileURLToPath(import.meta.url)){
    const [workspace,out]=process.argv.slice(2);
    const lines=name=>readFileSync(resolve(workspace,name),'utf8').trim().split(/\r?\n/).filter(Boolean).map(JSON.parse);
    const result=analyze(JSON.parse(readFileSync(resolve(workspace,'http-capacity-result.json'),'utf8')),lines('http-capacity-metrics.jsonl'),lines('http-capacity-minutes.jsonl'),lines('container-metrics.jsonl'));
    writeFileSync(out,JSON.stringify(result,null,2)+'\n');
}
