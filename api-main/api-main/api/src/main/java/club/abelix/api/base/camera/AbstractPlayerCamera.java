package club.abelix.api.base.camera;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.camera.event.CameraDestroyEvent;
import club.abelix.api.base.camera.state.PlayerState;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.version.GameVersion;
import club.abelix.core.bukkit.HeartBeat;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

@Getter
@RequiredArgsConstructor
public abstract class AbstractPlayerCamera implements PlayerCamera {

    protected static final WrappedDataWatcher.Serializer BYTE_SERIALIZER = WrappedDataWatcher.Registry.get(Byte.class);

    private static final int CAMERA_OFFSET = 75000;
    private static final PacketLibraryApi API = FalconAPI.getNmsApi().getPacketLibraryApi();

    protected final Player player;
    protected final Gamer gamer;

    protected final Location initialPosition;
    protected PlayerState oldPlayerState;
    protected Location position;
    protected BukkitTask cameraTask;

    public AbstractPlayerCamera(Player player, Location initialPosition) {
        this.player = player;
        this.gamer = Gamer.getGamer(player);
        this.initialPosition = initialPosition;
    }

    protected abstract void runTick();

    @Override
    public void move(Location position) {
        this.position = position;

        updateLocation();
    }

    public void onStart() {
    }

    public void onDestroy() {
    }

    @Override
    public void start() {
        // создаём энтити, в который будет вселяться игрок
        spawnEntity();

        // создаём таск для двигания камеры
        this.cameraTask = HeartBeat.runTimerSync(() -> {
            // игрок нажал шифт
            if (player.isSneaking()) {
                PlayerCameraManager.IMP.removeCamera(player, CameraDestroyReason.PLAYER_SNEAK);
                return;
            }

            runTick();
        }, 1, 1);

        // сохраняем статус игрока, чтоб потом вернуть
        this.oldPlayerState = new PlayerState(player);

        // обновляем инвентарь, чтобы PlayerCameraPacketListener отправил игроку пустой инвентарь
        this.player.updateInventory();

        // даём игроку невидимость
        this.player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                Integer.MAX_VALUE, 1, false, false));

        // скрываем игрока
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hidePlayer(this.player);
        }

        // даём игроку флай, чтобы его не кикнуло
        this.player.setAllowFlight(true);
        this.player.setFlying(true);

        // отправляем игроку пакет вселения в энтити
        setCamera();

        this.onStart();
    }

    @Override
    public final void destroy(CameraDestroyReason reason) {
        // убираем камеру игрока
        unsetCamera();

        // удаляем энтити
        destroyEntity();

        // удаляем таск
        if (cameraTask != null) {
            cameraTask.cancel();
            cameraTask = null;
        }

        // возвращаем игроку предыдущие значения
        if (oldPlayerState != null) {
            this.oldPlayerState.apply(gamer);
            this.oldPlayerState = null;
        }

        // показываем игрока
        for (Gamer gamer : Gamer.getGamers()) {
            if (!this.gamer.isVanished() || gamer.getRanks().hasPermission("lobby.hide.bypass")) {
                gamer.getPlayer().showPlayer(this.player);
            }
        }

        // обновляем инвентарь, чтобы вернуть игроку инвентарь
        this.player.updateInventory();

        // вызываем эвент, например, чтобы скайблок создал остров доконца, если игрок вышел или нажал шифт
        Bukkit.getPluginManager().callEvent(new CameraDestroyEvent(this, reason));

        onDestroy();
    }

    private Location fixPosition() {
        return position == null ? initialPosition : position;
    }

    private void setCamera() {
        API.sendPacket(player, API.getCamera(player.getEntityId() + CAMERA_OFFSET));
    }

    private void unsetCamera() {
        API.sendPacket(player, API.getCamera(player.getEntityId()));
    }

    private void spawnEntity() {
        WrappedDataWatcher dataWatcher = new WrappedDataWatcher();

        dataWatcher.setObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), (byte) 0x20
        );

        API.sendPacket(player, API.getSpawnLivingEntityPacket(
                player.getEntityId() + CAMERA_OFFSET, EntityType.ARMOR_STAND,
                fixPosition(), dataWatcher
        ));
    }

    private void updateLocation() {
        FalconAPI.getNmsApi().getPlayerApi().setLocation(player, position);

        if (gamer.getVersion().getVersion() >= GameVersion.MINECRAFT_1_13.getVersion()) {
            API.sendPacket(player, API.getHeadRotationPacket(player.getEntityId() + CAMERA_OFFSET, position.getYaw()));
        }

        API.sendPacket(player, API.getEntityTeleportPacket(player.getEntityId() + CAMERA_OFFSET, position));
    }

    private void destroyEntity() {
        API.sendPacket(player, API.getEntityDestroyPacket(
                player.getEntityId() + CAMERA_OFFSET
        ));
    }

}
