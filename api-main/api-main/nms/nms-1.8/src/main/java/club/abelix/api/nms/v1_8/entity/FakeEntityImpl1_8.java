package club.abelix.api.nms.v1_8.entity;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.FakeEntityBase;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

@Getter
public class FakeEntityImpl1_8 extends FakeEntityBase {

    @Setter
    private boolean glowing, elytraFlying;

    private boolean burning, sneaking, sprinting, invisible, blocking, customNameVisible;

    public FakeEntityImpl1_8(@NonNull NmsApi nmsApi, @NonNull EntityType entityType, @NonNull Location location) {
        super(nmsApi, entityType, location);
    }

    @Override
    public void removePassenger() {
        getReceivers().forEach(passenger::removeReceiver);
        broadcastPacket(nmsApi.getPacketLibraryApi().getEntityAttachmentPacket(-1, getEntityId()));
        // https://wiki.vg/index.php?title=Protocol&oldid=6553#Attach_Entity

        this.passenger = null;
        this.passengerId = 0;
    }

    @Override
    public boolean isGravity() {
        return true;
    }

    @Override
    public void setBurning(boolean burning) {
        if (this.burning == burning) {
            return;
        }

        this.burning = burning;

        getDataWatcher().setObject(0, getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setSneaking(boolean sneaking) {
        if (this.sneaking == sneaking) {
            return;
        }

        this.sneaking = sneaking;

        getDataWatcher().setObject(0, getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setSprinting(boolean sprinting) {
        if (this.sprinting == sprinting) {
            return;
        }

        this.sprinting = sprinting;

        getDataWatcher().setObject(0, getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setInvisible(boolean invisible) {
        if (this.invisible == invisible) {
            return;
        }

        this.invisible = invisible;

        getDataWatcher().setObject(0, getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setBlocking(boolean blocking) {
        if (this.blocking == blocking) {
            return;
        }

        this.blocking = blocking;

        getDataWatcher().setObject(0, getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setSwimming(boolean swimming) {
        setBlocking(swimming);
    }

    @Override
    public void setCustomNameVisible(boolean customNameVisible) {
        if (this.customNameVisible == customNameVisible) {
            return;
        }

        this.customNameVisible = customNameVisible;

        getDataWatcher().setObject(3, (byte) (customNameVisible ? 1 : 0));
        broadcastDataWatcherPacket();
    }

    @Override
    public void setGravity(boolean gravity) {
        // not supported by default in 1.8
    }

    @Override
    public boolean isSwimming() {
        return blocking;
    }

    protected void broadcastDataWatcherPacket() {
        broadcastPacket(nmsApi.getPacketLibraryApi().getDataWatcherPacket(getEntityId(), getDataWatcher()));
    }

    private byte getBitMask() {
        return (byte) ((burning ? 0x01 : 0)
                + (sneaking ? 0x02 : 0)
                + (sprinting ? 0x08 : 0)
                + (blocking ? 0x10 : 0)
                + (invisible ? 0x20 : 0));
    }

}
