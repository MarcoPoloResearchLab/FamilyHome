package com.mprlab.portal;

final class WeatherVisibility {
    static boolean isConfigured(String location) {
        return location != null && !location.trim().isEmpty();
    }

    private WeatherVisibility() {
    }
}
