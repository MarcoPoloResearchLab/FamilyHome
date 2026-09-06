package com.mprlab.portal;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
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
            return "Camera " + cameraId + " · preview " + previewSize + " · JPEG " + imageSize
                    + " · sensor " + sensorOrientation + " · facing " + lensFacing + " · level " + hardwareLevel;
        }
    }

    private final Context context;
    private final TextureView preview;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final HandlerThread thread = new HandlerThread("PhotoBoothCamera");
    private final Handler worker;
    private volatile boolean closed;
    private boolean started;
    private boolean opening;
    private boolean closeCompleted;
    private boolean ready;
    private boolean capturing;
    private Configuration configuration;
    private CameraDevice device;
    private CameraCaptureSession session;
    private ImageReader reader;
    private Surface previewSurface;

    PortalCamera(Context context, TextureView preview, Listener listener) {
        this.context = context; this.preview = preview; this.listener = listener;
        thread.start(); worker = new Handler(thread.getLooper());
    }

    void open() {
        if (started || closed) throw new IllegalStateException("Camera session cannot open twice");
        started = true;
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
            previewSurface = new Surface(texture);
            reader = ImageReader.newInstance(configuration.imageSize.getWidth(), configuration.imageSize.getHeight(),
                    ImageFormat.JPEG, 2);
            reader.setOnImageAvailableListener(source -> {
                try (Image image = source.acquireNextImage()) {
                    if (image == null || closed) return;
                    if (!capturing) throw new IllegalStateException("Camera delivered an unrequested JPEG");
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    if (buffer.remaining() == 0 || buffer.remaining() > MAX_JPEG_BYTES)
                        throw new IllegalStateException("Camera JPEG exceeds the image byte limit");
                    byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes);
                    capturing = false;
                    deliver(() -> listener.picture(bytes));
                } catch (RuntimeException error) { fail("Read JPEG from camera " + configuration.cameraId + ": " + error); }
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
            Size image = largest(streams.getOutputSizes(ImageFormat.JPEG), MAX_IMAGE_SIDE, MAX_IMAGE_SIDE, null);
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
                        session.setRepeatingRequest(request.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override public void onCaptureCompleted(CameraCaptureSession source, CaptureRequest request, TotalCaptureResult result) {
                                if (!ready && !closed) { ready = true; deliver(() -> listener.ready(configuration)); }
                            }
                            @Override public void onCaptureFailed(CameraCaptureSession source, CaptureRequest request, CaptureFailure failure) {
                                fail("Camera preview failed: reason " + failure.getReason());
                            }
                        }, worker);
                    } catch (CameraAccessException | RuntimeException error) { fail("Start camera preview: " + error); }
                }
                @Override public void onConfigureFailed(CameraCaptureSession configured) {
                    configured.close(); fail("Configure camera " + configuration.cameraId + " preview and JPEG session failed");
                }
            }, worker);
        } catch (CameraAccessException | RuntimeException error) { fail("Create camera session: " + error); }
    }

    void capture() {
        if (closed) throw new IllegalStateException("Capture after camera close");
        int orientation = imageRotation();
        worker.post(() -> {
            if (closed) return;
            if (!ready || capturing) { fail("Capture camera picture: camera is not ready"); return; }
            capturing = true;
            try {
                CaptureRequest.Builder request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                request.addTarget(reader.getSurface());
                request.set(CaptureRequest.JPEG_ORIENTATION, orientation);
                request.set(CaptureRequest.JPEG_QUALITY, (byte) JPEG_QUALITY);
                session.capture(request.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override public void onCaptureFailed(CameraCaptureSession source, CaptureRequest request, CaptureFailure failure) {
                        fail("Capture JPEG failed: reason " + failure.getReason());
                    }
                }, worker);
            } catch (CameraAccessException | RuntimeException error) { fail("Capture JPEG: " + error); }
        });
    }

    void close() {
        if (closed) return;
        closed = true;
        worker.post(() -> {
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
