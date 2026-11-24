package club.abelix.api.nms.v1_8.entity;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.FakeEntityApiImpl;
import club.abelix.api.nms.entity.type.FakeLivingEntity;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
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
import club.abelix.api.nms.v1_8.entity.type.*;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.data.skin.exception.SkinException;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class FakeEntityApi1_8 extends FakeEntityApiImpl {

    public FakeEntityApi1_8(NmsApi nmsApi) {
        super(nmsApi);
    }

    @Override
    public FakeLivingEntity createLivingEntity(@NotNull Location location, @NotNull EntityType entityType) {
        FakeLivingEntity entity = new FakeLivingEntity1_8(nmsApi, entityType, location);
        registerEntity(entity);
        return entity;
    }

    @Override
    public FakeEntityWither createWither(@NotNull Location location) {
        FakeEntityWither fakeWither = new FakeWither1_8(nmsApi, location);
        registerEntity(fakeWither);

        return fakeWither;
    }

    @Override
    public FakeEntityVillager createVillager(@NotNull Location location) {
        FakeEntityVillager fakeVillager = new FakeVillager1_8(nmsApi, location);
        registerEntity(fakeVillager);

        return fakeVillager;
    }

    @Override
    public FakeEntityArmorStand createArmorStand(@NotNull Location location) {
        FakeEntityArmorStand fakeEntityArmorStand = new FakeArmorStand1_8(nmsApi, location);
        registerEntity(fakeEntityArmorStand);

        return fakeEntityArmorStand;
    }

    @Override
    public FakeEntityCreeper createCreeper(@NonNull Location location) {
        FakeEntityCreeper creeper = new FakeCreeper1_8(nmsApi, location);
        registerEntity(creeper);

        return creeper;
    }

    @Override
    public FakeEntitySheep createSheep(@NonNull Location location) {
        FakeEntitySheep sheep = new FakeSheep1_8(nmsApi, location);
        registerEntity(sheep);

        return sheep;
    }

    @Override
    public FakeEntitySkeleton createSkeleton(@NonNull Location location) {
        FakeEntitySkeleton skeleton = new FakeSkeleton1_8(nmsApi, location);
        registerEntity(skeleton);

        return skeleton;
    }

    @Override
    public FakeEntityPlayer createPlayer(@NonNull Location location, @NonNull Skin skin) {
        return createPlayer(location, FakeEntityPlayer.generateRandomName(), skin);
    }

    @Override
    public FakeEntityPlayer createPlayer(@NotNull Location location, @NotNull String name, @NotNull Skin skin) {
        FakeEntityPlayer fakePlayer = new FakePlayer1_8(nmsApi, name, skin, location);
        registerEntity(fakePlayer);

        return fakePlayer;
    }

    @Override
    public FakeEntityItem createItem(@NotNull Location location, @NotNull ItemStack itemStack) {
        FakeEntityItem fakeItem = new FakeItem1_8(nmsApi, location);
        fakeItem.setItemStack(itemStack);

        registerEntity(fakeItem);

        return fakeItem;
    }

    @Override
    public FakeEntityItemFrame createItemFrame(@NotNull Location location, @NotNull BlockFace blockFace) {
        FakeEntityItemFrame fakeItemFrame = new FakeItemFrame1_8(nmsApi, location, blockFace);
        registerEntity(fakeItemFrame);

        return fakeItemFrame;
    }

    @Override
    public FakeEntityPlayer createPlayer(@NotNull Location location, @NotNull String skinName) {
        return createPlayer(location, FakeEntityPlayer.generateRandomName(), skinName);
    }

    @Override
    public FakeEntityPlayer createPlayer(@NotNull Location location, @NotNull String name, @NotNull String skinName) {
        try {
            FakeEntityPlayer fakePlayer = new FakePlayer1_8(nmsApi, name, skinName, location);
            registerEntity(fakePlayer);

            return fakePlayer;
        } catch (SkinException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public FakeEntityCorpse createCorpse(@NotNull Location location, @NotNull Skin skin) {
        FakeEntityCorpse fakePlayer = new FakeCorpse1_8(nmsApi, skin, location);
        registerEntity(fakePlayer);

        return fakePlayer;
    }

    @Override
    public FakeEntityCorpse createCorpse(@NotNull Location location, @NotNull String skinName) {
        try {
            FakeEntityCorpse fakePlayer = new FakeCorpse1_8(nmsApi, skinName, location);
            registerEntity(fakePlayer);

            return fakePlayer;
        } catch (SkinException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public FakeEntityGuardian createGuardian(@NonNull Location location) {
        FakeEntityGuardian fakeGuardian = new FakeGuardian1_8(nmsApi, location);
        registerEntity(fakeGuardian);

        return fakeGuardian;
    }

    @Override
    public FakeEntityGuardian createElderGuardian(@NonNull Location location) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public String getEntityCustomName(@NotNull WrappedDataWatcher watcher) {
        Object value = watcher.getObject(2);

        return value == null ? null : (String) value;
    }

    @Override
    public void setEntityCustomName(@NotNull WrappedDataWatcher watcher, String name) {
        if (name == null) {
            watcher.remove(2);
            return;
        }

        watcher.setObject(2, name);
    }
}
