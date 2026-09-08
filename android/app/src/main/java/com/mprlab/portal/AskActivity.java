package com.mprlab.portal;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;

public final class AskActivity extends PortalActivity {
    private static final int RECORD_PERMISSION = 401;
    private static final String DRAFT_STATE = "ask_question_draft";
    private enum State { LOADING, IDLE, RECORDING, SUBMITTING, ANSWER, ERROR, CANCELLED }
    private State interaction = State.LOADING;
    private String profileID, profileName;
    private EditText question;
    private TextView answer, speechStatus;
    private ProgressBar progress;
    private Button askButton, recordButton, cancelButton, retryButton;
    private AskMicrophone microphone;
    private AskSpeech speech;
    private AskClient.Settings settings;
    private AskClient.Call activeCall;
    private long generation;
    private boolean resumed;
    private boolean permissionPending;
    private SharedPreferences profiles;
    private final SharedPreferences.OnSharedPreferenceChangeListener profileChanged = (preferences, key) -> {
        if (ProfileStore.PREFS_ACTIVE.equals(key) && !selectedProfileMatches()) {
            cancelWork(); finish();
        }
    };

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        profileID = getIntent().getStringExtra("profile_id");
        profileName = getIntent().getStringExtra("profile_name");
        profiles = getSharedPreferences(ProfileStore.PREFS_NAME, MODE_PRIVATE);
        getWindow().setStatusBarColor(PortalStyle.INK);
        getWindow().setNavigationBarColor(PortalStyle.INK);
        microphone = new AskMicrophone(getCacheDir());
        render();
        if (saved != null) question.setText(saved.getString(DRAFT_STATE, ""));
        speech = new AskSpeech(this, () -> runOnUiThread(() -> {
            if (resumed && interaction == State.ANSWER) speechUnavailable();
        }));
    }

    private void render() {
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = PortalStyle.scroll(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24),dp(8),dp(24),dp(16)); scroll.addView(root);
        LinearLayout header = new LinearLayout(this);
        header.addView(text("Ask anything", PortalStyle.TextRole.TITLE));
        TextView intro = text(profileName == null ? "Choose a child on Home before asking a question." : "What are you curious about, " + profileName + "?", PortalStyle.TextRole.BODY);
        intro.setPadding(0,dp(12),0,dp(12)); root.addView(intro);
        question = textInput(); question.setHint("Type a question here…"); question.setContentDescription("Your question");
        question.setTextColor(PortalStyle.INK); question.setHintTextColor(PortalStyle.SECONDARY);
        question.setMinHeight(dp(112)); question.setGravity(Gravity.TOP);
        question.setBackground(PortalStyle.surface(this,PortalStyle.WHITE,22));
        question.setPadding(dp(18),dp(16),dp(18),dp(16));
        root.addView(question,new LinearLayout.LayoutParams(-1,-2));
        progress = new ProgressBar(this); root.addView(progress,new LinearLayout.LayoutParams(-1,dp(46)));
        answer = text("Connecting to Ask…",PortalStyle.TextRole.BODY);
        answer.setBackground(PortalStyle.surface(this,PortalStyle.YELLOW,22));
        answer.setPadding(dp(24),dp(22),dp(24),dp(22)); answer.setMinHeight(dp(140));
        answer.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(answer,new LinearLayout.LayoutParams(-1,-2));
        speechStatus = text("",PortalStyle.TextRole.BODY); root.addView(speechStatus);
        Button stop = button("Stop speaking",PortalStyle.YELLOW); stop.setOnClickListener(v -> { speech.stop(); speechStatus.setText("Spoken answer stopped."); });
        root.addView(stop,new LinearLayout.LayoutParams(-1,-2));
        retryButton = button("Retry connection",PortalStyle.YELLOW); retryButton.setOnClickListener(v -> loadSettings());
        root.addView(retryButton,new LinearLayout.LayoutParams(-1,-2));
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions = new LinearLayout(this); actions.setPadding(dp(24),dp(10),dp(24),dp(12));
        askButton = button("Type & ask",PortalStyle.BLUE); askButton.setOnClickListener(v -> askTyped());
        recordButton = button("Talk to ask",PortalStyle.PURPLE); recordButton.setOnClickListener(v -> toggleRecording());
        cancelButton = button("Cancel",PortalStyle.CORAL); cancelButton.setOnClickListener(v -> cancelWork());
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0,-2,1); first.rightMargin=dp(12);
        actions.addView(askButton,first); actions.addView(recordButton,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams cancel = new LinearLayout.LayoutParams(-2,-2); cancel.leftMargin=dp(12); actions.addView(cancelButton,cancel);
        page.addView(actions,new LinearLayout.LayoutParams(-1,-2));
        setContentView(PortalToolbar.screen(this,header,page,PortalStyle.PAPER)); updateControls();
    }

    @Override protected void onResume() {
        super.onResume(); resumed = true;
        profiles.registerOnSharedPreferenceChangeListener(profileChanged);
        if (!selectedProfileMatches()) { finish(); return; }
        updateControls();
        try { microphone.removeAbandoned(); }
        catch (IOException error) { setState(State.ERROR,error.getMessage()); return; }
        if (permissionPending) return;
        if (settings == null && activeCall == null) loadSettings();
    }
    private void loadSettings() {
        if (!resumed || activeCall != null) return;
        final long identity = ++generation; final AskClient.Call call = new AskClient.Call(); activeCall = call;
        setState(State.LOADING,"Connecting to Ask…");
        new Thread(() -> {
            try {
                AskClient.Settings loaded = call.settings();
                runOnUiThread(() -> {
                    if (!current(identity)) return;
                    activeCall = null; settings = loaded;
                    setState(State.IDLE,"Type a question or tap Talk to ask. Your answer will appear here.");
                });
            } catch (Exception error) { fail(identity,"Ask is unavailable right now. Tap Retry connection."); }
        },"Ask-settings").start();
    }
    private boolean canSubmit() {
        return resumed && !permissionPending && settings != null && interaction != State.SUBMITTING && interaction != State.RECORDING && interaction != State.LOADING;
    }
    private void askTyped() {
        if (!canSubmit()) return;
        String value = question.getText().toString().trim();
        if (value.isEmpty() || value.codePointCount(0,value.length()) > settings.questionLimit) { question.setError("Enter a shorter question"); return; }
        submit(value,null);
    }
    private void submit(String value, File recording) {
        speech.stop(); speechStatus.setText("");
        final long identity = ++generation; final AskClient.Call call = new AskClient.Call(); activeCall = call;
        final String childID = profileID, childName = profileName;
        setState(State.SUBMITTING,recording == null ? "Thinking…" : "Listening and thinking…");
        new Thread(() -> {
            try {
                String result = recording == null ? call.typed(childID,childName,value) : call.audio(childID,childName,recording);
                runOnUiThread(() -> {
                    if (!current(identity)) return;
                    activeCall = null;
                    if (!clearRecording()) return;
                    setState(State.ANSWER,result);
                    if (!speech.speak(result)) speechUnavailable();
                });
            } catch (Exception error) { fail(identity, error instanceof IOException ? error.getMessage() : "Ask returned an invalid answer."); }
        },"Ask-question").start();
    }
    private void toggleRecording() {
        if (interaction == State.RECORDING) { stopAndSendRecording(); return; }
        if (!canSubmit()) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionPending = true; updateControls();
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},RECORD_PERMISSION); return;
        }
        startRecording();
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(code,permissions,results);
        if (code != RECORD_PERMISSION || !permissionPending) return;
        permissionPending = false;
        // Require an explicit tap after a system dialog; never start a microphone in the background.
        setState(State.IDLE,results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED
                ? "Microphone ready. Tap Talk to ask." : "Microphone access was denied. You can type your question.");
    }
    private void startRecording() {
        speech.stop(); speechStatus.setText("");
        final long identity = ++generation;
        try {
            microphone.start(settings,() -> runOnUiThread(() -> { if (current(identity) && interaction == State.RECORDING) stopAndSendRecording(); }));
            setState(State.RECORDING,"I’m listening… Tap Send my question when you’re ready.");
        } catch (IOException error) { setState(State.ERROR,error.getMessage()); }
    }
    private void stopAndSendRecording() {
        if (!resumed || interaction != State.RECORDING) return;
        try { File recording = microphone.finish(); submit(null,recording); }
        catch (IOException error) { setState(State.ERROR,error.getMessage()); }
    }
    private boolean current(long identity) {
        return resumed && identity == generation && !isFinishing() && selectedProfileMatches();
    }
    private boolean selectedProfileMatches() {
        return profileID != null && profileName != null && !profileName.trim().isEmpty()
                && profileID.equals(profiles.getString(ProfileStore.PREFS_ACTIVE,null));
    }
    private void fail(long identity,String message) {
        runOnUiThread(() -> {
            if (!current(identity)) return;
            activeCall = null;
            if (clearRecording()) setState(State.ERROR,message);
        });
    }
    private boolean clearRecording() {
        try { microphone.discard(); return true; }
        catch (IOException error) { setState(State.ERROR,error.getMessage()); return false; }
    }
    private void cancelWork() {
        ++generation;
        if (activeCall != null) { activeCall.cancel(); activeCall = null; }
        speech.stop(); speechStatus.setText("");
        boolean pending = interaction == State.SUBMITTING;
        if (clearRecording() && (pending || interaction == State.RECORDING || interaction == State.LOADING)) {
            setState(State.CANCELLED,pending ? "Stopped waiting. Your question may still be processing." : "Question cancelled.");
        }
    }
    private void speechUnavailable() { speechStatus.setText("Speech is unavailable. You can read your answer."); }
    private void setState(State next,String message) { interaction = next; answer.setText(message); updateControls(); }
    private void updateControls() {
        boolean busy = interaction == State.LOADING || interaction == State.SUBMITTING;
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        askButton.setEnabled(canSubmit());
        recordButton.setEnabled(canSubmit() || (resumed && interaction == State.RECORDING));
        recordButton.setText(interaction == State.RECORDING ? "Send my question" : "Talk to ask");
        recordButton.setBackground(PortalStyle.surface(this,interaction == State.RECORDING ? PortalStyle.CORAL : PortalStyle.PURPLE,20));
        question.setEnabled(!busy && interaction != State.RECORDING);
        cancelButton.setVisibility(busy || interaction == State.RECORDING ? View.VISIBLE : View.GONE);
        retryButton.setVisibility(settings == null && !busy ? View.VISIBLE : View.GONE);
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString(DRAFT_STATE,question.getText().toString()); super.onSaveInstanceState(state); }
    @Override protected void onPause() {
        resumed = false; profiles.unregisterOnSharedPreferenceChangeListener(profileChanged);
        cancelWork(); super.onPause();
    }
    @Override protected void beforeHome() { cancelWork(); }
    @Override protected void onScreensaverStarted() { cancelWork(); }
    @Override protected void onDestroy() { cancelWork(); speech.close(); super.onDestroy(); }
    private TextView text(String value,PortalStyle.TextRole role) { TextView view=new TextView(this); view.setText(value); view.setTextColor(PortalStyle.INK); PortalStyle.text(view,role); return view; }
    private Button button(String value,int color) { Button view=new Button(this); view.setText(value); view.setAllCaps(false); PortalStyle.primary(view); view.setBackground(PortalStyle.surface(this,color,20)); view.setPadding(dp(16),dp(8),dp(16),dp(8)); return view; }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
}
