package com.ultraop.mocap.recording;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Owns active recordings, completed recordings and saved recording files. */
public final class RecordingManager {
    private final JavaPlugin plugin;
    private final RecordingRepository repository;
    private final Map<UUID, RecordingSession> active = new LinkedHashMap<>();
    private final Map<UUID, RecordingSession> completed = new LinkedHashMap<>();
    private final Map<String, RecordingSession> saved = new LinkedHashMap<>();
    private BukkitTask ticker;

    public RecordingManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.repository = new RecordingRepository(Path.of(plugin.getDataFolder().getPath(), "recordings"));
    }

    public void start() {
        if (ticker != null) return;
        try {
            repository.initialize();
            saved.clear();
            saved.putAll(repository.loadAll());
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to load saved recordings: " + e.getMessage());
        }
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public RecordingSession startRecording(Player player) {
        RecordingSession session = new RecordingSession(player);
        active.put(session.getId(), session);
        return session;
    }

    public RecordingSession startRecording(Player player, String instantSaveName) {
        RecordingSession session = startRecording(player);
        session.setInstantSaveName(instantSaveName);
        return session;
    }

    public RecordingSession stopRecording(UUID id) {
        RecordingSession session = active.remove(id);
        if (session == null) return null;
        session.stop();
        completed.put(session.getId(), session);
        if (session.getInstantSaveName() != null) {
            try {
                save(session.getInstantSaveName(), session);
            } catch (IOException e) {
                plugin.getLogger().severe("Unable to instantly save recording: " + e.getMessage());
            }
        }
        return session;
    }

    public RecordingSession stopRecordingForPlayer(Player player) {
        for (RecordingSession session : new ArrayList<>(active.values())) {
            if (session.getSourcePlayerId().equals(player.getUniqueId())) return stopRecording(session.getId());
        }
        return null;
    }

    public RecordingSession discard(UUID id) {
        RecordingSession session = active.remove(id);
        if (session != null) return session;
        return completed.remove(id);
    }

    public RecordingSession get(UUID id) {
        RecordingSession session = active.get(id);
        if (session != null) return session;
        return completed.get(id);
    }

    public Collection<RecordingSession> getActive() {
        return ListView.copyOf(active.values());
    }

    public Collection<RecordingSession> getCompleted() {
        return ListView.copyOf(completed.values());
    }

    public Collection<String> getSavedNames() {
        return ListView.copyOf(saved.keySet());
    }

    public RecordingSession getSaved(String name) {
        return saved.get(name);
    }

    public boolean save(String name, RecordingSession session) throws IOException {
        repository.save(name, session);
        saved.put(name, session);
        return true;
    }

    public boolean save(String name, UUID id) throws IOException {
        RecordingSession session = get(id);
        if (session == null) return false;
        return save(name, session);
    }

    public boolean rename(String oldName, String newName) throws IOException {
        if (!saved.containsKey(oldName) || saved.containsKey(newName) || repository.exists(newName)) return false;
        RecordingSession session = saved.remove(oldName);
        repository.save(newName, session);
        repository.delete(oldName);
        saved.put(newName, session);
        return true;
    }

    public boolean copy(String sourceName, String destinationName) throws IOException {
        if (saved.containsKey(destinationName) || repository.exists(destinationName)) return false;
        RecordingSession session = saved.get(sourceName);
        if (session == null) return false;
        repository.save(destinationName, session);
        saved.put(destinationName, session);
        return true;
    }

    public boolean removeSaved(String name) throws IOException {
        boolean removed = repository.delete(name);
        saved.remove(name);
        return removed;
    }

    public void shutdown() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        active.clear();
        completed.clear();
        saved.clear();
    }

    private void tick() {
        for (RecordingSession session : new ArrayList<>(active.values())) {
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
