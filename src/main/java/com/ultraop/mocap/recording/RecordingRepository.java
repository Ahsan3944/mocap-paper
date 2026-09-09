package com.ultraop.mocap.recording;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent storage for saved MoCap recordings. */
public final class RecordingRepository {
    private static final int FORMAT_VERSION = 1;
    private final Path directory;

    public RecordingRepository(Path directory) {
        this.directory = directory;
    }

    public void initialize() throws IOException {
        Files.createDirectories(directory);
    }

    public void save(String name, RecordingSession session) throws IOException {
        validateName(name);
        initialize();
        Path target = pathFor(name);

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(target)))) {
            out.writeInt(FORMAT_VERSION);
            out.writeUTF(session.getId().toString());
            out.writeUTF(session.getSourcePlayerId().toString());
            out.writeUTF(session.getSourcePlayerName());
            out.writeUTF(session.getStartedAt().toString());
            out.writeBoolean(session.getStoppedAt() != null);
            if (session.getStoppedAt() != null) {
                out.writeUTF(session.getStoppedAt().toString());
            }
            out.writeInt(session.getFrames().size());

            try (BukkitObjectOutputStream objects = new BukkitObjectOutputStream(out)) {
                for (PlayerStateFrame frame : session.getFrames()) {
                    writeFrame(objects, frame);
                }
            }
        }
    }

    public RecordingSession load(String name) throws IOException {
        validateName(name);
        Path source = pathFor(name);
        if (!Files.isRegularFile(source)) {
            return null;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(source)))) {
            int version = in.readInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("Unsupported recording format version: " + version);
            }

            UUID id = UUID.fromString(in.readUTF());
            UUID playerId = UUID.fromString(in.readUTF());
            String playerName = in.readUTF();
            Instant startedAt = Instant.parse(in.readUTF());
            Instant stoppedAt = in.readBoolean() ? Instant.parse(in.readUTF()) : null;
            int frameCount = in.readInt();
            if (frameCount < 0 || frameCount > 10_000_000) {
                throw new IOException("Invalid frame count: " + frameCount);
            }

            List<PlayerStateFrame> frames = new ArrayList<>(frameCount);
            try (BukkitObjectInputStream objects = new BukkitObjectInputStream(in)) {
                for (int i = 0; i < frameCount; i++) {
                    frames.add(readFrame(objects));
                }
            }
            return RecordingSession.loaded(id, playerId, playerName, startedAt, stoppedAt, frames);
        } catch (EOFException e) {
            throw new IOException("Truncated recording: " + name, e);
        }
    }

    public Map<String, RecordingSession> loadAll() throws IOException {
        initialize();
        Map<String, RecordingSession> result = new LinkedHashMap<>();
        List<Path> files;
        try (var stream = Files.list(directory)) {
            files = stream.filter(path -> path.getFileName().toString().endsWith(".mocap"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            String name = fileName.substring(0, fileName.length() - ".mocap".length());
            result.put(name, load(name));
        }
        return result;
    }

    public boolean delete(String name) throws IOException {
        validateName(name);
        return Files.deleteIfExists(pathFor(name));
    }

    public boolean exists(String name) {
        return Files.isRegularFile(pathFor(name));
    }

    public List<String> list() throws IOException {
        initialize();
        try (var stream = Files.list(directory)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".mocap"))
                    .map(path -> path.getFileName().toString())
                    .map(name -> name.substring(0, name.length() - ".mocap".length()))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
    }

    private Path pathFor(String name) {
        return directory.resolve(name + ".mocap").normalize();
    }

    private static void validateName(String name) throws IOException {
        if (name == null || name.isBlank() || name.length() > 128 || !name.matches("[A-Za-z0-9._-]+")) {
            throw new IOException("Invalid recording name. Use letters, numbers, '.', '_' or '-'.");
        }
    }

    private static void writeFrame(ObjectOutputStream out, PlayerStateFrame frame) throws IOException {
        out.writeLong(frame.tick());
        out.writeUTF(frame.worldKey());
        out.writeDouble(frame.x());
        out.writeDouble(frame.y());
        out.writeDouble(frame.z());
        out.writeFloat(frame.yaw());
        out.writeFloat(frame.pitch());
        out.writeDouble(frame.velocityX());
        out.writeDouble(frame.velocityY());
        out.writeDouble(frame.velocityZ());
        out.writeBoolean(frame.onGround());
        out.writeBoolean(frame.sprinting());
        out.writeBoolean(frame.sneaking());
        out.writeBoolean(frame.swimming());
        out.writeBoolean(frame.gliding());
        out.writeBoolean(frame.flying());
        out.writeFloat(frame.fallDistance());
        out.writeInt(frame.fireTicks());
        out.writeBoolean(frame.invisible());
        out.writeBoolean(frame.glowing());
        out.writeBoolean(frame.invulnerable());
        out.writeDouble(frame.health());
        out.writeUTF(frame.poseEntityType().name());
        out.writeObject(frame.mainHand());
        out.writeObject(frame.offHand());
        out.writeInt(frame.armor().length);
        for (ItemStack item : frame.armor()) {
            out.writeObject(item);
        }
    }

    private static PlayerStateFrame readFrame(ObjectInputStream in) throws IOException {
        try {
            long tick = in.readLong();
            String worldKey = in.readUTF();
            double x = in.readDouble();
            double y = in.readDouble();
            double z = in.readDouble();
            float yaw = in.readFloat();
            float pitch = in.readFloat();
            double velocityX = in.readDouble();
            double velocityY = in.readDouble();
            double velocityZ = in.readDouble();
            boolean onGround = in.readBoolean();
            boolean sprinting = in.readBoolean();
            boolean sneaking = in.readBoolean();
            boolean swimming = in.readBoolean();
            boolean gliding = in.readBoolean();
            boolean flying = in.readBoolean();
            float fallDistance = in.readFloat();
            int fireTicks = in.readInt();
            boolean invisible = in.readBoolean();
            boolean glowing = in.readBoolean();
            boolean invulnerable = in.readBoolean();
            double health = in.readDouble();
            org.bukkit.entity.EntityType entityType = org.bukkit.entity.EntityType.valueOf(in.readUTF());
            ItemStack mainHand = (ItemStack) in.readObject();
            ItemStack offHand = (ItemStack) in.readObject();
            int armorCount = in.readInt();
            if (armorCount < 0 || armorCount > 8) {
                throw new IOException("Invalid armor count: " + armorCount);
            }
            ItemStack[] armor = new ItemStack[armorCount];
            for (int i = 0; i < armorCount; i++) {
                armor[i] = (ItemStack) in.readObject();
            }
            return new PlayerStateFrame(tick, worldKey, x, y, z, yaw, pitch,
                    velocityX, velocityY, velocityZ, onGround, sprinting, sneaking,
                    swimming, gliding, flying, fallDistance, fireTicks, invisible,
                    glowing, invulnerable, health, entityType, mainHand, offHand, armor);
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to deserialize recording item data", e);
        }
    }
}
