package com.ultraop.mocap.command;

import com.ultraop.mocap.playback.EntityFilter;
import com.ultraop.mocap.playback.PlaybackModifiers;
import com.ultraop.mocap.playback.PlayerAsEntity;
import com.ultraop.mocap.playback.PlayerSkin;
import com.ultraop.mocap.scene.SceneData;
import com.ultraop.mocap.scene.SceneElement;
import com.ultraop.mocap.scene.SceneManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Command layer for /mocap scenes following the upstream modifier model. */
public final class SceneCommand {
    private final SceneManager sceneManager;
    public SceneCommand(SceneManager sceneManager) { this.sceneManager = sceneManager; }

    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { usage(sender); return; }
        try { switch (args[1].toLowerCase(Locale.ROOT)) {
            case "add" -> add(sender,args); case "copy" -> copy(sender,args); case "rename" -> rename(sender,args); case "remove" -> remove(sender,args);
            case "add_to" -> addTo(sender,args); case "remove_from" -> removeFrom(sender,args); case "modify" -> modify(sender,args); case "info" -> info(sender,args); case "list" -> list(sender,args);
            default -> usage(sender);
        }} catch (IllegalArgumentException e) { sender.sendMessage(ChatColor.RED+e.getMessage()); } catch (IOException e) { sender.sendMessage(ChatColor.RED+"Scene file operation failed: "+e.getMessage()); }
    }
    private void add(CommandSender s,String[] a)throws IOException{require(a,3,"/mocap scenes add <name>");sceneManager.create(a[2]);s.sendMessage(ChatColor.GREEN+"Scene created: "+a[2]);}
    private void copy(CommandSender s,String[] a)throws IOException{require(a,4,"/mocap scenes copy <src_name> <dest_name>");s.sendMessage(sceneManager.copy(a[2],a[3])?ChatColor.GREEN+"Scene copied.":ChatColor.RED+"Unable to copy scene.");}
    private void rename(CommandSender s,String[] a)throws IOException{require(a,4,"/mocap scenes rename <old_name> <new_name>");s.sendMessage(sceneManager.rename(a[2],a[3])?ChatColor.GREEN+"Scene renamed.":ChatColor.RED+"Unable to rename scene.");}
    private void remove(CommandSender s,String[] a)throws IOException{require(a,3,"/mocap scenes remove <name>");s.sendMessage(sceneManager.remove(a[2])?ChatColor.GREEN+"Scene removed.":ChatColor.RED+"Scene not found.");}

    private void addTo(CommandSender s,String[] a)throws IOException{
        require(a,4,"/mocap scenes add_to <scene_name> <to_add> [wait_on_start] [modifiers]");
        String name=a[2], element=a[3]; if(element.contains("*")){addPattern(s,name,element);return;}
        SceneData scene=requireScene(name); PlaybackModifiers m=PlaybackModifiers.DEFAULT; int i=4;
        if(i<a.length&&isNumber(a[i]))m=m.withWaitOnStart(nonNegative(a[i++]));
        if(i<a.length)m=modifyModifier(m,a,i);
        scene.add(new SceneElement(element,m));sceneManager.save(name,scene);s.sendMessage(ChatColor.GREEN+"Scene element added: "+element);
    }
    private void addPattern(CommandSender s,String name,String pattern)throws IOException{
        String[] p=pattern.split("\\*",-1);if(p.length!=2)throw new IllegalArgumentException("Invalid scene element pattern.");
        List<String> candidates=pattern.startsWith(".")?sceneManager.getSceneNames():sceneManager.getRecordingNames();SceneData scene=requireScene(name);int matched=0;
        for(String c:candidates)if(c.startsWith(p[0])&&c.endsWith(p[1])){scene.add(new SceneElement(c,PlaybackModifiers.DEFAULT));matched++;}
        if(matched==0){s.sendMessage(ChatColor.RED+"No scene elements matched the pattern.");return;}sceneManager.save(name,scene);s.sendMessage(ChatColor.GREEN+"Added "+matched+" scene element(s).");
    }
    private void removeFrom(CommandSender s,String[] a)throws IOException{require(a,4,"/mocap scenes remove_from <scene_name> <element_pos>");SceneData scene=requireScene(a[2]);int p=parsePosition(a[3]);if(!scene.remove(p))throw new IllegalArgumentException("Scene element not found: "+a[3]);sceneManager.save(a[2],scene);s.sendMessage(ChatColor.GREEN+"Scene element removed.");}

    private void modify(CommandSender s,String[] a)throws IOException{
        require(a,5,"/mocap scenes modify <scene_name> <element_pos> <modifier>");SceneData scene=requireScene(a[2]);int p=parsePosition(a[3]);
        if(p>scene.elements().size())throw new IllegalArgumentException("Scene element not found: "+a[3]);
        SceneElement old=scene.elements().get(p-1);PlaybackModifiers m=modifyModifier(old.modifiers(),a,4);List<SceneElement> elements=new ArrayList<>(scene.elements());elements.set(p-1,new SceneElement(old.name(),m));scene.clear();elements.forEach(scene::add);sceneManager.save(a[2],scene);s.sendMessage(ChatColor.GREEN+"Scene element modified.");
    }

    private PlaybackModifiers modifyModifier(PlaybackModifiers m,String[] a,int i){
        if(i>=a.length)throw new IllegalArgumentException("Missing modifier.");String group=a[i].toLowerCase(Locale.ROOT);
        return switch(group){
            case "time"->{requireArgs(a,i+2,"time <start_delay|wait_on_start|wait_on_end|wait_for_parent_end|loop> <value>");String k=a[i+1].toLowerCase(Locale.ROOT);String v=a[i+2];yield switch(k){case "start_delay"->m.withStartDelay(nonNegative(v));case "wait_on_start"->m.withWaitOnStart(nonNegative(v));case "wait_on_end"->m.withWaitOnEnd(nonNegative(v));case "wait_for_parent_end"->m.withWaitForParentEnd(bool(v));case "loop"->m.withLoop(bool(v));default->throw new IllegalArgumentException("Unknown time modifier: "+k);};}
            case "transformations"->modifyTransform(m,a,i+1);
            case "player_name"->{requireArgs(a,i+2,"player_name <inherited|blank|set> [name]");String k=a[i+1].toLowerCase(Locale.ROOT);yield switch(k){case "inherited"->m.withPlayerName(null);case "blank"->m.withPlayerName("");case "set"->m.withPlayerName(requireAt(a,i+2,"player name"));default->throw new IllegalArgumentException("Unknown player name mode: "+k);};}
            case "player_skin"->modifySkin(m,a,i+1);
            case "player_as_entity"->{requireArgs(a,i+1,"player_as_entity <disabled|enabled> [entity] [nbt]");String k=a[i+1].toLowerCase(Locale.ROOT);if(k.equals("disabled"))yield m.withPlayerAsEntity(PlayerAsEntity.DISABLED);if(!k.equals("enabled"))throw new IllegalArgumentException("Mode must be disabled or enabled.");String id=requireAt(a,i+2,"entity");EntityType type=EntityType.fromName(id.startsWith("minecraft:")?id.substring(10):id);if(type==null)throw new IllegalArgumentException("Unknown entity type: "+id);yield m.withPlayerAsEntity(PlayerAsEntity.enabled(type,a.length>i+3?a[i+3]:null));}
            case "entity_filter"->{requireArgs(a,i+2,"entity_filter <disabled|enabled> [filter]");String k=a[i+1].toLowerCase(Locale.ROOT);if(k.equals("disabled"))yield m.withEntityFilter(EntityFilter.disabled());if(!k.equals("enabled"))throw new IllegalArgumentException("Mode must be disabled or enabled.");yield m.withEntityFilter(requireAt(a,i+2,"entity filter"));}
            default->throw new IllegalArgumentException("Unknown modifier group: "+group);
        };
    }
    private PlaybackModifiers modifyTransform(PlaybackModifiers m,String[] a,int i){
        requireArgs(a,i+1,"transformation");String k=a[i].toLowerCase(Locale.ROOT);return switch(k){
            case "rotation"->m.withRotation(Double.parseDouble(requireAt(a,i+1,"degrees")));
            case "mirror"->m.withMirror(PlaybackModifiers.Mirror.valueOf(requireAt(a,i+1,"mirror").toUpperCase(Locale.ROOT)));
            case "scale"->{requireArgs(a,i+2,"scale <of_player|of_scene> <scale>");double v=nonNegative(a[i+2]);yield a[i+1].equalsIgnoreCase("of_player")?m.withPlayerScale(v):a[i+1].equalsIgnoreCase("of_scene")?m.withSceneScale(v):throw new IllegalArgumentException("Scale target must be of_player or of_scene.");}
            case "offset"->{requireArgs(a,i+3,"offset <x> <y> <z>");yield m.withOffset(Double.parseDouble(a[i+1]),Double.parseDouble(a[i+2]),Double.parseDouble(a[i+3]));}
            case "config"->modifyConfig(m,a,i+1);
            default->throw new IllegalArgumentException("Unknown transformation modifier: "+k);
        };}
    private PlaybackModifiers modifyConfig(PlaybackModifiers m,String[] a,int i){
        requireArgs(a,i+1,"config <round_block_pos|recording_center|scene_center|center_offset> ...");var c=m.transformationConfig();String k=a[i].toLowerCase(Locale.ROOT);return switch(k){
            case "round_block_pos"->m.withTransformationConfig(c.withRoundBlockPos(bool(requireAt(a,i+1,"value"))));
            case "recording_center"->m.withTransformationConfig(c.withRecordingCenter(PlaybackModifiers.RecordingCenter.valueOf(requireAt(a,i+1,"center").toUpperCase(Locale.ROOT))));
            case "scene_center"->{String v=requireAt(a,i+1,"scene center");var t=PlaybackModifiers.SceneCenterType.valueOf(v.toUpperCase(Locale.ROOT));yield m.withTransformationConfig(c.withSceneCenter(t,t==PlaybackModifiers.SceneCenterType.COMMON_SPECIFIC?requireAt(a,i+2,"scene element"):null));}
            case "center_offset"->{requireArgs(a,i+3,"center_offset <x> <y> <z>");yield m.withTransformationConfig(c.withCenterOffset(Double.parseDouble(a[i+1]),Double.parseDouble(a[i+2]),Double.parseDouble(a[i+3])));}
            default->throw new IllegalArgumentException("Unknown transformation config: "+k);
        };}
    private PlaybackModifiers modifySkin(PlaybackModifiers m,String[] a,int i){requireArgs(a,i+1,"player_skin <default|from_player|from_file|from_mineskin> [value]");String k=a[i].toLowerCase(Locale.ROOT);return switch(k){case "default"->m.withPlayerSkin(PlayerSkin.DEFAULT);case "from_player"->m.withPlayerSkin(PlayerSkin.fromPlayer(requireAt(a,i+1,"player name")));case "from_file"->m.withPlayerSkin(PlayerSkin.fromFile(requireAt(a,i+1,"skin filename")));case "from_mineskin"->m.withPlayerSkin(PlayerSkin.fromMineSkin(requireAt(a,i+1,"MineSkin URL")));default->throw new IllegalArgumentException("Unknown player skin source: "+k);};}

    private void info(CommandSender s,String[] a)throws IOException{require(a,3,"/mocap scenes info <scene_name> [element_pos]");SceneData scene=requireScene(a[2]);s.sendMessage(ChatColor.GOLD+"Scene: "+a[2]);s.sendMessage(ChatColor.GRAY+"Elements: "+scene.elements().size());for(int i=0;i<scene.elements().size();i++){SceneElement e=scene.elements().get(i);s.sendMessage(ChatColor.GRAY+"  "+(i+1)+" | "+e.name());}if(a.length>=4){int p=parsePosition(a[3]);if(p>scene.elements().size())throw new IllegalArgumentException("Scene element not found: "+a[3]);s.sendMessage(ChatColor.GRAY+"Element "+p+": "+scene.elements().get(p-1).name());s.sendMessage(ChatColor.GRAY+"  modifiers: "+scene.elements().get(p-1).modifiers());}}
    private void list(CommandSender s,String[] a)throws IOException{if(a.length>=3){SceneData scene=requireScene(a[2]);s.sendMessage(ChatColor.GOLD+"Scene elements: "+a[2]);for(int i=0;i<scene.elements().size();i++)s.sendMessage(ChatColor.GRAY+"  "+(i+1)+" | "+scene.elements().get(i).name());return;}List<String> n=sceneManager.getSceneNames();s.sendMessage(ChatColor.GOLD+"Scenes: "+n.size());n.forEach(x->s.sendMessage(ChatColor.GRAY+"  "+x));}
    private SceneData requireScene(String n)throws IOException{SceneData d=sceneManager.load(n);if(d==null)throw new IllegalArgumentException("Scene not found: "+n);return d;}
    private static int parsePosition(String v){try{int p=Integer.parseInt(v);if(p<1)throw new NumberFormatException();return p;}catch(NumberFormatException e){throw new IllegalArgumentException("Scene element position must be a positive integer.");}}
    private static double nonNegative(String v){double x=Double.parseDouble(v);if(!Double.isFinite(x)||x<0)throw new IllegalArgumentException("Value must be a finite non-negative number.");return x;}
    private static boolean bool(String v){if(!v.equalsIgnoreCase("true")&&!v.equalsIgnoreCase("false"))throw new IllegalArgumentException("Value must be true or false.");return Boolean.parseBoolean(v);}
    private static boolean isNumber(String v){try{nonNegative(v);return true;}catch(RuntimeException e){return false;}}
    private static String requireAt(String[] a,int i,String label){if(i>=a.length||a[i].isBlank())throw new IllegalArgumentException("Missing "+label+".");return a[i];}
    private static void requireArgs(String[] a,int end,String usage){if(a.length<=end)throw new IllegalArgumentException("Usage: /mocap scenes modify ... "+usage);}
    private static void require(String[] a,int count,String usage){if(a.length<count)throw new IllegalArgumentException("Usage: "+usage);}
    private static void usage(CommandSender s){s.sendMessage(ChatColor.YELLOW+"Usage: /mocap scenes <add|copy|rename|remove|add_to|remove_from|modify|info|list> ...");}
}
