package br.com.debateemcontexto;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.util.function.Consumer;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
final class AudioMonitor {
    private volatile boolean running;
    private Thread worker;
    synchronized void start(Consumer<String> listener) { start(listener, null); }
    @android.annotation.SuppressLint("MissingPermission")
    synchronized void start(Consumer<String> listener, Consumer<byte[]> onClip) {
        if (worker != null && worker.isAlive()) { listener.accept("O microfone está encerrando a sessão anterior. Aguarde e tente novamente."); return; }
        running = true;
        worker = new Thread(() -> {
            AudioRecord recorder = null;
            try {
                int size = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if (size <= 0) throw new IllegalStateException();
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(size,16000));
                if (recorder.getState()!=AudioRecord.STATE_INITIALIZED) throw new IllegalStateException();
                recorder.startRecording(); AudioQuality quality=new AudioQuality(); short[] buffer=new short[4000];
                ByteArrayOutputStream pcm=new ByteArrayOutputStream(); int audible=0;
                while(running){
                    int count=recorder.read(buffer,0,buffer.length,AudioRecord.READ_NON_BLOCKING);
                    if(count<0)throw new IllegalStateException();if(count==0){Thread.sleep(20);continue;}
                    listener.accept(quality.update(buffer,count,16000));
                    for(int i=0;i<count;i++){pcm.write(buffer[i]&255);pcm.write((buffer[i]>>8)&255);if(Math.abs((int)buffer[i])>500)audible++;}
                    if(pcm.size()>=16000*2*20){
                        if(onClip!=null&&audible>8000)onClip.accept(wav(pcm.toByteArray()));
                        byte[] tail=pcm.toByteArray(); int from=Math.max(0,tail.length-64000); pcm.reset();pcm.write(tail,from,tail.length-from);audible=0;
                    }
                }
            }catch(Exception error){if(running)listener.accept("Não foi possível ouvir. Confira a permissão e se outro aplicativo está usando o microfone.");}
            finally{if(recorder!=null){try{recorder.stop();}catch(IllegalStateException ignored){}recorder.release();}running=false;}
        },"debate-audio");worker.start();
    }
    static byte[] wav(byte[] pcm){
        ByteBuffer data=ByteBuffer.allocate(44+pcm.length).order(ByteOrder.LITTLE_ENDIAN);
        data.put(new byte[]{'R','I','F','F'}).putInt(36+pcm.length).put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short)1).putShort((short)1).putInt(16000).putInt(32000).putShort((short)2).putShort((short)16).put(new byte[]{'d','a','t','a'}).putInt(pcm.length).put(pcm);return data.array();
    }
    synchronized void stop(){running=false;if(worker!=null){try{worker.join(500);}catch(InterruptedException error){Thread.currentThread().interrupt();}}}
}
