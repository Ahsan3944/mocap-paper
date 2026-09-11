from pathlib import Path

p = Path("src/main/java/com/ultraop/mocap/recording/NonPlayerEntityData.java")
s = p.read_text()
start = s.index("    private static boolean captureParticles")
end = s.index("    private static void applyParticles", start)
replacement = '''    private static boolean captureParticles(Entity entity, CompoundTag data) {
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

'''
p.write_text(s[:start] + replacement + s[end:])
