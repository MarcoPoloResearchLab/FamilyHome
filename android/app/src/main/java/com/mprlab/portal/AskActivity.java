package com.mprlab.portal;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class AskActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int BG = Color.rgb(255, 248, 234);
    private static final int SYSTEM_BAR = Color.rgb(36, 49, 71);
    private static final int SURFACE = Color.WHITE;
    private static final int INK = Color.rgb(36, 49, 71);
    private static final int MUTED = Color.rgb(100, 110, 128);
    private static final int BLUE = Color.rgb(63, 132, 255);
    private static final int PURPLE = Color.rgb(124, 92, 252);
    private static final int CORAL = Color.rgb(231, 76, 91);
    private static final int PALE_YELLOW = Color.rgb(255, 245, 197);
    private static final int RECORD_PERMISSION = 401;
    private String profileID;
    private String profileName;
    private EditText question;
    private TextView answer;
    private ProgressBar progress;
    private Button recordButton;
    private MediaRecorder recorder;
    private File recording;
    private TextToSpeech speech;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        profileID = getIntent().getStringExtra("profile_id");
        profileName = getIntent().getStringExtra("profile_name");
        getWindow().setStatusBarColor(SYSTEM_BAR);
        getWindow().setNavigationBarColor(SYSTEM_BAR);
        speech = new TextToSpeech(this, this);
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(38), dp(32), dp(38), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = text("Ask anything", 34, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(62), 1f));
        Button done = button("Back home", SURFACE, INK);
        done.setOnClickListener(v -> finish());
        header.addView(done, new LinearLayout.LayoutParams(dp(130), dp(62)));
        root.addView(header);

        TextView intro = text("What are you curious about, " + profileName + "?", 21, false);
        intro.setPadding(0, dp(20), 0, dp(16));
        root.addView(intro);
        question = new EditText(this);
        question.setHint("Type a question here…");
        question.setTextColor(INK);
        question.setHintTextColor(MUTED);
        question.setTextSize(21);
        question.setMinHeight(dp(112));
        question.setGravity(Gravity.TOP);
        question.setBackground(rounded(SURFACE, 22));
        question.setElevation(dp(3));
        question.setPadding(dp(18), dp(16), dp(18), dp(16));
        root.addView(question, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(18), 0, dp(18));
        Button ask = button("Type & ask", BLUE);
        ask.setOnClickListener(v -> askTyped());
        recordButton = button("Talk to ask", PURPLE);
        recordButton.setOnClickListener(v -> toggleRecording());
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, dp(68), 1f); ap.rightMargin = dp(10);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, dp(68), 1f); rp.leftMargin = dp(10);
        actions.addView(ask, ap); actions.addView(recordButton, rp);
        root.addView(actions);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(46)));
        answer = text("Your answer will appear here—and I’ll read it aloud!", 23, false);
        answer.setBackground(rounded(PALE_YELLOW, 22));
        answer.setElevation(dp(2));
        answer.setPadding(dp(24), dp(22), dp(24), dp(22));
        answer.setMinHeight(dp(180));
        root.addView(answer, new LinearLayout.LayoutParams(-1, -2));
        setContentView(scroll);
    }

    private void askTyped() {
        String value = question.getText().toString().trim();
        if (value.isEmpty()) { question.setError("Enter a question"); return; }
        setBusy(true, "Thinking…");
        new Thread(() -> {
            try {
                JSONObject input = new JSONObject();
                input.put("profile_id", profileID);
                input.put("name", profileName);
                input.put("question", value);
                HttpURLConnection connection = open("/v1/ask", "application/json; charset=utf-8");
                byte[] bytes = input.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream output = connection.getOutputStream()) { output.write(bytes); }
                showResult(new JSONObject(MainActivity.read(connection)));
            } catch (Exception error) { showFailure(error); }
        }).start();
    }

    private void toggleRecording() {
        if (recorder != null) { stopAndSendRecording(); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_PERMISSION);
            return;
        }
        startRecording();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == RECORD_PERMISSION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startRecording();
    }

    private void startRecording() {
        try {
            recording = new File(getCacheDir(), "portal-question.m4a");
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(96000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(recording.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            recordButton.setText("Send my question");
            recordButton.setBackground(rounded(CORAL, 20));
            answer.setText("I’m listening… Tap “Send my question” when you’re ready.");
        } catch (Exception error) {
            recorder = null;
            Toast.makeText(this, "The microphone could not start.", Toast.LENGTH_LONG).show();
        }
    }

    private void stopAndSendRecording() {
        try { recorder.stop(); } catch (RuntimeException ignored) { }
        recorder.release(); recorder = null;
        recordButton.setText("Talk to ask"); recordButton.setBackground(rounded(PURPLE, 20));
        if (recording == null || !recording.isFile() || recording.length() == 0) { answer.setText("No voice question was recorded."); return; }
        setBusy(true, "Listening and thinking…");
        new Thread(() -> {
            try {
                String boundary = "PortalBoundary" + System.currentTimeMillis();
                HttpURLConnection connection = open("/v1/ask/audio", "multipart/form-data; boundary=" + boundary);
                try (BufferedOutputStream output = new BufferedOutputStream(connection.getOutputStream())) {
                    writePart(output, boundary, "profile_id", profileID);
                    writePart(output, boundary, "name", profileName);
                    output.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"question.m4a\"\r\nContent-Type: audio/m4a\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    try (FileInputStream input = new FileInputStream(recording)) {
                        byte[] buffer = new byte[8192]; int count;
                        while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
                    }
                    output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                }
                showResult(new JSONObject(MainActivity.read(connection)));
            } catch (Exception error) { showFailure(error); }
        }).start();
    }

    private static void writePart(OutputStream output, String boundary, String name, String value) throws Exception {
        String part = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n";
        output.write(part.getBytes(StandardCharsets.UTF_8));
    }

    private HttpURLConnection open(String path, String contentType) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(PortalConfig.serviceURL(path)).openConnection();
        connection.setRequestMethod("POST"); connection.setDoOutput(true);
        connection.setConnectTimeout(6000); connection.setReadTimeout(60000);
        connection.setRequestProperty("Content-Type", contentType);
        PortalConfig.authorize(connection);
        return connection;
    }

    private void showResult(JSONObject result) {
        String value = result.optString("answer", "No answer was returned.");
        runOnUiThread(() -> {
            setBusy(false, value);
            if (speech != null) speech.speak(value, TextToSpeech.QUEUE_FLUSH, null, "portal-answer");
        });
    }

    private void showFailure(Exception error) {
        String message = "Ask is unavailable right now. Please try again soon.";
        try {
            String raw = error.getMessage();
            int start = raw == null ? -1 : raw.indexOf('{');
            if (start >= 0) message = new JSONObject(raw.substring(start)).optString("error", message);
        } catch (Exception ignored) { }
        final String finalMessage = message;
        runOnUiThread(() -> setBusy(false, finalMessage));
    }

    private void setBusy(boolean busy, String message) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        answer.setText(message);
    }

    @Override public void onInit(int status) { if (status == TextToSpeech.SUCCESS) speech.setLanguage(Locale.US); }

    @Override protected void onDestroy() {
        if (recorder != null) { try { recorder.stop(); } catch (Exception ignored) { } recorder.release(); }
        if (speech != null) { speech.stop(); speech.shutdown(); }
        super.onDestroy();
    }

    private TextView text(String value, int size, boolean bold) { TextView view = new TextView(this); view.setText(value); view.setTextColor(INK); view.setTextSize(size); view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL)); return view; }
    private Button button(String value, int color) { return button(value, color, Color.WHITE); }
    private Button button(String value, int color, int textColor) { Button view = new Button(this); view.setText(value); view.setAllCaps(false); view.setTextSize(18); view.setTextColor(textColor); view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); view.setBackground(rounded(color, 20)); view.setPadding(dp(16), dp(8), dp(16), dp(8)); return view; }
    private GradientDrawable rounded(int color, int radius) { GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
