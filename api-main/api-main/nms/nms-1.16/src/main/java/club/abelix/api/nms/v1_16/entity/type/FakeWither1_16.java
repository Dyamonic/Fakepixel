package club.abelix.api.nms.v1_16.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.wither.FakeEntityWither;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeWither1_16 extends FakeLivingEntity1_16 implements FakeEntityWither {
    public FakeWither1_16(NmsApi nmsApi, Location location) {
        super(nmsApi, EntityType.WITHER, location);
    }

    @Override
    public int getInvulnerableTime() {
        return 0;
    }

    @Override
    public void setInvulnerableTime(int i) {
    }
}
