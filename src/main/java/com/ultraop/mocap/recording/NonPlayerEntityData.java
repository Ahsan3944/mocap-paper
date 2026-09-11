package com.ultraop.mocap.recording;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Camel;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Compact transient equivalent of upstream SET_NON_PLAYER_ENTITY_DATA and SET_EFFECT_PARTICLES. */
public final class NonPlayerEntityData {
    public static final String KEY = "mocap_non_player_entity_data";
    private static final String LEGACY_PREFIX = "MOCAP_NP:";

    private NonPlayerEntityData() {}

    public static String attach(Entity entity, String nbt) {
        CompoundTag data = capture(entity, nbt);
        if (data == null) return nbt;
        try {
            CompoundTag base = nbt == null ? new CompoundTag() : TagParser.parseCompoundFully(nbt);
            base.put(KEY, data);
            return base.toString();
        } catch (Exception ignored) {
            return LEGACY_PREFIX + data;
        }
    }

    public static boolean isOnly(String nbt) {
        return nbt != null && nbt.startsWith(LEGACY_PREFIX);
    }

    public static CompoundTag extract(String nbt) {
        if (nbt == null || nbt.isBlank()) return null;
        try {
            if (isOnly(nbt)) return TagParser.parseCompoundFully(nbt.substring(LEGACY_PREFIX.length()));
            return TagParser.parseCompoundFully(nbt).getCompound(KEY).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String strip(String nbt) {
        if (nbt == null || nbt.isBlank() || isOnly(nbt)) return null;
        try {
            CompoundTag base = TagParser.parseCompoundFully(nbt);
            base.remove(KEY);
            return base.toString();
        } catch (Exception ignored) {
            return nbt;
        }
    }

    private static CompoundTag capture(Entity entity, String nbt) {
        CompoundTag data = new CompoundTag();
        boolean used = false;

        // Upstream: AgeableMob - is baby.
        if (entity instanceof Ageable ageable) {
            data.putBoolean("flag2", ageable.getAge() < 0);
            used = true;
        }

        // Upstream: AbstractHorse carries the complete NMS data byte. This is deliberately
        // read from entity data instead of reconstructing only the known Bukkit flags.
        if (entity instanceof AbstractHorse horse) {
            data.putByte("byte1", horseFlags(horse));
            used = true;

            // Upstream stores Horse/Llama variant as int1. Prefer the live NMS value and
            // retain NBT as a compatibility fallback for servers where the accessor differs.
            Integer variant = null;
            if (entity instanceof Horse) {
                variant = invokeInteger(((CraftEntity) entity).getHandle(), "getTypeVariant");
            }
            if (entity instanceof Llama) {
                variant = llamaVariant(((CraftEntity) entity).getHandle());
            }
            if (variant == null) variant = readVariantFromNbt(nbt);
            if (variant != null) data.putInt("int1", variant);
        }

        // Camel is not an AbstractHorse in current Minecraft's class hierarchy, but upstream
        // records its dashing state in flag1.
        if (entity instanceof Camel camel) {
            Boolean dashing = invokeBoolean(((CraftEntity) camel).getHandle(), "isDashing");
            if (dashing != null) data.putBoolean("flag1", dashing);
            used = true;
        }

        // Upstream: AbstractChestedHorse - has chest.
        if (entity instanceof ChestedHorse chestedHorse) {
            data.putBoolean("flag1", chestedHorse.isCarryingChest());
            used = true;
        }

        // Upstream: Boat - paddle states, hurt time/direction, bubble timer, damage.
        if (entity instanceof Boat boat) {
            Object handle = ((CraftEntity) boat).getHandle();
            Boolean left = invokeBoolean(handle, "getPaddleState", 0);
            Boolean right = invokeBoolean(handle, "getPaddleState", 1);
            if (left != null) data.putBoolean("flag1", left);
            if (right != null) data.putBoolean("flag2", right);
            putInt(data, "int1", handle, "getHurtTime");
            putInt(data, "int2", handle, "getHurtDir");
            putInt(data, "int3", handle, "getBubbleTime");
            putFloat(data, "float1", handle, "getDamage");
            used = true;
        }

        // Important: the target upstream v1.4-alpha-10 implementation does NOT use
        // rollingAmplitude/rollingDirection here. It records AbstractMinecart's
        // hurt time, hurt direction and damage under int1/int2/float1.
        if (entity instanceof org.bukkit.entity.Minecart minecart) {
            Object handle = ((CraftEntity) minecart).getHandle();
            putInt(data, "int1", handle, "getHurtTime");
            putInt(data, "int2", handle, "getHurtDir");
            putFloat(data, "float1", handle, "getDamage");
            used = true;
        }

        // Upstream AbstractArrow (including trident) - is in ground.
        Object arrowHandle = ((CraftEntity) entity).getHandle();
        if (isNmsType(arrowHandle, "net.minecraft.world.entity.projectile.AbstractArrow")) {
            Boolean inGround = invokeBoolean(arrowHandle, "isInGround");
            if (inGround != null) data.putBoolean("flag1", inGround);
            used = true;
        }

        if (entity instanceof LivingEntity) used |= captureParticles(entity, data);
        return used ? data : null;
    }

    private static Integer readVariantFromNbt(String nbt) {
        try {
            if (nbt == null || nbt.isBlank()) return null;
            CompoundTag base = TagParser.parseCompoundFully(nbt);
            return base.contains("Variant") ? base.getIntOr("Variant", 0) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Integer llamaVariant(Object handle) {
        Object variant = invoke(handle, "getVariant");
        if (variant == null) return null;
        Integer id = invokeInteger(variant, "getId");
        if (id != null) return id;
        return invokeInteger(variant, "getId", handle);
    }

    public static void apply(Entity entity, CompoundTag data) {
        if (entity == null || data == null) return;
        try {
            // Upstream prevents the normal growth tick from resetting the synced baby flag.
            if (entity instanceof Ageable ageable && data.contains("flag2")) {
                if (ageable.getAge() < 0) ageable.setAdult();
                setAgeableBaby(entity, data.getBooleanOr("flag2", false));
            }

            if (entity instanceof AbstractHorse horse) {
                if (!data.contains("byte1")) return;
                byte flags = data.getByteOr("byte1", (byte) 0);
                setHorseFlags(horse, flags);

                // The saddle bit is part of ABSTRACT_HORSE_FLAGS in upstream and is also
                // mirrored into the horse inventory so the server state remains coherent.
                setHorseSaddle(horse, (flags & 0x04) != 0);

                if (data.contains("int1")) {
                    int variant = data.getIntOr("int1", 0);
                    if (horse instanceof Horse) {
                        invoke(((CraftEntity) horse).getHandle(), "setTypeVariant", variant);
                    } else if (horse instanceof Llama) {
                        setLlamaVariant(((CraftEntity) horse).getHandle(), variant);
                    }
                }

                if (horse instanceof ChestedHorse chestedHorse && data.contains("flag1")) {
                    chestedHorse.setCarryingChest(data.getBooleanOr("flag1", false));
                }
            }

            if (entity instanceof Camel camel && data.contains("flag1")) {
                invoke(((CraftEntity) camel).getHandle(), "setDashing", data.getBooleanOr("flag1", false));
            }

            if (entity instanceof Boat boat) {
                Object handle = ((CraftEntity) boat).getHandle();
                invoke(handle, "setPaddleState", data.getBooleanOr("flag1", false), data.getBooleanOr("flag2", false));
                setInt(handle, "setHurtTime", data, "int1");
                setInt(handle, "setHurtDir", data, "int2");
                setInt(handle, "setBubbleTime", data, "int3");
                if (data.contains("float1")) invoke(handle, "setDamage", data.getFloatOr("float1", 0));
            }

            if (entity instanceof org.bukkit.entity.Minecart minecart) {
                Object handle = ((CraftEntity) minecart).getHandle();
                setInt(handle, "setHurtTime", data, "int1");
                setInt(handle, "setHurtDir", data, "int2");
                if (data.contains("float1")) invoke(handle, "setDamage", data.getFloatOr("float1", 0));
            }

            Object arrowHandle = ((CraftEntity) entity).getHandle();
            if (data.contains("flag1") && isNmsType(arrowHandle, "net.minecraft.world.entity.projectile.AbstractArrow")) {
                invoke(arrowHandle, "setInGround", data.getBooleanOr("flag1", false));
            }

            if (entity instanceof LivingEntity) applyParticles(entity, data);
        } catch (Exception ignored) {
        }
    }

    private static void setAgeableBaby(Entity entity, boolean baby) {
        Object handle = ((CraftEntity) entity).getHandle();
        if (baby) {
            // setAge(-1) preserves the actual AgeableMob state; this mirrors the upstream
            // IS_BABY data accessor more closely than only changing the Bukkit age value.
            invoke(handle, "setAge", -1);
        } else {
            invoke(handle, "setAge", 0);
        }
    }

    private static byte horseFlags(AbstractHorse horse) {
        try {
            Object handle = ((CraftEntity) horse).getHandle();
            Object data = invoke(handle, "getEntityData");
            Field field = field(handle.getClass(), "DATA_ID_FLAGS");
            if (field == null) field = field(net.minecraft.world.entity.animal.equine.AbstractHorse.class, "DATA_ID_FLAGS");
            if (data != null && field != null) {
                Object value = invoke(data, "get", field.get(null));
                if (value instanceof Number number) return number.byteValue();
            }
        } catch (Exception ignored) {
        }

        // Compatibility fallback only. The NMS data accessor above is the authoritative path.
        byte flags = 0;
        if (horse.isTamed()) flags |= 2;
        if (!horse.getInventory().getSaddle().getType().isAir()) flags |= 4;
        if (horse.isEating()) flags |= 32;
        if (horse.isRearing()) flags |= 64;
        return flags;
    }

    private static void setHorseFlags(AbstractHorse horse, byte flags) {
        try {
            Object handle = ((CraftEntity) horse).getHandle();
            Object data = invoke(handle, "getEntityData");
            Field field = field(handle.getClass(), "DATA_ID_FLAGS");
            if (field == null) field = field(net.minecraft.world.entity.animal.equine.AbstractHorse.class, "DATA_ID_FLAGS");
            if (data != null && field != null) {
                invoke(data, "set", field.get(null), flags);
                return;
            }
        } catch (Exception ignored) {
        }

        horse.setTamed((flags & 2) != 0);
        horse.setEating((flags & 32) != 0);
        horse.setRearing((flags & 64) != 0);
    }

    private static void setHorseSaddle(AbstractHorse horse, boolean saddled) {
        try {
            Object handle = ((CraftEntity) horse).getHandle();
            Object inventory = invoke(handle, "getInventory");
            if (inventory != null) {
                invoke(inventory, "setItem", 0, saddled ? new ItemStack(Items.SADDLE) : ItemStack.EMPTY);
                return;
            }
        } catch (Exception ignored) {
        }
        // Bukkit fallback for Paper mappings where the NMS inventory accessor is unavailable.
        try {
            if (saddled) horse.getInventory().setSaddle(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SADDLE));
            else horse.getInventory().setSaddle(null);
        } catch (Exception ignored) {
        }
    }

    private static void setLlamaVariant(Object handle, int id) {
        try {
            Object variantClass = Class.forName("net.minecraft.world.entity.animal.equine.Llama$Variant");
            Method byId = ((Class<?>) variantClass).getDeclaredMethod("byId", int.class);
            byId.setAccessible(true);
            Object variant = byId.invoke(null, id);
            invoke(handle, "setVariant", variant);
        } catch (Exception ignored) {
        }
    }

    private static boolean captureParticles(Entity entity, CompoundTag data) {
        try {
            Object handle = ((CraftEntity) entity).getHandle();
            Object entityData = invoke(handle, "getEntityData");
            Field particleField = field(handle.getClass(), "DATA_EFFECT_PARTICLES");
            Field ambienceField = field(handle.getClass(), "DATA_EFFECT_AMBIENCE");
            if (particleField == null) particleField = field(net.minecraft.world.entity.LivingEntity.class, "DATA_EFFECT_PARTICLES");
            if (ambienceField == null) ambienceField = field(net.minecraft.world.entity.LivingEntity.class, "DATA_EFFECT_AMBIENCE");
            if (entityData == null || particleField == null || ambienceField == null) return false;

            Object particles = invoke(entityData, "get", particleField.get(null));
            Object ambience = invoke(entityData, "get", ambienceField.get(null));
            java.util.TreeSet<String> particleJsonSet = new java.util.TreeSet<>();
            if (particles instanceof List<?> particleList) {
                for (Object particle : particleList) {
                    try {
                        var json = ParticleTypes.CODEC.encodeStart(JsonOps.INSTANCE, (ParticleOptions) particle).result().orElse(null);
                        if (json != null) particleJsonSet.add(json.toString());
                    } catch (Exception ignored) {
                    }
                }
            }
            boolean ambient = ambience instanceof Boolean value && value;
            if (particleJsonSet.isEmpty() && !ambient) return false;

            ListTag list = new ListTag();
            for (String json : particleJsonSet) list.add(StringTag.valueOf(json));
            data.put("particles", list);
            data.putBoolean("ambience", ambient);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void applyParticles(Entity entity, CompoundTag data) {
        try {
            Object handle = ((CraftEntity) entity).getHandle();
            Object entityData = invoke(handle, "getEntityData");
            Field particleField = field(handle.getClass(), "DATA_EFFECT_PARTICLES");
            Field ambienceField = field(handle.getClass(), "DATA_EFFECT_AMBIENCE");
            if (particleField == null) particleField = field(net.minecraft.world.entity.LivingEntity.class, "DATA_EFFECT_PARTICLES");
            if (ambienceField == null) ambienceField = field(net.minecraft.world.entity.LivingEntity.class, "DATA_EFFECT_AMBIENCE");
            if (entityData == null || particleField == null || ambienceField == null) return;

            List<ParticleOptions> particles = new ArrayList<>();
            ListTag list = data.getList("particles").orElse(null);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    try {
                        String value = list.getString(i).orElse("");
                        if (value.isBlank()) continue;
                        var decoded = ParticleTypes.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseString(value)).result().orElse(null);
                        if (decoded != null) particles.add(decoded.getFirst());
                    } catch (Exception ignored) {
                    }
                }
            }
            invoke(entityData, "set", particleField.get(null), particles);
            if (data.contains("ambience")) invoke(entityData, "set", ambienceField.get(null), data.getBooleanOr("ambience", false));
        } catch (Exception ignored) {
        }
    }

    private static boolean isNmsType(Object handle, String className) {
        if (handle == null) return false;
        for (Class<?> current = handle.getClass(); current != null; current = current.getSuperclass()) {
            if (current.getName().equals(className)) return true;
        }
        return false;
    }

    private static Field field(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field value = current.getDeclaredField(name);
                value.setAccessible(true);
                return value;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static void putInt(CompoundTag data, String key, Object handle, String method) {
        Integer value = invokeInteger(handle, method);
        if (value != null) data.putInt(key, value);
    }

    private static void putFloat(CompoundTag data, String key, Object handle, String method) {
        Double value = invokeDouble(handle, method);
        if (value != null) data.putFloat(key, value.floatValue());
    }

    private static void setInt(Object handle, String method, CompoundTag data, String key) {
        if (data.contains(key)) invoke(handle, method, data.getIntOr(key, 0));
    }

    private static Boolean invokeBoolean(Object handle, String method, Object... args) {
        Object value = invoke(handle, method, args);
        return value instanceof Boolean bool ? bool : null;
    }

    private static Integer invokeInteger(Object handle, String method, Object... args) {
        Object value = invoke(handle, method, args);
        return value instanceof Number number ? number.intValue() : null;
    }

    private static Double invokeDouble(Object handle, String method, Object... args) {
        Object value = invoke(handle, method, args);
        return value instanceof Number number ? number.doubleValue() : null;
    }

    /** Reflection helper with parameter-compatible matching rather than name + arity only. */
    private static Object invoke(Object handle, String name, Object... args) {
        if (handle == null) return null;
        for (Class<?> type = handle.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
                Class<?>[] parameterTypes = method.getParameterTypes();
                boolean compatible = true;
                for (int i = 0; i < args.length; i++) {
                    if (!compatible(parameterTypes[i], args[i])) {
                        compatible = false;
                        break;
                    }
                }
                if (!compatible) continue;
                try {
                    method.setAccessible(true);
                    return method.invoke(handle, args);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static boolean compatible(Class<?> parameterType, Object value) {
        if (value == null) return !parameterType.isPrimitive();
        Class<?> valueType = value.getClass();
        if (!parameterType.isPrimitive()) return parameterType.isAssignableFrom(valueType);
        if (parameterType == boolean.class) return valueType == Boolean.class;
        if (parameterType == byte.class) return valueType == Byte.class;
        if (parameterType == short.class) return valueType == Short.class || valueType == Byte.class;
        if (parameterType == int.class) return valueType == Integer.class || valueType == Short.class || valueType == Byte.class;
        if (parameterType == long.class) return Number.class.isAssignableFrom(valueType);
        if (parameterType == float.class) return Number.class.isAssignableFrom(valueType);
        if (parameterType == double.class) return Number.class.isAssignableFrom(valueType);
        if (parameterType == char.class) return valueType == Character.class;
        return false;
    }
}
