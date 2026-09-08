package com.mprlab.portal;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;

public final class AskActivity extends PortalActivity {
    private static final int RECORD_PERMISSION = 401;
    private static final String DRAFT_STATE = "ask_question_draft";
    private enum State { LOADING, IDLE, RECORDING, TRANSCRIBING, SUBMITTING, ANSWER, ERROR, CANCELLED }
    private enum AfterTranscription { EDIT, SEND }
    private State interaction = State.LOADING;
    private String profileID, profileName;
    private EditText question;
    private TextView answer, speechStatus;
    private ProgressBar progress;
    private ImageButton askButton, recordButton;
    private Button cancelButton, retryButton;
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
        page.setPadding(dp(24),dp(8),dp(24),dp(16));
        ScrollView scroll = PortalStyle.scroll(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);
        LinearLayout header = new LinearLayout(this);
        header.addView(text("Ask anything", PortalStyle.TextRole.TITLE));
        TextView intro = text(profileName == null ? "Choose a child on Home before asking a question." : "What are you curious about, " + profileName + "?", PortalStyle.TextRole.BODY);
        intro.setPadding(0,dp(12),0,dp(12)); page.addView(intro);
        LinearLayout prompt = new LinearLayout(this); prompt.setGravity(Gravity.CENTER_VERTICAL);
        prompt.setBackground(PortalStyle.surface(this,PortalStyle.WHITE,22));
        prompt.setPadding(dp(8),dp(8),dp(16),dp(12));
        question = textInput(); question.setHint("Type a question or tap the microphone. Tap the paper airplane to send."); question.setContentDescription("Your question");
        question.setTextColor(PortalStyle.INK); question.setHintTextColor(PortalStyle.SECONDARY);
        question.setMinHeight(dp(92)); question.setMaxLines(4); question.setGravity(Gravity.TOP);
        question.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        question.setPadding(dp(10),dp(8),dp(10),dp(8));
        prompt.addView(question,new LinearLayout.LayoutParams(0,-2,1));
        recordButton = promptButton("Record question",R.drawable.ic_ask_microphone,PortalStyle.PURPLE);
        recordButton.setOnClickListener(v -> toggleRecording());
        askButton = promptButton("Send question",R.drawable.ic_ask_send,PortalStyle.BLUE);
        askButton.setOnClickListener(v -> { if (interaction == State.RECORDING) stopAndTranscribe(AfterTranscription.SEND); else askTyped(); });
        prompt.addView(recordButton,new LinearLayout.LayoutParams(dp(64),dp(64)));
        LinearLayout.LayoutParams send = new LinearLayout.LayoutParams(dp(64),dp(64)); send.leftMargin=dp(10);
        prompt.addView(askButton,send);
        page.addView(prompt,new LinearLayout.LayoutParams(-1,-2));
        progress = new ProgressBar(this); page.addView(progress,new LinearLayout.LayoutParams(-1,dp(46)));
        cancelButton = button("Cancel",PortalStyle.CORAL); cancelButton.setOnClickListener(v -> cancelWork());
        page.addView(cancelButton,new LinearLayout.LayoutParams(-1,-2));
        answer = text("Connecting to Ask…",PortalStyle.TextRole.BODY);
        answer.setPadding(0,dp(16),0,dp(16));
        answer.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(answer,new LinearLayout.LayoutParams(-1,-2));
        speechStatus = text("",PortalStyle.TextRole.BODY); root.addView(speechStatus);
        retryButton = button("Retry connection",PortalStyle.YELLOW); retryButton.setOnClickListener(v -> loadSettings());
        root.addView(retryButton,new LinearLayout.LayoutParams(-1,-2));
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
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
                    setState(State.IDLE,"");
                });
            } catch (Exception error) { fail(identity,"Ask is unavailable right now. Tap Retry connection."); }
        },"Ask-settings").start();
    }
    private boolean canSubmit() {
        return resumed && !permissionPending && settings != null && interaction != State.SUBMITTING && interaction != State.TRANSCRIBING && interaction != State.RECORDING && interaction != State.LOADING;
    }
    private void askTyped() {
        if (!canSubmit()) return;
        String value = question.getText().toString().trim();
        if (value.isEmpty() || value.codePointCount(0,value.length()) > settings.questionLimit) { question.setError("Enter a shorter question"); return; }
        submit(value);
    }
    private void submit(String value) {
        speech.stop(); speechStatus.setText("");
        final long identity = ++generation; final AskClient.Call call = new AskClient.Call(); activeCall = call;
        final String childID = profileID, childName = profileName;
        setState(State.SUBMITTING,"Thinking…");
        new Thread(() -> {
            try {
                String result = call.typed(childID,childName,value);
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
        if (interaction == State.RECORDING) { stopAndTranscribe(AfterTranscription.EDIT); return; }
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
                ? "Microphone ready. Tap the microphone to record." : "Microphone access was denied. You can type your question.");
    }
    private void startRecording() {
        speech.stop(); speechStatus.setText("");
        final long identity = ++generation;
        try {
            microphone.start(settings,() -> runOnUiThread(() -> { if (current(identity) && interaction == State.RECORDING) stopAndTranscribe(AfterTranscription.EDIT); }));
            setState(State.RECORDING,"I’m listening… Tap the red square to finish, or the paper airplane to send.");
        } catch (IOException error) { setState(State.ERROR,error.getMessage()); }
    }
    private void stopAndTranscribe(AfterTranscription next) {
        if (!resumed || interaction != State.RECORDING) return;
        try { File recording = microphone.finish(); transcribe(recording,next); }
        catch (IOException error) { setState(State.ERROR,error.getMessage()); }
    }
    private void transcribe(File recording, AfterTranscription next) {
        final long identity = ++generation; final AskClient.Call call = new AskClient.Call(); activeCall = call;
        final String childID = profileID, childName = profileName, draft = question.getText().toString().trim();
        setState(State.TRANSCRIBING,"Writing down your words…");
        new Thread(() -> {
            try {
                String transcript = call.transcribe(childID,childName,recording);
                runOnUiThread(() -> {
                    if (!current(identity)) return;
                    activeCall = null;
                    question.setText(draft.isEmpty() ? transcript : draft + " " + transcript);
                    question.setSelection(question.length());
                    if (!clearRecording()) return;
                    setState(State.IDLE,"Your words are ready. Edit your question or tap the paper airplane to send.");
                    if (next == AfterTranscription.SEND) askTyped();
                });
            } catch (Exception error) { fail(identity,error instanceof IOException ? error.getMessage() : "The recording could not be transcribed. Please try again."); }
        },"Ask-transcription").start();
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
        if (clearRecording() && (pending || interaction == State.TRANSCRIBING || interaction == State.RECORDING || interaction == State.LOADING)) {
            setState(State.CANCELLED,pending ? "Stopped waiting. Your question may still be processing." : "Question cancelled.");
        }
    }
    private void speechUnavailable() { speechStatus.setText("Speech is unavailable. You can read your answer."); }
    private void setState(State next,String message) { interaction = next; answer.setText(message); updateControls(); }
    private void updateControls() {
        answer.setVisibility(answer.length() == 0 ? View.GONE : View.VISIBLE);
        boolean busy = interaction == State.LOADING || interaction == State.SUBMITTING || interaction == State.TRANSCRIBING;
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        askButton.setEnabled(canSubmit() || (resumed && interaction == State.RECORDING));
        recordButton.setEnabled(canSubmit() || (resumed && interaction == State.RECORDING));
        recordButton.setContentDescription(interaction == State.RECORDING ? "Stop recording" : "Record question");
        recordButton.setImageResource(interaction == State.RECORDING ? R.drawable.ic_ask_stop : R.drawable.ic_ask_microphone);
        if (interaction == State.RECORDING) recordButton.clearColorFilter(); else recordButton.setColorFilter(PortalStyle.INK);
        recordButton.setSelected(interaction == State.RECORDING);
        recordButton.setBackground(PortalStyle.surface(this,interaction == State.RECORDING ? PortalStyle.WHITE : PortalStyle.PURPLE,16));
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
    private ImageButton promptButton(String label,int icon,int color) {
        ImageButton view = new ImageButton(this) {
            @Override protected void onDraw(android.graphics.Canvas canvas) {
                int saved = canvas.save();
                if (isPressed()) {
                    int offset = dp(PortalStyle.SHADOW_DP);
                    canvas.translate(offset,offset);
                }
                super.onDraw(canvas);
                canvas.restoreToCount(saved);
            }
        };
        view.setContentDescription(label); view.setImageResource(icon);
        view.setColorFilter(PortalStyle.INK); view.setBackground(PortalStyle.surface(this,color,16));
        int padding = dp(16), shadow = dp(PortalStyle.SHADOW_DP);
        // Center the icon on the face, which excludes the bottom-right shadow.
        view.setPadding(padding-shadow/2,padding-shadow/2,padding+shadow/2,padding+shadow/2);
        view.setStateListAnimator(null);
        return view;
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
}
