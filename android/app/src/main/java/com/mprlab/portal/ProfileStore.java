package com.mprlab.portal;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

final class ProfileStore {
    static final String PREFS_NAME = "children_portal";
    private static final String PREFS_PROFILES = "profiles_json";
    private static final String PREFS_ACTIVE = "active_profile_id";
    private static final String PREFS_WEATHER_LOCATION = "weather_location";
    private static final String PREFS_WEATHER_CACHE_LOCATION = "weather_cache_location";
    private static final String PREFS_WEATHER_CACHE_JSON = "weather_cache_json";
    private static final String PREFS_WEATHER_CACHE_UPDATED_AT = "weather_cache_updated_at";

    static final class Profile {
        String id;
        String name;
        String calendarUrl;
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
            profile.remainingMs = json.optLong("remaining_ms", 20L * 60L * 1000L);
            profile.timerEndEpochMs = json.optLong("timer_end_epoch_ms", 0L);
            profile.timerRunning = json.optBoolean("timer_running", false);
            profile.colorIndex = json.optInt("color_index", 0);
            profile.brushIndex = json.optInt("brush_index", 1);
            profile.legacyStrokes = json.optJSONArray("strokes");
            return profile;
        }

    }

    private final SharedPreferences preferences;
    final ArrayList<Profile> profiles = new ArrayList<>();
    Profile active;
    String weatherLocation = "";
    String weatherCacheLocation = "";
    String weatherCacheJson = "";
    long weatherCacheUpdatedAt;

    ProfileStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        load();
    }

    void load() {
        profiles.clear();
        active = null;
        weatherLocation = preferences.getString(PREFS_WEATHER_LOCATION, "");
        weatherCacheLocation = preferences.getString(PREFS_WEATHER_CACHE_LOCATION, "");
        weatherCacheJson = preferences.getString(PREFS_WEATHER_CACHE_JSON, "");
        weatherCacheUpdatedAt = preferences.getLong(PREFS_WEATHER_CACHE_UPDATED_AT, 0L);
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
                .putString(PREFS_WEATHER_CACHE_LOCATION, weatherCacheLocation == null ? "" : weatherCacheLocation)
                .putString(PREFS_WEATHER_CACHE_JSON, weatherCacheJson == null ? "" : weatherCacheJson)
                .putLong(PREFS_WEATHER_CACHE_UPDATED_AT, weatherCacheUpdatedAt)
                .apply();
    }

    void cacheWeather(String location, String json) {
        weatherCacheLocation = location;
        weatherCacheJson = json;
        weatherCacheUpdatedAt = System.currentTimeMillis();
        save();
    }

    boolean hasWeatherCacheFor(String location) {
        return location != null && location.equals(weatherCacheLocation)
                && weatherCacheJson != null && !weatherCacheJson.isEmpty();
    }

    Profile add(String name) {
        Profile profile = new Profile(name);
        profiles.add(profile);
        active = profile;
        save();
        return profile;
    }
}
