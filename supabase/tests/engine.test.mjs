import test from 'node:test';
import assert from 'node:assert/strict';
import {groundedResult,analyze,generate} from '../functions/fullfact/engine.mjs';
const response = data => async () => ({ok:true,json:async()=>data});
test('não emite verdadeiro sem evidência vinculada à resposta',()=>{
 const result=groundedResult({candidates:[{content:{parts:[{text:'CLASSIFICAÇÃO: Verdadeiro\nConfie em mim.'}]},groundingMetadata:{groundingChunks:[{web:{uri:'https://example.com',title:'Sem suporte'}}]}}]},'Exemplo');
 assert.equal(result.verdict,'Evidência insuficiente');assert.equal(result.sources.length,0);
});
test('links inseguros nunca viram fontes clicáveis',()=>{
 const result=groundedResult({candidates:[{content:{parts:[{text:'CLASSIFICAÇÃO: Falso\nExplicação'}]},groundingMetadata:{groundingChunks:[{web:{uri:'javascript:alert(1)'}}],groundingSupports:[{groundingChunkIndices:[0]}]}}]},'Exemplo');
 assert.equal(result.sources.length,0);assert.equal(result.verdict,'Evidência insuficiente');
});
test('mantém fonte HTTPS sustentada pelo grounding',()=>{
 const result=groundedResult({candidates:[{content:{parts:[{text:'CLASSIFICAÇÃO: Falso\nO dado não corresponde ao período.'}]},groundingMetadata:{groundingChunks:[{web:{uri:'https://ibge.gov.br/dado',title:'IBGE'}}],groundingSupports:[{groundingChunkIndices:[0]}]}}]},'Exemplo');
 assert.equal(result.verdict,'Evidência insuficiente');assert.equal(result.sources[0].title,'IBGE');
});
test('áudio incompreensível não dispara pesquisa nem veredito',async()=>{
 let calls=0;const result=await analyze('test',{audioBase64:'test'},async()=>{calls++;return {ok:true,json:async()=>({candidates:[{content:{parts:[{text:JSON.stringify({transcript:'',quality:'inaudible',claims:['Inventado']})}]}}]})}});
 assert.equal(calls,1);assert.equal(result.checks.length,0);
});
test('afirmações inventadas fora da transcrição são descartadas',async()=>{
 const result=await analyze('test',{transcript:'Acho que vamos melhorar.'},response({candidates:[{content:{parts:[{text:JSON.stringify({transcript:'Acho que vamos melhorar.',quality:'clear',claims:['O desemprego caiu pela metade.']})}]}}]}));
 assert.equal(result.claims.length,0);assert.equal(result.checks.length,0);
});
test('limite do provedor vira orientação compreensível',async()=>{
 await assert.rejects(generate('test',{},async()=>({ok:false,status:429})),e=>e.status===429&&e.code==='provider_limit');
});

test('classificação definitiva exige duas origens vinculadas',()=>{
const result=groundedResult({candidates:[{content:{parts:[{text:'CLASSIFICAÇÃO: Verdadeiro\nDados cruzados.'}]},groundingMetadata:{groundingChunks:[{web:{uri:'https://ibge.gov.br/dado',title:'IBGE'}},{web:{uri:'https://reuters.com/noticia',title:'Reuters'}}],groundingSupports:[{groundingChunkIndices:[0,1]}]}}]},'Exemplo');
assert.equal(result.verdict,'Verdadeiro');assert.equal(result.crossCheck.independentOrigins,2);
});

