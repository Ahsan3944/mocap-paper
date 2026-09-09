package com.ultraop.mocap.recording;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** Immutable per-tick snapshot of a tracked non-player entity. */
public record EntityStateFrame(
        long tick,
        UUID entityId,
        String entityType,
        String worldKey,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        double velocityX,
        double velocityY,
        double velocityZ,
        int fireTicks,
        boolean invisible,
        boolean glowing,
        boolean invulnerable,
        double health,
        ItemStack mainHand,
        ItemStack offHand,
        ItemStack[] armor
) {
    public static EntityStateFrame capture(Entity entity, long tick) {
        Location location = entity.getLocation();
        LivingEntity living = entity instanceof LivingEntity value ? value : null;
        EntityEquipment equipment = living == null ? null : living.getEquipment();
        ItemStack[] armor = equipment == null ? new ItemStack[0] : new ItemStack[] {
                cloneItem(equipment.getHelmet()), cloneItem(equipment.getChestplate()),
                cloneItem(equipment.getLeggings()), cloneItem(equipment.getBoots())
        };

        return new EntityStateFrame(
                tick, entity.getUniqueId(), entity.getType().getKey().toString(),
                location.getWorld().getKey().toString(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(), entity.getVelocity().getX(), entity.getVelocity().getY(),
                entity.getVelocity().getZ(), entity.getFireTicks(), entity.isInvisible(), entity.isGlowing(),
                living != null && living.isInvulnerable(),
                living == null ? -1.0 : living.getHealth(),
                equipment == null ? null : cloneItem(equipment.getItemInMainHand()),
                equipment == null ? null : cloneItem(equipment.getItemInOffHand()), armor);
    }

    private static ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
