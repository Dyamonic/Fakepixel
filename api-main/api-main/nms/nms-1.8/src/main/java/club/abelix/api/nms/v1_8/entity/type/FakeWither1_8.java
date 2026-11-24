package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.wither.FakeEntityWither;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeWither1_8 extends FakeLivingEntity1_8 implements FakeEntityWither {
    public FakeWither1_8(NmsApi nmsApi, Location location) {
        super(nmsApi, EntityType.WITHER, location);
    }

    @Override
    public int getInvulnerableTime() {
        return getDataWatcher().getInteger(20);
    }

    @Override
    public void setInvulnerableTime(int i) {
        getDataWatcher().setObject(20, i);
    }
}
