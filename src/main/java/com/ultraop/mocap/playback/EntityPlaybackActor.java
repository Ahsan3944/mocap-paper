package com.ultraop.mocap.playback;

import com.ultraop.mocap.recording.EntityStateFrame;
import com.ultraop.mocap.recording.PlayerStateFrame;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/** Applies recorded timelines to a playback entity. */
public final class EntityPlaybackActor {
    public static final String MOCAP_ENTITY_TAG="mocap_entity";
    private final Entity entity; private final double scale; private final boolean invulnerablePlayback;
    public EntityPlaybackActor(Entity entity,double scale){this(entity,scale,true);} public EntityPlaybackActor(Entity entity,double scale,boolean invulnerablePlayback){this.entity=entity;this.scale=scale;this.invulnerablePlayback=invulnerablePlayback;entity.addScoreboardTag(MOCAP_ENTITY_TAG);applyScale();}
    public Entity entity(){return entity;}
    public void apply(EntityStateFrame frame,Location location){if(!entity.isValid())return;entity.teleport(location);entity.setRotation(frame.yaw(),frame.pitch());entity.setVelocity(new Vector(frame.velocityX(),frame.velocityY(),frame.velocityZ()));entity.setFireTicks(frame.fireTicks());entity.setGlowing(frame.glowing());entity.setInvisible(frame.invisible());if(entity instanceof LivingEntity living){living.setPose(frame.pose());living.setFallDistance(frame.fallDistance());living.setInvulnerable(invulnerablePlayback||frame.invulnerable());if(frame.health()>0.0)living.setHealth(Math.min(living.getMaxHealth(),frame.health()));if(living.getEquipment()!=null){living.getEquipment().setItemInMainHand(clone(frame.mainHand()));living.getEquipment().setItemInOffHand(clone(frame.offHand()));ItemStack[] armor=frame.armor();if(armor.length>=4){living.getEquipment().setHelmet(clone(armor[0]));living.getEquipment().setChestplate(clone(armor[1]));living.getEquipment().setLeggings(clone(armor[2]));living.getEquipment().setBoots(clone(armor[3]));}}}if(frame.hurt())playHurt();applyScale();}
    public void apply(PlayerStateFrame frame,Location location){if(!entity.isValid())return;entity.teleport(location);entity.setRotation(frame.yaw(),frame.pitch());entity.setVelocity(new Vector(frame.velocityX(),frame.velocityY(),frame.velocityZ()));entity.setFireTicks(frame.fireTicks());entity.setGlowing(frame.glowing());entity.setInvisible(frame.invisible());if(entity instanceof LivingEntity living){living.setPose(frame.pose());living.setFallDistance(frame.fallDistance());living.setInvulnerable(invulnerablePlayback||frame.invulnerable());if(frame.health()>0.0)living.setHealth(Math.min(living.getMaxHealth(),frame.health()));if(living.getEquipment()!=null){living.getEquipment().setItemInMainHand(clone(frame.mainHand()));living.getEquipment().setItemInOffHand(clone(frame.offHand()));ItemStack[] armor=frame.armor();if(armor.length>=4){living.getEquipment().setHelmet(clone(armor[0]));living.getEquipment().setChestplate(clone(armor[1]));living.getEquipment().setLeggings(clone(armor[2]));living.getEquipment().setBoots(clone(armor[0]));}}}if(frame.hurt())playHurt();applyScale();}
    public void playHurt(){if(!invulnerablePlayback||!(entity instanceof LivingEntity living)||!living.isValid())return;boolean oldInv=living.isInvulnerable();double max=living.getMaxHealth();if(max<=0)return;try{living.setInvulnerable(false);living.setHealth(max);living.damage(1.0);}finally{if(living.isValid()&&living.getHealth()>0)living.setHealth(max);living.setInvulnerable(oldInv);}}
    public void remove(){if(entity.isValid())entity.remove();} private void applyScale(){if(!(entity instanceof LivingEntity living)||!Double.isFinite(scale)||scale<=0.0)return;if(living.getAttribute(Attribute.SCALE)!=null)living.getAttribute(Attribute.SCALE).setBaseValue(scale);} private static ItemStack clone(ItemStack item){return item==null?null:item.clone();}
}
