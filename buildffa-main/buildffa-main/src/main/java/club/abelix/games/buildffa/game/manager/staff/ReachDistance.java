package club.abelix.games.buildffa.game.manager.staff;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class ReachDistance {
    static final double MAX_MELEE_REACH = 3.1D;
    private static final ClassValue<Method> HANDLES = new ClassValue<Method>() {
        @Override
        protected Method computeValue(Class<?> type) {
            return method(type, "getHandle");
        }
    };
    private static final ClassValue<Method> BOXES = new ClassValue<Method>() {
        @Override
        protected Method computeValue(Class<?> type) {
            return method(type, "getBoundingBox");
        }
    };
    private static final ClassValue<Field[]> COORDINATES = new ClassValue<Field[]>() {
        @Override
        protected Field[] computeValue(Class<?> type) {
            try {
                return new Field[]{type.getField("a"), type.getField("b"), type.getField("c"),
                        type.getField("d"), type.getField("e"), type.getField("f")};
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot read the 1.8 entity bounding box", exception);
            }
        }
    };
    private static final ClassValue<Field> CONNECTIONS = new ClassValue<Field>() {
        @Override
        protected Field computeValue(Class<?> type) {
            try {
                return type.getField("playerConnection");
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot read the 1.8 player connection", exception);
            }
        }
    };
    private static final ClassValue<Field> MOVEMENT_CHECKS = new ClassValue<Field>() {
        @Override
        protected Field computeValue(Class<?> type) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                try {
                    Field field = current.getDeclaredField("checkMovement");
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                }
            }
            throw new IllegalStateException("Cannot read the 1.8 teleport acknowledgement state");
        }
    };

    private ReachDistance() {
    }

    private static Method method(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot access entity " + name, exception);
        }
    }

    static double measure(Location eye, Entity victim) throws ReflectiveOperationException {
        Object handle = HANDLES.get(victim.getClass()).invoke(victim);
        Object box = BOXES.get(handle.getClass()).invoke(handle);
        Field[] coordinates = COORDINATES.get(box.getClass());
        return toBox(eye.getX(), eye.getY(), eye.getZ(), coordinates[0].getDouble(box),
                coordinates[1].getDouble(box), coordinates[2].getDouble(box), coordinates[3].getDouble(box),
                coordinates[4].getDouble(box), coordinates[5].getDouble(box));
    }

    static boolean teleportPending(Player player) throws ReflectiveOperationException {
        Object handle = HANDLES.get(player.getClass()).invoke(player);
        Object connection = CONNECTIONS.get(handle.getClass()).get(handle);
        return connection == null || !MOVEMENT_CHECKS.get(connection.getClass()).getBoolean(connection);
    }

    static boolean exceedsLimit(double distance) {
        return distance > MAX_MELEE_REACH;
    }

    static double toBox(double x, double y, double z, double minX, double minY, double minZ,
                        double maxX, double maxY, double maxZ) {
        for (double coordinate : new double[]{x, y, z, minX, minY, minZ, maxX, maxY, maxZ})
            if (!Double.isFinite(coordinate)) throw new IllegalArgumentException("Non-finite reach coordinate");
        if (minX > maxX || minY > maxY || minZ > maxZ)
            throw new IllegalArgumentException("Invalid entity bounding box");
        double dx = Math.max(0.0D, Math.max(minX - x, x - maxX));
        double dy = Math.max(0.0D, Math.max(minY - y, y - maxY));
        double dz = Math.max(0.0D, Math.max(minZ - z, z - maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
