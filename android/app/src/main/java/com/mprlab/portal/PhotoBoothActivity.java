package com.mprlab.portal;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PhotoBoothActivity extends PortalActivity {
    private static final int CAMERA_PERMISSION_REQUEST = 51;
    private static final int BACKGROUND = PortalStyle.PAPER;
    private static final int INK = PortalStyle.INK;
    private static final int ACCENT = PortalStyle.PURPLE;
    private static final String STATUS_LABEL = "Photo status";
    private enum State { PERMISSION, OPENING, PREVIEW, COUNTDOWN, CAPTURING, PROCESSING, REVIEW, SAVING, SAVED, ALBUM, DETAIL, ERROR }
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService work = Executors.newSingleThreadExecutor();
    private final List<PhotoRenderer.Capture> captures = new ArrayList<>();
    private final List<Bitmap> thumbnails = new ArrayList<>();
    private State state = State.OPENING;
    private PhotoRenderer.Kind kind = PhotoRenderer.Kind.SINGLE;
    private PhotoRenderer.Frame frame = PhotoRenderer.Frame.NONE;
    private PhotoEffects.Effect effect = PhotoEffects.Effect.NORMAL;
    private boolean choosingFilters, choosingZoom, previewPending;
    private final PhotoFraming framing = new PhotoFraming();
    private PhotoMotion motion;
    private PhotoFraming.Frame displayedFrame, shutterFrame;
    private TextView framingHint;
    private Button captureButton;
    private LinearLayout primaryActions, panelNavigation;
    interface FrameSource { void request(PortalCamera.PreviewFrame callback); }
    private FrameSource frameSource;
    private int previewGeneration;
    private long previewSampleStart;
    private int previewSampleCount;
    private String previewSampleMode = "";
    private ImageView effectPreview;
    private Bitmap previewImage;
    private TextView filterHint;
    private final Runnable nextPreview = this::requestEffectPreview;
    private PhotoStore store;
    private String profileId, profileName, sequenceId;
    private boolean resumed, asleep, destroyed, cameraClosing;
    private int generation, remaining;
    private long captureByteLimit;
    private PortalCamera camera;
    private TextureView preview;
    private ImageView image;
    private Bitmap reviewImage;
    private PhotoStore.Entry selected;
    private FrameLayout stage;
    private LinearLayout controls;
    private TextView status;
    private View album;
    private Button albumButton;
    private final Runnable tick = this::countdownTick;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        String requested = getIntent().getStringExtra("profile_id");
        for (ProfileStore.Profile profile : new ProfileStore(this).profiles) {
            if (profile.id.equals(requested)) { profileId = profile.id; profileName = profile.name; break; }
        }
        buildScreen();
        if (profileId == null) { error("Select a child profile from Home."); return; }
        work.execute(() -> {
            try {
                PhotoFaces.initialize(this);
                motion = new PhotoMotion();
                PhotoStore photos = new PhotoStore(this);
                main.post(() -> { if (!destroyed) { store = photos; startPreview(); } });
            } catch (IOException failure) { postError(failure); }
        });
    }

    private void buildScreen() {
        LinearLayout toolbar = new LinearLayout(this);
        TextView title = text("Photo Booth", PortalStyle.TextRole.TITLE);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        albumButton = button("Album", this::showAlbum); toolbar.addView(albumButton);
        LinearLayout body = new LinearLayout(this); body.setPadding(dp(20), dp(8), dp(20), dp(20));
        stage = new FrameLayout(this); stage.setBackgroundColor(0xff202438);
        preview = new TextureView(this); preview.setAlpha(0); preview.setContentDescription("Camera preview");
        stage.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        effectPreview = new ImageView(this); effectPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        effectPreview.setBackgroundColor(0xff202438);
        effectPreview.setScaleX(-1); effectPreview.setContentDescription("Filtered camera preview");
        effectPreview.setVisibility(View.GONE);
        effectPreview.setOnTouchListener((view, event) -> {
            if (event.getAction() != android.view.MotionEvent.ACTION_UP || state != State.PREVIEW
                    || displayedFrame == null || previewImage == null) return true;
            float scale = Math.min((float) view.getWidth() / previewImage.getWidth(), (float) view.getHeight() / previewImage.getHeight());
            float w = previewImage.getWidth() * scale, h = previewImage.getHeight() * scale;
            float x = (event.getX() - (view.getWidth() - w) / 2) / w;
            float y = (event.getY() - (view.getHeight() - h) / 2) / h;
            // ImageView's inverse transform already maps mirrored touches into source coordinates.
            if (x >= 0 && x <= 1 && y >= 0 && y <= 1) {
                framing.tap(displayedFrame, x, y, (float) previewImage.getWidth() / previewImage.getHeight(), android.os.SystemClock.elapsedRealtime());
                refreshFraming();
            }
            return true;
        });
        stage.addView(effectPreview, new FrameLayout.LayoutParams(-1, -1));
        image = new ImageView(this); image.setScaleType(ImageView.ScaleType.FIT_CENTER); image.setContentDescription("Photo review");
        stage.addView(image, new FrameLayout.LayoutParams(-1, -1)); image.setVisibility(View.GONE);
        body.addView(stage, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout sidebar = new LinearLayout(this); sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setPadding(dp(20), 0, 0, 0);
        status = text("Opening Photo Booth", PortalStyle.TextRole.SECTION); status.setContentDescription(STATUS_LABEL);
        sidebar.addView(status, new LinearLayout.LayoutParams(-1, -2));
        panelNavigation = new LinearLayout(this); panelNavigation.setOrientation(LinearLayout.VERTICAL);
        sidebar.addView(panelNavigation, new LinearLayout.LayoutParams(-1, -2));
        controls = new LinearLayout(this); controls.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = PortalStyle.scroll(this); scroll.setFillViewport(true); scroll.addView(controls);
        sidebar.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        primaryActions = new LinearLayout(this);
        primaryActions.setOrientation(LinearLayout.VERTICAL);
        primaryActions.setPadding(0, dp(10), 0, 0);
        sidebar.addView(primaryActions, new LinearLayout.LayoutParams(-1, -2));
        body.addView(sidebar, new LinearLayout.LayoutParams(dp(340), -1));
        setContentView(PortalToolbar.screen(this, toolbar, body, BACKGROUND));
        preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) { openCamera(); }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { if (camera != null) camera.transform(); }
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { closeCamera(); return true; }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
        });
        renderControls();
    }

    @Override protected void onResume() { super.onResume(); resumed = true; openCamera(); }
    @Override protected void onPause() { resumed = false; suspendCamera(); super.onPause(); }
    @Override protected void onScreensaverStarted() { asleep = true; suspendCamera(); }
    @Override protected void onScreensaverStopped() { asleep = false; openCamera(); }
    @Override protected void beforeHome() { suspendCamera(); }
    @Override public void onBackPressed() {
        if (state == State.PREVIEW && (choosingFilters || choosingZoom)) showPhotoOptions();
        else if (state == State.DETAIL) { releaseImage(); showAlbum(); }
        else { suspendCamera(); finish(); }
    }
    @Override protected void onDestroy() {
        destroyed = true; generation++; main.removeCallbacks(tick); closeCamera();
        releaseImage(); clearAlbum();
        final String abandoned = sequenceId;
        if (store != null) work.execute(() -> { try { store.discardSequence(abandoned); } catch (IOException failure) { android.util.Log.e("PhotoBooth", "Remove temporary pictures", failure); } });
        work.execute(() -> { if (motion != null) motion.reset(); PhotoFaces.releaseWorker(); }); work.shutdown(); super.onDestroy();
    }

    private void suspendCamera() {
        main.removeCallbacks(tick);
        if (state == State.COUNTDOWN || state == State.CAPTURING || state == State.PROCESSING) {
            generation++; captures.clear(); cleanupTemporary(); state = State.OPENING;
            status.setText("Opening camera"); renderControls();
        }
        if (state == State.PREVIEW) { state = State.OPENING; status.setText("Opening camera"); renderControls(); }
        closeCamera();
    }
    private void closeCamera() {
        previewGeneration++; previewPending = false; displayedFrame = null; previewSampleStart = 0; framing.suspend(); main.removeCallbacks(nextPreview); clearEffectPreview();
        if (camera != null && !cameraClosing) { cameraClosing = true; camera.close(); }
    }
    private void clearEffectPreview() {
        effectPreview.setImageDrawable(null); effectPreview.setVisibility(View.GONE);
        if (previewImage != null) { previewImage.recycle(); previewImage = null; }
    }
    private boolean previewActive() {
        return resumed && !asleep && !destroyed && camera != null && !cameraClosing
                && (state == State.PREVIEW || state == State.COUNTDOWN || state == State.CAPTURING || state == State.PROCESSING);
    }
    private void requestEffectPreview() {
        if (!previewActive() || previewPending || frameSource == null) return;
        previewPending = true;
        final int token = previewGeneration;
        final PhotoEffects.Effect chosen = effect;
        final long selection = framing.selection();
        final PhotoFaces.Face selectedPerson = framing.target();
        frameSource.request(source -> {
            if (token != previewGeneration || !previewActive()) { source.recycle(); return; }
            final long received = android.os.SystemClock.elapsedRealtime();
            work.execute(() -> {
                try {
                    List<PhotoFaces.Face> faces = (selectedPerson == null || motion.needsDetection(selection))
                            ? PhotoFaces.find(source) : java.util.Collections.emptyList();
                    List<PhotoFaces.Face> measurements;
                    if (selectedPerson == null) { motion.reset(); measurements = faces; }
                    else measurements = motion.follow(source, selection, selectedPerson, faces);
                    main.post(() -> {
                        if (token != previewGeneration || !previewActive()) { source.recycle(); return; }
                        PhotoFraming.Frame framed = framing.update(faces, measurements, (float) source.getWidth() / source.getHeight(), received);
                        work.execute(() -> renderPreview(source, chosen, framed, token));
                    });
                } catch (RuntimeException failure) {
                    source.recycle();
                    main.post(() -> { if (token == previewGeneration) error("Find camera faces failed: " + failure.getMessage()); });
                }
            });
        });
    }
    private void renderPreview(Bitmap source, PhotoEffects.Effect chosen, PhotoFraming.Frame framed, int token) {
        Bitmap cropped = null;
        try {
            cropped = framed.crop.apply(source);
            PhotoEffects.Result filtered;
            if (framed.mode == PhotoFraming.Mode.SELECTING) {
                Bitmap selection = cropped.copy(Bitmap.Config.ARGB_8888, true);
                PhotoFraming.drawFaces(selection, framed.faces);
                filtered = new PhotoEffects.Result(selection, framed.faces.size());
            } else filtered = PhotoEffects.apply(cropped, chosen);
            if (cropped != source && cropped != filtered.image) cropped.recycle();
            if (source != filtered.image) source.recycle();
            main.post(() -> {
                if (token != previewGeneration || !previewActive()) { filtered.image.recycle(); return; }
                previewPending = false;
                effectPreview.setImageBitmap(filtered.image); effectPreview.setVisibility(View.VISIBLE);
                if (previewImage != null) previewImage.recycle(); previewImage = filtered.image;
                displayedFrame = framed;
                String sampleMode = framed.mode.name() + "/" + chosen.name();
                long now = android.os.SystemClock.elapsedRealtime();
                if (!sampleMode.equals(previewSampleMode) || previewSampleStart == 0) {
                    previewSampleMode = sampleMode; previewSampleStart = now; previewSampleCount = 0;
                } else if (++previewSampleCount == 20) {
                    android.util.Log.d("PhotoBoothPreview", sampleMode + ": 20 frames in " + (now - previewSampleStart) + " ms");
                    previewSampleStart = now; previewSampleCount = 0;
                }
                if (state == State.PREVIEW && status.getText().toString().equals("Preparing view")) status.setText("Ready · " + profileName);
                if (filterHint != null) filterHint.setText(chosen.needsFace && filtered.faces == 0
                        ? "Face the camera to add " + chosen.label.toLowerCase(java.util.Locale.US)
                        : chosen.label + (chosen.needsFace ? " · " + filtered.faces + " face" + (filtered.faces == 1 ? "" : "s") : " · live"));
                if (framingHint != null) framingHint.setText(framed.label);
                if (captureButton != null) captureButton.setEnabled(framed.ready(android.os.SystemClock.elapsedRealtime()));
                if ((framed.mode == PhotoFraming.Mode.LOST || framed.mode == PhotoFraming.Mode.SELECTING) && state == State.COUNTDOWN) {
                    main.removeCallbacks(tick); generation++; captures.clear(); cleanupTemporary();
                    state = State.PREVIEW; status.setText("Ready · " + profileName); renderControls();
                }
                main.post(nextPreview);
            });
        } catch (RuntimeException failure) {
            if (cropped != null && cropped != source && !cropped.isRecycled()) cropped.recycle();
            if (!source.isRecycled()) source.recycle();
            main.post(() -> { if (token == previewGeneration) error("Prepare photo preview failed: " + failure.getMessage()); });
        }
    }
    private void refreshFraming() {
        previewGeneration++; previewPending = false; displayedFrame = null;
        main.removeCallbacks(nextPreview); renderControls(); requestEffectPreview();
    }
    private void chooseEffect(PhotoEffects.Effect chosen) {
        effect = chosen; refreshFraming();
    }

    private void startPreview() {
        if (destroyed || store == null || profileId == null) return;
        generation++; main.removeCallbacks(tick); captures.clear(); selected = null; choosingFilters = false; choosingZoom = false;
        closeCamera();
        releaseImage(); clearAlbum(); cleanupTemporary();
        state = State.OPENING; status.setText("Opening camera");
        preview.setVisibility(View.VISIBLE); renderControls(); openCamera();
    }

    private void openCamera() {
        if (!resumed || asleep || destroyed || store == null || profileId == null || camera != null
                || (state != State.OPENING && state != State.PERMISSION)) return;
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            state = State.PERMISSION; status.setText("Camera permission required"); renderControls(); return;
        }
        state = State.OPENING; status.setText("Opening camera"); renderControls();
        if (!preview.isAvailable()) return;
        camera = new PortalCamera(this, preview, new PortalCamera.Listener() {
            @Override public void ready(PortalCamera.Configuration configuration) {
                if (!resumed || asleep || cameraClosing || state != State.OPENING) return;
                captureByteLimit = configuration.captureByteLimit();
                frameSource = camera::requestPreviewFrame;
                state = State.PREVIEW; status.setText("Preparing view"); renderControls(); requestEffectPreview();
            }
            @Override public void picture(byte[] jpeg) { receivePicture(jpeg); }
            @Override public void failed(String diagnostic) {
                android.util.Log.e("PhotoBooth", diagnostic);
                error("Camera unavailable. Check camera access and try again.");
            }
            @Override public void closed() { camera = null; cameraClosing = false; openCamera(); }
        });
        camera.open();
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == CAMERA_PERMISSION_REQUEST) openCamera();
    }

    private void beginCapture() {
        if (state != State.PREVIEW || displayedFrame == null || !displayedFrame.ready(android.os.SystemClock.elapsedRealtime())) return;
        sequenceId = UUID.randomUUID().toString(); captures.clear();
        final int token = ++generation;
        state = State.PROCESSING; status.setText("Checking photo storage"); renderControls();
        work.execute(() -> {
            try {
                store.reserve(profileId, kind.count, captureByteLimit);
                main.post(() -> { if (current(token)) beginCountdown(); });
            } catch (IOException | RuntimeException failure) { main.post(() -> { if (current(token)) error(failure.getMessage()); }); }
        });
    }
    private void beginCountdown() {
        if (!resumed || asleep || destroyed) return;
        state = State.COUNTDOWN; remaining = 3; renderControls(); countdownTick();
    }
    private void countdownTick() {
        if (state != State.COUNTDOWN || !resumed || asleep) return;
        if (remaining > 0) {
            status.setText(remaining + " · picture " + (captures.size() + 1) + " of " + kind.count);
            remaining--; main.postDelayed(tick, 1000);
        } else {
            if (displayedFrame == null || !displayedFrame.ready(android.os.SystemClock.elapsedRealtime())) {
                generation++; captures.clear(); cleanupTemporary(); state = State.PREVIEW;
                status.setText("Wait for a clear preview"); renderControls(); return;
            }
            shutterFrame = displayedFrame;
            state = State.CAPTURING; status.setText("Taking picture " + (captures.size() + 1)); renderControls();
            camera.capture();
        }
    }
    private void receivePicture(byte[] jpeg) {
        if (state != State.CAPTURING || asleep || !resumed) return;
        state = State.PROCESSING; status.setText("Preparing picture"); renderControls();
        final int token = generation, index = captures.size();
        final String sequence = sequenceId;
        final PhotoFraming.Frame shot = shutterFrame;
        work.execute(() -> {
            try {
                if (shot.mode == PhotoFraming.Mode.FOLLOWING) {
                    Bitmap captured = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
                    if (captured == null) throw new IOException("Read captured face failed");
                    try {
                        if (!motion.confirms(captured, shot.target))
                            throw new IOException("Step back into the picture and try again.");
                    } finally { captured.recycle(); }
                }
                File file = store.writeCapture(sequence, index, jpeg);
                main.post(() -> {
                    if (!current(token)) return;
                    captures.add(new PhotoRenderer.Capture(file, shot.crop));
                    if (captures.size() < kind.count) beginCountdown(); else assemble(token);
                });
            } catch (IOException | RuntimeException failure) { main.post(() -> { if (current(token)) error(failure.getMessage()); }); }
        });
    }
    private void assemble(int token) {
        closeCamera();
        List<PhotoRenderer.Capture> files = new ArrayList<>(captures);
        PhotoRenderer.Kind selectedKind = kind; PhotoRenderer.Frame selectedFrame = frame;
        PhotoEffects.Effect selectedEffect = effect;
        work.execute(() -> {
            try {
                Bitmap result = PhotoRenderer.render(files, selectedKind, selectedFrame, selectedEffect);
                main.post(() -> {
                    if (!current(token)) { result.recycle(); return; }
                    reviewImage = result; image.setImageBitmap(result); image.setVisibility(View.VISIBLE); preview.setVisibility(View.INVISIBLE);
                    state = State.REVIEW; status.setText("Review · " + result.getWidth() + " × " + result.getHeight()); renderControls();
                });
            } catch (IOException | RuntimeException failure) { main.post(() -> { if (current(token)) error(failure.getMessage()); }); }
        });
    }
    private void save() {
        if (state != State.REVIEW) return;
        state = State.SAVING; status.setText("Saving picture"); renderControls();
        Bitmap saving = reviewImage; reviewImage = null;
        work.execute(() -> {
            try {
                store.save(sequenceId, profileId, kind, frame, saving); store.discardSequence(sequenceId);
                main.post(() -> {
                    if (destroyed) { saving.recycle(); return; }
                    reviewImage = saving; state = State.SAVED; status.setText("Saved to " + profileName + "’s album"); renderControls();
                });
            } catch (IOException failure) {
                main.post(() -> { if (destroyed) saving.recycle(); else { reviewImage = saving; error(failure.getMessage()); } });
            }
        });
    }
    private void showAlbum() {
        if (store == null || state == State.SAVING) return;
        generation++; main.removeCallbacks(tick); closeCamera(); releaseImage(); clearAlbum(); cleanupTemporary();
        state = State.ALBUM; status.setText("Opening album"); renderControls(); preview.setVisibility(View.INVISIBLE);
        final int token = generation;
        work.execute(() -> {
            try {
                List<PhotoStore.Entry> entries = store.list(profileId);
                main.post(() -> { if (current(token)) renderAlbum(entries); });
            } catch (IOException | RuntimeException failure) { main.post(() -> { if (current(token)) error(failure.getMessage()); }); }
        });
    }
    private void renderAlbum(List<PhotoStore.Entry> entries) {
        clearAlbum();
        status.setText(entries.size() + (entries.size() == 1 ? " picture" : " pictures") + " · " + profileName);
        ScrollView scroll = PortalStyle.scroll(this);
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(20), dp(16), dp(20), dp(16));
        if (entries.isEmpty()) { TextView empty = text("Your pictures will appear here", PortalStyle.TextRole.SECTION); empty.setTextColor(Color.WHITE); list.addView(empty); }
        for (PhotoStore.Entry entry : entries) {
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(12), dp(8), dp(12), dp(8));
            row.setBackgroundColor(BACKGROUND); row.setContentDescription("Open photo " + entry.id);
            Bitmap thumbnail = BitmapFactory.decodeFile(entry.thumbnail().getPath());
            if (thumbnail == null) { error("Read photo thumbnail failed"); return; }
            thumbnails.add(thumbnail);
            ImageView thumb = new ImageView(this); thumb.setImageBitmap(thumbnail); thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
            row.addView(thumb, new LinearLayout.LayoutParams(dp(150), dp(120)));
            TextView caption = text(entry.kind.label + " · " + entry.frame.label + "\n" + entry.createdAt.substring(0, 10), PortalStyle.TextRole.BODY);
            row.addView(caption, new LinearLayout.LayoutParams(0, -2, 1)); row.setOnClickListener(view -> openEntry(entry));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(12); list.addView(row, params);
        }
        scroll.addView(list); album = scroll; stage.addView(album, new FrameLayout.LayoutParams(-1, -1)); renderControls();
    }
    private void openEntry(PhotoStore.Entry entry) {
        clearAlbum(); selected = entry; state = State.DETAIL; status.setText(entry.kind.label + " · " + entry.frame.label); renderControls();
        final int token = generation;
        work.execute(() -> {
            Bitmap decoded = BitmapFactory.decodeFile(entry.image().getPath());
            main.post(() -> {
                if (!current(token)) { if (decoded != null) decoded.recycle(); return; }
                if (decoded == null) { error("Read saved picture failed"); return; }
                reviewImage = decoded; image.setImageBitmap(decoded); image.setVisibility(View.VISIBLE);
            });
        });
    }
    private void confirmDelete() {
        PhotoStore.Entry entry = selected;
        showPortalDialog(new AlertDialog.Builder(this).setTitle("Delete this picture?")
                .setMessage("This removes the picture from " + profileName + "’s album.")
                .setNegativeButton("Keep picture", null).setPositiveButton("Delete picture", (dialog, which) -> {
                    work.execute(() -> {
                        try { store.delete(entry, profileId); main.post(() -> { if (!destroyed) showAlbum(); }); }
                        catch (IOException failure) { postError(failure); }
                    });
                }).create());
    }

    private void renderControls() {
        controls.removeAllViews(); primaryActions.removeAllViews(); panelNavigation.removeAllViews(); filterHint = null; framingHint = null; captureButton = null;
        albumButton.setEnabled(store != null && state != State.SAVING && state != State.PROCESSING);
        switch (state) {
            case PERMISSION:
                add("Allow camera", () -> requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST));
                add("Camera settings", () -> startActivity(new android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:" + getPackageName())))); break;
            case OPENING: break;
            case PREVIEW:
                captureButton = primaryAction("Take picture", this::beginCapture);
                captureButton.setEnabled(displayedFrame != null && displayedFrame.ready(android.os.SystemClock.elapsedRealtime()));
                if (choosingZoom || choosingFilters) {
                    Button back = button("← Back", this::showPhotoOptions);
                    back.setContentDescription("Back to photo options");
                    panelNavigation.addView(back, new LinearLayout.LayoutParams(-1, dp(PortalStyle.CONTROL_HEIGHT)));
                }
                if (choosingZoom) {
                    controls.addView(text("Zoom", PortalStyle.TextRole.SECTION));
                    for (PhotoFraming.Zoom zoom : PhotoFraming.Zoom.values())
                        choice(zoom.label, framing.zoom() == zoom, () -> { framing.zoom(zoom); refreshFraming(); });
                    break;
                }
                if (choosingFilters) {
                    controls.addView(text("Funhouse", PortalStyle.TextRole.SECTION));
                    for (PhotoEffects.Effect choice : PhotoEffects.Effect.values()) {
                        if (choice == PhotoEffects.Effect.BUNNY) controls.addView(text("Face accessories", PortalStyle.TextRole.SECTION));
                        choice(choice.label, effect == choice, () -> chooseEffect(choice));
                    }
                    filterHint = text(effect.label, PortalStyle.TextRole.BODY); filterHint.setContentDescription("Filter status"); controls.addView(filterHint);
                    break;
                }
                controls.addView(text("Picture", PortalStyle.TextRole.SECTION));
                LinearLayout formats = optionRow();
                for (PhotoRenderer.Kind choice : PhotoRenderer.Kind.values()) {
                    visualChoice(formats, choice.label, choice == PhotoRenderer.Kind.SINGLE ? "1 photo" : "4 photos",
                            choice == PhotoRenderer.Kind.SINGLE ? PhotoOptionIcon.Art.SINGLE : PhotoOptionIcon.Art.STRIP,
                            kind == choice, () -> { kind = choice; renderControls(); });
                }
                controls.addView(text("Frame", PortalStyle.TextRole.SECTION));
                LinearLayout frames = optionRow();
                for (PhotoRenderer.Frame choice : PhotoRenderer.Frame.values()) {
                    PhotoOptionIcon.Art art = choice == PhotoRenderer.Frame.STARS ? PhotoOptionIcon.Art.STARS
                            : choice == PhotoRenderer.Frame.CONFETTI ? PhotoOptionIcon.Art.CONFETTI : PhotoOptionIcon.Art.NONE;
                    visualChoice(frames, choice.label, "", art, frame == choice, () -> { frame = choice; renderControls(); });
                }
                add("Filters", () -> { choosingFilters = true; renderControls(); });
                filterHint = text(effect.label, PortalStyle.TextRole.BODY); filterHint.setContentDescription("Filter status"); controls.addView(filterHint);
                add("Zoom", () -> { choosingZoom = true; renderControls(); });
                break;
            case COUNTDOWN: case CAPTURING: case PROCESSING:
                add("Cancel", this::startPreview); break;
            case REVIEW:
                primaryAction("Save", this::save); add("Retake", this::startPreview); add("Discard", this::startPreview); break;
            case SAVING: break;
            case SAVED: case ALBUM: primaryAction("New picture", this::startPreview); break;
            case DETAIL: add("Delete", this::confirmDelete); add("Back to album", this::showAlbum); break;
            case ERROR: primaryAction("Try again", this::startPreview); break;
        }
        if (state == State.PREVIEW || state == State.COUNTDOWN || state == State.CAPTURING || state == State.PROCESSING) {
            framingHint = text(displayedFrame == null ? "Preparing view" : displayedFrame.label, PortalStyle.TextRole.BODY);
            framingHint.setContentDescription("Framing status"); primaryActions.addView(framingHint, 0);
        }
    }
    private void showPhotoOptions() { choosingFilters = false; choosingZoom = false; renderControls(); }
    private LinearLayout optionRow() {
        LinearLayout row = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(10);
        controls.addView(row, params); return row;
    }
    private void visualChoice(LinearLayout row, String label, String caption, PhotoOptionIcon.Art art, boolean selected, Runnable action) {
        Button control = button(label, action); control.setText(caption); control.setSelected(selected); control.setTooltipText(label);
        control.setPadding(dp(6), dp(10), dp(6), dp(10)); control.setMinWidth(0); control.setMinimumWidth(0);
        control.setMinHeight(dp(caption.isEmpty() ? 86 : 112)); control.setMinimumHeight(dp(caption.isEmpty() ? 86 : 112));
        PhotoOptionIcon icon = new PhotoOptionIcon(art); icon.setBounds(0, 0, dp(76), dp(60));
        control.setCompoundDrawables(null, icon, null, null); control.setCompoundDrawablePadding(dp(4));
        control.setBackground(PortalStyle.surface(this, selected ? PortalStyle.MINT : PortalStyle.WHITE, 12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        if (row.getChildCount() > 0) params.leftMargin = dp(8);
        row.addView(control, params);
    }
    private boolean current(int token) { return !destroyed && generation == token; }
    private void error(String message) {
        if (destroyed) return;
        generation++; main.removeCallbacks(tick); closeCamera();
        state = State.ERROR; status.setText("Error: " + message); renderControls();
    }
    private void postError(Exception failure) { main.post(() -> error(failure.getMessage())); }
    private void cleanupTemporary() {
        if (store == null) return;
        final String abandoned = sequenceId;
        work.execute(() -> { try { store.discardSequence(abandoned); } catch (IOException failure) { postError(failure); } });
    }
    private void releaseImage() { image.setImageDrawable(null); image.setVisibility(View.GONE); if (reviewImage != null) { reviewImage.recycle(); reviewImage = null; } }
    private void clearAlbum() {
        if (album != null) { stage.removeView(album); album = null; }
        for (Bitmap bitmap : thumbnails) bitmap.recycle(); thumbnails.clear();
    }
    private TextView text(String value, PortalStyle.TextRole role) { TextView text = new TextView(this); text.setText(value); PortalStyle.text(text, role); text.setTextColor(INK); text.setPadding(0, dp(4), 0, dp(8)); return text; }
    private Button button(String label, Runnable action) {
        Button result = new Button(this); result.setText(label); result.setAllCaps(false); result.setTextColor(INK);
        result.setContentDescription(label); result.setOnClickListener(view -> action.run());
        result.setMinHeight(dp(PortalStyle.CONTROL_HEIGHT));
        result.setBackgroundTintList(null);
        result.setBackground(PortalStyle.surface(this, ACCENT, 12));
        PortalStyle.button(result);
        return result;
    }
    private Button primaryAction(String label, Runnable action) {
        Button control = button(label, action);
        PortalStyle.primary(control);
        control.setBackground(PortalStyle.surface(this, PortalStyle.YELLOW, 16));
        primaryActions.addView(control, new LinearLayout.LayoutParams(-1, dp(PortalStyle.PRIMARY_HEIGHT)));
        return control;
    }
    private void add(String label, Runnable action) { controls.addView(button(label, action), new LinearLayout.LayoutParams(-1, dp(PortalStyle.CONTROL_HEIGHT))); }
    private void choice(String label, boolean selected, Runnable action) {
        Button control = button(label, action); control.setSelected(selected);
        control.setTextColor(INK);
        control.setBackground(PortalStyle.surface(this, selected ? PortalStyle.MINT : PortalStyle.WHITE, 12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(PortalStyle.CONTROL_HEIGHT)); params.bottomMargin = dp(7); controls.addView(control, params);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
