export const MAX_BYTES = 2_000_000;
const names = new Set(['send_message','list_apps','open_app','open_maps','set_alarm','find_contact','call_contact','prepare_sms',
  'search_music','control_media','observe_screen','tap_element','type_text','scroll_screen','press_back','press_home']);
export function validateRequest(value: unknown, model: string): Record<string, unknown> {
  if (!model || !/^[a-zA-Z0-9._:-]{1,100}$/.test(model)) throw Error('Invalid server model configuration');
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw Error('Invalid request');
  const body = value as Record<string, unknown>;
  const allowed = new Set(['input','tools','instructions','previous_response_id','model','parallel_tool_calls','reasoning','max_output_tokens']);
  if (Object.keys(body).some(k => !allowed.has(k))) throw Error('Unsupported request field');
  if (!(typeof body.input === 'string' || Array.isArray(body.input)) || JSON.stringify(body.input).length > 1_900_000) throw Error('Invalid input');
  if (typeof body.instructions !== 'string' || body.instructions.length > 12_000) throw Error('Invalid instructions');
  if (!Array.isArray(body.tools) || body.tools.length > names.size || JSON.stringify(body.tools).length > 30_000) throw Error('Invalid tools');
  const seen = new Set();
  for (const tool of body.tools) {
    if (!tool || tool.type !== 'function' || !names.has(tool.name) || seen.has(tool.name) ||
        !tool.parameters || typeof tool.parameters !== 'object' || tool.async) throw Error('Unsupported tool');
    seen.add(tool.name);
  }
  if (body.previous_response_id !== undefined && (typeof body.previous_response_id !== 'string' ||
      !/^resp_[A-Za-z0-9_-]{1,200}$/.test(body.previous_response_id))) throw Error('Invalid response reference');
  // Clients cannot choose price tier, token budget, hosted tools, endpoint or execution mode.
  return {input:body.input, tools:body.tools, instructions:body.instructions,
    ...(body.previous_response_id ? {previous_response_id:body.previous_response_id} : {}),
    model, reasoning:{effort:'low'}, max_output_tokens:4096,
    parallel_tool_calls:false, store:true};
}

export async function readBounded(req: Request): Promise<unknown> {
  if (Number(req.headers.get('content-length')) > MAX_BYTES) throw Error('Request too large');
  const reader = req.body?.getReader();
  if (!reader) throw Error('Missing body');
  const chunks: Uint8Array[] = []; let size=0;
  try {
    while (true) {
      const {done,value}=await reader.read(); if(done) break;
      size+=value.length; if(size>MAX_BYTES) {await reader.cancel(); throw Error('Request too large');}
      chunks.push(value);
    }
  } finally { reader.releaseLock(); }
  const bytes=new Uint8Array(size); let offset=0;
  for(const chunk of chunks) {bytes.set(chunk,offset);offset+=chunk.length;}
  return JSON.parse(new TextDecoder().decode(bytes));
}
