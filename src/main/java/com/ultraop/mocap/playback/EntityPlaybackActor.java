package com.ultraop.mocap.playback;

import com.ultraop.mocap.recording.PlayerStateFrame;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/** Applies the recorded player timeline to a normal Bukkit entity when player_as_entity is active. */
public final class EntityPlaybackActor {
    private final Entity entity;

    public EntityPlaybackActor(Entity entity) {
        this.entity = entity;
    }

    public Entity entity() { return entity; }

    public void apply(PlayerStateFrame frame, Location location) {
        entity.teleport(location);
        entity.setVelocity(new Vector(frame.velocityX(), frame.velocityY(), frame.velocityZ()));
        entity.setFireTicks(frame.fireTicks());
        entity.setGlowing(frame.glowing());
        entity.setInvisible(frame.invisible());

        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setInvulnerable(frame.invulnerable());
            living.setInvisible(frame.invisible());
            living.setGlowing(frame.glowing());
            living.setFireTicks(frame.fireTicks());
            living.setVelocity(new Vector(frame.velocityX(), frame.velocityY(), frame.velocityZ()));
            if (frame.health() > 0.0) {
                living.setHealth(Math.min(living.getMaxHealth(), frame.health()));
            }
            living.getEquipment().setItemInMainHand(clone(frame.mainHand()));
            living.getEquipment().setItemInOffHand(clone(frame.offHand()));
            living.getEquipment().setHelmet(clone(frame.armor()[3]));
            living.getEquipment().setChestplate(clone(frame.armor()[2]));
            living.getEquipment().setLeggings(clone(frame.armor()[1]));
            living.getEquipment().setBoots(clone(frame.armor()[0]));
        }
    }

    public void remove() {
        if (!entity.isDead()) entity.remove();
    }

    private static ItemStack clone(ItemStack item) { return item == null ? null : item.clone(); }
}
