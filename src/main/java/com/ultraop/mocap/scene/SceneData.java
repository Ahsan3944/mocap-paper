package com.ultraop.mocap.scene;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ultraop.mocap.playback.PlaybackModifiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Versioned JSON representation of a MoCap scene. */
public final class SceneData {
    public static final int CURRENT_VERSION = 1;

    private int version = CURRENT_VERSION;
    private boolean experimentalVersion;
    private final List<SceneElement> elements = new ArrayList<>();

    public int version() { return version; }
    public boolean experimentalVersion() { return experimentalVersion; }
    public List<SceneElement> elements() { return Collections.unmodifiableList(elements); }

    public SceneData add(String name) { elements.add(new SceneElement(name, PlaybackModifiers.DEFAULT)); return this; }
    public SceneData add(SceneElement element) { elements.add(element); return this; }
    public boolean remove(int position) { if (position < 1 || position > elements.size()) return false; elements.remove(position - 1); return true; }
    public void clear() { elements.clear(); }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        // Upstream uses a negative version number to mark an experimental scene format.
        root.addProperty("version", experimentalVersion ? -version : version);
        JsonArray array = new JsonArray();
        elements.forEach(e -> array.add(e.toJson()));
        root.add("subscenes", array);
        return root;
    }

    public static SceneData fromJson(JsonObject root) {
        if (root == null || !root.has("version")) throw new IllegalArgumentException("Scene version not specified");
        int rawVersion = root.get("version").getAsInt();
        SceneData data = new SceneData();
        data.version = Math.abs(rawVersion);
        data.experimentalVersion = rawVersion < 0;
        if (data.version > CURRENT_VERSION) throw new IllegalArgumentException("Unsupported scene version: " + data.version);
        if (!root.has("subscenes") || !root.get("subscenes").isJsonArray()) throw new IllegalArgumentException("Scene subscenes list not found");
        for (var element : root.getAsJsonArray("subscenes")) {
            if (!element.isJsonObject()) throw new IllegalArgumentException("Scene subscene isn't a JSON object");
            data.elements.add(SceneElement.fromJson(element.getAsJsonObject()));
        }
        return data;
    }

    public String toJsonString(boolean pretty) {
        Gson gson = pretty ? new GsonBuilder().setPrettyPrinting().create() : new GsonBuilder().create();
        return gson.toJson(toJson());
    }

    public static SceneData fromJsonString(String text) {
        return fromJson(new Gson().fromJson(text, JsonObject.class));
    }
}
