package com.ultraop.mocap.playback;

import com.ultraop.mocap.recording.RecordingManager;
import com.ultraop.mocap.recording.RecordingSession;
import com.ultraop.mocap.scene.SceneManager;
import com.ultraop.mocap.scene.ScenePlayback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Owns standalone and scene playback timelines and advances them at 20 TPS. */
public final class PlaybackManager {
    private final JavaPlugin plugin;
    private final RecordingManager recordingManager;
    private final Map<UUID, PlaybackSession> active = new LinkedHashMap<>();
    private final Map<UUID, ScenePlayback> activeScenes = new LinkedHashMap<>();
    private final Map<UUID, PlaybackModifiers> modifiers = new LinkedHashMap<>();
    private SceneManager sceneManager;
    private BukkitTask ticker;
    private boolean blockActionsPlayback = true;
    private boolean blockInitialization = true;

    public PlaybackManager(JavaPlugin plugin, RecordingManager recordingManager) { this.plugin = plugin; this.recordingManager = recordingManager; loadSettings(); }
    public void setSceneManager(SceneManager sceneManager) { this.sceneManager = sceneManager; }
    private void loadSettings() { blockActionsPlayback = plugin.getConfig().getBoolean("playback.block_actions_playback", true); blockInitialization = plugin.getConfig().getBoolean("playback.block_initialization", true); }
    public boolean isBlockActionsPlayback() { return blockActionsPlayback; }
    public boolean isBlockInitialization() { return blockInitialization; }
    public void setBlockActionsPlayback(boolean value) { blockActionsPlayback=value; plugin.getConfig().set("playback.block_actions_playback",value); plugin.saveConfig(); }
    public void setBlockInitialization(boolean value) { blockInitialization=value; plugin.getConfig().set("playback.block_initialization",value); plugin.saveConfig(); }

    public void start() { if (ticker == null) ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L); }
    public PlaybackSession play(UUID recordingId, Player viewer) { RecordingSession r = recordingManager.get(recordingId); return r == null ? null : play(r, viewer); }
    public PlaybackSession playSaved(String name, Player viewer) { RecordingSession r = recordingManager.getSaved(name); return r == null ? null : play(r, viewer); }
    public PlaybackSession play(RecordingSession recording, Player viewer) { return play(recording, viewer, getModifiers(viewer)); }
    public PlaybackSession play(RecordingSession recording, Player viewer, PlaybackModifiers effectiveModifiers) { PlaybackSession s = create(recording, viewer, effectiveModifiers, null, true); if (s == null) return null; active.put(s.getId(), s); return s; }
    public PlaybackSession create(RecordingSession recording, Player viewer, PlaybackModifiers effectiveModifiers, PositionTransformer transformer) { return create(recording, viewer, effectiveModifiers, transformer, false); }
    public PlaybackSession create(RecordingSession recording, Player viewer, PlaybackModifiers effectiveModifiers, PositionTransformer transformer, boolean root) { PlaybackSession s = new PlaybackSession(recording, viewer, effectiveModifiers, transformer, root, blockActionsPlayback, blockInitialization); return s.isStopped() ? null : s; }
    public PlaybackSession createSubscene(RecordingSession recording, Player viewer, PlaybackModifiers effectiveModifiers, PositionTransformer transformer) { return create(recording, viewer, effectiveModifiers, transformer, false); }
    public ScenePlayback playScene(String sceneName, Player viewer, PlaybackModifiers effectiveModifiers) { if (sceneManager == null) return null; ScenePlayback scene = ScenePlayback.start(sceneManager, this, sceneName, viewer, effectiveModifiers); if (scene == null) return null; activeScenes.put(scene.getId(), scene); return scene; }
    public PlaybackSession stop(UUID id) { PlaybackSession s = active.remove(id); if (s != null) s.stop(); return s; }
    public ScenePlayback stopScene(UUID id) { ScenePlayback s = activeScenes.remove(id); if (s != null) s.stop(); return s; }
    public int stopAll(Player owner) { int n=0; for(PlaybackSession s:new ArrayList<>(active.values())) if(owner==null||owner.getUniqueId().equals(s.getViewerPlayerId())){s.stop();active.remove(s.getId());n++;} for(ScenePlayback s:new ArrayList<>(activeScenes.values())){s.stop();activeScenes.remove(s.getId());n++;} return n; }
    public PlaybackSession get(UUID id){return active.get(id);} public ScenePlayback getScene(UUID id){return activeScenes.get(id);} public PlaybackModifiers getModifiers(Player player){return modifiers.getOrDefault(player.getUniqueId(),PlaybackModifiers.DEFAULT);} public void setModifiers(Player player,PlaybackModifiers value){modifiers.put(player.getUniqueId(),value);} public void resetModifiers(Player player){modifiers.remove(player.getUniqueId());}
    public Collection<PlaybackSession> getActive(){return java.util.Collections.unmodifiableList(new ArrayList<>(active.values()));} public Collection<ScenePlayback> getActiveScenes(){return java.util.Collections.unmodifiableList(new ArrayList<>(activeScenes.values()));}
    public void shutdown(){if(ticker!=null){ticker.cancel();ticker=null;}active.values().forEach(PlaybackSession::stop);activeScenes.values().forEach(ScenePlayback::stop);active.clear();activeScenes.clear();modifiers.clear();}
    private void tick(){for(PlaybackSession s:new ArrayList<>(active.values())){s.advance();if(s.isStopped())active.remove(s.getId());}for(ScenePlayback s:new ArrayList<>(activeScenes.values())){s.tick();if(s.isStopped())activeScenes.remove(s.getId());}}
}
