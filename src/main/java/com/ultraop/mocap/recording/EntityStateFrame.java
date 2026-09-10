package com.ultraop.mocap.recording;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** Immutable per-tick snapshot of a tracked non-player entity. */
public record EntityStateFrame(
        long tick, UUID entityId, String entityType, String worldKey,
        double x, double y, double z, float yaw, float pitch,
        double velocityX, double velocityY, double velocityZ,
        Pose pose, float fallDistance, int fireTicks, boolean invisible, boolean glowing,
        boolean invulnerable, double health, ItemStack mainHand, ItemStack offHand,
        ItemStack[] armor, UUID vehicleId, boolean hurt, String nbt) {
    public EntityStateFrame(long tick,UUID entityId,String entityType,String worldKey,double x,double y,double z,float yaw,float pitch,double velocityX,double velocityY,double velocityZ,Pose pose,float fallDistance,int fireTicks,boolean invisible,boolean glowing,boolean invulnerable,double health,ItemStack mainHand,ItemStack offHand,ItemStack[] armor,UUID vehicleId){this(tick,entityId,entityType,worldKey,x,y,z,yaw,pitch,velocityX,velocityY,velocityZ,pose,fallDistance,fireTicks,invisible,glowing,invulnerable,health,mainHand,offHand,armor,vehicleId,false,null);}
    public EntityStateFrame(long tick,UUID entityId,String entityType,String worldKey,double x,double y,double z,float yaw,float pitch,double velocityX,double velocityY,double velocityZ,Pose pose,float fallDistance,int fireTicks,boolean invisible,boolean glowing,boolean invulnerable,double health,ItemStack mainHand,ItemStack offHand,ItemStack[] armor,UUID vehicleId,boolean hurt){this(tick,entityId,entityType,worldKey,x,y,z,yaw,pitch,velocityX,velocityY,velocityZ,pose,fallDistance,fireTicks,invisible,glowing,invulnerable,health,mainHand,offHand,armor,vehicleId,hurt,null);}
    public static EntityStateFrame capture(Entity entity, long tick, boolean hurt) { return capture(entity,tick,hurt,null); }
    public static EntityStateFrame capture(Entity entity,long tick,boolean hurt,String nbt) {
        Location location=entity.getLocation(); LivingEntity living=entity instanceof LivingEntity value?value:null; EntityEquipment equipment=living==null?null:living.getEquipment();
        ItemStack[] armor=equipment==null?new ItemStack[0]:new ItemStack[]{cloneItem(equipment.getHelmet()),cloneItem(equipment.getChestplate()),cloneItem(equipment.getLeggings()),cloneItem(equipment.getBoots())};
        UUID vehicleId=entity.getVehicle()==null?null:entity.getVehicle().getUniqueId();
        return new EntityStateFrame(tick,entity.getUniqueId(),entity.getType().getKey().toString(),location.getWorld().getKey().toString(),location.getX(),location.getY(),location.getZ(),location.getYaw(),location.getPitch(),entity.getVelocity().getX(),entity.getVelocity().getY(),entity.getVelocity().getZ(),living==null?Pose.STANDING:living.getPose(),living==null?0.0f:living.getFallDistance(),entity.getFireTicks(),entity.isInvisible(),entity.isGlowing(),living!=null&&living.isInvulnerable(),living==null?-1.0:living.getHealth(),equipment==null?null:cloneItem(equipment.getItemInMainHand()),equipment==null?null:cloneItem(equipment.getItemInOffHand()),armor,vehicleId,hurt,nbt);
    }
    public static EntityStateFrame capture(Entity entity,long tick){return capture(entity,tick,false,null);}
    private static ItemStack cloneItem(ItemStack item){return item==null?null:item.clone();}
}
