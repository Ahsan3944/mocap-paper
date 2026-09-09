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
    private long tick;
    private boolean paused;
    private boolean stopped;

    public PlaybackSession(RecordingSession recording, Player viewer) {
        this.id = UUID.randomUUID();
        this.recording = recording;
        this.viewerPlayerId = viewer.getUniqueId();
        this.frames = recording.getFrames();

        if (frames.isEmpty()) {
            this.fakePlayer = null;
            this.stopped = true;
            return;
        }

        PlayerStateFrame first = frames.get(0);
        World world = findWorld(first.worldKey(), viewer.getWorld());
        Location spawn = new Location(world, first.x(), first.y(), first.z(), first.yaw(), first.pitch());
        this.fakePlayer = FakePlayer.spawn(spawn, recording.getSourcePlayerId(), recording.getSourcePlayerName());
        this.fakePlayer.apply(first);
    }

    public UUID getId() { return id; }
    public RecordingSession getRecording() { return recording; }
    public long getTick() { return tick; }
    public boolean isPaused() { return paused; }
    public boolean isStopped() { return stopped; }
    public boolean isFinished() { return tick >= frames.size(); }
    public UUID getViewerPlayerId() { return viewerPlayerId; }
    public FakePlayer getFakePlayer() { return fakePlayer; }

    public PlayerStateFrame currentFrame() {
        if (frames.isEmpty() || isFinished()) return null;
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
        tick++;
        if (isFinished()) {
            stop();
            return;
        }
        fakePlayer.apply(frames.get((int) tick));
    }

    public Location resolveLocation(Player fallback) {
        PlayerStateFrame frame = currentFrame();
        if (frame == null) return fallback.getLocation().clone();
        World world = findWorld(frame.worldKey(), fallback.getWorld());
        return new Location(world, frame.x(), frame.y(), frame.z(), frame.yaw(), frame.pitch());
    }

    private static World findWorld(String worldKey, World fallback) {
        for (World world : Bukkit.getWorlds()) {
            if (world.getKey().toString().equals(worldKey)) return world;
        }
        return fallback;
    }
}
