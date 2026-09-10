package com.ultraop.mocap.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/** Command layer for /mocap misc. */
public final class MiscCommand {
    private final SkinSuggestionCache skinSuggestions;
    public MiscCommand(SkinSuggestionCache skinSuggestions) { this.skinSuggestions = skinSuggestions; }
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { usage(sender); return; }
        switch (args[1].toLowerCase()) {
            case "clear_cache" -> { skinSuggestions.clear(); sender.sendMessage(ChatColor.GREEN + "MoCap caches cleared."); }
            case "refresh_suggestions" -> { skinSuggestions.refresh(); sender.sendMessage(ChatColor.GREEN + "MoCap command suggestions refreshed."); }
            case "extensions" -> sender.sendMessage(ChatColor.GRAY + "No MoCap extensions are loaded.");
            case "sync" -> sender.sendMessage(ChatColor.RED + "Sync is not available on the Paper port.");
            default -> usage(sender);
        }
    }
    private void usage(CommandSender sender) { sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap misc <sync|clear_cache|refresh_suggestions|extensions>"); }
}
