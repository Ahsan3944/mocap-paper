package com.ultraop.mocap;

import com.ultraop.mocap.command.MoCapCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MoCapPaperPlugin extends JavaPlugin {

    private MoCapCommand mocapCommand;

    @Override
    public void onEnable() {
        this.mocapCommand = new MoCapCommand(this);

        if (getCommand("mocap") == null) {
            getLogger().severe("The /mocap command is not registered in plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("mocap").setExecutor(mocapCommand);
        getCommand("mocap").setTabCompleter(mocapCommand);

        getLogger().info("MoCap Paper initialized for Minecraft 1.21.11.");
    }

    @Override
    public void onDisable() {
        if (mocapCommand != null) {
            mocapCommand.shutdown();
        }
    }
}
