package club.abelix.api.nms.entity.type.creeper;

import club.abelix.api.nms.entity.type.FakeLivingEntity;

public interface FakeEntityCreeper extends FakeLivingEntity {

    void setPowered(boolean powered);

    boolean isPowered();
}
