package com.ultraop.mocap.command;

import com.ultraop.mocap.recording.RecordingManager;
import com.ultraop.mocap.recording.RecordingSession;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class MoCapCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final RecordingManager recordingManager;

    public MoCapCommand(JavaPlugin plugin, RecordingManager recordingManager) {
        this.plugin = plugin;
        this.recordingManager = recordingManager;
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

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "recording" -> handleRecording(sender, args);
            case "recordings" -> listRecordings(sender);
            case "info" -> {
                sender.sendMessage(ChatColor.GRAY + "MoCap Paper 0.1.0-SNAPSHOT");
                sender.sendMessage(ChatColor.GRAY + "Paper 1.21.11 feature-parity implementation.");
            }
            case "playback", "scenes", "settings", "misc" ->
                    sender.sendMessage(ChatColor.RED + "That MoCap command is not implemented yet.");
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleRecording(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap recording <start|stop|discard|list>");
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> startRecording(sender);
            case "stop" -> stopRecording(sender, args);
            case "discard" -> discardRecording(sender, args);
            case "list" -> listRecordings(sender);
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap recording <start|stop|discard|list>");
        }
    }

    private void startRecording(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can start a recording without a target player.");
            return;
        }

        RecordingSession session = recordingManager.startRecording(player);
        sender.sendMessage(ChatColor.GREEN + "Recording started: " + session.getId());
    }

    private void stopRecording(CommandSender sender, String[] args) {
        RecordingSession session;
        if (args.length >= 3) {
            UUID id = parseId(sender, args[2]);
            if (id == null) {
                return;
            }
            session = recordingManager.stopRecording(id);
        } else if (sender instanceof Player player) {
            session = recordingManager.stopRecordingForPlayer(player);
        } else {
            sender.sendMessage(ChatColor.RED + "Specify a recording id when executing this from console.");
            return;
        }

        if (session == null) {
            sender.sendMessage(ChatColor.RED + "No active recording was found.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Recording stopped: " + session.getId()
                + " (" + session.getDurationTicks() + " ticks)");
    }

    private void discardRecording(CommandSender sender, String[] args) {
        RecordingSession session;
        if (args.length >= 3) {
            UUID id = parseId(sender, args[2]);
            if (id == null) {
                return;
            }
            session = recordingManager.discard(id);
        } else if (sender instanceof Player player) {
            session = recordingManager.stopRecordingForPlayer(player);
            if (session != null) {
                session = recordingManager.discard(session.getId());
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Specify a recording id when executing this from console.");
            return;
        }

        sender.sendMessage(session == null
                ? ChatColor.RED + "No recording was found."
                : ChatColor.GREEN + "Recording discarded: " + session.getId());
    }

    private void listRecordings(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Active recordings: " + recordingManager.getActive().size());
        for (RecordingSession session : recordingManager.getActive()) {
            sender.sendMessage(ChatColor.GRAY + "  " + session.getId() + " | "
                    + session.getSourcePlayerName() + " | " + session.getDurationTicks() + " ticks");
        }

        sender.sendMessage(ChatColor.GOLD + "Completed recordings: " + recordingManager.getCompleted().size());
        for (RecordingSession session : recordingManager.getCompleted()) {
            sender.sendMessage(ChatColor.GRAY + "  " + session.getId() + " | "
                    + session.getSourcePlayerName() + " | " + session.getDurationTicks() + " ticks");
        }
    }

    private @Nullable UUID parseId(CommandSender sender, String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            sender.sendMessage(ChatColor.RED + "Invalid recording id: " + value);
            return null;
        }
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
            return partial(args[0], List.of("recording", "playback", "recordings", "scenes", "settings", "misc", "info", "help"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("recording")) {
            return partial(args[1], List.of("start", "stop", "discard", "save", "list"));
        }
        return List.of();
    }

    private List<String> partial(String input, List<String> values) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.startsWith(lower)).toList();
    }

    public void shutdown() {
        // RecordingManager owns the tick task and is shut down by the plugin lifecycle.
    }
}
