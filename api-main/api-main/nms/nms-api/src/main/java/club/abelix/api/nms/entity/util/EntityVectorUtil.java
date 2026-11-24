package club.abelix.api.nms.entity.util;

import lombok.experimental.UtilityClass;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class EntityVectorUtil {

    public void setNoclipVector(@NotNull Entity entity, @NotNull Vector vector) {
        ((CraftEntity) entity).getHandle().noclip = true;
        entity.setVelocity(vector);
    }
}
