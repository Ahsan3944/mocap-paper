package com.ultraop.mocap;

import com.ultraop.mocap.command.MoCapCommand;
import com.ultraop.mocap.playback.PlaybackManager;
import com.ultraop.mocap.recording.RecordingManager;
import com.ultraop.mocap.scene.SceneManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class MoCapPaperPlugin extends JavaPlugin {
    private RecordingManager recordingManager;
    private PlaybackManager playbackManager;
    private SceneManager sceneManager;
    private MoCapCommand mocapCommand;

    @Override
    public void onEnable() {
        this.recordingManager = new RecordingManager(this);
        this.recordingManager.start();
        this.sceneManager = new SceneManager(this, recordingManager);
        try {
            this.sceneManager.start();
        } catch (IOException e) {
            getLogger().severe("Unable to initialize scene directory: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.playbackManager = new PlaybackManager(this, recordingManager);
        this.playbackManager.start();
        this.mocapCommand = new MoCapCommand(this, recordingManager, playbackManager);
        if (getCommand("mocap") == null) {
            getLogger().severe("The /mocap command is not registered in plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("mocap").setExecutor(mocapCommand);
        getCommand("mocap").setTabCompleter(mocapCommand);
        getLogger().info("MoCap Paper initialized for Minecraft 1.21.11.");
    }

    public PlaybackManager getPlaybackManager() { return playbackManager; }
    public SceneManager getSceneManager() { return sceneManager; }

    @Override
    public void onDisable() {
        if (mocapCommand != null) mocapCommand.shutdown();
        if (playbackManager != null) playbackManager.shutdown();
        if (recordingManager != null) recordingManager.shutdown();
    }
}
