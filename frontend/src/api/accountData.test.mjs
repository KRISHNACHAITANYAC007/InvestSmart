import test from "node:test";
import assert from "node:assert/strict";
import {createAccountDataLoader} from "./accountData.js";
const turn=()=>new Promise(r=>setTimeout(r,0));
function deferred(){let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b;});return {promise,resolve,reject};}
test("cash and holdings load immediately while slow prices refresh independently",async()=>{
    const prices=deferred();let state;let reads=0;
    const client={get:async()=>({data:{balance:++reads,holdings:[]}}),post:()=>prices.promise};
    const loader=createAccountDataLoader(client,s=>{state=s;});const work=loader.refresh(true);await turn();assert.equal(state.loading,false);assert.equal(state.portfolio.balance,1);assert.equal(state.refreshing,true);
    prices.resolve({data:{staleSymbols:[],asOf:"2026-11-02T15:30:00"}});await work;assert.equal(state.portfolio.balance,2);assert.equal(state.refreshing,false);loader.dispose();
});
test("outdated account responses cannot replace a newer fund balance",async()=>{
    const old=deferred();let reads=0,state;
    const loader=createAccountDataLoader({get:()=>++reads===1?old.promise:Promise.resolve({data:{balance:200}})},s=>{state=s;});
    const first=loader.reload();await loader.reload();old.resolve({data:{balance:100}});await first;assert.equal(state.portfolio.balance,200);loader.dispose();
});
test("provider and account errors preserve saved data and cleanup ignores late responses",async()=>{
    let fail=false,state;const late=deferred();
    const loader=createAccountDataLoader({get:()=>fail?late.promise:Promise.resolve({data:{balance:500}}),post:async()=>{throw new Error("network");}},s=>{state=s;});
    await loader.refresh();assert.equal(state.portfolio.balance,500);assert.equal(state.refreshInfo.failed,true);
    fail=true;const pending=loader.reload();loader.dispose();late.resolve({data:{balance:0}});await pending;assert.equal(state.portfolio.balance,500);
});
