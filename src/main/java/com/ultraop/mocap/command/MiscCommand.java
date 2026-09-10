package com.ultraop.mocap.command;

import com.ultraop.mocap.playback.MineSkinSkins;
import com.ultraop.mocap.playback.PlaybackSynchronization;
import com.ultraop.mocap.playback.ProfileUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Command layer for /mocap misc. */
public final class MiscCommand {
    private final SkinSuggestionCache skinSuggestions;
    public MiscCommand(SkinSuggestionCache skinSuggestions) { this.skinSuggestions = skinSuggestions; }
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { usage(sender); return; }
        switch (args[1].toLowerCase()) {
            case "clear_cache" -> { clearCaches(); sender.sendMessage(ChatColor.GREEN + "MoCap caches cleared."); }
            case "refresh_suggestions" -> { skinSuggestions.refresh(); sender.sendMessage(ChatColor.GREEN + "MoCap command suggestions refreshed."); }
            case "extensions" -> sender.sendMessage(ChatColor.GRAY + "No MoCap extensions are loaded.");
            case "sync" -> sync(sender, args);
            default -> usage(sender);
        }
    }
    private void clearCaches() { skinSuggestions.clear(); ProfileUtils.clearCache(); MineSkinSkins.clearCache(); }
    private void sync(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "A player is required for sync."); return; }
        if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap misc sync <enable|disable>"); return; }
        if (args[2].equalsIgnoreCase("enable") || args[2].equalsIgnoreCase("disable")) {
            boolean enabled = args[2].equalsIgnoreCase("enable");
            boolean old = PlaybackSynchronization.set(player.getUniqueId(), enabled);
            sender.sendMessage(ChatColor.GREEN + "Playback synchronization " + (enabled ? "enabled" : "disabled") + (old == enabled ? " (unchanged)." : "."));
        } else sender.sendMessage(ChatColor.RED + "Mode must be enable or disable.");
    }
    private void usage(CommandSender sender) { sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap misc <sync|clear_cache|refresh_suggestions|extensions>"); }
}
