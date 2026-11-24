package club.abelix.api.nms.entity.custom.fireball;

import club.abelix.api.nms.entity.custom.NmsEntity;
import org.bukkit.entity.Fireball;

/**
 * @author Unidentified Person
 */
public interface NmsFireball extends NmsEntity<Fireball> {

    void setSize(float width, float length);

    void disableRandomDirection();

    void setDirection(double d0, double d1, double d2);

}
