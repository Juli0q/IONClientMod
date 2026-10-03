package de.ionnetwork.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Reads a player's coin balance from the ION Network public stats API (the Strapi backend of
 * the website).
 *
 * <p>Primary route: {@code GET /api/stats/player/<uuid>/coins}, which answers
 * {@code {"uuid": "...", "coins": 123}} or 404 for an unknown player. Until that route is
 * deployed, and for any 404, the player search ({@code /api/stats/players/search?q=<name>})
 * is used as a fallback and the row with the player's UUID is picked out of it.
 *
 * <p>Both routes are public and read-only; nothing is sent beyond the player's own UUID and
 * name. The base URL can be overridden with {@code -Dionclient.api=https://...}.
 */
public final class IonCoinsApi {

    public static final String DEFAULT_BASE_URL = "https://beta-api.ion-network.de";
    private static final int TIMEOUT_MILLIS = 5000;

    private IonCoinsApi() {
    }

    public static String baseUrl() {
        String configured = System.getProperty("ionclient.api", DEFAULT_BASE_URL).trim();
        return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
    }

    /** Returns the balance, or null when the player has no ION profile. */
    public static Long fetch(UUID player, String name) throws Exception {
        String dashed = player.toString();
        Response direct = get(baseUrl() + "/api/stats/player/" + dashed + "/coins");
        if (direct.status == 200) {
            JsonObject body = direct.json().getAsJsonObject();
            return body.get("coins").getAsLong();
        }
        if (direct.status != 404) {
            throw new IOException("coins route answered " + direct.status);
        }
        return searchFallback(player, name);
    }

    private static Long searchFallback(UUID player, String name) throws Exception {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Response search = get(baseUrl() + "/api/stats/players/search?q=" + URLEncoder.encode(name, "UTF-8"));
        if (search.status != 200) {
            throw new IOException("player search answered " + search.status);
        }
        String bare = player.toString().replace("-", "");
        JsonArray results = search.json().getAsJsonObject().getAsJsonArray("results");
        for (JsonElement element : results) {
            JsonObject row = element.getAsJsonObject();
            String uuid = row.get("uuid").getAsString().replace("-", "");
            if (uuid.equalsIgnoreCase(bare)) {
                return row.get("coins").getAsLong();
            }
        }
        return null;
    }

    private static Response get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "IONClient/" + IonBrand.SERVER_ADDRESS);
        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body = "";
        if (stream != null) {
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                StringBuilder out = new StringBuilder();
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) >= 0) {
                    out.append(buffer, 0, read);
                }
                body = out.toString();
            }
        }
        connection.disconnect();
        return new Response(status, body);
    }

    private static final class Response {
        final int status;
        final String body;

        Response(int status, String body) {
            this.status = status;
            this.body = body;
        }

        JsonElement json() {
            return new JsonParser().parse(body);
        }
    }
}
