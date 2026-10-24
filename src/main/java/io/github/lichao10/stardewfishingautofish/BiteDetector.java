package io.github.lichao10.stardewfishingautofish;

import java.lang.reflect.Field;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/** Reads the client-synchronized bite flag owned by each supported bobber. */
public final class BiteDetector {
    private BiteDetector() {}
    private static final String LAVA_BOBBER =
            "com.scouter.netherdepthsupgrade.entity.entities.LavaFishingBobberEntity";
    private static final ClassValue<Field> FLAGS = new ClassValue<>() {
        @Override protected Field computeValue(Class<?> type) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                if (current.getName().equals(LAVA_BOBBER)) {
                    try {
                        // NDU updates this field from its own DATA_BITING in onSyncedDataUpdated.
                        // Its inherited vanilla biting field stays false, even when a fish bites.
                        Field field = current.getDeclaredField("bitingFish");
                        if (field.getType() != boolean.class)
                            throw new IllegalStateException("Unexpected NDU bite field type");
                        field.setAccessible(true);
                        return field;
                    } catch (NoSuchFieldException error) {
                        throw new IllegalStateException("Unsupported NDU bobber version", error);
                    }
                }
            }
            return ObfuscationReflectionHelper.findField(FishingHook.class, "f_37099_");
        }
    };

    public static boolean isBiting(FishingHook hook) throws IllegalAccessException {
        return FLAGS.get(hook.getClass()).getBoolean(hook);
    }
}
