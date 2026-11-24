package club.abelix.api.nms.packet;

import club.abelix.api.nms.entity.animation.FakeEntityAnimation;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.nms.scoreboard.objective.BoardObjective;
import club.abelix.api.nms.scoreboard.objective.ObjectiveActionMode;
import club.abelix.api.nms.scoreboard.objective.ObjectiveDisplayType;
import club.abelix.api.nms.scoreboard.objective.ObjectiveType;
import club.abelix.api.nms.scoreboard.score.ScoreActionMode;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.api.nms.team.action.TeamAction;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public interface PacketLibraryApi {

    ProtocolManager PROTOCOL_MANAGER = ProtocolLibrary.getProtocolManager();

    /**
     * Отправить пакет игроку
     *
     * @param player - игрок
     * @param packet - пакет
     */
    void sendPacket(@NonNull Player player, @NonNull Object packet);

    NmsPlayOutMapChunk wrapChunk(PacketContainer container);

    NmsPlayOutMapChunkBulk wrapChunkBulk(PacketContainer container);

    PacketContainer getEntityEffect(int entityId, int potionId, int amplifier, int duration, boolean hideParticles);

    PacketContainer getRemoveEntityEffect(int entityId, int potionId);

    /**
     * Получить Equipment-пакет для энтити
     *
     * @param itemSlot  - слот
     * @param itemStack - предмет
     */
    PacketContainer getEntityEquipmentPacket(int entityId, @NonNull EnumWrappers.ItemSlot itemSlot, @NonNull ItemStack itemStack);


    default PacketContainer getCamera(int cameraId) {
        PacketContainer container
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.CAMERA);

        container.getIntegers().write(0, cameraId);

        return container;
    }

    /**
     * Получить пакет телепорта энтити на локацию
     *
     * @param entityId - id энтити
     * @param location - новая локация
     */
    PacketContainer getEntityTeleportPacket(int entityId, @NonNull Location location);

    /**
     * Получить пакет изменения направления куда энтити будет смотреть
     *
     * @param entityId - id энтити
     * @param location - локация куда смотрит (берется только yaw/pitch)
     */
    PacketContainer getEntityLookPacket(int entityId, @NonNull Location location);

    /**
     * Получить пакет поворота головы энтити
     *
     * @param entityId - id энтити
     * @param yaw      - yaw, куда поворачивать
     */
    PacketContainer getHeadRotationPacket(int entityId, float yaw);

    /**
     * Получить пакет спавна living entity
     *
     * @param entityId    - id энтити
     * @param spawnTypeId - тип энтити
     * @param entityData  - данные энтити
     * @param location    - локация энтити
     * @param velocity    - вектор
     */
    PacketContainer getSpawnEntityPacket(int entityId,
                                         int spawnTypeId,
                                         int entityData,
                                         @NonNull Location location,
                                         @NonNull Vector velocity);

    /**
     * Получить пакет спавна entity
     *
     * @param entityId    - id энтити
     * @param entityType  - тип энтити
     * @param location    - локация энтити
     * @param dataWatcher - дата вотчер
     */
    PacketContainer getSpawnLivingEntityPacket(int entityId,
                                               @NonNull EntityType entityType,
                                               @NonNull Location location,
                                               @NonNull WrappedDataWatcher dataWatcher);

    /**
     * Получить пакет спавна для игроков
     *
     * @param entityId - id энтити
     * @param uuid     - uuid игрока
     * @param location - локация энтити
     */
    PacketContainer getSpawnNamedEntityPacket(int entityId,
                                              @NonNull UUID uuid,
                                              @NonNull Location location,
                                              @NonNull WrappedDataWatcher dataWatcher);

    PacketContainer getPlayerInfoPacket(@NonNull EnumWrappers.PlayerInfoAction action,
                                        @NonNull FakeEntityPlayer fakeEntityPlayer);

    /**
     * Получить пакет установки пассажира для энтити
     *
     * @param attachedEntityId - пассажир
     * @param holdingEntityId  - тот, на ком будет этот пассажир
     */
    PacketContainer getEntityAttachmentPacket(int attachedEntityId, int holdingEntityId);

    /**
     * Получить пакет скорборд тимы
     *
     * @param teamAction - действие
     * @param fakeTeam   - тима для пакета
     */
    PacketContainer getTeamPacket(@NonNull TeamAction teamAction, @NonNull FakeTeam fakeTeam, @NotNull List<String> players);

    default PacketContainer getAnimationPacket(int entityId, @NonNull FakeEntityAnimation animation) {
        PacketContainer packetContainer = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ANIMATION);

        packetContainer.getIntegers().write(0, entityId);
        packetContainer.getIntegers().write(1, animation.getAnimationId());

        return packetContainer;
    }

    /**
     * Получить пакет кровати
     */
    default PacketContainer getBedPacket(int entityId, int bedX, int bedY, int bedZ) {
        PacketContainer packetContainer = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.BED);

        packetContainer.getIntegers().write(0, entityId);

        packetContainer.getBlockPositionModifier().write(0, new BlockPosition(bedX, bedY, bedZ));

        return packetContainer;
    }

    /**
     * Получить пакет партиклов
     */
    default PacketContainer getParticlePacket(@NonNull String particleName, int count, float speed,
                                              int[] data, boolean longDistance, @NonNull Location center,
                                              float offsetX, float offsetY, float offsetZ) {

        PacketContainer packetContainer = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.WORLD_PARTICLES);

        packetContainer.getParticles().write(0, EnumWrappers.Particle.getByName(particleName));

        packetContainer.getFloat().write(0, (float) center.getX());
        packetContainer.getFloat().write(1, (float) center.getY());
        packetContainer.getFloat().write(2, (float) center.getZ());

        packetContainer.getFloat().write(3, offsetX);
        packetContainer.getFloat().write(4, offsetY);
        packetContainer.getFloat().write(5, offsetZ);

        packetContainer.getFloat().write(6, speed);

        packetContainer.getIntegers().write(0, count);

        packetContainer.getBooleans().write(0, longDistance);

        if (data != null) {
            packetContainer.getIntegerArrays().write(0, data);
        }

        return packetContainer;
    }

    default PacketContainer getScoreboardScorePacket(@NonNull BoardObjective objective,
                                                     @NonNull String text,
                                                     @NonNull ScoreActionMode actionMode,
                                                     int score) {

        PacketContainer packetContainer = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SCOREBOARD_SCORE);

        packetContainer.getStrings().write(0, text);
        packetContainer.getStrings().write(1, objective.getName());

        if (actionMode == ScoreActionMode.CHANGE) {
            packetContainer.getIntegers().write(0, score);
        }

        packetContainer.getScoreboardActions().write(0, EnumWrappers.ScoreboardAction.valueOf(actionMode.name()));

        return packetContainer;
    }

    /**
     * Получить пакет Scoreboard display-objective
     *
     * @param objective   - objective скорборда
     * @param displayType - режим
     */
    default PacketContainer getScoreboardDisplayObjectivePacket(@NonNull BoardObjective objective,
                                                                @NonNull ObjectiveDisplayType displayType) {

        PacketContainer packetContainer = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE);

        packetContainer.getIntegers().write(0, displayType.ordinal());

        packetContainer.getStrings().write(0, objective.getName());

        return packetContainer;
    }

    /**
     * Получить пакет Scoreboard-objective
     *
     * @param objective           - objective скорборда
     * @param objectiveActionMode - режим
     */
    default PacketContainer getScoreboardObjectivePacket(@NonNull BoardObjective objective,
                                                         @NonNull ObjectiveActionMode objectiveActionMode) {

        PacketContainer packetContainer = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);

        packetContainer.getStrings().write(0, objective.getName());

        packetContainer.getIntegers().write(0, objectiveActionMode.ordinal());

        if (objectiveActionMode == ObjectiveActionMode.CREATE || objectiveActionMode == ObjectiveActionMode.UPDATE) {
            packetContainer.getStrings().write(1, objective.getDisplayName());
            packetContainer.getEnumModifier(ObjectiveType.class, 2).write(0, objective.getObjectiveType());
        }

        return packetContainer;
    }

    /**
     * Получить пакет анимации энтити
     *
     * @param entityId        - id энтити
     * @param entityAnimation - анимация
     */
    default PacketContainer getEntityAnimationPacket(int entityId, FakeEntityAnimation entityAnimation) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ANIMATION);

        packetContainer.getIntegers().write(0, entityId);
        packetContainer.getIntegers().write(1, entityAnimation.ordinal());

        return packetContainer;
    }

    default PacketContainer getTitlePacket(@NonNull EnumWrappers.TitleAction titleAction,
                                           @NonNull String title,
                                           int fadeIn, int fadeOut, int stay) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.TITLE);

        packetContainer.getTitleActions().write(0, titleAction);

        packetContainer.getChatComponents().write(0, WrappedChatComponent.fromText(title));

        packetContainer.getIntegers().write(0, fadeIn);
        packetContainer.getIntegers().write(1, stay);
        packetContainer.getIntegers().write(2, fadeOut);

        return packetContainer;
    }

    /**
     * Получить пакет удаления энтити
     *
     * @param entityIds - id энтити
     *                  <p>
     *                  По дефолту одинаковый во всех версиях поддерживаемых нами
     */
    default PacketContainer getEntityDestroyPacket(int... entityIds) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_DESTROY);

        packetContainer.getIntegerArrays().write(0, entityIds);

        return packetContainer;
    }

    /**
     * Получить пакет Datawatcher для энтити
     *
     * @param entityId    - id энтити
     * @param dataWatcher - дата вотчер
     *                    <p>
     *                    По дефолту одинаковый во всех версиях поддерживаемых нами
     */
    default PacketContainer getDataWatcherPacket(int entityId, @NonNull WrappedDataWatcher dataWatcher) {
        PacketContainer packetContainer
                = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_METADATA);

        packetContainer.getIntegers().write(0, entityId);
        packetContainer.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());

        return packetContainer;
    }

}
