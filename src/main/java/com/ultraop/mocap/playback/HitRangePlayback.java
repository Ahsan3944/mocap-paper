package com.ultraop.mocap.playback;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.plugin.java.JavaPlugin;

/** Replays the upstream hit_range behavior for playback swings. */
public final class HitRangePlayback {
    private HitRangePlayback() {}

    public static void apply(Entity attacker, boolean swingMainHand, boolean swingOffHand) {
        if (attacker == null || !(attacker instanceof LivingEntity living) || (!swingMainHand && !swingOffHand)) return;
        double configured = 0.0;
        try {
            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(HitRangePlayback.class);
            configured = plugin.getConfig().getDouble("settings.hit_range", 0.0);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        double multiplier = Math.min(configured, 4.0);
        if (!Double.isFinite(multiplier) || multiplier <= 0.0) return;

        AttributeInstance interaction = living.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        double baseRange = interaction == null ? 4.5 : interaction.getValue();
        double range = baseRange * multiplier;
        if (!Double.isFinite(range) || range <= 0.0) return;

        Location eye = living.getEyeLocation();
        Vector direction = eye.getDirection();
        if (direction.lengthSquared() == 0.0) return;
        direction.normalize();

        RayTraceResult hit = attacker.getWorld().rayTraceEntities(
                eye, direction, range, Math.max(multiplier - 1.0, 0.0), entity -> entity != attacker);
        if (hit == null || hit.getHitEntity() == null) return;

        attack(attacker, hit.getHitEntity());
    }

    private static void attack(Entity attacker, Entity target) {
        if (attacker instanceof Player player && player instanceof CraftPlayer craftPlayer && target instanceof CraftEntity craftTarget) {
            try {
                craftPlayer.getHandle().attack(craftTarget.getHandle());
                return;
            } catch (RuntimeException ignored) {
                // Fall back to Bukkit damage when a server-side entity cannot use the native player attack path.
            }
        }

        if (attacker instanceof LivingEntity living && target instanceof LivingEntity targetLiving) {
            AttributeInstance damageAttribute = living.getAttribute(Attribute.ATTACK_DAMAGE);
            double damage = damageAttribute == null ? 1.0 : damageAttribute.getValue();
            if (Double.isFinite(damage) && damage > 0.0) {
                targetLiving.damage(damage, attacker);
            }
        }
    }
}
