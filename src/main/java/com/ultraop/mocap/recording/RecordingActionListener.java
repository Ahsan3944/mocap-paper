package com.ultraop.mocap.recording;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;

/** Captures discrete player actions that are not reliably represented by a 20 TPS state snapshot. */
public final class RecordingActionListener implements Listener {
    private final RecordingManager manager;
    public RecordingActionListener(RecordingManager manager){this.manager=manager;}
    @EventHandler public void onAnimation(PlayerAnimationEvent event){
        if(event.getAnimationType()==PlayerAnimationType.ARM_SWING) manager.markSwing(event.getPlayer(),false);
    }
    @EventHandler public void onInteract(PlayerInteractEvent event){
        Action action=event.getAction();
        if(action!=Action.LEFT_CLICK_AIR&&action!=Action.LEFT_CLICK_BLOCK)return;
        manager.markSwing(event.getPlayer(),false);
    }
}
