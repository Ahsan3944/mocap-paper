package com.ultraop.mocap.recording;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class RecordingContainerListener implements Listener {
    private final RecordingManager recordings;
    public RecordingContainerListener(RecordingManager recordings){this.recordings=recordings;}
    @EventHandler(priority=EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event){if(event.getPlayer() instanceof Player p)recordings.markCloseContainer(p);}
}