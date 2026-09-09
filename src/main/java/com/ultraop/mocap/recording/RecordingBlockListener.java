package com.ultraop.mocap.recording;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.entity.Player;

import java.util.Collection;

/** Captures server-side block actions into every active recording owned by the actor. */
public final class RecordingBlockListener implements Listener {
    private final RecordingManager recordings;

    public RecordingBlockListener(RecordingManager recordings) { this.recordings = recordings; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        record(event.getPlayer(), session -> session.recordBlockAction(
                BlockActionFrame.place(session.currentTick(), event.getBlock().getLocation(),
                        event.getBlockReplacedState().getBlockData(), event.getBlock().getBlockData())));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        record(event.getPlayer(), session -> session.recordBlockAction(
                BlockActionFrame.breakBlock(session.currentTick(), block.getLocation(), block.getBlockData(), null)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        record(event.getPlayer(), session -> session.recordBlockAction(
                BlockActionFrame.interact(session.currentTick(), block.getLocation(), block.getBlockData())));
    }

    private void record(Player player, java.util.function.Consumer<RecordingSession> consumer) {
        Collection<RecordingSession> active = recordings.getActive();
        for (RecordingSession session : active) {
            if (session.getSourcePlayerId().equals(player.getUniqueId())) consumer.accept(session);
        }
    }
}
