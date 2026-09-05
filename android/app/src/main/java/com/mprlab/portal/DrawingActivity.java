package com.mprlab.portal;

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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
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

public final class DrawingActivity extends PortalActivity {
    private static final int BG = Color.rgb(255, 248, 234);
    private static final int SYSTEM_BAR = Color.rgb(36, 49, 71);
    private static final int SURFACE = Color.WHITE;
    private static final int INK = Color.rgb(36, 49, 71);
    private static final int BLUE = Color.rgb(63, 132, 255);
    private static final int PURPLE = Color.rgb(124, 92, 252);
    private static final int CORAL = Color.rgb(255, 105, 105);
    private static final int TEAL = Color.rgb(0, 166, 153);
    private static final int PALE_PURPLE = Color.rgb(241, 237, 255);
    private static final int ICON_LIBRARY = 1;
    private static final int ICON_NEW = 2;
    private static final int ICON_SHARE = 3;
    private static final int ICON_ERASER = 5;
    private String profileID;
    private String profileName;
    private SharedPreferences preferences;
    private final ArrayList<DrawingDocument> documents = new ArrayList<>();
    private DrawingDocument active;
    private DrawingCanvas drawingCanvas;
    private LinearLayout topBar;
    private LinearLayout palette;
    private final ArrayList<Button> colorButtons = new ArrayList<>();
    private StrokeSizeControl sizeControl;
    private ToolButton eraserButton;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
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
        addTopButton("My drawings", PURPLE, Color.WHITE, ICON_LIBRARY, v -> showLibrary());
        addTopButton("New picture", TEAL, Color.WHITE, ICON_NEW, v -> newDrawing());
        addTopButton("Save & share", CORAL, Color.WHITE, ICON_SHARE, v -> saveAndShare());
        PortalToolbar.navigation(this, topBar);
        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(-1, dp(PortalToolbar.HEIGHT_DP), Gravity.TOP);
        frame.addView(topBar, topParams);

        palette = new LinearLayout(this);
        palette.setOrientation(LinearLayout.HORIZONTAL);
        palette.setGravity(Gravity.CENTER);
        palette.setPadding(dp(12), dp(8), dp(12), dp(8));
        palette.setBackground(rounded(SURFACE, 24));
        palette.setElevation(dp(4));
        int[] colors = drawingCanvas.colors;
        colorButtons.clear();
        for (int index = 0; index < colors.length; index++) {
            final int selected = index;
            Button color = button("●", SURFACE, colors[index]);
            color.setTextColor(colors[index]);
            color.setTextSize(30);
            color.setContentDescription(colorName(index) + " brush");
            color.setOnClickListener(v -> {
                drawingCanvas.colorIndex = selected;
                drawingCanvas.eraser = false;
                saveToolSettings();
                updateToolSelection();
                drawingCanvas.invalidate();
            });
            colorButtons.add(color);
            palette.addView(color, paletteParams(60));
        }

        sizeControl = new StrokeSizeControl(drawingCanvas.strokeWidth / getResources().getDisplayMetrics().density);
        sizeControl.setOnSizeChanged(widthDp -> drawingCanvas.strokeWidth = dp(widthDp));
        palette.addView(sizeControl, paletteParams(420));

        eraserButton = toolIconButton("Eraser", PALE_PURPLE, INK, ICON_ERASER);
        eraserButton.setOnClickListener(v -> {
            drawingCanvas.eraser = !drawingCanvas.eraser;
            saveToolSettings();
            updateToolSelection();
            drawingCanvas.invalidate();
        });
        palette.addView(eraserButton, paletteParams(62));
        updateToolSelection();
        FrameLayout.LayoutParams paletteParams = new FrameLayout.LayoutParams(-1, dp(78), Gravity.BOTTOM);
        paletteParams.leftMargin = dp(14); paletteParams.rightMargin = dp(14); paletteParams.bottomMargin = dp(10);
        frame.addView(palette, paletteParams);

        setContentView(frame);
    }

    private void addTopButton(String label, int color, int textColor, int iconKind, View.OnClickListener listener) {
        ToolButton button = toolButton(label, color, textColor, iconKind, false);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(56), 1f);
        params.leftMargin = dp(5); params.rightMargin = dp(5);
        topBar.addView(button, params);
    }

    private void updateToolSelection() {
        if (drawingCanvas == null) return;
        for (int index = 0; index < colorButtons.size(); index++) {
            colorButtons.get(index).setBackground(swatchBackground(!drawingCanvas.eraser && index == drawingCanvas.colorIndex));
        }
        if (eraserButton != null) eraserButton.setSelectedVisual(drawingCanvas.eraser);
    }

    private LinearLayout.LayoutParams paletteParams(int width) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(width), dp(58));
        params.leftMargin = dp(3); params.rightMargin = dp(3);
        return params;
    }

    @Override protected void beforeHome() { persist(); }

    @Override public void onBackPressed() {
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
                HttpURLConnection connection = (HttpURLConnection) new URL(PortalConfig.serviceURL("/v1/drawings")).openConnection();
                connection.setRequestMethod("POST"); connection.setDoOutput(true); connection.setConnectTimeout(5000); connection.setReadTimeout(15000);
                connection.setRequestProperty("Content-Type", "image/png");
                connection.setRequestProperty("X-Portal-Profile", profileID);
                connection.setRequestProperty("X-Portal-Title", active.title);
                PortalConfig.authorize(connection);
                try (OutputStream output = connection.getOutputStream(); FileInputStream input = new FileInputStream(image)) {
                    byte[] buffer = new byte[8192]; int count;
                    while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
                }
                sharedURL = PortalConfig.absoluteServiceURL(new JSONObject(MainActivity.read(connection)).optString("url", ""));
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
        saveToolSettings();
    }

    private void saveToolSettings() {
        if (drawingCanvas == null || preferences == null) return;
        float density = getResources().getDisplayMetrics().density;
        preferences.edit()
                .putInt("drawing_color_" + profileID, drawingCanvas.colorIndex)
                .putFloat("drawing_size_" + profileID, drawingCanvas.strokeWidth / density)
                .putBoolean("drawing_eraser_" + profileID, drawingCanvas.eraser)
                .apply();
    }

    private String colorName(int index) {
        String[] names = {"Blue", "Red", "Green", "Purple", "Orange", "Black"};
        return names[Math.max(0, Math.min(index, names.length - 1))];
    }

    private static String safeFile(String value) { return value.replaceAll("[^a-zA-Z0-9_-]+", "-").replaceAll("^-|-$", ""); }

    private Button button(String label, int color) { return button(label, color, Color.WHITE); }
    private Button button(String label, int color, int textColor) { Button button = new Button(this); button.setText(label); button.setAllCaps(false); button.setTextColor(textColor); button.setTextSize(17); button.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)); button.setBackground(rounded(color, 18)); button.setPadding(dp(10), dp(6), dp(10), dp(6)); return button; }
    private ToolButton toolButton(String label, int color, int textColor, int iconKind, boolean compact) { return new ToolButton(label, color, textColor, iconKind, compact); }
    private ToolButton toolIconButton(String label, int color, int textColor, int iconKind) { return new ToolButton(label, color, textColor, iconKind, true, true); }
    private GradientDrawable rounded(int color, int radius) { GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable; }
    private GradientDrawable swatchBackground(boolean selected) { GradientDrawable drawable = rounded(SURFACE, 18); drawable.setStroke(dp(selected ? 4 : 1), selected ? PURPLE : Color.rgb(230, 230, 235)); return drawable; }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private final class ToolButton extends LinearLayout {
        private final int normalColor;
        private final int normalTextColor;
        private final ToolIconView icon;
        private final TextView words;

        ToolButton(String label, int color, int textColor, int iconKind, boolean compact) {
            this(label, color, textColor, iconKind, compact, false);
        }

        ToolButton(String label, int color, int textColor, int iconKind, boolean compact, boolean iconOnly) {
            super(DrawingActivity.this);
            normalColor = color;
            normalTextColor = textColor;
            setOrientation(HORIZONTAL);
            setGravity(iconOnly ? Gravity.CENTER : Gravity.CENTER_VERTICAL);
            setPadding(iconOnly ? 0 : dp(compact ? 8 : 12), 0, iconOnly ? 0 : dp(compact ? 7 : 10), 0);
            setBackground(rounded(color, 18));
            setClickable(true);
            setFocusable(true);
            setContentDescription(label);

            icon = new ToolIconView(iconKind, textColor);
            int iconSize = dp(compact ? 30 : 34);
            addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

            if (iconOnly) {
                words = null;
            } else {
                words = new TextView(DrawingActivity.this);
                words.setText(label);
                words.setTextColor(textColor);
                words.setTextSize(compact ? 13 : 15);
                words.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
                words.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                words.setSingleLine(true);
                LinearLayout.LayoutParams wordParams = new LinearLayout.LayoutParams(0, -1, 1f);
                wordParams.leftMargin = dp(compact ? 6 : 8);
                addView(words, wordParams);
            }
        }

        void setSelectedVisual(boolean selected) {
            int color = selected ? PURPLE : normalColor;
            int textColor = selected ? Color.WHITE : normalTextColor;
            setBackground(rounded(color, 18));
            if (words != null) words.setTextColor(textColor);
            icon.setInkColor(textColor);
        }
    }

    private interface OnSizeChangedListener { void onSizeChanged(float widthDp); }

    private final class StrokeSizeControl extends View {
        private static final float MIN_SIZE = 3f;
        private static final float MAX_SIZE = 28f;
        private final Paint controlPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float widthDp;
        private OnSizeChangedListener listener;

        StrokeSizeControl(float initialWidthDp) {
            super(DrawingActivity.this);
            widthDp = Math.max(MIN_SIZE, Math.min(MAX_SIZE, initialWidthDp));
            setBackground(rounded(PALE_PURPLE, 18));
            setClickable(true);
            setFocusable(true);
            updateAccessibilityLabel();
        }

        void setOnSizeChanged(OnSizeChangedListener value) { listener = value; }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float startX = dp(30);
            float endX = getWidth() - dp(34);
            float centerY = getHeight() / 2f;
            float fraction = (widthDp - MIN_SIZE) / (MAX_SIZE - MIN_SIZE);
            float thumbX = startX + (endX - startX) * fraction;

            controlPaint.setStyle(Paint.Style.STROKE);
            controlPaint.setStrokeCap(Paint.Cap.ROUND);
            controlPaint.setStrokeWidth(dp(4));
            controlPaint.setColor(Color.rgb(196, 199, 213));
            canvas.drawLine(startX, centerY, endX, centerY, controlPaint);
            controlPaint.setColor(PURPLE);
            canvas.drawLine(startX, centerY, thumbX, centerY, controlPaint);

            controlPaint.setStyle(Paint.Style.FILL);
            controlPaint.setColor(Color.rgb(196, 199, 213));
            canvas.drawCircle(startX, centerY, dp(3), controlPaint);
            canvas.drawCircle(endX, centerY, dp(8), controlPaint);

            controlPaint.setColor(Color.WHITE);
            canvas.drawCircle(thumbX, centerY, dp(12), controlPaint);
            controlPaint.setStyle(Paint.Style.STROKE);
            controlPaint.setStrokeWidth(dp(1));
            controlPaint.setColor(Color.rgb(224, 221, 235));
            canvas.drawCircle(thumbX, centerY, dp(12), controlPaint);
            controlPaint.setStyle(Paint.Style.FILL);
            controlPaint.setColor(PURPLE);
            float thumbRadius = dp(3) + fraction * dp(6);
            canvas.drawCircle(thumbX, centerY, thumbRadius, controlPaint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    updateFromTouch(event.getX());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    updateFromTouch(event.getX());
                    return true;
                case MotionEvent.ACTION_UP:
                    updateFromTouch(event.getX());
                    saveToolSettings();
                    performClick();
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    saveToolSettings();
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                default:
                    return super.onTouchEvent(event);
            }
        }

        @Override public boolean performClick() {
            super.performClick();
            return true;
        }

        private void updateFromTouch(float x) {
            float startX = dp(30);
            float endX = Math.max(startX + 1, getWidth() - dp(34));
            float fraction = Math.max(0f, Math.min(1f, (x - startX) / (endX - startX)));
            widthDp = MIN_SIZE + fraction * (MAX_SIZE - MIN_SIZE);
            updateAccessibilityLabel();
            if (listener != null) listener.onSizeChanged(widthDp);
            invalidate();
        }

        private void updateAccessibilityLabel() {
            setContentDescription("Brush size " + Math.round(widthDp));
        }
    }

    private final class ToolIconView extends View {
        private final int kind;
        private int inkColor;
        private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Drawable standardIcon;

        ToolIconView(int kind, int inkColor) {
            super(DrawingActivity.this);
            this.kind = kind;
            this.inkColor = inkColor;
            standardIcon = kind == ICON_ERASER ? getDrawable(R.drawable.ic_ink_eraser).mutate() : null;
            if (standardIcon != null) standardIcon.setTint(inkColor);
        }

        void setInkColor(int color) {
            inkColor = color;
            if (standardIcon != null) standardIcon.setTint(color);
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();
            canvas.scale(getWidth() / 36f, getHeight() / 36f);
            iconPaint.setStyle(Paint.Style.FILL);
            iconPaint.setColor(Color.argb(30, 0, 0, 0));
            canvas.drawCircle(18, 18, 17, iconPaint);
            iconPaint.setColor(inkColor);
            iconPaint.setStyle(Paint.Style.STROKE);
            iconPaint.setStrokeWidth(2.4f);
            iconPaint.setStrokeCap(Paint.Cap.ROUND);
            iconPaint.setStrokeJoin(Paint.Join.ROUND);

            if (standardIcon != null) {
                standardIcon.setBounds(5, 5, 31, 31);
                standardIcon.draw(canvas);
                canvas.restore();
                return;
            }

            switch (kind) {
                case ICON_LIBRARY: drawLibrary(canvas); break;
                case ICON_NEW: drawNew(canvas); break;
                case ICON_SHARE: drawShare(canvas); break;
                default: break;
            }
            canvas.restore();
        }

        private void drawLibrary(Canvas canvas) {
            canvas.drawRoundRect(new RectF(8, 9, 25, 24), 2, 2, iconPaint);
            canvas.drawRoundRect(new RectF(12, 13, 29, 28), 2, 2, iconPaint);
            Path mountain = new Path();
            mountain.moveTo(15, 24); mountain.lineTo(19, 19); mountain.lineTo(22, 22); mountain.lineTo(25, 18); mountain.lineTo(28, 24);
            canvas.drawPath(mountain, iconPaint);
        }

        private void drawNew(Canvas canvas) {
            canvas.drawLine(18, 8, 18, 28, iconPaint);
            canvas.drawLine(8, 18, 28, 18, iconPaint);
        }

        private void drawShare(Canvas canvas) {
            canvas.drawLine(13, 18, 23, 12, iconPaint);
            canvas.drawLine(13, 18, 23, 24, iconPaint);
            canvas.drawCircle(10, 18, 3, iconPaint);
            canvas.drawCircle(26, 10, 3, iconPaint);
            canvas.drawCircle(26, 26, 3, iconPaint);
        }
    }

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
        int colorIndex; float strokeWidth; boolean eraser; float scale = 1f, panX, panY, lastFocusX, lastFocusY; Stroke current;
        DrawingCanvas() {
            super(DrawingActivity.this); paper.setColor(Color.rgb(250,247,239));
            colorIndex = Math.max(0, Math.min(colors.length - 1, preferences.getInt("drawing_color_" + profileID, 0)));
            float savedWidth = preferences.getFloat("drawing_size_" + profileID, 10f);
            strokeWidth = dp(Math.max(3f, Math.min(28f, savedWidth)));
            eraser = preferences.getBoolean("drawing_eraser_" + profileID, false);
            strokePaint.setStyle(Paint.Style.STROKE); strokePaint.setStrokeCap(Paint.Cap.ROUND); strokePaint.setStrokeJoin(Paint.Join.ROUND);
            scaleDetector = new ScaleGestureDetector(DrawingActivity.this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
                    lastFocusX = detector.getFocusX();
                    lastFocusY = detector.getFocusY();
                    return true;
                }
                @Override public boolean onScale(ScaleGestureDetector detector) {
                    int top = dp(78);
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
            int top = dp(78); int bottom = getHeight() - dp(98);
            canvas.drawRect(0, top, getWidth(), bottom, paper);
            canvas.save(); canvas.clipRect(0, top, getWidth(), bottom); canvas.translate(panX, top + panY); canvas.scale(scale, scale);
            for (Stroke stroke : active.strokes) { strokePaint.setColor(stroke.eraser ? paper.getColor() : stroke.color); strokePaint.setStrokeWidth(stroke.width); canvas.drawPath(stroke.path, strokePaint); }
            canvas.restore();
            if (scale > 1f) { Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG); hint.setColor(Color.DKGRAY); hint.setTextSize(dp(18)); canvas.drawText(String.format(Locale.US, "%.1f×", scale), dp(20), top + dp(30), hint); }
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            int top = dp(78); int bottom = getHeight() - dp(98);
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
