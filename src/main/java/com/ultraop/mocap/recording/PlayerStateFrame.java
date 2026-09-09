package com.ultraop.mocap.recording;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

/**
 * A single 20 TPS snapshot of the recorded player.
 *
 * The frame deliberately contains only immutable/copy-owned values so the live Bukkit
 * player state cannot mutate an already-recorded frame.
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
        float fallDistance,
        int fireTicks,
        boolean invisible,
        boolean glowing,
        boolean invulnerable,
        double health,
        EntityType poseEntityType,
        ItemStack mainHand,
        ItemStack offHand,
        ItemStack[] armor
) {
    public static PlayerStateFrame capture(Player player, long tick) {
        Location location = player.getLocation();
        ItemStack[] armor = Arrays.stream(player.getInventory().getArmorContents())
                .map(item -> item == null ? null : item.clone())
                .toArray(ItemStack[]::new);

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

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
                player.getFallDistance(),
                player.getFireTicks(),
                player.isInvisible(),
                player.isGlowing(),
                player.isInvulnerable(),
                player.getHealth(),
                EntityType.PLAYER,
                mainHand == null ? null : mainHand.clone(),
                offHand == null ? null : offHand.clone(),
                armor
        );
    }
}
