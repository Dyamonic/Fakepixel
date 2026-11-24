package club.abelix.api.nms.entity.type.object;

import club.abelix.api.nms.entity.type.FakeEntity;
import org.bukkit.util.Vector;

public interface FakeEntityObject extends FakeEntity {

    Vector getVelocity();

    int getData();

    int getSpawnTypeId();

}
