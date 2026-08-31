package com.mprlab.portal;

public final class WeatherVisibilityContract {
    public static void main(String[] arguments) {
        requireHidden(null);
        requireHidden("");
        requireHidden("   ");
        if (!WeatherVisibility.isConfigured("90210") || !WeatherVisibility.isConfigured("Beverly Hills, CA")) {
            throw new AssertionError("A configured ZIP code or city must show weather.");
        }
    }

    private static void requireHidden(String location) {
        if (WeatherVisibility.isConfigured(location)) {
            throw new AssertionError("Missing location must hide weather.");
        }
    }
}
