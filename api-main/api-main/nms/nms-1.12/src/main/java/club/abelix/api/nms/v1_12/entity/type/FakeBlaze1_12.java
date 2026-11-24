package club.abelix.api.nms.v1_12.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.blaze.FakeEntityBlaze;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeBlaze1_12 extends FakeLivingEntity1_12 implements FakeEntityBlaze {

    private static final WrappedDataWatcher.WrappedDataWatcherObject ON_FIRE =
            new WrappedDataWatcher.WrappedDataWatcherObject(12, BYTE_SERIALIZER);

    private boolean blazing;

    public FakeBlaze1_12(NmsApi nmsApi, Location location) {
        super(nmsApi, EntityType.BLAZE, location);

        getDataWatcher().setObject(ON_FIRE, (byte) 0);
    }

    @Override
    public void setBlazing(boolean blazing) {
        if (this.blazing == blazing) {
            return;
        }

        this.blazing = blazing;

        getDataWatcher().setObject(ON_FIRE, (byte) (blazing ? 1 : 0));
        broadcastDataWatcherPacket();
    }
}
