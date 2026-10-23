package br.com.debateemcontexto;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
final class AnalysisSession {
    private static AnalysisSession instance;
    static synchronized AnalysisSession get(Context context) { if(instance==null)instance=new AnalysisSession(context.getApplicationContext()); return instance; }
    final SecureSettings settings;
    final JSONArray checks=new JSONArray();
    String transcript="",stage="Aguardando a primeira fala.",source="",debateDate="";
    JSONArray participants=new JSONArray();
    final AtomicBoolean busy=new AtomicBoolean();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService network=Executors.newSingleThreadExecutor();
    private java.lang.ref.WeakReference<Runnable> observer=new java.lang.ref.WeakReference<>(null);
    private AnalysisSession(Context context){settings=new SecureSettings(context);}
    void observe(Runnable callback){observer=new java.lang.ref.WeakReference<>(callback);}
    void notifyUi(){main.post(()->{Runnable listener=observer.get();if(listener!=null)listener.run();});}
    void setStage(String message){main.post(()->{stage=message;notifyUi();});}
    void submit(byte[] wav,byte[] image,String text){ submitInput(wav,image,text,null,0); }
    void submitVideo(String url,int start){ submitInput(null,null,null,url,start); }
    private void submitInput(byte[] wav,byte[] image,String text,String youtubeUrl,int start){
        if(!settings.ready()){setStage("Ative a conexão com a análise na etapa Preparar.");return;}
        if(!busy.compareAndSet(false,true)){setStage("Analisando o trecho anterior. Este intervalo não foi enviado; a captação será retomada no próximo trecho.");return;}
        final String participantsJson=participants.toString(),date=debateDate,origin=source;
        network.execute(()->{
            try{
                setStage("Transcrevendo e identificando afirmações…");
                JSONObject input=new JSONObject();
                if(wav!=null)input.put("audioBase64",Base64.encodeToString(wav,Base64.NO_WRAP));
                if(image!=null)input.put("imageBase64",Base64.encodeToString(image,Base64.NO_WRAP));
                if(text!=null)input.put("transcript",text);
                if(youtubeUrl!=null){input.put("youtubeUrl",youtubeUrl);input.put("startSeconds",start);input.put("source",youtubeUrl);}
                input.put("participants",new JSONArray(participantsJson));input.put("debateDate",date);input.put("source",origin);
                JSONObject speech=AnalysisClient.request(settings.endpoint(),settings.token(),"/transcribe",input);
                String heard=speech.optString("transcript");main.post(()->{transcript=heard;notifyUi();});
                if(!speech.optString("quality").equals("clear")){setStage("Fala pouco compreensível: "+speech.optString("qualityReason","Ajuste o volume e reduza o ruído."));return;}
                JSONArray claims=speech.optJSONArray("claims");
                if(claims==null||claims.length()==0){setStage("Trecho transcrito. Nenhuma afirmação factual completa selecionada; continue acompanhando.");return;}
                for(int i=0;i<claims.length();i++){
                    setStage("Pesquisando fontes para a afirmação "+(i+1)+" de "+claims.length()+"…");
                    JSONObject request=new JSONObject();request.put("claim",claims.getString(i));request.put("transcript",heard);request.put("debateDate",date);request.put("source",origin);
                    JSONObject check=AnalysisClient.request(settings.endpoint(),settings.token(),"/check",request);
                    if(youtubeUrl!=null){check.put("sourceVideo",youtubeUrl);check.put("startSeconds",start);}
                    main.post(()->{checks.put(check);notifyUi();});
                }
                setStage("Checagem concluída. Veja os resultados e as fontes na etapa Checagens.");
            }catch(Exception error){setStage(error.getMessage()==null?"A análise não foi concluída. Tente novamente.":error.getMessage());}
            finally{busy.set(false);notifyUi();}
        });
    }
    void testConnection(String endpoint,String token,java.util.function.Consumer<String> callback){
        network.execute(()->{String result;try{AnalysisClient.request(endpoint,token,"/health",new JSONObject());settings.save(endpoint,token);result="Conexão ativada. A análise está disponível.";}catch(Exception error){result=error.getMessage();}final String message=result;main.post(()->{callback.accept(message);notifyUi();});});
    }
}
