import {test} from 'node:test';
import assert from 'node:assert/strict';
import {analyze} from './analyze-capacity.mjs';
test('missing resource evidence cannot certify monitoring or leak acceptance',()=>{
    const a=analyze({fullDuration:false,thresholdsPassed:true,financialReconciliation:true},[],[],[]);
    assert.equal(a.monitoring,'BLOCKED');assert.equal(a.memory.assessment,'INCONCLUSIVE');assert.equal(a.fullDuration,false);
});
test('financial failure remains failed despite good latency and resource data',()=>{
    const a=analyze({thresholdsPassed:true,financialReconciliation:false},[],[],[]);assert.equal(a.workload,'FAIL');
});
test('growth across GC observations requests investigation',()=>{
    const samples=Array.from({length:8},(_,i)=>({wallMillis:i*5000,phase:'steady',gc:[{count:i}],memoryPools:[{postGcUsed:100+i*25}],hikari:{active:1,max:12},database:[{Variable_name:'Threads_connected',Value:'2'}],heapUsed:200,heapMax:1000}));
    samples.push({...samples[0],wallMillis:40000,phase:'cooldown'});
    const a=analyze({completed:true,fullDuration:true,thresholdsPassed:true,financialReconciliation:true},samples,[],[{ok:true,at:new Date(0).toISOString()},{ok:true,at:new Date(5000).toISOString()}]);
    assert.equal(a.monitoring,'PASS');assert.equal(a.memory.assessment,'INVESTIGATE');
    assert.equal(a.fullDuration,false);assert.equal(a.capacityAcceptance,'BLOCKED');
});
test('an interrupted result cannot certify workload, capacity or memory despite optimistic flags',()=>{
    const a=analyze({completed:true,interrupted:true,fullDuration:true,thresholdsPassed:true,financialReconciliation:true},[],[],[]);
    assert.equal(a.workload,'FAIL');assert.equal(a.capacityAcceptance,'BLOCKED');assert.equal(a.memory.assessment,'INCONCLUSIVE');
});
test('missing completion and missing database telemetry fail closed',()=>{
    const a=analyze({thresholdsPassed:true,financialReconciliation:true},[{wallMillis:0,phase:'steady',gc:[],database:[],hikari:{max:12,active:0}}],[],[]);
    assert.equal(a.workload,'FAIL');assert.equal(a.monitoring,'BLOCKED');
});
test('only a complete continuous full workload can certify capacity',()=>{
    const samples=Array.from({length:480},(_,i)=>({wallMillis:i*5000,phase:i<360?'steady':i<420?'burst':'cooldown',heapUsed:100,heapMax:1000,hikari:{max:12,active:0},database:[{Variable_name:'Threads_connected',Value:'2'}],gc:[{count:i}],memoryPools:[{postGcUsed:100}]}));
    const containers=samples.map(s=>({at:new Date(s.wallMillis).toISOString(),ok:true}));
    const result={completed:true,fullDuration:true,thresholdsPassed:true,financialReconciliation:true,steady:{seconds:1800,scheduled:54000},burst:{seconds:300,scheduled:18000}};
    assert.equal(analyze(result,samples,[],containers).capacityAcceptance,'PASS');
    const interrupted=[...containers];interrupted[200]={...interrupted[200],at:new Date(1_900_000).toISOString()};
    assert.equal(analyze(result,samples,[],interrupted).capacityAcceptance,'BLOCKED');
});
test('host suspension gap blocks monitoring acceptance',()=>{
    const base={wallMillis:0,phase:'steady',hikari:{active:1,max:12},database:[],gc:[]};
    const a=analyze({},[base,{...base,phase:'cooldown',wallMillis:90000}],[],[{ok:true}]);
    assert.equal(a.monitoring,'BLOCKED');assert.equal(a.maxSampleGapSeconds,90);
});
