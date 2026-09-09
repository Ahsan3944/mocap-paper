package com.ultraop.mocap.command;

import com.ultraop.mocap.playback.EntityFilter;
import com.ultraop.mocap.playback.PlayerAsEntity;
import com.ultraop.mocap.playback.PlayerSkin;
import com.ultraop.mocap.playback.PlaybackManager;
import com.ultraop.mocap.playback.PlaybackModifiers;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Adds the structured playback-modifier command layer without changing the existing scene/recording handlers. */
public final class ParityMoCapCommand implements CommandExecutor, TabCompleter {
    private final RootMoCapCommand base;
    private final PlaybackManager playbackManager;

    public ParityMoCapCommand(RootMoCapCommand base, PlaybackManager playbackManager) {
        this.base = base;
        this.playbackManager = playbackManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 3 && args[0].equalsIgnoreCase("playback") && args[1].equalsIgnoreCase("modifiers")) {
            return handleModifiers(sender, args);
        }
        return base.onCommand(sender, command, label, args);
    }

    private boolean handleModifiers(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "A player is required for playback modifiers.");
            return true;
        }
        if (args.length == 3 || args[2].equalsIgnoreCase("list")) {
            list(player);
            return true;
        }
        if (args[2].equalsIgnoreCase("reset")) {
            playbackManager.resetModifiers(player);
            sender.sendMessage(ChatColor.GREEN + "Playback modifiers reset.");
            return true;
        }
        if (!args[2].equalsIgnoreCase("set") || args.length < 5) {
            usage(sender);
            return true;
        }

        PlaybackModifiers current = playbackManager.getModifiers(player);
        try {
            PlaybackModifiers next = switch (args[3].toLowerCase(Locale.ROOT)) {
                case "player_name" -> setPlayerName(current, args);
                case "player_skin" -> setPlayerSkin(current, args);
                case "player_as_entity" -> setPlayerAsEntity(current, args);
                case "entity_filter" -> setEntityFilter(current, args);
                default -> throw new IllegalArgumentException("Unknown modifier group: " + args[3]);
            };
            playbackManager.setModifiers(player, next);
            sender.sendMessage(ChatColor.GREEN + "Playback modifier updated.");
        } catch (RuntimeException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }

    private PlaybackModifiers setPlayerName(PlaybackModifiers m, String[] a) {
        if (a.length < 5) throw new IllegalArgumentException("Usage: ... player_name <inherited|blank|set> [name]");
        return switch (a[4].toLowerCase(Locale.ROOT)) {
            case "inherited" -> m.withPlayerName(null);
            case "blank" -> m.withPlayerName("");
            case "set" -> m.withPlayerName(require(a, 5, "player name"));
            default -> throw new IllegalArgumentException("Player name mode must be inherited, blank, or set.");
        };
    }

    private PlaybackModifiers setPlayerSkin(PlaybackModifiers m, String[] a) {
        if (a.length < 5) throw new IllegalArgumentException("Usage: ... player_skin <default|from_player|from_file|from_mineskin> [value]");
        return switch (a[4].toLowerCase(Locale.ROOT)) {
            case "default" -> m.withPlayerSkin(PlayerSkin.DEFAULT);
            case "from_player" -> m.withPlayerSkin(PlayerSkin.fromPlayer(require(a, 5, "player name")));
            case "from_file" -> m.withPlayerSkin(PlayerSkin.fromFile(require(a, 5, "skin filename")));
            case "from_mineskin" -> m.withPlayerSkin(PlayerSkin.fromMineSkin(require(a, 5, "MineSkin URL")));
            default -> throw new IllegalArgumentException("Skin source must be default, from_player, from_file, or from_mineskin.");
        };
    }

    private PlaybackModifiers setPlayerAsEntity(PlaybackModifiers m, String[] a) {
        if (a.length < 5) throw new IllegalArgumentException("Usage: ... player_as_entity <disabled|enabled> [entity] [nbt]");
        if (a[4].equalsIgnoreCase("disabled")) return m.withPlayerAsEntity(PlayerAsEntity.DISABLED);
        if (!a[4].equalsIgnoreCase("enabled")) throw new IllegalArgumentException("Mode must be disabled or enabled.");
        String id = require(a, 5, "entity");
        String nbt = a.length >= 7 ? a[6] : null;
        EntityType type = parseEntityType(id);
        return m.withPlayerAsEntity(PlayerAsEntity.enabled(type, nbt));
    }

    private PlaybackModifiers setEntityFilter(PlaybackModifiers m, String[] a) {
        if (a.length < 5) throw new IllegalArgumentException("Usage: ... entity_filter <disabled|enabled> [filter]");
        if (a[4].equalsIgnoreCase("disabled")) return m.withEntityFilter(EntityFilter.disabled());
        if (!a[4].equalsIgnoreCase("enabled")) throw new IllegalArgumentException("Mode must be disabled or enabled.");
        return m.withEntityFilter(new EntityFilter(require(a, 5, "entity filter")));
    }

    private EntityType parseEntityType(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("minecraft:")) normalized = normalized.substring("minecraft:".length());
        EntityType type = EntityType.fromName(normalized);
        if (type == null) throw new IllegalArgumentException("Unknown entity type: " + value);
        return type;
    }

    private String require(String[] args, int index, String label) {
        if (args.length <= index || args[index].isBlank()) throw new IllegalArgumentException("Missing " + label + ".");
        return args[index];
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "/mocap playback modifiers <set|list|reset> <player_name|player_skin|player_as_entity|entity_filter> ...");
    }

    private void list(Player player) {
        PlaybackModifiers m = playbackManager.getModifiers(player);
        player.sendMessage(ChatColor.GOLD + "Playback modifiers:");
        player.sendMessage(ChatColor.GRAY + "  player_name=" + (m.playerName() == null ? "inherited" : m.playerName().isEmpty() ? "blank" : m.playerName()));
        player.sendMessage(ChatColor.GRAY + "  player_skin=" + m.playerSkin().source() + (m.playerSkin().path() == null ? "" : " [" + m.playerSkin().path() + "]"));
        player.sendMessage(ChatColor.GRAY + "  player_as_entity=" + (m.playerAsEntity().enabled() ? m.playerAsEntity().entityId() : "disabled"));
        player.sendMessage(ChatColor.GRAY + "  entity_filter=" + (m.entityFilter().enabled() ? m.entityFilter().expression() : "disabled"));
        player.sendMessage(ChatColor.GRAY + "  time: start_delay=" + m.startDelaySeconds() + ", wait_on_start=" + m.waitOnStartSeconds() + ", wait_on_end=" + m.waitOnEndSeconds() + ", wait_for_parent_end=" + m.waitForParentEnd() + ", loop=" + m.loop());
        player.sendMessage(ChatColor.GRAY + "  transformations: rotation=" + m.rotationDegrees() + ", mirror=" + m.mirror() + ", player_scale=" + m.playerScale() + ", scene_scale=" + m.sceneScale() + ", offset=" + m.offsetX() + "," + m.offsetY() + "," + m.offsetZ());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length < 3 || !args[0].equalsIgnoreCase("playback") || !args[1].equalsIgnoreCase("modifiers")) {
            return base.onTabComplete(sender, command, alias, args);
        }
        if (args.length == 3) return filter(List.of("set", "list", "reset"), args[2]);
        if (args[2].equalsIgnoreCase("set") && args.length == 4) return filter(List.of("player_name", "player_skin", "player_as_entity", "entity_filter", "time", "transformations"), args[3]);
        if (args[2].equalsIgnoreCase("set")) return modifierValues(args);
        return List.of();
    }

    private List<String> modifierValues(String[] args) {
        String group = args[3].toLowerCase(Locale.ROOT);
        int n = args.length;
        if (group.equals("player_name") && n == 5) return filter(List.of("inherited", "blank", "set"), args[4]);
        if (group.equals("player_skin") && n == 5) return filter(List.of("default", "from_player", "from_file", "from_mineskin"), args[4]);
        if (group.equals("player_as_entity") && n == 5) return filter(List.of("disabled", "enabled"), args[4]);
        if (group.equals("entity_filter") && n == 5) return filter(List.of("disabled", "enabled"), args[4]);
        if (group.equals("time") && n == 5) return filter(List.of("start_delay", "wait_on_start", "wait_on_end", "wait_for_parent_end", "loop"), args[4]);
        if (group.equals("transformations") && n == 5) return filter(List.of("rotation", "mirror", "scale", "offset", "config"), args[4]);
        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.startsWith(lower)).toList();
    }
}
