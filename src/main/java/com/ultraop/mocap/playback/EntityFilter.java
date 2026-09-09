package com.ultraop.mocap.playback;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vehicle;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** MoCap-compatible entity filter value object. */
public record EntityFilter(String expression) {
    public static final String EMPTY_GROUP = "none";
    public static final EntityFilter DEFAULT_TRACK_ENTITIES = new EntityFilter("@vehicles;@projectiles;@items");

    private static final Set<EntityType> MINECARTS = EnumSet.of(
            EntityType.MINECART,
            EntityType.CHEST_MINECART,
            EntityType.FURNACE_MINECART,
            EntityType.HOPPER_MINECART,
            EntityType.TNT_MINECART,
            EntityType.COMMAND_BLOCK_MINECART
    );

    private static final Set<EntityType> RIDABLE_MOBS = EnumSet.of(
            EntityType.HORSE,
            EntityType.DONKEY,
            EntityType.MULE,
            EntityType.CAMEL,
            EntityType.SKELETON_HORSE,
            EntityType.ZOMBIE_HORSE,
            EntityType.PIG,
            EntityType.STRIDER
    );

    public static EntityFilter disabled() {
        return new EntityFilter(null);
    }

    public boolean enabled() {
        return expression != null && !expression.isBlank() && !expression.equalsIgnoreCase(EMPTY_GROUP);
    }

    public boolean matches(Entity entity) {
        if (!enabled()) return true;
        boolean matched = false;
        for (Token token : tokens()) {
            if (!token.matches(entity)) continue;
            matched = token.allowed();
        }
        return matched;
    }

    public boolean matches(EntityType type) {
        if (!enabled()) return true;
        boolean matched = false;
        for (Token token : tokens()) {
            if (!token.matches(type)) continue;
            matched = token.allowed();
        }
        return matched;
    }

    private List<Token> tokens() {
        List<Token> result = new ArrayList<>();
        for (String raw : expression.split(";")) {
            String token = raw.trim();
            if (token.isEmpty() || token.equalsIgnoreCase("@none")) continue;
            boolean allowed = true;
            if (token.startsWith("-")) {
                allowed = false;
                token = token.substring(1).trim();
            }
            if (!token.isEmpty()) result.add(new Token(token.toLowerCase(Locale.ROOT), allowed));
        }
        return result;
    }

    private record Token(String value, boolean allowed) {
        boolean matches(Entity entity) {
            if (value.startsWith("$")) {
                return entity.getScoreboardTags().stream()
                        .anyMatch(tag -> tag.equalsIgnoreCase(value.substring(1)));
            }
            return matches(entity.getType());
        }

        boolean matches(EntityType type) {
            String id = type.getKey().toString().toLowerCase(Locale.ROOT);
            if (value.equals("*")) return true;
            if (value.equals("@vehicles")) return RIDABLE_MOBS.contains(type) || isVehicleType(type);
            if (value.equals("@projectiles")) return isAssignable(type, Projectile.class);
            if (value.equals("@items")) return isAssignable(type, Item.class);
            if (value.equals("@mobs")) return isAssignable(type, Mob.class);
            if (value.equals("@minecarts")) return MINECARTS.contains(type);
            if (value.startsWith("$")) return false;
            if (value.endsWith(":*")) return id.startsWith(value.substring(0, value.length() - 1));
            if (value.endsWith("*")) return id.startsWith(value.substring(0, value.length() - 1));
            return value.equals(id) || value.equals(type.name().toLowerCase(Locale.ROOT));
        }

        private static boolean isAssignable(EntityType type, Class<?> base) {
            Class<? extends Entity> entityClass = type.getEntityClass();
            return entityClass != null && base.isAssignableFrom(entityClass);
        }

        private static boolean isVehicleType(EntityType type) {
            return isAssignable(type, Vehicle.class);
        }
    }
}
