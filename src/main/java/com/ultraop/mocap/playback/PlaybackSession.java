package com.ultraop.mocap.playback;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.ultraop.mocap.recording.BlockActionFrame;
import com.ultraop.mocap.recording.EntityStateFrame;
import com.ultraop.mocap.recording.PlayerStateFrame;
import com.ultraop.mocap.recording.RecordingSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tick-driven playback timeline backed by server-side playback actors. */
public final class PlaybackSession {
    private final UUID id = UUID.randomUUID();
    private final RecordingSession recording;
    private final UUID viewerPlayerId;
    private final List<PlayerStateFrame> frames;
    private final Map<UUID,List<EntityStateFrame>> entityFrames;
    private final List<BlockActionFrame> blockActions;
    private final Map<UUID,EntityPlaybackActor> entityActors = new LinkedHashMap<>();
    private final FakePlayer fakePlayer;
    private final EntityPlaybackActor entityActor;
    private final PlaybackModifiers modifiers;
    private final PositionTransformer transformer;
    private final boolean root;
    private final boolean blockActionsPlayback;
    private final boolean blockInitialization;
    private long tick, waitTicks, waitOnEnd;
    private boolean paused, finished, stopped;

    public PlaybackSession(RecordingSession r, Player v){this(r,v,PlaybackModifiers.DEFAULT,null,true,true,true);}
    public PlaybackSession(RecordingSession r, Player v, PlaybackModifiers m){this(r,v,m,null,true,true,true);}
    public PlaybackSession(RecordingSession r, Player v, PlaybackModifiers m, PositionTransformer p){this(r,v,m,p,false,true,true);}
    public PlaybackSession(RecordingSession r, Player v, PlaybackModifiers m, PositionTransformer p, boolean root){this(r,v,m,p,root,true,true);}
    public PlaybackSession(RecordingSession r, Player v, PlaybackModifiers m, PositionTransformer p, boolean root, boolean blockActionsPlayback, boolean blockInitialization){
        recording=r; viewerPlayerId=v.getUniqueId(); frames=r.getFrames(); entityFrames=r.getEntityFrames(); blockActions=r.getBlockActions(); modifiers=m==null?PlaybackModifiers.DEFAULT:m; this.root=root; this.blockActionsPlayback=blockActionsPlayback; this.blockInitialization=blockInitialization;
        transformer=new PositionTransformer(modifiers,p,calculateRecordingCenter());
        if(frames.isEmpty()){fakePlayer=null;entityActor=null;stopped=true;return;}
        PlayerStateFrame f=frames.get(0); World w=findWorld(f.worldKey(),v.getWorld()); Location spawn=transform(new Location(w,f.x(),f.y(),f.z(),f.yaw(),f.pitch()));
        if(modifiers.playerAsEntity().enabled()&&modifiers.playerAsEntity().entityType()!=EntityType.PLAYER){Entity e=w.spawnEntity(spawn,modifiers.playerAsEntity().entityType());fakePlayer=null;entityActor=new EntityPlaybackActor(e,modifiers.playerScale());}
        else{fakePlayer=FakePlayer.spawn(spawn,resolveProfile(v),modifiers.playerScale());entityActor=null;}
        waitTicks=secondsToTicks(modifiers.startDelaySeconds()+modifiers.waitOnStartSeconds());
        if(waitTicks==0){initializeBlocks();applyFrame(f);applyEntityFrames();applyRidingRelationships(f);applyBlockActions(0);}
    }
    public UUID getId(){return id;} public RecordingSession getRecording(){return recording;} public long getTick(){return tick;} public boolean isPaused(){return paused;} public boolean isStopped(){return stopped;} public boolean isFinished(){return finished;}
    public boolean isActive(){return !stopped&&(!finished||modifiers.loop()||!modifiers.waitForParentEnd());} public UUID getViewerPlayerId(){return viewerPlayerId;} public FakePlayer getFakePlayer(){return fakePlayer;} public EntityPlaybackActor getEntityActor(){return entityActor;} public PlaybackModifiers getModifiers(){return modifiers;}
    public PlayerStateFrame currentFrame(){return frames.isEmpty()||tick>=frames.size()?null:frames.get((int)tick);} public void pause(){if(!stopped)paused=true;} public void resume(){if(!stopped)paused=false;}
    public void stop(){if(stopped)return;stopped=true;finished=true;ejectPlayer();if(fakePlayer!=null)fakePlayer.remove();if(entityActor!=null)entityActor.remove();removeRecordedEntities();}
    public void advance(){if(paused||stopped)return;if(waitTicks>0){waitTicks--;return;}if(finished){if(modifiers.loop())restartLoop();else if(shouldSelfStop())stop();return;}if(waitOnEnd>0){waitOnEnd--;if(waitOnEnd==0){finished=true;if(modifiers.loop())restartLoop();else if(shouldSelfStop())stop();}return;}if(tick>=frames.size()){finishOrWaitOnEnd();return;}PlayerStateFrame f=frames.get((int)tick);applyFrame(f);applyEntityFrames();applyRidingRelationships(f);applyBlockActions(tick);tick++;}
    private void finishOrWaitOnEnd(){long n=secondsToTicks(modifiers.waitOnEndSeconds());if(n==0){finished=true;if(modifiers.loop())restartLoop();else if(shouldSelfStop())stop();}else waitOnEnd=n;}
    private void restartLoop(){ejectPlayer();removeRecordedEntities();initializeBlocks();tick=0;waitTicks=0;waitOnEnd=0;finished=false;if(!frames.isEmpty()){applyFrame(frames.get(0));applyEntityFrames();applyRidingRelationships(frames.get(0));applyBlockActions(0);}}
    private boolean shouldSelfStop(){return root||!modifiers.waitForParentEnd();}
    private void applyFrame(PlayerStateFrame f){World w=findWorld(f.worldKey(),currentWorld());Location l=transform(new Location(w,f.x(),f.y(),f.z(),f.yaw(),f.pitch()));if(entityActor!=null)entityActor.apply(f,l);else{fakePlayer.apply(f);fakePlayer.getBukkitEntity().teleport(l);}}
    private void applyEntityFrames(){for(Map.Entry<UUID,List<EntityStateFrame>> e:entityFrames.entrySet()){List<EntityStateFrame> t=e.getValue();EntityStateFrame f=frameAtTick(t,tick);EntityPlaybackActor a=entityActors.get(e.getKey());if(f==null){if(a!=null&&lastTick(t)<tick){eject(a.entity());a.remove();entityActors.remove(e.getKey());}continue;}if(a==null){EntityType type=EntityType.fromName(f.entityType());if(type==null||type==EntityType.PLAYER||!modifiers.entityFilter().matches(type))continue;World w=findWorld(f.worldKey(),currentWorld());Location l=transform(new Location(w,f.x(),f.y(),f.z(),f.yaw(),f.pitch()));try{a=new EntityPlaybackActor(w.spawnEntity(l,type),modifiers.sceneScale());entityActors.put(e.getKey(),a);}catch(IllegalArgumentException ignored){continue;}}World w=findWorld(f.worldKey(),a.entity().getWorld());a.apply(f,transform(new Location(w,f.x(),f.y(),f.z(),f.yaw(),f.pitch())));}}
    private void applyRidingRelationships(PlayerStateFrame pf){Entity p=playbackPlayerEntity();if(p==null)return;UUID id=pf.vehicleId();if(id==null)eject(p);else{EntityPlaybackActor v=entityActors.get(id);if(v!=null&&v.entity().isValid())v.entity().addPassenger(p);else eject(p);}for(Map.Entry<UUID,List<EntityStateFrame>> e:entityFrames.entrySet()){EntityPlaybackActor a=entityActors.get(e.getKey());if(a==null||!a.entity().isValid())continue;EntityStateFrame f=frameAtTick(e.getValue(),tick);if(f==null)continue;UUID vid=f.vehicleId();if(vid==null)eject(a.entity());else{EntityPlaybackActor v=entityActors.get(vid);if(v!=null&&v.entity().isValid()&&v.entity()!=a.entity())v.entity().addPassenger(a.entity());else eject(a.entity());}}}
    private void initializeBlocks(){if(!blockInitialization)return;for(BlockActionFrame a:blockActions){Location l=transformedBlockLocation(a);if(l==null)continue;try{l.getBlock().setBlockData(Bukkit.createBlockData(a.beforeState()),false);}catch(IllegalArgumentException ignored){}}}
    private void applyBlockActions(long target){if(!blockActionsPlayback)return;for(BlockActionFrame a:blockActions){if(a.tick()!=target||a.action()==BlockActionFrame.Action.INTERACT)continue;Location l=transformedBlockLocation(a);if(l==null)continue;try{BlockData d=Bukkit.createBlockData(a.afterState());l.getBlock().setBlockData(d,false);}catch(IllegalArgumentException ignored){}}}
    private Location transformedBlockLocation(BlockActionFrame a){World w=findWorld(a.worldKey(),currentWorld());Vector p=transformer.transformBlockPosition(new Vector(a.x(),a.y(),a.z()));return new Location(w,Math.floor(p.getX()),Math.floor(p.getY()),Math.floor(p.getZ()));}
    private Entity playbackPlayerEntity(){if(entityActor!=null)return entityActor.entity();return fakePlayer==null?null:fakePlayer.getBukkitEntity();} private static void eject(Entity p){Entity v=p.getVehicle();if(v!=null)v.removePassenger(p);} private void ejectPlayer(){Entity p=playbackPlayerEntity();if(p!=null)eject(p);}
    private static long lastTick(List<EntityStateFrame> t){return t.isEmpty()?Long.MIN_VALUE:t.get(t.size()-1).tick();}
    private static EntityStateFrame frameAtTick(List<EntityStateFrame> t,long target){int lo=0,hi=t.size()-1;while(lo<=hi){int m=(lo+hi)>>>1;long v=t.get(m).tick();if(v<target)lo=m+1;else if(v>target)hi=m-1;else return t.get(m);}return null;}
    private void removeRecordedEntities(){for(EntityPlaybackActor a:entityActors.values()){eject(a.entity());a.remove();}entityActors.clear();}
    private World currentWorld(){if(fakePlayer!=null)return fakePlayer.getBukkitEntity().getWorld();if(entityActor!=null)return entityActor.entity().getWorld();return Bukkit.getWorlds().get(0);}
    public Location resolveLocation(Player fallback){PlayerStateFrame f=currentFrame();if(f==null)return fallback.getLocation().clone();World w=findWorld(f.worldKey(),fallback.getWorld());return transform(new Location(w,f.x(),f.y(),f.z(),f.yaw(),f.pitch()));}
    private Location transform(Location l){return transformer.transform(l);}
    private GameProfile resolveProfile(Player viewer){PlayerSkin s=modifiers.playerSkin();if(s.source()==PlayerSkin.Source.FROM_PLAYER){Player p=Bukkit.getPlayerExact(s.path());if(p!=null)return((CraftPlayer)p).getProfile();}if(s.source()==PlayerSkin.Source.FROM_MINESKIN){Property p=MineSkinSkins.getProperty(s.path());if(p!=null){GameProfile g=new GameProfile(UUID.randomUUID(),modifiers.playerName()==null?"MoCap":modifiers.playerName());g.properties().put("textures",p);return g;}}if(s.source()==PlayerSkin.Source.DEFAULT&&modifiers.playerName()!=null){Player p=Bukkit.getPlayerExact(modifiers.playerName());if(p!=null)return((CraftPlayer)p).getProfile();}return((CraftPlayer)viewer).getProfile();}
    private Vector calculateRecordingCenter(){if(frames.isEmpty())return new Vector(0,0,0);PlayerStateFrame s=frames.get(0);Vector p=new Vector(s.x(),s.y(),s.z());PlaybackModifiers.TransformationConfig c=modifiers.transformationConfig();Vector center=switch(c.recordingCenter()){case ACTUAL->p.clone();case BLOCK_CENTER->blockCenter(p);case BLOCK_CORNER->blockCorner(p);case AUTO->autoCenter(p);};return center.add(new Vector(c.centerOffsetX(),c.centerOffsetY(),c.centerOffsetZ()));}
    private Vector autoCenter(Vector p){double s=modifiers.sceneScale();if(s==1.0||s!=Math.rint(s)){Vector c=blockCenter(p),q=blockCorner(p);return p.distanceSquared(c)>p.distanceSquared(q)?q:c;}return((int)s%2==1)?blockCenter(p):blockCorner(p);}
    private static Vector blockCenter(Vector p){return new Vector(Math.round(p.getX()-0.5)+0.5,Math.floor(p.getY()),Math.round(p.getZ()-0.5)+0.5);} private static Vector blockCorner(Vector p){return new Vector(Math.round(p.getX()),Math.floor(p.getY()),Math.round(p.getZ()));}
    private static long secondsToTicks(double s){return!Double.isFinite(s)||s<=0?0:Math.min(Integer.MAX_VALUE,(long)Math.ceil(s*20));}
    private static World findWorld(String key,World fallback){for(World w:Bukkit.getWorlds())if(w.getKey().toString().equals(key))return w;return fallback;}
}
