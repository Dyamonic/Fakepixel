package club.abelix.api.nms.v1_12.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.creeper.FakeEntityCreeper;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeCreeper1_12 extends FakeLivingEntity1_12 implements FakeEntityCreeper {

    private static final WrappedDataWatcher.WrappedDataWatcherObject ON_POWERED =
            new WrappedDataWatcher.WrappedDataWatcherObject(13, BYTE_SERIALIZER);

    private boolean powered;

    public FakeCreeper1_12(NmsApi nmsApi, Location location) {
        super(nmsApi, EntityType.CREEPER, location);
    }

    @Override
    public void setPowered(boolean powered) {
        if (this.powered == powered) {
            return;
        }

        this.powered = powered;

        getDataWatcher().setObject(ON_POWERED, (byte) (powered ? 1 : 0));
        broadcastDataWatcherPacket();
    }

    @Override
    public boolean isPowered() {
        return powered;
    }
}
