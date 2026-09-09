package com.ultraop.mocap.scene;

import com.google.gson.JsonObject;
import com.ultraop.mocap.playback.PlaybackModifiers;

/** A named playable element in a MoCap scene. */
public record SceneElement(String name, PlaybackModifiers modifiers) {
    public SceneElement {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Scene element name cannot be blank");
        if (modifiers == null) modifiers = PlaybackModifiers.DEFAULT;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        JsonObject m = new JsonObject();
        if (modifiers.playerName() != null) m.addProperty("player_name", modifiers.playerName());
        if (modifiers.playerSkin() != null) m.addProperty("player_skin", modifiers.playerSkin());
        if (modifiers.entityFilter() != null) m.addProperty("entity_filter", modifiers.entityFilter());
        m.addProperty("player_as_entity", modifiers.playerAsEntity());
        m.addProperty("start_delay", modifiers.startDelaySeconds());
        m.addProperty("wait_on_start", modifiers.waitOnStartSeconds());
        m.addProperty("wait_on_end", modifiers.waitOnEndSeconds());
        m.addProperty("wait_for_parent_end", modifiers.waitForParentEnd());
        m.addProperty("loop", modifiers.loop());
        m.addProperty("rotation", modifiers.rotationDegrees());
        m.addProperty("mirror", modifiers.mirror().name());
        m.addProperty("player_scale", modifiers.playerScale());
        m.addProperty("scene_scale", modifiers.sceneScale());
        m.addProperty("offset_x", modifiers.offsetX());
        m.addProperty("offset_y", modifiers.offsetY());
        m.addProperty("offset_z", modifiers.offsetZ());
        json.add("modifiers", m);
        return json;
    }

    public static SceneElement fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        JsonObject m = json.has("modifiers") ? json.getAsJsonObject("modifiers") : new JsonObject();
        PlaybackModifiers p = PlaybackModifiers.DEFAULT;
        if (m.has("player_name")) p = p.withPlayerName(m.get("player_name").getAsString());
        if (m.has("player_skin")) p = p.withPlayerSkin(m.get("player_skin").getAsString());
        if (m.has("entity_filter")) p = p.withEntityFilter(m.get("entity_filter").getAsString());
        if (m.has("player_as_entity")) p = p.withPlayerAsEntity(m.get("player_as_entity").getAsBoolean());
        if (m.has("start_delay")) p = p.withStartDelay(m.get("start_delay").getAsDouble());
        if (m.has("wait_on_start")) p = p.withWaitOnStart(m.get("wait_on_start").getAsDouble());
        if (m.has("wait_on_end")) p = p.withWaitOnEnd(m.get("wait_on_end").getAsDouble());
        if (m.has("wait_for_parent_end")) p = p.withWaitForParentEnd(m.get("wait_for_parent_end").getAsBoolean());
        if (m.has("loop")) p = p.withLoop(m.get("loop").getAsBoolean());
        if (m.has("rotation")) p = p.withRotation(m.get("rotation").getAsDouble());
        if (m.has("mirror")) p = p.withMirror(PlaybackModifiers.Mirror.valueOf(m.get("mirror").getAsString()));
        if (m.has("player_scale")) p = p.withPlayerScale(m.get("player_scale").getAsDouble());
        if (m.has("scene_scale")) p = p.withSceneScale(m.get("scene_scale").getAsDouble());
        if (m.has("offset_x") || m.has("offset_y") || m.has("offset_z")) {
            p = p.withOffset(m.has("offset_x") ? m.get("offset_x").getAsDouble() : 0,
                    m.has("offset_y") ? m.get("offset_y").getAsDouble() : 0,
                    m.has("offset_z") ? m.get("offset_z").getAsDouble() : 0);
        }
        return new SceneElement(name, p);
    }
}
