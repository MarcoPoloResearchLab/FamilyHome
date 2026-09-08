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
                assertNoRecordings();
                invalidAnswer();
                lateResponse();
                profileChange();
                deadline();
                recording();
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
                if (!request.startsWith("POST /v1/ask ") && !request.startsWith("POST /v1/ask/audio ")) throw new AssertionError(request);
                if (request.startsWith("POST /v1/ask/audio ")) audioBody = body;
                requests.incrementAndGet();
                CountDownLatch gate = release; value = response;
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
        await(() -> { View ask = text(activity, "Type & ask"); return ask != null && ask.isEnabled(); });
        return activity;
    }
    private void close(Activity activity) throws Exception { runOnMainSync(activity::finish); waitForIdleSync(); await(activity::isDestroyed); }
    private void typedAndDuplicate() throws Exception {
        Activity activity = open(); int before = requests.get(); release = new CountDownLatch(1);
        try {
            capture(activity, "ready");
            runOnMainSync(() -> {
                edit(activity.getWindow().getDecorView()).setText("Why is the sky blue?");
                text(activity, "Type & ask").performClick();
                if (text(activity, "Type & ask").isEnabled() || text(activity, "Talk to ask").isEnabled()) throw new AssertionError("Submissions remain enabled while busy");
                text(activity, "Type & ask").performClick();
            });
            await(() -> requests.get() == before+1); capture(activity, "thinking"); release.countDown();
            await(() -> text(activity, "The sky scatters blue light.") != null);
            if (requests.get() != before+1) throw new AssertionError("Duplicate question");
            runOnMainSync(() -> {
                View stop = text(activity, "Stop speaking"); if (stop == null) throw new AssertionError("Missing speech control");
                stop.performClick();
            });
            capture(activity, "answer");
        } finally { release.countDown(); close(activity); }
    }
    private void invalidAnswer() throws Exception {
        response = "{\"answer\":42}"; Activity activity = open();
        try {
            runOnMainSync(() -> { edit(activity.getWindow().getDecorView()).setText("Keep this draft"); text(activity, "Type & ask").performClick(); });
            await(() -> text(activity, "Ask returned an invalid answer.") != null);
            runOnMainSync(() -> { if (!edit(activity.getWindow().getDecorView()).getText().toString().equals("Keep this draft")) throw new AssertionError("Draft lost"); });
            capture(activity, "error");
        } finally { close(activity); response = "{\"answer\":\"The sky scatters blue light.\"}"; }
    }
    private void lateResponse() throws Exception {
        Activity activity = open(); int before = requests.get(); release = new CountDownLatch(1);
        runOnMainSync(() -> { edit(activity.getWindow().getDecorView()).setText("Late question"); text(activity, "Type & ask").performClick(); });
        await(() -> requests.get() == before+1);
        close(activity); release.countDown();
        Thread.sleep(250);
        runOnMainSync(() -> { if (text(activity, "The sky scatters blue light.") != null) throw new AssertionError("Late answer appeared after exit"); });
    }
    private void recording() throws Exception {
        Activity activity = open();
        try {
            runOnMainSync(() -> text(activity, "Talk to ask").performClick());
            await(() -> text(activity, "Send my question") != null); capture(activity, "recording");
            Thread.sleep(1000);
            runOnMainSync(() -> text(activity, "Send my question").performClick());
            await(() -> audioBody != null);
            if (!new String(audioBody, StandardCharsets.ISO_8859_1).contains("ftyp")) throw new AssertionError("No M4A recording in upload");
            await(() -> text(activity, "The sky scatters blue light.") != null);
            assertNoRecordings();
            runOnMainSync(() -> text(activity, "Talk to ask").performClick());
            await(() -> text(activity, "Send my question") != null);
        } finally { close(activity); }
        assertNoRecordings();
    }
    private void deadline() throws Exception {
        timeoutSeconds = 1; Activity activity = open(); release = new CountDownLatch(1);
        try {
            runOnMainSync(() -> { edit(activity.getWindow().getDecorView()).setText("Slow question"); text(activity,"Type & ask").performClick(); });
            await(() -> text(activity,"The connection stopped. Your question may still be processing.") != null);
        } finally { release.countDown(); close(activity); timeoutSeconds = 12; }
    }
    private void profileChange() throws Exception {
        Activity activity = open();
        try {
            runOnMainSync(() -> text(activity,"Talk to ask").performClick());
            await(() -> text(activity,"Send my question") != null);
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
            runOnMainSync(() -> text(activity,"Talk to ask").performClick());
            await(() -> text(activity,"Microphone access was denied. You can type your question.") != null);
            assertNoRecordings();
            runOnMainSync(() -> {
                if (!text(activity,"Type & ask").isEnabled()) throw new AssertionError("Microphone denial disabled typed questions");
            });
        } finally { close(activity); }
    }
    private void screensaver() throws Exception {
        getTargetContext().getSharedPreferences("screensaver",Context.MODE_PRIVATE).edit()
                .putString("mode","BLACK").putString("timeout","THIRTY_SECONDS").commit();
        Activity activity = open(); int before = requests.get();
        try {
            runOnMainSync(() -> text(activity,"Talk to ask").performClick());
            await(() -> text(activity,"Send my question") != null);
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
        waitForIdleSync(); Bitmap bitmap = getUiAutomation().takeScreenshot();
        try (OutputStream output = getTargetContext().openFileOutput("ask-"+name+".png", Context.MODE_PRIVATE)) { bitmap.compress(Bitmap.CompressFormat.PNG,100,output); }
        bitmap.recycle();
    }
}
