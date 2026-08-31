package com.mprlab.portal;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private static final String PROFILES = "["
            + "{\"id\":\"alice-profile\",\"name\":\"Alice\","
            + "\"calendar_url\":\"https://calendar.invalid/alice.ics\","
            + "\"freedoom_enabled\":true,\"kart_enabled\":true,"
            + "\"remaining_ms\":555000,\"timer_end_epoch_ms\":0,\"timer_running\":false,"
            + "\"color_index\":4,\"brush_index\":2},"
            + "{\"id\":\"bob-profile\",\"name\":\"Bob\","
            + "\"calendar_url\":\"\",\"freedoom_enabled\":false,\"kart_enabled\":false,"
            + "\"remaining_ms\":1200000,\"timer_end_epoch_ms\":0,\"timer_running\":false,"
            + "\"color_index\":1,\"brush_index\":1}"
            + "]";
    private static final String DRAWINGS = "["
            + "{\"id\":\"sunset-drawing\",\"title\":\"Sunset\",\"strokes\":["
            + "{\"color\":-16291073,\"width\":17.5,\"eraser\":false,\"points\":[10,10,80,60]}"
            + "]}"
            + "]";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        seedLegacyState();
        TextView ready = new TextView(this);
        ready.setText("Legacy fixture ready");
        ready.setTextSize(28);
        ready.setGravity(Gravity.CENTER);
        setContentView(ready);
    }

    private void seedLegacyState() {
        SharedPreferences preferences = getSharedPreferences("children_portal", MODE_PRIVATE);
        preferences.edit()
                .putString("profiles_json", PROFILES)
                .putString("active_profile_id", "alice-profile")
                .putString("drawings_alice-profile", DRAWINGS)
                .putString("active_drawing_alice-profile", "sunset-drawing")
                .putInt("drawing_color_alice-profile", 4)
                .putFloat("drawing_size_alice-profile", 22f)
                .putBoolean("drawing_eraser_alice-profile", true)
                .putString("weather_location", "90210")
                .putString("legacy_marker", "preserve-me")
                .commit();
        try (FileOutputStream output = new FileOutputStream(new File(getFilesDir(), "upgrade-sentinel.txt"))) {
            output.write("legacy-private-file".getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            throw new IllegalStateException("Could not seed upgrade sentinel", error);
        }
    }
}
