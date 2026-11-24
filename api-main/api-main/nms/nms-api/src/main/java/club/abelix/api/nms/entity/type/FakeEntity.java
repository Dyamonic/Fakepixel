package club.abelix.api.nms.entity.type;

import club.abelix.api.nms.entity.animation.FakeEntityAnimation;
import club.abelix.api.nms.entity.pos.FakeEntityPose;
import club.abelix.api.nms.packet.IVisibleObject;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public interface FakeEntity extends IVisibleObject {

    double getVisibilityDistance();

    void  setVisibilityDistance(double value);

    EntityType getEntityType();

    int getEntityId();

    Location getLocation();

    FakeEntity getPassenger();

    int getPassengerId();

    void setPassenger(@NonNull FakeEntity fakeEntity);

    void setPassenger(int entityId);

    void removePassenger();

    void spawnEntity(@NonNull Collection<? extends Player> players);

    FakeEntityPose getEntityPose();

    void destroyEntity(@NonNull Collection<? extends Player> players);

    void teleport(@NonNull Location location);

    void look(@NonNull Player player);

    void look(float yaw, float pitch);

    void startPlayerLooker(int delay);

    ItemStack getEquipment(@NonNull EnumWrappers.ItemSlot itemSlot);

    void setEquipment(@NonNull EnumWrappers.ItemSlot itemSlot, @NonNull ItemStack itemStack);

    boolean isBurning();

    boolean isSneaking();

    boolean isSprinting();

    boolean isInvisible();

    boolean isBlocking();

    // blocking became swimming in newer versions, make new methonds for it
    boolean isSwimming();

    boolean isCustomNameVisible();

    boolean isGlowing();

    boolean isElytraFlying();

    boolean isGravity();

    String getCustomName();

    void setBurning(boolean burning);

    void setSneaking(boolean sneaking);

    void setSprinting(boolean sprinting);

    void setInvisible(boolean invisible);

    void setBlocking(boolean blocking);

    void setSwimming(boolean swimming);

    void setGlowing(boolean glowing);

    void setCustomName(String customName);

    void setCustomNameVisible(boolean customNameVisible);

    void setElytraFlying(boolean elytraFlying);

    void setGravity(boolean gravity);

    void playAnimation(@NonNull Player player, @NonNull FakeEntityAnimation entityAnimation);

    void playAnimation(@NonNull FakeEntityAnimation entityAnimation);

    WrappedDataWatcher getDataWatcher();

}
