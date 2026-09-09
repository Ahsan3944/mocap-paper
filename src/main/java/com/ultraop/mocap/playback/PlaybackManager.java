package com.ultraop.mocap.playback;

import com.ultraop.mocap.recording.RecordingManager;
import com.ultraop.mocap.recording.RecordingSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Owns playback timelines and advances them at the server's 20 TPS cadence. */
public final class PlaybackManager {
    private final JavaPlugin plugin;
    private final RecordingManager recordingManager;
    private final Map<UUID, PlaybackSession> active = new LinkedHashMap<>();
    private final Map<UUID, PlaybackModifiers> modifiers = new LinkedHashMap<>();
    private BukkitTask ticker;

    public PlaybackManager(JavaPlugin plugin, RecordingManager recordingManager) {
        this.plugin = plugin;
        this.recordingManager = recordingManager;
    }

    public void start() {
        if (ticker == null) ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public PlaybackSession play(UUID recordingId, Player viewer) {
        RecordingSession recording = recordingManager.get(recordingId);
        return recording == null ? null : play(recording, viewer);
    }

    public PlaybackSession playSaved(String name, Player viewer) {
        RecordingSession recording = recordingManager.getSaved(name);
        return recording == null ? null : play(recording, viewer);
    }

    public PlaybackSession play(RecordingSession recording, Player viewer) {
        PlaybackSession session = new PlaybackSession(recording, viewer, getModifiers(viewer));
        if (session.isStopped()) return null;
        active.put(session.getId(), session);
        return session;
    }

    public PlaybackSession stop(UUID id) {
        PlaybackSession session = active.remove(id);
        if (session != null) session.stop();
        return session;
    }

    public int stopAll(Player owner) {
        int stopped = 0;
        for (PlaybackSession session : new ArrayList<>(active.values())) {
            if (owner == null || owner.getUniqueId().equals(session.getViewerPlayerId())) {
                session.stop();
                active.remove(session.getId());
                stopped++;
            }
        }
        return stopped;
    }

    public PlaybackSession get(UUID id) { return active.get(id); }

    public PlaybackModifiers getModifiers(Player player) {
        return modifiers.getOrDefault(player.getUniqueId(), PlaybackModifiers.DEFAULT);
    }

    public void setModifiers(Player player, PlaybackModifiers value) {
        modifiers.put(player.getUniqueId(), value);
    }

    public void resetModifiers(Player player) {
        modifiers.remove(player.getUniqueId());
    }

    public Collection<PlaybackSession> getActive() {
        return java.util.Collections.unmodifiableList(new ArrayList<>(active.values()));
    }

    public void shutdown() {
        if (ticker != null) { ticker.cancel(); ticker = null; }
        active.values().forEach(PlaybackSession::stop);
        active.clear();
        modifiers.clear();
    }

    private void tick() {
        for (PlaybackSession session : new ArrayList<>(active.values())) {
            session.advance();
            if (session.isStopped()) active.remove(session.getId());
        }
    }
}
