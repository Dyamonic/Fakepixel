package club.abelix.api.nms.entity.type.sheep;

import club.abelix.api.nms.entity.type.FakeLivingEntity;
import lombok.NonNull;
import org.bukkit.DyeColor;

public interface FakeEntitySheep extends FakeLivingEntity {

    void setColor(@NonNull DyeColor color);

    void setSheared(boolean sheared);

}
