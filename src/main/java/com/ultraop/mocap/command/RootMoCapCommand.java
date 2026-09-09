package com.ultraop.mocap.command;

import com.ultraop.mocap.scene.SceneManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Routes the root /mocap command while keeping the existing command implementation intact. */
public final class RootMoCapCommand implements CommandExecutor, TabCompleter {
    private final MoCapCommand base;
    private final SceneCommand scenes;

    public RootMoCapCommand(MoCapCommand base, SceneManager sceneManager) {
        this.base = base;
        this.scenes = new SceneCommand(sceneManager);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("scenes")) {
            scenes.execute(sender, args);
            return true;
        }
        return base.onCommand(sender, command, label, args);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && "scenes".startsWith(args[0].toLowerCase(Locale.ROOT))) return List.of("scenes");
        if (args.length >= 1 && args[0].equalsIgnoreCase("scenes")) return sceneTab(args);
        return base.onTabComplete(sender, command, alias, args);
    }

    private List<String> sceneTab(String[] args) {
        List<String> values = new ArrayList<>();
        if (args.length == 2) values.addAll(List.of("add", "copy", "rename", "remove", "add_to", "remove_from", "modify", "info", "list"));
        else if (args.length == 3 && List.of("copy", "rename", "remove", "add_to", "remove_from", "modify", "info", "list").contains(args[1].toLowerCase(Locale.ROOT))) {
            values.add("<scene_name>");
        } else if (args.length == 4 && List.of("copy", "rename", "add_to", "remove_from", "modify").contains(args[1].toLowerCase(Locale.ROOT))) {
            values.add(args[1].equalsIgnoreCase("add_to") ? "<recording_or_scene>" : args[1].equalsIgnoreCase("modify") ? "<element_pos>" : "<name>");
        }
        String input = args[args.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.startsWith(input) || v.startsWith("<")).toList();
    }
}
