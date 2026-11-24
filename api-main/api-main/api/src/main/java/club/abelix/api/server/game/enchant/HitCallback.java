package club.abelix.api.server.game.enchant;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;

/**
 * Created by saharNooby on 23:15 08.11.2017
 */
public interface HitCallback {

    // Ентити может быть нулл
    void onHit(Projectile projectile, Entity entity);

}
