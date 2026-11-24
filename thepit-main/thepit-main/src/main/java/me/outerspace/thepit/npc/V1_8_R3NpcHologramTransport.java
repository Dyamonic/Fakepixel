package me.outerspace.thepit.npc;

import org.bukkit.Location;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

final class V1_8_R3NpcHologramTransport {
    private final Constructor<?> armorStandConstructor;
    private final Constructor<?> spawnConstructor;
    private final Constructor<?> metadataConstructor;
    private final Constructor<?> teleportConstructor;
    private final Constructor<?> destroyConstructor;
    private final Method worldHandle;
    private final Method setLocation;
    private final Method setEntityId;
    private final Method getEntityId;
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

    V1_8_R3NpcHologramTransport() throws ReflectiveOperationException {
        Class<?> worldClass = Class.forName("net.minecraft.server.v1_8_R3.World");
        Class<?> entityClass = Class.forName("net.minecraft.server.v1_8_R3.Entity");
        Class<?> livingClass = Class.forName("net.minecraft.server.v1_8_R3.EntityLiving");
        Class<?> armorStandClass = Class.forName("net.minecraft.server.v1_8_R3.EntityArmorStand");
        Class<?> dataWatcherClass = Class.forName("net.minecraft.server.v1_8_R3.DataWatcher");
        Class<?> craftWorldClass = Class.forName("org.bukkit.craftbukkit.v1_8_R3.CraftWorld");
        armorStandConstructor = armorStandClass.getConstructor(worldClass);
        spawnConstructor = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving").getConstructor(livingClass);
        metadataConstructor = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata").getConstructor(int.class, dataWatcherClass, boolean.class);
        teleportConstructor = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport").getConstructor(entityClass);
        destroyConstructor = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy").getConstructor(int[].class);
        worldHandle = craftWorldClass.getMethod("getHandle");
        setLocation = entityClass.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
        setEntityId = entityClass.getMethod("d", int.class);
        getEntityId = entityClass.getMethod("getId");
        getUniqueId = entityClass.getMethod("getUniqueID");
        getDataWatcher = entityClass.getMethod("getDataWatcher");
        setCustomName = entityClass.getMethod("setCustomName", String.class);
        setCustomNameVisible = entityClass.getMethod("setCustomNameVisible", boolean.class);
        setInvisible = entityClass.getMethod("setInvisible", boolean.class);
        setGravity = armorStandClass.getMethod("setGravity", boolean.class);
        setSmall = armorStandClass.getMethod("setSmall", boolean.class);
        setBasePlate = armorStandClass.getMethod("setBasePlate", boolean.class);
        setMarker = armorStandClass.getMethod("n", boolean.class);
        getByte = dataWatcherClass.getMethod("getByte", int.class);
        watch = dataWatcherClass.getMethod("watch", int.class, Object.class);
        invulnerable = entityClass.getDeclaredField("invulnerable");
        invulnerable.setAccessible(true);
    }

    Line create(Location location, String text) throws ReflectiveOperationException {
        Object world = worldHandle.invoke(location.getWorld());
        Object entity = armorStandConstructor.newInstance(world);
        int entityId = PitPacketEntityEngine.shared().allocate().entityId();
        setEntityId.invoke(entity, entityId);
        setLocation.invoke(entity, location.getX(), location.getY(), location.getZ(), 0.0F, 0.0F);
        setCustomName.invoke(entity, text);
        setCustomNameVisible.invoke(entity, true);
        setInvisible.invoke(entity, true);
        setGravity.invoke(entity, true);
        setSmall.invoke(entity, true);
        setBasePlate.invoke(entity, true);
        setMarker.invoke(entity, true);
        invulnerable.setBoolean(entity, true);
        Object watcher = getDataWatcher.invoke(entity);
        byte entityFlags = ((Number) getByte.invoke(watcher, 0)).byteValue();
        watch.invoke(watcher, 0, (byte) (entityFlags | 0x20));
        watch.invoke(watcher, 2, text);
        watch.invoke(watcher, 3, (byte) 1);
        watch.invoke(watcher, 10, (byte) 0x1B);
        return new Line(entityId, (UUID) getUniqueId.invoke(entity), location.clone(), text, entity);
    }

    Object[] spawnPackets(Line line) throws ReflectiveOperationException {
        Object watcher = getDataWatcher.invoke(line.entity());
        return new Object[]{
                spawnConstructor.newInstance(line.entity()),
                metadataConstructor.newInstance(line.entityId(), watcher, true),
                teleportConstructor.newInstance(line.entity())
        };
    }

    Object teleportPacket(Line line, Location location) throws ReflectiveOperationException {
        setLocation.invoke(line.entity(), location.getX(), location.getY(), location.getZ(), 0.0F, 0.0F);
        line.location(location.clone());
        return teleportConstructor.newInstance(line.entity());
    }

    Object destroyPacket(int... entityIds) throws ReflectiveOperationException {
        return destroyConstructor.newInstance((Object) entityIds);
    }

    int entityId(Object entity) throws ReflectiveOperationException {
        return ((Number) getEntityId.invoke(entity)).intValue();
    }

    static final class Line {
        private final int entityId;
        private final UUID uuid;
        private Location location;
        private final String text;
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
        void location(Location value) { location = value; }
        String text() { return text; }
        Object entity() { return entity; }
    }
}
