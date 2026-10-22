package br.com.debateemcontexto;

import org.junit.Test;
import static org.junit.Assert.*;

public class AudioQualityTest {
    @Test public void silenceNeedsThreeSeconds() {
        AudioQuality quality = new AudioQuality();
        short[] silence = new short[16000];
        assertFalse(quality.update(silence, silence.length, 16000).contains("Áudio baixo"));
        quality.update(silence, silence.length, 16000);
        assertTrue(quality.update(silence, silence.length, 16000).contains("Áudio baixo"));
    }
    @Test public void loudAudioDoesNotProveSpeechIsUnderstandable() {
        short[] samples = new short[16000];
        java.util.Arrays.fill(samples, (short) 4000);
        assertTrue(new AudioQuality().update(samples, samples.length, 16000).contains("ainda não"));
    }
    @Test public void clippingWarnsUser() {
        short[] samples = new short[16000];
        java.util.Arrays.fill(samples, Short.MIN_VALUE);
        assertTrue(new AudioQuality().update(samples, samples.length, 16000).contains("distorcido"));
    }
    @Test public void audibleSampleResetsSilenceWindow() {
        AudioQuality quality = new AudioQuality();
        short[] silence = new short[16000];
        short[] sound = new short[16000];
        java.util.Arrays.fill(sound, (short) 4000);
        quality.update(silence, silence.length, 16000);
        quality.update(silence, silence.length, 16000);
        quality.update(sound, sound.length, 16000);
        assertFalse(quality.update(silence, silence.length, 16000).contains("Áudio baixo"));
    }
}
