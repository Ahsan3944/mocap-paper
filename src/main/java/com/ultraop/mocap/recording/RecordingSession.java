package com.ultraop.mocap.recording;

import com.ultraop.mocap.playback.EntityFilter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.time.Instant;
import java.util.*;

/** In-memory recording produced by one recording session. */
public final class RecordingSession {
    public enum OnDeath { END_RECORDING, CONTINUE_SYNCED, SPLIT_RECORDING }
    private static final double ENTITY_TRACKING_DISTANCE=128.0;
    private static final EntityFilter TRACK_ENTITIES=EntityFilter.DEFAULT_TRACK_ENTITIES;
    private final UUID id,sourcePlayerId; private final String sourcePlayerName; private final Instant startedAt;
    private final List<PlayerStateFrame> frames; private final Map<UUID,List<EntityStateFrame>> entityFrames; private final List<BlockActionFrame> blockActions;
    private final Map<UUID,Integer> trackedLastSeenTick; private final Set<UUID> swingMainHand=new HashSet<>(),swingOffHand=new HashSet<>(),hurt=new HashSet<>();
    private String instantSaveName; private long nextTick; private Instant stoppedAt; private OnDeath onDeath=OnDeath.END_RECORDING; private boolean died; private long diedTick=-1;
    private OnChangeDimension onChangeDimension=OnChangeDimension.END_RECORDING; private String lastWorldKey;
    public RecordingSession(Player p){this(UUID.randomUUID(),p.getUniqueId(),p.getName(),Instant.now(),null,new ArrayList<>(),new LinkedHashMap<>(),new ArrayList<>());lastWorldKey=worldKey(p);}
    private RecordingSession(UUID id,UUID pid,String pn,Instant start,Instant stop,List<PlayerStateFrame> frames,Map<UUID,List<EntityStateFrame>> entities,List<BlockActionFrame> blocks){this.id=id;sourcePlayerId=pid;sourcePlayerName=pn;startedAt=start;stoppedAt=stop;this.frames=new ArrayList<>(frames);entityFrames=new LinkedHashMap<>();entities.forEach((u,v)->entityFrames.put(u,new ArrayList<>(v)));blockActions=new ArrayList<>(blocks);trackedLastSeenTick=new LinkedHashMap<>();entityFrames.forEach((u,v)->{if(!v.isEmpty())trackedLastSeenTick.put(u,Math.toIntExact(v.get(v.size()-1).tick()));});nextTick=frames.stream().mapToLong(PlayerStateFrame::tick).max().orElse(-1L)+1L;lastWorldKey=frames.isEmpty()?null:frames.get(frames.size()-1).worldKey();}
    static RecordingSession loaded(UUID id,UUID pid,String pn,Instant start,Instant stop,List<PlayerStateFrame> f){return new RecordingSession(id,pid,pn,start,stop,f,Map.of(),List.of());}
    static RecordingSession loaded(UUID id,UUID pid,String pn,Instant start,Instant stop,List<PlayerStateFrame> f,Map<UUID,List<EntityStateFrame>> e){return new RecordingSession(id,pid,pn,start,stop,f,e,List.of());}
    static RecordingSession loaded(UUID id,UUID pid,String pn,Instant start,Instant stop,List<PlayerStateFrame> f,Map<UUID,List<EntityStateFrame>> e,List<BlockActionFrame> b){return new RecordingSession(id,pid,pn,start,stop,f,e,b);}
    public UUID getId(){return id;} public UUID getSourcePlayerId(){return sourcePlayerId;} public String getSourcePlayerName(){return sourcePlayerName;} public Instant getStartedAt(){return startedAt;} public Instant getStoppedAt(){return stoppedAt;} public long getDurationTicks(){return frames.size();} public List<PlayerStateFrame> getFrames(){return Collections.unmodifiableList(frames);}
    public Map<UUID,List<EntityStateFrame>> getEntityFrames(){Map<UUID,List<EntityStateFrame>> c=new LinkedHashMap<>();entityFrames.forEach((u,v)->c.put(u,Collections.unmodifiableList(v)));return Collections.unmodifiableMap(c);} public List<BlockActionFrame> getBlockActions(){return Collections.unmodifiableList(blockActions);}
    public String getInstantSaveName(){return instantSaveName;} public void setInstantSaveName(String n){instantSaveName=n;} public OnDeath getOnDeath(){return onDeath;} public void setOnDeath(OnDeath v){onDeath=v==null?OnDeath.END_RECORDING:v;}
    public OnChangeDimension getOnChangeDimension(){return onChangeDimension;} public void setOnChangeDimension(OnChangeDimension v){onChangeDimension=v==null?OnChangeDimension.END_RECORDING:v;}
    void markSwingMainHand(Player p){if(activeFor(p))swingMainHand.add(p.getUniqueId());} void markSwingOffHand(Player p){if(activeFor(p))swingOffHand.add(p.getUniqueId());} void markHurt(Entity e){if(activeFor(e))hurt.add(e.getUniqueId());} private boolean activeFor(Entity e){return sourcePlayerId.equals(e.getUniqueId());}
    boolean capture(Player p){
        if(stoppedAt!=null)return false;
        String currentWorld=worldKey(p);
        if(lastWorldKey!=null&&!lastWorldKey.equals(currentWorld)){
            if(onChangeDimension==OnChangeDimension.END_RECORDING){stop();return false;}
            if(onChangeDimension==OnChangeDimension.SPLIT_RECORDING){stop();return true;}
        }
        if(died){captureDeadTick(p);return false;}
        UUID u=p.getUniqueId();boolean main=swingMainHand.remove(u),off=swingOffHand.remove(u),wasHurt=hurt.remove(u);frames.add(PlayerStateFrame.capture(p,nextTick,main,off,wasHurt));trackEntities(p);if(p.isDead()){died=true;diedTick=nextTick;}nextTick++;lastWorldKey=currentWorld;
        if(died&&onDeath==OnDeath.END_RECORDING)stop();
        return false;
    }
    private void captureDeadTick(Player p){long diff=nextTick-diedTick;if(onDeath==OnDeath.CONTINUE_SYNCED||diff<20){UUID u=p.getUniqueId();boolean main=swingMainHand.remove(u),off=swingOffHand.remove(u),wasHurt=hurt.remove(u);frames.add(PlayerStateFrame.capture(p,nextTick,main,off,wasHurt));trackEntities(p);nextTick++;}if(diff>=20&&onDeath==OnDeath.END_RECORDING)stop();}
    /** Called after Bukkit's respawn event; true means the upstream SPLIT_RECORDING behavior was requested. */
    boolean onRespawn(Player newPlayer){
        if(onDeath!=OnDeath.SPLIT_RECORDING){
            died=false; diedTick=-1; lastWorldKey=worldKey(newPlayer); return false;
        }
        stop(); return true;
    }
    public boolean isDead(){return died;}
    public void recordBlockAction(BlockActionFrame a){if(a!=null)blockActions.add(a);} public long currentTick(){return Math.max(0,nextTick-1);}
    private void trackEntities(Player p){double max=ENTITY_TRACKING_DISTANCE*ENTITY_TRACKING_DISTANCE;Set<UUID> seen=new HashSet<>();for(Entity e:p.getWorld().getEntities()){if(e instanceof Player||!e.isValid()||e.getUniqueId().equals(sourcePlayerId))continue;if(p.getLocation().distanceSquared(e.getLocation())>max||isPlaybackEntity(e)||!TRACK_ENTITIES.matches(e))continue;UUID id=e.getUniqueId();seen.add(id);boolean wasHurt=hurt.remove(id);entityFrames.computeIfAbsent(id,x->new ArrayList<>()).add(EntityStateFrame.capture(e,nextTick,wasHurt));trackedLastSeenTick.put(id,Math.toIntExact(nextTick));}trackedLastSeenTick.keySet().removeIf(u->!seen.contains(u));}
    private static boolean isPlaybackEntity(Entity e){return e.getScoreboardTags().stream().anyMatch(t->t.equals("mocap_entity")||t.equals("mocap:entity"));}
    private static String worldKey(Player p){return p.getWorld().getKey().toString();}
    void stop(){trackedLastSeenTick.clear();swingMainHand.clear();swingOffHand.clear();hurt.clear();if(stoppedAt==null)stoppedAt=Instant.now();}
}
