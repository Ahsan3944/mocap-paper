package com.ultraop.mocap.playback;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.ultraop.mocap.recording.BlockActionFrame;
import com.ultraop.mocap.recording.ChatMessageFrame;
import com.ultraop.mocap.recording.EntityStateFrame;
import com.ultraop.mocap.recording.PlayerStateFrame;
import com.ultraop.mocap.recording.RecordingSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tick-driven playback timeline backed by server-side playback actors. */
public final class PlaybackSession {
    private final UUID id = UUID.randomUUID();
    private final RecordingSession recording;
    private final UUID viewerPlayerId;
    private final List<PlayerStateFrame> frames;
    private final Map<UUID, List<EntityStateFrame>> entityFrames;
    private final List<BlockActionFrame> blockActions;
    private final List<ChatMessageFrame> chatMessages;
    private final Map<UUID, EntityPlaybackActor> entityActors = new LinkedHashMap<>();
    private final FakePlayer fakePlayer;
    private final EntityPlaybackActor entityActor;
    private final PlaybackModifiers modifiers;
    private final PositionTransformer transformer;
    private final World targetWorld;
    private final boolean root, blockActionsPlayback, blockInitialization, blockAllowScaled, invulnerablePlayback,
            preventTrackingPlayedEntities, assignProfile, chatPlayback;
    private final String playerNameHandling;
    private final EntityFilter playEntities;
    private long tick, waitTicks, waitOnEnd;
    private boolean paused, finished, stopped;

    public PlaybackSession(RecordingSession r, Player v) {
        this(r, v, PlaybackModifiers.DEFAULT, null, true, true, true, EntityFilter.ALL, true, true,
                v.getWorld(), false, "ignore_casing", true);
    }

    public PlaybackSession(RecordingSession r, Player v, PlaybackModifiers m) {
        this(r, v, m, null, true, true, true, EntityFilter.ALL, true, true,
                v.getWorld(), false, "ignore_casing", true);
    }

    public PlaybackSession(RecordingSession r, Player v, PlaybackModifiers m, PositionTransformer p) {
        this(r, v, m, p, false, true, true, EntityFilter.ALL, true, true,
                v.getWorld(), false, "ignore_casing", true);
    }

    public PlaybackSession(RecordingSession r, Player v, PlaybackModifiers m, PositionTransformer p, boolean root) {
        this(r, v, m, p, root, true, true, EntityFilter.ALL, true, true,
                v.getWorld(), false, "ignore_casing", true);
    }

    public PlaybackSession(RecordingSession r, Player v, PlaybackModifiers m, PositionTransformer p, boolean root,
                           boolean blockActionsPlayback, boolean blockInitialization, EntityFilter playEntities,
                           boolean invulnerablePlayback, boolean preventTrackingPlayedEntities) {
        this(r, v, m, p, root, blockActionsPlayback, blockInitialization, playEntities,
                invulnerablePlayback, preventTrackingPlayedEntities, v.getWorld(), false, "ignore_casing", true);
    }

    public PlaybackSession(RecordingSession r, Player v, PlaybackModifiers m, PositionTransformer p, boolean root,
                           boolean blockActionsPlayback, boolean blockInitialization, EntityFilter playEntities,
                           boolean invulnerablePlayback, boolean preventTrackingPlayedEntities, World targetWorld) {
        this(r, v, m, p, root, blockActionsPlayback, blockInitialization, playEntities,
                invulnerablePlayback, preventTrackingPlayedEntities, targetWorld, false, "ignore_casing", true);
    }

    public PlaybackSession(RecordingSession r, Player v, PlaybackModifiers m, PositionTransformer p, boolean root,
                           boolean blockActionsPlayback, boolean blockInitialization, EntityFilter playEntities,
                           boolean invulnerablePlayback, boolean preventTrackingPlayedEntities, World targetWorld,
                           boolean assignProfile, String playerNameHandling) {
        this(r, v, m, p, root, blockActionsPlayback, blockInitialization, playEntities,
                invulnerablePlayback, preventTrackingPlayedEntities, targetWorld, assignProfile,
                playerNameHandling, true);
    }

    public PlaybackSession(RecordingSession r, Player v, PlaybackModifiers m, PositionTransformer p, boolean root,
                           boolean blockActionsPlayback, boolean blockInitialization, EntityFilter playEntities,
                           boolean invulnerablePlayback, boolean preventTrackingPlayedEntities, World targetWorld,
                           boolean assignProfile, String playerNameHandling, boolean chatPlayback) {
        recording = r;
        viewerPlayerId = v.getUniqueId();
        frames = r.getFrames();
        entityFrames = r.getEntityFrames();
        blockActions = r.getBlockActions();
        chatMessages = r.getChatMessages();
        modifiers = m == null ? PlaybackModifiers.DEFAULT : m;
        this.root = root;
        this.blockActionsPlayback = blockActionsPlayback;
        this.blockInitialization = blockInitialization;
        this.blockAllowScaled = JavaPlugin.getProvidingPlugin(PlaybackSession.class).getConfig()
                .getBoolean("settings.block_allow_scaled", false);
        this.playEntities = playEntities == null ? EntityFilter.ALL : playEntities;
        this.invulnerablePlayback = invulnerablePlayback;
        this.preventTrackingPlayedEntities = preventTrackingPlayedEntities;
        this.assignProfile = assignProfile;
        this.playerNameHandling = playerNameHandling == null ? "ignore_casing" : playerNameHandling;
        this.chatPlayback = chatPlayback;
        this.targetWorld = targetWorld == null ? v.getWorld() : targetWorld;
        transformer = new PositionTransformer(modifiers, p, calculateRecordingCenter());

        if (frames.isEmpty()) {
            fakePlayer = null;
            entityActor = null;
            stopped = true;
            return;
        }

        PlayerStateFrame f = frames.get(0);
        Location spawn = transform(new Location(this.targetWorld, f.x(), f.y(), f.z(), f.yaw(), f.pitch()));
        if (modifiers.playerAsEntity().enabled() && modifiers.playerAsEntity().entityType() != EntityType.PLAYER) {
            Entity e = this.targetWorld.spawnEntity(spawn, modifiers.playerAsEntity().entityType());
            fakePlayer = null;
            entityActor = new EntityPlaybackActor(e, modifiers.playerScale(), invulnerablePlayback);
        } else {
            fakePlayer = FakePlayer.spawn(spawn, resolveProfile(v), modifiers.playerScale(), invulnerablePlayback);
            entityActor = null;
        }

        waitTicks = secondsToTicks(modifiers.startDelaySeconds() + modifiers.waitOnStartSeconds());
        if (waitTicks == 0) {
            initializeBlocks();
            applyFrame(f);
            applyHitRange(f);
            applyEntityFrames();
            applyRidingRelationships(f);
            applyBlockActions(0);
            applyChatMessages(0);
        }
    }

    public UUID getId() { return id; }
    public RecordingSession getRecording() { return recording; }
    public long getTick() { return tick; }
    public boolean isPaused() { return paused; }
    public boolean isStopped() { return stopped; }
    public boolean isFinished() { return finished; }
    public boolean isActive() { return !stopped && (!finished || modifiers.loop() || !modifiers.waitForParentEnd()); }
    public UUID getViewerPlayerId() { return viewerPlayerId; }
    public FakePlayer getFakePlayer() { return fakePlayer; }
    public EntityPlaybackActor getEntityActor() { return entityActor; }
    public PlaybackModifiers getModifiers() { return modifiers; }

    public PlayerStateFrame currentFrame() {
        return frames.isEmpty() || tick >= frames.size() ? null : frames.get((int) tick);
    }

    public void pause() { if (!stopped) paused = true; }
    public void resume() { if (!stopped) paused = false; }

    public void stop() {
        if (stopped) return;
        stopped = true;
        finished = true;
        ejectPlayer();
        if (fakePlayer != null) fakePlayer.remove();
        if (entityActor != null) entityActor.remove();
        removeRecordedEntities();
    }

    public void advance() {
        if (paused || stopped) return;
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        if (finished) {
            if (modifiers.loop()) restartLoop();
            else if (shouldSelfStop()) stop();
            return;
        }
        if (waitOnEnd > 0) {
            waitOnEnd--;
            if (waitOnEnd == 0) {
                finished = true;
                if (modifiers.loop()) restartLoop();
                else if (shouldSelfStop()) stop();
            }
            return;
        }
        if (tick >= frames.size()) {
            finishOrWaitOnEnd();
            return;
        }
        PlayerStateFrame f = frames.get((int) tick);
        applyFrame(f);
        applyHitRange(f);
        applyEntityFrames();
        applyRidingRelationships(f);
        applyBlockActions(tick);
        applyChatMessages(tick);
        tick++;
    }

    private void finishOrWaitOnEnd() {
        long n = secondsToTicks(modifiers.waitOnEndSeconds());
        if (n == 0) {
            finished = true;
            if (modifiers.loop()) restartLoop();
            else if (shouldSelfStop()) stop();
        } else {
            waitOnEnd = n;
        }
    }

    private void restartLoop() {
        ejectPlayer();
        removeRecordedEntities();
        initializeBlocks();
        tick = 0;
        waitTicks = 0;
        waitOnEnd = 0;
        finished = false;
        if (!frames.isEmpty()) {
            applyFrame(frames.get(0));
            applyHitRange(frames.get(0));
            applyEntityFrames();
            applyRidingRelationships(frames.get(0));
            applyBlockActions(0);
            applyChatMessages(0);
        }
    }

    private boolean shouldSelfStop() { return root || !modifiers.waitForParentEnd(); }

    private void applyFrame(PlayerStateFrame f) {
        Location l = transform(new Location(targetWorld, f.x(), f.y(), f.z(), f.yaw(), f.pitch()));
        if (entityActor != null) {
            entityActor.apply(f, l);
        } else {
            fakePlayer.apply(f);
            fakePlayer.getBukkitEntity().teleport(l);
        }
    }

    private void applyHitRange(PlayerStateFrame f) {
        HitRangePlayback.apply(playbackPlayerEntity(), f.swingMainHand(), f.swingOffHand());
    }

    private void applyEntityFrames() {
        if (!playEntities.enabled()) return;
        for (Map.Entry<UUID, List<EntityStateFrame>> e : entityFrames.entrySet()) {
            List<EntityStateFrame> t = e.getValue();
            EntityStateFrame f = frameAtTick(t, tick);
            EntityPlaybackActor a = entityActors.get(e.getKey());
            if (f == null) {
                if (a != null && lastTick(t) < tick) {
                    eject(a.entity());
                    a.remove();
                    entityActors.remove(e.getKey());
                }
                continue;
            }
            if (a == null) {
                EntityType type = EntityType.fromName(f.entityType());
                if (type == null || type == EntityType.PLAYER || !playEntities.matches(type)) continue;
                Location l = transform(new Location(targetWorld, f.x(), f.y(), f.z(), f.yaw(), f.pitch()));
                try {
                    a = new EntityPlaybackActor(targetWorld.spawnEntity(l, type), modifiers.sceneScale(), invulnerablePlayback);
                    entityActors.put(e.getKey(), a);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
            }
            a.apply(f, transform(new Location(targetWorld, f.x(), f.y(), f.z(), f.yaw(), f.pitch())));
        }
    }

    private void applyRidingRelationships(PlayerStateFrame pf) {
        Entity p = playbackPlayerEntity();
        if (p == null) return;
        UUID id = pf.vehicleId();
        if (id == null) {
            eject(p);
        } else {
            EntityPlaybackActor v = entityActors.get(id);
            if (v != null && v.entity().isValid()) v.entity().addPassenger(p);
            else eject(p);
        }
        if (!playEntities.enabled()) return;
        for (Map.Entry<UUID, List<EntityStateFrame>> e : entityFrames.entrySet()) {
            EntityPlaybackActor a = entityActors.get(e.getKey());
            if (a == null || !a.entity().isValid()) continue;
            EntityStateFrame f = frameAtTick(e.getValue(), tick);
            if (f == null) continue;
            UUID vid = f.vehicleId();
            if (vid == null) {
                eject(a.entity());
            } else {
                EntityPlaybackActor v = entityActors.get(vid);
                if (v != null && v.entity().isValid() && v.entity() != a.entity()) v.entity().addPassenger(a.entity());
                else eject(a.entity());
            }
        }
    }

    private void initializeBlocks() {
        if (!blockInitialization) return;
        for (BlockActionFrame a : blockActions) {
            BlockData before = createTransformedBlockData(a.beforeState());
            for (Location l : transformedBlockLocations(a)) {
                if (before == null) continue;
                try { l.getBlock().setBlockData(before.clone(), false); }
                catch (IllegalArgumentException ignored) { }
            }
        }
    }

    private void applyBlockActions(long target) {
        if (!blockActionsPlayback) return;
        for (BlockActionFrame a : blockActions) {
            if (a.tick() != target || a.action() == BlockActionFrame.Action.INTERACT) continue;
            BlockData after = createTransformedBlockData(a.afterState());
            if (after == null) continue;
            for (Location l : transformedBlockLocations(a)) {
                try { l.getBlock().setBlockData(after.clone(), false); }
                catch (IllegalArgumentException ignored) { }
            }
        }
    }

    private BlockData createTransformedBlockData(String state) {
        try {
            return transformer.transformBlockState(Bukkit.createBlockData(state));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private List<Location> transformedBlockLocations(BlockActionFrame a) {
        List<Vector> positions = transformer.transformBlockPositions(
                new Vector(a.x(), a.y(), a.z()), blockAllowScaled);
        if (positions.isEmpty()) return List.of();
        return positions.stream()
                .map(p -> new Location(targetWorld, Math.floor(p.getX()), Math.floor(p.getY()), Math.floor(p.getZ())))
                .toList();
    }

    private void applyChatMessages(long target) {
        if (!chatPlayback) return;
        for (ChatMessageFrame chat : chatMessages) {
            if (chat.tick() != target) continue;
            try {
                Component message = GsonComponentSerializer.gson().deserialize(chat.messageJson());
                Bukkit.broadcast(Component.text("<").append(Component.text(recording.getSourcePlayerName()))
                        .append(Component.text("> ")).append(message));
            } catch (Exception ignored) { }
        }
    }

    private Entity playbackPlayerEntity() {
        if (entityActor != null) return entityActor.entity();
        return fakePlayer == null ? null : fakePlayer.getBukkitEntity();
    }

    private void ejectPlayer() {
        Entity p = playbackPlayerEntity();
        if (p != null) eject(p);
    }

    private static void eject(Entity p) {
        Entity v = p.getVehicle();
        if (v != null) v.removePassenger(p);
    }

    private static long lastTick(List<EntityStateFrame> t) {
        return t.isEmpty() ? Long.MIN_VALUE : t.get(t.size() - 1).tick();
    }

    private static EntityStateFrame frameAtTick(List<EntityStateFrame> t, long target) {
        int lo = 0, hi = t.size() - 1;
        while (lo <= hi) {
            int m = (lo + hi) >>> 1;
            long v = t.get(m).tick();
            if (v < target) lo = m + 1;
            else if (v > target) hi = m - 1;
            else return t.get(m);
        }
        return null;
    }

    private void removeRecordedEntities() {
        for (EntityPlaybackActor a : entityActors.values()) {
            eject(a.entity());
            a.remove();
        }
        entityActors.clear();
    }

    private World currentWorld() {
        if (fakePlayer != null) return fakePlayer.getBukkitEntity().getWorld();
        if (entityActor != null) return entityActor.entity().getWorld();
        return targetWorld;
    }

    public Location resolveLocation(Player fallback) {
        PlayerStateFrame f = currentFrame();
        if (f == null) return fallback.getLocation().clone();
        return transform(new Location(targetWorld, f.x(), f.y(), f.z(), f.yaw(), f.pitch()));
    }

    private Location transform(Location l) { return transformer.transform(l); }

    private GameProfile resolveProfile(Player viewer) {
        PlayerSkin s = modifiers.playerSkin();
        if (s.source() == PlayerSkin.Source.FROM_PLAYER) {
            Player p = findPlayerByName(s.path());
            if (p != null) return ((CraftPlayer) p).getProfile();
        }
        if (s.source() == PlayerSkin.Source.FROM_MINESKIN) {
            Property p = MineSkinSkins.getProperty(s.path());
            if (p != null) {
                GameProfile g = new GameProfile(UUID.randomUUID(), modifiers.playerName() == null ? "MoCap" : modifiers.playerName());
                g.properties().put("textures", p);
                return g;
            }
        }
        if (s.source() == PlayerSkin.Source.DEFAULT && modifiers.playerName() != null) {
            Player p = findPlayerByName(modifiers.playerName());
            if (p != null) return ((CraftPlayer) p).getProfile();
        }
        if (assignProfile) {
            Player source = Bukkit.getPlayer(recording.getSourcePlayerId());
            if (source != null) return ((CraftPlayer) source).getProfile();
            return new GameProfile(recording.getSourcePlayerId(), recording.getSourcePlayerName());
        }
        return ((CraftPlayer) viewer).getProfile();
    }

    private Player findPlayerByName(String name) {
        if (name == null) return null;
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) return exact;
        if (!"ignore_casing".equalsIgnoreCase(playerNameHandling)
                && !"ignore_and_replace_casing".equalsIgnoreCase(playerNameHandling)) return null;
        for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().equalsIgnoreCase(name)) return p;
        return null;
    }

    private Vector calculateRecordingCenter() {
        if (frames.isEmpty()) return new Vector(0, 0, 0);
        PlayerStateFrame s = frames.get(0);
        Vector p = new Vector(s.x(), s.y(), s.z());
        PlaybackModifiers.TransformationConfig c = modifiers.transformationConfig();
        Vector center = switch (c.recordingCenter()) {
            case ACTUAL -> p.clone();
            case BLOCK_CENTER -> blockCenter(p);
            case BLOCK_CORNER -> blockCorner(p);
            case AUTO -> autoCenter(p);
        };
        return center.add(new Vector(c.centerOffsetX(), c.centerOffsetY(), c.centerOffsetZ()));
    }

    private Vector autoCenter(Vector p) {
        double s = modifiers.sceneScale();
        if (s == 1.0 || s != Math.rint(s)) {
            Vector c = blockCenter(p), q = blockCorner(p);
            return p.distanceSquared(c) > p.distanceSquared(q) ? q : c;
        }
        return ((int) s % 2 == 1) ? blockCenter(p) : blockCorner(p);
    }

    private static Vector blockCenter(Vector p) {
        return new Vector(Math.round(p.getX() - 0.5) + 0.5, Math.floor(p.getY()), Math.round(p.getZ() - 0.5) + 0.5);
    }

    private static Vector blockCorner(Vector p) {
        return new Vector(Math.round(p.getX()), Math.floor(p.getY()), Math.round(p.getZ()));
    }

    private static long secondsToTicks(double s) {
        return !Double.isFinite(s) || s <= 0 ? 0 : Math.min(Integer.MAX_VALUE, (long) Math.ceil(s * 20));
    }
}
