package com.ultraop.mocap.command;

import com.ultraop.mocap.playback.PlaybackManager;
import com.ultraop.mocap.playback.PlaybackModifiers;
import com.ultraop.mocap.playback.PlayerSkin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Handles the upstream player_skin playback modifier forms. */
public final class SkinModifierCommand {
    private final PlaybackManager playbackManager;
    private final SkinSuggestionCache suggestions;
    public SkinModifierCommand(PlaybackManager playbackManager, SkinSuggestionCache suggestions) { this.playbackManager = playbackManager; this.suggestions = suggestions; }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "A player is required for playback modifiers."); return true; }
        if (args.length < 6) { sender.sendMessage(ChatColor.YELLOW + "Usage: /mocap playback modifiers set player_skin <from_player|from_file|from_mineskin|default> <value>"); return true; }
        String mode = args[5].toLowerCase();
        try {
            PlayerSkin skin = switch (mode) {
                case "default" -> PlayerSkin.DEFAULT;
                case "from_player" -> PlayerSkin.fromPlayer(args[6]);
                case "from_file" -> PlayerSkin.fromFile(args[6]);
                case "from_mineskin" -> PlayerSkin.fromMineSkin(args[6]);
                default -> throw new IllegalArgumentException("Unknown player skin source: " + args[5]);
            };
            PlaybackModifiers current = playbackManager.getModifiers(player);
            playbackManager.setModifiers(player, current.withPlayerSkin(skin));
            sender.sendMessage(ChatColor.GREEN + "Player skin modifier updated.");
        } catch (IllegalArgumentException e) { sender.sendMessage(ChatColor.RED + e.getMessage()); }
        return true;
    }

    public java.util.List<String> suggest(String input) { return suggestions.get(input); }
}
