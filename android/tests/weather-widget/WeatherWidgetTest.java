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
            result.putString("stream", "Weather widget passed: seven outfits, cache startup, and card bounds.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "Weather widget failed: " + error + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
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
