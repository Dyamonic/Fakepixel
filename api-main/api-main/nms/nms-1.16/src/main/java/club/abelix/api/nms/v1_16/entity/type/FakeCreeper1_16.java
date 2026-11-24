package club.abelix.api.nms.v1_16.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.creeper.FakeEntityCreeper;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeCreeper1_16 extends FakeLivingEntity1_16 implements FakeEntityCreeper {

    private static final WrappedDataWatcher.WrappedDataWatcherObject ON_POWERED =
            new WrappedDataWatcher.WrappedDataWatcherObject(15, BOOLEAN_SERIALIZER);

    private boolean powered;

    public FakeCreeper1_16(NmsApi nmsApi, Location location) {
        super(nmsApi, EntityType.CREEPER, location);

        getDataWatcher().setObject(ON_POWERED, 0);
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
