package com.mprlab.portal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;
import android.widget.Toast;

public final class TimerAlarmReceiver extends BroadcastReceiver {
    private static final int SAMPLE_RATE = 24000;
    private static final double[] NOTES = {523.25, 659.25, 783.99, 1046.50};
    private static final double NOTE_SPACING = 0.34;
    private static final double NOTE_LENGTH = 1.45;
    private static final double CHIME_LENGTH = 2.6;

    @Override public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Time is up! Nice job!", Toast.LENGTH_LONG).show();
        PendingResult delivery = goAsync();
        new Thread(() -> {
            AudioTrack track = null;
            try {
                short[] samples = chimeSamples();
                track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                        .setAudioFormat(new AudioFormat.Builder().setSampleRate(SAMPLE_RATE)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .setBufferSizeInBytes(SAMPLE_RATE / 5).build();
                track.play();
                int offset = 0;
                while (offset < samples.length) {
                    int written = track.write(samples, offset, samples.length - offset, AudioTrack.WRITE_BLOCKING);
                    if (written <= 0) throw new IllegalStateException("Timer chime write failed: " + written);
                    offset += written;
                }
                long deadline = android.os.SystemClock.elapsedRealtime() + 1000L;
                while (track.getPlaybackHeadPosition() < samples.length) {
                    if (android.os.SystemClock.elapsedRealtime() > deadline) {
                        throw new IllegalStateException("Timer chime playback stalled");
                    }
                    Thread.sleep(20L);
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                Log.e("FamilyHomeTimer", "Timer chime interrupted", error);
            } catch (RuntimeException error) {
                Log.e("FamilyHomeTimer", "Cannot play timer chime", error);
            } finally {
                if (track != null) track.release();
                delivery.finish();
            }
        }, "timer-chime").start();
    }

    private static short[] chimeSamples() {
        short[] samples = new short[(int) (SAMPLE_RATE * CHIME_LENGTH)];
        for (int frame = 0; frame < samples.length; frame++) {
            double time = (double) frame / SAMPLE_RATE;
            double value = 0;
            for (int note = 0; note < NOTES.length; note++) {
                double age = time - note * NOTE_SPACING;
                if (age < 0 || age >= NOTE_LENGTH) continue;
                double attack = Math.min(1, age / 0.045);
                double release = Math.min(1, (NOTE_LENGTH - age) / 0.25);
                double envelope = attack * release * Math.exp(-3.8 * age);
                double phase = 2 * Math.PI * NOTES[note] * age;
                value += 0.12 * envelope * (Math.sin(phase) + 0.15 * Math.sin(2 * phase));
            }
            samples[frame] = (short) (value * Short.MAX_VALUE);
        }
        return samples;
    }
}
