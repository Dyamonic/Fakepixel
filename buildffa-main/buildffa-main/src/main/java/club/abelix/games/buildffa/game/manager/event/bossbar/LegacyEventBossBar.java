package club.abelix.games.buildffa.game.manager.event.bossbar;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class LegacyEventBossBar {
    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(2_000_000_000);
    private final Player player;
    private final UUID playerId;
    private final int entityId;
    private UUID worldId;
    private boolean pendingDestroy;
    @lombok.Getter
    private boolean spawned;
    private String currentTitle;
    private float currentHealth = Float.NaN;
    private String lastOperation;
    private Throwable lastFailure;

    public LegacyEventBossBar(Player player) {
        this.player = player;
        this.playerId = player.getUniqueId();
        this.entityId = NEXT_ENTITY_ID.getAndDecrement();
    }

    public boolean update(String title, float progress) {
        clearFailure();
        if (!canSend() || !player.getUniqueId().equals(playerId)) return fail("viewer validation", null);
        String safeTitle = title == null ? "" : title;
        float health = 300.0F;
        UUID currentWorld = player.getWorld().getUID();
        if (spawned && (pendingDestroy || !currentWorld.equals(worldId))) {
            if (!destroy()) return false;
            resetSpawnState();
        }
        Location location = bossLocation();
        if (!spawned && !spawn(location, safeTitle, health)) return false;
        if (!teleport(location)) return false;
        if (!safeTitle.equals(currentTitle) || Float.compare(health, currentHealth) != 0) {
            if (!metadata(safeTitle, health)) return false;
            currentTitle = safeTitle;
            currentHealth = health;
        }
        return true;
    }

    public void respawn() {
        remove();
    }

    public boolean remove() {
        clearFailure();
        if (!Bukkit.isPrimaryThread()) return fail("destroy", new IllegalStateException("Removal is off the main thread"));
        if (!spawned || !player.isOnline()) {
            resetSpawnState();
            return true;
        }
        pendingDestroy = true;
        if (!destroy()) return false;
        resetSpawnState();
        return true;
    }

    public void discard() {
        remove();
    }

    public boolean updatePosition(Location destination) {
        if (!canSend() || !spawned || pendingDestroy || destination == null
                || destination.getWorld() == null || !destination.getWorld().getUID().equals(worldId)) return true;
        return teleport(bossLocation(destination.clone().add(0.0D, player.getEyeHeight(), 0.0D)));
    }

    public String getLastOperation() {
        return lastOperation;
    }

    public Throwable getLastFailure() {
        return lastFailure;
    }

    private boolean spawn(Location location, String title, float health) {
        beginOperation("spawn");
        try {
            PacketContainer packet = packet(PacketType.Play.Server.SPAWN_ENTITY_LIVING, "spawn");
            StructureModifier<Integer> integers = packet.getIntegers();
            requireSize(integers.size(), 5, "spawn integer fields");
            integers.write(0, entityId);
            StructureModifier<EntityType> entityTypes = packet.getEntityTypeModifier();
            if (entityTypes.size() > 0) entityTypes.write(0, EntityType.WITHER);
            else integers.write(1, (int) EntityType.WITHER.getTypeId());
            integers.write(2, fixed(location.getX()));
            integers.write(3, fixed(location.getY()));
            integers.write(4, fixed(location.getZ()));
            StructureModifier<WrappedDataWatcher> watchers = packet.getDataWatcherModifier();
            requireSize(watchers.size(), 1, "spawn data watcher fields");
            watchers.write(0, watcher(title, health));
            spawned = true;
            worldId = player.getWorld().getUID();
            pendingDestroy = true;
            if (!send(packet, "spawn")) return false;
            pendingDestroy = false;
            if (!metadata(title, health)) return false;
            currentTitle = title;
            currentHealth = health;
            return true;
        } catch (RuntimeException exception) {
            return preserveFailure("spawn", exception);
        }
    }

    private boolean metadata(String title, float health) {
        beginOperation("metadata");
        try {
            PacketContainer packet = packet(PacketType.Play.Server.ENTITY_METADATA, "metadata");
            requireSize(packet.getIntegers().size(), 1, "metadata integer fields");
            requireSize(packet.getWatchableCollectionModifier().size(), 1, "metadata collection fields");
            packet.getIntegers().write(0, entityId);
            packet.getWatchableCollectionModifier().write(0, watcher(title, health).getWatchableObjects());
            return send(packet, "metadata");
        } catch (RuntimeException exception) {
            return preserveFailure("metadata", exception);
        }
    }

    private boolean teleport(Location location) {
        beginOperation("teleport");
        try {
            PacketContainer packet = packet(PacketType.Play.Server.ENTITY_TELEPORT, "teleport");
            requireSize(packet.getIntegers().size(), 4, "teleport integer fields");
            packet.getIntegers().write(0, entityId)
                    .write(1, fixed(location.getX()))
                    .write(2, fixed(location.getY()))
                    .write(3, fixed(location.getZ()));
            return send(packet, "teleport");
        } catch (RuntimeException exception) {
            return preserveFailure("teleport", exception);
        }
    }

    private boolean destroy() {
        beginOperation("destroy");
        try {
            PacketContainer packet = packet(PacketType.Play.Server.ENTITY_DESTROY, "destroy");
            requireSize(packet.getIntegerArrays().size(), 1, "destroy integer array fields");
            packet.getIntegerArrays().write(0, new int[]{entityId});
            return send(packet, "destroy");
        } catch (RuntimeException exception) {
            return preserveFailure("destroy", exception);
        }
    }

    private PacketContainer packet(PacketType type, String operation) {
        try {
            PacketContainer packet = new PacketContainer(type);
            packet.getModifier().writeDefaults();
            return packet;
        } catch (RuntimeException exception) {
            fail(operation, exception);
            throw exception;
        }
    }

    private WrappedDataWatcher watcher(String title, float health) {
        try {
            WrappedDataWatcher watcher = new WrappedDataWatcher();
            watcher.setObject(0, Byte.valueOf((byte) 0x20));
            watcher.setObject(2, title);
            watcher.setObject(3, (byte) 1);
            watcher.setObject(6, health);
            watcher.setObject(17, 0);
            watcher.setObject(18, 0);
            watcher.setObject(19, 0);
            watcher.setObject(20, 0);
            return watcher;
        } catch (RuntimeException exception) {
            fail(lastOperation, exception);
            throw exception;
        }
    }

    private void resetSpawnState() {
        spawned = false;
        pendingDestroy = false;
        worldId = null;
        currentTitle = null;
        currentHealth = Float.NaN;
    }

    private void clearFailure() {
        lastOperation = null;
        lastFailure = null;
    }

    private void beginOperation(String operation) {
        lastOperation = operation;
        lastFailure = null;
    }

    private boolean fail(String operation, Throwable failure) {
        lastOperation = operation;
        lastFailure = failure;
        return false;
    }

    private boolean preserveFailure(String operation, RuntimeException failure) {
        return lastFailure != failure && fail(operation, failure);
    }

    private Location bossLocation() {
        return bossLocation(player.getEyeLocation());
    }

    private Location bossLocation(Location eye) {
        Vector direction = eye.getDirection();
        if (direction.lengthSquared() < 0.0001D) direction = new Vector(0.0D, 0.0D, 1.0D);
        return eye.clone().add(direction.normalize().multiply(40.0D));
    }

    private int fixed(double value) {
        return floor(value * 32.0D);
    }

    private int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private void requireSize(int actual, int required, String field) {
        if (actual < required) throw new IllegalStateException(field + " expected " + required + " but found " + actual);
    }

    private boolean canSend() {
        return Bukkit.isPrimaryThread() && player.isOnline();
    }

    private boolean send(PacketContainer packet, String operation) {
        if (!canSend()) return fail(operation, new IllegalStateException("Viewer is offline or packet send is off the main thread"));
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet, false);
            return true;
        } catch (InvocationTargetException | RuntimeException exception) {
            return fail(operation, exception);
        }
    }
}
