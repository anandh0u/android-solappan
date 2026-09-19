import { readBounded, validateRequest, MAX_BYTES } from './policy.ts';
const sample=()=>({input:'Hello',instructions:'Use registered tools.',tools:[]});
function assert(value:unknown) {if(!value) throw Error('Assertion failed');}
Deno.test('server controls model cost and disables parallel tools',()=>{
 const body=validateRequest({...sample(),model:'expensive-model',max_output_tokens:999999},'configured-model');
 assert(body.model==='configured-model' && body.max_output_tokens===4096 && body.parallel_tool_calls===false);
});
Deno.test('rejects hosted and unregistered tools',()=>{
 for(const tools of [[{type:'web_search'}],[{type:'function',name:'execute_shell',parameters:{}}]]) {
  let rejected=false;try{validateRequest({...sample(),tools},'configured-model');}catch{rejected=true;}assert(rejected);
 }
});
Deno.test('rejects extra request fields and invalid continuation',()=>{
 for(const extra of [{background:true},{previous_response_id:'https://example.com'},{input:null}]) {
  let rejected=false;try{validateRequest({...sample(),...extra},'configured-model');}catch{rejected=true;}assert(rejected);
 }
});
Deno.test('bounded reader rejects oversized body',async()=>{
 let rejected=false;try{await readBounded(new Request('https://localhost',{method:'POST',body:'x'.repeat(MAX_BYTES+1)}));}catch{rejected=true;}assert(rejected);
});
