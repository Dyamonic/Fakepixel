package club.abelix.api.nms.entity;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.listener.FakeEntityListener;
import club.abelix.api.nms.entity.type.FakeEntity;
import club.abelix.api.nms.entity.type.FakeLivingEntity;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import club.abelix.api.nms.entity.type.blaze.FakeEntityBlaze;
import club.abelix.api.nms.entity.type.creeper.FakeEntityCreeper;
import club.abelix.api.nms.entity.type.guardian.FakeEntityGuardian;
import club.abelix.api.nms.entity.type.object.FakeEntityItem;
import club.abelix.api.nms.entity.type.object.FakeEntityItemFrame;
import club.abelix.api.nms.entity.type.player.FakeEntityCorpse;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.nms.entity.type.sheep.FakeEntitySheep;
import club.abelix.api.nms.entity.type.skeleton.FakeEntitySkeleton;
import club.abelix.api.nms.entity.type.villager.FakeEntityVillager;
import club.abelix.api.nms.entity.type.wither.FakeEntityWither;
import club.abelix.api.nms.entity.type.wolf.FakeEntityWolf;
import club.abelix.commons.data.skin.Skin;
import com.comphenix.protocol.reflect.ExactReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public interface FakeEntityApi {

    // -----------------------------------------------------------------------

    FieldAccessor ENTITY_ID = getFieldAccessor(
            MinecraftReflection.getEntityClass(), "entityCount", true);

    // -----------------------------------------------------------------------

    default void registerFakeEntityListener(@NonNull NmsApi nmsApi) {
        FakeEntityListener fakeEntityListener = new FakeEntityListener(nmsApi);
        nmsApi.getPlugin().getServer().getPluginManager().registerEvents(fakeEntityListener, nmsApi.getPlugin());
    }

    /**
     * Получить всех энтити, которые трекаются
     *
     * @return - энтити, которые трекаются
     */
    Collection<FakeEntity> getTrackedEntities();

    /**
     * Зарегистрировать энтити как трекаемую
     *
     * @param fakeEntity - id энтити
     */
    void registerEntity(@NonNull FakeEntity fakeEntity);

    /**
     * Удалить трекаемую энтити
     *
     * @param fakeEntity - энтити
     */
    void removeEntity(@NonNull FakeEntity fakeEntity);

    /**
     * Удалить все трекаемые энтити
     */
    void removeEntities();

    /**
     * Получить трекаемую энтити по ID
     *
     * @param fakeEntityId - id энтити
     * @return - трекаемая энтити
     */
    FakeEntity getFakeEntity(int fakeEntityId);

    /**
     * Создать энтити определенного типа на локации
     *
     * @param location   - локация энтити
     * @param entityType - тип энтити
     * @return - созданная фейковая энтити
     */
    FakeLivingEntity createLivingEntity(@NonNull Location location, @NonNull EntityType entityType);

    FakeEntityWither createWither(@NonNull Location location);

    /**
     * Создать энтити-жителя на локации
     *
     * @param location - локация жителя
     * @return - созданный житель
     */
    FakeEntityVillager createVillager(@NonNull Location location);

    /**
     * Создать армор стенд на локации
     *
     * @param location - локация арморстенда
     * @return - созданный арморстенд
     */
    FakeEntityArmorStand createArmorStand(@NonNull Location location);

    /**
     * Создать блейза на опр. локации
     *
     * @param location - локация блейза
     * @return - созданный блейз
     */
    FakeEntityBlaze createBlaze(@NonNull Location location);

    /**
     * Создать волка на опр. локации
     *
     * @param location - локация волка
     * @return - созданный волк
     */
    FakeEntityWolf createWolf(@NonNull Location location);

    /**
     * Создать крипера на опр. локации
     *
     * @param location - локация крипера
     * @return - созданный крипер
     */
    FakeEntityCreeper createCreeper(@NonNull Location location);

    /**
     * Создать овцу на опр. локации
     *
     * @param location - локация овцы
     * @return - созданная овца
     */
    FakeEntitySheep createSheep(@NonNull Location location);

    /**
     * Создать скелета на опр. локации
     *
     * @param location - локация овцы
     * @return - созданная овца
     */
    FakeEntitySkeleton createSkeleton(@NonNull Location location);

    /**
     * Создать NPC игрока на локации
     *
     * @param location - локация игрока
     * @param skin     - скин NPC
     * @return - созданный NPC
     */
    FakeEntityPlayer createPlayer(@NonNull Location location, @NonNull Skin skin);

    /**
     * Создать NPC игрока на локации
     *
     * @param location - локация игрока
     * @param name     - имя NPC
     * @param skin     - скин NPC
     *
     * @return - созданный NPC
     */
    FakeEntityPlayer createPlayer(@NonNull Location location, @NonNull String name, @NonNull Skin skin);

    /**
     * Создать NPC игрока на локации
     *
     * @param location - локация арморстенда
     * @param skinName - имя скина NPC
     * @param name     - имя NPC
     *
     * @return - созданный NPC
     */
    FakeEntityPlayer createPlayer(@NonNull Location location, @NonNull String name, @NonNull String skinName);

    /**
     * Создать NPC игрока на локации
     *
     * @param location - локация арморстенда
     * @param skinName - имя скина NPC
     *
     * @return - созданный NPC
     */
    FakeEntityPlayer createPlayer(@NonNull Location location, @NonNull String skinName);

    /**
     * Создать NPC трупа на локации
     *
     * @param location - локация игрока
     * @param skin     - скин NPC
     * @return - созданный NPC
     */
    FakeEntityCorpse createCorpse(@NonNull Location location, @NonNull Skin skin);

    /**
     * Создать NPC трупа на локации
     *
     * @param location - локация игрока
     * @param skinName - скин NPC
     * @return - созданный NPC
     */
    FakeEntityCorpse createCorpse(@NonNull Location location, @NonNull String skinName);

    /**
     * Создать энтити-стража на локации
     *
     * @param location - локация стража
     * @return - созданный страж
     */
    FakeEntityGuardian createGuardian(@NonNull Location location);

    /**
     * Создать энтити-старшего стража на локации
     *
     * @param location - локация старшего стража
     * @return - созданный старший страж
     */
    FakeEntityGuardian createElderGuardian(@NonNull Location location);

    FakeEntityItemFrame createItemFrame(@NonNull Location location, @NonNull BlockFace blockFace);

    FakeEntityItem createItem(@NonNull Location location, @NonNull ItemStack itemStack);

    String getEntityCustomName(@NonNull WrappedDataWatcher watcher);

    void setEntityCustomName(@NonNull WrappedDataWatcher watcher, String name);

    WrappedDataWatcher cloneDataWatcher(@NonNull WrappedDataWatcher original);

    default int getAndIncrementEntityId() {
        val id = (int) ENTITY_ID.get(null);
        ENTITY_ID.set(null, id + 1);

        return id;
    }

    static FieldAccessor getFieldAccessor(Class<?> instanceClass, String fieldName, boolean forceAccess) {
        return Accessors.getFieldAccessor(ExactReflection.fromClass(instanceClass, forceAccess).getField(fieldName));
    }

}