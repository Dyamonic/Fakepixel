package me.outerspace.thepit.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import me.outerspace.thepit.npc.PitPacketEntityEngine;

final class ActiveLeaderboardPacketTransport {
    private final Constructor<?> armorStandConstructor;
    private final Constructor<?> spawnConstructor;
    private final Constructor<?> metadataConstructor;
    private final Constructor<?> teleportConstructor;
    private final Constructor<?> destroyConstructor;
    private final Method worldHandle;
    private final Method playerHandle;
    private final Method setLocation;
    private final Method setEntityId;
    private final Method getUniqueId;
    private final Method getDataWatcher;
    private final Method setCustomName;
    private final Method setCustomNameVisible;
    private final Method setInvisible;
    private final Method setGravity;
    private final Method setSmall;
    private final Method setBasePlate;
    private final Method setMarker;
    private final Method getByte;
    private final Method watch;
    private final Field invulnerable;

    ActiveLeaderboardPacketTransport() throws ReflectiveOperationException {
        Class<?> worldClass = Class.forName("net.minecraft.server.v1_8_R3.World");
        Class<?> entityClass = Class.forName("net.minecraft.server.v1_8_R3.Entity");
        Class<?> livingClass = Class.forName("net.minecraft.server.v1_8_R3.EntityLiving");
        Class<?> armorStandClass = Class.forName("net.minecraft.server.v1_8_R3.EntityArmorStand");
        Class<?> watcherClass = Class.forName("net.minecraft.server.v1_8_R3.DataWatcher");
        Class<?> craftWorldClass = Class.forName("org.bukkit.craftbukkit.v1_8_R3.CraftWorld");
        Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer");
        armorStandConstructor = armorStandClass.getConstructor(worldClass);
        spawnConstructor = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving").getConstructor(livingClass);
        metadataConstructor = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata").getConstructor(int.class, watcherClass, boolean.class);
        teleportConstructor = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport").getConstructor(entityClass);
        destroyConstructor = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy").getConstructor(int[].class);
        worldHandle = craftWorldClass.getMethod("getHandle");
        playerHandle = craftPlayerClass.getMethod("getHandle");
        setLocation = entityClass.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
        setEntityId = entityClass.getMethod("d", int.class);
        getUniqueId = entityClass.getMethod("getUniqueID");
        getDataWatcher = entityClass.getMethod("getDataWatcher");
        setCustomName = entityClass.getMethod("setCustomName", String.class);
        setCustomNameVisible = entityClass.getMethod("setCustomNameVisible", boolean.class);
        setInvisible = entityClass.getMethod("setInvisible", boolean.class);
        setGravity = armorStandClass.getMethod("setGravity", boolean.class);
        setSmall = armorStandClass.getMethod("setSmall", boolean.class);
        setBasePlate = armorStandClass.getMethod("setBasePlate", boolean.class);
        setMarker = armorStandClass.getMethod("n", boolean.class);
        getByte = watcherClass.getMethod("getByte", int.class);
        watch = watcherClass.getMethod("watch", int.class, Object.class);
        invulnerable = entityClass.getDeclaredField("invulnerable");
        invulnerable.setAccessible(true);
    }

    Line create(Location location, String text) throws ReflectiveOperationException {
        Object entity = armorStandConstructor.newInstance(worldHandle.invoke(location.getWorld()));
        int entityId = PitPacketEntityEngine.shared().allocate().entityId();
        setEntityId.invoke(entity, entityId);
        setLocation.invoke(entity, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        setCustomName.invoke(entity, text);
        setCustomNameVisible.invoke(entity, true);
        setInvisible.invoke(entity, true);
        setGravity.invoke(entity, false);
        setSmall.invoke(entity, true);
        setBasePlate.invoke(entity, false);
        setMarker.invoke(entity, true);
        invulnerable.setBoolean(entity, true);
        Object watcher = getDataWatcher.invoke(entity);
        byte flags = ((Number) getByte.invoke(watcher, 0)).byteValue();
        watch.invoke(watcher, 0, (byte) (flags | 0x20));
        watch.invoke(watcher, 2, text);
        watch.invoke(watcher, 3, (byte) 1);
        watch.invoke(watcher, 10, (byte) 0x1B);
        return new Line(entityId, (UUID) getUniqueId.invoke(entity), location.clone(), text, entity);
    }

    void spawn(Player viewer, Line line) throws ReflectiveOperationException {
        send(viewer, spawnConstructor.newInstance(line.entity));
        send(viewer, metadataConstructor.newInstance(line.entityId, getDataWatcher.invoke(line.entity), true));
        send(viewer, teleportConstructor.newInstance(line.entity));
    }

    void update(Player viewer, Line line, String text) throws ReflectiveOperationException {
        setCustomName.invoke(line.entity, text);
        Object watcher = getDataWatcher.invoke(line.entity);
        watch.invoke(watcher, 2, text);
        watch.invoke(watcher, 3, (byte) 1);
        line.text = text;
        send(viewer, metadataConstructor.newInstance(line.entityId, watcher, true));
    }

    void destroy(Player viewer, CollectionLine lines) throws ReflectiveOperationException {
        int[] ids = lines.entityIds();
        if (ids.length > 0) send(viewer, destroyConstructor.newInstance((Object) ids));
        for (int id : ids) PitPacketEntityEngine.shared().release(id);
    }

    private void send(Player player, Object packet) throws ReflectiveOperationException {
        Object handle = playerHandle.invoke(player);
        Field connectionField = findField(handle.getClass(), "playerConnection", "connection");
        Object connection = connectionField.get(handle);
        Method send = findSendMethod(connection.getClass(), packet.getClass());
        send.invoke(connection, packet);
    }

    private Field findField(Class<?> type, String... names) throws NoSuchFieldException {
        for (String name : names) {
            Class<?> current = type;
            while (current != null) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
        }
        throw new NoSuchFieldException(String.join("/", names));
    }

    private Method findSendMethod(Class<?> type, Class<?> packetType) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom(packetType)) return method;
        }
        throw new NoSuchMethodException("sendPacket");
    }

    interface CollectionLine {
        int[] entityIds();
    }

    static final class Line {
        private final int entityId;
        private final UUID uuid;
        private final Location location;
        private String text;
        private final Object entity;

        private Line(int entityId, UUID uuid, Location location, String text, Object entity) {
            this.entityId = entityId;
            this.uuid = uuid;
            this.location = location;
            this.text = text;
            this.entity = entity;
        }

        int entityId() { return entityId; }
        UUID uuid() { return uuid; }
        Location location() { return location.clone(); }
        String text() { return text; }
    }
}
