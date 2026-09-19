import { readBounded, validateRequest } from './policy.ts';

const reply = (status: number, message: string) => Response.json({error:{message}},
  {status,headers:{'Cache-Control':'no-store'}});

export async function handleRequest(req: Request, env = Deno.env.get, transport: typeof fetch = fetch): Promise<Response> {
  if(req.method !== 'POST') return reply(405,'POST required.');
  const url=env('SUPABASE_URL');
  const serviceKey=env('SUPABASE_SERVICE_ROLE_KEY');
  const openaiKey=env('OPENAI_API_KEY');
  const model=env('OPENAI_MODEL');
  if(!url || !serviceKey || !openaiKey || !model) return reply(503,'Gateway is not configured.');
  const authorization=req.headers.get('authorization') || '';
  if(!/^Bearer [A-Za-z0-9._-]+$/.test(authorization)) return reply(401,'Sign in to SOL.');
  try {
    // Never decode a JWT and assume it is authenticated. Auth checks signature and user state.
    const auth=await transport(`${url}/auth/v1/user`,{headers:{apikey:serviceKey,Authorization:authorization},signal:AbortSignal.timeout(10_000)});
    if(!auth.ok) return reply(401,'Your session expired. Sign in again.');
    const user=await auth.json();
    if(!user.id || user.is_anonymous || !user.email_confirmed_at) return reply(403,'A verified beta account is required.');
    let body:Record<string,unknown>;
    try {body=validateRequest(await readBounded(req),model);} catch {return reply(400,'Invalid or oversized model request.');}
    const headers={apikey:serviceKey,Authorization:`Bearer ${serviceKey}`,'Content-Type':'application/json'};
    if(body.previous_response_id) {
      const query=`response_id=eq.${encodeURIComponent(String(body.previous_response_id))}&user_id=eq.${encodeURIComponent(user.id)}&expires_at=gt.${encodeURIComponent(new Date().toISOString())}&select=response_id`;
      const owner=await transport(`${url}/rest/v1/sol_response_owners?${query}`,{headers,signal:AbortSignal.timeout(10_000)});
      if(!owner.ok) return reply(503,'Gateway storage unavailable.');
      if((await owner.json()).length !== 1) return reply(403,'Conversation is unavailable. Start a new request.');
    }
    const quota=await transport(`${url}/rest/v1/rpc/sol_reserve_request`,{method:'POST',headers,
      body:JSON.stringify({p_user:user.id}),signal:AbortSignal.timeout(10_000)});
    if(!quota.ok) return reply(503,'Gateway quota storage unavailable.');
    if(await quota.json() !== true) return reply(429,'Beta access is disabled or your request limit was reached.');
    const upstream=await transport('https://api.openai.com/v1/responses',{method:'POST',
      headers:{Authorization:`Bearer ${openaiKey}`,'Content-Type':'application/json'},
      body:JSON.stringify(body),signal:AbortSignal.timeout(55_000)});
    if(!upstream.ok) return reply(upstream.status===429?429:502,'Model service unavailable. Try again later.');
    const result=await upstream.json();
    if(typeof result.id !== 'string') return reply(502,'Invalid model response.');
    const saved=await transport(`${url}/rest/v1/sol_response_owners`,{method:'POST',headers,
      body:JSON.stringify({response_id:result.id,user_id:user.id}),signal:AbortSignal.timeout(10_000)});
    if(!saved.ok) return reply(503,'Conversation could not be secured. Start a new request.');
    // Do not log prompts, outputs, screenshots, credentials or contact data.
    return Response.json(result,{headers:{'Cache-Control':'no-store'}});
  } catch {return reply(503,'Gateway temporarily unavailable. Try again later.');}
}
if (import.meta.main) Deno.serve(req => handleRequest(req));
