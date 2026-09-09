package com.ultraop.mocap;

import com.ultraop.mocap.command.MoCapCommand;
import com.ultraop.mocap.command.ParityMoCapCommand;
import com.ultraop.mocap.command.RootMoCapCommand;
import com.ultraop.mocap.playback.PlaybackManager;
import com.ultraop.mocap.recording.RecordingActionListener;
import com.ultraop.mocap.recording.RecordingBlockListener;
import com.ultraop.mocap.recording.RecordingManager;
import com.ultraop.mocap.recording.RecordingRespawnListener;
import com.ultraop.mocap.scene.SceneManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;

public final class MoCapPaperPlugin extends JavaPlugin {
    private RecordingManager recordingManager; private PlaybackManager playbackManager; private SceneManager sceneManager;
    private MoCapCommand mocapCommand; private RootMoCapCommand rootCommand; private ParityMoCapCommand parityCommand;
    @Override public void onEnable(){
        saveDefaultConfig(); recordingManager=new RecordingManager(this); recordingManager.start();
        getServer().getPluginManager().registerEvents(new RecordingBlockListener(recordingManager),this);
        getServer().getPluginManager().registerEvents(new RecordingActionListener(recordingManager),this);
        getServer().getPluginManager().registerEvents(new RecordingRespawnListener(recordingManager),this);
        sceneManager=new SceneManager(this,recordingManager); try{sceneManager.start();}catch(IOException e){getLogger().severe("Unable to initialize scene directory: "+e.getMessage());getServer().getPluginManager().disablePlugin(this);return;}
        playbackManager=new PlaybackManager(this,recordingManager); playbackManager.setSceneManager(sceneManager); playbackManager.start();
        mocapCommand=new MoCapCommand(this,recordingManager,playbackManager); rootCommand=new RootMoCapCommand(mocapCommand,sceneManager,this); parityCommand=new ParityMoCapCommand(rootCommand,playbackManager);
        if(getCommand("mocap")==null){getLogger().severe("The /mocap command is not registered in plugin.yml.");getServer().getPluginManager().disablePlugin(this);return;}
        getCommand("mocap").setExecutor(parityCommand); getCommand("mocap").setTabCompleter(parityCommand); getLogger().info("MoCap Paper initialized for Minecraft 1.21.11.");
    }
    public PlaybackManager getPlaybackManager(){return playbackManager;} public SceneManager getSceneManager(){return sceneManager;}
    @Override public void onDisable(){if(mocapCommand!=null)mocapCommand.shutdown();if(playbackManager!=null)playbackManager.shutdown();if(recordingManager!=null)recordingManager.shutdown();}
}
