import {test} from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,writeFileSync,unlinkSync,rmdirSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
const tool=fileURLToPath(new URL('./check-report-index-evidence.mjs',import.meta.url));
function pair(){
    const specs=[['finance','paper_finance_events','e','ix_finance_event_user_time',100],['finance','paper_finance_events','e','ix_finance_event_user_time',2000],['execution','paper_fills','f','ix_paper_fill_user_time',100],['execution','paper_fills','f','ix_paper_fill_user_time',2000],['finance-index-after','paper_finance_events','e','ix_finance_event_user_time',2000],['fills-index-after','paper_fills','f','ix_paper_fill_user_time',2000]];
    const sample={wallMs:1,currentThreadCpuMs:null};
    const report={engine:'synthetic',observations:specs.map(([reader,table,alias,index,foreign])=>({reader,sql:`SELECT * FROM ${table} ${alias} WHERE user_id=?`,addedForeignRowsPerTable:foreign,parameterTypes:['Long'],estimatedPlan:'{}',actualIteratorPlan:[`Index range scan using ${index} (actual time=0..1 rows=1 loops=1)`],readerRepeatedMs:[1,1,1],queryRepeats:Array.from({length:3},()=>({returnedRows:1,elapsedMs:1,handlerReadDelta:{Handler_read_key:1}}))})),pureMatchingSamples:[sample,sample,sample],preparedMatchingSamples:[sample,sample,sample],matchingComparison:{instructions:1000,fullProofsEqual:true},distinctInstructionScenario:{matchedObligationIdentities:1000,fullProofsEqual:true},indexComparison:{migration:'20261127_reporting_period_indexes.sql'},finalTableRows:{fixture:1},mixedScenario:{fixture:true},indexes:{}};
    for(const key of ['ownedCandidateScenarios','productScenarios','batchScenarios','overflowScenarios','parallelReadScenarios'])report[key]=[{fixture:true}];
    for(const [name,columns] of Object.entries({ix_finance_event_user_time:['user_id','occurred_at','event_key'],ix_paper_fill_user_time:['user_id','created_at','id']}))report.indexes[name]=columns.map((Column_name,i)=>({Key_name:name,Column_name,Seq_in_index:i+1,Non_unique:1}));
    return [{...structuredClone(report),indexesPresentFromStart:false},{...structuredClone(report),indexesPresentFromStart:true}];
}
function run(reports){const dir=mkdtempSync(join(tmpdir(),'report-index-evidence-'));const paths=[join(dir,'before.json'),join(dir,'indexed.json')];try{reports.forEach((r,i)=>writeFileSync(paths[i],typeof r==='string'?r:JSON.stringify(r)));return spawnSync(process.execPath,[tool,...paths],{encoding:'utf8',windowsHide:true});}finally{for(const path of paths)unlinkSync(path);rmdirSync(dir);}}
test('valid comparison retains scope and never certifies production performance',()=>{const result=run(pair());assert.equal(result.status,0,result.stderr);const output=JSON.parse(result.stdout);assert.equal(output.structures[0].performanceAcceptance,false);assert.equal(output.narrowOwnedRangesVerified.length,6);});
test('changed outputs, absent scenarios, unique/wrong index order and missing range evidence fail',()=>{for(const alter of [r=>r.observations[0].queryRepeats[0].returnedRows=2,r=>delete r.overflowScenarios,r=>r.indexes.ix_paper_fill_user_time[0].Non_unique=0,r=>r.indexes.ix_paper_fill_user_time[0].Column_name='wrong',r=>r.observations[0].actualIteratorPlan=['Table scan'],r=>r.matchingComparison.fullProofsEqual=false]){const reports=pair();alter(reports[1]);assert.equal(run(reports).status,1);}});
test('malformed input does not leak report contents',()=>{const reports=pair();reports[0]='private-fixture-marker';const result=run(reports);assert.equal(result.status,1);assert.ok(!(result.stdout+result.stderr).includes('private-fixture-marker'));});
test('equal reports cannot replace growth evidence with duplicate small-history samples',()=>{const reports=pair();for(const report of reports)for(const row of report.observations)row.addedForeignRowsPerTable=100;assert.equal(run(reports).status,1);});
