package club.abelix.api.nms.entity.custom;

import org.bukkit.entity.Entity;

/**
 * @author Unidentified Person
 */
public interface NmsEntity<E extends Entity> {

    E asBukkitEntity();

}
