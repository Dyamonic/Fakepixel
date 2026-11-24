package club.abelix.api.nms.v1_12.entity;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.FakeEntityApiImpl;
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
import club.abelix.api.nms.v1_12.entity.type.*;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.data.skin.exception.SkinException;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import static club.abelix.api.nms.v1_12.entity.FakeEntityImpl1_12.STRING_SERIALIZER;

public class FakeEntityApi1_12 extends FakeEntityApiImpl {

    private static final WrappedDataWatcher.WrappedDataWatcherObject CUSTOM_NAME_OBJECT
            = new WrappedDataWatcher.WrappedDataWatcherObject(2, STRING_SERIALIZER);

    public FakeEntityApi1_12(NmsApi nmsApi) {
        super(nmsApi);
    }

    @Override
    public FakeLivingEntity createLivingEntity(@NotNull Location location, @NotNull EntityType entityType) {
        return new FakeLivingEntity1_12(nmsApi, entityType, location);
    }

    @Override
    public FakeEntityWither createWither(@NotNull Location location) {
        FakeEntityWither fakeWither = new FakeWither1_12(nmsApi, location);
        registerEntity(fakeWither);

        return fakeWither;
    }

    @Override
    public FakeEntityVillager createVillager(@NotNull Location location) {
        FakeEntityVillager fakeVillager = new FakeVillager1_12(nmsApi, location);
        registerEntity(fakeVillager);

        return fakeVillager;
    }

    @Override
    public FakeEntityArmorStand createArmorStand(@NotNull Location location) {
        FakeEntityArmorStand fakeEntityArmorStand = new FakeArmorStand1_12(nmsApi, location);
        registerEntity(fakeEntityArmorStand);

        return fakeEntityArmorStand;
    }

    @Override
    public FakeEntityBlaze createBlaze(@NonNull Location location) {
        FakeEntityBlaze blaze = new FakeBlaze1_12(nmsApi, location);
        registerEntity(blaze);

        return blaze;
    }

    @Override
    public FakeEntitySheep createSheep(@NonNull Location location) {
        FakeEntitySheep sheep = new FakeSheep1_12(nmsApi, location);
        registerEntity(sheep);

        return sheep;
    }

    @Override
    public FakeEntitySkeleton createSkeleton(@NonNull Location location) {
        return null;
    }

    @Override
    public FakeEntityWolf createWolf(@NonNull Location location) {
        FakeEntityWolf wolf = new FakeWolf1_12(nmsApi, location);
        registerEntity(wolf);

        return wolf;
    }

    @Override
    public FakeEntityCreeper createCreeper(@NonNull Location location) {
        FakeEntityCreeper creeper = new FakeCreeper1_12(nmsApi, location);
        registerEntity(creeper);

        return creeper;
    }

    @Override
    public FakeEntityPlayer createPlayer(@NonNull Location location, @NonNull Skin skin) {
        return createPlayer(location, FakeEntityPlayer.generateRandomName(), skin);
    }

    @Override
    public FakeEntityPlayer createPlayer(@NotNull Location location, @NotNull String name, @NotNull Skin skin) {
        FakeEntityPlayer fakePlayer = new FakePlayer1_12(nmsApi, name, skin, location);
        registerEntity(fakePlayer);

        return fakePlayer;
    }

    @Override
    public FakeEntityItem createItem(@NotNull Location location, @NotNull ItemStack itemStack) {
        FakeEntityItem fakeItem = new FakeItem1_12(nmsApi, location);
        fakeItem.setItemStack(itemStack);

        registerEntity(fakeItem);

        return fakeItem;
    }

    @Override
    public FakeEntityItemFrame createItemFrame(@NotNull Location location, @NotNull BlockFace blockFace) {
        FakeEntityItemFrame fakeItemFrame = new FakeItemFrame1_12(nmsApi, location, blockFace);
        registerEntity(fakeItemFrame);

        return fakeItemFrame;
    }

    @Override
    public FakeEntityPlayer createPlayer(@NonNull @NotNull Location location, @NonNull @NotNull String skinName) {
        return createPlayer(location, FakeEntityPlayer.generateRandomName(), skinName);
    }

    @Override
    public FakeEntityPlayer createPlayer(@NotNull Location location, @NotNull String name, @NotNull String skinName) {
        try {
            FakeEntityPlayer fakePlayer = new FakePlayer1_12(nmsApi, name, skinName, location);
            registerEntity(fakePlayer);

            return fakePlayer;
        } catch (SkinException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public FakeEntityCorpse createCorpse(@NotNull Location location, @NotNull Skin skin) {
        FakeEntityCorpse fakePlayer = new FakeCorpse1_12(nmsApi, skin, location);
        registerEntity(fakePlayer);

        return fakePlayer;
    }

    @Override
    public FakeEntityCorpse createCorpse(@NotNull Location location, @NotNull String skinName) {
        try {
            FakeEntityCorpse fakePlayer = new FakeCorpse1_12(nmsApi, skinName, location);
            registerEntity(fakePlayer);

            return fakePlayer;
        } catch (SkinException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public FakeEntityGuardian createGuardian(@NonNull Location location) {
        FakeEntityGuardian fakeGuardian = new FakeGuardian1_12(nmsApi, location);
        registerEntity(fakeGuardian);

        return fakeGuardian;
    }

    @Override
    public FakeEntityGuardian createElderGuardian(@NonNull Location location) {
        FakeEntityGuardian fakeGuardian = new FakeElderGuardian1_12(nmsApi, location);
        registerEntity(fakeGuardian);

        return fakeGuardian;
    }

    @Override
    public String getEntityCustomName(@NotNull WrappedDataWatcher watcher) {
        Object value = watcher.getObject(CUSTOM_NAME_OBJECT);

        return value == null ? null : (String) value;
    }

    @Override
    public void setEntityCustomName(@NotNull WrappedDataWatcher watcher, String name) {
        watcher.setObject(CUSTOM_NAME_OBJECT, name);
    }
}
