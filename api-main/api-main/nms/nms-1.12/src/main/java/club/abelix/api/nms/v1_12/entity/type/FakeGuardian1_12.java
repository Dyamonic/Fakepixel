package club.abelix.api.nms.v1_12.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.guardian.FakeEntityGuardian;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeGuardian1_12 extends FakeLivingEntity1_12 implements FakeEntityGuardian {

    // ----------------------------------------------------------------------

    private static final WrappedDataWatcher.WrappedDataWatcherObject GUARDIAN_SPIKES_BOOLEAN
            = new WrappedDataWatcher.WrappedDataWatcherObject(12, BOOLEAN_SERIALIZER);

    private static final WrappedDataWatcher.WrappedDataWatcherObject GUARDIAN_TARGET_OBJECT
            = new WrappedDataWatcher.WrappedDataWatcherObject(13, INT_SERIALIZER);

    // ----------------------------------------------------------------------


    @Getter
    private int targetId;

    @Getter
    private boolean shooting;

    public FakeGuardian1_12(NmsApi api, Location location) {
        super(api, EntityType.GUARDIAN, location);
    }

    @Override
    public void setShooting(boolean shooting) {
        if (this.shooting == shooting) return;

        this.shooting = shooting;

        getDataWatcher().setObject(GUARDIAN_SPIKES_BOOLEAN, shooting);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setTarget(int entityId) {
        this.targetId = entityId;

        getDataWatcher().setObject(GUARDIAN_TARGET_OBJECT, entityId);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setElder(boolean elder) {
        // need to be FakeElderGuardian
    }
}
