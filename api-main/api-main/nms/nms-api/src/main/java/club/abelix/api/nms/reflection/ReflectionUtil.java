package club.abelix.api.nms.reflection;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;

@UtilityClass
public class ReflectionUtil {

    public void setField(Object instance, String name, Object value) {
        try {
            Class<?> cls = instance.getClass();

            Field field = cls.getDeclaredField(name);

            field.setAccessible(true);
            field.set(instance, value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getField(Object instance, String name) {
        try {
            Class<?> cls = instance.getClass();

            Field field = cls.getDeclaredField(name);
            field.setAccessible(true);

            return (T) field.get(instance);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    public void setStaticField(Class<?> cls, String name, Object value) {
        try {
            Field field = cls.getDeclaredField(name);

            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getStaticField(Class<?> cls, String name) {
        try {
            Field field = cls.getDeclaredField(name);
            field.setAccessible(true);

            return (T) field.get(null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
