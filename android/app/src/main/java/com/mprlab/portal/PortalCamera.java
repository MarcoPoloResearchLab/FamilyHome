package com.mprlab.portal;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import java.io.ByteArrayOutputStream;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.nio.ByteBuffer;
import java.util.Arrays;

/** One camera session. Create a new adapter after close; all commands originate on the UI thread. */
final class PortalCamera {
    static final int MAX_IMAGE_SIDE = 1600;
    static final int JPEG_QUALITY = 90;
    static final int MAX_JPEG_BYTES = MAX_IMAGE_SIDE * MAX_IMAGE_SIDE * 4;

    interface Listener {
        void ready(Configuration configuration);
        void picture(byte[] jpeg);
        void failed(String diagnostic);
        void closed();
    }

    interface PreviewFrame { void received(Bitmap image); }
    private PreviewFrame pendingPreview;
    private int previewOrientation;

    /** One bounded, upright frame for local image processing; callback owns the bitmap. */
    void requestPreviewFrame(PreviewFrame callback) {
        if (closed) return;
        int orientation = imageRotation();
        worker.post(() -> { if (!closed) { pendingPreview = callback; previewOrientation = orientation; } });
    }

    static final class Configuration {
        final String cameraId;
        final Size previewSize;
        final Size imageSize;
        final int sensorOrientation;
        final int lensFacing;
        final int hardwareLevel;
        Configuration(String id, Size preview, Size image, int sensor, int facing, int level) {
            cameraId = id; previewSize = preview; imageSize = image;
            sensorOrientation = sensor; lensFacing = facing; hardwareLevel = level;
        }
        String description() {
            return "Camera " + cameraId + " · preview " + previewSize + " · YUV " + imageSize
                    + " · sensor " + sensorOrientation + " · facing " + lensFacing + " · level " + hardwareLevel;
        }
        long captureByteLimit() { return (long) imageSize.getWidth() * imageSize.getHeight() * 4 + 65536; }
    }

    private final Context context;
    private final TextureView preview;
    private final Listener listener;
    private final DisplayManager displays;
    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override public void onDisplayAdded(int displayId) { }
        @Override public void onDisplayRemoved(int displayId) { }
        @Override public void onDisplayChanged(int displayId) {
            if (preview.getDisplay() != null && preview.getDisplay().getDisplayId() == displayId) transform();
        }
    };
    private final Handler main = new Handler(Looper.getMainLooper());
    private static final long CAMERA_DEADLINE_MS = 10000;
    private final Runnable openDeadline = () -> fail("Camera preview did not become ready");
    private final Runnable captureDeadline = () -> fail("Camera did not deliver the requested picture");
    private final HandlerThread thread = new HandlerThread("PhotoBoothCamera");
    private final Handler worker;
    private volatile boolean closed;
    private boolean started;
    private boolean opening;
    private boolean closeCompleted;
    private boolean ready;
    private boolean capturing;
    private int captureOrientation;
    private Configuration configuration;
    private CameraDevice device;
    private CameraCaptureSession session;
    private ImageReader reader;
    private Surface previewSurface;

    PortalCamera(Context context, TextureView preview, Listener listener) {
        this.context = context; this.preview = preview; this.listener = listener;
        displays = context.getSystemService(DisplayManager.class);
        thread.start(); worker = new Handler(thread.getLooper());
    }

    void open() {
        if (started || closed) throw new IllegalStateException("Camera session cannot open twice");
        started = true;
        main.postDelayed(openDeadline, CAMERA_DEADLINE_MS);
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            fail("Open camera: camera permission is required"); return;
        }
        if (!preview.isAvailable()) { fail("Open camera: preview surface is unavailable"); return; }
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            configuration = select(manager);
            SurfaceTexture texture = preview.getSurfaceTexture();
            texture.setDefaultBufferSize(configuration.previewSize.getWidth(), configuration.previewSize.getHeight());
            transform();
            displays.registerDisplayListener(displayListener, main);
            previewSurface = new Surface(texture);
            reader = ImageReader.newInstance(configuration.imageSize.getWidth(), configuration.imageSize.getHeight(),
                    ImageFormat.YUV_420_888, 2);
            reader.setOnImageAvailableListener(source -> {
                try (Image image = source.acquireNextImage()) {
                    if (image == null || closed) return;
                    if (pendingPreview != null) {
                        PreviewFrame callback = pendingPreview; pendingPreview = null;
                        Bitmap frame = previewBitmap(image, previewOrientation);
                        main.post(() -> { if (closed) frame.recycle(); else callback.received(frame); });
                    }
                    if (!capturing) return;
                    byte[] bytes = encode(image, captureOrientation);
                    capturing = false;
                    deliver(() -> { main.removeCallbacks(captureDeadline); listener.picture(bytes); });
                } catch (RuntimeException error) { fail("Read YUV from camera " + configuration.cameraId + ": " + error); }
            }, worker);
            opening = true;
            manager.openCamera(configuration.cameraId, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice opened) {
                    opening = false;
                    device = opened;
                    if (closed) { opened.close(); return; }
                    createSession();
                }
                @Override public void onDisconnected(CameraDevice disconnected) {
                    opening = false; device = disconnected;
                    if (closed) disconnected.close();
                    else fail("Camera " + configuration.cameraId + " disconnected");
                }
                @Override public void onError(CameraDevice broken, int error) {
                    opening = false; device = broken;
                    if (closed) broken.close();
                    else fail("Open camera " + configuration.cameraId + ": CameraDevice error " + error);
                }
                @Override public void onClosed(CameraDevice released) {
                    device = null;
                    completeClose();
                }
            }, worker);
        } catch (CameraAccessException | RuntimeException error) {
            opening = false; fail("Open camera: " + error);
        }
    }

    private static Configuration select(CameraManager manager) throws CameraAccessException {
        String[] ids = manager.getCameraIdList();
        Arrays.sort(ids);
        // Photo Booth uses the camera that faces the child, with one deterministic current selection.
        for (String id : ids) {
            CameraCharacteristics values = manager.getCameraCharacteristics(id);
            Integer facing = values.get(CameraCharacteristics.LENS_FACING);
            if (facing == null || facing != CameraCharacteristics.LENS_FACING_FRONT) continue;
            StreamConfigurationMap streams = values.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Integer sensor = values.get(CameraCharacteristics.SENSOR_ORIENTATION);
            Integer level = values.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (streams == null || sensor == null || level == null)
                throw new IllegalStateException("Camera " + id + " has incomplete characteristics");
            Size image = largest(streams.getOutputSizes(ImageFormat.YUV_420_888), MAX_IMAGE_SIDE, MAX_IMAGE_SIDE, null);
            Size preview = largest(streams.getOutputSizes(SurfaceTexture.class), 1280, 960, image);
            return new Configuration(id, preview, image, sensor, facing, level);
        }
        throw new IllegalStateException("No front camera is available");
    }

    private static Size largest(Size[] sizes, int maxWidth, int maxHeight, Size aspect) {
        Size selected = null;
        if (sizes != null) for (Size size : sizes) {
            if (size.getWidth() > maxWidth || size.getHeight() > maxHeight) continue;
            if (aspect != null && (long) size.getWidth() * aspect.getHeight() != (long) size.getHeight() * aspect.getWidth()) continue;
            if (selected == null || (long) size.getWidth() * size.getHeight() > (long) selected.getWidth() * selected.getHeight()) selected = size;
        }
        if (selected == null) throw new IllegalStateException("No supported camera dimensions within " + maxWidth + "x" + maxHeight);
        return selected;
    }

    void transform() {
        if (configuration == null || closed) return;
        int degrees = displayRotation();
        int width = preview.getWidth(), height = preview.getHeight();
        // TextureView already applies the camera sensor rotation and front-camera reflection.
        boolean sensorSwapsSides = configuration.sensorOrientation % 180 != 0;
        float bufferWidth = sensorSwapsSides ? configuration.previewSize.getHeight() : configuration.previewSize.getWidth();
        float bufferHeight = sensorSwapsSides ? configuration.previewSize.getWidth() : configuration.previewSize.getHeight();
        Matrix matrix = new Matrix();
        matrix.setScale(bufferWidth / width, bufferHeight / height);
        matrix.postTranslate(-bufferWidth / 2f, -bufferHeight / 2f);
        matrix.postRotate(-degrees);
        float rotatedWidth = degrees % 180 == 0 ? bufferWidth : bufferHeight;
        float rotatedHeight = degrees % 180 == 0 ? bufferHeight : bufferWidth;
        float scale = Math.max(width / rotatedWidth, height / rotatedHeight);
        matrix.postScale(scale, scale);
        matrix.postTranslate(width / 2f, height / 2f);
        preview.setTransform(matrix);
    }

    private int displayRotation() {
        int rotation = preview.getDisplay().getRotation();
        return rotation == Surface.ROTATION_90 ? 90 : rotation == Surface.ROTATION_180 ? 180
                : rotation == Surface.ROTATION_270 ? 270 : 0;
    }

    private int imageRotation() {
        return (configuration.sensorOrientation + displayRotation()) % 360;
    }

    private void createSession() {
        try {
            device.createCaptureSession(Arrays.asList(previewSurface, reader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession configured) {
                    if (closed) { configured.close(); return; }
                    session = configured;
                    try {
                        CaptureRequest.Builder request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        request.addTarget(previewSurface);
                        request.addTarget(reader.getSurface());
                        session.setRepeatingRequest(request.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override public void onCaptureCompleted(CameraCaptureSession source, CaptureRequest request, TotalCaptureResult result) {
                                if (!ready && !closed) {
                                    ready = true;
                                    deliver(() -> { main.removeCallbacks(openDeadline); listener.ready(configuration); });
                                }
                            }
                            @Override public void onCaptureFailed(CameraCaptureSession source, CaptureRequest request, CaptureFailure failure) {
                                fail("Camera preview failed: reason " + failure.getReason());
                            }
                        }, worker);
                    } catch (CameraAccessException | RuntimeException error) { fail("Start camera preview: " + error); }
                }
                @Override public void onConfigureFailed(CameraCaptureSession configured) {
                    configured.close(); fail("Configure camera " + configuration.cameraId + " preview and YUV session failed");
                }
            }, worker);
        } catch (CameraAccessException | RuntimeException error) { fail("Create camera session: " + error); }
    }

    void capture() {
        if (closed) throw new IllegalStateException("Capture after camera close");
        int orientation = imageRotation();
        main.postDelayed(captureDeadline, CAMERA_DEADLINE_MS);
        worker.post(() -> {
            if (closed) return;
            if (!ready || capturing) { fail("Capture camera picture: camera is not ready"); return; }
            capturing = true;
            captureOrientation = orientation;

        });
    }

    private static byte[] encode(Image image, int orientation) {
        int width = image.getWidth(), height = image.getHeight();
        if (width <= 0 || height <= 0 || width > MAX_IMAGE_SIDE || height > MAX_IMAGE_SIDE
                || width % 2 != 0 || height % 2 != 0 || image.getFormat() != ImageFormat.YUV_420_888)
            throw new IllegalStateException("Invalid camera YUV dimensions or format");
        byte[] nv21 = new byte[width * height * 3 / 2];
        Image.Plane[] planes = image.getPlanes();
        for (int planeIndex = 0; planeIndex < 3; planeIndex++) {
            Image.Plane plane = planes[planeIndex];
            ByteBuffer buffer = plane.getBuffer();
            int rows = planeIndex == 0 ? height : height / 2;
            int columns = planeIndex == 0 ? width : width / 2;
            int output = planeIndex == 0 ? 0 : width * height + (planeIndex == 1 ? 1 : 0);
            int step = planeIndex == 0 ? 1 : 2;
            int start = buffer.position();
            for (int row = 0; row < rows; row++) {
                int offset = start + row * plane.getRowStride();
                for (int column = 0; column < columns; column++) {
                    nv21[output] = buffer.get(offset + column * plane.getPixelStride());
                    output += step;
                }
            }
        }
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        if (!new YuvImage(nv21, ImageFormat.NV21, width, height, null)
                .compressToJpeg(new Rect(0, 0, width, height), JPEG_QUALITY, encoded))
            throw new IllegalStateException("Encode camera YUV as JPEG failed");
        byte[] raw = encoded.toByteArray();
        Bitmap decoded = BitmapFactory.decodeByteArray(raw, 0, raw.length);
        if (decoded == null) throw new IllegalStateException("Decode camera image failed");
        Matrix transform = new Matrix(); transform.postRotate(orientation);
        Bitmap rotated = Bitmap.createBitmap(decoded, 0, 0, width, height, transform, true);
        try {
            encoded.reset();
            if (!rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, encoded))
                throw new IllegalStateException("Encode oriented camera picture failed");
            return encoded.toByteArray();
        } finally {
            if (rotated != decoded) rotated.recycle();
            decoded.recycle();
        }
    }

    private static Bitmap previewBitmap(Image image, int orientation) {
        int sourceWidth = image.getWidth(), sourceHeight = image.getHeight();
        if (image.getFormat() != ImageFormat.YUV_420_888 || sourceWidth < 1 || sourceHeight < 1
                || sourceWidth > MAX_IMAGE_SIDE || sourceHeight > MAX_IMAGE_SIDE)
            throw new IllegalStateException("Invalid preview YUV dimensions or format");
        float scale = Math.min(1f, 640f / Math.max(sourceWidth, sourceHeight));
        int width = Math.max(2, Math.round(sourceWidth * scale)), height = Math.max(2, Math.round(sourceHeight * scale));
        Image.Plane[] planes = image.getPlanes();
        byte[][] data = new byte[3][];
        int[] rows = new int[3], strides = new int[3];
        for (int plane = 0; plane < 3; plane++) {
            ByteBuffer buffer = planes[plane].getBuffer();
            data[plane] = new byte[buffer.remaining()]; buffer.duplicate().get(data[plane]);
            rows[plane] = planes[plane].getRowStride(); strides[plane] = planes[plane].getPixelStride();
        }
        int[] pixels = new int[width * height];
        for (int row = 0; row < height; row++) {
            int sy = row * sourceHeight / height;
            for (int column = 0; column < width; column++) {
                int sx = column * sourceWidth / width;
                int y = Math.max(0, (data[0][sy * rows[0] + sx * strides[0]] & 255) - 16);
                int u = (data[1][sy / 2 * rows[1] + sx / 2 * strides[1]] & 255) - 128;
                int v = (data[2][sy / 2 * rows[2] + sx / 2 * strides[2]] & 255) - 128;
                int r = Math.max(0, Math.min(255, (298 * y + 409 * v + 128) >> 8));
                int g = Math.max(0, Math.min(255, (298 * y - 100 * u - 208 * v + 128) >> 8));
                int b = Math.max(0, Math.min(255, (298 * y + 516 * u + 128) >> 8));
                pixels[row * width + column] = 0xff000000 | r << 16 | g << 8 | b;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        Matrix rotation = new Matrix(); rotation.postRotate(orientation);
        Bitmap upright = Bitmap.createBitmap(bitmap, 0, 0, width, height, rotation, true);
        if (upright != bitmap) bitmap.recycle();
        return upright;
    }

    void close() {
        if (closed) return;
        closed = true;
        main.removeCallbacks(openDeadline);
        main.removeCallbacks(captureDeadline);
        displays.unregisterDisplayListener(displayListener);
        worker.post(() -> {
            pendingPreview = null;
            if (session != null) { session.close(); session = null; }
            if (reader != null) { reader.close(); reader = null; }
            if (previewSurface != null) { previewSurface.release(); previewSurface = null; }
            if (device != null) device.close();
            else if (!opening) completeClose();
        });
    }

    private void completeClose() {
        if (closeCompleted) return;
        closeCompleted = true;
        thread.quitSafely();
        main.post(listener::closed);
    }

    private void fail(String diagnostic) {
        main.post(() -> {
            if (closed) return;
            close();
            listener.failed(diagnostic);
        });
    }
    private void deliver(Runnable callback) { main.post(() -> { if (!closed) callback.run(); }); }
}
