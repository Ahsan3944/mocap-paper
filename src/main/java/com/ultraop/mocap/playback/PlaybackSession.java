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
    private long tick;
    private long waitTicks;
    private boolean paused;
    private boolean stopped;

    public PlaybackSession(RecordingSession recording, Player viewer) {
        this(recording, viewer, PlaybackModifiers.DEFAULT);
    }

    public PlaybackSession(RecordingSession recording, Player viewer, PlaybackModifiers modifiers) {
        this.id = UUID.randomUUID();
        this.recording = recording;
        this.viewerPlayerId = viewer.getUniqueId();
        this.frames = recording.getFrames();
        this.modifiers = modifiers;

        if (frames.isEmpty()) {
            this.fakePlayer = null;
            this.stopped = true;
            return;
        }

        PlayerStateFrame first = frames.get(0);
        World world = findWorld(first.worldKey(), viewer.getWorld());
        Location spawn = transform(new Location(world, first.x(), first.y(), first.z(), first.yaw(), first.pitch()));
        this.fakePlayer = FakePlayer.spawn(spawn, recording.getSourcePlayerId(), recording.getSourcePlayerName());
        this.waitTicks = secondsToTicks(modifiers.startDelaySeconds() + modifiers.waitOnStartSeconds());
        if (waitTicks == 0) this.fakePlayer.apply(first);
    }

    public UUID getId() { return id; }
    public RecordingSession getRecording() { return recording; }
    public long getTick() { return tick; }
    public boolean isPaused() { return paused; }
    public boolean isStopped() { return stopped; }
    public boolean isFinished() { return stopped || (tick >= frames.size() && !modifiers.loop()); }
    public UUID getViewerPlayerId() { return viewerPlayerId; }
    public FakePlayer getFakePlayer() { return fakePlayer; }
    public PlaybackModifiers getModifiers() { return modifiers; }

    public PlayerStateFrame currentFrame() {
        if (frames.isEmpty() || tick >= frames.size()) return null;
        return frames.get((int) tick);
    }

    public void pause() { if (!stopped) paused = true; }
    public void resume() { if (!stopped) paused = false; }

    public void stop() {
        if (stopped) return;
        stopped = true;
        if (fakePlayer != null) fakePlayer.remove();
    }

    public void advance() {
        if (paused || stopped) return;
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        if (tick >= frames.size()) {
            if (modifiers.waitOnEndSeconds() > 0) {
                waitTicks = secondsToTicks(modifiers.waitOnEndSeconds());
                if (modifiers.loop()) {
                    tick = 0;
                    waitTicks = Math.max(waitTicks, secondsToTicks(modifiers.waitOnStartSeconds()));
                    return;
                }
                modifiersEndStop();
                return;
            }
            if (modifiers.loop()) {
                tick = 0;
                fakePlayer.apply(frames.get(0));
                return;
            }
            stop();
            return;
        }

        fakePlayer.apply(frames.get((int) tick));
        tick++;
    }

    private void modifiersEndStop() {
        if (!modifiers.loop()) stop();
    }

    public Location resolveLocation(Player fallback) {
        PlayerStateFrame frame = currentFrame();
        if (frame == null) return fallback.getLocation().clone();
        World world = findWorld(frame.worldKey(), fallback.getWorld());
        return transform(new Location(world, frame.x(), frame.y(), frame.z(), frame.yaw(), frame.pitch()));
    }

    private Location transform(Location source) {
        double x = source.getX();
        double y = source.getY();
        double z = source.getZ();
        double ox = modifiers.offsetX();
        double oy = modifiers.offsetY();
        double oz = modifiers.offsetZ();

        x *= modifiers.sceneScale();
        y *= modifiers.sceneScale();
        z *= modifiers.sceneScale();
        x += ox;
        y += oy;
        z += oz;

        double radians = Math.toRadians(modifiers.rotationDegrees());
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double rx = x * cos - z * sin;
        double rz = x * sin + z * cos;

        switch (modifiers.mirror()) {
            case X -> rx = -rx;
            case Z -> rz = -rz;
            case XZ -> { rx = -rx; rz = -rz; }
            case NONE -> { }
        }

        float yaw = source.getYaw() + (float) modifiers.rotationDegrees();
        if (modifiers.mirror() == PlaybackModifiers.Mirror.X || modifiers.mirror() == PlaybackModifiers.Mirror.XZ) yaw = -yaw;
        if (modifiers.mirror() == PlaybackModifiers.Mirror.Z || modifiers.mirror() == PlaybackModifiers.Mirror.XZ) yaw = -yaw - 180.0f;
        return new Location(source.getWorld(), rx, y, rz, yaw, source.getPitch());
    }

    private static long secondsToTicks(double seconds) {
        return Math.max(0L, Math.round(seconds * 20.0));
    }

    private static World findWorld(String worldKey, World fallback) {
        for (World world : Bukkit.getWorlds()) {
            if (world.getKey().toString().equals(worldKey)) return world;
        }
        return fallback;
    }
}
