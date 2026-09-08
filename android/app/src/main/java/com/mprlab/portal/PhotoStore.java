package com.mprlab.portal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** One atomic directory per saved photo. Only committed directories appear in albums. */
final class PhotoStore {
    static final String ROOT = "photos";
    static final String IMAGE = "picture.jpg";
    static final String THUMBNAIL = "thumbnail.jpg";
    static final String METADATA = "photo.json";
    static final long CHILD_LIMIT = 256L * 1024 * 1024;
    static final long TOTAL_LIMIT = 512L * 1024 * 1024;
    static final int ENTRY_LIMIT = 100;
    static final long RESULT_RESERVATION = 1200L * 3600 * 4 + 1024 * 1024;
    static final long DISK_MARGIN = 8L * 1024 * 1024;
    private static final Object LOCK = new Object();
    private static boolean initialized;
    private final File root;
    private final File temporary;

    static final class Entry {
        final String id, profileId, createdAt;
        final PhotoRenderer.Kind kind;
        final PhotoRenderer.Frame frame;
        final int width, height;
        final long bytes;
        final File directory;
        Entry(File directory, JSONObject json, long bytes) throws JSONException, IOException {
            this.directory = directory; this.bytes = bytes;
            if (json.length() != 8) throw new IOException("Invalid photo metadata fields");
            id = json.getString("id"); profileId = json.getString("profile_id"); createdAt = json.getString("created_at");
            kind = PhotoRenderer.Kind.valueOf(json.getString("kind"));
            frame = PhotoRenderer.Frame.valueOf(json.getString("frame_id"));
            width = json.getInt("width"); height = json.getInt("height");
            if (!id.equals(directory.getName()) || profileId.isEmpty() || width < 1 || height < 1
                    || width > 1600 || height > 3600 || !new File(directory, IMAGE).isFile()
                    || !new File(directory, THUMBNAIL).isFile()
                    || json.getLong("bytes") != new File(directory, IMAGE).length() + new File(directory, THUMBNAIL).length())
                throw new IOException("Invalid photo record " + directory.getName());
        }
        File image() { return new File(directory, IMAGE); }
        File thumbnail() { return new File(directory, THUMBNAIL); }
    }

    PhotoStore(Context context) throws IOException {
        root = new File(context.getFilesDir(), ROOT);
        temporary = new File(root, "temporary");
        synchronized (LOCK) {
            directory(root); directory(temporary);
            if (!initialized) { discardTemporary(); initialized = true; }
        }
    }

    void discardTemporary() throws IOException {
        synchronized (LOCK) { for (File file : children(temporary)) remove(file); }
    }

    void discardSequence(String sequence) throws IOException {
        if (sequence == null) return;
        synchronized (LOCK) {
            for (File file : children(temporary))
                if (file.getName().equals(sequence) || file.getName().startsWith(sequence + "-")) remove(file);
        }
    }

    List<Entry> list(String profile) throws IOException {
        synchronized (LOCK) {
            List<Entry> entries = new ArrayList<>();
            for (Entry entry : all()) if (entry.profileId.equals(profile)) entries.add(entry);
            entries.sort(Comparator.comparing((Entry entry) -> entry.createdAt).reversed().thenComparing(entry -> entry.id));
            return entries;
        }
    }

    void reserve(String profile, int count, long captureByteLimit) throws IOException {
        synchronized (LOCK) {
            capacity(profile, RESULT_RESERVATION);
            long temporaryBytes = captureByteLimit * count;
            if (root.getUsableSpace() < temporaryBytes + RESULT_RESERVATION + DISK_MARGIN)
                throw new IOException("Photo storage is full. Delete a saved picture before taking another.");
        }
    }

    File writeCapture(String sequence, int index, byte[] bytes) throws IOException {
        synchronized (LOCK) {
            if (bytes.length == 0 || bytes.length > PortalCamera.MAX_JPEG_BYTES)
                throw new IOException("Camera image byte limit exceeded");
            File file = new File(temporary, sequence + "-" + index + ".jpg");
            write(file, bytes); return file;
        }
    }

    Entry save(String id, String profile, PhotoRenderer.Kind kind, PhotoRenderer.Frame frame, Bitmap bitmap) throws IOException {
        synchronized (LOCK) {
            File destination = new File(root, id);
            if (destination.isDirectory()) {
                Entry existing = read(destination);
                if (!existing.profileId.equals(profile)) throw new IOException("Photo belongs to another profile");
                return existing;
            }
            File staging = new File(temporary, id);
            directory(staging);
            try {
                compress(new File(staging, IMAGE), bitmap);
                float scale = Math.min(1f, 240f / Math.max(bitmap.getWidth(), bitmap.getHeight()));
                Bitmap thumbnail = Bitmap.createScaledBitmap(bitmap, Math.max(1, (int) (bitmap.getWidth() * scale)),
                        Math.max(1, (int) (bitmap.getHeight() * scale)), true);
                try { compress(new File(staging, THUMBNAIL), thumbnail); }
                finally { if (thumbnail != bitmap) thumbnail.recycle(); }
                long imageBytes = new File(staging, IMAGE).length() + new File(staging, THUMBNAIL).length();
                JSONObject json = new JSONObject();
                json.put("id", id); json.put("profile_id", profile); json.put("created_at", Instant.now().toString());
                json.put("kind", kind.name()); json.put("frame_id", frame.name());
                json.put("width", bitmap.getWidth()); json.put("height", bitmap.getHeight()); json.put("bytes", imageBytes);
                byte[] metadata = json.toString().getBytes(StandardCharsets.UTF_8);
                capacity(profile, imageBytes + metadata.length);
                write(new File(staging, METADATA), metadata);
                if (!staging.renameTo(destination)) throw new IOException("Commit saved photo failed");
                return read(destination);
            } catch (JSONException error) { throw new IOException("Write photo metadata failed", error); }
            finally { if (staging.exists()) remove(staging); }
        }
    }

    void delete(Entry entry, String profile) throws IOException {
        synchronized (LOCK) {
            if (!entry.profileId.equals(profile)) throw new IOException("Photo belongs to another profile");
            if (!entry.directory.exists()) return;
            File deleting = new File(temporary, "delete-" + entry.id);
            if (!entry.directory.renameTo(deleting)) throw new IOException("Remove photo album entry failed");
            remove(deleting);
        }
    }

    private void capacity(String profile, long additional) throws IOException {
        long total = 0, child = 0; int count = 0;
        for (Entry entry : all()) {
            total += entry.bytes;
            if (entry.profileId.equals(profile)) { child += entry.bytes; count++; }
        }
        if (count >= ENTRY_LIMIT || child + additional > CHILD_LIMIT || total + additional > TOTAL_LIMIT)
            throw new IOException("Photo album is full. Delete a saved picture before taking another.");
    }
    private List<Entry> all() throws IOException {
        List<Entry> result = new ArrayList<>();
        for (File file : children(root)) {
            if (file.equals(temporary)) continue;
            if (!file.isDirectory()) throw new IOException("Unexpected photo storage entry " + file.getName());
            result.add(read(file));
        }
        return result;
    }
    private Entry read(File directory) throws IOException {
        File metadata = new File(directory, METADATA);
        if (metadata.length() > 8192) throw new IOException("Photo metadata exceeds size limit");
        try {
            JSONObject json = new JSONObject(new String(Files.readAllBytes(metadata.toPath()), StandardCharsets.UTF_8));
            long bytes = 0;
            for (File file : children(directory)) bytes += file.length();
            Entry entry = new Entry(directory, json, bytes);
            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(entry.image().getPath(), bounds);
            if (bounds.outWidth != entry.width || bounds.outHeight != entry.height)
                throw new IOException("Saved photo dimensions differ from its metadata");
            BitmapFactory.decodeFile(entry.thumbnail().getPath(), bounds);
            if (bounds.outWidth < 1 || bounds.outHeight < 1 || bounds.outWidth > 240 || bounds.outHeight > 240)
                throw new IOException("Saved photo thumbnail dimensions are invalid");
            Instant.parse(entry.createdAt);
            return entry;
        } catch (JSONException | RuntimeException error) { throw new IOException("Read photo " + directory.getName() + " failed", error); }
    }
    private static void directory(File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Create photo directory failed");
    }
    private static File[] children(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) throw new IOException("Read photo directory failed");
        return files;
    }
    private static void remove(File file) throws IOException {
        if (file.isDirectory()) for (File child : children(file)) remove(child);
        if (!file.delete()) throw new IOException("Remove temporary photo data failed");
    }
    private static void write(File file, byte[] bytes) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) { output.write(bytes); output.getFD().sync(); }
    }
    private static void compress(File file, Bitmap bitmap) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, PortalCamera.JPEG_QUALITY, output))
                throw new IOException("Encode saved photo failed");
            output.getFD().sync();
        }
    }
}
