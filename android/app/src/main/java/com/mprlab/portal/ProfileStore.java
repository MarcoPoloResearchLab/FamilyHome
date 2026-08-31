package com.mprlab.portal;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

final class ProfileStore {
    static final String PREFS_NAME = "children_portal";
    private static final String PREFS_PROFILES = "profiles_json";
    private static final String PREFS_ACTIVE = "active_profile_id";
    private static final String PREFS_WEATHER_LOCATION = "weather_location";

    static final class Profile {
        String id;
        String name;
        String calendarUrl;
        final Set<String> enabledGameIds = new LinkedHashSet<>();
        long remainingMs = 20L * 60L * 1000L;
        long timerEndEpochMs;
        boolean timerRunning;
        int colorIndex;
        int brushIndex = 1;
        JSONArray legacyStrokes;

        Profile(String name) {
            this(UUID.randomUUID().toString(), name);
        }

        Profile(String id, String name) {
            this.id = id;
            this.name = name;
        }

        JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("name", name);
            json.put("calendar_url", calendarUrl == null ? "" : calendarUrl);
            JSONArray games = new JSONArray();
            for (String gameId : enabledGameIds) games.put(gameId);
            json.put("enabled_game_ids", games);
            json.put("remaining_ms", remainingMs);
            json.put("timer_end_epoch_ms", timerEndEpochMs);
            json.put("timer_running", timerRunning);
            json.put("color_index", colorIndex);
            json.put("brush_index", brushIndex);
            if (legacyStrokes != null) {
                json.put("strokes", legacyStrokes);
            }
            return json;
        }

        static Profile fromJson(JSONObject json) {
            Profile profile = new Profile(json.optString("id", UUID.randomUUID().toString()), json.optString("name", "Child"));
            profile.calendarUrl = json.optString("calendar_url", "");
            JSONArray games = json.optJSONArray("enabled_game_ids");
            if (games != null) {
                for (int index = 0; index < games.length(); index++) {
                    String gameId = games.optString(index, "");
                    if (GameCatalog.find(gameId) != null) profile.enabledGameIds.add(gameId);
                }
            } else {
                if (json.optBoolean("freedoom_enabled", false)) profile.enabledGameIds.add(GameCatalog.ADVENTURE);
                if (json.optBoolean("kart_enabled", false)) profile.enabledGameIds.add(GameCatalog.KART);
            }
            profile.remainingMs = json.optLong("remaining_ms", 20L * 60L * 1000L);
            profile.timerEndEpochMs = json.optLong("timer_end_epoch_ms", 0L);
            profile.timerRunning = json.optBoolean("timer_running", false);
            profile.colorIndex = json.optInt("color_index", 0);
            profile.brushIndex = json.optInt("brush_index", 1);
            profile.legacyStrokes = json.optJSONArray("strokes");
            return profile;
        }

        boolean isGameEnabled(String gameId) {
            return enabledGameIds.contains(gameId);
        }

        void setGameEnabled(String gameId, boolean enabled) {
            if (enabled) enabledGameIds.add(gameId);
            else enabledGameIds.remove(gameId);
        }
    }

    private final SharedPreferences preferences;
    final ArrayList<Profile> profiles = new ArrayList<>();
    Profile active;
    String weatherLocation = "";

    ProfileStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        load();
    }

    void load() {
        profiles.clear();
        active = null;
        weatherLocation = preferences.getString(PREFS_WEATHER_LOCATION, "");
        String raw = preferences.getString(PREFS_PROFILES, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index++) {
                profiles.add(Profile.fromJson(array.getJSONObject(index)));
            }
        } catch (JSONException ignored) {
            profiles.clear();
        }
        String activeID = preferences.getString(PREFS_ACTIVE, "");
        for (Profile profile : profiles) {
            if (profile.id.equals(activeID)) {
                active = profile;
                break;
            }
        }
        if (active == null && !profiles.isEmpty()) {
            active = profiles.get(0);
        }
    }

    void save() {
        JSONArray array = new JSONArray();
        for (Profile profile : profiles) {
            try {
                array.put(profile.toJson());
            } catch (JSONException ignored) {
            }
        }
        preferences.edit()
                .putString(PREFS_PROFILES, array.toString())
                .putString(PREFS_ACTIVE, active == null ? "" : active.id)
                .putString(PREFS_WEATHER_LOCATION, weatherLocation == null ? "" : weatherLocation)
                .apply();
    }

    Profile add(String name) {
        Profile profile = new Profile(name);
        profiles.add(profile);
        active = profile;
        save();
        return profile;
    }
}
