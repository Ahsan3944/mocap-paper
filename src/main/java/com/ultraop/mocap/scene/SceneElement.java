package com.ultraop.mocap.scene;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ultraop.mocap.playback.EntityFilter;
import com.ultraop.mocap.playback.PlaybackModifiers;
import com.ultraop.mocap.playback.PlayerAsEntity;
import com.ultraop.mocap.playback.PlayerSkin;

import java.util.Locale;

/** A named playable element in a MoCap scene. */
public record SceneElement(String name, PlaybackModifiers modifiers) {
    public SceneElement {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Scene element name cannot be blank");
        if (modifiers == null) modifiers = PlaybackModifiers.DEFAULT;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        if (modifiers.playerName() != null) json.addProperty("player_name", modifiers.playerName());
        if (!modifiers.playerSkin().isDefault()) json.add("player_skin", modifiers.playerSkin().toJson());
        if (modifiers.playerAsEntity().enabled()) json.add("player_as_entity", modifiers.playerAsEntity().toJson());
        if (modifiers.entityFilter().enabled()) json.addProperty("entity_filter", modifiers.entityFilter().expression());

        PlaybackModifiers.TransformationConfig config = modifiers.transformationConfig();
        JsonObject transformations = new JsonObject();
        boolean hasTransformations = false;
        if (modifiers.rotationDegrees() != 0.0) { transformations.addProperty("rotation", modifiers.rotationDegrees()); hasTransformations = true; }
        if (modifiers.mirror() != PlaybackModifiers.Mirror.NONE) { transformations.addProperty("mirror", modifiers.mirror().name().toLowerCase(Locale.ROOT)); hasTransformations = true; }
        if (modifiers.playerScale() != 1.0 || modifiers.sceneScale() != 1.0) {
            JsonObject scale = new JsonObject();
            if (modifiers.playerScale() != 1.0) scale.addProperty("player_scale", modifiers.playerScale());
            if (modifiers.sceneScale() != 1.0) scale.addProperty("scene_scale", modifiers.sceneScale());
            transformations.add("scale", scale); hasTransformations = true;
        }
        if (modifiers.offsetX() != 0.0 || modifiers.offsetY() != 0.0 || modifiers.offsetZ() != 0.0) {
            JsonArray a = new JsonArray(); a.add(modifiers.offsetX()); a.add(modifiers.offsetY()); a.add(modifiers.offsetZ());
            transformations.add("offset", a); hasTransformations = true;
        }
        if (!config.isDefault()) {
            JsonObject c = new JsonObject();
            if (config.roundBlockPos()) c.addProperty("round_block_pos", true);
            if (config.recordingCenter() != PlaybackModifiers.RecordingCenter.AUTO) c.addProperty("recording_center", config.recordingCenter().name().toLowerCase(Locale.ROOT));
            if (config.sceneCenterType() != PlaybackModifiers.SceneCenterType.COMMON_FIRST) {
                JsonObject sc = new JsonObject();
                sc.addProperty("type", config.sceneCenterType().name().toLowerCase(Locale.ROOT));
                if (config.sceneCenterType() == PlaybackModifiers.SceneCenterType.COMMON_SPECIFIC && config.sceneCenterSpecific() != null) sc.addProperty("specific_str", config.sceneCenterSpecific());
                c.add("scene_center", sc);
            }
            if (config.centerOffsetX() != 0.0 || config.centerOffsetY() != 0.0 || config.centerOffsetZ() != 0.0) {
                JsonArray a = new JsonArray(); a.add(config.centerOffsetX()); a.add(config.centerOffsetY()); a.add(config.centerOffsetZ());
                c.add("center_offset", a);
            }
            transformations.add("config", c); hasTransformations = true;
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
        PlaybackModifiers p = PlaybackModifiers.DEFAULT;
        if (json.has("player_name")) p = p.withPlayerName(json.get("player_name").getAsString());
        if (json.has("player_skin") && json.get("player_skin").isJsonObject()) p = p.withPlayerSkin(PlayerSkin.fromJson(json.getAsJsonObject("player_skin")));
        if (json.has("player_as_entity") && json.get("player_as_entity").isJsonObject()) p = p.withPlayerAsEntity(PlayerAsEntity.fromJson(json.getAsJsonObject("player_as_entity")));
        if (json.has("entity_filter")) p = p.withEntityFilter(new EntityFilter(json.get("entity_filter").getAsString()));

        if (json.has("transformations")) {
            JsonObject t = json.getAsJsonObject("transformations");
            if (t.has("rotation")) p = p.withRotation(t.get("rotation").getAsDouble());
            if (t.has("mirror")) p = p.withMirror(PlaybackModifiers.Mirror.valueOf(t.get("mirror").getAsString().toUpperCase(Locale.ROOT)));
            if (t.has("scale")) { JsonObject s = t.getAsJsonObject("scale"); if (s.has("player_scale")) p = p.withPlayerScale(s.get("player_scale").getAsDouble()); if (s.has("scene_scale")) p = p.withSceneScale(s.get("scene_scale").getAsDouble()); }
            if (t.has("offset")) { JsonArray a = t.getAsJsonArray("offset"); if (a.size() != 3) throw new IllegalArgumentException("Transformation offset must contain 3 values"); p = p.withOffset(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()); }
            if (t.has("config")) {
                JsonObject c = t.getAsJsonObject("config"); PlaybackModifiers.TransformationConfig tc = p.transformationConfig();
                if (c.has("round_block_pos")) tc = tc.withRoundBlockPos(c.get("round_block_pos").getAsBoolean());
                if (c.has("recording_center")) tc = tc.withRecordingCenter(PlaybackModifiers.RecordingCenter.valueOf(c.get("recording_center").getAsString().toUpperCase(Locale.ROOT)));
                if (c.has("scene_center")) { JsonObject sc = c.getAsJsonObject("scene_center"); PlaybackModifiers.SceneCenterType type = sc.has("type") ? PlaybackModifiers.SceneCenterType.valueOf(sc.get("type").getAsString().toUpperCase(Locale.ROOT)) : PlaybackModifiers.SceneCenterType.COMMON_FIRST; tc = tc.withSceneCenter(type, sc.has("specific_str") ? sc.get("specific_str").getAsString() : null); }
                if (c.has("center_offset")) { JsonArray a = c.getAsJsonArray("center_offset"); if (a.size() != 3) throw new IllegalArgumentException("Center offset must contain 3 values"); tc = tc.withCenterOffset(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()); }
                p = p.withTransformationConfig(tc);
            }
        }
        if (json.has("time")) { JsonObject time = json.getAsJsonObject("time"); if (time.has("start_delay")) p = p.withStartDelay(time.get("start_delay").getAsDouble()); if (time.has("wait_on_start")) p = p.withWaitOnStart(time.get("wait_on_start").getAsDouble()); if (time.has("wait_on_end")) p = p.withWaitOnEnd(time.get("wait_on_end").getAsDouble()); if (time.has("wait_for_parent_end")) p = p.withWaitForParentEnd(time.get("wait_for_parent_end").getAsBoolean()); if (time.has("loop")) p = p.withLoop(time.get("loop").getAsBoolean()); }
        return new SceneElement(json.get("name").getAsString(), p);
    }
}
