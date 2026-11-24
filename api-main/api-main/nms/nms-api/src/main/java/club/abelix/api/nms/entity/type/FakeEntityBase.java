package club.abelix.api.nms.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.NmsConstant;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.animation.FakeEntityAnimation;
import club.abelix.api.nms.entity.pos.FakeEntityPose;
import club.abelix.api.nms.packet.AbstractVisibleObject;
import club.abelix.api.nms.packet.PacketLibraryApi;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class FakeEntityBase extends AbstractVisibleObject implements FakeEntity {

    protected final NmsApi nmsApi;

    private final Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap = new HashMap<>();

    @Getter
    private final WrappedDataWatcher dataWatcher = new WrappedDataWatcher();

    @Getter
    private final int entityId;

    @Getter
    private final EntityType entityType;

    @Getter
    private Location location;

    private BukkitTask lookPlayersTask;

    //пассажир (энтити которая сидит сверху)
    @Getter
    protected FakeEntity passenger;

    @Getter
    protected int passengerId;

    @Getter
    private String customName;

    @Getter
    @Setter
    protected double visibilityDistance = 40 * 40;

    //кешированный пакет
    private final PacketContainer entityDestroyPacket;

    protected FakeEntityBase(@NonNull NmsApi nmsApi, @NonNull EntityType entityType, @NonNull Location location) {
        this(nmsApi, nmsApi.getFakeEntityApi().getAndIncrementEntityId(), entityType, location);
    }

    protected FakeEntityBase(@NonNull NmsApi nmsApi, int entityId, @NonNull EntityType entityType, @NonNull Location location) {
        this.nmsApi = nmsApi;

        this.entityType = entityType;
        this.entityId = entityId;

        this.location = location;

        this.entityDestroyPacket = nmsApi.getPacketLibraryApi().getEntityDestroyPacket(entityId);
    }

    @Override
    public void setPassenger(@NotNull FakeEntity passenger) {
        this.passenger = passenger;

        getReceivers().forEach(passenger::addReceiver);

        passenger.teleport(location);
    }

    @Override
    public void setPassenger(int entityId) {
        this.passengerId = entityId;

        broadcastPacket(nmsApi.getPacketLibraryApi().getEntityAttachmentPacket(entityId, this.entityId));
    }

    @Override
    public ItemStack getEquipment(EnumWrappers.@NotNull ItemSlot itemSlot) {
        return equipmentMap.get(itemSlot);
    }

    @Override
    public void setEquipment(EnumWrappers.@NotNull ItemSlot itemSlot, @NotNull ItemStack itemStack) {
        equipmentMap.put(itemSlot, itemStack);

        broadcastPacket(nmsApi.getPacketLibraryApi().getEntityEquipmentPacket(entityId, itemSlot, itemStack));
    }

    @Override
    public void setCustomName(String customName) {
        if (Objects.equals(this.customName, customName)) {
            return;
        }

        this.customName = customName;

        FakeEntityApi entityApi = nmsApi.getFakeEntityApi();
        PacketLibraryApi packetApi = nmsApi.getPacketLibraryApi();

        for (Player player : viewers) {
            WrappedDataWatcher dataWatcher = this.dataWatcher;

            if (customName != null) {
                dataWatcher = entityApi.cloneDataWatcher(dataWatcher);

                entityApi.setEntityCustomName(dataWatcher, customName);
            }

            packetApi.sendPacket(player, packetApi.getDataWatcherPacket(entityId, dataWatcher));
        }
    }

    @Override
    public boolean addReceivers(@NonNull @NotNull Collection<? extends Player> players) {
        boolean add = super.addReceivers(players);

        if (add) {
            int x = getLocation().getChunk().getX();
            int z = getLocation().getChunk().getZ();

            for (Player player : players) {
                if (player.getWorld() == getLocation().getWorld()
                        && nmsApi.getPlayerApi().canSeeChunk(player, x, z)) {
                    this.addViewer(player);
                }
            }
        }

        return add;
    }

    @Override
    public boolean removeReceivers(@NonNull @NotNull Collection<? extends Player> players) {
        boolean remove = super.removeReceivers(players);

        if (remove) {
            destroyEntity(players);
        }

        return remove;
    }

    @Override
    public FakeEntityPose getEntityPose() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroyEntity(@NotNull Collection<? extends Player> players) {
        for (Player player : players) {
            if (passenger != null) {
                passenger.removeReceiver(player);
            }

            nmsApi.getPacketLibraryApi().sendPacket(player, entityDestroyPacket);
        }
    }

    @Override
    public void spawnEntity(@NotNull Collection<? extends Player> players) {
        PacketLibraryApi packetApi = nmsApi.getPacketLibraryApi();
        FakeEntityApi entityApi = nmsApi.getFakeEntityApi();

        // отправляем пакет спавна игроку, соответственно этот метод может быть переопределен
        List<PacketContainer> spawnPackets = new LinkedList<>();
        addSpawnEntityPackets(packetApi, spawnPackets);

        // custom name null - datawatcher можно отправить один всем,
        // иначе будем для каждого сетать свой
        PacketContainer dataWatcherPacket = customName == null ?
                packetApi.getDataWatcherPacket(entityId, dataWatcher) :
                null;

        players.forEach(player -> {
            spawnPackets.forEach(spawnPacket -> packetApi.sendPacket(player, spawnPacket));

            if (dataWatcherPacket == null) {
                WrappedDataWatcher dataWatcher = entityApi.cloneDataWatcher(this.dataWatcher);
                entityApi.setEntityCustomName(dataWatcher, customName);

                packetApi.sendPacket(player, packetApi.getDataWatcherPacket(entityId, dataWatcher));
            } else {
                packetApi.sendPacket(player, dataWatcherPacket);
            }
        });

        // если пассажир сидит на энтити - даем возможность игроку видеть пассажира
        // а затем отправляем пакет "ПРИСОЕДИНЕНИЯ"
        if (passenger != null) {
            passenger.addReceivers(players);

            PacketContainer attachmentPacket = packetApi.getEntityAttachmentPacket(passenger.getEntityId(), entityId);
            players.forEach(player -> packetApi.sendPacket(player, attachmentPacket));
        } else if (passengerId != 0) {
            PacketContainer attachmentPacket = packetApi.getEntityAttachmentPacket(passengerId, entityId);
            players.forEach(player -> packetApi.sendPacket(player, attachmentPacket));
        }

        // отправляем пакет equipment игроку
        for (EnumWrappers.ItemSlot itemSlot : equipmentMap.keySet()) {
            PacketContainer equipmentPacket
                    = packetApi.getEntityEquipmentPacket(entityId, itemSlot, equipmentMap.get(itemSlot));

            players.forEach(player -> packetApi.sendPacket(player, equipmentPacket));
        }
    }

    @Override
    public boolean hasReceiver(@NotNull Player player) {
        return receivers.contains(player);
    }

    @Override
    public boolean addViewer(@NotNull Player player) {
        boolean add = super.addViewer(player);

        if (add) {
            spawnEntity(Collections.singleton(player));
        }

        return add;
    }

    @Override
    public boolean removeViewer(@NotNull Player player) {
        boolean remove = super.removeViewer(player);

        if (remove) {
            destroyEntity(Collections.singleton(player));
        }

        return remove;
    }

    @Override
    public void setPublicStatus(boolean publicStatus) {
        super.setPublicStatus(publicStatus);

        // если энтити стал публичным и отправка не кастомная
        if (publicStatus && !NmsConstant.c_customEntitySending) {
            addReceivers(Bukkit.getOnlinePlayers());
        }
    }

    @Override
    public void teleport(@NotNull Location location) {
        this.location = location;

        PacketLibraryApi packetApi = nmsApi.getPacketLibraryApi();

        PacketContainer teleportPacket = packetApi.getEntityTeleportPacket(entityId, location);

        for (Player player : viewers) {
            packetApi.sendPacket(player, teleportPacket);
        }

        //viewers.forEach(player -> packetApi.sendPacket(player, teleportPacket));
    }

    @Override
    public void look(@NotNull Player player) {
        Vector direction = location.toVector().subtract(player.getLocation().toVector());
        direction.multiply(-1);

        Location location = direction.toLocation(this.location.getWorld());
        location.setDirection(direction);

        PacketLibraryApi packetApi = nmsApi.getPacketLibraryApi();

        packetApi.sendPacket(player, packetApi.getHeadRotationPacket(entityId, location.getYaw()));
        packetApi.sendPacket(player, packetApi.getEntityLookPacket(entityId, location));
    }

    @Override
    public void look(float yaw, float pitch) {
        Preconditions.checkState(lookPlayersTask == null, "NPC is always rotating to all players. How you want to rotate it to another location?!");

        location.setYaw(yaw);
        location.setPitch(pitch);

        PacketLibraryApi packetApi = nmsApi.getPacketLibraryApi();

        PacketContainer headRotationPacket = packetApi.getHeadRotationPacket(entityId, location.getYaw());
        PacketContainer entityLookPacket = packetApi.getEntityLookPacket(entityId, location);

        for (Player player : viewers) {
            packetApi.sendPacket(player, entityLookPacket);
            packetApi.sendPacket(player, headRotationPacket);
        }
    }

    private void updateLook() {
        for (Player player : viewers) {
            look(player);
        }
    }

    @Override
    public void startPlayerLooker(int delay) {
//        if (lookPlayersTask != null) {
//            lookPlayersTask.cancel();
//            lookPlayersTask = null;
//        }
//
//        if (delay > 0) {
//            this.lookPlayersTask = Bukkit.getScheduler().runTaskTimerAsynchronously(nmsApi.getPlugin(),
//                    this::updateLook, 0, delay);
//        }
    }

    @Override
    public void playAnimation(@NotNull FakeEntityAnimation entityAnimation) {
        broadcastPacket(nmsApi.getPacketLibraryApi().getAnimationPacket(getEntityId(), entityAnimation));
    }

    protected void broadcastDataWatcherPacket() {
        broadcastPacket(nmsApi.getPacketLibraryApi().getDataWatcherPacket(getEntityId(), getDataWatcher()));
    }

    @Override
    public void playAnimation(@NotNull Player player, @NotNull FakeEntityAnimation entityAnimation) {
        PacketLibraryApi packetApi = nmsApi.getPacketLibraryApi();

        packetApi.sendPacket(player, packetApi.getAnimationPacket(getEntityId(), entityAnimation));
    }

    /**
     * Получить пакет спавна энтити
     */
    protected void addSpawnEntityPackets(@NonNull PacketLibraryApi packetApi, @NonNull Collection<PacketContainer> result) {
        //по дефолту отправляем spawn living entity
        result.add(getSpawnEntityPacket(packetApi));
    }

    protected PacketContainer getSpawnEntityPacket(@NonNull PacketLibraryApi packetApi) {
        return packetApi.getSpawnLivingEntityPacket(entityId, entityType, location, dataWatcher);
    }

    /**
     * Отправить пакет всем тем, кто видит эту энтити
     *
     * @param packetContainer - пакет который отправлять
     */
    protected void broadcastPacket(@NonNull PacketContainer packetContainer) {
        viewers.forEach(player -> nmsApi.getPacketLibraryApi().sendPacket(player, packetContainer));
    }

    @Override
    public void remove() {
        //удаляем энтити из списка
        nmsApi.getFakeEntityApi().removeEntity(this);

        //удаляем энтити для всех кто видит ее сейчас
        destroyEntity(viewers);
        destroyEntity(receivers);

        //очищаем все нужные листы
        receivers.clear();
        viewers.clear();

        if (lookPlayersTask != null) {
            lookPlayersTask.cancel();
        }
    }
}
