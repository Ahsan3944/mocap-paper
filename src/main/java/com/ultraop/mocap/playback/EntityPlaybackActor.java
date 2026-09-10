package com.ultraop.mocap.playback;

import com.ultraop.mocap.recording.EntityStateFrame;
import com.ultraop.mocap.recording.PlayerStateFrame;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.bukkit.plugin.java.JavaPlugin;

/** Applies recorded timelines to a playback entity. */
public final class EntityPlaybackActor {
    public static final String MOCAP_ENTITY_TAG="mocap_entity";
    private static final ThreadLocal<String> QUEUED_PLAYER_AS_ENTITY_NBT = new ThreadLocal<>();
    public enum AfterPlayback { REMOVE, KILL, LEFT_UNTOUCHED, RELEASE_AS_NORMAL }
    private final Entity entity; private final double scale; private final boolean invulnerablePlayback;
    public static void queuePlayerAsEntityNbt(String nbt){if(nbt==null||nbt.isBlank())QUEUED_PLAYER_AS_ENTITY_NBT.remove();else QUEUED_PLAYER_AS_ENTITY_NBT.set(nbt);}
    private static String consumePlayerAsEntityNbt(){String nbt=QUEUED_PLAYER_AS_ENTITY_NBT.get();QUEUED_PLAYER_AS_ENTITY_NBT.remove();return nbt;}
    public EntityPlaybackActor(Entity entity,double scale){this(entity,scale,true);} public EntityPlaybackActor(Entity entity,double scale,boolean invulnerablePlayback){this.entity=entity;this.scale=scale;this.invulnerablePlayback=invulnerablePlayback;String nbt=consumePlayerAsEntityNbt();if(nbt!=null)applyNbt(nbt);entity.addScoreboardTag(MOCAP_ENTITY_TAG);applyScale();}
    private void applyNbt(String nbt){try{CompoundTag tag=TagParser.parseCompoundFully(nbt);if(entity.getWorld() instanceof org.bukkit.World world){var level=((CraftWorld)world).getHandle();((org.bukkit.craftbukkit.entity.CraftEntity)entity).getHandle().load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag));}}catch(Exception ignored){}}
    public Entity entity(){return entity;}
    public void apply(EntityStateFrame frame,Location location){if(!entity.isValid())return;entity.teleport(location);entity.setRotation(frame.yaw(),frame.pitch());entity.setVelocity(new Vector(frame.velocityX(),frame.velocityY(),frame.velocityZ()));entity.setFireTicks(frame.fireTicks());entity.setGlowing(frame.glowing());entity.setInvisible(frame.invisible());if(entity instanceof LivingEntity living){living.setPose(frame.pose());living.setFallDistance(frame.fallDistance());living.setInvulnerable(invulnerablePlayback||frame.invulnerable());if(frame.health()>0.0)living.setHealth(Math.min(living.getMaxHealth(),frame.health()));if(living.getEquipment()!=null){living.getEquipment().setItemInMainHand(clone(frame.mainHand()));living.getEquipment().setItemInOffHand(clone(frame.offHand()));ItemStack[] armor=frame.armor();if(armor.length>=4){living.getEquipment().setHelmet(clone(armor[0]));living.getEquipment().setChestplate(clone(armor[1]));living.getEquipment().setLeggings(clone(armor[2]));living.getEquipment().setBoots(clone(armor[3]));}}}if(frame.hurt())playHurt();applyScale();}
    public void apply(PlayerStateFrame frame,Location location){if(!entity.isValid())return;entity.teleport(location);entity.setRotation(frame.yaw(),frame.pitch());entity.setVelocity(new Vector(frame.velocityX(),frame.velocityY(),frame.velocityZ()));entity.setFireTicks(frame.fireTicks());entity.setGlowing(frame.glowing());entity.setInvisible(frame.invisible());if(entity instanceof LivingEntity living){living.setPose(frame.pose());living.setFallDistance(frame.fallDistance());living.setInvulnerable(invulnerablePlayback||frame.invulnerable());if(frame.health()>0.0)living.setHealth(Math.min(living.getMaxHealth(),frame.health()));if(living.getEquipment()!=null){living.getEquipment().setItemInMainHand(clone(frame.mainHand()));living.getEquipment().setItemInOffHand(clone(frame.offHand()));ItemStack[] armor=frame.armor();if(armor.length>=4){living.getEquipment().setHelmet(clone(armor[0]));living.getEquipment().setChestplate(clone(armor[1]));living.getEquipment().setLeggings(clone(armor[2]));living.getEquipment().setBoots(clone(armor[3]));}}}if(frame.hurt())playHurt();applyScale();}
    public void playHurt(){if(!invulnerablePlayback||!(entity instanceof LivingEntity living)||!living.isValid())return;boolean oldInv=living.isInvulnerable();double max=living.getMaxHealth();if(max<=0)return;try{living.setInvulnerable(false);living.setHealth(max);living.damage(1.0);}finally{if(living.isValid()&&living.getHealth()>0)living.setHealth(max);living.setInvulnerable(oldInv);}}
    public void remove(){if(!entity.isValid())return;AfterPlayback mode=mode();switch(mode){case REMOVE->entity.remove();case KILL->{if(entity instanceof LivingEntity living){living.setInvulnerable(false);living.setHealth(0.0);}else entity.remove();}case LEFT_UNTOUCHED->{}case RELEASE_AS_NORMAL->{entity.setGravity(true);entity.setInvulnerable(false);entity.removeScoreboardTag(MOCAP_ENTITY_TAG);if(entity instanceof Mob mob)mob.setAI(true);}}}
    private static AfterPlayback mode(){String value=JavaPlugin.getProvidingPlugin(EntityPlaybackActor.class).getConfig().getString("settings.entities_after_playback","remove");try{return AfterPlayback.valueOf(value.toUpperCase(java.util.Locale.ROOT));}catch(IllegalArgumentException ignored){return AfterPlayback.REMOVE;}}
    private void applyScale(){if(!(entity instanceof LivingEntity living)||!Double.isFinite(scale)||scale<=0.0)return;if(living.getAttribute(Attribute.SCALE)!=null)living.getAttribute(Attribute.SCALE).setBaseValue(scale);} private static ItemStack clone(ItemStack item){return item==null?null:item.clone();}
}
