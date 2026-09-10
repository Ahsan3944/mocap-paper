package com.ultraop.mocap.recording;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Camel;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Minecart;
import java.lang.reflect.Method;

/** Compact transient equivalent of upstream SET_NON_PLAYER_ENTITY_DATA. */
public final class NonPlayerEntityData {
    public static final String KEY="mocap_non_player_entity_data";
    private NonPlayerEntityData(){}
    public static String attach(Entity entity,String nbt){CompoundTag d=capture(entity,nbt);if(d==null)return nbt;try{CompoundTag base=nbt==null?new CompoundTag():TagParser.parseCompoundFully(nbt);base.put(KEY,d);return base.toString();}catch(Exception ignored){return "MOCAP_NP:"+d;}}
    public static boolean isOnly(String nbt){return nbt!=null&&nbt.startsWith("MOCAP_NP:");}
    public static CompoundTag extract(String nbt){if(nbt==null||nbt.isBlank())return null;try{if(isOnly(nbt))return TagParser.parseCompoundFully(nbt.substring(9));return TagParser.parseCompoundFully(nbt).getCompound(KEY).orElse(null);}catch(Exception ignored){return null;}}
    public static String strip(String nbt){if(nbt==null||nbt.isBlank()||isOnly(nbt))return null;try{CompoundTag base=TagParser.parseCompoundFully(nbt);base.remove(KEY);return base.toString();}catch(Exception ignored){return nbt;}}
    private static CompoundTag capture(Entity e,String nbt){CompoundTag d=new CompoundTag();boolean used=false;if(e instanceof Ageable a){d.putBoolean("flag2",a.isBaby());used=true;}if(e instanceof AbstractHorse h){byte flags=0;if(h.isTamed())flags|=2;if(!h.getInventory().getSaddle().getType().isAir())flags|=4;if(h.isEating())flags|=32;if(h.isRearing())flags|=64;d.putByte("byte1",flags);used=true;try{CompoundTag base=nbt==null?null:TagParser.parseCompoundFully(nbt);if(base!=null&&base.contains("Variant")){d.putInt("variant",base.getIntOr("Variant",0));}}catch(Exception ignored){}}if(e instanceof ChestedHorse h){d.putBoolean("flag1",h.isCarryingChest());used=true;}if(e instanceof Camel c){Boolean v=invokeBoolean(((CraftEntity)c).getHandle(),"isDashing");if(v!=null)d.putBoolean("flag1",v);used=true;}if(e instanceof Boat b){Object h=((CraftEntity)b).getHandle();Boolean right=invokeBoolean(h,"getPaddleState",1),left=invokeBoolean(h,"getPaddleState",0);if(right!=null)d.putBoolean("flag1",right);if(left!=null)d.putBoolean("leftPaddle",left);putInt(d,"int1",h,"getHurtTime");putInt(d,"int2",h,"getHurtDir");putInt(d,"int3",h,"getBubbleTime");putFloat(d,"float1",h,"getDamage");used=true;}if(e instanceof Minecart){Object h=((CraftEntity)e).getHandle();putInt(d,"int1",h,"getHurtTime");putInt(d,"int2",h,"getHurtDir");putInt(d,"int3",h,"getDamage");used=true;}if(e instanceof Arrow){Object h=((CraftEntity)e).getHandle();Boolean v=invokeBoolean(h,"isInGround");if(v!=null)d.putBoolean("flag1",v);used=true;}return used?d:null;}
    public static void apply(Entity e,CompoundTag d){if(e==null||d==null)return;try{if(e instanceof Ageable a&&d.contains("flag2")){if(d.getBooleanOr("flag2",false))a.setBaby();else a.setAdult();}if(e instanceof AbstractHorse h&&d.contains("byte1")){int f=d.getByteOr("byte1",(byte)0)&255;h.setTamed((f&2)!=0);h.setEating((f&32)!=0);h.setRearing((f&64)!=0);if(d.contains("variant")&&h instanceof Horse horse){int v=d.getIntOr("variant",0);Horse.Variant[] vs=Horse.Variant.values();if(v>=0&&v<vs.length)horse.setVariant(vs[v]);}}if(e instanceof ChestedHorse h&&d.contains("flag1"))h.setCarryingChest(d.getBooleanOr("flag1",false));if(e instanceof Camel c&&d.contains("flag1"))invoke(((CraftEntity)c).getHandle(),"setDashing",d.getBooleanOr("flag1",false));if(e instanceof Boat){Object h=((CraftEntity)e).getHandle();if(d.contains("leftPaddle")&&d.contains("flag1"))invoke(h,"setPaddleState",d.getBooleanOr("leftPaddle",false),d.getBooleanOr("flag1",false));setInt(h,"setHurtTime",d,"int1");setInt(h,"setHurtDir",d,"int2");setInt(h,"setBubbleTime",d,"int3");if(d.contains("float1"))invoke(h,"setDamage",d.getFloatOr("float1",0));}if(e instanceof Minecart){Object h=((CraftEntity)e).getHandle();setInt(h,"setHurtTime",d,"int1");setInt(h,"setHurtDir",d,"int2");setInt(h,"setDamage",d,"int3");}if(e instanceof Arrow&&d.contains("flag1"))invoke(((CraftEntity)e).getHandle(),"setInGround",d.getBooleanOr("flag1",false));}catch(Exception ignored){}}
    private static void putInt(CompoundTag d,String key,Object h,String method){Integer v=intValue(h,method);if(v!=null)d.putInt(key,v);}
    private static void putFloat(CompoundTag d,String key,Object h,String method){Double v=doubleValue(h,method);if(v!=null)d.putFloat(key,v.floatValue());}
    private static void setInt(Object h,String method,CompoundTag d,String key){if(d.contains(key))invoke(h,method,d.getIntOr(key,0));}
    private static Boolean invokeBoolean(Object h,String method,Object...a){Object v=invoke(h,method,a);return v instanceof Boolean b?b:null;}
    private static Integer intValue(Object h,String method,Object...a){Object v=invoke(h,method,a);return v instanceof Number n?n.intValue():null;}
    private static Double doubleValue(Object h,String method,Object...a){Object v=invoke(h,method,a);return v instanceof Number n?n.doubleValue():null;}
    private static Object invoke(Object h,String name,Object...a){if(h==null)return null;for(Method m:h.getClass().getMethods())if(m.getName().equals(name)&&m.getParameterCount()==a.length)try{return m.invoke(h,a);}catch(Exception ignored){}return null;}
}
