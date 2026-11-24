package club.abelix.api.nms.entity.type.villager;

import club.abelix.api.nms.entity.type.FakeLivingEntity;
import lombok.NonNull;

public interface FakeEntityVillager extends FakeLivingEntity {

    void setProfession(@NonNull VillagerProfession villagerProfession);

    VillagerProfession getProfession();

}
