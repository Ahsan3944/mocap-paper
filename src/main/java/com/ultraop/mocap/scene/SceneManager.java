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
import java.util.Scanner;

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
        try (Scanner scanner = new Scanner(text)) {
            if (!scanner.hasNext()) throw new IllegalArgumentException("Legacy scene version not specified");

            final int version;
            try {
                version = Integer.parseInt(scanner.next());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid legacy scene version", e);
            }

            // Legacy scene files store one subscene per line after the version token.
            SceneData scene = new SceneData();
            if (scanner.hasNextLine()) scanner.nextLine();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.isBlank()) continue;
                try (Scanner lineScanner = new Scanner(line)) {
                    scene.add(parseLegacyElement(lineScanner));
                }
            }
            return scene;
        }
    }

    private SceneElement parseLegacyElement(Scanner scanner) {
        if (!scanner.hasNext()) throw new IllegalArgumentException("Legacy scene element name not specified");
        String elementName = scanner.next();
        PlaybackModifiers modifiers = PlaybackModifiers.DEFAULT;
        try {
            if (scanner.hasNext()) {
                modifiers = modifiers.withWaitOnStart(Double.parseDouble(scanner.next()));
            }
            if (scanner.hasNext()) {
                double x = Double.parseDouble(scanner.next());
                double y = Double.parseDouble(scanner.next());
                double z = Double.parseDouble(scanner.next());
                modifiers = modifiers.withOffset(x, y, z)
                        .withTransformationConfig(modifiers.transformationConfig().withRoundBlockPos(true));
            }
            modifiers = modifiers.withPlayerName(parseLegacyPlayerName(scanner));
            modifiers = modifiers.withPlayerSkin(parseLegacyPlayerSkin(scanner));

            if (scanner.hasNext()) {
                String entityId = scanner.next();
                if (!NULL_TOKEN.equals(entityId)) {
                    EntityType type = EntityType.fromName(entityId.startsWith("minecraft:") ? entityId.substring("minecraft:".length()) : entityId);
                    if (type != null) modifiers = modifiers.withPlayerAsEntity(PlayerAsEntity.enabled(type, null));
                }
            }
        } catch (RuntimeException ignored) {
            // Match upstream legacy compatibility: malformed optional fields do not discard the element.
        }
        return new SceneElement(elementName, modifiers);
    }

    private static String parseLegacyPlayerName(Scanner scanner) {
        if (!scanner.hasNext()) return null;
        String value = scanner.next();
        return !NULL_TOKEN.equals(value) && value.length() <= 16 ? value : null;
    }

    private static PlayerSkin parseLegacyPlayerSkin(Scanner scanner) {
        if (!scanner.hasNext()) return PlayerSkin.DEFAULT;
        String skinPath = scanner.next();
        int sourceId = 0;
        if (scanner.hasNext()) {
            try { sourceId = Integer.parseInt(scanner.next()); }
            catch (NumberFormatException ignored) { return PlayerSkin.DEFAULT; }
        }
        if (NULL_TOKEN.equals(skinPath)) return PlayerSkin.DEFAULT;
        return switch (sourceId) {
            case 1 -> PlayerSkin.fromPlayer(skinPath);
            case 2 -> PlayerSkin.fromFile(skinPath);
            case 3 -> PlayerSkin.fromMineSkin(skinPath);
            default -> PlayerSkin.DEFAULT;
        };
    }

    private Path path(String name) { return directory.resolve(name + ".json"); }

    private static void validateName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank() || name.length() > 128 || !name.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid scene name: " + name);
        }
    }
}
