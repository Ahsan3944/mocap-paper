package com.ultraop.mocap.command;

import com.ultraop.mocap.playback.PlaybackManager;
import com.ultraop.mocap.playback.PlaybackModifiers;
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

    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("mocap.use")) { sender.sendMessage(ChatColor.RED + "You do not have permission to use /mocap."); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "recording" -> handleRecording(sender, args);
            case "recordings" -> handleRecordings(sender, args);
            case "playback" -> handlePlayback(sender, args);
            case "info" -> { sender.sendMessage(ChatColor.GRAY + "MoCap Paper 0.1.0-SNAPSHOT"); sender.sendMessage(ChatColor.GRAY + "Paper 1.21.11 feature-parity implementation."); }
            case "scenes", "settings", "misc" -> sender.sendMessage(ChatColor.RED + "That MoCap command is not implemented yet.");
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handlePlayback(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap playback <start|stop|stop_all|modifiers|list>"); return; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> startPlayback(sender, args); case "stop" -> stopPlayback(sender, args); case "stop_all" -> stopAllPlayback(sender, args);
            case "modifiers" -> handleModifiers(sender, args); case "list" -> listPlayback(sender);
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap playback <start|stop|stop_all|modifiers|list>");
        }
    }

    private void startPlayback(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap playback start <name> [player|selector]"); return; }
        RecordingSession recording = recordingManager.getSaved(args[2]);
        if (recording == null) { sender.sendMessage(ChatColor.RED + "Recording not found: " + args[2]); return; }
        List<Player> viewers = resolvePlaybackPlayers(sender, args.length >= 4 ? args[3] : null);
        if (viewers.isEmpty()) return;
        int started = 0;
        for (Player viewer : viewers) { PlaybackSession session = playbackManager.play(recording, viewer); if (session != null) { sender.sendMessage(ChatColor.GREEN + "Playback started: " + session.getId() + " (" + args[2] + ")"); started++; } }
        if (started == 0) sender.sendMessage(ChatColor.RED + "Playback could not be started.");
    }

    private List<Player> resolvePlaybackPlayers(CommandSender sender, String selector) {
        if (selector == null) { if (sender instanceof Player player) return List.of(player); sender.sendMessage(ChatColor.RED + "Specify a player when executing this from console."); return List.of(); }
        try { List<Player> players = new ArrayList<>(); for (Entity entity : Bukkit.selectEntities(sender, selector)) if (entity instanceof Player player) players.add(player); if (players.isEmpty()) sender.sendMessage(ChatColor.RED + "No players matched the playback target."); return players; }
        catch (IllegalArgumentException e) { sender.sendMessage(ChatColor.RED + "Invalid or unresolved player selector: " + selector); return List.of(); }
    }

    private void stopPlayback(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap playback stop <id>"); return; }
        UUID id = parseId(sender, args[2]); if (id == null) return;
        PlaybackSession session = playbackManager.stop(id);
        sender.sendMessage(session == null ? ChatColor.RED + "Playback not found: " + args[2] : ChatColor.GREEN + "Playback stopped: " + args[2]);
    }

    private void stopAllPlayback(CommandSender sender, String[] args) {
        boolean includingOthers = args.length >= 3 && args[2].equalsIgnoreCase("including_others");
        if (includingOthers) { sender.sendMessage(ChatColor.GREEN + "Stopped " + playbackManager.stopAll(null) + " playback(s)."); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "Specify 'including_others' when executing this from console."); return; }
        sender.sendMessage(ChatColor.GREEN + "Stopped " + playbackManager.stopAll(player) + " playback(s).");
    }

    private void handleModifiers(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "A player is required for playback modifiers."); return; }
        if (args.length < 3 || args[2].equalsIgnoreCase("list")) { listModifiers(sender, player); return; }
        if (args[2].equalsIgnoreCase("reset")) { playbackManager.resetModifiers(player); sender.sendMessage(ChatColor.GREEN + "Playback modifiers reset."); return; }
        if (!args[2].equalsIgnoreCase("set") || args.length < 5) { sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap playback modifiers <set|list|reset> ..."); return; }
        PlaybackModifiers current = playbackManager.getModifiers(player);
        try {
            PlaybackModifiers next = switch (args[3].toLowerCase(Locale.ROOT)) {
                case "time" -> setTimeModifier(current, args);
                case "transformations" -> setTransformationModifier(current, args);
                default -> throw new IllegalArgumentException("Unknown modifier group: " + args[3]);
            };
            playbackManager.setModifiers(player, next);
            sender.sendMessage(ChatColor.GREEN + "Playback modifier updated.");
        } catch (IllegalArgumentException e) { sender.sendMessage(ChatColor.RED + e.getMessage()); }
    }

    private PlaybackModifiers setTimeModifier(PlaybackModifiers m, String[] a) {
        if (a.length < 6) throw new IllegalArgumentException("Usage: ... time <start_delay|wait_on_start|wait_on_end|wait_for_parent_end|loop> <value>");
        return switch (a[4].toLowerCase(Locale.ROOT)) {
            case "start_delay" -> m.withStartDelay(nonNegativeDouble(a[5])); case "wait_on_start" -> m.withWaitOnStart(nonNegativeDouble(a[5])); case "wait_on_end" -> m.withWaitOnEnd(nonNegativeDouble(a[5]));
            case "wait_for_parent_end" -> m.withWaitForParentEnd(parseBoolean(a[5])); case "loop" -> m.withLoop(parseBoolean(a[5]));
            default -> throw new IllegalArgumentException("Unknown time modifier: " + a[4]);
        };
    }

    private PlaybackModifiers setTransformationModifier(PlaybackModifiers m, String[] a) {
        if (a.length < 6) throw new IllegalArgumentException("Missing transformation value.");
        return switch (a[4].toLowerCase(Locale.ROOT)) {
            case "rotation" -> m.withRotation(Double.parseDouble(a[5]));
            case "mirror" -> m.withMirror(PlaybackModifiers.Mirror.valueOf(a[5].toUpperCase(Locale.ROOT)));
            case "scale" -> { if (a.length < 7) throw new IllegalArgumentException("Usage: ... transformations scale <of_player|of_scene> <scale>"); double v = nonNegativeDouble(a[6]); yield switch (a[5].toLowerCase(Locale.ROOT)) { case "of_player" -> m.withPlayerScale(v); case "of_scene" -> m.withSceneScale(v); default -> throw new IllegalArgumentException("Scale target must be of_player or of_scene."); }; }
            case "offset" -> { if (a.length < 8) throw new IllegalArgumentException("Usage: ... transformations offset <x> <y> <z>"); yield m.withOffset(Double.parseDouble(a[5]), Double.parseDouble(a[6]), Double.parseDouble(a[7])); }
            case "config" -> setTransformationConfig(m, a);
            default -> throw new IllegalArgumentException("Unknown transformation modifier: " + a[4]);
        };
    }

    private PlaybackModifiers setTransformationConfig(PlaybackModifiers m, String[] a) {
        if (a.length < 7) throw new IllegalArgumentException("Usage: ... transformations config <round_block_pos|recording_center|scene_center|center_offset> ...");
        PlaybackModifiers.TransformationConfig c = m.transformationConfig();
        return switch (a[5].toLowerCase(Locale.ROOT)) {
            case "round_block_pos" -> m.withTransformationConfig(c.withRoundBlockPos(parseBoolean(a[6])));
            case "recording_center" -> m.withTransformationConfig(c.withRecordingCenter(PlaybackModifiers.RecordingCenter.valueOf(a[6].toUpperCase(Locale.ROOT))));
            case "scene_center" -> {
                PlaybackModifiers.SceneCenterType type = PlaybackModifiers.SceneCenterType.valueOf(a[6].toUpperCase(Locale.ROOT));
                String specific = type == PlaybackModifiers.SceneCenterType.COMMON_SPECIFIC ? requireArg(a, 7, "specific scene element") : null;
                yield m.withTransformationConfig(c.withSceneCenter(type, specific));
            }
            case "center_offset" -> {
                if (a.length < 9) throw new IllegalArgumentException("Usage: ... transformations config center_offset <x> <y> <z>");
                yield m.withTransformationConfig(c.withCenterOffset(Double.parseDouble(a[6]), Double.parseDouble(a[7]), Double.parseDouble(a[8])));
            }
            default -> throw new IllegalArgumentException("Unknown transformation config: " + a[5]);
        };
    }

    private String requireArg(String[] args, int index, String label) { if (args.length <= index) throw new IllegalArgumentException("Missing " + label + "."); return args[index]; }
    private boolean parseBoolean(String value) { if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) throw new IllegalArgumentException("Value must be true or false."); return Boolean.parseBoolean(value); }
    private double nonNegativeDouble(String value) { double result = Double.parseDouble(value); if (!Double.isFinite(result) || result < 0.0) throw new IllegalArgumentException("Value must be a finite non-negative number."); return result; }

    private void listModifiers(CommandSender sender, Player player) {
        PlaybackModifiers m = playbackManager.getModifiers(player); PlaybackModifiers.TransformationConfig c = m.transformationConfig();
        sender.sendMessage(ChatColor.GOLD + "Playback modifiers:");
        sender.sendMessage(ChatColor.GRAY + "  time: start_delay=" + m.startDelaySeconds() + ", wait_on_start=" + m.waitOnStartSeconds() + ", wait_on_end=" + m.waitOnEndSeconds() + ", wait_for_parent_end=" + m.waitForParentEnd() + ", loop=" + m.loop());
        sender.sendMessage(ChatColor.GRAY + "  transformations: rotation=" + m.rotationDegrees() + ", mirror=" + m.mirror() + ", player_scale=" + m.playerScale() + ", scene_scale=" + m.sceneScale());
        sender.sendMessage(ChatColor.GRAY + "  offset: " + m.offsetX() + ", " + m.offsetY() + ", " + m.offsetZ());
        sender.sendMessage(ChatColor.GRAY + "  config: round_block_pos=" + c.roundBlockPos() + ", recording_center=" + c.recordingCenter() + ", scene_center=" + c.sceneCenterType() + (c.sceneCenterSpecific() == null ? "" : " [" + c.sceneCenterSpecific() + "]") + ", center_offset=" + c.centerOffsetX() + ", " + c.centerOffsetY() + ", " + c.centerOffsetZ());
    }

    private void listPlayback(CommandSender sender) { List<PlaybackSession> sessions = new ArrayList<>(playbackManager.getActive()); sender.sendMessage(ChatColor.GOLD + "Active playbacks: " + sessions.size()); for (PlaybackSession session : sessions) sender.sendMessage(ChatColor.GRAY + "  " + session.getId() + " | " + session.getRecording().getSourcePlayerName() + " | tick " + session.getTick()); }

    private void handleRecording(CommandSender sender, String[] args) { if (args.length < 2) { sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap recording <start|stop|discard|save|list>"); return; } switch (args[1].toLowerCase(Locale.ROOT)) { case "start" -> startRecording(sender, args); case "stop" -> stopRecording(sender, args); case "discard" -> discardRecording(sender, args); case "save" -> saveRecording(sender, args); case "list" -> listRecording(sender, args); default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap recording <start|stop|discard|save|list>"); } }
    private void startRecording(CommandSender sender, String[] args) { List<Player> players = new ArrayList<>(); if (args.length == 2) { if (sender instanceof Player player) players.add(player); else { sender.sendMessage(ChatColor.RED + "Specify a player when executing this command from console."); return; } } else { try { for (Entity entity : Bukkit.selectEntities(sender, args[2])) if (entity instanceof Player player) players.add(player); } catch (IllegalArgumentException e) { sender.sendMessage(ChatColor.RED + "Invalid or unresolved player selector: " + args[2]); return; } if (players.isEmpty()) { sender.sendMessage(ChatColor.RED + "No players matched the recording target."); return; } } String instantSave = args.length >= 4 ? args[3] : null; int started = 0; for (Player player : players) { String saveName = players.size() > 1 && instantSave != null ? instantSave + "_" + player.getName() : instantSave; if (hasActiveRecording(player)) { sender.sendMessage(ChatColor.RED + "Player is already being recorded: " + player.getName()); continue; } RecordingSession session = recordingManager.startRecording(player, saveName); sender.sendMessage(ChatColor.GREEN + "Recording started: " + session.getId() + " (" + player.getName() + ")"); started++; } if (started == 0) sender.sendMessage(ChatColor.RED + "No recording was started."); }
    private boolean hasActiveRecording(Player player) { return recordingManager.getActive().stream().anyMatch(s -> s.getSourcePlayerId().equals(player.getUniqueId())); }
    private void stopRecording(CommandSender sender, String[] args) { RecordingSession session; if (args.length >= 3) { UUID id = parseId(sender, args[2]); if (id == null) return; session = recordingManager.stopRecording(id); } else if (sender instanceof Player player) session = recordingManager.stopRecordingForPlayer(player); else { sender.sendMessage(ChatColor.RED + "Specify a recording id when executing this from console."); return; } if (session == null) { sender.sendMessage(ChatColor.RED + "No active recording was found."); return; } sender.sendMessage(ChatColor.GREEN + "Recording stopped: " + session.getId() + " (" + session.getDurationTicks() + " ticks)"); if (session.getInstantSaveName() != null) sender.sendMessage(ChatColor.GREEN + "Saved as: " + session.getInstantSaveName()); }
    private void discardRecording(CommandSender sender, String[] args) { RecordingSession session; if (args.length >= 3) { UUID id = parseId(sender, args[2]); if (id == null) return; session = recordingManager.discard(id); } else if (sender instanceof Player player) { RecordingSession active = recordingManager.stopRecordingForPlayer(player); session = active == null ? null : recordingManager.discard(active.getId()); } else { sender.sendMessage(ChatColor.RED + "Specify a recording id when executing this from console."); return; } sender.sendMessage(session == null ? ChatColor.RED + "No recording was found." : ChatColor.GREEN + "Recording discarded: " + session.getId()); }
    private void saveRecording(CommandSender sender, String[] args) { if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap recording save <name> [id]"); return; } String name = args[2]; RecordingSession session = null; if (args.length >= 4) { UUID id = parseId(sender, args[3]); if (id == null) return; session = recordingManager.get(id); } else if (sender instanceof Player player) session = recordingManager.getCompleted().stream().filter(v -> v.getSourcePlayerId().equals(player.getUniqueId())).max(Comparator.comparing(RecordingSession::getStoppedAt, Comparator.nullsFirst(Comparator.naturalOrder()))).orElse(null); else if (!recordingManager.getCompleted().isEmpty()) session = recordingManager.getCompleted().stream().max(Comparator.comparing(RecordingSession::getStoppedAt, Comparator.nullsFirst(Comparator.naturalOrder()))).orElse(null); if (session == null) { sender.sendMessage(ChatColor.RED + "No completed recording is available to save."); return; } try { recordingManager.save(name, session); sender.sendMessage(ChatColor.GREEN + "Recording saved as: " + name); } catch (IOException e) { sender.sendMessage(ChatColor.RED + "Unable to save recording: " + e.getMessage()); } }
    private void listRecording(CommandSender sender, String[] args) { if (args.length >= 3) { UUID id = parseId(sender, args[2]); if (id == null) return; RecordingSession session = recordingManager.get(id); if (session == null) { sender.sendMessage(ChatColor.RED + "Recording not found: " + id); return; } sendRecordingInfo(sender, session); return; } sender.sendMessage(ChatColor.GOLD + "Active recordings: " + recordingManager.getActive().size()); for (RecordingSession session : recordingManager.getActive()) sendRecordingInfo(sender, session); sender.sendMessage(ChatColor.GOLD + "Completed recordings: " + recordingManager.getCompleted().size()); for (RecordingSession session : recordingManager.getCompleted()) sendRecordingInfo(sender, session); }
    private void handleRecordings(CommandSender sender, String[] args) { if (args.length < 2 || args[1].equalsIgnoreCase("list")) { sender.sendMessage(ChatColor.GOLD + "Saved recordings: " + recordingManager.getSavedNames().size()); for (String name : recordingManager.getSavedNames()) { RecordingSession s = recordingManager.getSaved(name); sender.sendMessage(ChatColor.GRAY + "  " + name + " | " + s.getDurationTicks() + " ticks | " + s.getSourcePlayerName()); } return; } try { switch (args[1].toLowerCase(Locale.ROOT)) { case "copy" -> { requireArgs(sender,args,4,"/mocap recordings copy <src_name> <dest_name>"); if(args.length<4)return; sender.sendMessage(recordingManager.copy(args[2],args[3])?ChatColor.GREEN+"Recording copied.":ChatColor.RED+"Unable to copy recording."); } case "rename" -> { requireArgs(sender,args,4,"/mocap recordings rename <old_name> <new_name>"); if(args.length<4)return; sender.sendMessage(recordingManager.rename(args[2],args[3])?ChatColor.GREEN+"Recording renamed.":ChatColor.RED+"Unable to rename recording."); } case "remove" -> { requireArgs(sender,args,3,"/mocap recordings remove <name>"); if(args.length<3)return; sender.sendMessage(recordingManager.removeSaved(args[2])?ChatColor.GREEN+"Recording removed.":ChatColor.RED+"Recording not found."); } case "info" -> { requireArgs(sender,args,3,"/mocap recordings info <name>"); if(args.length<3)return; RecordingSession s=recordingManager.getSaved(args[2]); if(s==null)sender.sendMessage(ChatColor.RED+"Recording not found."); else sendRecordingInfo(sender,s); } default -> sender.sendMessage(ChatColor.YELLOW+"Usage: /mocap recordings <copy|rename|remove|info|list> ..."); } } catch(Exception e){ sender.sendMessage(ChatColor.RED+e.getMessage()); } }
    private void sendRecordingInfo(CommandSender sender, RecordingSession s) { sender.sendMessage(ChatColor.GRAY + "  id=" + s.getId() + " player=" + s.getSourcePlayerName() + " ticks=" + s.getDurationTicks() + " frames=" + s.getFrames().size()); }
    private void sendHelp(CommandSender sender) { sender.sendMessage(ChatColor.GOLD + "/mocap recording|recordings|playback|info|help"); }
    private void requireArgs(CommandSender sender,String[] a,int n,String usage){ if(a.length<n)sender.sendMessage(ChatColor.YELLOW+"Usage: "+usage); }
    private UUID parseId(CommandSender sender,String value){ try{return UUID.fromString(value);}catch(IllegalArgumentException e){sender.sendMessage(ChatColor.RED+"Invalid UUID: "+value);return null;} }
    public void shutdown() {}
    @Override public List<String> onTabComplete(@NotNull CommandSender sender,@NotNull Command command,@NotNull String alias,@NotNull String[] args){ return List.of(); }
}
