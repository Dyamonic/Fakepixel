package club.abelix.api.nms.entity.type.wither;

import club.abelix.api.nms.entity.type.FakeLivingEntity;

public interface FakeEntityWither extends FakeLivingEntity {

    int getInvulnerableTime();

    void setInvulnerableTime(int i);

}
