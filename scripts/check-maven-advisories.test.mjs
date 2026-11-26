import { test } from 'node:test';
import assert from 'node:assert/strict';
import { queriesFromTree, evaluateResponse } from './check-maven-advisories.mjs';
test('Maven scan includes transitive and test coordinates, deduplicates and omits project identity', () => {
    const dependency = { groupId: 'org.example', artifactId: 'library', version: '1.2.3', scope: 'test' };
    const queries = queriesFromTree({ groupId:'private.project',artifactId:'app',version:'1',scope:'',children:[dependency,{ ...dependency,children:[{ groupId:'org.example',artifactId:'nested',version:'2',scope:'runtime' }] }] });
    assert.equal(queries.length,2);assert.equal(queries[0].package.name,'org.example:library');assert.equal(JSON.stringify(queries).includes('private.project'),false);
});
test('advisory results map to exact scanned versions and missing inventory cannot pass', () => {
    assert.throws(()=>queriesFromTree({ children:[] }));
    const q=[{ package:{name:'a:b'},version:'1' }];
    assert.deepEqual(evaluateResponse(q,{results:[{vulns:[{id:'GHSA-example'}]}]}).findings,[{name:'a:b',version:'1',id:'GHSA-example'}]);
    assert.equal(evaluateResponse(q,{results:[{}]}).findings.length,0);
    for(const result of [{results:[]},{error:'failed'},{results:[{next_page_token:'more'}]},{results:[{vulns:'invalid'}]}])assert.throws(()=>evaluateResponse(q,result));
});
