package com.mprlab.portal;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** Six independent strings with a decaying, plucked harmonic spectrum. */
final class GuitarPlayer implements AutoCloseable {
    static final int[] OPEN_MIDI = {64, 59, 55, 50, 45, 40};
    static final int STRING_COUNT = 6;
    static final int FRET_COUNT = 5;
    private static final int SAMPLE_RATE = 22050;
    private static final int SAMPLE_COUNT = SAMPLE_RATE * 2;
    private static final int HARMONICS = 10;
    private final short[][][] notes = new short[STRING_COUNT][FRET_COUNT + 1][];
    private final AudioTrack[] tracks = new AudioTrack[STRING_COUNT];

    GuitarPlayer() {
        for (int string = 0; string < STRING_COUNT; string++) {
            for (int fret = 0; fret <= FRET_COUNT; fret++) notes[string][fret] = synthesize(OPEN_MIDI[string] + fret);
        }
    }
    void play(int string, int fret) {
        AudioTrack track = tracks[string];
        try {
            if (track == null) {
                track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(SAMPLE_COUNT * 2).setTransferMode(AudioTrack.MODE_STATIC).build();
                tracks[string] = track;
            }
            if (track.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) track.stop();
            short[] samples = notes[string][fret];
            int written = track.write(samples, 0, samples.length);
            if (written != samples.length || track.getState() != AudioTrack.STATE_INITIALIZED) {
                throw new IllegalStateException("Cannot load guitar string " + (string + 1) + ": " + written);
            }
            int result = track.setPlaybackHeadPosition(0);
            if (result != AudioTrack.SUCCESS) throw new IllegalStateException("Cannot restart guitar string " + (string + 1) + ": " + result);
            track.play();
        } catch (RuntimeException error) {
            if (track != null) track.release();
            tracks[string] = null;
            throw new IllegalStateException("Cannot play guitar string " + (string + 1) + ", fret " + fret, error);
        }
    }
    @Override public void close() {
        for (int string = 0; string < STRING_COUNT; string++) {
            if (tracks[string] != null) { tracks[string].release(); tracks[string] = null; }
        }
    }
    private short[] synthesize(int midi) {
        double frequency = 440 * Math.pow(2, (midi - 69) / 12.0);
        double[] previous = new double[HARMONICS], current = new double[HARMONICS];
        double[] first = new double[HARMONICS], second = new double[HARMONICS];
        double amplitudeSum = 0;
        for (int index = 0; index < HARMONICS; index++) {
            int harmonic = index + 1;
            double amplitude = 1 / Math.pow(harmonic, 1.4);
            double angle = 2 * Math.PI * frequency * harmonic / SAMPLE_RATE;
            double decay = Math.exp(-(1.7 + .65 * harmonic) / SAMPLE_RATE);
            first[index] = 2 * decay * Math.cos(angle); second[index] = -decay * decay;
            current[index] = amplitude * Math.sin(angle) * decay;
            amplitudeSum += amplitude;
        }
        short[] samples = new short[SAMPLE_COUNT];
        for (int sample = 0; sample < samples.length; sample++) {
            double value = 0;
            for (int harmonic = 0; harmonic < HARMONICS; harmonic++) {
                value += current[harmonic];
                double next = first[harmonic] * current[harmonic] + second[harmonic] * previous[harmonic];
                previous[harmonic] = current[harmonic]; current[harmonic] = next;
            }
            double envelope = Math.min(1, sample / (SAMPLE_RATE * .003))
                    * Math.min(1, (samples.length - 1 - sample) / (SAMPLE_RATE * .04));
            // Six full-scale voices remain below the mixer clipping limit.
            samples[sample] = (short) (Short.MAX_VALUE * .16 * envelope * value / amplitudeSum);
        }
        return samples;
    }
}
