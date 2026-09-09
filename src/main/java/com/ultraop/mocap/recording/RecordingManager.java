package com.ultraop.mocap.recording;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Owns active recordings, completed recordings and saved recording files. */
public final class RecordingManager {
    private final JavaPlugin plugin; private final RecordingRepository repository;
    private final Map<UUID,RecordingSession> active=new LinkedHashMap<>(), completed=new LinkedHashMap<>(); private final Map<String,RecordingSession> saved=new LinkedHashMap<>(); private BukkitTask ticker;
    private RecordingSession.OnDeath onDeath=RecordingSession.OnDeath.END_RECORDING;
    private OnChangeDimension onChangeDimension=OnChangeDimension.END_RECORDING;
    private final Map<UUID,RecordingSession> waitingForRespawn=new HashMap<>();
    public RecordingManager(JavaPlugin plugin){this.plugin=plugin;this.repository=new RecordingRepository(Path.of(plugin.getDataFolder().getPath(),"recordings"));}
    public void start(){if(ticker!=null)return;loadSettings();try{repository.initialize();saved.clear();saved.putAll(repository.loadAll());}catch(IOException e){plugin.getLogger().severe("Unable to load saved recordings: "+e.getMessage());}ticker=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1L,1L);}
    private void loadSettings(){String death=plugin.getConfig().getString("recording.on_death",onDeath.name());String dimension=plugin.getConfig().getString("recording.on_change_dimension",onChangeDimension.name());try{onDeath=RecordingSession.OnDeath.valueOf(death.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ignored){onDeath=RecordingSession.OnDeath.END_RECORDING;}try{onChangeDimension=OnChangeDimension.valueOf(dimension.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ignored){onChangeDimension=OnChangeDimension.END_RECORDING;}}
    public RecordingSession.OnDeath getOnDeath(){return onDeath;} public void setOnDeath(RecordingSession.OnDeath value){onDeath=value==null?RecordingSession.OnDeath.END_RECORDING:value;plugin.getConfig().set("recording.on_death",onDeath.name().toLowerCase(Locale.ROOT));plugin.saveConfig();}
    public OnChangeDimension getOnChangeDimension(){return onChangeDimension;} public void setOnChangeDimension(OnChangeDimension value){onChangeDimension=value==null?OnChangeDimension.END_RECORDING:value;plugin.getConfig().set("recording.on_change_dimension",onChangeDimension.name().toLowerCase(Locale.ROOT));plugin.saveConfig();}
    public RecordingSession startRecording(Player p){RecordingSession s=new RecordingSession(p);s.setOnDeath(onDeath);s.setOnChangeDimension(onChangeDimension);active.put(s.getId(),s);return s;} public RecordingSession startRecording(Player p,String name){RecordingSession s=startRecording(p);s.setInstantSaveName(name);return s;}
    public void markSwing(Player p,boolean offHand){for(RecordingSession s:active.values())if(s.getSourcePlayerId().equals(p.getUniqueId())){if(offHand)s.markSwingOffHand(p);else s.markSwingMainHand(p);}}
    public void markHurt(Entity entity){for(RecordingSession s:active.values())s.markHurt(entity);}
    public RecordingSession stopRecording(UUID id){RecordingSession s=active.remove(id);if(s==null)return null;s.stop();waitingForRespawn.remove(s.getSourcePlayerId());complete(s);return s;}
    private void complete(RecordingSession s){completed.put(s.getId(),s);if(s.getInstantSaveName()!=null)try{save(s.getInstantSaveName(),s);}catch(IOException e){plugin.getLogger().severe("Unable to instantly save recording: "+e.getMessage());}}
    public RecordingSession stopRecordingForPlayer(Player p){for(RecordingSession s:new ArrayList<>(active.values()))if(s.getSourcePlayerId().equals(p.getUniqueId()))return stopRecording(s.getId());return null;} public RecordingSession discard(UUID id){RecordingSession s=active.remove(id);if(s!=null)waitingForRespawn.remove(s.getSourcePlayerId());return s!=null?s:completed.remove(id);} public RecordingSession get(UUID id){RecordingSession s=active.get(id);return s!=null?s:completed.get(id);}
    public Collection<RecordingSession> getActive(){return ListView.copyOf(active.values());} public Collection<RecordingSession> getCompleted(){return ListView.copyOf(completed.values());} public Collection<String> getSavedNames(){return ListView.copyOf(saved.keySet());} public RecordingSession getSaved(String n){return saved.get(n);}
    public boolean save(String n,RecordingSession s)throws IOException{repository.save(n,s);saved.put(n,s);return true;} public boolean save(String n,UUID id)throws IOException{RecordingSession s=get(id);return s!=null&&save(n,s);}
    public boolean rename(String old,String n)throws IOException{if(!saved.containsKey(old)||saved.containsKey(n)||repository.exists(n))return false;RecordingSession s=saved.remove(old);repository.save(n,s);repository.delete(old);saved.put(n,s);return true;} public boolean copy(String old,String n)throws IOException{if(saved.containsKey(n)||repository.exists(n))return false;RecordingSession s=saved.get(old);if(s==null)return false;repository.save(n,s);saved.put(n,s);return true;} public boolean removeSaved(String n)throws IOException{boolean r=repository.delete(n);saved.remove(n);return r;}
    public void shutdown(){if(ticker!=null){ticker.cancel();ticker=null;}waitingForRespawn.clear();active.clear();completed.clear();saved.clear();}
    public void handleRespawn(Player oldPlayer,Player newPlayer){RecordingSession s=waitingForRespawn.remove(oldPlayer.getUniqueId());if(s==null)return;if(!s.onRespawn(newPlayer))return;active.remove(s.getId());complete(s);RecordingSession next=startRecording(newPlayer);plugin.getLogger().fine("Recording split on respawn: "+s.getId()+" -> "+next.getId());}
    private void tick(){for(RecordingSession s:new ArrayList<>(active.values())){Player p=Bukkit.getPlayer(s.getSourcePlayerId());if(p==null||!p.isOnline())continue;if(s.isDead())waitingForRespawn.putIfAbsent(p.getUniqueId(),s);boolean split=s.capture(p);if(s.getStoppedAt()!=null){active.remove(s.getId());waitingForRespawn.remove(s.getSourcePlayerId());complete(s);continue;}if(split){active.remove(s.getId());waitingForRespawn.remove(s.getSourcePlayerId());complete(s);RecordingSession next=startRecording(p);plugin.getLogger().fine("Recording split on dimension change: "+s.getId()+" -> "+next.getId());}}}
    private static final class ListView{static<T>Collection<T> copyOf(Collection<T> c){return Collections.unmodifiableList(new ArrayList<>(c));}}
}
