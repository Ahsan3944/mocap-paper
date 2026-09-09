package com.ultraop.mocap.recording;

import com.ultraop.mocap.playback.FakePlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.UUID;

/** Immutable 20 TPS snapshot of the recorded player. */
public record PlayerStateFrame(
        long tick, String worldKey, double x, double y, double z, float yaw, float pitch,
        double velocityX, double velocityY, double velocityZ, boolean onGround,
        boolean sprinting, boolean sneaking, boolean swimming, boolean gliding, boolean flying,
        Pose pose, float fallDistance, int fireTicks, boolean invisible, boolean glowing,
        boolean invulnerable, double health, ItemStack mainHand, ItemStack offHand, ItemStack[] armor,
        UUID vehicleId, boolean swingMainHand, boolean swingOffHand,
        boolean activeItemUse, EquipmentSlot activeItemHand, ItemStack activeItem,
        int activeItemUsedTime, int activeItemRemainingTime
) implements FakePlayer.PlayerStateAdapter {
    public static PlayerStateFrame capture(Player player, long tick, boolean swingMainHand, boolean swingOffHand) {
        Location location = player.getLocation();
        ItemStack[] armor = Arrays.stream(player.getInventory().getArmorContents())
                .map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new);
        UUID vehicleId = player.getVehicle() == null ? null : player.getVehicle().getUniqueId();
        boolean active = player.hasActiveItem();
        EquipmentSlot hand = active ? player.getActiveItemHand() : null;
        ItemStack activeItem = active ? cloneItem(player.getActiveItem()) : null;
        int used = active ? player.getActiveItemUsedTime() : 0;
        int remaining = active ? player.getActiveItemRemainingTime() : 0;
        return new PlayerStateFrame(tick, location.getWorld().getKey().toString(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(), player.getVelocity().getX(), player.getVelocity().getY(), player.getVelocity().getZ(),
                player.isOnGround(), player.isSprinting(), player.isSneaking(), player.isSwimming(), player.isGliding(), player.isFlying(),
                player.getPose(), player.getFallDistance(), player.getFireTicks(), player.isInvisible(), player.isGlowing(), player.isInvulnerable(),
                player.getHealth(), cloneItem(player.getInventory().getItemInMainHand()), cloneItem(player.getInventory().getItemInOffHand()), armor,
                vehicleId, swingMainHand, swingOffHand, active, hand, activeItem, used, remaining);
    }
    private static ItemStack cloneItem(ItemStack item) { return item == null ? null : item.clone(); }
}
