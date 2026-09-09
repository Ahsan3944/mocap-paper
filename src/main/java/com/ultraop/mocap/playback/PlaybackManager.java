package com.ultraop.mocap.playback;

import com.ultraop.mocap.recording.RecordingManager;
import com.ultraop.mocap.recording.RecordingSession;
import com.ultraop.mocap.scene.SceneManager;
import com.ultraop.mocap.scene.ScenePlayback;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/** Owns standalone and scene playback timelines and advances them at the configured playback speed. */
public final class PlaybackManager {
    public enum DimensionSource { ASSIGNED_OR_CURRENT, ASSIGNED_OR_OVERWORLD, CURRENT, OVERWORLD }
    public enum AssignProfile { NO, ONLY_NAME, FULL }

    private final JavaPlugin plugin;
    private final RecordingManager recordingManager;
    private final Map<UUID, PlaybackSession> active = new LinkedHashMap<>();
    private final Map<UUID, ScenePlayback> activeScenes = new LinkedHashMap<>();
    private final Map<UUID, PlaybackModifiers> modifiers = new LinkedHashMap<>();
    private final Map<UUID, Double> accumulators = new HashMap<>();
    private SceneManager sceneManager;
    private BukkitTask ticker;
    private double playbackSpeed = 1.0;
    private boolean blockActionsPlayback = true, blockInitialization = true, invulnerablePlayback = true,
            preventTrackingPlayedEntities = true, chatPlayback = true, startAsRecorded = false;
    private AssignProfile assignProfile = AssignProfile.NO;
    private EntityFilter playEntities = EntityFilter.ALL;
    private DimensionSource dimensionSource = DimensionSource.ASSIGNED_OR_CURRENT;
    private String playerNameHandling = "ignore_casing";

    public PlaybackManager(JavaPlugin plugin, RecordingManager recordingManager) {
        this.plugin = plugin;
        this.recordingManager = recordingManager;
        loadSettings();
    }

    public void setSceneManager(SceneManager sceneManager) { this.sceneManager = sceneManager; }
    public void reloadSettings() { loadSettings(); }

    private void loadSettings() {
        playbackSpeed = Math.max(0.0, plugin.getConfig().getDouble("settings.playback_speed", 1.0));
        blockActionsPlayback = plugin.getConfig().getBoolean("settings.block_actions_playback", true);
        blockInitialization = plugin.getConfig().getBoolean("settings.block_initialization", true);
        invulnerablePlayback = plugin.getConfig().getBoolean("settings.invulnerable_playback", true);
        chatPlayback = plugin.getConfig().getBoolean("settings.chat_playback", true);
        startAsRecorded = plugin.getConfig().getBoolean("settings.start_as_recorded", false);
        playEntities = new EntityFilter(plugin.getConfig().getString("settings.play_entities", EntityFilter.ALL.expression()));
        preventTrackingPlayedEntities = plugin.getConfig().getBoolean("settings.prevent_tracking_played_entities", true);
        try {
            assignProfile = AssignProfile.valueOf(plugin.getConfig().getString("settings.assign_profile", "no").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            assignProfile = AssignProfile.NO;
        }
        playerNameHandling = plugin.getConfig().getString("settings.player_name_handling", "ignore_casing");
        try {
            dimensionSource = DimensionSource.valueOf(
                    plugin.getConfig().getString("settings.dimension_source", "assigned_or_current")
                            .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            dimensionSource = DimensionSource.ASSIGNED_OR_CURRENT;
        }
    }

    private World resolvePlaybackWorld(RecordingSession recording, Player viewer) {
        return switch (dimensionSource) {
            case CURRENT -> viewer.getWorld();
            case OVERWORLD -> overworldOr(viewer.getWorld());
            case ASSIGNED_OR_OVERWORLD -> assignedOr(recording, overworldOr(viewer.getWorld()));
            case ASSIGNED_OR_CURRENT -> assignedOr(recording, viewer.getWorld());
        };
    }

    private World assignedOr(RecordingSession recording, World fallback) {
        String assigned = recording.getAssignedDimensionKey();
        if (assigned == null || assigned.isBlank()) return fallback;
        return findWorld(assigned, fallback);
    }

    private World overworldOr(World fallback) {
        return Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElse(fallback);
    }

    private World findWorld(String key, World fallback) {
        World world = Bukkit.getWorld(key);
        if (world != null) return world;
        for (World candidate : Bukkit.getWorlds()) {
            if (candidate.getKey().toString().equalsIgnoreCase(key)) return candidate;
        }
        return fallback;
    }

    // Remaining playback lifecycle methods are intentionally unchanged in the repository.
}
