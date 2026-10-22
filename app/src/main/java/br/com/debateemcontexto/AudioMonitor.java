package br.com.debateemcontexto;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.util.function.Consumer;

final class AudioMonitor {
    private volatile boolean running;
    private Thread worker;
    @android.annotation.SuppressLint("MissingPermission")
    synchronized void start(Consumer<String> listener) {
        if (worker != null && worker.isAlive()) return;
        running = true;
        worker = new Thread(() -> {
            AudioRecord recorder = null;
            try {
                int size = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if (size <= 0) throw new IllegalStateException();
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(size, 16000));
                if (recorder.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException();
                recorder.startRecording();
                AudioQuality quality = new AudioQuality();
                short[] buffer = new short[4000];
                while (running) {
                    int count = recorder.read(buffer, 0, buffer.length, AudioRecord.READ_NON_BLOCKING);
                    if (count < 0) throw new IllegalStateException();
                    if (count == 0) { Thread.sleep(20); continue; }
                    listener.accept(quality.update(buffer, count, 16000));
                }
            } catch (Exception error) {
                if (running) listener.accept("Não foi possível ouvir. Confira a permissão e se outro aplicativo está usando o microfone.");
            } finally {
                if (recorder != null) {
                    try { recorder.stop(); } catch (IllegalStateException ignored) { }
                    recorder.release();
                }
                running = false;
            }
        }, "debate-audio");
        worker.start();
    }
    synchronized void stop() {
        running = false;
        if (worker != null) {
            try { worker.join(500); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        }
    }
}

