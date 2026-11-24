package club.abelix.api.nms.packet;

import java.lang.reflect.Field;

public abstract class NmsPacket {

    protected boolean getBooleanField(String name, Object instance) {
        try {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);

            return field.getBoolean(instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected int getIntField(String name, Object instance) {
        try {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);

            return field.getInt(instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T getField(Class<T> type, String name, Object instance) {
        try {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);

            return type.cast(field.get(instance));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
