package com.mprlab.portal;

import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;

final class AskClient {
    private static final int CONNECTION_TIMEOUT_MS = 6000;
    private static final int SETTINGS_TIMEOUT_MS = 10000;
    private static final int RESPONSE_BYTE_LIMIT = 1 << 20;
    private enum Result {
        ANSWER("answer", "Ask returned an invalid answer."),
        TRANSCRIPT("transcript", "The recording could not be transcribed. Please try again.");
        final String field, invalidMessage;
        Result(String field, String invalidMessage) { this.field = field; this.invalidMessage = invalidMessage; }
    }

    static final class Settings {
        final int timeoutMillis, questionLimit, recordingSeconds, audioBytes;
        private Settings(JSONObject value) throws Exception {
            timeoutMillis = integer(value, "request_timeout_seconds", 1, 97200)*1000;
            questionLimit = integer(value, "question_character_limit", 1, 8000);
            recordingSeconds = integer(value, "recording_duration_seconds", 1, 3600);
            audioBytes = integer(value, "audio_byte_limit", 1024, 12 << 20);
        }
        private static int integer(JSONObject value, String key, int min, int max) throws Exception {
            Object raw = value.get(key);
            if (!(raw instanceof Integer) || (Integer)raw < min || (Integer)raw > max) throw new IOException("Ask returned invalid settings.");
            return (Integer)raw;
        }
    }

    static final class Call {
        private HttpURLConnection connection;
        private Timer deadline;
        private boolean cancelled;
        void cancel() {
            HttpURLConnection current;
            synchronized (this) {
                cancelled = true; current = connection; connection = null;
                if (deadline != null) { deadline.cancel(); deadline = null; }
            }
            if (current != null) current.disconnect();
        }
        private HttpURLConnection open(String path, String method, int timeout) throws IOException {
            HttpURLConnection created = (HttpURLConnection)new URL(PortalConfig.serviceURL(path)).openConnection();
            created.setInstanceFollowRedirects(false);
            created.setRequestMethod(method); created.setConnectTimeout(CONNECTION_TIMEOUT_MS); created.setReadTimeout(timeout);
            created.setRequestProperty("Accept", "application/json"); PortalConfig.authorize(created);
            synchronized (this) {
                if (cancelled) { created.disconnect(); throw new IOException("Question cancelled."); }
                connection = created;
                deadline = new Timer("Ask-deadline", true);
                deadline.schedule(new TimerTask() { @Override public void run() { cancelCall(); } }, timeout);
            }
            return created;
        }
        private void close(HttpURLConnection current) {
            synchronized (this) {
                if (connection == current) {
                    connection = null;
                    if (deadline != null) { deadline.cancel(); deadline = null; }
                }
            }
            current.disconnect();
        }
        private void cancelCall() { cancel(); }
        Settings settings() throws Exception {
            HttpURLConnection current = open("/v1/ask/settings", "GET", SETTINGS_TIMEOUT_MS);
            try { return new Settings(read(current)); } finally { close(current); }
        }
        String typed(String profile, String name, String question) throws Exception {
            Settings settings = settings();
            if (question.codePointCount(0, question.length()) > settings.questionLimit) throw new IOException("Please ask a shorter question.");
            JSONObject value = new JSONObject().put("profile_id", profile).put("name", name).put("question", question);
            return send("/v1/ask", "application/json; charset=utf-8", value.toString().getBytes(StandardCharsets.UTF_8), settings, Result.ANSWER);
        }
        String transcribe(String profile, String name, File recording) throws Exception {
            Settings settings = settings();
            if (!recording.isFile() || recording.length() == 0 || recording.length() > settings.audioBytes) throw new IOException("The recording is empty or too large.");
            String boundary = "PortalQuestion"+UUID.randomUUID().toString();
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            part(body, boundary, "profile_id", profile); part(body, boundary, "name", name);
            body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"question.m4a\"\r\nContent-Type: audio/m4a\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            try (InputStream input = new FileInputStream(recording)) {
                byte[] buffer = new byte[8192]; int count, total = 0;
                while ((count = input.read(buffer)) != -1) {
                    total += count; if (total > settings.audioBytes) throw new IOException("The recording is too large.");
                    body.write(buffer, 0, count);
                }
            }
            body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
            return send("/v1/ask/transcriptions", "multipart/form-data; boundary="+boundary, body.toByteArray(), settings, Result.TRANSCRIPT);
        }
        private String send(String path, String contentType, byte[] bytes, Settings settings, Result expected) throws Exception {
            HttpURLConnection current = open(path, "POST", settings.timeoutMillis);
            try {
                current.setDoOutput(true); current.setFixedLengthStreamingMode(bytes.length); current.setRequestProperty("Content-Type", contentType);
                try (OutputStream output = current.getOutputStream()) { output.write(bytes); }
                JSONObject result = read(current);
                Object raw = result.opt(expected.field);
                if (!(raw instanceof String) || ((String)raw).trim().isEmpty()) throw new AnswerFailure(expected.invalidMessage);
                if (expected == Result.TRANSCRIPT && ((String)raw).codePointCount(0,((String)raw).length()) > settings.questionLimit)
                    throw new AnswerFailure(expected.invalidMessage);
                return ((String)raw).trim();
            } catch (AnswerFailure error) { throw error; }
            catch (IOException error) { throw new IOException("The connection stopped. Your question may still be processing."); }
            finally { close(current); }
        }
        private static void part(OutputStream body, String boundary, String name, String value) throws IOException {
            body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+name+"\"\r\n\r\n"+value+"\r\n").getBytes(StandardCharsets.UTF_8));
        }
        private JSONObject read(HttpURLConnection current) throws Exception {
            int status = current.getResponseCode();
            String type = current.getContentType();
            if (type == null || !type.split(";",2)[0].trim().equalsIgnoreCase("application/json")) throw new AnswerFailure("Ask returned an invalid answer.");
            InputStream source = status >= 200 && status < 300 ? current.getInputStream() : current.getErrorStream();
            if (source == null) throw new AnswerFailure("Ask returned an invalid answer.");
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            try (InputStream input = source) {
                byte[] buffer = new byte[8192]; int count;
                while ((count=input.read(buffer)) != -1) {
                    if (body.size()+count > RESPONSE_BYTE_LIMIT) throw new AnswerFailure("Ask returned an invalid answer.");
                    body.write(buffer,0,count);
                }
            }
            JSONObject value;
            try { value = new JSONObject(body.toString("UTF-8")); }
            catch (org.json.JSONException error) { throw new AnswerFailure("Ask returned an invalid answer."); }
            if (status != 200) {
                // Only application-owned error codes select child-facing text.
                String code = value.optString("code", "");
                switch (code) {
                    case "unsupported_audio": throw new AnswerFailure("Voice questions are not available. Please type your question.");
                    case "invalid_question": throw new AnswerFailure("Choose a child and enter a shorter question.");
                    case "invalid_audio": throw new AnswerFailure("The recording could not be read. Please record it again.");
                    case "invalid_transcript": throw new AnswerFailure(Result.TRANSCRIPT.invalidMessage);
                    case "ask_busy": case "provider_busy": throw new AnswerFailure("Ask is busy. Please wait before asking again.");
                    case "outcome_unknown": throw new AnswerFailure("The answer did not arrive. Your question may still be processing.");
                    default: throw new AnswerFailure("Ask is unavailable right now. Please try again later.");
                }
            }
            return value;
        }
    }
    private static final class AnswerFailure extends IOException { AnswerFailure(String message) { super(message); } }
}
