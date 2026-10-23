package br.com.debateemcontexto;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends ComponentActivity {
    private static final int BG = Color.rgb(244,245,239), INK = Color.rgb(24,46,41),
        MUTED = Color.rgb(91,111,101), GREEN = Color.rgb(23,107,75),
        LINE = Color.rgb(220,229,221), SOFT = Color.rgb(234,240,231),
        WARNING = Color.rgb(255,243,219);
    private final AudioMonitor audio = new AudioMonitor();
    private LinearLayout shell, body, participants, navigation;
    private ScrollView scroll;
    private TextView status, sessionBadge, cameraPlaceholder;
    private PreviewView preview;
    private Button startButton, stopButton;
    private ProcessCameraProvider cameraProvider;
    private boolean active, video = true;
    private int generation, page;
    private JSONArray roster;
    private AnalysisSession analysis;
    private TextView analysisStatus, transcriptText;
    private LinearLayout checksContainer;
    private boolean youtubeMode = true, youtubeAuto;
    private int youtubeStart = 618, renderedChecks = -1;
    private String youtubeUrl = "https://www.youtube.com/watch?v=5I_GPckal_4";
    private android.webkit.WebView player;
    private final android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable analysisObserver = this::refreshAnalysis;
    private final Runnable nextYoutubeClip = new Runnable() { public void run() {
        if (!youtubeAuto || page != 1 || !youtubeMode) return;
        if (!analysis.busy.get()) { analyzeYoutube(); }
        uiHandler.postDelayed(this, 2500);
    }};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        analysis = AnalysisSession.get(this); analysis.observe(analysisObserver);
        try { roster = new JSONArray(getPreferences(0).getString("roster", "[]")); }
        catch (Exception error) { roster = new JSONArray(); }
        if (state != null) { page = state.getInt("page"); video = state.getBoolean("video", true); }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        shell = column(); shell.setBackgroundColor(BG);
        shell.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(shell);
        LinearLayout header = row(); header.setPadding(dp(22), dp(18), dp(22), dp(16));
        TextView brand = text("fullfact IA.", 24, INK, true);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(badge("TESTE COM IA", SOFT, GREEN)); shell.addView(header);
        shell.addView(divider());
        scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        body = column(); body.setPadding(dp(22), dp(26), dp(22), dp(24)); scroll.addView(body);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        shell.addView(divider());
        navigation = row(); navigation.setPadding(dp(10), dp(8), dp(10), dp(8));
        shell.addView(navigation); showPage(page);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); return box; }
    private LinearLayout row() { LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.HORIZONTAL); box.setGravity(Gravity.CENTER_VERTICAL); return box; }
    private GradientDrawable shape(int color, int radius, int border) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(dp(radius));
        if (border != 0) shape.setStroke(dp(1), border); return shape;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        view.setIncludeFontPadding(false); view.setLineSpacing(dp(3), 1); return view;
    }
    private TextView badge(String value, int background, int foreground) {
        TextView view = text(value, 10, foreground, true); view.setLetterSpacing(.08f);
        view.setPadding(dp(10), dp(7), dp(10), dp(7)); view.setBackground(shape(background, 20, 0)); return view;
    }
    private void gap(LinearLayout parent, int height) { View spacer = new View(this); parent.addView(spacer, new LinearLayout.LayoutParams(1, dp(height))); }
    private View divider() { View line = new View(this); line.setBackgroundColor(LINE); line.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1))); return line; }
    private LinearLayout card() {
        LinearLayout box = column(); box.setPadding(dp(18), dp(20), dp(18), dp(20));
        box.setBackground(shape(Color.WHITE, 18, LINE));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(18);
        body.addView(box, params); return box;
    }
    private void paragraph(LinearLayout parent, String value) { TextView view = text(value, 14, MUTED, false); parent.addView(view); }
    private Button button(String title, boolean primary, Runnable action) {
        Button view = new Button(this); view.setText(title); view.setTextSize(14); view.setAllCaps(false);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setTextColor(primary ? Color.WHITE : GREEN); view.setMinHeight(dp(50)); view.setMinimumHeight(dp(50));
        view.setPadding(dp(16), dp(14), dp(16), dp(14)); view.setStateListAnimator(null);
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22000000), shape(primary ? GREEN : SOFT, 12, 0), null));
        view.setOnClickListener(v -> action.run()); return view;
    }
    private void fullButton(LinearLayout parent, Button button) { parent.addView(button, new LinearLayout.LayoutParams(-1, -2)); }
    private EditText field(LinearLayout parent, String label, String hint, int maxLength) {
        TextView caption = text(label, 12, INK, true); parent.addView(caption); gap(parent, 8);
        EditText input = new EditText(this); input.setSingleLine(true); input.setTextSize(15); input.setTextColor(INK);
        input.setHintTextColor(MUTED); input.setHint(hint); input.setPadding(dp(12), dp(14), dp(12), dp(14));
        input.setMinHeight(dp(50)); input.setBackground(shape(Color.WHITE, 10, LINE));
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(maxLength)});
        caption.setLabelFor(View.generateViewId()); input.setId(caption.getLabelFor());
        parent.addView(input, new LinearLayout.LayoutParams(-1, -2)); return input;
    }
    private void heading(String eyebrow, String title, String description) {
        TextView kicker = text(eyebrow, 11, GREEN, true); kicker.setLetterSpacing(.12f); body.addView(kicker); gap(body, 12);
        body.addView(text(title, 29, INK, true)); gap(body, 12); paragraph(body, description);
    }
    private void showPage(int destination) {
        youtubeAuto = false; uiHandler.removeCallbacks(nextYoutubeClip);
        if (destination != page || active) stopCapture();
        if (player != null) { player.destroy(); player = null; }
        analysisStatus = null; transcriptText = null; checksContainer = null; renderedChecks = -1;
        page = destination; status = null; preview = null; cameraPlaceholder = null;
        sessionBadge = null; startButton = null; stopButton = null;
        body.removeAllViews(); navigation.removeAllViews();
        String[] names = {"01  Preparar", "02  Acompanhar", "03  Checagens"};
        for (int i = 0; i < names.length; i++) {
            final int target = i;
            Button tab = button(names[i], false, () -> {
                ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(shell.getWindowToken(), 0);
                showPage(target);
            });
            tab.setTextSize(11); tab.setPadding(dp(4), dp(13), dp(4), dp(13));
            if (i != page) { tab.setTextColor(MUTED); tab.setBackgroundColor(Color.TRANSPARENT); }
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1); p.setMargins(dp(2), 0, dp(2), 0);
            tab.setSelected(i == page); navigation.addView(tab, p);
        }
        if (page == 0) setupPage(); else if (page == 1) livePage(); else checksPage();
        gap(body, 26); paragraph(body, "Teste com IA · trechos de áudio e imagens são enviados ao serviço de análise após você iniciar. Confira os resultados e as fontes.");
        scroll.post(() -> scroll.scrollTo(0, 0));
    }
    private void setupPage() {
        heading("ANTES DA PRIMEIRA FALA", "Prepare seu olhar\nsobre o debate.",
            "Escolha como acompanhar e informe quem participa. Você pode começar sem cadastrar ninguém.");
        connectionCard();
        LinearLayout source = card(); source.addView(text("Como acompanhar?", 18, INK, true)); gap(source, 14);
        Button cameraMode = button("Câmera + microfone da TV", video && !youtubeMode, () -> { video = true; youtubeMode = false; showPage(0); });
        Button audioMode = button("Somente áudio da TV", !video && !youtubeMode, () -> { video = false; youtubeMode = false; showPage(0); });
        fullButton(source, button("Vídeo público do YouTube", youtubeMode, () -> { youtubeMode = true; showPage(0); })); gap(source, 10);
        fullButton(source, cameraMode); gap(source, 10); fullButton(source, audioMode);
        gap(source, 14); paragraph(source, "YouTube: analisamos o trecho do vídeo pelo link. Outros streamings e a captura da tela de outros aplicativos ainda não estão disponíveis.");
        LinearLayout videoInfo = card(); videoInfo.addView(text("Contexto do debate", 18, INK, true)); gap(videoInfo, 16);
        EditText videoLink = field(videoInfo, "Link público do YouTube", "https://www.youtube.com/watch?v=…", 300); videoLink.setText(youtubeUrl);
        videoLink.addTextChangedListener(watch(value -> youtubeUrl = value)); gap(videoInfo, 16);
        EditText timestamp = field(videoInfo, "Começar em · minutos:segundos", "10:18", 12); timestamp.setText(clock(youtubeStart));
        timestamp.addTextChangedListener(watch(value -> { try { youtubeStart = parseTime(value); } catch (Exception ignored) {} })); gap(videoInfo, 16);
        EditText debateDate = field(videoInfo, "Data do debate · opcional", "Ex.: 28/08/2022", 32); debateDate.setText(analysis.debateDate);
        debateDate.addTextChangedListener(watch(value -> analysis.debateDate = value));
        LinearLayout people = card(); people.addView(text("Quem participa?", 18, INK, true)); gap(people, 8);
        paragraph(people, "Nome e contexto ajudam a organizar os participantes."); gap(people, 20);
        EditText name = field(people, "Nome do participante", "Ex.: nome do candidato", 80); gap(people, 16);
        EditText context = field(people, "Cargo, partido ou contexto · opcional", "Ex.: candidata à prefeitura", 160); gap(people, 16);
        fullButton(people, button("+ Adicionar participante", false, () -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty()) { name.setError("Informe o nome"); name.requestFocus(); return; }
            try {
                JSONObject person = new JSONObject(); person.put("name", value); person.put("context", context.getText().toString().trim());
                roster.put(person); persistRoster(); name.setText(""); context.setText(""); renderRoster();
                Toast.makeText(this, "Participante adicionado", Toast.LENGTH_SHORT).show();
            } catch (Exception error) { Toast.makeText(this, "Não foi possível salvar.", Toast.LENGTH_SHORT).show(); }
        }));
        participants = column(); people.addView(participants); renderRoster(); gap(people, 16);
        paragraph(people, "Os nomes ajudam a contextualizar o debate. A associação automática de voz e rosto ainda não está disponível; falas sem identificação confirmada permanecem sem autor.");
        gap(body, 22); fullButton(body, button("Continuar para acompanhamento →", true, () -> {
            ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(shell.getWindowToken(), 0);
            showPage(1);
        }));
    }
    private void persistRoster() { getPreferences(0).edit().putString("roster", roster.toString()).apply(); }
    private void renderRoster() {
        participants.removeAllViews();
        for (int i = 0; i < roster.length(); i++) {
            final int index = i; JSONObject person = roster.optJSONObject(i); if (person == null) continue;
            gap(participants, 18); participants.addView(divider()); gap(participants, 14);
            LinearLayout line = row();
            TextView initial = badge(person.optString("name").substring(0, 1).toUpperCase(java.util.Locale.ROOT), SOFT, GREEN);
            line.addView(initial); LinearLayout info = column(); info.setPadding(dp(12), 0, 0, 0);
            info.addView(text(person.optString("name"), 15, INK, true));
            String context = person.optString("context");
            if (!context.isEmpty()) { gap(info, 4); paragraph(info, context); }
            line.addView(info, new LinearLayout.LayoutParams(0, -2, 1)); participants.addView(line); gap(participants, 10);
            Button remove = button("Remover " + person.optString("name"), false, () -> {
                roster.remove(index); persistRoster(); renderRoster();
            }); remove.setTextSize(12); fullButton(participants, remove);
        }
    }
    private void livePage() {
        analysis.participants = roster; analysis.source = youtubeMode ? youtubeUrl : "Captação da TV";
        if (youtubeMode) { youtubePage(); return; }
        heading("ACOMPANHAR A TV", "O debate,\ncom contexto.", "Apoie o celular e inicie a captação. O indicador de áudio vai orientar os ajustes necessários.");
        gap(body, 18); sessionBadge = badge("CAPTAÇÃO PARADA", SOFT, GREEN); body.addView(sessionBadge);
        FrameLayout camera = new FrameLayout(this); camera.setBackground(shape(INK, 18, 0)); camera.setClipToOutline(true);
        LinearLayout.LayoutParams cameraSize = new LinearLayout.LayoutParams(-1, dp(215)); cameraSize.topMargin = dp(18); body.addView(camera, cameraSize);
        cameraPlaceholder = text(video ? "Câmera da TV\n\nA imagem aparece ao iniciar." : "Somente áudio\n\nA câmera permanece desligada.", 15, Color.WHITE, false);
        cameraPlaceholder.setGravity(Gravity.CENTER); cameraPlaceholder.setPadding(dp(20), dp(20), dp(20), dp(20));
        camera.addView(cameraPlaceholder, new FrameLayout.LayoutParams(-1, -1));
        preview = new PreviewView(this); preview.setVisibility(View.GONE); camera.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout signal = card(); signal.addView(text("Sinal de áudio", 17, INK, true)); gap(signal, 12);
        status = text("Pronto para começar. Toque em iniciar e autorize o microfone" + (video ? " e a câmera." : "."), 14, MUTED, false);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        status.setPadding(dp(14), dp(14), dp(14), dp(14)); status.setBackground(shape(SOFT, 12, 0)); signal.addView(status);
        gap(signal, 12); paragraph(signal, "Volume presente não garante fala compreensível. Esta versão mede nível baixo e distorção.");
        addAnalysisPanel();
        gap(body, 20); startButton = button(video ? "Iniciar câmera + microfone" : "Iniciar microfone", true, () -> requestStart(video)); fullButton(body, startButton);
        gap(body, 10); stopButton = button("Parar captação", false, this::stopCapture); fullButton(body, stopButton); stopButton.setVisibility(View.GONE);
        gap(body, 12); fullButton(body, button("Alterar modo de acompanhamento", false, () -> showPage(0)));
        LinearLayout people = card(); people.addView(text("Participantes cadastrados", 17, INK, true)); gap(people, 10);
        if (roster.length() == 0) paragraph(people, "Nenhum participante cadastrado. Você pode adicionar nomes na etapa Preparar.");
        else for (int i = 0; i < roster.length(); i++) { JSONObject p = roster.optJSONObject(i); if (p != null) { paragraph(people, "• " + p.optString("name")); gap(people, 8); } }
        gap(people, 8); paragraph(people, "A identificação automática de quem está falando ainda não está disponível.");
    }
    private void checksPage() {
        heading("DO QUE FOI DITO AO QUE SABEMOS", "Evidência antes\ndo veredito.", "Cada afirmação deve vir com contexto e fontes que você possa conferir.");
        analysisStatus = text(analysis.stage, 14, GREEN, true); gap(body, 18); body.addView(analysisStatus);
        checksContainer = column(); body.addView(checksContainer); renderChecks();
        LinearLayout manual = card(); manual.addView(text("Conferir uma fala por texto", 18, INK, true)); gap(manual, 12);
        paragraph(manual, "Cole a fala completa se quiser revisar uma transcrição antes de pesquisar."); gap(manual, 16);
        EditText statement = field(manual, "Fala para analisar", "Cole o trecho aqui", 12000); statement.setSingleLine(false); statement.setMinLines(3); gap(manual, 16);
        fullButton(manual, button("Pesquisar esta fala", true, () -> { if (statement.getText().toString().trim().length() < 15) { statement.setError("Informe uma fala completa"); return; } analysis.participants=roster; analysis.submit(null,null,statement.getText().toString().trim()); }));
    }
    private void requestStart(boolean withVideo) {
        if (!analysis.settings.ready()) { Toast.makeText(this,"Ative a conexão na etapa Preparar.",Toast.LENGTH_LONG).show(); return; }
        new android.app.AlertDialog.Builder(this).setTitle("Iniciar análise com IA?")
            .setMessage("A cada 20 segundos, enviaremos o áudio e, no modo câmera, uma imagem da TV ao serviço Fullfact-IA e ao Gemini para transcrição e pesquisa. A análise pode levar mais tempo que a fala. A captação para ao sair desta tela. A mídia não é guardada no projeto Fullfact-IA; o provedor aplica suas próprias condições de uso e retenção.")
            .setPositiveButton("Iniciar",(dialog,which)->startAuthorized(withVideo)).setNegativeButton("Agora não",null).show();
    }
    private void startAuthorized(boolean withVideo) {
        stopCapture(); video = withVideo;
        boolean mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean camera = !video || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (mic && camera) startCapture();
        else requestPermissions(video ? new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}
            : new String[]{Manifest.permission.RECORD_AUDIO}, 10);
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request != 10 || page != 1) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            (video && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            setStatus("Permissão não concedida. Tente iniciar novamente ou ajuste as permissões nas configurações do celular."); return;
        }
        startCapture();
    }
    private void setStatus(String message) {
        if (status == null || message.contentEquals(status.getText())) return;
        status.setText(message);
        boolean warning = message.contains("baixo") || message.contains("distorcido") || message.contains("possível") || message.contains("Permissão") || message.contains("indisponível");
        status.setBackground(shape(warning ? WARNING : SOFT, 12, 0)); status.setTextColor(INK);
    }
    @android.annotation.SuppressLint("MissingPermission")
    private void startCapture() {
        if (page != 1) return;
        active = true; final int session = ++generation;
        sessionBadge.setText("CAPTAÇÃO ATIVA"); startButton.setVisibility(View.GONE); stopButton.setVisibility(View.VISIBLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setStatus("Iniciando microfone…");
        audio.start(message -> runOnUiThread(() -> { if (active && session == generation) setStatus(message); }),
            wav -> runOnUiThread(() -> { if (active && session == generation) { analysis.participants=roster; analysis.submit(wav,video?cameraFrame():null,null); } }));
        if (!video) return;
        preview.setVisibility(View.VISIBLE); cameraPlaceholder.setVisibility(View.GONE);
        com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            if (!active || session != generation) return;
            try {
                cameraProvider = future.get(); Preview cameraPreview = new Preview.Builder().build();
                cameraPreview.setSurfaceProvider(preview.getSurfaceProvider());
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, cameraPreview);
            } catch (Exception error) { stopCapture(); setStatus("Câmera indisponível. Tente o modo somente áudio na etapa Preparar."); }
        }, getMainExecutor());
    }
    private void stopCapture() {
        active = false; generation++; audio.stop();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (cameraProvider != null) cameraProvider.unbindAll();
        if (preview != null) preview.setVisibility(View.GONE);
        if (cameraPlaceholder != null) cameraPlaceholder.setVisibility(View.VISIBLE);
        if (sessionBadge != null) sessionBadge.setText("CAPTAÇÃO PARADA");
        if (startButton != null) startButton.setVisibility(View.VISIBLE);
        if (stopButton != null) stopButton.setVisibility(View.GONE);
        setStatus("Captação parada. Toque em iniciar para continuar.");
    }
    @Override protected void onResume() { super.onResume(); if(player!=null)player.onResume(); if (analysis != null) { analysis.observe(analysisObserver); refreshAnalysis(); } }
    @Override protected void onDestroy() { uiHandler.removeCallbacks(nextYoutubeClip); if(player!=null)player.destroy(); super.onDestroy(); }
    @Override public void onSaveInstanceState(Bundle state) {
        state.putInt("page", page); state.putBoolean("video", video); super.onSaveInstanceState(state);
    }
    @Override public void onPause() { youtubeAuto=false; uiHandler.removeCallbacks(nextYoutubeClip); if(player!=null)player.onPause(); stopCapture(); super.onPause(); }
    private android.text.TextWatcher watch(java.util.function.Consumer<String> listener) {
        return new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { listener.accept(s.toString().trim()); }
            public void afterTextChanged(android.text.Editable s) {}
        };
    }
    private String clock(int seconds) { return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60); }
    private int parseTime(String value) {
        String[] parts=value.trim().split(":"); int seconds;
        if(parts.length==2)seconds=Integer.parseInt(parts[0])*60+Integer.parseInt(parts[1]);
        else if(parts.length==3)seconds=Integer.parseInt(parts[0])*3600+Integer.parseInt(parts[1])*60+Integer.parseInt(parts[2]);
        else seconds=Integer.parseInt(value.trim());
        if(seconds<0||seconds>28800)throw new IllegalArgumentException();return seconds;
    }
    private String canonicalYoutube(String raw) throws Exception {
        android.net.Uri uri=android.net.Uri.parse(raw);String host=uri.getHost(),id=null;
        if("youtu.be".equals(host))id=uri.getLastPathSegment();
        if("www.youtube.com".equals(host)||"youtube.com".equals(host)||"m.youtube.com".equals(host))id=uri.getQueryParameter("v");
        if(id==null||!id.matches("[A-Za-z0-9_-]{11}"))throw new Exception("Informe um link público válido do YouTube.");
        return "https://www.youtube.com/watch?v="+id;
    }
    private void connectionCard() {
        LinearLayout connection=card(); connection.addView(text("Conexão com a análise",18,INK,true)); gap(connection,12);
        TextView state=text(analysis.settings.ready()?"Conexão ativada neste celular":"Ative uma vez para começar",14,GREEN,true);connection.addView(state);gap(connection,12);
        paragraph(connection,"Use seu código de ativação do Fullfact-IA. A chave do Gemini fica protegida no servidor.");gap(connection,16);
        EditText code=field(connection,"Código de ativação","Cole seu código pessoal",32);
        code.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);gap(connection,16);
        fullButton(connection,button("Ativar e verificar conexão",true,()->{
            String token=code.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
            if(!token.matches("[a-f0-9]{32}")){code.setError("Cole o código de ativação com 32 caracteres");return;}
            state.setText("Verificando conexão…");
            analysis.testConnection(BuildConfig.BACKEND_URL,token,message->{code.setText("");state.setText(message);});
        }));
        if(analysis.settings.ready()){gap(connection,10);fullButton(connection,button("Remover ativação deste celular",false,()->{analysis.settings.clear();showPage(0);}));}
    }
    private void addAnalysisPanel() {
        LinearLayout panel=card();panel.addView(text("Análise do trecho",18,INK,true));gap(panel,12);
        analysisStatus=text(analysis.stage,14,GREEN,true);analysisStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);panel.addView(analysisStatus);gap(panel,16);
        panel.addView(text("Transcrição mais recente",12,MUTED,true));gap(panel,8);
        transcriptText=text(analysis.transcript.isEmpty()?"A primeira transcrição aparece aqui após um trecho de fala.":analysis.transcript,15,INK,false);transcriptText.setTextIsSelectable(true);panel.addView(transcriptText);gap(panel,12);
        paragraph(panel,"Participante não identificado quando a autoria não estiver confirmada. Resultados são automatizados e devem ser conferidos nas fontes.");gap(panel,16);
        fullButton(panel,button("Ver checagens e fontes",false,()->showPage(2)));
    }
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    private void youtubePage() {
        heading("DEBATE NO YOUTUBE","O vídeo,\ncom contexto.","Analise o debate em trechos de 25 segundos pelo link público. Você escolhe o ponto de início; a análise não acompanha automaticamente os comandos do player.");
        LinearLayout interval=card();interval.addView(text("Trecho a analisar",18,INK,true));gap(interval,14);
        EditText time=field(interval,"Começar em · minutos:segundos","10:18",12);time.setText(clock(youtubeStart));
        time.addTextChangedListener(watch(value->{try{youtubeStart=parseTime(value);}catch(Exception ignored){}}));gap(interval,12);
        paragraph(interval,"O áudio e as imagens do trecho serão lidos pelo Gemini a partir do link. Não há captação do microfone neste modo.");
        gap(body,18); FrameLayout videoFrame=new FrameLayout(this); videoFrame.setBackground(shape(INK,18,0)); videoFrame.setClipToOutline(true);
        LinearLayout intro=column();intro.setGravity(Gravity.CENTER);intro.setPadding(dp(18),dp(18),dp(18),dp(18));
        TextView previewTitle=text("Seu debate no YouTube",19,Color.WHITE,true);previewTitle.setGravity(Gravity.CENTER);intro.addView(previewTitle);gap(intro,18);
        intro.addView(button("Carregar vídeo para assistir",false,()->{try{loadYoutubePlayer(videoFrame);}catch(Exception error){Toast.makeText(this,"Não foi possível carregar. Use Abrir vídeo no YouTube.",Toast.LENGTH_LONG).show();}}));
        videoFrame.addView(intro,new FrameLayout.LayoutParams(-1,-1));body.addView(videoFrame,new LinearLayout.LayoutParams(-1,dp(230)));
        gap(body,16);fullButton(body,button("Analisar 25 segundos deste ponto",true,()->{
            try{youtubeStart=parseTime(time.getText().toString());}catch(Exception error){time.setError("Use minutos:segundos, como 10:18");return;}
            analyzeYoutube();time.setText(clock(youtubeStart));
        }));
        gap(body,10);fullButton(body,button("Abrir vídeo no YouTube",false,()->{try{startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(canonicalYoutube(youtubeUrl)+"&t="+youtubeStart+"s")));}catch(Exception error){Toast.makeText(this,"Não foi possível abrir o vídeo.",Toast.LENGTH_SHORT).show();}}));
        gap(body,14);Switch automatic=new Switch(this);automatic.setText("Analisar os próximos trechos em sequência");automatic.setTextSize(14);automatic.setTextColor(INK);automatic.setMinHeight(dp(48));body.addView(automatic);
        automatic.setOnCheckedChangeListener((view,checked)->{
            youtubeAuto=checked;uiHandler.removeCallbacks(nextYoutubeClip);
            if(checked){if(!analysis.settings.ready()){automatic.setChecked(false);Toast.makeText(this,"Ative a conexão na etapa Preparar.",Toast.LENGTH_LONG).show();return;}uiHandler.post(nextYoutubeClip);}
        });
        addAnalysisPanel();
    }
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    private void loadYoutubePlayer(FrameLayout frame) {
        player=new android.webkit.WebView(this);player.setBackgroundColor(INK);
        player.getSettings().setJavaScriptEnabled(true);player.getSettings().setDomStorageEnabled(true);
        player.getSettings().setAllowFileAccess(false);player.getSettings().setAllowContentAccess(false);
        player.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        player.setWebChromeClient(new android.webkit.WebChromeClient());
        player.setWebViewClient(new android.webkit.WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(android.webkit.WebView view,android.webkit.WebResourceRequest request){
                String host=request.getUrl().getHost();return host==null||!(host.equals("youtube.com")||host.endsWith(".youtube.com")||host.equals("youtu.be"));
            }
        });
        frame.removeAllViews(); frame.addView(player,new FrameLayout.LayoutParams(-1,-1));
        try{String url=canonicalYoutube(youtubeUrl);player.loadUrl(url+"&t="+youtubeStart+"s");}catch(Exception error){player.loadData("<p style='color:white'>Informe um link público na etapa Preparar.</p>","text/html","UTF-8");}
    }
    private void analyzeYoutube() {
        if(analysis.busy.get()){Toast.makeText(this,"Aguarde a análise do trecho anterior.",Toast.LENGTH_SHORT).show();return;}
        if(!analysis.settings.ready()){Toast.makeText(this,"Ative a conexão na etapa Preparar.",Toast.LENGTH_LONG).show();return;}
        try{
            String url=canonicalYoutube(youtubeUrl);analysis.participants=roster;analysis.source=url;
            analysis.submitVideo(url,youtubeStart);youtubeStart+=25;
        }catch(Exception error){Toast.makeText(this,error.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private byte[] cameraFrame() {
        if(preview==null||preview.getVisibility()!=View.VISIBLE)return null;
        android.graphics.Bitmap raw=preview.getBitmap();if(raw==null)return null;
        int width=Math.min(640,raw.getWidth());int height=Math.max(1,raw.getHeight()*width/raw.getWidth());
        android.graphics.Bitmap small=android.graphics.Bitmap.createScaledBitmap(raw,width,height,true);
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();small.compress(android.graphics.Bitmap.CompressFormat.JPEG,65,bytes);
        if(small!=raw)small.recycle();raw.recycle();return bytes.toByteArray();
    }
    private void refreshAnalysis() {
        if(analysisStatus!=null)analysisStatus.setText(analysis.stage);
        if(transcriptText!=null)transcriptText.setText(analysis.transcript.isEmpty()?"A primeira transcrição aparece aqui após um trecho de fala.":analysis.transcript);
        if(checksContainer!=null&&renderedChecks!=analysis.checks.length())renderChecks();
    }
    private void renderChecks() {
        checksContainer.removeAllViews();renderedChecks=analysis.checks.length();
        if(renderedChecks==0){
            TextView empty=text("Nenhuma checagem disponível",19,INK,true);gap(checksContainer,22);checksContainer.addView(empty);gap(checksContainer,12);
            paragraph(checksContainer,"Inicie a análise de uma fala ou de um trecho do YouTube. Opiniões, promessas e frases incompletas podem não gerar checagens.");return;
        }
        for(int i=analysis.checks.length()-1;i>=0;i--){
            JSONObject result=analysis.checks.optJSONObject(i);if(result==null)continue;
            LinearLayout box=column();box.setPadding(dp(18),dp(20),dp(18),dp(20));box.setBackground(shape(Color.WHITE,18,LINE));
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(18);checksContainer.addView(box,params);
            String verdict=result.optString("verdict","Evidência insuficiente");
            int color=verdict.equals("Verdadeiro")?SOFT:WARNING;box.addView(badge(verdict.toUpperCase(java.util.Locale.ROOT),color,INK));gap(box,16);
            box.addView(text("“"+result.optString("claim")+"”",19,INK,true));gap(box,14);
            TextView explanation=text(result.optString("explanation"),14,INK,false);explanation.setTextIsSelectable(true);box.addView(explanation);gap(box,16);
            box.addView(text("Fontes da pesquisa",12,MUTED,true));gap(box,10);
            JSONArray sources=result.optJSONArray("sources");
            if(sources!=null)for(int j=0;j<sources.length();j++){
                JSONObject source=sources.optJSONObject(j);if(source==null)continue;
                String title=source.optString("title"),url=source.optString("url");
                fullButton(box,button(title+" ↗",false,()->{try{if(url.startsWith("https://"))startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(Exception error){Toast.makeText(this,"Não foi possível abrir a fonte.",Toast.LENGTH_SHORT).show();}}));gap(box,8);
            }
            String suggestions=result.optString("searchSuggestions");
            if(!suggestions.isEmpty()){
                gap(box,12);box.addView(text("Pesquisa Google",12,MUTED,true));gap(box,8);
                android.webkit.WebView searchView=new android.webkit.WebView(this);
                searchView.getSettings().setJavaScriptEnabled(false);searchView.getSettings().setAllowFileAccess(false);searchView.getSettings().setAllowContentAccess(false);searchView.getSettings().setBlockNetworkLoads(true);
                searchView.setWebViewClient(new android.webkit.WebViewClient(){@Override public boolean shouldOverrideUrlLoading(android.webkit.WebView view,android.webkit.WebResourceRequest request){String link=request.getUrl().toString();try{if(link.startsWith("https://"))startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,request.getUrl()));}catch(Exception ignored){}return true;}});
                box.addView(searchView,new LinearLayout.LayoutParams(-1,dp(160)));searchView.loadDataWithBaseURL("https://www.google.com",suggestions,"text/html","UTF-8",null);
            }
            JSONObject crossed=result.optJSONObject("crossCheck");if(crossed!=null){gap(box,8);paragraph(box,"Origens distintas na pesquisa: "+crossed.optInt("independentOrigins"));}
            gap(box,8);paragraph(box,"Checagem automatizada · "+result.optString("checkedAt").replace("T"," ").replace("Z"," UTC"));
            if(result.has("startSeconds")){gap(box,8);paragraph(box,"Trecho do vídeo a partir de "+clock(result.optInt("startSeconds")));}
        }
    }

}

