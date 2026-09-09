package com.ultraop.mocap.command;

import com.ultraop.mocap.playback.PlaybackManager;
import com.ultraop.mocap.recording.RecordingManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
import java.util.Locale;

/** Command surface for the upstream MoCap settings namespace. */
public final class SettingsCommand {
    private static final List<String> NAMES = List.of("playback_speed","recording_synchronization","block_actions_playback","block_initialization","track_entities","entity_tracking_distance","play_entities","prevent_tracking_played_entities","on_death","on_change_dimension","assign_dimension","dimension_source","start_instantly","assign_profile","chat_recording","chat_playback","invulnerable_playback","hit_range","use_authlib_services","player_name_handling");
    private final JavaPlugin plugin; private final RecordingManager recordingManager; private final PlaybackManager playbackManager;
    public SettingsCommand(JavaPlugin plugin,RecordingManager recordingManager,PlaybackManager playbackManager){this.plugin=plugin;this.recordingManager=recordingManager;this.playbackManager=playbackManager;}
    public boolean execute(CommandSender sender,String[] args){if(args.length==1||args[1].equalsIgnoreCase("list")){list(sender);return true;}if(args.length<3){usage(sender);return true;}String key=args[1].toLowerCase(Locale.ROOT),value=args[2];if(!NAMES.contains(key)){sender.sendMessage(ChatColor.RED+"Unknown setting: "+args[1]);return true;}if(!valid(key,value)){sender.sendMessage(ChatColor.RED+"Invalid value for "+key+".");return true;}plugin.getConfig().set("settings."+key,parse(key,value));plugin.saveConfig();recordingManager.reloadSettings();playbackManager.reloadSettings();sender.sendMessage(ChatColor.GREEN+"Setting updated: "+key+" = "+plugin.getConfig().get("settings."+key));return true;}
    private void list(CommandSender sender){sender.sendMessage(ChatColor.GOLD+"MoCap settings:");for(String key:NAMES)sender.sendMessage(ChatColor.GRAY+"  "+key+" = "+plugin.getConfig().get("settings."+key,defaultValue(key)));}
    private void usage(CommandSender sender){sender.sendMessage(ChatColor.YELLOW+"/mocap settings <list|setting value>");}
    private static boolean valid(String key,String value){return switch(key){case "playback_speed","hit_range","entity_tracking_distance"->number(value);case "block_actions_playback","block_initialization","recording_synchronization","prevent_tracking_played_entities","assign_dimension","start_instantly","chat_recording","chat_playback","invulnerable_playback","use_authlib_services"->bool(value);case "on_death"->List.of("end_recording","split_recording","continue_synced").contains(value.toLowerCase(Locale.ROOT));case "on_change_dimension"->List.of("nothing","end_recording","split_recording").contains(value.toLowerCase(Locale.ROOT));case "dimension_source"->List.of("assigned","current","assigned_or_current").contains(value.toLowerCase(Locale.ROOT));default->!value.isBlank();};}
    private static boolean bool(String v){return v.equalsIgnoreCase("true")||v.equalsIgnoreCase("false");} private static boolean number(String v){try{double n=Double.parseDouble(v);return Double.isFinite(n)&&n>=0;}catch(NumberFormatException e){return false;}}
    private static Object parse(String key,String value){if(key.equals("playback_speed")||key.equals("hit_range")||key.equals("entity_tracking_distance"))return Double.parseDouble(value);if(boolSetting(key))return Boolean.parseBoolean(value);return value;}
    private static boolean boolSetting(String k){return List.of("block_actions_playback","block_initialization","recording_synchronization","prevent_tracking_played_entities","assign_dimension","start_instantly","chat_recording","chat_playback","invulnerable_playback","use_authlib_services").contains(k);}
    private static Object defaultValue(String key){return switch(key){case "playback_speed"->1.0;case "hit_range","entity_tracking_distance"->key.equals("entity_tracking_distance")?128.0:0.0;case "recording_synchronization","block_actions_playback","block_initialization","prevent_tracking_played_entities","assign_dimension","chat_playback","invulnerable_playback","assign_dimension"->true;case "start_instantly","chat_recording","use_authlib_services"->false;case "track_entities"->"@vehicles;@projectiles;@items";case "play_entities"->"*";case "on_death","on_change_dimension"->"end_recording";case "dimension_source"->"assigned_or_current";case "assign_profile","player_name_handling"->"ignore_and_replace_casing";default->"";};}
    public static List<String> names(){return NAMES;}
}
