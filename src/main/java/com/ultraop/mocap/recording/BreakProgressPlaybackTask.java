package com.ultraop.mocap.recording;

import com.ultraop.mocap.playback.PlaybackManager;
import com.ultraop.mocap.playback.PlaybackSession;
import com.ultraop.mocap.playback.PositionTransformer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import java.util.List;

/** Replays recorded block-destruction progress alongside playback timelines. */
public final class BreakProgressPlaybackTask {
    private final JavaPlugin plugin;private final PlaybackManager playback;private BukkitTask task;
    public BreakProgressPlaybackTask(JavaPlugin plugin,PlaybackManager playback){this.plugin=plugin;this.playback=playback;}
    public void start(){if(task!=null)return;task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1L,1L);}
    public void stop(){if(task!=null){task.cancel();task=null;}}
    private void tick(){for(PlaybackSession session:playback.getActive()){Player viewer=Bukkit.getPlayer(session.getViewerPlayerId());if(viewer==null)continue;List<BreakProgressFrame> frames=session.getRecording().getBreakProgress();if(frames.isEmpty())continue;long target=Math.max(0,session.getTick()-1);for(BreakProgressFrame frame:frames){if(frame.tick()!=target)continue;PositionTransformer transformer=new PositionTransformer(session.getModifiers(),null,recordingCenter(session));Location base=session.resolveLocation(viewer);boolean allowScaled=plugin.getConfig().getBoolean("settings.block_allow_scaled",false);for(Vector pos:transformer.transformBlockPositions(new Vector(frame.x(),frame.y(),frame.z()),allowScaled))BreakProgressPlayback.send(frame,new Location(base.getWorld(),pos.getX(),pos.getY(),pos.getZ()),session.getId().hashCode());}}
    }
    private static Vector recordingCenter(PlaybackSession session){if(session.getRecording().getFrames().isEmpty())return new Vector(0,0,0);var f=session.getRecording().getFrames().get(0);return new Vector(f.x(),f.y(),f.z());}
}