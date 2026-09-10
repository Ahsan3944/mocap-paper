package com.ultraop.mocap.playback;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** MineSkin profile-property resolver matching the upstream MoCap URL/API contract. */
public final class MineSkinSkins {
    private static final String PREFIX_SHORT = "minesk.in/";
    private static final String PREFIX_LONG = "mineskin.org/skins/";
    private static final String API = "https://api.mineskin.org/get/uuid/";
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]*");
    private static final int CACHE_SIZE = 5;
    private static final Map<String, Property> CACHE = new LinkedHashMap<>(CACHE_SIZE + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Property> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private MineSkinSkins() {}

    public static synchronized boolean verifyUrl(String value) {
        if (value == null || value.isBlank()) return false;
        if (CACHE.containsKey(value)) return true;
        String id = normalize(value);
        return id != null && UUID_PATTERN.matcher(id).matches() && id.length() == 32;
    }

    public static synchronized Property getProperty(String value) {
        Property cached = CACHE.get(value);
        if (cached != null) return cached;
        if (!verifyUrl(value)) return null;
        String id = normalize(value);
        try {
            URL url = URI.create(API + id).toURL();
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");
            try (InputStream stream = connection.getInputStream()) {
                JsonObject texture = JsonParser.parseString(new String(stream.readAllBytes()))
                        .getAsJsonObject().getAsJsonObject("data").getAsJsonObject("texture");
                Property property = new Property("textures", texture.get("value").getAsString(), texture.get("signature").getAsString());
                CACHE.put(value, property);
                return property;
            } finally {
                connection.disconnect();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    public static synchronized void clearCache() {
        CACHE.clear();
    }

    private static String normalize(String value) {
        String url = value;
        if (url.startsWith("https://")) url = url.substring(8);
        else if (url.startsWith("http://")) url = url.substring(7);
        if (url.startsWith(PREFIX_SHORT)) return url.substring(PREFIX_SHORT.length());
        if (url.startsWith(PREFIX_LONG)) return url.substring(PREFIX_LONG.length());
        return null;
    }
}
