package com.ultraop.mocap.scene;

import com.ultraop.mocap.playback.PlaybackModifiers;
import com.ultraop.mocap.playback.PlayerAsEntity;
import com.ultraop.mocap.playback.PlayerSkin;
import com.ultraop.mocap.recording.RecordingManager;
import com.ultraop.mocap.recording.RecordingSession;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Persistent scene manager with compatibility for the pre-JSON legacy scene format. */
public final class SceneManager {
    private static final String NULL_TOKEN = "[null]";
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

    public List<String> getRecordingNames() {
        return recordingManager.getSavedNames().stream().sorted().toList();
    }

    public boolean exists(String name) { return Files.isRegularFile(path(name)); }

    public SceneData load(String name) throws IOException {
        Path file = path(name);
        if (!Files.isRegularFile(file)) return null;
        String text = Files.readString(file, StandardCharsets.UTF_8);
        String trimmed = text.trim();
        if (trimmed.startsWith("{")) return SceneData.fromJsonString(text);
        return parseLegacyScene(trimmed);
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

    public RecordingSession resolveRecording(SceneElement element) {
        return recordingManager.getSaved(element.name());
    }

    private SceneData parseLegacyScene(String text) {
        String[] lines = text.split("\\R", -1);
        if (lines.length == 0 || lines[0].trim().isEmpty()) throw new IllegalArgumentException("Legacy scene version not specified");
        try { Integer.parseInt(lines[0].trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid scene version: " + lines[0].trim(), e); }

        SceneData scene = new SceneData();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] t = line.split("\\s+");
            if (t.length == 0) continue;
            PlaybackModifiers modifiers = PlaybackModifiers.DEFAULT;
            int p = 1;
            try {
                if (p < t.length) modifiers = modifiers.withWaitOnStart(Double.parseDouble(t[p++]));
                if (p + 2 < t.length) {
                    double x = Double.parseDouble(t[p++]);
                    double y = Double.parseDouble(t[p++]);
                    double z = Double.parseDouble(t[p++]);
                    modifiers = modifiers.withOffset(x, y, z)
                            .withTransformationConfig(modifiers.transformationConfig().withRoundBlockPos(true));
                }
                if (p < t.length && !NULL_TOKEN.equals(t[p])) {
                    String playerName = t[p];
                    if (playerName.length() <= 16) modifiers = modifiers.withPlayerName(playerName);
                }
                p++;
                if (p < t.length) {
                    String skinPath = t[p++];
                    String sourceToken = p < t.length ? t[p] : "0";
                    if (!NULL_TOKEN.equals(skinPath)) {
                        int source = Integer.parseInt(sourceToken);
                        modifiers = modifiers.withPlayerSkin(switch (source) {
                            case 1 -> PlayerSkin.fromPlayer(skinPath);
                            case 2 -> PlayerSkin.fromFile(skinPath);
                            case 3 -> PlayerSkin.fromMineSkin(skinPath);
                            default -> PlayerSkin.DEFAULT;
                        });
                    }
                    p++;
                }
                if (p < t.length && !NULL_TOKEN.equals(t[p])) {
                    String entityId = t[p];
                    EntityType type = EntityType.fromName(entityId.startsWith("minecraft:") ? entityId.substring(10) : entityId);
                    if (type != null) modifiers = modifiers.withPlayerAsEntity(PlayerAsEntity.enabled(type, null));
                }
            } catch (RuntimeException ignored) {
                // Preserve the scene element even when optional legacy modifier data is malformed.
            }
            scene.add(new SceneElement(t[0], modifiers));
        }
        return scene;
    }

    private Path path(String name) { return directory.resolve(name + ".json"); }

    private static void validateName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank() || name.length() > 128 || !name.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid scene name: " + name);
        }
    }
}
