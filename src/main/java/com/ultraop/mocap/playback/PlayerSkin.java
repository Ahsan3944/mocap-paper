package com.ultraop.mocap.playback;

import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Command and scene-file representation of MoCap's player_skin modifier.
 * Resolution is intentionally kept separate from the value object so scene
 * loading remains deterministic and does not perform network requests.
 */
public record PlayerSkin(Source source, String path) {
    private static final String MINESKIN_URL_PREFIX1 = "minesk.in/";
    private static final String MINESKIN_URL_PREFIX2 = "mineskin.org/skins/";
    private static final Pattern MINESKIN_UUID_PATTERN = Pattern.compile("[0-9a-f]*");

    public enum Source {
        DEFAULT,
        FROM_PLAYER,
        FROM_FILE,
        FROM_MINESKIN
    }

    public static final PlayerSkin DEFAULT = new PlayerSkin(Source.DEFAULT, null);

    public PlayerSkin {
        source = Objects.requireNonNull(source, "source");
        if (source == Source.DEFAULT && path != null) {
            throw new IllegalArgumentException("Default player skin cannot have a path");
        }
        if (source != Source.DEFAULT && (path == null || path.isBlank())) {
            throw new IllegalArgumentException("Player skin path cannot be blank");
        }
        if (source == Source.FROM_MINESKIN) validateMineSkinUrl(path);
    }

    public static PlayerSkin fromPlayer(String playerName) {
        return new PlayerSkin(Source.FROM_PLAYER, playerName);
    }

    public static PlayerSkin fromFile(String filename) {
        return new PlayerSkin(Source.FROM_FILE, filename);
    }

    public static PlayerSkin fromMineSkin(String url) {
        return new PlayerSkin(Source.FROM_MINESKIN, url);
    }

    public boolean isDefault() {
        return source == Source.DEFAULT;
    }

    public boolean isSkinList() {
        return source == Source.FROM_FILE && path != null && path.startsWith("skin_list:");
    }

    public PlayerSkin mergeWithParent(PlayerSkin parent) {
        return isDefault() && parent != null ? parent : this;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (source == Source.DEFAULT) return json;
        json.addProperty("skin_source", sourceKey());
        json.addProperty("skin_path", path);
        return json;
    }

    public static PlayerSkin fromJson(JsonObject json) {
        if (json == null || json.isEmpty()) return DEFAULT;
        String source = json.has("skin_source") ? json.get("skin_source").getAsString() : "default";
        String path = json.has("skin_path") ? json.get("skin_path").getAsString() : null;
        return switch (source.toLowerCase(Locale.ROOT)) {
            case "default" -> DEFAULT;
            case "from_player" -> fromPlayer(path);
            case "from_file" -> fromFile(path);
            case "from_mineskin" -> fromMineSkin(path);
            default -> throw new IllegalArgumentException("Unknown player skin source: " + source);
        };
    }

    private String sourceKey() {
        return switch (source) {
            case DEFAULT -> "default";
            case FROM_PLAYER -> "from_player";
            case FROM_FILE -> "from_file";
            case FROM_MINESKIN -> "from_mineskin";
        };
    }

    private static void validateMineSkinUrl(String value) {
        String url = value;
        if (url.startsWith("https://")) url = url.substring(8);
        else if (url.startsWith("http://")) url = url.substring(7);
        if (url.startsWith(MINESKIN_URL_PREFIX1)) url = url.substring(MINESKIN_URL_PREFIX1.length());
        else if (url.startsWith(MINESKIN_URL_PREFIX2)) url = url.substring(MINESKIN_URL_PREFIX2.length());
        else throw new IllegalArgumentException("Invalid MineSkin URL: " + value);
        if (url.length() != 32 || !MINESKIN_UUID_PATTERN.matcher(url).matches()) {
            throw new IllegalArgumentException("Invalid MineSkin URL: " + value);
        }
    }
}
