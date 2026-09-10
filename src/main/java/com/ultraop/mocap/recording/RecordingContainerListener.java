package com.ultraop.mocap.recording;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.Player;

/** Captures container close actions for the recording owner. */
public final class RecordingContainerListener implements Listener {
    private final RecordingManager recordings;
    public RecordingContainerListener(RecordingManager recordings){this.recordings=recordings;}
    @EventHandler(priority=EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event){if(!(event.getPlayer() instanceof Player player))return;for(RecordingSession session:recordings.getActive())if(session.getSourcePlayerId().equals(player.getUniqueId()))session.recordCloseContainer();}
}
