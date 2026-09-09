package com.ultraop.mocap.recording;

import com.ultraop.mocap.playback.EntityFilter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;

/** In-memory recording produced by one recording session. */
public final class RecordingSession {
    private static final double ENTITY_TRACKING_DISTANCE = 128.0;
    private static final EntityFilter TRACK_ENTITIES = EntityFilter.DEFAULT_TRACK_ENTITIES;
    private final UUID id;
    private final UUID sourcePlayerId;
    private final String sourcePlayerName;
    private final Instant startedAt;
    private final List<PlayerStateFrame> frames;
    private final Map<UUID, List<EntityStateFrame>> entityFrames;
    private final List<BlockActionFrame> blockActions;
    private final Map<UUID, Integer> trackedLastSeenTick;
    private final Set<UUID> swingMainHand = new HashSet<>();
    private final Set<UUID> swingOffHand = new HashSet<>();
    private String instantSaveName;
    private long nextTick;
    private Instant stoppedAt;

    public RecordingSession(Player player) { this(UUID.randomUUID(), player.getUniqueId(), player.getName(), Instant.now(), null, new ArrayList<>(), new LinkedHashMap<>(), new ArrayList<>()); }
    private RecordingSession(UUID id, UUID sourcePlayerId, String sourcePlayerName, Instant startedAt, Instant stoppedAt,
                             List<PlayerStateFrame> frames, Map<UUID, List<EntityStateFrame>> entityFrames, List<BlockActionFrame> blockActions) {
        this.id=id; this.sourcePlayerId=sourcePlayerId; this.sourcePlayerName=sourcePlayerName; this.startedAt=startedAt; this.stoppedAt=stoppedAt;
        this.frames=new ArrayList<>(frames); this.entityFrames=new LinkedHashMap<>(); entityFrames.forEach((u,v)->this.entityFrames.put(u,new ArrayList<>(v)));
        this.blockActions=new ArrayList<>(blockActions); this.trackedLastSeenTick=new LinkedHashMap<>();
        this.entityFrames.forEach((u,v)->{if(!v.isEmpty())trackedLastSeenTick.put(u,Math.toIntExact(v.get(v.size()-1).tick()));});
        this.nextTick=frames.stream().mapToLong(PlayerStateFrame::tick).max().orElse(-1L)+1L;
    }
    static RecordingSession loaded(UUID id, UUID sourcePlayerId, String sourcePlayerName, Instant startedAt, Instant stoppedAt, List<PlayerStateFrame> frames){return new RecordingSession(id,sourcePlayerId,sourcePlayerName,startedAt,stoppedAt,frames,Map.of(),List.of());}
    static RecordingSession loaded(UUID id, UUID sourcePlayerId, String sourcePlayerName, Instant startedAt, Instant stoppedAt, List<PlayerStateFrame> frames, Map<UUID,List<EntityStateFrame>> entityFrames){return new RecordingSession(id,sourcePlayerId,sourcePlayerName,startedAt,stoppedAt,frames,entityFrames,List.of());}
    static RecordingSession loaded(UUID id, UUID sourcePlayerId, String sourcePlayerName, Instant startedAt, Instant stoppedAt, List<PlayerStateFrame> frames, Map<UUID,List<EntityStateFrame>> entityFrames,List<BlockActionFrame> blockActions){return new RecordingSession(id,sourcePlayerId,sourcePlayerName,startedAt,stoppedAt,frames,entityFrames,blockActions);}
    public UUID getId(){return id;} public UUID getSourcePlayerId(){return sourcePlayerId;} public String getSourcePlayerName(){return sourcePlayerName;} public Instant getStartedAt(){return startedAt;} public Instant getStoppedAt(){return stoppedAt;}
    public long getDurationTicks(){return frames.size();} public List<PlayerStateFrame> getFrames(){return Collections.unmodifiableList(frames);}
    public Map<UUID,List<EntityStateFrame>> getEntityFrames(){Map<UUID,List<EntityStateFrame>> copy=new LinkedHashMap<>();entityFrames.forEach((u,v)->copy.put(u,Collections.unmodifiableList(v)));return Collections.unmodifiableMap(copy);}
    public List<BlockActionFrame> getBlockActions(){return Collections.unmodifiableList(blockActions);} public String getInstantSaveName(){return instantSaveName;} public void setInstantSaveName(String n){instantSaveName=n;}
    void markSwingMainHand(Player player){if(activeFor(player))swingMainHand.add(player.getUniqueId());}
    void markSwingOffHand(Player player){if(activeFor(player))swingOffHand.add(player.getUniqueId());}
    private boolean activeFor(Player p){return sourcePlayerId.equals(p.getUniqueId());}
    void capture(Player player){UUID u=player.getUniqueId();boolean main=swingMainHand.remove(u),off=swingOffHand.remove(u);frames.add(PlayerStateFrame.capture(player,nextTick,main,off));trackEntities(player);nextTick++;}
    public void recordBlockAction(BlockActionFrame action){if(action!=null)blockActions.add(action);} public long currentTick(){return Math.max(0L,nextTick-1L);}
    private void trackEntities(Player player){double maxDistanceSquared=ENTITY_TRACKING_DISTANCE*ENTITY_TRACKING_DISTANCE;Set<UUID> seenThisTick=new HashSet<>();for(Entity entity:player.getWorld().getEntities()){
        if(entity instanceof Player||!entity.isValid()||entity.getUniqueId().equals(sourcePlayerId))continue;if(player.getLocation().distanceSquared(entity.getLocation())>maxDistanceSquared)continue;if(isPlaybackEntity(entity))continue;if(!TRACK_ENTITIES.matches(entity))continue;
        UUID id=entity.getUniqueId();seenThisTick.add(id);entityFrames.computeIfAbsent(id,ignored->new ArrayList<>()).add(EntityStateFrame.capture(entity,nextTick));trackedLastSeenTick.put(id,Math.toIntExact(nextTick));}
        trackedLastSeenTick.keySet().removeIf(u->!seenThisTick.contains(u));}
    private static boolean isPlaybackEntity(Entity e){return e.getScoreboardTags().stream().anyMatch(t->t.equals("mocap_entity")||t.equals("mocap:entity"));}
    void stop(){trackedLastSeenTick.clear();swingMainHand.clear();swingOffHand.clear();if(stoppedAt==null)stoppedAt=Instant.now();}
}
