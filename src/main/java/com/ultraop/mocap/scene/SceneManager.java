package com.ultraop.mocap.scene;

import com.ultraop.mocap.playback.PlaybackModifiers;
import com.ultraop.mocap.recording.RecordingManager;
import com.ultraop.mocap.recording.RecordingSession;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Persistent scene manager. Scene names intentionally share the same safe filename rules as recordings. */
public final class SceneManager {
    private final Path directory;
    private final RecordingManager recordingManager;

    public SceneManager(JavaPlugin plugin, RecordingManager recordingManager) {
        this.directory = plugin.getDataFolder().toPath().resolve("scenes");
        this.recordingManager = recordingManager;
    }

    public void start() throws IOException { Files.createDirectories(directory); }

    public List<String> getSceneNames() throws IOException {
        if (!Files.exists(directory)) return List.of();
        try (var stream = Files.list(directory)) {
            return stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().substring(0, p.getFileName().toString().length() - 5))
                    .sorted().toList();
        }
    }

    public boolean exists(String name) { return Files.isRegularFile(path(name)); }

    public SceneData load(String name) throws IOException {
        Path file = path(name);
        if (!Files.isRegularFile(file)) return null;
        return SceneData.fromJsonString(Files.readString(file, StandardCharsets.UTF_8));
    }

    public void create(String name) throws IOException {
        validateName(name);
        Path file = path(name);
        if (Files.exists(file)) throw new IOException("Scene already exists: " + name);
        save(name, new SceneData());
    }

    public void save(String name, SceneData data) throws IOException {
        validateName(name);
        Files.createDirectories(directory);
        Files.writeString(path(name), data.toJsonString(true), StandardCharsets.UTF_8);
    }

    public boolean remove(String name) throws IOException { return Files.deleteIfExists(path(name)); }

    public boolean rename(String oldName, String newName) throws IOException {
        validateName(newName);
        if (!exists(oldName) || exists(newName)) return false;
        Files.move(path(oldName), path(newName));
        return true;
    }

    public boolean copy(String source, String destination) throws IOException {
        validateName(destination);
        if (!exists(source) || exists(destination)) return false;
        Files.copy(path(source), path(destination));
        return true;
    }

    /** Resolve a scene element as a saved recording. Nested scene resolution is deliberately deferred to ScenePlayback. */
    public RecordingSession resolveRecording(SceneElement element) {
        return recordingManager.getSaved(element.name());
    }

    private Path path(String name) { return directory.resolve(name + ".json"); }

    private static void validateName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank() || name.length() > 128 || !name.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid scene name: " + name);
        }
    }
}
