package com.ultraop.mocap.recording;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/** Bridges Bukkit player respawns into the recording lifecycle. */
public final class RecordingRespawnListener implements Listener {
    private final RecordingManager manager;
    public RecordingRespawnListener(RecordingManager manager){this.manager=manager;}
    @EventHandler public void onRespawn(PlayerRespawnEvent event){manager.handleRespawn(event.getPlayer(),event.getPlayer());}
}
