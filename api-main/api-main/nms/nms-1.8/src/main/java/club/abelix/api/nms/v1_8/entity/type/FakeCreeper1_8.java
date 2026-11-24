package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.creeper.FakeEntityCreeper;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeCreeper1_8 extends FakeLivingEntity1_8 implements FakeEntityCreeper {

    private boolean powered;

    public FakeCreeper1_8(NmsApi nmsApi, Location location) {
        super(nmsApi, EntityType.CREEPER, location);

        getDataWatcher().setObject(16, (byte) 0);
        getDataWatcher().setObject(17, (byte) 0);
    }

    @Override
    public void setPowered(boolean powered) {
        if (this.powered == powered) {
            return;
        }

        this.powered = powered;

        getDataWatcher().setObject(17, (byte) (powered ? 1 : 0));
        broadcastDataWatcherPacket();
    }

    @Override
    public boolean isPowered() {
        return powered;
    }
}
