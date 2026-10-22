package br.com.debateemcontexto;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends ComponentActivity {
    private final AudioMonitor audio = new AudioMonitor();
    private TextView status;
    private LinearLayout participants;
    private PreviewView preview;
    private ProcessCameraProvider cameraProvider;
    private boolean active;
    private boolean video;
    private int generation;
    private JSONArray roster;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try { roster = new JSONArray(getPreferences(0).getString("roster", "[]")); }
        catch (Exception error) { roster = new JSONArray(); }
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.rgb(15, 23, 42));
        scroll.addView(root);
        setContentView(scroll);
        // Keep content below system bars when targeting Android 15.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(24 + insets.getSystemWindowInsetLeft(), 24 + insets.getSystemWindowInsetTop(),
                24 + insets.getSystemWindowInsetRight(), 24 + insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.addView(label("Debate em Contexto", 28));
        root.addView(label("Base experimental • câmera e teste de volume", 15));
        root.addView(label("1. Quem participa?", 21));
        EditText name = input("Nome do participante");
        EditText context = input("Cargo, partido e contexto (opcional)");
        root.addView(name); root.addView(context);
        root.addView(button("Adicionar participante", () -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty()) { name.setError("Informe o nome"); return; }
            try {
                JSONObject person = new JSONObject();
                person.put("name", value); person.put("context", context.getText().toString().trim());
                roster.put(person);
                getPreferences(0).edit().putString("roster", roster.toString()).apply();
                name.setText(""); context.setText(""); renderRoster();
            } catch (Exception error) { status.setText("Não foi possível salvar o participante."); }
        }));
        participants = new LinearLayout(this); participants.setOrientation(LinearLayout.VERTICAL);
        root.addView(participants); renderRoster();
        root.addView(label("Cadastro manual. Associação de rosto e voz ainda não disponível.", 14));
        root.addView(label("2. Acompanhar a TV", 21));
        preview = new PreviewView(this);
        preview.setVisibility(View.GONE);
        root.addView(preview, new LinearLayout.LayoutParams(-1, 540));
        status = label("Toque em uma opção para começar. Áudio e imagem não são salvos nem enviados.", 17);
        root.addView(status);
        root.addView(button("Usar câmera + microfone", () -> requestStart(true)));
        root.addView(button("Usar somente microfone", () -> requestStart(false)));
        root.addView(button("Parar", this::stopCapture));
        root.addView(label("Debate em outro aplicativo", 21));
        root.addView(label("Captura de tela e áudio de YouTube/streaming ainda não implementada. Não há reprodução de vídeo dentro deste aplicativo.", 15));
        root.addView(label("Checagens", 21));
        root.addView(label("Ainda sem serviço de transcrição e pesquisa conectado. Esta versão não identifica falas nem emite vereditos.", 16));
    }
    private TextView label(String text, int size) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size);
        view.setTextColor(Color.WHITE); view.setPadding(0, 12, 0, 12); return view;
    }
    private EditText input(String hint) {
        EditText field = new EditText(this); field.setHint(hint); field.setTextColor(Color.WHITE);
        field.setHintTextColor(Color.LTGRAY); return field;
    }
    private Button button(String text, Runnable action) {
        Button button = new Button(this); button.setText(text);
        button.setOnClickListener(view -> action.run()); return button;
    }
    private void renderRoster() {
        participants.removeAllViews();
        for (int i = 0; i < roster.length(); i++) {
            JSONObject person = roster.optJSONObject(i);
            if (person != null) participants.addView(label(person.optString("name") + " • " + person.optString("context"), 16));
        }
        if (roster.length() > 0) participants.addView(button("Apagar cadastro", () -> {
            roster = new JSONArray(); getPreferences(0).edit().remove("roster").apply(); renderRoster();
        }));
    }
    private void requestStart(boolean withVideo) {
        stopCapture(); video = withVideo;
        boolean mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean camera = !video || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (mic && camera) startCapture();
        else requestPermissions(video ? new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}
            : new String[]{Manifest.permission.RECORD_AUDIO}, 10);
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request != 10) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            (video && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            status.setText("Permissão não concedida. Toque novamente para tentar ou ajuste as permissões nas configurações do celular."); return;
        }
        startCapture();
    }
    @android.annotation.SuppressLint("MissingPermission")
    private void startCapture() {
        active = true;
        final int session = ++generation;
        status.setText("Iniciando microfone…");
        audio.start(message -> runOnUiThread(() -> { if (active && session == generation) status.setText(message); }));
        if (!video) return;
        preview.setVisibility(View.VISIBLE);
        com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            if (!active || session != generation) return;
            try {
                cameraProvider = future.get();
                Preview cameraPreview = new Preview.Builder().build();
                cameraPreview.setSurfaceProvider(preview.getSurfaceProvider());
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, cameraPreview);
            } catch (Exception error) {
                stopCapture(); status.setText("Câmera indisponível. Você pode tentar o modo somente microfone.");
            }
        }, getMainExecutor());
    }
    private void stopCapture() {
        active = false; generation++; audio.stop();
        if (cameraProvider != null) cameraProvider.unbindAll();
        if (preview != null) preview.setVisibility(View.GONE);
        if (status != null) status.setText("Captação parada.");
    }
    @Override public void onPause() { stopCapture(); super.onPause(); }
}

