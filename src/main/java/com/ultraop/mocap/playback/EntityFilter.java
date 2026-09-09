package com.ultraop.mocap.playback;

import org.bukkit.entity.EntityType;

import java.util.Locale;

/** MoCap-compatible entity filter value object. */
public record EntityFilter(String expression) {
    public static final String EMPTY_GROUP = "none";

    public static EntityFilter disabled() {
        return new EntityFilter(null);
    }

    public boolean enabled() {
        return expression != null && !expression.isBlank() && !expression.equalsIgnoreCase(EMPTY_GROUP);
    }

    public boolean matches(EntityType type) {
        if (!enabled()) return true;
        String value = type.getKey().toString().toLowerCase(Locale.ROOT);
        for (String token : expression.split("[, ]+")) {
            token = token.trim().toLowerCase(Locale.ROOT);
            if (token.equals(value) || token.equals(type.name().toLowerCase(Locale.ROOT))) return true;
            if (token.endsWith("*") && value.startsWith(token.substring(0, token.length() - 1))) return true;
        }
        return false;
    }
}
