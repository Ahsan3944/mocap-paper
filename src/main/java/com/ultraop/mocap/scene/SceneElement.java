package com.ultraop.mocap.scene;

import com.google.gson.JsonObject;
import com.ultraop.mocap.playback.PlaybackModifiers;

/** A named playable element in a MoCap scene. */
public record SceneElement(String name, PlaybackModifiers modifiers) {
    public SceneElement {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Scene element name cannot be blank");
        if (modifiers == null) modifiers = PlaybackModifiers.DEFAULT;
    }

    /**
     * Uses the upstream scene layout: name at the element level and modifier groups
     * under player_skin/player_as_entity/transformations/time/entity_filter.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);

        if (modifiers.playerName() != null) json.addProperty("player_name", modifiers.playerName());

        if (modifiers.playerSkin() != null) {
            JsonObject skin = new JsonObject();
            skin.addProperty("skin_source", "from_file");
            skin.addProperty("skin_path", modifiers.playerSkin());
            json.add("player_skin", skin);
        }

        if (modifiers.playerAsEntity()) {
            JsonObject entity = new JsonObject();
            entity.addProperty("id", "minecraft:player");
            json.add("player_as_entity", entity);
        }

        if (modifiers.entityFilter() != null) json.addProperty("entity_filter", modifiers.entityFilter());

        JsonObject transformations = new JsonObject();
        boolean hasTransformations = false;
        if (modifiers.rotationDegrees() != 0.0) {
            transformations.addProperty("rotation", modifiers.rotationDegrees());
            hasTransformations = true;
        }
        if (modifiers.mirror() != PlaybackModifiers.Mirror.NONE) {
            transformations.addProperty("mirror", modifiers.mirror().name().toLowerCase(java.util.Locale.ROOT));
            hasTransformations = true;
        }
        if (modifiers.playerScale() != 1.0 || modifiers.sceneScale() != 1.0) {
            JsonObject scale = new JsonObject();
            if (modifiers.playerScale() != 1.0) scale.addProperty("player_scale", modifiers.playerScale());
            if (modifiers.sceneScale() != 1.0) scale.addProperty("scene_scale", modifiers.sceneScale());
            transformations.add("scale", scale);
            hasTransformations = true;
        }
        if (modifiers.offsetX() != 0.0 || modifiers.offsetY() != 0.0 || modifiers.offsetZ() != 0.0) {
            com.google.gson.JsonArray offset = new com.google.gson.JsonArray();
            offset.add(modifiers.offsetX());
            offset.add(modifiers.offsetY());
            offset.add(modifiers.offsetZ());
            transformations.add("offset", offset);
            hasTransformations = true;
        }
        if (hasTransformations) json.add("transformations", transformations);

        JsonObject time = new JsonObject();
        boolean hasTime = false;
        if (modifiers.startDelaySeconds() != 0.0) { time.addProperty("start_delay", modifiers.startDelaySeconds()); hasTime = true; }
        if (modifiers.waitOnStartSeconds() != 0.0) { time.addProperty("wait_on_start", modifiers.waitOnStartSeconds()); hasTime = true; }
        if (modifiers.waitOnEndSeconds() != 0.0) { time.addProperty("wait_on_end", modifiers.waitOnEndSeconds()); hasTime = true; }
        if (!modifiers.waitForParentEnd()) { time.addProperty("wait_for_parent_end", false); hasTime = true; }
        if (modifiers.loop()) { time.addProperty("loop", true); hasTime = true; }
        if (hasTime) json.add("time", time);

        return json;
    }

    public static SceneElement fromJson(JsonObject json) {
        if (!json.has("name")) throw new IllegalArgumentException("JSON \"name\" element not found");
        String name = json.get("name").getAsString();
        PlaybackModifiers p = PlaybackModifiers.DEFAULT;

        if (json.has("player_name")) p = p.withPlayerName(json.get("player_name").getAsString());

        if (json.has("player_skin") && json.get("player_skin").isJsonObject()) {
            JsonObject skin = json.getAsJsonObject("player_skin");
            if (skin.has("skin_path")) p = p.withPlayerSkin(skin.get("skin_path").getAsString());
        } else if (json.has("player_skin")) {
            // Compatibility with the preliminary Paper format.
            p = p.withPlayerSkin(json.get("player_skin").getAsString());
        }

        if (json.has("player_as_entity")) {
            JsonObject entity = json.get("player_as_entity").isJsonObject()
                    ? json.getAsJsonObject("player_as_entity") : null;
            p = p.withPlayerAsEntity(entity != null && entity.has("id"));
        }
        if (json.has("entity_filter")) p = p.withEntityFilter(json.get("entity_filter").getAsString());

        if (json.has("transformations")) {
            JsonObject t = json.getAsJsonObject("transformations");
            if (t.has("rotation")) p = p.withRotation(t.get("rotation").getAsDouble());
            if (t.has("mirror")) p = p.withMirror(PlaybackModifiers.Mirror.valueOf(t.get("mirror").getAsString().toUpperCase(java.util.Locale.ROOT)));
            if (t.has("scale")) {
                JsonObject s = t.getAsJsonObject("scale");
                if (s.has("player_scale")) p = p.withPlayerScale(s.get("player_scale").getAsDouble());
                if (s.has("scene_scale")) p = p.withSceneScale(s.get("scene_scale").getAsDouble());
            }
            if (t.has("offset")) {
                var a = t.getAsJsonArray("offset");
                if (a.size() != 3) throw new IllegalArgumentException("Transformation offset must contain 3 values");
                p = p.withOffset(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
            }
        }

        if (json.has("time")) {
            JsonObject time = json.getAsJsonObject("time");
            if (time.has("start_delay")) p = p.withStartDelay(time.get("start_delay").getAsDouble());
            if (time.has("wait_on_start")) p = p.withWaitOnStart(time.get("wait_on_start").getAsDouble());
            if (time.has("wait_on_end")) p = p.withWaitOnEnd(time.get("wait_on_end").getAsDouble());
            if (time.has("wait_for_parent_end")) p = p.withWaitForParentEnd(time.get("wait_for_parent_end").getAsBoolean());
            if (time.has("loop")) p = p.withLoop(time.get("loop").getAsBoolean());
        }

        return new SceneElement(name, p);
    }
}
