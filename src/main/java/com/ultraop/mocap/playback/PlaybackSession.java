package com.ultraop.mocap.playback;

import com.ultraop.mocap.recording.PlayerStateFrame;
import com.ultraop.mocap.recording.RecordingSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** Tick-driven playback timeline. Entity presentation is deliberately isolated from the timeline. */
public final class PlaybackSession {
    private final UUID id;
    private final RecordingSession recording;
    private final UUID viewerPlayerId;
    private final List<PlayerStateFrame> frames;
    private long tick;
    private boolean paused;
    private boolean stopped;

    public PlaybackSession(RecordingSession recording, Player viewer) {
        this.id = UUID.randomUUID();
        this.recording = recording;
        this.viewerPlayerId = viewer.getUniqueId();
        this.frames = recording.getFrames();
    }

    public UUID getId() { return id; }
    public RecordingSession getRecording() { return recording; }
    public long getTick() { return tick; }
    public boolean isPaused() { return paused; }
    public boolean isStopped() { return stopped; }
    public boolean isFinished() { return tick >= frames.size(); }
    public UUID getViewerPlayerId() { return viewerPlayerId; }

    public PlayerStateFrame currentFrame() {
        if (frames.isEmpty() || isFinished()) return null;
        return frames.get((int) tick);
    }

    public void pause() { if (!stopped) paused = true; }
    public void resume() { if (!stopped) paused = false; }
    public void stop() { stopped = true; }

    public void advance() {
        if (!paused && !stopped) {
            tick++;
            if (isFinished()) stopped = true;
        }
    }

    public Location resolveLocation(Player fallback) {
        PlayerStateFrame frame = currentFrame();
        if (frame == null) return fallback.getLocation().clone();
        if (!frame.worldKey().equals(fallback.getWorld().getKey().toString())) {
            return fallback.getLocation().clone();
        }
        return new Location(fallback.getWorld(), frame.x(), frame.y(), frame.z(), frame.yaw(), frame.pitch());
    }
}
