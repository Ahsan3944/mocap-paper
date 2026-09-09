package com.ultraop.mocap.recording;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Captures the recorded player's chat messages when chat_recording is enabled. */
public final class RecordingChatListener implements Listener {
    private final RecordingManager manager;
    public RecordingChatListener(RecordingManager manager){this.manager=manager;}

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onChat(AsyncChatEvent event){
        manager.recordChat(event.getPlayer(), GsonComponentSerializer.gson().serialize(event.message()));
    }
}
