package com.mprlab.portal;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class WeatherReport {
    private static final int HOT_DAY_F = 85;
    private static final int WARM_DAY_F = 70;
    private static final int COOL_DAY_F = 55;
    private static final int VERY_COLD_DAY_F = 40;
    private static final int PRECIPITATION_POSSIBLE = 40;
    private static final int PRECIPITATION_LIKELY = 70;
    final String place, condition, icon;
    final int temperature, feelsLike, high, low, precipitation;

    private WeatherReport(JSONObject json) throws JSONException {
        place = requiredText(json, "location");
        condition = requiredText(json, "condition");
        icon = requiredText(json, "icon");
        if (!icon.matches("clear|partly_cloudy|cloudy|fog|rain|snow|storm")) {
            throw new JSONException("Invalid weather icon");
        }
        temperature = requiredInteger(json, "temperature_f");
        feelsLike = requiredInteger(json, "feels_like_f");
        high = requiredInteger(json, "high_f");
        low = requiredInteger(json, "low_f");
        precipitation = requiredInteger(json, "precipitation_probability");
        if (precipitation < 0 || precipitation > 100 || low > high) {
            throw new JSONException("Invalid weather forecast range");
        }
    }

    static WeatherReport parse(String raw) throws JSONException {
        return new WeatherReport(new JSONObject(raw));
    }

    private static String requiredText(JSONObject json, String key) throws JSONException {
        Object value = json.get(key);
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            throw new JSONException("Invalid weather field: " + key);
        }
        return (String) value;
    }

    private static int requiredInteger(JSONObject json, String key) throws JSONException {
        Object value = json.get(key);
        if (!(value instanceof Integer)) throw new JSONException("Invalid weather field: " + key);
        return (Integer) value;
    }

    Outfit outfit() {
        boolean wet = icon.equals("rain") || icon.equals("storm");
        boolean snow = icon.equals("snow");
        if (snow || feelsLike < 40) {
            return new Outfit(R.drawable.ic_weather_hoodie, "Warm coat", R.drawable.ic_weather_boot,
                    "Warm boots");
        }
        if (wet) {
            return new Outfit(R.drawable.ic_weather_hoodie, "Raincoat", R.drawable.ic_weather_boot,
                    "Rain boots");
        }
        if (feelsLike < 55) {
            return new Outfit(R.drawable.ic_weather_hoodie, "Jacket", R.drawable.ic_weather_sneaker,
                    "Sneakers");
        }
        if (feelsLike < 70) {
            return new Outfit(R.drawable.ic_weather_hoodie, "Light layer", R.drawable.ic_weather_sneaker,
                    "Sneakers");
        }
        return new Outfit(R.drawable.ic_weather_t_shirt, "T-shirt", R.drawable.ic_weather_sneaker,
                "Sneakers");
    }

    DayPlan dayPlan() {
        List<String> advice = new ArrayList<>();
        String outlook;
        if (high >= HOT_DAY_F) {
            outlook = "Hot";
            advice.add("Bring water and a sun hat.");
        } else if (high >= WARM_DAY_F) {
            outlook = "Warm";
            advice.add("Wear light clothes.");
        } else if (high >= COOL_DAY_F) {
            outlook = "Cool";
            advice.add("Take a light layer.");
        } else if (high >= VERY_COLD_DAY_F) {
            outlook = "Cold";
            advice.add("Take a warm jacket.");
        } else {
            outlook = "Very cold";
            advice.add("Take a coat, hat and gloves.");
        }
        String precipitationLabel = high < VERY_COLD_DAY_F ? "Rain / snow chance " : "Rain chance ";
        if (precipitation >= PRECIPITATION_POSSIBLE) {
            String kind = high < VERY_COLD_DAY_F ? "Rain or snow" : "Rain";
            outlook += " · " + kind + (precipitation >= PRECIPITATION_LIKELY ? " likely" : " possible");
            advice.add(high < VERY_COLD_DAY_F ? "Pack waterproof layers and boots."
                    : precipitation >= PRECIPITATION_LIKELY ? "Pack a raincoat and waterproof shoes." : "Pack a raincoat.");
        }
        if (high >= WARM_DAY_F && low < 60 && high - low >= 20) {
            advice.add("Take a layer for cooler hours.");
        }
        return new DayPlan(outlook, precipitationLabel + precipitation + "%", advice);
    }

    static final class DayPlan {
        final String outlook, precipitation;
        final List<String> advice;

        private DayPlan(String outlook, String precipitation, List<String> advice) {
            this.outlook = outlook;
            this.precipitation = precipitation;
            this.advice = Collections.unmodifiableList(advice);
        }
    }

    static final class Outfit {
        final int topIcon, shoesIcon;
        final String top, shoes;

        private Outfit(int topIcon, String top, int shoesIcon, String shoes) {
            this.topIcon = topIcon;
            this.top = top;
            this.shoesIcon = shoesIcon;
            this.shoes = shoes;
        }
    }
}
