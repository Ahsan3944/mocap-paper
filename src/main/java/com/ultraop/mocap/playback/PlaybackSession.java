package com.ultraop.mocap.playback;

import com.ultraop.mocap.recording.PlayerStateFrame;
import com.ultraop.mocap.recording.RecordingSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

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
    private long tick;
    private long waitTicks;
    private boolean waitingForEnd;
    private boolean paused;
    private boolean stopped;

    public PlaybackSession(RecordingSession recording, Player viewer) {
        this(recording, viewer, PlaybackModifiers.DEFAULT, null);
    }

    public PlaybackSession(RecordingSession recording, Player viewer, PlaybackModifiers modifiers) {
        this(recording, viewer, modifiers, null);
    }

    public PlaybackSession(RecordingSession recording, Player viewer, PlaybackModifiers modifiers, PositionTransformer transformer) {
        this.id = UUID.randomUUID();
        this.recording = recording;
        this.viewerPlayerId = viewer.getUniqueId();
        this.frames = recording.getFrames();
        this.modifiers = modifiers == null ? PlaybackModifiers.DEFAULT : modifiers;
        this.transformer = transformer == null
                ? new PositionTransformer(this.modifiers, null, calculateDefaultCenter())
                : transformer;
        if (frames.isEmpty()) { this.fakePlayer = null; this.stopped = true; return; }

        PlayerStateFrame first = frames.get(0);
        World world = findWorld(first.worldKey(), viewer.getWorld());
        Location spawn = transform(new Location(world, first.x(), first.y(), first.z(), first.yaw(), first.pitch()));
        String displayName = this.modifiers.playerName() == null ? recording.getSourcePlayerName() : this.modifiers.playerName();
        this.fakePlayer = FakePlayer.spawn(spawn, recording.getSourcePlayerId(), displayName);
        this.waitTicks = secondsToTicks(this.modifiers.startDelaySeconds() + this.modifiers.waitOnStartSeconds());
        if (waitTicks == 0) applyFrame(first);
    }

    public UUID getId() { return id; }
    public RecordingSession getRecording() { return recording; }
    public long getTick() { return tick; }
    public boolean isPaused() { return paused; }
    public boolean isStopped() { return stopped; }
    public boolean isFinished() { return stopped || (tick >= frames.size() && !modifiers.loop() && !waitingForEnd && waitTicks == 0); }
    public UUID getViewerPlayerId() { return viewerPlayerId; }
    public FakePlayer getFakePlayer() { return fakePlayer; }
    public PlaybackModifiers getModifiers() { return modifiers; }
    public PlayerStateFrame currentFrame() { return frames.isEmpty() || tick >= frames.size() ? null : frames.get((int) tick); }
    public void pause() { if (!stopped) paused = true; }
    public void resume() { if (!stopped) paused = false; }
    public void stop() { if (stopped) return; stopped = true; if (fakePlayer != null) fakePlayer.remove(); }

    public void advance() {
        if (paused || stopped) return;
        if (waitTicks > 0) { waitTicks--; return; }
        if (waitingForEnd) { waitingForEnd = false; if (modifiers.loop()) restartLoop(); else stop(); return; }
        if (tick >= frames.size()) {
            if (modifiers.loop()) { restartLoop(); return; }
            if (modifiers.waitOnEndSeconds() > 0) { waitingForEnd = true; waitTicks = secondsToTicks(modifiers.waitOnEndSeconds()); return; }
            stop(); return;
        }
        applyFrame(frames.get((int) tick));
        tick++;
    }

    private void restartLoop() {
        tick = 0;
        waitTicks = secondsToTicks(modifiers.waitOnStartSeconds());
        waitingForEnd = false;
        if (waitTicks == 0 && !frames.isEmpty()) applyFrame(frames.get(0));
    }

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

    private org.bukkit.util.Vector calculateDefaultCenter() {
        if (frames.isEmpty()) return new org.bukkit.util.Vector(0, 0, 0);
        PlayerStateFrame start = frames.get(0);
        org.bukkit.util.Vector pos = new org.bukkit.util.Vector(start.x(), start.y(), start.z());
        org.bukkit.util.Vector center = new org.bukkit.util.Vector(
                Math.round(pos.getX() - 0.5) + 0.5,
                Math.floor(pos.getY()),
                Math.round(pos.getZ() - 0.5) + 0.5);
        org.bukkit.util.Vector corner = new org.bukkit.util.Vector(
                Math.round(pos.getX()), Math.floor(pos.getY()), Math.round(pos.getZ()));
        return pos.distanceSquared(center) > pos.distanceSquared(corner) ? corner : center;
    }

    private static long secondsToTicks(double seconds) { return Math.max(0L, (long) Math.ceil(seconds * 20.0)); }
    private static World findWorld(String worldKey, World fallback) {
        for (World world : Bukkit.getWorlds()) if (world.getKey().toString().equals(worldKey)) return world;
        return fallback;
    }
}
