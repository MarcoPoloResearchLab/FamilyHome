package com.mprlab.portal.weathertest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.json.JSONObject;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public final class WeatherWidgetTest extends Instrumentation {
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            scenario(79, 0, "clear", "Sunny", "T-shirt", "Sneakers");
            scenario(69, 0, "cloudy", "Cloudy", "Light layer", "Sneakers");
            scenario(54, 0, "fog", "Foggy", "Jacket", "Sneakers");
            scenario(39, 0, "clear", "Sunny", "Warm coat", "Warm boots");
            scenario(75, 40, "clear", "Sunny", "Raincoat", "Rain boots");
            scenario(52, 0, "rain", "Light rain showers", "Raincoat", "Rain boots");
            scenario(42, 80, "snow", "Snowy", "Warm coat", "Warm boots");
            refreshWhileHomeStaysOpen();
            result.putString("stream", "Weather widget passed: seven outfits, cache startup, card bounds, expiry, and automatic recovery.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "Weather widget failed: " + error + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void refreshWhileHomeStaysOpen() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        try (ServerSocket server = new ServerSocket(18765)) {
            Thread provider = new Thread(() -> {
                try {
                    while (!server.isClosed()) {
                        try (Socket socket = server.accept()) {
                            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                            String request = input.readLine();
                            if (!request.startsWith("GET /v1/weather?location=90266 ")) throw new AssertionError(request);
                            String line;
                            boolean authorized = false;
                            while ((line = input.readLine()) != null && !line.isEmpty()) {
                                if (line.equals("Authorization: Bearer familyhome-weather-test-token-000000")) authorized = true;
                            }
                            if (!authorized) throw new AssertionError("Missing weather authorization");
                            int count = requests.incrementAndGet();
                            String body = count == 1 ? "{}" : "{\"location\":\"Manhattan Beach, California\",\"temperature_f\":68,\"feels_like_f\":73,\"high_f\":80,\"low_f\":66,\"precipitation_probability\":75,\"condition\":\"Rainy\",\"icon\":\"rain\"}";
                            String status = count == 1 ? "503 Service Unavailable" : "200 OK";
                            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                            socket.getOutputStream().write(("HTTP/1.1 " + status + "\r\nContent-Type: application/json\r\nContent-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                            socket.getOutputStream().write(bytes);
                        }
                    }
                } catch (Exception error) {
                    if (!server.isClosed()) throw new AssertionError(error);
                }
            });
            provider.setDaemon(true);
            provider.start();
            JSONObject cached = new JSONObject().put("location", "Manhattan Beach, California")
                    .put("temperature_f", 72).put("feels_like_f", 74).put("high_f", 79).put("low_f", 62)
                    .put("precipitation_probability", 1).put("condition", "Sunny").put("icon", "clear");
            getTargetContext().getSharedPreferences("children_portal", Context.MODE_PRIVATE).edit()
                    .putString("weather_cache_json", cached.toString())
                    .putLong("weather_cache_updated_at", System.currentTimeMillis() - 15L * 60L * 1000L + 4000L).commit();
            Activity activity = startActivitySync(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal.MainActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            try {
                awaitText(activity, "72°", 2000L);
                awaitText(activity, "Weather unavailable", 8000L);
                runOnMainSync(() -> {
                    View root = activity.getWindow().getDecorView();
                    if (containsText(root, "72°") || containsText(root, "Sunny") || containsText(root, "T-shirt")) {
                        throw new AssertionError("Expired weather is still presented as current");
                    }
                });
                if (requests.get() != 1) throw new AssertionError("Expected one failed request, got " + requests.get());
                awaitText(activity, "Rainy", 40000L);
                runOnMainSync(() -> {
                    View root = activity.getWindow().getDecorView();
                    if (!containsText(root, "68°") || !containsText(root, "Raincoat")) throw new AssertionError("Live weather was not rendered");
                });
                Thread.sleep(1000L);
                if (requests.get() != 2) throw new AssertionError("Duplicate weather requests: " + requests.get());
            } finally {
                runOnMainSync(activity::finish);
                waitForIdleSync();
            }
        }
    }

    private void awaitText(Activity activity, String text, long timeout) throws Exception {
        long deadline = android.os.SystemClock.uptimeMillis() + timeout;
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            boolean[] found = new boolean[1];
            runOnMainSync(() -> found[0] = containsText(activity.getWindow().getDecorView(), text));
            if (found[0]) return;
            Thread.sleep(100L);
        }
        throw new AssertionError("Missing text while Home stays open: " + text);
    }

    private void scenario(int feelsLike, int rain, String icon, String condition, String top, String shoes) throws Exception {
        JSONObject weather = new JSONObject().put("location", "Manhattan Beach, California")
                .put("temperature_f", 65).put("feels_like_f", feelsLike).put("high_f", 76).put("low_f", 36)
                .put("precipitation_probability", rain).put("condition", condition).put("icon", icon);
        boolean saved = getTargetContext().getSharedPreferences("children_portal", Context.MODE_PRIVATE).edit()
                .putString("profiles_json", "[{\"id\":\"weather-test\",\"name\":\"Weather Test\"}]")
                .putString("active_profile_id", "weather-test")
                .putString("weather_location", "90266")
                .putString("weather_cache_location", "90266")
                .putString("weather_cache_json", weather.toString())
                .putLong("weather_cache_updated_at", System.currentTimeMillis()).commit();
        if (!saved) throw new AssertionError("Cannot save weather fixture");
        Activity activity = startActivitySync(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal.MainActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        Throwable[] failure = new Throwable[1];
        runOnMainSync(() -> {
            try {
                ViewGroup card = findCard(activity.getWindow().getDecorView());
                if (card == null) throw new AssertionError("Weather card is missing");
                float density = activity.getResources().getDisplayMetrics().density;
                if (Math.abs(card.getHeight() / density - 300) > 1) throw new AssertionError("Weather card height changed");
                requireText(card, "Feels like " + feelsLike + "°");
                requireText(card, "High 76°  •  Low 36°  •  Rain " + rain + "%");
                requireText(card, "READY TO GO?");
                requireText(card, top);
                requireText(card, "Pants");
                requireText(card, shoes);
                requireText(card, "Weather by Open-Meteo");
                Rect bounds = new Rect();
                card.getGlobalVisibleRect(bounds);
                checkBounds(card, bounds);
            } catch (Throwable error) {
                failure[0] = error;
            } finally {
                activity.finish();
            }
        });
        waitForIdleSync();
        long finishDeadline = android.os.SystemClock.uptimeMillis() + 3000L;
        while (!activity.isDestroyed() && android.os.SystemClock.uptimeMillis() < finishDeadline) Thread.sleep(50L);
        if (!activity.isDestroyed()) throw new AssertionError("Weather activity did not finish");
        if (failure[0] != null) throw new AssertionError(condition + " / " + feelsLike + ": " + failure[0]);
    }

    private ViewGroup findCard(View view) {
        if (view.getContentDescription() != null && view.getContentDescription().toString().startsWith("Weather for ")) {
            return (ViewGroup) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ViewGroup card = findCard(group.getChildAt(i));
                if (card != null) return card;
            }
        }
        return null;
    }

    private boolean containsText(View view, String text) {
        if (view instanceof TextView && ((TextView) view).getText().toString().equals(text)) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (containsText(group.getChildAt(i), text)) return true;
        }
        return false;
    }

    private void requireText(ViewGroup card, String text) {
        if (!containsText(card, text)) throw new AssertionError("Missing text: " + text);
    }

    private void checkBounds(View view, Rect cardBounds) {
        if (view.getVisibility() != View.VISIBLE) return;
        Rect visible = new Rect();
        if (!view.getGlobalVisibleRect(visible) || visible.width() < view.getWidth() || visible.height() < view.getHeight()
                || !cardBounds.contains(visible)) throw new AssertionError("Clipped view: " + view);
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getLayout() == null || text.getLayout().getHeight() > text.getHeight() - text.getCompoundPaddingTop()
                    - text.getCompoundPaddingBottom()) throw new AssertionError("Clipped text: " + text.getText());
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) checkBounds(group.getChildAt(i), cardBounds);
        }
    }
}
