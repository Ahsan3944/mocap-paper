package com.ultraop.mocap.playback;

import com.google.gson.JsonObject;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;

/** Command/runtime representation of MoCap's player_as_entity modifier. */
public record PlayerAsEntity(EntityType entityType, String entityNbt) {
    public static final PlayerAsEntity DISABLED = new PlayerAsEntity(null, null);

    public PlayerAsEntity {
        if (entityType == null && entityNbt != null) {
            throw new IllegalArgumentException("Disabled player_as_entity cannot contain NBT");
        }
    }

    public static PlayerAsEntity enabled(EntityType type, String nbt) {
        if (type == null) throw new IllegalArgumentException("Entity type cannot be null");
        return new PlayerAsEntity(type, nbt == null || nbt.isBlank() ? null : nbt);
    }

    public static PlayerAsEntity fromId(String id, String nbt) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Entity id cannot be blank");
        NamespacedKey key = NamespacedKey.fromString(id);
        if (key == null) throw new IllegalArgumentException("Invalid entity id: " + id);
        EntityType type = EntityType.fromName(key.getKey());
        if (type == null) throw new IllegalArgumentException("Unknown entity type: " + id);
        return enabled(type, nbt);
    }

    public boolean enabled() {
        return entityType != null;
    }

    @Override
    public EntityType entityType() {
        EntityPlaybackActor.queuePlayerAsEntityNbt(entityNbt);
        return entityType;
    }

    public String entityId() {
        return entityType == null ? null : entityType.getKey().toString();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (!enabled()) return json;
        json.addProperty("id", entityId());
        if (entityNbt != null) json.addProperty("nbt", entityNbt);
        return json;
    }

    public static PlayerAsEntity fromJson(JsonObject json) {
        if (json == null || !json.has("id")) return DISABLED;
        return fromId(json.get("id").getAsString(), json.has("nbt") ? json.get("nbt").getAsString() : null);
    }
}
