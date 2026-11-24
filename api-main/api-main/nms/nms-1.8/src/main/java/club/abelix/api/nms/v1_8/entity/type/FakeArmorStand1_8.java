package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import com.comphenix.protocol.wrappers.Vector3F;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeArmorStand1_8 extends FakeLivingEntity1_8 implements FakeEntityArmorStand {

    private boolean arms, basePlate;

    @Getter
    private boolean small, gravity, marker;

    public FakeArmorStand1_8(NmsApi nmsApi, Location location) {
        super(nmsApi, EntityType.ARMOR_STAND, location);
    }

    @Override
    public void setHeadPose(Vector3F vector) {
        getDataWatcher().setObject(11, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setBodyPose(Vector3F vector) {
        getDataWatcher().setObject(12, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setLeftArmPose(Vector3F vector) {
        getDataWatcher().setObject(13, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setRightArmPose(Vector3F vector) {
        getDataWatcher().setObject(14, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setLeftLegPose(Vector3F vector) {
        getDataWatcher().setObject(15, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setRightLegPose(Vector3F vector) {
        getDataWatcher().setObject(16, vector);
        broadcastDataWatcherPacket();
    }


    @Override
    public Vector3F getHeadPose() {
        return (Vector3F) getDataWatcher().getObject(11);
    }

    @Override
    public Vector3F getBodyPose() {
        return (Vector3F) getDataWatcher().getObject(12);
    }

    @Override
    public Vector3F getLeftArmPose() {
        return (Vector3F) getDataWatcher().getObject(13);
    }

    @Override
    public Vector3F getRightArmPose() {
        return (Vector3F) getDataWatcher().getObject(14);
    }

    @Override
    public Vector3F getLeftLegPose() {
        return (Vector3F) getDataWatcher().getObject(15);
    }

    @Override
    public Vector3F getRightLegPose() {
        return (Vector3F) getDataWatcher().getObject(16);
    }

    @Override
    public void setBasePlate(boolean basePlate) {
        this.basePlate = basePlate;

        getDataWatcher().setObject(10, getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setArms(boolean arms) {
        this.arms = arms;

        getDataWatcher().setObject(10, getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setSmall(boolean small) {
        this.small = small;

        getDataWatcher().setObject(10, getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public boolean hasBasePlate() {
        return basePlate;
    }

    @Override
    public boolean hasArms() {
        return arms;
    }

    @Override
    public void setMarker(boolean marker) {
        this.marker = marker;

        getDataWatcher().setObject(10, getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setGravity(boolean gravity) {
        this.gravity = gravity;

        getDataWatcher().setObject(10, getBitMask());
        broadcastDataWatcherPacket();
    }

    private byte getBitMask() {
        return (byte) ((small ? 0x01 : 0) + (gravity ? 0x02 : 0) + (arms ? 0x04 : 0) + (!basePlate ? 0x08 : 0) + (marker ? 0x10 : 0));
    }

}
