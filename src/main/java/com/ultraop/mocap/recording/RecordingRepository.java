package com.ultraop.mocap.recording;

import org.bukkit.entity.Pose;
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
    private static final int FORMAT_VERSION = 2;
    private final Path directory;

    public RecordingRepository(Path directory) { this.directory = directory; }
    public void initialize() throws IOException { Files.createDirectories(directory); }

    public void save(String name, RecordingSession session) throws IOException {
        validateName(name); initialize(); Path target = pathFor(name);
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(target)))) {
            out.writeInt(FORMAT_VERSION);
            out.writeUTF(session.getId().toString());
            out.writeUTF(session.getSourcePlayerId().toString());
            out.writeUTF(session.getSourcePlayerName());
            out.writeUTF(session.getStartedAt().toString());
            out.writeBoolean(session.getStoppedAt() != null);
            if (session.getStoppedAt() != null) out.writeUTF(session.getStoppedAt().toString());
            out.writeInt(session.getFrames().size());
            out.writeInt(session.getEntityFrames().size());
            try (BukkitObjectOutputStream objects = new BukkitObjectOutputStream(out)) {
                for (PlayerStateFrame frame : session.getFrames()) writeFrame(objects, frame);
                for (var entry : session.getEntityFrames().entrySet()) {
                    out.writeUTF(entry.getKey().toString());
                    out.writeInt(entry.getValue().size());
                    for (EntityStateFrame frame : entry.getValue()) writeEntityFrame(objects, frame);
                }
            }
        }
    }

    public RecordingSession load(String name) throws IOException {
        validateName(name); Path source = pathFor(name);
        if (!Files.isRegularFile(source)) return null;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(source)))) {
            int version = in.readInt();
            if (version != FORMAT_VERSION) throw new IOException("Unsupported recording format version: " + version);
            UUID id = UUID.fromString(in.readUTF());
            UUID playerId = UUID.fromString(in.readUTF());
            String playerName = in.readUTF();
            Instant startedAt = Instant.parse(in.readUTF());
            Instant stoppedAt = in.readBoolean() ? Instant.parse(in.readUTF()) : null;
            int frameCount = in.readInt();
            int entityCount = in.readInt();
            if (frameCount < 0 || frameCount > 10_000_000 || entityCount < 0 || entityCount > 1_000_000)
                throw new IOException("Invalid recording counts");
            List<PlayerStateFrame> frames = new ArrayList<>(frameCount);
            Map<UUID, List<EntityStateFrame>> entities = new LinkedHashMap<>();
            try (BukkitObjectInputStream objects = new BukkitObjectInputStream(in)) {
                for (int i = 0; i < frameCount; i++) frames.add(readFrame(objects));
                for (int i = 0; i < entityCount; i++) {
                    UUID entityId = UUID.fromString(in.readUTF());
                    int count = in.readInt();
                    if (count < 0 || count > 10_000_000) throw new IOException("Invalid entity frame count");
                    List<EntityStateFrame> values = new ArrayList<>(count);
                    for (int j = 0; j < count; j++) values.add(readEntityFrame(objects));
                    entities.put(entityId, values);
                }
            }
            return RecordingSession.loaded(id, playerId, playerName, startedAt, stoppedAt, frames, entities);
        } catch (EOFException e) { throw new IOException("Truncated recording: " + name, e); }
    }

    public Map<String, RecordingSession> loadAll() throws IOException {
        initialize(); Map<String, RecordingSession> result = new LinkedHashMap<>(); List<Path> files;
        try (var stream = Files.list(directory)) {
            files = stream.filter(path -> path.getFileName().toString().endsWith(".mocap"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
        }
        for (Path file : files) {
            String f = file.getFileName().toString();
            result.put(f.substring(0, f.length() - 6), load(f.substring(0, f.length() - 6)));
        }
        return result;
    }
    public boolean delete(String name) throws IOException { validateName(name); return Files.deleteIfExists(pathFor(name)); }
    public boolean exists(String name) { return Files.isRegularFile(pathFor(name)); }
    public List<String> list() throws IOException {
        initialize();
        try (var stream = Files.list(directory)) {
            return stream.filter(p -> p.getFileName().toString().endsWith(".mocap"))
                    .map(p -> p.getFileName().toString().substring(0, p.getFileName().toString().length() - 6))
                    .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
    }
    private Path pathFor(String name) { return directory.resolve(name + ".mocap").normalize(); }
    private static void validateName(String name) throws IOException {
        if (name == null || name.isBlank() || name.length() > 128 || !name.matches("[A-Za-z0-9._-]+"))
            throw new IOException("Invalid recording name. Use letters, numbers, '.', '_' or '-'.");
    }

    private static void writeFrame(ObjectOutputStream out, PlayerStateFrame f) throws IOException {
        out.writeLong(f.tick()); out.writeUTF(f.worldKey()); out.writeDouble(f.x()); out.writeDouble(f.y()); out.writeDouble(f.z());
        out.writeFloat(f.yaw()); out.writeFloat(f.pitch()); out.writeDouble(f.velocityX()); out.writeDouble(f.velocityY()); out.writeDouble(f.velocityZ());
        out.writeBoolean(f.onGround()); out.writeBoolean(f.sprinting()); out.writeBoolean(f.sneaking()); out.writeBoolean(f.swimming()); out.writeBoolean(f.gliding()); out.writeBoolean(f.flying());
        out.writeUTF(f.pose().name()); out.writeFloat(f.fallDistance()); out.writeInt(f.fireTicks()); out.writeBoolean(f.invisible()); out.writeBoolean(f.glowing());
        out.writeBoolean(f.invulnerable()); out.writeDouble(f.health()); out.writeObject(f.mainHand()); out.writeObject(f.offHand()); out.writeInt(f.armor().length);
        for (ItemStack item : f.armor()) out.writeObject(item);
    }

    private static PlayerStateFrame readFrame(ObjectInputStream in) throws IOException {
        try {
            long tick=in.readLong(); String world=in.readUTF(); double x=in.readDouble(),y=in.readDouble(),z=in.readDouble(); float yaw=in.readFloat(),pitch=in.readFloat();
            double vx=in.readDouble(),vy=in.readDouble(),vz=in.readDouble(); boolean ground=in.readBoolean(),sprint=in.readBoolean(),sneak=in.readBoolean(),swim=in.readBoolean(),glide=in.readBoolean(),fly=in.readBoolean();
            Pose pose=Pose.valueOf(in.readUTF()); float fall=in.readFloat(); int fire=in.readInt(); boolean invisible=in.readBoolean(),glowing=in.readBoolean(),invulnerable=in.readBoolean(); double health=in.readDouble();
            ItemStack main=(ItemStack)in.readObject(),off=(ItemStack)in.readObject(); int ac=in.readInt(); if(ac<0||ac>8)throw new IOException("Invalid armor count"); ItemStack[] armor=new ItemStack[ac]; for(int i=0;i<ac;i++)armor[i]=(ItemStack)in.readObject();
            return new PlayerStateFrame(tick,world,x,y,z,yaw,pitch,vx,vy,vz,ground,sprint,sneak,swim,glide,fly,pose,fall,fire,invisible,glowing,invulnerable,health,main,off,armor);
        } catch(ClassNotFoundException|IllegalArgumentException e){throw new IOException("Invalid player frame",e);}
    }

    private static void writeEntityFrame(ObjectOutputStream out, EntityStateFrame f) throws IOException {
        out.writeLong(f.tick()); out.writeUTF(f.entityId().toString()); out.writeUTF(f.entityType()); out.writeUTF(f.worldKey());
        out.writeDouble(f.x()); out.writeDouble(f.y()); out.writeDouble(f.z()); out.writeFloat(f.yaw()); out.writeFloat(f.pitch());
        out.writeDouble(f.velocityX()); out.writeDouble(f.velocityY()); out.writeDouble(f.velocityZ()); out.writeInt(f.fireTicks());
        out.writeBoolean(f.invisible()); out.writeBoolean(f.glowing()); out.writeBoolean(f.invulnerable()); out.writeDouble(f.health());
        out.writeObject(f.mainHand()); out.writeObject(f.offHand()); out.writeInt(f.armor().length); for(ItemStack item:f.armor())out.writeObject(item);
    }

    private static EntityStateFrame readEntityFrame(ObjectInputStream in) throws IOException {
        try {
            long tick=in.readLong(); UUID id=UUID.fromString(in.readUTF()); String type=in.readUTF(),world=in.readUTF(); double x=in.readDouble(),y=in.readDouble(),z=in.readDouble(); float yaw=in.readFloat(),pitch=in.readFloat();
            double vx=in.readDouble(),vy=in.readDouble(),vz=in.readDouble(); int fire=in.readInt(); boolean invisible=in.readBoolean(),glowing=in.readBoolean(),invulnerable=in.readBoolean(); double health=in.readDouble();
            ItemStack main=(ItemStack)in.readObject(),off=(ItemStack)in.readObject(); int ac=in.readInt(); if(ac<0||ac>8)throw new IOException("Invalid entity armor count"); ItemStack[] armor=new ItemStack[ac]; for(int i=0;i<ac;i++)armor[i]=(ItemStack)in.readObject();
            return new EntityStateFrame(tick,id,type,world,x,y,z,yaw,pitch,vx,vy,vz,fire,invisible,glowing,invulnerable,health,main,off,armor);
        } catch(ClassNotFoundException|IllegalArgumentException e){throw new IOException("Invalid entity frame",e);}
    }
}
