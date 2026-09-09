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
    private final MoCapCommand base; private final SceneCommand scenes; private final SettingsCommand settings;
    public RootMoCapCommand(MoCapCommand base,SceneManager sceneManager,JavaPlugin plugin,RecordingManager recordingManager,PlaybackManager playbackManager){this.base=base;this.scenes=new SceneCommand(sceneManager);this.settings=new SettingsCommand(plugin,recordingManager,playbackManager);}
    @Override public boolean onCommand(@NotNull CommandSender sender,@NotNull Command command,@NotNull String label,@NotNull String[] args){if(args.length>0&&args[0].equalsIgnoreCase("scenes")){scenes.execute(sender,args);return true;}if(args.length>0&&args[0].equalsIgnoreCase("settings")){settings.execute(sender,args);return true;}return base.onCommand(sender,command,label,args);}
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,@NotNull Command command,@NotNull String alias,@NotNull String[] args){if(args.length==1)return filter(List.of("recording","playback","recordings","scenes","settings","misc","info","help"),args[0]);if(args[0].equalsIgnoreCase("scenes"))return sceneTab(args);if(args[0].equalsIgnoreCase("settings")){if(args.length==2)return filter(SettingsCommand.names(),args[1]);return List.of();}return base.onTabComplete(sender,command,alias,args);}
    private List<String> sceneTab(String[] args){List<String> v=new ArrayList<>();if(args.length==2)v.addAll(List.of("add","copy","rename","remove","add_to","remove_from","modify","info","list"));else if(args.length==3&&List.of("copy","rename","remove","add_to","remove_from","modify","info","list").contains(args[1].toLowerCase(Locale.ROOT)))v.add("<scene_name>");else if(args.length==4&&List.of("copy","rename","add_to","remove_from","modify").contains(args[1].toLowerCase(Locale.ROOT)))v.add(args[1].equalsIgnoreCase("add_to")?"<recording_or_scene>":args[1].equalsIgnoreCase("modify")?"<element_pos>":"<name>");return filter(v,args[args.length-1]);}
    private List<String> filter(List<String> v,String input){String l=input.toLowerCase(Locale.ROOT);return v.stream().filter(x->x.startsWith(l)||x.startsWith("<")).toList();}
}
