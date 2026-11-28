import {readFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import assert from 'node:assert/strict';
import {validateReport} from './check-report-cost-evidence.mjs';
try {
const paths=process.argv.slice(2);assert.equal(paths.length,2,'Pass pre-index and indexed report paths');
const reports=paths.map(p=>JSON.parse(readFileSync(p,'utf8')));
assert.equal(reports[0].indexesPresentFromStart,false);assert.equal(reports[1].indexesPresentFromStart,true);
const structures=reports.map(r=>validateReport(r,{requirePrepared:true}));
for(const report of reports){
    for(const key of ['ownedCandidateScenarios','productScenarios','batchScenarios','overflowScenarios','parallelReadScenarios'])assert.ok(Array.isArray(report[key])&&report[key].length>0);
    assert.ok(report.finalTableRows&&Object.keys(report.finalTableRows).length>0);assert.ok(report.mixedScenario);
    assert.equal(report.matchingComparison.instructions,1000);assert.equal(report.distinctInstructionScenario.matchedObligationIdentities,1000);
}
const shape=r=>r.observations.map(o=>({reader:o.reader,sql:o.sql,parameterTypes:o.parameterTypes,returnedRows:o.queryRepeats.map(s=>s.returnedRows)}));
assert.deepEqual(shape(reports[0]),shape(reports[1]));
for(const key of ['finalTableRows','mixedScenario','matchingComparison','ownedCandidateScenarios','productScenarios','batchScenarios','overflowScenarios','indexComparison','parallelReadScenarios'])assert.deepEqual(reports[0][key],reports[1][key]);
const expected={ix_finance_event_user_time:['user_id','occurred_at','event_key'],ix_paper_fill_user_time:['user_id','created_at','id']};
for(const report of reports){
    assert.equal(report.indexComparison.migration,'20261127_reporting_period_indexes.sql');
    assert.equal(report.matchingComparison.fullProofsEqual,true);assert.equal(report.distinctInstructionScenario.fullProofsEqual,true);
    for(const [name,columns] of Object.entries(expected)){
        const rows=Object.values(report.indexes).flat().filter(row=>row.Key_name===name).sort((a,b)=>a.Seq_in_index-b.Seq_in_index);
        assert.deepEqual(rows.map(row=>row.Column_name),columns);assert.ok(rows.every(row=>row.Non_unique===1));
    }
    assert.ok(Object.values(report.indexes).flat().every(row=>!row.Key_name.startsWith('ix_cost_probe_')));
}
function range(record,name){
    const lines=record.actualIteratorPlan.join('\n').split('\n');const selected=lines.find(line=>line.includes('Index range scan')&&line.includes('using '+name));
    assert.ok(selected,'Missing executed user/time range');assert.match(selected,/actual time=.*rows=1 loops=1/);
    return {reader:record.reader,foreignRows:record.addedForeignRowsPerTable,index:name,iteratorRows:1,handlers:record.queryRepeats.map(s=>s.handlerReadDelta)};
}
const ranges=[];
for(const [table,name,reader] of [['paper_finance_events','ix_finance_event_user_time','finance'],['paper_fills','ix_paper_fill_user_time','execution']]){
    const selection='FROM '+table+(reader==='finance'?' e ':' f ');
    const indexed=reports[1].observations.filter(o=>o.reader===reader&&o.sql.includes(selection));assert.equal(indexed.length,2);
    assert.deepEqual(indexed.map(row=>row.addedForeignRowsPerTable).sort((a,b)=>a-b),[100,2000]);
    for(const row of indexed)ranges.push(range(row,name));
    const after=reports[0].observations.find(o=>o.reader===(reader==='finance'?'finance-index-after':'fills-index-after')&&o.sql.includes(selection));ranges.push(range(after,name));
}
const result={structures,statementAndOutputShapesEqual:true,fixtureCardinalitiesGuardsAndParallelProofsEqual:true,approvedCatalogsVerified:true,narrowOwnedRangesVerified:ranges,reportSha256:paths.map(p=>({path:p,sha256:createHash('sha256').update(readFileSync(p)).digest('hex')})),scope:'Actual approved migration A/B and entire indexed synthetic matrix. Exact financial rows/report DTOs and ledger/replay assertions executed in Java; no retained DB, public-report completeness or production capacity/SLA acceptance.'};

console.log(JSON.stringify({...result,narrowOwnedRangesVerified:ranges.map(({handlers,...rest})=>rest)}));

} catch { console.error('Index evidence rejected: invalid input or measurement contract; source contents not printed.'); process.exitCode=1; }
