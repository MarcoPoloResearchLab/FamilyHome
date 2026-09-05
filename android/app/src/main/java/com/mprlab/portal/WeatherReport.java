package com.mprlab.portal;

import org.json.JSONException;
import org.json.JSONObject;

final class WeatherReport {
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
        boolean wet = precipitation >= 40 || icon.equals("rain") || icon.equals("storm");
        boolean snow = icon.equals("snow");
        if (snow || feelsLike < 40) {
            return new Outfit(R.drawable.ic_weather_hoodie, "Warm coat", R.drawable.ic_weather_boot,
                    "Warm boots", "Add a hat and gloves.");
        }
        if (wet) {
            return new Outfit(R.drawable.ic_weather_hoodie, "Raincoat", R.drawable.ic_weather_boot,
                    "Rain boots", feelsLike < 55 ? "Wear warm layers underneath." : "Take a raincoat for rainy moments.");
        }
        if (feelsLike < 55) {
            return new Outfit(R.drawable.ic_weather_hoodie, "Jacket", R.drawable.ic_weather_sneaker,
                    "Sneakers", "A warm layer will feel cozy.");
        }
        if (feelsLike < 70) {
            return new Outfit(R.drawable.ic_weather_hoodie, "Light layer", R.drawable.ic_weather_sneaker,
                    "Sneakers", "A light layer for the cool air.");
        }
        return new Outfit(R.drawable.ic_weather_t_shirt, "T-shirt", R.drawable.ic_weather_sneaker,
                "Sneakers", "Light clothes for a warm day.");
    }

    static final class Outfit {
        final int topIcon, shoesIcon;
        final String top, shoes, advice;

        private Outfit(int topIcon, String top, int shoesIcon, String shoes, String advice) {
            this.topIcon = topIcon;
            this.top = top;
            this.shoesIcon = shoesIcon;
            this.shoes = shoes;
            this.advice = advice;
        }
    }
}
