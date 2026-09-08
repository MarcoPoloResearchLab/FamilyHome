package com.mprlab.portal;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;

final class AskSpeech {
    interface Listener { void unavailable(); }
    private TextToSpeech engine;
    private boolean ready;
    private boolean closed;
    AskSpeech(Context context, Listener listener) {
        engine = new TextToSpeech(context, status -> {
            if (closed) return;
            ready = status == TextToSpeech.SUCCESS && engine.setLanguage(Locale.US) >= TextToSpeech.LANG_AVAILABLE;
            if (!ready) listener.unavailable();
        });
        engine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { }
            @Override public void onDone(String id) { }
            @Override public void onError(String id) { if (!closed) listener.unavailable(); }
        });
    }
    boolean speak(String answer) {
        return ready && !closed && engine.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "portal-answer") == TextToSpeech.SUCCESS;
    }
    void stop() { engine.stop(); }
    void close() { closed = true; engine.stop(); engine.shutdown(); }
}
