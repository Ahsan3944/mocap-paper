package com.ultraop.mocap.recording;

import com.ultraop.mocap.playback.FakePlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

/**
 * Immutable 20 TPS snapshot of the recorded player.
 *
 * Every mutable Bukkit value is copied at capture time so later player changes cannot
 * mutate an already-recorded frame.
 */
public record PlayerStateFrame(
        long tick,
        String worldKey,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        double velocityX,
        double velocityY,
        double velocityZ,
        boolean onGround,
        boolean sprinting,
        boolean sneaking,
        boolean swimming,
        boolean gliding,
        boolean flying,
        Pose pose,
        float fallDistance,
        int fireTicks,
        boolean invisible,
        boolean glowing,
        boolean invulnerable,
        double health,
        ItemStack mainHand,
        ItemStack offHand,
        ItemStack[] armor
) implements FakePlayer.PlayerStateAdapter {
    public static PlayerStateFrame capture(Player player, long tick) {
        Location location = player.getLocation();
        ItemStack[] armor = Arrays.stream(player.getInventory().getArmorContents())
                .map(item -> item == null ? null : item.clone())
                .toArray(ItemStack[]::new);

        return new PlayerStateFrame(
                tick,
                location.getWorld().getKey().toString(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                player.getVelocity().getX(),
                player.getVelocity().getY(),
                player.getVelocity().getZ(),
                player.isOnGround(),
                player.isSprinting(),
                player.isSneaking(),
                player.isSwimming(),
                player.isGliding(),
                player.isFlying(),
                player.getPose(),
                player.getFallDistance(),
                player.getFireTicks(),
                player.isInvisible(),
                player.isGlowing(),
                player.isInvulnerable(),
                player.getHealth(),
                cloneItem(player.getInventory().getItemInMainHand()),
                cloneItem(player.getInventory().getItemInOffHand()),
                armor
        );
    }

    private static ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
