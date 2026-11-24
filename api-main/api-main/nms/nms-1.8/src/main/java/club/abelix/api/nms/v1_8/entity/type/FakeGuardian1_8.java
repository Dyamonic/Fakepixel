package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.guardian.FakeEntityGuardian;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeGuardian1_8 extends FakeLivingEntity1_8 implements FakeEntityGuardian {

    @Getter
    private int targetId;

    @Getter
    private boolean shooting;

    @Getter
    private boolean elder;

    public FakeGuardian1_8(NmsApi api, Location location) {
        super(api, EntityType.GUARDIAN, location);
    }

    @Override
    public void setShooting(boolean shooting) {
        if (this.shooting == shooting) {
            return;
        }

        this.shooting = shooting;

        getDataWatcher().setObject(16, 0x02);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setTarget(int entityId) {
        this.targetId = entityId;

        getDataWatcher().setObject(17, entityId);
        broadcastDataWatcherPacket();
    }

    @Override
    public void setElder(boolean elder) {
        this.elder = elder;

        getDataWatcher().setObject(16, elder ? 0x02 : 0x00);
        broadcastDataWatcherPacket();
    }
}