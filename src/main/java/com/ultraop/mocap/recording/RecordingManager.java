package com.ultraop.mocap.recording;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Owns active recordings, completed recordings and saved recording files. */
public final class RecordingManager {
    private final JavaPlugin plugin; private final RecordingRepository repository;
    private final Map<UUID,RecordingSession> active=new LinkedHashMap<>(), completed=new LinkedHashMap<>();
    private final Map<String,RecordingSession> saved=new LinkedHashMap<>(); private BukkitTask ticker;
    public RecordingManager(JavaPlugin plugin){this.plugin=plugin;this.repository=new RecordingRepository(Path.of(plugin.getDataFolder().getPath(),"recordings"));}
    public void start(){if(ticker!=null)return;try{repository.initialize();saved.clear();saved.putAll(repository.loadAll());}catch(IOException e){plugin.getLogger().severe("Unable to load saved recordings: "+e.getMessage());}ticker=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1L,1L);}
    public RecordingSession startRecording(Player p){RecordingSession s=new RecordingSession(p);active.put(s.getId(),s);return s;}
    public RecordingSession startRecording(Player p,String name){RecordingSession s=startRecording(p);s.setInstantSaveName(name);return s;}
    public void markSwing(Player p,boolean offHand){for(RecordingSession s:active.values())if(s.getSourcePlayerId().equals(p.getUniqueId())){if(offHand)s.markSwingOffHand(p);else s.markSwingMainHand(p);}}
    public RecordingSession stopRecording(UUID id){RecordingSession s=active.remove(id);if(s==null)return null;s.stop();completed.put(s.getId(),s);if(s.getInstantSaveName()!=null)try{save(s.getInstantSaveName(),s);}catch(IOException e){plugin.getLogger().severe("Unable to instantly save recording: "+e.getMessage());}return s;}
    public RecordingSession stopRecordingForPlayer(Player p){for(RecordingSession s:new ArrayList<>(active.values()))if(s.getSourcePlayerId().equals(p.getUniqueId()))return stopRecording(s.getId());return null;}
    public RecordingSession discard(UUID id){RecordingSession s=active.remove(id);return s!=null?s:completed.remove(id);} public RecordingSession get(UUID id){RecordingSession s=active.get(id);return s!=null?s:completed.get(id);}
    public Collection<RecordingSession> getActive(){return ListView.copyOf(active.values());} public Collection<RecordingSession> getCompleted(){return ListView.copyOf(completed.values());}
    public Collection<String> getSavedNames(){return ListView.copyOf(saved.keySet());} public RecordingSession getSaved(String n){return saved.get(n);}
    public boolean save(String n,RecordingSession s)throws IOException{repository.save(n,s);saved.put(n,s);return true;} public boolean save(String n,UUID id)throws IOException{RecordingSession s=get(id);return s!=null&&save(n,s);}
    public boolean rename(String old,String n)throws IOException{if(!saved.containsKey(old)||saved.containsKey(n)||repository.exists(n))return false;RecordingSession s=saved.remove(old);repository.save(n,s);repository.delete(old);saved.put(n,s);return true;}
    public boolean copy(String old,String n)throws IOException{if(saved.containsKey(n)||repository.exists(n))return false;RecordingSession s=saved.get(old);if(s==null)return false;repository.save(n,s);saved.put(n,s);return true;}
    public boolean removeSaved(String n)throws IOException{boolean r=repository.delete(n);saved.remove(n);return r;}
    public void shutdown(){if(ticker!=null){ticker.cancel();ticker=null;}active.clear();completed.clear();saved.clear();}
    private void tick(){for(RecordingSession s:new ArrayList<>(active.values())){Player p=Bukkit.getPlayer(s.getSourcePlayerId());if(p!=null&&p.isOnline())s.capture(p);}}
    private static final class ListView{static<T>Collection<T> copyOf(Collection<T> c){return Collections.unmodifiableList(new ArrayList<>(c));}}
}
