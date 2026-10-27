package net.minecraftforge.fml.util;
import java.lang.reflect.Field;
public final class ObfuscationReflectionHelper {
    public static Field findField(Class<?> type, String name) {
        try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
}
