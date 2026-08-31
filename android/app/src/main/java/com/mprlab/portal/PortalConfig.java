package com.mprlab.portal;

import java.net.HttpURLConnection;

final class PortalConfig {
    static String serviceURL(String path) {
        return RuntimeConfig.SERVICE_BASE_URL + path;
    }

    static String absoluteServiceURL(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.startsWith("/")) return RuntimeConfig.SERVICE_BASE_URL + value;
        return value;
    }

    static void authorize(HttpURLConnection connection) {
        connection.setRequestProperty("Authorization", "Bearer " + RuntimeConfig.DEVICE_TOKEN);
    }

    private PortalConfig() {
    }
}
