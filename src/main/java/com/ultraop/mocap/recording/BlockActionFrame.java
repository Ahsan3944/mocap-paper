package com.ultraop.mocap.recording;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

/** A recorded block mutation or interaction. */
public record BlockActionFrame(
        long tick,
        String worldKey,
        int x,
        int y,
        int z,
        Action action,
        String beforeState,
        String afterState
) {
    public enum Action { PLACE, BREAK, INTERACT }

    public static BlockActionFrame place(long tick, Location location, BlockData before, BlockData after) {
        return create(tick, location, Action.PLACE, before, after);
    }

    public static BlockActionFrame breakBlock(long tick, Location location, BlockData before, BlockData after) {
        return create(tick, location, Action.BREAK, before, after);
    }

    public static BlockActionFrame interact(long tick, Location location, BlockData state) {
        return create(tick, location, Action.INTERACT, state, state);
    }

    private static BlockActionFrame create(long tick, Location location, Action action, BlockData before, BlockData after) {
        return new BlockActionFrame(tick, location.getWorld().getKey().toString(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ(), action, before == null ? "minecraft:air" : before.getAsString(),
                after == null ? "minecraft:air" : after.getAsString());
    }

    public Location location(World fallback) {
        World world = fallback;
        for (World candidate : Bukkit.getWorlds()) {
            if (candidate.getKey().toString().equals(worldKey)) { world = candidate; break; }
        }
        return new Location(world, x, y, z);
    }
}
