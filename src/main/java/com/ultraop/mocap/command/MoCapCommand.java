package com.ultraop.mocap.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MoCapCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;

    public MoCapCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!sender.hasPermission("mocap.use")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use /mocap.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(ChatColor.GRAY + "MoCap Paper 0.1.0-SNAPSHOT");
            sender.sendMessage(ChatColor.GRAY + "Paper 1.21.11 feature-parity implementation.");
            return true;
        }

        // The command tree is intentionally kept centralized here during the foundation phase.
        // Individual command domains will be moved into dedicated handlers as their parity
        // implementation is completed.
        sender.sendMessage(ChatColor.RED + "That MoCap command is not implemented yet.");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "/mocap");
        sender.sendMessage(ChatColor.GRAY + "  recording");
        sender.sendMessage(ChatColor.GRAY + "  playback");
        sender.sendMessage(ChatColor.GRAY + "  recordings");
        sender.sendMessage(ChatColor.GRAY + "  scenes");
        sender.sendMessage(ChatColor.GRAY + "  settings");
        sender.sendMessage(ChatColor.GRAY + "  misc");
        sender.sendMessage(ChatColor.GRAY + "  info");
        sender.sendMessage(ChatColor.GRAY + "  help");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                 @NotNull Command command,
                                                 @NotNull String alias,
                                                 @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("recording", "playback", "recordings", "scenes", "settings", "misc", "info", "help")
                    .stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    public void shutdown() {
        // Runtime managers will be stopped here once the recording/playback engine is installed.
    }
}
