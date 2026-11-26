import {spawnSync} from 'node:child_process';
import {readFileSync,writeFileSync,existsSync,statSync} from 'node:fs';
import {resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

// Never return matching text, credentials, identities, or raw Git diagnostics.
export function scanText(text,path){
    const findings=[];
    const patterns=[
        ['jwt',/\beyJ[A-Za-z0-9_-]{12,}\.[A-Za-z0-9_-]{12,}\.[A-Za-z0-9_-]{12,}/g],
        ['private-key',/-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/g],
        ['provider-token',/\b(?:ghp_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|AKIA[A-Z0-9]{16}|AIza[A-Za-z0-9_-]{35}|sk-(?:proj-)?[A-Za-z0-9_-]{32,})/g],
        ['password-field',/["'](?:password|currentPassword|newPassword)["']\s*:\s*["']([^"'\r\n]+)["']/gi],
        ['secret-property',/^(?:spring\.datasource\.password|jwt\.secret|upstox\.api\.token|(?:DB|SMTP)_PASSWORD|JWT_SECRET|SECURITY_ENCRYPTION_KEY)[ \t]*=[ \t]*([^\r\n]+)/gmi]
    ];
    for(const [kind,pattern] of patterns)for(const match of text.matchAll(pattern)){
        const value=match[1]||match[0];
        if(value.includes('${')||value.includes('{{')||value.trim().startsWith('$(')||/^\s*(?:#|$)/.test(value))continue;
        findings.push({path,line:text.slice(0,match.index).split('\n').length,kind,context:/src\/test\/|\.test\.|\.example$/.test(path)?'fixture-or-template-review':'review-required'});
    }
    return findings;
}
function git(args,input){
    const r=spawnSync('git',args,{input,encoding:null,windowsHide:true,timeout:60000,maxBuffer:64*1024*1024});
    if(r.error||r.status!==0)throw Error('Git exposure scan failed; no raw diagnostics reproduced');
    return r.stdout;
}
export function review(){
    const paths=git(['ls-files','--cached','--others','--exclude-standard','-z']).toString().split('\0').filter(Boolean);
    const current=[],skipped=[];
    for(const path of new Set(paths)){
        if(!existsSync(path)||!statSync(path).isFile())continue;
        if(statSync(path).size>2*1024*1024){skipped.push({path,reason:'size-limit'});continue;}
        const bytes=readFileSync(path);if(bytes.includes(0)){skipped.push({path,reason:'binary'});continue;}
        current.push(...scanText(bytes.toString('utf8'),path));
    }
    const objects=git(['rev-list','--objects','--all']).toString().trim().split('\n').map(line=>{const at=line.indexOf(' ');return {id:at<0?line:line.slice(0,at),path:at<0?'':line.slice(at+1)};});
    const inventory=git(['cat-file','--batch-check=%(objectname) %(objecttype) %(objectsize)'],Buffer.from(objects.map(o=>o.id).join('\n')+'\n')).toString().trim().split('\n');
    const blobs=[];
    for(let i=0;i<inventory.length;i++){const [id,type,size]=inventory[i].split(' ');if(type==='blob'){if(Number(size)<=2*1024*1024)blobs.push({...objects[i],id,size:Number(size)});else skipped.push({path:objects[i].path,blob:id,reason:'history-size-limit'});}}
    const history=[];let binaryHistory=0;
    if(blobs.length){const batch=git(['cat-file','--batch'],Buffer.from(blobs.map(o=>o.id).join('\n')+'\n'));let offset=0;
        for(const blob of blobs){const end=batch.indexOf(10,offset);if(end<0)throw Error('Incomplete Git blob response');const header=batch.subarray(offset,end).toString().split(' ');if(header[0]!==blob.id||header[1]!=='blob'||Number(header[2])!==blob.size)throw Error('Unexpected Git blob response');const bytes=batch.subarray(end+1,end+1+blob.size);if(bytes.length!==blob.size)throw Error('Truncated Git blob');offset=end+1+blob.size+1;if(bytes.includes(0)){binaryHistory++;continue;}history.push(...scanText(bytes.toString('utf8'),blob.path).map(f=>({...f,blob:blob.id})));}
    }
    return {at:new Date().toISOString(),reachableCommits:Number(git(['rev-list','--all','--count']).toString()),historyBlobs:blobs.length,binaryHistory,current,history,skipped,scope:'Current tracked/nonignored text and all locally reachable Git blobs up to2MiB; no remote fetch, unreachable objects, Git-LFS contents or unknown-secret guarantee. Findings require private review; fixtures are not automatically waived.'};
}
if(process.argv[1]&&resolve(process.argv[1])===fileURLToPath(import.meta.url)){
    try {const report=review();writeFileSync(process.argv[2]||'target/secret-exposure-review.json',JSON.stringify(report,null,2));console.log(JSON.stringify({reachableCommits:report.reachableCommits,historyBlobs:report.historyBlobs,currentFindings:report.current.length,historyFindings:report.history.length}));}
    catch {console.error('Exposure review incomplete; no secret values or raw diagnostics printed.');process.exitCode=1;}
}
