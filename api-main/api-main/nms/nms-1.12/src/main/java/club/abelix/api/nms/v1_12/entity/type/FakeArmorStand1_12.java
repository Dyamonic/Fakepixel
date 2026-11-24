package club.abelix.api.nms.v1_12.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.armorstand.FakeEntityArmorStand;
import com.comphenix.protocol.wrappers.Vector3F;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeArmorStand1_12 extends FakeLivingEntity1_12 implements FakeEntityArmorStand {

    // -----------------------------------------------------------------------------------

    private static final WrappedDataWatcher.WrappedDataWatcherObject HEAD_POSE_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(12, ROTATION_SERIALIZER);

    private static final WrappedDataWatcher.WrappedDataWatcherObject LEFT_LEG_POSE_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(16, ROTATION_SERIALIZER);

    private static final WrappedDataWatcher.WrappedDataWatcherObject RIGHT_LEG_POSE_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(17, ROTATION_SERIALIZER);

    private static final WrappedDataWatcher.WrappedDataWatcherObject LEFT_ARM_POSE_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(14, ROTATION_SERIALIZER);

    private static final WrappedDataWatcher.WrappedDataWatcherObject RIGHT_ARM_POSE_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(15, ROTATION_SERIALIZER);

    private static final WrappedDataWatcher.WrappedDataWatcherObject BODY_POSE_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(13, ROTATION_SERIALIZER);

    private static final WrappedDataWatcher.WrappedDataWatcherObject ARMORSTAND_PARAMETERS_OBJECT =
            new WrappedDataWatcher.WrappedDataWatcherObject(11, BYTE_SERIALIZER);

    // -----------------------------------------------------------------------------------

    private boolean arms, basePlate;

    @Getter
    private boolean small, marker;

    public FakeArmorStand1_12(NmsApi nmsApi, Location location) {
        super(nmsApi, EntityType.ARMOR_STAND, location);
    }

    @Override
    public synchronized void setLeftLegPose(Vector3F vector) {
        getDataWatcher().setObject(LEFT_LEG_POSE_OBJECT, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public synchronized void setRightLegPose(Vector3F vector) {
        getDataWatcher().setObject(RIGHT_LEG_POSE_OBJECT, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public synchronized void setRightArmPose(Vector3F vector) {
        getDataWatcher().setObject(RIGHT_ARM_POSE_OBJECT, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public synchronized void setLeftArmPose(Vector3F vector) {
        getDataWatcher().setObject(LEFT_ARM_POSE_OBJECT, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public synchronized void setBodyPose(Vector3F vector) {
        getDataWatcher().setObject(BODY_POSE_OBJECT, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public synchronized void setHeadPose(Vector3F vector) {
        getDataWatcher().setObject(HEAD_POSE_OBJECT, vector);
        broadcastDataWatcherPacket();
    }

    @Override
    public synchronized Vector3F getHeadPose() {
        return (Vector3F) getDataWatcher().getObject(HEAD_POSE_OBJECT);
    }

    @Override
    public synchronized Vector3F getBodyPose() {
        return (Vector3F) getDataWatcher().getObject(BODY_POSE_OBJECT);
    }

    @Override
    public synchronized Vector3F getLeftArmPose() {
        return (Vector3F) getDataWatcher().getObject(LEFT_ARM_POSE_OBJECT);
    }

    @Override
    public synchronized Vector3F getRightArmPose() {
        return (Vector3F) getDataWatcher().getObject(RIGHT_ARM_POSE_OBJECT);
    }

    @Override
    public synchronized Vector3F getLeftLegPose() {
        return (Vector3F) getDataWatcher().getObject(LEFT_LEG_POSE_OBJECT);
    }

    @Override
    public synchronized Vector3F getRightLegPose() {
        return (Vector3F) getDataWatcher().getObject(RIGHT_LEG_POSE_OBJECT);
    }

    @Override
    public synchronized void setBasePlate(boolean basePlate) {
        this.basePlate = basePlate;

        getDataWatcher().setObject(ARMORSTAND_PARAMETERS_OBJECT, getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public synchronized void setArms(boolean arms) {
        this.arms = arms;

        getDataWatcher().setObject(ARMORSTAND_PARAMETERS_OBJECT, getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public synchronized void setSmall(boolean small) {
        this.small = small;

        getDataWatcher().setObject(ARMORSTAND_PARAMETERS_OBJECT, getBitMask());
        broadcastDataWatcherPacket();
    }

    @Override
    public synchronized boolean hasBasePlate() {
        return basePlate;
    }

    @Override
    public synchronized boolean hasArms() {
        return arms;
    }

    @Override
    public synchronized void setMarker(boolean marker) {
        this.marker = marker;

        getDataWatcher().setObject(ARMORSTAND_PARAMETERS_OBJECT, getBitMask());
        broadcastDataWatcherPacket();
    }


    // todo enum with 1 << ordinal()
    private synchronized byte getBitMask() {
        return (byte) ((small ? 0x01 : 0) + (arms ? 0x04 : 0) + (!basePlate ? 0x08 : 0) + (marker ? 0x10 : 0));
    }
}
