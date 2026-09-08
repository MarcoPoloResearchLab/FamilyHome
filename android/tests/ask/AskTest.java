package com.mprlab.portal.asktest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.graphics.Rect;
import android.widget.EditText;
import android.widget.TextView;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class AskTest extends Instrumentation {
    private final AtomicInteger requests = new AtomicInteger();
    private volatile String response = "{\"answer\":\"The sky scatters blue light.\"}";
    private volatile CountDownLatch release = new CountDownLatch(0);
    private volatile Throwable serverFailure;
    private volatile byte[] audioBody;
    private volatile String typedBody;
    private volatile String transcriptResponse = "{\"transcript\":\"Why is the sky blue?\"}";
    private boolean denied;
    private boolean profileOnly;
    private volatile int timeoutSeconds = 12;

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); denied = "denied".equals(arguments.getString("phase")); profileOnly = "profile".equals(arguments.getString("phase")); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try (ServerSocket server = new ServerSocket(18765)) {
            Thread provider = new Thread(() -> {
                while (!server.isClosed()) {
                    try {
                        Socket socket = server.accept();
                        new Thread(() -> serve(socket)).start();
                    } catch (IOException error) { if (!server.isClosed()) serverFailure = error; }
                }
            });
            provider.setDaemon(true); provider.start();
            getTargetContext().getSharedPreferences("children_portal", Context.MODE_PRIVATE).edit()
                    .putString("profiles_json", "[{\"id\":\"alice\",\"name\":\"Alice\"}]")
                    .putString("active_profile_id", "alice").commit();
            if (denied) microphoneDenied();
            else if (profileOnly) profileChange();
            else {
                try (OutputStream orphan = new FileOutputStream(new File(getTargetContext().getCacheDir(), "portal-question-orphan.m4a"))) { orphan.write(1); }
                typedAndDuplicate();
                scrollingAnswer();
                assertNoRecordings();
                invalidAnswer();
                lateResponse();
                profileChange();
                deadline();
                recording();
                transcriptionCancellation();
                screensaver();
            }
            if (serverFailure != null) throw new AssertionError(serverFailure);
            result.putString("stream", "Ask passed: typed HTTP, duplicate prevention, malformed answer, draft preservation, cancellation, recording upload, cleanup, and controls.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "Ask failed: " + error + "\n"); finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void serve(Socket socket) {
        try (Socket connection = socket) {
            connection.setSoTimeout(10000);
            InputStream input = connection.getInputStream();
            String request = line(input); int length = 0; boolean authorized = false;
            for (String header = line(input); !header.isEmpty(); header = line(input)) {
                if (header.toLowerCase().startsWith("content-length:")) length = Integer.parseInt(header.substring(15).trim());
                if (header.equals("Authorization: Bearer familyhome-ask-test-token-000000")) authorized = true;
            }
            if (!authorized) throw new AssertionError("Missing device authorization");
            byte[] body = new byte[length]; int offset = 0;
            while (offset < length) { int count = input.read(body, offset, length-offset); if (count < 0) throw new EOFException(); offset += count; }
            String value;
            if (request.startsWith("GET /v1/ask/settings ")) {
                value = "{\"request_timeout_seconds\":"+timeoutSeconds+",\"question_character_limit\":2000,\"recording_duration_seconds\":60,\"audio_byte_limit\":6291456}";
            } else {
                if (request.startsWith("POST /v1/ask/transcriptions ")) { audioBody = body; value = transcriptResponse; }
                else if (request.startsWith("POST /v1/ask ")) { typedBody = new String(body,StandardCharsets.UTF_8); requests.incrementAndGet(); value = response; }
                else throw new AssertionError(request);
                CountDownLatch gate = release;
                if (!gate.await(10, TimeUnit.SECONDS)) throw new AssertionError("Question gate did not open");
            }
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            connection.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "+bytes.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            connection.getOutputStream().write(bytes);
        } catch (SocketException ignored) { /* The cancellation scenario closes its socket. */ }
        catch (Throwable error) { serverFailure = error; }
    }
    private String line(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int value; (value = input.read()) != -1;) { if (value == '\n') return bytes.toString("UTF-8").replace("\r", ""); bytes.write(value); }
        throw new EOFException();
    }
    private Activity open() throws Exception {
        Activity activity = startActivitySync(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal.AskActivity")
                .putExtra("profile_id", "alice").putExtra("profile_name", "Alice").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await(() -> text(activity, "Connecting to Ask…") == null);
        runOnMainSync(() -> assertPromptControls(activity));
        return activity;
    }
    private void assertPromptControls(Activity activity) {
        View microphone = control(activity, "Record question"), send = control(activity, "Send question");
        EditText input = edit(activity.getWindow().getDecorView());
        String guidance = "Type a question or tap the microphone. Tap the paper airplane to send.";
        if (!guidance.contentEquals(input.getHint()))
            throw new AssertionError("Ask instructions must appear in the input placeholder");
        if (text(activity,guidance) != null)
            throw new AssertionError("Separate instruction panel must not be present");
        if (!(microphone instanceof ImageButton) || !(send instanceof ImageButton))
            throw new AssertionError("Question field requires microphone and paper airplane icon buttons");
        if (text(activity, "Type & ask") != null || text(activity, "Talk to ask") != null)
            throw new AssertionError("Separate bottom question buttons remain");
        if (text(activity,"Stop speaking") != null || text(activity,"Start speaking") != null)
            throw new AssertionError("Speech playback buttons must not be present");
        if (input.getParent() != microphone.getParent() || microphone.getParent() != send.getParent())
            throw new AssertionError("Question and both icons must share the input surface");
        if (((ImageButton)microphone).getDrawable() == null || ((ImageButton)send).getDrawable() == null)
            throw new AssertionError("Missing microphone or send illustration");
        Rect field = new Rect(), mic = new Rect(), plane = new Rect();
        if (!input.getGlobalVisibleRect(field) || !microphone.getGlobalVisibleRect(mic) || !send.getGlobalVisibleRect(plane)
                || field.right > mic.left || mic.right > plane.left || mic.top != plane.top)
            throw new AssertionError("Input icons overlap or have incorrect order");
        int minimum = Math.round(64 * activity.getResources().getDisplayMetrics().density);
        if (mic.width() < minimum || mic.height() < minimum || plane.width() < minimum || plane.height() < minimum)
            throw new AssertionError("Question icons require 64 dp touch targets");
    }
    private View control(Activity activity, String label) { return control(activity.getWindow().getDecorView(), label); }
    private View control(View view, String label) {
        if (label.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) for (int i=0;i<((ViewGroup)view).getChildCount();i++) {
            View found = control(((ViewGroup)view).getChildAt(i), label); if (found != null) return found;
        }
        return null;
    }
    private void close(Activity activity) throws Exception { runOnMainSync(activity::finish); waitForIdleSync(); await(activity::isDestroyed); }
    private void typedAndDuplicate() throws Exception {
        Activity activity = open(); int before = requests.get(); release = new CountDownLatch(1);
        try {
            capture(activity, "ready");
            assertIconPressAndCancel(activity,"Record question","microphone-pressed",0xff000000);
            assertIconPressAndCancel(activity,"Send question","send-pressed",0xff000000);
            runOnMainSync(() -> {
                edit(activity.getWindow().getDecorView()).setText("Why is the sky blue?");
                control(activity, "Send question").performClick();
                if (control(activity, "Send question").isEnabled() || control(activity, "Record question").isEnabled()) throw new AssertionError("Submissions remain enabled while busy");
                control(activity, "Send question").performClick();
            });
            await(() -> requests.get() == before+1); capture(activity, "thinking"); release.countDown();
            await(() -> text(activity, "The sky scatters blue light.") != null);
            if (requests.get() != before+1) throw new AssertionError("Duplicate question");
            runOnMainSync(() -> {
                if (text(activity,"The sky scatters blue light.").getBackground() != null)
                    throw new AssertionError("Answers must use plain text without a colored panel");
                if (text(activity,"Stop speaking") != null || text(activity,"Start speaking") != null)
                    throw new AssertionError("Speech playback buttons must not be present");
            });
            capture(activity, "answer");
        } finally { release.countDown(); close(activity); }
    }
    private void invalidAnswer() throws Exception {
        response = "{\"answer\":42}"; Activity activity = open();
        try {
            runOnMainSync(() -> { edit(activity.getWindow().getDecorView()).setText("Keep this draft"); control(activity, "Send question").performClick(); });
            await(() -> text(activity, "Ask returned an invalid answer.") != null);
            runOnMainSync(() -> { if (!edit(activity.getWindow().getDecorView()).getText().toString().equals("Keep this draft")) throw new AssertionError("Draft lost"); });
            capture(activity, "error");
        } finally { close(activity); response = "{\"answer\":\"The sky scatters blue light.\"}"; }
    }
    private void scrollingAnswer() throws Exception {
        StringBuilder longAnswer = new StringBuilder();
        for (int i=0;i<50;i++) longAnswer.append("A long answer still leaves room for the question controls. ");
        response = new org.json.JSONObject().put("answer",longAnswer.toString()).toString();
        Activity activity = open();
        try {
            runOnMainSync(() -> {
                edit(activity.getWindow().getDecorView()).setText("Explain the sky.\nExplain the clouds.\nExplain the rain.\nExplain the rainbow.");
                assertPromptControls(activity);
                control(activity,"Send question").performClick();
            });
            await(() -> text(activity,longAnswer.toString().trim()) != null);
            runOnMainSync(() -> {
                scroll(activity.getWindow().getDecorView()).fullScroll(View.FOCUS_DOWN);
            });
            waitForIdleSync();
            runOnMainSync(() -> assertPromptControls(activity));
            capture(activity,"composer");
        } finally { close(activity); response = "{\"answer\":\"The sky scatters blue light.\"}"; }
    }
    private ScrollView scroll(View view) {
        if (view instanceof ScrollView) return (ScrollView)view;
        if (view instanceof ViewGroup) for (int i=0;i<((ViewGroup)view).getChildCount();i++) {
            ScrollView found = scroll(((ViewGroup)view).getChildAt(i)); if (found != null) return found;
        }
        return null;
    }
    private void lateResponse() throws Exception {
        Activity activity = open(); int before = requests.get(); release = new CountDownLatch(1);
        runOnMainSync(() -> { edit(activity.getWindow().getDecorView()).setText("Late question"); control(activity, "Send question").performClick(); });
        await(() -> requests.get() == before+1);
        close(activity); release.countDown();
        Thread.sleep(250);
        runOnMainSync(() -> { if (text(activity, "The sky scatters blue light.") != null) throw new AssertionError("Late answer appeared after exit"); });
    }
    private void recording() throws Exception {
        Activity activity = open(); int before = requests.get();
        try {
            runOnMainSync(() -> edit(activity.getWindow().getDecorView()).setText("Please explain."));
            tapIcon(activity,"Record question");
            await(() -> control(activity,"Stop recording") != null); capture(activity,"recording");
            runOnMainSync(() -> assertStopAlignment(activity));
            assertIconPressAndCancel(activity,"Stop recording","stop-pressed",0xffe53935);
            Thread.sleep(1000);
            tapIcon(activity,"Stop recording");
            await(() -> edit(activity.getWindow().getDecorView()).getText().toString().equals("Please explain. Why is the sky blue?"));
            if (requests.get() != before) throw new AssertionError("Stop submitted an answer request");
            if (audioBody == null || !new String(audioBody,StandardCharsets.ISO_8859_1).contains("ftyp")) throw new AssertionError("No M4A recording in transcription upload");
            assertNoRecordings(); capture(activity,"transcript");
            runOnMainSync(() -> control(activity,"Send question").performClick());
            await(() -> text(activity,"The sky scatters blue light.") != null);
            if (!new org.json.JSONObject(typedBody).getString("question").equals("Please explain. Why is the sky blue?")) throw new AssertionError("Typed words or transcript lost");
            runOnMainSync(() -> { edit(activity.getWindow().getDecorView()).setText("Another question."); control(activity,"Record question").performClick(); });
            await(() -> control(activity,"Stop recording") != null); Thread.sleep(1000);
            runOnMainSync(() -> control(activity,"Send question").performClick());
            await(() -> requests.get() == before+2 && text(activity,"The sky scatters blue light.") != null);
            if (!new org.json.JSONObject(typedBody).getString("question").equals("Another question. Why is the sky blue?")) throw new AssertionError("Send omitted active speech");
            assertNoRecordings();
            transcriptResponse = "{\"transcript\":42}";
            runOnMainSync(() -> { edit(activity.getWindow().getDecorView()).setText("Keep typed text"); control(activity,"Record question").performClick(); });
            await(() -> control(activity,"Stop recording") != null); Thread.sleep(1000);
            runOnMainSync(() -> control(activity,"Send question").performClick());
            await(() -> text(activity,"The recording could not be transcribed. Please try again.") != null);
            if (requests.get() != before+2 || !edit(activity.getWindow().getDecorView()).getText().toString().equals("Keep typed text")) throw new AssertionError("Failed transcription submitted or erased text");
            assertNoRecordings();
        } finally { transcriptResponse = "{\"transcript\":\"Why is the sky blue?\"}"; close(activity); }
    }
    private void assertIconPressAndCancel(Activity activity,String label,String screenshot,int color) throws Exception {
        View icon = control(activity,label);
        Rect[] bounds = new Rect[3];
        runOnMainSync(() -> {
            bounds[0] = iconPixels(icon,color);
            touch(icon,android.view.MotionEvent.ACTION_DOWN);
            if (!icon.isPressed()) throw new AssertionError(label+" did not enter the pressed state");
            bounds[1] = iconPixels(icon,color);
            int shadow = Math.round(4*activity.getResources().getDisplayMetrics().density);
            Rect expected = new Rect(bounds[0]); expected.offset(shadow,shadow);
            if (!expected.equals(bounds[1])) throw new AssertionError(label+" icon did not move with its pressed button face");
        });
        capture(activity,screenshot);
        runOnMainSync(() -> {
            touch(icon,android.view.MotionEvent.ACTION_CANCEL);
            bounds[2] = iconPixels(icon,color);
            if (icon.isPressed() || !bounds[0].equals(bounds[2]))
                throw new AssertionError(label+" icon did not return when the press ended");
        });
    }
    private void tapIcon(Activity activity,String label) throws Exception {
        View icon=control(activity,label);
        runOnMainSync(() -> { touch(icon,android.view.MotionEvent.ACTION_DOWN); touch(icon,android.view.MotionEvent.ACTION_UP); });
        await(() -> !icon.isPressed());
    }
    private void touch(View view,int action) {
        long now = android.os.SystemClock.uptimeMillis();
        android.view.MotionEvent event = android.view.MotionEvent.obtain(now,now,action,view.getWidth()/2f,view.getHeight()/2f,0);
        try { view.dispatchTouchEvent(event); } finally { event.recycle(); }
    }
    private Rect iconPixels(View view,int color) {
        Bitmap pixels = Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);
        view.draw(new android.graphics.Canvas(pixels));
        int left=pixels.getWidth(),top=pixels.getHeight(),right=-1,bottom=-1;
        float density=view.getResources().getDisplayMetrics().density;
        int start=Math.round(16*density),end=Math.round(12*density);
        for (int y=start;y<pixels.getHeight()-end;y++) for (int x=start;x<pixels.getWidth()-end;x++) {
            if (pixels.getPixel(x,y)==color) {
                left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
            }
        }
        pixels.recycle();
        if (right<left) throw new AssertionError("Prompt icon was not drawn");
        return new Rect(left,top,right+1,bottom+1);
    }
    private void assertStopAlignment(Activity activity) {
        View stop = control(activity,"Stop recording"), send = control(activity,"Send question");
        Rect stopBounds = new Rect(), sendBounds = new Rect();
        stop.getGlobalVisibleRect(stopBounds); send.getGlobalVisibleRect(sendBounds);
        if (stopBounds.top != sendBounds.top || stopBounds.bottom != sendBounds.bottom)
            throw new AssertionError("Stop and send buttons must align");
        Bitmap pixels = Bitmap.createBitmap(stop.getWidth(),stop.getHeight(),Bitmap.Config.ARGB_8888);
        stop.draw(new android.graphics.Canvas(pixels));
        int left = pixels.getWidth(), top = pixels.getHeight(), right = -1, bottom = -1;
        for (int y=0;y<pixels.getHeight();y++) for (int x=0;x<pixels.getWidth();x++) {
            if (pixels.getPixel(x,y) == 0xffe53935) {
                left = Math.min(left,x); right = Math.max(right,x);
                top = Math.min(top,y); bottom = Math.max(bottom,y);
            }
        }
        pixels.recycle();
        float shadow = Math.round(4 * activity.getResources().getDisplayMetrics().density);
        if (right < left || Math.abs((left+right+1)/2f - (stop.getWidth()-shadow)/2f) > .75f
                || Math.abs((top+bottom+1)/2f - (stop.getHeight()-shadow)/2f) > .75f)
            throw new AssertionError("Red stop square must be centered on the button face, excluding its shadow");
    }
    private void deadline() throws Exception {
        timeoutSeconds = 1; Activity activity = open(); release = new CountDownLatch(1);
        try {
            runOnMainSync(() -> { edit(activity.getWindow().getDecorView()).setText("Slow question"); control(activity,"Send question").performClick(); });
            await(() -> text(activity,"The connection stopped. Your question may still be processing.") != null);
        } finally { release.countDown(); close(activity); timeoutSeconds = 12; }
    }
    private void transcriptionCancellation() throws Exception {
        Activity activity = open(); int before = requests.get(); audioBody = null;
        release = new CountDownLatch(1);
        try {
            runOnMainSync(() -> { edit(activity.getWindow().getDecorView()).setText("Keep this draft"); control(activity,"Record question").performClick(); });
            await(() -> control(activity,"Stop recording") != null); Thread.sleep(1000);
            runOnMainSync(() -> control(activity,"Send question").performClick());
            await(() -> audioBody != null);
            runOnMainSync(() -> text(activity,"Cancel").performClick());
            release.countDown(); Thread.sleep(300);
            runOnMainSync(() -> {
                if (!edit(activity.getWindow().getDecorView()).getText().toString().equals("Keep this draft")) throw new AssertionError("Cancelled transcription changed draft");
            });
            if (requests.get() != before) throw new AssertionError("Cancelled transcription sent a question");
            assertNoRecordings();
        } finally { release.countDown(); close(activity); }
    }
    private void profileChange() throws Exception {
        Activity activity = open();
        try {
            runOnMainSync(() -> control(activity,"Record question").performClick());
            await(() -> control(activity,"Stop recording") != null);
            getTargetContext().getSharedPreferences("children_portal",Context.MODE_PRIVATE).edit().putString("active_profile_id","bob").commit();
            await(activity::isDestroyed);
            assertNoRecordings();
        } finally {
            close(activity);
            getTargetContext().getSharedPreferences("children_portal",Context.MODE_PRIVATE).edit().putString("active_profile_id","alice").commit();
        }
    }
    private void microphoneDenied() throws Exception {
        Activity activity = open();
        try {
            runOnMainSync(() -> control(activity,"Record question").performClick());
            await(() -> text(activity,"Microphone access was denied. You can type your question.") != null);
            assertNoRecordings();
            runOnMainSync(() -> {
                if (!control(activity,"Send question").isEnabled()) throw new AssertionError("Microphone denial disabled typed questions");
            });
        } finally { close(activity); }
    }
    private void screensaver() throws Exception {
        getTargetContext().getSharedPreferences("screensaver",Context.MODE_PRIVATE).edit()
                .putString("mode","BLACK").putString("timeout","THIRTY_SECONDS").commit();
        Activity activity = open(); int before = requests.get();
        try {
            runOnMainSync(() -> control(activity,"Record question").performClick());
            await(() -> control(activity,"Stop recording") != null);
            await(() -> text(activity,"Question cancelled.") != null,40000);
            assertNoRecordings();
            if (requests.get() != before) throw new AssertionError("Screensaver submitted a recording");
        } finally {
            close(activity);
            getTargetContext().getSharedPreferences("screensaver",Context.MODE_PRIVATE).edit()
                    .putString("timeout","FIVE_MINUTES").commit();
        }
    }
    private void assertNoRecordings() {
        File[] recordings = getTargetContext().getCacheDir().listFiles((dir, name) -> name.startsWith("portal-question"));
        if (recordings == null || recordings.length != 0) throw new AssertionError("Temporary recording remains");
    }
    private interface Check { boolean ready(); }
    private void await(Check condition) throws Exception {
        await(condition,10000);
    }
    private void await(Check condition,long timeout) throws Exception {
        for (long end = android.os.SystemClock.uptimeMillis()+timeout; android.os.SystemClock.uptimeMillis()<end;) {
            boolean[] ready = {false}; runOnMainSync(() -> ready[0] = condition.ready()); if (ready[0]) return; Thread.sleep(50);
        }
        throw new AssertionError("Expected Ask state did not arrive");
    }
    private TextView text(Activity activity, String value) { return text(activity.getWindow().getDecorView(), value); }
    private TextView text(View view, String value) {
        if (view instanceof TextView && ((TextView)view).getText().toString().equals(value)) return (TextView)view;
        if (view instanceof ViewGroup) for (int i=0;i<((ViewGroup)view).getChildCount();i++) { TextView found=text(((ViewGroup)view).getChildAt(i),value); if(found!=null)return found; }
        return null;
    }
    private EditText edit(View view) {
        if(view instanceof EditText)return (EditText)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){EditText found=edit(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}
        return null;
    }
    private void capture(Activity activity, String name) throws Exception {
        waitForIdleSync();
        CountDownLatch frames = new CountDownLatch(1);
        runOnMainSync(() -> activity.getWindow().getDecorView().postOnAnimation(
                () -> activity.getWindow().getDecorView().postOnAnimation(frames::countDown)));
        if (!frames.await(2,TimeUnit.SECONDS)) throw new AssertionError("Ask frame did not render");
        Bitmap bitmap = getUiAutomation().takeScreenshot();
        try (OutputStream output = getTargetContext().openFileOutput("ask-"+name+".png", Context.MODE_PRIVATE)) { bitmap.compress(Bitmap.CompressFormat.PNG,100,output); }
        bitmap.recycle();
    }
}
