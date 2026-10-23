import { analyze, extractSpeech, checkClaim, ServiceError } from './engine.mjs';
const json = (data: unknown, status=200) => new Response(JSON.stringify(data), {status,headers:{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'}});
async function rpc(name: string, args: unknown) {
  const key = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY');
  const res = await fetch(Deno.env.get('SUPABASE_URL') + '/rest/v1/rpc/' + name, { method:'POST',headers:{apikey:key!,Authorization:'Bearer '+key,'Content-Type':'application/json'},body:JSON.stringify(args),signal:AbortSignal.timeout(10000) });
  if (!res.ok) throw new Error('configuration');
  return res.json();
}
Deno.serve(async req => {
  try {
    if (req.method !== 'POST') return json({error:'Método não permitido.'},405);
    const token = req.headers.get('x-fullfact-token') ?? '';
    if (!/^[a-f0-9]{32}$/.test(token)) return json({error:'Ative o aplicativo com seu código pessoal.'},401);
    const hash = [...new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(token)))].map(x=>x.toString(16).padStart(2,'0')).join('');
    // Health checks authenticate but do not consume the daily analysis allowance.
    const health = new URL(req.url).pathname.endsWith('/health');
    const access = await rpc('fullfact_authorize',{token_hash:hash,consume:!health});
    if (access !== 'ok') return json({error:access==='limit'?'Seu limite de teste diário foi atingido. Tente amanhã.':'Código de ativação inválido ou expirado.'},access==='limit'?429:401);
    if (health) return json({ready:true,model:'gemini-3.5-flash-lite'});
    if (Number(req.headers.get('content-length') ?? 0)>4000000) return json({error:'Trecho muito longo. Use até 25 segundos.'},413);
    const reader=req.body?.getReader();if(!reader)return json({error:'Trecho ausente.'},400);
    const chunks:Uint8Array[]=[];let bytes=0;
    while(true){const {done,value}=await reader.read();if(done)break;bytes+=value.length;if(bytes>4000000){await reader.cancel();return json({error:'Trecho muito longo.'},413);}chunks.push(value);}
    const body=new Uint8Array(bytes);let offset=0;for(const chunk of chunks){body.set(chunk,offset);offset+=chunk.length;}
    let input;try{input=JSON.parse(new TextDecoder().decode(body));}catch{return json({error:'Pedido inválido.'},400);}
    if ((!input.audioBase64 && !input.transcript && !input.youtubeUrl) || (input.audioBase64 && (typeof input.audioBase64!=='string'||input.audioBase64.length>1300000||!/^UklGR/.test(input.audioBase64))) || (input.imageBase64 && (typeof input.imageBase64!=='string'||input.imageBase64.length>1000000)) || (input.transcript && (typeof input.transcript!=='string'||input.transcript.length>12000))) return json({error:'Use um trecho de áudio ou uma transcrição válida.'},400);
    if(input.youtubeUrl && (typeof input.youtubeUrl!=='string'||!/^https:\/\/www\.youtube\.com\/watch\?v=[A-Za-z0-9_-]{11}$/.test(input.youtubeUrl)))return json({error:'Use um link público válido do YouTube.'},400);
    input.startSeconds=Math.max(0,Math.min(28800,Number(input.startSeconds)||0));
    input.participants=Array.isArray(input.participants)?input.participants.slice(0,12).map((p:any)=>({name:String(p.name??'').slice(0,80),context:String(p.context??'').slice(0,160)})):[];
    input.debateDate=String(input.debateDate??'').slice(0,32);input.source=String(input.source??'').slice(0,300);
    const apiKey=await rpc('fullfact_gemini_secret',{});
    if(typeof apiKey!=='string'||!apiKey)return json({error:'Serviço ainda não configurado.'},503);
    const route = new URL(req.url).pathname.split('/').pop();
    if(route==='transcribe')return json(await extractSpeech(apiKey,input));
    if(route==='check'){
      if(typeof input.claim!=='string'||input.claim.length<15||input.claim.length>3000||!input.transcript?.includes(input.claim))return json({error:'A afirmação precisa constar literalmente na transcrição.'},400);
      return json(await checkClaim(apiKey,input.claim,input));
    }
    return json(await analyze(apiKey,input));
  } catch(error) {
    // No audio, API keys, activation codes or upstream bodies in logs.
    if(error instanceof ServiceError)return json({error:error.message,code:error.code},error.status);
    return json({error:'O serviço não concluiu a análise. Tente novamente em instantes.'},503);
  }
});

