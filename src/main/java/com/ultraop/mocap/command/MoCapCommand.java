package com.ultraop.mocap.command;

import com.ultraop.mocap.playback.PlaybackManager;
import com.ultraop.mocap.playback.PlaybackSession;
import com.ultraop.mocap.recording.RecordingManager;
import com.ultraop.mocap.recording.RecordingSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class MoCapCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final RecordingManager recordingManager;
    private final PlaybackManager playbackManager;

    public MoCapCommand(JavaPlugin plugin, RecordingManager recordingManager, PlaybackManager playbackManager) {
        this.plugin = plugin;
        this.recordingManager = recordingManager;
        this.playbackManager = playbackManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
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
            case "recordings" -> handleRecordings(sender, args);
            case "playback" -> handlePlayback(sender, args);
            case "info" -> {
                sender.sendMessage(ChatColor.GRAY + "MoCap Paper 0.1.0-SNAPSHOT");
                sender.sendMessage(ChatColor.GRAY + "Paper 1.21.11 feature-parity implementation.");
            }
            case "scenes", "settings", "misc" ->
                    sender.sendMessage(ChatColor.RED + "That MoCap command is not implemented yet.");
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handlePlayback(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap playback <start|stop|stop_all|list>");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> startPlayback(sender, args);
            case "stop" -> stopPlayback(sender, args);
            case "stop_all" -> stopAllPlayback(sender, args);
            case "list" -> listPlayback(sender);
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap playback <start|stop|stop_all|list>");
        }
    }

    private void startPlayback(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap playback start <name> [player|selector]");
            return;
        }
        RecordingSession recording = recordingManager.getSaved(args[2]);
        if (recording == null) {
            sender.sendMessage(ChatColor.RED + "Recording not found: " + args[2]);
            return;
        }

        List<Player> viewers = resolvePlaybackPlayers(sender, args.length >= 4 ? args[3] : null);
        if (viewers.isEmpty()) return;

        int started = 0;
        for (Player viewer : viewers) {
            PlaybackSession session = playbackManager.play(recording, viewer);
            if (session != null) {
                sender.sendMessage(ChatColor.GREEN + "Playback started: " + session.getId()
                        + " (" + args[2] + ")");
                started++;
            }
        }
        if (started == 0) sender.sendMessage(ChatColor.RED + "Playback could not be started.");
    }

    private List<Player> resolvePlaybackPlayers(CommandSender sender, String selector) {
        if (selector == null) {
            if (sender instanceof Player player) return List.of(player);
            sender.sendMessage(ChatColor.RED + "Specify a player when executing this command from console.");
            return List.of();
        }
        try {
            List<Player> players = new ArrayList<>();
            for (Entity entity : Bukkit.selectEntities(sender, selector)) {
                if (entity instanceof Player player) players.add(player);
            }
            if (players.isEmpty()) sender.sendMessage(ChatColor.RED + "No players matched the playback target.");
            return players;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid or unresolved player selector: " + selector);
            return List.of();
        }
    }

    private void stopPlayback(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap playback stop <id>");
            return;
        }
        UUID id = parseId(sender, args[2]);
        if (id == null) return;
        PlaybackSession session = playbackManager.stop(id);
        sender.sendMessage(session == null
                ? ChatColor.RED + "Playback not found: " + args[2]
                : ChatColor.GREEN + "Playback stopped: " + args[2]);
    }

    private void stopAllPlayback(CommandSender sender, String[] args) {
        boolean includingOthers = args.length >= 3 && args[2].equalsIgnoreCase("including_others");
        if (includingOthers && !(sender instanceof Player)) {
            sender.sendMessage(ChatColor.GREEN + "Stopped " + playbackManager.stopAll(null) + " playback(s).");
            return;
        }
        if (includingOthers) {
            sender.sendMessage(ChatColor.GREEN + "Stopped " + playbackManager.stopAll(null) + " playback(s).");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Specify 'including_others' when executing this from console.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Stopped " + playbackManager.stopAll(player) + " playback(s).");
    }

    private void listPlayback(CommandSender sender) {
        CollectionSnapshot snapshot = new CollectionSnapshot(playbackManager.getActive());
        sender.sendMessage(ChatColor.GOLD + "Active playbacks: " + snapshot.sessions.size());
        for (PlaybackSession session : snapshot.sessions) {
            sender.sendMessage(ChatColor.GRAY + "  " + session.getId()
                    + " | " + session.getRecording().getSourcePlayerName()
                    + " | tick " + session.getTick());
        }
    }

    private static final class CollectionSnapshot {
        private final List<PlaybackSession> sessions;
        private CollectionSnapshot(java.util.Collection<PlaybackSession> sessions) {
            this.sessions = new ArrayList<>(sessions);
        }
    }

    private void handleRecording(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap recording <start|stop|discard|save|list>");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> startRecording(sender, args);
            case "stop" -> stopRecording(sender, args);
            case "discard" -> discardRecording(sender, args);
            case "save" -> saveRecording(sender, args);
            case "list" -> listRecording(sender, args);
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap recording <start|stop|discard|save|list>");
        }
    }

    private void startRecording(CommandSender sender, String[] args) {
        List<Player> players = new ArrayList<>();
        if (args.length == 2) {
            if (sender instanceof Player player) players.add(player);
            else {
                sender.sendMessage(ChatColor.RED + "Specify a player when executing this command from console.");
                return;
            }
        } else {
            try {
                for (Entity entity : Bukkit.selectEntities(sender, args[2])) {
                    if (entity instanceof Player player) players.add(player);
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid or unresolved player selector: " + args[2]);
                return;
            }
            if (players.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No players matched the recording target.");
                return;
            }
        }

        String instantSave = args.length >= 4 ? args[3] : null;
        int started = 0;
        for (Player player : players) {
            String saveName = players.size() > 1 && instantSave != null
                    ? instantSave + "_" + player.getName() : instantSave;
            if (hasActiveRecording(player)) {
                sender.sendMessage(ChatColor.RED + "Player is already being recorded: " + player.getName());
                continue;
            }
            RecordingSession session = recordingManager.startRecording(player, saveName);
            sender.sendMessage(ChatColor.GREEN + "Recording started: " + session.getId() + " (" + player.getName() + ")");
            started++;
        }
        if (started == 0) sender.sendMessage(ChatColor.RED + "No recording was started.");
    }

    private boolean hasActiveRecording(Player player) {
        return recordingManager.getActive().stream()
                .anyMatch(session -> session.getSourcePlayerId().equals(player.getUniqueId()));
    }

    private void stopRecording(CommandSender sender, String[] args) {
        RecordingSession session;
        if (args.length >= 3) {
            UUID id = parseId(sender, args[2]);
            if (id == null) return;
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
        if (session.getInstantSaveName() != null) sender.sendMessage(ChatColor.GREEN + "Saved as: " + session.getInstantSaveName());
    }

    private void discardRecording(CommandSender sender, String[] args) {
        RecordingSession session;
        if (args.length >= 3) {
            UUID id = parseId(sender, args[2]);
            if (id == null) return;
            session = recordingManager.discard(id);
        } else if (sender instanceof Player player) {
            RecordingSession active = recordingManager.stopRecordingForPlayer(player);
            session = active == null ? null : recordingManager.discard(active.getId());
        } else {
            sender.sendMessage(ChatColor.RED + "Specify a recording id when executing this from console.");
            return;
        }
        sender.sendMessage(session == null ? ChatColor.RED + "No recording was found."
                : ChatColor.GREEN + "Recording discarded: " + session.getId());
    }

    private void saveRecording(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap recording save <name> [id]");
            return;
        }
        String name = args[2];
        RecordingSession session = null;
        if (args.length >= 4) {
            UUID id = parseId(sender, args[3]);
            if (id == null) return;
            session = recordingManager.get(id);
        } else if (sender instanceof Player player) {
            session = recordingManager.getCompleted().stream()
                    .filter(value -> value.getSourcePlayerId().equals(player.getUniqueId()))
                    .max(Comparator.comparing(RecordingSession::getStoppedAt,
                            Comparator.nullsFirst(Comparator.naturalOrder()))).orElse(null);
        } else if (!recordingManager.getCompleted().isEmpty()) {
            session = recordingManager.getCompleted().stream()
                    .max(Comparator.comparing(RecordingSession::getStoppedAt,
                            Comparator.nullsFirst(Comparator.naturalOrder()))).orElse(null);
        }
        if (session == null) {
            sender.sendMessage(ChatColor.RED + "No completed recording is available to save.");
            return;
        }
        try {
            recordingManager.save(name, session);
            sender.sendMessage(ChatColor.GREEN + "Recording saved as: " + name);
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Unable to save recording: " + e.getMessage());
        }
    }

    private void listRecording(CommandSender sender, String[] args) {
        if (args.length >= 3) {
            UUID id = parseId(sender, args[2]);
            if (id == null) return;
            RecordingSession session = recordingManager.get(id);
            if (session == null) {
                sender.sendMessage(ChatColor.RED + "Recording not found: " + id);
                return;
            }
            sendRecordingInfo(sender, session);
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "Active recordings: " + recordingManager.getActive().size());
        for (RecordingSession session : recordingManager.getActive()) sendRecordingInfo(sender, session);
        sender.sendMessage(ChatColor.GOLD + "Completed recordings: " + recordingManager.getCompleted().size());
        for (RecordingSession session : recordingManager.getCompleted()) sendRecordingInfo(sender, session);
    }

    private void handleRecordings(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
            sender.sendMessage(ChatColor.GOLD + "Saved recordings: " + recordingManager.getSavedNames().size());
            for (String name : recordingManager.getSavedNames()) {
                RecordingSession session = recordingManager.getSaved(name);
                sender.sendMessage(ChatColor.GRAY + "  " + name + " | " + session.getDurationTicks() + " ticks | " + session.getSourcePlayerName());
            }
            return;
        }
        try {
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "copy" -> {
                    requireArgs(sender, args, 4, "/mocap recordings copy <src_name> <dest_name>");
                    if (args.length < 4) return;
                    sender.sendMessage(recordingManager.copy(args[2], args[3]) ? ChatColor.GREEN + "Recording copied." : ChatColor.RED + "Unable to copy recording.");
                }
                case "rename" -> {
                    requireArgs(sender, args, 4, "/mocap recordings rename <old_name> <new_name>");
                    if (args.length < 4) return;
                    sender.sendMessage(recordingManager.rename(args[2], args[3]) ? ChatColor.GREEN + "Recording renamed." : ChatColor.RED + "Unable to rename recording.");
                }
                case "remove" -> {
                    requireArgs(sender, args, 3, "/mocap recordings remove <name>");
                    if (args.length < 3) return;
                    sender.sendMessage(recordingManager.removeSaved(args[2]) ? ChatColor.GREEN + "Recording removed." : ChatColor.RED + "Recording not found.");
                }
                case "info" -> {
                    requireArgs(sender, args, 3, "/mocap recordings info <name>");
                    if (args.length < 3) return;
                    RecordingSession session = recordingManager.getSaved(args[2]);
                    if (session == null) sender.sendMessage(ChatColor.RED + "Recording not found: " + args[2]);
                    else sendRecordingInfo(sender, session);
                }
                default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap recordings <copy|rename|remove|info|list> ...");
            }
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Recording file operation failed: " + e.getMessage());
        }
    }

    private void requireArgs(CommandSender sender, String[] args, int count, String usage) {
        if (args.length < count) sender.sendMessage(ChatColor.YELLOW + "Usage: " + usage);
    }

    private void sendRecordingInfo(CommandSender sender, RecordingSession session) {
        sender.sendMessage(ChatColor.GRAY + "  " + session.getId() + " | " + session.getSourcePlayerName() + " | " + session.getDurationTicks() + " ticks");
    }

    private @Nullable UUID parseId(CommandSender sender, String value) {
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException ignored) {
            sender.sendMessage(ChatColor.RED + "Invalid id: " + value);
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
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return partial(args[0], List.of("recording", "playback", "recordings", "scenes", "settings", "misc", "info", "help"));
        if (args.length == 2 && args[0].equalsIgnoreCase("recording")) return partial(args[1], List.of("start", "stop", "discard", "save", "list"));
        if (args.length == 2 && args[0].equalsIgnoreCase("playback")) return partial(args[1], List.of("start", "stop", "stop_all", "modifiers", "list"));
        if (args.length == 2 && args[0].equalsIgnoreCase("recordings")) return partial(args[1], List.of("copy", "rename", "remove", "info", "list"));
        if (args.length == 3 && args[0].equalsIgnoreCase("playback") && args[1].equalsIgnoreCase("start")) return new ArrayList<>(recordingManager.getSavedNames());
        if (args.length == 3 && args[0].equalsIgnoreCase("playback") && args[1].equalsIgnoreCase("stop_all")) return partial(args[2], List.of("including_others", "excluding_others"));
        return List.of();
    }

    private List<String> partial(String input, List<String> values) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.startsWith(lower)).toList();
    }

    public void shutdown() { }
}
