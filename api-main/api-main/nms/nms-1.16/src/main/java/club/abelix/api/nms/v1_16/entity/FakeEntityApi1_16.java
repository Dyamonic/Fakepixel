package club.abelix.api.nms.v1_16.entity;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.FakeEntityApiImpl;
import club.abelix.api.nms.entity.pos.FakeEntityPose;
import club.abelix.api.nms.entity.type.FakeLivingEntity;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import club.abelix.api.nms.entity.type.creeper.FakeEntityCreeper;
import club.abelix.api.nms.entity.type.guardian.FakeEntityGuardian;
import club.abelix.api.nms.entity.type.object.FakeEntityItem;
import club.abelix.api.nms.entity.type.object.FakeEntityItemFrame;
import club.abelix.api.nms.entity.type.player.FakeEntityCorpse;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.nms.entity.type.skeleton.FakeEntitySkeleton;
import club.abelix.api.nms.entity.type.villager.FakeEntityVillager;
import club.abelix.api.nms.entity.type.wither.FakeEntityWither;
import club.abelix.api.nms.v1_16.entity.type.*;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.data.skin.exception.SkinException;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.NonNull;
import net.minecraft.server.v1_16_R3.ChatComponentText;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class FakeEntityApi1_16 extends FakeEntityApiImpl {

    private static final WrappedDataWatcher.Serializer CHAT_COMPONENT_SERIALIZER = WrappedDataWatcher.Registry.getChatComponentSerializer(true);

    private static final WrappedDataWatcher.WrappedDataWatcherObject CUSTOM_NAME_OBJECT
            = new WrappedDataWatcher.WrappedDataWatcherObject(2, CHAT_COMPONENT_SERIALIZER);

    public FakeEntityApi1_16(NmsApi nmsApi) {
        super(nmsApi);
    }

    @Override
    public FakeLivingEntity createLivingEntity(@NotNull Location location, @NotNull EntityType entityType) {
        return new FakeLivingEntity1_16(nmsApi, entityType, location);
    }

    @Override
    public FakeEntityWither createWither(@NotNull Location location) {
        FakeEntityWither fakeWither = new FakeWither1_16(nmsApi, location);
        registerEntity(fakeWither);

        return fakeWither;
    }

    @Override
    public FakeEntityVillager createVillager(@NotNull Location location) {
        FakeEntityVillager fakeVillager = new FakeVillager1_16(nmsApi, location);
        registerEntity(fakeVillager);

        return fakeVillager;
    }

    @Override
    public FakeEntityArmorStand createArmorStand(@NotNull Location location) {
        FakeEntityArmorStand fakeEntityArmorStand = new FakeArmorStand1_16(nmsApi, location);
        registerEntity(fakeEntityArmorStand);

        return fakeEntityArmorStand;
    }

    @Override
    public FakeEntityCreeper createCreeper(@NonNull Location location) {
        FakeEntityCreeper creeper = new FakeCreeper1_16(nmsApi, location);
        registerEntity(creeper);

        return creeper;
    }

    @Override
    public FakeEntitySkeleton createSkeleton(@NonNull Location location) {
        return null;
    }

    @Override
    public FakeEntityPlayer createPlayer(@NonNull Location location, @NonNull Skin skin) {
        return createPlayer(location, FakeEntityPlayer.generateRandomName(), skin);
    }

    @Override
    public FakeEntityPlayer createPlayer(@NonNull @NotNull Location location, @NonNull @NotNull String name, @NonNull @NotNull Skin skin) {
        FakeEntityPlayer fakePlayer = new FakePlayer1_16(nmsApi, name, skin, location);
        registerEntity(fakePlayer);

        return fakePlayer;
    }

    @Override
    public FakeEntityItem createItem(@NonNull @NotNull Location location, @NonNull @NotNull ItemStack itemStack) {
        FakeEntityItem fakeItem = new FakeItem1_16(nmsApi, location);
        fakeItem.setItemStack(itemStack);

        registerEntity(fakeItem);

        return fakeItem;
    }

    @Override
    public FakeEntityItemFrame createItemFrame(@NotNull Location location, @NotNull BlockFace blockFace) {
        FakeEntityItemFrame fakeItemFrame = new FakeItemFrame1_16(nmsApi, location, blockFace);
        registerEntity(fakeItemFrame);

        return fakeItemFrame;
    }

    @Override
    public FakeEntityPlayer createPlayer(@NonNull @NotNull Location location, @NonNull @NotNull String skinName) {
        return createPlayer(location, FakeEntityPlayer.generateRandomName(), skinName);
    }

    @Override
    public FakeEntityPlayer createPlayer(@NonNull @NotNull Location location, @NonNull @NotNull String name, @NonNull @NotNull String skinName) {
        try {
            FakeEntityPlayer fakePlayer = new FakePlayer1_16(nmsApi, name, skinName, location);
            registerEntity(fakePlayer);

            return fakePlayer;
        } catch (SkinException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public FakeEntityCorpse createCorpse(@NotNull Location location, @NotNull Skin skin) {
        FakeEntityCorpse fakePlayer = new FakeCorpse1_16(nmsApi, skin, location);
        ((FakeEntityImpl1_16) fakePlayer).setPose(FakeEntityPose.SLEEPING);

        registerEntity(fakePlayer);

        return fakePlayer;
    }

    @Override
    public FakeEntityCorpse createCorpse(@NotNull Location location, @NotNull String skinName) {
        try {
            FakeEntityCorpse fakePlayer = new FakeCorpse1_16(nmsApi, skinName, location);
            ((FakeEntityImpl1_16) fakePlayer).setPose(FakeEntityPose.SLEEPING);

            registerEntity(fakePlayer);

            return fakePlayer;
        } catch (SkinException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public FakeEntityGuardian createGuardian(@NonNull Location location) {
        FakeEntityGuardian fakeGuardian = new FakeGuardian1_16(nmsApi, location);
        registerEntity(fakeGuardian);

        return fakeGuardian;
    }

    @Override
    public FakeEntityGuardian createElderGuardian(@NonNull Location location) {
        FakeEntityGuardian fakeGuardian = new FakeElderGuardian1_16(nmsApi, location);
        registerEntity(fakeGuardian);

        return fakeGuardian;
    }

    @Override
    public String getEntityCustomName(@NotNull WrappedDataWatcher watcher) {
        Object object = watcher.getObject(CUSTOM_NAME_OBJECT);

        return object == null ? null : ((Optional<ChatComponentText>) object).map(ChatComponentText::getText)
                .orElse(null);
    }

    @Override
    public void setEntityCustomName(@NonNull @NotNull WrappedDataWatcher watcher, String name) {
        if (name == null) {
            watcher.remove(2);
            return;
        }

        Optional<?> opt = Optional.of(WrappedChatComponent.fromText(name).getHandle());
        watcher.setObject(CUSTOM_NAME_OBJECT, opt);
    }

    @Override
    public int getAndIncrementEntityId() {
        return ((AtomicInteger) ENTITY_ID.get(null)).incrementAndGet();
    }
}
