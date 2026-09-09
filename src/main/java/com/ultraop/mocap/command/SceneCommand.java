package com.ultraop.mocap.command;

import com.ultraop.mocap.playback.PlaybackModifiers;
import com.ultraop.mocap.scene.SceneData;
import com.ultraop.mocap.scene.SceneElement;
import com.ultraop.mocap.scene.SceneManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Command layer for /mocap scenes. The syntax follows the upstream MoCap command model. */
public final class SceneCommand {
    private final SceneManager sceneManager;

    public SceneCommand(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender);
            return;
        }
        try {
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "add" -> add(sender, args);
                case "copy" -> copy(sender, args);
                case "rename" -> rename(sender, args);
                case "remove" -> remove(sender, args);
                case "add_to" -> addTo(sender, args);
                case "remove_from" -> removeFrom(sender, args);
                case "modify" -> modify(sender, args);
                case "info" -> info(sender, args);
                case "list" -> list(sender, args);
                default -> usage(sender);
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Scene file operation failed: " + e.getMessage());
        }
    }

    private void add(CommandSender sender, String[] args) throws IOException {
        require(args, 3, "/mocap scenes add <name>");
        sceneManager.create(args[2]);
        sender.sendMessage(ChatColor.GREEN + "Scene created: " + args[2]);
    }

    private void copy(CommandSender sender, String[] args) throws IOException {
        require(args, 4, "/mocap scenes copy <src_name> <dest_name>");
        sender.sendMessage(sceneManager.copy(args[2], args[3])
                ? ChatColor.GREEN + "Scene copied."
                : ChatColor.RED + "Unable to copy scene.");
    }

    private void rename(CommandSender sender, String[] args) throws IOException {
        require(args, 4, "/mocap scenes rename <old_name> <new_name>");
        sender.sendMessage(sceneManager.rename(args[2], args[3])
                ? ChatColor.GREEN + "Scene renamed."
                : ChatColor.RED + "Unable to rename scene.");
    }

    private void remove(CommandSender sender, String[] args) throws IOException {
        require(args, 3, "/mocap scenes remove <name>");
        sender.sendMessage(sceneManager.remove(args[2])
                ? ChatColor.GREEN + "Scene removed."
                : ChatColor.RED + "Scene not found.");
    }

    private void addTo(CommandSender sender, String[] args) throws IOException {
        require(args, 4, "/mocap scenes add_to <scene_name> <to_add> [wait_on_start]");
        String sceneName = args[2];
        String elementName = args[3];
        if (elementName.contains("*")) {
            addPattern(sender, sceneName, elementName);
            return;
        }
        SceneData scene = requireScene(sceneName);
        double wait = args.length >= 5 ? nonNegative(args[4]) : 0.0;
        PlaybackModifiers modifiers = PlaybackModifiers.DEFAULT.withWaitOnStart(wait);
        scene.add(new SceneElement(elementName, modifiers));
        sceneManager.save(sceneName, scene);
        sender.sendMessage(ChatColor.GREEN + "Scene element added: " + elementName);
    }

    private void addPattern(CommandSender sender, String sceneName, String pattern) throws IOException {
        String[] parts = pattern.split("\\*", -1);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid scene element pattern.");
        List<String> candidates = pattern.startsWith(".")
                ? sceneManager.getSceneNames()
                : sceneManager.getRecordingNames();
        int matched = 0;
        SceneData scene = requireScene(sceneName);
        for (String candidate : candidates) {
            if (candidate.startsWith(parts[0]) && candidate.endsWith(parts[1])) {
                scene.add(new SceneElement(candidate, PlaybackModifiers.DEFAULT));
                matched++;
            }
        }
        if (matched == 0) {
            sender.sendMessage(ChatColor.RED + "No scene elements matched the pattern.");
            return;
        }
        sceneManager.save(sceneName, scene);
        sender.sendMessage(ChatColor.GREEN + "Added " + matched + " scene element(s).");
    }

    private void removeFrom(CommandSender sender, String[] args) throws IOException {
        require(args, 4, "/mocap scenes remove_from <scene_name> <element_pos>");
        SceneData scene = requireScene(args[2]);
        int position = parsePosition(args[3]);
        if (!scene.remove(position)) throw new IllegalArgumentException("Scene element not found: " + args[3]);
        sceneManager.save(args[2], scene);
        sender.sendMessage(ChatColor.GREEN + "Scene element removed.");
    }

    private void modify(CommandSender sender, String[] args) throws IOException {
        require(args, 5, "/mocap scenes modify <scene_name> <element_pos> <modifier>");
        SceneData scene = requireScene(args[2]);
        int position = parsePosition(args[3]);
        if (position < 1 || position > scene.elements().size()) throw new IllegalArgumentException("Scene element not found: " + args[3]);
        SceneElement element = scene.elements().get(position - 1);
        PlaybackModifiers modifiers = modifyModifier(element.modifiers(), args, 4);
        scene.remove(position);
        scene.add(new SceneElement(element.name(), modifiers));
        // Preserve ordering: add() above appends, so rebuild the scene in its original order.
        List<SceneElement> original = new java.util.ArrayList<>(scene.elements());
        scene.clear();
        for (int i = 0; i < original.size(); i++) {
            if (i == original.size() - 1) {
                // The modified element was appended; move it back to its original position below.
            }
        }
        original.remove(original.size() - 1);
        for (int i = 0; i < original.size() + 1; i++) {
            if (i == position - 1) scene.add(new SceneElement(element.name(), modifiers));
            else scene.add(original.get(i < position - 1 ? i : i - 1));
        }
        sceneManager.save(args[2], scene);
        sender.sendMessage(ChatColor.GREEN + "Scene element modified.");
    }

    private PlaybackModifiers modifyModifier(PlaybackModifiers m, String[] args, int index) {
        if (args.length <= index) throw new IllegalArgumentException("Missing modifier.");
        String[] parts = args[index].split("\\s+");
        if (parts.length == 0) throw new IllegalArgumentException("Missing modifier.");
        return switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "time" -> {
                if (parts.length < 3) throw new IllegalArgumentException("Usage: time <start_delay|wait_on_start|wait_on_end|wait_for_parent_end|loop> <value>");
                yield switch (parts[1].toLowerCase(Locale.ROOT)) {
                    case "start_delay" -> m.withStartDelay(nonNegative(parts[2]));
                    case "wait_on_start" -> m.withWaitOnStart(nonNegative(parts[2]));
                    case "wait_on_end" -> m.withWaitOnEnd(nonNegative(parts[2]));
                    case "wait_for_parent_end" -> m.withWaitForParentEnd(Boolean.parseBoolean(parts[2]));
                    case "loop" -> m.withLoop(Boolean.parseBoolean(parts[2]));
                    default -> throw new IllegalArgumentException("Unknown time modifier: " + parts[1]);
                };
            }
            case "transformations" -> {
                if (parts.length < 3) throw new IllegalArgumentException("Missing transformation value.");
                yield switch (parts[1].toLowerCase(Locale.ROOT)) {
                    case "rotation" -> m.withRotation(Double.parseDouble(parts[2]));
                    case "mirror" -> m.withMirror(PlaybackModifiers.Mirror.valueOf(parts[2].toUpperCase(Locale.ROOT)));
                    case "scale" -> {
                        if (parts.length < 4) throw new IllegalArgumentException("Usage: transformations scale <of_player|of_scene> <scale>");
                        double v = nonNegative(parts[3]);
                        yield parts[2].equalsIgnoreCase("of_player") ? m.withPlayerScale(v) : m.withSceneScale(v);
                    }
                    case "offset" -> {
                        if (parts.length < 5) throw new IllegalArgumentException("Usage: transformations offset <x> <y> <z>");
                        yield m.withOffset(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4]));
                    }
                    default -> throw new IllegalArgumentException("Unknown transformation modifier: " + parts[1]);
                };
            }
            default -> throw new IllegalArgumentException("Unknown modifier group: " + parts[0]);
        };
    }

    private void info(CommandSender sender, String[] args) throws IOException {
        require(args, 3, "/mocap scenes info <scene_name> [element_pos]");
        SceneData scene = requireScene(args[2]);
        sender.sendMessage(ChatColor.GOLD + "Scene: " + args[2]);
        sender.sendMessage(ChatColor.GRAY + "Elements: " + scene.elements().size());
        for (int i = 0; i < scene.elements().size(); i++) {
            SceneElement e = scene.elements().get(i);
            sender.sendMessage(ChatColor.GRAY + "  " + (i + 1) + " | " + e.name());
        }
        if (args.length >= 4) {
            int position = parsePosition(args[3]);
            if (position < 1 || position > scene.elements().size()) throw new IllegalArgumentException("Scene element not found: " + args[3]);
            SceneElement e = scene.elements().get(position - 1);
            sender.sendMessage(ChatColor.GRAY + "Element " + position + ": " + e.name());
            sender.sendMessage(ChatColor.GRAY + "  modifiers: " + e.modifiers());
        }
    }

    private void list(CommandSender sender, String[] args) throws IOException {
        if (args.length >= 3) {
            SceneData scene = requireScene(args[2]);
            sender.sendMessage(ChatColor.GOLD + "Scene elements: " + args[2]);
            for (int i = 0; i < scene.elements().size(); i++) sender.sendMessage(ChatColor.GRAY + "  " + (i + 1) + " | " + scene.elements().get(i).name());
            return;
        }
        List<String> names = sceneManager.getSceneNames();
        sender.sendMessage(ChatColor.GOLD + "Scenes: " + names.size());
        names.forEach(name -> sender.sendMessage(ChatColor.GRAY + "  " + name));
    }

    private SceneData requireScene(String name) throws IOException {
        SceneData data = sceneManager.load(name);
        if (data == null) throw new IllegalArgumentException("Scene not found: " + name);
        return data;
    }

    private static int parsePosition(String value) {
        try {
            int p = Integer.parseInt(value);
            if (p < 1) throw new NumberFormatException();
            return p;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Scene element position must be a positive integer.");
        }
    }

    private static double nonNegative(String value) {
        double v = Double.parseDouble(value);
        if (!Double.isFinite(v) || v < 0) throw new IllegalArgumentException("Value must be a finite non-negative number.");
        return v;
    }

    private static void require(String[] args, int count, String usage) {
        if (args.length < count) throw new IllegalArgumentException("Usage: " + usage);
    }

    private static void usage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap scenes <add|copy|rename|remove|add_to|remove_from|modify|info|list> ...");
    }
}
