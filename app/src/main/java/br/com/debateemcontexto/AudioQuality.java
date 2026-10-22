package br.com.debateemcontexto;

/** Signal level only: loud noise must never be presented as intelligible speech. */
public final class AudioQuality {
    private double quietSeconds;
    public String update(short[] samples, int count, int sampleRate) {
        if (count <= 0) return "Sem amostras de áudio.";
        double sum = 0;
        int clipped = 0;
        for (int i = 0; i < count; i++) {
            double normalized = samples[i] / 32768.0;
            sum += normalized * normalized;
            if (Math.abs((int) samples[i]) >= 32700) clipped++;
        }
        double db = 20 * Math.log10(Math.max(1e-9, Math.sqrt(sum / count)));
        quietSeconds = db < -45 ? quietSeconds + (double) count / sampleRate : 0;
        if ((double) clipped / count > 0.02) return "Áudio distorcido. Afaste o celular ou reduza o volume da TV.";
        if (quietSeconds >= 3) return "Áudio baixo ou silêncio. Se alguém estiver falando, aproxime o celular ou aumente o volume da TV.";
        return "Medindo o áudio. A compreensão das falas ainda não está sendo analisada.";
    }
}
