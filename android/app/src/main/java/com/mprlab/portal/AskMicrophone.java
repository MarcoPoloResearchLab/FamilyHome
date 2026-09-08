package com.mprlab.portal;

import android.media.MediaRecorder;
import java.io.File;
import java.io.IOException;

final class AskMicrophone {
    private final File directory;
    private MediaRecorder recorder;
    private File recording;
    AskMicrophone(File directory) { this.directory = directory; }

    void removeAbandoned() throws IOException {
        File[] files = directory.listFiles((dir, name) -> name.startsWith("portal-question"));
        if (files == null) throw new IOException("Temporary recordings could not be checked.");
        for (File file : files) remove(file);
    }
    void start(AskClient.Settings settings, Runnable limitReached) throws IOException {
        recording = File.createTempFile("portal-question-", ".m4a", directory);
        recorder = new MediaRecorder();
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(96000); recorder.setAudioSamplingRate(44100);
            recorder.setMaxDuration(settings.recordingSeconds*1000); recorder.setMaxFileSize(settings.audioBytes);
            recorder.setOnInfoListener((source, what, extra) -> {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) limitReached.run();
            });
            recorder.setOutputFile(recording.getAbsolutePath()); recorder.prepare(); recorder.start();
        } catch (IOException | RuntimeException error) {
            discard(); throw new IOException("The microphone could not start.");
        }
    }
    File finish() throws IOException {
        try { recorder.stop(); }
        catch (RuntimeException error) { discard(); throw new IOException("No voice question was recorded."); }
        recorder.release(); recorder = null;
        if (recording == null || recording.length() == 0) { discard(); throw new IOException("No voice question was recorded."); }
        return recording;
    }
    void discard() throws IOException {
        if (recorder != null) { recorder.release(); recorder = null; }
        if (recording != null) { remove(recording); recording = null; }
    }
    private static void remove(File file) throws IOException {
        if (file.exists() && !file.delete()) throw new IOException("The temporary recording could not be removed.");
    }
}
