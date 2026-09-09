package com.ultraop.mocap.playback;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.ultraop.mocap.recording.PlayerStateFrame;
import com.ultraop.mocap.recording.RecordingSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

/** Tick-driven playback timeline backed by a server-side fake player. */
public final class PlaybackSession {
    private final UUID id;
    private final RecordingSession recording;
    private final UUID viewerPlayerId;
    private final List<PlayerStateFrame> frames;
    private final FakePlayer fakePlayer;
    private final PlaybackModifiers modifiers;
    private final PositionTransformer transformer;
    private final boolean root;
    private long tick;
    private long waitTicks;
    private long waitOnEnd;
    private boolean paused;
    private boolean finished;
    private boolean stopped;

    public PlaybackSession(RecordingSession recording, Player viewer) { this(recording, viewer, PlaybackModifiers.DEFAULT, null, true); }
    public PlaybackSession(RecordingSession recording, Player viewer, PlaybackModifiers modifiers) { this(recording, viewer, modifiers, null, true); }
    public PlaybackSession(RecordingSession recording, Player viewer, PlaybackModifiers modifiers, PositionTransformer parentTransformer) { this(recording, viewer, modifiers, parentTransformer, false); }

    public PlaybackSession(RecordingSession recording, Player viewer, PlaybackModifiers modifiers,
                           PositionTransformer parentTransformer, boolean root) {
        this.id = UUID.randomUUID();
        this.recording = recording;
        this.viewerPlayerId = viewer.getUniqueId();
        this.frames = recording.getFrames();
        this.modifiers = modifiers == null ? PlaybackModifiers.DEFAULT : modifiers;
        this.root = root;
        this.transformer = new PositionTransformer(this.modifiers, parentTransformer, calculateRecordingCenter());
        if (frames.isEmpty()) { this.fakePlayer = null; this.stopped = true; return; }

        PlayerStateFrame first = frames.get(0);
        World world = findWorld(first.worldKey(), viewer.getWorld());
        Location spawn = transform(new Location(world, first.x(), first.y(), first.z(), first.yaw(), first.pitch()));
        GameProfile profile = resolveProfile(viewer);
        this.fakePlayer = FakePlayer.spawn(spawn, profile, this.modifiers.playerScale());
        this.waitTicks = secondsToTicks(this.modifiers.startDelaySeconds() + this.modifiers.waitOnStartSeconds());
        if (waitTicks == 0) applyFrame(first);
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
    public PlaybackModifiers getModifiers() { return modifiers; }
    public PlayerStateFrame currentFrame() { return frames.isEmpty() || tick >= frames.size() ? null : frames.get((int) tick); }
    public void pause() { if (!stopped) paused = true; }
    public void resume() { if (!stopped) paused = false; }

    public void stop() {
        if (stopped) return;
        stopped = true;
        finished = true;
        if (fakePlayer != null) fakePlayer.remove();
    }

    public void advance() {
        if (paused || stopped) return;
        if (waitTicks > 0) { waitTicks--; return; }
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
        if (tick >= frames.size()) { finishOrWaitOnEnd(); return; }
        applyFrame(frames.get((int) tick));
        tick++;
    }

    private void finishOrWaitOnEnd() {
        long endTicks = secondsToTicks(modifiers.waitOnEndSeconds());
        if (endTicks == 0) {
            finished = true;
            if (modifiers.loop()) restartLoop();
            else if (shouldSelfStop()) stop();
        } else waitOnEnd = endTicks;
    }

    private void restartLoop() {
        tick = 0;
        waitTicks = 0;
        waitOnEnd = 0;
        finished = false;
        if (!frames.isEmpty()) applyFrame(frames.get(0));
    }

    private boolean shouldSelfStop() { return root || !modifiers.waitForParentEnd(); }

    private void applyFrame(PlayerStateFrame frame) {
        fakePlayer.apply(frame);
        World world = findWorld(frame.worldKey(), fakePlayer.getBukkitEntity().getWorld());
        Location transformed = transform(new Location(world, frame.x(), frame.y(), frame.z(), frame.yaw(), frame.pitch()));
        fakePlayer.getBukkitEntity().teleport(transformed);
    }

    public Location resolveLocation(Player fallback) {
        PlayerStateFrame frame = currentFrame();
        if (frame == null) return fallback.getLocation().clone();
        World world = findWorld(frame.worldKey(), fallback.getWorld());
        return transform(new Location(world, frame.x(), frame.y(), frame.z(), frame.yaw(), frame.pitch()));
    }

    private Location transform(Location source) { return transformer.transform(source); }

    private GameProfile resolveProfile(Player viewer) {
        PlayerSkin skin = modifiers.playerSkin();
        if (skin.source() == PlayerSkin.Source.FROM_PLAYER) {
            Player online = Bukkit.getPlayerExact(skin.path());
            if (online != null) return ((CraftPlayer) online).getProfile();
        }
        if (skin.source() == PlayerSkin.Source.FROM_MINESKIN) {
            Property property = MineSkinSkins.getProperty(skin.path());
            if (property != null) {
                GameProfile profile = new GameProfile(UUID.randomUUID(), modifiers.playerName() == null ? "MoCap" : modifiers.playerName());
                profile.getProperties().put("textures", property);
                return profile;
            }
        }
        if (skin.source() == PlayerSkin.Source.DEFAULT && modifiers.playerName() != null) {
            Player online = Bukkit.getPlayerExact(modifiers.playerName());
            if (online != null) return ((CraftPlayer) online).getProfile();
        }
        return ((CraftPlayer) viewer).getProfile();
    }

    private Vector calculateRecordingCenter() {
        if (frames.isEmpty()) return new Vector(0, 0, 0);
        PlayerStateFrame start = frames.get(0);
        Vector pos = new Vector(start.x(), start.y(), start.z());
        PlaybackModifiers.TransformationConfig config = modifiers.transformationConfig();
        Vector center = switch (config.recordingCenter()) {
            case ACTUAL -> pos.clone();
            case BLOCK_CENTER -> blockCenter(pos);
            case BLOCK_CORNER -> blockCorner(pos);
            case AUTO -> autoCenter(pos);
        };
        return center.add(new Vector(config.centerOffsetX(), config.centerOffsetY(), config.centerOffsetZ()));
    }

    private Vector autoCenter(Vector pos) {
        double scale = modifiers.sceneScale();
        if (scale == 1.0 || scale != Math.rint(scale)) {
            Vector center = blockCenter(pos);
            Vector corner = blockCorner(pos);
            return pos.distanceSquared(center) > pos.distanceSquared(corner) ? corner : center;
        }
        return ((int) scale % 2 == 1) ? blockCenter(pos) : blockCorner(pos);
    }

    private static Vector blockCenter(Vector pos) { return new Vector(Math.round(pos.getX() - 0.5) + 0.5, Math.floor(pos.getY()), Math.round(pos.getZ() - 0.5) + 0.5); }
    private static Vector blockCorner(Vector pos) { return new Vector(Math.round(pos.getX()), Math.floor(pos.getY()), Math.round(pos.getZ())); }
    private static long secondsToTicks(double seconds) { return !Double.isFinite(seconds) || seconds <= 0.0 ? 0L : Math.min(Integer.MAX_VALUE, (long) Math.ceil(seconds * 20.0)); }
    private static World findWorld(String worldKey, World fallback) { for (World world : Bukkit.getWorlds()) if (world.getKey().toString().equals(worldKey)) return world; return fallback; }
}
