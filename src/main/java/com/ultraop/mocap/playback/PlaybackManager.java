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

    public PlaybackManager(JavaPlugin plugin, RecordingManager recordingManager) { this.plugin = plugin; this.recordingManager = recordingManager; }
    public void start() { if (ticker == null) ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L); }
    public PlaybackSession play(UUID recordingId, Player viewer) { RecordingSession r=recordingManager.get(recordingId); return r==null?null:play(r,viewer); }
    public PlaybackSession playSaved(String name, Player viewer) { RecordingSession r=recordingManager.getSaved(name); return r==null?null:play(r,viewer); }
    public PlaybackSession play(RecordingSession recording, Player viewer) { return play(recording,viewer,getModifiers(viewer)); }
    public PlaybackSession play(RecordingSession recording, Player viewer, PlaybackModifiers effectiveModifiers) {
        PlaybackSession s=new PlaybackSession(recording,viewer,effectiveModifiers);
        if(s.isStopped()) return null; active.put(s.getId(),s); return s;
    }
    public PlaybackSession stop(UUID id) { PlaybackSession s=active.remove(id); if(s!=null)s.stop(); return s; }
    public int stopAll(Player owner) { int n=0; for(PlaybackSession s:new ArrayList<>(active.values())) if(owner==null||owner.getUniqueId().equals(s.getViewerPlayerId())){s.stop();active.remove(s.getId());n++;} return n; }
    public PlaybackSession get(UUID id){return active.get(id);}
    public PlaybackModifiers getModifiers(Player player){return modifiers.getOrDefault(player.getUniqueId(),PlaybackModifiers.DEFAULT);}
    public void setModifiers(Player player,PlaybackModifiers value){modifiers.put(player.getUniqueId(),value);}
    public void resetModifiers(Player player){modifiers.remove(player.getUniqueId());}
    public Collection<PlaybackSession> getActive(){return java.util.Collections.unmodifiableList(new ArrayList<>(active.values()));}
    public void shutdown(){if(ticker!=null){ticker.cancel();ticker=null;}active.values().forEach(PlaybackSession::stop);active.clear();modifiers.clear();}
    private void tick(){for(PlaybackSession s:new ArrayList<>(active.values())){s.advance();if(s.isStopped())active.remove(s.getId());}}
}
