package com.mprlab.portal;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.ExifInterface;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Qualification UI for the candidate adapter. Installed under a separate application ID. */
public final class CameraQualificationActivity extends Activity {
    private static final String CAPTURE_FILE = "camera-qualification.jpg";
    private TextureView preview;
    private ImageView picture;
    private TextView status;
    private TextView details;
    private Button capture;
    private Button open;
    private PortalCamera camera;
    private boolean resumed;
    private boolean requested;
    private Bitmap image;
    private int captures;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16); root.setBackgroundColor(0xfffcf8f0);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.addView(button("Allow camera", () -> requestPermissions(new String[]{Manifest.permission.CAMERA}, 1)));
        open = button("Open camera", () -> { requested = true; openCamera(); }); toolbar.addView(open);
        capture = button("Take picture", () -> { capture.setEnabled(false); status.setText("Capturing"); camera.capture(); });
        capture.setEnabled(false); toolbar.addView(capture);
        toolbar.addView(button("Release camera", () -> { requested = false; closeCamera(); }));
        root.addView(toolbar);
        status = new TextView(this); status.setTextSize(22); status.setContentDescription("Camera status");
        root.addView(status);
        details = new TextView(this); details.setTextSize(16); details.setContentDescription("Camera details"); root.addView(details);
        LinearLayout images = new LinearLayout(this);
        preview = new TextureView(this); preview.setContentDescription("Camera preview");
        picture = new ImageView(this); picture.setContentDescription("Camera JPEG"); picture.setScaleType(ImageView.ScaleType.FIT_CENTER);
        images.addView(preview, new LinearLayout.LayoutParams(0, -1, 1));
        images.addView(picture, new LinearLayout.LayoutParams(0, -1, 1));
        root.addView(images, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) { openCamera(); }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { if (camera != null) camera.transform(); }
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { closeCamera(); return true; }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
        });
        requested = true;
    }

    @Override protected void onResume() { super.onResume(); resumed = true; openCamera(); }
    @Override protected void onPause() { resumed = false; closeCamera(); super.onPause(); }
    @Override protected void onDestroy() { if (image != null) image.recycle(); super.onDestroy(); }

    private Button button(String label, Runnable action) {
        Button result = new Button(this); result.setText(label); result.setContentDescription(label);
        result.setOnClickListener(view -> action.run()); return result;
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == 1) openCamera();
    }

    private void openCamera() {
        if (!resumed || !requested || camera != null) return;
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            status.setText("Camera permission required"); return;
        }
        if (!preview.isAvailable()) { status.setText("Waiting for preview surface"); return; }
        status.setText("Opening camera"); open.setEnabled(false);
        camera = new PortalCamera(this, preview, new PortalCamera.Listener() {
            @Override public void ready(PortalCamera.Configuration configuration) {
                details.setText(configuration.description()); status.setText("Preview ready"); capture.setEnabled(true);
            }
            @Override public void picture(byte[] jpeg) {
                try {
                    Bitmap decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
                    if (decoded == null) throw new IOException("Camera returned an invalid JPEG");
                    ExifInterface exif = new ExifInterface(new ByteArrayInputStream(jpeg));
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    Matrix transform = new Matrix();
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90) transform.postRotate(90);
                    else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) transform.postRotate(180);
                    else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) transform.postRotate(270);
                    else if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                        decoded.recycle(); throw new IOException("Unexpected camera EXIF orientation " + orientation);
                    }
                    Bitmap oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), transform, true);
                    if (oriented != decoded) decoded.recycle();
                    try (FileOutputStream file = new FileOutputStream(new File(getFilesDir(), CAPTURE_FILE))) {
                        file.write(jpeg); file.getFD().sync();
                    }
                    picture.setImageBitmap(oriented);
                    if (image != null) image.recycle(); image = oriented;
                    captures++;
                    status.setText("JPEG ready " + captures + " · " + oriented.getWidth() + "x" + oriented.getHeight()
                            + " · " + jpeg.length + " bytes · EXIF " + orientation);
                    capture.setEnabled(true);
                } catch (IOException error) { requested = false; status.setText("Camera error: " + error); closeCamera(); }
            }
            @Override public void failed(String diagnostic) { requested = false; status.setText("Camera error: " + diagnostic); }
            @Override public void closed() {
                camera = null; capture.setEnabled(false); open.setEnabled(true);
                if (!status.getText().toString().startsWith("Camera error:")) status.setText("Camera released");
                if (resumed && requested && !isFinishing()) openCamera();
            }
        });
        camera.open();
    }
    private void closeCamera() {
        capture.setEnabled(false);
        if (camera != null) camera.close();
    }
}
