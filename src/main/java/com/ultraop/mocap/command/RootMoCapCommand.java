package com.ultraop.mocap.command;

import com.ultraop.mocap.playback.PlaybackManager;
import com.ultraop.mocap.recording.RecordingManager;
import com.ultraop.mocap.scene.SceneManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RootMoCapCommand implements CommandExecutor, TabCompleter {
    private final MoCapCommand base; private final SceneCommand scenes; private final SettingsCommand settings; private final MiscCommand misc; private final SkinModifierCommand skins; private final SkinSuggestionCache skinSuggestions;
    public RootMoCapCommand(MoCapCommand base, SceneManager sceneManager, JavaPlugin plugin, RecordingManager recordingManager, PlaybackManager playbackManager) { this.base = base; this.scenes = new SceneCommand(sceneManager); this.settings = new SettingsCommand(plugin, recordingManager, playbackManager); this.skinSuggestions = new SkinSuggestionCache(); this.skinSuggestions.refresh(); this.misc = new MiscCommand(skinSuggestions); this.skins = new SkinModifierCommand(playbackManager, skinSuggestions); }
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("scenes")) { scenes.execute(sender, args); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("settings")) { settings.execute(sender, args); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("misc")) { misc.execute(sender, args); return true; }
        if (args.length >= 5 && args[0].equalsIgnoreCase("playback") && args[1].equalsIgnoreCase("modifiers") && args[2].equalsIgnoreCase("set") && args[3].equalsIgnoreCase("player_skin")) return skins.execute(sender, args);
        return base.onCommand(sender, command, label, args);
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return filter(List.of("recording","playback","recordings","scenes","settings","misc","info","help"), args[0]);
        if (args[0].equalsIgnoreCase("scenes")) return sceneTab(args);
        if (args[0].equalsIgnoreCase("settings")) { if (args.length == 2) return filter(SettingsCommand.names(), args[1]); if(args.length==3)return settingValues(args[1],args[2]); return List.of(); }
        if (args[0].equalsIgnoreCase("misc")) { if (args.length == 2) return filter(List.of("sync","clear_cache","refresh_suggestions","extensions"), args[1]); if (args.length == 3 && args[1].equalsIgnoreCase("sync")) return filter(List.of("enable","disable"), args[2]); return List.of(); }
        if (args.length >= 5 && args[0].equalsIgnoreCase("playback") && args[1].equalsIgnoreCase("modifiers") && args[2].equalsIgnoreCase("set") && args[3].equalsIgnoreCase("player_skin")) {
            if (args.length == 5) return filter(List.of("from_player","from_file","from_mineskin","default"), args[4]);
            if (args.length == 6 && args[4].equalsIgnoreCase("from_file")) return filter(skinSuggestions.get(args[5]), args[5]);
            return List.of();
        }
        return base.onTabComplete(sender, command, alias, args);
    }
    private List<String> settingValues(String key,String input){return switch(key.toLowerCase(Locale.ROOT)){case "on_death"->filter(List.of("end_recording","split_recording","continue_synced","continue_skip_ticks"),input);case "on_change_dimension"->filter(List.of("nothing","end_recording","split_recording"),input);case "dimension_source"->filter(List.of("assigned_or_current","assigned_or_overworld","current","overworld"),input);case "assign_profile"->filter(List.of("no","only_name","full"),input);case "player_name_handling"->filter(List.of("disable_loading_profiles","match_exact_name","ignore_casing","ignore_and_replace_casing"),input);case "entities_after_playback"->filter(List.of("remove","kill","left_untouched","release_as_normal"),input);case "required_extensions"->filter(List.of("let_extension_decide","require_all","ignore_all"),input);case "nbt_recording_mode"->filter(List.of("disabled","filtered","full"),input);case "block_actions_playback","block_initialization","block_allow_scaled","prevent_tracking_played_entities","can_push_entities","assign_dimension","start_instantly","chat_recording","chat_playback","invulnerable_playback","use_authlib_services","start_as_recorded","drop_from_blocks","quick_discard","allow_ghosts","pretty_scene_files","show_tips","allow_mineskin_requests","prevent_saving_entities","use_creative_game_mode","experimental_release_warning"->filter(List.of("true","false"),input);default->List.of();};}
    private List<String> sceneTab(String[] args) { List<String> v = new ArrayList<>(); if (args.length == 2) v.addAll(List.of("add","copy","rename","remove","add_to","remove_from","modify","info","list")); else if (args.length == 3 && List.of("copy","rename","remove","add_to","remove_from","modify","info","list").contains(args[1].toLowerCase(Locale.ROOT))) v.add("<scene_name>"); else if (args.length == 4 && List.of("copy","rename","add_to","remove_from","modify").contains(args[1].toLowerCase(Locale.ROOT))) v.add(args[1].equalsIgnoreCase("add_to") ? "<recording_or_scene>" : args[1].equalsIgnoreCase("modify") ? "<element_pos>" : "<name>"); return filter(v, args[args.length - 1]); }
    private List<String> filter(List<String> v, String input) { String l = input.toLowerCase(Locale.ROOT); return v.stream().filter(x -> x.startsWith(l) || x.startsWith("<")).toList(); }
}
