package com.ultraop.mocap.recording;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;

/** Captures the vanilla block-breaking progress lifecycle available through Bukkit events. */
public final class RecordingBreakProgressListener implements Listener {
    private final RecordingManager recordings;
    public RecordingBreakProgressListener(RecordingManager recordings){this.recordings=recordings;}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onDamage(BlockDamageEvent event){for(RecordingSession s:recordings.getActive())if(s.getSourcePlayerId().equals(event.getPlayer().getUniqueId()))s.recordBreakProgress(BreakProgressFrame.create(s.currentTick(),event.getBlock().getLocation(),event.getInstaBreak()?10:0));}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event){for(RecordingSession s:recordings.getActive())if(s.getSourcePlayerId().equals(event.getPlayer().getUniqueId()))s.recordBreakProgress(BreakProgressFrame.create(s.currentTick(),event.getBlock().getLocation(),10));}
}
