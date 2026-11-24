package club.abelix.api.nms.v1_8.world;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.border.BorderColor;
import club.abelix.api.nms.entity.custom.fireball.NmsFireball;
import club.abelix.api.nms.nbt.tag.CompoundTag;
import club.abelix.api.nms.v1_8.entity.custom.fireball.NmsFireball1_8;
import club.abelix.api.nms.world.WorldApi;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftTNTPrimed;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_8_R3.metadata.BlockMetadataStore;
import org.bukkit.craftbukkit.v1_8_R3.metadata.EntityMetadataStore;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataStoreBase;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class WorldApi1_8 implements WorldApi {

    private final NmsApi api;

    private static Field metadataMapField;
    private static Field entitiesByIdField;
    private static Field primedTntSourceField;

    private static Map<String, IntHashMap<net.minecraft.server.v1_8_R3.Entity>> byEntityId = new HashMap<>();
    private static Map<String, Map<Plugin, MetadataValue>> entityMetadataMap = null;
    private static Map<String, Map<String, Map<Plugin, MetadataValue>>> blockMetadataMap = new HashMap<>();

    private static void loadWorld(World world) {
        BlockMetadataStore store = ((CraftWorld) world).getBlockMetadata();

        try {
            blockMetadataMap.put(world.getName(), (Map<String, Map<Plugin, MetadataValue>>) metadataMapField.get(store));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    static {
        try {
            metadataMapField = MetadataStoreBase.class.getDeclaredField("metadataMap");
            metadataMapField.setAccessible(true);

            for (World world : Bukkit.getWorlds()) {
                BlockMetadataStore store = ((CraftWorld) world).getBlockMetadata();

                blockMetadataMap.put(world.getName(), (Map<String, Map<Plugin, MetadataValue>>) metadataMapField.get(store));
            }

            primedTntSourceField = EntityTNTPrimed.class.getDeclaredField("source");
            primedTntSourceField.setAccessible(true);

            EntityMetadataStore store = ((CraftServer) Bukkit.getServer()).getEntityMetadata();

            entityMetadataMap = (Map<String, Map<Plugin, MetadataValue>>) metadataMapField.get(store);

            entitiesByIdField = net.minecraft.server.v1_8_R3.World.class.getDeclaredField("entitiesById");
            entitiesByIdField.setAccessible(true);

            for (World world : Bukkit.getWorlds()) {
                loadEntities(world);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static void loadEntities(World world) {
        String worldName = world.getName();

        CraftWorld craftWorld = (CraftWorld) world;
        WorldServer server = craftWorld.getHandle();

        try {
            byEntityId.put(worldName, (IntHashMap<net.minecraft.server.v1_8_R3.Entity>) entitiesByIdField.get(server));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setBlockTag(Block block, CompoundTag tag) {
        CraftWorld world = ((CraftWorld) block.getWorld());
        TileEntity tile = world.getTileEntityAt(block.getX(), block.getY(), block.getZ());

        tile.a((NBTTagCompound) api.getNbtApi().toNMS(tag));
    }

    @Override
    public CompoundTag getBlockTag(Block block) {
        CraftWorld world = ((CraftWorld) block.getWorld());
        TileEntity tile = world.getTileEntityAt(block.getX(), block.getY(), block.getZ());

        NBTTagCompound nmsTag = new NBTTagCompound();
        tile.b(nmsTag);

        return (CompoundTag) api.getNbtApi().fromNMS(nmsTag);
    }

    @Override
    public void leakWorldUnload(@NonNull World world) {
/*        CraftWorld craftWorld = ((CraftWorld) world);
        WorldServer worldServer = craftWorld.getHandle();

        for ( Field field : WorldServer.class.getDeclaredFields() ){

            try {
                field.set( worldServer, null );
            } catch ( IllegalAccessException e ) {
                e.printStackTrace();
            }

        }

        // Хуй знает, тотальная писька дьявола

        worldServer = null;
        craftWorld = null;
        world = null;

        System.gc();*/

    }

    @Override
    public void loadWorldEntities(World world) {
        long start = System.currentTimeMillis();

        System.out.println("[World] Loading world entities: " + world.getName());
        loadEntities(world);
        System.out.println("[World] Entities loaded for " + (System.currentTimeMillis() - start) + " millis");
    }

    @Override
    public void unloadWorldEntities(World world) {
        System.out.println("[World] Unloading world entities: " + world.getName());

        byEntityId.remove(world.getName());
    }

    @Override
    public NmsFireball launchFireball(Player player, Location location, Vector vector) {
        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        NmsFireball1_8 fireball = new NmsFireball1_8(craftWorld.getHandle(), (CraftPlayer) player, location, vector);

        craftWorld.addEntity(fireball, CreatureSpawnEvent.SpawnReason.CUSTOM);

        return fireball;
    }

    @Override
    public void removeEntityMetadata(Entity entity) {
        String entityUuid = entity.getUniqueId().toString();

        entityMetadataMap.entrySet().removeIf(entry -> entry.getKey().startsWith(entityUuid));
    }

    @Override
    public void removeBlockMetadata(Block block) {
        String metadataKey = String.format("%d:%d:%d", block.getX(), block.getY(), block.getZ());

        Map<String, Map<Plugin, MetadataValue>> map = blockMetadataMap.get(block.getWorld().getName());

        if (map == null) {
            loadWorld(block.getWorld());

            map = blockMetadataMap.get(block.getWorld().getName());
        }

        map.entrySet().removeIf(entry -> entry.getKey().startsWith(metadataKey));
    }

    @Override
    public void setChestTitle(Chest chest, String title) {
        IInventory inventory = ((CraftInventory) chest.getInventory()).getInventory();

        if (inventory instanceof TileEntityChest) {
            ((TileEntityChest) inventory).a(title);
        }
    }

    @Override
    public void setTntSource(TNTPrimed tnt, Player source) {
        EntityTNTPrimed entityTNTPrimed = ((CraftTNTPrimed) tnt).getHandle();

        try {
            primedTntSourceField.set(entityTNTPrimed, ((CraftPlayer) source).getHandle());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setChestOpenState(Chest chest, boolean open) {
        Location location = chest.getLocation();
        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition position = new BlockPosition(location.getX(), location.getY(), location.getZ());
        TileEntityChest tileChest = (TileEntityChest) world.getTileEntity(position);

        if (tileChest == null) {
            return;
        }

        world.playBlockAction(position, tileChest.w(), 1, open ? 1 : 0);
    }

    @Override
    public void preloadEntity(World world, Object object) {
        net.minecraft.server.v1_8_R3.Entity nmsEntity = (net.minecraft.server.v1_8_R3.Entity) object;

        IntHashMap<net.minecraft.server.v1_8_R3.Entity> entities = byEntityId.get(world.getName());
        entities.a(nmsEntity.getId(), nmsEntity);
    }

    @Override
    public <T extends Entity> T getCraftEntityById(World world, int entityId) {
        net.minecraft.server.v1_8_R3.Entity nmsEntity = getNmsEntityById(world, entityId);

        return nmsEntity == null ? null : (T) nmsEntity.getBukkitEntity();
    }

    @Override
    public <T> T getNmsEntityById(World world, int entityId) {
        IntHashMap<net.minecraft.server.v1_8_R3.Entity> entities = byEntityId.get(world.getName());

        if (entities == null) {
            return null;
        }

        net.minecraft.server.v1_8_R3.Entity entity = entities.get(entityId);

        return entity == null ? null : (T) entity;
    }

    @Override
    public void spawnDeathParticles(Entity entity) {
        net.minecraft.server.v1_8_R3.Entity handle = ((CraftEntity) entity).getHandle();

        handle.world.broadcastEntityEffect(handle, (byte) 3);
    }

    @Override
    public void modifySpawnerType(@NonNull ItemStack egg, @NonNull EntityType type) {
        egg.setDurability((short) egg.getTypeId());
    }

    @Override
    public void sendWorldBorder(@NonNull Player player, @NonNull Location center,
                                int size, @NonNull BorderColor borderColor, int delay) {
        Bukkit.getScheduler().runTaskLater(api.getPlugin(), () -> sendWorldBorder(player, center, size, borderColor), delay);
    }

    @Override
    public void sendWorldBorder(@NonNull Player player, @NonNull Location center,
                                int size, @NonNull BorderColor borderColor) {
        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        WorldBorder border = new CustomBorder(borderColor);
        border.setCenter(center.getBlockX(), center.getBlockZ());
        border.setSize(size);
        border.world = ((CraftWorld) center.getWorld()).getHandle();

        connection.sendPacket(
                new PacketPlayOutWorldBorder(border, PacketPlayOutWorldBorder.EnumWorldBorderAction.INITIALIZE)
        );
    }

    @AllArgsConstructor
    private static class CustomBorder extends WorldBorder {

        private static final double VAL = 0.00001d;

        private final BorderColor color;

        @Override
        public double j() { // new size
            double size = getSize();

            return color == BorderColor.BLUE
                    ? size // BLUE
                    : (color == BorderColor.GREEN
                    ? size + VAL // GREEN
                    : size - VAL); // RED
        }

        @Override
        public long i() {
            return color == BorderColor.BLUE ? 0 : Long.MAX_VALUE;
        }
    }


}
