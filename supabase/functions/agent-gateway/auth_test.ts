import {handleRequest} from './index.ts';
const env=(name:string)=>({SUPABASE_URL:'https://test.supabase.co',SUPABASE_SERVICE_ROLE_KEY:'test-service',OPENAI_API_KEY:'test-provider',OPENAI_MODEL:'test-model'}[name]);
const user={id:'00000000-0000-0000-0000-000000000001',email_confirmed_at:'2026-01-01'};
const request=(extra={},auth='Bearer test-token')=>new Request('https://test.supabase.co/functions/v1/agent-gateway',{
 method:'POST',headers:{Authorization:auth},body:JSON.stringify({input:'Hello',instructions:'Use tools',tools:[],...extra})});
function mock(replies:unknown[]) {
 let count=0;
 const transport:typeof fetch=()=>Promise.resolve(Response.json(replies[count++]));
 return {transport,count:()=>count};
}
function assert(value:unknown){if(!value)throw Error('Assertion failed');}
Deno.test('missing bearer never reaches auth or model',async()=>{
 const m=mock([]);const response=await handleRequest(request({},''),env,m.transport);
 assert(response.status===401 && m.count()===0);
});
Deno.test('invalid token rejected before database and model',async()=>{
 const transport:typeof fetch=()=>Promise.resolve(new Response('',{status:401}));
 assert((await handleRequest(request(),env,transport)).status===401);
});
Deno.test('anonymous and unverified users cannot spend credits',async()=>{
 for(const account of [{...user,is_anonymous:true},{id:user.id}]) {
  const m=mock([account]);assert((await handleRequest(request(),env,m.transport)).status===403 && m.count()===1);
 }
});
Deno.test('foreign continuation fails closed',async()=>{
 const m=mock([user,[]]);assert((await handleRequest(request({previous_response_id:'resp_other'}),env,m.transport)).status===403 && m.count()===2);
});
Deno.test('quota rejection never calls model',async()=>{
 const m=mock([user,false]);assert((await handleRequest(request(),env,m.transport)).status===429 && m.count()===2);
});
Deno.test('successful request records response ownership before returning',async()=>{
 const m=mock([user,true,{id:'resp_owned',output:[]},{}]);
 const response=await handleRequest(request(),env,m.transport);
 assert(response.status===200 && m.count()===4 && (await response.json()).id==='resp_owned');
});
