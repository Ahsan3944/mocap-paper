package com.ultraop.mocap.recording;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns active and completed recordings and samples active sessions once per server tick.
 */
public final class RecordingManager {
    private final JavaPlugin plugin;
    private final Map<UUID, RecordingSession> active = new LinkedHashMap<>();
    private final Map<UUID, RecordingSession> completed = new LinkedHashMap<>();
    private BukkitTask ticker;

    public RecordingManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (ticker != null) {
            return;
        }
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public RecordingSession startRecording(Player player) {
        RecordingSession session = new RecordingSession(player);
        active.put(session.getId(), session);
        return session;
    }

    public RecordingSession stopRecording(UUID id) {
        RecordingSession session = active.remove(id);
        if (session == null) {
            return null;
        }
        session.stop();
        completed.put(session.getId(), session);
        return session;
    }

    public RecordingSession stopRecordingForPlayer(Player player) {
        for (RecordingSession session : new ArrayList<>(active.values())) {
            if (session.getSourcePlayerId().equals(player.getUniqueId())) {
                return stopRecording(session.getId());
            }
        }
        return null;
    }

    public RecordingSession discard(UUID id) {
        RecordingSession session = active.remove(id);
        if (session != null) {
            return session;
        }
        return completed.remove(id);
    }

    public RecordingSession get(UUID id) {
        RecordingSession session = active.get(id);
        return session != null ? session : completed.get(id);
    }

    public Collection<RecordingSession> getActive() {
        return ListView.copyOf(active.values());
    }

    public Collection<RecordingSession> getCompleted() {
        return ListView.copyOf(completed.values());
    }

    public void shutdown() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        active.clear();
        completed.clear();
    }

    private void tick() {
        for (RecordingSession session : active.values()) {
            Player player = Bukkit.getPlayer(session.getSourcePlayerId());
            if (player != null && player.isOnline()) {
                session.capture(player);
            }
        }
    }

    private static final class ListView {
        private ListView() {}

        static <T> Collection<T> copyOf(Collection<T> source) {
            return java.util.Collections.unmodifiableList(new ArrayList<>(source));
        }
    }
}
