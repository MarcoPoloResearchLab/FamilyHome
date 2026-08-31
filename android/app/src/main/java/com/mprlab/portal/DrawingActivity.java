package com.mprlab.portal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public final class DrawingActivity extends Activity {
    private static final int BG = Color.rgb(255, 248, 234);
    private static final int SYSTEM_BAR = Color.rgb(36, 49, 71);
    private static final int SURFACE = Color.WHITE;
    private static final int INK = Color.rgb(36, 49, 71);
    private static final int BLUE = Color.rgb(63, 132, 255);
    private static final int PURPLE = Color.rgb(124, 92, 252);
    private static final int CORAL = Color.rgb(255, 105, 105);
    private static final int TEAL = Color.rgb(0, 166, 153);
    private static final int PALE_PURPLE = Color.rgb(241, 237, 255);
    private String profileID;
    private String profileName;
    private SharedPreferences preferences;
    private final ArrayList<DrawingDocument> documents = new ArrayList<>();
    private DrawingDocument active;
    private DrawingCanvas drawingCanvas;
    private LinearLayout topBar;
    private LinearLayout palette;
    private Button fullscreenDone;
    private boolean fullscreen;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(SYSTEM_BAR);
        getWindow().setNavigationBarColor(SYSTEM_BAR);
        profileID = getIntent().getStringExtra("profile_id");
        profileName = getIntent().getStringExtra("profile_name");
        preferences = getSharedPreferences(ProfileStore.PREFS_NAME, MODE_PRIVATE);
        loadDocuments();
        render();
    }

    private void render() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);
        drawingCanvas = new DrawingCanvas();
        frame.addView(drawingCanvas, new FrameLayout.LayoutParams(-1, -1));

        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(14), dp(12), dp(14), dp(10));
        topBar.setBackgroundColor(BG);
        addTopButton("My drawings", v -> showLibrary());
        addTopButton("New picture", v -> newDrawing());
        addTopButton("Fill screen", v -> setFullscreen(true));
        addTopButton("Save & share", v -> saveAndShare());
        addTopButton("Done", v -> { persist(); finish(); });
        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(-1, dp(78), Gravity.TOP);
        frame.addView(topBar, topParams);

        palette = new LinearLayout(this);
        palette.setOrientation(LinearLayout.HORIZONTAL);
        palette.setGravity(Gravity.CENTER);
        palette.setPadding(dp(12), dp(8), dp(12), dp(8));
        palette.setBackground(rounded(SURFACE, 24));
        palette.setElevation(dp(4));
        int[] colors = drawingCanvas.colors;
        for (int index = 0; index < colors.length; index++) {
            final int selected = index;
            Button color = button("●", SURFACE, colors[index]);
            color.setTextColor(colors[index]);
            color.setTextSize(30);
            color.setOnClickListener(v -> { drawingCanvas.colorIndex = selected; drawingCanvas.eraser = false; drawingCanvas.invalidate(); });
            palette.addView(color, paletteParams(60));
        }
        addPaletteButton("Pencil", 4f, false);
        addPaletteButton("Marker", 10f, false);
        addPaletteButton("Paint", 22f, false);
        addPaletteButton("Eraser", 30f, true);
        FrameLayout.LayoutParams paletteParams = new FrameLayout.LayoutParams(-1, dp(78), Gravity.BOTTOM);
        paletteParams.leftMargin = dp(14); paletteParams.rightMargin = dp(14); paletteParams.bottomMargin = dp(10);
        frame.addView(palette, paletteParams);

        fullscreenDone = button("Done", CORAL);
        fullscreenDone.setVisibility(View.GONE);
        fullscreenDone.setOnClickListener(v -> { persist(); finish(); });
        FrameLayout.LayoutParams doneParams = new FrameLayout.LayoutParams(dp(110), dp(58), Gravity.TOP | Gravity.RIGHT);
        doneParams.topMargin = dp(18); doneParams.rightMargin = dp(18);
        frame.addView(fullscreenDone, doneParams);
        setContentView(frame);
    }

    private void addTopButton(String label, View.OnClickListener listener) {
        int color = label.equals("Save & share") ? CORAL : label.equals("New picture") ? TEAL : label.equals("My drawings") ? PURPLE : label.equals("Fill screen") ? BLUE : SURFACE;
        Button button = button(label, color, label.equals("Done") ? INK : Color.WHITE);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(56), 1f);
        params.leftMargin = dp(5); params.rightMargin = dp(5);
        topBar.addView(button, params);
    }

    private void addPaletteButton(String label, float width, boolean eraser) {
        Button button = button(label, PALE_PURPLE, INK);
        button.setOnClickListener(v -> { drawingCanvas.strokeWidth = dp(width); drawingCanvas.eraser = eraser; });
        palette.addView(button, paletteParams(105));
    }

    private LinearLayout.LayoutParams paletteParams(int width) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(width), dp(58));
        params.leftMargin = dp(3); params.rightMargin = dp(3);
        return params;
    }

    private void setFullscreen(boolean value) {
        fullscreen = value;
        topBar.setVisibility(value ? View.GONE : View.VISIBLE);
        fullscreenDone.setVisibility(value ? View.VISIBLE : View.GONE);
        getWindow().getDecorView().setSystemUiVisibility(value
                ? View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                : View.SYSTEM_UI_FLAG_VISIBLE);
        drawingCanvas.invalidate();
    }

    @Override public void onBackPressed() {
        if (fullscreen) { setFullscreen(false); return; }
        persist();
        super.onBackPressed();
    }

    private void newDrawing() {
        EditText input = new EditText(this);
        input.setHint("Drawing title");
        input.setSingleLine();
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.DKGRAY);
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert).setTitle("Name your new picture").setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) title = "Untitled drawing";
                    active = new DrawingDocument(UUID.randomUUID().toString(), title);
                    documents.add(active);
                    drawingCanvas.resetView();
                    persist();
                }).setNegativeButton("Cancel", null).show();
    }

    private void showLibrary() {
        String[] titles = new String[documents.size()];
        for (int i = 0; i < documents.size(); i++) titles[i] = documents.get(i).title;
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert).setTitle(profileName + "’s pictures")
                .setItems(titles, (dialog, which) -> { active = documents.get(which); drawingCanvas.resetView(); persist(); })
                .setPositiveButton("New picture", (dialog, which) -> newDrawing()).setNegativeButton("Close", null).show();
    }

    private void saveAndShare() {
        persist();
        try {
            File shareDirectory = new File(getCacheDir(), "shared_drawings");
            if (!shareDirectory.isDirectory() && !shareDirectory.mkdirs()) throw new IllegalStateException("share directory");
            File image = new File(shareDirectory, safeFile(active.title) + ".png");
            Bitmap bitmap = drawingCanvas.renderBitmap(1280, 800);
            try (FileOutputStream output = new FileOutputStream(image)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); }
            bitmap.recycle();
            Toast.makeText(this, "Drawing saved. Preparing sharing…", Toast.LENGTH_SHORT).show();
            uploadAndShare(image);
        } catch (Exception error) {
            Toast.makeText(this, "The drawing could not be saved.", Toast.LENGTH_LONG).show();
        }
    }

    private void uploadAndShare(final File image) {
        new Thread(() -> {
            String sharedURL = "";
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(PortalConfig.SERVICE_BASE_URL + "/v1/drawings").openConnection();
                connection.setRequestMethod("POST"); connection.setDoOutput(true); connection.setConnectTimeout(5000); connection.setReadTimeout(15000);
                connection.setRequestProperty("Content-Type", "image/png");
                connection.setRequestProperty("X-Portal-Profile", profileID);
                connection.setRequestProperty("X-Portal-Title", active.title);
                try (OutputStream output = connection.getOutputStream(); FileInputStream input = new FileInputStream(image)) {
                    byte[] buffer = new byte[8192]; int count;
                    while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
                }
                sharedURL = new JSONObject(MainActivity.read(connection)).optString("url", "");
            } catch (Exception ignored) { }
            final String webURL = sharedURL;
            runOnUiThread(() -> showShareDialog(image, webURL));
        }).start();
    }

    private void showShareDialog(File image, String webURL) {
        String message = webURL.isEmpty()
                ? "The PNG is saved on this Portal."
                : "The PNG is saved, and this private-LAN link is ready to copy:\n\n" + webURL;
        AlertDialog.Builder dialog = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle("Drawing saved")
                .setMessage(message)
                .setPositiveButton("Share image", (value, which) -> shareFile(image, webURL))
                .setNegativeButton("Done", null);
        if (!webURL.isEmpty()) {
            dialog.setNeutralButton("Copy link", (value, which) -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText(active.title, webURL));
                Toast.makeText(this, "Drawing link copied.", Toast.LENGTH_SHORT).show();
            });
        }
        dialog.show();
    }

    private void shareFile(File image, String webURL) {
        Uri uri = ShareProvider.uriFor(this, image);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/png");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_SUBJECT, active.title);
        share.putExtra(Intent.EXTRA_TEXT, webURL.isEmpty() ? profileName + " made “" + active.title + "” on Children's Portal." : webURL);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(share, "Share drawing"));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "The drawing is saved. Add a sharing app to send it from this Portal.", Toast.LENGTH_LONG).show();
        }
    }

    private void loadDocuments() {
        try {
            JSONArray array = new JSONArray(preferences.getString("drawings_" + profileID, "[]"));
            for (int i = 0; i < array.length(); i++) documents.add(DrawingDocument.fromJson(array.getJSONObject(i)));
        } catch (Exception ignored) { documents.clear(); }
        if (documents.isEmpty()) documents.add(new DrawingDocument(UUID.randomUUID().toString(), profileName + "'s drawing"));
        String activeID = preferences.getString("active_drawing_" + profileID, "");
        for (DrawingDocument document : documents) if (document.id.equals(activeID)) active = document;
        if (active == null) active = documents.get(0);
    }

    private void persist() {
        JSONArray array = new JSONArray();
        try { for (DrawingDocument document : documents) array.put(document.toJson()); } catch (Exception ignored) { }
        preferences.edit().putString("drawings_" + profileID, array.toString()).putString("active_drawing_" + profileID, active.id).apply();
    }

    private static String safeFile(String value) { return value.replaceAll("[^a-zA-Z0-9_-]+", "-").replaceAll("^-|-$", ""); }

    private Button button(String label, int color) { return button(label, color, Color.WHITE); }
    private Button button(String label, int color, int textColor) { Button button = new Button(this); button.setText(label); button.setAllCaps(false); button.setTextColor(textColor); button.setTextSize(17); button.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)); button.setBackground(rounded(color, 18)); button.setPadding(dp(10), dp(6), dp(10), dp(6)); return button; }
    private GradientDrawable rounded(int color, int radius) { GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable; }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static final class DrawingDocument {
        final String id; String title; final ArrayList<Stroke> strokes = new ArrayList<>();
        DrawingDocument(String id, String title) { this.id = id; this.title = title; }
        JSONObject toJson() throws Exception { JSONObject json = new JSONObject(); json.put("id", id); json.put("title", title); JSONArray array = new JSONArray(); for (Stroke stroke : strokes) array.put(stroke.toJson()); json.put("strokes", array); return json; }
        static DrawingDocument fromJson(JSONObject json) throws Exception { DrawingDocument document = new DrawingDocument(json.getString("id"), json.optString("title", "Untitled drawing")); JSONArray array = json.optJSONArray("strokes"); if (array != null) for (int i = 0; i < array.length(); i++) document.strokes.add(Stroke.fromJson(array.getJSONObject(i))); return document; }
    }

    private static final class Stroke {
        final Path path = new Path(); final ArrayList<Float> points = new ArrayList<>(); final int color; final float width; final boolean eraser;
        Stroke(int color, float width, boolean eraser) { this.color = color; this.width = width; this.eraser = eraser; }
        void move(float x, float y) { path.moveTo(x, y); points.add(x); points.add(y); }
        void line(float x, float y) { path.lineTo(x, y); points.add(x); points.add(y); }
        JSONObject toJson() throws Exception { JSONObject json = new JSONObject(); json.put("color", color); json.put("width", width); json.put("eraser", eraser); JSONArray array = new JSONArray(); for (Float point : points) array.put(point); json.put("points", array); return json; }
        static Stroke fromJson(JSONObject json) throws Exception { Stroke stroke = new Stroke(json.getInt("color"), (float) json.getDouble("width"), json.optBoolean("eraser")); JSONArray points = json.getJSONArray("points"); for (int i = 0; i + 1 < points.length(); i += 2) { float x = (float) points.getDouble(i), y = (float) points.getDouble(i + 1); if (i == 0) stroke.move(x, y); else stroke.line(x, y); } return stroke; }
    }

    private final class DrawingCanvas extends View {
        final int[] colors = {Color.rgb(8,102,255), Color.rgb(232,65,66), Color.rgb(47,168,79), Color.rgb(139,92,246), Color.rgb(255,138,0), Color.rgb(30,30,30)};
        final Paint paper = new Paint(); final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG); final ScaleGestureDetector scaleDetector;
        int colorIndex; float strokeWidth = dp(10); boolean eraser; float scale = 1f, panX, panY, lastFocusX, lastFocusY; Stroke current;
        DrawingCanvas() {
            super(DrawingActivity.this); paper.setColor(Color.rgb(250,247,239));
            strokePaint.setStyle(Paint.Style.STROKE); strokePaint.setStrokeCap(Paint.Cap.ROUND); strokePaint.setStrokeJoin(Paint.Join.ROUND);
            scaleDetector = new ScaleGestureDetector(DrawingActivity.this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
                    lastFocusX = detector.getFocusX();
                    lastFocusY = detector.getFocusY();
                    return true;
                }
                @Override public boolean onScale(ScaleGestureDetector detector) {
                    int top = fullscreen ? 0 : dp(78);
                    float oldScale = scale;
                    float localX = (lastFocusX - panX) / oldScale;
                    float localY = (lastFocusY - top - panY) / oldScale;
                    scale = Math.max(1f, Math.min(4f, oldScale * detector.getScaleFactor()));
                    panX = detector.getFocusX() - localX * scale;
                    panY = detector.getFocusY() - top - localY * scale;
                    lastFocusX = detector.getFocusX();
                    lastFocusY = detector.getFocusY();
                    if (scale <= 1.01f) { scale = 1f; panX = 0; panY = 0; }
                    invalidate();
                    return true;
                }
            });
        }
        @Override protected void onDraw(Canvas canvas) {
            int top = fullscreen ? 0 : dp(78); int bottom = getHeight() - dp(98);
            canvas.drawRect(0, top, getWidth(), bottom, paper);
            canvas.save(); canvas.clipRect(0, top, getWidth(), bottom); canvas.translate(panX, top + panY); canvas.scale(scale, scale);
            for (Stroke stroke : active.strokes) { strokePaint.setColor(stroke.eraser ? paper.getColor() : stroke.color); strokePaint.setStrokeWidth(stroke.width); canvas.drawPath(stroke.path, strokePaint); }
            canvas.restore();
            if (scale > 1f) { Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG); hint.setColor(Color.DKGRAY); hint.setTextSize(dp(18)); canvas.drawText(String.format(Locale.US, "%.1f×", scale), dp(20), top + dp(30), hint); }
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            int top = fullscreen ? 0 : dp(78); int bottom = getHeight() - dp(98);
            if (event.getY() < top || event.getY() > bottom) return true;
            scaleDetector.onTouchEvent(event);
            if (event.getPointerCount() > 1 || scaleDetector.isInProgress()) { current = null; return true; }
            float x = (event.getX() - panX) / scale; float y = (event.getY() - top - panY) / scale;
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) { current = new Stroke(colors[colorIndex], strokeWidth, eraser); current.move(x, y); current.line(x + .01f, y + .01f); active.strokes.add(current); }
            else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && current != null) current.line(x, y);
            else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) { current = null; persist(); }
            invalidate(); return true;
        }
        void resetView() { scale = 1f; panX = 0; panY = 0; invalidate(); }
        Bitmap renderBitmap(int width, int height) { Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); Canvas canvas = new Canvas(bitmap); canvas.drawColor(paper.getColor()); float sx = width / (float) Math.max(1, getWidth()); float sy = height / (float) Math.max(1, getHeight() - dp(176)); canvas.scale(sx, sy); for (Stroke stroke : active.strokes) { strokePaint.setColor(stroke.eraser ? paper.getColor() : stroke.color); strokePaint.setStrokeWidth(stroke.width); canvas.drawPath(stroke.path, strokePaint); } return bitmap; }
    }
}
