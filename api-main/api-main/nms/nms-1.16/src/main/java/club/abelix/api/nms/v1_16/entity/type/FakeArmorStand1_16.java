package club.abelix.api.nms.v1_16.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import club.abelix.api.nms.v1_16.entity.FakeEntityImpl1_16;
import com.comphenix.protocol.wrappers.Vector3F;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeArmorStand1_16 extends FakeLivingEntity1_16 implements FakeEntityArmorStand {

    // -----------------------------------------------------------------------------------

    private static final WrappedDataWatcher.WrappedDataWatcherObject HEAD_POSE_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(15, FakeEntityImpl1_16.ROTATION_SERIALIZER);

    private static final WrappedDataWatcher.WrappedDataWatcherObject LEFT_LEG_POSE_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(19, FakeEntityImpl1_16.ROTATION_SERIALIZER);

    private static final WrappedDataWatcher.WrappedDataWatcherObject RIGHT_LEG_POSE_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(20, FakeEntityImpl1_16.ROTATION_SERIALIZER);

    private static final WrappedDataWatcher.WrappedDataWatcherObject LEFT_ARM_POSE_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(17, FakeEntityImpl1_16.ROTATION_SERIALIZER);

    private static final WrappedDataWatcher.WrappedDataWatcherObject RIGHT_ARM_POSE_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(18, FakeEntityImpl1_16.ROTATION_SERIALIZER);

    private static final WrappedDataWatcher.WrappedDataWatcherObject BODY_POSE_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(16, FakeEntityImpl1_16.ROTATION_SERIALIZER);

    private static final WrappedDataWatcher.WrappedDataWatcherObject ARMORSTAND_PARAMETERS_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(14, FakeEntityImpl1_16.BYTE_SERIALIZER);

    // -----------------------------------------------------------------------------------

    private boolean arms, basePlate;

    @Getter
    private boolean small, marker;

    public FakeArmorStand1_16(NmsApi nmsApi, Location location) {
        super(nmsApi, EntityType.ARMOR_STAND, location);
    }

    @Override
    public void setLeftLegPose(Vector3F vector) {
        getDataWatcher().setObject(LEFT_LEG_POSE_OBJECT, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setRightLegPose(Vector3F vector) {
        getDataWatcher().setObject(RIGHT_LEG_POSE_OBJECT, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setRightArmPose(Vector3F vector) {
        getDataWatcher().setObject(RIGHT_ARM_POSE_OBJECT, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setLeftArmPose(Vector3F vector) {
        getDataWatcher().setObject(LEFT_ARM_POSE_OBJECT, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setBodyPose(Vector3F vector) {
        getDataWatcher().setObject(BODY_POSE_OBJECT, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setHeadPose(Vector3F vector) {
        getDataWatcher().setObject(HEAD_POSE_OBJECT, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public Vector3F getHeadPose() {
        return (Vector3F) getDataWatcher().getObject(HEAD_POSE_OBJECT);
    }

    @Override
    public Vector3F getBodyPose() {
        return (Vector3F) getDataWatcher().getObject(BODY_POSE_OBJECT);
    }

    @Override
    public Vector3F getLeftArmPose() {
        return (Vector3F) getDataWatcher().getObject(LEFT_ARM_POSE_OBJECT);
    }

    @Override
    public Vector3F getRightArmPose() {
        return (Vector3F) getDataWatcher().getObject(RIGHT_ARM_POSE_OBJECT);
    }

    @Override
    public Vector3F getLeftLegPose() {
        return (Vector3F) getDataWatcher().getObject(LEFT_LEG_POSE_OBJECT);
    }

    @Override
    public Vector3F getRightLegPose() {
        return (Vector3F) getDataWatcher().getObject(RIGHT_LEG_POSE_OBJECT);
    }

    @Override
    public void setBasePlate(boolean basePlate) {
        this.basePlate = basePlate;

        getDataWatcher().setObject(ARMORSTAND_PARAMETERS_OBJECT, getArmorStandBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setArms(boolean arms) {
        this.arms = arms;

        getDataWatcher().setObject(ARMORSTAND_PARAMETERS_OBJECT, getArmorStandBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public void setSmall(boolean small) {
        this.small = small;

        getDataWatcher().setObject(ARMORSTAND_PARAMETERS_OBJECT, getArmorStandBitMask());
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

        getDataWatcher().setObject(ARMORSTAND_PARAMETERS_OBJECT, getArmorStandBitMask());
        broadcastDataWatcherPacket();
    }

    private byte getArmorStandBitMask() {
        return (byte) ((small ? 0x01 : 0) + (arms ? 0x04 : 0) + (!basePlate ? 0x08 : 0) + (marker ? 0x10 : 0));
    }
}
