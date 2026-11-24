package club.abelix.api.nms.v1_12.entity;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.FakeEntityBase;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.Vector3F;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeEntityImpl1_12 extends FakeEntityBase {

    /* ProtocolLib Serializers */

    protected static final WrappedDataWatcher.Serializer BYTE_SERIALIZER = WrappedDataWatcher.Registry.get(Byte.class);
    protected static final WrappedDataWatcher.Serializer INT_SERIALIZER = WrappedDataWatcher.Registry.get(Integer.class);
    protected static final WrappedDataWatcher.Serializer STRING_SERIALIZER = WrappedDataWatcher.Registry.get(String.class);
    protected static final WrappedDataWatcher.Serializer BOOLEAN_SERIALIZER = WrappedDataWatcher.Registry.get(Boolean.class);
    protected static final WrappedDataWatcher.Serializer ROTATION_SERIALIZER = WrappedDataWatcher.Registry.get(Vector3F.getMinecraftClass());
    protected static final WrappedDataWatcher.Serializer ITEMSTACK_SERIALIZER = WrappedDataWatcher.Registry.get(MinecraftReflection.getItemStackClass());

    /* ProtocolLib Serializers */

    @Getter
    private boolean burning, sneaking, sprinting, blocking, invisible, glowing, elytraFlying, customNameVisible, gravity = true;

    public FakeEntityImpl1_12(@NonNull NmsApi nmsApi, @NonNull EntityType entityType, @NonNull Location location) {
        super(nmsApi, entityType, location);
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
        if (gravity == this.gravity) return;

        this.gravity = gravity;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(5, BOOLEAN_SERIALIZER), !gravity);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setBurning(boolean burning) {
        if (burning == this.burning) return;

        this.burning = burning;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setSneaking(boolean sneaking) {
        if (sneaking == this.sneaking) return;

        this.sneaking = sneaking;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setSprinting(boolean sprinting) {
        if (sprinting == this.sprinting) return;

        this.sprinting = sprinting;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setInvisible(boolean invisible) {
        if (invisible == this.invisible) return;

        this.invisible = invisible;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setGlowing(boolean glowing) {
        if (glowing == this.glowing) return;

        this.glowing = glowing;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setElytraFlying(boolean elytraFlying) {
        if (elytraFlying == this.elytraFlying) return;

        this.elytraFlying = elytraFlying;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, BYTE_SERIALIZER), getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setCustomNameVisible(boolean customNameVisible) {
        if (customNameVisible == this.customNameVisible) return;

        this.customNameVisible = customNameVisible;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, BOOLEAN_SERIALIZER), customNameVisible);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setBlocking(boolean blocking) {
        if (blocking == this.blocking) return;

        this.blocking = blocking;

        getDataWatcher().setObject(new WrappedDataWatcher.WrappedDataWatcherObject(6, BYTE_SERIALIZER),
                (blocking ? 0x01 : 0));
        broadcastDataWatcherPacket();
    }

    @Override
    public void setSwimming(boolean swimming) {
        setBlocking(swimming);
    }

    @Override
    public boolean isSwimming() {
        return blocking;
    }

    protected void broadcastDataWatcherPacket() {
        broadcastPacket(nmsApi.getPacketLibraryApi().getDataWatcherPacket(getEntityId(), getDataWatcher()));
    }

    private byte getBitMask() {
        return (byte) ((burning ? 0x01 : 0) + (sneaking ? 0x02 : 0) + (sprinting ? 0x08 : 0) + (invisible ? 0x20 : 0) + (glowing ? 0x40 : 0) + (elytraFlying ? 0x80 : 0));
    }

}
