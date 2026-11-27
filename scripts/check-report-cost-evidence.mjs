import {readFileSync} from 'node:fs';
import {resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

const nonnegative=value=>typeof value==='number'&&Number.isFinite(value)&&value>=0;
const requireValue=(condition,message)=>{if(!condition)throw Error(message);};
/** Structure/measurement-bound checks only; does not authenticate source, plans or an SLA. */
export function validateReport(report,{requirePrepared=false}={}){
    requireValue(report&&typeof report.engine==='string'&&report.engine.length>0,'Missing engine');
    requireValue(Array.isArray(report.observations)&&report.observations.length>0,'Missing observations');
    const readers=new Set();let querySamples=0;
    for(const row of report.observations){
        requireValue(row&&typeof row.reader==='string'&&row.reader.length>0,'Missing reader');readers.add(row.reader);
        requireValue(typeof row.sql==='string'&&/^\s*SELECT\b/i.test(row.sql),'Expected captured SELECT');
        requireValue(Array.isArray(row.parameterTypes),'Missing parameter types');
        requireValue(typeof row.estimatedPlan==='string','Missing estimated plan');
        try{const plan=JSON.parse(row.estimatedPlan);requireValue(plan&&typeof plan==='object'&&!Array.isArray(plan),'Invalid estimated plan');}catch{throw Error('Invalid estimated plan');}
        requireValue(Array.isArray(row.actualIteratorPlan)&&row.actualIteratorPlan.length>0&&row.actualIteratorPlan.every(line=>typeof line==='string'&&line.length>0),'Missing executed iterator plan');
        requireValue(Array.isArray(row.readerRepeatedMs)&&row.readerRepeatedMs.length===3&&row.readerRepeatedMs.every(nonnegative),'Missing/invalid reader repeats');
        requireValue(Array.isArray(row.queryRepeats)&&row.queryRepeats.length===3,'Missing query repeats');
        for(const sample of row.queryRepeats){
            requireValue(Number.isInteger(sample.returnedRows)&&sample.returnedRows>=0&&sample.returnedRows<=1001,'Returned-row bound violated');
            requireValue(nonnegative(sample.elapsedMs),'Invalid query timing');
            requireValue(sample.handlerReadDelta&&typeof sample.handlerReadDelta==='object'&&!Array.isArray(sample.handlerReadDelta)&&Object.keys(sample.handlerReadDelta).length>0,'Missing handler deltas');
            requireValue(Object.values(sample.handlerReadDelta).every(value=>Number.isInteger(value)&&nonnegative(value)),'Invalid handler delta');querySamples++;
        }
    }
    for(const name of ['pureMatchingSamples',...(requirePrepared?['preparedMatchingSamples']:[])]){
        requireValue(Array.isArray(report[name])&&report[name].length===3,`Missing ${name}`);
        for(const sample of report[name])requireValue(nonnegative(sample.wallMs)&&(sample.currentThreadCpuMs===null||nonnegative(sample.currentThreadCpuMs)),`Invalid ${name}`);
    }
    return {evidenceStructureValid:true,performanceAcceptance:false,observations:report.observations.length,querySamples,readers:readers.size,scope:'Structure and output bounds only; iterator rows, handler counters and elapsed/thread CPU are distinct. No source authentication or production SLA.'};
}
if(process.argv[1]&&resolve(process.argv[1])===fileURLToPath(import.meta.url)){
    try{requireValue(process.argv[2],'Pass a local report JSON path');console.log(JSON.stringify(validateReport(JSON.parse(readFileSync(process.argv[2],'utf8')),{requirePrepared:process.argv.includes('--prepared')})));}
    catch{console.error('Report evidence rejected: invalid JSON or measurement structure; source contents not printed.');process.exitCode=1;}
}
