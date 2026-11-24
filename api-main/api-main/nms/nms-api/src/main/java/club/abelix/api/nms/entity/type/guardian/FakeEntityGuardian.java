package club.abelix.api.nms.entity.type.guardian;

import club.abelix.api.nms.entity.type.FakeLivingEntity;

public interface FakeEntityGuardian extends FakeLivingEntity {

    void setShooting(boolean shooting);

    void setTarget(int entityId);

    void setElder(boolean elder);
}

