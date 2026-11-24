package club.abelix.api.nms.v1_16.entity;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.pos.FakeEntityPose;
import club.abelix.api.nms.entity.type.FakeEntityBase;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.Vector3F;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.server.v1_16_R3.DataWatcherRegistry;
import net.minecraft.server.v1_16_R3.EntityPose;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeEntityImpl1_16 extends FakeEntityBase {

    protected static final WrappedDataWatcher.Serializer POSE_SERIALIZER =
            new WrappedDataWatcher.Serializer(EntityPose.class, DataWatcherRegistry.s, false);

    /* ProtocolLib Serializers */

    protected static final WrappedDataWatcher.Serializer BYTE_SERIALIZER = WrappedDataWatcher.Registry.get(Byte.class);
    protected static final WrappedDataWatcher.Serializer INT_SERIALIZER = WrappedDataWatcher.Registry.get(Integer.class);
    protected static final WrappedDataWatcher.Serializer CHAT_COMPONENT_SERIALIZER = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
    protected static final WrappedDataWatcher.Serializer BOOLEAN_SERIALIZER = WrappedDataWatcher.Registry.get(Boolean.class);
    protected static final WrappedDataWatcher.Serializer ROTATION_SERIALIZER = WrappedDataWatcher.Registry.get(Vector3F.getMinecraftClass());
    protected static final WrappedDataWatcher.Serializer ITEMSTACK_SERIALIZER = WrappedDataWatcher.Registry.get(MinecraftReflection.getItemStackClass());

    /* ProtocolLib Serializers */

    private static final EntityPose[] ENTITY_POSES = EntityPose.values();

    @Getter
    private boolean burning, sneaking, sprinting, invisible, glowing, elytraFlying, customNameVisible, swimming, gravity = true;

    @Getter
    protected FakeEntityPose entityPose;

    public FakeEntityImpl1_16(@NonNull NmsApi nmsApi, @NonNull EntityType entityType, @NonNull Location location) {
        super(nmsApi, nmsApi.getFakeEntityApi().getAndIncrementEntityId(), entityType, location);
    }

    @Override
    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), getBitMask());
        setPose(sneaking ? FakeEntityPose.SNEAKING : FakeEntityPose.STANDING);
    }

    @Override
    public void removePassenger() {
        getReceivers().forEach(passenger::removeReceiver);
        broadcastPacket(nmsApi.getPacketLibraryApi().getEntityAttachmentPacket(passenger.getEntityId(), -1));
        // https://wiki.vg/index.php?title=Protocol&oldid=13363#Attach_Entity

        this.passenger = null;
        this.passengerId = 0;
    }

    @Override
    public void setGravity(boolean gravity) {
        this.gravity = gravity;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(5, BOOLEAN_SERIALIZER), !gravity);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setBurning(boolean burning) {
        this.burning = burning;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setInvisible(boolean invisible) {
        this.invisible = invisible;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setGlowing(boolean glowing) {
        this.glowing = glowing;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER),
                getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public boolean isBlocking() {
        return isSwimming();
    }

    @Override
    public void setBlocking(boolean blocking) {
        setSwimming(blocking);
    }

    @Override
    public void setSwimming(boolean swimming) {
        this.swimming = swimming;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER),
                getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setElytraFlying(boolean elytraFlying) {
        this.elytraFlying = elytraFlying;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setCustomNameVisible(boolean customNameVisible) {
        this.customNameVisible = customNameVisible;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, BOOLEAN_SERIALIZER), customNameVisible);
        broadcastDataWatcherPacket();
    }

    public void setPose(final FakeEntityPose pose) {
        if (entityPose == pose) {
            return;
        }

        this.entityPose = pose;

        getDataWatcher().setObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(6, POSE_SERIALIZER),
                ENTITY_POSES[pose.ordinal()]
        );
        broadcastDataWatcherPacket();
    }

    protected void broadcastDataWatcherPacket() {
        broadcastPacket(nmsApi.getPacketLibraryApi().getDataWatcherPacket(getEntityId(), getDataWatcher()));
    }

    protected byte getBitMask() {
        return (byte) ((burning ? 0x01 : 0)
                + (sneaking ? 0x02 : 0)
                + (sprinting ? 0x08 : 0)
                + (swimming ? 0x10 : 0)
                + (invisible ? 0x20 : 0)
                + (glowing ? 0x40 : 0)
                + (elytraFlying ? 0x80 : 0));
    }
}
