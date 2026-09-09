package com.ultraop.mocap.playback;

import com.ultraop.mocap.recording.PlayerStateFrame;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/** Applies the recorded player timeline to the entity selected by player_as_entity. */
public final class EntityPlaybackActor {
    private final Entity entity;
    private final double scale;

    public EntityPlaybackActor(Entity entity, double scale) {
        this.entity = entity;
        this.scale = scale;
        applyScale();
    }

    public Entity entity() {
        return entity;
    }

    public void apply(PlayerStateFrame frame, Location location) {
        entity.teleport(location);
        entity.setVelocity(new Vector(frame.velocityX(), frame.velocityY(), frame.velocityZ()));
        entity.setFireTicks(frame.fireTicks());
        entity.setGlowing(frame.glowing());
        entity.setInvisible(frame.invisible());

        if (entity instanceof LivingEntity living) {
            living.setInvulnerable(frame.invulnerable());
            living.setInvisible(frame.invisible());
            living.setGlowing(frame.glowing());
            living.setFireTicks(frame.fireTicks());
            living.setVelocity(new Vector(frame.velocityX(), frame.velocityY(), frame.velocityZ()));
            if (frame.health() > 0.0) {
                living.setHealth(Math.min(living.getMaxHealth(), frame.health()));
            }
            if (living.getEquipment() != null) {
                living.getEquipment().setItemInMainHand(clone(frame.mainHand()));
                living.getEquipment().setItemInOffHand(clone(frame.offHand()));
                living.getEquipment().setHelmet(clone(frame.armor()[3]));
                living.getEquipment().setChestplate(clone(frame.armor()[2]));
                living.getEquipment().setLeggings(clone(frame.armor()[1]));
                living.getEquipment().setBoots(clone(frame.armor()[0]));
            }
        }
        applyScale();
    }

    public void remove() {
        if (!entity.isDead()) entity.remove();
    }

    private void applyScale() {
        if (!(entity instanceof LivingEntity living) || !Double.isFinite(scale) || scale <= 0.0) return;
        if (living.getAttribute(Attribute.SCALE) != null) {
            living.getAttribute(Attribute.SCALE).setBaseValue(scale);
        }
    }

    private static ItemStack clone(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
