package com.ultraop.mocap.playback;

import org.bukkit.entity.EntityType;

/** Command/runtime representation of MoCap's player_as_entity modifier. */
public record PlayerAsEntity(EntityType entityType, String entityNbt) {
    public static final PlayerAsEntity DISABLED = new PlayerAsEntity(null, null);

    public boolean enabled() {
        return entityType != null;
    }

    public String entityId() {
        return entityType == null ? null : entityType.getKey().toString();
    }
}
